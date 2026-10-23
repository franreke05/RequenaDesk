package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.domain.model.OrganizationSubscription
import com.requena.supportdesk.server.domain.model.PlanUsage
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerPlanRequest
import com.requena.supportdesk.server.domain.model.SubscriptionPlan
import com.requena.supportdesk.server.domain.repository.OrganizationPlanRepository
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/** Postgres implementation of company plans (tables from V19). */
class PostgresOrganizationPlanRepository(
    private val dataSource: PostgresSupportDeskDataSource,
) : OrganizationPlanRepository {

    override fun plans(): List<SubscriptionPlan> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT plan_key, name, monthly_price_cents, annual_monthly_price_cents, included_users, max_clients,
                   modules, selectable_free_modules, is_purchasable, sort_order
            FROM subscription_plans ORDER BY sort_order
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            SubscriptionPlan(
                                key = resultSet.getString("plan_key"),
                                name = resultSet.getString("name"),
                                monthlyPriceCents = resultSet.getInt("monthly_price_cents"),
                                annualMonthlyPriceCents = resultSet.getInt("annual_monthly_price_cents"),
                                includedUsers = resultSet.getObject("included_users") as Int?,
                                maxClients = resultSet.getObject("max_clients") as Int?,
                                modules = resultSet.textSet("modules"),
                                selectableFreeModules = resultSet.textSet("selectable_free_modules"),
                                isPurchasable = resultSet.getBoolean("is_purchasable"),
                                sortOrder = resultSet.getInt("sort_order"),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun subscription(organizationId: String): OrganizationSubscription? = dataSource.withConnection { connection ->
        subscription(connection, organizationId)
    }

    override fun planUsage(organizationId: String): PlanUsage = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                (SELECT COUNT(*) FROM organization_members m JOIN users u ON u.id = m.user_id AND u.is_active
                  WHERE m.organization_id::text = ? AND m.role <> 'ACCOUNTANT')
              + (SELECT COUNT(*) FROM organization_invitations i
                  WHERE i.organization_id::text = ? AND i.role <> 'ACCOUNTANT'
                    AND i.accepted_at IS NULL AND i.revoked_at IS NULL AND i.expires_at > NOW()) AS staff_users,
                (SELECT COUNT(*) FROM clients c WHERE c.organization_id::text = ?) AS clients
            """.trimIndent(),
        ).use { statement ->
            repeat(3) { statement.setString(it + 1, organizationId) }
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                PlanUsage(staffUsers = resultSet.getInt("staff_users"), clients = resultSet.getInt("clients"))
            }
        }
    }

    override fun isPlatformOperator(organizationId: String): Boolean = dataSource.withConnection { connection ->
        connection.prepareStatement("SELECT is_platform_operator FROM organizations WHERE id::text = ?").use { statement ->
            statement.setString(1, organizationId)
            statement.executeQuery().use { it.next() && it.getBoolean(1) }
        }
    }

    override fun changePlan(
        organizationId: String,
        planKey: String,
        status: String,
        billingPeriod: String?,
        action: String,
        actorUserId: String?,
        priceCents: Int?,
    ): OrganizationSubscription = transaction { connection ->
        val previous = subscription(connection, organizationId)
        connection.prepareStatement(
            """
            INSERT INTO organization_subscriptions (organization_id, plan_key, status, billing_period, started_at)
            VALUES (CAST(? AS uuid), ?, ?, ?, NOW())
            ON CONFLICT (organization_id) DO UPDATE SET
                plan_key = EXCLUDED.plan_key,
                status = EXCLUDED.status,
                billing_period = EXCLUDED.billing_period,
                trial_ends_at = NULL,
                started_at = NOW()
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, planKey)
            statement.setString(3, status)
            statement.setString(4, billingPeriod)
            statement.executeUpdate()
        }
        recordEvent(connection, organizationId, action, previous?.planKey, planKey, billingPeriod, priceCents, actorUserId)
        requireNotNull(subscription(connection, organizationId))
    }

    override fun setFreeModule(organizationId: String, module: String, actorUserId: String): OrganizationSubscription =
        transaction { connection ->
            val updated = connection.prepareStatement(
                "UPDATE organization_subscriptions SET free_module = ? WHERE organization_id::text = ?",
            ).use { statement ->
                statement.setString(1, module)
                statement.setString(2, organizationId)
                statement.executeUpdate()
            }
            if (updated == 0) {
                connection.prepareStatement(
                    "INSERT INTO organization_subscriptions (organization_id, plan_key, status, free_module) VALUES (CAST(? AS uuid), 'FREE', 'ACTIVE', ?)",
                ).use { statement ->
                    statement.setString(1, organizationId)
                    statement.setString(2, module)
                    statement.executeUpdate()
                }
            }
            recordEvent(connection, organizationId, "FREE_MODULE_CHOSEN:$module", null, null, null, null, actorUserId)
            requireNotNull(subscription(connection, organizationId))
        }

    override fun createPlanRequest(
        organizationId: String,
        planKey: String,
        billingPeriod: String,
        quotedMonthlyPriceCents: Int,
        requestedBy: String,
    ): ServerPlanRequest = transaction { connection ->
        val id = try {
            connection.prepareStatement(
                """
                INSERT INTO organization_plan_requests
                    (organization_id, plan_key, billing_period, quoted_monthly_price_cents, requested_by, accepted_terms_at)
                VALUES (CAST(? AS uuid), ?, ?, ?, CAST(? AS uuid), NOW())
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, organizationId)
                statement.setString(2, planKey)
                statement.setString(3, billingPeriod)
                statement.setInt(4, quotedMonthlyPriceCents)
                statement.setString(5, requestedBy)
                statement.executeQuery().use { it.next(); it.getString(1) }
            }
        } catch (error: SQLException) {
            if (error.sqlState == "23505") throw ServerConflictException("There is already a pending plan request")
            throw error
        }
        recordEvent(connection, organizationId, "REQUESTED", null, planKey, billingPeriod, quotedMonthlyPriceCents, requestedBy)
        requireNotNull(requests(connection, requestId = id, organizationId = null, status = null).firstOrNull())
    }

    override fun planRequests(organizationId: String?, status: String?): List<ServerPlanRequest> =
        dataSource.withConnection { connection -> requests(connection, requestId = null, organizationId = organizationId, status = status) }

    override fun planRequest(requestId: String): ServerPlanRequest? = dataSource.withConnection { connection ->
        requests(connection, requestId = requestId, organizationId = null, status = null).firstOrNull()
    }

    override fun decidePlanRequest(requestId: String, status: String, decidedBy: String, note: String?): ServerPlanRequest? =
        transaction { connection ->
            val updated = connection.prepareStatement(
                """
                UPDATE organization_plan_requests
                SET status = ?, decided_by = CAST(? AS uuid), decided_at = NOW(), note = ?
                WHERE id::text = ? AND status = 'REQUESTED'
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, status)
                statement.setString(2, decidedBy)
                statement.setString(3, note)
                statement.setString(4, requestId)
                statement.executeUpdate()
            }
            if (updated == 0) return@transaction null
            val request = requests(connection, requestId = requestId, organizationId = null, status = null).first()
            recordEvent(connection, request.organizationId, status, null, request.planKey, request.billingPeriod, request.quotedMonthlyPriceCents, decidedBy)
            request
        }

    // --- helpers -----------------------------------------------------------------------

    private fun <T> transaction(block: (Connection) -> T): T = dataSource.withConnection { connection ->
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

    private fun subscription(connection: Connection, organizationId: String): OrganizationSubscription? =
        connection.prepareStatement(
            """
            SELECT organization_id::text, plan_key, status, billing_period, trial_ends_at, free_module, beta_ends_on,
                   founder_discount_percent
            FROM organization_subscriptions WHERE organization_id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) return@use null
                OrganizationSubscription(
                    organizationId = resultSet.getString(1),
                    planKey = resultSet.getString("plan_key"),
                    status = resultSet.getString("status"),
                    billingPeriod = resultSet.getString("billing_period"),
                    trialEndsAt = resultSet.getTimestamp("trial_ends_at")?.toInstant(),
                    freeModule = resultSet.getString("free_module"),
                    betaEndsOn = resultSet.getDate("beta_ends_on")?.toLocalDate(),
                    founderDiscountPercent = resultSet.getInt("founder_discount_percent"),
                )
            }
        }

    private fun requests(connection: Connection, requestId: String?, organizationId: String?, status: String?): List<ServerPlanRequest> =
        connection.prepareStatement(
            """
            SELECT r.id::text AS id, r.organization_id::text AS organization_id, o.name AS organization_name,
                   r.plan_key, r.billing_period, r.quoted_monthly_price_cents, r.status, u.name AS requested_by_name,
                   r.created_at, r.decided_at, r.note
            FROM organization_plan_requests r
            JOIN organizations o ON o.id = r.organization_id
            JOIN users u ON u.id = r.requested_by
            WHERE (? IS NULL OR r.id::text = ?)
              AND (? IS NULL OR r.organization_id::text = ?)
              AND (? IS NULL OR r.status = ?)
            ORDER BY r.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            listOf(requestId, requestId, organizationId, organizationId, status, status)
                .forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerPlanRequest(
                                id = resultSet.getString("id"),
                                organizationId = resultSet.getString("organization_id"),
                                organizationName = resultSet.getString("organization_name"),
                                planKey = resultSet.getString("plan_key"),
                                billingPeriod = resultSet.getString("billing_period"),
                                quotedMonthlyPriceCents = resultSet.getInt("quoted_monthly_price_cents"),
                                status = resultSet.getString("status"),
                                requestedByName = resultSet.getString("requested_by_name"),
                                createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                                decidedAt = resultSet.getTimestamp("decided_at")?.toInstant()?.toString(),
                                note = resultSet.getString("note"),
                            ),
                        )
                    }
                }
            }
        }

    private fun recordEvent(
        connection: Connection,
        organizationId: String,
        action: String,
        fromPlan: String?,
        toPlan: String?,
        billingPeriod: String?,
        priceCents: Int?,
        actorUserId: String?,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO organization_plan_events (organization_id, action, from_plan, to_plan, billing_period, price_cents, actor_user_id)
            VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, CAST(? AS uuid))
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, action.take(30))
            statement.setString(3, fromPlan)
            statement.setString(4, toPlan)
            statement.setString(5, billingPeriod)
            statement.setObject(6, priceCents, java.sql.Types.INTEGER)
            statement.setString(7, actorUserId)
            statement.executeUpdate()
        }
    }

    private fun ResultSet.textSet(column: String): Set<String> =
        (getArray(column)?.array as? Array<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
}
