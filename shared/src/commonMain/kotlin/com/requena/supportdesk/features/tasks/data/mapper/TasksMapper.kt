package com.requena.supportdesk.features.tasks.data.mapper

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.features.tasks.data.dto.TaskDto
import com.requena.supportdesk.features.tasks.data.dto.TaskLabelDto
import com.requena.supportdesk.features.tasks.data.dto.TaskLogDto

object TasksMapper {
    fun fromLabelDto(dto: TaskLabelDto): TaskCategory = TaskCategory(
        id = dto.id,
        name = dto.name,
        colorHex = dto.colorHex,
        tasksCount = dto.tasksCount,
    )

    fun fromTaskDto(dto: TaskDto): WorkTask = WorkTask(
        id = dto.id,
        title = dto.title,
        clientId = dto.clientId,
        categoryId = dto.labelId,
        description = dto.description,
        dueDate = null,
        completed = dto.completed,
        loggedMinutes = dto.loggedMinutes,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
    )

    fun fromTaskLogDto(dto: TaskLogDto): TaskLog = TaskLog(
        id = dto.id,
        taskId = dto.taskId,
        clientId = dto.clientId,
        authorId = dto.authorId,
        authorName = dto.authorName,
        minutes = dto.minutes,
        workDate = dto.workDate,
        note = dto.note,
        billable = dto.billable,
        createdAt = dto.createdAt,
    )
}
