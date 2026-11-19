package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CatalogCategory
import com.example.domain.model.CatalogEntry
import com.example.ui.components.OledCard
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CatalogScreen(
    entries: List<CatalogEntry>,
    selectedCategory: CatalogCategory,
    existingHashes: Set<String>,
    onSelectCategory: (CatalogCategory) -> Unit,
    onAddCatalogEntry: (CatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleEntries = if (selectedCategory == CatalogCategory.ALL) {
        entries
    } else {
        entries.filter { it.category == selectedCategory }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
                Text(
                    text = "Демо-каталог",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Проверенные легальные Open-Source и CC раздачи для теста P2P",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CatalogCategory.entries) { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(category) },
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("catalog_category_${category.name.lowercase()}"),
                        label = { Text(category.label) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CarbonSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.14f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = NeonCyan.copy(alpha = 0.55f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    val alreadyAdded = entry.infoHash.lowercase() in existingHashes
                    CatalogItemCard(
                        entry = entry,
                        alreadyAdded = alreadyAdded,
                        onDownload = { onAddCatalogEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogItemCard(
    entry: CatalogEntry,
    alreadyAdded: Boolean,
    onDownload: () -> Unit
) {
    OledCard(
        borderColor = if (alreadyAdded) NeonEmerald.copy(alpha = 0.4f) else HairlineBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.subtitle,
                    style = MonoMetricSmall,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Text(
                    text = entry.sizeLabel,
                    style = MonoMetricSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${entry.license} • ${entry.trackerCount} трек.",
                    style = MonoMetricSmall,
                    color = TextMuted
                )
            }

            Button(
                onClick = onDownload,
                enabled = !alreadyAdded,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("catalog_download_${entry.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = OledBlack,
                    disabledContainerColor = ElevatedSlate,
                    disabledContentColor = NeonEmerald
                )
            ) {
                Icon(
                    imageVector = if (alreadyAdded) Icons.Outlined.CheckCircle else Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (alreadyAdded) "В очереди" else "Скачать",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
