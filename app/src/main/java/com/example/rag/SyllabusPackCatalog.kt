package com.example.rag

import com.example.data.AcademicStream
import com.example.data.StandardGrade

/**
 * Curated per-standard and per-stream NCERT / JEE / Board syllabus textbook chunks.
 * Downloaded & indexed strictly on demand for the student's active (standardId, streamId)
 * with zero cross-stream leakage.
 */
object SyllabusPackCatalog {

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
        return allChunks.filter { it.standardId == std && it.streamId == effectiveStream }
    }

    private val allChunks: List<TextbookChunk> = listOf(
        // ==========================================
        // CLASS 9 • GENERAL FOUNDATION
        // ==========================================
        TextbookChunk(
            chunkId = "c9_gen_math_ch1_num",
            standardId = "class_9",
            streamId = "general",
            subjectId = "math",
            subjectName = "Mathematics",
            chapterNumber = 1,
            levelId = "c9_math_l1",
            topicTitle = "Real Numbers, Rationalization & Surds",
            contentText = "Every rational number has a terminating or non-terminating recurring decimal expansion, while irrational numbers like sqrt(2) and pi are non-terminating non-recurring. To rationalize 1 / (a + sqrt(b)), multiply numerator and denominator by the conjugate (a - sqrt(b)).",
            formulaTags = "(a+b)(a-b)=a^2-b^2, x^m * x^n = x^(m+n), x^(1/n) = nth_root(x)",
            workedExample = "Rationalize 1 / (2 + sqrt(3)): Multiply by (2 - sqrt(3)) / (2 - sqrt(3)) = (2 - sqrt(3)) / (4 - 3) = 2 - sqrt(3)."
        ),
        TextbookChunk(
            chunkId = "c9_gen_phy_ch8_motion",
            standardId = "class_9",
            streamId = "general",
            subjectId = "science",
            subjectName = "Physics & Science",
            chapterNumber = 8,
            levelId = "c9_sci_l1",
            topicTitle = "Equations of Uniformly Accelerated Motion & Newton's Laws",
            contentText = "For uniformly accelerated linear motion, velocity-time slope gives acceleration and area under the v-t graph gives displacement. Newton's First Law defines inertia, Second Law states net force equals rate of change of momentum (F = ma), and Third Law pairs equal and opposite action-reaction forces on distinct bodies.",
            formulaTags = "v = u + at, s = ut + (1/2)at^2, v^2 - u^2 = 2as, F = ma, p = mv",
            workedExample = "A car starts from rest (u=0) with a = 4 m/s^2 for 5s. Distance s = 0 + 0.5 * 4 * 25 = 50 m; final velocity v = 0 + 4 * 5 = 20 m/s."
        ),
        TextbookChunk(
            chunkId = "c9_gen_chem_ch3_atoms",
            standardId = "class_9",
            streamId = "general",
            subjectId = "science",
            subjectName = "Chemistry & Science",
            chapterNumber = 3,
            levelId = "c9_sci_l2",
            topicTitle = "Atoms, Molecules & The Mole Concept",
            contentText = "Law of Conservation of Mass states mass is neither created nor destroyed in a chemical reaction. One mole of any substance contains Avogadro's number (6.022 x 10^23) particles, and its mass in grams equals its relative atomic or molecular mass.",
            formulaTags = "n = given_mass / molar_mass, N = n * 6.022e23, Na = 23u, H2O = 18u",
            workedExample = "Number of moles in 54 g of water (H2O molar mass = 18 g/mol): n = 54 / 18 = 3 moles."
        ),

        // ==========================================
        // CLASS 10 • GENERAL FOUNDATION
        // ==========================================
        TextbookChunk(
            chunkId = "c10_gen_math_ch4_quad",
            standardId = "class_10",
            streamId = "general",
            subjectId = "math",
            subjectName = "Mathematics",
            chapterNumber = 4,
            levelId = "c10_math_l1",
            topicTitle = "Quadratic Equations & Discriminant Nature of Roots",
            contentText = "Standard quadratic form is ax^2 + bx + c = 0 (a != 0). The discriminant D = b^2 - 4ac determines root nature: D > 0 gives two distinct real roots, D = 0 gives two equal real roots, and D < 0 implies no real roots.",
            formulaTags = "x = (-b +- sqrt(b^2 - 4ac)) / 2a, D = b^2 - 4ac, sum_roots = -b/a, prod_roots = c/a",
            workedExample = "Solve x^2 - 5x + 6 = 0: D = 25 - 24 = 1 > 0. Roots = (5 +- 1)/2 -> x = 3 or x = 2."
        ),
        TextbookChunk(
            chunkId = "c10_gen_math_ch8_trig",
            standardId = "class_10",
            streamId = "general",
            subjectId = "math",
            subjectName = "Mathematics",
            chapterNumber = 8,
            levelId = "c10_math_l2",
            topicTitle = "Trigonometric Identities & Heights and Distances",
            contentText = "In a right triangle, sin(theta) = opposite/hypotenuse, cos(theta) = adjacent/hypotenuse, tan(theta) = sin(theta)/cos(theta). Pythagorean identities relate squares of ratios for any acute angle.",
            formulaTags = "sin^2(x) + cos^2(x) = 1, 1 + tan^2(x) = sec^2(x), 1 + cot^2(x) = csc^2(x)",
            workedExample = "If tan(A) = 3/4, hypotenuse = sqrt(3^2 + 4^2) = 5, so sin(A) = 3/5 and cos(A) = 4/5."
        ),
        TextbookChunk(
            chunkId = "c10_gen_phy_ch10_optics",
            standardId = "class_10",
            streamId = "general",
            subjectId = "science",
            subjectName = "Physics & Science",
            chapterNumber = 10,
            levelId = "c10_sci_l1",
            topicTitle = "Reflection, Refraction & Mirror/Lens Formula",
            contentText = "Snell's Law relates incident angle i and refracted angle r across media: n1 * sin(i) = n2 * sin(r). Apply New Cartesian Sign Convention where object distance u is always negative in front of mirrors/lenses.",
            formulaTags = "1/f = 1/v + 1/u (mirror), 1/f = 1/v - 1/u (lens), m = -v/u, n = c/v, P = 1/f(m)",
            workedExample = "Concave mirror f = -15 cm, u = -30 cm: 1/v = 1/(-15) - 1/(-30) = -1/30 -> v = -30 cm (real, inverted, same size)."
        ),
        TextbookChunk(
            chunkId = "c10_gen_phy_ch12_elec",
            standardId = "class_10",
            streamId = "general",
            subjectId = "science",
            subjectName = "Physics & Science",
            chapterNumber = 12,
            levelId = "c10_sci_l2",
            topicTitle = "Ohm's Law, Series/Parallel Resistors & Joule Heating",
            contentText = "Ohm's Law states V = I * R at constant temperature. Resistances in series add directly (Req = R1 + R2), while in parallel reciprocals add (1/Req = 1/R1 + 1/R2). Heat dissipated in time t follows Joule's Law.",
            formulaTags = "V = IR, R = rho * L / A, P = VI = I^2*R = V^2/R, H = I^2*R*t",
            workedExample = "Two 6 ohm resistors in parallel give 1/Req = 1/6 + 1/6 = 1/3 -> Req = 3 ohm. With 12V battery, I = 12/3 = 4 A."
        ),

        // ==========================================
        // CLASS 11 • SCIENCE (PCM)
        // ==========================================
        TextbookChunk(
            chunkId = "c11_pcm_math_ch13_calc",
            standardId = "class_11",
            streamId = "science_pcm",
            subjectId = "math",
            subjectName = "Mathematics",
            chapterNumber = 13,
            levelId = "c11_pcm_math_l1",
            topicTitle = "Limits, Continuity & First-Principles Derivatives",
            contentText = "A limit evaluates f(x) as x approaches c. Standard limits include lim(x->0) sin(x)/x = 1 and lim(x->0) (e^x - 1)/x = 1. The derivative f'(x) = lim(h->0) [f(x+h) - f(x)] / h represents instantaneous rate of change and tangent slope.",
            formulaTags = "d/dx(x^n) = n*x^(n-1), d/dx(sin x) = cos x, d/dx(cos x) = -sin x, d/dx(e^x) = e^x, product rule (uv)' = u'v + uv'",
            workedExample = "Find derivative of f(x) = 3x^4 - 5x^2 + 7: Using power rule term-by-term, f'(x) = 12x^3 - 10x."
        ),
        TextbookChunk(
            chunkId = "c11_pcm_phy_ch5_laws",
            standardId = "class_11",
            streamId = "science_pcm",
            subjectId = "physics",
            subjectName = "Physics",
            chapterNumber = 5,
            levelId = "c11_pcm_phy_l1",
            topicTitle = "Free Body Diagrams, Friction, Centripetal Force & Work-Energy",
            contentText = "Decompose all forces along normal and tangential axes on inclined planes (mg*sin(theta) down slope, N = mg*cos(theta)). Limiting static friction f_max = mu_s * N. Work-Energy Theorem equates net work done by all forces to change in kinetic energy.",
            formulaTags = "F = ma, f_max = mu_s * N, a_c = v^2 / r, W_net = Delta_K = (1/2)m(v^2 - u^2), Impulse J = F*dt = Delta_p",
            workedExample = "A 2 kg block slides down a smooth 30 deg incline: acceleration a = g * sin(30) = 9.8 * 0.5 = 4.9 m/s^2."
        ),
        TextbookChunk(
            chunkId = "c11_pcm_phy_ch12_thermo",
            standardId = "class_11",
            streamId = "science_pcm",
            subjectId = "physics",
            subjectName = "Physics",
            chapterNumber = 12,
            levelId = "c11_pcm_phy_l2",
            topicTitle = "First Law of Thermodynamics & PV Processes",
            contentText = "Heat supplied dQ equals increase in internal energy dU plus work done by the gas dW (dQ = dU + P dV). In isothermal processes (T constant), dU = 0 so dQ = dW = nRT ln(V2/V1). In adiabatic processes (dQ = 0), PV^gamma = constant.",
            formulaTags = "PV = nRT, dQ = dU + dW, dU = n * Cv * dT, Cp - Cv = R, gamma = Cp / Cv",
            workedExample = "200 J heat is added to a gas doing 120 J external work: Delta_U = 200 - 120 = +80 J internal energy increase."
        ),
        TextbookChunk(
            chunkId = "c11_pcm_chem_ch2_atomic",
            standardId = "class_11",
            streamId = "science_pcm",
            subjectId = "chemistry",
            subjectName = "Chemistry",
            chapterNumber = 2,
            levelId = "c11_pcm_chem_l1",
            topicTitle = "Bohr Model, Quantum Numbers & Redox Balancing",
            contentText = "Principal (n), Azimuthal (l = 0..n-1), Magnetic (ml = -l..+l), and Spin (ms = +-1/2) quantum numbers uniquely specify every electron (Pauli Exclusion Principle). In redox reactions, oxidation loses electrons (OIL) while reduction gains electrons (RIG).",
            formulaTags = "E_n = -13.6 * Z^2 / n^2 eV, lambda = h / mv (de Broglie), Delta_x * Delta_p >= h / 4pi",
            workedExample = "For 3d orbital: n = 3, l = 2, ml can be {-2, -1, 0, +1, +2}; maximum electrons = 2*(2*2+1) = 10."
        ),
        TextbookChunk(
            chunkId = "c11_pcm_cs_ch1_python",
            standardId = "class_11",
            streamId = "science_pcm",
            subjectId = "coding",
            subjectName = "Computer Science & Python",
            chapterNumber = 6,
            levelId = "c11_pcm_cs_l1",
            topicTitle = "Python Functions, Recursion, Lists & Time Complexity",
            contentText = "Python functions encapsulate reusable logic using def, default arguments, and list comprehensions. Recursion solves problems via base case + smaller subproblems. Binary search on sorted arrays runs in O(log n) compared to O(n) linear scan.",
            formulaTags = "def fn(x):, O(log n) binary search, O(n^2) nested loops, memoization",
            workedExample = "Recursive factorial: def fact(n): return 1 if n <= 1 else n * fact(n - 1). For n=5 returns 120."
        ),

        // ==========================================
        // CLASS 11 • SCIENCE (PCB)
        // ==========================================
        TextbookChunk(
            chunkId = "c11_pcb_bio_ch8_cell",
            standardId = "class_11",
            streamId = "science_pcb",
            subjectId = "biology",
            subjectName = "Biology",
            chapterNumber = 8,
            levelId = "c11_pcb_bio_l1",
            topicTitle = "Cell Cycle, Mitosis vs Meiosis & Fluid Mosaic Membrane",
            contentText = "Singer and Nicolson's Fluid Mosaic Model describes a phospholipid bilayer with embedded integral and peripheral proteins allowing lateral fluidity. Mitosis yields two genetically identical diploid (2n) daughter cells for growth, whereas Meiosis halves ploidy to four haploid (n) gametes with crossing over in Pachytene of Prophase I.",
            formulaTags = "2n -> 2n (Mitosis), 2n -> n (Meiosis), G1 -> S (DNA doubles 2C->4C) -> G2 -> M",
            workedExample = "If a diploid cell has 2n = 16 chromosomes before S phase, after S phase it still has 16 chromosomes but 32 chromatids (4C DNA)."
        ),
        TextbookChunk(
            chunkId = "c11_pcb_phy_ch5_laws",
            standardId = "class_11",
            streamId = "science_pcb",
            subjectId = "physics",
            subjectName = "Physics",
            chapterNumber = 5,
            levelId = "c11_pcb_phy_l1",
            topicTitle = "Newton's Laws, Projectile Motion & Centripetal Dynamics",
            contentText = "Projectile motion decomposes into horizontal constant velocity (u cos theta) and vertical acceleration -g. Maximum horizontal range occurs at theta = 45 degrees. Net centripetal force mv^2/r points radially inward during circular turns.",
            formulaTags = "R = u^2 * sin(2*theta) / g, H_max = u^2 * sin^2(theta) / (2g), F = ma, F_c = mv^2/r",
            workedExample = "At u = 20 m/s, theta = 45 deg, g = 10 m/s^2: Max range R = 400 * sin(90) / 10 = 40 m."
        ),
        TextbookChunk(
            chunkId = "c11_pcb_chem_ch4_bond",
            standardId = "class_11",
            streamId = "science_pcb",
            subjectId = "chemistry",
            subjectName = "Chemistry",
            chapterNumber = 4,
            levelId = "c11_pcb_chem_l1",
            topicTitle = "VSEPR Geometry, Hybridization & Chemical Equilibrium",
            contentText = "Hybridization predicts molecular geometry: sp (linear, 180 deg), sp2 (trigonal planar, 120 deg), sp3 (tetrahedral, 109.5 deg; NH3 pyramidal 107 deg due to 1 lone pair; H2O bent 104.5 deg due to 2 lone pairs). Le Chatelier's Principle predicts equilibrium shift opposing applied stress.",
            formulaTags = "K_c = [Products]^p / [Reactants]^r, K_p = K_c*(RT)^Delta_ng, pH = -log10[H+]",
            workedExample = "In CH4, carbon has 4 bond pairs and 0 lone pairs -> sp3 hybridization with tetrahedral 109.5 deg angles."
        ),

        // ==========================================
        // CLASS 11 • COMMERCE
        // ==========================================
        TextbookChunk(
            chunkId = "c11_com_acc_ch3_rules",
            standardId = "class_11",
            streamId = "commerce",
            subjectId = "accountancy",
            subjectName = "Accountancy",
            chapterNumber = 3,
            levelId = "c11_com_acc_l1",
            topicTitle = "Accounting Equation & Golden Rules of Double-Entry",
            contentText = "Every financial transaction has dual aspect maintaining Assets = Liabilities + Capital. Personal Accounts: Debit the receiver, Credit the giver. Real Accounts: Debit what comes in, Credit what goes out. Nominal Accounts: Debit all expenses and losses, Credit all incomes and gains.",
            formulaTags = "Assets = Liabilities + Capital, Closing Capital = Opening Capital + Profit - Drawings",
            workedExample = "Purchased machinery for cash Rs 50,000: Real A/c Machinery comes in (Debit Machinery A/c Rs 50,000), Cash goes out (Credit Cash A/c Rs 50,000)."
        ),
        TextbookChunk(
            chunkId = "c11_com_econ_ch2_demand",
            standardId = "class_11",
            streamId = "commerce",
            subjectId = "economics",
            subjectName = "Microeconomics",
            chapterNumber = 2,
            levelId = "c11_com_econ_l1",
            topicTitle = "Consumer Equilibrium, Marginal Utility & Price Elasticity of Demand",
            contentText = "Law of Diminishing Marginal Utility states additional satisfaction declines as consumption rises. Consumer equilibrium for two goods occurs when MUx/Px = MUy/Py. Price Elasticity Ed = (% change in Qd) / (% change in P).",
            formulaTags = "Ed = -(Delta_Q / Delta_P) * (P / Q), MU_n = TU_n - TU_(n-1)",
            workedExample = "If price drops 10% (Rs 20 to Rs 18) and quantity demanded rises 20% (100 to 120 units), Ed = 20% / 10% = 2 (elastic demand)."
        ),

        // ==========================================
        // CLASS 11 • ARTS / HUMANITIES
        // ==========================================
        TextbookChunk(
            chunkId = "c11_art_pol_ch1_const",
            standardId = "class_11",
            streamId = "arts",
            subjectId = "political_science",
            subjectName = "Political Science",
            chapterNumber = 1,
            levelId = "c11_art_pol_l1",
            topicTitle = "Constitution as a Living Document & Fundamental Rights",
            contentText = "A Constitution establishes fundamental rules coordinating society, limits arbitrary state power, and enables aspirations. India's Fundamental Rights (Articles 14-32) guarantee Equality, Freedom, Protection against Exploitation, Freedom of Religion, Cultural/Educational Rights, and Constitutional Remedies (Art 32).",
            formulaTags = "Art 14 Equality, Art 19 Speech, Art 21 Life & Liberty, Art 32 Writs (Habeas Corpus, Mandamus)",
            workedExample = "If a citizen is unlawfully detained, High Court (Art 226) or Supreme Court (Art 32) issues a writ of Habeas Corpus."
        ),

        // ==========================================
        // CLASS 12 • SCIENCE (PCM)
        // ==========================================
        TextbookChunk(
            chunkId = "c12_pcm_math_ch7_int",
            standardId = "class_12",
            streamId = "science_pcm",
            subjectId = "math",
            subjectName = "Mathematics",
            chapterNumber = 7,
            levelId = "c12_pcm_math_l1",
            topicTitle = "Integration by Substitution, Parts & Definite Integrals",
            contentText = "Integration reverses differentiation and computes signed area under curves. Use u-substitution for composite integrands f(g(x))*g'(x), and Integration by Parts (ILATE priority) for products: integral(u dv) = u*v - integral(v du). King's property: integral_a^b f(x)dx = integral_a^b f(a+b-x)dx.",
            formulaTags = "int x^n dx = x^(n+1)/(n+1) + C, int 2x*e^(x^2)dx = e^(x^2) + C, int_a^b f'(x)dx = f(b) - f(a)",
            workedExample = "Integrate 2x * e^(x^2) dx: Put u = x^2 -> du = 2x dx. Integral becomes int e^u du = e^u + C = e^(x^2) + C."
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
            contentText = "Kirchhoff's Junction Rule (KCL) reflects charge conservation: sum of currents entering a junction equals sum leaving. Loop Rule (KVL) reflects energy conservation: algebraic sum of EMFs and IR drops around any closed loop is zero. Balanced Wheatstone bridge satisfies P/Q = R/S.",
            formulaTags = "Sum I_in = Sum I_out, Sum V = 0, v_d = I / (n*e*A), P/Q = R/S",
            workedExample = "In a Wheatstone bridge with P=10 ohm, Q=20 ohm, R=15 ohm, null deflection occurs when S = R * (Q/P) = 15 * 2 = 30 ohm."
        ),
        TextbookChunk(
            chunkId = "c12_pcm_phy_ch6_emi",
            standardId = "class_12",
            streamId = "science_pcm",
            subjectId = "physics",
            subjectName = "Physics",
            chapterNumber = 6,
            levelId = "c12_pcm_phy_l2",
            topicTitle = "Faraday's Law, Lenz's Law & Alternating Current Impedance",
            contentText = "Induced EMF equals negative time rate of change of magnetic flux: e = -N * d(Phi)/dt, where the minus sign expresses Lenz's Law (induced current opposes the flux change causing it). In series LCR AC circuits, resonance occurs when XL = XC, giving minimum impedance Z = R.",
            formulaTags = "Phi = B*A*cos(theta), e = -N*dPhi/dt, Z = sqrt(R^2 + (XL - XC)^2), f_res = 1 / (2*pi*sqrt(LC))",
            workedExample = "Flux changes uniformly from 0.8 Wb to 0.2 Wb in 0.1 s through a 50-turn coil: |e| = 50 * (0.6 / 0.1) = 300 V."
        ),
        TextbookChunk(
            chunkId = "c12_pcm_chem_ch3_electro",
            standardId = "class_12",
            streamId = "science_pcm",
            subjectId = "chemistry",
            subjectName = "Chemistry",
            chapterNumber = 3,
            levelId = "c12_pcm_chem_l1",
            topicTitle = "Nernst Equation, Electrochemistry & Chemical Kinetics",
            contentText = "Nernst Equation adjusts electrode potential for non-standard ion concentrations at 298K. First-order chemical kinetics has constant half-life independent of initial reactant concentration.",
            formulaTags = "E_cell = E0_cell - (0.0591 / n) * log10(Q), Delta_G0 = -nFE0, k = (2.303/t)*log10([R0]/[R]), t_1/2 = 0.693/k",
            workedExample = "A first-order reaction has rate constant k = 0.0693 min^-1. Its half-life t_1/2 = 0.693 / 0.0693 = 10 minutes."
        ),

        // ==========================================
        // CLASS 12 • SCIENCE (PCB)
        // ==========================================
        TextbookChunk(
            chunkId = "c12_pcb_bio_ch5_genetics",
            standardId = "class_12",
            streamId = "science_pcb",
            subjectId = "biology",
            subjectName = "Biology",
            chapterNumber = 5,
            levelId = "c12_pcb_bio_l1",
            topicTitle = "Mendelian Genetics, DNA Replication & Lac Operon",
            contentText = "Semiconservative DNA replication proceeds in 5' -> 3' direction catalyzed by DNA Polymerase III (leading strand continuous, lagging strand Okazaki fragments). Hardy-Weinberg equilibrium models constant allele frequencies p + q = 1 and genotype frequencies p^2 + 2pq + q^2 = 1.",
            formulaTags = "p + q = 1, p^2 + 2pq + q^2 = 1, Central Dogma: DNA -> mRNA -> Protein, 5'->3' synthesis",
            workedExample = "If recessive phenotype frequency q^2 = 0.09, then q = 0.3, p = 0.7, and heterozygous carrier frequency 2pq = 2*0.7*0.3 = 0.42 (42%)."
        ),
        TextbookChunk(
            chunkId = "c12_pcb_phy_ch3_kirchhoff",
            standardId = "class_12",
            streamId = "science_pcb",
            subjectId = "physics",
            subjectName = "Physics",
            chapterNumber = 3,
            levelId = "c12_pcb_phy_l1",
            topicTitle = "Kirchhoff's Laws, Photoelectric Effect & Bohr Hydrogen Spectrum",
            contentText = "Einstein's Photoelectric Equation states K_max = h*nu - phi_0, demonstrating light's photon nature: kinetic energy depends on frequency above threshold nu_0, while photocurrent scales with intensity.",
            formulaTags = "K_max = h*nu - phi_0 = e*V_0, Sum I = 0, Sum V = 0, 1/lambda = R_H*(1/n1^2 - 1/n2^2)",
            workedExample = "Light of 5.0 eV hits metal of work function 2.2 eV: K_max = 5.0 - 2.2 = 2.8 eV, stopping potential V_0 = 2.8 V."
        ),

        // ==========================================
        // CLASS 12 • COMMERCE
        // ==========================================
        TextbookChunk(
            chunkId = "c12_com_acc_ch2_ratio",
            standardId = "class_12",
            streamId = "commerce",
            subjectId = "accountancy",
            subjectName = "Accountancy",
            chapterNumber = 4,
            levelId = "c12_com_acc_l1",
            topicTitle = "Accounting Ratios, Liquidity & Partnership Goodwill",
            contentText = "Current Ratio measures short-term solvency (ideal 2:1), while Quick/Liquid Ratio excludes inventories and prepaid expenses (ideal 1:1). Super Profit Goodwill capitalizes excess earnings above normal industry return.",
            formulaTags = "Current Ratio = Current Assets / Current Liabilities, Quick Assets = CA - Inventory - Prepaid, Super Profit = Avg Profit - Normal Profit",
            workedExample = "If Current Assets = Rs 4,00,000, Inventory = Rs 1,00,000, Current Liabilities = Rs 2,00,000 -> Current Ratio = 2:1, Quick Ratio = 3,00,000 / 2,00,000 = 1.5:1."
        ),
        TextbookChunk(
            chunkId = "c12_com_econ_ch3_macro",
            standardId = "class_12",
            streamId = "commerce",
            subjectId = "economics",
            subjectName = "Macroeconomics",
            chapterNumber = 3,
            levelId = "c12_com_econ_l1",
            topicTitle = "National Income (GDP), Money Multiplier & Investment Multiplier",
            contentText = "GDP at market price sums consumption, investment, government spending, and net exports (C + I + G + (X - M)). Investment Multiplier k = 1 / (1 - MPC) = 1 / MPS amplifies autonomous investment changes into national income growth.",
            formulaTags = "GDP_MP = C + I + G + (X - M), NNP_FC = GDP_MP - Depreciation + NFIA - Net_Indirect_Tax, k = 1 / (1 - MPC)",
            workedExample = "If MPC = 0.8, Multiplier k = 1 / (1 - 0.8) = 5. An Rs 100 Cr investment raises national income by 5 * 100 = Rs 500 Cr."
        ),

        // ==========================================
        // CLASS 12 • ARTS / HUMANITIES
        // ==========================================
        TextbookChunk(
            chunkId = "c12_art_hist_ch1_mod",
            standardId = "class_12",
            streamId = "arts",
            subjectId = "history",
            subjectName = "History & Humanities",
            chapterNumber = 13,
            levelId = "c12_art_hist_l1",
            topicTitle = "Indian National Movement, Non-Cooperation & Constitutional Framing",
            contentText = "Gandhi's Champaran (1917), Kheda (1918), and Non-Cooperation Movement (1920-22) transformed nationalism into mass satyagraha. The Constituent Assembly (1946-49) chaired by Dr. Rajendra Prasad with Drafting Committee Chair Dr. B.R. Ambedkar adopted the Constitution on 26 Nov 1949.",
            formulaTags = "1917 Champaran, 1920 Non-Cooperation, 1930 Dandi Salt March, 1942 Quit India, 26 Jan 1950 Republic",
            workedExample = "Historical cause-effect: Salt Tax monopoly (1930) united rural and urban classes during Civil Disobedience via the 240-mile Dandi March."
        )
    )
}
