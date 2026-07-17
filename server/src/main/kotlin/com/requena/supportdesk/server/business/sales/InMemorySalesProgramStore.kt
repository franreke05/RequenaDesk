package com.requena.supportdesk.server.business.sales

import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItemType
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteLine
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSaleStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPage
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRules
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovement
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovementType
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary
import com.requena.supportdesk.features.business.sales.domain.CalculatedBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CalculatedBusinessQuoteUpdate
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import java.time.Instant
import java.util.UUID

/** Thread-safe test and local-development implementation with the same tenant and stock guarantees. */
class InMemorySalesProgramStore(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> String = { Instant.now().toString() },
) : SalesProgramStore {
    private val lock = Any()
    private val customers = mutableMapOf<String, StoredCustomer>()
    private val contacts = mutableMapOf<String, StoredContact>()
    private val catalog = mutableMapOf<String, StoredCatalogItem>()
    private val movements = mutableMapOf<String, StoredMovement>()
    private val quotes = mutableMapOf<String, StoredQuote>()
    private val sales = mutableMapOf<String, StoredSale>()
    private val adjustmentKeys = mutableMapOf<Pair<String, String>, String>()
    private val quoteKeys = mutableMapOf<Pair<String, String>, String>()
    private val conversionKeys = mutableMapOf<Pair<String, String>, String>()
    private val quoteNumbers = mutableMapOf<String, Int>()
    private val saleNumbers = mutableMapOf<String, Int>()

    override fun customers(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCustomer> = synchronized(lock) {
        val query = request.query
        page(
            customers.values.asSequence()
                .filter { it.clientId == clientId }
                .map(StoredCustomer::value)
                .filter { request.status == null || it.status.name == request.status }
                .filter { query == null || it.displayName.contains(query, ignoreCase = true) }
                .sortedBy { it.id }
                .toList(),
            request,
        ) { it.id }
    }

    override fun customer(clientId: String, customerId: String): BusinessCustomerDetail? = synchronized(lock) {
        val customer = customers[customerId]?.takeIf { it.clientId == clientId }?.value ?: return@synchronized null
        BusinessCustomerDetail(
            customer = customer,
            contacts = contacts.values.asSequence().filter { it.clientId == clientId && it.value.customerId == customerId }
                .map(StoredContact::value).sortedBy { it.fullName.lowercase() }.toList(),
        )
    }

    override fun createCustomer(clientId: String, actorUserId: String, input: CreateBusinessCustomerInput): BusinessCustomer = synchronized(lock) {
        val value = BusinessCustomer(idFactory(), input.displayName, input.taxId, input.email, input.phone, input.address, BusinessCustomerStatus.ACTIVE, 1, now())
        customers[value.id] = StoredCustomer(clientId, value)
        value
    }

    override fun updateCustomer(clientId: String, actorUserId: String, customerId: String, input: UpdateBusinessCustomerInput): BusinessCustomer = synchronized(lock) {
        val current = customers[customerId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, input.expectedVersion)
        val updated = current.value.copy(
            displayName = input.displayName, taxId = input.taxId, email = input.email, phone = input.phone, address = input.address,
            version = input.expectedVersion + 1, updatedAt = now(),
        )
        customers[customerId] = current.copy(value = updated)
        updated
    }

    override fun archiveCustomer(clientId: String, actorUserId: String, customerId: String, expectedVersion: Int): BusinessCustomer = synchronized(lock) {
        val current = customers[customerId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, expectedVersion)
        val updated = current.value.copy(status = BusinessCustomerStatus.ARCHIVED, version = expectedVersion + 1, updatedAt = now())
        customers[customerId] = current.copy(value = updated)
        updated
    }

    override fun createContact(clientId: String, actorUserId: String, customerId: String, input: CreateBusinessContactInput): BusinessContact = synchronized(lock) {
        requireActiveCustomer(clientId, customerId)
        if (input.isPrimary) clearPrimary(clientId, customerId)
        val value = BusinessContact(idFactory(), customerId, input.fullName, input.role, input.email, input.phone, input.isPrimary, BusinessCustomerStatus.ACTIVE, 1, now())
        contacts[value.id] = StoredContact(clientId, value)
        value
    }

    override fun updateContact(clientId: String, actorUserId: String, customerId: String, contactId: String, input: UpdateBusinessContactInput): BusinessContact = synchronized(lock) {
        requireCustomer(clientId, customerId)
        val current = contacts[contactId]?.takeIf { it.clientId == clientId && it.value.customerId == customerId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, input.expectedVersion)
        if (input.isPrimary && input.status == BusinessCustomerStatus.ACTIVE) clearPrimary(clientId, customerId, except = contactId)
        val updated = current.value.copy(
            fullName = input.fullName, role = input.role, email = input.email, phone = input.phone,
            isPrimary = input.isPrimary && input.status == BusinessCustomerStatus.ACTIVE,
            status = input.status, version = input.expectedVersion + 1, updatedAt = now(),
        )
        contacts[contactId] = current.copy(value = updated)
        updated
    }

    override fun catalogItems(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCatalogItem> = synchronized(lock) {
        val query = request.query
        val status = request.status
        page(
            catalog.values.asSequence().filter { it.clientId == clientId }.map(StoredCatalogItem::value)
                .filter { status == null || when (status) { "ACTIVE" -> !it.archived; "ARCHIVED" -> it.archived; else -> it.type.name == status } }
                .filter { query == null || it.name.contains(query, ignoreCase = true) || (it.sku?.contains(query, ignoreCase = true) == true) }
                .sortedBy { it.id }.toList(),
            request,
        ) { it.id }
    }

    override fun createCatalogItem(clientId: String, actorUserId: String, input: CreateBusinessCatalogItemInput): BusinessCatalogItem = synchronized(lock) {
        BusinessSalesRules.validateCatalogItem(input)
        requireUniqueSku(clientId, input.sku)
        val value = BusinessCatalogItem(
            idFactory(), input.type, input.name, input.sku, input.description, input.unit, input.referencePriceCents,
            tracksStock = input.tracksStock, stockMinimumMilli = input.stockMinimumMilli, version = 1, updatedAt = now(),
        )
        catalog[value.id] = StoredCatalogItem(clientId, value)
        value
    }

    override fun updateCatalogItem(clientId: String, actorUserId: String, itemId: String, input: UpdateBusinessCatalogItemInput): BusinessCatalogItem = synchronized(lock) {
        val current = catalog[itemId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, input.expectedVersion)
        BusinessSalesRules.validateCatalogItem(input, current.value.type)
        requireUniqueSku(clientId, input.sku, except = itemId)
        val updated = current.value.copy(
            name = input.name, sku = input.sku, description = input.description, unit = input.unit,
            referencePriceCents = input.referencePriceCents, tracksStock = input.tracksStock,
            stockMinimumMilli = input.stockMinimumMilli, version = input.expectedVersion + 1, updatedAt = now(),
        )
        catalog[itemId] = current.copy(value = updated)
        updated
    }

    override fun archiveCatalogItem(clientId: String, actorUserId: String, itemId: String, expectedVersion: Int): BusinessCatalogItem = synchronized(lock) {
        val current = catalog[itemId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, expectedVersion)
        val updated = current.value.copy(archived = true, version = expectedVersion + 1, updatedAt = now())
        catalog[itemId] = current.copy(value = updated)
        updated
    }

    override fun stock(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockSummary> = synchronized(lock) {
        val query = request.query
        val status = request.status
        page(
            catalog.values.asSequence().filter { it.clientId == clientId && it.value.type == BusinessCatalogItemType.PRODUCT && it.value.tracksStock }
                .map { stored -> summary(stored.value) }
                .filter { query == null || it.item.name.contains(query, ignoreCase = true) }
                .filter { status == null || status != "LOW" || it.isBelowMinimum }
                .sortedBy { it.item.id }.toList(),
            request,
        ) { it.item.id }
    }

    override fun stockMovements(clientId: String, itemId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockMovement> = synchronized(lock) {
        requireStockItem(clientId, itemId)
        page(
            movements.values.asSequence().filter { it.clientId == clientId && it.value.itemId == itemId }.map(StoredMovement::value).sortedBy { it.id }.toList(),
            request,
        ) { it.id }
    }

    override fun adjustStock(clientId: String, actorUserId: String, itemId: String, input: StockAdjustmentInput): BusinessStockMovement = synchronized(lock) {
        BusinessSalesRules.validateStockAdjustment(input)
        adjustmentKeys[clientId to input.idempotencyKey]?.let { movementId -> return@synchronized requireNotNull(movements[movementId]).value }
        requireStockItem(clientId, itemId)
        if (available(itemId, clientId) + input.deltaMilli < 0) throw SalesProgramConflictException("Insufficient stock for this adjustment")
        val movement = BusinessStockMovement(idFactory(), itemId, input.type, input.deltaMilli, input.reason, createdAt = now())
        movements[movement.id] = StoredMovement(clientId, movement)
        adjustmentKeys[clientId to input.idempotencyKey] = movement.id
        movement
    }

    override fun quotes(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessQuote> = synchronized(lock) {
        val query = request.query
        val status = request.status
        page(
            quotes.values.asSequence().filter { it.clientId == clientId }.map { it.value.copy(lines = emptyList()) }
                .filter { status == null || it.status.name == status }
                .filter { query == null || it.number.contains(query, true) || it.buyerName.contains(query, true) }
                .sortedBy { it.id }.toList(),
            request,
        ) { it.id }
    }

    override fun quote(clientId: String, quoteId: String): BusinessQuote? = synchronized(lock) {
        quotes[quoteId]?.takeIf { it.clientId == clientId }?.value
    }

    override fun createQuote(clientId: String, actorUserId: String, input: CalculatedBusinessQuoteInput): BusinessQuote = synchronized(lock) {
        quoteKeys[clientId to input.source.idempotencyKey]?.let { return@synchronized requireNotNull(quotes[it]).value }
        input.source.customerId?.let { requireCustomer(clientId, it) }
        input.lines.forEach { it.sourceCatalogItemId?.let { itemId -> requireCatalogItem(clientId, itemId) } }
        val value = BusinessQuote(
            idFactory(), "Q-${nextNumber(quoteNumbers, clientId)}", input.source.customerId, input.source.buyerName, input.source.buyerEmail,
            input.source.buyerPhone, BusinessQuoteStatus.DRAFT, input.source.issueDate, input.source.validUntil, input.source.notes,
            subtotalCents = input.subtotalCents, taxCents = input.taxCents, totalCents = input.totalCents, version = 1,
            lines = input.lines.map { it.copy(id = idFactory()) }, updatedAt = now(),
        )
        quotes[value.id] = StoredQuote(clientId, value)
        quoteKeys[clientId to input.source.idempotencyKey] = value.id
        value
    }

    override fun updateQuote(clientId: String, actorUserId: String, quoteId: String, input: CalculatedBusinessQuoteUpdate): BusinessQuote = synchronized(lock) {
        val current = quotes[quoteId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, input.source.expectedVersion)
        if (current.value.status != BusinessQuoteStatus.DRAFT) throw SalesProgramConflictException("Only draft quotes can be edited")
        input.source.customerId?.let { requireCustomer(clientId, it) }
        input.lines.forEach { it.sourceCatalogItemId?.let { itemId -> requireCatalogItem(clientId, itemId) } }
        val updated = current.value.copy(
            customerId = input.source.customerId, buyerName = input.source.buyerName, buyerEmail = input.source.buyerEmail,
            buyerPhone = input.source.buyerPhone, issueDate = input.source.issueDate, validUntil = input.source.validUntil, notes = input.source.notes,
            subtotalCents = input.subtotalCents, taxCents = input.taxCents, totalCents = input.totalCents, version = input.source.expectedVersion + 1,
            lines = input.lines.map { it.copy(id = idFactory()) }, updatedAt = now(),
        )
        quotes[quoteId] = current.copy(value = updated)
        updated
    }

    override fun transitionQuote(clientId: String, actorUserId: String, quoteId: String, target: BusinessQuoteStatus, expectedVersion: Int): BusinessQuote = synchronized(lock) {
        val current = quotes[quoteId]?.takeIf { it.clientId == clientId } ?: throw SalesProgramNotFoundException()
        requireVersion(current.value.version, expectedVersion)
        if (!validTransition(current.value.status, target)) throw SalesProgramConflictException("This quote transition is not allowed")
        val updated = current.value.copy(status = target, version = expectedVersion + 1, updatedAt = now())
        quotes[quoteId] = current.copy(value = updated)
        updated
    }

    override fun convertQuote(clientId: String, actorUserId: String, quoteId: String, input: ConvertBusinessQuoteInput): BusinessSale = synchronized(lock) {
        conversionKeys[clientId to quoteId]?.let { return@synchronized requireNotNull(sales[it]).value }
        val quote = quotes[quoteId]?.takeIf { it.clientId == clientId }?.value ?: throw SalesProgramNotFoundException()
        if (quote.status != BusinessQuoteStatus.ACCEPTED) throw SalesProgramConflictException("Only accepted quotes can become sales")
        val stockLines = quote.lines.mapNotNull { line -> line.sourceCatalogItemId?.let { it to line } }
            .filter { (itemId, _) -> catalog[itemId]?.value?.tracksStock == true }
        stockLines.groupBy({ it.first }, { it.second.quantityMilli }).forEach { (itemId, quantities) ->
            if (available(itemId, clientId) < quantities.sum()) throw SalesProgramConflictException("Insufficient stock to convert this quote")
        }
        val sale = BusinessSale(
            idFactory(), "S-${nextNumber(saleNumbers, clientId)}", quote.id, quote.buyerName, quote.currency, quote.subtotalCents,
            quote.taxCents, quote.totalCents, BusinessSaleStatus.CONFIRMED, now(), quote.lines,
        )
        sales[sale.id] = StoredSale(clientId, sale)
        stockLines.forEach { (itemId, line) ->
            val movement = BusinessStockMovement(idFactory(), itemId, BusinessStockMovementType.SALE, -line.quantityMilli, "Sale ${sale.number}", sale.id, now())
            movements[movement.id] = StoredMovement(clientId, movement)
        }
        conversionKeys[clientId to quoteId] = sale.id
        sale
    }

    override fun sales(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessSale> = synchronized(lock) {
        val query = request.query
        val status = request.status
        page(
            sales.values.asSequence().filter { it.clientId == clientId }.map { it.value.copy(lines = emptyList()) }
                .filter { status == null || it.status.name == status }
                .filter { query == null || it.number.contains(query, true) || it.buyerName.contains(query, true) }
                .sortedBy { it.id }.toList(),
            request,
        ) { it.id }
    }

    override fun sale(clientId: String, saleId: String): BusinessSale? = synchronized(lock) {
        sales[saleId]?.takeIf { it.clientId == clientId }?.value
    }

    private fun requireCustomer(clientId: String, customerId: String): BusinessCustomer =
        customers[customerId]?.takeIf { it.clientId == clientId }?.value ?: throw SalesProgramNotFoundException()

    private fun requireActiveCustomer(clientId: String, customerId: String) {
        if (requireCustomer(clientId, customerId).status != BusinessCustomerStatus.ACTIVE) throw SalesProgramConflictException("Archived customers cannot receive new contacts")
    }

    private fun requireCatalogItem(clientId: String, itemId: String): BusinessCatalogItem =
        catalog[itemId]?.takeIf { it.clientId == clientId }?.value ?: throw SalesProgramNotFoundException()

    private fun requireStockItem(clientId: String, itemId: String): BusinessCatalogItem {
        val item = requireCatalogItem(clientId, itemId)
        if (item.type != BusinessCatalogItemType.PRODUCT || !item.tracksStock || item.archived) throw SalesProgramConflictException("This item does not support stock movements")
        return item
    }

    private fun clearPrimary(clientId: String, customerId: String, except: String? = null) {
        contacts.entries.filter { (_, stored) -> stored.clientId == clientId && stored.value.customerId == customerId && stored.value.isPrimary && stored.value.id != except }
            .forEach { (id, stored) -> contacts[id] = stored.copy(value = stored.value.copy(isPrimary = false, version = stored.value.version + 1, updatedAt = now())) }
    }

    private fun requireUniqueSku(clientId: String, sku: String?, except: String? = null) {
        if (sku == null) return
        if (catalog.values.any { it.clientId == clientId && it.value.id != except && it.value.sku.equals(sku, ignoreCase = true) }) {
            throw SalesProgramConflictException("SKU already exists for this business")
        }
    }

    private fun summary(item: BusinessCatalogItem): BusinessStockSummary {
        val available = available(item.id, catalog.getValue(item.id).clientId)
        return BusinessStockSummary(item, available, item.stockMinimumMilli?.let { available < it } == true)
    }

    private fun available(itemId: String, clientId: String): Long = movements.values.asSequence()
        .filter { it.clientId == clientId && it.value.itemId == itemId }
        .sumOf { it.value.deltaMilli }

    private fun validTransition(from: BusinessQuoteStatus, to: BusinessQuoteStatus): Boolean = when (from) {
        BusinessQuoteStatus.DRAFT -> to == BusinessQuoteStatus.SENT || to == BusinessQuoteStatus.EXPIRED
        BusinessQuoteStatus.SENT -> to in setOf(BusinessQuoteStatus.ACCEPTED, BusinessQuoteStatus.REJECTED, BusinessQuoteStatus.EXPIRED)
        else -> false
    }

    private fun nextNumber(numbers: MutableMap<String, Int>, clientId: String): String = "%06d".format((numbers[clientId] ?: 0) + 1).also { numbers[clientId] = (numbers[clientId] ?: 0) + 1 }

    private fun requireVersion(actual: Int, expected: Int) {
        if (actual != expected) throw SalesProgramConflictException("This record was changed by another session")
    }

    private fun <T> page(items: List<T>, request: BusinessSalesPageRequest, id: (T) -> String): BusinessSalesPage<T> {
        val afterCursor = request.cursor?.let { cursor -> items.indexOfFirst { id(it) == cursor }.takeIf { it >= 0 }?.plus(1) ?: throw IllegalArgumentException("Cursor is invalid") } ?: 0
        val results = items.drop(afterCursor).take(request.limit)
        val next = if (afterCursor + results.size < items.size) id(results.last()) else null
        return BusinessSalesPage(results, next)
    }

    private data class StoredCustomer(val clientId: String, val value: BusinessCustomer)
    private data class StoredContact(val clientId: String, val value: BusinessContact)
    private data class StoredCatalogItem(val clientId: String, val value: BusinessCatalogItem)
    private data class StoredMovement(val clientId: String, val value: BusinessStockMovement)
    private data class StoredQuote(val clientId: String, val value: BusinessQuote)
    private data class StoredSale(val clientId: String, val value: BusinessSale)
}
