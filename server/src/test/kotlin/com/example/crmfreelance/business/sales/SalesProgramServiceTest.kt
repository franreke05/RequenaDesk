package com.example.crmfreelance.business.sales

import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItemType
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteLineInput
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesValidationException
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivityType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStage
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.QuoteTransitionInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovementType
import com.requena.supportdesk.server.business.sales.InMemorySalesProgramStore
import com.requena.supportdesk.server.business.sales.SalesProgramAccessGuard
import com.requena.supportdesk.server.business.sales.SalesProgramForbiddenException
import com.requena.supportdesk.server.business.sales.SalesProgramIdentity
import com.requena.supportdesk.server.business.sales.SalesProgramNotFoundException
import com.requena.supportdesk.server.business.sales.SalesProgramService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SalesProgramServiceTest {
    private val clientA = SalesProgramIdentity("user-a", "00000000-0000-0000-0000-000000000001")
    private val clientB = SalesProgramIdentity("user-b", "00000000-0000-0000-0000-000000000002")

    @Test
    fun everyProgramOperationRequiresTheInjectedEntitlementGuard() {
        val service = SalesProgramService(InMemorySalesProgramStore(), SalesProgramAccessGuard { _, _ -> false })

        assertFailsWith<SalesProgramForbiddenException> { service.customers(clientA, BusinessSalesPageRequest()) }
    }

    @Test
    fun customersAreNeverVisibleAcrossTenants() {
        val service = service()
        val customer = service.createCustomer(clientA, CreateBusinessCustomerInput("Cliente privado"))

        assertFailsWith<SalesProgramNotFoundException> { service.customer(clientB, customer.id) }
    }

    @Test
    fun customerTagsStagesActivitiesAndRestoreStayInsideTheTenant() {
        val service = service()
        val tag = service.createCustomerTag(clientA, CreateBusinessCustomerTagInput("Piloto", "#445566"))
        val customer = service.createCustomer(
            clientA,
            CreateBusinessCustomerInput(
                displayName = "Cliente con seguimiento",
                stage = BusinessCustomerStage.PROSPECT,
                tagIds = listOf(tag.id),
            ),
        )
        service.createCustomerActivity(
            clientA,
            customer.id,
            CreateBusinessCustomerActivityInput(
                type = BusinessCustomerActivityType.FOLLOW_UP,
                subject = "Llamar",
                dueAt = "2026-08-03T09:00:00Z",
            ),
        )

        val detail = service.customer(clientA, customer.id)
        assertEquals(listOf("Piloto"), detail.tags.map { it.name })
        assertEquals("Llamar", detail.nextAction?.subject)
        assertEquals(
            listOf(customer.id),
            service.customers(clientA, BusinessSalesPageRequest(stage = BusinessCustomerStage.PROSPECT, tagId = tag.id)).items.map { it.id },
        )
        assertFailsWith<SalesProgramNotFoundException> {
            service.createCustomer(clientB, CreateBusinessCustomerInput("Cruce", tagIds = listOf(tag.id)))
        }

        val archived = service.archiveCustomer(clientA, customer.id, customer.version)
        val restored = service.restoreCustomer(clientA, customer.id, archived.version)
        assertEquals(com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus.ACTIVE, restored.status)
    }

    @Test
    fun customerActivitiesRejectInvalidServerTimestamps() {
        val service = service()
        val customer = service.createCustomer(clientA, CreateBusinessCustomerInput("Cliente"))

        assertFailsWith<BusinessSalesValidationException> {
            service.createCustomerActivity(
                clientA,
                customer.id,
                CreateBusinessCustomerActivityInput(
                    type = BusinessCustomerActivityType.FOLLOW_UP,
                    subject = "Llamar",
                    dueAt = "mañana",
                ),
            )
        }
    }

    @Test
    fun concurrentQuoteConversionIsIdempotentAndNeverDuplicatesStockOutput() {
        val store = InMemorySalesProgramStore()
        val service = SalesProgramService(store, SalesProgramAccessGuard { _, _ -> true })
        val product = service.createCatalogItem(
            clientA,
            CreateBusinessCatalogItemInput(BusinessCatalogItemType.PRODUCT, "Producto trazable", referencePriceCents = 500, tracksStock = true),
        )
        service.adjustStock(clientA, product.id, StockAdjustmentInput(BusinessStockMovementType.INITIAL, 2_000, "Existencia inicial", "stock-test-0001"))
        val quote = service.createQuote(
            clientA,
            CreateBusinessQuoteInput(
                buyerName = "Comprador real",
                issueDate = "2026-07-17",
                idempotencyKey = "quote-test-0001",
                lines = listOf(BusinessQuoteLineInput(1, product.id, "Producto trazable", 1_000, 500)),
            ),
        )
        val sent = service.transitionQuote(clientA, quote.id, BusinessQuoteStatus.SENT, QuoteTransitionInput(quote.version))
        val accepted = service.transitionQuote(clientA, quote.id, BusinessQuoteStatus.ACCEPTED, QuoteTransitionInput(sent.version))
        assertEquals(BusinessQuoteStatus.ACCEPTED, accepted.status)

        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val sales: List<Future<BusinessSale>> = (1..2).map {
                executor.submit(Callable {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    service.convertQuote(clientA, quote.id, ConvertBusinessQuoteInput("convert-test-0001"))
                })
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val first = sales[0].get(5, TimeUnit.SECONDS)
            val second = sales[1].get(5, TimeUnit.SECONDS)
            assertEquals(first.id, second.id)
        } finally {
            executor.shutdownNow()
        }

        val movements = service.stockMovements(clientA, product.id, BusinessSalesPageRequest(limit = 10)).items
        assertEquals(2, movements.size)
        assertEquals(1_000, movements.sumOf { it.deltaMilli })
    }

    private fun service(): SalesProgramService = SalesProgramService(
        InMemorySalesProgramStore(),
        SalesProgramAccessGuard { _, _ -> true },
    )
}
