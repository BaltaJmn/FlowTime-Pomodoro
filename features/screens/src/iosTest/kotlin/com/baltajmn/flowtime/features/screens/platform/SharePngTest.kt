package com.baltajmn.flowtime.features.screens.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import platform.UIKit.UIImage

/** Lo que se comparte en el iPhone: el PNG de la tarjeta dibujada, y que UIKit lo lea como imagen. */
class SharePngTest {

    @Test
    fun laImagenDibujadaSeCodificaComoPngYUIKitLaLee() {
        val bitmap = ImageBitmap(64, 48)
        Canvas(bitmap).drawRect(Rect(0f, 0f, 64f, 48f), Paint().apply { color = Color.Red })

        val png = bitmap.toPng()

        // La firma de un PNG: 0x89 "PNG".
        assertEquals(listOf<Byte>(0x89.toByte(), 0x50, 0x4E, 0x47), png.take(4))
        val image = UIImage.imageWithData(png.toNSData())
        assertNotNull(image)
    }
}
