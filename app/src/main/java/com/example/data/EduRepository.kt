package com.example.data

import com.google.firebase.firestore.FirebaseFirestore

object EduRepository {
    val sampleCourses = listOf(
        Course(
            id = "math_101",
            title = "Advanced Calculus & Algebra",
            subject = "Mathematics",
            description = "Master limits, derivatives, integrals, and algebraic problem-solving with PW-style visual intuition.",
            iconName = "calculate",
            totalLessons = 6,
            completedCount = 2,
            colorHex = "#6366F1"
        ),
        Course(
            id = "physics_101",
            title = "Quantum Physics & Mechanics",
            subject = "Physics",
            description = "Understand motion, energy, electromagnetic waves, and quantum phenomena step by step.",
            iconName = "bolt",
            totalLessons = 5,
            completedCount = 1,
            colorHex = "#3B82F6"
        ),
        Course(
            id = "chem_101",
            title = "Organic Chemistry & Reactions",
            subject = "Chemistry",
            description = "Explore molecular structures, functional groups, and reaction mechanisms intuitively.",
            iconName = "science",
            totalLessons = 5,
            completedCount = 0,
            colorHex = "#10B981"
        ),
        Course(
            id = "ai_coding_101",
            title = "AI & Modern Python/Kotlin Dev",
            subject = "Coding & AI",
            description = "Build intelligent applications, understand machine learning algorithms, and code efficiently.",
            iconName = "code",
            totalLessons = 8,
            completedCount = 3,
            colorHex = "#F59E0B"
        ),
        Course(
            id = "english_101",
            title = "Advanced Communication & Writing",
            subject = "English",
            description = "Enhance vocabulary, persuasive writing, public speaking, and professional articulation.",
            iconName = "menu_book",
            totalLessons = 4,
            completedCount = 1,
            colorHex = "#EC4899"
        )
    )

    val sampleLessons = mapOf(
        "math_101" to listOf(
            Lesson(
                id = "m_1",
                courseId = "math_101",
                title = "Introduction to Derivatives",
                conceptNotes = "A derivative represents the instantaneous rate of change of a function with respect to one of its variables. Think of it as the speedometer of a car telling you your exact speed at any split second!\n\nKey Formula: d/dx (x^n) = n * x^(n-1)",
                question = "What is the derivative of x^3 with respect to x?",
                options = listOf("3x^2", "x^3", "3x", "x^2"),
                correctIndex = 0,
                explanation = "Using the power rule d/dx(x^n) = n*x^(n-1), bringing 3 to the front gives 3x^2."
            ),
            Lesson(
                id = "m_2",
                courseId = "math_101",
                title = "The Chain Rule",
                conceptNotes = "The chain rule is used to find the derivative of a composite function. If y = f(g(x)), then dy/dx = f'(g(x)) * g'(x).\n\nAnalogy: Like Russian nesting dolls, you differentiate the outer function while keeping the inner function intact, then multiply by the derivative of the inner function.",
                question = "What is the derivative of (2x + 1)^4?",
                options = listOf("4(2x+1)^3", "8(2x+1)^3", "2(2x+1)^3", "4(2x+1)^4"),
                correctIndex = 1,
                explanation = "Outer derivative: 4(2x+1)^3. Inner derivative of (2x+1) is 2. Multiplying them gives 8(2x+1)^3."
            )
        ),
        "physics_101" to listOf(
            Lesson(
                id = "p_1",
                courseId = "physics_101",
                title = "Newton's Laws of Motion",
                conceptNotes = "1. An object at rest stays at rest unless acted upon by a force.\n2. Force equals mass times acceleration (F = ma).\n3. Every action has an equal and opposite reaction.",
                question = "If a 10 kg mass accelerates at 2 m/s^2, what is the net force applied?",
                options = listOf("5 N", "10 N", "20 N", "200 N"),
                correctIndex = 2,
                explanation = "According to F = ma, Force = 10 kg * 2 m/s^2 = 20 Newtons."
            )
        ),
        "chem_101" to listOf(
            Lesson(
                id = "c_1",
                courseId = "chem_101",
                title = "Introduction to Covalent Bonds",
                conceptNotes = "Covalent bonding involves the sharing of electron pairs between atoms. These electron pairs are known as shared pairs or bonding pairs.",
                question = "How many covalent bonds are typically formed in a water molecule (H2O)?",
                options = listOf("1 single bond", "2 single bonds", "1 double bond", "4 single bonds"),
                correctIndex = 1,
                explanation = "Oxygen shares one electron with each of the two hydrogen atoms, forming 2 single covalent O-H bonds."
            )
        ),
        "ai_coding_101" to listOf(
            Lesson(
                id = "a_1",
                courseId = "ai_coding_101",
                title = "Introduction to LLMs & Prompting",
                conceptNotes = "Large Language Models (LLMs) are trained on vast corpora of text to predict subsequent tokens. Effective prompt engineering combines role assignment, context, clear constraints, and few-shot examples.",
                question = "Which technique helps guide an LLM to output structured JSON reliably?",
                options = listOf("Writing in all caps", "Specifying response schema and mime type", "Using random punctuation", "Asking politely"),
                correctIndex = 1,
                explanation = "Defining a strict JSON schema and responseMimeType guarantees valid structured output."
            )
        ),
        "english_101" to listOf(
            Lesson(
                id = "e_1",
                courseId = "english_101",
                title = "Mastering Persuasive Argumentation",
                conceptNotes = "Aristotle's rhetorical triangle relies on Ethos (credibility), Pathos (emotion), and Logos (logic) to craft compelling, persuasive speeches and essays.",
                question = "Which appeal relies primarily on facts, statistics, and logical reasoning?",
                options = listOf("Ethos", "Pathos", "Logos", "Mythos"),
                correctIndex = 2,
                explanation = "Logos refers to internal consistency of the message and the clarity of the claim, logic, and supporting evidence."
            )
        )
    )

    val sampleProjects = mutableListOf(
        CommunityProject(
            id = "p1",
            title = "Solar System 3D Interactive Model",
            description = "Built an interactive Kepler orbit simulator using Kotlin and Jetpack Compose canvas with orbital velocity calculations.",
            authorName = "Aarav Sharma",
            authorEmail = "aarav@example.com",
            category = "Physics & Code",
            upvotes = 142
        ),
        CommunityProject(
            id = "p2",
            title = "AI Study Notes Summarizer App",
            description = "Android app that parses PDF textbook chapters and uses Gemini AI to generate flashcards and bullet-point summaries.",
            authorName = "Priya Patel",
            authorEmail = "priya@example.com",
            category = "AI & ML",
            upvotes = 98
        ),
        CommunityProject(
            id = "p3",
            title = "Eco-Friendly Smart Campus Tracker",
            description = "Community project tracking carbon footprint reduction across university hostels with real-time leaderboards.",
            authorName = "Rohan Verma",
            authorEmail = "rohan@example.com",
            category = "Sustainability",
            upvotes = 76
        )
    )

    fun seedBots(db: FirebaseFirestore) {
        val bots = listOf(
            Contact("bot_1", "BOT9991", "AI Study Buddy"),
            Contact("bot_2", "BOT9992", "Math Wizard Bot"),
            Contact("bot_3", "BOT9993", "Physics Solver Bot")
        )
        bots.forEach { bot ->
            db.collection("users").document(bot.uid).set(
                mapOf(
                    "uid" to bot.uid,
                    "name" to bot.name,
                    "contactCode" to bot.contactCode,
                    "level" to "AI Bot"
                )
            )
        }
    }
}
