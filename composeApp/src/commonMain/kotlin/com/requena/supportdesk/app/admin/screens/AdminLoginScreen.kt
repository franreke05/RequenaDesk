package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import orykaisoftware.composeapp.generated.resources.Res
import orykaisoftware.composeapp.generated.resources.login_dragon_bg
import orykaisoftware.composeapp.generated.resources.login_dragon_bg_claro
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.LocalSupportDeskThemeController
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState

private val Gold        = Color(0xFFD4A74A)
private val GoldDim     = Color(0xFFD4A74A).copy(alpha = 0.55f)
private val LoginBgDark  = Color(0xFF090C15)   // fondo dark mode
private val LoginBgLight = Color(0xFFF5EFE0)   // crema cálido light mode
private val CardBgDark   = Color(0xFF10141F)   // tarjeta glass dark
private val CardBgLight  = Color(0xFFFDF9F2)   // tarjeta glass light

private enum class LoginTab { ADMIN, CLIENT }

@Composable
fun AdminLoginScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(LoginTab.ADMIN) }
    val isDark   = LocalSupportDeskThemeController.current.isDarkMode
    val loginBg  = if (isDark) LoginBgDark else LoginBgLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(loginBg),
    ) {
        // Imagen del dragón — oscuro en dark mode, crema en light mode
        Image(
            painter = painterResource(
                if (isDark) Res.drawable.login_dragon_bg else Res.drawable.login_dragon_bg_claro,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (isDark) 0.72f else 0.82f),
        )

        // Vignette — adapta colores al modo activo
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isDark) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, LoginBgDark.copy(alpha = 0.60f)),
                        center = center,
                        radius = size.maxDimension * 0.62f,
                    ),
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(LoginBgDark.copy(alpha = 0.28f), LoginBgDark.copy(alpha = 0.80f)),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
            } else {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, LoginBgLight.copy(alpha = 0.52f)),
                        center = center,
                        radius = size.maxDimension * 0.65f,
                    ),
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(LoginBgLight.copy(alpha = 0.20f), LoginBgLight.copy(alpha = 0.76f)),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
            }
        }

        // Theme toggle — top right
        SupportDeskEntrance(
            index = 0,
            horizontal = true,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
        ) {
            ThemeModeButton()
        }

        // Login form column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // OryKai branding
            SupportDeskEntrance(index = 1) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "OryKai",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                    Text(
                        text = "Plataforma de gestion y soporte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldDim,
                    )
                }
            }

            // Tab selector
            SupportDeskEntrance(index = 2) {
                GoldTabSelector(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it },
                )
            }

            // Form panel
            SupportDeskEntrance(index = 3) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        val forward = targetState == LoginTab.CLIENT
                        (slideInHorizontally(tween(280)) { if (forward) it else -it } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { if (forward) -it else it } + fadeOut(tween(160)))
                    },
                    label = "login_tab",
                ) { tab ->
                    when (tab) {
                        LoginTab.ADMIN -> AdminLoginForm(state = state, onEvent = onEvent)
                        LoginTab.CLIENT -> ClientLoginForm(state = state, onEvent = onEvent)
                    }
                }
            }

            // Error message
            state.errorMessage?.let { msg ->
                SupportDeskEntrance(index = 4) {
                    Text(
                        text = msg,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE57373),
                    )
                }
            }
        }
    }
}

// ── Tab Selector ─────────────────────────────────────────────────────────────

@Composable
private fun GoldTabSelector(
    activeTab: LoginTab,
    onTabSelected: (LoginTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalSupportDeskThemeController.current.isDarkMode
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF151820) else Color(0xFFF0E6CC),
        border = BorderStroke(1.dp, Gold.copy(alpha = if (isDark) 0.22f else 0.42f)),
    ) {
        Row(modifier = Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LoginTab.entries.forEach { tab ->
                val selected = tab == activeTab
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Gold.copy(alpha = if (isDark) 0.14f else 0.28f) else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, Gold.copy(alpha = if (isDark) 0.38f else 0.60f)) else null,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (tab == LoginTab.ADMIN) "Acceso Admin" else "Acceso Cliente",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Gold else Gold.copy(alpha = if (isDark) 0.45f else 0.55f),
                        )
                    }
                }
            }
        }
    }
}

// ── Forms ─────────────────────────────────────────────────────────────────────

@Composable
private fun AdminLoginForm(state: AuthUiState, onEvent: (AuthUiEvent) -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }
    GlassCard(title = "Admin", subtitle = "Accede con tu cuenta interna para gestionar clientes, tareas y tickets.") {
        GoldTextField(
            value = state.email,
            onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
            label = "Correo",
            leadingGlyph = { MailGlyph() },
            keyboardType = KeyboardType.Email,
        )
        GoldTextField(
            value = state.password,
            onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
            label = "Contrasena",
            leadingGlyph = { LockGlyph() },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Ver",
                        color = GoldDim,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
        )
        GoldButton(
            text = if (state.isLoading) "Entrando..." else "Entrar",
            onClick = { onEvent(AuthUiEvent.Submit) },
            enabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.isLoading,
        )
    }
}

@Composable
private fun ClientLoginForm(state: AuthUiState, onEvent: (AuthUiEvent) -> Unit) {
    GlassCard(title = "Portal Cliente", subtitle = "Entra con el correo y el codigo de acceso que te dio tu admin.") {
        GoldTextField(
            value = state.clientEmail,
            onValueChange = { onEvent(AuthUiEvent.ClientEmailChanged(it)) },
            label = "Tu correo",
            leadingGlyph = { MailGlyph() },
            keyboardType = KeyboardType.Email,
        )
        GoldTextField(
            value = state.clientAccessCode,
            onValueChange = { onEvent(AuthUiEvent.ClientAccessCodeChanged(it)) },
            label = "Codigo de acceso",
            leadingGlyph = { KeyGlyph() },
        )
        GoldButton(
            text = if (state.isLoading) "Entrando..." else "Entrar al portal",
            onClick = { onEvent(AuthUiEvent.ClaimClientAccess) },
            enabled = state.clientEmail.isNotBlank() && state.clientAccessCode.isNotBlank() && !state.isLoading,
        )
    }
}

// ── Shared UI Atoms ───────────────────────────────────────────────────────────

@Composable
private fun GlassCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val isDark = LocalSupportDeskThemeController.current.isDarkMode
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDark) CardBgDark else CardBgLight,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = if (isDark) 0.25f else 0.45f)),
        shadowElevation = if (isDark) 12.dp else 8.dp,
        tonalElevation = if (isDark) 0.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldDim,
                )
            }
            content()
        }
    }
}

@Composable
private fun GoldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingGlyph: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val isDark = LocalSupportDeskThemeController.current.isDarkMode
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = GoldDim) },
        leadingIcon = leadingGlyph,
        trailingIcon = trailingContent,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Gold.copy(alpha = 0.28f),
            focusedLabelColor = Gold,
            cursorColor = Gold,
            focusedTextColor   = if (isDark) Color(0xFFEEE8D8) else Color(0xFF2A1F0A),
            unfocusedTextColor = if (isDark) Color(0xFFCCC5B0) else Color(0xFF5C4A2A),
            unfocusedContainerColor = if (isDark) Color(0xFF0E1119) else Color(0xFFF5EDE0),
            focusedContainerColor   = if (isDark) Color(0xFF0E1119) else Color(0xFFF5EDE0),
            focusedLeadingIconColor   = Gold,
            unfocusedLeadingIconColor = GoldDim,
        ),
    )
}

@Composable
private fun GoldButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val isDark = LocalSupportDeskThemeController.current.isDarkMode
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = when {
            !enabled -> if (isDark) Color(0xFF111111) else Color(0xFFE0D0A8)
            isDark   -> Color(0xFF1A1508)
            else     -> Gold  // light mode: botón relleno dorado
        },
        border = BorderStroke(1.dp, if (enabled) Gold.copy(alpha = if (isDark) 0.55f else 0.80f) else Gold.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !enabled -> GoldDim
                    isDark   -> Gold
                    else     -> Color(0xFF1A0E00)  // texto oscuro sobre fondo dorado
                },
            )
        }
    }
}

// ── Canvas Glyphs ─────────────────────────────────────────────────────────────

@Composable
private fun MailGlyph() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val l = size.width * 0.08f; val t = size.height * 0.24f
        drawRoundRect(
            color = Gold,
            topLeft = Offset(l, t),
            size = Size(size.width * 0.84f, size.height * 0.52f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = s,
        )
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.28f)
            lineTo(size.width * 0.50f, size.height * 0.58f)
            lineTo(size.width * 0.92f, size.height * 0.28f)
        }
        drawPath(path, color = Gold, style = s)
    }
}

@Composable
private fun LockGlyph() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = Gold,
            topLeft = Offset(size.width * 0.20f, size.height * 0.48f),
            size = Size(size.width * 0.60f, size.height * 0.44f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = s,
        )
        val arc = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.48f)
            cubicTo(
                size.width * 0.30f, size.height * 0.18f,
                size.width * 0.70f, size.height * 0.18f,
                size.width * 0.70f, size.height * 0.48f,
            )
        }
        drawPath(arc, color = Gold, style = s)
        drawCircle(Gold, radius = 2.dp.toPx(), center = Offset(size.width * 0.50f, size.height * 0.72f))
    }
}

@Composable
private fun KeyGlyph() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Gold, radius = size.width * 0.22f, center = Offset(size.width * 0.30f, size.height * 0.40f), style = s)
        val stem = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.52f)
            lineTo(size.width * 0.90f, size.height * 0.52f)
            moveTo(size.width * 0.78f, size.height * 0.52f)
            lineTo(size.width * 0.78f, size.height * 0.66f)
            moveTo(size.width * 0.90f, size.height * 0.52f)
            lineTo(size.width * 0.90f, size.height * 0.66f)
        }
        drawPath(stem, color = Gold, style = s)
    }
}

