package com.requena.supportdesk.app.client.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.app.client.screens.business.finance.BusinessAccountingScreen
import com.requena.supportdesk.app.client.screens.business.finance.BusinessInvoicingScreen
import com.requena.supportdesk.app.client.screens.business.operations.BookingSetupScreen
import com.requena.supportdesk.app.client.screens.business.operations.BookingsOperationsScreen
import com.requena.supportdesk.app.client.screens.business.operations.DocumentsOperationsScreen
import com.requena.supportdesk.app.client.screens.business.sales.BusinessCatalogRoute
import com.requena.supportdesk.app.client.screens.business.sales.BusinessCustomersRoute
import com.requena.supportdesk.app.client.screens.business.sales.BusinessQuotesRoute
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.finance.domain.BUSINESS_ACCOUNTING
import com.requena.supportdesk.features.business.finance.domain.BUSINESS_INVOICING
import com.requena.supportdesk.features.business.finance.presentation.BusinessAccountingViewModel
import com.requena.supportdesk.features.business.finance.presentation.BusinessInvoicingViewModel
import com.requena.supportdesk.features.business.operations.CreateAvailabilityRuleDto
import com.requena.supportdesk.features.business.operations.CreateBookingResourceDto
import com.requena.supportdesk.features.business.operations.CreateBookingServiceDto
import com.requena.supportdesk.features.business.operations.OperationsUiEvent
import com.requena.supportdesk.features.business.operations.OperationsViewModel
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CATALOG
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CUSTOMERS
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_QUOTES
import com.requena.supportdesk.features.business.sales.presentation.BusinessCatalogViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessCustomersViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessQuotesViewModel

private const val BUSINESS_BOOKINGS = "BUSINESS_BOOKINGS"
private const val BUSINESS_DOCUMENTS = "BUSINESS_DOCUMENTS"

/**
 * Single responsive entry point for the seven administrator-authorized beta utilities.
 * The server remains the authority for every entitlement and mutation.
 */
@Composable
fun ClientBusinessProgramWorkspace(
    programKey: String,
    invoicingViewModel: BusinessInvoicingViewModel,
    accountingViewModel: BusinessAccountingViewModel,
    operationsViewModel: OperationsViewModel,
    customersViewModel: BusinessCustomersViewModel,
    catalogViewModel: BusinessCatalogViewModel,
    quotesViewModel: BusinessQuotesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (programKey) {
        BUSINESS_INVOICING -> InvoicingWorkspace(invoicingViewModel, onBack, modifier)
        BUSINESS_ACCOUNTING -> AccountingWorkspace(accountingViewModel, onBack, modifier)
        BUSINESS_CUSTOMERS -> SalesWorkspace(onBack, modifier) { BusinessCustomersRoute(customersViewModel, Modifier.fillMaxSize()) }
        BUSINESS_CATALOG -> SalesWorkspace(onBack, modifier) { BusinessCatalogRoute(catalogViewModel, Modifier.fillMaxSize()) }
        BUSINESS_QUOTES -> SalesWorkspace(onBack, modifier) { BusinessQuotesRoute(quotesViewModel, Modifier.fillMaxSize()) }
        BUSINESS_BOOKINGS -> BookingsWorkspace(operationsViewModel, onBack, modifier)
        BUSINESS_DOCUMENTS -> DocumentsWorkspace(operationsViewModel, onBack, modifier)
        else -> UnknownProgramWorkspace(onBack, modifier)
    }
}

@Composable
private fun InvoicingWorkspace(
    viewModel: BusinessInvoicingViewModel,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    ProgramFrame(onBack, modifier) {
        BusinessInvoicingScreen(
            state = state,
            onSave = viewModel::save,
            onArchive = viewModel::archive,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AccountingWorkspace(
    viewModel: BusinessAccountingViewModel,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh(currentIsoDate().take(7)) }
    ProgramFrame(onBack, modifier) {
        BusinessAccountingScreen(
            state = state,
            onSave = viewModel::save,
            onRecord = viewModel::record,
            onVoid = viewModel::void,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SalesWorkspace(
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    ProgramFrame(onBack, modifier, content)
}

@Composable
private fun BookingsWorkspace(
    viewModel: OperationsViewModel,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsState()
    var configurationOpen by remember { mutableStateOf(false) }
    val today = currentIsoDate()
    LaunchedEffect(viewModel, today) {
        viewModel.onEvent(OperationsUiEvent.LoadBookingConfiguration)
        viewModel.onEvent(OperationsUiEvent.LoadAgenda("${today}T00:00:00Z", "${today}T23:59:59Z"))
    }
    ProgramFrame(onBack, modifier) {
        if (configurationOpen) {
            BookingSetupScreen(
                resources = state.configuration?.resources.orEmpty(),
                onCreateService = { name, duration ->
                    viewModel.onEvent(OperationsUiEvent.CreateBookingService(CreateBookingServiceDto(name, duration)))
                },
                onCreateResource = { name, timeZone ->
                    viewModel.onEvent(OperationsUiEvent.CreateBookingResource(CreateBookingResourceDto(name, timeZone)))
                },
                onCreateAvailability = { resourceId, weekday, startsAt, endsAt, timeZone ->
                    viewModel.onEvent(
                        OperationsUiEvent.CreateAvailabilityRule(
                            CreateAvailabilityRuleDto(resourceId, weekday, startsAt, endsAt, timeZone),
                        ),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BookingsOperationsScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onOpenConfiguration = { configurationOpen = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DocumentsWorkspace(
    viewModel: OperationsViewModel,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.onEvent(OperationsUiEvent.LoadDocuments) }
    ProgramFrame(onBack, modifier) {
        DocumentsOperationsScreen(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ProgramFrame(
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SecondaryButton(
            text = "Volver a programas",
            onClick = onBack,
            modifier = Modifier.padding(horizontal = SupportDeskThemeTokens.spacing.xl, vertical = SupportDeskThemeTokens.spacing.sm),
        )
        content()
    }
}

@Composable
private fun UnknownProgramWorkspace(onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SupportDeskThemeTokens.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.lg),
    ) {
        ClientPortalPageHeader("Programa no disponible", "Vuelve al catálogo para actualizar sus permisos.")
        ClientPortalSurfaceCard {
            Text("No hemos podido identificar este programa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SecondaryButton("Volver a programas", onClick = onBack)
        }
        EmptyState("Acceso pendiente", "El administrador puede revisar tus autorizaciones desde el área de clientes.")
    }
}
