package com.baltajmn.flowtime.features.screens.coop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.components.FlowCard
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.design.theme.SheetTitle
import com.baltajmn.flowtime.data.coop.CoopAccount
import com.baltajmn.flowtime.data.coop.CoopRoom
import com.baltajmn.flowtime.data.coop.FRIEND_LINK
import com.baltajmn.flowtime.data.coop.ROOM_LINK
import com.baltajmn.flowtime.data.coop.isValidName
import com.baltajmn.flowtime.data.coop.qrEncode
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.features.screens.platform.rememberShareText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlin.math.floor
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Coop (docs/coop.md 5): estudiar juntos en una sala. Solo aquí se pide cuenta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoopSheet(onDismiss: () -> Unit, viewModel: CoopViewModel = koinViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.coop_title),
                style = SheetTitle,
                modifier = Modifier.semantics { heading() }
            )
            ui.message?.let { message ->
                Text(text = stringResource(message), color = MaterialTheme.colorScheme.primary)
                LaunchedEffect(message) {
                    delay(4_000)
                    viewModel.onMessageShown()
                }
            }
            when (ui.stage) {
                CoopStage.SIGNED_OUT -> Intro(onFailed = viewModel::onSignInFailed)
                CoopStage.LOADING -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                CoopStage.OFFLINE -> {
                    Text(stringResource(Res.string.coop_offline))
                    TextButton(onClick = viewModel::load) { Text(stringResource(Res.string.retry)) }
                }
                CoopStage.NEEDS_NAME -> NameStep(busy = ui.busy, onDone = viewModel::createProfile)
                CoopStage.READY -> Ready(ui, viewModel)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Intro(onFailed: () -> Unit, account: CoopAccount = koinInject()) {
    val auth = account.client.composeAuth
    val onResult = { r: NativeSignInResult -> if (r is NativeSignInResult.Error || r is NativeSignInResult.NetworkError) onFailed() }
    // Nativo donde la plataforma lo tiene (Apple en iPhone, Google en Android) y por la web en el otro.
    val apple = auth.rememberSignInWithApple(onResult = onResult, fallback = { account.client.auth.signInWith(Apple) })
    val google = auth.rememberSignInWithGoogle(onResult = onResult, fallback = { account.client.auth.signInWith(Google) })

    Text(stringResource(Res.string.coop_intro))
    Text(stringResource(Res.string.coop_no_account_needed), color = MaterialTheme.colorScheme.onSurfaceVariant)
    // Apple primero e igual de visible que Google: App Store 4.8.
    OutlinedButton(onClick = { apple.startFlow() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_sign_in_apple))
    }
    OutlinedButton(onClick = { google.startFlow() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_sign_in_google))
    }
}

@Composable
private fun NameStep(busy: Boolean, onDone: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var adult by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(Res.string.coop_name_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(Res.string.coop_name_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(30) },
        label = { Text(stringResource(Res.string.coop_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = adult, onCheckedChange = { adult = it })
        Text(stringResource(Res.string.coop_age))
    }
    Button(onClick = { onDone(name) }, enabled = !busy && adult && isValidName(name), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_continue))
    }
}

@Composable
private fun Ready(ui: CoopUi, viewModel: CoopViewModel) {
    val room = ui.room
    if (room == null) NoRoom(ui, viewModel) else Room(room, ui, viewModel)
    Friends(ui, viewModel, inRoom = room != null)
    Account(ui, viewModel)
}

@Composable
private fun NoRoom(ui: CoopUi, viewModel: CoopViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Text(stringResource(Res.string.coop_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = viewModel::createRoom, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.coop_create_room))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(stringResource(Res.string.coop_join_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(onClick = { viewModel.join(code) }, enabled = !ui.busy && code.isNotBlank()) {
            Text(stringResource(Res.string.coop_join))
        }
    }
    ui.invites.forEach { invite ->
        FlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.coop_invited_by, invite.inviterName))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { viewModel.join(invite.code) }, enabled = !ui.busy) {
                        Text(stringResource(Res.string.coop_join))
                    }
                    TextButton(onClick = { viewModel.declineInvite(invite.roomId) }) {
                        Text(stringResource(Res.string.coop_decline))
                    }
                }
            }
        }
    }
}

@Composable
private fun Room(room: CoopRoom, ui: CoopUi, viewModel: CoopViewModel, engine: FocusEngine = koinInject()) {
    val session by engine.state.collectAsStateWithLifecycle()
    val share = rememberShareText()
    val link = ROOM_LINK + room.code
    val shareText = stringResource(Res.string.coop_share_room_text, link)
    FlowCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.coop_room_code, room.code), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(phaseLabel(session)), color = MaterialTheme.colorScheme.primary)
            ui.event?.let { Text(stringResource(eventLabel(it.action), it.byName), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(stringResource(Res.string.coop_room_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = { share(shareText) }) { Text(stringResource(Res.string.coop_share_room)) }
        }
    }
    room.members.forEach { member ->
        PersonRow(
            name = member.displayName,
            actions = buildList {
                if (member.id != ui.me?.id) {
                    if (room.owner == ui.me?.id) add(Res.string.coop_remove to { viewModel.removeMember(member.id) })
                    add(Res.string.coop_report to { viewModel.report(member.id) })
                    add(Res.string.coop_block to { viewModel.block(member.id) })
                }
            },
            blockName = member.displayName
        )
    }
    TextButton(onClick = viewModel::leave, enabled = !ui.busy) { Text(stringResource(Res.string.coop_leave)) }
}

@Composable
private fun Friends(ui: CoopUi, viewModel: CoopViewModel, inRoom: Boolean) {
    val me = ui.me ?: return
    val share = rememberShareText()
    val link = FRIEND_LINK + me.inviteCode
    val shareText = stringResource(Res.string.coop_share_link_text, link)
    var friendLink by rememberSaveable { mutableStateOf("") }
    var showQr by rememberSaveable { mutableStateOf(false) }

    val requests = ui.friends.filter { it.status != "accepted" }
    if (requests.isNotEmpty()) {
        Text(stringResource(Res.string.coop_requests), style = MaterialTheme.typography.titleMedium)
        requests.forEach { request ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.displayName, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.accept(request.id) }) { Text(stringResource(Res.string.coop_accept)) }
                TextButton(onClick = { viewModel.decline(request.id) }) { Text(stringResource(Res.string.coop_decline)) }
            }
        }
    }

    Text(stringResource(Res.string.coop_friends), style = MaterialTheme.typography.titleMedium)
    val accepted = ui.friends.filter { it.status == "accepted" }
    if (accepted.isEmpty()) Text(stringResource(Res.string.coop_no_friends), color = MaterialTheme.colorScheme.onSurfaceVariant)
    accepted.forEach { friend ->
        val here = ui.room?.members?.any { it.id == friend.id } == true
        PersonRow(
            name = friend.displayName,
            trailing = if (inRoom && !here) {
                {
                    val invited = friend.id in ui.invited
                    TextButton(onClick = { viewModel.inviteFriend(friend.id) }, enabled = !invited) {
                        Text(stringResource(if (invited) Res.string.coop_invited else Res.string.coop_invite))
                    }
                }
            } else {
                null
            },
            actions = listOf(
                Res.string.coop_remove_friend to { viewModel.removeFriend(friend.id) },
                Res.string.coop_report to { viewModel.report(friend.id) },
                Res.string.coop_block to { viewModel.block(friend.id) }
            ),
            blockName = friend.displayName
        )
    }

    Text(stringResource(Res.string.coop_my_link), style = MaterialTheme.typography.titleMedium)
    Text(link, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { share(shareText) }) { Text(stringResource(Res.string.coop_share_room)) }
        TextButton(onClick = { showQr = !showQr }) { Text("QR") }
        TextButton(onClick = viewModel::newLink, enabled = !ui.busy) { Text(stringResource(Res.string.coop_new_link)) }
    }
    if (showQr) Qr(link)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = friendLink,
            onValueChange = { friendLink = it },
            label = { Text(stringResource(Res.string.coop_friend_link_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = {
                viewModel.addFriend(friendLink)
                friendLink = ""
            },
            enabled = !ui.busy && friendLink.isNotBlank()
        ) { Text(stringResource(Res.string.coop_add)) }
    }
}

/** Nombre, cerrar sesión y borrar la cuenta (Apple 5.1.1(v)): siempre a mano dentro de Coop. */
@Composable
private fun Account(ui: CoopUi, viewModel: CoopViewModel) {
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Text(stringResource(Res.string.coop_account), style = MaterialTheme.typography.titleMedium)
    ui.me?.let { Text(it.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { renaming = true }) { Text(stringResource(Res.string.coop_rename)) }
        TextButton(onClick = viewModel::loadBlocked) { Text(stringResource(Res.string.coop_blocked)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = viewModel::signOut) { Text(stringResource(Res.string.coop_sign_out)) }
        TextButton(onClick = { deleting = true }) {
            Text(stringResource(Res.string.coop_delete_account), color = MaterialTheme.colorScheme.error)
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(ui.me?.displayName.orEmpty()) }
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
                ) { Text(stringResource(Res.string.coop_delete), color = MaterialTheme.colorScheme.error) }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(person.displayName, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.unblock(person.id) }) { Text(stringResource(Res.string.coop_unblock)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closeBlocked) { Text(stringResource(Res.string.close)) } }
        )
    }
}

/** Un nombre con su menú; bloquear pide confirmación, lo demás no. */
@Composable
private fun PersonRow(
    name: String,
    actions: List<Pair<StringResource, () -> Unit>>,
    blockName: String,
    trailing: (@Composable () -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, modifier = Modifier.weight(1f))
        trailing?.invoke()
        if (actions.isNotEmpty()) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = name, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    actions.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            onClick = {
                                menu = false
                                if (label == Res.string.coop_block) confirm = action else action()
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
