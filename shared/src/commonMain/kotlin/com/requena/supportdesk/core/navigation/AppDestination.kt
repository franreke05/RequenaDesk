package com.requena.supportdesk.core.navigation

sealed interface AppDestination {
    val route: String

    object Login : AppDestination {
        override val route: String = "login"
    }

    object Dashboard : AppDestination {
        override val route: String = "dashboard"
    }

    object Tasks : AppDestination {
        override val route: String = "tasks"
    }

    object Labels : AppDestination {
        override val route: String = "labels"
    }

    object Tickets : AppDestination {
        override val route: String = "tickets"
    }

    object CreateTicket : AppDestination {
        override val route: String = "tickets/create"
    }

    data class TicketDetail(val ticketId: String) : AppDestination {
        override val route: String = "tickets/$ticketId"
    }

    object Clients : AppDestination {
        override val route: String = "clients"
    }

    object Notifications : AppDestination {
        override val route: String = "notifications"
    }

    object Boards : AppDestination {
        override val route: String = "boards"
    }

    object Invoices : AppDestination {
        override val route: String = "invoices"
    }

    object CreateInvoice : AppDestination {
        override val route: String = "invoices/create"
    }

    data class InvoiceDetail(val invoiceId: String) : AppDestination {
        override val route: String = "invoices/$invoiceId"
    }
}
