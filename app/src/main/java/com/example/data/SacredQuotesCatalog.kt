package com.example.data

data class SacredQuote(
    val text: String,
    val source: String,
    val scripture: String
)

/**
 * Direct, faithful English translations of authentic verses from the Quran, Bible,
 * Bhagavad Gita, Ramayana, and Mahabharata displayed on the sign-in screen.
 */
object SacredQuotesCatalog {
    val quotes: List<SacredQuote> = listOf(
        // Bhagavad Gita
        SacredQuote(
            text = "To action alone you have a right, and never at all to its fruits; let not the fruits of action be your motive, neither let there be in you any attachment to inaction.",
            source = "Bhagavad Gita 2.47",
            scripture = "Bhagavad Gita"
        ),
        SacredQuote(
            text = "Indeed, there is nothing in this world as purifying as knowledge; one who has attained perfection in yoga realizes it within one's own self in due time.",
            source = "Bhagavad Gita 4.38",
            scripture = "Bhagavad Gita"
        ),
        SacredQuote(
            text = "The one who has faith, is devoted, and has mastered the senses attains knowledge; having attained knowledge, one swiftly reaches supreme peace.",
            source = "Bhagavad Gita 4.39",
            scripture = "Bhagavad Gita"
        ),
        SacredQuote(
            text = "Let a person uplift oneself by one's own self, and not degrade oneself; for the self alone is the friend of the self, and the self alone is the enemy of the self.",
            source = "Bhagavad Gita 6.5",
            scripture = "Bhagavad Gita"
        ),

        // Quran
        SacredQuote(
            text = "And say: My Lord, increase me in knowledge.",
            source = "Quran 20:114",
            scripture = "Quran"
        ),
        SacredQuote(
            text = "Say: Are those who know equal to those who do not know? Only those of understanding will take heed.",
            source = "Quran 39:9",
            scripture = "Quran"
        ),
        SacredQuote(
            text = "Read, and your Lord is the Most Generous, Who taught by the pen—taught humankind that which they knew not.",
            source = "Quran 96:3–5",
            scripture = "Quran"
        ),
        SacredQuote(
            text = "For indeed, with hardship comes ease. Indeed, with hardship comes ease.",
            source = "Quran 94:5–6",
            scripture = "Quran"
        ),

        // Bible
        SacredQuote(
            text = "The beginning of wisdom is this: Get wisdom; and though it cost all you have, get understanding.",
            source = "Bible — Proverbs 4:7",
            scripture = "Bible"
        ),
        SacredQuote(
            text = "Let the wise hear and increase in learning, and the one who understands obtain guidance.",
            source = "Bible — Proverbs 1:5",
            scripture = "Bible"
        ),
        SacredQuote(
            text = "Ask, and it will be given to you; seek, and you will find; knock, and it will be opened to you.",
            source = "Bible — Matthew 7:7",
            scripture = "Bible"
        ),
        SacredQuote(
            text = "If any of you lacks wisdom, let him ask God, who gives generously to all without reproach, and it will be given to him.",
            source = "Bible — James 1:5",
            scripture = "Bible"
        ),

        // Ramayana
        SacredQuote(
            text = "Truth is the supreme sovereign in this world, and righteousness is ever rooted in truth; there is nothing higher than truth.",
            source = "Ramayana — Ayodhya Kanda 2.109.10",
            scripture = "Ramayana"
        ),
        SacredQuote(
            text = "Freedom from despair is the root of prosperity; steady perseverance is the highest happiness and accomplishes every task.",
            source = "Ramayana — Sundara Kanda 5.12.10",
            scripture = "Ramayana"
        ),
        SacredQuote(
            text = "Enthusiasm possesses great strength; there is no power superior to enthusiasm. For one who is steadfast, nothing in the world is unattainable.",
            source = "Ramayana — Kishkindha Kanda 4.1.121",
            scripture = "Ramayana"
        ),
        SacredQuote(
            text = "Truth, compassion, self-discipline, and kind speech toward all beings form the eternal path of the noble.",
            source = "Ramayana — Ayodhya Kanda 2.109",
            scripture = "Ramayana"
        ),

        // Mahabharata
        SacredQuote(
            text = "One should never do to another that which one regards as injurious to one's own self. This, in brief, is the rule of righteousness.",
            source = "Mahabharata — Anushasana Parva 13.113.8",
            scripture = "Mahabharata"
        ),
        SacredQuote(
            text = "Of all gains, knowledge is the highest; of all strengths, forgiveness is the greatest; and truth is the highest virtue.",
            source = "Mahabharata — Udyoga Parva 5.33",
            scripture = "Mahabharata"
        ),
        SacredQuote(
            text = "Righteousness is the sole friend that accompanies a person beyond this life; all else perishes with the body.",
            source = "Mahabharata — Vana Parva 3.313",
            scripture = "Mahabharata"
        ),
        SacredQuote(
            text = "By truth is the sun upheld, in truth are all worlds established; therefore, speak truth and walk the path of learning.",
            source = "Mahabharata — Shanti Parva",
            scripture = "Mahabharata"
        )
    )
}
