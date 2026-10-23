package com.requena.supportdesk.core.appearance

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopAppearanceStoreJvmTest {
    private fun tempFile() = Files.createTempDirectory("apariencia").resolve("sub/apariencia.properties")

    @Test
    fun missingFileReturnsDefaults() = runBlocking {
        val store = DesktopAppearanceStore(tempFile())
        assertEquals(AppearancePreferences(), store.load())
    }

    @Test
    fun savedPreferencesRoundTrip() = runBlocking {
        val file = tempFile()
        val prefs = AppearancePreferences(SupportDeskStyle.GLASS, ThemeModePreference.DARK)
        DesktopAppearanceStore(file).save(prefs)
        assertEquals(prefs, DesktopAppearanceStore(file).load())
    }

    @Test
    fun unknownValuesFallBackToDefaults() = runBlocking {
        val file = tempFile()
        Files.createDirectories(file.parent)
        Files.writeString(file, "style=NEON\nmode=sepia\n")
        assertEquals(AppearancePreferences(), DesktopAppearanceStore(file).load())
    }

    @Test
    fun keysAreCaseInsensitive() {
        assertEquals(SupportDeskStyle.MINIMAL, SupportDeskStyle.fromKey(" minimal "))
        assertEquals(ThemeModePreference.LIGHT, ThemeModePreference.fromKey("light"))
    }
}
