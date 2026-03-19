package com.requena.supportdesk.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Immutable
data class SupportDeskThemeController(
    val isDarkMode: Boolean,
    val toggleDarkMode: () -> Unit,
)

val LocalSupportDeskThemeController = compositionLocalOf<SupportDeskThemeController> {
    error("SupportDeskThemeController not provided")
}

@Composable
fun SupportDeskApp() {
    val systemDarkMode = isSystemInDarkTheme()
    var isDarkMode by rememberSaveable { mutableStateOf(systemDarkMode) }
    val controller = remember(isDarkMode) {
        SupportDeskThemeController(
            isDarkMode = isDarkMode,
            toggleDarkMode = { isDarkMode = !isDarkMode },
        )
    }

    CompositionLocalProvider(LocalSupportDeskThemeController provides controller) {
        SupportDeskTheme(useDarkTheme = isDarkMode) {
            SupportDeskPlatformApp()
        }
    }
}

@Composable
expect fun SupportDeskPlatformApp()
