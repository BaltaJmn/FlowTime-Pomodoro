package com.baltajmn.flowtime.data.coop

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import io.ktor.http.parameters
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Google en el iPhone sin el SDK de Google: compose-auth solo lo tiene nativo en Android, y su flujo web
 * pasa por el primer cliente de Supabase, que es el de Chroma (su pantalla diria "Chroma"). Aqui se
 * habla con Google con el cliente iOS de FlowTime (PKCE, ASWebAuthenticationSession) y el id_token se
 * entrega a Supabase, que lo acepta porque ese cliente esta en su lista de Google.
 *
 * Si la persona cancela no pasa nada; si algo falla, lanza.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
actual suspend fun CoopAccount.signInWithGoogleFallback() {
    val clientId = SupabaseConfig.googleIosClientId ?: return
    val scheme = "com.googleusercontent.apps." + clientId.removeSuffix(".apps.googleusercontent.com")
    val redirect = "$scheme:/oauth2redirect"
    // Dos UUID v4 (aleatorios del sistema): 72 caracteres validos para un code_verifier.
    val verifier = NSUUID().UUIDString + NSUUID().UUIDString
    val state = NSUUID().UUIDString
    val digest = UByteArray(32)
    val bytes = verifier.encodeToByteArray()
    bytes.usePinned { b -> digest.usePinned { d -> CC_SHA256(b.addressOf(0), bytes.size.convert(), d.addressOf(0)) } }
    val challenge = Base64.UrlSafe.encode(digest.toByteArray()).trimEnd('=')

    val url = "https://accounts.google.com/o/oauth2/v2/auth" +
        "?client_id=${clientId.encodeURLParameter()}&redirect_uri=${redirect.encodeURLParameter()}" +
        "&response_type=code&scope=${"openid email profile".encodeURLParameter()}" +
        "&code_challenge=$challenge&code_challenge_method=S256&state=$state"

    val callback = withContext(Dispatchers.Main) { authenticate(url, scheme) } ?: return
    val query = NSURLComponents(uRL = callback, resolvingAgainstBaseURL = false).queryItems.orEmpty()
        .map { it as NSURLQueryItem }.associate { it.name to it.value }
    check(query["state"] == state) { "google: state" }
    val code = query["code"] ?: error("google: ${query["error"]}")

    val tokens = HttpClient().use { http ->
        http.submitForm(
            "https://oauth2.googleapis.com/token",
            parameters {
                append("client_id", clientId)
                append("code", code)
                append("code_verifier", verifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirect)
            }
        ).bodyAsText()
    }
    val idToken = Json.parseToJsonElement(tokens).jsonObject["id_token"]?.jsonPrimitive?.content
        ?: error("google: sin id_token")
    client.auth.signInWith(IDToken) {
        this.idToken = idToken
        provider = Google
    }
}

/** La URL con la que Google vuelve, o null si se cancela. */
private suspend fun authenticate(url: String, scheme: String): NSURL? = suspendCancellableCoroutine { cont ->
    val anchor = Anchor()
    val session = ASWebAuthenticationSession(uRL = NSURL(string = url), callbackURLScheme = scheme) { callback, error ->
        anchor.hashCode() // la sesion no retiene a su proveedor: se mantiene vivo hasta aqui.
        when {
            callback != null -> cont.resume(callback)
            error == null || error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin -> cont.resume(null)
            else -> cont.resumeWith(Result.failure(IllegalStateException(error.localizedDescription)))
        }
    }
    session.presentationContextProvider = anchor
    cont.invokeOnCancellation { session.cancel() }
    session.start()
}

private class Anchor :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
        UIApplication.sharedApplication.keyWindow ?: UIWindow()
}
