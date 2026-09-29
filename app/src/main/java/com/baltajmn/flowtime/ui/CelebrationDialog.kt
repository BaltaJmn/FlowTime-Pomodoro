package com.baltajmn.flowtime.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.features.screens.settings.enum.MotivationalPhrases
import com.baltajmn.flowtime.goal.Celebration

/** El objetivo de hoy, cumplido con la app abierta: una frase motivadora y la racha. */
@Composable
fun CelebrationDialog(celebration: Celebration, onDismiss: () -> Unit) {
    val phrase = remember(celebration) { MotivationalPhrases.entries.random() }
    val text = celebration.text(LocalContext.current.resources)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_confetti),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(text = stringResource(R.string.goal_reached), textAlign = TextAlign.Center) },
        text = {
            Text(
                text = stringResource(phrase.resourceId) + "\n\n" + text,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.dialog_confirm)) }
        }
    )
}
