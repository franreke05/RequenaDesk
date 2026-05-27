package com.requena.supportdesk.desktop.di

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class DesktopAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    private val ticketsViewModelDelegate = lazy { SupportDeskSharedModule.createTicketsViewModel() }
    private val clientsViewModelDelegate = lazy { SupportDeskSharedModule.createClientsViewModel() }
    private val dashboardViewModelDelegate = lazy { SupportDeskSharedModule.createDashboardViewModel() }

    val ticketsViewModel: TicketsViewModel
        get() = ticketsViewModelDelegate.value

    val clientsViewModel: ClientsViewModel
        get() = clientsViewModelDelegate.value

    val dashboardViewModel: DashboardViewModel
        get() = dashboardViewModelDelegate.value

    fun clear() {
        authViewModel.clear()
        if (ticketsViewModelDelegate.isInitialized()) {
            ticketsViewModelDelegate.value.clear()
        }
        if (clientsViewModelDelegate.isInitialized()) {
            clientsViewModelDelegate.value.clear()
        }
        if (dashboardViewModelDelegate.isInitialized()) {
            dashboardViewModelDelegate.value.clear()
        }
    }
}
