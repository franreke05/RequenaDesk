package com.requena.supportdesk.features.boards.presentation.viewmodel

import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.features.boards.domain.model.Board
import com.requena.supportdesk.features.boards.domain.model.BoardCard
import com.requena.supportdesk.features.boards.domain.model.BoardColumn
import com.requena.supportdesk.features.boards.domain.model.BoardType
import com.requena.supportdesk.features.boards.presentation.event.BoardsUiEvent
import com.requena.supportdesk.features.boards.presentation.state.BoardsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BoardsViewModel {
    private val _state = MutableStateFlow(BoardsUiState())
    val state: StateFlow<BoardsUiState> = _state.asStateFlow()

    fun updateTickets(tickets: List<Ticket>) {
        _state.value = _state.value.copy(tickets = tickets)
    }

    fun onEvent(event: BoardsUiEvent) {
        when (event) {
            is BoardsUiEvent.Load -> loadBoards()
            is BoardsUiEvent.SelectBoard -> selectBoard(event.boardId)
            is BoardsUiEvent.SelectCard -> selectCard(event.cardId)
            is BoardsUiEvent.CreateBoard -> createBoard(event.name, event.description)
            is BoardsUiEvent.MoveCard -> moveCard(event.cardId, event.toColumnId, event.toPosition)
            is BoardsUiEvent.AddTicketToBoard -> addTicketToBoard(event.boardId, event.ticketId, event.columnId)
            is BoardsUiEvent.HideCard -> hideCard(event.cardId)
        }
    }

    private fun loadBoards() {
        try {
            _state.value = _state.value.copy(isLoading = true)
            // TODO: Cargar tableros desde API
            _state.value = _state.value.copy(isLoading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                statusMessage = "Error: ${e.message}"
            )
        }
    }

    private fun selectBoard(boardId: String?) {
        val board = if (boardId != null) {
            _state.value.boards.find { it.id == boardId }
        } else {
            null
        }
        _state.value = _state.value.copy(selectedBoard = board, selectedCard = null)
    }

    private fun selectCard(cardId: String?) {
        val card = if (cardId != null) {
            _state.value.selectedBoard?.cards?.find { it.id == cardId }
        } else {
            null
        }
        _state.value = _state.value.copy(selectedCard = card)
    }

    private fun createBoard(name: String, description: String) {
        try {
            _state.value = _state.value.copy(isLoading = true)

            val newBoard = Board(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                type = BoardType.GLOBAL,
                ownerId = "",
                columns = listOf(
                    BoardColumn(UUID.randomUUID().toString(), "", "Abiertos", 1, "OPEN", "#E74C3C"),
                    BoardColumn(UUID.randomUUID().toString(), "", "En Progreso", 2, "IN_PROGRESS", "#F39C12"),
                    BoardColumn(UUID.randomUUID().toString(), "", "Pendiente Cliente", 3, "PENDING_CLIENT", "#3498DB"),
                    BoardColumn(UUID.randomUUID().toString(), "", "Resuelto", 4, "RESOLVED", "#27AE60"),
                )
            )

            val updatedBoards = _state.value.boards + newBoard
            _state.value = _state.value.copy(
                boards = updatedBoards,
                selectedBoard = newBoard,
                isLoading = false,
                statusMessage = "Tablero creado: $name"
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                statusMessage = "Error: ${e.message}"
            )
        }
    }

    private fun moveCard(cardId: String, toColumnId: String, toPosition: Int) {
        val currentBoard = _state.value.selectedBoard ?: return
        val updatedCards = currentBoard.cards.map { card ->
            if (card.id == cardId) {
                card.copy(columnId = toColumnId, position = toPosition)
            } else {
                card
            }
        }

        val updatedBoard = currentBoard.copy(cards = updatedCards)
        _state.value = _state.value.copy(
            selectedBoard = updatedBoard,
            boards = _state.value.boards.map { if (it.id == currentBoard.id) updatedBoard else it }
        )
    }

    private fun addTicketToBoard(boardId: String, ticketId: String, columnId: String) {
        try {
            _state.value = _state.value.copy(isLoading = true)

            val board = _state.value.boards.find { it.id == boardId } ?: return
            val maxPosition = board.cards.filter { it.columnId == columnId }.maxOfOrNull { it.position } ?: 0

            val newCard = BoardCard(
                id = UUID.randomUUID().toString(),
                boardId = boardId,
                ticketId = ticketId,
                columnId = columnId,
                position = maxPosition + 1
            )

            val updatedBoard = board.copy(cards = board.cards + newCard)
            val updatedBoards = _state.value.boards.map { if (it.id == boardId) updatedBoard else it }

            _state.value = _state.value.copy(
                boards = updatedBoards,
                selectedBoard = if (board.id == _state.value.selectedBoard?.id) updatedBoard else _state.value.selectedBoard,
                isLoading = false,
                statusMessage = "Ticket agregado al tablero"
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                statusMessage = "Error: ${e.message}"
            )
        }
    }

    private fun hideCard(cardId: String) {
        val currentBoard = _state.value.selectedBoard ?: return
        val updatedCards = currentBoard.cards.map { card ->
            if (card.id == cardId) {
                card.copy(isHidden = true)
            } else {
                card
            }
        }

        val updatedBoard = currentBoard.copy(cards = updatedCards)
        _state.value = _state.value.copy(
            selectedBoard = updatedBoard,
            boards = _state.value.boards.map { if (it.id == currentBoard.id) updatedBoard else it }
        )
    }

    fun clear() {
        // Cleanup si es necesario
    }
}
