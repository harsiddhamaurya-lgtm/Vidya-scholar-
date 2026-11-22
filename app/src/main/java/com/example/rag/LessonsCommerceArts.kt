package com.example.rag

/**
 * Verified textbook-grade structured lessons for Class 11 & Class 12 Commerce and Arts / Humanities
 * across Accountancy, Microeconomics, Macroeconomics, Political Science, and Modern History.
 *
 * All dates, constitutional articles, accounting rules, and economic multipliers are strictly verified.
 */
internal object LessonsCommerceArts {

    val lessons: Map<String, String> = mapOf(
        "c11_com_acc_ch3_rules" to """
The **Accounting Equation** and **Golden Rules of Double-Entry Bookkeeping** form the foundation of modern financial accounting: every transaction affects at least two accounts so that total debits equal total credits at all times.

### 1. Mathematical Formulation

At any point in time, a firm's resources (Assets) must equal claims of creditors (Liabilities) plus owners (Capital):

$$
\text{Assets} = \text{Liabilities} + \text{Capital}
$$

**where:**
- **Assets:** Economic resources owned by the business (Cash, Machinery, Debtors, Inventory).
- **Liabilities:** External obligations payable to outsiders (Creditors, Bank Loans).
- **Capital (Owner's Equity):** Residual claim of the owner, updated at year-end by:

$$
\text{Closing Capital} = \text{Opening Capital} + \text{Net Profit} - \text{Drawings}
$$

The three **Golden Rules of Accounting** classify every transaction:
- **Personal A/c:** *Debit the receiver, Credit the giver.*
- **Real A/c (Tangible/Intangible Assets):** *Debit what comes in, Credit what goes out.*
- **Nominal A/c (Expenses/Losses, Incomes/Gains):** *Debit all expenses and losses, Credit all incomes and gains.*

### 2. Why It Works (Dual-Aspect Concept)

Every business event has a two-sided economic effect—receiving value in one account and giving equal value from another—so the Balance Sheet always stays balanced.

### 3. Crucial Nuances & Common Pitfalls

- **Prepaid expenses & outstanding expenses are Personal/Representative Personal Accounts:** Do not treat them as purely Nominal accounts.
- **Drawings reduce Capital:** Cash or goods withdrawn by the proprietor for personal use is *not* a business expense; it directly lowers Capital.

### 4. Quick Worked Example

Record the journal entry when a business purchases machinery for cash ₹50,000.

Both Machinery and Cash are **Real Accounts**: Machinery comes into the business (Debit) and Cash goes out (Credit):

$$
\text{Machinery A/c} \quad \text{Dr. } ₹50,000 \quad \longrightarrow \quad \text{To Cash A/c} \quad ₹50,000
$$

Notice total Assets remain unchanged ($+₹50,000$ Machinery, $-₹50,000$ Cash), preserving $\text{Assets} = \text{Liabilities} + \text{Capital}$.
""".trim(),

        "c11_com_econ_ch2_demand" to """
The **Law of Diminishing Marginal Utility (DMU)** states that as a consumer consumes successive identical units of a commodity continuously, the additional satisfaction (**Marginal Utility**) derived from each extra unit steadily declines.

### 1. Mathematical Formulation

Marginal Utility of the ${'$'}n${'$'}-th unit (${'$'}MU_n${'$'}) is the change in Total Utility (${'$'}TU${'$'}):

$$
MU_n = TU_n - TU_{n-1}
$$

**where:**
- ${'$'}TU_n${'$'} is cumulative utility from consuming ${'$'}n${'$'} units.
- ${'$'}MU_n${'$'} is additional satisfaction gained specifically from the ${'$'}n${'$'}-th unit.

For **two-commodity consumer equilibrium** with prices ${'$'}P_x, P_y${'$'}, a rational consumer allocates budget until the marginal utility per rupee is equal across goods:

$$
\frac{MU_x}{P_x} = \frac{MU_y}{P_y}
$$

### 2. Why It Works (Satiation & Equi-Marginal Logic)

When $\frac{MU_x}{P_x} > \frac{MU_y}{P_y}$, spending one more rupee on good ${'$'}X${'$'} yields more satisfaction than on good ${'$'}Y${'$'}. As the consumer buys more ${'$'}X${'$'}, DMU lowers ${'$'}MU_x${'$'} until the ratios equalize at maximum total utility.

### 3. Crucial Nuances & Common Pitfalls

- **${'$'}TU${'$'} peaks when ${'$'}MU = 0${'$'}:** When ${'$'}MU > 0${'$'}, ${'$'}TU${'$'} rises; at the point of satiety ${'$'}MU = 0${'$'} and ${'$'}TU${'$'} is maximum; when ${'$'}MU < 0${'$'}, ${'$'}TU${'$'} falls.
- **Assumptions required:** Standard unit size, continuous consumption without time gaps, and unchanged consumer tastes.

### 4. Quick Worked Example

Suppose ${'$'}TU_3 = 28${'$'} utils and ${'$'}TU_4 = 33${'$'} utils. Find ${'$'}MU_4${'$'}, and check equilibrium if ${'$'}P_x = ₹5${'$'} and ${'$'}MU_y/P_y = 1${'$'}.

Compute ${'$'}MU_4${'$'}:

$$
MU_4 = TU_4 - TU_3 = 33 - 28 = 5\text{ utils}
$$

Check equilibrium ratio:

$$
\frac{MU_x}{P_x} = \frac{5}{5} = 1 = \frac{MU_y}{P_y} \quad (\text{Consumer is in equilibrium})
$$
""".trim(),

        "c11_com_econ_ch3_elasticity" to """
**Price Elasticity of Demand (${'$'}E_d${'$'})** measures the percentage responsiveness of quantity demanded of a good to a percentage change in its own price, keeping other factors constant.

### 1. Mathematical Formulation

Using the percentage-change and absolute-change forms:

$$
E_d = -\frac{\%\,\Delta Q_d}{\%\,\Delta P} = -\left(\frac{\Delta Q}{\Delta P}\right) \cdot \frac{P}{Q}
$$

**where:**
- ${'$'}P${'$'} is original price and ${'$'}Q${'$'} is original quantity demanded.
- $\Delta P = P_1 - P$ and $\Delta Q = Q_1 - Q$ are changes in price and quantity.
- $|E_d| > 1$: **Elastic** demand; $|E_d| = 1$: **Unitary elastic**; $|E_d| < 1$: **Inelastic** demand.

### 2. Why It Works (Slope vs. Elasticity)

Demand curve slope $\frac{\Delta P}{\Delta Q}$ changes with unit choices (rupees vs. paise, kg vs. grams). Multiplying by $\frac{P}{Q}$ makes ${'$'}E_d${'$'} completely unit-free so elasticities can be compared across different goods.

### 3. Crucial Nuances & Common Pitfalls

- **Minus sign convention:** Because price and demand move inversely ($\Delta Q / \Delta P < 0$), economists prefix a negative sign or report $|E_d| \ge 0$.
- **Slope is not elasticity:** Along a straight-line demand curve, slope is constant while ${'$'}E_d${'$'} varies from $\infty$ at the vertical intercept to $0$ at the horizontal intercept.

### 4. Quick Worked Example

Price drops from ${'$'}P = ₹20${'$'} to $₹18$ ($-10\%$), causing quantity demanded to rise from ${'$'}Q = 100${'$'} to $120$ units ($+20\%$). Find ${'$'}E_d${'$'}.

$$
E_d = -\left(\frac{+20}{-2}\right) \cdot \frac{20}{100} = 10 \times 0.2 = 2 \quad (\text{Elastic, } E_d > 1)
$$
""".trim(),

        "c11_art_pol_ch1_const" to """
A **Constitution** is the supreme foundational legal document that coordinates society, limits arbitrary government power, and empowers democratic governance. In India, **Part III (Articles 12–35)** enshrines justiciable **Fundamental Rights** (core clusters in **Articles 14–32**).

### 1. Core Rights & Constitutional Articles

The six Fundamental Rights guaranteed under the Indian Constitution are:

- **Right to Equality (Articles 14–18):** Equality before law (Art. 14), non-discrimination (Art. 15), abolition of untouchability (Art. 17).
- **Right to Freedom (Articles 19–22):** Six freedoms under Art. 19 (speech, assembly, movement, etc.) and **Protection of Life and Personal Liberty (Art. 21)**.
- **Right against Exploitation (Articles 23–24):** Prohibits human trafficking, forced labour, and hazardous child labour below age $14$.
- **Right to Freedom of Religion (Articles 25–28):** Freedom of conscience and religious practice subject to public order, morality, and health.
- **Cultural and Educational Rights (Articles 29–30):** Protects minority languages, scripts, and educational institutions.
- **Right to Constitutional Remedies (Article 32):** Empowers citizens to move the Supreme Court directly—described by Dr. B.R. Ambedkar as the **"heart and soul"** of the Constitution.

### 2. Why It Works (Five Constitutional Writs)

Under **Article 32** (Supreme Court) and **Article 226** (High Courts), courts enforce Fundamental Rights through five writs:
- **Habeas Corpus:** Produces an unlawfully detained person before a court.
- **Mandamus:** Commands a public official or body to perform a legal public duty.
- **Prohibition & Certiorari:** Prevent or quash excess jurisdiction by lower courts/tribunals.
- **Quo Warranto:** Challenges an unauthorized claim to a public office.

### 3. Crucial Nuances & Common Pitfalls

- **Fundamental Rights vs. DPSPs:** Fundamental Rights (Part III) are legally **justiciable** in courts, whereas Directive Principles of State Policy (Part IV) are non-justiciable guiding ideals.
- **Right to Property is no longer a Fundamental Right:** The **44th Constitutional Amendment (1978)** moved it from Article 31 to a legal/constitutional right under **Article 300A**.

### 4. Quick Applied Example

If a citizen is detained without lawful authority or judicial production, which constitutional mechanism applies?

The affected person or relative can petition the High Court under **Article 226** or the Supreme Court directly under **Article 32** for a writ of **Habeas Corpus**, compelling immediate judicial review of the detention's legality.
""".trim(),

        "c12_com_acc_ch2_ratio" to """
**Liquidity Ratios** evaluate a business's short-term solvency—its ability to pay off current liabilities due within 12 months without financial distress.

### 1. Mathematical Formulation

The two primary liquidity ratios are the **Current Ratio** and **Quick (Acid-Test / Liquid) Ratio**:

$$
\text{Current Ratio} = \frac{\text{Current Assets}}{\text{Current Liabilities}}, \qquad \text{Quick Ratio} = \frac{\text{Quick Assets}}{\text{Current Liabilities}}
$$

**where:**
- $\text{Quick Assets} = \text{Current Assets} - \text{Inventory} - \text{Prepaid Expenses}$.
- Ideal benchmark for **Current Ratio** is $2 : 1$.
- Ideal benchmark for **Quick Ratio** is $1 : 1$.

### 2. Why It Works (Excluding Slow-Converting Assets)

Inventory (stock) takes time and market demand to sell into cash, and prepaid expenses cannot be refunded as cash to pay creditors. Subtracting both yields **Quick Assets**, testing immediate cash-readiness.

### 3. Crucial Nuances & Common Pitfalls

- **Exclude Loose Tools & Stores/Spares** when computing Current Assets for liquidity ratios per NCERT/CBSE Accountancy norms.
- **Too high a Current Ratio is not always good:** A ratio like $6:1$ can indicate idle cash or unsold slow-moving inventory rather than healthy efficiency.

### 4. Quick Worked Example

A firm has Current Assets $= ₹4{,}00{,}000$, Inventory $= ₹1{,}00{,}000$, Prepaid Expenses $= ₹0$, and Current Liabilities $= ₹2{,}00{,}000$. Compute Current Ratio and Quick Ratio.

First compute Current Ratio:

$$
\text{Current Ratio} = \frac{4{,}00{,}000}{2{,}00{,}000} = 2 : 1
$$

Next find Quick Assets $= 4{,}00{,}000 - 1{,}00{,}000 = ₹3{,}00{,}000$ and Quick Ratio:

$$
\text{Quick Ratio} = \frac{3{,}00{,}000}{2{,}00{,}000} = 1.5 : 1
$$
""".trim(),

        "c12_com_acc_ch3_goodwill" to """
**Goodwill** is an intangible asset representing the present value of a firm's future earning capacity above normal industry returns due to reputation, location, and customer loyalty. The **Super Profit Method** values only those excess profits.

### 1. Mathematical Formulation

Goodwill under the Super Profit method is calculated in three steps:

$$
\text{Normal Profit} = \text{Capital Employed} \times \frac{\text{Normal Rate of Return}}{100}
$$

$$
\text{Super Profit} = \text{Average Adjusted Profit} - \text{Normal Profit}
$$

$$
\text{Goodwill} = \text{Super Profit} \times \text{Number of Years' Purchase}
$$

**where:**
- **Capital Employed** $= \text{Total Assets (excluding goodwill/fictitious assets)} - \text{Outside Liabilities}$.
- **Years' Purchase** indicates how many years the firm expects to sustain that competitive edge.

### 2. Why It Works (Opportunity Cost Logic)

A buyer could invest the same Capital Employed elsewhere in the industry and earn **Normal Profit** without paying for reputation. Goodwill pays strictly for the extra **Super Profit** beyond that normal benchmark.

### 3. Crucial Nuances & Common Pitfalls

- **Adjust abnormal items first:** Remove abnormal gains (e.g., lottery/asset-sale profit) and add back abnormal losses (e.g., fire loss) before averaging past profits.
- **Non-positive Super Profit:** If $\text{Average Profit} \le \text{Normal Profit}$, Super Profit is zero and Goodwill is nil.

### 4. Quick Worked Example

A firm has Capital Employed $= ₹5{,}00{,}000$, Normal Rate of Return $= 10\%$, Average Profit $= ₹80{,}000$, and $3$ Years' Purchase. Find Goodwill.

Compute Normal Profit and Super Profit:

$$
\text{Normal Profit} = 5{,}00{,}000 \times \frac{10}{100} = ₹50{,}000
$$

$$
\text{Super Profit} = 80{,}000 - 50{,}000 = ₹30{,}000
$$

Compute Goodwill:

$$
\text{Goodwill} = 30{,}000 \times 3 = ₹90{,}000
$$
""".trim(),

        "c12_com_econ_ch3_macro" to """
**National Income (GDP)** measures the total final output produced within an economy, while the **Investment Multiplier (${'$'}k${'$'})** shows how an initial change in autonomous investment creates a larger multiplied rise in national income.

### 1. Mathematical Formulation

By the expenditure method, Gross Domestic Product at Market Price ($\text{GDP}_{MP}$) and Net National Product at Factor Cost ($\text{NNP}_{FC}$, National Income) are:

$$
\text{GDP}_{MP} = C + I + G + (X - M)
$$

$$
\text{NNP}_{FC} = \text{GDP}_{MP} - \text{Depreciation} + \text{NFIA} - \text{Net Indirect Taxes}
$$

**where:**
- ${'$'}C${'$'} is private consumption, ${'$'}I${'$'} is investment, ${'$'}G${'$'} is government expenditure, and $(X - M)$ is net exports.
- $\text{NFIA}$ is Net Factor Income from Abroad; $\text{Net Indirect Taxes} = \text{Indirect Taxes} - \text{Subsidies}$.

Keynes's **Investment Multiplier (${'$'}k${'$'})** connects income change $\Delta Y$ to investment change $\Delta I$ via Marginal Propensity to Consume ($\text{MPC}$) and Save ($\text{MPS}$):

$$
k = \frac{\Delta Y}{\Delta I} = \frac{1}{1 - \text{MPC}} = \frac{1}{\text{MPS}}
$$

### 2. Why It Works (Circular Spending Rounds)

One person's expenditure becomes another person's income. If $\text{MPC} = 0.8$, an extra $₹100$ of investment creates $₹100 + ₹80 + ₹64 + \dots = 100 \times \frac{1}{1 - 0.8} = ₹500$ in total national income.

### 3. Crucial Nuances & Common Pitfalls

- **$\text{MPC} + \text{MPS} = 1$ always:** Higher saving ($\text{MPS}$) shrinks the multiplier, which is the *Paradox of Thrift*.
- **Avoid double counting:** Include only **final** goods and services in GDP, never intermediate inputs.

### 4. Quick Worked Example

If $\text{MPC} = 0.8$ and autonomous investment rises by $\Delta I = ₹100\text{ Cr}$, find the multiplier ${'$'}k${'$'} and total increase in national income $\Delta Y$.

$$
k = \frac{1}{1 - 0.8} = \frac{1}{0.2} = 5
$$

$$
\Delta Y = k \cdot \Delta I = 5 \times 100 = ₹500\text{ Cr}
$$
""".trim(),

        "c12_art_hist_ch1_mod" to """
The **Indian National Movement** evolved into a nationwide mass struggle under Mahatma Gandhi's leadership (1917–1947), culminating in independence and the framing of the **Constitution of India** by the Constituent Assembly (1946–1949).

### 1. Core Historical Milestones & Verified Dates

Key chronological milestones of the mass movement and constitutional transition:

- **1917 — Champaran Satyagraha (Bihar):** Gandhi's first major civil disobedience in India against the exploitative indigo *tinkathia* system.
- **1918 — Kheda Satyagraha & Ahmedabad Mill Strike (Gujarat):** Peasant tax-remission struggle and industrial workers' wage arbitration.
- **1920–1922 — Non-Cooperation Movement:** Boycott of colonial schools, courts, titles, and foreign cloth (combined with Khilafat); withdrawn in **February 1922** after the **Chauri Chaura** incident.
- **1930 — Civil Disobedience & Dandi Salt March (12 March – 6 April 1930):** $240$-mile march from Sabarmati Ashram to Dandi to break the British salt tax monopoly.
- **1942 (8 August) — Quit India Movement:** Call for immediate end to British rule (*"Do or Die"*).
- **1946–1949 — Framing of the Constitution:** Constituent Assembly (first sat **9 December 1946**), chaired by President **Dr. Rajendra Prasad**, with Drafting Committee Chair **Dr. B.R. Ambedkar**.
- **26 November 1949:** Constitution **adopted** by the Constituent Assembly; **26 January 1950:** Constitution came into full force (**Republic Day**, honouring the **1930 Purna Swaraj** pledge).

### 2. Why It Works (Cause & Mass Mobilization)

Targeting symbols shared across caste and class—like **khadi** (economic self-reliance) and **salt** (a universal household necessity taxed by the colonial state)—transformed elitist politics into a disciplined nationwide movement.

### 3. Crucial Nuances & Common Pitfalls

- **Adoption vs. Enforcement dates:** **26 November 1949** is the date of *adoption* (now Constitution Day), whereas **26 January 1950** is the date of *commencement* (Republic Day).
- **Assembly President vs. Drafting Chair:** **Dr. Rajendra Prasad** was President of the Constituent Assembly; **Dr. B.R. Ambedkar** was Chairman of the seven-member Drafting Committee.

### 4. Quick Analytical Example

*Question:* Why did Mahatma Gandhi choose Salt as the central symbol of Civil Disobedience in 1930?

*Answer:* The British state monopoly on salt taxed an essential daily commodity consumed equally by rich and poor across all regions and communities. Breaking the salt law at Dandi on **6 April 1930** unified rural peasants and urban citizens into peaceful mass defiance without dividing social groups.
""".trim()
    )
}
