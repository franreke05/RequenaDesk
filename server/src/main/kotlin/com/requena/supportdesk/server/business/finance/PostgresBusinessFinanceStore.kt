package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentKind
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.CalculatedFinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.CalculatedSalesLine
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInputWithTotals
import java.sql.Connection
import java.sql.ResultSet

/** Adapter supplied by the application module around PostgresSupportDeskDataSource. */
interface BusinessFinanceConnectionProvider {
    fun <T> withConnection(block: (Connection) -> T): T
}

class PostgresBusinessFinanceStore(
    private val connectionProvider: BusinessFinanceConnectionProvider,
) : BusinessFinanceStore {
    override fun listSalesDocuments(clientId: String): List<BusinessSalesDocument> = connectionProvider.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id::text, document_kind, status, issuer_name, customer_name, issue_date, due_date,
                   notes, currency, subtotal_cents, tax_cents, total_cents, version
            FROM business_sales_documents
            WHERE client_id = CAST(? AS uuid)
            ORDER BY issue_date DESC, id DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { result ->
                buildList { while (result.next()) add(readSalesDocument(connection, clientId, result)) }
            }
        }
    }

    override fun createSalesDocument(
        clientId: String,
        actorUserId: String,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument = transaction { connection ->
        val documentId = connection.prepareStatement(
            """
            INSERT INTO business_sales_documents (
                client_id, document_kind, status, issuer_name, customer_name, issue_date, due_date,
                currency, notes, subtotal_cents, tax_cents, total_cents, version, created_by_user_id
            ) VALUES (CAST(? AS uuid), ?, 'DRAFT', ?, ?, CAST(? AS date), CAST(? AS date), ?, ?, ?, ?, ?, 1, CAST(? AS uuid))
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, draft.input.kind.name)
            statement.setString(3, draft.input.issuerName)
            statement.setString(4, draft.input.customerName)
            statement.setString(5, draft.input.issueDate)
            statement.setString(6, draft.input.dueDate)
            statement.setString(7, draft.input.currency)
            statement.setString(8, draft.input.notes)
            statement.setLong(9, draft.subtotalCents)
            statement.setLong(10, draft.taxCents)
            statement.setLong(11, draft.totalCents)
            statement.setString(12, actorUserId)
            statement.executeQuery().use { result -> check(result.next()); result.getString(1) }
        }
        draft.lines.forEachIndexed { index, line -> insertLine(connection, documentId, line, index) }
        insertDocumentEvent(connection, clientId, documentId, 1, "CREATED", actorUserId)
        getSalesDocument(connection, clientId, documentId)
    }

    override fun archiveSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
    ): BusinessSalesDocument = transaction { connection ->
        val version = connection.prepareStatement(
            """
            UPDATE business_sales_documents
            SET status = 'ARCHIVED', archived_at = NOW(), version = version + 1, updated_at = NOW()
            WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid) AND version = ? AND status = 'DRAFT'
            RETURNING version
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, documentId)
            statement.setString(2, clientId)
            statement.setInt(3, expectedVersion)
            statement.executeQuery().use { result -> if (result.next()) result.getInt(1) else 0 }
        }
        if (version == 0) throw BusinessFinanceConflictException("The draft was changed, archived, or does not exist")
        insertDocumentEvent(connection, clientId, documentId, version, "ARCHIVED", actorUserId)
        getSalesDocument(connection, clientId, documentId)
    }

    override fun updateSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument = transaction { connection ->
        val version = connection.prepareStatement(
            """
            UPDATE business_sales_documents
            SET document_kind = ?, issuer_name = ?, customer_name = ?, issue_date = CAST(? AS date),
                due_date = CAST(? AS date), currency = ?, notes = ?, subtotal_cents = ?, tax_cents = ?,
                total_cents = ?, version = version + 1, updated_at = NOW()
            WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid) AND version = ? AND status = 'DRAFT'
            RETURNING version
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, draft.input.kind.name); statement.setString(2, draft.input.issuerName)
            statement.setString(3, draft.input.customerName); statement.setString(4, draft.input.issueDate)
            statement.setString(5, draft.input.dueDate); statement.setString(6, draft.input.currency)
            statement.setString(7, draft.input.notes); statement.setLong(8, draft.subtotalCents)
            statement.setLong(9, draft.taxCents); statement.setLong(10, draft.totalCents)
            statement.setString(11, documentId); statement.setString(12, clientId); statement.setInt(13, expectedVersion)
            statement.executeQuery().use { result -> if (result.next()) result.getInt(1) else 0 }
        }
        if (version == 0) throw BusinessFinanceConflictException("The draft was changed, archived, or does not exist")
        connection.prepareStatement("DELETE FROM business_sales_document_lines WHERE document_id = CAST(? AS uuid)").use { statement ->
            statement.setString(1, documentId); statement.executeUpdate()
        }
        draft.lines.forEachIndexed { index, line -> insertLine(connection, documentId, line, index) }
        insertDocumentEvent(connection, clientId, documentId, version, "UPDATED", actorUserId)
        getSalesDocument(connection, clientId, documentId)
    }

    override fun listFinanceEntries(clientId: String): List<BusinessFinanceEntry> = connectionProvider.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id::text, direction, status, occurred_on, counterparty_name, category_name, description,
                   net_cents, tax_rate_basis_points, tax_cents, gross_cents, currency, payment_status,
                   external_reference, void_reason, version
            FROM business_finance_entries
            WHERE client_id = CAST(? AS uuid)
            ORDER BY occurred_on DESC, id DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { result -> buildList { while (result.next()) add(readEntry(result)) } }
        }
    }

    override fun createFinanceEntry(
        clientId: String,
        actorUserId: String,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry = transaction { connection ->
        val id = connection.prepareStatement(
            """
            INSERT INTO business_finance_entries (
                client_id, direction, status, occurred_on, counterparty_name, category_name, description,
                net_cents, tax_rate_basis_points, tax_cents, gross_cents, currency, payment_status,
                external_reference, version, created_by_user_id
            ) VALUES (CAST(? AS uuid), ?, 'DRAFT', CAST(? AS date), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, CAST(? AS uuid))
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, entry.input.direction.name)
            statement.setString(3, entry.input.occurredOn)
            statement.setString(4, entry.input.counterpartyName)
            statement.setString(5, entry.input.categoryName)
            statement.setString(6, entry.input.description)
            statement.setLong(7, entry.input.netCents)
            statement.setInt(8, entry.input.taxRateBasisPoints)
            statement.setLong(9, entry.taxCents)
            statement.setLong(10, entry.grossCents)
            statement.setString(11, entry.input.currency)
            statement.setString(12, entry.input.paymentStatus.name)
            statement.setString(13, entry.input.externalReference)
            statement.setString(14, actorUserId)
            statement.executeQuery().use { result -> check(result.next()); result.getString(1) }
        }
        insertEntryEvent(connection, clientId, id, 1, "CREATED", actorUserId)
        getFinanceEntry(connection, clientId, id)
    }

    override fun recordFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
    ): BusinessFinanceEntry = updateEntryState(clientId, actorUserId, entryId, expectedVersion, "DRAFT", "RECORDED", null)

    override fun updateFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry = transaction { connection ->
        val version = connection.prepareStatement(
            """
            UPDATE business_finance_entries
            SET direction = ?, occurred_on = CAST(? AS date), counterparty_name = ?, category_name = ?,
                description = ?, net_cents = ?, tax_rate_basis_points = ?, tax_cents = ?, gross_cents = ?,
                currency = ?, payment_status = ?, external_reference = ?, version = version + 1, updated_at = NOW()
            WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid) AND version = ? AND status = 'DRAFT'
            RETURNING version
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, entry.input.direction.name); statement.setString(2, entry.input.occurredOn)
            statement.setString(3, entry.input.counterpartyName); statement.setString(4, entry.input.categoryName)
            statement.setString(5, entry.input.description); statement.setLong(6, entry.input.netCents)
            statement.setInt(7, entry.input.taxRateBasisPoints); statement.setLong(8, entry.taxCents)
            statement.setLong(9, entry.grossCents); statement.setString(10, entry.input.currency)
            statement.setString(11, entry.input.paymentStatus.name); statement.setString(12, entry.input.externalReference)
            statement.setString(13, entryId); statement.setString(14, clientId); statement.setInt(15, expectedVersion)
            statement.executeQuery().use { result -> if (result.next()) result.getInt(1) else 0 }
        }
        if (version == 0) throw BusinessFinanceConflictException("The draft was changed or does not exist")
        insertEntryEvent(connection, clientId, entryId, version, "UPDATED", actorUserId)
        getFinanceEntry(connection, clientId, entryId)
    }

    override fun voidFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        reason: String,
    ): BusinessFinanceEntry = updateEntryState(clientId, actorUserId, entryId, expectedVersion, null, "VOID", reason)

    override fun financeOverview(clientId: String, period: String): FinanceOverview {
        val entries = listFinanceEntries(clientId).filter { it.status != BusinessFinanceEntryStatus.VOID && it.occurredOn.startsWith(period) }
        val income = entries.filter { it.direction == BusinessFinanceDirection.INCOME }.sumOf(BusinessFinanceEntry::grossCents)
        val expenses = entries.filter { it.direction == BusinessFinanceDirection.EXPENSE }.sumOf(BusinessFinanceEntry::grossCents)
        val pending = entries.filter { it.paymentStatus == BusinessPaymentStatus.PENDING }.sumOf(BusinessFinanceEntry::grossCents)
        return FinanceOverview(period, income, expenses, income - expenses, pending)
    }

    override fun auditEvents(clientId: String): List<BusinessFinanceAuditEvent> = connectionProvider.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT client_id::text, 'SALES_DOCUMENT' AS aggregate_type, document_id::text AS aggregate_id,
                   action, COALESCE(actor_user_id::text, '') AS actor_user_id, version
            FROM business_sales_document_events WHERE client_id = CAST(? AS uuid)
            UNION ALL
            SELECT client_id::text, 'FINANCE_ENTRY', entry_id::text, action,
                   COALESCE(actor_user_id::text, ''), version
            FROM business_finance_entry_events WHERE client_id = CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) add(
                        BusinessFinanceAuditEvent(
                            result.getString("client_id"), result.getString("aggregate_type"),
                            result.getString("aggregate_id"), result.getString("action"),
                            result.getString("actor_user_id"), result.getInt("version"),
                        ),
                    )
                }
            }
        }
    }

    private fun updateEntryState(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        requiredStatus: String?,
        newStatus: String,
        voidReason: String?,
    ): BusinessFinanceEntry = transaction { connection ->
        val conditions = if (requiredStatus == null) "status <> 'VOID'" else "status = '$requiredStatus'"
        val version = connection.prepareStatement(
            """
            UPDATE business_finance_entries
            SET status = ?, void_reason = ?, version = version + 1, updated_at = NOW()
            WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid) AND version = ? AND $conditions
            RETURNING version
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, newStatus)
            statement.setString(2, voidReason)
            statement.setString(3, entryId)
            statement.setString(4, clientId)
            statement.setInt(5, expectedVersion)
            statement.executeQuery().use { result -> if (result.next()) result.getInt(1) else 0 }
        }
        if (version == 0) throw BusinessFinanceConflictException("The entry was changed or is not available for this action")
        insertEntryEvent(connection, clientId, entryId, version, if (newStatus == "VOID") "VOIDED" else "RECORDED", actorUserId)
        getFinanceEntry(connection, clientId, entryId)
    }

    private fun insertLine(connection: Connection, documentId: String, line: CalculatedSalesLine, order: Int) {
        connection.prepareStatement(
            """
            INSERT INTO business_sales_document_lines (
                document_id, description, quantity_milli, unit_price_cents, tax_rate_basis_points,
                discount_basis_points, subtotal_cents, tax_cents, total_cents, sort_order
            ) VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, documentId); statement.setString(2, line.description)
            statement.setLong(3, line.quantityMilli); statement.setLong(4, line.unitPriceCents)
            statement.setInt(5, line.taxRateBasisPoints); statement.setInt(6, line.discountBasisPoints)
            statement.setLong(7, line.subtotalCents); statement.setLong(8, line.taxCents)
            statement.setLong(9, line.totalCents); statement.setInt(10, order)
            statement.executeUpdate()
        }
    }

    private fun insertDocumentEvent(connection: Connection, clientId: String, id: String, version: Int, action: String, actorId: String) {
        connection.prepareStatement(
            "INSERT INTO business_sales_document_events (client_id, document_id, version, action, actor_user_id) VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS uuid))",
        ).use { statement ->
            statement.setString(1, clientId); statement.setString(2, id); statement.setInt(3, version)
            statement.setString(4, action); statement.setString(5, actorId); statement.executeUpdate()
        }
    }

    private fun insertEntryEvent(connection: Connection, clientId: String, id: String, version: Int, action: String, actorId: String) {
        connection.prepareStatement(
            "INSERT INTO business_finance_entry_events (client_id, entry_id, version, action, actor_user_id) VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS uuid))",
        ).use { statement ->
            statement.setString(1, clientId); statement.setString(2, id); statement.setInt(3, version)
            statement.setString(4, action); statement.setString(5, actorId); statement.executeUpdate()
        }
    }

    private fun getSalesDocument(connection: Connection, clientId: String, id: String): BusinessSalesDocument = connection.prepareStatement(
        """SELECT id::text, document_kind, status, issuer_name, customer_name, issue_date, due_date, notes, currency,
                  subtotal_cents, tax_cents, total_cents, version FROM business_sales_documents
           WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid)""",
    ).use { statement ->
        statement.setString(1, id); statement.setString(2, clientId)
        statement.executeQuery().use { result -> if (result.next()) readSalesDocument(connection, clientId, result) else throw BusinessFinanceNotFoundException() }
    }

    private fun readSalesDocument(connection: Connection, clientId: String, result: ResultSet): BusinessSalesDocument {
        val id = result.getString("id")
        val lines = connection.prepareStatement(
            """SELECT description, quantity_milli, unit_price_cents, tax_rate_basis_points, discount_basis_points,
                      subtotal_cents, tax_cents, total_cents FROM business_sales_document_lines
               WHERE document_id = CAST(? AS uuid) ORDER BY sort_order""",
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { lineResult ->
                buildList {
                    while (lineResult.next()) add(
                        CalculatedSalesLine(
                            lineResult.getString("description"), lineResult.getLong("quantity_milli"),
                            lineResult.getLong("unit_price_cents"), lineResult.getInt("tax_rate_basis_points"),
                            lineResult.getInt("discount_basis_points"), lineResult.getLong("subtotal_cents"),
                            lineResult.getLong("tax_cents"), lineResult.getLong("total_cents"),
                        ),
                    )
                }
            }
        }
        return BusinessSalesDocument(
            id = id, kind = BusinessSalesDocumentKind.valueOf(result.getString("document_kind")),
            status = BusinessSalesDocumentStatus.valueOf(result.getString("status")),
            issuerName = result.getString("issuer_name"), customerName = result.getString("customer_name"),
            issueDate = result.getDate("issue_date").toLocalDate().toString(),
            dueDate = result.getDate("due_date")?.toLocalDate()?.toString(), notes = result.getString("notes"),
            currency = result.getString("currency"), subtotalCents = result.getLong("subtotal_cents"),
            taxCents = result.getLong("tax_cents"), totalCents = result.getLong("total_cents"),
            version = result.getInt("version"), lines = lines,
        )
    }

    private fun getFinanceEntry(connection: Connection, clientId: String, id: String): BusinessFinanceEntry = connection.prepareStatement(
        """SELECT id::text, direction, status, occurred_on, counterparty_name, category_name, description,
                  net_cents, tax_rate_basis_points, tax_cents, gross_cents, currency, payment_status,
                  external_reference, void_reason, version FROM business_finance_entries
           WHERE id = CAST(? AS uuid) AND client_id = CAST(? AS uuid)""",
    ).use { statement ->
        statement.setString(1, id); statement.setString(2, clientId)
        statement.executeQuery().use { result -> if (result.next()) readEntry(result) else throw BusinessFinanceNotFoundException() }
    }

    private fun readEntry(result: ResultSet): BusinessFinanceEntry = BusinessFinanceEntry(
        id = result.getString("id"), direction = BusinessFinanceDirection.valueOf(result.getString("direction")),
        status = BusinessFinanceEntryStatus.valueOf(result.getString("status")),
        occurredOn = result.getDate("occurred_on").toLocalDate().toString(),
        counterpartyName = result.getString("counterparty_name"), categoryName = result.getString("category_name"),
        description = result.getString("description"), netCents = result.getLong("net_cents"),
        taxRateBasisPoints = result.getInt("tax_rate_basis_points"), taxCents = result.getLong("tax_cents"),
        grossCents = result.getLong("gross_cents"), currency = result.getString("currency"),
        paymentStatus = BusinessPaymentStatus.valueOf(result.getString("payment_status")),
        externalReference = result.getString("external_reference"), voidReason = result.getString("void_reason"),
        version = result.getInt("version"),
    )

    private fun <T> transaction(block: (Connection) -> T): T = connectionProvider.withConnection { connection ->
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            block(connection).also { connection.commit() }
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }
}
