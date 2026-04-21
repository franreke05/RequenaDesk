# Compose UI Creation Flow: Labels, Clients, and Tasks

This document details the exact flow of how the admin UI in `composeApp/src` handles creating labels, clients, and tasks, including form handling, data submission, and error handling.

---

## 1. CREATING LABELS

### 1.1 UI Component: LabelCreateCard
**File:** [composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminNotificationsScreen.kt](composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminNotificationsScreen.kt#L119)

```kotlin
@Composable
private fun LabelCreateCard(
    onCreate: (String, String) -> Unit,  // Callback with (name, colorHex)
    modifier: Modifier = Modifier,
) {
    var categoryName by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf("#6B7A5B") }
    val presetColors = remember { listOf("#6B7A5B", "#A67C52", "#7D4E57", "#355C5B", "#8C6A43") }

    SectionCard(
        modifier = modifier,
        title = "Nueva etiqueta",
        subtitle = "Cada etiqueta crea una agrupacion visual clara para el trabajo diario.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Form Field 1: Label name
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de la etiqueta") },
                singleLine = true,
            )
            
            // Form Field 2: Color HEX code (with normalization)
            OutlinedTextField(
                value = selectedColor,
                onValueChange = { selectedColor = normalizeHex(it) },  // Validation: normalizes hex
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Color HEX") },
                singleLine = true,
            )
            
            // Color palette selector (visual aid)
            ColorPaletteRow(
                presetColors = presetColors,
                selectedColor = selectedColor,
                onSelect = { selectedColor = it },
            )
            
            // Submit button with validation
            PrimaryButton(
                text = "Crear etiqueta",
                onClick = {
                    // Data is collected and submitted here
                    onCreate(categoryName.trim(), normalizeHex(selectedColor))
                    // Reset form after submission
                    categoryName = ""
                },
                enabled = categoryName.isNotBlank(),  // Validation: name required
            )
        }
    }
}
```

### 1.2 Form Data Collection Flow
1. **User Input**: Collected into mutable state variables
   - `categoryName`: String (label name)
   - `selectedColor`: String (HEX color code like "#6B7A5B")

2. **Validation Before Submission**:
   - `categoryName.isNotBlank()` - Button only enabled when name is not empty
   - `normalizeHex(colorHex)` - Normalizes hex format (adds # if missing)

3. **Data Submission**: Calls `onCreate(categoryName.trim(), normalizeHex(selectedColor))`

---

### 1.3 ViewModel: Event Processing
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/viewmodel/TasksViewModel.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/viewmodel/TasksViewModel.kt#L130)

```kotlin
// The onClick from LabelCreateCard calls this event handler
onTasksEvent(TasksUiEvent.CreateCategory(name, colorHex))

// In TasksViewModel.onEvent():
is TasksUiEvent.CreateCategory -> createLabel(event.name, event.colorHex)

private fun createLabel(name: String, colorHex: String) {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return  // Validation: skip if name is empty
    
    launch {
        // Call use case which calls repository
        when (val result = createTaskLabelUseCase(TaskLabelDraft(cleanName, normalizeHex(colorHex)))) {
            is AppResult.Error -> {
                // Error handling: set error message in state
                handleWorkspaceError(result.message)
            }
            is AppResult.Success -> {
                // Success: reload workspace with newly created label selected
                loadWorkspace(
                    selectedCategoryId = result.data.id,
                    statusMessage = "Etiqueta creada",  // Show success message to user
                )
            }
        }
    }
}
```

### 1.4 Repository Layer: API Call
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/repository/TasksRepositoryImpl.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/repository/TasksRepositoryImpl.kt#L81)

```kotlin
override suspend fun createLabel(input: TaskLabelDraft): AppResult<TaskCategory> = runCatching {
    // Call datasource to make HTTP POST request
    dataSource.createLabel(
        CreateTaskLabelRequestDto(
            name = input.name,
            colorHex = input.colorHex,
        ),
    ).let(TasksMapper::fromLabelDto)
}.fold(
    onSuccess = { AppResult.Success(it) },
    // Error handling: wrap exception in AppResult with message
    onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear la etiqueta.", cause = it) },
)
```

### 1.5 DataSource: HTTP Request
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/datasource/TasksDataSource.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/datasource/TasksDataSource.kt#L65)

```kotlin
override suspend fun createLabel(request: CreateTaskLabelRequestDto): TaskLabelDto =
    httpClient.post("${supportDeskBaseUrl()}/admin/labels") {
        setBody(jsonRequestBody(request))  // Serialize to JSON
    }.requireApiData()  // Extract data if successful, throw if error
```

### 1.6 Request/Response DTOs

**CreateTaskLabelRequestDto** (sent to API):
```kotlin
@Serializable
data class CreateTaskLabelRequestDto(
    val name: String,      // e.g., "Hoy"
    val colorHex: String,  // e.g., "#6B7A5B"
)
```

**TaskLabelDto** (received from API):
```kotlin
@Serializable
data class TaskLabelDto(
    val id: String,           // Generated by server
    val name: String,
    val colorHex: String,
    val tasksCount: Int = 0,
)
```

---

## 2. CREATING CLIENTS

### 2.1 UI Component: CompactCreateClientCard
**File:** [composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminClientsScreen.kt](composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminClientsScreen.kt#L137)

```kotlin
@Composable
private fun CompactCreateClientCard(
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var companyName by rememberSaveable { mutableStateOf("") }
    var productName by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    SectionCard(
        modifier = modifier,
        title = "Nuevo cliente",
        subtitle = "Alta minima para dejarlo disponible y asociarlo a tareas.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Form Field 1: Company name
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Empresa") },
                    singleLine = true,
                )
                // Form Field 2: Product name
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Producto") },
                    singleLine = true,
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Form Field 3: Contact name
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Contacto") },
                    singleLine = true,
                )
                // Form Field 4: Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Email") },
                    singleLine = true,
                )
            }
            
            // Submit button with all-fields-required validation
            PrimaryButton(
                text = "Crear cliente",
                onClick = {
                    // Data submission
                    onEvent(
                        ClientsUiEvent.CreateClient(
                            companyName = companyName,
                            productName = productName,
                            contactName = contactName,
                            email = email,
                        ),
                    )
                    // Reset form after submission
                    companyName = ""
                    productName = ""
                    contactName = ""
                    email = ""
                },
                // Validation: Button only enabled when ALL fields are filled
                enabled = companyName.isNotBlank() && 
                         productName.isNotBlank() && 
                         contactName.isNotBlank() && 
                         email.isNotBlank(),
            )
        }
    }
}
```

### 2.2 Form Data Collection Flow
1. **User Input**: Four form fields
   - `companyName`: Company name
   - `productName`: Product name
   - `contactName`: Contact person name
   - `email`: Email address

2. **Validation Before Submission**:
   - All four fields must be non-empty (`.isNotBlank()`)
   - Trimming applied at submission level

3. **Data Submission**: Emits `ClientsUiEvent.CreateClient` with all four fields

---

### 2.3 ViewModel: Event Processing
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/presentation/viewmodel/ClientsViewModel.kt](shared/src/commonMain/kotlin/com/requena\supportdesk\features\clients\presentation\viewmodel\ClientsViewModel.kt#L85)

```kotlin
// In ClientsViewModel.onEvent():
is ClientsUiEvent.CreateClient -> createClient(event)

private fun createClient(event: ClientsUiEvent.CreateClient) {
    // Convert event to domain model (draft)
    val draft = event.toDraft()
    
    // Validation: Check draft validity
    if (!draft.isValid()) return
    
    launch {
        // Call use case which calls repository
        when (val result = createClientUseCase(draft)) {
            is AppResult.Error -> {
                // Error handling: Set error state AND emit effect to show message
                _state.update { it.copy(errorMessage = result.message) }
                _effects.emit(ClientsUiEffect.ShowMessage(result.message))
            }
            is AppResult.Success -> {
                // Success: Reload clients list with newly created client selected
                loadClients(
                    preferredSelectedId = result.data.id,
                    successMessage = "Cliente creado",  // Show success message
                )
            }
        }
    }
}

// Conversion from event to domain model
private fun ClientsUiEvent.CreateClient.toDraft(): ClientDraft = ClientDraft(
    companyName = companyName.trim(),
    productName = productName.trim(),
    contactName = contactName.trim(),
    email = email.trim(),
    accountStatus = ClientAccountStatus.ACTIVE,  // Default
    serviceTier = ClientServiceTier.STANDARD,    // Default
    preferredContactChannel = PreferredContactChannel.TICKET,  // Default
)

// Validation function
private fun ClientDraft.isValid(): Boolean =
    companyName.isNotBlank() &&
    productName.isNotBlank() &&
    contactName.isNotBlank() &&
    email.isNotBlank()
```

### 2.4 Repository Layer: API Call
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/data/repository/ClientsRepositoryImpl.kt](shared/src/commonMain/kotlin/com/requena/supportdesk\features\clients\data\repository\ClientsRepositoryImpl.kt#L20)

```kotlin
override suspend fun createClient(input: ClientDraft): AppResult<Client> = runCatching {
    // Call datasource to make HTTP POST request
    dataSource.createClient(
        CreateClientRequestDto(
            companyName = input.companyName,
            productName = input.productName,
            contactName = input.contactName,
            email = input.email,
            accountStatus = input.accountStatus.name,  // Convert enum to string
            serviceTier = input.serviceTier.name,
            preferredContactChannel = input.preferredContactChannel.name,
        ),
    ).let(ClientsMapper::fromDto)  // Map DTO to domain model
}.fold(
    onSuccess = { AppResult.Success(it) },
    // Error handling: wrap exception with user-friendly message
    onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear el cliente.", cause = it) },
)
```

### 2.5 DataSource: HTTP Request
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/data/datasource/ClientsDataSource.kt](shared/src/commonMain/kotlin/com\requena\supportdesk\features\clients\data\datasource\ClientsDataSource.kt#L26)

```kotlin
override suspend fun createClient(request: CreateClientRequestDto): ClientDto =
    httpClient.post("${supportDeskBaseUrl()}/admin/clients") {
        setBody(jsonRequestBody(request))  // Serialize to JSON
    }.requireApiData()  // Extract data, throw if error
```

### 2.6 Request/Response DTOs

**CreateClientRequestDto** (sent to API):
```kotlin
@Serializable
data class CreateClientRequestDto(
    val companyName: String,              // e.g., "Pixel Forge"
    val productName: String,              // e.g., "Design System"
    val contactName: String,              // e.g., "Juan García"
    val email: String,                    // e.g., "juan@pixelforge.dev"
    val accountStatus: String,            // e.g., "ACTIVE"
    val serviceTier: String,              // e.g., "STANDARD"
    val preferredContactChannel: String,  // e.g., "TICKET"
)
```

**ClientDto** (received from API):
```kotlin
@Serializable
data class ClientDto(
    val id: String,                       // Generated by server
    val companyName: String,
    val contactName: String,
    val email: String,
    val productName: String,
    val accountStatus: String,            // Default "ACTIVE"
    val serviceTier: String,              // Default "STANDARD"
    val preferredContactChannel: String,  // Default "TICKET"
    val activeTicketCount: Int,
    val openTasksCount: Int,
    val monthlyLoggedMinutes: Int,
)
```

---

## 3. CREATING TASKS

### 3.1 UI Component: TaskCreateDialog
**File:** [composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminTasksScreen.kt](composeApp/src/commonMain/kotlin/com\requena\supportdesk\app\admin\screens\AdminTasksScreen.kt#L201)

```kotlin
@Composable
private fun TaskCreateDialog(
    visible: Boolean,
    clients: List<Client>,              // List of available clients
    categories: List<TaskCategory>,     // List of labels/categories
    onDismiss: () -> Unit,
    onCreate: (String, String, String?, String) -> Unit,  // (title, desc, clientId?, categoryId)
) {
    if (!visible) return

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf("none") }
    var selectedCategoryId by remember(categories) { 
        mutableStateOf(categories.firstOrNull()?.id.orEmpty()) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva tarea",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Form Field 1: Task title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la tarea") },
                    singleLine = true,
                )
                
                // Form Field 2: Task description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripcion") },
                    minLines = 3,
                )
                
                // Form Field 3: Label/Category (FilterBar = dropdown)
                FilterBar(
                    label = "Etiqueta",
                    options = categories.map { FilterOption(it.id, it.name) },
                    selected = selectedCategoryId.takeIf { it.isNotBlank() },
                    onSelected = { selectedCategoryId = it.orEmpty() },
                )
                
                // Form Field 4: Client (optional, FilterBar = dropdown)
                FilterBar(
                    label = "Cliente",
                    options = listOf(FilterOption("none", "Sin cliente")) + 
                             clients.map { FilterOption(it.id, it.companyName) },
                    selected = selectedClientId,
                    onSelected = { selectedClientId = it ?: "none" },
                )
            }
        },
        confirmButton = {
            // Submit button with validation: title and label required
            PrimaryButton(
                text = "Crear",
                enabled = title.isNotBlank() && selectedCategoryId.isNotBlank(),
                onClick = {
                    onCreate(
                        title.trim(),
                        description.trim(),
                        selectedClientId.takeUnless { it == "none" },  // null if "none"
                        selectedCategoryId,
                    )
                    title = ""
                    description = ""
                    selectedClientId = "none"
                },
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancelar",
                onClick = onDismiss,
            )
        },
    )
}
```

### 3.2 Form Data Collection Flow
1. **User Input**: Collected into mutable state
   - `title`: Task title (required)
   - `description`: Task description (optional but form includes it)
   - `selectedCategoryId`: Label/category ID (required)
   - `selectedClientId`: Client ID or "none" (optional)

2. **Validation Before Submission**:
   - `title.isNotBlank()` - Title required
   - `selectedCategoryId.isNotBlank()` - Label required
   - Client is optional (filtered with `.takeUnless { it == "none" }`)
   - Both title and description trimmed

3. **Data Submission**: Calls `onCreate(title, description, clientId?, categoryId)`

---

### 3.3 ViewModel: Event Processing
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/viewmodel/TasksViewModel.kt](shared/src/commonMain/kotlin/com\requena\supportdesk\features\tasks\presentation\viewmodel\TasksViewModel.kt#L169)

```kotlin
// The onCreate from TaskCreateDialog calls this event handler
onTasksEvent(TasksUiEvent.CreateTask(title, description, clientId, categoryId))

// In TasksViewModel.onEvent():
is TasksUiEvent.CreateTask -> createTask(event)

private fun createTask(event: TasksUiEvent.CreateTask) {
    val title = event.title.trim()
    
    // Validation: Skip if title or category is empty
    if (title.isBlank() || event.categoryId.isBlank()) return
    
    launch {
        // Call use case which calls repository
        when (
            val result = createTaskUseCase(
                TaskDraft(
                    title = title,
                    description = event.description.trim(),
                    clientId = event.clientId?.takeIf { it.isNotBlank() },  // null if blank
                    categoryId = event.categoryId,
                ),
            )
        ) {
            is AppResult.Error -> {
                // Error handling: log error message
                handleWorkspaceError(result.message)
            }
            is AppResult.Success -> {
                // Success: Update workspace state with newly created task
                loadWorkspace(
                    selectedTaskId = result.data.id,
                    selectedCategoryId = result.data.categoryId,
                    selectedClientFilterId = result.data.clientId,
                    selectedDashboardClientId = result.data.clientId ?: state.value.selectedDashboardClientId,
                    statusMessage = "Tarea creada",  // Show success message
                )
            }
        }
    }
}
```

### 3.4 Repository Layer: API Call
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/repository/TasksRepositoryImpl.kt](shared/src/commonMain/kotlin/com\requena\supportdesk\features\tasks\data\repository\TasksRepositoryImpl.kt#L45)

```kotlin
override suspend fun createTask(input: TaskDraft): AppResult<WorkTask> = runCatching {
    // Call datasource to make HTTP POST request
    dataSource.createTask(
        CreateTaskRequestDto(
            title = input.title,
            description = input.description,
            clientId = input.clientId,
            labelId = input.categoryId,  // Note: renamed to labelId for API
        ),
    ).let(TasksMapper::fromTaskDto)
}.fold(
    onSuccess = { AppResult.Success(it) },
    // Error handling: wrap exception with user-friendly message
    onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear la tarea.", cause = it) },
)
```

### 3.5 DataSource: HTTP Request
**File:** [shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/datasource/TasksDataSource.kt](shared/src/commonMain/kotlin/com\requena\supportdesk\features\tasks\data\datasource\TasksDataSource.kt#L47)

```kotlin
override suspend fun createTask(request: CreateTaskRequestDto): TaskDto =
    httpClient.post("${supportDeskBaseUrl()}/admin/tasks") {
        setBody(jsonRequestBody(request))  // Serialize to JSON
    }.requireApiData()  // Extract data, throw if error
```

### 3.6 Request/Response DTOs

**CreateTaskRequestDto** (sent to API):
```kotlin
@Serializable
data class CreateTaskRequestDto(
    val title: String,              // e.g., "Fix login flow"
    val description: String,        // e.g., "Users cannot login with SSO"
    val clientId: String? = null,   // e.g., "client-1" or null
    val labelId: String,            // e.g., "55555555-5555-5555-5555-555555555555" (required)
)
```

**TaskDto** (received from API):
```kotlin
@Serializable
data class TaskDto(
    val id: String,              // Generated by server
    val title: String,
    val description: String,
    val clientId: String? = null,
    val clientName: String? = null,
    val labelId: String,
    val labelName: String,
    val labelColorHex: String,
    val completed: Boolean,
    val loggedMinutes: Int,
    val createdAt: String,
    val updatedAt: String,
)
```

---

## 4. ERROR HANDLING & LOGGING SUMMARY

### 4.1 Error Handling Patterns

**In ViewModel Layer** (where repositories are called):
```kotlin
when (val result = useCase(...)) {
    is AppResult.Error -> {
        // For clients:
        _state.update { it.copy(errorMessage = result.message) }
        _effects.emit(ClientsUiEffect.ShowMessage(result.message))
        
        // For tasks:
        handleWorkspaceError(result.message)  // Sets errorMessage in state
    }
    is AppResult.Success -> {
        // Update UI state with success and show status message
        _effects.emit(ClientsUiEffect.ShowMessage(successMessage))
    }
}
```

**In Repository Layer**:
```kotlin
.fold(
    onSuccess = { AppResult.Success(it) },
    onFailure = { AppResult.Error(
        message = it.message ?: "No se pudo crear la [recurso].", 
        cause = it
    )},
)
```

### 4.2 User-Friendly Error Messages

The system provides default error messages in Spanish:
- **Labels**: "No se pudo crear la etiqueta."
- **Clients**: "No se pudo crear el cliente."
- **Tasks**: "No se pudo crear la tarea."

If the server provides a specific error message, that takes precedence.

### 4.3 Validation Logging

The system validates data BEFORE calling the repository:

**UI Level**:
- Buttons disabled when required fields are empty
- Form fields trimmed before submission

**ViewModel Level**:
- Additional validation checks before calling use cases
- Returns early if validation fails (`if (titleIsBlank || categoryIdIsBlank) return`)

**Repository Level**:
- Client/server validation via `AppResult.Error` wrapping
- Exceptions caught and wrapped with contextual messages

### 4.4 Success Feedback

**Status Messages** shown to user:
- "Etiqueta creada" - Label created successfully
- "Cliente creado" - Client created successfully
- "Tarea creada" - Task created successfully

These messages are sent via `_effects.emit()` or stored in `statusMessage` state, then displayed in the UI.

---

## 5. DATA FLOW DIAGRAM

```
┌─────────────────────┐
│   UI Component      │
│ (LabelCreateCard    │
│  CompactCreateCard  │
│  TaskCreateDialog)  │
└──────────┬──────────┘
           │ Form fields collected
           │ Validation at button level
           ├─ categoryName.isNotBlank()
           ├─ companyName.isNotBlank()
           ├─ title.isNotBlank()
           ▼
┌──────────────────────────┐
│   ViewModel              │
│  (TasksViewModel         │
│   ClientsViewModel)      │
│                          │
│ createLabel()            │
│ createClient()           │
│ createTask()             │
└──────────┬───────────────┘
           │ Validation check
           │ if (!draft.isValid()) return
           │
           ├─ Emit useCase(draft)
           │
           ▼
    ┌──────────────┐
    │ Use Cases    │
    │              │
    │ CreateLabel  │
    │ CreateClient │
    │ CreateTask   │
    └──────┬───────┘
           │
           ▼
┌────────────────────────┐
│ Repository Layer       │
│   (Impl)               │
│                        │
│ createLabel()          │
│ createClient()         │
│ createTask()           │
│                        │
│ runCatching { }        │
│ .fold(                 │
│  on Success,           │
│  onFailure)            │
└──────────┬─────────────┘
           │
           ▼
┌────────────────────────┐
│ DataSource Layer       │
│ (RemoteDataSource)     │
│                        │
│ httpClient.post()      │
│ setBody(jsonRequest)   │
│ .requireApiData()      │
└──────────┬─────────────┘
           │
           ▼
    ┌─────────────┐
    │  HTTP POST  │
    │  /admin/    │
    │  labels     │
    │  clients    │
    │  tasks      │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │   Server    │
    │   (Ktor)    │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │    JSON     │
    │  Response   │
    │  (with id)  │
    └──────┬──────┘
           │
           ▼
┌──────────────────────────┐
│ DataSource Response      │
│ .requireApiData()        │
│ extracts JSON            │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Repository fold()        │
│                          │
│ onSuccess: AppResult.    │
│   Success + Mapper       │
│                          │
│ onFailure: AppResult.    │
│   Error + Message        │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ ViewModel when branch    │
│                          │
│ Success: loadWorkspace() │
│   Update state           │
│   statusMessage =        │
│   "[Recurso] creado"     │
│                           │
│ Error:                   │
│   handleWorkspaceError() │
│   errorMessage = msg     │
│   (emitted to UI)        │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ UI Updates               │
│ - Form reset             │
│ - List refreshed         │
│ - Success/error message  │
│   displayed              │
└──────────────────────────┘
```

---

## 6. KEY FILES REFERENCE

### Compose UI Components
- [AdminNotificationsScreen.kt](composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminNotificationsScreen.kt) - Label creation UI
- [AdminClientsScreen.kt](composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminClientsScreen.kt) - Client creation UI
- [AdminTasksScreen.kt](composeApp/src/commonMain/kotlin/com/requena/supportdesk/app/admin/screens/AdminTasksScreen.kt) - Task creation UI

### ViewModels
- [TasksViewModel.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/viewmodel/TasksViewModel.kt) - Label & task logic
- [ClientsViewModel.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/presentation/viewmodel/ClientsViewModel.kt) - Client logic

### Repository Layer
- [TasksRepositoryImpl.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/repository/TasksRepositoryImpl.kt)
- [ClientsRepositoryImpl.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/data/repository/ClientsRepositoryImpl.kt)

### DataSource/HTTP
- [TasksDataSource.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/datasource/TasksDataSource.kt)
- [ClientsDataSource.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/data/datasource/ClientsDataSource.kt)

### DTOs
- [TaskDtos.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/data/dto/TaskDtos.kt)
- [ClientsDtos.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/data/dto/ClientsDtos.kt)

### Events
- [TasksUiEvent.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/tasks/presentation/event/TasksUiEvent.kt)
- [ClientsUiEvent.kt](shared/src/commonMain/kotlin/com/requena/supportdesk/features/clients/presentation/event/ClientsUiEvent.kt)
