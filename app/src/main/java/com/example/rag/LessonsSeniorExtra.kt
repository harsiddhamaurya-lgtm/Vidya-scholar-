package com.example.rag

/**
 * Additional latest NCERT lessons for Class 11 & Class 12 across Physics, Biology,
 * Mathematics, Accountancy, Business Studies, History, and Political Science.
 */
internal object LessonsSeniorExtra {

    val lessons: Map<String, String> = mapOf(
        "c11_pcm_phy_ch8_grav" to """
**Gravitation, Orbital Velocity & Escape Velocity** (NCERT Class 11 Physics) describe planetary orbits via Kepler's laws and compute the minimum speed needed for a projectile to escape a planet's gravitational pull.

### 1. Mathematical Formulation

Gravitational potential energy ${'$'}U${'$'} of a mass ${'$'}m${'$'} at distance ${'$'}r${'$'} from planet mass ${'$'}M${'$'}, orbital speed ${'$'}v_o${'$'} near radius ${'$'}R${'$'}, and escape speed ${'$'}v_e${'$'} are:

$$
U(r) = -\frac{G M m}{r}, \qquad v_o = \sqrt{\frac{G M}{R}} = \sqrt{g R}, \qquad v_e = \sqrt{\frac{2 G M}{R}} = \sqrt{2 g R}
$$

**where:**
- ${'$'}G = 6.67 \times 10^{-11}\text{ N}\cdot\text{m}^2/\text{kg}^2${'$'} and ${'$'}M${'$'}, ${'$'}R${'$'} are planet mass and radius.
- On Earth (${'$'}g \approx 9.8\text{ m/s}^2, R \approx 6400\text{ km}${'$'}), ${'$'}v_e \approx 11.2\text{ km/s}${'$'}.

### 2. Why It Works (Conservation of Mechanical Energy)

For an object launched from surface radius ${'$'}R${'$'} to just reach infinity with zero leftover kinetic and potential energy (${'$'}K_\infty + U_\infty = 0${'$'}):

$$
\frac{1}{2}m v_e^2 - \frac{G M m}{R} = 0 \implies v_e = \sqrt{\frac{2 G M}{R}} = \sqrt{2}\,v_o
$$

### 3. Crucial Nuances & Common Pitfalls

- **Independent of projectile mass and launch angle:** Ignoring air drag, ${'$'}v_e${'$'} is the same ($11.2\text{ km/s}$ on Earth) whether launching a $1\text{ kg}$ stone or a $10{,}000\text{ kg}$ rocket, at any upward angle.
- **Kepler's Third Law (${'$'}T^2 \propto r^3${'$'}):** Squaring orbital period scales with the cube of the semi-major axis.

### 4. Quick Worked Example

A satellite orbits very close to Earth where low-orbit speed is ${'$'}v_o = 7.9\text{ km/s}${'$'}. What escape speed ${'$'}v_e${'$'} is needed from Earth's surface?

$$
v_e = \sqrt{2}\,v_o \approx 1.414 \times 7.9 \approx 11.2\text{ km/s}
$$
""".trim(),

        "c11_pcb_bio_ch13_photo" to """
**Photosynthesis in Higher Plants** (NCERT Class 11 Biology) converts solar energy into chemical energy across two coupled stages inside chloroplasts: the **Light Reaction** (thylakoid membranes) and the **Calvin Cycle / C3 Cycle** (stroma).

### 1. Core Biochemical Equation & Stages

Overall fixation of carbon dioxide into hexose sugar requires $18\text{ ATP}$ and $12\text{ NADPH}$ per glucose molecule:

$$
6\text{CO}_2 + 18\text{ATP} + 12\text{NADPH} \xrightarrow{\text{RuBisCO / Calvin Cycle}} \text{C}_6\text{H}_{12}\text{O}_6 + 18\text{ADP} + 18\text{P}_i + 12\text{NADP}^+
$$

**where:**
- **Non-cyclic photophosphorylation (Z-scheme):** Involves both PS II (${'$'}P_{680}${'$'}) and PS I (${'$'}P_{700}${'$'}), splitting $\text{H}_2\text{O}$ (photolysis liberating $\text{O}_2$) and synthesizing both **ATP** and **$\text{NADPH}$**.
- **Calvin Cycle steps:** (1) **Carboxylation** of RuBP ($5\text{C}$) by **RuBisCO** into two $3\text{-PGA}$ molecules; (2) **Reduction** using ATP and NADPH; (3) **Regeneration** of RuBP using $1\text{ ATP}$ per $\text{CO}_2$.

### 2. Why It Works (Chemiosmotic ATP Synthesis)

Proton pumping into the thylakoid lumen builds an electrochemical proton gradient across the thylakoid membrane; protons flowing back out through **$\text{CF}_0\text{–CF}_1$ ATP synthase** phosphorylate $\text{ADP} \to \text{ATP}$.

### 3. Crucial Nuances & Common Pitfalls

- **C3 vs. C4 (Hatch-Slack) plants:** C4 plants (maize, sugarcane) use **Kranz anatomy** and **PEPcase** in mesophyll cells to concentrate $\text{CO}_2$ around RuBisCO in bundle-sheath cells, eliminating wasteful **photorespiration**.
- **Cyclic photophosphorylation:** Uses only PS I (${'$'}P_{700}${'$'}), produces **only ATP** (no NADPH and no $\text{O}_2$ evolution).

### 4. Quick Competency Example

How many ATP and NADPH molecules are needed to fix **3 turns** of the Calvin cycle to export one net $3\text{-carbon}$ triose phosphate?

Each $\text{CO}_2$ turn consumes $3\text{ ATP}$ and $2\text{ NADPH}$, so $3$ turns consume **$9\text{ ATP}$ and $6\text{ NADPH}$** (doubling to $18\text{ ATP} + 12\text{ NADPH}$ for one $6$-carbon glucose).
""".trim(),

        "c11_com_acc_ch7_depr" to """
**Depreciation** (NCERT Class 11 Accountancy) systematically allocates the depreciable cost of a tangible fixed asset over its estimated useful life due to wear and tear, passage of time, or obsolescence.

### 1. Mathematical Formulation

The two primary NCERT methods are **Straight Line Method (SLM)** and **Written Down Value Method (WDV)**:

$$
\text{Annual Depreciation (SLM)} = \frac{\text{Original Cost} - \text{Scrap (Salvage) Value}}{\text{Estimated Useful Life (Years)}}
$$

$$
\text{Annual Depreciation (WDV)} = \text{Book Value at Start of Year} \times \frac{\text{Rate } (\%)}{100}
$$

**where:**
- Under **SLM**, annual depreciation stays fixed every year on original cost until book value reaches salvage value.
- Under **WDV**, percentage rate applies to the *reducing* carrying balance each year, so annual depreciation declines over time and book value never reaches exact zero.

### 2. Why It Works (Matching Principle)

Fixed assets generate revenue across multiple accounting years. Charging depreciation matches asset cost against each period's revenue rather than expensing the entire purchase price in Year 1.

### 3. Crucial Nuances & Common Pitfalls

- **Depreciation is a non-cash expense:** Recording `Depreciation A/c Dr. To Machinery A/c` reduces accounting profit without any cash outflow.
- **Land is not depreciated:** Freehold land normally has unlimited useful life.

### 4. Quick Worked Example

A machine costs $₹1{,}10{,}000$, has scrap value $₹10{,}000$, and useful life $5\text{ years}$. Find annual SLM depreciation and its Book Value after $2$ years.

$$
\text{Annual Depreciation} = \frac{1{,}10{,}000 - 10{,}000}{5} = ₹20{,}000\text{ per year}
$$

After $2$ years, total accumulated depreciation is $2 \times 20{,}000 = ₹40{,}000$, leaving:

$$
\text{Book Value (End of Year 2)} = 1{,}10{,}000 - 40{,}000 = ₹70{,}000
$$
""".trim(),

        "c11_com_bst_ch2_forms" to """
**Forms of Business Organisation** (NCERT Class 11 Business Studies Ch. 2) compare Sole Proprietorship, Partnership, Cooperative Society, and Joint Stock Company based on liability, capital, continuity, and legal identity.

### 1. Core Structural Comparison

- **Sole Proprietorship:** Single owner, full control and profits, **unlimited liability**, no separate legal entity.
- **Partnership (Indian Partnership Act, 1932):** Minimum $2$ (max $50$) partners sharing profits under a **Partnership Deed**; partners bear **unlimited joint and several liability** and mutual agency.
- **Joint Stock Company (Companies Act, 2013):** Artificial legal person with **perpetual succession**, common seal, and **limited liability** (shareholder risk capped at unpaid share value).

### 2. Why It Works (Limited Liability & Scale)

Limiting investor liability to share capital enables thousands of public investors to pool large capital safely without risking personal household assets if the company incurs losses.

### 3. Crucial Nuances & Common Pitfalls

- **Registration rules:** Partnership registration is *optional* (though unregistered firms cannot sue outsiders or partners); Company incorporation is *compulsory*.
- **Private vs. Public Company:** A Private Ltd. company needs minimum $2$ members (max $200$) and restricts share transfers; a Public Ltd. company needs minimum $7$ members with no upper limit.

### 4. Quick Applied Example

If a partnership firm owes $₹10\text{ lakh}$ after liquidating business assets worth $₹6\text{ lakh}$, creditors can legally recover the remaining $₹4\text{ lakh}$ from the partners' **personal assets** due to unlimited liability—unlike a Joint Stock Company where shareholders' personal wealth stays shielded.
""".trim(),

        "c11_art_hist_ch2_empire" to """
**Writing, City Life & Early Empires** (NCERT Class 11 History Themes) trace how Mesopotamia's cuneiform records and the Roman Empire's urban-administrative network transformed trade, taxation, and legal governance.

### 1. Core Historical Milestones & Institutional Anchors

- **Mesopotamia (c. 3200 BCE onwards):** **Cuneiform** clay-tablet script developed in Sumer (cities like **Ur** and **Uruk**) to track grain, temple accounts, and commercial contracts; irrigated Euphrates agriculture supported craft specialization.
- **Roman Empire (27 BCE – 476 CE West):** Founded by **Augustus (Principate, 27 BCE)**; relied on Mediterranean sea trade (*Mare Nostrum*), unified silver currency (**Denarius**), urban civic centers (Carthage, Alexandria, Antioch), and **Roman Law**.
- **Third-Century Crisis & Late Antiquity:** Emperor **Diocletian** cut back over-extended borders; **Constantine** created a second capital at **Constantinople** and introduced the gold **Solidus**.

### 2. Why It Works (Surplus, Literacy & Infrastructure)

River/sea transport dramatically lowered the cost of moving bulk grain, olive oil, and amphorae, while standardized writing and coinage allowed emperors and merchants to audit taxes and contracts across distant provinces.

### 3. Crucial Nuances & Common Pitfalls

- **Mesopotamian cylinder seals vs. clay tablets:** Cylinder seals authenticated ownership and rolled signatures over wet clay envelopes containing cuneiform tablets.
- **Slavery in Rome:** Roman economy used both enslaved labour on aristocratic *latifundia* estates and hired wage labour (especially as imperial conquests slowed in late antiquity).

### 4. Quick Analytical Example

*Question:* Why were cities so central to Roman imperial governance?

*Answer:* Roman cities acted as administrative hubs with local councils (*curiales*) responsible for collecting imperial taxes from surrounding rural territories, enforcing Roman law, and maintaining public baths, amphitheatres, and grain supplies.
""".trim(),

        "c12_pcm_math_ch4_matrix" to """
**Matrices & Determinants** (NCERT Class 12 Maths) solve systems of linear equations and represent linear transformations via square-array determinants, cofactors, and the matrix inverse ${'$'}A^{-1}${'$'}.

### 1. Mathematical Formulation

For a $2 \times 2$ square matrix ${'$'}A = \begin{bmatrix} a & b \\ c & d \end{bmatrix}${'$'}:

$$
|A| = \det(A) = a d - b c
$$

If ${'$'}|A| \neq 0${'$'} (${'$'}A${'$'} is **non-singular**), its **adjoint** swaps diagonal elements and negates off-diagonals, giving its unique inverse:

$$
A^{-1} = \frac{1}{|A|}\operatorname{adj}(A) = \frac{1}{ad - bc}\begin{bmatrix} d & -b \\ -c & a \end{bmatrix}
$$

**where:**
- ${'$'}A \cdot \operatorname{adj}(A) = \operatorname{adj}(A) \cdot A = |A|\,I${'$'}.
- For an ${'$'}n \times n${'$'} matrix, ${'$'}|\operatorname{adj}(A)| = |A|^{n - 1}${'$'} and ${'$'}|k A| = k^n |A|${'$'}.

### 2. Why It Works (Solving ${'$'}AX = B${'$'})

Multiplying ${'$'}A X = B${'$'} on the left by ${'$'}A^{-1}${'$'} collapses ${'$'}A^{-1} A = I${'$'}, yielding the unique solution vector directly:

$$
X = A^{-1} B
$$

### 3. Crucial Nuances & Common Pitfalls

- **Matrix multiplication is NOT commutative:** In general ${'$'}A B \neq B A${'$'}, and ${'$'}{(A B)^{-1} = B^{-1} A^{-1}}${'$'} (reversed order!).
- **Factor ${'$'}k^n${'$'} from determinants:** Scaling an ${'$'}n \times n${'$'} matrix by ${'$'}k${'$'} multiplies every row by ${'$'}k${'$'}, scaling ${'$'}|k A| = k^n |A|${'$'}, not ${'$'}k |A|${'$'}.

### 4. Quick Worked Example

Find ${'$'}A^{-1}${'$'} for ${'$'}A = \begin{bmatrix} 4 & 5 \\ 2 & 3 \end{bmatrix}${'$'}.

First compute determinant ${'$'}|A|${'$'}:

$$
|A| = (4)(3) - (5)(2) = 12 - 10 = 2 \neq 0
$$

Divide the adjoint by ${'$'}|A| = 2${'$'}:

$$
A^{-1} = \frac{1}{2}\begin{bmatrix} 3 & -5 \\ -2 & 4 \end{bmatrix} = \begin{bmatrix} 3/2 & -5/2 \\ -1 & 2 \end{bmatrix}
$$
""".trim(),

        "c12_pcm_math_ch13_bayes" to """
**Conditional Probability & Bayes' Theorem** (NCERT Class 12 Maths Ch. 13) update the probability of a hidden cause ${'$'}E_i${'$'} once new evidence or event ${'$'}A${'$'} has actually been observed.

### 1. Mathematical Formulation

Conditional probability of ${'$'}A${'$'} given ${'$'}B${'$'} (${'$'}P(B) > 0${'$'}) is:

$$
P(A \mid B) = \frac{P(A \cap B)}{P(B)}
$$

If ${'$'}E_1, E_2, \dots, E_n${'$'} partition the sample space into mutually exclusive exhaustive events, **Bayes' Theorem** gives:

$$
P(E_i \mid A) = \frac{P(E_i)\,P(A \mid E_i)}{\sum_{j=1}^{n} P(E_j)\,P(A \mid E_j)}
$$

**where:**
- ${'$'}P(E_i)${'$'} is the **prior probability** of hypothesis ${'$'}E_i${'$'}.
- ${'$'}P(A \mid E_i)${'$'} is the **likelihood** of observing evidence ${'$'}A${'$'} under ${'$'}E_i${'$'}.
- The denominator equals total probability ${'$'}P(A)${'$'} (**Law of Total Probability**).

### 2. Why It Works (Restricting the Sample Space)

Conditioning on ${'$'}A${'$'} shrinks the universe of outcomes to ${'$'}A${'$'} itself, so the updated probability of cause ${'$'}E_i${'$'} is the fraction of ${'$'}A${'$'}'s total probability weight that passed through branch ${'$'}E_i${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Do not confuse ${'$'}P(A \mid E_i)${'$'} with ${'$'}P(E_i \mid A)${'$'}:** Testing positive given a disease ($99\%$) does not mean having the disease given a positive test ($99\%$) if the base rate ${'$'}P(E_i)${'$'} is rare!
- **Independent vs. Mutually Exclusive:** Independent events satisfy ${'$'}P(A \cap B) = P(A)P(B)${'$'}, whereas mutually exclusive events (with non-zero probabilities) satisfy ${'$'}P(A \cap B) = 0${'$'}.

### 4. Quick Worked Example

Bag I has $3$ red and $4$ black balls; Bag II has $5$ red and $2$ black balls. One bag is chosen at random and a red ball is drawn. Find the probability it came from Bag II.

Here ${'$'}P(B_1) = P(B_2) = \frac{1}{2}${'$'}, ${'$'}P(R \mid B_1) = \frac{3}{7}${'$'}, and ${'$'}P(R \mid B_2) = \frac{5}{7}${'$'}:

$$
P(B_2 \mid R) = \frac{\frac{1}{2} \cdot \frac{5}{7}}{\frac{1}{2} \cdot \frac{3}{7} + \frac{1}{2} \cdot \frac{5}{7}} = \frac{5}{3 + 5} = \frac{5}{8}
$$
""".trim(),

        "c12_com_bst_ch2_fayol" to """
**Principles of Management** (NCERT Class 12 Business Studies Ch. 2) organize Henri Fayol's 14 Administrative Principles and F.W. Taylor's Scientific Management to improve organizational coordination, accountability, and productivity.

### 1. Core Principles (Fayol & Taylor)

Key administrative principles formulated by **Henri Fayol**:

- **Division of Work:** Specialization raises efficiency and output quality.
- **Authority and Responsibility:** Authority must stay balanced with matching responsibility.
- **Unity of Command:** Every employee should receive orders from **one boss only** to prevent dual-subordination confusion.
- **Unity of Direction:** One head and one plan for a group of activities sharing the same objective.
- **Scalar Chain & Gang Plank:** Formal hierarchy line from top to bottom—with **Gang Plank** allowing direct horizontal peer communication during emergencies.
- **Esprit de Corps:** Promoting team spirit (*"I"* replaced by *"We"*).

**F.W. Taylor's Scientific Management** pairs **"Science, Not Rule of Thumb"** with **Functional Foremanship** (separating planning and execution under $8$ specialist bosses).

### 2. Why It Works (Accountability & Speed)

Clear reporting lines (**Unity of Command**) eliminate conflicting instructions, while **Management by Exception** lets managers focus only on significant deviations rather than minor routine variances.

### 3. Crucial Nuances & Common Pitfalls

- **Unity of Command vs. Functional Foremanship:** Fayol insists on **1 boss per worker** (Unity of Command), whereas Taylor's Functional Foremanship deliberately breaks Unity of Command using $8$ specialized foremen.
- **Unity of Command vs. Unity of Direction:** Unity of Command prevents conflict at the *individual worker* level; Unity of Direction aligns *departmental teams* toward one plan.

### 4. Quick Competency Example

*Question:* A sales executive is instructed simultaneously by the Finance Manager to halt discounts and by the Marketing Manager to offer a $10\%$ festive discount. Which principle is violated?

*Answer:* **Unity of Command** is violated because the executive is receiving conflicting orders from two superiors, creating confusion and divided accountability.
""".trim(),

        "c12_art_pol_ch1_nation" to """
**Challenges of Nation Building & Integration of Princely States** (NCERT Class 12 Political Science, *Politics in India Since Independence* Ch. 1) examine how India consolidated unity, linguistic federalism, and democratic institutions after August 1947.

### 1. Three Founding Challenges & Key Milestones

1. **Shaping a Unified Nation:** Accommodating vast linguistic, religious, and cultural diversity right after the Partition of 1947.
2. **Establishing Democracy:** Implementing universal adult franchise and parliamentary accountability.
3. **Inclusive Development:** Ensuring equitable welfare rather than growth for a narrow elite.

**Key Constitutional & Historical Dates:**
- **Instrument of Accession (1947):** Negotiated with $565$ princely states under Home Minister **Sardar Vallabhbhai Patel** and V.P. Menon; **Junagadh, Hyderabad (Sept 1948), Kashmir (Oct 1947), and Manipur** required distinct negotiations/actions.
- **1953 — States Reorganisation Commission (SRC):** Appointed after the creation of **Andhra State (1953)** following Potti Sriramulu's fast.
- **States Reorganisation Act (1956):** Created **14 States and 6 Union Territories** primarily on linguistic lines.

### 2. Why It Works (Linguistic Federalism Strengthened Unity)

Fears that linguistic states would spark separatism proved false: respecting regional languages inside a federal framework integrated communities democratically into India's political mainstream.

### 3. Crucial Nuances & Common Pitfalls

- **1953 vs. 1956:** **Andhra State** was formed in **1953**; the broader **States Reorganisation Act** took effect in **1956** (initially forming 14 states and 6 UTs—Gujarat & Maharashtra split later in **1960**, and Punjab/Haryana in **1966**).
- **Nizam's Hyderabad & Bodhachandra Singh's Manipur:** Distinguish Hyderabad's Standstill Agreement & *Operation Polo* (Sept 1948) from Manipur's June 1948 assembly elections and September 1949 merger agreement.

### 4. Quick Analytical Example

*Question:* Why did the Indian leadership initially hesitate to form linguistic states in 1947, and why did they later accept it in 1956?

*Answer:* Having just witnessed Partition's trauma, leaders feared linguistic division might encourage separatism. However, popular movements and the States Reorganisation Commission showed that linguistic autonomy actually deepened grassroots democracy and strengthened national unity.
""".trim()
    )
}
