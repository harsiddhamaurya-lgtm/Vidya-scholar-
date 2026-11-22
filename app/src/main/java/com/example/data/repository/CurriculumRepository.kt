package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.AcademicStream
import com.example.data.BatchType
import com.example.data.LevelPlaybackProgress
import com.example.data.MidVideoCheckpoint
import com.example.data.QuestLevel
import com.example.data.RealmId
import com.example.data.RealmVisualKind
import com.example.data.StandardGrade
import com.example.data.SubjectRealm
import com.example.data.VidyaQuizQuestion
import com.example.data.VidyaRepository
import com.example.security.SecurePrefsFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class CurriculumRepository(private val context: Context) {
    private val prefs by lazy {
        SecurePrefsFactory.getEncryptedPrefs(context.applicationContext, PREFS_PLAYBACK_NAME)
    }

    private val db: FirebaseFirestore? by lazy {
        runCatching {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }.getOrNull()
    }

    fun getSubjectsForStudent(
        standardId: String,
        streamId: String,
        batchType: String
    ): List<SubjectRealm> {
        val grade = StandardGrade.fromId(standardId)
        val stream = AcademicStream.fromId(streamId)
        val batch = BatchType.fromId(batchType)

        return when {
            !grade.hasStreams -> {
                // Class 9 or 10 Foundation
                listOf(
                    buildMathematicsRealm(grade, stream, batch),
                    buildScienceFoundationRealm(grade, stream, batch),
                    buildSocialScienceRealm(grade, stream, batch),
                    buildCodingAiRealm(grade, stream, batch),
                    buildEnglishRealm(grade, stream, batch)
                )
            }
            stream == AcademicStream.SCIENCE_PCB -> {
                // Class 11/12 Science PCB
                listOf(
                    buildBiologyRealm(grade, stream, batch),
                    buildPhysicsRealm(grade, stream, batch),
                    buildChemistryRealm(grade, stream, batch),
                    buildEnglishRealm(grade, stream, batch)
                )
            }
            stream == AcademicStream.COMMERCE -> {
                // Class 11/12 Commerce
                listOf(
                    buildAccountancyRealm(grade, stream, batch),
                    buildBusinessStudiesRealm(grade, stream, batch),
                    buildEconomicsRealm(grade, stream, batch),
                    buildMathematicsRealm(grade, stream, batch),
                    buildEnglishRealm(grade, stream, batch)
                )
            }
            stream == AcademicStream.ARTS_HUMANITIES -> {
                // Class 11/12 Arts / Humanities
                listOf(
                    buildHistoryRealm(grade, stream, batch),
                    buildPoliticalScienceRealm(grade, stream, batch),
                    buildEconomicsRealm(grade, stream, batch),
                    buildEnglishRealm(grade, stream, batch)
                )
            }
            else -> {
                // Default: Class 11/12 Science PCM
                listOf(
                    buildMathematicsRealm(grade, stream, batch),
                    buildPhysicsRealm(grade, stream, batch),
                    buildChemistryRealm(grade, stream, batch),
                    buildCodingAiRealm(grade, stream, batch),
                    buildEnglishRealm(grade, stream, batch)
                )
            }
        }
    }

    fun isStandardCompleted(
        standardId: String,
        streamId: String,
        batchType: String,
        completedLevelIds: Set<String>
    ): Boolean {
        val subjects = getSubjectsForStudent(standardId, streamId, batchType)
        val allRequiredIds = subjects.flatMap { realm -> realm.levels.map { it.id } }
        return allRequiredIds.isNotEmpty() && allRequiredIds.all { it in completedLevelIds }
    }

    fun getNextStandard(standardId: String): StandardGrade? {
        val curr = StandardGrade.fromId(standardId)
        val nextId = curr.nextStandardId ?: return null
        return StandardGrade.fromId(nextId)
    }

    // --- Playback Progress (Encrypted Local Storage + Async Firestore sync) ---

    fun getPlaybackProgress(vidyaId: String, levelId: String): LevelPlaybackProgress? {
        val key = makePlaybackKey(vidyaId, levelId)
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            LevelPlaybackProgress(
                levelId = json.optString("levelId", levelId),
                vidyaId = json.optString("vidyaId", vidyaId),
                lastPositionSec = json.optInt("lastPositionSec", 0),
                totalDurationSec = json.optInt("totalDurationSec", 0),
                completed = json.optBoolean("completed", false),
                updatedAtMillis = json.optLong("updatedAtMillis", 0L)
            )
        }.getOrNull()
    }

    fun savePlaybackProgress(
        vidyaId: String,
        levelId: String,
        lastPositionSec: Int,
        totalDurationSec: Int,
        completed: Boolean,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        val now = System.currentTimeMillis()
        val key = makePlaybackKey(vidyaId, levelId)
        val json = JSONObject().apply {
            put("levelId", levelId)
            put("vidyaId", vidyaId)
            put("lastPositionSec", lastPositionSec)
            put("totalDurationSec", totalDurationSec)
            put("completed", completed)
            put("updatedAtMillis", now)
        }
        prefs.edit().putString(key, json.toString()).apply()

        // Sync to cloud Firestore asynchronously when user is authenticated
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        val firestore = db
        if (user != null && firestore != null) {
            scope.launch {
                runCatching {
                    val docRef = firestore.collection("users")
                        .document(user.uid)
                        .collection("playback")
                        .document(levelId)
                    val payload = mapOf(
                        "levelId" to levelId,
                        "vidyaId" to vidyaId,
                        "lastPositionSec" to lastPositionSec,
                        "totalDurationSec" to totalDurationSec,
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload, SetOptions.merge())
                }.onFailure { e ->
                    Log.w(TAG, "Failed to sync playback progress to Firestore: ${e.message}")
                }
            }
        }
    }

    private fun makePlaybackKey(vidyaId: String, levelId: String): String {
        val cleanVidya = vidyaId.replace('#', '_').ifBlank { "guest" }
        return "pb_${cleanVidya}_$levelId"
    }

    // --- Dynamic Subject Realm Builders ---

    private fun buildMathematicsRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val baseRealm = VidyaRepository.getRealm(RealmId.MATHEMATICS)
        val prefix = "${std.id}_${stream.id}_${batch.id}_math"
        val levels = baseRealm.levels.mapIndexed { idx, lvl ->
            lvl.copy(
                id = "${prefix}_l${idx + 1}",
                levelNumber = idx + 1,
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id,
                isLiveStream = batch == BatchType.LIVE_INTERACTIVE && idx == 0
            )
        }
        return baseRealm.copy(levels = levels)
    }

    private fun buildPhysicsRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val baseRealm = VidyaRepository.getRealm(RealmId.PHYSICS)
        val prefix = "${std.id}_${stream.id}_${batch.id}_phy"
        val levels = baseRealm.levels.mapIndexed { idx, lvl ->
            lvl.copy(
                id = "${prefix}_l${idx + 1}",
                levelNumber = idx + 1,
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id,
                isLiveStream = batch == BatchType.LIVE_INTERACTIVE && idx == 0
            )
        }
        return baseRealm.copy(levels = levels)
    }

    private fun buildChemistryRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val baseRealm = VidyaRepository.getRealm(RealmId.CHEMISTRY)
        val prefix = "${std.id}_${stream.id}_${batch.id}_chem"
        val levels = baseRealm.levels.mapIndexed { idx, lvl ->
            lvl.copy(
                id = "${prefix}_l${idx + 1}",
                levelNumber = idx + 1,
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id,
                isLiveStream = batch == BatchType.LIVE_INTERACTIVE && idx == 0
            )
        }
        return baseRealm.copy(levels = levels)
    }

    private fun buildCodingAiRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val baseRealm = VidyaRepository.getRealm(RealmId.CODING_AI)
        val prefix = "${std.id}_${stream.id}_${batch.id}_ai"
        val levels = baseRealm.levels.mapIndexed { idx, lvl ->
            lvl.copy(
                id = "${prefix}_l${idx + 1}",
                levelNumber = idx + 1,
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        }
        return baseRealm.copy(levels = levels)
    }

    private fun buildEnglishRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val baseRealm = VidyaRepository.getRealm(RealmId.ENGLISH)
        val prefix = "${std.id}_${stream.id}_${batch.id}_eng"
        val levels = baseRealm.levels.mapIndexed { idx, lvl ->
            lvl.copy(
                id = "${prefix}_l${idx + 1}",
                levelNumber = idx + 1,
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        }
        return baseRealm.copy(levels = levels)
    }

    private fun buildBiologyRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_bio"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.BIOLOGY,
                levelNumber = 1,
                title = "Cell Structure & Biomolecules",
                chapterSubtitle = "Microscopic Architecture & Energy",
                videoTitle = "Cellular Organelles & Membrane Dynamics",
                videoDurationSeconds = 24,
                lectureCaptions = listOf(
                    "The cell is the structural and metabolic foundation of all life.",
                    "Phospholipid bilayers regulate active and passive transport.",
                    "ATP provides universal cellular biochemical currency."
                ),
                checkpoints = listOf(
                    MidVideoCheckpoint(
                        id = "${prefix}_l1_cp1",
                        triggerProgress = 0.45f,
                        timestampLabel = "0:10",
                        question = VidyaQuizQuestion(
                            id = "${prefix}_l1_q1",
                            prompt = "Which organelle generates the majority of cellular ATP?",
                            options = listOf("Mitochondria", "Ribosome", "Golgi apparatus", "Lysosome"),
                            correctIndex = 0,
                            explanation = "Mitochondria produce ATP via oxidative phosphorylation and the Krebs cycle."
                        )
                    )
                ),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "DNA transcription creates which primary molecule?",
                        options = listOf("mRNA", "tRNA", "Lipid", "Enzyme"),
                        correctIndex = 0,
                        explanation = "RNA Polymerase reads the DNA template to synthesize messenger RNA (mRNA)."
                    )
                ),
                scrollTitle = "Cell Biology Sutra",
                scrollSummary = "Core organelle mechanisms, DNA replication, and membrane transport.",
                keyFormulas = listOf("C6H12O6 + 6O2 -> 6CO2 + 6H2O + 38 ATP"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id,
                isLiveStream = batch == BatchType.LIVE_INTERACTIVE
            ),
            QuestLevel(
                id = "${prefix}_l2",
                realmId = RealmId.BIOLOGY,
                levelNumber = 2,
                title = "Genetics & Inheritance",
                chapterSubtitle = "Mendelian Laws & Chromosomal Mapping",
                videoTitle = "Principles of Inheritance & Variation",
                videoDurationSeconds = 22,
                lectureCaptions = listOf(
                    "Mendel established alleles and dominant-recessive traits.",
                    "Punnett squares map phenotypic and genotypic ratios.",
                    "Recombination creates biological diversity during meiosis."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l2_qt1",
                        prompt = "What is the phenotypic ratio of a standard monohybrid cross?",
                        options = listOf("3:1", "1:2:1", "9:3:3:1", "2:1"),
                        correctIndex = 0,
                        explanation = "Crossing two heterozygotes (Aa x Aa) yields 3 dominant to 1 recessive phenotype."
                    )
                ),
                scrollTitle = "Genetics Compendium",
                scrollSummary = "Mendelian ratios, sex linkage, and gene mutations.",
                keyFormulas = listOf("Monohybrid: 3:1 phenotypic, 1:2:1 genotypic"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )

        return SubjectRealm(
            realmId = RealmId.BIOLOGY,
            visualKind = RealmVisualKind.PRANA_CHAKRA,
            rankTitle = "Jeeva Siddha",
            darkSurfaceTop = Color(0xFF162B1D),
            darkSurfaceBottom = Color(0xFF0F1A12),
            lightSurfaceTop = Color(0xFFD4EBD9),
            lightSurfaceBottom = Color(0xFFBCE0C4),
            accentPrimary = Color(0xFF2EBD6E),
            accentSecondary = Color(0xFF00E5FF),
            levels = levels
        )
    }

    private fun buildAccountancyRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_acc"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.ACCOUNTANCY,
                levelNumber = 1,
                title = "Financial Accounting Principles",
                chapterSubtitle = "Double-Entry Bookkeeping & Journal Entries",
                videoTitle = "Accounting Equations & Ledger Balances",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Assets = Liabilities + Owner's Capital.",
                    "Every debit transaction must have an equal credit counterpart.",
                    "Trial balance verifies arithmetical balance before final accounts."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "Assets = Liabilities + ?",
                        options = listOf("Capital", "Expenses", "Revenue", "Debtors"),
                        correctIndex = 0,
                        explanation = "The fundamental accounting equation states Assets = Liabilities + Capital."
                    )
                ),
                scrollTitle = "Lekha Shastra Sutra",
                scrollSummary = "Debit-credit golden rules, journal ledgers, and trial balances.",
                keyFormulas = listOf("Assets = Liabilities + Equity"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id,
                isLiveStream = batch == BatchType.LIVE_INTERACTIVE
            )
        )
        return SubjectRealm(
            realmId = RealmId.ACCOUNTANCY,
            visualKind = RealmVisualKind.GOLDEN_SCALES,
            rankTitle = "Lekha Ratna",
            darkSurfaceTop = Color(0xFF2E2214),
            darkSurfaceBottom = Color(0xFF1A130A),
            lightSurfaceTop = Color(0xFFF7EBD4),
            lightSurfaceBottom = Color(0xFFEEDAC0),
            accentPrimary = Color(0xFFE5A93C),
            accentSecondary = Color(0xFF4ADE80),
            levels = levels
        )
    }

    private fun buildBusinessStudiesRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_bst"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.BUSINESS_STUDIES,
                levelNumber = 1,
                title = "Nature & Principles of Management",
                chapterSubtitle = "Fayol's 14 Principles & Taylor's Scientific Approach",
                videoTitle = "Organizational Strategy & Leadership Structure",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Management coordinates human and material resources.",
                    "Planning, organizing, staffing, directing, and controlling.",
                    "Unity of command avoids conflicting instructions."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "Who formulated the 14 Principles of Management?",
                        options = listOf("Henri Fayol", "F.W. Taylor", "Peter Drucker", "Adam Smith"),
                        correctIndex = 0,
                        explanation = "Henri Fayol authored the 14 fundamental principles of administrative management."
                    )
                ),
                scrollTitle = "Vyapar Niti Scrolls",
                scrollSummary = "Management hierarchies, operational efficiency, and team coordination.",
                keyFormulas = listOf("Functions: POSDC (Plan, Organize, Staff, Direct, Control)"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.BUSINESS_STUDIES,
            visualKind = RealmVisualKind.DHANUR_VAULT,
            rankTitle = "Vyapar Nayak",
            darkSurfaceTop = Color(0xFF1C222D),
            darkSurfaceBottom = Color(0xFF10141C),
            lightSurfaceTop = Color(0xFFD6E2F0),
            lightSurfaceBottom = Color(0xFFC0D2E5),
            accentPrimary = Color(0xFF38BDF8),
            accentSecondary = Color(0xFFFBBF24),
            levels = levels
        )
    }

    private fun buildEconomicsRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_eco"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.ECONOMICS,
                levelNumber = 1,
                title = "Microeconomics & Market Equilibrium",
                chapterSubtitle = "Elasticity of Demand & Consumer Surplus",
                videoTitle = "Supply-Demand Curves & Price Elasticity",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Price elasticity measures demand responsiveness to price changes.",
                    "Equilibrium occurs where quantity demanded equals quantity supplied.",
                    "Consumer surplus reflects value received over price paid."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "When demand is unitary elastic, Ed equals:",
                        options = listOf("1", "0", "Infinity", "< 1"),
                        correctIndex = 0,
                        explanation = "Unitary elasticity occurs when percentage change in quantity matches percentage change in price (Ed = 1)."
                    )
                ),
                scrollTitle = "Artha Shastra Compendium",
                scrollSummary = "Price mechanisms, market structures, and national income.",
                keyFormulas = listOf("Ed = (% Change in Q) / (% Change in P)"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.ECONOMICS,
            visualKind = RealmVisualKind.ARTHA_WHEEL,
            rankTitle = "Artha Shastri",
            darkSurfaceTop = Color(0xFF261C14),
            darkSurfaceBottom = Color(0xFF17100B),
            lightSurfaceTop = Color(0xFFF2E0D0),
            lightSurfaceBottom = Color(0xFFE4C9B4),
            accentPrimary = Color(0xFFFB923C),
            accentSecondary = Color(0xFF22D3EE),
            levels = levels
        )
    }

    private fun buildHistoryRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_hist"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.HISTORY,
                levelNumber = 1,
                title = "Civilizations & Trade Networks",
                chapterSubtitle = "Harappan Architecture & Vedic Epigraphy",
                videoTitle = "Urban Planning in Ancient Indus Valley",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Standardized baked brick grids and drainage systems.",
                    "Long-distance maritime trade with ancient Mesopotamia.",
                    "The Great Bath symbolizes community ritual architecture."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "Which Harappan city featured a dockyard connecting to the sea?",
                        options = listOf("Lothal", "Harappa", "Mohenjo-daro", "Kalibangan"),
                        correctIndex = 0,
                        explanation = "Lothal in modern Gujarat served as the prominent port and dockyard of the Indus civilization."
                    )
                ),
                scrollTitle = "Itihasa Epigraphs",
                scrollSummary = "Archaeological findings, trade routes, and urban layouts.",
                keyFormulas = listOf("Timeline: Mature Harappan (2600–1900 BCE)"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.HISTORY,
            visualKind = RealmVisualKind.KALACHAKRA,
            rankTitle = "Itihasa Vidwan",
            darkSurfaceTop = Color(0xFF271A1A),
            darkSurfaceBottom = Color(0xFF170E0E),
            lightSurfaceTop = Color(0xFFF2DEDE),
            lightSurfaceBottom = Color(0xFFE5C5C5),
            accentPrimary = Color(0xFFE11D48),
            accentSecondary = Color(0xFFF59E0B),
            levels = levels
        )
    }

    private fun buildPoliticalScienceRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_${stream.id}_${batch.id}_pol"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.POLITICAL_SCIENCE,
                levelNumber = 1,
                title = "Constitution as a Living Document",
                chapterSubtitle = "Preamble, Fundamental Rights & Federal Checks",
                videoTitle = "Constituent Assembly Debates & Democratic Framework",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "The Indian Constitution balances rigidity with progressive amendment flexibility.",
                    "Fundamental Rights protect individual liberty against state excess.",
                    "Judicial review maintains separation of powers."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "Who was the Chairman of the Drafting Committee of the Indian Constitution?",
                        options = listOf("Dr. B.R. Ambedkar", "Dr. Rajendra Prasad", "Jawaharlal Nehru", "Sardar Patel"),
                        correctIndex = 0,
                        explanation = "Dr. B.R. Ambedkar led the Drafting Committee that shaped the Indian Constitution."
                    )
                ),
                scrollTitle = "Rajneeti Shastra Scrolls",
                scrollSummary = "Constitutional governance, federalism, and fundamental rights.",
                keyFormulas = listOf("Preamble: Sovereign, Socialist, Secular, Democratic Republic"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.POLITICAL_SCIENCE,
            visualKind = RealmVisualKind.RAJASIC_SHIELD,
            rankTitle = "Nyaya Shastri",
            darkSurfaceTop = Color(0xFF1A1C29),
            darkSurfaceBottom = Color(0xFF0F1019),
            lightSurfaceTop = Color(0xFFD8DCF0),
            lightSurfaceBottom = Color(0xFFC4C9E5),
            accentPrimary = Color(0xFF818CF8),
            accentSecondary = Color(0xFFF43F5E),
            levels = levels
        )
    }

    private fun buildScienceFoundationRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_sci_found_${batch.id}"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.SCIENCE_FOUNDATION,
                levelNumber = 1,
                title = "Matter, Energy & Motion",
                chapterSubtitle = "Atoms, Chemical Reactions & Force",
                videoTitle = "Atomic Models & Universal Laws of Motion",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Matter is composed of discrete atoms and molecules.",
                    "Conservation of mass holds across all chemical changes.",
                    "Balanced and unbalanced forces dictate acceleration."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "The rate of change of momentum is proportional to:",
                        options = listOf("Applied unbalanced force", "Inertia", "Velocity", "Displacement"),
                        correctIndex = 0,
                        explanation = "Newton's second law: F = dp/dt = ma."
                    )
                ),
                scrollTitle = "Vigyan Sutras",
                scrollSummary = "Integrated foundational physics, chemistry, and biology principles.",
                keyFormulas = listOf("F = m * a", "Kinetic Energy = 0.5 * m * v^2"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.SCIENCE_FOUNDATION,
            visualKind = RealmVisualKind.ATOMIC_CHAKRA,
            rankTitle = "Vigyan Shishya",
            darkSurfaceTop = Color(0xFF1E2824),
            darkSurfaceBottom = Color(0xFF111714),
            lightSurfaceTop = Color(0xFFDCEDE5),
            lightSurfaceBottom = Color(0xFFC7E2D5),
            accentPrimary = Color(0xFF10B981),
            accentSecondary = Color(0xFF38BDF8),
            levels = levels
        )
    }

    private fun buildSocialScienceRealm(std: StandardGrade, stream: AcademicStream, batch: BatchType): SubjectRealm {
        val prefix = "${std.id}_sst_${batch.id}"
        val levels = listOf(
            QuestLevel(
                id = "${prefix}_l1",
                realmId = RealmId.SOCIAL_SCIENCE,
                levelNumber = 1,
                title = "India & The Contemporary World",
                chapterSubtitle = "Nationalism, Resource Geography & Democratic Rights",
                videoTitle = "Modern Indian Freedom Movement & Federal Polity",
                videoDurationSeconds = 20,
                lectureCaptions = listOf(
                    "Non-cooperation and Civil Disobedience united diverse sectors.",
                    "Natural resource planning ensures sustainable economic development.",
                    "Democratic rights empower citizens via universal adult franchise."
                ),
                checkpoints = emptyList(),
                quickTest = listOf(
                    VidyaQuizQuestion(
                        id = "${prefix}_l1_qt1",
                        prompt = "The Salt March to Dandi was launched in which year?",
                        options = listOf("1930", "1920", "1942", "1919"),
                        correctIndex = 0,
                        explanation = "Mahatma Gandhi launched the historic Salt March on March 12, 1930."
                    )
                ),
                scrollTitle = "Rashtra Shastra Compendium",
                scrollSummary = "Indian history, democratic politics, and economic geography.",
                keyFormulas = listOf("Democracy = Government of, by, and for the people"),
                standardId = std.id,
                streamId = stream.id,
                batchType = batch.id
            )
        )
        return SubjectRealm(
            realmId = RealmId.SOCIAL_SCIENCE,
            visualKind = RealmVisualKind.RAJASIC_SHIELD,
            rankTitle = "Rashtra Bodhi",
            darkSurfaceTop = Color(0xFF281F1A),
            darkSurfaceBottom = Color(0xFF17110E),
            lightSurfaceTop = Color(0xFFEEDCD3),
            lightSurfaceBottom = Color(0xFFDEC3B7),
            accentPrimary = Color(0xFFF97316),
            accentSecondary = Color(0xFF6366F1),
            levels = levels
        )
    }

    companion object {
        private const val TAG = "CurriculumRepository"
        private const val PREFS_PLAYBACK_NAME = "vidya_curriculum_playback_v1"
    }
}
