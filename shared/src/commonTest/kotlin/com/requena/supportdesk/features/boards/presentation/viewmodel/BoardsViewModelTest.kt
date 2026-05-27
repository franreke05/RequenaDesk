package com.requena.supportdesk.features.boards.presentation.viewmodel

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardsViewModelTest {

    @Test
    fun changingAdminClearsPreviousAdminBoardsAndCards() {
        val viewModel = BoardsViewModel()

        viewModel.updateClients(listOf(client("client-a", "admin-a", "Alpha")), ownerAdminId = "admin-a")
        viewModel.updateTickets(listOf(ticket("ticket-a", "client-a")))

        assertEquals("admin-a", viewModel.state.value.boards.single().ownerId)
        assertEquals(1, viewModel.state.value.boards.single().cards.size)

        viewModel.updateClients(listOf(client("client-b", "admin-b", "Beta")), ownerAdminId = "admin-b")

        val state = viewModel.state.value
        assertEquals(1, state.boards.size)
        assertEquals("admin-b", state.boards.single().ownerId)
        assertEquals("client-b", state.boards.single().clientId)
        assertTrue(state.tickets.isEmpty())
        assertTrue(state.boards.single().cards.isEmpty())
    }

    @Test
    fun removingClientRemovesItsBoard() {
        val viewModel = BoardsViewModel()

        viewModel.updateClients(
            clients = listOf(
                client("client-a", "admin-a", "Alpha"),
                client("client-b", "admin-a", "Beta"),
            ),
            ownerAdminId = "admin-a",
        )

        viewModel.updateClients(listOf(client("client-b", "admin-a", "Beta")), ownerAdminId = "admin-a")

        assertEquals(listOf("client-b"), viewModel.state.value.boards.map { it.clientId })
        assertEquals("client-b", viewModel.state.value.selectedBoard?.clientId)
    }

    @Test
    fun ignoresClientsOwnedByAnotherAdmin() {
        val viewModel = BoardsViewModel()

        viewModel.updateClients(listOf(client("client-a", "admin-a", "Alpha")), ownerAdminId = "admin-b")

        assertTrue(viewModel.state.value.boards.isEmpty())
        assertTrue(viewModel.state.value.selectedBoard == null)
    }

    private fun client(id: String, ownerAdminId: String, companyName: String): Client =
        Client(
            id = id,
            ownerAdminId = ownerAdminId,
            companyName = companyName,
            productName = "$companyName Desk",
            contactName = "$companyName Contact",
            email = "${companyName.lowercase()}@example.com",
        )

    private fun ticket(id: String, clientId: String): Ticket =
        Ticket(
            id = id,
            clientId = clientId,
            ticketNumber = id,
            subject = "Support request",
            description = "Needs attention",
            status = TicketStatus.OPEN,
            priority = TicketPriority.MEDIUM,
            requester = User(
                id = "requester-$clientId",
                name = "Requester",
                email = "requester@example.com",
                role = UserRole.CLIENT,
                clientId = clientId,
            ),
            createdAt = "2026-05-20T08:00:00Z",
            updatedAt = "2026-05-20T08:00:00Z",
        )
}
