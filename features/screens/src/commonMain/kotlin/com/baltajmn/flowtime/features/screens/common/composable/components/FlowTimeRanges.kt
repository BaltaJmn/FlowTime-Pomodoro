package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.flowTimeRanges(
    ranges: MutableList<RangeModel>,
    onValueChanged: (Int, RangeModel) -> Unit,
    onDeleteClicked: (Int) -> Unit,
    onAddRangeClicked: () -> Unit
) {
    itemsIndexed(ranges) { index, range ->
        val lastIndex = ranges.size - 1
        when (index) {
            0 -> {
                FirstRangeItem(index = index, range = range, onValueChanged = onValueChanged)
            }

            lastIndex -> {
                LastRangeItem(
                    index = index,
                    previousRange = ranges[index - 1],
                    range = range,
                    onValueChanged = onValueChanged
                )
            }

            else -> {
                RangeItem(
                    index = index,
                    range = range,
                    previousRange = ranges[index - 1],
                    onValueChanged = onValueChanged,
                    onDeleteClicked = onDeleteClicked
                )
            }
        }
    }
    item { ButtonAddRange(onAddRangeClicked = onAddRangeClicked) }
}

@Composable
fun ButtonAddRange(onAddRangeClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Secundario: el botón lleno de la hoja es "Guardar".
        TextButton(onClick = onAddRangeClicked) {
            Text(text = stringResource(Res.string.flow_time_add_range))
        }
    }
}