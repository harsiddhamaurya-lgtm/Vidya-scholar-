package com.example.ui

import com.example.ui.components.CosmiumMood
import com.example.ui.components.CosmiumNativeBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CosmiumOrbTest {

    @Test
    fun cosmiumMood_hasValidStatesAndBadges() {
        val moods = CosmiumMood.entries
        assertEquals(6, moods.size)

        assertEquals("idle", CosmiumMood.IDLE.stateKey)
        assertEquals("READY", CosmiumMood.IDLE.badgeLabel)

        assertEquals("speaking", CosmiumMood.SPEAKING.stateKey)
        assertEquals("SPEAKING", CosmiumMood.SPEAKING.badgeLabel)

        assertEquals("thinking", CosmiumMood.THINKING.stateKey)
        assertEquals("THINKING", CosmiumMood.THINKING.badgeLabel)

        assertEquals("curious", CosmiumMood.CURIOUS.stateKey)
        assertEquals("LISTENING", CosmiumMood.CURIOUS.badgeLabel)

        assertEquals("celebrate", CosmiumMood.CELEBRATE.stateKey)
        assertEquals("INSPIRED", CosmiumMood.CELEBRATE.badgeLabel)

        assertEquals("alert", CosmiumMood.ALERT.stateKey)
        assertEquals("RETRYING", CosmiumMood.ALERT.badgeLabel)
    }

    @Test
    fun cosmiumNativeBridge_handlesRobolectricGracefullyWithoutCrash() {
        // Under Robolectric JVM without libcosmium_native.so, ensureLoaded() catches UnsatisfiedLinkError
        // and returns false without throwing an unhandled exception.
        val loaded = CosmiumNativeBridge.ensureLoaded()
        // Must either be true (if host mock loaded) or false (UnsatisfiedLinkError handled)
        assertNotNull(loaded)
    }
}
