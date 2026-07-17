package com.requena.supportdesk.features.business.sales.data.repository

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.sales.data.datasource.BusinessSalesDataSource
import com.requena.supportdesk.features.business.sales.data.mapper.BusinessSalesMapper
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPage
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRepository
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovement
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.QuoteTransitionInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessQuoteInput
import kotlinx.coroutines.CancellationException

class BusinessSalesRepositoryImpl(
    private val dataSource: BusinessSalesDataSource,
) : BusinessSalesRepository {
    override suspend fun customers(request: BusinessSalesPageRequest) = request("No se pudieron cargar los clientes comerciales.") {
        BusinessSalesMapper.page(dataSource.customers(request), BusinessSalesMapper::customer)
    }

    override suspend fun customer(customerId: String) = request("No se pudo cargar el cliente comercial.") {
        BusinessSalesMapper.customerDetail(dataSource.customer(customerId))
    }

    override suspend fun createCustomer(input: CreateBusinessCustomerInput) = request("No se pudo crear el cliente comercial.") {
        BusinessSalesMapper.customer(dataSource.createCustomer(input))
    }

    override suspend fun updateCustomer(customerId: String, input: UpdateBusinessCustomerInput) = request("No se pudo actualizar el cliente comercial.") {
        BusinessSalesMapper.customer(dataSource.updateCustomer(customerId, input))
    }

    override suspend fun archiveCustomer(customerId: String, expectedVersion: Int) = request("No se pudo archivar el cliente comercial.") {
        BusinessSalesMapper.customer(dataSource.archiveCustomer(customerId, expectedVersion))
    }

    override suspend fun createContact(customerId: String, input: CreateBusinessContactInput) = request("No se pudo crear el contacto.") {
        BusinessSalesMapper.contact(dataSource.createContact(customerId, input))
    }

    override suspend fun updateContact(customerId: String, contactId: String, input: UpdateBusinessContactInput) = request("No se pudo actualizar el contacto.") {
        BusinessSalesMapper.contact(dataSource.updateContact(customerId, contactId, input))
    }

    override suspend fun catalogItems(request: BusinessSalesPageRequest) = request("No se pudo cargar el catÃ¡logo.") {
        BusinessSalesMapper.page(dataSource.catalogItems(request), BusinessSalesMapper::catalogItem)
    }

    override suspend fun createCatalogItem(input: CreateBusinessCatalogItemInput) = request("No se pudo crear el artÃ­culo.") {
        BusinessSalesMapper.catalogItem(dataSource.createCatalogItem(input))
    }

    override suspend fun updateCatalogItem(itemId: String, input: UpdateBusinessCatalogItemInput) = request("No se pudo actualizar el artÃ­culo.") {
        BusinessSalesMapper.catalogItem(dataSource.updateCatalogItem(itemId, input))
    }

    override suspend fun archiveCatalogItem(itemId: String, expectedVersion: Int) = request("No se pudo archivar el artÃ­culo.") {
        BusinessSalesMapper.catalogItem(dataSource.archiveCatalogItem(itemId, expectedVersion))
    }

    override suspend fun stock(request: BusinessSalesPageRequest) = request("No se pudo cargar el stock.") {
        BusinessSalesMapper.page(dataSource.stock(request), BusinessSalesMapper::stockSummary)
    }

    override suspend fun stockMovements(itemId: String, request: BusinessSalesPageRequest) = request("No se pudieron cargar los movimientos.") {
        BusinessSalesMapper.page(dataSource.stockMovements(itemId, request), BusinessSalesMapper::stockMovement)
    }

    override suspend fun adjustStock(itemId: String, input: StockAdjustmentInput) = request("No se pudo ajustar el stock.") {
        BusinessSalesMapper.stockMovement(dataSource.adjustStock(itemId, input))
    }

    override suspend fun quotes(request: BusinessSalesPageRequest) = request("No se pudieron cargar los presupuestos.") {
        BusinessSalesMapper.page(dataSource.quotes(request), BusinessSalesMapper::quote)
    }

    override suspend fun quote(quoteId: String) = request("No se pudo cargar el presupuesto.") {
        BusinessSalesMapper.quote(dataSource.quote(quoteId))
    }

    override suspend fun createQuote(input: CreateBusinessQuoteInput) = request("No se pudo crear el presupuesto.") {
        BusinessSalesMapper.quote(dataSource.createQuote(input))
    }

    override suspend fun updateQuote(quoteId: String, input: UpdateBusinessQuoteInput) = request("No se pudo actualizar el presupuesto.") {
        BusinessSalesMapper.quote(dataSource.updateQuote(quoteId, input))
    }

    override suspend fun transitionQuote(quoteId: String, target: BusinessQuoteStatus, input: QuoteTransitionInput) = request("No se pudo cambiar el estado del presupuesto.") {
        BusinessSalesMapper.quote(dataSource.transitionQuote(quoteId, target, input))
    }

    override suspend fun convertQuote(quoteId: String, input: ConvertBusinessQuoteInput) = request("No se pudo convertir el presupuesto en venta.") {
        BusinessSalesMapper.sale(dataSource.convertQuote(quoteId, input))
    }

    override suspend fun sales(request: BusinessSalesPageRequest) = request("No se pudieron cargar las ventas.") {
        BusinessSalesMapper.page(dataSource.sales(request), BusinessSalesMapper::sale)
    }

    override suspend fun sale(saleId: String) = request("No se pudo cargar la venta.") {
        BusinessSalesMapper.sale(dataSource.sale(saleId))
    }

    private suspend fun <T> request(message: String, block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AppResult.Error(error.message ?: message, error)
    }
}
