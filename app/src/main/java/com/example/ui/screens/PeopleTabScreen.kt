package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Person
import com.example.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.IllustratedEmptyStateCard
import com.example.ui.components.ItemThumbnail
import com.example.ui.components.LottieButtonSpinner
import com.example.ui.components.LottieLoadingCard
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun PeopleTabScreen(
    viewModel: MainViewModel,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    val peopleState by viewModel.peopleState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }

    val errName = stringResource(R.string.error_person_name)
    val errExists = stringResource(R.string.error_person_exists)
    val savedMsg = stringResource(R.string.saved_success)
    val updatedMsg = stringResource(R.string.updated_success)

    val existingPeople = (peopleState as? UiState.Success)?.data.orEmpty()

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
                        text = stringResource(R.string.add_person_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; formError = null },
                        singleLine = true,
                        label = { Text(stringResource(R.string.person_name_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("person_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    if (formError != null) {
                        Text(formError.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                    }
                    TealGlowButton(
                        onClick = {
                            val clean = nameInput.trim()
                            when {
                                clean.isEmpty() -> formError = errName
                                existingPeople.any { it.name.equals(clean, ignoreCase = true) } -> formError = errExists
                                else -> {
                                    isSaving = true
                                    scope.launch {
                                        val res = viewModel.addPerson(clean)
                                        isSaving = false
                                        if (res.isSuccess) {
                                            nameInput = ""
                                            onTaskSuccess(savedMsg)
                                        } else {
                                            formError = res.exceptionOrNull()?.localizedMessage
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().testTag("add_person_button"),
                    ) {
                        if (isSaving) {
                            LottieButtonSpinner(size = 24.dp)
                        } else {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when (val state = peopleState) {
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
                if (state.data.isEmpty()) {
                    item {
                        IllustratedEmptyStateCard(
                            imageRes = R.drawable.img_empty_people_1795044790705,
                            message = stringResource(R.string.no_people_yet),
                        )
                    }
                } else {
                    items(state.data, key = { it.id }) { person ->
                        GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ItemThumbnail(name = person.name, photoBase64 = null, size = 42.dp, cornerRadius = 21.dp)
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { personToEdit = person },
                                    modifier = Modifier.size(48.dp).testTag("edit_person_button_${person.id}"),
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.edit_person),
                                        tint = TealBright,
                                    )
                                }
                                IconButton(
                                    onClick = { personToDelete = person },
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

    personToEdit?.let { person ->
        EditPersonDialog(
            person = person,
            existingPeople = existingPeople,
            errName = errName,
            errExists = errExists,
            onDismiss = { personToEdit = null },
            onSave = { newName ->
                val id = person.id
                scope.launch {
                    val res = viewModel.updatePerson(id, newName)
                    if (res.isSuccess) {
                        personToEdit = null
                        onTaskSuccess(updatedMsg)
                    } else {
                        onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed")
                    }
                }
            },
        )
    }

    personToDelete?.let { person ->
        ConfirmDeleteDialog(
            itemName = person.name,
            onConfirm = {
                val id = person.id
                personToDelete = null
                scope.launch { viewModel.deletePerson(id) }
            },
            onDismiss = { personToDelete = null },
        )
    }
}

@Composable
private fun EditPersonDialog(
    person: Person,
    existingPeople: List<Person>,
    errName: String,
    errExists: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var editName by remember(person.id) { mutableStateOf(person.name) }
    var errorMsg by remember(person.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(stringResource(R.string.edit_person), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it; errorMsg = null },
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_name_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_person_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = editName.trim()
                    when {
                        clean.isEmpty() -> errorMsg = errName
                        existingPeople.any { it.id != person.id && it.name.equals(clean, ignoreCase = true) } ->
                            errorMsg = errExists
                        else -> onSave(clean)
                    }
                },
                modifier = Modifier.testTag("save_edit_person_button"),
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
