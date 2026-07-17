package com.requena.supportdesk.app.mobile

import com.requena.supportdesk.core.common.SupportDeskSharedModule
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.clients.presentation.viewmodel.ClientsViewModel
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import com.requena.supportdesk.features.programs.presentation.viewmodel.ProgramsViewModel

class MobileAppModule {
    val authViewModel: AuthViewModel = SupportDeskSharedModule.createAuthViewModel()
    private val clientsViewModelDelegate = lazy { SupportDeskSharedModule.createClientsViewModel() }
    private val tasksViewModelDelegate = lazy { SupportDeskSharedModule.createTasksViewModel() }
    private val programsViewModelDelegate = lazy { SupportDeskSharedModule.createProgramsViewModel() }

    val clientsViewModel: ClientsViewModel
        get() = clientsViewModelDelegate.value

    val tasksViewModel: TasksViewModel
        get() = tasksViewModelDelegate.value

    val programsViewModel: ProgramsViewModel
        get() = programsViewModelDelegate.value

    fun clear() {
        authViewModel.clear()
        if (clientsViewModelDelegate.isInitialized()) {
            clientsViewModelDelegate.value.clear()
        }
        if (tasksViewModelDelegate.isInitialized()) {
            tasksViewModelDelegate.value.clear()
        }
        if (programsViewModelDelegate.isInitialized()) {
            programsViewModelDelegate.value.clear()
        }
    }
}
