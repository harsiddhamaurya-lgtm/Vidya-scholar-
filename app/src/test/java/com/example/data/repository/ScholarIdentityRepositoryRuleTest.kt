package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.AvatarMode
import com.example.data.AvatarSpec
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ScholarIdentityRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun onboarding_claimsVidyaIdAtomically_andSupportsPeerLookup() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = ScholarIdentityRepository(firestore)

        val initResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrInitializeUserProfile(aliceUid, "Aarav Scholar", "alice@test.com", "")
        }
        assertTrue(initResult.isSuccess)
        assertFalse(initResult.getOrThrow().onboardingCompleted)

        val handle = "aarav${(100..999).random()}"
        val code = (1000..9999).random().toString()
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repo.isVidyaIdAvailable(handle, code, aliceUid) })

        val avatar = AvatarSpec(
            mode = AvatarMode.BUILDER,
            presetId = "vedic_strategist",
            crestId = "chakra",
            frameId = "ashtapada",
            toneId = "cyan"
        )
        val claimResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.completeOnboardingAndClaimVidyaId(
                uid = aliceUid,
                displayName = "Aarav Scholar",
                email = "alice@test.com",
                photoUrl = "",
                handle = handle,
                discriminator = code,
                avatarSpec = avatar,
                focusRealm = "PHYSICS"
            )
        }
        assertTrue(claimResult.isSuccess)
        val claimed = claimResult.getOrThrow()
        assertTrue(claimed.onboardingCompleted)
        assertEquals("$handle#$code", claimed.vidyaId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { it?.vidyaId == "$handle#$code" }
        }
        assertNotNull(observed)

        // Second user can look up Alice by handle#code, but cannot hijack her Vidya ID or read her private user doc
        val bobUid = signInTestUser("bob_${UUID.randomUUID()}@test.com")
        val bobRepo = ScholarIdentityRepository(firestore)

        val lookup = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.lookupScholarByVidyaId("$handle#$code").getOrThrow()
        }
        assertNotNull(lookup)
        assertEquals(aliceUid, lookup?.uid)
        assertEquals("Aarav Scholar", lookup?.displayName)

        assertFalse(withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.isVidyaIdAvailable(handle, code, bobUid) })

        val hijackAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.completeOnboardingAndClaimVidyaId(
                uid = bobUid,
                displayName = "Bob Intruder",
                email = "bob@test.com",
                photoUrl = "",
                handle = handle,
                discriminator = code,
                avatarSpec = AvatarSpec(),
                focusRealm = "MATHEMATICS"
            )
        }
        assertTrue(hijackAttempt.isFailure)

        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            denied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Cross-user /users read must fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun observeUserProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ScholarIdentityRepository(firestore)
        var gotPermissionDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserProfile("some_user_id").first()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            gotPermissionDenied = fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Unauthenticated user profile read must fail with PERMISSION_DENIED", gotPermissionDenied)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
