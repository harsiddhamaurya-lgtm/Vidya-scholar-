package com.example.rag

import com.example.data.AcademicStream
import com.example.data.StandardGrade

/**
 * Lazy-loaded, strictly scoped latest rationalized NCERT syllabus catalog.
 *
 * Each standard/stream pack (`class_9`, `class_10`, `class_11` / `class_12` x `science_pcm`,
 * `science_pcb`, `commerce`, `arts`) is allocated lazily ONLY when requested so switching to
 * Class 9 or Class 10 never touches or loads Class 11/12 packs into memory or disk.
 *
 * Every chunk represents a single coherent textbook topic (zero mega-chunk mixing across
 * unrelated laws/chapters), while full interleaved Markdown + KaTeX lessons live in the
 * `Lessons*` maps resolved on demand via [getStructuredLesson].
 */
object SyllabusPackCatalog {

    const val CATALOG_VERSION = 4

    fun normalizeStreamForStandard(standardId: String, streamId: String): String {
        val std = StandardGrade.fromId(standardId)
        return if (!std.hasStreams) AcademicStream.GENERAL_FOUNDATION.id else streamId
    }

    fun getPackMetadata(standardId: String, streamId: String): Pair<String, String> {
        val std = StandardGrade.fromId(standardId)
        val effectiveStreamId = normalizeStreamForStandard(standardId, streamId)
        val stream = AcademicStream.fromId(effectiveStreamId)
        return "${std.shortName} • ${stream.title}" to stream.description
    }

    fun getChunksForScope(standardId: String, streamId: String): List<TextbookChunk> {
        val std = StandardGrade.fromId(standardId).id
        val effectiveStream = normalizeStreamForStandard(std, streamId)
        return when (std) {
            "class_9" -> class9Chunks
            "class_10" -> class10Chunks
            "class_11" -> when (effectiveStream) {
                "science_pcm" -> class11PcmChunks
                "science_pcb" -> class11PcbChunks
                "commerce" -> class11CommerceChunks
                "arts" -> class11ArtsChunks
                else -> class11PcmChunks
            }
            "class_12" -> when (effectiveStream) {
                "science_pcm" -> class12PcmChunks
                "science_pcb" -> class12PcbChunks
                "commerce" -> class12CommerceChunks
                "arts" -> class12ArtsChunks
                else -> class12PcmChunks
            }
            else -> class11PcmChunks
        }
    }

    /**
     * Resolves the verified textbook Markdown + KaTeX structured lesson for [chunkId], loading
     * only the matching curriculum tier object lazily.
     */
    fun getStructuredLesson(chunkId: String): String? {
        return when {
            chunkId.startsWith("c9_") || chunkId.startsWith("c10_") ->
                LessonsFoundation.lessons[chunkId] ?: LessonsFoundationExtra.lessons[chunkId]
            chunkId.startsWith("c11_com_") || chunkId.startsWith("c12_com_") ||
                chunkId.startsWith("c11_art_") || chunkId.startsWith("c12_art_") ->
                LessonsCommerceArts.lessons[chunkId] ?: LessonsSeniorExtra.lessons[chunkId]
            else -> {
                val canonicalId = when (chunkId) {
                    "c11_pcb_phy_ch5_laws" -> "c11_pcm_phy_ch5_laws"
                    "c11_pcb_phy_ch5_friction" -> "c11_pcm_phy_ch5_friction"
                    "c11_pcb_phy_ch5_circular" -> "c11_pcm_phy_ch5_circular"
                    "c11_pcb_phy_ch6_work_energy" -> "c11_pcm_phy_ch6_work_energy"
                    "c11_pcb_phy_ch6_impulse" -> "c11_pcm_phy_ch6_impulse"
                    "c11_pcb_phy_ch8_grav" -> "c11_pcm_phy_ch8_grav"
                    "c11_pcb_phy_ch12_thermo" -> "c11_pcm_phy_ch12_thermo"
                    "c11_pcb_chem_ch2_atomic" -> "c11_pcm_chem_ch2_atomic"
                    "c11_pcb_chem_ch8_redox" -> "c11_pcm_chem_ch8_redox"
                    "c12_pcb_phy_ch3_kirchhoff" -> "c12_pcm_phy_ch3_kirchhoff"
                    "c12_pcb_phy_ch6_emi" -> "c12_pcm_phy_ch6_emi"
                    "c12_pcb_phy_ch7_ac" -> "c12_pcm_phy_ch7_ac"
                    "c12_pcb_chem_ch3_electro" -> "c12_pcm_chem_ch3_electro"
                    "c12_pcb_chem_ch4_kinetics" -> "c12_pcm_chem_ch4_kinetics"
                    "c12_pcm_phy_ch11_photoelectric" -> "c12_pcb_phy_ch11_photoelectric"
                    "c12_pcm_phy_ch12_bohr" -> "c12_pcb_phy_ch12_bohr"
                    else -> chunkId
                }
                LessonsScienceSenior.lessons[canonicalId] ?: LessonsSeniorExtra.lessons[canonicalId]
            }
        }
    }

    // =========================================================================
    // CLASS 9 • GENERAL FOUNDATION (Latest Rationalized NCERT)
    // =========================================================================
    private val class9Chunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c9_gen_math_ch1_num",
                standardId = "class_9",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 1,
                levelId = "c9_math_l1",
                topicTitle = "Rationalization & Irrational Surds",
                contentText = "Rationalizing the denominator removes square root surds from the denominator by multiplying top and bottom by the conjugate (a - sqrt(b)) via difference of squares.",
                formulaTags = "1/(a+sqrt(b)) = (a-sqrt(b))/(a^2-b), (a+sqrt(b))(a-sqrt(b)) = a^2-b",
                workedExample = "Rationalize 1 / (2 + sqrt(3)) = (2 - sqrt(3)) / (4 - 3) = 2 - sqrt(3)."
            ),
            TextbookChunk(
                chunkId = "c9_gen_math_ch1_exp",
                standardId = "class_9",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 1,
                levelId = "c9_math_l1",
                topicTitle = "Laws of Exponents for Real Numbers",
                contentText = "For positive real bases, multiplying powers adds rational exponents, dividing subtracts exponents, and fractional power 1/n represents the nth root.",
                formulaTags = "a^m * a^n = a^(m+n), a^m / a^n = a^(m-n), (a^m)^n = a^(mn), a^(1/n) = nth_root(a)",
                workedExample = "Evaluate 16^(3/4) = (2^4)^(3/4) = 2^3 = 8."
            ),
            TextbookChunk(
                chunkId = "c9_gen_math_ch2_poly",
                standardId = "class_9",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 2,
                levelId = "c9_math_l2",
                topicTitle = "Polynomials, Factor Theorem & Algebraic Identities",
                contentText = "By the Factor Theorem, polynomial p(x) has linear factor (x - r) if and only if p(r) = 0. Standard identities factor squares and cubes cleanly.",
                formulaTags = "(a+b)^2 = a^2+2ab+b^2, a^2-b^2 = (a+b)(a-b), x^3+y^3 = (x+y)(x^2-xy+y^2)",
                workedExample = "Evaluate 103 * 97 = (100+3)(100-3) = 100^2 - 3^2 = 9991."
            ),
            TextbookChunk(
                chunkId = "c9_gen_math_ch3_coord",
                standardId = "class_9",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 3,
                levelId = "c9_math_l3",
                topicTitle = "Coordinate Geometry, Abscissa, Ordinate & Quadrants",
                contentText = "Any Cartesian point P(x, y) is uniquely located by its x-coordinate (abscissa) and y-coordinate (ordinate) across four quadrants.",
                formulaTags = "Quadrant I (+,+), Quadrant II (-,+), Quadrant III (-,-), Quadrant IV (+,-), origin (0,0)",
                workedExample = "Point P(-6, 4) lies in Quadrant II at perpendicular distance 4 from x-axis and 6 from y-axis."
            ),
            TextbookChunk(
                chunkId = "c9_gen_math_ch10_heron",
                standardId = "class_9",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 10,
                levelId = "c9_math_l4",
                topicTitle = "Heron's Formula for Triangle Area",
                contentText = "Heron's formula computes the exact area of any triangle directly from its three side lengths a, b, c using semiperimeter s.",
                formulaTags = "s = (a+b+c)/2, Area = sqrt(s*(s-a)*(s-b)*(s-c))",
                workedExample = "Triangle sides 13, 14, 15 cm give s = 21 cm and Area = sqrt(21*8*7*6) = 84 cm^2."
            ),
            TextbookChunk(
                chunkId = "c9_gen_phy_ch8_motion",
                standardId = "class_9",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 7,
                levelId = "c9_sci_l1",
                topicTitle = "Equations of Uniformly Accelerated Motion",
                contentText = "Kinematic equations relate initial velocity u, final velocity v, constant acceleration a, displacement s, and time t. Slope of a v-t graph equals acceleration.",
                formulaTags = "v = u + at, s = ut + (1/2)at^2, v^2 - u^2 = 2as",
                workedExample = "Starting from rest (u=0) with a = 4 m/s^2 for 5s: v = 20 m/s and s = 50 m."
            ),
            TextbookChunk(
                chunkId = "c9_gen_phy_ch9_newton",
                standardId = "class_9",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 8,
                levelId = "c9_sci_l2",
                topicTitle = "Newton's Laws of Motion, Inertia & Momentum",
                contentText = "Newton's First Law defines inertia, Second Law equates net force to rate of change of momentum F = ma, and Third Law pairs action-reaction on distinct bodies.",
                formulaTags = "F = ma, p = mv, F*dt = Delta_p",
                workedExample = "A 18 N net force on a 6 kg crate produces acceleration a = 18 / 6 = 3 m/s^2."
            ),
            TextbookChunk(
                chunkId = "c9_gen_phy_ch10_gravitation",
                standardId = "class_9",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 9,
                levelId = "c9_sci_l3",
                topicTitle = "Universal Law of Gravitation, Mass vs Weight & Buoyancy",
                contentText = "Every two masses attract with force G*M*m/d^2. Free fall acceleration g = GM/R^2 is independent of falling object mass, while Archimedes' buoyant force equals displaced fluid weight.",
                formulaTags = "F = G*M*m/d^2, g = G*M/R^2, W = mg, F_B = rho*V*g",
                workedExample = "A 60 kg astronaut weighs 588 N on Earth and 588 / 6 = 98 N on the Moon."
            ),
            TextbookChunk(
                chunkId = "c9_gen_phy_ch11_work_energy",
                standardId = "class_9",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 10,
                levelId = "c9_sci_l4",
                topicTitle = "Work-Energy Theorem & Kinetic Energy",
                contentText = "The Work-Energy Theorem states that net work done by all forces on an object equals the change in its kinetic energy K = (1/2)mv^2.",
                formulaTags = "W_net = Delta_K = (1/2)m*v^2 - (1/2)m*u^2, W = F*s*cos(theta)",
                workedExample = "A 4 kg cart accelerating from 2 m/s to 6 m/s gains net work 0.5*4*(36 - 4) = 64 J."
            ),
            TextbookChunk(
                chunkId = "c9_gen_chem_ch3_atoms",
                standardId = "class_9",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Chemistry)",
                chapterNumber = 3,
                levelId = "c9_sci_l5",
                topicTitle = "Atoms, Molecules & The Mole Concept",
                contentText = "One mole contains Avogadro's constant 6.022 x 10^23 particles, linking laboratory sample mass in grams directly to particle counts via molar mass.",
                formulaTags = "n = m / M, N = n * 6.022e23",
                workedExample = "54 g of water (molar mass 18 g/mol) equals 54 / 18 = 3 moles = 1.8066 x 10^24 molecules."
            ),
            TextbookChunk(
                chunkId = "c9_gen_sst_ch1_french",
                standardId = "class_9",
                streamId = "general",
                subjectId = "social_science",
                subjectName = "Social Science (History)",
                chapterNumber = 1,
                levelId = "c9_sst_l1",
                topicTitle = "French Revolution (1789), Estates-General & Reign of Terror",
                contentText = "Social inequality among Three Estates, subsistence bread crises, and Enlightenment ideals triggered the 1789 Bastille storming, 1791 Constitution, and 1792 Republic.",
                formulaTags = "5 May 1789 Estates-General, 20 June Tennis Court Oath, 14 July 1789 Bastille, 1793-94 Jacobin Robespierre",
                workedExample = "Third Estate rejected one-vote-per-estate and formed the National Assembly on 20 June 1789."
            )
        )
    }

    // =========================================================================
    // CLASS 10 • GENERAL FOUNDATION (Latest Rationalized NCERT)
    // =========================================================================
    private val class10Chunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c10_gen_math_ch1_real",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 1,
                levelId = "c10_math_l1",
                topicTitle = "Fundamental Theorem of Arithmetic, HCF, LCM & Irrationality",
                contentText = "Every composite number factors uniquely into primes. For two positive integers, HCF(a, b) * LCM(a, b) = a * b, and prime divisibility proves sqrt(p) irrational by contradiction.",
                formulaTags = "HCF(a,b) * LCM(a,b) = a * b, sqrt(2) irrational contradiction",
                workedExample = "HCF(96, 404) = 4, so LCM(96, 404) = (96 * 404) / 4 = 9696."
            ),
            TextbookChunk(
                chunkId = "c10_gen_math_ch3_linear",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 3,
                levelId = "c10_math_l2",
                topicTitle = "Pair of Linear Equations: Consistency, Substitution & Elimination",
                contentText = "Coefficient ratios classify two linear equations as intersecting (unique solution), coincident (infinitely many), or parallel (no solution, inconsistent).",
                formulaTags = "a1/a2 != b1/b2 unique, a1/a2 = b1/b2 != c1/c2 parallel no solution",
                workedExample = "2x + 3y = 7 and 4x + ky = 10 are parallel with no solution when 2/4 = 3/k -> k = 6."
            ),
            TextbookChunk(
                chunkId = "c10_gen_math_ch4_quad",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 4,
                levelId = "c10_math_l3",
                topicTitle = "Quadratic Equations & Discriminant Nature of Roots",
                contentText = "Standard quadratic ax^2 + bx + c = 0 solves via the Quadratic Formula, with discriminant D = b^2 - 4ac distinguishing real distinct (D>0), equal (D=0), or non-real roots (D<0).",
                formulaTags = "x = (-b +- sqrt(b^2 - 4ac)) / 2a, D = b^2 - 4ac, sum = -b/a, prod = c/a",
                workedExample = "Solve x^2 - 5x + 6 = 0: D = 25 - 24 = 1 -> x = 3 or x = 2."
            ),
            TextbookChunk(
                chunkId = "c10_gen_math_ch5_ap",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 5,
                levelId = "c10_math_l4",
                topicTitle = "Arithmetic Progressions (AP), nth Term & Sum of n Terms",
                contentText = "An Arithmetic Progression increases by constant common difference d = a2 - a1, giving linear nth-term formula a_n and closed-form sum S_n.",
                formulaTags = "a_n = a + (n-1)d, S_n = (n/2)*(2a + (n-1)d) = (n/2)*(a + l)",
                workedExample = "For AP 3, 7, 11...: a_10 = 3 + 9*4 = 39 and S_10 = 5*(3 + 39) = 210."
            ),
            TextbookChunk(
                chunkId = "c10_gen_math_ch7_coord",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 6,
                levelId = "c10_math_l5",
                topicTitle = "Coordinate Geometry: Distance, Midpoint & Section Formula",
                contentText = "The Distance Formula computes segment length from coordinate differences, while the Section Formula divides AB internally in ratio m1 : m2.",
                formulaTags = "d = sqrt((x2-x1)^2 + (y2-y1)^2), P = ((m1*x2+m2*x1)/(m1+m2), (m1*y2+m2*y1)/(m1+m2))",
                workedExample = "Dividing A(4,-3) and B(8,5) in ratio 3:1 yields P(7, 3)."
            ),
            TextbookChunk(
                chunkId = "c10_gen_math_ch8_trig",
                standardId = "class_10",
                streamId = "general",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 8,
                levelId = "c10_math_l6",
                topicTitle = "Trigonometric Identities & Right-Triangle Ratios",
                contentText = "Pythagorean identities sin^2(theta) + cos^2(theta) = 1, 1 + tan^2 = sec^2, and 1 + cot^2 = csc^2 connect acute-angle ratios in right triangles.",
                formulaTags = "sin^2(theta)+cos^2(theta) = 1, 1+tan^2(theta) = sec^2(theta), 1+cot^2(theta) = csc^2(theta)",
                workedExample = "If tan(A) = 3/4, hypotenuse = 5, giving sin(A) = 3/5 and cos(A) = 4/5."
            ),
            TextbookChunk(
                chunkId = "c10_gen_phy_ch10_optics",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 9,
                levelId = "c10_sci_l1",
                topicTitle = "Spherical Mirror Formula & Magnification",
                contentText = "Under New Cartesian Sign Convention measured from the mirror pole, the spherical mirror formula relates object distance u, image distance v, and focal length f.",
                formulaTags = "1/f = 1/v + 1/u, m = h_i/h_o = -v/u",
                workedExample = "Concave mirror f = -15 cm, u = -30 cm gives v = -30 cm and real inverted m = -1."
            ),
            TextbookChunk(
                chunkId = "c10_gen_phy_ch10_refraction",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 9,
                levelId = "c10_sci_l2",
                topicTitle = "Snell's Law of Refraction, Thin Lens Formula & Dioptre Power",
                contentText = "Snell's Law n1*sin(i) = n2*sin(r) governs bending of light across optical interfaces, while thin lenses obey 1/f = 1/v - 1/u with power P = 1/f(m).",
                formulaTags = "n1*sin(i) = n2*sin(r), 1/f = 1/v - 1/u, m = v/u, P = 1/f",
                workedExample = "A convex lens of focal length +25 cm (+0.25 m) has optical power P = +4.0 D."
            ),
            TextbookChunk(
                chunkId = "c10_gen_phy_ch12_elec",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 11,
                levelId = "c10_sci_l3",
                topicTitle = "Ohm's Law, Resistivity & Series/Parallel Resistors",
                contentText = "Ohm's Law states V = I * R at constant temperature. Series resistors sum directly whereas parallel resistors combine via reciprocals.",
                formulaTags = "V = IR, R = rho*L/A, R_series = R1+R2, 1/R_parallel = 1/R1+1/R2",
                workedExample = "Two 6 ohm resistors in parallel give Req = 3 ohm; across 12 V current I = 4 A."
            ),
            TextbookChunk(
                chunkId = "c10_gen_phy_ch12_joule",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Physics)",
                chapterNumber = 11,
                levelId = "c10_sci_l4",
                topicTitle = "Joule's Law of Heating & Electric Power",
                contentText = "Electric heat dissipated in a resistor equals H = I^2 * R * t, while power P = VI = I^2*R = V^2/R measures energy transfer per second.",
                formulaTags = "H = I^2*R*t, P = VI = I^2*R = V^2/R",
                workedExample = "A 5 ohm resistor carrying 2 A for 10 s produces H = 4 * 5 * 10 = 200 J heat."
            ),
            TextbookChunk(
                chunkId = "c10_gen_chem_ch1_rxn",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Chemistry)",
                chapterNumber = 1,
                levelId = "c10_sci_l5",
                topicTitle = "Chemical Reactions, Balancing & Displacement Types",
                contentText = "Balanced chemical reactions conserve atoms across Combination, Thermal/Photolytic Decomposition, Single Displacement, Precipitation, and Redox reactions.",
                formulaTags = "A + BC -> AC + B, 2FeSO4 -> Fe2O3 + SO2 + SO3, exothermic vs endothermic",
                workedExample = "Iron displaces copper from CuSO4(aq) forming FeSO4(aq) and reddish-brown Cu(s)."
            ),
            TextbookChunk(
                chunkId = "c10_gen_bio_ch5_life",
                standardId = "class_10",
                streamId = "general",
                subjectId = "science",
                subjectName = "Science (Biology)",
                chapterNumber = 5,
                levelId = "c10_sci_l6",
                topicTitle = "Life Processes: Photosynthesis, Respiration & Circulation",
                contentText = "Autotrophic photosynthesis fixes CO2 into glucose, while cytoplasmic glycolysis splits glucose into pyruvate before anaerobic or mitochondrial aerobic respiration.",
                formulaTags = "6CO2 + 12H2O -> C6H12O6 + 6O2 + 6H2O, Pyruvate -> 38 ATP aerobic vs Ethanol/Lactic acid",
                workedExample = "Bile emulsifies fats into small globules without containing digestive enzymes."
            ),
            TextbookChunk(
                chunkId = "c10_gen_sst_ch2_federal",
                standardId = "class_10",
                streamId = "general",
                subjectId = "social_science",
                subjectName = "Social Science (Civics)",
                chapterNumber = 2,
                levelId = "c10_sst_l1",
                topicTitle = "Indian Federalism: Union, State, Concurrent Lists & 73rd/74th Amendments",
                contentText = "India's Seventh Schedule allocates subjects across Union, State, Concurrent, and Residuary lists, reinforced by 1992 Panchayati Raj decentralization.",
                formulaTags = "Union List Defence/Banking, State List Police/Agriculture, Concurrent List Education/Forests, 73rd Amendment 1/3 women reservation",
                workedExample = "On a Concurrent List conflict like Education, Union parliamentary law prevails over State law."
            )
        )
    }

    // =========================================================================
    // CLASS 11 • SCIENCE (PCM)
    // =========================================================================
    private val class11PcmChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c11_pcm_math_ch13_calc",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 12,
                levelId = "c11_pcm_math_l1",
                topicTitle = "Derivatives, Power Rule & Product Rule",
                contentText = "The derivative f'(x) measures instantaneous rate of change and tangent slope. Differentiate terms using Power Rule d/dx(x^n) = n*x^(n-1) and Product Rule (uv)' = u'v + uv'.",
                formulaTags = "d/dx(x^n) = n*x^(n-1), d/dx(sin x) = cos x, d/dx(cos x) = -sin x, d/dx(e^x) = e^x",
                workedExample = "Derivative of 3x^4 - 5x^2 + 7 by Power Rule is 12x^3 - 10x."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_math_ch13_limits",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 12,
                levelId = "c11_pcm_math_l2",
                topicTitle = "Limits & Continuity",
                contentText = "A limit exists when left-hand and right-hand limits agree as x -> c. Standard limits evaluate indeterminate 0/0 forms cleanly.",
                formulaTags = "lim(x->0) sin(x)/x = 1, lim(x->0) (e^x - 1)/x = 1, LHL = RHL = f(c)",
                workedExample = "Factor (x^2 - 4)/(x - 2) = x + 2, giving limit 4 as x -> 2."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch5_laws",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 4,
                levelId = "c11_pcm_phy_l1",
                topicTitle = "Newton's Laws of Motion & Free Body Diagrams",
                contentText = "Isolate each object in a Free Body Diagram and resolve net external force along perpendicular normal and slope axes via Newton's Second Law F_net = ma.",
                formulaTags = "F_net = ma, mg_parallel = mg*sin(theta), N = mg*cos(theta)",
                workedExample = "A 2 kg block on a smooth 30 deg incline accelerates at a = g*sin(30) = 4.9 m/s^2."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch5_friction",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 4,
                levelId = "c11_pcm_phy_l2",
                topicTitle = "Static Friction, Kinetic Friction & Angle of Repose",
                contentText = "Limiting static friction f_s_max = mu_s * N opposes impending slipping, whereas kinetic friction f_k = mu_k * N opposes active sliding.",
                formulaTags = "f_s <= mu_s * N, f_k = mu_k * N, tan(theta_r) = mu_s",
                workedExample = "A 5 kg block with mu_s = 0.4 on a floor (N = 50 N) starts sliding at 20 N."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch5_circular",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 4,
                levelId = "c11_pcm_phy_l3",
                topicTitle = "Uniform Circular Motion, Centripetal Acceleration & Banking",
                contentText = "Uniform circular motion requires inward radial centripetal acceleration a_c = v^2 / r supplied by tension, friction, or banked normal reaction.",
                formulaTags = "a_c = v^2/r = omega^2*r, F_c = mv^2/r, v = sqrt(rg*tan(theta))",
                workedExample = "A 2 kg stone whirled on a 1 m string at 4 m/s needs tension T = 32 N."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch6_work_energy",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 5,
                levelId = "c11_pcm_phy_l4",
                topicTitle = "Work-Energy Theorem & Change in Kinetic Energy",
                contentText = "The Work-Energy Theorem equates net work done by all forces acting on a particle to its change in kinetic energy W_net = Delta K = K_f - K_i = (1/2)mv_f^2 - (1/2)mv_i^2.",
                formulaTags = "W_net = Delta_K = (1/2)m*(v_f^2 - v_i^2), W = int F_net . dr",
                workedExample = "A 2 kg block pulled by 14 N against 4 N friction over 5 m gets W_net = 50 J and v_f = sqrt(50) m/s."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch6_impulse",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 5,
                levelId = "c11_pcm_phy_l5",
                topicTitle = "Impulse-Momentum Theorem & Collisions",
                contentText = "Impulse J equals time integral of net force and change in linear momentum Delta p = m*v_f - m*v_i, conserving total momentum in isolated collisions.",
                formulaTags = "J = int F dt = Delta_p = m*v_f - m*v_i, m1*u1 + m2*u2 = m1*v1 + m2*v2",
                workedExample = "A 0.2 kg ball reversing from +20 m/s to -25 m/s in 0.01 s suffers impulse -9 N*s and average force 900 N."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch8_grav",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 7,
                levelId = "c11_pcm_phy_l6",
                topicTitle = "Gravitation, Orbital Velocity & Escape Velocity",
                contentText = "Total mechanical energy conservation gives escape velocity v_e = sqrt(2GM/R) = sqrt(2)*v_o from a planet surface, independent of projectile mass.",
                formulaTags = "U = -GMm/r, v_o = sqrt(GM/R), v_e = sqrt(2GM/R) = 11.2 km/s",
                workedExample = "From low-orbit speed v_o = 7.9 km/s, escape speed v_e = sqrt(2)*7.9 = 11.2 km/s."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_phy_ch12_thermo",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 11,
                levelId = "c11_pcm_phy_l7",
                topicTitle = "First Law of Thermodynamics & PV Processes",
                contentText = "Heat absorbed dQ equals internal energy increase dU plus PV work done by gas dW = P dV, with isothermal dU = 0 and adiabatic dQ = 0.",
                formulaTags = "dQ = dU + P*dV, PV = nRT, isothermal W = nRT*ln(V2/V1), PV^gamma = const",
                workedExample = "Adding 200 J heat while gas performs 120 J work increases internal energy by Delta_U = +80 J."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_chem_ch2_atomic",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 2,
                levelId = "c11_pcm_chem_l1",
                topicTitle = "Bohr Model, Quantum Numbers & Heisenberg Uncertainty",
                contentText = "Quantum numbers (n, l, ml, ms) specify atomic orbital size, shape, orientation, and electron spin alongside Bohr energies and de Broglie wavelength.",
                formulaTags = "E_n = -13.6*Z^2/n^2 eV, lambda = h/(mv), Delta_x * Delta_p >= h/(4*pi)",
                workedExample = "For 3d subshell (n=3, l=2), ml spans -2..+2 holding maximum 10 electrons."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_chem_ch8_redox",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 7,
                levelId = "c11_pcm_chem_l2",
                topicTitle = "Redox Reactions, Oxidation States & Half-Reaction Balancing",
                contentText = "Oxidation loses electrons (oxidation number rises) while reduction gains electrons (oxidation number falls), balancing charge across half-reactions.",
                formulaTags = "OIL: M -> M^(n+) + ne^-, RIG: X + ne^- -> X^(n-)",
                workedExample = "In Zn + Cu^2+ -> Zn^2+ + Cu, Zn is oxidized (reducing agent) and Cu^2+ is reduced."
            ),
            TextbookChunk(
                chunkId = "c11_pcm_cs_ch1_python",
                standardId = "class_11",
                streamId = "science_pcm",
                subjectId = "coding",
                subjectName = "Computer Science & Python",
                chapterNumber = 6,
                levelId = "c11_pcm_cs_l1",
                topicTitle = "Python Functions, Recursion & Big-O Complexity",
                contentText = "Python functions and recursion reduce problems to a base case, while Binary Search halves sorted intervals in O(log n) worst-case time.",
                formulaTags = "T(n) = T(n/2) + O(1) -> O(log n), def fact(n):",
                workedExample = "Recursive fact(5) unwinds 5 * 4 * 3 * 2 * 1 = 120."
            )
        )
    }

    // =========================================================================
    // CLASS 11 • SCIENCE (PCB)
    // =========================================================================
    private val class11PcbChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c11_pcb_bio_ch8_cell",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "biology",
                subjectName = "Biology",
                chapterNumber = 8,
                levelId = "c11_pcb_bio_l1",
                topicTitle = "Cell Cycle, Mitosis vs Meiosis & Fluid Mosaic Membrane",
                contentText = "Singer and Nicolson's Fluid Mosaic Model explains membrane fluidity, while Mitosis maintains diploid 2n and Meiosis halves ploidy to haploid n gametes.",
                formulaTags = "2n -> 2n Mitosis, 2n -> n Meiosis, S phase DNA doubles 2C -> 4C",
                workedExample = "A 2n=16 diploid cell after S phase retains 16 chromosomes with 32 chromatids (4C DNA)."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_bio_ch13_photo",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "biology",
                subjectName = "Biology",
                chapterNumber = 11,
                levelId = "c11_pcb_bio_l2",
                topicTitle = "Photosynthesis: Light Reaction, Calvin Cycle (C3) & C4 Hatch-Slack",
                contentText = "Thylakoid light reactions generate ATP and NADPH via PS II/PS I chemiosmosis, driving stroma RuBisCO Calvin cycle carbon fixation (C3 vs Kranz-anatomy C4).",
                formulaTags = "6CO2 + 18ATP + 12NADPH -> C6H12O6, Cyclic PS I ATP only, Non-cyclic ATP + NADPH + O2",
                workedExample = "Fixing 1 CO2 needs 3 ATP + 2 NADPH, so 3 Calvin turns consume 9 ATP + 6 NADPH."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_phy_ch4_projectile",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 3,
                levelId = "c11_pcb_phy_l1",
                topicTitle = "Projectile Motion, Range & Maximum Height",
                contentText = "Projectile motion combines constant horizontal velocity u*cos(theta) with vertical acceleration -g, achieving maximum horizontal range at theta = 45 degrees.",
                formulaTags = "T = 2u*sin(theta)/g, H_max = u^2*sin^2(theta)/(2g), R = u^2*sin(2*theta)/g",
                workedExample = "Launch u = 20 m/s at 45 deg (g = 10 m/s^2) travels horizontal range R = 40 m."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_phy_ch5_laws",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 4,
                levelId = "c11_pcb_phy_l2",
                topicTitle = "Newton's Laws of Motion & Free Body Diagrams",
                contentText = "Isolate each object in a Free Body Diagram and resolve net external force along perpendicular normal and slope axes via F_net = ma.",
                formulaTags = "F_net = ma, mg_parallel = mg*sin(theta), N = mg*cos(theta)",
                workedExample = "A 2 kg block slides down a smooth 30 deg incline with acceleration a = 4.9 m/s^2."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_phy_ch6_work_energy",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 5,
                levelId = "c11_pcb_phy_l3",
                topicTitle = "Work-Energy Theorem & Change in Kinetic Energy",
                contentText = "The Work-Energy Theorem equates net work done by all forces acting on a particle to its change in kinetic energy W_net = Delta K = K_f - K_i.",
                formulaTags = "W_net = Delta_K = (1/2)m*(v_f^2 - v_i^2)",
                workedExample = "Net work of 50 J accelerates a 2 kg block from rest to sqrt(50) m/s."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_phy_ch8_grav",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 7,
                levelId = "c11_pcb_phy_l4",
                topicTitle = "Gravitation, Orbital Velocity & Escape Velocity",
                contentText = "Mechanical energy conservation gives Earth escape velocity v_e = sqrt(2GM/R) = 11.2 km/s independent of mass or launch angle.",
                formulaTags = "U = -GMm/r, v_o = sqrt(GM/R), v_e = sqrt(2GM/R)",
                workedExample = "Escape speed is sqrt(2) times low circular orbital speed: 1.414 * 7.9 = 11.2 km/s."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_chem_ch2_atomic",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 2,
                levelId = "c11_pcb_chem_l0",
                topicTitle = "Bohr Model, Quantum Numbers & Heisenberg Uncertainty",
                contentText = "Four quantum numbers (n, l, ml, ms) specify atomic orbitals cleanly under Pauli Exclusion Principle alongside Bohr orbit energies.",
                formulaTags = "E_n = -13.6*Z^2/n^2 eV, lambda = h/(mv), Delta_x*Delta_p >= h/(4*pi)",
                workedExample = "For 3d subshell (n=3, l=2), 5 orbitals accommodate at most 10 electrons."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_chem_ch4_bond",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 4,
                levelId = "c11_pcb_chem_l1",
                topicTitle = "VSEPR Molecular Geometry & Orbital Hybridization",
                contentText = "Steric number of sigma bonds plus lone pairs sets sp, sp2, or sp3 hybridization, while lone-pair repulsion compresses bond angles.",
                formulaTags = "SN = sigma_bonds + lone_pairs, sp 180, sp2 120, sp3 109.5 deg",
                workedExample = "CH4 has 4 sigma bonds and 0 lone pairs -> sp3 tetrahedral with 109.5 deg angle."
            ),
            TextbookChunk(
                chunkId = "c11_pcb_chem_ch7_eq",
                standardId = "class_11",
                streamId = "science_pcb",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 6,
                levelId = "c11_pcb_chem_l2",
                topicTitle = "Chemical Equilibrium, Kp/Kc & Le Chatelier's Principle",
                contentText = "Reversible reactions satisfy K_p = K_c*(RT)^Delta_ng at equilibrium; Le Chatelier shifts counteract applied concentration or pressure stresses.",
                formulaTags = "K_p = K_c*(RT)^Delta_ng, pH = -log10[H+]",
                workedExample = "Strong acid 0.001 M HCl gives [H+] = 10^-3 M and pH = 3."
            )
        )
    }

    // =========================================================================
    // CLASS 11 • COMMERCE
    // =========================================================================
    private val class11CommerceChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c11_com_acc_ch3_rules",
                standardId = "class_11",
                streamId = "commerce",
                subjectId = "accountancy",
                subjectName = "Accountancy",
                chapterNumber = 3,
                levelId = "c11_com_acc_l1",
                topicTitle = "Accounting Equation & Golden Rules of Double-Entry",
                contentText = "Dual-aspect bookkeeping preserves Assets = Liabilities + Capital using Personal, Real, and Nominal debit/credit rules.",
                formulaTags = "Assets = Liabilities + Capital, Closing Capital = Opening + Profit - Drawings",
                workedExample = "Cash machinery purchase Rs 50,000 debits Machinery A/c and credits Cash A/c."
            ),
            TextbookChunk(
                chunkId = "c11_com_acc_ch7_depr",
                standardId = "class_11",
                streamId = "commerce",
                subjectId = "accountancy",
                subjectName = "Accountancy",
                chapterNumber = 7,
                levelId = "c11_com_acc_l2",
                topicTitle = "Depreciation: Straight Line (SLM) & Written Down Value (WDV)",
                contentText = "Depreciation allocates tangible fixed asset cost over useful life via fixed Straight Line Method or reducing-balance Written Down Value.",
                formulaTags = "SLM = (Cost - Scrap)/Life, WDV = Book_Value * Rate/100",
                workedExample = "Machine Rs 1,10,000 minus Rs 10,000 scrap over 5 years gives Rs 20,000/year SLM depreciation."
            ),
            TextbookChunk(
                chunkId = "c11_com_econ_ch2_demand",
                standardId = "class_11",
                streamId = "commerce",
                subjectId = "economics",
                subjectName = "Microeconomics",
                chapterNumber = 2,
                levelId = "c11_com_econ_l1",
                topicTitle = "Diminishing Marginal Utility & Consumer Equilibrium",
                contentText = "As consumption rises, Marginal Utility MU_n = TU_n - TU_(n-1) declines; two-good equilibrium requires MU_x / P_x = MU_y / P_y.",
                formulaTags = "MU_n = TU_n - TU_(n-1), MU_x/P_x = MU_y/P_y",
                workedExample = "TU3 = 28, TU4 = 33 gives MU4 = 5 utils; at Px = Rs 5 ratio equals 1."
            ),
            TextbookChunk(
                chunkId = "c11_com_econ_ch3_elasticity",
                standardId = "class_11",
                streamId = "commerce",
                subjectId = "economics",
                subjectName = "Microeconomics",
                chapterNumber = 3,
                levelId = "c11_com_econ_l2",
                topicTitle = "Price Elasticity of Demand (Ed) & Demand Curve Slope",
                contentText = "Price Elasticity Ed = -(Delta Q / Delta P) * (P / Q) measures unit-free demand responsiveness to own price.",
                formulaTags = "Ed = -(Delta_Q/Delta_P)*(P/Q), |Ed|>1 elastic, |Ed|<1 inelastic",
                workedExample = "Price dropping 10% (20->18) raising quantity 20% (100->120) gives Ed = 2 (elastic)."
            ),
            TextbookChunk(
                chunkId = "c11_com_bst_ch2_forms",
                standardId = "class_11",
                streamId = "commerce",
                subjectId = "business_studies",
                subjectName = "Business Studies",
                chapterNumber = 2,
                levelId = "c11_com_bst_l1",
                topicTitle = "Forms of Business Organisation: Sole Proprietorship, Partnership & Company",
                contentText = "Sole proprietors and 1932 Act partnerships face unlimited liability, whereas a Companies Act 2013 Joint Stock Company enjoys perpetual succession and limited liability.",
                formulaTags = "Partnership Act 1932, Companies Act 2013, Private Ltd 2..200, Public Ltd >=7",
                workedExample = "Shareholders risk only unpaid share capital unlike partners liable with personal assets."
            )
        )
    }

    // =========================================================================
    // CLASS 11 • ARTS / HUMANITIES
    // =========================================================================
    private val class11ArtsChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c11_art_pol_ch1_const",
                standardId = "class_11",
                streamId = "arts",
                subjectId = "political_science",
                subjectName = "Political Science",
                chapterNumber = 1,
                levelId = "c11_art_pol_l1",
                topicTitle = "Indian Constitution, Fundamental Rights & Five Writs",
                contentText = "Part III guarantees six justiciable Fundamental Rights (Art. 14-32), enforced by Supreme/High Courts via five writs including Habeas Corpus and Mandamus.",
                formulaTags = "Art 14 Equality, Art 21 Life & Liberty, Art 32/226 Writs, 44th Amendment Art 300A",
                workedExample = "Unlawful detention triggers a Habeas Corpus writ petition under Article 32 or 226."
            ),
            TextbookChunk(
                chunkId = "c11_art_hist_ch2_empire",
                standardId = "class_11",
                streamId = "arts",
                subjectId = "history",
                subjectName = "History",
                chapterNumber = 2,
                levelId = "c11_art_hist_l1",
                topicTitle = "Mesopotamian Cuneiform & Roman Empire Across Three Continents",
                contentText = "Mesopotamian Ur/Uruk pioneered cuneiform tablets and cylinder seals, while Augustus's Roman Principate unified Mediterranean trade with denarius silver coinage.",
                formulaTags = "c.3200 BCE Cuneiform Sumer, 27 BCE Augustus Principate, Silver Denarius, Constantine Solidus",
                workedExample = "Roman curiales city councils administered taxation and urban grain supply across provinces."
            )
        )
    }

    // =========================================================================
    // CLASS 12 • SCIENCE (PCM)
    // =========================================================================
    private val class12PcmChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c12_pcm_math_ch4_matrix",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 4,
                levelId = "c12_pcm_math_l1",
                topicTitle = "Matrices, Determinants, Adjoint & Matrix Inverse",
                contentText = "Non-singular square matrices (|A| != 0) invert via A^-1 = adj(A) / |A|, solving linear systems AX = B cleanly.",
                formulaTags = "A^-1 = adj(A)/|A|, |kA| = k^n*|A|, (AB)^-1 = B^-1*A^-1",
                workedExample = "For [[4,5],[2,3]], |A| = 12 - 10 = 2 and A^-1 = 0.5*[[3,-5],[-2,4]]."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_math_ch7_int",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 7,
                levelId = "c12_pcm_math_l2",
                topicTitle = "Integration by Substitution, Parts & Definite Integrals",
                contentText = "Indefinite integrals recover antiderivatives plus constant C via substitution and ILATE parts, while definite integrals compute signed area.",
                formulaTags = "int x^n dx = x^(n+1)/(n+1) + C, int u dv = uv - int v du, King's property",
                workedExample = "Integrate 2x*e^(x^2) with u = x^2 to obtain e^(x^2) + C."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_math_ch13_bayes",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "math",
                subjectName = "Mathematics",
                chapterNumber = 13,
                levelId = "c12_pcm_math_l3",
                topicTitle = "Conditional Probability & Bayes' Theorem",
                contentText = "Bayes' Theorem updates posterior cause probability P(Ei | A) from prior probability P(Ei) and observed evidence likelihood P(A | Ei).",
                formulaTags = "P(A|B) = P(A and B)/P(B), P(Ei|A) = P(Ei)*P(A|Ei) / Sum(P(Ej)*P(A|Ej))",
                workedExample = "Drawing red ball from Bag I (3R,4B) or Bag II (5R,2B) gives P(Bag II | Red) = 5/8."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_phy_ch3_kirchhoff",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 3,
                levelId = "c12_pcm_phy_l1",
                topicTitle = "Kirchhoff's Circuit Laws, Wheatstone Bridge & Drift Velocity",
                contentText = "Kirchhoff's Junction Rule conserves charge and Loop Rule conserves closed-loop energy, yielding balanced Wheatstone ratio P/Q = R/S.",
                formulaTags = "Sum I_in = Sum I_out, Sum V = 0, P/Q = R/S, v_d = I/(n*e*A)",
                workedExample = "With P=10, Q=20, R=15 ohm, null bridge balance gives S = 30 ohm."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_phy_ch6_emi",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 6,
                levelId = "c12_pcm_phy_l2",
                topicTitle = "Faraday's Law of Electromagnetic Induction & Lenz's Law",
                contentText = "Induced EMF equals negative rate of change of magnetic flux e = -N*dPhi/dt, where Lenz's Law opposes the flux change causing it.",
                formulaTags = "Phi_B = B*A*cos(theta), E = -N*dPhi/dt",
                workedExample = "Flux dropping 0.6 Wb in 0.1 s through 50 turns induces |E| = 300 V."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_phy_ch7_ac",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 7,
                levelId = "c12_pcm_phy_l3",
                topicTitle = "Series LCR AC Circuit, Impedance & Resonance",
                contentText = "Impedance Z combines resistance R and net reactance (XL - XC) via phasors, minimizing Z = R at resonance when XL = XC.",
                formulaTags = "Z = sqrt(R^2 + (XL - XC)^2), f_res = 1/(2*pi*sqrt(LC))",
                workedExample = "Series R=30, XL=70, XC=30 ohm has impedance Z = sqrt(900 + 1600) = 50 ohm."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_phy_ch11_photoelectric",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 11,
                levelId = "c12_pcm_phy_l4",
                topicTitle = "Einstein's Photoelectric Equation & Stopping Potential",
                contentText = "Each photon of energy h*nu ejects electrons with maximum kinetic energy K_max = h*nu - phi_0 above threshold frequency.",
                formulaTags = "K_max = h*nu - phi_0 = e*V_0",
                workedExample = "A 5.0 eV photon on 2.2 eV metal yields K_max = 2.8 eV and stopping potential 2.8 V."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_chem_ch3_electro",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 2,
                levelId = "c12_pcm_chem_l1",
                topicTitle = "Nernst Equation, Electrochemistry & Gibbs Free Energy",
                contentText = "Nernst Equation adjusts standard electrode voltage at 298 K using reaction quotient Q and links cell EMF to Gibbs free energy.",
                formulaTags = "E_cell = E0_cell - (0.0591/n)*log10(Q), Delta_G0 = -n*F*E0_cell",
                workedExample = "Daniell cell (E0=1.10V, n=2) with [Zn2+]=1M, [Cu2+]=0.01M gives E_cell = 1.0409 V."
            ),
            TextbookChunk(
                chunkId = "c12_pcm_chem_ch4_kinetics",
                standardId = "class_12",
                streamId = "science_pcm",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 3,
                levelId = "c12_pcm_chem_l2",
                topicTitle = "First-Order Chemical Kinetics & Half-Life",
                contentText = "First-order reaction decay obeys k = (2.303/t)*log10([R]0/[R]) with constant half-life t_1/2 = 0.693 / k.",
                formulaTags = "k = (2.303/t)*log10([R]0/[R]), t_1/2 = 0.693/k",
                workedExample = "Rate constant k = 0.0693 min^-1 yields constant half-life t_1/2 = 10 min."
            )
        )
    }

    // =========================================================================
    // CLASS 12 • SCIENCE (PCB)
    // =========================================================================
    private val class12PcbChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c12_pcb_bio_ch5_genetics",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "biology",
                subjectName = "Biology",
                chapterNumber = 5,
                levelId = "c12_pcb_bio_l1",
                topicTitle = "DNA Replication, Central Dogma & Hardy-Weinberg Equilibrium",
                contentText = "Semiconservative DNA Polymerase III synthesizes 5'->3' leading and lagging Okazaki strands; Hardy-Weinberg models constant allele frequencies p^2 + 2pq + q^2 = 1.",
                formulaTags = "p + q = 1, p^2 + 2pq + q^2 = 1, DNA -> mRNA -> Protein",
                workedExample = "Recessive q^2 = 0.09 gives q = 0.3, p = 0.7, and carrier frequency 2pq = 42%."
            ),
            TextbookChunk(
                chunkId = "c12_pcb_phy_ch11_photoelectric",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 11,
                levelId = "c12_pcb_phy_l1",
                topicTitle = "Einstein's Photoelectric Equation & Stopping Potential",
                contentText = "Incident photons of frequency above threshold transfer quantum energy h*nu to eject electrons with K_max = h*nu - phi_0 = e*V_0.",
                formulaTags = "K_max = h*nu - phi_0 = e*V_0",
                workedExample = "5.0 eV light on 2.2 eV work function yields K_max = 2.8 eV and V_0 = 2.8 V."
            ),
            TextbookChunk(
                chunkId = "c12_pcb_phy_ch12_bohr",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 12,
                levelId = "c12_pcb_phy_l2",
                topicTitle = "Bohr Hydrogen Spectrum & Rydberg Formula",
                contentText = "Quantized electron transitions between shell n2 and lower shell n1 emit hydrogen spectral lines obeying 1/lambda = R_H * (1/n1^2 - 1/n2^2).",
                formulaTags = "1/lambda = R_H*(1/n1^2 - 1/n2^2), Lyman n1=1, Balmer n1=2",
                workedExample = "First Balmer transition n2=3 -> n1=2 gives wavelength lambda = 36 / (5 * R_H)."
            ),
            TextbookChunk(
                chunkId = "c12_pcb_phy_ch3_kirchhoff",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "physics",
                subjectName = "Physics",
                chapterNumber = 3,
                levelId = "c12_pcb_phy_l3",
                topicTitle = "Kirchhoff's Circuit Laws & Wheatstone Bridge",
                contentText = "Kirchhoff's Junction and Loop laws solve multi-branch circuits and balance a Wheatstone bridge at P/Q = R/S.",
                formulaTags = "Sum I_in = Sum I_out, Sum V = 0, P/Q = R/S",
                workedExample = "Balanced bridge P=10, Q=20, R=15 ohm gives S = 30 ohm."
            ),
            TextbookChunk(
                chunkId = "c12_pcb_chem_ch3_electro",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 2,
                levelId = "c12_pcb_chem_l1",
                topicTitle = "Nernst Equation & Electrochemical Cell Potential",
                contentText = "At 298 K, Nernst Equation E_cell = E0 - (0.0591/n)*log10(Q) relates cell potential to concentration ratio Q.",
                formulaTags = "E_cell = E0_cell - (0.0591/n)*log10(Q), Delta_G0 = -n*F*E0",
                workedExample = "At [Zn2+]=1M, [Cu2+]=0.01M, E_cell = 1.10 - 0.0591 = 1.0409 V."
            ),
            TextbookChunk(
                chunkId = "c12_pcb_chem_ch4_kinetics",
                standardId = "class_12",
                streamId = "science_pcb",
                subjectId = "chemistry",
                subjectName = "Chemistry",
                chapterNumber = 3,
                levelId = "c12_pcb_chem_l2",
                topicTitle = "First-Order Chemical Kinetics & Half-Life",
                contentText = "First-order reaction constant k = (2.303/t)*log10([R]0/[R]) yields concentration-independent half-life 0.693/k.",
                formulaTags = "k = (2.303/t)*log10([R]0/[R]), t_1/2 = 0.693/k",
                workedExample = "k = 0.0693 min^-1 produces half-life 10 minutes."
            )
        )
    }

    // =========================================================================
    // CLASS 12 • COMMERCE
    // =========================================================================
    private val class12CommerceChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c12_com_acc_ch2_ratio",
                standardId = "class_12",
                streamId = "commerce",
                subjectId = "accountancy",
                subjectName = "Accountancy",
                chapterNumber = 5,
                levelId = "c12_com_acc_l1",
                topicTitle = "Accounting Liquidity Ratios (Current & Quick Ratio)",
                contentText = "Current Ratio measures short-term solvency (benchmark 2:1), while Quick Ratio excludes inventory and prepaid expenses (benchmark 1:1).",
                formulaTags = "Current Ratio = Current Assets / Current Liabilities, Quick Ratio = (CA - Inventory - Prepaid)/CL",
                workedExample = "CA Rs 4L, Inventory Rs 1L, CL Rs 2L gives Current Ratio 2:1 and Quick Ratio 1.5:1."
            ),
            TextbookChunk(
                chunkId = "c12_com_acc_ch3_goodwill",
                standardId = "class_12",
                streamId = "commerce",
                subjectId = "accountancy",
                subjectName = "Accountancy",
                chapterNumber = 2,
                levelId = "c12_com_acc_l2",
                topicTitle = "Partnership Goodwill Valuation (Super Profit Method)",
                contentText = "Super Profit equals adjusted Average Profit minus Normal Profit on Capital Employed, capitalized over Years' Purchase as Goodwill.",
                formulaTags = "Normal Profit = Capital * NRR/100, Super Profit = Avg Profit - Normal Profit, Goodwill = Super Profit * Years",
                workedExample = "Capital Rs 5L at 10% normal return and Rs 80,000 average profit gives Super Profit Rs 30,000; x3 years = Rs 90,000 Goodwill."
            ),
            TextbookChunk(
                chunkId = "c12_com_econ_ch3_macro",
                standardId = "class_12",
                streamId = "commerce",
                subjectId = "economics",
                subjectName = "Macroeconomics",
                chapterNumber = 3,
                levelId = "c12_com_econ_l1",
                topicTitle = "National Income (GDP, NNP_FC) & Investment Multiplier",
                contentText = "GDP at market price sums C + I + G + (X - M), while Keynesian Investment Multiplier k = 1/(1 - MPC) amplifies autonomous investment changes.",
                formulaTags = "GDP_MP = C+I+G+(X-M), NNP_FC = GDP_MP - Dep + NFIA - NIT, k = 1/(1-MPC)",
                workedExample = "With MPC = 0.8, multiplier k = 5, turning Rs 100 Cr investment into Rs 500 Cr income."
            ),
            TextbookChunk(
                chunkId = "c12_com_bst_ch2_fayol",
                standardId = "class_12",
                streamId = "commerce",
                subjectId = "business_studies",
                subjectName = "Business Studies",
                chapterNumber = 2,
                levelId = "c12_com_bst_l1",
                topicTitle = "Principles of Management: Henri Fayol & F.W. Taylor",
                contentText = "Fayol's 14 principles emphasize Unity of Command (one boss per worker), Unity of Direction, and Scalar Chain/Gang Plank, contrasted with Taylor's Functional Foremanship.",
                formulaTags = "Unity of Command, Unity of Direction, Scalar Chain & Gang Plank, Taylor Functional Foremanship",
                workedExample = "Two managers issuing conflicting instructions to one salesperson violates Unity of Command."
            )
        )
    }

    // =========================================================================
    // CLASS 12 • ARTS / HUMANITIES
    // =========================================================================
    private val class12ArtsChunks: List<TextbookChunk> by lazy {
        listOf(
            TextbookChunk(
                chunkId = "c12_art_hist_ch1_mod",
                standardId = "class_12",
                streamId = "arts",
                subjectId = "history",
                subjectName = "History & Humanities",
                chapterNumber = 13,
                levelId = "c12_art_hist_l1",
                topicTitle = "Indian National Movement (1917–1947) & Framing of the Constitution",
                contentText = "Gandhian mass movements from Champaran (1917), Non-Cooperation (1920), Dandi March (1930) to Quit India (1942) led to the Constitution adopted on 26 Nov 1949.",
                formulaTags = "1917 Champaran, 1920-22 Non-Cooperation, 1930 Dandi March, 1942 Quit India, 26 Nov 1949 Adoption, 26 Jan 1950 Republic",
                workedExample = "Constituent Assembly chaired by Dr. Rajendra Prasad with Drafting Chair Dr. B.R. Ambedkar adopted the Constitution on 26 Nov 1949."
            ),
            TextbookChunk(
                chunkId = "c12_art_pol_ch1_nation",
                standardId = "class_12",
                streamId = "arts",
                subjectId = "political_science",
                subjectName = "Political Science",
                chapterNumber = 1,
                levelId = "c12_art_pol_l1",
                topicTitle = "Challenges of Nation Building, Princely States & SRC (1956)",
                contentText = "Post-1947 nation building integrated 565 princely states via Instrument of Accession under Sardar Patel and reorganized linguistic states under the 1956 SRC Act.",
                formulaTags = "1947 Instrument of Accession, Sept 1948 Hyderabad, 1953 Andhra State & SRC, 1956 States Reorganisation Act (14 States, 6 UTs)",
                workedExample = "Following Andhra State (1953), the States Reorganisation Act 1956 created 14 states and 6 UTs."
            )
        )
    }
}
