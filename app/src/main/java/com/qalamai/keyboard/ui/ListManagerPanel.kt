package com.qalamai.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalamai.keyboard.ui.theme.KeyboardPalette

/** لوحة قائمة عامة تُستخدم للجمل المستخدمة والقاموس الشخصي. */
@Composable
fun ListManagerPanel(
    title: String,
    emptyHint: String,
    addLabel: String,
    items: List<String>,
    palette: KeyboardPalette,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .background(palette.toolbarSurface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = palette.keyText, fontSize = 13.sp)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.accentColor.copy(alpha = 0.22f))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = palette.accentColor, modifier = Modifier.size(16.dp))
                Text(addLabel, color = palette.accentColor, fontSize = 12.sp)
            }
        }
        if (items.isEmpty()) {
            Text(emptyHint, color = palette.mutedText, fontSize = 11.sp)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(items) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.keySurface)
                        .clickable { onPick(item) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        item,
                        color = palette.keyText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDelete(index) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "حذف", tint = palette.mutedText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
