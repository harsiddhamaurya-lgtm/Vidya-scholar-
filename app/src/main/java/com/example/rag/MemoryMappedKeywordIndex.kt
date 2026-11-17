package com.example.rag

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.ln

/**
 * Binary Memory-Mapped Inverted Keyword Index (`*.idx.bin`) backed by `FileChannel.map(READ_ONLY)`.
 *
 * Designed for sub-10ms exact symbol, formula, and BM25 keyword navigation inside offline
 * textbook chunks without loading all text tokens into JVM heap.
 *
 * Binary Format (Big-Endian):
 * - Magic: 4 bytes ("CRG1")
 * - DocCount: Int
 * - AvgDocLength: Float
 * - Docs Section: [DocCount] entries of (UTF8 chunkId, Int docLength)
 * - TermCount: Int
 * - Terms Dictionary: [TermCount] entries of (UTF8 term, Int docFreq, Int postingCount,
 *   followed by [postingCount] pairs of (Int docOrdinal, Int termFreq))
 */
class MemoryMappedKeywordIndex(private val indexDir: File) {

    companion object {
        private const val MAGIC = 0x43524731 // "CRG1"
        private const val BM25_K1 = 1.5f
        private const val BM25_B = 0.75f

        private val STOP_WORDS = setOf(
            "the", "is", "are", "was", "were", "a", "an", "and", "or", "of", "to", "in",
            "for", "on", "with", "by", "from", "at", "as", "that", "this", "what", "how",
            "why", "when", "which", "who", "please", "explain", "solve", "tell", "me", "my",
            "can", "you", "it", "be", "into", "using", "about"
        )

        fun tokenize(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            val normalized = text.lowercase()
                .replace("f=ma", " f_eq_ma force mass acceleration newton ")
                .replace("v=ir", " v_eq_ir ohm voltage current resistance ")
                .replace("e=mc^2", " e_eq_mc2 energy relativity ")
                .replace("d/dx", " d_dx derivative differentiation calculus ")
                .replace("pv=nrt", " pv_eq_nrt ideal gas constant ")
                .replace("a=l+c", " accounting equation assets liabilities capital ")

            val rawTokens = Regex("[a-z0-9_]+(?:[+^=-][a-z0-9_]+)?").findAll(normalized)
                .map { it.value.trim() }
                .filter { it.length >= 2 && it !in STOP_WORDS }
                .toList()

            return rawTokens
        }
    }

    fun indexFileForScope(standardId: String, streamId: String): File {
        if (!indexDir.exists()) indexDir.mkdirs()
        return File(indexDir, "rag_kw_${standardId}_${streamId}.idx.bin")
    }

    fun buildAndPersist(standardId: String, streamId: String, chunks: List<TextbookChunk>): File {
        val outFile = indexFileForScope(standardId, streamId)
        if (outFile.exists()) outFile.delete()

        val postingsMap = linkedMapOf<String, MutableList<Pair<Int, Int>>>()
        val docLengths = IntArray(chunks.size)
        var totalLen = 0L

        chunks.forEachIndexed { ordinal, chunk ->
            val combinedText = buildString {
                append(chunk.topicTitle).append(' ')
                append(chunk.topicTitle).append(' ') // Boost title terms
                append(chunk.subjectName).append(' ')
                append(chunk.formulaTags).append(' ')
                append(chunk.formulaTags).append(' ') // Boost formula tags
                append(chunk.contentText).append(' ')
                append(chunk.workedExample)
            }
            val tokens = tokenize(combinedText)
            docLengths[ordinal] = tokens.size.coerceAtLeast(1)
            totalLen += docLengths[ordinal]

            val tfMap = linkedMapOf<String, Int>()
            for (token in tokens) {
                tfMap[token] = (tfMap[token] ?: 0) + 1
            }
            for ((term, tf) in tfMap) {
                postingsMap.getOrPut(term) { mutableListOf() }.add(ordinal to tf)
            }
        }

        val avgDocLen = if (chunks.isEmpty()) 1f else totalLen.toFloat() / chunks.size.toFloat()

        val byteOut = ByteArrayOutputStream(64 * 1024)
        DataOutputStream(byteOut).use { dos ->
            dos.writeInt(MAGIC)
            dos.writeInt(chunks.size)
            dos.writeFloat(avgDocLen)

            chunks.forEachIndexed { idx, chunk ->
                val idBytes = chunk.chunkId.toByteArray(Charsets.UTF_8)
                dos.writeShort(idBytes.size)
                dos.write(idBytes)
                dos.writeInt(docLengths[idx])
            }

            dos.writeInt(postingsMap.size)
            for ((term, postingList) in postingsMap) {
                val termBytes = term.toByteArray(Charsets.UTF_8)
                dos.writeShort(termBytes.size)
                dos.write(termBytes)
                dos.writeInt(postingList.size)
                for ((docOrd, tf) in postingList) {
                    dos.writeInt(docOrd)
                    dos.writeInt(tf)
                }
            }
        }

        outFile.writeBytes(byteOut.toByteArray())
        return outFile
    }

    /**
     * Queries the binary inverted index via `FileChannel.map(READ_ONLY)` (`MappedByteBuffer`).
     * Returns a map of `chunkId -> Pair(normalizedBm25Score [0..1], matchedTerms)`.
     */
    fun search(
        standardId: String,
        streamId: String,
        query: String
    ): Map<String, Pair<Float, List<String>>> {
        val file = indexFileForScope(standardId, streamId)
        if (!file.exists() || file.length() < 12L) return emptyMap()

        val queryTokens = tokenize(query).distinct()
        if (queryTokens.isEmpty()) return emptyMap()
        val querySet = queryTokens.toHashSet()

        val rawScores = linkedMapOf<Int, Float>()
        val matchedTermsByDoc = linkedMapOf<Int, MutableList<String>>()
        val docIds = mutableListOf<String>()

        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            val mapped: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size())
            mapped.order(ByteOrder.BIG_ENDIAN)

            val magic = mapped.int
            if (magic != MAGIC) return emptyMap()

            val docCount = mapped.int
            if (docCount <= 0) return emptyMap()
            val avgDocLen = mapped.float.coerceAtLeast(1f)

            val docLengths = IntArray(docCount)
            for (i in 0 until docCount) {
                val len = mapped.short.toInt() and 0xFFFF
                val bytes = ByteArray(len)
                mapped.get(bytes)
                docIds += String(bytes, Charsets.UTF_8)
                docLengths[i] = mapped.int
            }

            val termCount = mapped.int
            var matchedTermsFound = 0
            for (t in 0 until termCount) {
                val termLen = mapped.short.toInt() and 0xFFFF
                val termBytes = ByteArray(termLen)
                mapped.get(termBytes)
                val term = String(termBytes, Charsets.UTF_8)
                val postingCount = mapped.int

                if (term in querySet) {
                    matchedTermsFound++
                    val idf = ln(1.0 + ((docCount - postingCount + 0.5) / (postingCount + 0.5))).toFloat()
                        .coerceAtLeast(0.15f)
                    for (p in 0 until postingCount) {
                        val docOrd = mapped.int
                        val tf = mapped.int.toFloat()
                        val dl = docLengths[docOrd].toFloat()
                        val numerator = tf * (BM25_K1 + 1f)
                        val denominator = tf + BM25_K1 * (1f - BM25_B + BM25_B * (dl / avgDocLen))
                        val termScore = idf * (numerator / denominator)
                        rawScores[docOrd] = (rawScores[docOrd] ?: 0f) + termScore
                        matchedTermsByDoc.getOrPut(docOrd) { mutableListOf() }.add(term)
                    }
                    if (matchedTermsFound >= querySet.size) {
                        // All query terms resolved via memory-mapped cursor; exit early
                        break
                    }
                } else {
                    // Skip unread posting list bytes in O(1) cursor jump
                    mapped.position(mapped.position() + postingCount * 8)
                }
            }
        }

        if (rawScores.isEmpty()) return emptyMap()
        val maxScore = rawScores.values.maxOrNull()?.coerceAtLeast(0.001f) ?: 1f

        return rawScores.entries.associate { (docOrd, score) ->
            val chunkId = docIds[docOrd]
            val normalized = (score / maxScore).coerceIn(0f, 1f)
            val terms = matchedTermsByDoc[docOrd]?.distinct() ?: emptyList()
            chunkId to (normalized to terms)
        }
    }

    fun getIndexSizeBytes(standardId: String, streamId: String): Long {
        val file = indexFileForScope(standardId, streamId)
        return if (file.exists()) file.length() else 0L
    }

    fun deleteScope(standardId: String, streamId: String) {
        val file = indexFileForScope(standardId, streamId)
        if (file.exists()) file.delete()
    }
}
