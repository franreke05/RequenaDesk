package com.example.crmfreelance

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SupportDeskServiceTest {

    private val service = SupportDeskService(
        repository = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
    )

    @Test
    fun loginReturnsSession() {
        val session = service.login(email = "admin@requenadesk.dev", password = "Admin1requena")

        assertNotNull(session)
        assertEquals("ADMIN", session.role)
        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())
    }

    @Test
    fun refreshReturnsRotatedSession() {
        val login = service.login(email = "admin@requenadesk.dev", password = "Admin1requena")
        val refreshed = service.refresh(RefreshSessionRequest(refreshToken = login!!.refreshToken))

        assertNotNull(refreshed)
        assertTrue(refreshed.accessToken.isNotBlank())
        assertTrue(refreshed.refreshToken.isNotBlank())
    }

    @Test
    fun logoutRevokesSession() {
        val login = service.login(email = "admin@requenadesk.dev", password = "Admin1requena")
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
}
