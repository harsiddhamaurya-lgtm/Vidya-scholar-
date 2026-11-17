package com.example.rag

/**
 * Curriculum-scoped textbook unit stored in the encrypted SQLCipher vector cache
 * and indexed by the Memory-Mapped Inverted Keyword Index.
 */
data class TextbookChunk(
    val chunkId: String,
    val standardId: String,
    val streamId: String,
    val subjectId: String,
    val subjectName: String,
    val chapterNumber: Int,
    val levelId: String,
    val topicTitle: String,
    val contentText: String,
    val formulaTags: String,
    val workedExample: String
)

data class VectorizedChunk(
    val chunk: TextbookChunk,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorizedChunk) return false
        return chunk == other.chunk && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int = 31 * chunk.hashCode() + embedding.contentHashCode()
}

data class RetrievedChunk(
    val chunk: TextbookChunk,
    val denseScore: Float,
    val bm25Score: Float,
    val fusedScore: Float,
    val matchedKeywords: List<String>
)

enum class BrainMode(
    val pillLabel: String,
    val badgeTitle: String
) {
    ONLINE_GEMINI(
        pillLabel = "Cloud Gemini • Connected",
        badgeTitle = "Cloud Gemini + Curriculum RAG"
    ),
    OFFLINE_QWEN_4BIT_RAG(
        pillLabel = "Offline 4-Bit Medium LM + RAG",
        badgeTitle = "Qwen 2.5 3B Medium LM (4-Bit) + Room FTS"
    ),
    OFFLINE_HYBRID_RAG(
        pillLabel = "Offline Room FTS + Vector RAG",
        badgeTitle = "Offline Encrypted Room FTS + Vector RAG"
    )
}

enum class ModelLifecycleState(val label: String) {
    UNLOADED("Sleeping (Unloaded)"),
    LOADING("Loading Weights..."),
    READY("Loaded in RAM"),
    LOW_RAM_FALLBACK("Low RAM • Instant RAG Mode")
}

enum class DownloadTaskState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING,
    READY,
    FAILED
}

data class ModelDownloadStatus(
    val spec: OfflineModelSpec,
    val state: DownloadTaskState = DownloadTaskState.NOT_DOWNLOADED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

enum class OfflineModelSpec(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val folderName: String,
    val primaryFileName: String,
    val tokenizerFileName: String,
    val primaryDownloadUrl: String,
    val tokenizerDownloadUrl: String,
    val approxSizeLabel: String,
    val approxBytes: Long,
    val minFreeRamMb: Long
) {
    MINILM_ENCODER(
        id = "minilm_l6_v2",
        displayName = "all-MiniLM-L6-v2 Embedding Encoder",
        subtitle = "Quantized ONNX semantic vector encoder (384-dim embeddings)",
        folderName = "minilm_l6_v2",
        primaryFileName = "model_quantized.onnx",
        tokenizerFileName = "tokenizer.json",
        primaryDownloadUrl = "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model_quantized.onnx",
        tokenizerDownloadUrl = "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/tokenizer.json",
        approxSizeLabel = "~23 MB",
        approxBytes = 24_000_000L,
        minFreeRamMb = 180L
    ),
    QWEN_2_5_3B_INT4(
        id = "qwen_2_5_3b_int4",
        displayName = "Qwen 2.5 3B Medium LM (4-Bit INT4)",
        subtitle = "Primary pre-trained 4-bit Medium Language Model (MLM) brain for step-by-step STEM & Commerce synthesis",
        folderName = "qwen_2_5_3b_int4",
        primaryFileName = "model_q4.onnx",
        tokenizerFileName = "tokenizer.json",
        primaryDownloadUrl = "https://huggingface.co/onnx-community/Qwen2.5-3B-Instruct-ONNX/resolve/main/onnx/model_q4.onnx",
        tokenizerDownloadUrl = "https://huggingface.co/onnx-community/Qwen2.5-3B-Instruct-ONNX/resolve/main/tokenizer.json",
        approxSizeLabel = "~1.9 GB",
        approxBytes = 1_950_000_000L,
        minFreeRamMb = 2_400L
    )
}

data class StandardPackStatus(
    val standardId: String,
    val streamId: String,
    val title: String,
    val subtitle: String,
    val chunkCount: Int = 0,
    val mmapIndexSizeBytes: Long = 0L,
    val isInstalled: Boolean = false,
    val isInstalling: Boolean = false,
    val installProgress: Float = 0f,
    val sqlCipherActive: Boolean = false
)

data class RagAnswerResult(
    val answerText: String,
    val brainMode: BrainMode,
    val retrievedChunks: List<RetrievedChunk>,
    val latencyMs: Long
)

data class VoiceLanguageOption(
    val code: String,
    val displayName: String,
    val nativeScript: String
)

object CosmiumVoiceLanguageCatalog {
    val allLanguages: List<VoiceLanguageOption> = listOf(
        VoiceLanguageOption("en", "English", "English"),
        VoiceLanguageOption("hi", "Hindi", "हिन्दी"),
        VoiceLanguageOption("bn", "Bengali", "বাংলা"),
        VoiceLanguageOption("ta", "Tamil", "தமிழ்"),
        VoiceLanguageOption("te", "Telugu", "తెలుగు"),
        VoiceLanguageOption("mr", "Marathi", "मराठी"),
        VoiceLanguageOption("gu", "Gujarati", "ગુજરાતી"),
        VoiceLanguageOption("kn", "Kannada", "ಕನ್ನಡ"),
        VoiceLanguageOption("ml", "Malayalam", "മലയാളം"),
        VoiceLanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        VoiceLanguageOption("or", "Odia", "ଓଡ଼ିଆ"),
        VoiceLanguageOption("as", "Assamese", "অসমীয়া"),
        VoiceLanguageOption("ur", "Urdu", "اردو"),
        VoiceLanguageOption("sa", "Sanskrit", "संस्कृतम्"),
        VoiceLanguageOption("ne", "Nepali", "नेपाली"),
        VoiceLanguageOption("kok", "Konkani", "कोंकणी"),
        VoiceLanguageOption("sd", "Sindhi", "सिंधी"),
        VoiceLanguageOption("ks", "Kashmiri", "کٲشُر"),
        VoiceLanguageOption("doi", "Dogri", "डोगरी"),
        VoiceLanguageOption("mai", "Maithili", "मैथिली"),
        VoiceLanguageOption("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ"),
        VoiceLanguageOption("mni", "Manipuri", "মৈতৈলোন্"),
        VoiceLanguageOption("brx", "Bodo", "बड़ो")
    )
}

data class OfflineVaultUiState(
    val standardId: String = "class_11",
    val streamId: String = "science_pcm",
    val isOnline: Boolean = true,
    val activeBrainMode: BrainMode = BrainMode.ONLINE_GEMINI,
    val modelLifecycle: ModelLifecycleState = ModelLifecycleState.UNLOADED,
    val freeRamMb: Long = 0L,
    val totalRamMb: Long = 0L,
    val availableStorageMb: Long = 0L,
    val packStatus: StandardPackStatus = StandardPackStatus(
        standardId = "class_11",
        streamId = "science_pcm",
        title = "Class 11 • Science (PCM)",
        subtitle = "Physics, Chemistry, Advanced Calculus & Coding"
    ),
    val miniLmStatus: ModelDownloadStatus = ModelDownloadStatus(OfflineModelSpec.MINILM_ENCODER),
    val qwenStatus: ModelDownloadStatus = ModelDownloadStatus(OfflineModelSpec.QWEN_2_5_3B_INT4),
    val showOnboardingVaultBanner: Boolean = true,
    val selectedVoiceLanguageCode: String = "en"
)
