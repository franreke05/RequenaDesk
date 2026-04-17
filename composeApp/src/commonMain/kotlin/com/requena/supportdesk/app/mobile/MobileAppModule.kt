package com.requena.supportdesk.app.mobile

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel

class MobileAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    val clientsViewModel: ClientsViewModel = SupportDeskSharedModule.createClientsViewModel()
    val tasksViewModel: TasksViewModel = SupportDeskSharedModule.createTasksViewModel()

    fun clear() {
        authViewModel.clear()
        clientsViewModel.clear()
        tasksViewModel.clear()
    }
}
