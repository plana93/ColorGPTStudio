package com.example.colorgptstudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Componente per la selezione e inserimento di tag.
 *
 * Layout (dall'alto verso il basso):
 * 1. Chip dei tag già selezionati (eliminabili)
 * 2. Suggerimenti rapidi per MACRO-CATEGORIA (scroll orizzontale, label categoria sopra ogni gruppo)
 * 3. Campo testo libero per cercare/aggiungere un tag custom
 * 4. Dropdown dei suggerimenti filtrati dall'input
 *
 * @param selectedTags       Tag attualmente selezionati
 * @param availableTags      Tutti i tag disponibili (flat, preset + custom)
 * @param categoryTags       Mappa categoria → lista tag preset (per i suggerimenti a categorie)
 * @param onTagsChanged      Callback quando la lista cambia
 * @param onNewTagAdded      Chiamato quando viene aggiunto un tag nuovo (per persistenza)
 * @param label              Etichetta del campo testo libero
 * @param modifier           Modifier esterno
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagInputField(
    selectedTags: List<String>,
    availableTags: List<String>,
    categoryTags: Map<String, List<String>> = emptyMap(),
    onTagsChanged: (List<String>) -> Unit,
    onNewTagAdded: (String) -> Unit = {},
    label: String = "Tag",
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Suggerimenti filtrati in base all'input
    val suggestions = remember(inputText, availableTags, selectedTags) {
        if (inputText.isBlank()) emptyList()
        else availableTags
            .filter { it.contains(inputText.trim(), ignoreCase = true) }
            .filter { it !in selectedTags }
            .take(6)
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank() || trimmed in selectedTags) return
        if (trimmed !in availableTags) onNewTagAdded(trimmed)
        onTagsChanged(selectedTags + trimmed)
        inputText = ""
        showSuggestions = false
    }

    Column(modifier = modifier) {

        // ─── 1. Chip dei tag selezionati ──────────────────────────────────────
        if (selectedTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onTagsChanged(selectedTags - tag) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Rimuovi $tag",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // ─── 2. Suggerimenti per macro-categoria ──────────────────────────────
        if (categoryTags.isNotEmpty()) {
            // Categorie da mostrare: massimo 4 per non appesantire
            val visibleCategories = categoryTags.entries
                .filter { (_, tags) -> tags.any { it !in selectedTags } }
                .take(4)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleCategories.forEach { (category, tags) ->
                    val available = tags.filter { it !in selectedTags }
                    if (available.isEmpty()) return@forEach

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            available.take(8).forEach { tag ->
                                FilterChip(
                                    selected = false,
                                    onClick = { addTag(tag) },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // ─── 3. Campo testo libero ─────────────────────────────────────────────
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                showSuggestions = it.isNotBlank()
            },
            label = { Text(label) },
            placeholder = { Text("Cerca o aggiungi tag…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused) showSuggestions = false },
            trailingIcon = {
                if (inputText.isNotBlank()) {
                    IconButton(onClick = { addTag(inputText) }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Aggiungi")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (inputText.isNotBlank()) addTag(inputText)
                else focusManager.clearFocus()
            })
        )

        // ─── 4. Dropdown suggerimenti filtrati ────────────────────────────────
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                tonalElevation = 4.dp
            ) {
                Column {
                    suggestions.forEach { suggestion ->
                        TextButton(
                            onClick = { addTag(suggestion) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+ aggiungi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
