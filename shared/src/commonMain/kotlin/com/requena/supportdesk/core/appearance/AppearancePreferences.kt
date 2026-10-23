package com.requena.supportdesk.core.appearance

/** Visual style each user can pick. ARTESANAL is the original warm "editorial paper" look. */
enum class SupportDeskStyle(val label: String, val description: String) {
    ARTESANAL("Artesanal", "Papel cálido, tinta y un acento bermellón."),
    MINIMAL("Minimalista", "Limpio y denso, pensado para muchos datos."),
    GLASS("Glass", "Capas translúcidas con profundidad y luz suave."),
    ;

    companion object {
        val Default = ARTESANAL

        fun fromKey(key: String?): SupportDeskStyle =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) } ?: Default
    }
}

enum class ThemeModePreference(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
    ;

    companion object {
        val Default = SYSTEM

        fun fromKey(key: String?): ThemeModePreference =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) } ?: Default
    }
}

data class AppearancePreferences(
    val style: SupportDeskStyle = SupportDeskStyle.Default,
    val mode: ThemeModePreference = ThemeModePreference.Default,
)

/** Remembers the user's appearance choice on this device. */
interface AppearanceStore {
    suspend fun load(): AppearancePreferences
    suspend fun save(preferences: AppearancePreferences)
}

/** Keeps the choice only while the app runs; used where there is no local persistence yet. */
class InMemoryAppearanceStore(
    private var current: AppearancePreferences = AppearancePreferences(),
) : AppearanceStore {
    override suspend fun load(): AppearancePreferences = current
    override suspend fun save(preferences: AppearancePreferences) {
        current = preferences
    }
}

expect fun createAppearanceStore(): AppearanceStore
