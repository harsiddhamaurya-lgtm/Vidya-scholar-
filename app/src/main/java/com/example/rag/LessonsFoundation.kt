package com.example.rag

/**
 * Verified textbook-grade structured lessons for Class 9 and Class 10 (General Foundation)
 * across Mathematics, Physics, and Chemistry.
 *
 * Every lesson strictly follows interleaved pedagogical formatting:
 * definition + intuition -> ### 1. Mathematical Formulation (equation + where: breakdown) ->
 * ### 2. Why It Works (Derivation) -> ### 3. Crucial Nuances & Common Pitfalls ->
 * ### 4. Quick Worked Example.
 */
internal object LessonsFoundation {

    val lessons: Map<String, String> = mapOf(
        "c9_gen_math_ch1_num" to """
**Rationalizing the denominator** removes irrational square roots (surds) from the denominator of a fraction without changing its numerical value. This makes expressions easier to compare, add, and approximate.

- If a denominator is a single surd $\sqrt{b}$, multiply numerator and denominator by $\sqrt{b}$.
- If a denominator is a binomial $\left(a + \sqrt{b}\right)$, multiply top and bottom by its **rationalizing conjugate** $\left(a - \sqrt{b}\right)$.

### 1. Mathematical Formulation

To rationalize a binomial denominator containing a square root, apply the difference-of-squares identity:

$$
\frac{1}{a + \sqrt{b}} = \frac{1}{a + \sqrt{b}} \cdot \frac{a - \sqrt{b}}{a - \sqrt{b}} = \frac{a - \sqrt{b}}{a^2 - b}
$$

**where:**
- ${'$'}a${'$'} and ${'$'}b${'$'} are rational numbers with ${'$'}b > 0${'$'} and ${'$'}a^2 \neq b${'$'}.
- $\left(a - \sqrt{b}\right)$ is the **conjugate** of $\left(a + \sqrt{b}\right)$.

### 2. Why It Works (Derivation)

Multiplying any non-zero expression by $\frac{a - \sqrt{b}}{a - \sqrt{b}}$ is equivalent to multiplying by $1$, so the value stays identical. Expanding the denominator eliminates the cross-term $\sqrt{b}$:

$$
\left(a + \sqrt{b}\right)\left(a - \sqrt{b}\right) = a^2 - a\sqrt{b} + a\sqrt{b} - \left(\sqrt{b}\right)^2 = a^2 - b
$$

Since both ${'$'}a^2${'$'} and ${'$'}b${'$'} are rational, the new denominator ${'$'}a^2 - b${'$'} is guaranteed to be rational.

### 3. Crucial Nuances & Common Pitfalls

- **Flip only the sign between terms:** The conjugate of $\sqrt{5} + \sqrt{2}$ is $\sqrt{5} - \sqrt{2}$, not $-\sqrt{5} - \sqrt{2}$.
- **Multiply both numerator and denominator:** Multiplying only the denominator changes the value of the fraction.
- **Check non-zero denominator:** Rationalization requires ${'$'}a^2 - b \neq 0${'$'}.

### 4. Quick Worked Example

Rationalize the denominator of $\frac{1}{2 + \sqrt{3}}$.

Multiply both numerator and denominator by the conjugate $2 - \sqrt{3}$:

$$
\frac{1}{2 + \sqrt{3}} \cdot \frac{2 - \sqrt{3}}{2 - \sqrt{3}} = \frac{2 - \sqrt{3}}{2^2 - \left(\sqrt{3}\right)^2}
$$

Simplifying the denominator $4 - 3 = 1$:

$$
\frac{2 - \sqrt{3}}{4 - 3} = 2 - \sqrt{3}
$$
""".trim(),

        "c9_gen_math_ch1_exp" to """
The **Laws of Exponents for Real Numbers** summarize how powers with the same base multiply, divide, and nest, and how fractional powers represent roots.

### 1. Mathematical Formulation

For positive real base ${'$'}a > 0${'$'} and rational exponents ${'$'}m${'$'} and ${'$'}n${'$'}, multiplication adds exponents while division subtracts them:

$$
a^m \cdot a^n = a^{m+n}, \qquad \frac{a^m}{a^n} = a^{m-n}
$$

**where:**
- ${'$'}a > 0${'$'} is the real base.
- ${'$'}m, n${'$'} are rational exponents.

Raising a power to another power multiplies the exponents, and a fractional exponent $\frac{1}{n}$ represents the ${'$'}n${'$'}-th root:

$$
\left(a^m\right)^n = a^{mn}, \qquad a^{1/n} = \sqrt[n]{a}, \qquad a^{-m} = \frac{1}{a^m}
$$

### 2. Why It Works (Intuition)

For positive integers, ${'$'}a^m${'$'} means ${'$'}a${'$'} multiplied by itself ${'$'}m${'$'} times. Putting ${'$'}m${'$'} copies next to ${'$'}n${'$'} copies produces ${'$'}m + n${'$'} total factors of ${'$'}a${'$'}, explaining why exponents add when bases match. Defining ${'$'}a^{1/n} = \sqrt[n]{a}${'$'} keeps $\left(a^{1/n}\right)^n = a^{n/n} = a^1 = a$ consistent.

### 3. Crucial Nuances & Common Pitfalls

- **Bases must match:** $2^3 \cdot 3^2 \neq 6^5$. You can only add exponents when the base is identical.
- **Zero exponent rule:** For any non-zero real number ${'$'}a \neq 0${'$'}, ${'$'}a^0 = 1${'$'}.
- **Distributing over addition is invalid:** $(a + b)^2 \neq a^2 + b^2$.

### 4. Quick Worked Example

Evaluate $16^{3/4}$.

Rewrite $16 = 2^4$ and apply $\left(a^m\right)^n = a^{mn}$:

$$
16^{3/4} = \left(2^4\right)^{3/4} = 2^{4 \cdot \frac{3}{4}} = 2^3 = 8
$$
""".trim(),

        "c9_gen_math_ch2_poly" to """
**Algebraic Identities** are equations that hold true for every real value of the variables, allowing fast expansion and factorization of polynomials.

### 1. Mathematical Formulation

The three foundational quadratic identities are:

$$
(a + b)^2 = a^2 + 2ab + b^2, \qquad (a - b)^2 = a^2 - 2ab + b^2
$$

**where:**
- ${'$'}a${'$'} and ${'$'}b${'$'} represent any real numbers or algebraic expressions.

For the difference of two squares and sum/difference of cubes:

$$
a^2 - b^2 = (a + b)(a - b), \qquad x^3 + y^3 = (x + y)\left(x^2 - xy + y^2\right)
$$

### 2. Why It Works (Factor Theorem Connection)

Expanding $(a+b)(a-b)$ gives ${'$'}a^2 - ab + ab - b^2 = a^2 - b^2${'$'}, where the middle cross-terms cancel identically. By the **Factor Theorem**, a polynomial ${'$'}p(x)${'$'} has $(x - r)$ as a factor if and only if ${'$'}p(r) = 0${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Do not drop the middle term:** $(a+b)^2$ always includes $+2ab$, not just ${'$'}a^2 + b^2${'$'}.
- **Watch the minus signs in $(a-b)^2$:** The last term is $+b^2$ because $(-b)^2 = +b^2$.

### 4. Quick Worked Example

Evaluate $103 \times 97$ without direct long multiplication.

Write $103 = 100 + 3$ and $97 = 100 - 3$, then apply ${'$'}a^2 - b^2 = (a+b)(a-b)${'$'}:

$$
(100 + 3)(100 - 3) = 100^2 - 3^2 = 10000 - 9 = 9991
$$
""".trim(),

        "c9_gen_phy_ch8_motion" to """
The **Equations of Uniformly Accelerated Motion** describe how an object moves along a straight line when its acceleration ${'$'}a${'$'} remains constant over time.

- On a velocity-time (${'$'}v${'$'}-${'$'}t${'$'}) graph, the **slope** equals acceleration ${'$'}a${'$'}.
- The **area** under the ${'$'}v${'$'}-${'$'}t${'$'} curve equals total displacement ${'$'}s${'$'}.

### 1. Mathematical Formulation

The three kinematic equations connect initial velocity ${'$'}u${'$'}, final velocity ${'$'}v${'$'}, constant acceleration ${'$'}a${'$'}, displacement ${'$'}s${'$'}, and time ${'$'}t${'$'}:

$$
v = u + at
$$

$$
s = ut + \frac{1}{2}at^2
$$

$$
v^2 - u^2 = 2as
$$

**where:**
- ${'$'}u${'$'} is initial velocity ($\text{m/s}$).
- ${'$'}v${'$'} is final velocity ($\text{m/s}$).
- ${'$'}a${'$'} is constant acceleration ($\text{m/s}^2$).
- ${'$'}s${'$'} is displacement ($\text{m}$) during time interval ${'$'}t${'$'} ($\text{s}$).

### 2. Why It Works (Graphical Derivation)

Because acceleration is constant, velocity increases linearly from ${'$'}u${'$'} to ${'$'}v${'$'} in time ${'$'}t${'$'}, giving ${'$'}a = \frac{v - u}{t} \implies v = u + at${'$'}. The area under the ${'$'}v${'$'}-${'$'}t${'$'} trapezoid is average velocity times time:

$$
s = \left(\frac{u + v}{2}\right) t = \left(\frac{u + (u + at)}{2}\right) t = ut + \frac{1}{2}at^2
$$

### 3. Crucial Nuances & Common Pitfalls

- **Constant acceleration only:** Never apply these equations when acceleration varies with time.
- **Sign convention:** Choose one positive direction; deceleration or gravity ($-g$) takes a negative sign if opposing initial motion.

### 4. Quick Worked Example

A car starts from rest (${'$'}u = 0${'$'}) and accelerates uniformly at ${'$'}a = 4\text{ m/s}^2${'$'} for ${'$'}t = 5\text{ s}${'$'}. Find its displacement ${'$'}s${'$'} and final velocity ${'$'}v${'$'}.

First compute final velocity:

$$
v = u + at = 0 + (4)(5) = 20\text{ m/s}
$$

Next compute displacement:

$$
s = ut + \frac{1}{2}at^2 = 0 + \frac{1}{2}(4)(5)^2 = 50\text{ m}
$$
""".trim(),

        "c9_gen_phy_ch9_newton" to """
**Newton's Three Laws of Motion** explain how forces change the state of rest or uniform motion of an object.

- **First Law (Inertia):** A body stays at rest or moves at constant velocity unless an unbalanced net external force acts on it.
- **Second Law:** The net force equals the time rate of change of linear momentum.
- **Third Law:** For every action force on object B by object A, there is an equal and opposite reaction force on object A by object B.

### 1. Mathematical Formulation

For a body of constant mass ${'$'}m${'$'}, Newton's Second Law gives:

$$
\vec{F}_{\text{net}} = \frac{d\vec{p}}{dt} = m\vec{a}
$$

**where:**
- $\vec{F}_{\text{net}}$ is the vector sum of all external forces ($\text{N}$).
- $\vec{p} = m\vec{v}$ is linear momentum ($\text{kg}\cdot\text{m/s}$).
- ${'$'}m${'$'} is mass ($\text{kg}$) and $\vec{a}$ is acceleration ($\text{m/s}^2$).

### 2. Why It Works (Conservation Link)

For two colliding bodies with no external force, Newton's Third Law ($\vec{F}_{12} = -\vec{F}_{21}$) ensures total change in momentum is zero ($\Delta\vec{p}_1 + \Delta\vec{p}_2 = 0$), which directly yields the **Law of Conservation of Momentum**.

### 3. Crucial Nuances & Common Pitfalls

- **Action and reaction never cancel each other:** They act on **two different bodies**, so they never appear on the same Free Body Diagram.
- **Mass measures inertia:** Greater mass means greater resistance to change in velocity.

### 4. Quick Worked Example

A constant net horizontal force of ${'$'}F = 18\text{ N}${'$'} pushes a ${'$'}m = 6\text{ kg}${'$'} crate across a smooth floor. Find its acceleration ${'$'}a${'$'}.

$$
a = \frac{F_{\text{net}}}{m} = \frac{18}{6} = 3\text{ m/s}^2
$$
""".trim(),

        "c9_gen_phy_ch11_work_energy" to """
The **Work-Energy Theorem** states that the net work done by all forces acting on an object equals the change in its kinetic energy.

- If net work is positive (${'$'}W_{\text{net}} > 0${'$'}), speed increases.
- If net work is negative (${'$'}W_{\text{net}} < 0${'$'}), speed decreases.
- If net work is zero (${'$'}W_{\text{net}} = 0${'$'}), speed remains unchanged.

### 1. Mathematical Formulation

For a particle of mass ${'$'}m${'$'} whose speed changes from ${'$'}u${'$'} to ${'$'}v${'$'}:

$$
W_{\text{net}} = \Delta K = K_f - K_i = \frac{1}{2}m v^2 - \frac{1}{2}m u^2
$$

**where:**
- ${'$'}W_{\text{net}}${'$'} is total work done by the net force ($\text{J}$).
- ${'$'}K_i = \frac{1}{2}m u^2${'$'} is initial kinetic energy ($\text{J}$).
- ${'$'}K_f = \frac{1}{2}m v^2${'$'} is final kinetic energy ($\text{J}$).
- ${'$'}m${'$'} is mass ($\text{kg}$), ${'$'}u${'$'} is initial speed, and ${'$'}v${'$'} is final speed.

### 2. Why It Works (Derivation)

Starting from constant net force ${'$'}F_{\text{net}} = ma${'$'} along displacement ${'$'}s${'$'}, multiply both sides by ${'$'}s${'$'} and substitute ${'$'}as = \frac{v^2 - u^2}{2}${'$'} from ${'$'}v^2 - u^2 = 2as${'$'}:

$$
W_{\text{net}} = F_{\text{net}} \cdot s = m(as) = m\left(\frac{v^2 - u^2}{2}\right) = \frac{1}{2}mv^2 - \frac{1}{2}mu^2
$$

### 3. Crucial Nuances & Common Pitfalls

- **Use the net force:** Include all forces doing work (gravity, tension, applied force, and friction).
- **Zero work condition:** Any force perpendicular to displacement ($\cos 90^\circ = 0$) does zero work.

### 4. Quick Worked Example

A ${'$'}m = 4\text{ kg}${'$'} cart moving at ${'$'}u = 2\text{ m/s}${'$'} accelerates to ${'$'}v = 6\text{ m/s}${'$'}. Find the net work done on the cart.

$$
W_{\text{net}} = \frac{1}{2}m v^2 - \frac{1}{2}m u^2 = \frac{1}{2}(4)(6^2 - 2^2) = 2(36 - 4) = 64\text{ J}
$$
""".trim(),

        "c9_gen_chem_ch3_atoms" to """
The **Mole Concept** links macroscopic laboratory mass in grams to the microscopic number of atoms, molecules, or ions using **Avogadro's constant**.

### 1. Mathematical Formulation

Number of moles ${'$'}n${'$'} is calculated from sample mass ${'$'}m${'$'} and molar mass ${'$'}M${'$'}:

$$
n = \frac{m}{M}, \qquad N = n \cdot N_A
$$

**where:**
- ${'$'}n${'$'} is the number of moles ($\text{mol}$).
- ${'$'}m${'$'} is the given mass of substance ($\text{g}$).
- ${'$'}M${'$'} is molar mass ($\text{g/mol}$).
- ${'$'}N${'$'} is total number of particles and ${'$'}N_A = 6.022 \times 10^{23}\text{ mol}^{-1}${'$'} is Avogadro's number.

### 2. Why It Works (Atomic Mass Calibration)

Because $1\text{ mole}$ of any element contains exactly $6.022 \times 10^{23}$ entities, its mass in grams is numerically equal to its molecular mass in unified atomic mass units ($\text{u}$). For example, $\text{H}_2\text{O}$ has molecular mass $2(1) + 16 = 18\text{ u}$, so its molar mass is $18\text{ g/mol}$.

### 3. Crucial Nuances & Common Pitfalls

- **Check atoms vs. molecules:** $1\text{ mol}$ of $\text{O}_2$ contains ${'$'}N_A${'$'} oxygen *molecules*, which equals $2 N_A$ oxygen *atoms*.
- **Units must match:** Always express sample mass ${'$'}m${'$'} in grams when dividing by molar mass in $\text{g/mol}$.

### 4. Quick Worked Example

Find the number of moles and molecules in $54\text{ g}$ of water ($\text{H}_2\text{O}$, ${'$'}M = 18\text{ g/mol}${'$'}).

First calculate moles ${'$'}n${'$'}:

$$
n = \frac{54}{18} = 3\text{ mol}
$$

Multiply by Avogadro's number to find total molecules:

$$
N = 3 \times 6.022 \times 10^{23} = 1.8066 \times 10^{24}\text{ molecules}
$$
""".trim(),

        "c10_gen_math_ch4_quad" to """
The **Quadratic Formula** solves any quadratic equation ${'$'}ax^2 + bx + c = 0${'$'} (${'$'}a \neq 0${'$'}) directly, while the **discriminant** ${'$'}D${'$'} reveals the nature of its roots before solving.

### 1. Mathematical Formulation

The roots of ${'$'}ax^2 + bx + c = 0${'$'} are given by:

$$
x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
$$

**where:**
- ${'$'}a, b, c${'$'} are real coefficients with ${'$'}a \neq 0${'$'}.
- ${'$'}D = b^2 - 4ac${'$'} is the **discriminant**:
  - ${'$'}D > 0 \implies${'$'} two distinct real roots.
  - ${'$'}D = 0 \implies${'$'} two equal real roots (${'$'}x = -\frac{b}{2a}${'$'}).
  - ${'$'}D < 0 \implies${'$'} no real roots.

For roots $\alpha$ and $\beta$, Vieta's relations also give:

$$
\alpha + \beta = -\frac{b}{a}, \qquad \alpha \beta = \frac{c}{a}
$$

### 2. Why It Works (Derivation by Completing the Square)

Divide ${'$'}ax^2 + bx + c = 0${'$'} by ${'$'}a${'$'}, move $\frac{c}{a}$ to the right, and add $\left(\frac{b}{2a}\right)^2$ to both sides:

$$
\left(x + \frac{b}{2a}\right)^2 = \frac{b^2 - 4ac}{4a^2}
$$

Taking the square root of both sides isolates ${'$'}x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Divide the entire numerator by $2a$:** Do not forget to include $-b$ inside the fraction numerator over $2a$.
- **Check signs of ${'$'}b${'$'} and ${'$'}c${'$'}:** If the equation is ${'$'}x^2 - 5x + 6 = 0${'$'}, then ${'$'}b = -5${'$'}, so $-b = +5$.

### 4. Quick Worked Example

Solve ${'$'}x^2 - 5x + 6 = 0${'$'}.

Identify ${'$'}a = 1${'$'}, ${'$'}b = -5${'$'}, ${'$'}c = 6${'$'}, and compute ${'$'}D${'$'}:

$$
D = (-5)^2 - 4(1)(6) = 25 - 24 = 1 > 0
$$

Substitute into the quadratic formula:

$$
x = \frac{5 \pm \sqrt{1}}{2} \implies x = 3 \quad \text{or} \quad x = 2
$$
""".trim(),

        "c10_gen_math_ch5_ap" to """
An **Arithmetic Progression (AP)** is a sequence of numbers in which every consecutive pair differs by a fixed constant called the **common difference** ${'$'}d${'$'}.

### 1. Mathematical Formulation

The ${'$'}n${'$'}-th term ${'$'}a_n${'$'} and the sum of the first ${'$'}n${'$'} terms ${'$'}S_n${'$'} are:

$$
a_n = a + (n - 1)d
$$

$$
S_n = \frac{n}{2}\left[2a + (n - 1)d\right] = \frac{n}{2}(a + l)
$$

**where:**
- ${'$'}a${'$'} is the first term.
- ${'$'}d = a_2 - a_1${'$'} is the common difference.
- ${'$'}n${'$'} is the positive integer term index and ${'$'}l = a_n${'$'} is the last term.

### 2. Why It Works (Pairing Intuition)

Writing the sum ${'$'}S_n${'$'} forwards and backwards and adding term-by-term makes every vertical pair equal to $(a + l)$. Since there are ${'$'}n${'$'} identical pairs, $2S_n = n(a + l)$, giving ${'$'}S_n = \frac{n}{2}(a + l)${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **${'$'}d${'$'} can be positive, negative, or zero:** Always compute ${'$'}d = a_{k+1} - a_k${'$'}, never ${'$'}a_k - a_{k+1}${'$'}.
- **${'$'}n${'$'} must be a positive integer:** If solving for ${'$'}n${'$'} produces a negative or fractional value, the given number is not a term of that AP.

### 4. Quick Worked Example

Find the $10$-th term and sum of the first $10$ terms of the AP $3, 7, 11, 15, \dots$

Here ${'$'}a = 3${'$'} and ${'$'}d = 7 - 3 = 4${'$'}:

$$
a_{10} = 3 + (10 - 1)(4) = 3 + 36 = 39
$$

Using ${'$'}S_n = \frac{n}{2}(a + l)${'$'}:

$$
S_{10} = \frac{10}{2}(3 + 39) = 5 \times 42 = 210
$$
""".trim(),

        "c10_gen_math_ch8_trig" to """
**Trigonometric Identities** relate sine, cosine, and tangent ratios of an acute angle $\theta$ in a right-angled triangle and hold true for all angles where the functions are defined.

### 1. Mathematical Formulation

The three Pythagorean trigonometric identities are:

$$
\sin^2\theta + \cos^2\theta = 1
$$

**where:**
- $\sin\theta = \frac{\text{Opposite}}{\text{Hypotenuse}}$ and $\cos\theta = \frac{\text{Adjacent}}{\text{Hypotenuse}}$.
- $\tan\theta = \frac{\sin\theta}{\cos\theta}$ for $\cos\theta \neq 0$.

Dividing the primary identity by $\cos^2\theta$ or $\sin^2\theta$ yields the companion forms:

$$
1 + \tan^2\theta = \sec^2\theta, \qquad 1 + \cot^2\theta = \csc^2\theta
$$

### 2. Why It Works (Derivation)

In a right triangle with legs ${'$'}p${'$'} (opposite), ${'$'}b${'$'} (adjacent), and hypotenuse ${'$'}h${'$'}, Pythagoras' theorem gives ${'$'}p^2 + b^2 = h^2${'$'}. Dividing every term by ${'$'}h^2${'$'}:

$$
\left(\frac{p}{h}\right)^2 + \left(\frac{b}{h}\right)^2 = 1 \implies \sin^2\theta + \cos^2\theta = 1
$$

### 3. Crucial Nuances & Common Pitfalls

- **Notation $\sin^2\theta$ means $(\sin\theta)^2$:** It is not $\sin(\theta^2)$.
- **Domain checks:** $\tan\theta$ and $\sec\theta$ are undefined at $\theta = 90^\circ$.

### 4. Quick Worked Example

Given $\tan A = \frac{3}{4}$ for acute angle ${'$'}A${'$'}, find $\sin A$ and $\cos A$.

Compute the hypotenuse ${'$'}h = \sqrt{3^2 + 4^2} = 5${'$'}:

$$
\sin A = \frac{3}{5}, \qquad \cos A = \frac{4}{5}
$$
""".trim(),

        "c10_gen_phy_ch10_optics" to """
The **Spherical Mirror Formula** connects object distance ${'$'}u${'$'}, image distance ${'$'}v${'$'}, and focal length ${'$'}f${'$'} for concave and convex mirrors under the New Cartesian Sign Convention.

### 1. Mathematical Formulation

For a spherical mirror with small aperture:

$$
\frac{1}{f} = \frac{1}{v} + \frac{1}{u}
$$

**where:**
- ${'$'}f${'$'} is focal length (${'$'}f < 0${'$'} for concave mirror, ${'$'}f > 0${'$'} for convex mirror).
- ${'$'}u${'$'} is object distance (always negative when the object is placed in front of the mirror).
- ${'$'}v${'$'} is image distance (${'$'}v < 0${'$'} real/in front; ${'$'}v > 0${'$'} virtual/behind mirror).

Linear magnification ${'$'}m${'$'} compares image height ${'$'}h_i${'$'} to object height ${'$'}h_o${'$'}:

$$
m = \frac{h_i}{h_o} = -\frac{v}{u}
$$

### 2. Why It Works (Sign Convention Logic)

All distances are measured from the **pole (${'$'}P${'$'})** of the mirror: distances along the direction of incident light are positive, while distances measured against incident light (in front of the mirror) are negative.

### 3. Crucial Nuances & Common Pitfalls

- **Mirror vs. Lens sign:** Mirror formula uses $+$ ($\frac{1}{v} + \frac{1}{u}$), whereas thin lens formula uses $-$ ($\frac{1}{v} - \frac{1}{u}$).
- **Magnification interpretation:** Negative ${'$'}m${'$'} means an **inverted, real** image; positive ${'$'}m${'$'} means an **erect, virtual** image.

### 4. Quick Worked Example

An object sits at ${'$'}u = -30\text{ cm}${'$'} in front of a concave mirror of focal length ${'$'}f = -15\text{ cm}${'$'}. Find ${'$'}v${'$'} and ${'$'}m${'$'}.

Substitute into $\frac{1}{v} = \frac{1}{f} - \frac{1}{u}$:

$$
\frac{1}{v} = \frac{1}{-15} - \frac{1}{-30} = -\frac{1}{30} \implies v = -30\text{ cm}
$$

Compute magnification:

$$
m = -\frac{-30}{-30} = -1 \quad (\text{real, inverted, same size})
$$
""".trim(),

        "c10_gen_phy_ch10_refraction" to """
**Snell's Law** governs how light bends when crossing between two optical media of different refractive indices, forming the basis of image formation in **thin lenses**.

### 1. Mathematical Formulation

At an interface between media with refractive indices ${'$'}n_1${'$'} and ${'$'}n_2${'$'}:

$$
n_1 \sin i = n_2 \sin r, \qquad n = \frac{c}{v}
$$

**where:**
- ${'$'}i${'$'} is the angle of incidence and ${'$'}r${'$'} is the angle of refraction (measured from the normal).
- ${'$'}n_1, n_2${'$'} are absolute refractive indices and ${'$'}c = 3 \times 10^8\text{ m/s}${'$'} is light speed in vacuum.

For a thin spherical lens, image distance ${'$'}v${'$'}, object distance ${'$'}u${'$'}, focal length ${'$'}f${'$'}, and optical power ${'$'}P${'$'} obey:

$$
\frac{1}{f} = \frac{1}{v} - \frac{1}{u}, \qquad m = +\frac{v}{u}, \qquad P = \frac{1}{f\text{ (in meters)}}
$$

### 2. Why It Works (Wave Speed Change)

Light slows down in optically denser media (${'$'}v = c/n${'$'}). Because frequency remains constant while wavelength shortens ($\lambda_2 = \lambda_1 n_1 / n_2$), wavefronts tilt toward the normal when entering a denser medium.

### 3. Crucial Nuances & Common Pitfalls

- **Measure angles from the normal:** Never measure ${'$'}i${'$'} or ${'$'}r${'$'} from the surface boundary itself.
- **Power in dioptres ($\text{D}$):** Convert ${'$'}f${'$'} from centimeters to meters before computing ${'$'}P = 1/f${'$'}.

### 4. Quick Worked Example

A convex lens has focal length ${'$'}f = +25\text{ cm} = +0.25\text{ m}${'$'}. Find its power ${'$'}P${'$'}.

$$
P = \frac{1}{+0.25\text{ m}} = +4.0\text{ D}
$$
""".trim(),

        "c10_gen_phy_ch12_elec" to """
**Ohm's Law** states that at constant temperature and physical conditions, the potential difference across a metallic conductor is directly proportional to the electric current flowing through it.

### 1. Mathematical Formulation

Potential difference ${'$'}V${'$'}, current ${'$'}I${'$'}, and resistance ${'$'}R${'$'} are related by:

$$
V = I R, \qquad R = \rho \frac{L}{A}
$$

**where:**
- ${'$'}V${'$'} is potential difference in volts ($\text{V}$).
- ${'$'}I${'$'} is current in amperes ($\text{A}$).
- ${'$'}R${'$'} is resistance in ohms ($\Omega$), $\rho$ is resistivity ($\Omega\cdot\text{m}$), ${'$'}L${'$'} is length, and ${'$'}A${'$'} is cross-sectional area.

For resistors connected in **series** versus **parallel**:

$$
R_{\text{series}} = R_1 + R_2, \qquad \frac{1}{R_{\text{parallel}}} = \frac{1}{R_1} + \frac{1}{R_2}
$$

### 2. Why It Works (Circuit Conservation)

In series, the same current ${'$'}I${'$'} passes through every resistor while voltages add (${'$'}V = V_1 + V_2${'$'}). In parallel, voltage ${'$'}V${'$'} is identical across each branch while currents split (${'$'}I = I_1 + I_2${'$'}).

### 3. Crucial Nuances & Common Pitfalls

- **Always take the reciprocal back in parallel circuits:** After finding $\frac{1}{R_{\text{eq}}} = \frac{1}{3}$, remember ${'$'}R_{\text{eq}} = 3\,\Omega${'$'}.
- **Parallel equivalent is always smaller** than the smallest individual branch resistor.

### 4. Quick Worked Example

Two $6\,\Omega$ resistors are connected in parallel across a $12\text{ V}$ battery. Find the total current ${'$'}I${'$'}.

First compute equivalent resistance:

$$
\frac{1}{R_{\text{eq}}} = \frac{1}{6} + \frac{1}{6} = \frac{1}{3} \implies R_{\text{eq}} = 3\,\Omega
$$

Apply Ohm's Law:

$$
I = \frac{V}{R_{\text{eq}}} = \frac{12}{3} = 4\text{ A}
$$
""".trim(),

        "c10_gen_phy_ch12_joule" to """
**Joule's Law of Heating** explains how electrical energy converts into thermal energy inside a conductor due to collisions between drifting electrons and lattice ions.

### 1. Mathematical Formulation

Heat ${'$'}H${'$'} generated in a resistor ${'$'}R${'$'} carrying steady current ${'$'}I${'$'} for time ${'$'}t${'$'} is:

$$
H = I^2 R t
$$

**where:**
- ${'$'}H${'$'} is heat dissipated ($\text{J}$).
- ${'$'}I${'$'} is steady current ($\text{A}$), ${'$'}R${'$'} is resistance ($\Omega$), and ${'$'}t${'$'} is time ($\text{s}$).

Electrical power ${'$'}P${'$'} (energy per unit time) has three equivalent forms:

$$
P = V I = I^2 R = \frac{V^2}{R}
$$

### 2. Why It Works (Derivation)

Moving charge ${'$'}Q = I t${'$'} across potential drop ${'$'}V = I R${'$'} expends work ${'$'}W = Q V = (I t)(I R) = I^2 R t${'$'}. Dividing by time ${'$'}t${'$'} directly yields electrical power ${'$'}P = V I${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Series vs. parallel power choice:** Use ${'$'}P = I^2 R${'$'} when comparing resistors in series (same ${'$'}I${'$'}), and use ${'$'}P = \frac{V^2}{R}${'$'} in parallel (same ${'$'}V${'$'}).
- **Convert minutes/hours to seconds** when computing ${'$'}H${'$'} in joules.

### 4. Quick Worked Example

Find the heat produced in a $5\,\Omega$ resistor carrying $2\text{ A}$ for $10\text{ s}$.

$$
H = I^2 R t = (2)^2 (5)(10) = 200\text{ J}
$$
""".trim()
    )
}
