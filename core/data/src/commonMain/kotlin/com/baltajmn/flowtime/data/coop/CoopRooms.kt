package com.baltajmn.flowtime.data.coop

import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerAction
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RoomInvite(
    @SerialName("room_id") val roomId: String,
    val code: String,
    @SerialName("invited_by") val invitedBy: String,
    @SerialName("inviter_name") val inviterName: String
)

/** Lo último que alguien hizo en la sala, para la línea "Ana ha pausado". */
data class RoomEvent(val action: String, val byName: String)

/**
 * La sala de esta persona: la lee del servidor, mueve el [FocusEngine] con ella y escucha su canal
 * de Realtime. docs/coop.md 3.4 y 4.
 */
class CoopRooms(
    private val account: CoopAccount,
    private val engine: FocusEngine,
    private val time: TimeSource,
    private val friends: CoopFriends
) {
    private val scope: CoroutineScope = MainScope()
    private val json = Json { ignoreUnknownKeys = true }

    private val _room = MutableStateFlow<CoopRoom?>(null)
    val room: StateFlow<CoopRoom?> = _room.asStateFlow()

    private val _invites = MutableStateFlow<List<RoomInvite>>(emptyList())
    val invites: StateFlow<List<RoomInvite>> = _invites.asStateFlow()

    private val _lastEvent = MutableStateFlow<RoomEvent?>(null)
    val lastEvent: StateFlow<RoomEvent?> = _lastEvent.asStateFlow()

    private var channel: RealtimeChannel? = null
    private var listening: Job? = null

    /** El canal `user:<id>`: avisa de solicitudes de amistad e invitaciones a sala mientras hay sesión. */
    private var inbox: RealtimeChannel? = null
    private var inboxJob: Job? = null

    init {
        engine.coopControl = { action -> scope.launch { runCatching { control(action) } } }
        // Tras reiniciar la app con una sala a medias: en cuanto la sesión esté viva, se relee.
        if (account.available && engine.state.value.coop != null) {
            scope.launch {
                account.status.first { it is SessionStatus.Authenticated }
                runCatching { refresh() }
            }
        }
        if (account.available) {
            scope.launch {
                account.status.collect {
                    if (it is SessionStatus.Authenticated) {
                        listenInbox()
                    } else if (it is SessionStatus.NotAuthenticated) {
                        stopInbox()
                    }
                }
            }
        }
    }

    /** Lo relee todo: al abrir Coop, al volver de segundo plano y con cada aviso del canal. */
    suspend fun refresh() {
        val sentAt = time.wallMillis()
        val raw = account.client.postgrest.rpc("my_room").decodeAs<JsonElement>()
        apply(raw, sentAt)
        refreshInvites()
    }

    /** Devuelve el código de la sala nueva. */
    suspend fun create(workMillis: Long, breakMillis: Long): String {
        val code = account.client.postgrest.rpc(
            "create_room",
            buildJsonObject {
                put("work_ms", workMillis)
                put("break_ms", breakMillis)
            }
        ).decodeAs<String>()
        refresh()
        return code
    }

    /** `joined`, `full`, `not_found` o `too_many`, como contesta el servidor. */
    suspend fun join(code: String): String {
        val answer = account.client.postgrest.rpc("join_room", buildJsonObject { put("code", code) }).decodeAs<String>()
        if (answer == "joined") refresh()
        return answer
    }

    suspend fun leave() {
        account.client.postgrest.rpc("leave_room")
        engine.leaveCoop()
        refresh()
    }

    /** `sent`, `no_room`, `not_friend` o `already`. */
    suspend fun invite(friendId: String): String =
        account.client.postgrest.rpc("invite_to_room", buildJsonObject { put("friend", friendId) }).decodeAs<String>()

    suspend fun declineInvite(roomId: String) {
        account.client.postgrest.rpc("decline_room_invite", buildJsonObject { put("room", roomId) })
        _invites.value = _invites.value.filterNot { it.roomId == roomId }
    }

    suspend fun removeMember(memberId: String) {
        account.client.postgrest.rpc("remove_member", buildJsonObject { put("member", memberId) })
        refresh()
    }

    suspend fun control(action: TimerAction) {
        val name = when (action) {
            TimerAction.START -> "start"
            TimerAction.PAUSE -> "pause"
            TimerAction.RESUME -> "resume"
            TimerAction.SKIP_BREAK -> "skip_break"
            TimerAction.STOP -> "stop"
            // Pomodoro: el trabajo termina solo, no hay descanso a petición.
            TimerAction.BREAK -> return
        }
        val sentAt = time.wallMillis()
        val raw = account.client.postgrest.rpc("room_control", buildJsonObject { put("action", name) }).decodeAs<JsonElement>()
        apply(raw, sentAt)
    }

    /** Al cerrar sesión o borrar la cuenta: el móvil vuelve a estudiar solo. */
    fun forget() {
        stopListening()
        _room.value = null
        _invites.value = emptyList()
        _lastEvent.value = null
        engine.leaveCoop()
    }

    private fun apply(raw: JsonElement, sentAt: Long) {
        val room = if (raw is JsonNull) null else json.decodeFromJsonElement(CoopRoom.serializer(), raw)
        _room.value = room
        if (room == null) {
            stopListening()
            engine.leaveCoop()
            return
        }
        engine.follow(room, clockOffset(room.serverNow, sentAt, time.wallMillis()))
        if (channel?.topic?.endsWith(room.id) != true) listen(room.id)
    }

    private fun listen(roomId: String) {
        stopListening()
        val ch = account.client.channel("room:$roomId") { isPrivate = true }
        channel = ch
        listening = scope.launch {
            launch {
                merge(*EVENTS.map { event -> ch.broadcastFlow<JsonObject>(event).map { event to it } }.toTypedArray())
                    .collect { (event, payload) ->
                        val by = (payload["by"] as? JsonPrimitive)?.contentOrNull
                        runCatching { refresh() }
                        val name = _room.value?.members?.firstOrNull { it.id == by }?.displayName
                        if (name != null && by != account.userId() && event != "members") {
                            _lastEvent.value = RoomEvent(event, name)
                        }
                    }
            }
            // Lo que llegó mientras el canal estaba caído (pantalla apagada, red, un error al unirse) no se
            // repite: cada vez que queda suscrito, también el primero (cierra el hueco desde my_room), se relee.
            launch {
                ch.status.collect { if (it == RealtimeChannel.Status.SUBSCRIBED) runCatching { refresh() } }
            }
            runCatching { ch.subscribe(blockUntilSubscribed = true) }
        }
    }

    private fun listenInbox() {
        val id = account.userId() ?: return
        if (inbox?.topic?.endsWith(id) == true) return
        stopInbox()
        val ch = account.client.channel("user:$id") { isPrivate = true }
        inbox = ch
        inboxJob = scope.launch {
            launch { ch.broadcastFlow<JsonObject>("friends").collect { runCatching { friends.refresh() } } }
            launch { ch.broadcastFlow<JsonObject>("invites").collect { runCatching { refreshInvites() } } }
            // Como en la sala: lo que llegó con el canal caído se recupera al volver a estar suscrito.
            launch {
                ch.status.collect {
                    if (it == RealtimeChannel.Status.SUBSCRIBED) {
                        runCatching {
                            friends.refresh()
                            refreshInvites()
                        }
                    }
                }
            }
            runCatching { ch.subscribe() }
        }
    }

    private fun stopInbox() {
        inboxJob?.cancel()
        inboxJob = null
        val ch = inbox ?: return
        inbox = null
        scope.launch { runCatching { account.client.realtime.removeChannel(ch) } }
    }

    private suspend fun refreshInvites() {
        _invites.value = account.client.postgrest.rpc("my_room_invites").decodeList<RoomInvite>()
    }

    private fun stopListening() {
        listening?.cancel()
        listening = null
        val ch = channel ?: return
        channel = null
        scope.launch { runCatching { account.client.realtime.removeChannel(ch) } }
    }

    private companion object {
        val EVENTS = listOf("start", "pause", "resume", "skip_break", "stop", "members")
    }
}
