package com.baltajmn.flowtime.features.screens.coop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.data.coop.Blocked
import com.baltajmn.flowtime.data.coop.CoopAccount
import com.baltajmn.flowtime.data.coop.CoopFriends
import com.baltajmn.flowtime.data.coop.CoopRooms
import com.baltajmn.flowtime.data.coop.FRIEND_LINK
import com.baltajmn.flowtime.data.coop.Friendship
import com.baltajmn.flowtime.data.coop.Profile
import com.baltajmn.flowtime.data.coop.RoomEvent
import com.baltajmn.flowtime.data.coop.CoopRoom
import com.baltajmn.flowtime.data.coop.RoomInvite
import com.baltajmn.flowtime.data.coop.friendCodeOf
import com.baltajmn.flowtime.data.coop.roomCodeOf
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimerMode
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** En qué punto está Coop para esta persona, de fuera hacia dentro. */
enum class CoopStage { SIGNED_OUT, LOADING, OFFLINE, NEEDS_NAME, READY }

data class CoopUi(
    val stage: CoopStage = CoopStage.LOADING,
    val me: Profile? = null,
    val room: CoopRoom? = null,
    val invites: List<RoomInvite> = emptyList(),
    val friends: List<Friendship> = emptyList(),
    val event: RoomEvent? = null,
    val busy: Boolean = false,
    /** Lo que contestó la última acción, para enseñarlo una vez. */
    val message: StringResource? = null,
    val blocked: List<Blocked>? = null,
    val invited: Set<String> = emptySet()
)

class CoopViewModel(
    private val account: CoopAccount,
    private val rooms: CoopRooms,
    private val friends: CoopFriends,
    private val engine: FocusEngine
) : ViewModel() {

    private val local = MutableStateFlow(CoopUi())
    private val loaded = MutableStateFlow(false)
    private val offline = MutableStateFlow(false)

    val ui: StateFlow<CoopUi> = combine(
        combine(account.status, account.me, account.needsName, loaded, offline) { status, me, needsName, isLoaded, isOffline ->
            when {
                status is SessionStatus.NotAuthenticated -> CoopStage.SIGNED_OUT
                isOffline || status is SessionStatus.RefreshFailure -> CoopStage.OFFLINE
                needsName -> CoopStage.NEEDS_NAME
                me != null && isLoaded -> CoopStage.READY
                else -> CoopStage.LOADING
            } to me
        },
        rooms.room,
        rooms.invites,
        friends.friends,
        combine(rooms.lastEvent, local, ::Pair)
    ) { (stage, me), room, invites, list, (event, extra) ->
        extra.copy(stage = stage, me = me, room = room, invites = invites, friends = list, event = event)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoopUi())

    init {
        viewModelScope.launch {
            account.status.collect { if (it is SessionStatus.Authenticated) load() }
        }
        // Un enlace que llega con la hoja ya abierta y la cuenta lista.
        viewModelScope.launch {
            friends.pendingLink.collect { link ->
                if (link != null && account.me.value != null && loaded.value) {
                    friends.pendingLink.value = null
                    openLink(link)
                }
            }
        }
    }

    /** Al abrir la hoja, al volver la red y al pulsar reintentar. */
    fun load() = launch {
        offline.value = false
        runCatching {
            account.loadMe()
            if (account.me.value != null) {
                rooms.refresh()
                friends.refresh()
                friends.pendingLink.value?.let { link ->
                    friends.pendingLink.value = null
                    openLink(link)
                }
            }
            loaded.value = true
        }.onFailure { offline.value = true }
    }

    fun createProfile(name: String) = act {
        account.createProfile(name)
        load()
    }

    fun rename(name: String) = act { account.rename(name) }

    fun createRoom() = act {
        // Los tiempos del Pomodoro de quien la crea, dentro de lo que la sala admite.
        val work = engine.workMillis(TimerMode.POMODORO).coerceIn(5 * MINUTE, 90 * MINUTE)
        val rest = engine.pomodoroBreakMillis().coerceIn(MINUTE, 30 * MINUTE)
        rooms.create(work, rest)
    }

    fun join(text: String) = act {
        val code = roomCodeOf(text) ?: return@act say(Res.string.coop_join_not_found)
        when (rooms.join(code)) {
            "full" -> say(Res.string.coop_join_full)
            "not_found" -> say(Res.string.coop_join_not_found)
            "too_many" -> say(Res.string.coop_too_many)
        }
    }

    fun declineInvite(roomId: String) = act { rooms.declineInvite(roomId) }

    fun leave() = act { rooms.leave() }

    fun removeMember(id: String) = act { rooms.removeMember(id) }

    fun inviteFriend(id: String) = act {
        rooms.invite(id)
        local.value = local.value.copy(invited = local.value.invited + id)
    }

    fun addFriend(link: String) = act {
        val code = friendCodeOf(link) ?: link.trim().lowercase().takeIf { it.matches(Regex("[0-9a-f]{10}")) }
            ?: return@act say(Res.string.coop_friend_not_found)
        say(friendAnswer(friends.request(code)))
    }

    /** Un enlace abierto desde fuera: de amistad o de sala. Sin cuenta, espera en pendingLink. */
    private fun openLink(link: String) {
        if (link.startsWith(FRIEND_LINK)) addFriend(link) else join(link)
    }

    fun accept(id: String) = act { friends.accept(id) }

    fun decline(id: String) = act { friends.decline(id) }

    fun removeFriend(id: String) = act { friends.remove(id) }

    fun block(id: String) = act {
        friends.block(id)
        rooms.refresh()
    }

    fun report(id: String) = act {
        friends.report(id)
        say(Res.string.coop_reported)
    }

    fun loadBlocked() = act { local.value = local.value.copy(blocked = friends.blocked()) }

    fun closeBlocked() {
        local.value = local.value.copy(blocked = null)
    }

    fun unblock(id: String) = act {
        friends.unblock(id)
        local.value = local.value.copy(blocked = friends.blocked())
    }

    fun newLink() = act { friends.regenerateCode() }

    fun signOut() = act {
        rooms.forget()
        friends.forget()
        account.signOut()
        loaded.value = false
    }

    fun deleteAccount() = act {
        account.deleteAccount()
        rooms.forget()
        friends.forget()
        loaded.value = false
    }

    fun onMessageShown() {
        local.value = local.value.copy(message = null)
    }

    fun onSignInFailed() = say(Res.string.coop_sign_in_failed)

    private fun say(message: StringResource) {
        local.value = local.value.copy(message = message)
    }

    private fun act(block: suspend () -> Unit) = launch {
        local.value = local.value.copy(busy = true)
        runCatching { block() }.onFailure { say(Res.string.coop_offline) }
        local.value = local.value.copy(busy = false)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val MINUTE = 60_000L

        fun friendAnswer(answer: String) = when (answer) {
            "sent" -> Res.string.coop_friend_sent
            "accepted" -> Res.string.coop_friend_accepted
            "already" -> Res.string.coop_friend_already
            "self" -> Res.string.coop_friend_self
            "limit" -> Res.string.coop_friend_limit
            "too_many" -> Res.string.coop_too_many
            // "blocked" se dice igual que "not_found": nadie averigua por un enlace que lo bloquearon.
            else -> Res.string.coop_friend_not_found
        }
    }
}
