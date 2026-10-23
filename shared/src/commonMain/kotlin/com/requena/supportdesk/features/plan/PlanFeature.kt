package com.requena.supportdesk.features.plan

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.core.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable

// --- Modules and models ----------------------------------------------------------------

/** Plan modules (server V19). The staff screens they unlock are in [moduleFor]. */
enum class PlanModule(val label: String) {
    CORE("Núcleo"),
    WORK("Trabajo y horas"),
    SUPPORT("Soporte"),
    SALES("Ventas"),
    BILLING("Facturación y gastos"),
    AGENDA("Agenda y reservas"),
    DOCUMENTS("Documentos"),
    PORTAL("Portal de cliente"),
    REPORTS("Informes"),
    ;

    companion object {
        fun fromKey(key: String): PlanModule? = entries.firstOrNull { it.name == key }
    }
}

enum class BillingPeriod(val label: String) { MONTHLY("Mensual"), ANNUAL("Anual") }

data class PlanOffer(
    val key: String,
    val name: String,
    val monthlyPriceCents: Int,
    val annualMonthlyPriceCents: Int,
    val includedUsers: Int?,
    val maxClients: Int?,
    val modules: Set<PlanModule>,
    val selectableFreeModules: Set<PlanModule>,
) {
    fun priceCents(period: BillingPeriod): Int = if (period == BillingPeriod.ANNUAL) annualMonthlyPriceCents else monthlyPriceCents
}

data class CompanyPlan(
    val planKey: String,
    val planName: String,
    /** TRIAL, ACTIVE, BETA or TRIAL_ENDED (an ended trial behaves as Free). */
    val status: String,
    val modules: Set<PlanModule>,
    val includedUsers: Int?,
    val maxClients: Int?,
    val trialDaysLeft: Int?,
    val freeModule: PlanModule?,
    val billingPeriod: BillingPeriod?,
) {
    fun includes(module: PlanModule): Boolean = module in modules
    val isTrial: Boolean get() = status == "TRIAL"
    val trialEnded: Boolean get() = status == "TRIAL_ENDED"
}

data class PlanRequest(
    val id: String,
    val organizationName: String,
    val planKey: String,
    val billingPeriod: BillingPeriod,
    val quotedMonthlyPriceCents: Int,
    val status: String,
    val requestedByName: String,
    val createdAt: String,
)

data class PlanOverview(
    val plan: CompanyPlan,
    val staffUsers: Int,
    val clients: Int,
    val catalog: List<PlanOffer>,
    val pendingRequest: PlanRequest?,
    val isPlatformOperator: Boolean,
)

/** "29 €" / "24,50 €" from cents, prices without VAT. */
fun formatPlanPrice(cents: Int): String {
    val euros = cents / 100
    val rest = cents % 100
    return if (rest == 0) "$euros €" else "$euros,${rest.toString().padStart(2, '0')} €"
}

/**
 * Plan the onboarding answers point to (pure): a team of 6+ → Equipo; support or documents
 * → Pro; invoicing, bookings or time → Autónomo; nothing → Gratis.
 */
fun recommendedPlan(goals: Set<OnboardingGoal>, teamSize: String): String = when {
    teamSize == "6-20" || teamSize == "Más de 20" -> "TEAM"
    OnboardingGoal.SUPPORT in goals || OnboardingGoal.DOCUMENTS in goals || teamSize == "2-5" -> "PRO"
    goals.isNotEmpty() -> "AUTONOMO"
    else -> "FREE"
}

/** Plan offering a module, for the "Incluido en …" hint on locked screens (pure). */
fun cheapestPlanWith(module: PlanModule, catalog: List<PlanOffer>): PlanOffer? =
    catalog.filter { module in it.modules }.minByOrNull { it.monthlyPriceCents }

// --- DTOs and repository -----------------------------------------------------------------

@Serializable
private data class PlanDto(
    val key: String,
    val name: String,
    val monthlyPriceCents: Int,
    val annualMonthlyPriceCents: Int,
    val includedUsers: Int? = null,
    val maxClients: Int? = null,
    val modules: List<String> = emptyList(),
    val selectableFreeModules: List<String> = emptyList(),
)

@Serializable
private data class EffectivePlanDto(
    val planKey: String,
    val planName: String,
    val status: String,
    val modules: List<String> = emptyList(),
    val includedUsers: Int? = null,
    val maxClients: Int? = null,
    val trialDaysLeft: Int? = null,
    val freeModule: String? = null,
    val billingPeriod: String? = null,
)

@Serializable
private data class UsageDto(val staffUsers: Int = 0, val clients: Int = 0)

@Serializable
private data class PlanRequestDto(
    val id: String,
    val organizationName: String = "",
    val planKey: String,
    val billingPeriod: String = "MONTHLY",
    val quotedMonthlyPriceCents: Int = 0,
    val status: String,
    val requestedByName: String = "",
    val createdAt: String = "",
)

@Serializable
private data class OverviewDto(
    val plan: EffectivePlanDto,
    val usage: UsageDto = UsageDto(),
    val catalog: List<PlanDto> = emptyList(),
    val pendingRequest: PlanRequestDto? = null,
    val isPlatformOperator: Boolean = false,
)

@Serializable
private data class PlanRequestBody(val planKey: String, val billingPeriod: String, val acceptPrice: Boolean)

@Serializable
private data class FreeModuleBody(val module: String)

@Serializable
private data class DecisionBody(val note: String? = null)

private fun modulesOf(keys: List<String>) = keys.mapNotNull(PlanModule::fromKey).toSet()

private fun PlanDto.toModel() = PlanOffer(
    key, name, monthlyPriceCents, annualMonthlyPriceCents, includedUsers, maxClients,
    modulesOf(modules), modulesOf(selectableFreeModules),
)

private fun PlanRequestDto.toModel() = PlanRequest(
    id, organizationName, planKey,
    BillingPeriod.entries.firstOrNull { it.name == billingPeriod } ?: BillingPeriod.MONTHLY,
    quotedMonthlyPriceCents, status, requestedByName, createdAt,
)

private fun OverviewDto.toModel() = PlanOverview(
    plan = CompanyPlan(
        planKey = plan.planKey,
        planName = plan.planName,
        status = plan.status,
        modules = modulesOf(plan.modules),
        includedUsers = plan.includedUsers,
        maxClients = plan.maxClients,
        trialDaysLeft = plan.trialDaysLeft,
        freeModule = plan.freeModule?.let(PlanModule::fromKey),
        billingPeriod = plan.billingPeriod?.let { key -> BillingPeriod.entries.firstOrNull { it.name == key } },
    ),
    staffUsers = usage.staffUsers,
    clients = usage.clients,
    catalog = catalog.map { it.toModel() },
    pendingRequest = pendingRequest?.toModel(),
    isPlatformOperator = isPlatformOperator,
)

interface PlanRepository {
    suspend fun overview(): AppResult<PlanOverview>
    suspend fun request(planKey: String, period: BillingPeriod): AppResult<PlanOverview>
    suspend fun cancel(requestId: String): AppResult<Unit>
    suspend fun chooseFreeModule(module: PlanModule): AppResult<PlanOverview>
    suspend fun operatorQueue(): AppResult<List<PlanRequest>>
    suspend fun approve(requestId: String, note: String?): AppResult<Unit>
    suspend fun reject(requestId: String, note: String?): AppResult<Unit>
}

class RemotePlanRepository(private val httpClient: HttpClient) : PlanRepository {
    private val base get() = supportDeskBaseUrl()

    override suspend fun overview() = call { httpClient.get("$base/org/plan").requireApiData<OverviewDto>().toModel() }

    override suspend fun request(planKey: String, period: BillingPeriod) = call {
        httpClient.post("$base/org/plan/requests") {
            // The user confirmed the price shown (always the server's) before this call.
            setBody(jsonRequestBody(PlanRequestBody(planKey, period.name, acceptPrice = true)))
        }.requireApiData<OverviewDto>().toModel()
    }

    override suspend fun cancel(requestId: String) = call { httpClient.delete("$base/org/plan/requests/$requestId").requireSuccess() }

    override suspend fun chooseFreeModule(module: PlanModule) = call {
        httpClient.patch("$base/org/plan/free-module") { setBody(jsonRequestBody(FreeModuleBody(module.name))) }
            .requireApiData<OverviewDto>().toModel()
    }

    override suspend fun operatorQueue() = call {
        httpClient.get("$base/platform/plan-requests?status=REQUESTED").requireApiData<List<PlanRequestDto>>().map { it.toModel() }
    }

    override suspend fun approve(requestId: String, note: String?) = call {
        httpClient.post("$base/platform/plan-requests/$requestId/approve") { setBody(jsonRequestBody(DecisionBody(note))) }.requireSuccess()
    }

    override suspend fun reject(requestId: String, note: String?) = call {
        httpClient.post("$base/platform/plan-requests/$requestId/reject") { setBody(jsonRequestBody(DecisionBody(note))) }.requireSuccess()
    }

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppResult.Error(planMessage(error.message.orEmpty()), error)
        }
}

/** Spanish text for plan errors (also used app-wide for 402/409 from other screens). */
fun planMessage(raw: String): String {
    val normalized = raw.lowercase()
    val module = Regex("module not included in your plan: ([a-z_]+)").find(normalized)
        ?.groupValues?.get(1)?.uppercase()?.let(PlanModule::fromKey)
    return when {
        module != null -> "Tu plan no incluye ${module.label}. Puedes cambiarlo en Ajustes › Plan y pagos; tus datos se conservan."
        "client limit of your plan" in normalized -> "Has llegado al máximo de clientes de tu plan. Cambia de plan para añadir más."
        "user limit of your plan" in normalized -> "Has llegado al máximo de usuarios de tu plan. La gestoría no cuenta: puedes invitarla igualmente."
        "already a pending plan request" in normalized -> "Ya tienes una solicitud de plan pendiente."
        "price must be accepted" in normalized -> "Tienes que aceptar el precio para pedir un plan de pago."
        "only the platform operator" in normalized -> "Solo OryKai puede aprobar planes."
        "no longer pending" in normalized || "plan request not found" in normalized -> "Esa solicitud ya no está pendiente."
        "cannot be chosen for the free plan" in normalized -> "Ese módulo no se puede elegir en el plan Gratis."
        raw.isBlank() -> "No se pudo completar la operación."
        else -> raw
    }
}

// --- ViewModel ---------------------------------------------------------------------------

data class PlanUiState(
    val overview: PlanOverview? = null,
    val operatorQueue: List<PlanRequest> = emptyList(),
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PlanUiEvent {
    object Load : PlanUiEvent
    data class PeriodChanged(val period: BillingPeriod) : PlanUiEvent
    data class Request(val planKey: String) : PlanUiEvent
    data class Cancel(val requestId: String) : PlanUiEvent
    data class ChooseFreeModule(val module: PlanModule) : PlanUiEvent
    data class Approve(val requestId: String, val note: String?) : PlanUiEvent
    data class Reject(val requestId: String, val note: String?) : PlanUiEvent
}

sealed interface PlanUiEffect {
    data class ShowMessage(val message: String) : PlanUiEffect
}

class PlanViewModel(private val repository: PlanRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PlanUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<PlanUiEffect> = _effects.asSharedFlow()

    private val writeMutex = Mutex()

    fun onEvent(event: PlanUiEvent) {
        when (event) {
            PlanUiEvent.Load -> load()
            is PlanUiEvent.PeriodChanged -> _state.update { it.copy(period = event.period) }
            is PlanUiEvent.Request -> write(
                success = { if (event.planKey == "FREE") "Ya estás en el plan Gratis." else "Solicitud enviada. OryKai la revisará y te enviará la factura; no se cobra nada automáticamente." },
            ) { repository.request(event.planKey, state.value.period).also(::applyOverview) }
            is PlanUiEvent.Cancel -> write(success = { "Solicitud cancelada." }) { repository.cancel(event.requestId).also { reload() } }
            is PlanUiEvent.ChooseFreeModule -> write(success = { "Módulo del plan Gratis: ${event.module.label}." }) {
                repository.chooseFreeModule(event.module).also(::applyOverview)
            }
            is PlanUiEvent.Approve -> write(success = { "Plan aprobado. Recuerda emitir la factura (carril A)." }) {
                repository.approve(event.requestId, event.note).also { reload() }
            }
            is PlanUiEvent.Reject -> write(success = { "Solicitud rechazada." }) {
                repository.reject(event.requestId, event.note).also { reload() }
            }
        }
    }

    private fun applyOverview(result: AppResult<PlanOverview>) {
        if (result is AppResult.Success) _state.update { it.copy(overview = result.data) }
    }

    private fun load() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            reload()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun reload() {
        when (val overview = repository.overview()) {
            is AppResult.Success -> {
                _state.update { it.copy(overview = overview.data, errorMessage = null) }
                if (overview.data.isPlatformOperator) {
                    (repository.operatorQueue() as? AppResult.Success)?.let { queue -> _state.update { it.copy(operatorQueue = queue.data) } }
                }
            }
            is AppResult.Error -> _state.update { it.copy(errorMessage = overview.message) }
        }
    }

    private fun write(success: () -> String, block: suspend () -> AppResult<*>) {
        guardedLaunch(writeMutex) {
            _state.update { it.copy(isSaving = true) }
            val message = when (val result = block()) {
                is AppResult.Success -> success()
                is AppResult.Error -> result.message
            }
            _state.update { it.copy(isSaving = false) }
            _effects.emit(PlanUiEffect.ShowMessage(message))
        }
    }
}
