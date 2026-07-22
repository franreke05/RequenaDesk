package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Columns3
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.UserRound
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints

private enum class ClientSettingsPanel {
    OVERVIEW,
    ACCOUNT_SECURITY,
    BILLING,
    LEGAL_NOTICE,
    TERMS,
    PRIVACY,
    DATA_RIGHTS,
    COOKIES,
    CANCELLATION,
}

/**
 * Customer-facing settings. It intentionally distinguishes subscription billing from the
 * separate business invoicing program available from Programas.
 */
@Composable
fun ClientSettingsScreen(
    activeProgramCount: Int,
    onManagePrograms: () -> Unit,
    onOpenAccount: () -> Unit,
    onContactSupport: () -> Unit,
) {
    var panel by remember { mutableStateOf(ClientSettingsPanel.OVERVIEW) }

    when (panel) {
        ClientSettingsPanel.OVERVIEW -> ClientSettingsOverview(
            activeProgramCount = activeProgramCount,
            onOpenPanel = { panel = it },
        )
        ClientSettingsPanel.ACCOUNT_SECURITY -> ClientSettingsDetail(
            title = "Cuenta y seguridad",
            subtitle = "Gestiona quién accede a tu organización y protege tu cuenta.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                ClientPortalSectionTitle(
                    "Perfil de la organización",
                    "Los datos de contacto y servicio se gestionan desde Cuenta.",
                )
                SecondaryButton(
                    text = "Ir a Cuenta",
                    icon = Lucide.UserRound,
                    onClick = onOpenAccount,
                )
            }
            ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                ClientPortalSectionTitle(
                    "Acceso seguro",
                    "Las sesiones, contraseñas y la verificación en dos pasos aparecerán aquí cuando estén disponibles.",
                )
                SettingsStatusRow("Verificación en dos pasos", "Próximamente", isPositive = false)
                SettingsStatusRow("Sesiones activas", "Próximamente", isPositive = false)
            }
        }
        ClientSettingsPanel.BILLING -> ClientSettingsDetail(
            title = "Facturación",
            subtitle = "Tu suscripción a RequenaDesk. No es la facturación que emites a tus clientes.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientBillingPanel(
                activeProgramCount = activeProgramCount,
                onManagePrograms = onManagePrograms,
            )
        }
        ClientSettingsPanel.LEGAL_NOTICE -> ClientSettingsDetail(
            title = "Aviso legal",
            subtitle = "Identificación y datos de contacto del prestador del servicio.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientLegalPublicationNotice(
                title = "Documento pendiente de publicación",
                body = "Mostraremos aquí la razón social, NIF, domicilio, datos registrales si proceden y un canal de contacto directo. Estos datos deben corresponder a la entidad que realmente contrata y factura el servicio.",
            )
        }
        ClientSettingsPanel.TERMS -> ClientSettingsDetail(
            title = "Términos de uso",
            subtitle = "Condiciones de la suscripción y del uso del portal.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientLegalPublicationNotice(
                title = "Documento pendiente de publicación",
                body = "Publicaremos una versión descargable con la identidad legal, precio, impuestos, renovación, baja y acuerdo de tratamiento revisados. No te pediremos aceptar un documento incompleto.",
            )
        }
        ClientSettingsPanel.PRIVACY -> ClientSettingsDetail(
            title = "Política de privacidad",
            subtitle = "Cómo se tratan los datos de tu cuenta y cómo ejercer tus derechos.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientLegalPublicationNotice(
                title = "Documento pendiente de publicación",
                body = "La política definitiva identificará al responsable, las finalidades, bases jurídicas, conservación, proveedores, transferencias y el canal de derechos. Una política informativa no se usará como consentimiento genérico.",
            )
        }
        ClientSettingsPanel.DATA_RIGHTS -> ClientSettingsDetail(
            title = "Tus datos y derechos",
            subtitle = "Acceso, rectificación, supresión, oposición, limitación y portabilidad.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientLegalPublicationNotice(
                title = "Canal de derechos pendiente de activar",
                body = "Antes de activarlo, configuraremos el responsable, el contacto de privacidad, la comprobación de identidad y el plazo de respuesta. Solicitar una baja del servicio no sustituye este derecho.",
            )
        }
        ClientSettingsPanel.COOKIES -> ClientSettingsDetail(
            title = "Cookies y comunicaciones",
            subtitle = "Preferencias de tecnologías no esenciales y comunicaciones comerciales.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientLegalPublicationNotice(
                title = "Preferencias pendientes de activar",
                body = "Las tecnologías esenciales para iniciar sesión y proteger la cuenta se informarán con transparencia. Las analíticas o de marketing solo se activarán mediante una elección separada y nunca con casillas premarcadas.",
            )
        }
        ClientSettingsPanel.CANCELLATION -> ClientSettingsDetail(
            title = "Dar de baja",
            subtitle = "Cancela la renovación con una fecha clara y conserva el control de tus datos.",
            onBack = { panel = ClientSettingsPanel.OVERVIEW },
        ) {
            ClientCancellationPanel(onContactSupport = onContactSupport)
        }
    }
}

@Composable
private fun ClientSettingsOverview(
    activeProgramCount: Int,
    onOpenPanel: (ClientSettingsPanel) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            title = "Ajustes",
            subtitle = "Cuenta, facturación, privacidad y condiciones de tu servicio.",
        )

        ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ClientPortalSectionTitle(
                "Tu portal, bajo control",
                "La información contractual y de pago se separa de las herramientas que usas para tu negocio.",
            )
            SettingsStatusRow(
                label = "Programas activos",
                value = if (activeProgramCount == 1) "1 programa incluido" else "$activeProgramCount programas",
                isPositive = true,
            )
        }

        ClientSettingsSectionTitle("Cuenta")
        SettingsEntry(
            title = "Cuenta y seguridad",
            description = "Datos de la organización, accesos y protección de la cuenta.",
            icon = Lucide.UserRound,
            onClick = { onOpenPanel(ClientSettingsPanel.ACCOUNT_SECURITY) },
        )

        ClientSettingsSectionTitle("Suscripción")
        SettingsEntry(
            title = "Facturación",
            description = "Plan CRM, programas, datos fiscales, pagos y facturas de RequenaDesk.",
            icon = Lucide.ReceiptText,
            onClick = { onOpenPanel(ClientSettingsPanel.BILLING) },
        )

        ClientSettingsSectionTitle("Privacidad y legal")
        SettingsEntry(
            title = "Aviso legal",
            description = "Identidad y contacto de la entidad que presta el servicio.",
            icon = Lucide.UserRound,
            onClick = { onOpenPanel(ClientSettingsPanel.LEGAL_NOTICE) },
        )
        SettingsEntry(
            title = "Términos de uso",
            description = "Condiciones de la suscripción, uso permitido, renovación y baja.",
            icon = Lucide.ListTodo,
            onClick = { onOpenPanel(ClientSettingsPanel.TERMS) },
        )
        SettingsEntry(
            title = "Política de privacidad",
            description = "Datos tratados, derechos, conservación y proveedores.",
            icon = Lucide.CircleCheck,
            onClick = { onOpenPanel(ClientSettingsPanel.PRIVACY) },
        )
        SettingsEntry(
            title = "Tus datos y derechos",
            description = "Gestiona solicitudes de privacidad y conoce el tratamiento de tus datos.",
            icon = Lucide.ListTodo,
            onClick = { onOpenPanel(ClientSettingsPanel.DATA_RIGHTS) },
        )
        SettingsEntry(
            title = "Cookies y comunicaciones",
            description = "Configura tecnologías no esenciales y comunicaciones comerciales.",
            icon = Lucide.CircleCheck,
            onClick = { onOpenPanel(ClientSettingsPanel.COOKIES) },
        )

        ClientSettingsSectionTitle("Finalizar servicio")
        SettingsEntry(
            title = "Dar de baja",
            description = "Solicita el fin de la renovación y prepara la exportación de tus datos.",
            icon = Lucide.Columns3,
            isDestructive = true,
            onClick = { onOpenPanel(ClientSettingsPanel.CANCELLATION) },
        )
    }
}

@Composable
private fun ClientSettingsDetail(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SecondaryButton(text = "Volver a Ajustes", icon = Lucide.ChevronLeft, onClick = onBack)
        ClientPortalPageHeader(title = title, subtitle = subtitle)
        content()
    }
}

@Composable
private fun ClientBillingPanel(
    activeProgramCount: Int,
    onManagePrograms: () -> Unit,
) {
    // The free-beta framing is the headline fact of this whole panel - worth the hero treatment.
    ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth(), emphasized = true) {
        ClientPortalSectionTitle(
            "Programas durante la beta",
            "Todos los programas están disponibles sin coste mientras realizamos las pruebas. Su activación requiere autorización del administrador.",
        )
        SettingsStatusRow(
            "Programas activos",
            if (activeProgramCount == 1) "1 autorizado · gratis durante la beta" else "$activeProgramCount autorizados · gratis durante la beta",
            isPositive = true,
        )
        PrimaryButton(
            text = "Gestionar programas",
            icon = Lucide.Columns3,
            onClick = onManagePrograms,
        )
    }

    ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        ClientPortalSectionTitle(
            "Facturación de la suscripción",
            "No se emitirán cargos ni facturas durante esta beta gratuita.",
        )
        SettingsStatusRow("Estado", "Beta gratuita activa", isPositive = true)
        Text(
            "Cualquier cambio comercial futuro se comunicará y requerirá una aceptación expresa antes de aplicarse.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        ClientPortalSectionTitle(
            "Método de pago y facturas",
            "Durante la beta no solicitamos ningún método de pago.",
        )
        SettingsStatusRow("Método de pago", "No requerido", isPositive = true)
        SettingsStatusRow("Facturas de RequenaDesk", "No se emitirán durante la beta", isPositive = true)
        Text(
            "Las facturas que emites a tus propios clientes se gestionan en el programa Facturación, no en este apartado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClientCancellationPanel(onContactSupport: () -> Unit) {
    ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        ClientPortalSectionTitle(
            "Antes de solicitar la baja",
            "Cuando la suscripción esté activa, podrás cancelar la renovación al final del período y solicitar una exportación de datos.",
        )
        Text(
            "Cancelar el servicio no elimina de inmediato las facturas, evidencias contractuales o datos que deban conservarse o bloquearse por ley.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsStatusRow("Estado de la baja", "No hay una suscripción de cobro activada", isPositive = false)
        SecondaryButton(
            text = "Contactar con soporte",
            onClick = onContactSupport,
        )
    }
}

@Composable
private fun ClientLegalPublicationNotice(title: String, body: String) {
    ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        ClientPortalSectionTitle(title, "Estado: pendiente de revisión y publicación.")
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "La versión vigente, su fecha y una descarga se mostrarán aquí antes de requerir cualquier aceptación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClientSettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsEntry(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val contentColor = if (isDestructive) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isDestructive) SupportDeskThemeTokens.semanticColors.dangerContainer.copy(alpha = 0.38f) else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, if (isDestructive) SupportDeskThemeTokens.semanticColors.danger.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isDestructive) SupportDeskThemeTokens.semanticColors.danger.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = contentColor,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) contentColor.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(imageVector = Lucide.ChevronRight, contentDescription = "Abrir $title")
        }
    }
}

@Composable
private fun SettingsStatusRow(label: String, value: String, isPositive: Boolean) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                imageVector = if (isPositive) Lucide.CircleCheck else Lucide.ListTodo,
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
                tint = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
