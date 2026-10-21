package com.baltajmn.flowtime.features.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.app_search_name
import com.baltajmn.flowtime.core.design.resources.ic_fire
import com.baltajmn.flowtime.core.design.resources.ic_flowtime
import com.baltajmn.flowtime.core.design.resources.share
import com.baltajmn.flowtime.core.design.resources.share_focus
import com.baltajmn.flowtime.core.design.resources.stats_sessions
import com.baltajmn.flowtime.core.design.resources.streak_days
import com.baltajmn.flowtime.features.screens.platform.Formats
import com.baltajmn.flowtime.features.screens.platform.rememberShareImage
import com.baltajmn.flowtime.features.screens.platform.toPng
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * La vista previa de las tarjetas con "Compartir": la imagen sale de la propia tarjeta dibujada, sin
 * una copia aparte que mantener. Con varias, se pasan de lado y se comparte la que se ve. El pie de
 * cada tarjeta lleva la búsqueda completa de la app en el idioma del móvil ([Res.string.app_search_name],
 * la misma que el título de la ficha), y el texto que acompaña a la imagen es esa misma búsqueda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCardSheet(onDismiss: () -> Unit, cards: List<@Composable () -> Unit>) {
    val layers = List(cards.size) { rememberGraphicsLayer() }
    val pager = rememberPagerState { cards.size }
    val scope = rememberCoroutineScope()
    val share = rememberShareImage()
    val text = stringResource(Res.string.app_search_name)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), pageSpacing = 16.dp) { page ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // El recorte es de fuera: lo que se graba en la capa va a esquinas rectas y sin transparencia.
                    Column(
                        modifier = Modifier
                            .widthIn(max = CARD_WIDTH)
                            .clip(RoundedCornerShape(24.dp))
                            .drawWithContent {
                                layers[page].record { this@drawWithContent.drawContent() }
                                drawLayer(layers[page])
                            }
                    ) {
                        // Con la letra grande del sistema, la tarjeta se desbordaría: sale igual en todos los móviles.
                        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                            cards[page]()
                        }
                    }
                }
            }
            if (cards.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(cards.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pager.currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                }
            }
            Button(
                onClick = { scope.launch { share(layers[pager.currentPage].toImageBitmap().toPng(), text) } }
            ) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(Res.string.share))
            }
        }
    }
}

private val CARD_WIDTH = 360.dp

/** El marco común de las tarjetas, 4:5: marca arriba, el contenido en medio y la búsqueda de la app abajo. */
@Composable
internal fun ShareCardFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_flowtime),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "FlowTime",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            content = content
        )
        Text(
            text = stringResource(Res.string.app_search_name),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 15.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center
        )
    }
}

/** Una cifra grande con su rótulo debajo. */
@Composable
internal fun BigStat(value: String, label: String, size: Int = 28) {
    Column {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            fontSize = 14.sp
        )
    }
}

/** El día: minutos de foco, sesiones y, si se pasa [streak], la racha. */
@Composable
fun DayShareCard(day: LocalDate, seconds: Long, sessions: Int, streak: Int) {
    ShareCardFrame {
        Text(
            text = Formats.mediumDate(day),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            fontSize = 16.sp
        )
        BigStat(
            value = (seconds / 60).formatMinutesStudying(),
            label = stringResource(Res.string.share_focus),
            size = 48
        )
        Spacer(modifier = Modifier.size(32.dp))
        BigStat(value = sessions.toString(), label = stringResource(Res.string.stats_sessions))
        if (streak > 0) {
            Spacer(modifier = Modifier.size(24.dp))
            StreakLine(streak)
        }
    }
}

/** "5 días seguidos" con la llama. */
@Composable
internal fun StreakLine(days: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            painter = painterResource(Res.drawable.ic_fire),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = pluralStringResource(Res.plurals.streak_days, days, days),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp,
            fontWeight = FontWeight.W600
        )
    }
}
