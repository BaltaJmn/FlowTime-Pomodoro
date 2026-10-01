package com.baltajmn.flowtime.data.backup

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL

actual class PickedFile(val url: NSURL)

/**
 * En el iPhone, los ficheros que da el selector de documentos son de fuera de la app: hay que pedir
 * permiso para tocarlos ([NSURL.startAccessingSecurityScopedResource]) y devolverlo al terminar.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
actual class DocumentFiles {

    actual suspend fun read(file: PickedFile): String = withContext(Dispatchers.IO) {
        file.scoped {
            val size = NSFileManager.defaultManager.attributesOfItemAtPath(file.url.path.orEmpty(), null)
                ?.get(NSFileSize) as? NSNumber
            val bytes = size?.longLongValue ?: 0L
            check(bytes <= MAX_BYTES) { "${file.url} pasa de $MAX_BYTES bytes" }
            NSString.stringWithContentsOfURL(file.url, NSUTF8StringEncoding, null)
                ?: error("No se puede leer ${file.url}")
        }
    }

    actual suspend fun write(file: PickedFile, text: String) = withContext(Dispatchers.IO) {
        file.scoped {
            val written = NSString.create(string = text).writeToURL(file.url, true, NSUTF8StringEncoding, null)
            check(written) { "No se puede escribir ${file.url}" }
        }
    }

    private inline fun <T> PickedFile.scoped(block: () -> T): T {
        val granted = url.startAccessingSecurityScopedResource()
        return try {
            block()
        } finally {
            if (granted) url.stopAccessingSecurityScopedResource()
        }
    }
}
