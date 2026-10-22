package com.baltajmn.flowtime.features.screens.coop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.design.theme.SheetTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.coop.CoopAccount
import com.baltajmn.flowtime.data.coop.CoopRoom
import com.baltajmn.flowtime.data.coop.FRIEND_LINK
import com.baltajmn.flowtime.data.coop.Friendship
import com.baltajmn.flowtime.data.coop.ROOM_LINK
import com.baltajmn.flowtime.data.coop.isValidName
import com.baltajmn.flowtime.data.coop.qrEncode
import com.baltajmn.flowtime.data.coop.signInWithGoogleFallback
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.features.screens.platform.hasAppleSignIn
import com.baltajmn.flowtime.features.screens.platform.rememberShareText
import com.baltajmn.flowtime.features.screens.settings.ButtonRow
import com.baltajmn.flowtime.features.screens.settings.SettingsCard
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Coop (docs/coop.md 5): estudiar juntos en una sala. Solo aquí se pide cuenta. Secciones con título
 * sobre la hoja y no tarjetas: la única superficie con color es lo que está vivo (la sala, una invitación).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoopSheet(onDismiss: () -> Unit, viewModel: CoopViewModel = koinViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    // Se marca antes de entrar y vale para crear el perfil sin pedir nada más (Apple 4).
    var adult by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.coop_title),
                    style = SheetTitle,
                    modifier = Modifier.semantics { heading() }
                )
                // Con sesión, con qué nombre te ven: ocupa el sitio del párrafo de presentación.
                val me = ui.me
                val subtitle = when {
                    ui.stage == CoopStage.SIGNED_OUT -> stringResource(Res.string.coop_intro)
                    ui.stage == CoopStage.READY && me != null -> stringResource(Res.string.coop_signed_in_as, me.displayName)
                    else -> null
                }
                subtitle?.let { Text(it, style = SubBody, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            // Se queda hasta la siguiente acción: borrado a los 4 s no daba tiempo a leerlo.
            ui.message?.let { message ->
                Text(
                    text = stringResource(message),
                    style = SubBody,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            when (ui.stage) {
                CoopStage.SIGNED_OUT -> Intro(adult = adult, onAdult = { adult = it }, onFailed = viewModel::onSignInFailed)
                CoopStage.LOADING -> Loading()
                CoopStage.OFFLINE -> Offline(onRetry = viewModel::load)
                CoopStage.NEEDS_NAME -> NewProfileStep(adult = adult, onAdult = { adult = it }, busy = ui.busy, onCreate = viewModel::createProfile)
                CoopStage.READY -> Ready(ui, viewModel)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Cargando y sin conexión reservan alto: la hoja no da saltos de un estado a otro. */
@Composable
private fun Loading() {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(Res.string.coop_loading),
            style = SubBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
private fun Offline(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(Res.string.coop_offline),
            style = SubBody,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
private fun Intro(adult: Boolean, onAdult: (Boolean) -> Unit, onFailed: () -> Unit, account: CoopAccount = koinInject()) {
    val auth = account.client.composeAuth
    val onResult = { r: NativeSignInResult -> if (r is NativeSignInResult.Error || r is NativeSignInResult.NetworkError) onFailed() }
    // Google en los dos (nativo en Android, propio en el iPhone); Apple solo en el iPhone.
    val google = auth.rememberSignInWithGoogle(
        onResult = onResult,
        fallback = { runCatching { account.signInWithGoogleFallback() }.onFailure { onFailed() } }
    )
    val apple = auth.rememberSignInWithApple(onResult = onResult, fallback = { account.client.auth.signInWith(Apple) })

    // Para la cuenta de App Review, que no puede usar Apple ni Google en un dispositivo ajeno. A la vista
    // y no un gesto oculto (Apple 2.3.1); discreto porque nadie más lo necesita.
    var showEmail by rememberSaveable { mutableStateOf(false) }
    if (showEmail) {
        EmailForm(account = account, onCancel = { showEmail = false })
        return
    }
    SharedRing()
    AgeCheck(adult, onAdult)
    // Apple primero e igual de visible que Google donde está: App Store 4.8.
    if (hasAppleSignIn) SignInButton(Res.string.coop_sign_in_apple, adult) { apple.startFlow() }
    SignInButton(Res.string.coop_sign_in_google, adult) { google.startFlow() }
    // Apagados sin decir por qué parecían rotos.
    if (!adult) Hint(Res.string.coop_age_needed, live = true)
    Hint(Res.string.coop_no_account_needed)
    TextButton(onClick = { showEmail = true }, enabled = adult, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_sign_in_email), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (adult) 1f else 0.38f))
    }
}

@Composable
private fun SignInButton(label: StringResource, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(stringResource(label), textAlign = TextAlign.Center)
    }
}

@Composable
private fun Hint(text: StringResource, live: Boolean = false) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().then(if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)
    )
}

/** Un anillo y tres personas en él: "un temporizador para todos" antes de leer nada. Callado para el lector. */
@Composable
private fun SharedRing() {
    val colors = MaterialTheme.colorScheme
    val people = listOf(colors.primary, colors.tertiary, colors.secondary)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(88.dp).clearAndSetSemantics {}) {
            val stroke = 5.dp.toPx()
            val radius = size.minDimension / 2 - 10.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            drawCircle(colors.outlineVariant, radius, style = Stroke(stroke))
            drawArc(colors.primary, -90f, 140f, false, topLeft, Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            people.forEachIndexed { i, color ->
                val angle = (i * 120.0 - 90.0) * PI / 180.0
                drawCircle(color, 8.dp.toPx(), Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat()))
            }
        }
    }
}

/** Correo y contraseña, solo para entrar: no hay alta (docs/coop.md 5). Al entrar, la hoja sigue como tras Apple o Google. */
@Composable
private fun EmailForm(account: CoopAccount, onCancel: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Sin rememberSaveable: la contraseña no se guarda en el estado de la pantalla.
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canSubmit = !busy && email.isNotBlank() && password.isNotEmpty()
    val submit = {
        busy = true
        failed = false
        scope.launch {
            runCatching { account.signInWithEmail(email, password) }.onFailure { failed = true }
            busy = false
        }
        Unit
    }
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(Res.string.coop_email_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(Res.string.coop_password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
        modifier = Modifier.fillMaxWidth()
    )
    if (failed) {
        Text(
            text = stringResource(Res.string.coop_email_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    Button(onClick = { submit() }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_email_submit))
    }
    TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_cancel))
    }
}

/** Toda la fila marca la casilla, y el lector la lee con su texto: antes eran una casilla sin nombre y un texto suelto. */
@Composable
private fun AgeCheck(adult: Boolean, onAdult: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .toggleable(value = adult, role = Role.Checkbox, onValueChange = onAdult)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = adult, onCheckedChange = null, modifier = Modifier.padding(8.dp))
        Text(stringResource(Res.string.coop_age), style = SubBody.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(vertical = 12.dp))
    }
}

/**
 * Primera vez: el perfil se crea solo, con el nombre de la cuenta o uno por defecto que se cambia en Ajustes
 * (Apple 4: tras Sign in with Apple no se pide nombre ni correo). Solo queda la edad si no se marcó al entrar.
 */
@Composable
private fun NewProfileStep(adult: Boolean, onAdult: (Boolean) -> Unit, busy: Boolean, onCreate: () -> Unit) {
    LaunchedEffect(adult) { if (adult && !busy) onCreate() }
    if (!adult) AgeCheck(adult, onAdult)
    if (busy) {
        Loading()
    } else {
        Button(onClick = onCreate, enabled = adult, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.coop_continue))
        }
    }
}

@Composable
private fun Ready(ui: CoopUi, viewModel: CoopViewModel) {
    val room = ui.room
    if (room == null) NoRoom(ui, viewModel) else Room(room, ui, viewModel)
    SectionDivider()
    Friends(ui, viewModel, inRoom = room != null)
    SectionDivider()
    AddFriend(ui, viewModel)
}

@Composable
private fun SectionDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

/** Un bloque de la hoja: su título como encabezado y lo suyo debajo, sin tarjeta. */
@Composable
private fun Section(title: StringResource, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = SmallTitle, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** El rótulo de un grupo dentro de una sección (solicitudes, tus amigos). */
@Composable
private fun GroupLabel(text: StringResource) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() }
    )
}

@Composable
private fun NoRoom(ui: CoopUi, viewModel: CoopViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    // Una invitación espera a alguien: va primero y con color, y con ella crear una sala deja de ser lo principal.
    ui.invites.forEach { invite ->
        LiveBox {
            Text(stringResource(Res.string.coop_invited_by, invite.inviterName), style = SubBody)
            Answers(
                name = invite.inviterName,
                no = Res.string.coop_decline to { viewModel.declineInvite(invite.roomId) },
                yes = Res.string.coop_join to { viewModel.join(invite.code) },
                enabled = !ui.busy,
                filled = true
            )
        }
    }
    Section(Res.string.coop_section_room) {
        if (ui.invites.isEmpty()) {
            Button(onClick = viewModel::createRoom, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.coop_create_room))
            }
        } else {
            FilledTonalButton(onClick = viewModel::createRoom, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.coop_create_room))
            }
        }
        FieldWithButton(
            value = code,
            onValueChange = { code = it },
            label = Res.string.coop_join_label,
            button = Res.string.coop_join,
            enabled = !ui.busy && code.isNotBlank(),
            onSubmit = { viewModel.join(code) }
        )
    }
}

/** Lo vivo de la hoja: una sala en marcha o una invitación, en el contenedor del acento suave. */
@Composable
private fun LiveBox(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onSecondaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.secondaryContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * Lo que descarta a la izquierda y lo que confirma a la derecha, en todas las filas. Cada botón dice a
 * quién contesta: el lector repite "Aceptar" por persona y sin el nombre no se sabe de quién.
 */
@Composable
private fun Answers(
    name: String,
    no: Pair<StringResource, () -> Unit>,
    yes: Pair<StringResource, () -> Unit>,
    enabled: Boolean,
    filled: Boolean = false
) {
    val noLabel = stringResource(no.first)
    val yesLabel = stringResource(yes.first)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = no.second, enabled = enabled, modifier = Modifier.semantics { contentDescription = "$noLabel, $name" }) {
            Text(noLabel)
        }
        val yesModifier = Modifier.semantics { contentDescription = "$yesLabel, $name" }
        if (filled) {
            Button(onClick = yes.second, enabled = enabled, modifier = yesModifier) { Text(yesLabel) }
        } else {
            FilledTonalButton(onClick = yes.second, enabled = enabled, modifier = yesModifier) { Text(yesLabel) }
        }
    }
}

/** Un campo y su botón al lado, como unirse a una sala o añadir un amigo. */
@Composable
private fun FieldWithButton(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    button: StringResource,
    enabled: Boolean,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit() }),
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onSubmit, enabled = enabled) { Text(stringResource(button)) }
    }
}

@Composable
private fun Room(room: CoopRoom, ui: CoopUi, viewModel: CoopViewModel, engine: FocusEngine = koinInject()) {
    val session by engine.state.collectAsStateWithLifecycle()
    val share = rememberShareText()
    val shareText = stringResource(Res.string.coop_share_room_text, ROOM_LINK + room.code)
    val colors = MaterialTheme.colorScheme
    LiveBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // El código grande y espaciado, para dictarlo; el lector lo lee junto a su rótulo.
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(stringResource(Res.string.coop_section_room), style = MaterialTheme.typography.labelLarge)
                Text(room.code, style = Title.copy(letterSpacing = 2.sp))
            }
            Button(onClick = { share(shareText) }) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.coop_share_room))
            }
        }
        // Lo que pasa ahora en la pastilla; lo último que hizo alguien, debajo y más pequeño.
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.primary))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(phaseLabel(session)), style = SubBody.copy(fontWeight = FontWeight.W600))
            }
            ui.event?.let { Text(stringResource(eventLabel(it.action), it.byName), style = MaterialTheme.typography.bodyMedium) }
        }
        HorizontalDivider(color = colors.onSecondaryContainer.copy(alpha = 0.2f))
        room.members.forEach { member ->
            val mine = member.id == ui.me?.id
            PersonRow(
                name = if (mine) stringResource(Res.string.coop_you, member.displayName) else member.displayName,
                initialOf = member.displayName,
                actions = buildList {
                    if (!mine) {
                        if (room.owner == ui.me?.id) add(Res.string.coop_remove to { viewModel.removeMember(member.id) })
                        add(Res.string.coop_report to { viewModel.report(member.id) })
                        add(Res.string.coop_block to { viewModel.block(member.id) })
                    }
                },
                blockName = member.displayName
            )
        }
        Text(stringResource(Res.string.coop_room_hint), style = MaterialTheme.typography.bodyMedium)
        // Aparte y al pie: salir no es lo mismo que compartir.
        OutlinedButton(
            onClick = viewModel::leave,
            enabled = !ui.busy,
            border = BorderStroke(1.dp, colors.onSecondaryContainer.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSecondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(Res.string.coop_leave)) }
    }
}

/** Amigos: solicitudes (si hay) y la lista. Sin nadie todavía, una línea que lleva a la sección de abajo. */
@Composable
private fun Friends(ui: CoopUi, viewModel: CoopViewModel, inRoom: Boolean) {
    val requests = ui.friends.filter { it.status != "accepted" }
    val accepted = ui.friends.filter { it.status == "accepted" }
    Section(Res.string.coop_friends) {
        if (requests.isEmpty() && accepted.isEmpty()) {
            Text(stringResource(Res.string.coop_no_friends), style = SubBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (requests.isNotEmpty()) {
            GroupLabel(Res.string.coop_requests)
            requests.forEach { request -> RequestRow(request, ui.busy, viewModel) }
        }
        if (accepted.isNotEmpty()) {
            if (requests.isNotEmpty()) GroupLabel(Res.string.coop_your_friends)
            accepted.forEach { friend ->
                val here = ui.room?.members?.any { it.id == friend.id } == true
                PersonRow(
                    name = friend.displayName,
                    trailing = when {
                        !inRoom -> null
                        // Ya dentro: se dice, en vez de un botón apagado que no se lee.
                        here -> { { Status(Res.string.coop_in_room) } }
                        friend.id in ui.invited -> { { Status(Res.string.coop_invited) } }
                        else -> {
                            {
                                val label = stringResource(Res.string.coop_invite)
                                FilledTonalButton(
                                    onClick = { viewModel.inviteFriend(friend.id) },
                                    modifier = Modifier.semantics { contentDescription = "$label, ${friend.displayName}" }
                                ) { Text(label) }
                            }
                        }
                    },
                    actions = listOf(
                        Res.string.coop_remove_friend to { viewModel.removeFriend(friend.id) },
                        Res.string.coop_report to { viewModel.report(friend.id) },
                        Res.string.coop_block to { viewModel.block(friend.id) }
                    ),
                    blockName = friend.displayName
                )
            }
        }
    }
}

@Composable
private fun Status(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Quién pide en su propia línea y la respuesta debajo: con letra grande, a su lado el nombre se quedaba sin sitio. */
@Composable
private fun RequestRow(request: Friendship, busy: Boolean, viewModel: CoopViewModel) {
    Column {
        PersonRow(name = request.displayName, actions = emptyList(), blockName = request.displayName)
        Answers(
            name = request.displayName,
            no = Res.string.coop_decline to { viewModel.decline(request.id) },
            yes = Res.string.coop_accept to { viewModel.accept(request.id) },
            enabled = !busy
        )
    }
}

/**
 * Tu enlace (compartirlo o enseñar su QR) y el de otra persona. Cambiar el enlace está en Ajustes:
 * deja sin valor el viejo y no es de todos los días. La respuesta a añadir se lee aquí mismo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFriend(ui: CoopUi, viewModel: CoopViewModel) {
    val me = ui.me ?: return
    val link = FRIEND_LINK + me.inviteCode
    val share = rememberShareText()
    val shareText = stringResource(Res.string.coop_share_link_text, link)
    var showQr by rememberSaveable { mutableStateOf(false) }
    var friendLink by rememberSaveable { mutableStateOf("") }
    Section(Res.string.coop_add_friend) {
        Text(stringResource(Res.string.coop_add_hint), style = SubBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { share(shareText) }) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.coop_share_my_link))
            }
            OutlinedButton(onClick = { showQr = !showQr }) { Text(stringResource(Res.string.coop_show_qr)) }
        }
        if (showQr) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Qr(link) }
        }
        FieldWithButton(
            value = friendLink,
            onValueChange = { friendLink = it },
            label = Res.string.coop_friend_link_label,
            button = Res.string.coop_add,
            enabled = !ui.busy && friendLink.isNotBlank(),
            onSubmit = { viewModel.addFriend(friendLink) { friendLink = "" } }
        )
        ui.friendMessage?.let { message ->
            Text(
                text = stringResource(message),
                style = SubBody,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

/**
 * Ajustes > Cuenta de Coop: nombre, bloqueados, el enlace y, aparte y al final, cerrar sesión y borrar
 * la cuenta (Apple 5.1.1(v)). Solo con la cuenta lista; sin ella no ocupa sitio.
 */
@Composable
fun CoopAccountCard(viewModel: CoopViewModel = koinViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.stage != CoopStage.READY) return
    Column {
        SettingsCard(Res.string.coop_settings_title) { Account(ui, viewModel) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Account(ui: CoopUi, viewModel: CoopViewModel) {
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val me = ui.me
    if (me != null) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(me.displayName, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(me.displayName, style = SubBody.copy(fontWeight = FontWeight.W600, color = colors.onSurface))
                Text(stringResource(Res.string.coop_name_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            val change = stringResource(Res.string.coop_change)
            val rename = stringResource(Res.string.coop_rename)
            TextButton(onClick = { renaming = true }, modifier = Modifier.semantics { contentDescription = rename }) { Text(change) }
        }
    }
    ButtonRow(text = Res.string.coop_blocked, button = Res.string.settings_intro_button, onClick = viewModel::loadBlocked)
    ButtonRow(text = Res.string.coop_friend_link_row, button = Res.string.coop_new_link, onClick = { confirmNew = true })
    // Lo que cierra o borra, aparte y al final: lo último que se busca.
    HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
    ButtonRow(text = Res.string.coop_session_here, button = Res.string.coop_sign_out, onClick = viewModel::signOut)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(stringResource(Res.string.coop_delete_account), style = SubBody.copy(fontWeight = FontWeight.W600, color = colors.error))
            Text(stringResource(Res.string.coop_delete_text), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        val delete = stringResource(Res.string.coop_delete)
        val deleteAccount = stringResource(Res.string.coop_delete_account)
        TextButton(onClick = { deleting = true }, modifier = Modifier.semantics { contentDescription = deleteAccount }) {
            Text(delete, color = colors.error)
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(me?.displayName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(Res.string.coop_rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, singleLine = true) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rename(name)
                        renaming = false
                    },
                    enabled = isValidName(name)
                ) { Text(stringResource(Res.string.coop_continue)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(Res.string.coop_cancel)) } }
        )
    }
    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text(stringResource(Res.string.coop_new_link)) },
            text = { Text(stringResource(Res.string.coop_new_link_text)) },
            confirmButton = {
                // El viejo deja de valer: no se deshace.
                TextButton(
                    onClick = {
                        viewModel.newLink()
                        confirmNew = false
                    }
                ) { Text(stringResource(Res.string.coop_new_link), color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text(stringResource(Res.string.coop_cancel)) } }
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(Res.string.coop_delete_account)) },
            text = { Text(stringResource(Res.string.coop_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        deleting = false
                    }
                ) { Text(stringResource(Res.string.coop_delete), color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(Res.string.coop_cancel)) } }
        )
    }
    ui.blocked?.let { blocked ->
        AlertDialog(
            onDismissRequest = viewModel::closeBlocked,
            title = { Text(stringResource(Res.string.coop_blocked)) },
            text = {
                Column {
                    if (blocked.isEmpty()) Text(stringResource(Res.string.coop_no_blocked))
                    blocked.forEach { person ->
                        val unblock = stringResource(Res.string.coop_unblock)
                        Text(person.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        TextButton(
                            onClick = { viewModel.unblock(person.id) },
                            modifier = Modifier.align(Alignment.End).semantics { contentDescription = "$unblock, ${person.displayName}" }
                        ) { Text(unblock) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closeBlocked) { Text(stringResource(Res.string.close)) } }
        )
    }
}

/** La inicial en un círculo, del color de una etiqueta fijo para cada nombre. De adorno: el nombre ya se lee al lado. */
@Composable
private fun Avatar(name: String, size: Int = 36) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val fill = TagPalette.color(name.hashCode(), dark)
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(fill).clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = SubBody.copy(fontWeight = FontWeight.W600, color = if (dark) Color(0xFF1B1B1F) else Color.White)
        )
    }
}

/** Un nombre con su inicial y su menú; bloquear pide confirmación, lo demás no. */
@Composable
private fun PersonRow(
    name: String,
    actions: List<Pair<StringResource, () -> Unit>>,
    blockName: String,
    initialOf: String = name,
    trailing: (@Composable () -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Avatar(initialOf)
        Spacer(Modifier.width(12.dp))
        Text(name, style = SubBody, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
        if (actions.isNotEmpty()) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.coop_person_menu, blockName), tint = LocalContentColor.current)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    actions.forEach { (label, action) ->
                        val destructive = label == Res.string.coop_block
                        DropdownMenuItem(
                            text = { Text(stringResource(label), color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified) },
                            onClick = {
                                menu = false
                                if (destructive) confirm = action else action()
                            }
                        )
                    }
                }
            }
        }
    }
    confirm?.let { block ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(Res.string.coop_block)) },
            text = { Text(stringResource(Res.string.coop_block_text, blockName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        block()
                        confirm = null
                    }
                ) { Text(stringResource(Res.string.coop_block), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(Res.string.coop_cancel)) } }
        )
    }
}

@Composable
private fun Qr(link: String) {
    val qr = remember(link) { qrEncode(link) } ?: return
    val label = stringResource(Res.string.coop_qr)
    // Siempre negro sobre blanco: una cámara no lee un QR claro sobre oscuro.
    Canvas(
        Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .semantics { contentDescription = label }
    ) {
        // Píxeles enteros por módulo: uno fraccionario deja rayas entre módulos oscuros.
        val cell = floor(size.width / (qr.size + 8))
        val origin = (size.width - cell * qr.size) / 2
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (qr[x, y]) drawRect(Color.Black, Offset(origin + x * cell, origin + y * cell), Size(cell, cell))
            }
        }
    }
}

// La fase del motor, que sigue a la sala y cambia sola a su hora, no la de la última lectura.
private fun phaseLabel(state: FocusState): StringResource = when {
    state.isPaused -> Res.string.coop_phase_paused
    state.phase == Phase.WORK -> Res.string.coop_phase_focus
    state.phase == Phase.BREAK -> Res.string.coop_phase_break
    else -> Res.string.coop_phase_idle
}

private fun eventLabel(action: String): StringResource = when (action) {
    "start" -> Res.string.coop_event_start
    "pause" -> Res.string.coop_event_pause
    "resume" -> Res.string.coop_event_resume
    "skip_break" -> Res.string.coop_event_skip_break
    else -> Res.string.coop_event_stop
}
