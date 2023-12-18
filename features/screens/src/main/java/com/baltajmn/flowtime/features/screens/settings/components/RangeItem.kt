package com.baltajmn.flowtime.features.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.isNumericOrBlank
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.core.persistence.model.RangeModel

@Composable
fun RangeItem(
    index: Int,
    range: RangeModel,
    previousRange: RangeModel,
    onValueChanged: (Int, RangeModel) -> Unit,
    onDeleteClicked: (Int) -> Unit
) {
    var time by remember(range.endRange) { mutableStateOf(range.endRange.toString()) }
    var rest by remember(range.rest) { mutableStateOf(range.rest.toString()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .weight(0.15f)
                .padding(top = 12.dp),
            text = LocalContext.current.getString(R.string.flow_time_settings_range, index),
            style = Title.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        )

        TextField(
            value = time,
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.tertiary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary
            ),
            onValueChange = {
                if (it.isNumericOrBlank()) {
                    time = it
                    if (it.isNotBlank()) {
                        onValueChanged.invoke(
                            index,
                            RangeModel(
                                totalRange = previousRange.totalRange + it.toInt(),
                                endRange = it.toInt(),
                                rest = range.rest
                            )
                        )
                    }
                }
            },
            label = {
                Text(
                    text = LocalContext.current.getString(
                        R.string.flow_time_settings_between,
                        previousRange.totalRange,
                        if (time.isBlank()) {
                            ""
                        } else {
                            (previousRange.totalRange + time.toInt()).toString()
                        }
                    ),
                    style = Title.copy(fontSize = 10.sp),
                    maxLines = 1
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(0.45f)
                .onFocusChanged { focus ->
                    if (!focus.hasFocus && time.isBlank()) {
                        time = 1.toString()
                    }
                }
        )

        TextField(
            value = rest,
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.tertiary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary
            ),
            onValueChange = {
                if (it.isNumericOrBlank()) {
                    rest = it
                    if (it.isNotBlank()) {
                        onValueChanged.invoke(
                            index,
                            RangeModel(
                                totalRange = range.totalRange,
                                endRange = range.endRange,
                                rest = it.toInt()
                            )
                        )
                    }
                }
            },
            label = {
                Text(
                    text = LocalContext.current.getString(R.string.flow_time_settings_rest),
                    style = Title.copy(fontSize = 10.sp),
                    maxLines = 1
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(0.3f)
                .onFocusChanged { focus ->
                    if (!focus.hasFocus && rest.isBlank()) {
                        rest = 1.toString()
                    }
                }
        )

        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "delete",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .weight(0.1f)
                .clickable { onDeleteClicked.invoke(index) }
        )
    }
}