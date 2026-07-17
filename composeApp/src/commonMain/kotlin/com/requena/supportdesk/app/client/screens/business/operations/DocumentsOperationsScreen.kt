package com.requena.supportdesk.app.client.screens.business.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.operations.ClientDocumentDto
import com.requena.supportdesk.features.business.operations.CreateDocumentDto
import com.requena.supportdesk.features.business.operations.OperationsUiEvent
import com.requena.supportdesk.features.business.operations.OperationsUiState

/** Documents are metadata-first: uploads are issued by the backend and files are never selected from a public URL. */
@Composable
fun DocumentsOperationsScreen(state: OperationsUiState, onEvent: (OperationsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    var title by remember { mutableStateOf("") }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        item { ClientPortalPageHeader("Documentos", "Archivos privados, versiones inmutables y confirmaciones autenticadas. No sustituye una firma cualificada.") }
        item {
            ClientPortalSurfaceCard {
                Text("Crear documento", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onEvent(OperationsUiEvent.CreateDocument(CreateDocumentDto(title))); title = "" }, enabled = title.isNotBlank()) { Text("Crear y preparar subida") }
            }
        }
        item { Text("Tus documentos", fontWeight = FontWeight.SemiBold) }
        items(state.documents, key = ClientDocumentDto::id) { document ->
            ClientPortalSurfaceCard {
                Text(document.title, fontWeight = FontWeight.SemiBold)
                Text("Estado: ${document.status}")
                Text("Las nuevas versiones se suben mediante una URL privada caducable.")
            }
        }
    }
}
