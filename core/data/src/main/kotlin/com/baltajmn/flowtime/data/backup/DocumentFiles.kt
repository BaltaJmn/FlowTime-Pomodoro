package com.baltajmn.flowtime.data.backup

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Leer y escribir el fichero que el usuario ha elegido con el selector de archivos del sistema. */
class DocumentFiles(private val context: Context) {

    // Con un tope: un vídeo elegido por error no debe acabar entero en memoria.
    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("No se puede leer $uri")
        val bytes = stream.use { it.readAtMost(MAX_BYTES) } ?: error("$uri pasa de $MAX_BYTES bytes")
        bytes.toString(Charsets.UTF_8)
    }

    // "wt": truncar. Con "w", sobrescribir un fichero más largo dejaba restos del anterior al final.
    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("No se puede escribir $uri")
    }

    /** Todo el contenido, o null si pasa de [limit] bytes. */
    private fun InputStream.readAtMost(limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = read(buffer)
            if (read < 0) return out.toByteArray()
            out.write(buffer, 0, read)
            if (out.size() > limit) return null
        }
    }

    private companion object {
        // Diez años de sesiones a diario ocupan unos pocos megas.
        const val MAX_BYTES = 32 * 1024 * 1024
        const val BUFFER_BYTES = 64 * 1024
    }
}
