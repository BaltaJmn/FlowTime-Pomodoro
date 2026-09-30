package com.baltajmn.flowtime.features.screens.pro

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.pro.ProFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que abre la pantalla de Pro (#57), que enseña esa función la primera. */
enum class ProFeature { STATS, CSV }

/** Cómo se ve algo de Pro: nada mientras Pro no se venda, difuminado sin Pro, o abierto. */
enum class ProAccess {
    HIDDEN,
    LOCKED,
    OPEN;

    companion object {
        fun of(isPro: Boolean, enabled: Boolean = ProFeatures.enabled) = when {
            !enabled -> HIDDEN
            isPro -> OPEN
            else -> LOCKED
        }
    }
}

/** Abre la pantalla de Pro desde cualquier pantalla. La enseña la raíz de la app (#57). */
class ProLauncher {
    private val _request = MutableStateFlow<ProFeature?>(null)
    val request: StateFlow<ProFeature?> = _request.asStateFlow()

    fun open(feature: ProFeature) {
        _request.value = feature
    }

    fun close() {
        _request.value = null
    }
}

/**
 * Lo de Pro sin Pro se ve, con los datos reales del usuario, difuminado y con un candado que abre
 * la pantalla de Pro. `blur` solo funciona desde Android 12: antes, un velo más tupido tapa lo mismo.
 */
@Composable
fun ProGate(access: ProAccess, onUnlock: () -> Unit, content: @Composable () -> Unit) {
    when (access) {
        ProAccess.HIDDEN -> Unit
        ProAccess.OPEN -> content()
        ProAccess.LOCKED -> {
            val label = stringResource(R.string.pro_unlock)
            val veil = MaterialTheme.colorScheme.surface
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClickLabel = label, role = Role.Button, onClick = onUnlock)
            ) {
                // Lo difuminado no se lee con TalkBack: solo el botón de desbloquear.
                Box(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .then(if (canBlur) Modifier.blur(16.dp) else Modifier)
                ) {
                    content()
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                if (canBlur) {
                                    listOf(veil.copy(alpha = 0.1f), veil.copy(alpha = 0.4f))
                                } else {
                                    listOf(veil.copy(alpha = 0.85f), veil.copy(alpha = 0.97f))
                                }
                            )
                        )
                )
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock_on),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
