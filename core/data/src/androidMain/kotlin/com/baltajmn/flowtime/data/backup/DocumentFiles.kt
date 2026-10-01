package com.baltajmn.flowtime.data.backup

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PickedFile(val uri: Uri)

actual class DocumentFiles(private val context: Context) {

    actual suspend fun read(file: PickedFile): String = withContext(Dispatchers.IO) {
        val uri = file.uri
        val stream = context.contentResolver.openInputStream(uri) ?: error("No se puede leer $uri")
        val bytes = stream.use { it.readAtMost(MAX_BYTES) } ?: error("$uri pasa de $MAX_BYTES bytes")
        bytes.toString(Charsets.UTF_8)
    }

    // "wt": truncar. Con "w", sobrescribir un fichero más largo dejaba restos del anterior al final.
    actual suspend fun write(file: PickedFile, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("No se puede escribir ${file.uri}")
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
        const val BUFFER_BYTES = 64 * 1024
    }
}
