package com.baltajmn.flowtime.data.coop

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.Realtime
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Adonde vuelve el inicio de sesión por la web: com.baltajmn.flowtime://login. */
const val LOGIN_SCHEME = "com.baltajmn.flowtime"
const val LOGIN_HOST = "login"

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("invite_code") val inviteCode: String = ""
)

/** `platform`: android, ios o ios-sandbox (builds de Xcode). `lang`: el idioma de los avisos. */
@Serializable
data class PushToken(val token: String, val platform: String, val lang: String)

@Serializable
private data class NewProfile(val id: String, @SerialName("display_name") val displayName: String)

@Serializable
private data class Rename(@SerialName("display_name") val displayName: String)

/** La regla del servidor para el nombre (de 1 a 30), para el campo y el botón. */
fun isValidName(name: String): Boolean = name.trim().length in 1..30

/**
 * La sesión con Supabase y quién es esta persona en Coop. Nada de aquí hace falta para usar FlowTime:
 * sin cuenta, o sin proyecto, la app es la de siempre. Es el `Social` de Chroma, con su esquema propio.
 */
class CoopAccount {

    val available: Boolean get() = SupabaseConfig.url != null && SupabaseConfig.anonKey != null

    val client: SupabaseClient by lazy {
        createSupabaseClient(SupabaseConfig.url!!, SupabaseConfig.anonKey!!) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = LOGIN_SCHEME
                host = LOGIN_HOST
            }
            install(Postgrest) { defaultSchema = "flowtime" }
            install(Realtime)
            install(Functions)
            install(ComposeAuth) {
                SupabaseConfig.googleServerClientId?.let { googleNativeLogin(serverClientId = it) }
                appleNativeLogin()
            }
        }
    }

    private val _me = MutableStateFlow<Profile?>(null)

    /** Esta persona, ya con sesión y nombre. Null mientras no se sabe, y cuando aún no hay. */
    val me: StateFlow<Profile?> = _me.asStateFlow()

    private val _needsName = MutableStateFlow(false)

    /** Se pidió el perfil y no hay: la primera vez, que aún necesita un nombre. */
    val needsName: StateFlow<Boolean> = _needsName.asStateFlow()

    /** Sesión viva o a la espera de red para renovarse. Sin proyecto, siempre NotAuthenticated. */
    val status: StateFlow<SessionStatus>
        get() = if (available) client.auth.sessionStatus else noSession

    fun userId(): String? = if (available) client.auth.currentUserOrNull()?.id else null

    /**
     * Lanza sin red y sin una sesión viva (un token caducado que aún no se renovó): quien llama lo dice
     * y ofrece reintentar, en vez de una espera que no acaba.
     */
    suspend fun loadMe() {
        userId() ?: error("no session")
        // Por función: el código de invitación no se lee de la tabla, ni el propio.
        val row = client.postgrest.rpc("my_profile").decodeList<Profile>().firstOrNull()
        _me.value = row
        _needsName.value = row == null
    }

    suspend fun createProfile(name: String) {
        val id = userId() ?: return
        client.from("profiles").insert(NewProfile(id, name.trim()))
        loadMe()
    }

    suspend fun rename(name: String) {
        val id = userId() ?: return
        client.from("profiles").update(Rename(name.trim())) { filter { eq("id", id) } }
        loadMe()
    }

    /** El de este móvil, cuando la plataforma lo tiene: FCM en Android, APNs en iOS (docs/coop-push.md). */
    val pushToken = MutableStateFlow<PushToken?>(null)

    /**
     * Apunta el token al arrancar con sesión, sin esperar a que se abra Coop, y otra vez si cambia la
     * cuenta o el token. Sin perfil la base no lo admite: espera a que se elija nombre. Sin permiso de
     * notificaciones se apunta igual y el sistema no enseña nada. Una vez por proceso.
     */
    fun watchPush(scope: CoroutineScope) {
        if (!available || watchingPush) return
        watchingPush = true
        scope.launch {
            combine(status, me, pushToken) { status, me, token ->
                if (status is SessionStatus.Authenticated && token != null) Triple(userId(), me != null, token) else null
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { (_, hasProfile, token) ->
                    // Sin red falla y se queda así hasta el próximo arranque: el aviso no es crítico.
                    runCatching {
                        if (hasProfile) client.postgrest.rpc("register_push_token", token) else loadMe()
                    }
                }
        }
    }

    private var watchingPush = false

    /** Deja la cuenta donde está: al volver a entrar vuelven los amigos. */
    suspend fun signOut() = withContext(NonCancellable) {
        // Antes de soltar la sesión, que hace falta para borrarlo: este móvil deja de avisar a esta cuenta.
        pushToken.value?.let { token ->
            runCatching { client.postgrest.rpc("unregister_push_token", buildJsonObject { put("token", token.token) }) }
        }
        // Sin red no se le puede decir al servidor, pero el móvil tiene que soltar la sesión igual.
        runCatching { client.auth.signOut() }.onFailure { runCatching { client.auth.clearSession() } }
        forget()
    }

    /**
     * Apple 5.1.1(v) y Play. El servidor borra lo de FlowTime y, si no hay nada en Chroma, el usuario
     * (docs/coop.md 6). El historial del móvil no es del servidor.
     */
    suspend fun deleteAccount() {
        val answer = client.functions.invoke("flowtime-delete-account")
        if (!answer.status.isSuccess()) error("flowtime-delete-account: ${answer.status}")
        // Desde aquí la cuenta ya no está, pase lo que pase con la pantalla que lo pidió.
        withContext(NonCancellable) {
            // Sin signOut: quizá el usuario ya no existe para cerrar su sesión.
            client.auth.clearSession()
            forget()
        }
    }

    private var watching = false

    /**
     * El cliente suelta la sesión solo cuando le revocan el token o borran el usuario desde el panel, y
     * nadie más lo dice: el móvil tiene que dejar de enseñar esa cuenta. Una vez por proceso.
     */
    fun watchSession(scope: CoroutineScope) {
        if (!available || watching) return
        watching = true
        scope.launch {
            var before: SessionStatus? = null
            client.auth.sessionStatus.collect { now ->
                if (sessionLost(before, now)) forget()
                before = now
            }
        }
    }

    private fun forget() {
        _me.value = null
        _needsName.value = false
    }

    private companion object {
        val noSession = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
    }
}

/**
 * Una sesión que el móvil tenía y el cliente soltó. No el NotAuthenticated con el que arranca un
 * móvil sin cuenta (isSignOut false): ahí no hay nada de nadie que olvidar.
 */
internal fun sessionLost(before: SessionStatus?, now: SessionStatus): Boolean =
    now is SessionStatus.NotAuthenticated &&
        (now.isSignOut || before is SessionStatus.Authenticated || before is SessionStatus.RefreshFailure)
