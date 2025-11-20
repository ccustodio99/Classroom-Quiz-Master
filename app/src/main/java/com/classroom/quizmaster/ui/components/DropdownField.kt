package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.classroom.quizmaster.ui.model.SelectionOptionUi

@Composable
fun DropdownField(
    label: String,
    options: List<SelectionOptionUi>,
    selectedId: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Select",
    supportingText: (SelectionOptionUi) -> String = { option -> option.supportingText }
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.label ?: "",
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                val icon = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown
                Icon(
                    imageVector = icon,
                    contentDescription = if (expanded) "$label collapse" else "$label expand"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.DropdownList
                    contentDescription = label
                }
                .clickable(enabled = enabled) { expanded = true }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        val helper = supportingText(option)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(option.label)
                            if (helper.isNotBlank()) {
                                Text(
                                    text = helper,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    }
                )
            }
        }
    }
}
