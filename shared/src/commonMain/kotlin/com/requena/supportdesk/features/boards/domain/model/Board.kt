package com.requena.supportdesk.features.boards.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Board(
    val id: String,
    val name: String,
    val description: String = "",
    val type: BoardType = BoardType.GLOBAL,
    val ownerId: String,
    val clientId: String? = null,
    val isArchived: Boolean = false,
    val isPublic: Boolean = false,
    val columns: List<BoardColumn> = emptyList(),
    val cards: List<BoardCard> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
enum class BoardType {
    GLOBAL, PERSONAL, CLIENT
}

@Serializable
data class BoardColumn(
    val id: String,
    val boardId: String,
    val name: String,
    val position: Int,
    val status: String,
    val colorHex: String = "#3498db",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class BoardCard(
    val id: String,
    val boardId: String,
    val ticketId: String,
    val columnId: String,
    val position: Int,
    val isHidden: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val movedAt: String? = null,
)
