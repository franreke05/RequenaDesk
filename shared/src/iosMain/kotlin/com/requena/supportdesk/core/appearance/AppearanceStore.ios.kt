package com.requena.supportdesk.core.appearance

// Mobile has no local persistence yet (same as AuthSessionStore); server sync arrives with workspaces.
actual fun createAppearanceStore(): AppearanceStore = InMemoryAppearanceStore()
