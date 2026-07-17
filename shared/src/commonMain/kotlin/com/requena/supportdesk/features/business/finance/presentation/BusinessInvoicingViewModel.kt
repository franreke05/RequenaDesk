package com.requena.supportdesk.features.business.finance.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.finance.data.repository.BusinessFinanceAccessDeniedException
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceRepository
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

class BusinessInvoicingViewModel(
    private val repository: BusinessFinanceRepository,
    private val idempotencyKeyFactory: BusinessFinanceIdempotencyKeyFactory = RandomBusinessFinanceIdempotencyKeyFactory,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessInvoicingUiState())
    val state: StateFlow<BusinessInvoicingUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessFinanceUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)
    val effects: SharedFlow<BusinessFinanceUiEffect> = _effects.asSharedFlow()

    private val refreshMutex = Mutex()
    private val saveMutex = Mutex()

    fun refresh() {
        if (!refreshMutex.tryLock()) return
        launch {
            try {
                _state.update { it.copy(isLoading = true, accessDenied = false, errorMessage = null) }
                when (val result = repository.listSalesDocuments()) {
                    is AppResult.Error -> fail(result, isSaving = false)
                    is AppResult.Success -> _state.update {
                        it.copy(
                            documents = result.data,
                            isLoading = false,
                            accessDenied = false,
                            errorMessage = null,
                        )
                    }
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    fun save(
        documentId: String?,
        expectedVersion: Int?,
        input: SalesDocumentDraftInput,
    ) {
        if (documentId == null) {
            if (expectedVersion != null) return inputError("Un borrador nuevo no puede incluir versiÃ³n.")
            createDraft(input)
        } else {
            val version = expectedVersion ?: return inputError("Falta la versiÃ³n del borrador a editar.")
            updateDraft(documentId, version, input)
        }
    }

    fun createDraft(input: SalesDocumentDraftInput) = saveDocument("create-draft") { key ->
        repository.createSalesDraft(input, key)
    }

    fun updateDraft(
        documentId: String,
        expectedVersion: Int,
        input: SalesDocumentDraftInput,
    ) = saveDocument("update-draft") { key ->
        repository.updateSalesDraft(documentId, expectedVersion, input, key)
    }

    fun archive(documentId: String, expectedVersion: Int) = saveDocument("archive-draft") { key ->
        repository.archiveSalesDocument(documentId, expectedVersion, key)
    }

    private fun saveDocument(
        operation: String,
        request: suspend (idempotencyKey: String) -> AppResult<BusinessSalesDocument>,
    ) {
        if (!saveMutex.tryLock()) return
        launch {
            try {
                _state.update { it.copy(isSaving = true, errorMessage = null) }
                when (val result = request(idempotencyKeyFactory.create(operation))) {
                    is AppResult.Error -> fail(result, isSaving = true)
                    is AppResult.Success -> {
                        replaceDocument(result.data)
                        _state.update { it.copy(isSaving = false, accessDenied = false) }
                        _effects.emit(BusinessFinanceUiEffect.ShowMessage(successMessageFor(operation)))
                    }
                }
            } finally {
                saveMutex.unlock()
            }
        }
    }

    private fun replaceDocument(document: BusinessSalesDocument) {
        _state.update { current ->
            val index = current.documents.indexOfFirst { it.id == document.id }
            val documents = if (index < 0) listOf(document) + current.documents else {
                current.documents.toMutableList().also { it[index] = document }
            }
            current.copy(documents = documents)
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
                    errorMessage = result.message,
                )
            }
            _effects.emit(BusinessFinanceUiEffect.ShowMessage(result.message))
        }
    }

    private fun successMessageFor(operation: String): String = when (operation) {
        "create-draft" -> "Borrador creado"
        "update-draft" -> "Borrador actualizado"
        else -> "Borrador archivado"
    }
}

internal const val EFFECT_BUFFER_CAPACITY = 2
