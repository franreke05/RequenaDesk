package com.requena.supportdesk.core.appearance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Properties

actual fun createAppearanceStore(): AppearanceStore = DesktopAppearanceStore()

/** Stores the choice in `~/.supportdesk/apariencia.properties`. Unknown or missing values fall back to defaults. */
internal class DesktopAppearanceStore(
    private val file: Path = Paths.get(System.getProperty("user.home"), ".supportdesk", "apariencia.properties"),
) : AppearanceStore {
    private val mutex = Mutex()

    override suspend fun load(): AppearancePreferences = withContext(Dispatchers.IO) {
        mutex.withLock {
            val properties = Properties()
            runCatching {
                if (Files.exists(file)) {
                    Files.newBufferedReader(file, Charsets.UTF_8).use(properties::load)
                }
            }
            AppearancePreferences(
                style = SupportDeskStyle.fromKey(properties.getProperty(KEY_STYLE)),
                mode = ThemeModePreference.fromKey(properties.getProperty(KEY_MODE)),
            )
        }
    }

    override suspend fun save(preferences: AppearancePreferences) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val properties = Properties().apply {
                setProperty(KEY_STYLE, preferences.style.name)
                setProperty(KEY_MODE, preferences.mode.name)
            }
            file.parent?.let(Files::createDirectories)
            val temp = Files.createTempFile(file.parent, "apariencia", ".tmp")
            Files.newBufferedWriter(temp, Charsets.UTF_8).use { properties.store(it, "RequenaDesk apariencia") }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            Unit
        }
    }

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_MODE = "mode"
    }
}
