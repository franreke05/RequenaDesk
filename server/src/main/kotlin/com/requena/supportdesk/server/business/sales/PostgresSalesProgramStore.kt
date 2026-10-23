package com.requena.supportdesk.server.business.sales

import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItemType
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStage
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerTag
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivity
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivityType
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
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerActivityInput
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.UUID

/** PostgreSQL source of truth. Every query scopes its tenant with [clientId], even with RLS enabled. */
class PostgresSalesProgramStore(
    private val dataSource: PostgresSupportDeskDataSource,
) : SalesProgramStore {
    override fun customers(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCustomer> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, display_name, tax_id, email, phone, address, status, version, updated_at,
                   customer_type, commercial_stage, commercial_source
            FROM business_sales_customers
            WHERE client_id = ?
              AND (? IS NULL OR status = ?)
              AND (? IS NULL OR display_name ILIKE '%' || ? || '%')
              AND (CAST(? AS uuid) IS NULL OR id > CAST(? AS uuid))
              AND (? IS NULL OR commercial_stage = ?)
              AND (? IS NULL OR EXISTS (
                    SELECT 1
                    FROM business_sales_customer_tag_links link
                    WHERE link.client_id=business_sales_customers.client_id
                      AND link.customer_id=business_sales_customers.id
                      AND link.tag_id=CAST(? AS uuid)
              ))
            ORDER BY id
            LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.string(2, request.status); statement.string(3, request.status)
            statement.string(4, request.query); statement.string(5, request.query); statement.uuid(6, request.cursor); statement.uuid(7, request.cursor)
            statement.string(8, request.stage?.name); statement.string(9, request.stage?.name)
            statement.string(10, request.tagId); statement.uuid(11, request.tagId)
            statement.setInt(12, request.limit + 1)
            val values = statement.executeQuery().use { it.map(::customer) }
                .map { hydrateCustomer(connection, clientId, it) }
            page(values, request.limit) { it.id }
        }
    }

    override fun customer(clientId: String, customerId: String): BusinessCustomerDetail? = dataSource.withConnection { connection ->
        val customer = connection.prepareStatement(
            """
            SELECT id, display_name, tax_id, email, phone, address, status, version, updated_at,
                   customer_type, commercial_stage, commercial_source
            FROM business_sales_customers WHERE client_id=? AND id=?
            """.trimIndent(),
        ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, customerId); statement.executeQuery().use { rs -> if (rs.next()) customer(rs) else null } }
            ?: return@withConnection null
        val contacts = connection.prepareStatement(
            "SELECT id, customer_id, full_name, role, email, phone, is_primary, status, version, updated_at FROM business_sales_contacts WHERE client_id=? AND customer_id=? ORDER BY lower(full_name), id",
        ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, customerId); statement.executeQuery().use { it.map(::contact) } }
        val tags = customerTagsFor(connection, clientId, customerId)
        val activities = customerActivitiesFor(connection, clientId, customerId)
        BusinessCustomerDetail(
            customer = customer.copy(tags = tags, nextAction = nextAction(activities)),
            contacts = contacts,
            tags = tags,
            activities = activities,
            nextAction = nextAction(activities),
        )
    }

    override fun createCustomer(clientId: String, actorUserId: String, input: CreateBusinessCustomerInput): BusinessCustomer = transaction { connection ->
        connection.prepareStatement(
            """
            INSERT INTO business_sales_customers (
                client_id, display_name, tax_id, email, phone, address,
                customer_type, commercial_stage, commercial_source, created_by_user_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, display_name, tax_id, email, phone, address, status, version, updated_at,
                      customer_type, commercial_stage, commercial_source
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.setString(2, input.displayName); statement.setString(3, input.taxId); statement.setString(4, input.email)
            statement.setString(5, input.phone); statement.setString(6, input.address)
            statement.setString(7, input.type.name); statement.setString(8, input.stage.name); statement.setString(9, input.source); statement.uuid(10, actorUserId)
            statement.executeQuery().use { rs ->
                rs.next()
                val created = customer(rs)
                requireCustomerTags(connection, clientId, input.tagIds)
                replaceCustomerTags(connection, clientId, actorUserId, created.id, input.tagIds)
                audit(connection, clientId, actorUserId, "CUSTOMER", created.id, "CREATED", created.version)
                hydrateCustomer(connection, clientId, created)
            }
        }
    }

    override fun updateCustomer(clientId: String, actorUserId: String, customerId: String, input: UpdateBusinessCustomerInput): BusinessCustomer = transaction { connection ->
        connection.prepareStatement(
            """
            UPDATE business_sales_customers
            SET display_name=?, tax_id=?, email=?, phone=?, address=?, customer_type=?,
                commercial_stage=?, commercial_source=?, version=version+1
            WHERE client_id=? AND id=? AND version=?
            RETURNING id, display_name, tax_id, email, phone, address, status, version, updated_at,
                      customer_type, commercial_stage, commercial_source
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, input.displayName); statement.setString(2, input.taxId); statement.setString(3, input.email); statement.setString(4, input.phone)
            statement.setString(5, input.address); statement.setString(6, input.type.name); statement.setString(7, input.stage.name); statement.setString(8, input.source)
            statement.uuid(9, clientId); statement.uuid(10, customerId); statement.setInt(11, input.expectedVersion)
            statement.executeQuery().use { rs ->
                if (!rs.next()) staleOrNotFound(connection, "business_sales_customers", clientId, customerId)
                val updated = customer(rs)
                requireCustomerTags(connection, clientId, input.tagIds)
                replaceCustomerTags(connection, clientId, actorUserId, customerId, input.tagIds)
                audit(connection, clientId, actorUserId, "CUSTOMER", updated.id, "UPDATED", updated.version)
                hydrateCustomer(connection, clientId, updated)
            }
        }
    }

    override fun archiveCustomer(clientId: String, actorUserId: String, customerId: String, expectedVersion: Int): BusinessCustomer = transaction { connection ->
        connection.prepareStatement(
            """
            UPDATE business_sales_customers SET status='ARCHIVED', version=version+1
            WHERE client_id=? AND id=? AND version=?
            RETURNING id, display_name, tax_id, email, phone, address, status, version, updated_at,
                      customer_type, commercial_stage, commercial_source
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.uuid(2, customerId); statement.setInt(3, expectedVersion)
            statement.executeQuery().use { rs ->
                if (!rs.next()) staleOrNotFound(connection, "business_sales_customers", clientId, customerId)
                hydrateCustomer(connection, clientId, customer(rs).also { audit(connection, clientId, actorUserId, "CUSTOMER", it.id, "ARCHIVED", it.version) })
            }
        }
    }

    override fun restoreCustomer(
        clientId: String,
        actorUserId: String,
        customerId: String,
        expectedVersion: Int,
    ): BusinessCustomer = transaction { connection ->
        connection.prepareStatement(
            """
            UPDATE business_sales_customers SET status='ACTIVE', version=version+1
            WHERE client_id=? AND id=? AND version=?
            RETURNING id, display_name, tax_id, email, phone, address, status, version, updated_at,
                      customer_type, commercial_stage, commercial_source
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId)
            statement.uuid(2, customerId)
            statement.setInt(3, expectedVersion)
            statement.executeQuery().use { result ->
                if (!result.next()) staleOrNotFound(connection, "business_sales_customers", clientId, customerId)
                hydrateCustomer(
                    connection,
                    clientId,
                    customer(result).also { audit(connection, clientId, actorUserId, "CUSTOMER", it.id, "RESTORED", it.version) },
                )
            }
        }
    }

    override fun createContact(clientId: String, actorUserId: String, customerId: String, input: CreateBusinessContactInput): BusinessContact = transaction { connection ->
        requireActiveCustomer(connection, clientId, customerId)
        if (input.isPrimary) clearPrimaryContact(connection, clientId, customerId, null)
        connection.prepareStatement(
            """
            INSERT INTO business_sales_contacts (client_id, customer_id, full_name, role, email, phone, is_primary, created_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, customer_id, full_name, role, email, phone, is_primary, status, version, updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.uuid(2, customerId); statement.setString(3, input.fullName); statement.setString(4, input.role)
            statement.setString(5, input.email); statement.setString(6, input.phone); statement.setBoolean(7, input.isPrimary); statement.uuid(8, actorUserId)
            statement.executeQuery().use { rs -> rs.next(); contact(rs).also { audit(connection, clientId, actorUserId, "CONTACT", it.id, "CREATED", it.version) } }
        }
    }

    override fun updateContact(clientId: String, actorUserId: String, customerId: String, contactId: String, input: UpdateBusinessContactInput): BusinessContact = transaction { connection ->
        requireCustomer(connection, clientId, customerId)
        if (input.isPrimary && input.status == BusinessCustomerStatus.ACTIVE) clearPrimaryContact(connection, clientId, customerId, contactId)
        connection.prepareStatement(
            """
            UPDATE business_sales_contacts
            SET full_name=?, role=?, email=?, phone=?, is_primary=?, status=?, version=version+1
            WHERE client_id=? AND customer_id=? AND id=? AND version=?
            RETURNING id, customer_id, full_name, role, email, phone, is_primary, status, version, updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, input.fullName); statement.setString(2, input.role); statement.setString(3, input.email); statement.setString(4, input.phone)
            statement.setBoolean(5, input.isPrimary && input.status == BusinessCustomerStatus.ACTIVE); statement.setString(6, input.status.name)
            statement.uuid(7, clientId); statement.uuid(8, customerId); statement.uuid(9, contactId); statement.setInt(10, input.expectedVersion)
            statement.executeQuery().use { rs -> if (!rs.next()) staleOrNotFound(connection, "business_sales_contacts", clientId, contactId); contact(rs).also { audit(connection, clientId, actorUserId, "CONTACT", it.id, "UPDATED", it.version) } }
        }
    }

    override fun customerTags(clientId: String): List<BusinessCustomerTag> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id::text AS id, name, color_hex, is_archived, version, updated_at::text AS updated_at
            FROM business_sales_customer_tags
            WHERE client_id=CAST(? AS uuid)
            ORDER BY is_archived, lower(name), id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { it.map(::customerTag) }
        }
    }

    override fun createCustomerTag(
        clientId: String,
        actorUserId: String,
        input: CreateBusinessCustomerTagInput,
    ): BusinessCustomerTag = transaction { connection ->
        try {
            val id = connection.prepareStatement(
                """
                INSERT INTO business_sales_customer_tags (client_id, name, color_hex, created_by_user_id)
                VALUES (CAST(? AS uuid), ?, ?, CAST(? AS uuid))
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, input.name)
                statement.setString(3, input.colorHex)
                statement.setString(4, actorUserId)
                statement.executeQuery().use { result -> result.next(); result.getString(1) }
            }
            requireNotNull(customerTag(connection, clientId, id))
        } catch (error: SQLException) {
            if (error.sqlState == "23505") throw SalesProgramConflictException("A customer tag with that name already exists")
            throw error
        }
    }

    override fun updateCustomerTag(
        clientId: String,
        actorUserId: String,
        tagId: String,
        input: UpdateBusinessCustomerTagInput,
    ): BusinessCustomerTag = transaction { connection ->
        try {
            val id = connection.prepareStatement(
                """
                UPDATE business_sales_customer_tags SET name=?, color_hex=?, version=version+1
                WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, input.name)
                statement.setString(2, input.colorHex)
                statement.setString(3, clientId)
                statement.setString(4, tagId)
                statement.setInt(5, input.expectedVersion)
                statement.executeQuery().use { result ->
                    if (!result.next()) staleOrNotFound(connection, "business_sales_customer_tags", clientId, tagId)
                    result.getString(1)
                }
            }
            audit(connection, clientId, actorUserId, "CUSTOMER_TAG", id, "UPDATED", input.expectedVersion + 1)
            requireNotNull(customerTag(connection, clientId, id))
        } catch (error: SQLException) {
            if (error.sqlState == "23505") throw SalesProgramConflictException("A customer tag with that name already exists")
            throw error
        }
    }

    override fun archiveCustomerTag(
        clientId: String,
        actorUserId: String,
        tagId: String,
        expectedVersion: Int,
    ): BusinessCustomerTag = transaction { connection ->
        val id = connection.prepareStatement(
            """
            UPDATE business_sales_customer_tags SET is_archived=TRUE, version=version+1
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, tagId)
            statement.setInt(3, expectedVersion)
            statement.executeQuery().use { result ->
                if (!result.next()) staleOrNotFound(connection, "business_sales_customer_tags", clientId, tagId)
                result.getString(1)
            }
        }
        audit(connection, clientId, actorUserId, "CUSTOMER_TAG", id, "ARCHIVED", expectedVersion + 1)
        requireNotNull(customerTag(connection, clientId, id))
    }

    override fun customerActivities(clientId: String, customerId: String): List<BusinessCustomerActivity> =
        dataSource.withConnection { connection ->
            requireCustomer(connection, clientId, customerId)
            customerActivitiesFor(connection, clientId, customerId)
        }

    override fun createCustomerActivity(
        clientId: String,
        actorUserId: String,
        customerId: String,
        input: CreateBusinessCustomerActivityInput,
    ): BusinessCustomerActivity = transaction { connection ->
        requireActiveCustomer(connection, clientId, customerId)
        requireContact(connection, clientId, customerId, input.contactId)
        val id = connection.prepareStatement(
            """
            INSERT INTO business_sales_customer_activities (
                client_id, customer_id, contact_id, activity_type, subject, details,
                occurred_at, due_at, created_by_user_id
            ) VALUES (
                CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?,
                CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS uuid)
            )
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, customerId)
            statement.nullableUuid(3, input.contactId)
            statement.setString(4, input.type.name)
            statement.setString(5, input.subject)
            statement.setString(6, input.details)
            statement.string(7, input.occurredAt)
            statement.string(8, input.dueAt)
            statement.setString(9, actorUserId)
            statement.executeQuery().use { result -> result.next(); result.getString(1) }
        }
        audit(connection, clientId, actorUserId, "CUSTOMER_ACTIVITY", id, "CREATED", 1)
        requireNotNull(customerActivity(connection, clientId, customerId, id))
    }

    override fun updateCustomerActivity(
        clientId: String,
        actorUserId: String,
        customerId: String,
        activityId: String,
        input: UpdateBusinessCustomerActivityInput,
    ): BusinessCustomerActivity = transaction { connection ->
        requireCustomer(connection, clientId, customerId)
        requireContact(connection, clientId, customerId, input.contactId)
        val id = connection.prepareStatement(
            """
            UPDATE business_sales_customer_activities
            SET contact_id=CAST(? AS uuid), activity_type=?, subject=?, details=?,
                occurred_at=CAST(? AS timestamptz), due_at=CAST(? AS timestamptz),
                completed_at=CASE WHEN ? THEN COALESCE(completed_at, NOW()) ELSE NULL END,
                version=version+1
            WHERE client_id=CAST(? AS uuid) AND customer_id=CAST(? AS uuid)
              AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.nullableUuid(1, input.contactId)
            statement.setString(2, input.type.name)
            statement.setString(3, input.subject)
            statement.setString(4, input.details)
            statement.string(5, input.occurredAt)
            statement.string(6, input.dueAt)
            statement.setBoolean(7, input.completed)
            statement.setString(8, clientId)
            statement.setString(9, customerId)
            statement.setString(10, activityId)
            statement.setInt(11, input.expectedVersion)
            statement.executeQuery().use { result ->
                if (!result.next()) staleOrNotFound(connection, "business_sales_customer_activities", clientId, activityId)
                result.getString(1)
            }
        }
        audit(connection, clientId, actorUserId, "CUSTOMER_ACTIVITY", id, "UPDATED", input.expectedVersion + 1)
        requireNotNull(customerActivity(connection, clientId, customerId, id))
    }

    override fun catalogItems(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCatalogItem> = dataSource.withConnection { connection ->
        val status = request.status
        connection.prepareStatement(
            """
            SELECT id, item_type, name, sku, description, unit, reference_price_cents, currency, tracks_stock, stock_minimum_milli, is_archived, version, updated_at
            FROM business_catalog_items
            WHERE client_id=?
              AND (? IS NULL OR (? = 'ACTIVE' AND is_archived=FALSE) OR (? = 'ARCHIVED' AND is_archived=TRUE) OR item_type=?)
              AND (? IS NULL OR name ILIKE '%' || ? || '%' OR sku ILIKE '%' || ? || '%')
              AND (? IS NULL OR id > ?)
            ORDER BY id LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.string(2, status); statement.string(3, status); statement.string(4, status); statement.string(5, status)
            statement.string(6, request.query); statement.string(7, request.query); statement.string(8, request.query); statement.uuid(9, request.cursor); statement.uuid(10, request.cursor); statement.setInt(11, request.limit + 1)
            statement.executeQuery().use { page(it.map(::catalogItem), request.limit) { item -> item.id } }
        }
    }

    override fun createCatalogItem(clientId: String, actorUserId: String, input: CreateBusinessCatalogItemInput): BusinessCatalogItem = transaction { connection ->
        BusinessSalesRules.validateCatalogItem(input)
        connection.prepareStatement(
            """
            INSERT INTO business_catalog_items (client_id, item_type, name, sku, description, unit, reference_price_cents, tracks_stock, stock_minimum_milli, created_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, item_type, name, sku, description, unit, reference_price_cents, currency, tracks_stock, stock_minimum_milli, is_archived, version, updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.setString(2, input.type.name); statement.setString(3, input.name); statement.setString(4, input.sku); statement.setString(5, input.description)
            statement.setString(6, input.unit); statement.setLong(7, input.referencePriceCents); statement.setBoolean(8, input.tracksStock); statement.longOrNull(9, input.stockMinimumMilli); statement.uuid(10, actorUserId)
            statement.executeQuery().use { rs -> rs.next(); catalogItem(rs).also { audit(connection, clientId, actorUserId, "CATALOG_ITEM", it.id, "CREATED", it.version) } }
        }
    }

    override fun updateCatalogItem(clientId: String, actorUserId: String, itemId: String, input: UpdateBusinessCatalogItemInput): BusinessCatalogItem = transaction { connection ->
        val current = catalogItemForUpdate(connection, clientId, itemId)
        if (current.version != input.expectedVersion) throw SalesProgramConflictException("This record was changed by another session")
        BusinessSalesRules.validateCatalogItem(input, current.type)
        if (current.tracksStock && !input.tracksStock && hasMovements(connection, clientId, itemId)) throw SalesProgramConflictException("Stock tracking cannot be disabled after movements exist")
        connection.prepareStatement(
            """
            UPDATE business_catalog_items
            SET name=?, sku=?, description=?, unit=?, reference_price_cents=?, tracks_stock=?, stock_minimum_milli=?, version=version+1
            WHERE client_id=? AND id=? AND version=?
            RETURNING id, item_type, name, sku, description, unit, reference_price_cents, currency, tracks_stock, stock_minimum_milli, is_archived, version, updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, input.name); statement.setString(2, input.sku); statement.setString(3, input.description); statement.setString(4, input.unit)
            statement.setLong(5, input.referencePriceCents); statement.setBoolean(6, input.tracksStock); statement.longOrNull(7, input.stockMinimumMilli)
            statement.uuid(8, clientId); statement.uuid(9, itemId); statement.setInt(10, input.expectedVersion)
            statement.executeQuery().use { rs -> rs.next(); catalogItem(rs).also { audit(connection, clientId, actorUserId, "CATALOG_ITEM", it.id, "UPDATED", it.version) } }
        }
    }

    override fun archiveCatalogItem(clientId: String, actorUserId: String, itemId: String, expectedVersion: Int): BusinessCatalogItem = transaction { connection ->
        connection.prepareStatement(
            """
            UPDATE business_catalog_items SET is_archived=TRUE, version=version+1
            WHERE client_id=? AND id=? AND version=?
            RETURNING id, item_type, name, sku, description, unit, reference_price_cents, currency, tracks_stock, stock_minimum_milli, is_archived, version, updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.uuid(2, itemId); statement.setInt(3, expectedVersion)
            statement.executeQuery().use { rs -> if (!rs.next()) staleOrNotFound(connection, "business_catalog_items", clientId, itemId); catalogItem(rs).also { audit(connection, clientId, actorUserId, "CATALOG_ITEM", it.id, "ARCHIVED", it.version) } }
        }
    }

    override fun stock(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockSummary> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT item.id, item.item_type, item.name, item.sku, item.description, item.unit, item.reference_price_cents, item.currency,
                   item.tracks_stock, item.stock_minimum_milli, item.is_archived, item.version, item.updated_at, summary.available_milli
            FROM business_catalog_items item
            JOIN business_catalog_stock_summary summary ON summary.client_id=item.client_id AND summary.item_id=item.id
            WHERE item.client_id=? AND item.item_type='PRODUCT' AND item.tracks_stock=TRUE
              AND (? IS NULL OR item.name ILIKE '%' || ? || '%')
              AND (? <> 'LOW' OR item.stock_minimum_milli IS NOT NULL AND summary.available_milli < item.stock_minimum_milli)
              AND (? IS NULL OR item.id > ?)
            ORDER BY item.id LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.string(2, request.query); statement.string(3, request.query); statement.string(4, request.status); statement.uuid(5, request.cursor); statement.uuid(6, request.cursor); statement.setInt(7, request.limit + 1)
            statement.executeQuery().use { page(it.map(::stockSummary), request.limit) { summary -> summary.item.id } }
        }
    }

    override fun stockMovements(clientId: String, itemId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockMovement> = dataSource.withConnection { connection ->
        requireStockItem(connection, clientId, itemId, lock = false)
        connection.prepareStatement(
            """
            SELECT id, item_id, movement_type, delta_milli, reason, reference_id, created_at
            FROM business_stock_movements WHERE client_id=? AND item_id=? AND (? IS NULL OR id > ?)
            ORDER BY id LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.uuid(2, itemId); statement.uuid(3, request.cursor); statement.uuid(4, request.cursor); statement.setInt(5, request.limit + 1)
            statement.executeQuery().use { page(it.map(::stockMovement), request.limit) { movement -> movement.id } }
        }
    }

    override fun adjustStock(clientId: String, actorUserId: String, itemId: String, input: StockAdjustmentInput): BusinessStockMovement = transaction { connection ->
        existingAdjustment(connection, clientId, input.idempotencyKey)?.let { return@transaction it }
        requireStockItem(connection, clientId, itemId, lock = true)
        val available = availableStock(connection, clientId, itemId)
        if (available + input.deltaMilli < 0) throw SalesProgramConflictException("Insufficient stock for this adjustment")
        try {
            connection.prepareStatement(
                """
                INSERT INTO business_stock_movements (client_id, item_id, movement_type, delta_milli, reason, idempotency_key, created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id, item_id, movement_type, delta_milli, reason, reference_id, created_at
                """.trimIndent(),
            ).use { statement ->
                statement.uuid(1, clientId); statement.uuid(2, itemId); statement.setString(3, input.type.name); statement.setLong(4, input.deltaMilli)
                statement.setString(5, input.reason); statement.setString(6, input.idempotencyKey); statement.uuid(7, actorUserId)
                statement.executeQuery().use { rs -> rs.next(); stockMovement(rs).also { audit(connection, clientId, actorUserId, "STOCK_MOVEMENT", it.id, "CREATED", null) } }
            }
        } catch (error: SQLException) {
            if (error.sqlState == UNIQUE_VIOLATION) existingAdjustment(connection, clientId, input.idempotencyKey) ?: throw error else throw error
        }
    }

    override fun quotes(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessQuote> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, quote_number, customer_id, buyer_name, buyer_email, buyer_phone, status, issue_date, valid_until, notes, currency,
                   subtotal_cents, tax_cents, total_cents, version, updated_at
            FROM business_sales_quotes
            WHERE client_id=? AND (? IS NULL OR status=?) AND (? IS NULL OR quote_number ILIKE '%' || ? || '%' OR buyer_name ILIKE '%' || ? || '%')
              AND (? IS NULL OR id > ?)
            ORDER BY id LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.string(2, request.status); statement.string(3, request.status); statement.string(4, request.query); statement.string(5, request.query); statement.string(6, request.query)
            statement.uuid(7, request.cursor); statement.uuid(8, request.cursor); statement.setInt(9, request.limit + 1)
            statement.executeQuery().use { page(it.map(::quoteHeader), request.limit) { quote -> quote.id } }
        }
    }

    override fun quote(clientId: String, quoteId: String): BusinessQuote? = dataSource.withConnection { connection -> quote(connection, clientId, quoteId, lock = false) }

    override fun createQuote(clientId: String, actorUserId: String, input: CalculatedBusinessQuoteInput): BusinessQuote = transaction { connection ->
        existingQuoteForKey(connection, clientId, input.source.idempotencyKey)?.let { return@transaction it }
        input.source.customerId?.let { requireActiveCustomer(connection, clientId, it) }
        input.lines.forEach { line -> line.sourceCatalogItemId?.let { requireCatalogItem(connection, clientId, it, lock = false) } }
        val quoteId = connection.prepareStatement(
            """
            INSERT INTO business_sales_quotes (client_id, quote_number, customer_id, buyer_name, buyer_email, buyer_phone, status, issue_date, valid_until, notes,
                                               subtotal_cents, tax_cents, total_cents, create_idempotency_key, created_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', CAST(? AS date), CAST(? AS date), ?, ?, ?, ?, ?, ?)
            ON CONFLICT (client_id, create_idempotency_key) DO NOTHING
            RETURNING id
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.setString(2, nextDocumentNumber(connection, clientId, quote = true)); statement.uuid(3, input.source.customerId)
            statement.setString(4, input.source.buyerName); statement.setString(5, input.source.buyerEmail); statement.setString(6, input.source.buyerPhone)
            statement.setString(7, input.source.issueDate); statement.setString(8, input.source.validUntil); statement.setString(9, input.source.notes)
            statement.setLong(10, input.subtotalCents); statement.setLong(11, input.taxCents); statement.setLong(12, input.totalCents); statement.setString(13, input.source.idempotencyKey); statement.uuid(14, actorUserId)
            statement.executeQuery().use { rs -> if (rs.next()) rs.id("id") else null }
        } ?: return@transaction requireNotNull(existingQuoteForKey(connection, clientId, input.source.idempotencyKey))
        insertQuoteLines(connection, clientId, quoteId, input.lines)
        quote(connection, clientId, quoteId, lock = false)!!.also { audit(connection, clientId, actorUserId, "QUOTE", it.id, "CREATED", it.version) }
    }

    override fun updateQuote(clientId: String, actorUserId: String, quoteId: String, input: CalculatedBusinessQuoteUpdate): BusinessQuote = transaction { connection ->
        val current = requireNotNull(quote(connection, clientId, quoteId, lock = true)) { "Quote not found" }
        if (current.version != input.source.expectedVersion) throw SalesProgramConflictException("This record was changed by another session")
        if (current.status != BusinessQuoteStatus.DRAFT) throw SalesProgramConflictException("Only draft quotes can be edited")
        input.source.customerId?.let { requireActiveCustomer(connection, clientId, it) }
        input.lines.forEach { line -> line.sourceCatalogItemId?.let { requireCatalogItem(connection, clientId, it, lock = false) } }
        connection.prepareStatement(
            """
            UPDATE business_sales_quotes
            SET customer_id=?, buyer_name=?, buyer_email=?, buyer_phone=?, issue_date=CAST(? AS date), valid_until=CAST(? AS date), notes=?,
                subtotal_cents=?, tax_cents=?, total_cents=?, version=version+1
            WHERE client_id=? AND id=? AND version=?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, input.source.customerId); statement.setString(2, input.source.buyerName); statement.setString(3, input.source.buyerEmail); statement.setString(4, input.source.buyerPhone)
            statement.setString(5, input.source.issueDate); statement.setString(6, input.source.validUntil); statement.setString(7, input.source.notes); statement.setLong(8, input.subtotalCents); statement.setLong(9, input.taxCents); statement.setLong(10, input.totalCents)
            statement.uuid(11, clientId); statement.uuid(12, quoteId); statement.setInt(13, input.source.expectedVersion)
            if (statement.executeUpdate() != 1) throw SalesProgramConflictException("This record was changed by another session")
        }
        connection.prepareStatement("DELETE FROM business_sales_quote_lines WHERE client_id=? AND quote_id=?").use { statement -> statement.uuid(1, clientId); statement.uuid(2, quoteId); statement.executeUpdate() }
        insertQuoteLines(connection, clientId, quoteId, input.lines)
        quote(connection, clientId, quoteId, lock = false)!!.also { audit(connection, clientId, actorUserId, "QUOTE", it.id, "UPDATED", it.version) }
    }

    override fun transitionQuote(clientId: String, actorUserId: String, quoteId: String, target: BusinessQuoteStatus, expectedVersion: Int): BusinessQuote = transaction { connection ->
        val current = quote(connection, clientId, quoteId, lock = true) ?: throw SalesProgramNotFoundException()
        if (current.version != expectedVersion) throw SalesProgramConflictException("This record was changed by another session")
        if (!validTransition(current.status, target)) throw SalesProgramConflictException("This quote transition is not allowed")
        val timestampField = when (target) {
            BusinessQuoteStatus.SENT -> "sent_at"
            BusinessQuoteStatus.ACCEPTED -> "accepted_at"
            BusinessQuoteStatus.REJECTED -> "rejected_at"
            BusinessQuoteStatus.EXPIRED -> "expired_at"
            BusinessQuoteStatus.DRAFT -> throw SalesProgramConflictException("Draft is not a transition target")
        }
        connection.prepareStatement("UPDATE business_sales_quotes SET status=?, $timestampField=NOW(), version=version+1 WHERE client_id=? AND id=? AND version=?").use { statement ->
            statement.setString(1, target.name); statement.uuid(2, clientId); statement.uuid(3, quoteId); statement.setInt(4, expectedVersion)
            if (statement.executeUpdate() != 1) throw SalesProgramConflictException("This record was changed by another session")
        }
        quote(connection, clientId, quoteId, lock = false)!!.also { audit(connection, clientId, actorUserId, "QUOTE", it.id, target.name, it.version) }
    }

    override fun convertQuote(clientId: String, actorUserId: String, quoteId: String, input: ConvertBusinessQuoteInput): BusinessSale = transaction { connection ->
        saleForQuote(connection, clientId, quoteId)?.let { return@transaction it }
        val quote = quote(connection, clientId, quoteId, lock = true) ?: throw SalesProgramNotFoundException()
        if (quote.status != BusinessQuoteStatus.ACCEPTED) throw SalesProgramConflictException("Only accepted quotes can become sales")
        val stockByItem = quote.lines.mapNotNull { line -> line.sourceCatalogItemId?.let { it to line.quantityMilli } }.groupBy({ it.first }, { it.second })
            .mapValues { (_, quantities) -> quantities.sum() }
        stockByItem.keys.sorted().forEach { itemId -> requireStockItem(connection, clientId, itemId, lock = true) }
        stockByItem.forEach { (itemId, quantity) -> if (availableStock(connection, clientId, itemId) < quantity) throw SalesProgramConflictException("Insufficient stock to convert this quote") }
        val saleId = connection.prepareStatement(
            """
            INSERT INTO business_sales_sales (client_id, sale_number, quote_id, buyer_name, subtotal_cents, tax_cents, total_cents, conversion_idempotency_key, confirmed_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.setString(2, nextDocumentNumber(connection, clientId, quote = false)); statement.uuid(3, quoteId); statement.setString(4, quote.buyerName)
            statement.setLong(5, quote.subtotalCents); statement.setLong(6, quote.taxCents); statement.setLong(7, quote.totalCents); statement.setString(8, input.idempotencyKey); statement.uuid(9, actorUserId)
            statement.executeQuery().use { rs -> rs.next(); rs.id("id") }
        }
        insertSaleLines(connection, clientId, saleId, quote.lines)
        stockByItem.forEach { (itemId, quantity) -> insertSaleMovement(connection, clientId, itemId, saleId, quantity, actorUserId) }
        sale(connection, clientId, saleId, lock = false)!!.also { audit(connection, clientId, actorUserId, "SALE", it.id, "CONFIRMED", null) }
    }

    override fun sales(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessSale> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, sale_number, quote_id, buyer_name, currency, subtotal_cents, tax_cents, total_cents, status, confirmed_at
            FROM business_sales_sales
            WHERE client_id=? AND (? IS NULL OR status=?) AND (? IS NULL OR sale_number ILIKE '%' || ? || '%' OR buyer_name ILIKE '%' || ? || '%')
              AND (? IS NULL OR id > ?)
            ORDER BY id LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.uuid(1, clientId); statement.string(2, request.status); statement.string(3, request.status); statement.string(4, request.query); statement.string(5, request.query); statement.string(6, request.query)
            statement.uuid(7, request.cursor); statement.uuid(8, request.cursor); statement.setInt(9, request.limit + 1)
            statement.executeQuery().use { page(it.map(::saleHeader), request.limit) { sale -> sale.id } }
        }
    }

    override fun sale(clientId: String, saleId: String): BusinessSale? = dataSource.withConnection { connection -> sale(connection, clientId, saleId, lock = false) }

    private fun quote(connection: Connection, clientId: String, quoteId: String, lock: Boolean): BusinessQuote? = connection.prepareStatement(
        """
        SELECT id, quote_number, customer_id, buyer_name, buyer_email, buyer_phone, status, issue_date, valid_until, notes, currency,
               subtotal_cents, tax_cents, total_cents, version, updated_at
        FROM business_sales_quotes WHERE client_id=? AND id=? ${if (lock) "FOR UPDATE" else ""}
        """.trimIndent(),
    ).use { statement ->
        statement.uuid(1, clientId); statement.uuid(2, quoteId); statement.executeQuery().use { rs ->
            if (!rs.next()) null else quoteHeader(rs).let { header -> header.copy(lines = quoteLines(connection, clientId, header.id)) }
        }
    }

    private fun sale(connection: Connection, clientId: String, saleId: String, lock: Boolean): BusinessSale? = connection.prepareStatement(
        "SELECT id, sale_number, quote_id, buyer_name, currency, subtotal_cents, tax_cents, total_cents, status, confirmed_at FROM business_sales_sales WHERE client_id=? AND id=? ${if (lock) "FOR UPDATE" else ""}",
    ).use { statement ->
        statement.uuid(1, clientId); statement.uuid(2, saleId); statement.executeQuery().use { rs -> if (!rs.next()) null else saleHeader(rs).let { header -> header.copy(lines = saleLines(connection, clientId, header.id)) } }
    }

    private fun existingQuoteForKey(connection: Connection, clientId: String, key: String): BusinessQuote? = connection.prepareStatement(
        "SELECT id FROM business_sales_quotes WHERE client_id=? AND create_idempotency_key=?",
    ).use { statement -> statement.uuid(1, clientId); statement.setString(2, key); statement.executeQuery().use { rs -> if (rs.next()) quote(connection, clientId, rs.id("id"), false) else null } }

    private fun saleForQuote(connection: Connection, clientId: String, quoteId: String): BusinessSale? = connection.prepareStatement(
        "SELECT id FROM business_sales_sales WHERE client_id=? AND quote_id=?",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, quoteId); statement.executeQuery().use { rs -> if (rs.next()) sale(connection, clientId, rs.id("id"), false) else null } }

    private fun insertQuoteLines(connection: Connection, clientId: String, quoteId: String, lines: List<BusinessQuoteLine>) {
        connection.prepareStatement(
            """
            INSERT INTO business_sales_quote_lines (client_id, quote_id, position, source_catalog_item_id, description, quantity_milli, unit_price_cents,
                                                    discount_basis_points, tax_basis_points, subtotal_cents, tax_cents, total_cents)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            lines.forEach { line ->
                statement.uuid(1, clientId); statement.uuid(2, quoteId); statement.setInt(3, line.position); statement.uuid(4, line.sourceCatalogItemId); statement.setString(5, line.description)
                statement.setLong(6, line.quantityMilli); statement.setLong(7, line.unitPriceCents); statement.setInt(8, line.discountBasisPoints); statement.setInt(9, line.taxBasisPoints)
                statement.setLong(10, line.subtotalCents); statement.setLong(11, line.taxCents); statement.setLong(12, line.totalCents); statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertSaleLines(connection: Connection, clientId: String, saleId: String, lines: List<BusinessQuoteLine>) {
        connection.prepareStatement(
            """
            INSERT INTO business_sales_sale_lines (client_id, sale_id, position, source_catalog_item_id, description, quantity_milli, unit_price_cents,
                                                   discount_basis_points, tax_basis_points, subtotal_cents, tax_cents, total_cents)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            lines.forEach { line ->
                statement.uuid(1, clientId); statement.uuid(2, saleId); statement.setInt(3, line.position); statement.uuid(4, line.sourceCatalogItemId); statement.setString(5, line.description)
                statement.setLong(6, line.quantityMilli); statement.setLong(7, line.unitPriceCents); statement.setInt(8, line.discountBasisPoints); statement.setInt(9, line.taxBasisPoints)
                statement.setLong(10, line.subtotalCents); statement.setLong(11, line.taxCents); statement.setLong(12, line.totalCents); statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertSaleMovement(connection: Connection, clientId: String, itemId: String, saleId: String, quantityMilli: Long, actorUserId: String) {
        connection.prepareStatement(
            "INSERT INTO business_stock_movements (client_id, item_id, movement_type, delta_milli, reason, reference_id, created_by_user_id) VALUES (?, ?, 'SALE', ?, 'Sale conversion', ?, ?)",
        ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, itemId); statement.setLong(3, -quantityMilli); statement.uuid(4, saleId); statement.uuid(5, actorUserId); statement.executeUpdate() }
    }

    private fun quoteLines(connection: Connection, clientId: String, quoteId: String): List<BusinessQuoteLine> = connection.prepareStatement(
        "SELECT id, position, source_catalog_item_id, description, quantity_milli, unit_price_cents, discount_basis_points, tax_basis_points, subtotal_cents, tax_cents, total_cents FROM business_sales_quote_lines WHERE client_id=? AND quote_id=? ORDER BY position",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, quoteId); statement.executeQuery().use { it.map(::quoteLine) } }

    private fun saleLines(connection: Connection, clientId: String, saleId: String): List<BusinessQuoteLine> = connection.prepareStatement(
        "SELECT id, position, source_catalog_item_id, description, quantity_milli, unit_price_cents, discount_basis_points, tax_basis_points, subtotal_cents, tax_cents, total_cents FROM business_sales_sale_lines WHERE client_id=? AND sale_id=? ORDER BY position",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, saleId); statement.executeQuery().use { it.map(::quoteLine) } }

    private fun hydrateCustomer(connection: Connection, clientId: String, value: BusinessCustomer): BusinessCustomer {
        val activities = customerActivitiesFor(connection, clientId, value.id)
        return value.copy(
            tags = customerTagsFor(connection, clientId, value.id),
            nextAction = nextAction(activities),
        )
    }

    private fun customerTagsFor(connection: Connection, clientId: String, customerId: String): List<BusinessCustomerTag> =
        connection.prepareStatement(
            """
            SELECT tag.id::text AS id, tag.name, tag.color_hex, tag.is_archived,
                   tag.version, tag.updated_at::text AS updated_at
            FROM business_sales_customer_tag_links link
            JOIN business_sales_customer_tags tag
              ON tag.id=link.tag_id AND tag.client_id=link.client_id
            WHERE link.client_id=CAST(? AS uuid) AND link.customer_id=CAST(? AS uuid)
            ORDER BY lower(tag.name), tag.id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, customerId)
            statement.executeQuery().use { it.map(::customerTag) }
        }

    private fun customerTag(connection: Connection, clientId: String, tagId: String): BusinessCustomerTag? =
        connection.prepareStatement(
            """
            SELECT id::text AS id, name, color_hex, is_archived, version, updated_at::text AS updated_at
            FROM business_sales_customer_tags
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, tagId)
            statement.executeQuery().use { result -> if (result.next()) customerTag(result) else null }
        }

    private fun customerActivitiesFor(
        connection: Connection,
        clientId: String,
        customerId: String,
    ): List<BusinessCustomerActivity> = connection.prepareStatement(
        """
        SELECT activity.id::text AS id, activity.customer_id::text AS customer_id,
               activity.contact_id::text AS contact_id, contact.full_name AS contact_name,
               activity.activity_type, activity.subject, activity.details,
               activity.occurred_at::text AS occurred_at, activity.due_at::text AS due_at,
               activity.completed_at::text AS completed_at, activity.version,
               activity.created_at::text AS created_at, activity.updated_at::text AS updated_at
        FROM business_sales_customer_activities activity
        LEFT JOIN business_sales_contacts contact
          ON contact.id=activity.contact_id AND contact.client_id=activity.client_id
        WHERE activity.client_id=CAST(? AS uuid) AND activity.customer_id=CAST(? AS uuid)
        ORDER BY activity.created_at DESC, activity.id
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, clientId)
        statement.setString(2, customerId)
        statement.executeQuery().use { it.map(::customerActivity) }
    }

    private fun customerActivity(
        connection: Connection,
        clientId: String,
        customerId: String,
        activityId: String,
    ): BusinessCustomerActivity? = connection.prepareStatement(
        """
        SELECT activity.id::text AS id, activity.customer_id::text AS customer_id,
               activity.contact_id::text AS contact_id, contact.full_name AS contact_name,
               activity.activity_type, activity.subject, activity.details,
               activity.occurred_at::text AS occurred_at, activity.due_at::text AS due_at,
               activity.completed_at::text AS completed_at, activity.version,
               activity.created_at::text AS created_at, activity.updated_at::text AS updated_at
        FROM business_sales_customer_activities activity
        LEFT JOIN business_sales_contacts contact
          ON contact.id=activity.contact_id AND contact.client_id=activity.client_id
        WHERE activity.client_id=CAST(? AS uuid) AND activity.customer_id=CAST(? AS uuid)
          AND activity.id=CAST(? AS uuid)
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, clientId)
        statement.setString(2, customerId)
        statement.setString(3, activityId)
        statement.executeQuery().use { result -> if (result.next()) customerActivity(result) else null }
    }

    private fun nextAction(activities: List<BusinessCustomerActivity>): BusinessCustomerActivity? =
        activities.filter { it.completedAt == null && it.dueAt != null }.minByOrNull { requireNotNull(it.dueAt) }

    private fun requireCustomerTags(connection: Connection, clientId: String, tagIds: List<String>) {
        tagIds.forEach { tagId ->
            val tag = customerTag(connection, clientId, tagId) ?: throw SalesProgramNotFoundException()
            if (tag.archived) throw SalesProgramConflictException("Archived tags cannot be assigned")
        }
    }

    private fun replaceCustomerTags(
        connection: Connection,
        clientId: String,
        actorUserId: String,
        customerId: String,
        tagIds: List<String>,
    ) {
        connection.prepareStatement(
            "DELETE FROM business_sales_customer_tag_links WHERE client_id=CAST(? AS uuid) AND customer_id=CAST(? AS uuid)",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, customerId)
            statement.executeUpdate()
        }
        tagIds.forEach { tagId ->
            connection.prepareStatement(
                """
                INSERT INTO business_sales_customer_tag_links (client_id, customer_id, tag_id, created_by_user_id)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, customerId)
                statement.setString(3, tagId)
                statement.setString(4, actorUserId)
                statement.executeUpdate()
            }
        }
    }

    private fun requireCustomer(connection: Connection, clientId: String, customerId: String) {
        val exists = connection.prepareStatement("SELECT 1 FROM business_sales_customers WHERE client_id=? AND id=?").use { statement -> statement.uuid(1, clientId); statement.uuid(2, customerId); statement.executeQuery().use { it.next() } }
        if (!exists) throw SalesProgramNotFoundException()
    }

    private fun requireActiveCustomer(connection: Connection, clientId: String, customerId: String) {
        val active = connection.prepareStatement("SELECT 1 FROM business_sales_customers WHERE client_id=? AND id=? AND status='ACTIVE'").use { statement -> statement.uuid(1, clientId); statement.uuid(2, customerId); statement.executeQuery().use { it.next() } }
        if (!active) throw SalesProgramConflictException("Customer is archived or not found")
    }

    private fun requireContact(connection: Connection, clientId: String, customerId: String, contactId: String?) {
        if (contactId == null) return
        val exists = connection.prepareStatement(
            """
            SELECT 1 FROM business_sales_contacts
            WHERE client_id=CAST(? AS uuid) AND customer_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, customerId)
            statement.setString(3, contactId)
            statement.executeQuery().use { it.next() }
        }
        if (!exists) throw SalesProgramNotFoundException()
    }

    private fun requireCatalogItem(connection: Connection, clientId: String, itemId: String, lock: Boolean): BusinessCatalogItem = catalogItemFor(connection, clientId, itemId, lock) ?: throw SalesProgramNotFoundException()

    private fun requireStockItem(connection: Connection, clientId: String, itemId: String, lock: Boolean): BusinessCatalogItem {
        val item = requireCatalogItem(connection, clientId, itemId, lock)
        if (item.type != BusinessCatalogItemType.PRODUCT || !item.tracksStock || item.archived) throw SalesProgramConflictException("This item does not support stock movements")
        return item
    }

    private fun catalogItemForUpdate(connection: Connection, clientId: String, itemId: String): BusinessCatalogItem =
        catalogItemFor(connection, clientId, itemId, lock = true) ?: throw SalesProgramNotFoundException()

    private fun catalogItemFor(connection: Connection, clientId: String, itemId: String, lock: Boolean): BusinessCatalogItem? = connection.prepareStatement(
        "SELECT id, item_type, name, sku, description, unit, reference_price_cents, currency, tracks_stock, stock_minimum_milli, is_archived, version, updated_at FROM business_catalog_items WHERE client_id=? AND id=? ${if (lock) "FOR UPDATE" else ""}",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, itemId); statement.executeQuery().use { rs -> if (rs.next()) catalogItem(rs) else null } }

    private fun clearPrimaryContact(connection: Connection, clientId: String, customerId: String, exceptContactId: String?) {
        connection.prepareStatement(
            "UPDATE business_sales_contacts SET is_primary=FALSE, version=version+1 WHERE client_id=? AND customer_id=? AND is_primary=TRUE AND (? IS NULL OR id <> ?)",
        ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, customerId); statement.uuid(3, exceptContactId); statement.uuid(4, exceptContactId); statement.executeUpdate() }
    }

    private fun existingAdjustment(connection: Connection, clientId: String, key: String): BusinessStockMovement? = connection.prepareStatement(
        "SELECT id, item_id, movement_type, delta_milli, reason, reference_id, created_at FROM business_stock_movements WHERE client_id=? AND idempotency_key=?",
    ).use { statement -> statement.uuid(1, clientId); statement.setString(2, key); statement.executeQuery().use { rs -> if (rs.next()) stockMovement(rs) else null } }

    private fun hasMovements(connection: Connection, clientId: String, itemId: String): Boolean = connection.prepareStatement(
        "SELECT 1 FROM business_stock_movements WHERE client_id=? AND item_id=? LIMIT 1",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, itemId); statement.executeQuery().use { it.next() } }

    private fun availableStock(connection: Connection, clientId: String, itemId: String): Long = connection.prepareStatement(
        "SELECT COALESCE(SUM(delta_milli), 0) FROM business_stock_movements WHERE client_id=? AND item_id=?",
    ).use { statement -> statement.uuid(1, clientId); statement.uuid(2, itemId); statement.executeQuery().use { rs -> rs.next(); rs.getLong(1) } }

    private fun nextDocumentNumber(connection: Connection, clientId: String, quote: Boolean): String {
        connection.prepareStatement("INSERT INTO business_sales_document_counters (client_id) VALUES (?) ON CONFLICT (client_id) DO NOTHING").use { statement -> statement.uuid(1, clientId); statement.executeUpdate() }
        val column = if (quote) "next_quote_number" else "next_sale_number"
        val number = connection.prepareStatement("UPDATE business_sales_document_counters SET $column=$column+1 WHERE client_id=? RETURNING $column-1").use { statement -> statement.uuid(1, clientId); statement.executeQuery().use { rs -> rs.next(); rs.getInt(1) } }
        return (if (quote) "Q-" else "S-") + number.toString().padStart(6, '0')
    }

    private fun validTransition(from: BusinessQuoteStatus, to: BusinessQuoteStatus): Boolean = when (from) {
        BusinessQuoteStatus.DRAFT -> to == BusinessQuoteStatus.SENT || to == BusinessQuoteStatus.EXPIRED
        BusinessQuoteStatus.SENT -> to in setOf(BusinessQuoteStatus.ACCEPTED, BusinessQuoteStatus.REJECTED, BusinessQuoteStatus.EXPIRED)
        else -> false
    }

    private fun staleOrNotFound(connection: Connection, table: String, clientId: String, id: String): Nothing {
        val exists = connection.prepareStatement("SELECT 1 FROM $table WHERE client_id=? AND id=?").use { statement -> statement.uuid(1, clientId); statement.uuid(2, id); statement.executeQuery().use { it.next() } }
        if (exists) throw SalesProgramConflictException("This record was changed by another session") else throw SalesProgramNotFoundException()
    }

    private fun audit(connection: Connection, clientId: String, actorUserId: String, entityType: String, entityId: String, action: String, version: Int?) {
        connection.prepareStatement("INSERT INTO business_sales_audit_events (client_id, entity_type, entity_id, action, actor_user_id, version) VALUES (?, ?, ?, ?, ?, ?)").use { statement ->
            statement.uuid(1, clientId); statement.setString(2, entityType); statement.uuid(3, entityId); statement.setString(4, action); statement.uuid(5, actorUserId)
            if (version == null) statement.setNull(6, Types.INTEGER) else statement.setInt(6, version); statement.executeUpdate()
        }
    }

    private fun customer(rs: ResultSet) = BusinessCustomer(
        id = rs.id("id"),
        displayName = rs.getString("display_name"),
        taxId = rs.getString("tax_id"),
        email = rs.getString("email"),
        phone = rs.getString("phone"),
        address = rs.getString("address"),
        status = BusinessCustomerStatus.valueOf(rs.getString("status")),
        version = rs.getInt("version"),
        updatedAt = rs.time("updated_at"),
        type = BusinessCustomerType.valueOf(rs.getString("customer_type")),
        stage = BusinessCustomerStage.valueOf(rs.getString("commercial_stage")),
        source = rs.getString("commercial_source"),
    )
    private fun customerTag(rs: ResultSet) = BusinessCustomerTag(
        rs.getString("id"), rs.getString("name"), rs.getString("color_hex"),
        rs.getBoolean("is_archived"), rs.getInt("version"), rs.getString("updated_at"),
    )
    private fun customerActivity(rs: ResultSet) = BusinessCustomerActivity(
        id = rs.getString("id"),
        customerId = rs.getString("customer_id"),
        contactId = rs.getString("contact_id"),
        contactName = rs.getString("contact_name"),
        type = BusinessCustomerActivityType.valueOf(rs.getString("activity_type")),
        subject = rs.getString("subject"),
        details = rs.getString("details"),
        occurredAt = rs.getString("occurred_at"),
        dueAt = rs.getString("due_at"),
        completedAt = rs.getString("completed_at"),
        version = rs.getInt("version"),
        createdAt = rs.getString("created_at"),
        updatedAt = rs.getString("updated_at"),
    )
    private fun contact(rs: ResultSet) = BusinessContact(rs.id("id"), rs.id("customer_id"), rs.getString("full_name"), rs.getString("role"), rs.getString("email"), rs.getString("phone"), rs.getBoolean("is_primary"), BusinessCustomerStatus.valueOf(rs.getString("status")), rs.getInt("version"), rs.time("updated_at"))
    private fun catalogItem(rs: ResultSet) = BusinessCatalogItem(rs.id("id"), BusinessCatalogItemType.valueOf(rs.getString("item_type")), rs.getString("name"), rs.getString("sku"), rs.getString("description"), rs.getString("unit"), rs.getLong("reference_price_cents"), rs.getString("currency"), rs.getBoolean("tracks_stock"), rs.getLong("stock_minimum_milli").takeUnless { rs.wasNull() }, rs.getBoolean("is_archived"), rs.getInt("version"), rs.time("updated_at"))
    private fun stockSummary(rs: ResultSet): BusinessStockSummary { val item = catalogItem(rs); val available = rs.getLong("available_milli"); return BusinessStockSummary(item, available, item.stockMinimumMilli?.let { available < it } == true) }
    private fun stockMovement(rs: ResultSet) = BusinessStockMovement(rs.id("id"), rs.id("item_id"), BusinessStockMovementType.valueOf(rs.getString("movement_type")), rs.getLong("delta_milli"), rs.getString("reason"), rs.idOrNull("reference_id"), rs.time("created_at"))
    private fun quoteLine(rs: ResultSet) = BusinessQuoteLine(rs.id("id"), rs.getInt("position"), rs.idOrNull("source_catalog_item_id"), rs.getString("description"), rs.getLong("quantity_milli"), rs.getLong("unit_price_cents"), rs.getInt("discount_basis_points"), rs.getInt("tax_basis_points"), rs.getLong("subtotal_cents"), rs.getLong("tax_cents"), rs.getLong("total_cents"))
    private fun quoteHeader(rs: ResultSet) = BusinessQuote(rs.id("id"), rs.getString("quote_number"), rs.idOrNull("customer_id"), rs.getString("buyer_name"), rs.getString("buyer_email"), rs.getString("buyer_phone"), BusinessQuoteStatus.valueOf(rs.getString("status")), rs.getObject("issue_date").toString(), rs.getObject("valid_until")?.toString(), rs.getString("notes"), rs.getString("currency"), rs.getLong("subtotal_cents"), rs.getLong("tax_cents"), rs.getLong("total_cents"), rs.getInt("version"), updatedAt = rs.time("updated_at"))
    private fun saleHeader(rs: ResultSet) = BusinessSale(rs.id("id"), rs.getString("sale_number"), rs.id("quote_id"), rs.getString("buyer_name"), rs.getString("currency"), rs.getLong("subtotal_cents"), rs.getLong("tax_cents"), rs.getLong("total_cents"), BusinessSaleStatus.valueOf(rs.getString("status")), rs.time("confirmed_at"))

    private fun <T> transaction(block: (Connection) -> T): T = dataSource.withConnection { connection ->
        val autoCommit = connection.autoCommit
        connection.autoCommit = false
        try { block(connection).also { connection.commit() } } catch (error: Throwable) { connection.rollback(); throw error } finally { connection.autoCommit = autoCommit }
    }

    private fun <T> page(items: List<T>, limit: Int, id: (T) -> String): BusinessSalesPage<T> {
        val visible = items.take(limit)
        return BusinessSalesPage(visible, if (items.size > limit) id(visible.last()) else null)
    }

    private fun PreparedStatement.uuid(index: Int, value: String?) { if (value == null) setNull(index, Types.OTHER) else setObject(index, UUID.fromString(value)) }
    private fun PreparedStatement.nullableUuid(index: Int, value: String?) = uuid(index, value)
    private fun PreparedStatement.string(index: Int, value: String?) { if (value == null) setNull(index, Types.VARCHAR) else setString(index, value) }
    private fun PreparedStatement.longOrNull(index: Int, value: Long?) { if (value == null) setNull(index, Types.BIGINT) else setLong(index, value) }
    private fun ResultSet.id(column: String): String = getObject(column).toString()
    private fun ResultSet.idOrNull(column: String): String? = getObject(column)?.toString()
    private fun ResultSet.time(column: String): String = getObject(column).toString()
    private fun <T> ResultSet.map(mapper: (ResultSet) -> T): List<T> = buildList { while (next()) add(mapper(this@map)) }

    private companion object { const val UNIQUE_VIOLATION = "23505" }
}
