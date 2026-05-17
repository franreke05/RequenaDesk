package com.requena.supportdesk.app.mobile

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class MobileAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    private val clientsViewModelDelegate = lazy { SupportDeskSharedModule.createClientsViewModel() }
    private val notificationsViewModelDelegate = lazy { SupportDeskSharedModule.createNotificationsViewModel() }
    private val tasksViewModelDelegate = lazy { SupportDeskSharedModule.createTasksViewModel() }
    private val ticketsViewModelDelegate = lazy { SupportDeskSharedModule.createTicketsViewModel() }

    val clientsViewModel: ClientsViewModel
        get() = clientsViewModelDelegate.value

    val tasksViewModel: TasksViewModel
        get() = tasksViewModelDelegate.value

    val notificationsViewModel: NotificationsViewModel
        get() = notificationsViewModelDelegate.value

    val ticketsViewModel: TicketsViewModel
        get() = ticketsViewModelDelegate.value

    fun clear() {
        authViewModel.clear()
        if (clientsViewModelDelegate.isInitialized()) {
            clientsViewModelDelegate.value.clear()
        }
        if (tasksViewModelDelegate.isInitialized()) {
            tasksViewModelDelegate.value.clear()
        }
        if (notificationsViewModelDelegate.isInitialized()) {
            notificationsViewModelDelegate.value.clear()
        }
        if (ticketsViewModelDelegate.isInitialized()) {
            ticketsViewModelDelegate.value.clear()
        }
    }
}
