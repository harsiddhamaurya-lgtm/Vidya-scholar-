package com.example.data

import androidx.compose.ui.graphics.Color

enum class RealmId(val subjectName: String, val subtitle: String, val courseId: String) {
    MATHEMATICS("Mathematics", "Calculus & Sacred Geometry", "math_101"),
    PHYSICS("Physics", "Quantum & Classical Laws", "physics_101"),
    CHEMISTRY("Chemistry", "Molecular Alchemy & Bonds", "chem_101"),
    CODING_AI("Coding & AI", "Neural Logic & Algorithms", "ai_coding_101"),
    ENGLISH("English", "Rhetoric & Kavya Craft", "english_101"),
    BIOLOGY("Biology", "Cellular Charkha & Genetics", "bio_101"),
    ACCOUNTANCY("Accountancy", "Balance & Lekha Shastra", "acc_101"),
    BUSINESS_STUDIES("Business Studies", "Commerce & Vyapar Niti", "bst_101"),
    ECONOMICS("Economics", "Wealth & Artha Shastra", "eco_101"),
    HISTORY("History", "Civilizations & Itihasa", "hist_101"),
    POLITICAL_SCIENCE("Political Science", "Rajneeti & Constitution", "pol_101"),
    SCIENCE_FOUNDATION("Science", "Physics, Chemistry & Living Systems", "sci_101"),
    SOCIAL_SCIENCE("Social Science", "History, Geography & Civics", "sst_101")
}

enum class RealmVisualKind {
    SRI_YANTRA_COMPASS,
    ATOMIC_CHAKRA,
    ALCHEMICAL_LOTUS,
    NEURAL_MANDALA,
    KAVYA_QUILL,
    PRANA_CHAKRA,
    GOLDEN_SCALES,
    DHANUR_VAULT,
    ARTHA_WHEEL,
    KALACHAKRA,
    RAJASIC_SHIELD
}

enum class QuestNodeState {
    COMPLETED,
    ACTIVE,
    LOCKED
}

data class VidyaQuizQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceLabel: String = "Chapter Quest"
)

data class MidVideoCheckpoint(
    val id: String,
    val triggerProgress: Float, // 0.0..1.0 along video timeline
    val timestampLabel: String,
    val question: VidyaQuizQuestion
)

data class QuestLevel(
    val id: String,
    val realmId: RealmId,
    val levelNumber: Int, // 1..5 (5 is Boss Battle)
    val title: String,
    val chapterSubtitle: String,
    val isBossBattle: Boolean = false,
    val videoTitle: String,
    val videoDurationSeconds: Int,
    val lectureCaptions: List<String>,
    val checkpoints: List<MidVideoCheckpoint>,
    val quickTest: List<VidyaQuizQuestion>, // 4 rapid questions
    val scrollTitle: String,
    val scrollSummary: String,
    val keyFormulas: List<String>,
    val bossTitle: String = "",
    val bossQuestions: List<VidyaQuizQuestion> = emptyList(), // 10 questions x 5 marks = 50 marks
    val isLiveStream: Boolean = false,
    val videoUrl: String = "",
    val standardId: String = "class_11",
    val streamId: String = "science_pcm",
    val batchType: String = "live"
)

data class SubjectRealm(
    val realmId: RealmId,
    val visualKind: RealmVisualKind,
    val rankTitle: String,
    val darkSurfaceTop: Color,
    val darkSurfaceBottom: Color,
    val lightSurfaceTop: Color,
    val lightSurfaceBottom: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val levels: List<QuestLevel>
)

data class AnsweredQuestionRecord(
    val id: String,
    val realmId: RealmId,
    val levelNumber: Int,
    val levelTitle: String,
    val questionSource: String, // "Video Checkpoint", "Quick Test", "Boss Battle"
    val questionPrompt: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String,
    val timestampMillis: Long
)

object VidyaRepository {
    val realms: List<SubjectRealm> = listOf(
        // 1. MATHEMATICS — Royal Saffron-Bronze
        SubjectRealm(
            realmId = RealmId.MATHEMATICS,
            visualKind = RealmVisualKind.SRI_YANTRA_COMPASS,
            rankTitle = "Ganita Siddha",
            darkSurfaceTop = Color(0xFF2D1E14),
            darkSurfaceBottom = Color(0xFF191310),
            lightSurfaceTop = Color(0xFFF3E2C7),
            lightSurfaceBottom = Color(0xFFE7CEAA),
            accentPrimary = Color(0xFFE88D14),
            accentSecondary = Color(0xFF00D2E6),
            levels = listOf(
                QuestLevel(
                    id = "math_l1",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 1,
                    title = "Limits & Continuity",
                    chapterSubtitle = "Approaching the infinitesimal edge",
                    videoTitle = "Visualizing Limits on Curved Paths",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "A limit asks where f(x) is heading as x approaches a.",
                        "Left-hand and right-hand paths must converge to one point.",
                        "Continuity bridges the point cleanly without jumps or holes."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "math_l1_cp1",
                            triggerProgress = 0.38f,
                            timestampLabel = "0:07",
                            question = VidyaQuizQuestion(
                                id = "math_l1_cp1_q",
                                prompt = "For lim(x->a) f(x) to exist, what must be true?",
                                options = listOf(
                                    "Left-hand limit equals right-hand limit",
                                    "f(a) must be zero",
                                    "f(x) must be a straight line",
                                    "x must equal infinity"
                                ),
                                correctIndex = 0,
                                explanation = "A limit exists only when both directional approaches converge to the exact same finite value.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "math_l1_cp2",
                            triggerProgress = 0.76f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "math_l1_cp2_q",
                                prompt = "What is lim(x->0) (sin x) / x?",
                                options = listOf("0", "1", "Infinity", "Undefined"),
                                correctIndex = 1,
                                explanation = "As x approaches 0 in radians, sin(x) matches x closely, making the ratio converge to 1.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion(
                            id = "math_l1_qt1",
                            prompt = "Evaluate lim(x->3) (x^2 - 9) / (x - 3).",
                            options = listOf("0", "3", "6", "9"),
                            correctIndex = 2,
                            explanation = "Factor numerator as (x-3)(x+3); cancel (x-3) and substitute x=3 to get 6."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l1_qt2",
                            prompt = "Which discontinuity occurs when a single point is missing on a smooth curve?",
                            options = listOf("Jump", "Removable (Hole)", "Infinite", "Oscillating"),
                            correctIndex = 1,
                            explanation = "When the limit exists but doesn't match f(a), it forms a removable hole."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l1_qt3",
                            prompt = "Evaluate lim(x->∞) (4x^2 + 1) / (2x^2 - 5).",
                            options = listOf("0", "1", "2", "4"),
                            correctIndex = 2,
                            explanation = "Divide top and bottom by x^2; the leading coefficients yield 4 / 2 = 2."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l1_qt4",
                            prompt = "If f(x) is continuous at x = c, then lim(x->c) f(x) equals:",
                            options = listOf("0", "f'(c)", "f(c)", "c"),
                            correctIndex = 2,
                            explanation = "Continuity requires the limit at c to match the actual value f(c)."
                        )
                    ),
                    scrollTitle = "Scroll of Infinitesimal Bridges",
                    scrollSummary = "Limits inspect neighborhood behavior around x = a without requiring f(a) itself to be defined. Continuity locks the bridge in place by ensuring LHL = RHL = f(a).",
                    keyFormulas = listOf(
                        "lim(x->0) sin(x)/x = 1",
                        "lim(x->0) (1 + x)^(1/x) = e",
                        "Continuity: lim(x->a-) f(x) = lim(x->a+) f(x) = f(a)"
                    )
                ),
                QuestLevel(
                    id = "math_l2",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 2,
                    title = "Derivatives & Power Rule",
                    chapterSubtitle = "Instantaneous velocity on a curve",
                    videoTitle = "Tangent Slopes & Instant Change",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Zooming into any smooth curve reveals a straight tangent line.",
                        "The derivative measures the exact slope at a single instant.",
                        "Use d/dx(x^n) = n * x^(n-1) to differentiate powers swiftly."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "math_l2_cp1",
                            triggerProgress = 0.42f,
                            timestampLabel = "0:08",
                            question = VidyaQuizQuestion(
                                id = "math_l2_cp1_q",
                                prompt = "Geometrically, what does f'(x) represent at a point?",
                                options = listOf(
                                    "Area beneath the curve",
                                    "Slope of the tangent line",
                                    "Y-intercept",
                                    "Total arc length"
                                ),
                                correctIndex = 1,
                                explanation = "The derivative at x equals the instantaneous slope of the tangent touching the curve.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "math_l2_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "math_l2_cp2_q",
                                prompt = "What is the derivative of a constant 42?",
                                options = listOf("42", "1", "0", "42x"),
                                correctIndex = 2,
                                explanation = "A constant never changes, so its rate of change (slope) is everywhere 0.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion(
                            id = "math_l2_qt1",
                            prompt = "Find d/dx (x^4).",
                            options = listOf("4x^3", "x^3", "4x^4", "3x^4"),
                            correctIndex = 0,
                            explanation = "Applying power rule: bring 4 down and reduce exponent to 3 -> 4x^3."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l2_qt2",
                            prompt = "What is d/dx (5x^2 + 3x)?",
                            options = listOf("5x + 3", "10x + 3", "10x", "5x^2 + 3"),
                            correctIndex = 1,
                            explanation = "d/dx(5x^2) = 10x and d/dx(3x) = 3, giving 10x + 3."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l2_qt3",
                            prompt = "What is the derivative of e^x?",
                            options = listOf("x*e^(x-1)", "ln(x)", "e^x", "1/x"),
                            correctIndex = 2,
                            explanation = "The natural exponential e^x is uniquely equal to its own derivative."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l2_qt4",
                            prompt = "If position s(t) = t^3, what is velocity at t = 2?",
                            options = listOf("6", "8", "12", "24"),
                            correctIndex = 2,
                            explanation = "Velocity v(t) = s'(t) = 3t^2. At t = 2, v(2) = 3(4) = 12."
                        )
                    ),
                    scrollTitle = "Scroll of Tangent Mastery",
                    scrollSummary = "Differentiation transforms secant approximations into exact tangent slopes via lim(h->0) [f(x+h)-f(x)]/h.",
                    keyFormulas = listOf(
                        "d/dx(x^n) = n * x^(n-1)",
                        "d/dx(sin x) = cos x",
                        "d/dx(e^x) = e^x"
                    )
                ),
                QuestLevel(
                    id = "math_l3",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 3,
                    title = "Chain & Product Rules",
                    chapterSubtitle = "Unwrapping nested & multiplied functions",
                    videoTitle = "Nested Layers & Product Derivatives",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Composite functions wrap inner transformations inside outer shells.",
                        "Differentiate the outer shell first, keeping the inner core intact.",
                        "Then multiply by the inner core's derivative!"
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "math_l3_cp1",
                            triggerProgress = 0.4f,
                            timestampLabel = "0:07",
                            question = VidyaQuizQuestion(
                                id = "math_l3_cp1_q",
                                prompt = "In d/dx [f(g(x))], the Chain Rule yields:",
                                options = listOf(
                                    "f'(g'(x))",
                                    "f'(g(x)) * g'(x)",
                                    "f(x) * g'(x)",
                                    "f'(x) + g'(x)"
                                ),
                                correctIndex = 1,
                                explanation = "Multiply outer derivative evaluated at g(x) by inner derivative g'(x).",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "math_l3_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "math_l3_cp2_q",
                                prompt = "For product u*v, d/dx(uv) equals:",
                                options = listOf("u'v'", "u'v + uv'", "u'v - uv'", "(u+v)'"),
                                correctIndex = 1,
                                explanation = "Product rule differentiates one factor at a time and sums both products.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion(
                            id = "math_l3_qt1",
                            prompt = "Differentiate (2x + 1)^4.",
                            options = listOf("4(2x+1)^3", "8(2x+1)^3", "2(2x+1)^3", "8(2x+1)^4"),
                            correctIndex = 1,
                            explanation = "Outer: 4(2x+1)^3, Inner derivative of 2x+1 is 2. Product = 8(2x+1)^3."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l3_qt2",
                            prompt = "Find d/dx [sin(3x)].",
                            options = listOf("cos(3x)", "3cos(3x)", "-3cos(3x)", "3sin(3x)"),
                            correctIndex = 1,
                            explanation = "Derivative of sin(u) is cos(u)*u'. Here u=3x so u'=3."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l3_qt3",
                            prompt = "Find d/dx [x * e^x].",
                            options = listOf("e^x", "x*e^x", "(x + 1)e^x", "e^x / x"),
                            correctIndex = 2,
                            explanation = "Using u'v + uv': (1)e^x + x(e^x) = (x + 1)e^x."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l3_qt4",
                            prompt = "What is d/dx [ln(5x)]?",
                            options = listOf("5/x", "1/(5x)", "1/x", "5ln(x)"),
                            correctIndex = 2,
                            explanation = "(1 / 5x) * 5 = 1/x!"
                        )
                    ),
                    scrollTitle = "Scroll of Composite Chains",
                    scrollSummary = "Whenever functions nest or multiply, avoid naive term-by-term differentiation—peel layers cleanly from outside to inside.",
                    keyFormulas = listOf(
                        "Chain Rule: (f∘g)'(x) = f'(g(x)) · g'(x)",
                        "Product Rule: (uv)' = u'v + uv'",
                        "Quotient Rule: (u/v)' = (u'v - uv') / v^2"
                    )
                ),
                QuestLevel(
                    id = "math_l4",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 4,
                    title = "Definite Integrals & Area",
                    chapterSubtitle = "Summing infinite thin slices",
                    videoTitle = "Riemann Slices to Fundamental Theorem",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Integration is the reverse mirror of differentiation.",
                        "Definite integrals sum infinitely thin vertical strips under a curve.",
                        "Evaluate F(b) - F(a) to find exact enclosed area."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "math_l4_cp1",
                            triggerProgress = 0.42f,
                            timestampLabel = "0:08",
                            question = VidyaQuizQuestion(
                                id = "math_l4_cp1_q",
                                prompt = "What does the integral ∫[a,b] f(x) dx compute for f(x) >= 0?",
                                options = listOf(
                                    "Slope at b",
                                    "Signed area under the curve from a to b",
                                    "Perimeter of the curve",
                                    "Maximum height"
                                ),
                                correctIndex = 1,
                                explanation = "A definite integral accumulates the net signed area beneath f(x) over [a,b].",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "math_l4_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "math_l4_cp2_q",
                                prompt = "What is ∫ x^2 dx?",
                                options = listOf("2x + C", "x^3 + C", "(x^3)/3 + C", "3x^3 + C"),
                                correctIndex = 2,
                                explanation = "Increase exponent by 1 and divide by the new exponent: x^3 / 3 + C.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion(
                            id = "math_l4_qt1",
                            prompt = "Compute ∫[0,2] 3x^2 dx.",
                            options = listOf("4", "6", "8", "12"),
                            correctIndex = 2,
                            explanation = "Antiderivative is x^3. Evaluate 2^3 - 0^3 = 8."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l4_qt2",
                            prompt = "Why do indefinite integrals include '+ C'?",
                            options = listOf(
                                "To adjust units",
                                "Because any constant's derivative is 0",
                                "For negative domains",
                                "To avoid infinity"
                            ),
                            correctIndex = 1,
                            explanation = "Shifting a function vertically by any constant C leaves its derivative unchanged."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l4_qt3",
                            prompt = "What is ∫ cos(x) dx?",
                            options = listOf("-sin(x) + C", "sin(x) + C", "cos(x) + C", "tan(x) + C"),
                            correctIndex = 1,
                            explanation = "Since d/dx(sin x) = cos x, the antiderivative of cos x is sin x + C."
                        ),
                        VidyaQuizQuestion(
                            id = "math_l4_qt4",
                            prompt = "What is ∫[a,a] f(x) dx?",
                            options = listOf("f(a)", "1", "0", "2a"),
                            correctIndex = 2,
                            explanation = "Integrating over an interval of zero width yields zero area."
                        )
                    ),
                    scrollTitle = "Scroll of Accumulated Areas",
                    scrollSummary = "The Fundamental Theorem of Calculus links slopes and areas: accumulating instantaneous changes reconstructs total displacement.",
                    keyFormulas = listOf(
                        "∫ x^n dx = x^(n+1)/(n+1) + C  (n != -1)",
                        "∫[a,b] f(x) dx = F(b) - F(a)",
                        "∫ (1/x) dx = ln|x| + C"
                    )
                ),
                QuestLevel(
                    id = "math_l5",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 5,
                    title = "Sri Yantra Calculus Trial",
                    chapterSubtitle = "50-Mark Boss Battle across all 5 chapters",
                    isBossBattle = true,
                    videoTitle = "Grand Calculus Synthesis",
                    videoDurationSeconds = 12,
                    lectureCaptions = listOf(
                        "Synthesize limits, derivatives, chain rules, and integrals."
                    ),
                    checkpoints = emptyList(),
                    quickTest = emptyList(),
                    scrollTitle = "Crown Scroll of Ganita Siddha",
                    scrollSummary = "You have synthesized the four pillars of Calculus—continuity, instantaneous slopes, nested transformations, and cumulative integrals—into mastery.",
                    keyFormulas = listOf(
                        "LHL = RHL = f(a)",
                        "(f∘g)' = f'(g)·g'",
                        "∫[a,b] f'(x)dx = f(b)-f(a)"
                    ),
                    bossTitle = "Acharya Bhaskara's Calculus Arena",
                    bossQuestions = listOf(
                        VidyaQuizQuestion("mb1", "Evaluate lim(x->2) (x^2 - 4)/(x - 2).", listOf("0", "2", "4", "8"), 2, "Factor (x-2)(x+2)/(x-2) = x+2 -> 4.", "Boss Trial"),
                        VidyaQuizQuestion("mb2", "If LHL = 5 and RHL = 3 at x = 1, the limit:", listOf("Equals 4", "Equals 5", "Does not exist", "Equals 0"), 2, "Unequal one-sided limits mean no overall limit exists.", "Boss Trial"),
                        VidyaQuizQuestion("mb3", "Find d/dx (6x^3 - 4x + 9).", listOf("18x^2 - 4", "18x^2 + 9", "6x^2 - 4", "18x - 4"), 0, "Power rule term by term gives 18x^2 - 4.", "Boss Trial"),
                        VidyaQuizQuestion("mb4", "Derivative of sin(2x) is:", listOf("cos(2x)", "2cos(2x)", "-2cos(2x)", "sin(2x)"), 1, "Chain rule multiplies cos(2x) by inner derivative 2.", "Boss Trial"),
                        VidyaQuizQuestion("mb5", "Find d/dx [(3x - 2)^5].", listOf("5(3x-2)^4", "15(3x-2)^4", "3(3x-2)^4", "15(3x-2)^5"), 1, "Outer 5(3x-2)^4 times inner 3 gives 15(3x-2)^4.", "Boss Trial"),
                        VidyaQuizQuestion("mb6", "Using Product Rule, d/dx [x * sin(x)] =", listOf("cos(x)", "sin(x) + x cos(x)", "x cos(x)", "sin(x) - cos(x)"), 1, "u'v + uv' = (1)sin(x) + x cos(x).", "Boss Trial"),
                        VidyaQuizQuestion("mb7", "What is ∫ 4x^3 dx?", listOf("12x^2 + C", "x^4 + C", "4x^4 + C", "x^3 + C"), 1, "4 * x^4 / 4 = x^4 + C.", "Boss Trial"),
                        VidyaQuizQuestion("mb8", "Evaluate ∫[1,3] 2x dx.", listOf("4", "6", "8", "10"), 2, "Antiderivative x^2 from 1 to 3 is 9 - 1 = 8.", "Boss Trial"),
                        VidyaQuizQuestion("mb9", "At a smooth local maximum, f'(x) equals:", listOf("1", "-1", "0", "Infinity"), 2, "The tangent at a smooth peak is horizontal, so slope = 0.", "Boss Trial"),
                        VidyaQuizQuestion("mb10", "Compute d/dx [∫[0,x] cos(t) dt].", listOf("sin(x)", "cos(x)", "-sin(x)", "0"), 1, "By Fundamental Theorem Part 1, differentiating an integral recovers the integrand cos(x).", "Boss Trial")
                    )
                )
            )
        ),

        // 2. PHYSICS — Deep Indigo-Slate
        SubjectRealm(
            realmId = RealmId.PHYSICS,
            visualKind = RealmVisualKind.ATOMIC_CHAKRA,
            rankTitle = "Astra Physicist",
            darkSurfaceTop = Color(0xFF1A2238),
            darkSurfaceBottom = Color(0xFF101524),
            lightSurfaceTop = Color(0xFFDCE4F5),
            lightSurfaceBottom = Color(0xFFC9D4EC),
            accentPrimary = Color(0xFF00D2E6),
            accentSecondary = Color(0xFFF59E0B),
            levels = listOf(
                QuestLevel(
                    id = "phys_l1",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 1,
                    title = "Newton's Laws of Motion",
                    chapterSubtitle = "Inertia, force & balanced reaction",
                    videoTitle = "Force Vectors & F = ma Dynamics",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Objects preserve velocity unless a net external force intervenes.",
                        "Acceleration scales directly with net force and inversely with mass.",
                        "Every contact force pairs with an equal opposite reaction."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "phys_l1_cp1",
                            triggerProgress = 0.4f,
                            timestampLabel = "0:07",
                            question = VidyaQuizQuestion(
                                id = "phys_l1_cp1_q",
                                prompt = "Which property measures an object's resistance to acceleration?",
                                options = listOf("Weight", "Mass", "Momentum", "Volume"),
                                correctIndex = 1,
                                explanation = "Inertial mass quantifies resistance to changes in velocity under net force.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "phys_l1_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "phys_l1_cp2_q",
                                prompt = "Action-reaction forces act on:",
                                options = listOf(
                                    "The same body",
                                    "Two different interacting bodies",
                                    "Only stationary bodies",
                                    "Only in vacuum"
                                ),
                                correctIndex = 1,
                                explanation = "Newton's Third Law pairs act on opposite bodies so they do not cancel on one object.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("p_qt1", "A 10 kg mass accelerates at 2 m/s^2. Net force?", listOf("5 N", "10 N", "20 N", "200 N"), 2, "F = m*a = 10 * 2 = 20 N."),
                        VidyaQuizQuestion("p_qt2", "If net force on a moving puck is zero, its speed:", listOf("Drops to zero", "Doubles", "Stays constant", "Reverses"), 2, "Zero net force produces zero acceleration."),
                        VidyaQuizQuestion("p_qt3", "SI unit of force is:", listOf("Joule", "Watt", "Newton", "Pascal"), 2, "1 Newton = 1 kg·m/s^2."),
                        VidyaQuizQuestion("p_qt4", "Doubling mass while keeping force constant makes acceleration:", listOf("Double", "Halve", "Quadruple", "Unchanged"), 1, "Since a = F/m, doubling mass halves acceleration.")
                    ),
                    scrollTitle = "Scroll of Classical Force",
                    scrollSummary = "Draw a free-body diagram first—every mechanics problem reduces to summing vector forces along orthogonal axes.",
                    keyFormulas = listOf("F_net = m * a", "W = m * g", "F_AB = -F_BA")
                ),
                QuestLevel(
                    id = "phys_l2",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 2,
                    title = "Work, Energy & Power",
                    chapterSubtitle = "Conserving kinetic & potential energy",
                    videoTitle = "Energy Transformation on Slopes",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Work transfers energy when a force acts along displacement.",
                        "Kinetic energy 1/2 mv^2 trades seamlessly with mgh height.",
                        "Total mechanical energy remains conserved without friction."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "phys_l2_cp1",
                            triggerProgress = 0.42f,
                            timestampLabel = "0:08",
                            question = VidyaQuizQuestion(
                                id = "phys_l2_cp1_q",
                                prompt = "Work done by a force perpendicular to motion is:",
                                options = listOf("Maximum", "Negative", "Zero", "Infinite"),
                                correctIndex = 2,
                                explanation = "W = F·d·cos(90°) = 0.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "phys_l2_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "phys_l2_cp2_q",
                                prompt = "If speed triples, kinetic energy becomes:",
                                options = listOf("3x", "6x", "9x", "1.5x"),
                                correctIndex = 2,
                                explanation = "KE = 1/2 mv^2 depends on v^2, so 3^2 = 9 times.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("p2_qt1", "SI unit of Energy and Work:", listOf("Newton", "Joule", "Watt", "Tesla"), 1, "Both Work and Energy are measured in Joules."),
                        VidyaQuizQuestion("p2_qt2", "Power is defined as:", listOf("Force × time", "Work / time", "Mass × velocity", "Force / area"), 1, "Power measures the rate at which work is performed."),
                        VidyaQuizQuestion("p2_qt3", "A 2 kg stone held 5 m high (g=10) has PE:", listOf("10 J", "20 J", "50 J", "100 J"), 3, "PE = mgh = 2 * 10 * 5 = 100 J."),
                        VidyaQuizQuestion("p2_qt4", "Total work done equals change in:", listOf("Force", "Momentum", "Kinetic Energy", "Mass"), 2, "Work-Energy Theorem: W_net = ΔKE.")
                    ),
                    scrollTitle = "Scroll of Conservation",
                    scrollSummary = "Energy never vanishes; it shifts accounts between kinetic motion, gravitational height, springs, and thermal heat.",
                    keyFormulas = listOf("W = F·d·cosθ", "KE = ½mv²", "P = W/t = F·v")
                ),
                QuestLevel(
                    id = "phys_l3",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 3,
                    title = "Electromagnetism & Waves",
                    chapterSubtitle = "Oscillating E and B fields in flight",
                    videoTitle = "Transverse Waves & Induction",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Wave speed equals frequency multiplied by wavelength: v = fλ.",
                        "Changing magnetic flux induces electromotive force.",
                        "Light travels as orthogonal electric and magnetic waves."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "phys_l3_cp1",
                            triggerProgress = 0.42f,
                            timestampLabel = "0:08",
                            question = VidyaQuizQuestion(
                                id = "phys_l3_cp1_q",
                                prompt = "In a vacuum, if wavelength doubles, light frequency:",
                                options = listOf("Doubles", "Halves", "Stays same", "Becomes zero"),
                                correctIndex = 1,
                                explanation = "Since c = fλ is constant, f and λ are inversely proportional.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "phys_l3_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "phys_l3_cp2_q",
                                prompt = "Faraday's law states induced EMF is proportional to:",
                                options = listOf("Static flux", "Rate of change of magnetic flux", "Wire mass", "Resistance"),
                                correctIndex = 1,
                                explanation = "Only changing magnetic flux ΔΦ/Δt generates induced voltage.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("p3_qt1", "Wave speed formula is:", listOf("v = f/λ", "v = fλ", "v = λ/f", "v = f + λ"), 1, "Wave velocity = frequency × wavelength."),
                        VidyaQuizQuestion("p3_qt2", "Which wave requires NO medium?", listOf("Sound", "Water ripple", "Light", "Seismic S-wave"), 2, "Electromagnetic waves propagate through vacuum."),
                        VidyaQuizQuestion("p3_qt3", "Unit of frequency is:", listOf("Hertz", "Coulomb", "Weber", "Ohm"), 0, "1 Hz = 1 cycle per second."),
                        VidyaQuizQuestion("p3_qt4", "Lenz's law enforces conservation of:", listOf("Charge", "Energy", "Mass", "Momentum"), 1, "Induced currents oppose flux changes to conserve energy.")
                    ),
                    scrollTitle = "Scroll of Radiant Fields",
                    scrollSummary = "Electricity and magnetism weave into self-sustaining electromagnetic waves flying at speed c.",
                    keyFormulas = listOf("v = f · λ", "ε = -N · dΦ/dt", "c ≈ 3 × 10^8 m/s")
                ),
                QuestLevel(
                    id = "phys_l4",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 4,
                    title = "Quantum Photons & Atoms",
                    chapterSubtitle = "Discrete packets & wave-particle duality",
                    videoTitle = "Photoelectric Effect & Energy Quanta",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Light delivers energy in discrete packets called photons: E = hf.",
                        "Frequency—not brightness—controls photoelectron max energy.",
                        "Electrons inhabit quantized orbits with discrete spectral lines."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint(
                            id = "phys_l4_cp1",
                            triggerProgress = 0.42f,
                            timestampLabel = "0:08",
                            question = VidyaQuizQuestion(
                                id = "phys_l4_cp1_q",
                                prompt = "Photon energy depends directly on:",
                                options = listOf("Amplitude", "Frequency", "Beam width", "Phase"),
                                correctIndex = 1,
                                explanation = "Planck-Einstein relation: E = h * f.",
                                sourceLabel = "Video Checkpoint"
                            )
                        ),
                        MidVideoCheckpoint(
                            id = "phys_l4_cp2",
                            triggerProgress = 0.78f,
                            timestampLabel = "0:14",
                            question = VidyaQuizQuestion(
                                id = "phys_l4_cp2_q",
                                prompt = "Increasing intensity above threshold frequency increases:",
                                options = listOf("Photon energy", "Number of emitted electrons", "Work function", "Planck's constant"),
                                correctIndex = 1,
                                explanation = "Higher intensity means more photons per second, ejecting more electrons.",
                                sourceLabel = "Video Checkpoint"
                            )
                        )
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("p4_qt1", "Formula for photon energy:", listOf("E = mv^2", "E = hf", "E = qV", "E = F/d"), 1, "E = h * f where h is Planck's constant."),
                        VidyaQuizQuestion("p4_qt2", "Who proposed light quanta to explain the photoelectric effect?", listOf("Newton", "Einstein", "Faraday", "Archimedes"), 1, "Albert Einstein won the Nobel Prize for the photoelectric quantum model."),
                        VidyaQuizQuestion("p4_qt3", "de Broglie wavelength equals:", listOf("h / p", "h * p", "p / h", "h * c"), 0, "Matter wavelength λ = h / momentum."),
                        VidyaQuizQuestion("p4_qt4", "An electron dropping to a lower orbit emits:", listOf("A proton", "A photon", "A neutron", "Zero energy"), 1, "Energy difference ΔE is emitted as a discrete photon.")
                    ),
                    scrollTitle = "Scroll of Quantum Granularity",
                    scrollSummary = "At atomic scales, energy arrives in crisp quanta and particles carry wave interference patterns.",
                    keyFormulas = listOf("E = h · f = hc / λ", "KE_max = hf - Φ", "λ = h / p")
                ),
                QuestLevel(
                    id = "phys_l5",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 5,
                    title = "Quantum Chakra Boss Trial",
                    chapterSubtitle = "50-Mark Physics Boss Battle",
                    isBossBattle = true,
                    videoTitle = "Grand Physics Synthesis",
                    videoDurationSeconds = 12,
                    lectureCaptions = listOf("Synthesize motion, energy, fields, and quanta."),
                    checkpoints = emptyList(),
                    quickTest = emptyList(),
                    scrollTitle = "Crown Scroll of Astra Physicist",
                    scrollSummary = "All four regimes—Newtonian forces, conserved energy, EM waves, and quantum photons—stand unified.",
                    keyFormulas = listOf("F=ma", "W=ΔKE", "v=fλ", "E=hf"),
                    bossTitle = "Kanada & Bose's Physics Arena",
                    bossQuestions = listOf(
                        VidyaQuizQuestion("pb1", "Net force on a 5 kg mass moving at constant 4 m/s:", listOf("20 N", "5 N", "0 N", "9 N"), 2, "Constant velocity means zero acceleration -> 0 N.", "Boss Trial"),
                        VidyaQuizQuestion("pb2", "Work done lifting 4 kg by 3 m (g = 10 m/s^2):", listOf("12 J", "40 J", "120 J", "7 J"), 2, "W = mgh = 4 * 10 * 3 = 120 J.", "Boss Trial"),
                        VidyaQuizQuestion("pb3", "If velocity doubles, kinetic energy becomes:", listOf("2x", "4x", "8x", "1/2x"), 1, "KE ~ v^2 -> 2^2 = 4x.", "Boss Trial"),
                        VidyaQuizQuestion("pb4", "A wave has frequency 50 Hz and wavelength 4 m. Speed?", listOf("12.5 m/s", "54 m/s", "200 m/s", "46 m/s"), 2, "v = fλ = 50 * 4 = 200 m/s.", "Boss Trial"),
                        VidyaQuizQuestion("pb5", "Which device generates electricity via changing magnetic flux?", listOf("Resistor", "Generator", "Capacitor", "Battery"), 1, "Generators rotate coils in magnetic fields per Faraday's Law.", "Boss Trial"),
                        VidyaQuizQuestion("pb6", "Minimum energy needed to free an electron from metal is:", listOf("Threshold bias", "Work function", "Torque", "Flux"), 1, "The work function Φ is the binding threshold.", "Boss Trial"),
                        VidyaQuizQuestion("pb7", "What increases when blue light replaces red light (same intensity)?", listOf("Wavelength", "Max photoelectron KE", "Work function", "Light speed"), 1, "Higher frequency blue photons carry greater energy hf.", "Boss Trial"),
                        VidyaQuizQuestion("pb8", "Power delivered by a 10 N force moving at 3 m/s:", listOf("3.3 W", "13 W", "30 W", "300 W"), 2, "P = F * v = 10 * 3 = 30 W.", "Boss Trial"),
                        VidyaQuizQuestion("pb9", "de Broglie wavelength decreases as particle momentum:", listOf("Decreases", "Increases", "Stays zero", "Inverts"), 1, "λ = h/p decreases as p increases.", "Boss Trial"),
                        VidyaQuizQuestion("pb10", "Action and reaction forces are always:", listOf("Equal in magnitude, opposite in direction", "Different magnitudes", "Same direction", "Only gravitational"), 0, "Newton's Third Law defines equal and opposite pairs.", "Boss Trial")
                    )
                )
            )
        ),

        // 3. CHEMISTRY — Oxidized Emerald-Basalt
        SubjectRealm(
            realmId = RealmId.CHEMISTRY,
            visualKind = RealmVisualKind.ALCHEMICAL_LOTUS,
            rankTitle = "Rasayana Alchemist",
            darkSurfaceTop = Color(0xFF152C29),
            darkSurfaceBottom = Color(0xFF0E1B1A),
            lightSurfaceTop = Color(0xFFD5E9E3),
            lightSurfaceBottom = Color(0xFFC0DDD5),
            accentPrimary = Color(0xFF10B981),
            accentSecondary = Color(0xFFD4AF37),
            levels = listOf(
                QuestLevel(
                    id = "chem_l1",
                    realmId = RealmId.CHEMISTRY,
                    levelNumber = 1,
                    title = "Covalent & Ionic Bonds",
                    chapterSubtitle = "Electron sharing vs transfer",
                    videoTitle = "Octet Stability & Bond Geometry",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Atoms seek filled valence shells to reach lowest energy.",
                        "Ionic bonds transfer electrons between metals and non-metals.",
                        "Covalent bonds share electron pairs cleanly between non-metals."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("c1_cp1", 0.4f, "0:07", VidyaQuizQuestion("c1_cp1q", "How many single covalent bonds form in H2O?", listOf("1", "2", "3", "4"), 1, "Oxygen shares one electron with each of the 2 H atoms.", "Video Checkpoint")),
                        MidVideoCheckpoint("c1_cp2", 0.78f, "0:14", VidyaQuizQuestion("c1_cp2q", "NaCl crystals are held together primarily by:", listOf("Covalent sharing", "Hydrogen bonding", "Ionic attraction", "Metallic sea"), 2, "Na+ and Cl- ions attract via electrostatic ionic forces.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("c1_qt1", "A triple covalent bond shares how many electrons?", listOf("2", "3", "6", "8"), 2, "3 shared pairs = 6 electrons."),
                        VidyaQuizQuestion("c1_qt2", "Electronegativity difference > 1.7 usually indicates:", listOf("Nonpolar covalent", "Ionic bond", "Van der Waals", "Isotope"), 1, "Large difference transfers electrons into ions."),
                        VidyaQuizQuestion("c1_qt3", "Shape of CH4 (methane) molecule:", listOf("Linear", "Planar", "Tetrahedral", "Bent"), 2, "4 bonding pairs repel at 109.5° tetrahedral angles."),
                        VidyaQuizQuestion("c1_qt4", "Most electronegative element:", listOf("Oxygen", "Nitrogen", "Fluorine", "Chlorine"), 2, "Fluorine tops the Pauling scale at 3.98.")
                    ),
                    scrollTitle = "Scroll of Atomic Bonds",
                    scrollSummary = "Valence electrons dictate molecular shape, polarity, and crystal strength.",
                    keyFormulas = listOf("Octet Rule = 8 valence e-", "Bond Order = (Bonding - Antibonding)/2", "H2O angle = 104.5°")
                ),
                QuestLevel(
                    id = "chem_l2",
                    realmId = RealmId.CHEMISTRY,
                    levelNumber = 2,
                    title = "Stoichiometry & Moles",
                    chapterSubtitle = "Counting atoms by weight",
                    videoTitle = "Avogadro's Bridge & Limiting Reagents",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "One mole packs 6.022 × 10^23 elementary particles.",
                        "Balanced equations preserve atom counts across reactions.",
                        "The limiting reagent caps total product yield."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("c2_cp1", 0.42f, "0:08", VidyaQuizQuestion("c2_cp1q", "Molar mass of CO2 (C=12, O=16):", listOf("28 g/mol", "32 g/mol", "44 g/mol", "56 g/mol"), 2, "12 + 2(16) = 44 g/mol.", "Video Checkpoint")),
                        MidVideoCheckpoint("c2_cp2", 0.78f, "0:14", VidyaQuizQuestion("c2_cp2q", "Which reactant runs out first?", listOf("Excess reagent", "Catalyst", "Limiting reagent", "Solvent"), 2, "The limiting reagent halts further product formation.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("c2_qt1", "How many moles in 36 g of H2O (M=18)?", listOf("1", "2", "3", "18"), 1, "36 / 18 = 2 moles."),
                        VidyaQuizQuestion("c2_qt2", "Molarity (M) equals:", listOf("moles / kg solvent", "moles / L solution", "grams / L", "L / mole"), 1, "Molarity = moles of solute per liter of solution."),
                        VidyaQuizQuestion("c2_qt3", "In 2H2 + O2 -> 2H2O, 4 mol H2 + 1 mol O2 produces:", listOf("4 mol H2O", "2 mol H2O", "1 mol H2O", "3 mol H2O"), 1, "O2 is limiting; 1 mol O2 yields 2 mol H2O."),
                        VidyaQuizQuestion("c2_qt4", "Avogadro's number is approximately:", listOf("3.14 × 10^8", "6.022 × 10^23", "9.81 × 10^10", "1.6 × 10^-19"), 1, "6.022 × 10^23 entities per mole.")
                    ),
                    scrollTitle = "Scroll of Molar Balance",
                    scrollSummary = "Convert grams -> moles -> stoichiometric ratio -> target grams on every quantitative problem.",
                    keyFormulas = listOf("n = m / M", "N_A = 6.022 × 10²³", "M1V1 = M2V2")
                ),
                QuestLevel(
                    id = "chem_l3",
                    realmId = RealmId.CHEMISTRY,
                    levelNumber = 3,
                    title = "Acids, Bases & pH",
                    chapterSubtitle = "Proton donors & logarithmic balance",
                    videoTitle = "Neutralization & Buffer Harmony",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Brønsted acids donate protons (H+); bases accept them.",
                        "Each step on the pH scale shifts H+ concentration 10-fold.",
                        "Buffers resist sharp pH swings using conjugate pairs."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("c3_cp1", 0.42f, "0:08", VidyaQuizQuestion("c3_cp1q", "pH of a 0.01 M HCl solution:", listOf("1", "2", "7", "12"), 1, "-log10(10^-2) = 2.", "Video Checkpoint")),
                        MidVideoCheckpoint("c3_cp2", 0.78f, "0:14", VidyaQuizQuestion("c3_cp2q", "At 25°C, pH + pOH always equals:", listOf("7", "10", "14", "100"), 2, "Water auto-ionization Kw = 10^-14 gives pH + pOH = 14.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("c3_qt1", "Neutral pure water at 25°C has pH:", listOf("0", "5", "7", "14"), 2, "[H+] = 10^-7 M -> pH = 7."),
                        VidyaQuizQuestion("c3_qt2", "Conjugate base of H2SO4:", listOf("SO4^2-", "HSO4^-", "H3SO4+", "OH-"), 1, "Removing one H+ yields HSO4^-."),
                        VidyaQuizQuestion("c3_qt3", "Acid + Base produces Salt +", listOf("Hydrogen gas", "Water", "Oxygen", "Carbon"), 1, "Neutralization yields salt and H2O."),
                        VidyaQuizQuestion("c3_qt4", "Strong acids dissociate in water:", listOf("Partially", "Completely", "Only when frozen", "Never"), 1, "Strong acids ionize ~100%.")
                    ),
                    scrollTitle = "Scroll of Equilibrium Balance",
                    scrollSummary = "Le Chatelier's principle and conjugate pairs keep chemical ecosystems steady under stress.",
                    keyFormulas = listOf("pH = -log[H+]", "pH + pOH = 14", "pH = pKa + log([A-]/[HA])")
                ),
                QuestLevel(
                    id = "chem_l4",
                    realmId = RealmId.CHEMISTRY,
                    levelNumber = 4,
                    title = "Organic Functional Groups",
                    chapterSubtitle = "Carbon skeletons & reaction mechanisms",
                    videoTitle = "Alcohols, Carbonyls & Nucleophiles",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Carbon forms four stable bonds, building chains and rings.",
                        "Functional groups act as reactive hubs on carbon backbones.",
                        "Nucleophiles seek positive centers; electrophiles accept electrons."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("c4_cp1", 0.42f, "0:08", VidyaQuizQuestion("c4_cp1q", "Which group defines an alcohol?", listOf("-COOH", "-OH", "-NH2", "-CHO"), 1, "Hydroxyl group (-OH) characterizes alcohols.", "Video Checkpoint")),
                        MidVideoCheckpoint("c4_cp2", 0.78f, "0:14", VidyaQuizQuestion("c4_cp2q", "A nucleophile is best described as:", listOf("Electron-pair donor", "Electron-pair acceptor", "Proton emitter", "Noble gas"), 0, "Nucleophiles carry lone pairs/negative charge to attack positive centers.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("c4_qt1", "Formula of benzene aromatic ring:", listOf("C6H12", "C6H6", "C5H10", "C6H14"), 1, "Benzene has 6 carbons and 6 hydrogens with delocalized π electrons."),
                        VidyaQuizQuestion("c4_qt2", "-COOH identifies a:", listOf("Ketone", "Ester", "Carboxylic acid", "Alkene"), 2, "Carboxyl group (-COOH) defines carboxylic acids."),
                        VidyaQuizQuestion("c4_qt3", "Carbon-carbon double bond (-C=C-) belongs to:", listOf("Alkanes", "Alkenes", "Alkynes", "Ethers"), 1, "Alkenes contain sp2 C=C double bonds."),
                        VidyaQuizQuestion("c4_qt4", "IUPAC suffix for ketones:", listOf("-ol", "-al", "-one", "-oic acid"), 2, "Ketones end in '-one' (e.g., propanone).")
                    ),
                    scrollTitle = "Scroll of Carbon Architecture",
                    scrollSummary = "Identify functional groups first—reaction outcomes follow electron density arrows from nucleophile to electrophile.",
                    keyFormulas = listOf("Alkane: CnH(2n+2)", "Alkene: CnH2n", "Benzene: C6H6 aromatic")
                ),
                QuestLevel(
                    id = "chem_l5",
                    realmId = RealmId.CHEMISTRY,
                    levelNumber = 5,
                    title = "Alchemical Lotus Boss Trial",
                    chapterSubtitle = "50-Mark Chemistry Boss Battle",
                    isBossBattle = true,
                    videoTitle = "Grand Chemistry Synthesis",
                    videoDurationSeconds = 12,
                    lectureCaptions = listOf("Unite bonding, stoichiometry, pH, and organic synthesis."),
                    checkpoints = emptyList(),
                    quickTest = emptyList(),
                    scrollTitle = "Crown Scroll of Rasayana",
                    scrollSummary = "From electron orbitals to organic carbon rings, you command quantitative and structural chemistry.",
                    keyFormulas = listOf("n=m/M", "pH=-log[H+]", "C6H6 aromatic"),
                    bossTitle = "Nagarjuna's Alchemy Arena",
                    bossQuestions = listOf(
                        VidyaQuizQuestion("cb1", "Number of covalent bonds in CH4:", listOf("2", "3", "4", "6"), 2, "Carbon forms 4 single C-H bonds.", "Boss Trial"),
                        VidyaQuizQuestion("cb2", "Moles in 88 g of CO2 (M=44):", listOf("1", "2", "4", "44"), 1, "88 / 44 = 2 moles.", "Boss Trial"),
                        VidyaQuizQuestion("cb3", "Solution with pH 11 is:", listOf("Strongly acidic", "Weakly acidic", "Neutral", "Basic"), 3, "pH > 7 is basic/alkaline.", "Boss Trial"),
                        VidyaQuizQuestion("cb4", "Functional group -CHO represents:", listOf("Alcohol", "Aldehyde", "Amine", "Ester"), 1, "Terminal carbonyl -CHO is an aldehyde.", "Boss Trial"),
                        VidyaQuizQuestion("cb5", "Ionic compounds conduct electricity best when:", listOf("Solid crystal", "Molten or aqueous", "Frozen dry", "In hexane"), 1, "Mobile ions carry current when dissolved or melted.", "Boss Trial"),
                        VidyaQuizQuestion("cb6", "If pH drops from 5 to 3, [H+] concentration:", listOf("Doubles", "Increases 20x", "Increases 100x", "Halves"), 2, "2 pH units = 10^2 = 100x higher [H+].", "Boss Trial"),
                        VidyaQuizQuestion("cb7", "Molecular geometry of CO2 is:", listOf("Bent", "Linear", "Pyramidal", "Tetrahedral"), 1, "Two double bonds with zero lone pairs on C give 180° linear shape.", "Boss Trial"),
                        VidyaQuizQuestion("cb8", "Reaction forming an ester from acid + alcohol:", listOf("Saponification", "Esterification", "Crackling", "Sublimation"), 1, "Condensation yields an ester + water.", "Boss Trial"),
                        VidyaQuizQuestion("cb9", "Percentage yield = (Actual / Theoretical) ×", listOf("10", "50", "100", "6.022"), 2, "Expressed out of 100%.", "Boss Trial"),
                        VidyaQuizQuestion("cb10", "Delocalized π ring stability is called:", listOf("Aromaticity", "Isomerism", "Osmosis", "Hydration"), 0, "Conjugated cyclic planar rings gain extra aromatic stability.", "Boss Trial")
                    )
                )
            )
        ),

        // 4. CODING & AI — Abyssal Teal-Obsidian
        SubjectRealm(
            realmId = RealmId.CODING_AI,
            visualKind = RealmVisualKind.NEURAL_MANDALA,
            rankTitle = "Yantra Architect",
            darkSurfaceTop = Color(0xFF132A30),
            darkSurfaceBottom = Color(0xFF0C181C),
            lightSurfaceTop = Color(0xFFD2E8EC),
            lightSurfaceBottom = Color(0xFFBDDBE1),
            accentPrimary = Color(0xFF00E5FF),
            accentSecondary = Color(0xFFE88D14),
            levels = listOf(
                QuestLevel(
                    id = "ai_l1",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 1,
                    title = "Kotlin Flows & Coroutines",
                    chapterSubtitle = "Structured concurrency & reactive streams",
                    videoTitle = "Async State Without Blocking Threads",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Suspend functions pause cleanly without freezing the UI thread.",
                        "StateFlow broadcasts live UI updates to Compose collectors.",
                        "Structured concurrency cancels child jobs automatically."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("a1_cp1", 0.4f, "0:07", VidyaQuizQuestion("a1_cp1q", "Which Dispatcher suits heavy disk/network I/O?", listOf("Dispatchers.Main", "Dispatchers.IO", "Dispatchers.Unconfined", "UI Looper"), 1, "Dispatchers.IO uses an elastic pool optimized for blocking I/O.", "Video Checkpoint")),
                        MidVideoCheckpoint("a1_cp2", 0.78f, "0:14", VidyaQuizQuestion("a1_cp2q", "StateFlow differs from regular Flow because it:", listOf("Always holds a current value", "Runs only once", "Blocks Main", "Cannot emit nulls"), 0, "StateFlow is hot and always replays its latest state value.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("a1_qt1", "Which keyword allows pausing inside a coroutine?", listOf("async", "suspend", "volatile", "inline"), 1, "suspend marks functions capable of non-blocking suspension."),
                        VidyaQuizQuestion("a1_qt2", "Lifecycle-aware collector in Compose:", listOf("collectAsStateWithLifecycle()", "runBlocking()", "Thread.sleep()", "launchNow()"), 0, "collectAsStateWithLifecycle stops collecting when UI goes to background."),
                        VidyaQuizQuestion("a1_qt3", "Which coroutine builder returns a Deferred<T> result?", listOf("launch", "async", "delay", "yield"), 1, "async returns Deferred<T> with .await()."),
                        VidyaQuizQuestion("a1_qt4", "Why avoid GlobalScope?", listOf("Slower compiler", "Leaks work beyond screen lifecycle", "Lacks generics", "Fails on JDK 11"), 1, "viewModelScope ties jobs safely to lifecycle.")
                    ),
                    scrollTitle = "Scroll of Concurrent Streams",
                    scrollSummary = "Use viewModelScope + MutableStateFlow -> collectAsStateWithLifecycle for jitter-free reactive Android UIs.",
                    keyFormulas = listOf("Dispatchers.Main -> UI", "Dispatchers.IO -> Network/DB", "StateFlow = Hot + Current State")
                ),
                QuestLevel(
                    id = "ai_l2",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 2,
                    title = "Algorithms & Complexity",
                    chapterSubtitle = "Big-O speed, hash maps & graph search",
                    videoTitle = "From O(n^2) to O(log n) Scaling",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Big-O measures growth rate as input N scales toward millions.",
                        "Binary search halves sorted domains in O(log n).",
                        "Hash tables trade memory for O(1) average lookup."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("a2_cp1", 0.42f, "0:08", VidyaQuizQuestion("a2_cp1q", "Time complexity of Binary Search on sorted array:", listOf("O(1)", "O(log n)", "O(n)", "O(n log n)"), 1, "Halving search space each step runs in O(log n).", "Video Checkpoint")),
                        MidVideoCheckpoint("a2_cp2", 0.78f, "0:14", VidyaQuizQuestion("a2_cp2q", "Shortest path in unweighted graph uses:", listOf("DFS", "BFS", "Bubble Sort", "Stack only"), 1, "Breadth-First Search explores layer by layer, guaranteeing shortest hops.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("a2_qt1", "Average lookup time in a HashMap:", listOf("O(1)", "O(log n)", "O(n)", "O(n^2)"), 0, "Hash buckets provide constant O(1) expected access."),
                        VidyaQuizQuestion("a2_qt2", "Merge Sort worst-case complexity:", listOf("O(n)", "O(n log n)", "O(n^2)", "O(2^n)"), 1, "Divide-and-conquer split + linear merge is always O(n log n)."),
                        VidyaQuizQuestion("a2_qt3", "LIFO data structure:", listOf("Queue", "Stack", "Heap", "Trie"), 1, "Stacks push and pop Last-In-First-Out."),
                        VidyaQuizQuestion("a2_qt4", "Which tree traversal prints BST values sorted?", listOf("Pre-order", "In-order", "Post-order", "Random"), 1, "Left -> Root -> Right (In-order) visits BST nodes in ascending order.")
                    ),
                    scrollTitle = "Scroll of Asymptotic Speed",
                    scrollSummary = "Pick the data structure that matches your dominant query—HashMap for exact match, Heap for top-K, BFS for shortest hops.",
                    keyFormulas = listOf("O(1) < O(log n) < O(n) < O(n log n) < O(n^2)", "BFS -> Queue", "DFS -> Stack/Recursion")
                ),
                QuestLevel(
                    id = "ai_l3",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 3,
                    title = "Neural Nets & Gradient Descent",
                    chapterSubtitle = "Weights, backprop & activation curves",
                    videoTitle = "Walking Down the Loss Landscape",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Neurons compute weighted sums passed through non-linear activations.",
                        "Loss functions measure prediction error against truth.",
                        "Backpropagation chains gradients to step weights downhill."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("a3_cp1", 0.42f, "0:08", VidyaQuizQuestion("a3_cp1q", "Why do neural networks need non-linear activations like ReLU?", listOf("Faster RAM", "Without them, stacked layers collapse to linear regression", "Smaller files", "To sort labels"), 1, "Non-linearities enable learning curved boundaries and XOR.", "Video Checkpoint")),
                        MidVideoCheckpoint("a3_cp2", 0.78f, "0:14", VidyaQuizQuestion("a3_cp2q", "Backpropagation relies mathematically on:", listOf("Binary trees", "The Chain Rule of Calculus", "Bubble sort", "Modulo"), 1, "Chain rule propagates partial derivatives backward layer by layer.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("a3_qt1", "ReLU(x) outputs:", listOf("max(0, x)", "x^2", "1/(1+e^-x)", "sin(x)"), 0, "Rectified Linear Unit zeros negative inputs and passes positives."),
                        VidyaQuizQuestion("a3_qt2", "Too large a learning rate causes gradient descent to:", listOf("Never start", "Overshoot & diverge", "Shrink data", "Become O(1)"), 1, "Oversized steps bounce past minima."),
                        VidyaQuizQuestion("a3_qt3", "Softmax converts logits into:", listOf("Integers", "Probability distribution summing to 1", "Weights", "Gradients"), 1, "Softmax normalizes exponentials across classes to sum to 1."),
                        VidyaQuizQuestion("a3_qt4", "Overfitting means the model:", listOf("Memorized training noise, fails on new data", "Undertrained", "Has zero weights", "Is too fast"), 0, "Dropout, regularization, and validation splits prevent overfitting.")
                    ),
                    scrollTitle = "Scroll of Neural Learning",
                    scrollSummary = "Forward pass computes loss; backward pass applies the calculus chain rule to nudge parameters downhill.",
                    keyFormulas = listOf("y = σ(W·x + b)", "w_new = w - η · ∇L", "ReLU(x) = max(0, x)")
                ),
                QuestLevel(
                    id = "ai_l4",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 4,
                    title = "Transformers & Prompt Engineering",
                    chapterSubtitle = "Self-attention, tokens & structured outputs",
                    videoTitle = "Attention Heads & Context Windows",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Transformers inspect every token against all others via Self-Attention.",
                        "Queries, Keys, and Values weigh contextual relevance dynamically.",
                        "Clear schemas and few-shot grounding turn LLMs into reliable systems."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("a4_cp1", 0.42f, "0:08", VidyaQuizQuestion("a4_cp1q", "What three vectors drive Transformer Self-Attention?", listOf("X, Y, Z", "Query, Key, Value", "Input, Hidden, Bias", "Mean, Variance, Norm"), 1, "Attention computes softmax(Q·K^T / √d) · V.", "Video Checkpoint")),
                        MidVideoCheckpoint("a4_cp2", 0.78f, "0:14", VidyaQuizQuestion("a4_cp2q", "To ensure valid JSON output from an LLM, specify:", listOf("All caps text", "Response schema and JSON mime type", "Longer emojis", "Lower RAM"), 1, "Structured output schemas constrain decoding reliably.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("a4_qt1", "Temperature = 0.0 makes generation:", listOf("More random", "Deterministic / greedy", "Faster by 10x", "Multilingual"), 1, "Near-zero temperature selects highest-probability tokens consistently."),
                        VidyaQuizQuestion("a4_qt2", "RAG improves accuracy by:", listOf("Deleting weights", "Retrieving grounded external docs into context", "Halving layers", "Using PNGs"), 1, "Retrieval-Augmented Generation grounds answers in verified facts."),
                        VidyaQuizQuestion("a4_qt3", "Why do Transformers scale better than RNNs?", listOf("Self-attention processes sequences in parallel", "No GPUs needed", "No tokens used", "Smaller vocab"), 0, "Transformers avoid sequential step bottlenecks during training."),
                        VidyaQuizQuestion("a4_qt4", "An embedding maps text into:", listOf("A ZIP file", "A dense semantic vector", "A SQL table", "An RGB pixel"), 1, "Semantic similarity becomes geometric distance in vector space.")
                    ),
                    scrollTitle = "Scroll of Attention Craft",
                    scrollSummary = "Self-attention connects distant concepts in O(1) sequential path length; RAG + schemas unlock production reliability.",
                    keyFormulas = listOf("Attn(Q,K,V) = softmax(QKᵀ/√dₖ)V", "RAG = Retrieve + Ground + Generate")
                ),
                QuestLevel(
                    id = "ai_l5",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 5,
                    title = "Yantra AI Boss Trial",
                    chapterSubtitle = "50-Mark Coding & AI Boss Battle",
                    isBossBattle = true,
                    videoTitle = "Grand Systems Synthesis",
                    videoDurationSeconds = 12,
                    lectureCaptions = listOf("Synthesize concurrency, algorithms, neural math, and transformers."),
                    checkpoints = emptyList(),
                    quickTest = emptyList(),
                    scrollTitle = "Crown Scroll of Yantra Architect",
                    scrollSummary = "You command modern asynchronous Kotlin architecture, algorithmic efficiency, and Transformer intelligence.",
                    keyFormulas = listOf("StateFlow + Compose", "O(n log n)", "Attention(Q,K,V)"),
                    bossTitle = "Panini's Neural Yantra Arena",
                    bossQuestions = listOf(
                        VidyaQuizQuestion("ab1", "Coroutine scope tied to an Android ViewModel:", listOf("GlobalScope", "viewModelScope", "MainScope", "ThreadScope"), 1, "viewModelScope auto-cancels when ViewModel clears.", "Boss Trial"),
                        VidyaQuizQuestion("ab2", "Complexity of searching a balanced Binary Search Tree:", listOf("O(1)", "O(log n)", "O(n)", "O(n^2)"), 1, "Balanced BST height is O(log n).", "Boss Trial"),
                        VidyaQuizQuestion("ab3", "Which algorithm guarantees shortest path in unweighted graphs?", listOf("DFS", "BFS", "Quicksort", "Kruskal"), 1, "BFS visits vertices in increasing hop distance.", "Boss Trial"),
                        VidyaQuizQuestion("ab4", "Chain rule computes parameter updates during:", listOf("Tokenization", "Backpropagation", "Hashing", "Caching"), 1, "Backprop propagates loss gradients backward.", "Boss Trial"),
                        VidyaQuizQuestion("ab5", "Output of ReLU(-7.5) is:", listOf("-7.5", "0", "7.5", "1"), 1, "max(0, -7.5) = 0.", "Boss Trial"),
                        VidyaQuizQuestion("ab6", "Softmax outputs represent:", listOf("Token IDs", "Class probabilities summing to 1", "Learning rates", "Tree depths"), 1, "Normalized probability distribution.", "Boss Trial"),
                        VidyaQuizQuestion("ab7", "Self-Attention scales dot products by 1/√d_k to:", listOf("Shrink RAM", "Prevent softmax saturation gradients", "Remove keys", "Sort arrays"), 1, "Keeps dot-product variance stable.", "Boss Trial"),
                        VidyaQuizQuestion("ab8", "Best Compose collector for Flow with lifecycle safety:", listOf("collectAsStateWithLifecycle", "rememberUpdatedState", "LaunchedEffect", "derivedStateOf"), 0, "Respects STARTED lifecycle automatically.", "Boss Trial"),
                        VidyaQuizQuestion("ab9", "Cosine similarity compares two embeddings by:", listOf("Byte length", "Angle between vectors", "ASCII sum", "String length"), 1, "Measures directional alignment regardless of magnitude.", "Boss Trial"),
                        VidyaQuizQuestion("ab10", "Dropout during training helps reduce:", listOf("Dataset size", "Overfitting / co-adaptation", "Batch count", "GPU voltage"), 1, "Randomly zeroing activations forces redundant robust features.", "Boss Trial")
                    )
                )
            )
        ),

        // 5. ENGLISH — Terracotta-Vermilion / Manuscript Plum
        SubjectRealm(
            realmId = RealmId.ENGLISH,
            visualKind = RealmVisualKind.KAVYA_QUILL,
            rankTitle = "Kavya Orator",
            darkSurfaceTop = Color(0xFF331D24),
            darkSurfaceBottom = Color(0xFF1C1116),
            lightSurfaceTop = Color(0xFFF0D9D5),
            lightSurfaceBottom = Color(0xFFE3C4BF),
            accentPrimary = Color(0xFFD6335C),
            accentSecondary = Color(0xFFE88D14),
            levels = listOf(
                QuestLevel(
                    id = "eng_l1",
                    realmId = RealmId.ENGLISH,
                    levelNumber = 1,
                    title = "Aristotle's Rhetorical Triangle",
                    chapterSubtitle = "Ethos, Pathos & Logos in speech",
                    videoTitle = "Designing Persuasive Arguments",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Ethos builds trust and speaker credibility.",
                        "Pathos stirs audience empathy and shared values.",
                        "Logos anchors claims in crisp logic and verified data."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("e1_cp1", 0.42f, "0:08", VidyaQuizQuestion("e1_cp1q", "Which appeal relies on data, evidence, and reasoning?", listOf("Ethos", "Pathos", "Logos", "Kairos"), 2, "Logos appeals to logic and factual proof.", "Video Checkpoint")),
                        MidVideoCheckpoint("e1_cp2", 0.78f, "0:14", VidyaQuizQuestion("e1_cp2q", "Citing peer-reviewed experience establishes:", listOf("Ethos", "Pathos", "Hyperbole", "Irony"), 0, "Credibility and authority reflect Ethos.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("e1_qt1", "A personal story evoking compassion uses:", listOf("Logos", "Pathos", "Syntax", "Jargon"), 1, "Pathos connects emotionally."),
                        VidyaQuizQuestion("e1_qt2", "Kairos refers to:", listOf("Grammar", "Timeliness / opportune moment", "Meter", "Tone"), 1, "Kairos delivers the right message at the decisive moment."),
                        VidyaQuizQuestion("e1_qt3", "Strong thesis statements should be:", listOf("Vague", "Debatable & specific", "A question", "200 words"), 1, "A sharp thesis stakes a clear, arguable claim."),
                        VidyaQuizQuestion("e1_qt4", "Addressing counterarguments strengthens:", listOf("Ethos and Logos", "Passive voice", "Plagiarism", "Redundancy"), 0, "Fair rebuttal proves depth and honesty.")
                    ),
                    scrollTitle = "Scroll of Persuasive Speech",
                    scrollSummary = "Balance authority (Ethos), human resonance (Pathos), and tight proof (Logos) in every essay or pitch.",
                    keyFormulas = listOf("Ethos = Trust", "Pathos = Emotion", "Logos = Logic + Proof")
                ),
                QuestLevel(
                    id = "eng_l2",
                    realmId = RealmId.ENGLISH,
                    levelNumber = 2,
                    title = "Precision Style & Clarity",
                    chapterSubtitle = "Active verbs over bloated passive prose",
                    videoTitle = "Cutting Clutter for Vivid Voice",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Active voice places the actor directly in motion.",
                        "Replace weak verb + adverb pairs with one vivid verb.",
                        "Vary sentence rhythm to keep readers leaning forward."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("e2_cp1", 0.42f, "0:08", VidyaQuizQuestion("e2_cp1q", "Which sentence uses crisp active voice?", listOf("The prize was won by Maya.", "Maya won the prize.", "It was won.", "Winning occurred."), 1, "Subject 'Maya' performs the action directly.", "Video Checkpoint")),
                        MidVideoCheckpoint("e2_cp2", 0.78f, "0:14", VidyaQuizQuestion("e2_cp2q", "Trimming 'due to the fact that' to 'because' improves:", listOf("Ambiguity", "Conciseness", "Length", "Passive tone"), 1, "Concise prose eliminates filler prepositional chains.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("e2_qt1", "Best replacement for 'walked very slowly':", listOf("Ran quickly", "Amblingly moved", "Strolled / trudged", "Did walk"), 2, "Specific verbs outperform weak adverbs."),
                        VidyaQuizQuestion("e2_qt2", "Parallel structure keeps lists:", listOf("Identical in grammatical form", "All caps", "Always negative", "Rhyming"), 0, "E.g., 'to read, to write, and to reflect'."),
                        VidyaQuizQuestion("e2_qt3", "A semicolon joins:", listOf("Two independent clauses", "A noun and verb", "Two fragments", "Title and author"), 0, "Semicolons link closely related complete sentences."),
                        VidyaQuizQuestion("e2_qt4", "Show-don't-tell relies on:", listOf("Abstract labels", "Sensory concrete details", "Footnotes", "Passive tense"), 1, "Concrete imagery lets readers feel the scene.")
                    ),
                    scrollTitle = "Scroll of Lucidity",
                    scrollSummary = "Great writing removes every word that fails to carry meaning, rhythm, or image.",
                    keyFormulas = listOf("Actor -> Action -> Object", "Cut 'very' + weak adj", "Match parallel forms")
                ),
                QuestLevel(
                    id = "eng_l3",
                    realmId = RealmId.ENGLISH,
                    levelNumber = 3,
                    title = "Literary Devices & Metaphor",
                    chapterSubtitle = "Symbolism, irony & sonic cadence",
                    videoTitle = "Layering Meaning Beneath Literal Text",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Metaphors fuse distant worlds into fresh insight.",
                        "Irony creates tension between expectation and reality.",
                        "Alliteration and cadence shape emotional atmosphere."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("e3_cp1", 0.42f, "0:08", VidyaQuizQuestion("e3_cp1q", "'Time is a river' without 'like' or 'as' is a:", listOf("Simile", "Metaphor", "Oxymoron", "Pun"), 1, "Direct equation without comparison words is a metaphor.", "Video Checkpoint")),
                        MidVideoCheckpoint("e3_cp2", 0.78f, "0:14", VidyaQuizQuestion("e3_cp2q", "When the audience knows a secret the character misses:", listOf("Verbal irony", "Dramatic irony", "Allusion", "Anaphora"), 1, "Dramatic irony builds suspense from uneven knowledge.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("e3_qt1", "Repeating initial consonant sounds is:", listOf("Assonance", "Alliteration", "Hyperbole", "Elegy"), 1, "E.g., 'silent stars shimmered'."),
                        VidyaQuizQuestion("e3_qt2", "Anaphora repeats words at the:", listOf("End of lines", "Start of successive clauses", "Middle only", "Title"), 1, "E.g., 'We shall fight... We shall never surrender.'"),
                        VidyaQuizQuestion("e3_qt3", "Brief reference to a classic myth or history:", listOf("Allusion", "Euphemism", "Synecdoche", "Parody"), 0, "Allusion enriches meaning by invoking shared heritage."),
                        VidyaQuizQuestion("e3_qt4", "Giving human traits to storms or stones:", listOf("Personification", "Satire", "Litotes", "Diction"), 0, "Personification animates non-human subjects.")
                    ),
                    scrollTitle = "Scroll of Poetic Alankara",
                    scrollSummary = "Figurative craft turns abstract ideas into memorable sensory resonance.",
                    keyFormulas = listOf("Metaphor = Direct fusion", "Anaphora = Opening echo", "Irony = Expectation gap")
                ),
                QuestLevel(
                    id = "eng_l4",
                    realmId = RealmId.ENGLISH,
                    levelNumber = 4,
                    title = "Critical Synthesis & Debate",
                    chapterSubtitle = "Weighing multiple texts & logical fallacies",
                    videoTitle = "Spotting Fallacies & Building Synthesis",
                    videoDurationSeconds = 18,
                    lectureCaptions = listOf(
                        "Synthesis weaves diverse sources into one original perspective.",
                        "Spot strawman, ad hominem, and false dichotomy traps early.",
                        "Conclude with implications that outlive the essay itself."
                    ),
                    checkpoints = listOf(
                        MidVideoCheckpoint("e4_cp1", 0.42f, "0:08", VidyaQuizQuestion("e4_cp1q", "Attacking a speaker's character instead of their argument:", listOf("Strawman", "Ad hominem", "Slippery slope", "Red herring"), 1, "Ad hominem targets the person rather than logic.", "Video Checkpoint")),
                        MidVideoCheckpoint("e4_cp2", 0.78f, "0:14", VidyaQuizQuestion("e4_cp2q", "Distorting an opponent's point to make it easy to knock down:", listOf("Strawman fallacy", "Appeal to authority", "Induction", "Synergy"), 0, "Strawman misrepresents the real argument.", "Video Checkpoint"))
                    ),
                    quickTest = listOf(
                        VidyaQuizQuestion("e4_qt1", "Claiming only two extreme choices exist when more exist:", listOf("False dilemma", "Deductive proof", "Anecdote", "Tautology"), 0, "False dichotomy ignores nuanced middle paths."),
                        VidyaQuizQuestion("e4_qt2", "Effective synthesis essays:", listOf("Summarize texts separately", "Connect sources in dialogue around your thesis", "Avoid quotes", "Copy abstracts"), 1, "Sources should converse to support your central claim."),
                        VidyaQuizQuestion("e4_qt3", "Correlation proves causation:", listOf("Always", "Never automatically", "Only in poetry", "Only with pie charts"), 1, "Co-occurrence may hide confounding causes."),
                        VidyaQuizQuestion("e4_qt4", "Strongest evidence in academic debate:", listOf("Anonymous rumors", "Empirical data & primary texts", "Loud tone", "Sarcasm"), 1, "Verified primary sources anchor credible synthesis.")
                    ),
                    scrollTitle = "Scroll of Dialectical Synthesis",
                    scrollSummary = "Test arguments against steel-manned opposition before finalizing your thesis.",
                    keyFormulas = listOf("Thesis + Evidence + Rebuttal", "Beware: Ad Hominem / Strawman / False Dilemma")
                ),
                QuestLevel(
                    id = "eng_l5",
                    realmId = RealmId.ENGLISH,
                    levelNumber = 5,
                    title = "Kavya Quill Boss Trial",
                    chapterSubtitle = "50-Mark Rhetoric & Craft Boss Battle",
                    isBossBattle = true,
                    videoTitle = "Grand Rhetoric Synthesis",
                    videoDurationSeconds = 12,
                    lectureCaptions = listOf("Synthesize persuasion, clarity, literary craft, and critical debate."),
                    checkpoints = emptyList(),
                    quickTest = emptyList(),
                    scrollTitle = "Crown Scroll of Kavya Orator",
                    scrollSummary = "You wield persuasive Ethos/Pathos/Logos, lucid style, poetic device, and fallacy-free reasoning.",
                    keyFormulas = listOf("Ethos · Pathos · Logos", "Active Clarity", "Steel-Man Synthesis"),
                    bossTitle = "Kalidasa's Sabha Rhetoric Arena",
                    bossQuestions = listOf(
                        VidyaQuizQuestion("eb1", "Presenting medical credentials before health advice uses:", listOf("Ethos", "Pathos", "Irony", "Alliteration"), 0, "Credentials establish speaker Ethos.", "Boss Trial"),
                        VidyaQuizQuestion("eb2", "Logical statistics and syllogisms belong to:", listOf("Pathos", "Logos", "Hyperbole", "Elegy"), 1, "Logos centers on reason and evidence.", "Boss Trial"),
                        VidyaQuizQuestion("eb3", "Rewrite passively 'The song was sung by Priya' into active:", listOf("Priya sang the song.", "Singing happened to Priya.", "The song sang.", "Priya was sung."), 0, "Actor 'Priya' leads the verb.", "Boss Trial"),
                        VidyaQuizQuestion("eb4", "'The wind whispered secrets' exemplifies:", listOf("Personification", "Ad hominem", "Syllogism", "Oxymoron"), 0, "Human action 'whispered' assigned to wind.", "Boss Trial"),
                        VidyaQuizQuestion("eb5", "Repeating opening words 'Every dawn... Every dusk...' is:", listOf("Metonymy", "Anaphora", "Jargon", "Paradox"), 1, "Anaphora repeats opening phrases.", "Boss Trial"),
                        VidyaQuizQuestion("eb6", "Attacking a debater's shoes instead of their data is:", listOf("Logos", "Ad hominem", "Kairos", "Simile"), 1, "Irrelevant personal attack.", "Boss Trial"),
                        VidyaQuizQuestion("eb7", "'Either ban all phones or fail every class' commits:", listOf("False dilemma", "Ethos", "Deductive logic", "Allusion"), 0, "Ignores balanced policy options.", "Boss Trial"),
                        VidyaQuizQuestion("eb8", "Parallelism error check: 'She likes hiking, swimming, and ___':", listOf("to ride bikes", "cycling", "biked", "bike ride"), 1, "Matching gerund '-ing' form requires 'cycling'.", "Boss Trial"),
                        VidyaQuizQuestion("eb9", "In academic synthesis, counterarguments should be:", listOf("Ignored", "Fairly addressed and refuted", "Mocked", "Placed in title"), 1, "Addressing counterclaims builds credibility.", "Boss Trial"),
                        VidyaQuizQuestion("eb10", "Correct punctuation for two linked independent clauses:", listOf("Comma alone", "Semicolon", "Apostrophe", "Hyphen"), 1, "Semicolons join complete clauses without comma splices.", "Boss Trial")
                    )
                )
            )
        )
    )

    fun getRealm(realmId: RealmId): SubjectRealm = realms.first { it.realmId == realmId }

    fun getRealmByCourseId(courseId: String): SubjectRealm? = realms.firstOrNull { it.realmId.courseId == courseId }
}
