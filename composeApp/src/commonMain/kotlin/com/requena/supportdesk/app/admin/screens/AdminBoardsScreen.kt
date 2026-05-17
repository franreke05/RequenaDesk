package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.boards.domain.model.Board
import com.requena.supportdesk.features.boards.domain.model.BoardCard
import com.requena.supportdesk.features.boards.domain.model.BoardColumn
import com.requena.supportdesk.features.boards.presentation.event.BoardsUiEvent
import com.requena.supportdesk.features.boards.presentation.state.BoardsUiState

private fun String.parseHexColor(): Color {
    val hexString = this.removePrefix("#")
    return try {
        Color(hexString.toLong(16).toInt() or 0xFF000000.toInt())
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun AdminBoardsScreen(
    state: BoardsUiState,
    onEvent: (BoardsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var showCreateBoard by remember { mutableStateOf(false) }
    var newBoardName by remember { mutableStateOf("") }
    var newBoardDescription by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Encabezado con botón crear tablero
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tableros Kanban",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { showCreateBoard = !showCreateBoard },
                modifier = Modifier.height(40.dp),
            ) {
                Text("+ Nuevo Tablero")
            }
        }

        // Formulario crear tablero (expandible)
        if (showCreateBoard) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(spacing.md))
                    .padding(spacing.md),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    OutlinedTextField(
                        value = newBoardName,
                        onValueChange = { newBoardName = it },
                        label = { Text("Nombre del tablero") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        value = newBoardDescription,
                        onValueChange = { newBoardDescription = it },
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newBoardName.isNotBlank()) {
                                    onEvent(BoardsUiEvent.CreateBoard(newBoardName, newBoardDescription))
                                    newBoardName = ""
                                    newBoardDescription = ""
                                    showCreateBoard = false
                                }
                            }
                        ),
                        maxLines = 3,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Button(
                            onClick = {
                                if (newBoardName.isNotBlank()) {
                                    onEvent(BoardsUiEvent.CreateBoard(newBoardName, newBoardDescription))
                                    newBoardName = ""
                                    newBoardDescription = ""
                                    showCreateBoard = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Crear")
                        }
                        Button(
                            onClick = { showCreateBoard = false },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }

        // Lista de tableros o Vista Kanban
        if (state.boards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No hay tableros creados. Crea uno para comenzar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (state.selectedBoard == null) {
            // Lista de tableros
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                items(state.boards) { board ->
                    BoardCard(
                        board = board,
                        onClick = { onEvent(BoardsUiEvent.SelectBoard(board.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else if (state.selectedBoard != null) {
            // Vista Kanban del tablero seleccionado
            KanbanBoardView(
                board = state.selectedBoard!!,
                selectedCard = state.selectedCard,
                onSelectCard = { cardId -> onEvent(BoardsUiEvent.SelectCard(cardId)) },
                onMoveCard = { cardId, columnId, position ->
                    onEvent(BoardsUiEvent.MoveCard(cardId, columnId, position))
                },
                onBack = { onEvent(BoardsUiEvent.SelectBoard(null)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoardCard(
    board: Board,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(spacing.md))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(spacing.md))
            .clickable(onClick = onClick)
            .padding(spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = board.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (board.description.isNotBlank()) {
                Text(
                    text = board.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = "${board.columns.size} columnas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${board.cards.size} tarjetas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KanbanBoardView(
    board: Board,
    selectedCard: BoardCard?,
    onSelectCard: (String) -> Unit,
    onMoveCard: (String, String, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Column(modifier = modifier.fillMaxSize()) {
        // Encabezado del tablero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (board.description.isNotBlank()) {
                    Text(
                        text = board.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onBack) {
                Text("Volver a Tableros")
            }
        }

        // Vista Kanban: Columnas horizontales
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            board.columns.forEach { column ->
                KanbanColumn(
                    column = column,
                    cards = board.cards.filter { it.columnId == column.id && !it.isHidden }.sortedBy { it.position },
                    selectedCard = selectedCard,
                    onSelectCard = onSelectCard,
                    onMoveCard = { cardId, position -> onMoveCard(cardId, column.id, position) },
                )
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    column: BoardColumn,
    cards: List<BoardCard>,
    selectedCard: BoardCard?,
    onSelectCard: (String) -> Unit,
    onMoveCard: (String, Int) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(spacing.md))
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        // Encabezado de columna
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .background(
                        column.colorHex.parseHexColor(),
                        RoundedCornerShape(2.dp),
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${cards.size} tarjetas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Lista de tarjetas
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            items(cards) { card ->
                KanbanCardItem(
                    card = card,
                    isSelected = selectedCard?.id == card.id,
                    onClick = { onSelectCard(card.id) },
                )
            }
        }
    }
}

@Composable
private fun KanbanCardItem(
    card: BoardCard,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(spacing.sm),
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(spacing.sm),
            )
            .clickable(onClick = onClick)
            .padding(spacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = "Ticket #${card.ticketId.take(8)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = "Creado: ${card.createdAt.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
