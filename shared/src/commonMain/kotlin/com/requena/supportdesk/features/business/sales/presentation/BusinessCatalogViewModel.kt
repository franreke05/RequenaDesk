package com.requena.supportdesk.features.business.sales.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRepository
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BusinessCatalogViewModel(
    private val repository: BusinessSalesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessCatalogUiState())
    val state: StateFlow<BusinessCatalogUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessSalesUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<BusinessSalesUiEffect> = _effects.asSharedFlow()

    fun refresh(query: String? = null) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val items = repository.catalogItems(BusinessSalesPageRequest(query = query))
        val stock = repository.stock(BusinessSalesPageRequest(query = query))
        when {
            items is AppResult.Error -> fail(items.message)
            stock is AppResult.Error -> fail(stock.message)
            items is AppResult.Success && stock is AppResult.Success -> _state.update {
                it.copy(isLoading = false, items = items.data.items, stock = stock.data.items)
            }
        }
    }

    fun createItem(input: CreateBusinessCatalogItemInput) = save {
        repository.createCatalogItem(input).mapSuccess { item ->
            _state.update { it.copy(items = listOf(item) + it.items) }
            emit("ArtÃ­culo creado")
        }
    }

    fun updateItem(itemId: String, input: UpdateBusinessCatalogItemInput) = save {
        repository.updateCatalogItem(itemId, input).mapSuccess { item ->
            _state.update { state ->
                state.copy(
                    items = state.items.map { if (it.id == item.id) item else it },
                    stock = state.stock.map { summary -> if (summary.item.id == item.id) summary.copy(item = item) else summary },
                )
            }
            emit("ArtÃ­culo actualizado")
        }
    }

    fun archiveItem(itemId: String, expectedVersion: Int) = save {
        repository.archiveCatalogItem(itemId, expectedVersion).mapSuccess { item ->
            _state.update { state -> state.copy(items = state.items.map { if (it.id == item.id) item else it }) }
            emit("ArtÃ­culo archivado")
        }
    }

    fun adjustStock(itemId: String, input: StockAdjustmentInput) = save {
        when (val result = repository.adjustStock(itemId, input)) {
            is AppResult.Error -> result
            is AppResult.Success -> reloadStock().onSuccess {
                emit("Movimiento de stock registrado")
            }
        }
    }

    private suspend fun reloadStock(): AppResult<Unit> = when (val stock = repository.stock()) {
        is AppResult.Error -> stock
        is AppResult.Success -> {
            _state.update { state -> state.copy(stock = stock.data.items) }
            AppResult.Success(Unit)
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

    private suspend fun AppResult<Unit>.onSuccess(block: suspend () -> Unit): AppResult<Unit> = when (this) {
        is AppResult.Error -> this
        is AppResult.Success -> {
            block()
            this
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
