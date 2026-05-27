package com.requena.supportdesk.features.tickets.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.InternalComment
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketEvent
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.TimeEntry
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.time.currentIsoDateTime
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters
import com.requena.supportdesk.features.tickets.domain.usecase.AcceptTicketCloseUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketPriorityUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketStatusUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.CreateTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.DeleteTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketsUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.RateTicketUseCase
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
import kotlinx.coroutines.launch

class TicketsViewModel(
    private val getTicketsUseCase: GetTicketsUseCase,
    private val getTicketUseCase: GetTicketUseCase,
    private val createTicketUseCase: CreateTicketUseCase,
    private val changeTicketStatusUseCase: ChangeTicketStatusUseCase,
    private val changeTicketPriorityUseCase: ChangeTicketPriorityUseCase,
    private val acceptTicketCloseUseCase: AcceptTicketCloseUseCase,
    private val rateTicketUseCase: RateTicketUseCase,
    private val deleteTicketUseCase: DeleteTicketUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TicketsUiState())
    val state: StateFlow<TicketsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TicketsUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<TicketsUiEffect> = _effects.asSharedFlow()

    private var sourceTickets = emptyList<Ticket>()
    private val ticketOverrides = mutableMapOf<String, Ticket>()

    init {
        onEvent(TicketsUiEvent.Load)
    }

    fun onEvent(event: TicketsUiEvent) {
        when (event) {
            TicketsUiEvent.Load -> loadTickets()
            is TicketsUiEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                renderTickets()
            }
            is TicketsUiEvent.StatusFilterChanged -> {
                _state.update { it.copy(statusFilter = event.status) }
                renderTickets()
            }
            is TicketsUiEvent.PriorityFilterChanged -> {
                _state.update { it.copy(priorityFilter = event.priority) }
                renderTickets()
            }
            is TicketsUiEvent.CategoryFilterChanged -> {
                _state.update { it.copy(categoryFilter = event.category) }
                renderTickets()
            }
            is TicketsUiEvent.PlatformFilterChanged -> {
                _state.update { it.copy(platformFilter = event.platform) }
                renderTickets()
            }
            is TicketsUiEvent.WaitingOnFilterChanged -> {
                _state.update { it.copy(waitingOnFilter = event.waitingOn) }
                renderTickets()
            }
            is TicketsUiEvent.SelectTicket -> selectTicket(event.ticketId)
            is TicketsUiEvent.CreateTicket -> createTicket(event.input)
            TicketsUiEvent.CreateSampleTicket -> createTicket()
            is TicketsUiEvent.ChangeSelectedStatus -> changeSelectedStatus(event.status)
            is TicketsUiEvent.ChangeTicketStatus -> changeTicketStatus(event.ticketId, event.status)
            is TicketsUiEvent.ChangeSelectedPriority -> changeSelectedPriority(event.priority)
            is TicketsUiEvent.AcceptSelectedClose -> acceptSelectedClose(event.resolutionSummary)
            is TicketsUiEvent.RateSelected -> rateSelected(event.rating)
            is TicketsUiEvent.AddInternalNote -> addInternalNote(event)
            is TicketsUiEvent.AddTimeEntry -> addTimeEntry(event)
            is TicketsUiEvent.DeleteTicket -> deleteTicket(event.ticketId)
        }
    }

    private fun loadTickets() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getTicketsUseCase(currentFilters())) {
                is AppResult.Error -> {
                    renderTickets(errorMessage = result.message)
                    _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    sourceTickets = result.data
                    renderTickets()
                }
            }
        }
    }

    private fun selectTicket(ticketId: String) {
        launch {
            when (val result = getTicketUseCase(ticketId)) {
                is AppResult.Error -> {
                    val fallback = mergedTickets().firstOrNull { it.id == ticketId }
                    if (fallback != null) {
                        _state.update { it.copy(selectedTicket = fallback) }
                        _effects.emit(TicketsUiEffect.TicketSelected(ticketId))
                    } else {
                        _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                    }
                }
                is AppResult.Success -> {
                    ticketOverrides[ticketId] = mergeWithLocal(result.data)
                    renderTickets(selectId = ticketId)
                    _effects.emit(TicketsUiEffect.TicketSelected(ticketId))
                }
            }
        }
    }

    private fun createTicket(input: CreateTicketInput = CreateTicketInput()) {
        launch {
            when (val result = createTicketUseCase(input)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    sourceTickets = listOf(result.data) + sourceTickets.filterNot { it.id == result.data.id }
                    renderTickets(selectId = result.data.id)
                    _effects.emit(TicketsUiEffect.ShowMessage("Ticket creado"))
                }
            }
        }
    }

    private fun changeSelectedStatus(status: TicketStatus) {
        val selected = state.value.selectedTicket ?: return
        launch {
            when (val result = changeTicketStatusUseCase(selected.id, status)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    ticketOverrides[selected.id] = mergeWithLocal(result.data)
                    renderTickets(selectId = selected.id)
                }
            }
        }
    }

    private fun changeTicketStatus(ticketId: String, status: TicketStatus) {
        launch {
            when (val result = changeTicketStatusUseCase(ticketId, status)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    ticketOverrides[ticketId] = mergeWithLocal(result.data)
                    renderTickets()
                }
            }
        }
    }

    private fun changeSelectedPriority(priority: TicketPriority) {
        val selected = state.value.selectedTicket ?: return
        launch {
            when (val result = changeTicketPriorityUseCase(selected.id, priority)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    ticketOverrides[selected.id] = mergeWithLocal(result.data)
                    renderTickets(selectId = selected.id)
                }
            }
        }
    }

    private fun acceptSelectedClose(resolutionSummary: String?) {
        val selected = state.value.selectedTicket ?: return
        launch {
            when (val result = acceptTicketCloseUseCase(selected.id, resolutionSummary)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    ticketOverrides[selected.id] = mergeWithLocal(result.data)
                    renderTickets(selectId = selected.id)
                }
            }
        }
    }

    private fun rateSelected(rating: Int) {
        val selected = state.value.selectedTicket ?: return
        launch {
            when (val result = rateTicketUseCase(selected.id, rating)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    ticketOverrides[selected.id] = mergeWithLocal(result.data)
                    renderTickets(selectId = selected.id)
                }
            }
        }
    }

    private fun addInternalNote(event: TicketsUiEvent.AddInternalNote) {
        val selected = state.value.selectedTicket ?: return
        val body = event.body.trim()
        if (body.isBlank()) return
        val note = InternalComment(
            id = "comment-local-${selected.internalComments.size + 1}",
            ticketId = selected.id,
            authorId = event.authorId,
            authorName = event.authorName,
            body = body,
            createdAt = currentIsoDateTime(),
        )
        ticketOverrides[selected.id] = selected.copy(
            internalComments = listOf(note) + selected.internalComments,
            updatedAt = currentIsoDateTime(),
        )
        renderTickets(selectId = selected.id)
    }

    private fun addTimeEntry(event: TicketsUiEvent.AddTimeEntry) {
        val selected = state.value.selectedTicket ?: return
        val noteBody = event.note.trim()
        if (event.minutes <= 0 || noteBody.isBlank()) return
        val entry = TimeEntry(
            id = "time-local-${selected.timeEntries.size + 1}",
            clientId = selected.clientId,
            ticketId = selected.id,
            authorId = event.authorId,
            authorName = event.authorName,
            minutes = event.minutes,
            workDate = currentIsoDate(),
            note = noteBody,
            billable = event.billable,
            createdAt = currentIsoDateTime(),
        )
        val eventLog = TicketEvent(
            id = "event-local-${selected.events.size + 1}",
            ticketId = selected.id,
            type = "TIME_LOGGED",
            description = "${event.authorName} registro ${event.minutes} min",
            actorName = event.authorName,
            createdAt = currentIsoDateTime(),
        )
        ticketOverrides[selected.id] = selected.copy(
            timeEntries = listOf(entry) + selected.timeEntries,
            events = listOf(eventLog) + selected.events,
            updatedAt = currentIsoDateTime(),
        )
        renderTickets(selectId = selected.id)
    }

    private fun deleteTicket(ticketId: String) {
        launch {
            when (val result = deleteTicketUseCase(ticketId)) {
                is AppResult.Error -> _effects.emit(TicketsUiEffect.ShowMessage(result.message))
                is AppResult.Success -> {
                    sourceTickets = sourceTickets.filterNot { it.id == ticketId }
                    ticketOverrides.remove(ticketId)
                    renderTickets(selectId = null)
                    _effects.emit(TicketsUiEffect.ShowMessage("Ticket eliminado"))
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

    private fun mergedTickets(): List<Ticket> = sourceTickets.map(::mergeWithLocal)

    private fun mergeWithLocal(ticket: Ticket): Ticket {
        val local = ticketOverrides[ticket.id]
        return local ?: ticket
    }

    private fun renderTickets(selectId: String? = state.value.selectedTicket?.id, errorMessage: String? = null) {
        val filtered = mergedTickets()
            .filter { it.matchesSearchAndFilters(state.value) }
            .sortedWith(compareBy<Ticket> { it.status == TicketStatus.CLOSED }.thenByDescending { it.updatedAt })
        val selected = selectId?.let { id -> filtered.firstOrNull { it.id == id } }
            ?: state.value.selectedTicket?.let { selected -> filtered.firstOrNull { it.id == selected.id } }
            ?: filtered.firstOrNull()
        _state.update {
            it.copy(
                tickets = filtered,
                allTickets = mergedTickets(),
                selectedTicket = selected,
                isLoading = false,
                errorMessage = errorMessage,
            )
        }
    }

    private fun Ticket.matchesSearchAndFilters(state: TicketsUiState): Boolean =
        (state.searchQuery.isBlank() || listOf(subject, ticketNumber, affectedApp, clientReference.orEmpty())
            .any { it.contains(state.searchQuery, ignoreCase = true) }) &&
            (state.statusFilter == null || status == state.statusFilter) &&
            (state.priorityFilter == null || priority == state.priorityFilter) &&
            (state.categoryFilter == null || category == state.categoryFilter) &&
            (state.platformFilter == null || platform == state.platformFilter) &&
            (state.waitingOnFilter == null || waitingOn == state.waitingOnFilter)
}
