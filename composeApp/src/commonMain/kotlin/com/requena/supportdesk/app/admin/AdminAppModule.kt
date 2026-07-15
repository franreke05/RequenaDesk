package com.requena.supportdesk.app.admin

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.invoices.presentation.viewmodel.InvoicesViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class AdminAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    private val clientsViewModelDelegate = lazy { SupportDeskSharedModule.createClientsViewModel() }
    private val tasksViewModelDelegate = lazy { SupportDeskSharedModule.createTasksViewModel() }
    private val invoicesViewModelDelegate = lazy { SupportDeskSharedModule.createInvoicesViewModel() }
    private val ticketsViewModelDelegate = lazy { SupportDeskSharedModule.createTicketsViewModel() }

    val clientsViewModel: ClientsViewModel
        get() = clientsViewModelDelegate.value

    val tasksViewModel: TasksViewModel
        get() = tasksViewModelDelegate.value

    val invoicesViewModel: InvoicesViewModel
        get() = invoicesViewModelDelegate.value

    val ticketsViewModel: TicketsViewModel
        get() = ticketsViewModelDelegate.value

    fun clear() {
        authViewModel.clear()
        if (clientsViewModelDelegate.isInitialized()) clientsViewModelDelegate.value.clear()
        if (tasksViewModelDelegate.isInitialized()) tasksViewModelDelegate.value.clear()
        if (invoicesViewModelDelegate.isInitialized()) invoicesViewModelDelegate.value.clear()
        if (ticketsViewModelDelegate.isInitialized()) ticketsViewModelDelegate.value.clear()
    }
}
