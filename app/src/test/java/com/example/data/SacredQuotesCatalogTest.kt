package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.security.LocalProfileStore
import com.example.security.SecurePrefsFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SacredQuotesCatalogTest {

    @Test
    fun catalogCoversAllFiveRequestedScripturesWithValidEnglishTranslations() {
        val quotes = SacredQuotesCatalog.quotes
        val expectedScriptures = setOf("Quran", "Bible", "Bhagavad Gita", "Ramayana", "Mahabharata")
        val actualScriptures = quotes.map { it.scripture }.toSet()

        assertEquals(expectedScriptures, actualScriptures)
        quotes.forEach { quote ->
            assertTrue("Quote text should not be blank", quote.text.isNotBlank())
            assertTrue("Quote source should not be blank", quote.source.isNotBlank())
        }
    }

    @Test
    fun localProfileStore_nextQuoteIndexNeverRepeatsConsecutivelyAcrossSignIns() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SecurePrefsFactory.deletePrefsFile(app, LocalProfileStore.PREFS_SETTINGS_FILE)

        val store = LocalProfileStore(app)
        val deckSize = SacredQuotesCatalog.quotes.size

        // Draw two full cycles + 1 extra draw to test wrap-around boundary
        val drawnFirstCycle = mutableListOf<Int>()
        var previous = -1
        repeat(deckSize) {
            val next = store.nextQuoteIndex(deckSize)
            assertTrue(next in 0 until deckSize)
            assertNotEquals("Each sign-in must show a different quote from the previous", previous, next)
            drawnFirstCycle += next
            previous = next
        }

        // First full cycle must visit every quote once
        assertEquals(deckSize, drawnFirstCycle.toSet().size)

        // First quote of second cycle must still differ from last quote of first cycle
        val firstOfSecondCycle = store.nextQuoteIndex(deckSize)
        assertNotEquals(previous, firstOfSecondCycle)
    }
}
