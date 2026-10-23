package com.requena.supportdesk.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.requena.supportdesk.core.appearance.AppearancePreferences
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.core.appearance.ThemeModePreference
import com.requena.supportdesk.core.appearance.createAppearanceStore
import kotlinx.coroutines.launch

@Immutable
data class SupportDeskThemeController(
    val style: SupportDeskStyle,
    val mode: ThemeModePreference,
    val isDarkMode: Boolean,
    val setStyle: (SupportDeskStyle) -> Unit,
    val setMode: (ThemeModePreference) -> Unit,
) {
    /** Flips between light and dark explicitly (leaves "follow system"). */
    fun toggleDarkMode() = setMode(if (isDarkMode) ThemeModePreference.LIGHT else ThemeModePreference.DARK)
}

val LocalSupportDeskThemeController = compositionLocalOf<SupportDeskThemeController> {
    error("SupportDeskThemeController not provided")
}

@Composable
fun SupportDeskApp() {
    SupportDeskApp {
        SupportDeskPlatformApp()
    }
}

@Composable
fun SupportDeskApp(
    content: @Composable () -> Unit,
) {
    val systemDarkMode = isSystemInDarkTheme()
    val store = remember { createAppearanceStore() }
    val scope = rememberCoroutineScope()
    var preferences by remember { mutableStateOf(AppearancePreferences()) }

    LaunchedEffect(store) {
        preferences = store.load()
    }

    fun update(next: AppearancePreferences) {
        if (next == preferences) return
        preferences = next
        scope.launch { runCatching { store.save(next) } }
    }

    val isDarkMode = when (preferences.mode) {
        ThemeModePreference.SYSTEM -> systemDarkMode
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
    }
    val controller = SupportDeskThemeController(
        style = preferences.style,
        mode = preferences.mode,
        isDarkMode = isDarkMode,
        setStyle = { update(preferences.copy(style = it)) },
        setMode = { update(preferences.copy(mode = it)) },
    )

    CompositionLocalProvider(LocalSupportDeskThemeController provides controller) {
        SupportDeskTheme(useDarkTheme = isDarkMode, style = preferences.style) {
            content()
        }
    }
}

@Composable
expect fun SupportDeskPlatformApp()
