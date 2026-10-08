package com.baltajmn.flowtime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.goal.Celebration
import com.baltajmn.flowtime.core.design.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * El objetivo de hoy, cumplido con la app abierta: un aviso arriba, con la racha si la hay, que no
 * tapa la sesión y se va solo. Antes era un diálogo con una frase al azar que había que cerrar, a
 * veces en mitad del descanso.
 */
@Composable
fun CelebrationBanner(celebration: Celebration?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // El texto se queda mientras sale, para que no desaparezca antes que la tarjeta.
    var text by remember { mutableStateOf("") }
    // Con TalkBack, o si el sistema pide más tiempo para leer, el que pida.
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(celebration) {
        celebration ?: return@LaunchedEffect
        text = celebration.text()
        delay(
            accessibility?.calculateRecommendedTimeoutMillis(
                SHOWN_MILLIS,
                containsIcons = true,
                containsText = true
            ) ?: SHOWN_MILLIS
        )
        onDismiss()
    }
    AnimatedVisibility(
        visible = celebration != null && text.isNotEmpty(),
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .widthIn(max = 480.dp)
                .clickable(onClickLabel = stringResource(Res.string.close), role = Role.Button, onClick = onDismiss)
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_confetti),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = stringResource(Res.string.goal_reached),
                        style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(text = text, style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    }
}

private const val SHOWN_MILLIS = 6_000L
