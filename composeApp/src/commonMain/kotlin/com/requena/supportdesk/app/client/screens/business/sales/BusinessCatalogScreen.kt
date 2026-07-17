package com.requena.supportdesk.app.client.screens.business.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItemType
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovementType
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.presentation.BusinessCatalogUiState
import com.requena.supportdesk.features.business.sales.presentation.BusinessCatalogViewModel

@Composable
fun BusinessCatalogRoute(viewModel: BusinessCatalogViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    BusinessCatalogScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onCreateItem = viewModel::createItem,
        onUpdateItem = viewModel::updateItem,
        onArchiveItem = viewModel::archiveItem,
        onAdjustStock = viewModel::adjustStock,
        modifier = modifier,
    )
}

@Composable
fun BusinessCatalogScreen(
    state: BusinessCatalogUiState,
    onRefresh: () -> Unit,
    onCreateItem: (CreateBusinessCatalogItemInput) -> Unit,
    onUpdateItem: (String, UpdateBusinessCatalogItemInput) -> Unit,
    onArchiveItem: (String, Int) -> Unit,
    onAdjustStock: (String, StockAdjustmentInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var tab by remember { mutableStateOf("PRODUCT") }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        ClientPortalPageHeader("CatÃ¡logo y stock", "Productos, servicios y movimientos trazables. Las existencias no se editan directamente.")
        state.errorMessage?.let { ErrorState(message = it, onRetry = onRefresh) }
        CatalogCreateCard(state.isSaving, onCreateItem)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf("PRODUCT" to "Productos", "SERVICE" to "Servicios", "STOCK" to "Stock").forEach { (key, label) ->
                FilterChip(selected = tab == key, onClick = { tab = key }, label = { Text(label) })
            }
        }
        if (state.isLoading && state.items.isEmpty()) LoadingState()
        when (tab) {
            "STOCK" -> if (state.stock.isEmpty() && !state.isLoading) EmptyState("Sin stock que revisar", "Crea un producto con seguimiento de stock para registrar movimientos.") else {
                state.stock.forEach { summary ->
                    ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "Stock de ${summary.item.name}" }) {
                        Text(summary.item.name, style = MaterialTheme.typography.titleMedium)
                        Text("Disponible: ${formatMilli(summary.availableMilli)} ${summary.item.unit}")
                        if (summary.isBelowMinimum) Text("Stock por debajo del mÃ­nimo", color = MaterialTheme.colorScheme.error)
                        StockAdjustmentForm(summary.item.id, state.isSaving, onAdjustStock)
                    }
                }
            }
            else -> {
                val items = state.items.filter { it.type.name == tab }
                if (items.isEmpty() && !state.isLoading) EmptyState("No hay ${if (tab == "PRODUCT") "productos" else "servicios"}", "El catÃ¡logo estÃ¡ vacÃ­o; aÃ±ade un registro real cuando estÃ© disponible.")
                items.forEach { item ->
                    CatalogItemEditor(item, state.isSaving, onUpdateItem, onArchiveItem)
                }
            }
        }
    }
}

@Composable
private fun CatalogItemEditor(
    item: com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem,
    isSaving: Boolean,
    onUpdate: (String, UpdateBusinessCatalogItemInput) -> Unit,
    onArchive: (String, Int) -> Unit,
) {
    var name by remember(item.id, item.version) { mutableStateOf(item.name) }
    var price by remember(item.id, item.version) { mutableStateOf(item.referencePriceCents.toString()) }
    var unit by remember(item.id, item.version) { mutableStateOf(item.unit) }
    ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "${if (item.type == BusinessCatalogItemType.PRODUCT) "Producto" else "Servicio"} ${item.name}" }) {
        Text(if (item.archived) "Artículo archivado" else item.name, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving && !item.archived)
        OutlinedTextField(unit, { unit = it }, Modifier.fillMaxWidth(), label = { Text("Unidad") }, enabled = !isSaving && !item.archived)
        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Precio de referencia en céntimos") }, enabled = !isSaving && !item.archived)
        Text(if (item.tracksStock) "Seguimiento de stock activo" else "Sin seguimiento de stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!item.archived) {
            PrimaryButton(
                "Guardar artículo",
                fullWidth = true,
                enabled = name.isNotBlank() && unit.isNotBlank() && price.toLongOrNull() != null && !isSaving,
                onClick = {
                    onUpdate(
                        item.id,
                        UpdateBusinessCatalogItemInput(
                            name = name,
                            sku = item.sku,
                            description = item.description,
                            unit = unit,
                            referencePriceCents = requireNotNull(price.toLongOrNull()),
                            tracksStock = item.tracksStock,
                            stockMinimumMilli = item.stockMinimumMilli,
                            expectedVersion = item.version,
                        ),
                    )
                },
            )
            SecondaryButton("Archivar artículo", fullWidth = true, enabled = !isSaving, onClick = { onArchive(item.id, item.version) })
        }
    }
}

@Composable
private fun CatalogCreateCard(isSaving: Boolean, onCreate: (CreateBusinessCatalogItemInput) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var type by remember { mutableStateOf(BusinessCatalogItemType.PRODUCT) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var tracksStock by remember { mutableStateOf(true) }
    ClientPortalSurfaceCard {
        Text("Nuevo artÃ­culo", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            FilterChip(type == BusinessCatalogItemType.PRODUCT, { type = BusinessCatalogItemType.PRODUCT; tracksStock = true }, { Text("Producto") })
            FilterChip(type == BusinessCatalogItemType.SERVICE, { type = BusinessCatalogItemType.SERVICE; tracksStock = false }, { Text("Servicio") })
            if (type == BusinessCatalogItemType.PRODUCT) FilterChip(tracksStock, { tracksStock = !tracksStock }, { Text("Controlar stock") })
        }
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Precio de referencia en cÃ©ntimos") }, enabled = !isSaving)
        PrimaryButton("Crear artÃ­culo", fullWidth = true, isLoading = isSaving, enabled = name.isNotBlank() && price.toLongOrNull() != null, onClick = {
            onCreate(CreateBusinessCatalogItemInput(type, name, referencePriceCents = requireNotNull(price.toLongOrNull()), tracksStock = tracksStock))
            name = ""; price = ""
        })
    }
}

@Composable
private fun StockAdjustmentForm(itemId: String, isSaving: Boolean, onAdjust: (String, StockAdjustmentInput) -> Unit) {
    var delta by remember(itemId) { mutableStateOf("") }
    var reason by remember(itemId) { mutableStateOf("") }
    OutlinedTextField(delta, { delta = it.filter { char -> char.isDigit() || char == '-' } }, Modifier.fillMaxWidth(), label = { Text("Ajuste en milÃ©simas (p. ej., 1000)") }, enabled = !isSaving)
    OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Motivo del ajuste") }, enabled = !isSaving)
    SecondaryButton("Registrar movimiento", fullWidth = true, enabled = delta.toLongOrNull()?.let { it != 0L } == true && reason.isNotBlank() && !isSaving, onClick = {
        onAdjust(itemId, StockAdjustmentInput(BusinessStockMovementType.ADJUSTMENT, requireNotNull(delta.toLongOrNull()), reason, newIdempotencyKey(itemId)))
        delta = ""; reason = ""
    })
}

private fun formatCents(value: Long): String = "${value / 100},${(value % 100).toString().padStart(2, '0')}"
private fun formatMilli(value: Long): String = "${value / 1000},${(value % 1000).toString().padStart(3, '0')}"
private fun newIdempotencyKey(scope: String): String = "ui-$scope-${kotlin.random.Random.nextLong().toString().replace('-', 'n')}"
