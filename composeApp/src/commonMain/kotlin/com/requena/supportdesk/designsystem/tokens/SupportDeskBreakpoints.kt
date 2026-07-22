package com.requena.supportdesk.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SupportDeskBreakpoints {
    val adminCompact: Dp = 760.dp
    val adminMedium: Dp = 1180.dp
    val adminTicketsStacked: Dp = 960.dp
    val adminListDetailStacked: Dp = 1080.dp
    val adminSplitPane: Dp = 900.dp

    // Content-relative tiers: every client screen except the shell itself measures its
    // OWN remaining content width against these (see clientShellExpandedThreshold below
    // for why the shell needs a different, larger number for the same physical layout).
    val clientMedium: Dp = 640.dp
    val clientWide: Dp = 960.dp
    val clientUltraWide: Dp = 1400.dp

    // Names the sidebar's own fixed width (AppSidebar, NavigationComponents.kt) so the
    // shell threshold below can be derived from it instead of drifting independently.
    val clientSidebarWidth: Dp = 284.dp

    // The shell measures full WINDOW width to decide sidebar-vs-compact, but every other
    // client screen measures remaining CONTENT width (window minus the 284dp sidebar) for
    // its own layout decisions. Using clientWide directly here means the shell can show
    // the sidebar while content underneath still thinks it's compact. This threshold is
    // clientWide + clientSidebarWidth (960+284=1244), rounded up for density-rounding
    // safety at the boundary. Only ClientPortalScreen's own compact/sidebar switch should
    // use this - every other screen's inner layout branching uses clientMedium/clientWide/
    // clientUltraWide directly.
    val clientShellExpandedThreshold: Dp = 1280.dp

    val mobileCompact: Dp = 380.dp
    val mobileStackedTrailing: Dp = 360.dp
}
