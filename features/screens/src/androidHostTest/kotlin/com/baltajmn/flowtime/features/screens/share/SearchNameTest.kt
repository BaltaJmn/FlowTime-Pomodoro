package com.baltajmn.flowtime.features.screens.share

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La firma de las tarjetas para compartir es la búsqueda completa de la app: la misma que el título de
 * la ficha de cada idioma. Si se cambia una y no la otra, esto avisa.
 */
class SearchNameTest {

    private val resources = File("../../core/design/src/commonMain/composeResources")
    private val listings = File("../../store/listings")

    @Test
    fun `app_search_name es el titulo de la ficha en cada idioma`() {
        // La carpeta de recursos del idioma -> la de la ficha.
        val folders = mapOf(
            "values" to "en-US", "values-de" to "de-DE", "values-es" to "es-ES", "values-fr" to "fr-FR",
            "values-hi" to "hi-IN", "values-id" to "id", "values-in" to "id", "values-it" to "it-IT",
            "values-ja" to "ja-JP", "values-ko" to "ko-KR", "values-nl" to "nl-NL", "values-pl" to "pl-PL",
            "values-pt" to "pt-BR", "values-ru" to "ru-RU", "values-tr" to "tr-TR"
        )
        assertTrue("sin la carpeta de recursos", resources.isDirectory)
        folders.forEach { (values, listing) ->
            val xml = File(resources, "$values/strings.xml").readText()
            val name = Regex("""<string name="app_search_name">(.*?)</string>""").find(xml)?.groupValues?.get(1)
            val title = File(listings, "$listing/title.txt").readText().trim()
            assertEquals("$values frente a store/listings/$listing/title.txt", title, name)
        }
    }
}
