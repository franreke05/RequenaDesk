package com.requena.supportdesk.features.tickets.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketPriorityUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketStatusUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.CreateTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketsUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ReplyTicketUseCase
import com.requena.supportdesk.features.tickets.presentation.effect.TicketsUiEffect
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TicketsViewModel(
    private val getTicketsUseCase: GetTicketsUseCase,
    private val getTicketUseCase: GetTicketUseCase,
    private val createTicketUseCase: CreateTicketUseCase,
    private val replyTicketUseCase: ReplyTicketUseCase,
    private val changeTicketStatusUseCase: ChangeTicketStatusUseCase,
    private val changeTicketPriorityUseCase: ChangeTicketPriorityUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TicketsUiState())
    val state: StateFlow<TicketsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TicketsUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<TicketsUiEffect> = _effects.asSharedFlow()

    init {
        onEvent(TicketsUiEvent.Load)
    }

    fun onEvent(event: TicketsUiEvent) {
        when (event) {
            TicketsUiEvent.Load -> loadTickets()
            is TicketsUiEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                loadTickets()
            }
            is TicketsUiEvent.StatusFilterChanged -> {
                _state.update { it.copy(statusFilter = event.status) }
                loadTickets()
            }
            is TicketsUiEvent.PriorityFilterChanged -> {
                _state.update { it.copy(priorityFilter = event.priority) }
                loadTickets()
            }
            is TicketsUiEvent.CategoryFilterChanged -> {
                _state.update { it.copy(categoryFilter = event.category) }
                loadTickets()
            }
            is TicketsUiEvent.PlatformFilterChanged -> {
                _state.update { it.copy(platformFilter = event.platform) }
                loadTickets()
            }
            is TicketsUiEvent.WaitingOnFilterChanged -> {
                _state.update { it.copy(waitingOnFilter = event.waitingOn) }
                loadTickets()
            }
            is TicketsUiEvent.SelectTicket -> selectTicket(event.ticketId)
            is TicketsUiEvent.CreateTicket -> createTicket(event.input)
            TicketsUiEvent.CreateSampleTicket -> createTicket()
            is TicketsUiEvent.ReplyToSelected -> replyToSelected(event.message)
            is TicketsUiEvent.ChangeSelectedStatus -> updateSelectedTicket { ticket ->
                changeTicketStatusUseCase(ticket.id, event.status)
            }
            is TicketsUiEvent.ChangeSelectedPriority -> updateSelectedTicket { ticket ->
                changeTicketPriorityUseCase(ticket.id, event.priority)
            }
        }
    }

    private fun loadTickets() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getTicketsUseCase(currentFilters())) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    val selected = state.value.selectedTicket?.id?.let { selectedId ->
                        result.data.firstOrNull { it.id == selectedId }
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            tickets = result.data,
                            selectedTicket = selected ?: result.data.firstOrNull(),
                        )
                    }
                }
            }
        }
    }

    private fun selectTicket(ticketId: String) {
        launch {
            when (val result = getTicketUseCase(ticketId)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    _state.update { it.copy(selectedTicket = result.data) }
                    _effects.emit(TicketsUiEffect.TicketSelected(ticketId))
                }
            }
        }
    }

    private fun createTicket(input: CreateTicketInput = CreateTicketInput()) {
        launch {
            when (val result = createTicketUseCase(input)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    _effects.emit(TicketsUiEffect.ShowMessage("Ticket placeholder creado"))
                    _state.update { it.copy(selectedTicket = result.data) }
                    loadTickets()
                }
            }
        }
    }

    private fun replyToSelected(message: String) {
        val ticketId = state.value.selectedTicket?.id ?: return
        launch {
            when (val result = replyTicketUseCase(ticketId, message)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    _effects.emit(TicketsUiEffect.ShowMessage("Respuesta anadida"))
                    selectTicket(result.data.ticketId)
                    loadTickets()
                }
            }
        }
    }

    private fun updateSelectedTicket(block: suspend (Ticket) -> AppResult<Ticket>) {
        val selected = state.value.selectedTicket ?: return
        launch {
            when (val result = block(selected)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    _state.update { it.copy(selectedTicket = result.data) }
                    loadTickets()
                }
            }
        }
    }

    private fun currentFilters(): TicketFilters = TicketFilters(
        query = state.value.searchQuery,
        status = state.value.statusFilter,
        priority = state.value.priorityFilter,
        category = state.value.categoryFilter,
        platform = state.value.platformFilter,
        waitingOn = state.value.waitingOnFilter,
    )
}
