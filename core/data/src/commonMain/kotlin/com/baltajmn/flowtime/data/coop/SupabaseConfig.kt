package com.baltajmn.flowtime.data.coop

/**
 * La mitad pública del proyecto de Supabase, compartido con Chroma (docs/coop.md 3.1). Los dos valores
 * viajan en el binario de todas formas; la clave de servicio, nunca. Nulos hasta tener el proyecto:
 * sin ellos Coop no aparece y la app es la de siempre.
 */
object SupabaseConfig {
    /** "https://<ref>.supabase.co". */
    val url: String? = null

    /** La clave anon. Pública a propósito: lo que protege los datos es RLS. */
    val anonKey: String? = null

    /** El cliente OAuth web de Google, que pide el inicio de sesión nativo en Android. */
    val googleServerClientId: String? = null
}
