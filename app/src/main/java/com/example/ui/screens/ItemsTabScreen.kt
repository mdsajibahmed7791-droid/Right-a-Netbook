package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Category
import com.example.model.MenuItem
import com.example.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.IllustratedEmptyStateCard
import com.example.ui.components.ItemThumbnail
import com.example.ui.components.LottieButtonSpinner
import com.example.ui.components.LottieLoadingCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryLabel
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.util.ImageUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemsTabScreen(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(Category.FOOD) }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var itemToUpdatePhoto by remember { mutableStateOf<MenuItem?>(null) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MenuItem?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val srUnit = stringResource(R.string.currency_sr)
    val errName = stringResource(R.string.error_item_name)
    val errPrice = stringResource(R.string.error_item_price)
    val camDeniedMsg = stringResource(R.string.camera_permission_denied)
    val photoUpdatedMsg = stringResource(R.string.photo_updated)
    val savedSuccessMsg = stringResource(R.string.saved_success)
    val updatedSuccessMsg = stringResource(R.string.updated_success)

    fun handlePickedUri(uri: Uri) {
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                ImageUtils.encodeUriToBase64Thumbnail(context, uri)
            }
            if (encoded != null) {
                val targetItem = itemToUpdatePhoto
                if (targetItem != null) {
                    itemToUpdatePhoto = null
                    val res = viewModel.updateMenuItemPhoto(targetItem.id, encoded)
                    if (res.isSuccess) onTaskSuccess(photoUpdatedMsg)
                    else onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed")
                } else {
                    photoBase64 = encoded
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) handlePickedUri(uri)
        else itemToUpdatePhoto = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            handlePickedUri(uri)
        } else {
            itemToUpdatePhoto = null
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            itemToUpdatePhoto = null
            onShowMessage(camDeniedMsg)
        }
    }

    fun launchCamera(forExistingItem: MenuItem? = null) {
        itemToUpdatePhoto = forExistingItem
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createTempImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.add_new_item),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Category.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(categoryLabel(cat)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = GlassSurface,
                                    labelColor = TextPrimary,
                                    iconColor = TealBright,
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = NavyDeep,
                                    selectedLeadingIconColor = NavyDeep,
                                ),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; formError = null },
                        singleLine = true,
                        label = { Text(stringResource(R.string.item_name_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it; formError = null },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(R.string.item_price_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("item_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (photoBase64 != null) {
                            Box {
                                ItemThumbnail(
                                    name = nameInput.ifBlank { "I" },
                                    photoBase64 = photoBase64,
                                    size = 52.dp,
                                )
                                IconButton(
                                    onClick = { photoBase64 = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.clear_photo),
                                        tint = ErrorCoral,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        GlassPillButton(
                            onClick = { launchCamera(null) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.camera))
                        }

                        GlassPillButton(
                            onClick = {
                                itemToUpdatePhoto = null
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.gallery))
                        }
                    }

                    if (formError != null) {
                        Text(formError.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                    }

                    TealGlowButton(
                        onClick = {
                            val cleanName = nameInput.trim()
                            val parsedPrice = priceInput.trim().toDoubleOrNull()
                            when {
                                cleanName.isEmpty() -> formError = errName
                                parsedPrice == null || parsedPrice < 0.0 -> formError = errPrice
                                else -> {
                                    isSaving = true
                                    scope.launch {
                                        val res = viewModel.addMenuItem(selectedCategory, cleanName, parsedPrice, photoBase64)
                                        isSaving = false
                                        if (res.isSuccess) {
                                            nameInput = ""
                                            priceInput = ""
                                            photoBase64 = null
                                            onTaskSuccess(savedSuccessMsg)
                                        } else {
                                            formError = res.exceptionOrNull()?.localizedMessage
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().testTag("add_item_button"),
                    ) {
                        if (isSaving) {
                            LottieButtonSpinner(size = 24.dp)
                        } else {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when (val state = itemsState) {
            is UiState.Loading -> {
                item {
                    LottieLoadingCard()
                }
            }
            is UiState.Error -> {
                item {
                    GlassCard(errorBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Text(state.message, color = ErrorCoral, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is UiState.Success -> {
                val filteredList = state.data.filter {
                    it.cat.equals(selectedCategory.key, ignoreCase = true)
                }.ifEmpty { state.data }

                if (filteredList.isEmpty()) {
                    item {
                        IllustratedEmptyStateCard(
                            imageRes = R.drawable.img_empty_items_1795044790155,
                            message = stringResource(R.string.no_items_found),
                        )
                    }
                } else {
                    items(filteredList, key = { it.id }) { item ->
                    GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ItemThumbnail(name = item.name, photoBase64 = item.photo, size = 50.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "${categoryLabel(Category.fromKey(item.cat))} • ${FormatUtils.formatCurrency(item.price, langCode, srUnit)}",
                                    style = MonoNumberStyle,
                                    color = TealBright,
                                )
                            }
                            IconButton(
                                onClick = { itemToEdit = item },
                                modifier = Modifier.size(48.dp).testTag("edit_item_button_${item.id}"),
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.edit_item),
                                    tint = TealBright,
                                )
                            }
                            IconButton(
                                onClick = { launchCamera(forExistingItem = item) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = stringResource(R.string.change_photo),
                                    tint = TextSecondary,
                                )
                            }
                            IconButton(
                                onClick = { itemToDelete = item },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = ErrorCoral,
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    itemToEdit?.let { item ->
        EditMenuItemDialog(
            item = item,
            errName = errName,
            errPrice = errPrice,
            onDismiss = { itemToEdit = null },
            onSave = { cat, newName, newPrice, clearPhoto ->
                val id = item.id
                scope.launch {
                    val res = viewModel.updateMenuItem(
                        itemId = id,
                        cat = cat,
                        name = newName,
                        price = newPrice,
                        clearPhoto = clearPhoto,
                    )
                    if (res.isSuccess) {
                        itemToEdit = null
                        onTaskSuccess(updatedSuccessMsg)
                    } else {
                        onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed")
                    }
                }
            },
        )
    }

    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            itemName = item.name,
            onConfirm = {
                val id = item.id
                itemToDelete = null
                scope.launch { viewModel.deleteMenuItem(id) }
            },
            onDismiss = { itemToDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMenuItemDialog(
    item: MenuItem,
    errName: String,
    errPrice: String,
    onDismiss: () -> Unit,
    onSave: (Category, String, Double, Boolean) -> Unit,
) {
    var editCat by remember(item.id) { mutableStateOf(Category.fromKey(item.cat)) }
    var editName by remember(item.id) { mutableStateOf(item.name) }
    var editPrice by remember(item.id) { mutableStateOf(item.price.toString().removeSuffix(".0")) }
    var clearPhoto by remember(item.id) { mutableStateOf(false) }
    var errorMsg by remember(item.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(stringResource(R.string.edit_item), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Category.entries.forEach { cat ->
                        val isSelected = editCat == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { editCat = cat },
                            label = { Text(categoryLabel(cat)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = GlassSurface,
                                labelColor = TextPrimary,
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = NavyDeep,
                            ),
                        )
                    }
                }

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it; errorMsg = null },
                    singleLine = true,
                    label = { Text(stringResource(R.string.item_name_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_item_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it; errorMsg = null },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.item_price_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_item_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )

                if (!item.photo.isNullOrBlank() && !clearPhoto) {
                    TextButton(onClick = { clearPhoto = true }) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = ErrorCoral, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.clear_photo), color = ErrorCoral)
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = editName.trim()
                    val parsed = editPrice.trim().toDoubleOrNull()
                    when {
                        clean.isEmpty() -> errorMsg = errName
                        parsed == null || parsed < 0.0 -> errorMsg = errPrice
                        else -> onSave(editCat, clean, parsed, clearPhoto)
                    }
                },
                modifier = Modifier.testTag("save_edit_item_button"),
            ) {
                Text(stringResource(R.string.save), color = TealBright, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
    )
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
