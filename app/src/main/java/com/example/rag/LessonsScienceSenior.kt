package com.example.rag

/**
 * Verified textbook-grade structured lessons for Class 11 & Class 12 Science (PCM & PCB).
 * Every topic is isolated into a single focused concept with interleaved prose, display
 * formulas ($$...$$), variable breakdowns (**where:**), derivation, nuances, and example.
 */
internal object LessonsScienceSenior {

    val lessons: Map<String, String> = mapOf(
        "c11_pcm_math_ch13_calc" to """
The **Derivative** measures the instantaneous rate of change of a function ${'$'}f(x)${'$'} with respect to ${'$'}x${'$'}, geometrically equal to the slope of the tangent line at $(x, f(x))$.

- Differentiation turns polynomial and elementary functions into exact slope formulas.
- Using the **Power Rule** and **Product Rule** lets you differentiate complex expressions term by term.

### 1. Mathematical Formulation

By first principles, the derivative ${'$'}f'(x)${'$'} is the limiting slope of the secant line as step size ${'$'}h \to 0${'$'}:

$$
f'(x) = \frac{d}{dx}[f(x)] = \lim_{h \to 0} \frac{f(x + h) - f(x)}{h}
$$

**where:**
- ${'$'}f'(x)${'$'} or $\frac{df}{dx}$ is the instantaneous derivative at point ${'$'}x${'$'}.
- ${'$'}h${'$'} is an infinitesimal increment along the ${'$'}x${'$'}-axis.

For power functions and products of differentiable functions ${'$'}u(x)${'$'} and ${'$'}v(x)${'$'}:

$$
\frac{d}{dx}\left(x^n\right) = n x^{n-1}, \qquad \frac{d}{dx}(u \cdot v) = u'v + u v'
$$

Standard elementary derivatives include:

$$
\frac{d}{dx}(\sin x) = \cos x, \qquad \frac{d}{dx}(\cos x) = -\sin x, \qquad \frac{d}{dx}(e^x) = e^x
$$

### 2. Why It Works (Power Rule Intuition)

Expanding $(x + h)^n$ via the Binomial Theorem gives ${'$'}x^n + n x^{n-1} h + \binom{n}{2} x^{n-2} h^2 + \dots${'$'}. Subtracting ${'$'}x^n${'$'}, dividing by ${'$'}h${'$'}, and letting ${'$'}h \to 0${'$'} eliminates every term containing higher powers of ${'$'}h${'$'}, leaving solely ${'$'}n x^{n-1}${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Derivative of a constant is zero:** $\frac{d}{dx}(c) = 0$ because a constant has zero rate of change.
- **Product rule is not ${'$'}u' v'${'$'}:** Never multiply derivatives directly; always sum ${'$'}u'v + u v'${'$'}.
- **Radians only in calculus:** $\frac{d}{dx}(\sin x) = \cos x$ holds only when ${'$'}x${'$'} is measured in radians.

### 4. Quick Worked Example

Find the derivative of ${'$'}f(x) = 3x^4 - 5x^2 + 7${'$'} using the Power Rule.

Differentiate term-by-term using $\frac{d}{dx}(c x^n) = c n x^{n-1}$ and $\frac{d}{dx}(7) = 0$:

$$
f'(x) = 3(4x^3) - 5(2x^1) + 0 = 12x^3 - 10x
$$
""".trim(),

        "c11_pcm_math_ch13_limits" to """
A **Limit** describes the value that a function ${'$'}f(x)${'$'} approaches as its input ${'$'}x${'$'} gets arbitrarily close to a target point ${'$'}c${'$'}, even if ${'$'}f(c)${'$'} itself is undefined. A function is **continuous** at ${'$'}c${'$'} when its left-hand limit, right-hand limit, and function value all match.

### 1. Mathematical Formulation

Continuity at ${'$'}x = c${'$'} requires:

$$
\lim_{x \to c^-} f(x) = \lim_{x \to c^+} f(x) = f(c)
$$

**where:**
- $\lim_{x \to c^-} f(x)$ is the left-hand limit approaching ${'$'}c${'$'} from values $< c$.
- $\lim_{x \to c^+} f(x)$ is the right-hand limit approaching ${'$'}c${'$'} from values $> c$.

Two fundamental trigonometric and exponential standard limits are:

$$
\lim_{x \to 0} \frac{\sin x}{x} = 1, \qquad \lim_{x \to 0} \frac{e^x - 1}{x} = 1
$$

### 2. Why It Works (Indeterminate Forms)

Direct substitution in $\frac{\sin x}{x}$ at ${'$'}x = 0${'$'} yields the indeterminate form $\frac{0}{0}$. By the Squeeze Theorem on the unit circle ($\cos x < \frac{\sin x}{x} < 1$ near $0$), the ratio approaches $1$ smoothly from both sides.

### 3. Crucial Nuances & Common Pitfalls

- **Check both sides at piecewise jumps:** Always verify $\text{LHL} = \text{RHL}$ before concluding a limit exists.
- **$\frac{0}{0}$ is not $1$ or $0$:** Factor, rationalize, or apply standard limits to resolve $\frac{0}{0}$.

### 4. Quick Worked Example

Evaluate $\lim_{x \to 2} \frac{x^2 - 4}{x - 2}$.

Factor the numerator ${'$'}x^2 - 4 = (x - 2)(x + 2)${'$'} and cancel $(x - 2)$ since ${'$'}x \to 2${'$'} implies ${'$'}x \neq 2${'$'}:

$$
\lim_{x \to 2} \frac{(x - 2)(x + 2)}{x - 2} = \lim_{x \to 2} (x + 2) = 4
$$
""".trim(),

        "c11_pcm_phy_ch5_laws" to """
**Newton's Laws of Motion & Free Body Diagrams (FBDs)** provide the systematic framework to analyze how forces produce acceleration on rigid bodies and systems.

- Isolate the body of interest completely before drawing vectors.
- Resolve every force along perpendicular coordinate axes (for inclined planes: perpendicular normal axis and tangential slope axis).

### 1. Mathematical Formulation

By Newton's Second Law, net force equals mass times acceleration along each axis:

$$
\sum \vec{F}_{\text{ext}} = m \vec{a}
$$

**where:**
- $\sum \vec{F}_{\text{ext}}$ is the vector sum of all external forces acting *on* the body ($\text{N}$).
- ${'$'}m${'$'} is inertial mass ($\text{kg}$) and $\vec{a}$ is acceleration ($\text{m/s}^2$).

On a smooth plane inclined at angle $\theta$ to the horizontal, weight ${'$'}mg${'$'} decomposes into:

$$
mg_{\parallel} = m g \sin\theta, \qquad N = m g \cos\theta
$$

### 2. Why It Works (Axis Decomposition)

Because motion is constrained along the plane surface, acceleration perpendicular to the plane is zero (${'$'}a_{\perp} = 0${'$'}), fixing normal reaction ${'$'}N = mg\cos\theta${'$'}, while ${'$'}mg\sin\theta${'$'} drives tangential acceleration ${'$'}a = g\sin\theta${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Draw forces acting *on* the body only:** Never include forces the body exerts on other objects.
- **Normal force is not always ${'$'}mg${'$'}:** On an incline or under vertical acceleration, ${'$'}N${'$'} adjusts to satisfy $\sum F_{\perp} = m a_{\perp}$.

### 4. Quick Worked Example

A $2\text{ kg}$ block slides down a smooth frictionless $30^\circ$ incline (${'$'}g = 9.8\text{ m/s}^2${'$'}). Find its acceleration ${'$'}a${'$'} down the slope.

Since there is no friction, only ${'$'}mg\sin 30^\circ${'$'} acts down the slope:

$$
a = \frac{mg \sin 30^\circ}{m} = g \sin 30^\circ = 9.8 \times 0.5 = 4.9\text{ m/s}^2
$$
""".trim(),

        "c11_pcm_phy_ch5_friction" to """
**Friction** is the contact force tangent to two surfaces that opposes impending or actual relative sliding motion.

- **Static friction (${'$'}f_s${'$'})** is self-adjusting up to a limiting threshold before motion begins.
- **Kinetic friction (${'$'}f_k${'$'})** acts once surfaces slide past one another and is nearly constant.

### 1. Mathematical Formulation

Limiting static friction and kinetic friction are proportional to the normal contact force ${'$'}N${'$'}:

$$
f_s \le f_{s,\max} = \mu_s N, \qquad f_k = \mu_k N
$$

**where:**
- $\mu_s$ and $\mu_k$ are dimensionless coefficients of static and kinetic friction ($\mu_s > \mu_k$).
- ${'$'}N${'$'} is the normal reaction perpendicular to the contact surface ($\text{N}$).

On a rough incline of angle $\theta$, the block just begins to slide at the **angle of repose** $\theta_r$:

$$
\tan\theta_r = \mu_s
$$

### 2. Why It Works (Microscopic Interlocking)

Real surfaces have microscopic asperities that cold-weld and interlock under normal pressure ${'$'}N${'$'}. Breaking static junctions requires a higher tangential threshold ($\mu_s N$) than keeping surfaces sliding ($\mu_k N$).

### 3. Crucial Nuances & Common Pitfalls

- **Static friction is not always $\mu_s N$:** Below slipping, ${'$'}f_s${'$'} equals whatever external pushing force tries to start motion (${'$'}f_s = F_{\text{applied}}${'$'} up to $\mu_s N$).
- **Direction opposes *relative* motion:** Friction does not always oppose net motion; it opposes slipping between the two touching surfaces (for example, friction accelerates your shoes forward when walking).

### 4. Quick Worked Example

A $5\text{ kg}$ block rests on a horizontal floor ($\mu_s = 0.4$, ${'$'}g = 10\text{ m/s}^2${'$'}). What minimum horizontal force starts moving it?

First find normal reaction ${'$'}N = mg = 5 \times 10 = 50\text{ N}${'$'}, then compute limiting static friction:

$$
f_{s,\max} = \mu_s N = 0.4 \times 50 = 20\text{ N}
$$
""".trim(),

        "c11_pcm_phy_ch5_circular" to """
**Uniform Circular Motion** occurs when an object moves along a circular path of radius ${'$'}r${'$'} at constant speed ${'$'}v${'$'}. Although speed is constant, velocity direction continuously changes, requiring an inward **centripetal acceleration**.

### 1. Mathematical Formulation

Centripetal acceleration ${'$'}a_c${'$'} and centripetal force ${'$'}F_c${'$'} point radially toward the circle center:

$$
a_c = \frac{v^2}{r} = \omega^2 r, \qquad F_c = \frac{m v^2}{r}
$$

**where:**
- ${'$'}v${'$'} is tangential linear speed ($\text{m/s}$) and $\omega = \frac{v}{r}$ is angular speed ($\text{rad/s}$).
- ${'$'}r${'$'} is circle radius ($\text{m}$) and ${'$'}m${'$'} is object mass ($\text{kg}$).

On a frictionless banked curve of angle $\theta$, optimal safe speed is:

$$
v = \sqrt{r g \tan\theta}
$$

### 2. Why It Works (Direction Change)

Over a tiny angle $\Delta\theta$, velocity vector $\vec{v}$ rotates inward by $\Delta\theta$, giving $|\Delta\vec{v}| \approx v\,\Delta\theta$. Dividing by $\Delta t$ yields ${'$'}a_c = v \frac{d\theta}{dt} = v\omega = \frac{v^2}{r}${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Centripetal force is a net radial requirement, not an extra force:** Gravity, tension, friction, or normal reaction supplies ${'$'}F_c${'$'}. Never draw a separate "centripetal force" arrow alongside tension or friction.
- **Zero work by centripetal force:** Because $\vec{F}_c \perp \vec{v}$, centripetal force changes direction without changing speed.

### 4. Quick Worked Example

A $2\text{ kg}$ stone tied to a $1\text{ m}$ string whirls horizontally at $4\text{ m/s}$. Find the tension ${'$'}T${'$'} supplying centripetal force.

$$
T = F_c = \frac{m v^2}{r} = \frac{2 \times 4^2}{1} = 32\text{ N}
$$
""".trim(),

        "c11_pcm_phy_ch6_work_energy" to """
The **Work-Energy Theorem** is a fundamental bridge between Newton's laws (forces) and energy. It states that the **net work done by all forces acting on a particle equals the change in its kinetic energy**.

In simple terms:
- If net work is positive (${'$'}W_{\text{net}} > 0${'$'}), the object gains kinetic energy and speeds up.
- If net work is negative (${'$'}W_{\text{net}} < 0${'$'}), the object loses kinetic energy and slows down.
- If net work is zero (${'$'}W_{\text{net}} = 0${'$'}), its speed stays constant (even if direction changes).

### 1. Mathematical Formulation

Mathematically, the theorem is expressed as:

$$
W_{\text{net}} = \Delta K = K_f - K_i = \frac{1}{2}m v_f^2 - \frac{1}{2}m v_i^2
$$

**where:**
- ${'$'}W_{\text{net}}${'$'} is the total work done by the net force $\vec{F}_{\text{net}}$.
- ${'$'}K_i = \frac{1}{2}m v_i^2${'$'} is the initial kinetic energy.
- ${'$'}K_f = \frac{1}{2}m v_f^2${'$'} is the final kinetic energy.
- ${'$'}m${'$'} is particle mass, while ${'$'}v_i${'$'} and ${'$'}v_f${'$'} are initial and final speeds.

For a variable force acting along a curved path from position $\vec{r}_i$ to $\vec{r}_f$, net work is computed by the line integral:

$$
W_{\text{net}} = \int_{\vec{r}_i}^{\vec{r}_f} \vec{F}_{\text{net}} \cdot d\vec{r}
$$

### 2. Why It Works (Derivation)

Start from Newton's Second Law for a particle of constant mass ${'$'}m${'$'} and take the dot product with infinitesimal displacement ${'$'}d\vec{r} = \vec{v}\,dt${'$'}:

$$
\vec{F}_{\text{net}} \cdot d\vec{r} = m\frac{d\vec{v}}{dt} \cdot (\vec{v}\,dt) = m\vec{v} \cdot d\vec{v}
$$

Notice that $\frac{d}{dt}\left(\frac{1}{2}m\vec{v}\cdot\vec{v}\right) = m\vec{v}\cdot\frac{d\vec{v}}{dt}$. Integrating both sides from initial velocity ${'$'}v_i${'$'} to final velocity ${'$'}v_f${'$'} gives:

$$
W_{\text{net}} = \int_{v_i}^{v_f} m\vec{v} \cdot d\vec{v} = \left[\frac{1}{2} m v^2\right]_{v_i}^{v_f} = \frac{1}{2}m v_f^2 - \frac{1}{2}m v_i^2 = \Delta K
$$

### 3. Crucial Nuances & Common Pitfalls

- **Include all forces on the left-hand side:** ${'$'}W_{\text{net}}${'$'} is the algebraic sum of work done by *every* force (gravity, normal, tension, spring, kinetic friction)—conservative or non-conservative.
- **Forces perpendicular to motion do zero work:** A normal force on a track or centripetal force in circular motion contributes zero work because $\vec{F} \cdot d\vec{r} = 0$.
- **Scalar equation in inertial frames:** Work and $\Delta K$ depend on your chosen inertial frame, though ${'$'}W_{\text{net}} = \Delta K${'$'} holds identically in every inertial frame.
- **Extended or deformable bodies:** For rotating or deformable objects, translational $\Delta K_{\text{cm}}$ alone omits internal work and rotational kinetic energy $\frac{1}{2}I\omega^2$.

### 4. Quick Worked Example

A ${'$'}m = 2\text{ kg}${'$'} block starts from rest (${'$'}v_i = 0${'$'}) and is pulled across a rough horizontal floor by a constant horizontal force ${'$'}F_{\text{pull}} = 14\text{ N}${'$'} over ${'$'}d = 5\text{ m}${'$'} while kinetic friction exerts ${'$'}f_k = 4\text{ N}${'$'} opposite to displacement. Find its final speed ${'$'}v_f${'$'}.

Compute net work done by both horizontal forces (gravity and normal do zero work):

$$
W_{\text{net}} = (F_{\text{pull}} - f_k)\,d = (14 - 4) \times 5 = 50\text{ J}
$$

Equate net work to $\Delta K = \frac{1}{2}m v_f^2 - 0$:

$$
50 = \frac{1}{2}(2) v_f^2 \implies v_f^2 = 50 \implies v_f = \sqrt{50} \approx 7.07\text{ m/s}
$$
""".trim(),

        "c11_pcm_phy_ch6_impulse" to """
**Impulse** measures the total effect of a force acting over a time interval, directly equaling the **change in linear momentum** of the object. When no net external impulse acts on a system, total linear momentum is conserved.

### 1. Mathematical Formulation

Impulse $\vec{J}$ is the time integral of net force $\vec{F}$ and equals $\Delta \vec{p}$:

$$
\vec{J} = \int_{t_i}^{t_f} \vec{F}\,dt = \Delta \vec{p} = m\vec{v}_f - m\vec{v}_i
$$

**where:**
- $\vec{J}$ is impulse ($\text{N}\cdot\text{s}$ or $\text{kg}\cdot\text{m/s}$), equal to the area under the ${'$'}F${'$'}-${'$'}t${'$'} graph.
- $\vec{p} = m\vec{v}$ is linear momentum.

For an isolated colliding system ($\vec{F}_{\text{ext}} = 0$):

$$
m_1\vec{u}_1 + m_2\vec{u}_2 = m_1\vec{v}_1 + m_2\vec{v}_2
$$

### 2. Why It Works (Derivation)

Integrating Newton's Second Law $\vec{F} = \frac{d\vec{p}}{dt}$ from ${'$'}t_i${'$'} to ${'$'}t_f${'$'} directly yields $\int_{t_i}^{t_f}\vec{F}\,dt = \vec{p}_f - \vec{p}_i = \Delta\vec{p}$. Increasing collision time $\Delta t$ (like airbags or catching a ball with soft hands) lowers peak impact force ${'$'}F_{\text{avg}} = \Delta p / \Delta t${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Momentum is a vector:** Rebounding balls reverse velocity sign ($\Delta v = v_f - (-v_i) = v_f + v_i$), doubling momentum change.
- **Kinetic energy vs. momentum in collisions:** Total momentum is conserved in *all* isolated collisions, whereas kinetic energy is conserved only in *elastic* collisions.

### 4. Quick Worked Example

A $0.2\text{ kg}$ cricket ball moving at $+20\text{ m/s}$ is hit straight back at $-25\text{ m/s}$ in $0.01\text{ s}$. Find impulse and average force.

Compute change in momentum with signs:

$$
J = m(v_f - v_i) = 0.2(-25 - 20) = -9.0\text{ N}\cdot\text{s}
$$

Average force magnitude:

$$
F_{\text{avg}} = \frac{|J|}{\Delta t} = \frac{9.0}{0.01} = 900\text{ N}
$$
""".trim(),

        "c11_pcm_phy_ch12_thermo" to """
The **First Law of Thermodynamics** expresses conservation of energy for thermodynamic systems: heat supplied to a system either raises its internal energy or enables the system to do external pressure-volume work.

### 1. Mathematical Formulation

Differential and finite forms of the First Law are:

$$
\delta Q = dU + dW = dU + P\,dV
$$

**where:**
- $\delta Q$ is heat absorbed *by* the system ($\text{J}$).
- ${'$'}dU = n C_V\,dT${'$'} is the change in internal energy ($\text{J}$), a **state function** depending only on temperature for an ideal gas.
- ${'$'}dW = P\,dV${'$'} is work done *by* the gas during expansion ($\text{J}$).

Special ideal-gas processes (${'$'}P V = n R T${'$'}, $\gamma = C_P / C_V$, ${'$'}C_P - C_V = R${'$'}):

$$
\text{Isothermal }(T = \text{const}): \; dU = 0 \implies Q = W = n R T \ln\left(\frac{V_2}{V_1}\right)
$$

$$
\text{Adiabatic }(\delta Q = 0): \; P V^\gamma = \text{const}, \qquad dU = -dW
$$

### 2. Why It Works (State vs. Path Functions)

Internal energy ${'$'}U${'$'} depends only on the equilibrium state $(P, V, T)$, whereas heat ${'$'}Q${'$'} and work ${'$'}W${'$'} depend on the exact thermodynamic path taken on a ${'$'}P${'$'}-${'$'}V${'$'} diagram (area under the ${'$'}P${'$'}-${'$'}V${'$'} curve gives ${'$'}W${'$'}).

### 3. Crucial Nuances & Common Pitfalls

- **Sign convention consistency:** In physics (IUPAC vs. engineering check): $\delta Q > 0$ means heat *into* system; ${'$'}dW = P\,dV > 0${'$'} means expansion work *by* the gas.
- **Internal energy of an ideal gas depends only on ${'$'}T${'$'}:** Even if pressure and volume change, ${'$'}dU = 0${'$'} in any isothermal process for an ideal gas.

### 4. Quick Worked Example

A gas absorbs $200\text{ J}$ of heat while expanding and doing $120\text{ J}$ of external work. Find its change in internal energy $\Delta U$.

$$
\Delta U = Q - W = 200 - 120 = +80\text{ J}
$$
""".trim(),

        "c11_pcm_chem_ch2_atomic" to """
**Quantum Numbers & the Bohr Model** describe quantized electronic energy levels in atoms and uniquely specify the size, shape, orientation, and spin of every atomic orbital.

### 1. Mathematical Formulation

In the Bohr hydrogen-like atom (nuclear charge ${'$'}Z${'$'}), the ${'$'}n${'$'}-th orbit energy and de Broglie wavelength are:

$$
E_n = -13.6\,\frac{Z^2}{n^2}\text{ eV}, \qquad \lambda = \frac{h}{m v}
$$

**where:**
- ${'$'}n = 1, 2, 3, \dots${'$'} is the **principal quantum number** (shell & energy).
- ${'$'}l \in \{0, 1, \dots, n-1\}${'$'} is the **azimuthal quantum number** (subshell shape: ${'$'}s=0, p=1, d=2, f=3${'$'}).
- ${'$'}m_l \in \{-l, \dots, +l\}${'$'} is the **magnetic quantum number** (orbital orientation).
- ${'$'}m_s = \pm\frac{1}{2}${'$'} is the **spin quantum number**.

Heisenberg's Uncertainty Principle limits simultaneous position-momentum precision:

$$
\Delta x \cdot \Delta p \ge \frac{h}{4\pi}
$$

### 2. Why It Works (Pauli Exclusion Principle)

No two electrons in the same atom can share an identical set of all four quantum numbers $(n, l, m_l, m_s)$, which limits each spatial orbital to at most $2$ electrons of opposite spin and explains the periodic table's shell structure.

### 3. Crucial Nuances & Common Pitfalls

- **${'$'}l${'$'} never equals ${'$'}n${'$'}:** For ${'$'}n = 3${'$'}, maximum ${'$'}l${'$'} is $2$ ($3d$ subshell); a $3f$ orbital (${'$'}n=3, l=3${'$'}) cannot exist.
- **Subshell capacity:** Each subshell ${'$'}l${'$'} has $2l + 1$ orbitals and holds at most $2(2l + 1)$ electrons.

### 4. Quick Worked Example

List the allowed ${'$'}m_l${'$'} values and maximum electrons for a $3d$ subshell.

For $3d$, ${'$'}n = 3${'$'} and ${'$'}l = 2${'$'}, so ${'$'}m_l${'$'} takes $2(2) + 1 = 5$ orientations:

$$
m_l \in \{-2, -1, 0, +1, +2\} \implies \text{Max electrons} = 2 \times 5 = 10
$$
""".trim(),

        "c11_pcm_chem_ch8_redox" to """
**Redox Reactions** involve simultaneous electron transfer between reacting species: **oxidation** is loss of electrons (increase in oxidation state), while **reduction** is gain of electrons (decrease in oxidation state).

### 1. Mathematical Formulation

A complete redox process combines balanced oxidation and reduction half-reactions:

$$
\text{Oxidation (OIL): } M \rightarrow M^{n+} + n e^-, \qquad \text{Reduction (RIG): } X + n e^- \rightarrow X^{n-}
$$

**where:**
- The **reducing agent** donates electrons and gets oxidized.
- The **oxidizing agent** accepts electrons and gets reduced.

### 2. Why It Works (Charge & Electron Conservation)

In any balanced chemical equation, total electrons lost in oxidation must strictly equal total electrons gained in reduction, preserving net ionic charge on both sides.

### 3. Crucial Nuances & Common Pitfalls

- **Agents work inversely:** The species that is *oxidized* acts as the *reducing agent*, and the species *reduced* acts as the *oxidizing agent*.
- **Check oxygen/peroxide exceptions:** Oxygen is $-2$ in most compounds, $-1$ in peroxides ($\text{H}_2\text{O}_2$), and $+2$ in $\text{OF}_2$.

### 4. Quick Worked Example

Identify the oxidized and reduced species in $\text{Zn} + \text{Cu}^{2+} \rightarrow \text{Zn}^{2+} + \text{Cu}$.

$\text{Zn}$ goes from $0$ to $+2$ (loses $2e^-$, oxidized / reducing agent), while $\text{Cu}^{2+}$ goes from $+2$ to $0$ (gains $2e^-$, reduced / oxidizing agent):

$$
\text{Zn} \rightarrow \text{Zn}^{2+} + 2e^-, \qquad \text{Cu}^{2+} + 2e^- \rightarrow \text{Cu}
$$
""".trim(),

        "c11_pcm_cs_ch1_python" to """
**Python Functions, Recursion & Time Complexity** help structure modular programs and analyze how runtime scales as input size ${'$'}n${'$'} grows.

- **Recursion** solves a problem by reducing it toward an explicit **base case**.
- **Big-O notation** measures worst-case asymptotic growth with input size ${'$'}n${'$'}.

### 1. Mathematical Formulation

Recurrence relation for Binary Search on a sorted list of size ${'$'}n${'$'}:

$$
T(n) = T\left(\frac{n}{2}\right) + O(1) \implies T(n) = O(\log_2 n)
$$

**where:**
- ${'$'}T(n)${'$'} is operation count on input size ${'$'}n${'$'}.
- ${'$'}O(n)${'$'} is linear scan, while ${'$'}O(n^2)${'$'} characterizes nested loops over pairs.

### 2. Why It Works (Call Stack & Divide-and-Conquer)

Each recursive call pushes a stack frame holding local variables and return address. Halving the search interval on each comparison takes at most $\lceil \log_2 n \rceil$ steps instead of ${'$'}n${'$'} steps.

### 3. Crucial Nuances & Common Pitfalls

- **Always define a reachable base case:** Missing or unreachable base cases trigger `RecursionError: maximum recursion depth exceeded`.
- **Binary search requires sorted input:** Applying binary search to an unsorted array yields incorrect results.

### 4. Quick Worked Example

Evaluate `fact(5)` for `def fact(n): return 1 if n <= 1 else n * fact(n - 1)`.

Unwinding the recursive frames back from base case `fact(1) = 1`:

$$
5! = 5 \times 4 \times 3 \times 2 \times 1 = 120
$$
""".trim(),

        "c11_pcb_bio_ch8_cell" to """
The **Cell Cycle** coordinates DNA replication and cell division, while the **Fluid Mosaic Model** (Singer & Nicolson) explains how phospholipid bilayers and mobile proteins govern membrane transport.

### 1. Mathematical Formulation

Comparing ploidy (${'$'}n${'$'}) and DNA content (${'$'}C${'$'}) across the cycle:

$$
\text{Mitosis (equational): } 2n \xrightarrow{S\text{ phase }(2C \to 4C)} 2n + 2n
$$

$$
\text{Meiosis (reductional): } 2n \xrightarrow{\text{Meiosis I + II}} n + n + n + n
$$

**where:**
- ${'$'}G_1 \to S \to G_2 \to M${'$'} represents Interphase followed by Mitotic division.
- In **S phase**, DNA content doubles ($2C \to 4C$) while chromosome number ($2n$) stays unchanged.

### 2. Why It Works (Genetic Continuity vs. Variation)

Mitosis preserves identical diploid chromosomes for growth and repair. Meiosis halves chromosome count to haploid gametes and introduces variation through crossing over during **Pachytene of Prophase I**.

### 3. Crucial Nuances & Common Pitfalls

- **Chromosome count does not double in S phase:** Only sister chromatids double ($2C \to 4C$); centromere count stays $2n$.
- **Integral vs. peripheral membrane proteins:** Integral proteins span the hydrophobic bilayer core; peripheral proteins lie on outer/inner surfaces.

### 4. Quick Worked Example

A diploid root cell has $2n = 16$ chromosomes and $2C$ DNA in ${'$'}G_1${'$'}. What are its chromosome and chromatid counts in ${'$'}G_2${'$'} after S phase?

Centromeres (chromosomes) stay $16$, while DNA replication doubles chromatids to $32$ ($4C$ DNA).
""".trim(),

        "c11_pcb_phy_ch4_projectile" to """
**Projectile Motion** is two-dimensional motion under uniform gravity alone, combining constant horizontal velocity with vertical constant acceleration $-g$.

### 1. Mathematical Formulation

For a projectile launched at speed ${'$'}u${'$'} and angle $\theta$ above horizontal:

$$
x(t) = (u\cos\theta)\,t, \qquad y(t) = (u\sin\theta)\,t - \frac{1}{2}g t^2
$$

**where:**
- ${'$'}u_x = u\cos\theta${'$'} stays constant throughout flight.
- ${'$'}g \approx 9.8\text{ m/s}^2${'$'} (or $10\text{ m/s}^2$) acts vertically downward.

Time of flight ${'$'}T${'$'}, maximum height ${'$'}H_{\max}${'$'}, and horizontal range ${'$'}R${'$'} are:

$$
T = \frac{2 u \sin\theta}{g}, \qquad H_{\max} = \frac{u^2 \sin^2\theta}{2g}, \qquad R = \frac{u^2 \sin(2\theta)}{g}
$$

### 2. Why It Works (Independence of Orthogonal Axes)

Horizontal and vertical components do not affect each other because gravity acts purely along $-y$. At the peak, ${'$'}v_y = 0${'$'} while horizontal speed ${'$'}v_x = u\cos\theta${'$'} remains non-zero.

### 3. Crucial Nuances & Common Pitfalls

- **Peak speed is not zero:** At maximum height, vertical velocity is $0$, but speed is ${'$'}u\cos\theta${'$'}.
- **Maximum range angle:** ${'$'}R${'$'} peaks at $\theta = 45^\circ$ (where $\sin 90^\circ = 1$) on level ground; complementary angles $\theta$ and $90^\circ - \theta$ give the same range ${'$'}R${'$'}.

### 4. Quick Worked Example

A ball is launched at ${'$'}u = 20\text{ m/s}${'$'} at $\theta = 45^\circ$ (${'$'}g = 10\text{ m/s}^2${'$'}). Find its horizontal range ${'$'}R${'$'}.

$$
R = \frac{u^2 \sin(2\theta)}{g} = \frac{20^2 \sin 90^\circ}{10} = \frac{400 \times 1}{10} = 40\text{ m}
$$
""".trim(),

        "c11_pcb_chem_ch4_bond" to """
**VSEPR Theory & Hybridization** predict 3D molecular geometry by minimizing electrostatic repulsion among bonding pairs and lone pairs around a central atom.

### 1. Mathematical Formulation

Steric number ${'$'}SN${'$'} determines orbital hybridization and ideal electron geometry:

$$
SN = (\text{sigma bonds around central atom}) + (\text{lone pairs})
$$

**where:**
- ${'$'}SN = 2 \implies sp${'$'} (linear, $180^\circ$).
- ${'$'}SN = 3 \implies sp^2${'$'} (trigonal planar, $120^\circ$).
- ${'$'}SN = 4 \implies sp^3${'$'} (tetrahedral, $109.5^\circ$).

### 2. Why It Works (Lone-Pair Repulsion Hierarchy)

Lone pairs occupy wider spatial orbitals closer to one nucleus than shared bond pairs, producing stronger repulsion in the order $\text{lp–lp} > \text{lp–bp} > \text{bp–bp}$ and compressing bond angles.

### 3. Crucial Nuances & Common Pitfalls

- **Only count sigma ($\sigma$) bonds in hybridization:** Pi ($\pi$) bonds form from unhybridized ${'$'}p${'$'} orbitals and do not change ${'$'}SN${'$'}.
- **Distinguish electron geometry vs. molecular shape:** $\text{NH}_3$ and $\text{H}_2\text{O}$ are both ${'$'}sp^3${'$'} tetrahedral electronically, yet their shapes are **trigonal pyramidal ($107^\circ$)** and **bent ($104.5^\circ$)**.

### 4. Quick Worked Example

Determine the hybridization and bond angle of methane ($\text{CH}_4$).

Carbon forms $4$ single $\sigma$ bonds with $0$ lone pairs (${'$'}SN = 4${'$'}):

$$
SN = 4 + 0 = 4 \implies sp^3\text{ hybridization, tetrahedral, } 109.5^\circ
$$
""".trim(),

        "c11_pcb_chem_ch7_eq" to """
**Chemical Equilibrium & Le Chatelier's Principle** describe reversible reactions where forward and reverse reaction rates equalize, keeping macroscopic concentrations constant.

### 1. Mathematical Formulation

For a reversible gas-phase reaction ${'$'}aA + bB \rightleftharpoons cC + dD${'$'}:

$$
K_c = \frac{[C]^c[D]^d}{[A]^a[B]^b}, \qquad K_p = K_c (RT)^{\Delta n_g}
$$

**where:**
- $\Delta n_g = (c + d) - (a + b)$ counts gaseous product moles minus gaseous reactant moles.
- $\text{pH} = -\log_{10}[\text{H}^+]$ measures acidity on the logarithmic scale.

### 2. Why It Works (Le Chatelier's Shift)

If a system at equilibrium experiences a stress (concentration, pressure, or temperature change), the reaction shifts in the direction that partially counteracts that stress. Only a **temperature** change alters the numerical value of ${'$'}K${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Exclude pure solids and liquids from ${'$'}K_c${'$'} and ${'$'}K_p${'$'}:** Their active concentrations remain constant.
- **Catalysts speed up both rates equally:** Catalysts help reach equilibrium faster without shifting equilibrium position or changing ${'$'}K${'$'}.

### 4. Quick Worked Example

Compute the $\text{pH}$ of a $0.001\text{ M}$ strong $\text{HCl}$ aqueous solution.

Since $\text{HCl}$ ionizes completely, $[\text{H}^+] = 10^{-3}\text{ M}$:

$$
\text{pH} = -\log_{10}\left(10^{-3}\right) = 3
$$
""".trim(),

        "c12_pcm_math_ch7_int" to """
**Integration** reverses differentiation to recover antiderivatives (indefinite integrals) and computes exact signed area under curves (definite integrals).

### 1. Mathematical Formulation

Power rule of integration (${'$'}n \neq -1${'$'}) and Fundamental Theorem of Calculus:

$$
\int x^n\,dx = \frac{x^{n+1}}{n + 1} + C, \qquad \int_a^b f'(x)\,dx = f(b) - f(a)
$$

**where:**
- ${'$'}C${'$'} is the arbitrary constant of integration for indefinite integrals.
- For products, apply **Integration by Parts** following ILATE priority:

$$
\int u\,dv = u v - \int v\,du
$$

For symmetric definite integrals, King's Property states:

$$
\int_a^b f(x)\,dx = \int_a^b f(a + b - x)\,dx
$$

### 2. Why It Works (${'$'}u${'$'}-Substitution)

When an integrand has the composite structure ${'$'}f(g(x))\,g'(x)${'$'}, setting ${'$'}u = g(x)${'$'} gives ${'$'}du = g'(x)\,dx${'$'}, collapsing the integral into $\int f(u)\,du$.

### 3. Crucial Nuances & Common Pitfalls

- **Never omit $+ C$** in indefinite integrals.
- **Update limits in definite substitution:** When changing variables from ${'$'}x${'$'} to ${'$'}u${'$'}, also transform the lower and upper limits ${'$'}a, b${'$'} to ${'$'}u(a), u(b)${'$'}.

### 4. Quick Worked Example

Evaluate $\int 2x \, e^{x^2}\,dx$.

Substitute ${'$'}u = x^2 \implies du = 2x\,dx${'$'}:

$$
\int 2x\,e^{x^2}\,dx = \int e^u\,du = e^u + C = e^{x^2} + C
$$
""".trim(),

        "c12_pcm_phy_ch3_kirchhoff" to """
**Kirchhoff's Circuit Laws** analyze multi-loop electrical networks using charge conservation at nodes (**Junction Rule**) and energy conservation around closed loops (**Loop Rule**), forming the exact basis of a **balanced Wheatstone Bridge**.

### 1. Mathematical Formulation

Kirchhoff's Current Law (KCL) and Voltage Law (KVL) state:

$$
\sum I_{\text{in}} = \sum I_{\text{out}}, \qquad \sum_{\text{closed loop}} \Delta V = 0
$$

**where:**
- ${'$'}I${'$'} is branch current ($\text{A}$), governed microscopically by drift velocity ${'$'}v_d = \frac{I}{n e A}${'$'}.
- In a four-resistor Wheatstone bridge (${'$'}P, Q, R, S${'$'}), zero galvanometer current (null deflection) occurs when:

$$
\frac{P}{Q} = \frac{R}{S}
$$

### 2. Why It Works (Conservation Foundations)

Because charge cannot accumulate at an ideal wire junction, total entering current equals total leaving current (KCL). Because electrostatic fields are conservative, returning to the starting node around any closed loop produces zero net potential change (KVL).

### 3. Crucial Nuances & Common Pitfalls

- **Sign convention around loops:** Crossing a resistor *with* your assumed current arrow gives $-IR$; crossing from $-$ to $+$ terminal of a battery gives $+E$.
- **Swapping cell and galvanometer** does not disturb the balance ratio ${'$'}P/Q = R/S${'$'}.

### 4. Quick Worked Example

A Wheatstone bridge has ${'$'}P = 10\,\Omega${'$'}, ${'$'}Q = 20\,\Omega${'$'}, and ${'$'}R = 15\,\Omega${'$'}. Find ${'$'}S${'$'} for null deflection.

Apply the balance condition $\frac{P}{Q} = \frac{R}{S}$:

$$
\frac{10}{20} = \frac{15}{S} \implies S = 15 \times \frac{20}{10} = 30\,\Omega
$$
""".trim(),

        "c12_pcm_phy_ch6_emi" to """
**Faraday's Law of Electromagnetic Induction** states that changing magnetic flux through a coil induces an electromotive force (EMF), while **Lenz's Law** gives its polarity: induced current always opposes the flux change causing it.

### 1. Mathematical Formulation

Magnetic flux $\Phi_B$ through ${'$'}N${'$'} turns and induced EMF $\mathcal{E}$ are:

$$
\Phi_B = B A \cos\theta, \qquad \mathcal{E} = -N \frac{d\Phi_B}{dt}
$$

**where:**
- ${'$'}B${'$'} is magnetic field ($\text{T}$), ${'$'}A${'$'} is loop area ($\text{m}^2$), and $\theta$ is angle between $\vec{B}$ and the area normal.
- The minus sign mathematically encodes **Lenz's Law**.

### 2. Why It Works (Energy Conservation in Lenz's Law)

If induced current reinforced the flux change rather than opposing it, a tiny motion would self-amplify indefinitely, creating free energy. Work must be done against Lenz's opposing force to convert mechanical work into electrical energy.

### 3. Crucial Nuances & Common Pitfalls

- **Constant flux induces zero EMF:** Even a very strong magnetic field produces $\mathcal{E} = 0$ unless $\Phi_B$ changes with time (via ${'$'}B${'$'}, ${'$'}A${'$'}, or $\theta$).
- **Angle $\theta$ is with the normal:** If $\vec{B}$ is parallel to the coil plane, $\theta = 90^\circ$ and $\Phi_B = 0$.

### 4. Quick Worked Example

Magnetic flux through a $50$-turn coil drops uniformly from $0.8\text{ Wb}$ to $0.2\text{ Wb}$ in $0.1\text{ s}$. Find the magnitude of induced EMF $|\mathcal{E}|$.

$$
|\mathcal{E}| = N \left|\frac{\Delta\Phi_B}{\Delta t}\right| = 50 \times \frac{0.8 - 0.2}{0.1} = 50 \times 6 = 300\text{ V}
$$
""".trim(),

        "c12_pcm_phy_ch7_ac" to """
In a **Series LCR Alternating Current (AC) Circuit**, resistance ${'$'}R${'$'}, inductive reactance ${'$'}X_L${'$'}, and capacitive reactance ${'$'}X_C${'$'} combine vectorially to determine total **impedance ${'$'}Z${'$'}** and **resonance frequency ${'$'}f_{\text{res}}${'$'}**.

### 1. Mathematical Formulation

Total impedance ${'$'}Z${'$'} and reactances at AC frequency ${'$'}f${'$'} ($\omega = 2\pi f$) are:

$$
Z = \sqrt{R^2 + (X_L - X_C)^2}, \qquad X_L = \omega L, \qquad X_C = \frac{1}{\omega C}
$$

**where:**
- ${'$'}R${'$'} is resistance ($\Omega$), ${'$'}L${'$'} is inductance ($\text{H}$), and ${'$'}C${'$'} is capacitance ($\text{F}$).

Electrical **resonance** occurs when ${'$'}X_L = X_C${'$'}, minimizing impedance (${'$'}Z_{\min} = R${'$'}) at:

$$
f_{\text{res}} = \frac{1}{2\pi \sqrt{L C}}
$$

### 2. Why It Works (Phasor Phase Shift)

Inductor voltage leads current by $90^\circ$ while capacitor voltage lags by $90^\circ$, placing ${'$'}X_L${'$'} and ${'$'}X_C${'$'} $180^\circ$ opposite each other on a phasor diagram. At resonance they cancel completely, leaving purely resistive current ${'$'}I_{\max} = V / R${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **Do not add ${'$'}R + X_L + X_C${'$'} algebraically:** Combine them via the orthogonal Pythagorean phasor formula.
- **At resonance, power factor is unity:** $\cos\phi = R / Z = 1$.

### 4. Quick Worked Example

Find impedance ${'$'}Z${'$'} of a series LCR circuit with ${'$'}R = 30\,\Omega${'$'}, ${'$'}X_L = 70\,\Omega${'$'}, and ${'$'}X_C = 30\,\Omega${'$'}.

$$
Z = \sqrt{30^2 + (70 - 30)^2} = \sqrt{900 + 1600} = 50\,\Omega
$$
""".trim(),

        "c12_pcm_chem_ch3_electro" to """
The **Nernst Equation** calculates electrochemical cell potential under non-standard ion concentrations and links cell voltage directly to **Gibbs Free Energy** $\Delta G^\circ$.

### 1. Mathematical Formulation

At $298\text{ K}$ ($25^\circ\text{C}$), the Nernst Equation and standard free-energy relation are:

$$
E_{\text{cell}} = E^\circ_{\text{cell}} - \frac{0.0591}{n}\log_{10} Q, \qquad \Delta G^\circ = -n F E^\circ_{\text{cell}}
$$

**where:**
- ${'$'}E^\circ_{\text{cell}} = E^\circ_{\text{cathode}} - E^\circ_{\text{anode}}${'$'} is standard cell potential ($\text{V}$).
- ${'$'}n${'$'} is moles of electrons transferred per balanced reaction.
- ${'$'}Q = \frac{[\text{Anode products}]}{[\text{Cathode reactants}]}${'$'} is the reaction quotient, and ${'$'}F = 96485\text{ C/mol}${'$'}.

### 2. Why It Works (Thermodynamic Equilibrium)

As a galvanic cell discharges, ${'$'}Q${'$'} rises until ${'$'}E_{\text{cell}} = 0${'$'} at chemical equilibrium (${'$'}Q = K_{\text{eq}}${'$'}), yielding $\log_{10} K_{\text{eq}} = \frac{n E^\circ_{\text{cell}}}{0.0591}$.

### 3. Crucial Nuances & Common Pitfalls

- **Pure solids/liquids equal $1$ in ${'$'}Q${'$'}:** Omit pure solid electrodes ($\text{Zn}(s), \text{Cu}(s)$) from the reaction quotient.
- **Spontaneity sign:** Spontaneous galvanic cells have ${'$'}E_{\text{cell}} > 0${'$'} and $\Delta G < 0$.

### 4. Quick Worked Example

At $298\text{ K}$, if a Daniell cell (${'$'}E^\circ = 1.10\text{ V}, n = 2${'$'}) has $[\text{Zn}^{2+}] = 1\text{ M}$ and $[\text{Cu}^{2+}] = 0.01\text{ M}$, find ${'$'}E_{\text{cell}}${'$'}.

Reaction quotient ${'$'}Q = \frac{[\text{Zn}^{2+}]}{[\text{Cu}^{2+}]} = \frac{1}{0.01} = 10^2${'$'}:

$$
E_{\text{cell}} = 1.10 - \frac{0.0591}{2}\log_{10}(100) = 1.10 - 0.0591 = 1.0409\text{ V}
$$
""".trim(),

        "c12_pcm_chem_ch4_kinetics" to """
**First-Order Chemical Kinetics** describes reactions whose rate depends linearly on one reactant concentration, resulting in a constant half-life independent of initial concentration.

### 1. Mathematical Formulation

Integrated rate law and half-life ${'$'}t_{1/2}${'$'} for a first-order reaction ${'$'}R \to P${'$'} are:

$$
k = \frac{2.303}{t}\log_{10}\left(\frac{[R]_0}{[R]}\right), \qquad t_{1/2} = \frac{0.693}{k}
$$

**where:**
- ${'$'}k${'$'} is the first-order rate constant ($\text{s}^{-1}$ or $\text{min}^{-1}$).
- $[R]_0$ is initial reactant concentration and $[R]$ is concentration remaining at time ${'$'}t${'$'}.

### 2. Why It Works (Exponential Decay Derivation)

Integrating $-\frac{d[R]}{dt} = k[R]$ gives $\ln\frac{[R]_0}{[R]} = kt$. Setting $[R] = \frac{[R]_0}{2}$ yields ${'$'}t_{1/2} = \frac{\ln 2}{k} \approx \frac{0.693}{k}${'$'}, showing initial concentration $[R]_0$ cancels completely.

### 3. Crucial Nuances & Common Pitfalls

- **Only first-order reactions have constant ${'$'}t_{1/2}${'$'}:** Zero-order half-life (${'$'}t_{1/2} = \frac{[R]_0}{2k}${'$'}) depends directly on initial concentration.
- **Log base conversion:** Always use $2.303 \log_{10}$ (never omit $2.303$).

### 4. Quick Worked Example

A first-order reaction has rate constant ${'$'}k = 0.0693\text{ min}^{-1}${'$'}. Calculate its half-life ${'$'}t_{1/2}${'$'}.

$$
t_{1/2} = \frac{0.693}{k} = \frac{0.693}{0.0693} = 10\text{ minutes}
$$
""".trim(),

        "c12_pcb_bio_ch5_genetics" to """
**DNA Replication** duplicates genetic information semiconservatively in the $5' \to 3'$ direction, while the **Hardy-Weinberg Equilibrium** models stable allele and genotype frequencies in non-evolving populations.

### 1. Mathematical Formulation

Under Hardy-Weinberg equilibrium for two alleles with frequencies ${'$'}p${'$'} (dominant) and ${'$'}q${'$'} (recessive):

$$
p + q = 1, \qquad p^2 + 2pq + q^2 = 1
$$

**where:**
- ${'$'}p^2${'$'} is homozygous dominant frequency, $2pq$ is heterozygous carrier frequency, and ${'$'}q^2${'$'} is homozygous recessive frequency.
- Central Dogma flow: $\text{DNA} \xrightarrow{\text{Transcription}} \text{mRNA} \xrightarrow{\text{Translation}} \text{Protein}$.

### 2. Why It Works (Semiconservative Replication & Binomial Gene Pool)

DNA Polymerase III always adds nucleotides to a free $3'$-$\text{OH}$ group, making the leading strand continuous ($5'\to 3'$) and the lagging strand discontinuous via **Okazaki fragments**. Random gamete union expands $(p + q)^2 = p^2 + 2pq + q^2$.

### 3. Crucial Nuances & Common Pitfalls

- **Solve for ${'$'}q${'$'} from ${'$'}q^2${'$'} first:** Never take ${'$'}q${'$'} directly from dominant phenotype (${'$'}p^2 + 2pq${'$'}), because heterozygous individuals also display the dominant phenotype.
- **Conditions for equilibrium:** No mutation, migration, selection, genetic drift, or non-random mating.

### 4. Quick Worked Example

In a population at Hardy-Weinberg equilibrium, $9\%$ show a recessive trait (${'$'}q^2 = 0.09${'$'}). Find carrier frequency $2pq$.

First find ${'$'}q = \sqrt{0.09} = 0.3${'$'} and ${'$'}p = 1 - 0.3 = 0.7${'$'}:

$$
2pq = 2(0.7)(0.3) = 0.42 \quad (42\%)
$$
""".trim(),

        "c12_pcb_phy_ch11_photoelectric" to """
**Einstein's Photoelectric Equation** demonstrates the quantum photon nature of light: each incident photon transfers energy ${'$'}h\nu${'$'} in an all-or-nothing collision to knock an electron out of a metal surface.

### 1. Mathematical Formulation

Maximum kinetic energy ${'$'}K_{\max}${'$'} of emitted photoelectrons and stopping potential ${'$'}V_0${'$'} obey:

$$
K_{\max} = h\nu - \phi_0 = e V_0
$$

**where:**
- ${'$'}h${'$'} is Planck's constant ($6.626 \times 10^{-34}\text{ J}\cdot\text{s}$) and $\nu$ is incident frequency.
- $\phi_0 = h\nu_0$ is the metal's **work function** (minimum energy needed to eject an electron).

### 2. Why It Works (Threshold Frequency)

Unless a single photon carries energy ${'$'}h\nu \ge \phi_0${'$'} (i.e., $\nu \ge \nu_0$), no electrons escape regardless of how intense the light beam is. Increasing intensity above threshold increases photon count—raising photocurrent—without raising ${'$'}K_{\max}${'$'}.

### 3. Crucial Nuances & Common Pitfalls

- **${'$'}K_{\max}${'$'} depends on frequency, not intensity.**
- **Unit consistency:** When $\phi_0$ and photon energy ${'$'}E = h\nu${'$'} are in electron-volts ($\text{eV}$), numerical stopping potential ${'$'}V_0${'$'} in volts equals ${'$'}K_{\max}${'$'} in $\text{eV}$.

### 4. Quick Worked Example

Light of photon energy $5.0\text{ eV}$ strikes a metal of work function $\phi_0 = 2.2\text{ eV}$. Find ${'$'}K_{\max}${'$'} and stopping potential ${'$'}V_0${'$'}.

$$
K_{\max} = 5.0 - 2.2 = 2.8\text{ eV} \implies V_0 = 2.8\text{ V}
$$
""".trim(),

        "c12_pcb_phy_ch12_bohr" to """
The **Bohr Hydrogen Spectrum & Rydberg Formula** explain the discrete emission lines of hydrogen (Lyman, Balmer, Paschen series) as quantized electron transitions between energy shells.

### 1. Mathematical Formulation

When an electron jumps from a higher shell ${'$'}n_2${'$'} to a lower shell ${'$'}n_1${'$'} (${'$'}n_2 > n_1${'$'}), the emitted photon wavelength $\lambda$ satisfies:

$$
\frac{1}{\lambda} = R_H \left(\frac{1}{n_1^2} - \frac{1}{n_2^2}\right)
$$

**where:**
- ${'$'}R_H \approx 1.097 \times 10^7\text{ m}^{-1}${'$'} is the Rydberg constant for hydrogen.
- ${'$'}n_1 = 1${'$'} (Lyman UV), ${'$'}n_1 = 2${'$'} (Balmer visible), ${'$'}n_1 = 3${'$'} (Paschen IR).

### 2. Why It Works (Energy Conservation in Transitions)

Emitted photon energy equals the exact energy gap $\Delta E = E_{n_2} - E_{n_1} = \frac{h c}{\lambda}$. Because hydrogen's orbits are quantized (${'$'}E_n = -13.6 / n^2\text{ eV}${'$'}), only discrete sharp spectral lines appear.

### 3. Crucial Nuances & Common Pitfalls

- **Always take reciprocal at the end:** The formula computes $\frac{1}{\lambda}$; invert your value to obtain $\lambda$.
- **${'$'}n_1 < n_2${'$'} for emission:** Otherwise you would obtain a negative wavelength.

### 4. Quick Worked Example

Find the wavelength of the first Balmer line (${'$'}n_2 = 3 \to n_1 = 2${'$'}) in terms of ${'$'}R_H${'$'}.

$$
\frac{1}{\lambda} = R_H\left(\frac{1}{2^2} - \frac{1}{3^2}\right) = R_H\left(\frac{1}{4} - \frac{1}{9}\right) = \frac{5 R_H}{36} \implies \lambda = \frac{36}{5 R_H}
$$
""".trim()
    )
}
