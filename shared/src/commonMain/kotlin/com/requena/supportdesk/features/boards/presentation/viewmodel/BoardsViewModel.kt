package com.requena.supportdesk.features.boards.presentation.viewmodel

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.model.WorkTaskStatus
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

    private var currentOwnerAdminId: String? = null

    fun updateClients(clients: List<Client>, ownerAdminId: String) {
        val cleanOwnerId = ownerAdminId.trim()
        if (cleanOwnerId.isBlank()) {
            clear()
            return
        }

        val previousOwnerId = currentOwnerAdminId
        val baseState = if (previousOwnerId != null && previousOwnerId != cleanOwnerId) {
            BoardsUiState()
        } else {
            _state.value
        }
        currentOwnerAdminId = cleanOwnerId

        val scopedClients = clients
            .filter { it.ownerAdminId == cleanOwnerId }
            .distinctBy { it.id }
        val clientIds = scopedClients.map { it.id }.toSet()
        val existingBoardsByClientId = baseState.boards
            .filter { board -> board.ownerId == cleanOwnerId && board.clientId != null && board.clientId in clientIds }
            .associateBy { it.clientId }

        val boards = scopedClients.map { client ->
            existingBoardsByClientId[client.id]?.copy(
                name = client.companyName,
                description = client.productName,
                type = BoardType.CLIENT,
                ownerId = cleanOwnerId,
                clientId = client.id,
                isArchived = false,
                isPublic = false,
            ) ?: run {
                val boardId = boardIdFor(cleanOwnerId, client.id)
                Board(
                    id = boardId,
                    name = client.companyName,
                    description = client.productName,
                    type = BoardType.CLIENT,
                    ownerId = cleanOwnerId,
                    clientId = client.id,
                    isPublic = false,
                    columns = defaultColumns(boardId),
                )
            }
        }

        val selectedClientId = baseState.selectedBoard?.clientId
        val selected = boards.firstOrNull { it.clientId == selectedClientId } ?: boards.firstOrNull()
        val selectedCard = baseState.selectedCard?.takeIf { card ->
            selected?.cards?.any { it.id == card.id } == true
        }

        _state.value = baseState.copy(
            boards = boards,
            selectedBoard = selected,
            selectedCard = selectedCard,
        )
        syncCardsToBoards()
    }

    fun updateTickets(tickets: List<Ticket>) {
        if (currentOwnerAdminId == null) return
        _state.value = _state.value.copy(tickets = tickets)
        syncCardsToBoards()
    }

    fun updateTasks(tasks: List<WorkTask>) {
        if (currentOwnerAdminId == null) return
        _state.value = _state.value.copy(tasks = tasks)
        syncCardsToBoards()
    }

    fun onEvent(event: BoardsUiEvent) {
        when (event) {
            is BoardsUiEvent.Load -> Unit
            is BoardsUiEvent.SelectBoard -> selectBoard(event.boardId)
            is BoardsUiEvent.SelectCard -> selectCard(event.cardId)
            is BoardsUiEvent.MoveCard -> moveCard(event.cardId, event.toColumnId, event.toPosition)
            is BoardsUiEvent.AddTicketToBoard -> addTicketToBoard(event.boardId, event.ticketId, event.columnId)
            is BoardsUiEvent.HideCard -> hideCard(event.cardId)
        }
    }

    private fun syncCardsToBoards() {
        val tickets = _state.value.tickets
        val tasks = _state.value.tasks
        val updatedBoards = _state.value.boards.map { board ->
            val ticketCards = tickets
                .filter { it.clientId == board.clientId }
                .mapNotNull { ticket ->
                    val col = board.columns.firstOrNull { it.status == ticket.status.name }
                        ?: board.columns.lastOrNull()
                        ?: return@mapNotNull null
                    BoardCard(
                        id = "card-ticket-${ticket.id}",
                        boardId = board.id,
                        ticketId = ticket.id,
                        taskId = null,
                        columnId = col.id,
                        position = 0,
                    )
                }
            val taskCards = tasks
                .filter { it.clientId == board.clientId && it.status != WorkTaskStatus.ARCHIVED }
                .mapNotNull { task ->
                    val col = columnForTaskStatus(board.columns, task.status)
                        ?: return@mapNotNull null
                    BoardCard(
                        id = "card-task-${task.id}",
                        boardId = board.id,
                        ticketId = null,
                        taskId = task.id,
                        columnId = col.id,
                        position = 0,
                    )
                }
            board.copy(cards = ticketCards + taskCards)
        }
        val updatedSelected = _state.value.selectedBoard?.let { sel ->
            updatedBoards.firstOrNull { it.id == sel.id }
        }
        val updatedSelectedCard = _state.value.selectedCard?.let { selected ->
            updatedSelected?.cards?.firstOrNull { it.id == selected.id }
        }
        _state.value = _state.value.copy(
            boards = updatedBoards,
            selectedBoard = updatedSelected,
            selectedCard = updatedSelectedCard,
        )
    }

    private fun columnForTaskStatus(columns: List<BoardColumn>, status: WorkTaskStatus): BoardColumn? =
        when (status) {
            WorkTaskStatus.TODO -> columns.firstOrNull { it.status == "OPEN" }
            WorkTaskStatus.IN_PROGRESS -> columns.firstOrNull { it.status == "IN_PROGRESS" }
            WorkTaskStatus.WAITING_CLIENT, WorkTaskStatus.REVIEW -> columns.firstOrNull { it.status == "PENDING_CLIENT" }
            WorkTaskStatus.DONE -> columns.firstOrNull { it.status == "RESOLVED" }
            WorkTaskStatus.ARCHIVED -> null
        }

    private fun selectBoard(boardId: String?) {
        val board = if (boardId != null) _state.value.boards.find { it.id == boardId } else null
        _state.value = _state.value.copy(selectedBoard = board, selectedCard = null)
    }

    private fun selectCard(cardId: String?) {
        val card = if (cardId != null) _state.value.selectedBoard?.cards?.find { it.id == cardId } else null
        _state.value = _state.value.copy(selectedCard = card)
    }

    private fun moveCard(cardId: String, toColumnId: String, toPosition: Int) {
        val currentBoard = _state.value.selectedBoard ?: return
        val updatedCards = currentBoard.cards.map { card ->
            if (card.id == cardId) card.copy(columnId = toColumnId, position = toPosition) else card
        }
        val updatedBoard = currentBoard.copy(cards = updatedCards)
        val updatedSelected = updatedCards.firstOrNull { it.id == cardId } ?: _state.value.selectedCard
        _state.value = _state.value.copy(
            selectedBoard = updatedBoard,
            selectedCard = updatedSelected,
            boards = _state.value.boards.map { if (it.id == currentBoard.id) updatedBoard else it },
        )
    }

    private fun addTicketToBoard(boardId: String, ticketId: String, columnId: String) {
        val board = _state.value.boards.find { it.id == boardId } ?: return
        if (board.cards.any { it.ticketId == ticketId }) return
        val maxPosition = board.cards.filter { it.columnId == columnId }.maxOfOrNull { it.position } ?: 0
        val newCard = BoardCard(
            id = "card-ticket-$ticketId",
            boardId = boardId,
            ticketId = ticketId,
            columnId = columnId,
            position = maxPosition + 1,
        )
        val updatedBoard = board.copy(cards = board.cards + newCard)
        _state.value = _state.value.copy(
            boards = _state.value.boards.map { if (it.id == boardId) updatedBoard else it },
            selectedBoard = if (_state.value.selectedBoard?.id == boardId) updatedBoard else _state.value.selectedBoard,
        )
    }

    private fun hideCard(cardId: String) {
        val currentBoard = _state.value.selectedBoard ?: return
        val updatedCards = currentBoard.cards.map { card ->
            if (card.id == cardId) card.copy(isHidden = true) else card
        }
        val updatedBoard = currentBoard.copy(cards = updatedCards)
        _state.value = _state.value.copy(
            selectedBoard = updatedBoard,
            boards = _state.value.boards.map { if (it.id == currentBoard.id) updatedBoard else it },
        )
    }

    private fun defaultColumns(boardId: String): List<BoardColumn> = listOf(
        BoardColumn(UUID.randomUUID().toString(), boardId, "Abiertos", 1, "OPEN", "#E74C3C"),
        BoardColumn(UUID.randomUUID().toString(), boardId, "En Progreso", 2, "IN_PROGRESS", "#F39C12"),
        BoardColumn(UUID.randomUUID().toString(), boardId, "Pendiente Cliente", 3, "PENDING_CLIENT", "#3498DB"),
        BoardColumn(UUID.randomUUID().toString(), boardId, "Resuelto", 4, "RESOLVED", "#27AE60"),
    )

    private fun boardIdFor(ownerAdminId: String, clientId: String): String =
        "board-${ownerAdminId.toStableIdSegment()}-${clientId.toStableIdSegment()}"

    private fun String.toStableIdSegment(): String =
        trim()
            .map { char ->
                if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_'
            }
            .joinToString("")
            .ifBlank { "unknown" }

    fun clear() {
        currentOwnerAdminId = null
        _state.value = BoardsUiState()
    }
}
