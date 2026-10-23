package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.LocalSupportDeskThemeController
import com.requena.supportdesk.core.model.InvitationStatus
import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.Organization
import com.requena.supportdesk.core.model.OrganizationInvitation
import com.requena.supportdesk.core.model.OrganizationMember
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.canManageCompany
import com.requena.supportdesk.core.model.isReadOnly
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.AppearancePicker
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.organization.data.OrganizationUpdate
import com.requena.supportdesk.features.organization.presentation.OrganizationUiEffect
import com.requena.supportdesk.features.organization.presentation.OrganizationUiEvent
import com.requena.supportdesk.features.organization.presentation.OrganizationUiState
import com.requena.supportdesk.features.organization.presentation.OrganizationViewModel
import com.requena.supportdesk.features.plan.PlanUiEffect
import com.requena.supportdesk.features.plan.PlanUiEvent
import com.requena.supportdesk.features.plan.PlanViewModel

/** Ajustes tabs. [PLAN] can be opened directly (e.g. from a locked module). */
enum class SettingsTab(val title: String) {
    COMPANY("Empresa"),
    TEAM("Usuarios"),
    PLAN("Plan y pagos"),
    OPERATOR("Solicitudes de plan"),
    APPEARANCE("Apariencia"),
}

/**
 * Ajustes. Owners and admins see Empresa, Usuarios and Apariencia; members and the gestoría
 * see their company read-only plus Apariencia (the server enforces the same rules).
 */
@Composable
fun AdminSettingsScreen(
    viewModel: OrganizationViewModel,
    planViewModel: PlanViewModel,
    modifier: Modifier = Modifier,
    requestedTab: SettingsTab? = null,
    onRequestedTabShown: () -> Unit = {},
) {
    val user by AdminSessionContext.user.collectAsState()
    val state by viewModel.state.collectAsState()
    val planState by planViewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is OrganizationUiEffect.ShowMessage) snackbar.showSnackbar(effect.message)
        }
    }
    LaunchedEffect(planViewModel) {
        planViewModel.onEvent(PlanUiEvent.Load)
        planViewModel.effects.collect { effect ->
            if (effect is PlanUiEffect.ShowMessage) snackbar.showSnackbar(effect.message)
        }
    }
    val canManage = user?.canManageCompany == true
    val isOperator = canManage && planState.overview?.isPlatformOperator == true
    val tabs = SettingsTab.entries.filter { tab ->
        when (tab) {
            SettingsTab.TEAM -> canManage
            SettingsTab.OPERATOR -> isOperator
            else -> true
        }
    }
    var selected by rememberSaveable { mutableStateOf(SettingsTab.COMPANY) }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            selected = requestedTab
            onRequestedTabShown()
        }
    }
    if (selected !in tabs) selected = tabs.first()

    Box(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.lg)) {
            PrimaryTabRow(selectedTabIndex = tabs.indexOf(selected)) {
                tabs.forEach { tab ->
                    Tab(selected = tab == selected, onClick = { selected = tab }, text = { Text(tab.title) })
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.lg),
            ) {
                when (selected) {
                    SettingsTab.COMPANY -> CompanyTab(state, canManage, viewModel::onEvent)
                    SettingsTab.TEAM -> TeamTab(state, user, viewModel::onEvent)
                    SettingsTab.PLAN -> PlanTab(planState, canManage, planViewModel::onEvent)
                    SettingsTab.OPERATOR -> OperatorRequestsTab(planState, planViewModel::onEvent)
                    SettingsTab.APPEARANCE -> AppearanceTab()
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CompanyTab(state: OrganizationUiState, canManage: Boolean, onEvent: (OrganizationUiEvent) -> Unit) {
    val organization = state.organization
    if (organization == null) {
        Text(state.errorMessage ?: "Cargando tu empresa...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val spacing = SupportDeskThemeTokens.spacing
    val key = organization.id
    var name by rememberSaveable(key) { mutableStateOf(organization.name) }
    var legalForm by rememberSaveable(key) { mutableStateOf(organization.legalForm) }
    var sector by rememberSaveable(key) { mutableStateOf(organization.sector) }
    var legalName by rememberSaveable(key) { mutableStateOf(organization.legalName) }
    var taxId by rememberSaveable(key) { mutableStateOf(organization.taxId) }
    var addressLine by rememberSaveable(key) { mutableStateOf(organization.addressLine) }
    var postalCode by rememberSaveable(key) { mutableStateOf(organization.postalCode) }
    var city by rememberSaveable(key) { mutableStateOf(organization.city) }
    var province by rememberSaveable(key) { mutableStateOf(organization.province) }
    var country by rememberSaveable(key) { mutableStateOf(organization.country) }
    val update = OrganizationUpdate(
        name = name.trim(), legalForm = legalForm?.name, sector = sector.trim(), legalName = legalName.trim(),
        taxId = taxId.trim(), addressLine = addressLine.trim(), postalCode = postalCode.trim(), city = city.trim(),
        province = province.trim(), country = country.trim().uppercase(),
    )
    val problem = OrganizationViewModel.companyProblem(update)

    SectionCard(
        modifier = Modifier.widthIn(max = 760.dp),
        title = "Tu empresa",
        subtitle = if (canManage) {
            "Estos datos salen en tus presupuestos, facturas y en el portal de tus clientes."
        } else {
            "Solo los dueños y admins pueden cambiar estos datos."
        },
    ) {
        SettingsField("Nombre comercial", name, canManage) { name = it }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            LegalForm.entries.forEach { form ->
                FilterChip(
                    selected = legalForm == form,
                    onClick = { legalForm = form },
                    enabled = canManage,
                    label = { Text(form.label) },
                )
            }
        }
        SettingsField("Sector", sector, canManage) { sector = it }
        HorizontalDivider()
        Text("Datos fiscales", style = MaterialTheme.typography.titleSmall)
        SettingsField("Razón social o nombre fiscal", legalName, canManage) { legalName = it }
        SettingsField(
            "NIF / CIF", taxId, canManage,
            error = com.requena.supportdesk.features.clients.domain.model.clientTaxIdProblem(taxId),
            capitalization = KeyboardCapitalization.Characters,
        ) { taxId = it }
        SettingsField("Dirección", addressLine, canManage) { addressLine = it }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box(Modifier.weight(1f)) { SettingsField("Código postal", postalCode, canManage) { postalCode = it } }
            Box(Modifier.weight(2f)) { SettingsField("Ciudad", city, canManage) { city = it } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box(Modifier.weight(2f)) { SettingsField("Provincia", province, canManage) { province = it } }
            Box(Modifier.weight(1f)) {
                SettingsField("País", country, canManage, capitalization = KeyboardCapitalization.Characters) { country = it }
            }
        }
        if (canManage) {
            PrimaryButton(
                text = "Guardar cambios",
                onClick = { onEvent(OrganizationUiEvent.SaveCompany(update)) },
                enabled = problem == null && !state.isSaving,
                isLoading = state.isSaving,
            )
            problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    enabled: Boolean,
    error: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TeamTab(state: OrganizationUiState, user: User?, onEvent: (OrganizationUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var pendingDeactivation by remember { mutableStateOf<OrganizationMember?>(null) }
    var inviteRole by rememberSaveable { mutableStateOf(OrganizationRole.MEMBER) }

    SectionCard(
        modifier = Modifier.widthIn(max = 760.dp),
        title = "Invitar a alguien",
        subtitle = "Crea un código de un solo uso y compártelo tú (correo, WhatsApp...). Caduca en 7 días.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            OrganizationRole.invitable.forEach { role ->
                FilterChip(selected = inviteRole == role, onClick = { inviteRole = role }, label = { Text(role.label) })
            }
        }
        Text(inviteRole.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(
            text = "Crear código de invitación",
            onClick = { onEvent(OrganizationUiEvent.Invite(inviteRole)) },
            enabled = !state.isSaving,
            isLoading = state.isSaving,
        )
        state.newInvitationCode?.let { code -> NewInvitationCode(code) { onEvent(OrganizationUiEvent.DismissInvitationCode) } }
    }

    SectionCard(modifier = Modifier.widthIn(max = 760.dp), title = "Equipo", subtitle = "${state.members.count { it.isActive }} personas con acceso") {
        state.members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider()
            MemberRow(
                member = member,
                isSelf = member.userId == user?.id,
                actorIsOwner = user?.organizationRole == OrganizationRole.OWNER,
                onRoleChange = { onEvent(OrganizationUiEvent.ChangeRole(member.userId, it)) },
                onDeactivate = { pendingDeactivation = member },
            )
        }
    }

    val pending = state.invitations.filter { it.status == InvitationStatus.PENDING }
    if (state.invitations.isNotEmpty()) {
        SectionCard(modifier = Modifier.widthIn(max = 760.dp), title = "Invitaciones", subtitle = "${pending.size} pendientes") {
            state.invitations.forEach { invitation -> InvitationRow(invitation) { onEvent(OrganizationUiEvent.Revoke(invitation.id)) } }
        }
    }

    ConfirmDialog(
        visible = pendingDeactivation != null,
        title = "¿Quitar el acceso?",
        message = "${pendingDeactivation?.name} no podrá volver a entrar. Su trabajo y sus horas se conservan.",
        confirmText = "Quitar acceso",
        onConfirm = {
            pendingDeactivation?.let { onEvent(OrganizationUiEvent.Deactivate(it.userId)) }
            pendingDeactivation = null
        },
        onDismiss = { pendingDeactivation = null },
    )
}

@Composable
private fun NewInvitationCode(code: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(SupportDeskThemeTokens.spacing.md)) {
            Text("Código creado (solo se muestra ahora)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(code, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("Copiar") }
                TextButton(onClick = onDismiss) { Text("Hecho") }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: OrganizationMember,
    isSelf: Boolean,
    actorIsOwner: Boolean,
    onRoleChange: (OrganizationRole) -> Unit,
    onDeactivate: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Only an owner can touch an owner or hand out ownership (server rule too).
    val editable = member.isActive && (actorIsOwner || member.role != OrganizationRole.OWNER)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (isSelf) "${member.name} (tú)" else member.name, style = MaterialTheme.typography.titleSmall)
            Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!member.isActive) {
            SupportDeskBadge("Sin acceso", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            SecondaryButton(text = member.role.label, onClick = { menuOpen = true }, enabled = editable)
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                OrganizationRole.entries
                    .filter { actorIsOwner || it != OrganizationRole.OWNER }
                    .forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.label) },
                            onClick = {
                                menuOpen = false
                                if (role != member.role) onRoleChange(role)
                            },
                        )
                    }
            }
        }
        if (editable) {
            TextButton(onClick = onDeactivate) { Text("Quitar acceso", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun InvitationRow(invitation: OrganizationInvitation, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text("${invitation.role.label} · ${invitation.status.label}", style = MaterialTheme.typography.titleSmall)
            Text(
                "Creada por ${invitation.createdByName} · caduca ${invitation.expiresAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (invitation.status == InvitationStatus.PENDING) {
            TextButton(onClick = onRevoke) { Text("Revocar") }
        }
    }
}

@Composable
private fun AppearanceTab() {
    val controller = LocalSupportDeskThemeController.current
    SectionCard(modifier = Modifier.widthIn(max = 760.dp), title = "Apariencia", subtitle = "Solo cambia cómo ves tú la app en este equipo.") {
        AppearancePicker(
            selectedStyle = controller.style,
            selectedMode = controller.mode,
            previewDark = controller.isDarkMode,
            onStyleSelected = controller.setStyle,
            onModeSelected = controller.setMode,
        )
    }
}

/** Shown on every staff screen to a gestoría user: they can look but not change anything. */
@Composable
fun ReadOnlyRoleBanner() {
    val user by AdminSessionContext.user.collectAsState()
    if (user?.isReadOnly != true) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Acceso de gestoría: puedes consultarlo todo, pero no hacer cambios. Si necesitas algo, pídeselo a un admin de ${user?.organizationName ?: "la empresa"}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(SupportDeskThemeTokens.spacing.md),
        )
    }
}

@Suppress("unused")
private fun Organization.displayLegalForm(): String = legalForm?.label ?: "Sin indicar"
