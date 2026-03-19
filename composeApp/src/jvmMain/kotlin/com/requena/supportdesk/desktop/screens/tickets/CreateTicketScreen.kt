package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.layout.FormSection
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.tickets.AttachmentChip
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput

@Composable
fun CreateTicketScreen(
    affectedApp: String,
    onBack: () -> Unit,
    onCreateTicket: (CreateTicketInput) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var appVersion by rememberSaveable { mutableStateOf("") }
    var clientReference by rememberSaveable { mutableStateOf("") }
    var stepsToReproduce by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(TicketCategory.BUG) }
    var selectedPlatform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val attachmentPlaceholders = listOf(
        Attachment(
            id = "local-brief",
            fileName = "brief.pdf",
            contentType = "application/pdf",
            sizeBytes = 48213,
            uploadedBy = "Local draft",
            uploadedAt = "Draft",
        ),
        Attachment(
            id = "local-screenshot",
            fileName = "screenshot.png",
            contentType = "image/png",
            sizeBytes = 128032,
            uploadedBy = "Local draft",
            uploadedAt = "Draft",
        ),
    )

    val categoryOptions = TicketCategory.entries.map { FilterOption(value = it, label = it.displayName()) }
    val platformOptions = SupportPlatform.entries.map { FilterOption(value = it, label = it.displayName()) }
    val isValid = subject.isNotBlank() && description.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Create ticket",
            subtitle = "Capture the issue fast, add the minimum technical context, and let triage happen without extra back-and-forth.",
            eyebrow = "Client workflow",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SecondaryButton(
                        text = "Back",
                        onClick = {
                            if (subject.isBlank() && description.isBlank() && appVersion.isBlank() && clientReference.isBlank() && stepsToReproduce.isBlank()) {
                                onBack()
                            } else {
                                showDiscardDialog = true
                            }
                        },
                    )
                    PrimaryButton(
                        text = "Create ticket",
                        onClick = {
                            showErrors = true
                            if (isValid) {
                                onCreateTicket(
                                    CreateTicketInput(
                                        subject = subject,
                                        description = description,
                                        category = selectedCategory,
                                        affectedApp = affectedApp,
                                        platform = selectedPlatform,
                                        appVersion = appVersion,
                                        stepsToReproduce = stepsToReproduce,
                                        clientReference = clientReference,
                                    ),
                                )
                            }
                        },
                    )
                }
            },
        )

        FormSection(
            title = "Issue overview",
            subtitle = "Keep the first block short. Subject, category and product context should be enough to route the ticket correctly.",
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Subject") },
                placeholder = { Text("Desktop installer fails after the latest update") },
                isError = showErrors && subject.isBlank(),
                singleLine = true,
            )
            OutlinedTextField(
                value = affectedApp,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Affected app") },
                enabled = false,
                supportingText = { Text("Assigned from the client account to keep the form focused.") },
                singleLine = true,
            )
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterBar(
                label = "Category",
                options = categoryOptions,
                selected = selectedCategory,
                onSelected = { selectedCategory = it ?: TicketCategory.BUG },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TicketCategoryBadge(category = selectedCategory)
                SupportPlatformBadge(platform = selectedPlatform)
            }
        }

        FormSection(
            title = "Technical context",
            subtitle = "Only the details that help you debug quickly: platform, version, repro steps and a reference if the client has one.",
        ) {
            Text(
                text = "Platform",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterBar(
                label = "Platform",
                options = platformOptions,
                selected = selectedPlatform,
                onSelected = { selectedPlatform = it ?: SupportPlatform.DESKTOP },
            )
            OutlinedTextField(
                value = appVersion,
                onValueChange = { appVersion = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("App version") },
                placeholder = { Text("1.8.2 (418)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = clientReference,
                onValueChange = { clientReference = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Client reference") },
                placeholder = { Text("Optional build, invoice or sprint reference") },
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                placeholder = { Text("Describe what happened, what changed, and the impact on the app.") },
                isError = showErrors && description.isBlank(),
                minLines = 5,
            )
            OutlinedTextField(
                value = stepsToReproduce,
                onValueChange = { stepsToReproduce = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Steps to reproduce") },
                placeholder = { Text("Open the app, sign in, then the crash appears after pressing Sync.") },
                minLines = 4,
            )
            if (showErrors && !isValid) {
                Text(
                    text = "Subject and description are required before sending the ticket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Priority is assigned during triage to keep the client form short and consistent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FormSection(
            title = "Attachments",
            subtitle = "The upload area is still placeholder-only, but screenshots and logs already have a clear place in the flow.",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                attachmentPlaceholders.forEach { attachment ->
                    AttachmentChip(attachment = attachment)
                }
            }
            Text(
                text = "Final upload wiring will connect this area to the attachment endpoint later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    ConfirmDialog(
        visible = showDiscardDialog,
        title = "Discard this draft?",
        message = "The current subject, description and technical context will be lost.",
        confirmText = "Discard draft",
        onConfirm = {
            showDiscardDialog = false
            onBack()
        },
        onDismiss = { showDiscardDialog = false },
    )
}
