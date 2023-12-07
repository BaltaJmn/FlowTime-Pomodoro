package com.baltajmn.flowtime.features.screens.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.isNumeric
import com.baltajmn.flowtime.core.common.extensions.isNumericOrBlank
import com.baltajmn.flowtime.core.design.theme.Blue
import com.baltajmn.flowtime.core.design.theme.DarkBlue
import com.baltajmn.flowtime.core.design.theme.LightBlue
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.core.persistence.model.RangeModel

@Composable
fun LastRangeItem(
    index: Int,
    range: RangeModel,
    previousRange: RangeModel,
    onValueChanged: (Int, RangeModel) -> Unit
) {

    var rest by remember(range.rest) { mutableStateOf(range.rest.toString()) }

    Row {
        TextField(
            value = rest,
            colors = TextFieldDefaults.colors(
                focusedTextColor = DarkBlue,
                unfocusedTextColor = Blue,
                focusedContainerColor = LightBlue,
                unfocusedContainerColor = LightBlue,
                focusedIndicatorColor = DarkBlue,
                unfocusedIndicatorColor = Blue,
                focusedLabelColor = DarkBlue,
                unfocusedLabelColor = Blue
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
                                rest = it.toInt(),
                            )
                        )
                    }
                }
            },
            label = {
                Text(
                    text = "and after ${previousRange.totalRange} minutes, I will rest...",
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
                .onFocusChanged { focus ->
                    if (!focus.hasFocus && rest.isBlank()) {
                        rest = 1.toString()
                    }
                }
        )
    }
}