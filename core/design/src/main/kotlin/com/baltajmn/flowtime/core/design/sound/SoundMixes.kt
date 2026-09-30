package com.baltajmn.flowtime.core.design.sound

import androidx.annotation.StringRes
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SOUND_MIXES
import com.baltajmn.flowtime.core.persistence.sharedpreferences.getObject
import com.baltajmn.flowtime.core.persistence.sharedpreferences.setObject
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** Una mezcla: el volumen de cada sonido que suena. Las de ejemplo llevan su nombre traducido en [label]. */
data class SoundMix(
    val id: String,
    val name: String,
    val volumes: Map<PlayerType, Float>,
    @StringRes val label: Int = 0
) {
    val builtIn: Boolean get() = label != 0
}

/**
 * Las mezclas guardadas (#44), en un solo ajuste. Las de ejemplo vienen con la app: no cuentan para
 * el límite de la versión gratis y no se borran, se ocultan.
 */
class SoundMixes(private val prefs: DataProvider, private val newId: () -> String = { UUID.randomUUID().toString() }) {

    private var stored = prefs.getObject<StoredMixes>(SOUND_MIXES) ?: StoredMixes()
    private val _all = MutableStateFlow(visible())
    val all: StateFlow<List<SoundMix>> = _all.asStateFlow()

    /** Las guardadas por el usuario: las que cuentan para el límite. */
    val saved: Int get() = stored.mixes.size

    fun save(name: String, volumes: Map<PlayerType, Float>) = update {
        copy(mixes = mixes + StoredMix(newId(), name.trim(), volumes.mapKeys { it.key.name }))
    }

    fun rename(id: String, name: String) = update {
        copy(mixes = mixes.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun delete(mix: SoundMix) = update {
        if (mix.builtIn) copy(hidden = hidden + mix.id) else copy(mixes = mixes.filterNot { it.id == mix.id })
    }

    /** Las de una copia de seguridad (#35): se suman las que no estén ya, por nombre. */
    fun restore(mixes: List<Pair<String, Map<String, Float>>>) = update {
        val names = this.mixes.map { it.name }.toSet()
        copy(this.mixes + mixes.filter { it.first !in names }.map { (name, volumes) -> StoredMix(newId(), name, volumes) })
    }

    /** Para la copia de seguridad: las del usuario, con los sonidos por su nombre. */
    fun export(): List<Pair<String, Map<String, Float>>> = stored.mixes.map { it.name to it.volumes }

    private fun update(change: StoredMixes.() -> StoredMixes) {
        stored = stored.change()
        prefs.setObject(SOUND_MIXES, stored)
        _all.value = visible()
    }

    // Un sonido que ya no existe se salta; si no queda ninguno, la mezcla no se enseña.
    private fun visible() = BUILT_IN.filterNot { it.id in stored.hidden } + stored.mixes.mapNotNull { mix ->
        val volumes = mix.volumes.mapNotNull { (name, volume) ->
            PlayerType.entries.firstOrNull { it.name == name }?.let { it to volume.coerceIn(0f, 1f) }
        }.toMap()
        volumes.takeIf { it.isNotEmpty() }?.let { SoundMix(mix.id, mix.name, it) }
    }

    // Como se guardan: por el nombre del sonido, para no depender del orden de PlayerType.
    @Serializable
    private data class StoredMixes(val mixes: List<StoredMix> = emptyList(), val hidden: List<String> = emptyList())

    @Serializable
    private data class StoredMix(val id: String = "", val name: String = "", val volumes: Map<String, Float> = emptyMap())

    companion object {
        val BUILT_IN = listOf(
            SoundMix("rainy_cafe", "", mapOf(PlayerType.RAIN to 0.5f, PlayerType.COFFEE_HOUSE to 0.6f), R.string.mix_rainy_cafe),
            SoundMix("distant_storm", "", mapOf(PlayerType.THUNDER to 0.45f, PlayerType.WIND to 0.3f), R.string.mix_distant_storm),
            SoundMix("soft_brown", "", mapOf(PlayerType.BROWN to 0.35f), R.string.mix_soft_brown)
        )
    }
}
