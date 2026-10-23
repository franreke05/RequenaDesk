package com.requena.supportdesk.designsystem.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarSplitTest {
    @Test
    fun fiveOrFewerItemsAreAllShown() {
        assertEquals(listOf(1, 2, 3, 4, 5) to emptyList(), splitBottomBarItems(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun moreThanFiveKeepFourAndGroupTheRestUnderMore() {
        val (visible, overflow) = splitBottomBarItems((1..8).toList())
        assertEquals(listOf(1, 2, 3, 4), visible)
        assertEquals(listOf(5, 6, 7, 8), overflow)
    }
}
