package org.ukky.notitrace.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (onClick != null) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(tag) },
            modifier = modifier,
        )
    } else {
        AssistChip(
            onClick = {},
            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
            modifier = modifier.padding(0.dp),
        )
    }
}

