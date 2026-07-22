package com.requena.supportdesk.features.business.finance.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.finance.data.repository.BusinessFinanceAccessDeniedException
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceRepository
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

class BusinessAccountingViewModel(
    private val repository: BusinessFinanceRepository,
    private val idempotencyKeyFactory: BusinessFinanceIdempotencyKeyFactory = RandomBusinessFinanceIdempotencyKeyFactory,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessAccountingUiState())
    val state: StateFlow<BusinessAccountingUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessFinanceUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<BusinessFinanceUiEffect> = _effects.asSharedFlow()

    private val refreshMutex = Mutex()
    private val saveMutex = Mutex()
    private var activePeriod: String? = null

    fun refresh(period: String) {
        if (!refreshMutex.tryLock()) return
        activePeriod = period
        launch {
            try {
                _state.update { it.copy(isLoading = true, accessDenied = false, errorMessage = null) }
                when (val entries = repository.listFinanceEntries()) {
                    is AppResult.Error -> fail(entries, isSaving = false)
                    is AppResult.Success -> when (val overview = repository.financeOverview(period)) {
                        is AppResult.Error -> fail(overview, isSaving = false)
                        is AppResult.Success -> _state.update {
                            it.copy(
                                entries = entries.data,
                                overview = overview.data,
                                isLoading = false,
                                accessDenied = false,
                                errorMessage = null,
                            )
                        }
                    }
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    fun save(
        entryId: String?,
        expectedVersion: Int?,
        input: FinanceEntryInput,
    ) {
        if (entryId == null) {
            if (expectedVersion != null) return inputError("Un registro nuevo no puede incluir versiÃ³n.")
            createEntry(input)
        } else {
            val version = expectedVersion ?: return inputError("Falta la versiÃ³n del registro a editar.")
            updateEntry(entryId, version, input)
        }
    }

    fun createEntry(input: FinanceEntryInput) = saveEntry("create-entry") { key ->
        repository.createFinanceEntry(input, key)
    }

    fun updateEntry(
        entryId: String,
        expectedVersion: Int,
        input: FinanceEntryInput,
    ) = saveEntry("update-entry") { key ->
        repository.updateFinanceEntry(entryId, expectedVersion, input, key)
    }

    fun record(entryId: String, expectedVersion: Int) = saveEntry("record-entry") { key ->
        repository.recordFinanceEntry(entryId, expectedVersion, key)
    }

    fun void(entryId: String, expectedVersion: Int, reason: String) = saveEntry("void-entry") { key ->
        repository.voidFinanceEntry(entryId, expectedVersion, reason, key)
    }

    private fun saveEntry(
        operation: String,
        request: suspend (idempotencyKey: String) -> AppResult<BusinessFinanceEntry>,
    ) {
        if (!saveMutex.tryLock()) return
        launch {
            try {
                // Reset accessDenied at the start of a fresh attempt too (refresh() already
                // does this) - otherwise a retry after a stale access-denied can render the
                // full-screen "ask your admin" panel for the entire in-flight window even
                // though this attempt hasn't failed yet.
                _state.update { it.copy(isSaving = true, accessDenied = false, errorMessage = null) }
                when (val result = request(idempotencyKeyFactory.create(operation))) {
                    is AppResult.Error -> fail(result, isSaving = true)
                    is AppResult.Success -> {
                        replaceEntry(result.data)
                        _state.update { it.copy(isSaving = false, accessDenied = false) }
                        refreshOverviewAfterMutation()
                        _effects.emit(BusinessFinanceUiEffect.ShowMessage(successMessageFor(operation)))
                    }
                }
            } finally {
                saveMutex.unlock()
            }
        }
    }

    private suspend fun refreshOverviewAfterMutation() {
        val period = activePeriod ?: return
        when (val result = repository.financeOverview(period)) {
            is AppResult.Success -> _state.update { it.copy(overview = result.data) }
            is AppResult.Error -> {
                if (result.cause is BusinessFinanceAccessDeniedException) {
                    _state.update { it.copy(accessDenied = true, errorMessage = null) }
                    _effects.emit(BusinessFinanceUiEffect.AccessDenied)
                } else {
                    // Same accessDenied-must-not-survive-an-unrelated-error rule as fail()
                    // below - this is a separate error path (the post-save overview refresh)
                    // that had the identical gap.
                    _state.update { it.copy(accessDenied = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun replaceEntry(entry: BusinessFinanceEntry) {
        _state.update { current ->
            val index = current.entries.indexOfFirst { it.id == entry.id }
            val entries = if (index < 0) listOf(entry) + current.entries else {
                current.entries.toMutableList().also { it[index] = entry }
            }
            current.copy(entries = entries)
        }
    }

    private fun inputError(message: String) = launch {
        _state.update { it.copy(errorMessage = message) }
        _effects.emit(BusinessFinanceUiEffect.ShowMessage(message))
    }

    private suspend fun fail(result: AppResult.Error, isSaving: Boolean) {
        if (result.cause is BusinessFinanceAccessDeniedException) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isSaving = if (isSaving) false else it.isSaving,
                    accessDenied = true,
                    errorMessage = null,
                )
            }
            _effects.emit(BusinessFinanceUiEffect.AccessDenied)
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    isSaving = if (isSaving) false else it.isSaving,
                    // A stale accessDenied=true from an earlier failure must not survive
                    // into an unrelated later error - otherwise the UI shows this error
                    // AND a full-screen "ask your admin for access" state together.
                    accessDenied = false,
                    errorMessage = result.message,
                )
            }
            _effects.emit(BusinessFinanceUiEffect.ShowMessage(result.message))
        }
    }

    private fun successMessageFor(operation: String): String = when (operation) {
        "create-entry" -> "Registro creado"
        "update-entry" -> "Registro actualizado"
        "record-entry" -> "Registro contabilizado"
        else -> "Registro anulado"
    }
}
