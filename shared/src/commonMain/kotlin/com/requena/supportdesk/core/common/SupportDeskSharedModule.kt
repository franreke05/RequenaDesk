package com.requena.supportdesk.core.common

import com.requena.supportdesk.core.network.configuredSupportDeskHttpClient
import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.features.auth.data.datasource.RemoteAuthDataSource
import com.requena.supportdesk.features.auth.data.repository.AuthRepositoryImpl
import com.requena.supportdesk.features.auth.data.session.AuthSessionStore
import com.requena.supportdesk.features.auth.domain.usecase.ClearSessionUseCase
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
import com.requena.supportdesk.features.auth.domain.usecase.RestoreSessionUseCase
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.data.datasource.RemoteClientsDataSource
import com.requena.supportdesk.features.clients.data.repository.ClientsRepositoryImpl
import com.requena.supportdesk.features.clients.domain.usecase.CreateClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.DeleteClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.GetClientsUseCase
import com.requena.supportdesk.features.clients.domain.usecase.UpdateClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.UpdateClientCredentialsUseCase
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.dashboard.data.datasource.RemoteDashboardDataSource
import com.requena.supportdesk.features.dashboard.data.repository.DashboardRepositoryImpl
import com.requena.supportdesk.features.dashboard.domain.usecase.GetDashboardSummaryUseCase
import com.requena.supportdesk.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.requena.supportdesk.features.notifications.data.datasource.RemoteNotificationsDataSource
import com.requena.supportdesk.features.notifications.data.repository.NotificationsRepositoryImpl
import com.requena.supportdesk.features.notifications.domain.usecase.RegisterDeviceUseCase
import com.requena.supportdesk.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.requena.supportdesk.features.tasks.data.datasource.RemoteTasksDataSource
import com.requena.supportdesk.features.tasks.data.repository.TasksRepositoryImpl
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTimeLogUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLabelsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLogsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTasksUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskUseCase
import com.requena.supportdesk.features.tickets.data.datasource.RemoteTicketsDataSource
import com.requena.supportdesk.features.tickets.data.repository.TicketsRepositoryImpl
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketPriorityUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ChangeTicketStatusUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.CreateTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.GetTicketsUseCase
import com.requena.supportdesk.features.tickets.domain.usecase.ReplyTicketUseCase
import com.requena.supportdesk.features.invoices.data.storage.createInvoicePdfStorage
import com.requena.supportdesk.features.invoices.presentation.viewmodel.InvoicesViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

object SupportDeskSharedModule {
    private val sessionManager = SupportDeskSessionManager(AuthSessionStore())
    private val httpClient = configuredSupportDeskHttpClient(sessionManager)
    private val authRepository = AuthRepositoryImpl(
        dataSource = RemoteAuthDataSource(httpClient),
        sessionManager = sessionManager,
    )
    private val ticketsRepository = TicketsRepositoryImpl(RemoteTicketsDataSource(httpClient))
    private val clientsRepository = ClientsRepositoryImpl(RemoteClientsDataSource(httpClient))
    private val tasksRepository = TasksRepositoryImpl(RemoteTasksDataSource(httpClient))
    private val dashboardRepository = DashboardRepositoryImpl(RemoteDashboardDataSource(httpClient))
    private val notificationsRepository = NotificationsRepositoryImpl(RemoteNotificationsDataSource(httpClient))

    private val loginUseCase = LoginUseCase(authRepository)
    private val restoreSessionUseCase = RestoreSessionUseCase(authRepository)
    private val clearSessionUseCase = ClearSessionUseCase(authRepository)
    private val getTicketsUseCase = GetTicketsUseCase(ticketsRepository)
    private val getTicketUseCase = GetTicketUseCase(ticketsRepository)
    private val createTicketUseCase = CreateTicketUseCase(ticketsRepository)
    private val replyTicketUseCase = ReplyTicketUseCase(ticketsRepository)
    private val changeTicketStatusUseCase = ChangeTicketStatusUseCase(ticketsRepository)
    private val changeTicketPriorityUseCase = ChangeTicketPriorityUseCase(ticketsRepository)
    private val getClientsUseCase = GetClientsUseCase(clientsRepository)
    private val createClientUseCase = CreateClientUseCase(clientsRepository)
    private val updateClientUseCase = UpdateClientUseCase(clientsRepository)
    private val updateClientCredentialsUseCase = UpdateClientCredentialsUseCase(clientsRepository)
    private val deleteClientUseCase = DeleteClientUseCase(clientsRepository)
    private val getTaskLabelsUseCase = GetTaskLabelsUseCase(tasksRepository)
    private val getTasksUseCase = GetTasksUseCase(tasksRepository)
    private val getTaskLogsUseCase = GetTaskLogsUseCase(tasksRepository)
    private val createTaskUseCase = CreateTaskUseCase(tasksRepository)
    private val updateTaskUseCase = UpdateTaskUseCase(tasksRepository)
    private val deleteTaskUseCase = DeleteTaskUseCase(tasksRepository)
    private val createTaskLabelUseCase = CreateTaskLabelUseCase(tasksRepository)
    private val updateTaskLabelUseCase = UpdateTaskLabelUseCase(tasksRepository)
    private val deleteTaskLabelUseCase = DeleteTaskLabelUseCase(tasksRepository)
    private val createTimeLogUseCase = CreateTimeLogUseCase(tasksRepository)
    private val getDashboardSummaryUseCase = GetDashboardSummaryUseCase(dashboardRepository)
    private val registerDeviceUseCase = RegisterDeviceUseCase(notificationsRepository)

    fun createAuthViewModel(): AuthViewModel = AuthViewModel(
        loginUseCase = loginUseCase,
        restoreSessionUseCase = restoreSessionUseCase,
        clearSessionUseCase = clearSessionUseCase,
    )

    fun createTicketsViewModel(): TicketsViewModel = TicketsViewModel(
        getTicketsUseCase = getTicketsUseCase,
        getTicketUseCase = getTicketUseCase,
        createTicketUseCase = createTicketUseCase,
        replyTicketUseCase = replyTicketUseCase,
        changeTicketStatusUseCase = changeTicketStatusUseCase,
        changeTicketPriorityUseCase = changeTicketPriorityUseCase,
    )

    fun createClientsViewModel(): ClientsViewModel = ClientsViewModel(
        getClientsUseCase = getClientsUseCase,
        createClientUseCase = createClientUseCase,
        updateClientUseCase = updateClientUseCase,
        updateClientCredentialsUseCase = updateClientCredentialsUseCase,
        deleteClientUseCase = deleteClientUseCase,
    )

    fun createDashboardViewModel(): DashboardViewModel = DashboardViewModel(getDashboardSummaryUseCase)

    fun createNotificationsViewModel(): NotificationsViewModel = NotificationsViewModel(registerDeviceUseCase)

    fun createInvoicesViewModel(): InvoicesViewModel = InvoicesViewModel(
        invoicePdfStorage = createInvoicePdfStorage(),
    )

    fun createTasksViewModel(): TasksViewModel = TasksViewModel(
        getTaskLabelsUseCase = getTaskLabelsUseCase,
        getTasksUseCase = getTasksUseCase,
        getTaskLogsUseCase = getTaskLogsUseCase,
        createTaskUseCase = createTaskUseCase,
        updateTaskUseCase = updateTaskUseCase,
        deleteTaskUseCase = deleteTaskUseCase,
        createTaskLabelUseCase = createTaskLabelUseCase,
        updateTaskLabelUseCase = updateTaskLabelUseCase,
        deleteTaskLabelUseCase = deleteTaskLabelUseCase,
        createTimeLogUseCase = createTimeLogUseCase,
    )
}
