package com.baltajmn.flowtime.data.backup

/** Un fichero que el usuario ha elegido con el selector del sistema: un Uri en Android, una URL en iPhone. */
expect class PickedFile

/** Leer y escribir el fichero que el usuario ha elegido con el selector de archivos del sistema. */
expect class DocumentFiles {
    /** Todo el texto. Lanza si no se puede leer o si pasa de [MAX_BYTES]. */
    suspend fun read(file: PickedFile): String

    suspend fun write(file: PickedFile, text: String)
}

// Diez años de sesiones a diario ocupan unos pocos megas: un vídeo elegido por error no debe acabar
// entero en memoria.
internal const val MAX_BYTES = 32 * 1024 * 1024
