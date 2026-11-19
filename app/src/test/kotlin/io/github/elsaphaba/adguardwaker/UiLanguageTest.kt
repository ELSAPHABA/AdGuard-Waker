package io.github.elsaphaba.adguardwaker

import org.junit.Assert.assertEquals
import org.junit.Test

class UiLanguageTest {
    @Test fun automaticUsesKoreanOnlyForKoreanSystem() {
        assertEquals("ko", UiLanguage.resolve("auto", "ko"))
        assertEquals("en", UiLanguage.resolve("auto", "ja"))
        assertEquals("en", UiLanguage.resolve("auto", "fr"))
        assertEquals("en", UiLanguage.resolve(null, "en"))
        assertEquals("en", UiLanguage.resolve("auto", null))
    }

    @Test fun manualChoiceOverridesSystemAndSurvivesInvalidPreference() {
        assertEquals("en", UiLanguage.resolve("en", "ko"))
        assertEquals("ko", UiLanguage.resolve("ko", "ja"))
        assertEquals("auto", UiLanguage.normalize("invalid"))
        assertEquals("en", UiLanguage.resolve("invalid", "de"))
    }
}
