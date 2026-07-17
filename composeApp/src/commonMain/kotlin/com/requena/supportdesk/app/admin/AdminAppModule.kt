package com.requena.supportdesk.app.admin

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.business.finance.presentation.BusinessAccountingViewModel
import com.requena.supportdesk.features.business.finance.presentation.BusinessInvoicingViewModel
import com.requena.supportdesk.features.business.operations.OperationsViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessCatalogViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessCustomersViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessQuotesViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.invoices.presentation.viewmodel.InvoicesViewModel
import com.requena.supportdesk.features.programs.presentation.viewmodel.ProgramsViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class AdminAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    private val clientsViewModelDelegate = lazy { SupportDeskSharedModule.createClientsViewModel() }
    private val tasksViewModelDelegate = lazy { SupportDeskSharedModule.createTasksViewModel() }
    private val invoicesViewModelDelegate = lazy { SupportDeskSharedModule.createInvoicesViewModel() }
    private val ticketsViewModelDelegate = lazy { SupportDeskSharedModule.createTicketsViewModel() }
    private val programsViewModelDelegate = lazy { SupportDeskSharedModule.createProgramsViewModel() }
    private val businessInvoicingViewModelDelegate = lazy { SupportDeskSharedModule.createBusinessInvoicingViewModel() }
    private val businessAccountingViewModelDelegate = lazy { SupportDeskSharedModule.createBusinessAccountingViewModel() }
    private val operationsViewModelDelegate = lazy { SupportDeskSharedModule.createOperationsViewModel() }
    private val businessCustomersViewModelDelegate = lazy { SupportDeskSharedModule.createBusinessCustomersViewModel() }
    private val businessCatalogViewModelDelegate = lazy { SupportDeskSharedModule.createBusinessCatalogViewModel() }
    private val businessQuotesViewModelDelegate = lazy { SupportDeskSharedModule.createBusinessQuotesViewModel() }

    val clientsViewModel: ClientsViewModel
        get() = clientsViewModelDelegate.value

    val tasksViewModel: TasksViewModel
        get() = tasksViewModelDelegate.value

    val invoicesViewModel: InvoicesViewModel
        get() = invoicesViewModelDelegate.value

    val ticketsViewModel: TicketsViewModel
        get() = ticketsViewModelDelegate.value

    val programsViewModel: ProgramsViewModel
        get() = programsViewModelDelegate.value

    val businessInvoicingViewModel: BusinessInvoicingViewModel
        get() = businessInvoicingViewModelDelegate.value

    val businessAccountingViewModel: BusinessAccountingViewModel
        get() = businessAccountingViewModelDelegate.value

    val operationsViewModel: OperationsViewModel
        get() = operationsViewModelDelegate.value

    val businessCustomersViewModel: BusinessCustomersViewModel
        get() = businessCustomersViewModelDelegate.value

    val businessCatalogViewModel: BusinessCatalogViewModel
        get() = businessCatalogViewModelDelegate.value

    val businessQuotesViewModel: BusinessQuotesViewModel
        get() = businessQuotesViewModelDelegate.value

    fun clear() {
        authViewModel.clear()
        if (clientsViewModelDelegate.isInitialized()) clientsViewModelDelegate.value.clear()
        if (tasksViewModelDelegate.isInitialized()) tasksViewModelDelegate.value.clear()
        if (invoicesViewModelDelegate.isInitialized()) invoicesViewModelDelegate.value.clear()
        if (ticketsViewModelDelegate.isInitialized()) ticketsViewModelDelegate.value.clear()
        if (programsViewModelDelegate.isInitialized()) programsViewModelDelegate.value.clear()
        if (businessInvoicingViewModelDelegate.isInitialized()) businessInvoicingViewModelDelegate.value.clear()
        if (businessAccountingViewModelDelegate.isInitialized()) businessAccountingViewModelDelegate.value.clear()
        if (operationsViewModelDelegate.isInitialized()) operationsViewModelDelegate.value.clear()
        if (businessCustomersViewModelDelegate.isInitialized()) businessCustomersViewModelDelegate.value.clear()
        if (businessCatalogViewModelDelegate.isInitialized()) businessCatalogViewModelDelegate.value.clear()
        if (businessQuotesViewModelDelegate.isInitialized()) businessQuotesViewModelDelegate.value.clear()
    }
}
