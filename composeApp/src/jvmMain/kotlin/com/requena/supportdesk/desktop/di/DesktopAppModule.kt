package com.requena.supportdesk.desktop.di

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class DesktopAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    val ticketsViewModel: TicketsViewModel = SupportDeskSharedModule.createTicketsViewModel()
    val clientsViewModel: ClientsViewModel = SupportDeskSharedModule.createClientsViewModel()
    val dashboardViewModel: DashboardViewModel = SupportDeskSharedModule.createDashboardViewModel()

    fun clear() {
        authViewModel.clear()
        ticketsViewModel.clear()
        clientsViewModel.clear()
        dashboardViewModel.clear()
    }
}
