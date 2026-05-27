package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.model.WorkTaskStatus
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.boards.domain.model.Board
import com.requena.supportdesk.features.boards.domain.model.BoardCard
import com.requena.supportdesk.features.boards.domain.model.BoardColumn
import com.requena.supportdesk.features.boards.presentation.event.BoardsUiEvent
import com.requena.supportdesk.features.boards.presentation.state.BoardsUiState
import kotlin.math.roundToInt

private val DoneColumnStatuses = setOf("RESOLVED", "CLOSED")
private val HoldColumnStatuses = setOf("PENDING_CLIENT", "WAITING_CLIENT")

private fun String.parseHexColor(): Color = runCatching {
    val hex = removePrefix("#")
    Color(hex.toLong(16).toInt() or 0xFF000000.toInt())
}.getOrElse { Color.Gray }

private fun columnStatusToTaskStatus(status: String): WorkTaskStatus = when (status) {
    "OPEN", "TODO" -> WorkTaskStatus.TODO
    "IN_PROGRESS" -> WorkTaskStatus.IN_PROGRESS
    "PENDING_CLIENT" -> WorkTaskStatus.WAITING_CLIENT
    "REVIEW" -> WorkTaskStatus.REVIEW
    "RESOLVED", "DONE", "CLOSED" -> WorkTaskStatus.DONE
    else -> WorkTaskStatus.TODO
}

private fun columnStatusToTicketStatus(status: String): TicketStatus? =
    runCatching { TicketStatus.valueOf(status) }.getOrNull()

private fun String.initials(): String {
    val words = trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "?"
    }
}

@Composable
fun AdminBoardsScreen(
    state: BoardsUiState,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    onEvent: (BoardsUiEvent) -> Unit,
    onTicketStatusChange: (String, TicketStatus) -> Unit,
    onTaskStatusChange: (String, WorkTaskStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedBoard = state.selectedBoard
    val boardAccent = selectedBoard?.columns
        ?.minByOrNull { it.position }
        ?.colorHex
        ?.parseHexColor()
        ?: MaterialTheme.colorScheme.primary
    val summary = buildBoardSummary(state.boards, selectedBoard)

    val onMove: (BoardCard, BoardColumn) -> Unit = { card, toColumn ->
        onEvent(BoardsUiEvent.MoveCard(card.id, toColumn.id, 0))
        card.ticketId?.let { ticketId ->
            columnStatusToTicketStatus(toColumn.status)?.let { status ->
                onTicketStatusChange(ticketId, status)
            }
        }
        card.taskId?.let { taskId ->
            onTaskStatusChange(taskId, columnStatusToTaskStatus(toColumn.status))
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val narrow = maxWidth < 1120.dp

        if (state.boards.isEmpty()) {
            SupportDeskEntrance(index = 0) {
                EmptyBoardsState(
                    loading = state.isLoading,
                    message = state.statusMessage,
                    accentColor = boardAccent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            return@BoxWithConstraints
        }

        if (compact) {
            SupportDeskEntrance(index = 0) {
                CompactBoardsLayout(
                    state = state,
                    summary = summary,
                    boardAccent = boardAccent,
                    tasks = tasks,
                    categories = categories,
                    onSelectBoard = { onEvent(BoardsUiEvent.SelectBoard(it)) },
                    onSelectCard = { onEvent(BoardsUiEvent.SelectCard(it)) },
                    onMoveCard = onMove,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                SupportDeskEntrance(index = 0) {
                    CommandCenterHeader(
                        state = state,
                        summary = summary,
                        boardAccent = boardAccent,
                        compact = narrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (narrow) {
                    SupportDeskEntrance(index = 1) {
                        BoardSelectorStrip(
                            boards = state.boards,
                            selectedBoardId = selectedBoard?.id,
                            onSelect = { onEvent(BoardsUiEvent.SelectBoard(it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    if (!narrow) {
                        SupportDeskEntrance(index = 1, horizontal = true) {
                            ClientsPanel(
                                boards = state.boards,
                                selectedBoardId = selectedBoard?.id,
                                onSelect = { onEvent(BoardsUiEvent.SelectBoard(it)) },
                                modifier = Modifier
                                    .widthIn(min = 248.dp, max = 300.dp)
                                    .fillMaxHeight(),
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    }

                    SupportDeskEntrance(index = 2, horizontal = true, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        BoardStage(
                            state = state,
                            tasks = tasks,
                            categories = categories,
                            boardAccent = boardAccent,
                            compactColumns = false,
                            onSelectCard = { onEvent(BoardsUiEvent.SelectCard(it)) },
                            onMoveCard = onMove,
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBoardsLayout(
    state: BoardsUiState,
    summary: BoardSummary,
    boardAccent: Color,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    onSelectBoard: (String) -> Unit,
    onSelectCard: (String) -> Unit,
    onMoveCard: (BoardCard, BoardColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        contentPadding = PaddingValues(bottom = spacing.md),
    ) {
        item {
            CommandCenterHeader(
                state = state,
                summary = summary,
                boardAccent = boardAccent,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            BoardSelectorStrip(
                boards = state.boards,
                selectedBoardId = state.selectedBoard?.id,
                onSelect = onSelectBoard,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            BoardStage(
                state = state,
                tasks = tasks,
                categories = categories,
                boardAccent = boardAccent,
                compactColumns = true,
                onSelectCard = onSelectCard,
                onMoveCard = onMoveCard,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CommandCenterHeader(
    state: BoardsUiState,
    summary: BoardSummary,
    boardAccent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedBoard = state.selectedBoard

    BoxWithConstraints(modifier = modifier) {
        val stacked = compact || maxWidth < 980.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CommandHeroPanel(
                    selectedBoard = selectedBoard,
                    summary = summary,
                    loading = state.isLoading,
                    statusMessage = state.statusMessage,
                    boardAccent = boardAccent,
                    modifier = Modifier.fillMaxWidth(),
                )
                CompactMetricGrid(summary = summary, boardAccent = boardAccent)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                CommandHeroPanel(
                    selectedBoard = selectedBoard,
                    summary = summary,
                    loading = state.isLoading,
                    statusMessage = state.statusMessage,
                    boardAccent = boardAccent,
                    modifier = Modifier.weight(1.35f),
                )
                MetricCard(
                    label = "Tableros",
                    value = summary.boardCount.toString(),
                    supportingText = "${summary.totalCards} tarjetas visibles",
                    neonAccentColor = boardAccent,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Tickets",
                    value = summary.ticketCards.toString(),
                    supportingText = "${summary.holdCards} en espera",
                    neonAccentColor = SupportDeskThemeTokens.semanticColors.warning,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Tareas",
                    value = summary.taskCards.toString(),
                    supportingText = "${summary.doneCards} terminadas",
                    neonAccentColor = SupportDeskThemeTokens.semanticColors.success,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompactMetricGrid(
    summary: BoardSummary,
    boardAccent: Color,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val singleColumn = maxWidth < 480.dp
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    label = "Tarjetas",
                    value = summary.totalCards.toString(),
                    supportingText = "${summary.boardCount} tableros",
                    neonAccentColor = boardAccent,
                    modifier = Modifier.fillMaxWidth(),
                )
                MetricCard(
                    label = "Tickets",
                    value = summary.ticketCards.toString(),
                    supportingText = "${summary.holdCards} en espera",
                    neonAccentColor = semantic.warning,
                    modifier = Modifier.fillMaxWidth(),
                )
                MetricCard(
                    label = "Tareas",
                    value = summary.taskCards.toString(),
                    supportingText = "${summary.doneCards} terminadas",
                    neonAccentColor = semantic.success,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MetricCard(
                    label = "Tarjetas",
                    value = summary.totalCards.toString(),
                    supportingText = "${summary.boardCount} tableros",
                    neonAccentColor = boardAccent,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Tickets",
                    value = summary.ticketCards.toString(),
                    supportingText = "${summary.holdCards} en espera",
                    neonAccentColor = semantic.warning,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Tareas",
                    value = summary.taskCards.toString(),
                    supportingText = "${summary.doneCards} terminadas",
                    neonAccentColor = semantic.success,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CommandHeroPanel(
    selectedBoard: Board?,
    summary: BoardSummary,
    loading: Boolean,
    statusMessage: String?,
    boardAccent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val completion by animateFloatAsState(
        targetValue = summary.completionRate,
        animationSpec = tween(550),
    )

    Surface(
        modifier = modifier
            .heightIn(min = 132.dp)
            .shadow(SupportDeskThemeTokens.elevations.subtle, RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        boardAccent.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    ),
                ),
                RoundedCornerShape(18.dp),
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            boardAccent.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                        ),
                    ),
                )
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommandLabel(
                    text = if (selectedBoard == null) "CONTROL GLOBAL" else "TABLERO ACTIVO",
                    color = boardAccent,
                )
                if (loading) {
                    StatusPill(
                        text = "Sincronizando",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    StatusPill(
                        text = "${(completion * 100).roundToInt()}% cerrado",
                        containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                        contentColor = SupportDeskThemeTokens.semanticColors.success,
                    )
                }
            }

            Text(
                text = selectedBoard?.name ?: "Command center Kanban",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedBoard?.description?.takeIf { it.isNotBlank() }
                    ?: "Radar operativo de clientes, tickets y tareas en curso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            ProgressRail(
                progress = completion,
                accentColor = boardAccent,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniSignal(label = "Activas", value = summary.totalCards.toString(), color = boardAccent)
                MiniSignal(label = "Tickets", value = summary.ticketCards.toString(), color = SupportDeskThemeTokens.semanticColors.warning)
                MiniSignal(label = "Tareas", value = summary.taskCards.toString(), color = SupportDeskThemeTokens.semanticColors.success)
            }

            statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ClientsPanel(
    boards: List<Board>,
    selectedBoardId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = SupportDeskThemeTokens.elevations.subtle,
        shadowElevation = SupportDeskThemeTokens.elevations.subtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                CommandLabel(text = "CLIENTES", color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${boards.size} tableros de trabajo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                contentPadding = PaddingValues(bottom = spacing.xs),
            ) {
                items(boards, key = { it.id }) { board ->
                    val ticketCount = board.visibleCards().count { it.ticketId != null }
                    val taskCount = board.visibleCards().count { it.taskId != null }
                    ClientBoardRow(
                        board = board,
                        isSelected = board.id == selectedBoardId,
                        ticketCount = ticketCount,
                        taskCount = taskCount,
                        onClick = { onSelect(board.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardSelectorStrip(
    boards: List<Board>,
    selectedBoardId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(boards, key = { it.id }) { board ->
            BoardSelectorChip(
                board = board,
                isSelected = board.id == selectedBoardId,
                onClick = { onSelect(board.id) },
            )
        }
    }
}

@Composable
private fun BoardSelectorChip(
    board: Board,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val accent = board.columns.minByOrNull { it.position }?.colorHex?.parseHexColor() ?: MaterialTheme.colorScheme.primary
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(220),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        animationSpec = tween(220),
    )

    Surface(
        modifier = Modifier
            .widthIn(min = 148.dp, max = 220.dp)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${board.visibleCards().size} tarjetas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BoardStage(
    state: BoardsUiState,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    boardAccent: Color,
    compactColumns: Boolean,
    onSelectCard: (String) -> Unit,
    onMoveCard: (BoardCard, BoardColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBoard = state.selectedBoard
    if (selectedBoard == null) {
        SelectBoardEmptyState(
            accentColor = boardAccent,
            modifier = modifier,
        )
    } else {
        KanbanBoardView(
            board = selectedBoard,
            tickets = state.tickets,
            tasks = tasks,
            categories = categories,
            selectedCard = state.selectedCard,
            boardAccent = boardAccent,
            compactColumns = compactColumns,
            onSelectCard = onSelectCard,
            onMoveCard = onMoveCard,
            modifier = modifier,
        )
    }
}

@Composable
private fun ClientBoardRow(
    board: Board,
    isSelected: Boolean,
    ticketCount: Int,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val accent = board.columns.minByOrNull { it.position }?.colorHex?.parseHexColor() ?: MaterialTheme.colorScheme.primary
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
        animationSpec = tween(220),
    )
    val elevation: Dp by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = background,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = if (isSelected) 0.95f else 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = board.name.initials(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else accent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = board.description.ifBlank { "${board.columns.size} columnas" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CountPill(
                    text = "T $ticketCount",
                    containerColor = semantic.warningContainer,
                    contentColor = semantic.warning,
                )
                CountPill(
                    text = "K $taskCount",
                    containerColor = semantic.successContainer,
                    contentColor = semantic.success,
                )
            }
        }
    }
}

@Composable
private fun KanbanBoardView(
    board: Board,
    tickets: List<Ticket>,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    selectedCard: BoardCard?,
    boardAccent: Color,
    compactColumns: Boolean,
    onSelectCard: (String) -> Unit,
    onMoveCard: (BoardCard, BoardColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val orderedColumns = board.columns.sortedBy { it.position }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        BoardFocusPanel(
            board = board,
            boardAccent = boardAccent,
            modifier = Modifier.fillMaxWidth(),
        )

        if (compactColumns) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                orderedColumns.forEachIndexed { index, column ->
                    KanbanColumn(
                        column = column,
                        cards = board.cardsForColumn(column),
                        tickets = tickets,
                        tasks = tasks,
                        categories = categories,
                        selectedCard = selectedCard,
                        prevColumn = orderedColumns.getOrNull(index - 1),
                        nextColumn = orderedColumns.getOrNull(index + 1),
                        onSelectCard = onSelectCard,
                        onMoveCard = onMoveCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 560.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                        RoundedCornerShape(18.dp),
                    )
                    .horizontalScroll(rememberScrollState())
                    .padding(spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                orderedColumns.forEachIndexed { index, column ->
                    KanbanColumn(
                        column = column,
                        cards = board.cardsForColumn(column),
                        tickets = tickets,
                        tasks = tasks,
                        categories = categories,
                        selectedCard = selectedCard,
                        prevColumn = orderedColumns.getOrNull(index - 1),
                        nextColumn = orderedColumns.getOrNull(index + 1),
                        onSelectCard = onSelectCard,
                        onMoveCard = onMoveCard,
                        modifier = Modifier
                            .width(292.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardFocusPanel(
    board: Board,
    boardAccent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val summary = buildBoardSummary(listOf(board), board)
    val completion by animateFloatAsState(
        targetValue = summary.completionRate,
        animationSpec = tween(500),
    )

    Surface(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
        ) {
            val stacked = maxWidth < 680.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    BoardFocusCopy(board = board, boardAccent = boardAccent)
                    BoardFocusSignals(summary = summary, completion = completion, boardAccent = boardAccent)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoardFocusCopy(
                        board = board,
                        boardAccent = boardAccent,
                        modifier = Modifier.weight(1f),
                    )
                    BoardFocusSignals(
                        summary = summary,
                        completion = completion,
                        boardAccent = boardAccent,
                        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardFocusCopy(
    board: Board,
    boardAccent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        CommandLabel(text = "LINEA DE TRABAJO", color = boardAccent)
        Text(
            text = board.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = board.description.ifBlank { "Sin descripcion de tablero" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BoardFocusSignals(
    summary: BoardSummary,
    completion: Float,
    boardAccent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ProgressRail(
            progress = completion,
            accentColor = boardAccent,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            StatusPill(
                text = "${summary.totalCards} activas",
                containerColor = boardAccent.copy(alpha = 0.14f),
                contentColor = boardAccent,
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                text = "${summary.holdCards} espera",
                containerColor = SupportDeskThemeTokens.semanticColors.warningContainer,
                contentColor = SupportDeskThemeTokens.semanticColors.warning,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    column: BoardColumn,
    cards: List<BoardCard>,
    tickets: List<Ticket>,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    selectedCard: BoardCard?,
    prevColumn: BoardColumn?,
    nextColumn: BoardColumn?,
    onSelectCard: (String) -> Unit,
    onMoveCard: (BoardCard, BoardColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val columnColor = column.colorHex.parseHexColor()

    Surface(
        modifier = modifier
            .shadow(SupportDeskThemeTokens.elevations.subtle, RoundedCornerShape(16.dp))
            .border(1.dp, columnColor.copy(alpha = 0.32f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ColumnHeader(
                column = column,
                cardCount = cards.size,
                columnColor = columnColor,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(columnColor.copy(alpha = 0.035f)),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                contentPadding = PaddingValues(spacing.sm),
            ) {
                if (cards.isEmpty()) {
                    item {
                        EmptyColumnState(columnColor = columnColor)
                    }
                }
                items(cards, key = { it.id }) { card ->
                    val ticket = tickets.find { it.id == card.ticketId }
                    val task = tasks.find { it.id == card.taskId }
                    val category = task?.let { currentTask ->
                        categories.find { it.id == currentTask.categoryId }
                    }
                    KanbanCardItem(
                        card = card,
                        ticket = ticket,
                        task = task,
                        category = category,
                        columnColor = columnColor,
                        isSelected = selectedCard?.id == card.id,
                        prevColumn = prevColumn,
                        nextColumn = nextColumn,
                        onClick = { onSelectCard(card.id) },
                        onMoveLeft = { prevColumn?.let { onMoveCard(card, it) } },
                        onMoveRight = { nextColumn?.let { onMoveCard(card, it) } },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(250),
                            fadeOutSpec = tween(250),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(
    column: BoardColumn,
    cardCount: Int,
    columnColor: Color,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        columnColor.copy(alpha = 0.95f),
                        columnColor.copy(alpha = 0.70f),
                    ),
                ),
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = column.status.lowercase().replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CountPill(
                text = cardCount.toString(),
                containerColor = Color.White.copy(alpha = 0.20f),
                contentColor = Color.White,
            )
        }
    }
}

@Composable
private fun KanbanCardItem(
    card: BoardCard,
    ticket: Ticket?,
    task: WorkTask?,
    category: TaskCategory?,
    columnColor: Color,
    isSelected: Boolean,
    prevColumn: BoardColumn?,
    nextColumn: BoardColumn?,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val isTicket = ticket != null
    val accentColor = when {
        isTicket -> semantic.warning
        task != null -> category?.colorHex?.parseHexColor() ?: semantic.success
        else -> columnColor
    }
    val cardBackground by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        animationSpec = tween(220),
    )
    val elevation: Dp by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = cardBackground,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.35f)),
                        ),
                    ),
            )

            Column(
                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                when {
                    ticket != null -> TicketCardContent(ticket = ticket, isSelected = isSelected)
                    task != null -> TaskCardContent(task = task, category = category, accentColor = accentColor, isSelected = isSelected)
                    else -> MissingCardContent(card = card)
                }

                if (isSelected && (prevColumn != null || nextColumn != null)) {
                    MoveActionsRow(
                        prevColumn = prevColumn,
                        nextColumn = nextColumn,
                        onMoveLeft = onMoveLeft,
                        onMoveRight = onMoveRight,
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCardContent(
    ticket: Ticket,
    isSelected: Boolean,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${ticket.ticketNumber.ifBlank { ticket.id.take(6) }}",
                style = MaterialTheme.typography.labelMedium,
                color = semantic.warning,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            TicketPriorityBadge(priority = ticket.priority)
        }
        Text(
            text = ticket.subject,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (isSelected) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallDot(color = semantic.warning)
            Text(
                text = ticket.requester.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ticket.updatedAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isSelected && ticket.description.isNotBlank()) {
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TaskCardContent(
    task: WorkTask,
    category: TaskCategory?,
    accentColor: Color,
    isSelected: Boolean,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            SmallDot(color = accentColor)
            Text(
                text = category?.name ?: "Tarea",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (task.completed) {
                CountPill(
                    text = "Listo",
                    containerColor = semantic.successContainer,
                    contentColor = semantic.success,
                )
            }
        }
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (isSelected) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            task.dueDate?.takeIf { it.isNotBlank() }?.let { dueDate ->
                MetaChip(text = dueDate, color = MaterialTheme.colorScheme.secondary)
            }
            if (task.loggedMinutes > 0) {
                MetaChip(
                    text = formatSupportDeskDuration(task.loggedMinutes),
                    color = accentColor,
                )
            }
        }
        if (isSelected && task.description.isNotBlank()) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MissingCardContent(card: BoardCard) {
    Text(
        text = "Tarjeta ${card.id.take(8)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MoveActionsRow(
    prevColumn: BoardColumn?,
    nextColumn: BoardColumn?,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        if (prevColumn != null) {
            MoveButton(
                label = "< ${prevColumn.name}",
                onClick = onMoveLeft,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (nextColumn != null) {
            MoveButton(
                label = "${nextColumn.name} >",
                onClick = onMoveRight,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyBoardsState(
    loading: Boolean,
    message: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        SectionCard(
            title = if (loading) "Cargando tableros" else "Sin tableros",
            subtitle = message?.takeIf { it.isNotBlank() }
                ?: "Los tableros apareceran cuando existan clientes, tickets o tareas enlazadas.",
            neonAccentColor = accentColor,
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            ProgressRail(
                progress = if (loading) 0.42f else 0f,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectBoardEmptyState(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        SectionCard(
            title = "Selecciona un tablero",
            subtitle = "El panel cargara las columnas y tarjetas reales del cliente elegido.",
            neonAccentColor = accentColor,
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            ProgressRail(
                progress = 0f,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyColumnState(columnColor: Color) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .border(1.dp, columnColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            CommandLabel(text = "SIN TARJETAS", color = columnColor)
            Text(
                text = "Cuando una tarjeta entre en esta fase aparecera aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressRail(
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.96f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.78f)),
                    ),
                ),
        )
    }
}

@Composable
private fun CommandLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniSignal(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SmallDot(color = color)
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SmallDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun CountPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 24.dp)
            .background(containerColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 28.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(100.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetaChip(
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class BoardSummary(
    val boardCount: Int,
    val totalCards: Int,
    val ticketCards: Int,
    val taskCards: Int,
    val doneCards: Int,
    val holdCards: Int,
) {
    val completionRate: Float
        get() = if (totalCards == 0) 0f else doneCards.toFloat() / totalCards.toFloat()
}

private fun buildBoardSummary(boards: List<Board>, selectedBoard: Board?): BoardSummary {
    val scopedBoards = selectedBoard?.let { listOf(it) } ?: boards
    val visibleCards = scopedBoards.flatMap { it.visibleCards() }
    val doneCards = scopedBoards.sumOf { board ->
        board.visibleCards().count { card ->
            board.columnStatusFor(card) in DoneColumnStatuses
        }
    }
    val holdCards = scopedBoards.sumOf { board ->
        board.visibleCards().count { card ->
            board.columnStatusFor(card) in HoldColumnStatuses
        }
    }
    return BoardSummary(
        boardCount = boards.size,
        totalCards = visibleCards.size,
        ticketCards = visibleCards.count { it.ticketId != null },
        taskCards = visibleCards.count { it.taskId != null },
        doneCards = doneCards,
        holdCards = holdCards,
    )
}

private fun Board.visibleCards(): List<BoardCard> = cards.filter { !it.isHidden }

private fun Board.cardsForColumn(column: BoardColumn): List<BoardCard> =
    visibleCards()
        .filter { it.columnId == column.id }
        .sortedBy { it.position }

private fun Board.columnStatusFor(card: BoardCard): String? =
    columns.firstOrNull { it.id == card.columnId }?.status
