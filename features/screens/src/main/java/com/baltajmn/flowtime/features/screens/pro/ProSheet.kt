package com.baltajmn.flowtime.features.screens.pro

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.features.screens.support.StoreUnavailable
import org.koin.androidx.compose.koinViewModel

/**
 * La pantalla de Pro, solo cuando se pide: al tocar algo de Pro o desde Ajustes. Se cierra siempre
 * con la X o con el gesto de atrás. Sin cuentas atrás, descuentos ni nada premarcado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSheet(from: ProFeature?, onDismiss: () -> Unit, viewModel: ProViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LaunchedEffect(Unit) { viewModel.load() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ProContent(
            state = state,
            from = from,
            onBuy = { activity?.let(viewModel::buy) },
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
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.pro_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
        Column(
            modifier = Modifier.padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isPro) Thanks(onClose) else Offer(state, from, onBuy, onRestore, onRetry)
        }
    }
}

@Composable
private fun ColumnScope.Offer(
    state: ProUiState,
    from: ProFeature?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit
) {
    Text(
        text = stringResource(R.string.pro_headline),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    // Desde una función bloqueada, esa la primera.
    ProFeature.entries.sortedByDescending { it == from }.forEach { FeatureRow(it) }
    Text(
        text = stringResource(R.string.pro_future),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    HorizontalDivider()
    // Lo que sigue gratis, más suave: es la razón para fiarse de lo de arriba.
    Text(
        text = stringResource(R.string.pro_free_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = stringResource(R.string.pro_free_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = listOfNotNull(state.product?.price, stringResource(R.string.pro_one_time)).joinToString(
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
        Text(text = stringResource(R.string.pro_buy))
    }
    val problem = when {
        state.failed -> R.string.purchase_failed
        state.nothingToRestore -> R.string.pro_restore_nothing
        else -> null
    }
    problem?.let {
        Text(
            text = stringResource(it),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    TextButton(
        onClick = onRestore,
        enabled = !state.busy,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(text = stringResource(R.string.restore_purchases))
    }
}

@Composable
private fun ColumnScope.Thanks(onClose: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.ic_confetti),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(48.dp)
            .align(Alignment.CenterHorizontally)
    )
    Text(
        text = stringResource(R.string.support_thanks_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(R.string.pro_thanks_text),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.close))
    }
}

@Composable
private fun FeatureRow(feature: ProFeature) {
    val (icon, title, text) = when (feature) {
        ProFeature.STATS -> Triple(
            Icons.Filled.DateRange,
            R.string.pro_stats_title,
            R.string.pro_stats_text
        )
        ProFeature.CSV -> Triple(Icons.Filled.Share, R.string.pro_csv_title, R.string.pro_csv_text)
        ProFeature.TAGS -> Triple(
            Icons.Filled.Star,
            R.string.pro_tags_title,
            R.string.pro_tags_text
        )
        ProFeature.TASKS -> Triple(
            Icons.Filled.CheckCircle,
            R.string.pro_tasks_title,
            R.string.pro_tasks_text
        )
        ProFeature.MIXES -> Triple(
            Icons.Filled.Favorite,
            R.string.pro_mixes_title,
            R.string.pro_mixes_text
        )
        ProFeature.SOUNDS -> Triple(
            Icons.Filled.PlayArrow,
            R.string.pro_sounds_title,
            R.string.pro_sounds_text
        )
        ProFeature.THEMES -> Triple(
            Icons.Filled.Face,
            R.string.pro_themes_title,
            R.string.pro_themes_text
        )
        ProFeature.DND -> Triple(
            Icons.Filled.Notifications,
            R.string.pro_dnd_title,
            R.string.pro_dnd_text
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
private fun FeatureRow(icon: ImageVector, @StringRes title: Int, text: String) {
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
                imageVector = icon,
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
