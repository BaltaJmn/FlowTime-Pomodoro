package com.baltajmn.flowtime.features.screens.support

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.data.pro.CatalogItem
import org.koin.compose.viewmodel.koinViewModel
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(onDismiss: () -> Unit, viewModel: SupportViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val close = {
        viewModel.onClosed()
        onDismiss()
    }
    // Cada vez que se abre: si la tienda no respondió la última vez, puede que ahora sí.
    LaunchedEffect(Unit) { viewModel.load() }

    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        SupportContent(
            state = state,
            onTip = { item -> activity?.let { viewModel.tip(it, item) } },
            onRetry = viewModel::load,
            onUseTheme = {
                viewModel.useSupporterTheme()
                close()
            },
            onClose = close
        )
    }
}

@Composable
fun SupportContent(
    state: SupportState,
    onTip: (CatalogItem) -> Unit,
    onRetry: () -> Unit,
    onUseTheme: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.thanked) {
            Icon(
                painter = painterResource(Res.drawable.ic_confetti),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(Res.string.support_thanks_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = stringResource(Res.string.support_thanks_text), textAlign = TextAlign.Center)
            Button(onClick = onUseTheme, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.support_use_theme))
            }
            TextButton(onClick = onClose) { Text(text = stringResource(Res.string.close)) }
            return@Column
        }

        Text(
            text = stringResource(Res.string.support_sheet_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = stringResource(Res.string.support_sheet_text), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        when {
            state.loading -> CircularProgressIndicator()
            state.tips.isEmpty() -> StoreUnavailable(onRetry)
            else -> state.tips.forEach { item ->
                OutlinedButton(
                    onClick = { onTip(item) },
                    enabled = !state.buying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(tipLabel(item.id)))
                        Text(text = item.price)
                    }
                }
            }
        }
        if (state.failed) {
            Text(
                text = stringResource(Res.string.purchase_failed),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** La tienda no responde: sin conexión, o un móvil sin Google Play. */
@Composable
fun StoreUnavailable(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.alert_google_play),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Text(text = stringResource(Res.string.alert_google_play_desc), textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text(text = stringResource(Res.string.retry)) }
    }
}

private fun tipLabel(id: String) = when (id) {
    "tip_small" -> Res.string.tip_small
    "tip_medium" -> Res.string.tip_medium
    else -> Res.string.tip_large
}
