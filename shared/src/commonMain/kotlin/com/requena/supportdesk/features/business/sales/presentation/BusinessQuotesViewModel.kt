package com.requena.supportdesk.features.business.sales.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRepository
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.QuoteTransitionInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessQuoteInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BusinessQuotesViewModel(
    private val repository: BusinessSalesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessQuotesUiState())
    val state: StateFlow<BusinessQuotesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessSalesUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<BusinessSalesUiEffect> = _effects.asSharedFlow()

    fun refresh(status: String? = null) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val quotes = repository.quotes(BusinessSalesPageRequest(status = status))
        val sales = repository.sales()
        when {
            quotes is AppResult.Error -> fail(quotes.message)
            sales is AppResult.Error -> fail(sales.message)
            quotes is AppResult.Success && sales is AppResult.Success -> _state.update {
                it.copy(isLoading = false, quotes = quotes.data.items, sales = sales.data.items)
            }
        }
    }

    fun selectQuote(quoteId: String) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = repository.quote(quoteId)) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, selectedQuote = result.data) }
        }
    }

    fun createQuote(input: CreateBusinessQuoteInput) = save {
        repository.createQuote(input).mapSuccess { quote ->
            _state.update { it.copy(quotes = listOf(quote) + it.quotes, selectedQuote = quote) }
            emit("Presupuesto creado")
        }
    }

    fun updateQuote(quoteId: String, input: UpdateBusinessQuoteInput) = save {
        repository.updateQuote(quoteId, input).mapSuccess { quote ->
            updateQuoteInState(quote)
            emit("Presupuesto actualizado")
        }
    }

    fun transitionQuote(quoteId: String, target: BusinessQuoteStatus, expectedVersion: Int) = save {
        repository.transitionQuote(quoteId, target, QuoteTransitionInput(expectedVersion)).mapSuccess { quote ->
            updateQuoteInState(quote)
            emit("Estado del presupuesto actualizado")
        }
    }

    fun convertQuote(quoteId: String, idempotencyKey: String) = save {
        repository.convertQuote(quoteId, ConvertBusinessQuoteInput(idempotencyKey)).mapSuccess { sale ->
            _state.update { state -> state.copy(sales = listOf(sale) + state.sales) }
            emit("Venta interna creada; el stock se ha actualizado si era necesario")
        }
    }

    private fun updateQuoteInState(quote: com.requena.supportdesk.features.business.sales.domain.BusinessQuote) {
        _state.update { state ->
            state.copy(
                quotes = state.quotes.map { if (it.id == quote.id) quote else it },
                selectedQuote = state.selectedQuote?.takeIf { it.id != quote.id } ?: quote,
            )
        }
    }

    private fun save(block: suspend () -> AppResult<Unit>) {
        if (state.value.isSaving) return
        launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = block()) {
                is AppResult.Error -> fail(result.message, saving = true)
                is AppResult.Success -> _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun <T> AppResult<T>.mapSuccess(block: suspend (T) -> Unit): AppResult<Unit> = when (this) {
        is AppResult.Error -> this
        is AppResult.Success -> {
            block(data)
            AppResult.Success(Unit)
        }
    }

    private suspend fun fail(message: String, saving: Boolean = false) {
        _state.update { it.copy(isLoading = false, isSaving = if (saving) false else it.isSaving, errorMessage = message) }
        _effects.emit(BusinessSalesUiEffect.ShowMessage(message))
    }

    private suspend fun emit(message: String) {
        _effects.emit(BusinessSalesUiEffect.ShowMessage(message))
    }
}
