package com.baltajmn.flowtime.data.coop

/**
 * La mitad pública del proyecto de Supabase, compartido con Chroma (docs/coop.md 3.1). Los dos valores
 * viajan en el binario de todas formas; la clave de servicio, nunca. Sin ellos Coop no aparece y la app
 * es la de siempre. Proyecto baltajmn-social, el de Chroma (store/coop.md).
 */
object SupabaseConfig {
    /** "https://<ref>.supabase.co". */
    val url: String? = "https://djfbmvagflfptqhabdln.supabase.co"

    /** La clave anon. Pública a propósito: lo que protege los datos es RLS. */
    val anonKey: String? =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRqZmJtdmFnZmxmcHRxaGFiZGxuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI0MDEyMzQsImV4cCI6MjEwNzk3NzIzNH0.1Ybi39q6bcAnPfR9AKulFgdacx8NhtpWF-VF3vzGL98"

    /** El cliente OAuth web de Google, que pide el inicio de sesión nativo en Android. */
    val googleServerClientId: String? = "682890414578-3kmkv8rjqkl6pk9obqq6jisbpl0e1q5q.apps.googleusercontent.com"
}
