package com.example.crmfreelance

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.ApproveClientProgramRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.ServerAuthSettings
import com.requena.supportdesk.server.security.SupportDeskTokenService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SupportDeskServiceTest {
    private val tokenService = SupportDeskTokenService(
        ServerAuthSettings(
            secret = "supportdesk-test-secret-1234567890",
            issuer = "test-suite",
            audience = "test-clients",
            accessTokenLifetimeMinutes = 60,
            refreshTokenLifetimeDays = 30,
        ),
    )

    private val service = SupportDeskService(
        repository = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
        tokenService = tokenService,
    )

    @Test
    fun loginReturnsSession() {
        val session = service.login(email = "admin@orykai.dev", password = "UnitTestAdminPassword1")

        assertNotNull(session)
        assertEquals("ADMIN", session.role)
        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())
    }

    @Test
    fun refreshReturnsRotatedSession() {
        val login = service.login(email = "admin@orykai.dev", password = "UnitTestAdminPassword1")
        val refreshed = service.refresh(RefreshSessionRequest(refreshToken = login!!.refreshToken))

        assertNotNull(refreshed)
        assertTrue(refreshed.accessToken.isNotBlank())
        assertTrue(refreshed.refreshToken.isNotBlank())
    }

    @Test
    fun logoutRevokesSession() {
        val login = service.login(email = "admin@orykai.dev", password = "UnitTestAdminPassword1")
        val result = service.logout(LogoutRequest(refreshToken = login!!.refreshToken))

        assertTrue(result)
    }

    @Test
    fun registerDeviceReturnsStoredDeviceShape() {
        val device = service.registerDevice(
            RegisterDeviceRequest(
                userId = "user-admin",
                token = "device-token-1",
                platform = "ANDROID",
            ),
        )

        assertEquals("user-admin", device.userId)
        assertEquals("ANDROID", device.platform)
    }

    @Test
    fun programApprovalRejectsAnyChargeDuringFreeBeta() {
        assertFailsWith<com.requena.supportdesk.server.domain.model.ServerValidationException> {
            service.approvedClientProgramRequest(
                requestId = "missing-request",
                request = ApproveClientProgramRequest(monthlyPriceCents = 1),
                reviewedByUserId = "user-admin",
                ownerAdminId = "user-admin",
            )
        }
    }
}
