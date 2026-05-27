package com.requena.supportdesk.features.tasks.data.mapper

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.model.WorkTaskStatus
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
        dueDate = dto.dueDate,
        completed = dto.completed,
        status = runCatching { WorkTaskStatus.valueOf(dto.status) }.getOrElse {
            if (dto.completed) WorkTaskStatus.DONE else WorkTaskStatus.TODO
        },
        loggedMinutes = dto.loggedMinutes,
        loggedSeconds = dto.loggedSeconds,
        pinnedAt = dto.pinnedAt,
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
        seconds = dto.seconds,
        workDate = dto.workDate,
        note = dto.note,
        billable = dto.billable,
        createdAt = dto.createdAt,
    )
}
