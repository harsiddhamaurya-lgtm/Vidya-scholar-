package com.example.data

import com.example.data.repository.CurriculumRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CurriculumTest {

    @Test
    fun testClass10FoundationSubjects() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val subjects = repo.getSubjectsForStudent("class_10", "general", "live")
        assertEquals(5, subjects.size)
        assertTrue(subjects.any { it.realmId == RealmId.MATHEMATICS })
        assertTrue(subjects.any { it.realmId == RealmId.SCIENCE_FOUNDATION })
        assertTrue(subjects.any { it.realmId == RealmId.SOCIAL_SCIENCE })
        assertTrue(subjects.any { it.realmId == RealmId.CODING_AI })
        assertTrue(subjects.any { it.realmId == RealmId.ENGLISH })
    }

    @Test
    fun testClass11PcmSubjects() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val subjects = repo.getSubjectsForStudent("class_11", "science_pcm", "live")
        assertEquals(5, subjects.size)
        assertTrue(subjects.any { it.realmId == RealmId.MATHEMATICS })
        assertTrue(subjects.any { it.realmId == RealmId.PHYSICS })
        assertTrue(subjects.any { it.realmId == RealmId.CHEMISTRY })
        assertTrue(subjects.any { it.realmId == RealmId.CODING_AI })
        assertTrue(subjects.any { it.realmId == RealmId.ENGLISH })
    }

    @Test
    fun testClass11PcbSubjects() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val subjects = repo.getSubjectsForStudent("class_11", "science_pcb", "animated")
        assertEquals(4, subjects.size)
        assertTrue(subjects.any { it.realmId == RealmId.BIOLOGY })
        assertTrue(subjects.any { it.realmId == RealmId.PHYSICS })
        assertTrue(subjects.any { it.realmId == RealmId.CHEMISTRY })
        assertTrue(subjects.any { it.realmId == RealmId.ENGLISH })
    }

    @Test
    fun testClass11CommerceSubjects() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val subjects = repo.getSubjectsForStudent("class_11", "commerce", "recorded")
        assertEquals(5, subjects.size)
        assertTrue(subjects.any { it.realmId == RealmId.ACCOUNTANCY })
        assertTrue(subjects.any { it.realmId == RealmId.BUSINESS_STUDIES })
        assertTrue(subjects.any { it.realmId == RealmId.ECONOMICS })
        assertTrue(subjects.any { it.realmId == RealmId.MATHEMATICS })
        assertTrue(subjects.any { it.realmId == RealmId.ENGLISH })
    }

    @Test
    fun testNextStandardProgression() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val next = repo.getNextStandard("class_9")
        assertNotNull(next)
        assertEquals("class_10", next?.id)

        val next11 = repo.getNextStandard("class_11")
        assertNotNull(next11)
        assertEquals("class_12", next11?.id)
    }

    @Test
    fun testPlaybackProgressPersistence() {
        val repo = CurriculumRepository(RuntimeEnvironment.getApplication())
        val vidyaId = "VIDYA_TEST_123"
        val levelId = "math_l1"

        repo.savePlaybackProgress(vidyaId, levelId, 120, 180, false, null)
        val loaded = repo.getPlaybackProgress(vidyaId, levelId)
        assertNotNull(loaded)
        assertEquals(120, loaded?.lastPositionSec)
        assertEquals(180, loaded?.totalDurationSec)
        assertFalse(loaded?.completed == true)
    }
}
