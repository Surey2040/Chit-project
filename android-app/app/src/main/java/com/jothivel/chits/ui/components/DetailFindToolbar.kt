package com.jothivel.chits.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.DividerGray
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.TextGray

/** Find-in-details toolbar: search, result navigation and export; intentionally no zoom/upload. */
@Composable
fun DetailFindToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    resultCount: Int,
    activeResultIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DividerGray),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AshAnimatedSearchBar(query, onQueryChange, placeholder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    when {
                        query.isBlank() -> "Search details"
                        resultCount == 0 -> "No results"
                        else -> "${activeResultIndex + 1} / $resultCount"
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp, top = 9.dp),
                    color = if (query.isNotBlank() && resultCount == 0) MaterialTheme.colorScheme.error else TextGray,
                    fontSize = 11.sp
                )
                SmallToolbarButton(Icons.Default.KeyboardArrowUp, "Previous result", resultCount > 0, onPrevious)
                SmallToolbarButton(Icons.Default.KeyboardArrowDown, "Next result", resultCount > 0, onNext)
                SmallToolbarButton(Icons.Default.FileDownload, "Download", true, onDownload, MaroonPrimary)
            }
        }
    }
}

@Composable
private fun SmallToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = TextGray
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, description, tint = if (enabled) tint else DividerGray, modifier = Modifier.size(19.dp))
    }
}
