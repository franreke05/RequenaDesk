package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.UpdateOrganizationRequest
import com.requena.supportdesk.server.security.PasswordHasher
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Signup, onboarding data, invitations and member rules against real Postgres (V17 tables). */
class PostgresOrganizationRepositoryIntegrationTest {

    @Test
    fun companyLifecycleWorksOnRealPostgres() {
        EmbeddedPostgres.start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
                connection.createStatement().use {
                    it.execute("CREATE ROLE anon NOLOGIN")
                    it.execute("CREATE ROLE authenticated NOLOGIN")
                }
            }
            val dataSource = PostgresSupportDeskDataSource(
                DatabaseSettings(
                    jdbcUrl = postgres.getJdbcUrl("postgres", "postgres"),
                    username = "postgres",
                    password = "",
                    maximumPoolSize = 2,
                ),
            )
            dataSource.use {
                dataSource.migrate()
                val repository = PostgresSupportDeskRepository(dataSource)

                // Signup: company + OWNER, usable for login straight away.
                val owner = repository.signupOrganization("Lucía", "lucia@nueva.dev", "PasswordSegura123", "Estudio Lucía", "v1")
                val organizationId = requireNotNull(owner.organizationId)
                assertEquals("OWNER", owner.organizationRole)
                assertEquals("Estudio Lucía", owner.organizationName)
                assertNotNull(repository.authenticate("lucia@nueva.dev", "PasswordSegura123"))
                assertFailsWith<ServerConflictException> {
                    repository.signupOrganization("Otra", "LUCIA@nueva.dev", "PasswordSegura123", "Otra", "v1")
                }

                // Onboarding data: partial updates keep the rest; completion is stamped once.
                assertNull(repository.organization(organizationId).onboardingCompletedAt)
                repository.updateOrganization(
                    organizationId,
                    UpdateOrganizationRequest(legalForm = "AUTONOMO", onboardingGoals = listOf("INVOICING", "SUPPORT"), hasAccountant = true),
                )
                val completed = repository.updateOrganization(organizationId, UpdateOrganizationRequest(onboardingCompleted = true))
                assertEquals("AUTONOMO", completed.legalForm)
                assertEquals(listOf("INVOICING", "SUPPORT"), completed.onboardingGoals)
                assertEquals(true, completed.hasAccountant)
                assertNotNull(completed.onboardingCompletedAt)
                assertEquals("Estudio Lucía", completed.name)

                // Invitation: single use, gives its role, joins the same company.
                val codeHash = PasswordHasher.hashToken("INV-AAAA-BBBB-CCCC")
                val invitation = repository.createOrganizationInvitation(
                    organizationId, "MEMBER", owner.userId, codeHash, Instant.now().plus(7, ChronoUnit.DAYS),
                )
                assertEquals("PENDING", invitation.status())
                val member = repository.acceptOrganizationInvitation(codeHash, "Mario", "mario@nueva.dev", "PasswordSegura123", "v1")
                assertEquals(organizationId, member.organizationId)
                assertEquals("MEMBER", member.organizationRole)
                assertFailsWith<ServerNotFoundException> {
                    repository.acceptOrganizationInvitation(codeHash, "Otro", "otro@nueva.dev", "PasswordSegura123", "v1")
                }
                assertEquals("ACCEPTED", repository.organizationInvitations(organizationId).single().status())

                // Expired and revoked invitations cannot be used.
                val expiredHash = PasswordHasher.hashToken("INV-EXPI-RED0-0000")
                repository.createOrganizationInvitation(organizationId, "ADMIN", owner.userId, expiredHash, Instant.now().minusSeconds(60))
                assertFailsWith<ServerNotFoundException> {
                    repository.acceptOrganizationInvitation(expiredHash, "Tarde", "tarde@nueva.dev", "PasswordSegura123", "v1")
                }
                val revokedHash = PasswordHasher.hashToken("INV-REVO-KED0-0000")
                val revoked = repository.createOrganizationInvitation(
                    organizationId, "ACCOUNTANT", owner.userId, revokedHash, Instant.now().plus(1, ChronoUnit.DAYS),
                )
                repository.revokeOrganizationInvitation(organizationId, revoked.id)
                assertFailsWith<ServerNotFoundException> {
                    repository.acceptOrganizationInvitation(revokedHash, "Gestor", "gestor@nueva.dev", "PasswordSegura123", "v1")
                }

                // Members: the last active OWNER can be neither demoted nor deactivated.
                assertEquals(listOf("OWNER", "MEMBER"), repository.organizationMembers(organizationId).map { it.role })
                assertFailsWith<ServerConflictException> { repository.updateOrganizationMemberRole(organizationId, owner.userId, "ADMIN") }
                assertFailsWith<ServerConflictException> { repository.deactivateOrganizationMember(organizationId, owner.userId) }
                assertEquals("ADMIN", repository.updateOrganizationMemberRole(organizationId, member.userId, "ADMIN").role)

                // Deactivation blocks login and revokes refresh tokens.
                repository.storeRefreshToken(member.userId, "member-refresh", Instant.now().plus(1, ChronoUnit.DAYS))
                repository.deactivateOrganizationMember(organizationId, member.userId)
                assertNull(repository.authenticate("mario@nueva.dev", "PasswordSegura123"))
                assertNull(repository.rotateRefreshToken("member-refresh", "next", Instant.now().plus(1, ChronoUnit.DAYS)))
                assertFalse(repository.organizationMembers(organizationId).first { it.userId == member.userId }.isActive)

                // Members of another company are not reachable from this one.
                val other = repository.signupOrganization("Bea", "bea@otra.dev", "PasswordSegura123", "Otra empresa", "v1")
                assertFailsWith<ServerNotFoundException> {
                    repository.updateOrganizationMemberRole(organizationId, other.userId, "MEMBER")
                }
                assertTrue(repository.organizationMembers(organizationId).none { it.userId == other.userId })

                // Legal acceptances are stored separately per document.
                dataSource.withConnection { connection ->
                    connection.prepareStatement(
                        "SELECT COUNT(*) FROM legal_acceptances WHERE user_id::text = ? AND version = 'v1'",
                    ).use { statement ->
                        statement.setString(1, owner.userId)
                        statement.executeQuery().use { resultSet ->
                            resultSet.next()
                            assertEquals(2, resultSet.getInt(1))
                        }
                    }
                }
            }
        }
    }
}
