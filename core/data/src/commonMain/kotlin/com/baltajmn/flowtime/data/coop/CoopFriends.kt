package com.baltajmn.flowtime.data.coop

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Friendship(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val status: String,
    @SerialName("requested_by") val requestedBy: String
)

@Serializable
data class Blocked(val id: String, @SerialName("display_name") val displayName: String)

@Serializable
private data class NewReport(val reporter: String, val reported: String, val reason: String)

/** El enlace de amistad: https://flowtime.baltajmn.dev/f/<código>. */
const val FRIEND_LINK = "https://flowtime.baltajmn.dev/f/"

/** El de una sala: https://flowtime.baltajmn.dev/r/<código>. */
const val ROOM_LINK = "https://flowtime.baltajmn.dev/r/"

/** Abre Coop sin hacer nada más: lo que manda tocar un aviso push (docs/coop-push.md). */
const val COOP_LINK = "https://flowtime.baltajmn.dev/"

/** El código de un enlace de amistad, o null si no es uno: 10 hexadecimales, como los da el servidor. */
fun friendCodeOf(link: String): String? =
    link.trim().removePrefix(FRIEND_LINK).lowercase().takeIf { link.trim().startsWith(FRIEND_LINK) && it.matches(Regex("[0-9a-f]{10}")) }

/** El código de un enlace de sala, o el código tal cual si se escribió a mano. */
fun roomCodeOf(text: String): String? =
    text.trim().removePrefix(ROOM_LINK).uppercase().takeIf { it.matches(Regex("[A-HJKMNP-Z2-9]{6}")) }

/**
 * Amigos y solicitudes, bloqueos y reportes: el Amigos de Chroma con su esquema propio
 * (docs/coop.md 3.3). Un enlace abierto antes de tener sesión se guarda en [pendingCode].
 */
class CoopFriends(private val account: CoopAccount) {

    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())

    /** Aceptados y solicitudes recibidas; las enviadas sin respuesta no se enseñan. */
    val friends: StateFlow<List<Friendship>> = _friends.asStateFlow()

    /**
     * Un enlace de amistad o de sala que llegó de fuera y aún no se usó: abre Coop, y se manda en cuanto
     * hay sesión y nombre. Solo en memoria: tras instalar, el enlace se vuelve a abrir.
     */
    val pendingLink = MutableStateFlow<String?>(null)

    suspend fun refresh() {
        val me = account.userId()
        _friends.value = account.client.postgrest.rpc("my_friendships").decodeList<Friendship>()
            .filter { it.status == ACCEPTED || it.requestedBy != me }
    }

    /** `sent`, `accepted`, `already`, `self`, `blocked`, `not_found`, `limit` o `too_many`. */
    suspend fun request(code: String): String {
        val answer = account.client.postgrest.rpc("request_friend", buildJsonObject { put("code", code) }).decodeAs<String>()
        runCatching { refresh() }
        return answer
    }

    suspend fun accept(id: String) = call("accept_friend", id)

    suspend fun decline(id: String) = call("decline_friend", id)

    suspend fun remove(id: String) = call("remove_friend", id)

    /** Lo saca de las listas antes de preguntar al servidor otra vez: el bloqueo ya está hecho. */
    suspend fun block(id: String) {
        account.client.postgrest.rpc("block_user", buildJsonObject { put("other", id) })
        _friends.value = _friends.value.filterNot { it.id == id }
        runCatching { refresh() }
    }

    suspend fun blocked(): List<Blocked> = account.client.postgrest.rpc("my_blocks").decodeList()

    suspend fun unblock(id: String) {
        account.client.postgrest.rpc("unblock_user", buildJsonObject { put("other", id) })
    }

    suspend fun regenerateCode() {
        account.client.postgrest.rpc("regenerate_code")
        account.loadMe()
    }

    /** Un segundo reporte de la misma persona choca con la clave única: para la app, ya está hecho. */
    suspend fun report(id: String) {
        val me = account.userId() ?: return
        runCatching { account.client.from("reports").insert(NewReport(me, id, "name")) }
            .onFailure { if (it.message?.contains("23505") != true) throw it }
    }

    fun forget() {
        _friends.value = emptyList()
        pendingLink.value = null
    }

    private suspend fun call(function: String, id: String) {
        account.client.postgrest.rpc(function, buildJsonObject { put("other", id) })
        runCatching { refresh() }
    }

    private companion object {
        const val ACCEPTED = "accepted"
    }
}
