package com.requena.supportdesk.core.common

import com.requena.supportdesk.features.auth.data.datasource.StubAuthDataSource
import com.requena.supportdesk.features.auth.data.repository.FakeAuthRepository
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.data.repository.FakeClientsRepository
import com.requena.supportdesk.features.clients.domain.usecase.GetClientsUseCase
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.dashboard.data.repository.FakeDashboardRepository
import com.requena.supportdesk.features.dashboard.domain.usecase.GetDashboardSummaryUseCase
import com.requena.supportdesk.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.requena.supportdesk.features.notifications.data.repository.FakeNotificationsRepository
import com.requena.supportdesk.features.notifications.domain.usecase.RegisterDeviceUseCase
import com.requena.supportdesk.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.requena.supportdesk.features.tickets.data.repository.FakeTicketsRepository
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketPriorityUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketStatusUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.CreateTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketsUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ReplyTicketUseCase
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

object SupportDeskSharedModule {
    private val authRepository = FakeAuthRepository(StubAuthDataSource())
    private val ticketsRepository = FakeTicketsRepository()
    private val clientsRepository = FakeClientsRepository()
    private val dashboardRepository = FakeDashboardRepository()
    private val notificationsRepository = FakeNotificationsRepository()

    private val loginUseCase = LoginUseCase(authRepository)
    private val getTicketsUseCase = GetTicketsUseCase(ticketsRepository)
    private val getTicketUseCase = GetTicketUseCase(ticketsRepository)
    private val createTicketUseCase = CreateTicketUseCase(ticketsRepository)
    private val replyTicketUseCase = ReplyTicketUseCase(ticketsRepository)
    private val changeTicketStatusUseCase = ChangeTicketStatusUseCase(ticketsRepository)
    private val changeTicketPriorityUseCase = ChangeTicketPriorityUseCase(ticketsRepository)
    private val getClientsUseCase = GetClientsUseCase(clientsRepository)
    private val getDashboardSummaryUseCase = GetDashboardSummaryUseCase(dashboardRepository)
    private val registerDeviceUseCase = RegisterDeviceUseCase(notificationsRepository)

    fun createAuthViewModel(): AuthViewModel = AuthViewModel(loginUseCase)

    fun createTicketsViewModel(): TicketsViewModel = TicketsViewModel(
        getTicketsUseCase = getTicketsUseCase,
        getTicketUseCase = getTicketUseCase,
        createTicketUseCase = createTicketUseCase,
        replyTicketUseCase = replyTicketUseCase,
        changeTicketStatusUseCase = changeTicketStatusUseCase,
        changeTicketPriorityUseCase = changeTicketPriorityUseCase,
    )

    fun createClientsViewModel(): ClientsViewModel = ClientsViewModel(getClientsUseCase)

    fun createDashboardViewModel(): DashboardViewModel = DashboardViewModel(getDashboardSummaryUseCase)

    fun createNotificationsViewModel(): NotificationsViewModel = NotificationsViewModel(registerDeviceUseCase)
}
