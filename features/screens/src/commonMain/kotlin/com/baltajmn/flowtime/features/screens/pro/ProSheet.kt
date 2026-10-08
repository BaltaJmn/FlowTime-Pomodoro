package com.baltajmn.flowtime.features.screens.pro

import com.baltajmn.flowtime.core.design.theme.SheetTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.features.screens.support.StoreUnavailable
import org.koin.compose.viewmodel.koinViewModel
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.features.screens.platform.proRestoreNothingText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.baltajmn.flowtime.features.screens.platform.hasFocusMode
import com.baltajmn.flowtime.features.screens.platform.rememberPurchaseHost

/**
 * La pantalla de Pro, solo cuando se pide: al tocar algo de Pro o desde Ajustes. Se cierra siempre
 * con la X o con el gesto de atrás. Sin cuentas atrás, descuentos ni nada premarcado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSheet(from: ProFeature?, onDismiss: () -> Unit, viewModel: ProViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val host = rememberPurchaseHost()
    LaunchedEffect(Unit) { viewModel.load() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ProContent(
            state = state,
            from = from,
            onBuy = { host?.let(viewModel::buy) },
            onRestore = viewModel::restore,
            onRetry = viewModel::load,
            onClose = onDismiss
        )
    }
}

@Composable
fun ProContent(
    state: ProUiState,
    from: ProFeature?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.pro_title),
                style = SheetTitle,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.close)
                )
            }
        }
        // Lo que se compra se desplaza; el precio y el botón se quedan siempre a la vista.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isPro) Thanks(onClose) else Offer(from)
        }
        if (!state.isPro) {
            Column(
                modifier = Modifier.padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Purchase(state, onBuy, onRestore, onRetry)
            }
        }
    }
}

@Composable
private fun Offer(from: ProFeature?) {
    Text(
        text = stringResource(Res.string.pro_headline),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    // Desde una función bloqueada, esa la primera. No molestar, solo donde existe.
    ProFeature.entries.filter { it != ProFeature.DND || hasFocusMode }
        .sortedByDescending { it == from }
        .forEach { FeatureRow(it) }
    Text(
        text = stringResource(Res.string.pro_future),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    HorizontalDivider()
    // Lo que sigue gratis, más suave: es la razón para fiarse de lo de arriba.
    Text(
        text = stringResource(Res.string.pro_free_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = stringResource(Res.string.pro_free_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ColumnScope.Purchase(
    state: ProUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit
) {
    HorizontalDivider()
    Text(
        text = listOfNotNull(state.product?.price, stringResource(Res.string.pro_one_time)).joinToString(
            " · "
        ),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    when {
        state.loading -> CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        state.product == null -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            StoreUnavailable(onRetry)
        }
    }
    Button(
        onClick = onBuy,
        enabled = state.product != null && !state.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mientras la tienda contesta, que se vea que algo pasa.
        if (state.busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text = stringResource(Res.string.pro_buy))
        }
    }
    val problem = when {
        state.failed -> Res.string.purchase_failed
        state.nothingToRestore -> proRestoreNothingText
        else -> null
    }
    problem?.let {
        Text(
            text = stringResource(it),
            // No haber nada que restaurar no es un error.
            color = if (state.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    TextButton(
        onClick = onRestore,
        enabled = !state.busy,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(text = stringResource(Res.string.restore_purchases))
    }
}

@Composable
private fun ColumnScope.Thanks(onClose: () -> Unit) {
    Icon(
        painter = painterResource(Res.drawable.ic_confetti),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(48.dp)
            .align(Alignment.CenterHorizontally)
    )
    Text(
        text = stringResource(Res.string.support_thanks_title),
        style = SheetTitle,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(Res.string.pro_thanks_text),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(Res.string.close))
    }
}

@Composable
private fun FeatureRow(feature: ProFeature) {
    val (icon, title, text) = when (feature) {
        ProFeature.STATS -> Triple(
            painterResource(Res.drawable.ic_stats),
            Res.string.pro_stats_title,
            Res.string.pro_stats_text
        )
        ProFeature.CSV -> Triple(rememberVectorPainter(Icons.Filled.Share), Res.string.pro_csv_title, Res.string.pro_csv_text)
        ProFeature.TAGS -> Triple(
            rememberVectorPainter(Icons.Filled.Star),
            Res.string.pro_tags_title,
            Res.string.pro_tags_text
        )
        ProFeature.TASKS -> Triple(
            painterResource(Res.drawable.ic_list),
            Res.string.pro_tasks_title,
            Res.string.pro_tasks_text
        )
        ProFeature.MIXES -> Triple(
            painterResource(Res.drawable.ic_tune),
            Res.string.pro_mixes_title,
            Res.string.pro_mixes_text
        )
        ProFeature.SOUNDS -> Triple(
            painterResource(Res.drawable.ic_music),
            Res.string.pro_sounds_title,
            Res.string.pro_sounds_text
        )
        ProFeature.THEMES -> Triple(
            rememberVectorPainter(Icons.Filled.Face),
            Res.string.pro_themes_title,
            Res.string.pro_themes_text
        )
        ProFeature.DND -> Triple(
            rememberVectorPainter(Icons.Filled.Notifications),
            Res.string.pro_dnd_title,
            Res.string.pro_dnd_text
        )
    }
    val limit = when (feature) {
        ProFeature.TAGS -> Limits.FREE_TAGS
        ProFeature.TASKS -> Limits.FREE_PENDING_TASKS
        ProFeature.MIXES -> Limits.FREE_MIXES
        else -> null
    }
    FeatureRow(
        icon,
        title,
        if (limit == null) stringResource(text) else stringResource(text, limit)
    )
}

@Composable
private fun FeatureRow(icon: Painter, title: StringResource, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
