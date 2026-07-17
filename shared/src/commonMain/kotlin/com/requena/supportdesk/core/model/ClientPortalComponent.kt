package com.requena.supportdesk.core.model

/**
 * Add-on capabilities a client can have enabled from the admin workspace.
 *
 * Keep this enum small and backed by server-side entitlements: Compose may hide
 * unavailable UI, but Ktor remains the authority for access to future APIs.
 */
enum class ClientPortalComponent {
    SERVICE_SLA,
}

fun ClientPortalComponent.displayName(): String = when (this) {
    ClientPortalComponent.SERVICE_SLA -> "Servicio y SLA"
}

fun ClientPortalComponent.description(): String = when (this) {
    ClientPortalComponent.SERVICE_SLA -> "Consumo de servicio, actividad y seguimiento de soporte."
}
