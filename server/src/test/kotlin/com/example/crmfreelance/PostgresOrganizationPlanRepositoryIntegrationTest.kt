package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.domain.model.PlanKeys
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.SubscriptionStatus
import com.requena.supportdesk.server.security.PasswordHasher
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Plans (V19) against real Postgres: trial on signup, usage, plan changes and requests. */
class PostgresOrganizationPlanRepositoryIntegrationTest {

    @Test
    fun planLifecycleWorksOnRealPostgres() {
        EmbeddedPostgres.start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
                connection.createStatement().use {
                    it.execute("CREATE ROLE anon NOLOGIN")
                    it.execute("CREATE ROLE authenticated NOLOGIN")
                }
            }
            val dataSource = PostgresSupportDeskDataSource(
                DatabaseSettings(postgres.getJdbcUrl("postgres", "postgres"), "postgres", "", maximumPoolSize = 2),
            )
            dataSource.use {
                dataSource.migrate()
                val repository = PostgresSupportDeskRepository(dataSource)

                // Catalog seeded by V19, ordered; BETA is not purchasable.
                val plans = repository.plans()
                assertEquals(listOf("FREE", "AUTONOMO", "PRO", "TEAM", "BETA"), plans.map { it.key })
                assertEquals(25, plans.first().maxClients)
                assertNull(plans.first { it.key == "BETA" }.includedUsers)

                // Signup starts a 14-day Pro trial in the same transaction.
                val owner = repository.signupOrganization("Lucía", "lucia@plan.dev", "PasswordSegura123", "Estudio", "v1")
                val organizationId = requireNotNull(owner.organizationId)
                val trial = requireNotNull(repository.subscription(organizationId))
                assertEquals(PlanKeys.PRO to SubscriptionStatus.TRIAL, trial.planKey to trial.status)
                val days = Duration.between(Instant.now(), requireNotNull(trial.trialEndsAt)).toDays()
                assertTrue(days in 13..14, "trial days $days")

                // Usage: the owner, plus pending non-gestoría invitations; clients by company.
                repository.createOrganizationInvitation(organizationId, "MEMBER", owner.userId, PasswordHasher.hashToken("a"), Instant.now().plus(1, ChronoUnit.DAYS))
                repository.createOrganizationInvitation(organizationId, "ACCOUNTANT", owner.userId, PasswordHasher.hashToken("b"), Instant.now().plus(1, ChronoUnit.DAYS))
                assertEquals(2, repository.planUsage(organizationId).staffUsers)
                assertEquals(0, repository.planUsage(organizationId).clients)
                assertEquals("MEMBER", repository.pendingInvitationTarget(PasswordHasher.hashToken("a"))?.second)

                // Free module and plan change keep a history.
                repository.changePlan(organizationId, PlanKeys.FREE, SubscriptionStatus.ACTIVE, null, "DOWNGRADED_TO_FREE", owner.userId, 0)
                val free = repository.setFreeModule(organizationId, "SUPPORT", owner.userId)
                assertEquals("SUPPORT", free.freeModule)
                assertNull(free.trialEndsAt)

                // One pending request per company; decide it once.
                val request = repository.createPlanRequest(organizationId, PlanKeys.PRO, "ANNUAL", 2400, owner.userId)
                assertEquals("Estudio", request.organizationName)
                assertFailsWith<ServerConflictException> {
                    repository.createPlanRequest(organizationId, PlanKeys.TEAM, "MONTHLY", 4900, owner.userId)
                }
                assertEquals(listOf(request.id), repository.planRequests(null, "REQUESTED").map { it.id })
                val approved = assertNotNull(repository.decidePlanRequest(request.id, "APPROVED", owner.userId, "Factura 1"))
                assertEquals("APPROVED", approved.status)
                assertNull(repository.decidePlanRequest(request.id, "REJECTED", owner.userId, null))
                val active = repository.changePlan(organizationId, PlanKeys.PRO, SubscriptionStatus.ACTIVE, "ANNUAL", "APPROVED", owner.userId, 2400)
                assertEquals("ANNUAL", active.billingPeriod)

                // Only the company marked by V19 is the operator (none here: fresh database).
                assertEquals(false, repository.isPlatformOperator(organizationId))

                dataSource.withConnection { connection ->
                    connection.prepareStatement("SELECT action FROM organization_plan_events WHERE organization_id::text = ? ORDER BY at").use { statement ->
                        statement.setString(1, organizationId)
                        val actions = statement.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
                        assertEquals(
                            listOf("TRIAL_STARTED", "DOWNGRADED_TO_FREE", "FREE_MODULE_CHOSEN:SUPPORT", "REQUESTED", "APPROVED", "APPROVED"),
                            actions,
                        )
                    }
                }
            }
        }
    }
}
