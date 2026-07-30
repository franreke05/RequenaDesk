package com.requena.supportdesk.features.tasks.domain.model

data class TaskDraft(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
    val dueDate: String? = null,
)

const val MAX_DERIVED_TASK_STEPS = 20

fun parseDerivedTaskSteps(raw: String): List<String> =
    raw.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
        .take(MAX_DERIVED_TASK_STEPS)
        .toList()

fun derivedTaskStepTitle(parentTitle: String, stepTitle: String): String =
    "[${parentTitle.trim()}] ${stepTitle.trim()}"

fun derivedTaskStepDescription(parentTitle: String): String =
    "Paso derivado de la tarea «${parentTitle.trim()}»."

data class TaskUpdateInput(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
    val dueDate: String? = null,
    val completed: Boolean,
)

data class TaskLabelDraft(
    val name: String,
    val colorHex: String,
    val ownerAdminId: String,
)
