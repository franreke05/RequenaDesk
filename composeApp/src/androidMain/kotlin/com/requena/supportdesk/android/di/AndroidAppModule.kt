package com.requena.supportdesk.android.di

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.requena.supportdesk.features.tickets.presentation.viewmodel.TicketsViewModel

class AndroidAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    val ticketsViewModel: TicketsViewModel = SupportDeskSharedModule.createTicketsViewModel()
    val notificationsViewModel: NotificationsViewModel = SupportDeskSharedModule.createNotificationsViewModel()

    fun clear() {
        authViewModel.clear()
        ticketsViewModel.clear()
        notificationsViewModel.clear()
    }
}
