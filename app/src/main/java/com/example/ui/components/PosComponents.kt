package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CatalogProduct
import com.example.model.Category
import com.example.model.SavedNote
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderTeal
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealTint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun categoryLabel(category: Category): String = when (category) {
    Category.FOOD -> stringResource(R.string.cat_food)
    Category.COFFEE -> stringResource(R.string.cat_coffee)
    Category.TEA -> stringResource(R.string.cat_tea)
    Category.JUICE -> stringResource(R.string.cat_juice)
    Category.COLD_DRINKS -> stringResource(R.string.cat_cold_drinks)
    Category.MOJITO -> stringResource(R.string.cat_mojito)
    Category.CAKE -> stringResource(R.string.cat_cake)
}

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.FOOD -> Icons.Outlined.Restaurant
    Category.COFFEE -> Icons.Outlined.LocalCafe
    Category.TEA -> Icons.Outlined.EmojiFoodBeverage
    Category.JUICE -> Icons.Outlined.WaterDrop
    Category.COLD_DRINKS -> Icons.Outlined.LocalDrink
    Category.MOJITO -> Icons.Outlined.LocalBar
    Category.CAKE -> Icons.Outlined.Cake
}

@Composable
fun CategoryTileGrid(
    selectedKey: String?,
    onSelectKey: (String?) -> Unit,
    includeDeliveryOnlyFilter: Boolean = false,
) {
    data class TileDef(val key: String?, val label: String, val icon: ImageVector)

    val tiles = buildList {
        add(TileDef(null, stringResource(R.string.cat_all), Icons.Outlined.Apps))
        Category.entries.forEach { cat ->
            add(TileDef(cat.key, categoryLabel(cat), categoryIcon(cat)))
        }
        if (includeDeliveryOnlyFilter) {
            add(TileDef("delivery_only", stringResource(R.string.delivery_only_badge), Icons.Outlined.LocalShipping))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(3).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTiles.forEach { tile ->
                    val isSelected = selectedKey == tile.key
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectKey(tile.key) }
                            .testTag("cat_tile_${tile.key ?: "all"}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) TealPrimary else TealTint,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) TealBright else GlassBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = tile.icon,
                                contentDescription = null,
                                tint = if (isSelected) NavyDeep else TealBright,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tile.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NavyDeep else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(3 - rowTiles.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.search_items_hint),
                color = TextMuted,
            )
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = TealPrimary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.clear_order),
                        tint = TextSecondary,
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface,
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = TealPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("item_search_input"),
    )
}

@Composable
fun ItemThumbnail(
    name: String,
    photoBase64: String?,
    size: Dp = 56.dp,
    cornerRadius: Dp = 14.dp,
) {
    val imageBitmap = remember(photoBase64) { ImageUtils.decodeBase64ToImageBitmap(photoBase64) }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius)),
        )
    } else {
        val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val bgColor = AvatarPalette[abs(name.hashCode()) % AvatarPalette.size]
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(bgColor.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
fun PosProductCard(
    product: CatalogProduct,
    addedQty: Int,
    showPrice: Boolean,
    langCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val srUnit = stringResource(R.string.currency_sr)
    GlassCard(
        highlighted = addedQty > 0,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("pos_card_${product.id}"),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ItemThumbnail(
                        name = product.name,
                        photoBase64 = product.photo,
                        size = 76.dp,
                        cornerRadius = 16.dp,
                    )
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showPrice) {
                    Text(
                        text = FormatUtils.formatCurrency(product.price, langCode, srUnit),
                        style = MonoNumberStyle,
                        color = TealBright,
                    )
                }
            }

            if (addedQty > 0) {
                Surface(
                    shape = CircleShape,
                    color = TealPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .border(1.5.dp, NavyDeep, CircleShape),
                ) {
                    Text(
                        text = FormatUtils.formatNumber(addedQty, langCode),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingOrderBar(
    totalPieces: Int,
    totalPrice: Double?,
    langCode: String,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val srUnit = stringResource(R.string.currency_sr)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CircleShape,
        color = GlassSurfaceElevated,
        border = BorderStroke(1.5.dp, GlassBorderTeal),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(
                        R.string.pieces_count,
                        FormatUtils.formatNumber(totalPieces, langCode),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (totalPrice != null) {
                    Text(
                        text = FormatUtils.formatCurrency(totalPrice, langCode, srUnit),
                        style = MonoNumberStyle,
                        color = TealBright,
                    )
                }
            }
            com.example.ui.theme.TealGlowButton(
                onClick = onReviewClick,
                modifier = Modifier.testTag("floating_review_button"),
            ) {
                Text(
                    text = stringResource(R.string.review),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemNotesBottomSheet(
    product: CatalogProduct,
    savedNotes: List<SavedNote>,
    showPrice: Boolean,
    langCode: String,
    onDismiss: () -> Unit,
    onAddSavedNote: suspend (String) -> Result<Unit>,
    onDeleteSavedNote: suspend (String) -> Result<Unit>,
    onAddToOrder: (selectedNotes: List<String>, qty: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedNotes = remember(product.id) { mutableStateListOf<String>() }
    var newNoteText by remember { mutableStateOf("") }
    var qty by remember { mutableIntStateOf(1) }
    var pendingOp by remember { mutableStateOf(false) }
    var opError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val srUnit = stringResource(R.string.currency_sr)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1D2B),
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemThumbnail(name = product.name, photoBase64 = product.photo, size = 54.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    if (showPrice) {
                        Text(
                            text = FormatUtils.formatCurrency(product.price, langCode, srUnit),
                            style = MonoNumberStyle,
                            color = TealBright,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.saved_notes),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )

            if (savedNotes.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    savedNotes.forEach { saved ->
                        val isSelected = selectedNotes.contains(saved.text)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedNotes.remove(saved.text)
                                else selectedNotes.add(saved.text)
                            },
                            label = { Text(saved.text) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.delete),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            scope.launch {
                                                selectedNotes.remove(saved.text)
                                                onDeleteSavedNote(saved.id)
                                            }
                                        },
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = GlassSurface,
                                labelColor = TextPrimary,
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = NavyDeep,
                                selectedLeadingIconColor = NavyDeep,
                                selectedTrailingIconColor = NavyDeep,
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.new_note_hint), color = TextMuted) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                com.example.ui.theme.GlassPillButton(
                    onClick = {
                        val trimmed = newNoteText.trim()
                        if (trimmed.isNotEmpty() && !pendingOp) {
                            pendingOp = true
                            opError = null
                            scope.launch {
                                val res = onAddSavedNote(trimmed)
                                pendingOp = false
                                if (res.isSuccess) {
                                    if (!selectedNotes.contains(trimmed)) selectedNotes.add(trimmed)
                                    newNoteText = ""
                                } else {
                                    opError = res.exceptionOrNull()?.localizedMessage
                                }
                            }
                        }
                    },
                    enabled = newNoteText.isNotBlank() && !pendingOp,
                ) {
                    Text(stringResource(R.string.add))
                }
            }

            if (opError != null) {
                Text(opError.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
            }

            // Quantity stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = { if (qty > 1) qty-- },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, GlassBorder, CircleShape),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "-", tint = TextPrimary)
                    }
                    Text(
                        text = FormatUtils.formatNumber(qty, langCode),
                        style = MonoNumberStyle,
                        color = TealBright,
                    )
                    IconButton(
                        onClick = { qty++ },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, GlassBorderTeal, CircleShape),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "+", tint = TealBright)
                    }
                }
            }

            com.example.ui.theme.TealGlowButton(
                onClick = {
                    val draft = newNoteText.trim()
                    val finalNotes = if (draft.isNotEmpty() && !selectedNotes.contains(draft)) {
                        selectedNotes.toList() + draft
                    } else {
                        selectedNotes.toList()
                    }
                    onAddToOrder(finalNotes, qty)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_to_order_button"),
            ) {
                Text(
                    text = stringResource(R.string.add_to_order),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102232),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(stringResource(R.string.confirm_delete_title)) },
        text = { Text(stringResource(R.string.confirm_delete_message, itemName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = ErrorCoral, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
    )
}

@Composable
fun IllustratedEmptyStateCard(
    @DrawableRes imageRes: Int,
    message: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorderTeal, RoundedCornerShape(16.dp)),
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    NavyDeep.copy(alpha = 0.28f),
                                ),
                            ),
                        ),
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
