package com.requena.supportdesk.features.business.sales.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRepository
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BusinessCustomersViewModel(
    private val repository: BusinessSalesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessCustomersUiState())
    val state: StateFlow<BusinessCustomersUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessSalesUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<BusinessSalesUiEffect> = _effects.asSharedFlow()

    fun refresh(query: String? = null) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = repository.customers(BusinessSalesPageRequest(query = query))) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, customers = result.data.items) }
        }
    }

    fun selectCustomer(customerId: String) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = repository.customer(customerId)) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, selectedCustomer = result.data) }
        }
    }

    fun createCustomer(input: CreateBusinessCustomerInput) = save {
        repository.createCustomer(input).mapSuccess { created ->
            _state.update { state -> state.copy(customers = listOf(created) + state.customers) }
            emit("Cliente comercial creado")
        }
    }

    fun updateCustomer(customerId: String, input: UpdateBusinessCustomerInput) = save {
        repository.updateCustomer(customerId, input).mapSuccess { updated ->
            updateCustomerInState(updated)
            emit("Cliente comercial actualizado")
        }
    }

    fun archiveCustomer(customerId: String, expectedVersion: Int) = save {
        repository.archiveCustomer(customerId, expectedVersion).mapSuccess { archived ->
            updateCustomerInState(archived)
            emit("Cliente comercial archivado")
        }
    }

    fun createContact(customerId: String, input: CreateBusinessContactInput) = save {
        when (val result = repository.createContact(customerId, input)) {
            is AppResult.Error -> result
            is AppResult.Success -> reloadSelectedCustomer(customerId).onSuccess {
                emit("Contacto creado")
            }
        }
    }

    fun updateContact(customerId: String, contactId: String, input: UpdateBusinessContactInput) = save {
        when (val result = repository.updateContact(customerId, contactId, input)) {
            is AppResult.Error -> result
            is AppResult.Success -> reloadSelectedCustomer(customerId).onSuccess {
                emit("Contacto actualizado")
            }
        }
    }

    private suspend fun reloadSelectedCustomer(customerId: String): AppResult<Unit> {
        when (val result = repository.customer(customerId)) {
            is AppResult.Error -> return result
            is AppResult.Success -> _state.update { it.copy(selectedCustomer = result.data) }
        }
        return AppResult.Success(Unit)
    }

    private fun updateCustomerInState(updated: com.requena.supportdesk.features.business.sales.domain.BusinessCustomer) {
        _state.update { current ->
            current.copy(
                customers = current.customers.map { if (it.id == updated.id) updated else it },
                selectedCustomer = current.selectedCustomer?.takeIf { it.customer.id != updated.id }
                    ?: current.selectedCustomer?.copy(customer = updated),
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

    private suspend fun AppResult<Unit>.onSuccess(block: suspend () -> Unit): AppResult<Unit> = when (this) {
        is AppResult.Error -> this
        is AppResult.Success -> {
            block()
            this
        }
    }

    private suspend fun emit(message: String) {
        _effects.emit(BusinessSalesUiEffect.ShowMessage(message))
    }

    private suspend fun fail(message: String, saving: Boolean = false) {
        _state.update { it.copy(isLoading = false, isSaving = if (saving) false else it.isSaving, errorMessage = message) }
        emit(message)
    }
}
