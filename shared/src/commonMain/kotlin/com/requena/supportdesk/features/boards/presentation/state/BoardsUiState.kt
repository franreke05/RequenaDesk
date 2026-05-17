package com.requena.supportdesk.features.boards.presentation.state

import com.requena.supportdesk.features.boards.domain.model.Board
import com.requena.supportdesk.features.boards.domain.model.BoardCard
import com.requena.supportdesk.features.boards.domain.model.BoardColumn

data class BoardsUiState(
    val boards: List<Board> = emptyList(),
    val selectedBoard: Board? = null,
    val selectedCard: BoardCard? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
)
