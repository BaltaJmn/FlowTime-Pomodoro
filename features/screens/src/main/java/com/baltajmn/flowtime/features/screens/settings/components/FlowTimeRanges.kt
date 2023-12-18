package com.baltajmn.flowtime.features.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.persistence.model.RangeModel

context(LazyListScope)
fun FlowTimeRanges(
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
        Button(onClick = { onAddRangeClicked.invoke() }) {
            Text(
                text = LocalContext.current.getString(R.string.flow_time_add_range),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}