package com.requena.supportdesk.features.boards.presentation.event

import com.requena.supportdesk.features.boards.domain.model.BoardCard

sealed interface BoardsUiEvent {
    data object Load : BoardsUiEvent
    data class SelectBoard(val boardId: String?) : BoardsUiEvent
    data class SelectCard(val cardId: String?) : BoardsUiEvent
    data class MoveCard(val cardId: String, val toColumnId: String, val toPosition: Int) : BoardsUiEvent
    data class AddTicketToBoard(val boardId: String, val ticketId: String, val columnId: String) : BoardsUiEvent
    data class HideCard(val cardId: String) : BoardsUiEvent
}
