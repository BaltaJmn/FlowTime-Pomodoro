package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import com.baltajmn.flowtime.core.design.components.selectedChipBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

/** Con qué etiqueta se guarda el trabajo: las activas y "Sin etiqueta". Se desplaza si no caben. */
@Composable
fun TagChips(tags: List<Tag>, selected: Long?, onSelect: (Long?) -> Unit) {
    if (tags.isEmpty()) return
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            FilterChip(
                selected = tags.none { it.id == selected },
                onClick = { onSelect(null) },
                label = { Text(text = stringResource(Res.string.tag_none)) },
                border = selectedChipBorder(tags.none { it.id == selected })
            )
        }
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = tag.id == selected,
                onClick = { onSelect(tag.id) },
                label = { Text(text = tag.name) },
                border = selectedChipBorder(tag.id == selected),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(TagPalette.color(tag.color, dark), CircleShape)
                    )
                }
            )
        }
    }
}
