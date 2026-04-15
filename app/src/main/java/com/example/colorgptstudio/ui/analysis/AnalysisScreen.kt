package com.example.colorgptstudio.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.colorgptstudio.domain.model.ColorPoint
import com.example.colorgptstudio.ui.components.ColorDetailSheet
import com.example.colorgptstudio.ui.components.InteractiveImageCanvas
import com.example.colorgptstudio.ui.components.PaletteCard
import com.example.colorgptstudio.ui.components.TagInputField
import com.example.colorgptstudio.ui.components.copyColorToClipboard
import com.example.colorgptstudio.util.extractColorAtRatioFromFile
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    projectId: Long,
    imageId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AnalysisViewModel = koinViewModel(parameters = { parametersOf(projectId, imageId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showPointEditor by remember { mutableStateOf(false) }
    var showLlmSheet by remember { mutableStateOf(false) }

    // Dati tag (condivisi tra la lista punti e il bottom sheet)
    val availableTags by viewModel.tagRepository.allTags.collectAsState()
    val presetCategories by viewModel.tagRepository.presetCategories.collectAsState()
    val categoryTagsMap = remember(presetCategories) {
        presetCategories.associate { it.name to it.tags }
    }

    // Mostra il bottom sheet quando viene selezionato un punto
    LaunchedEffect(uiState.selectedPoint) {
        if (uiState.selectedPoint != null) showPointEditor = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analisi Immagine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Bottone "Ask AI" — genera prompt LLM dai punti colore
                    if (uiState.colorPoints.isNotEmpty()) {
                        IconButton(onClick = { showLlmSheet = true }) {
                            Icon(Icons.Outlined.AutoFixHigh, contentDescription = "Ask AI")
                        }
                    }
                    // Bottone estrai palette
                    IconButton(
                        onClick = { viewModel.extractPalette(context) },
                        enabled = !uiState.isExtractingPalette
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Estrai palette")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Canvas interattivo (60% schermo) — lente al drag, pinch-to-zoom ─
            val imageLocalPath = uiState.imageLocalPath
            val imageFile = if (imageLocalPath.isNotBlank()) java.io.File(imageLocalPath) else null
            InteractiveImageCanvas(
                imageSource = imageFile,
                colorPoints = uiState.colorPoints,
                selectedPointId = uiState.selectedPoint?.id,
                onTap = { xRatio, yRatio ->
                    val currentPath = uiState.imageLocalPath
                    if (currentPath.isNotBlank()) {
                        val colorData = extractColorAtRatioFromFile(
                            file = java.io.File(currentPath),
                            xRatio = xRatio,
                            yRatio = yRatio
                        )
                        colorData?.let { viewModel.onColorPicked(xRatio, yRatio, it) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            )

            // Indicatore "Estrazione in corso..." sovrapposto
            if (uiState.isExtractingPalette) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    contentAlignment = Alignment.Center
                ) { /* handled inside PaletteCard */ }
            }

            // ─── Pannello inferiore (palette + punti colore) ──────────────────
            // Mappa: categoria → lista punti. "Altro" per i non categorizzati.
            val groupedPoints = remember(uiState.colorPoints, presetCategories) {
                val catTagToCategory: Map<String, String> = presetCategories.flatMap { cat ->
                    cat.tags.map { tag -> tag to cat.name }
                }.toMap()
                val grouped = mutableMapOf<String, MutableList<com.example.colorgptstudio.domain.model.ColorPoint>>()
                uiState.colorPoints.forEach { point ->
                    val category = point.tags.firstNotNullOfOrNull { catTagToCategory[it] } ?: "Altro"
                    grouped.getOrPut(category) { mutableListOf() }.add(point)
                }
                grouped.toMap()
            }
            // Header collassati per categoria
            var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Palette estratta
                PaletteCard(
                    palette = uiState.palette,
                    isLoading = uiState.isExtractingPalette,
                    onColorSelected = {}
                )

                // Lista punti colore raggruppati per categoria
                if (uiState.colorPoints.isNotEmpty()) {
                    groupedPoints.forEach { (category, points) ->
                        val isCollapsed = category in collapsedGroups
                        // Header gruppo
                        TextButton(
                            onClick = {
                                collapsedGroups = if (isCollapsed)
                                    collapsedGroups - category
                                else
                                    collapsedGroups + category
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCollapsed) "▶  $category (${points.size})" else "▼  $category",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (!isCollapsed) {
                            points.forEach { point ->
                                ColorPointRow(
                                    point = point,
                                    isSelected = point.id == uiState.selectedPoint?.id,
                                    onClick = { viewModel.selectPoint(point) },
                                    onDelete = { viewModel.deletePoint(point.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Bottom sheet: prompt LLM ─────────────────────────────────────────────
    if (showLlmSheet) {
        LlmPromptSheet(
            prompt = viewModel.generateLlmPrompt(),
            onDismiss = { showLlmSheet = false }
        )
    }

    // ─── Bottom sheet: editor del punto colore selezionato ────────────────────
    if (showPointEditor && uiState.selectedPoint != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showPointEditor = false
                viewModel.deselectPoint()
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ColorPointEditorSheet(
                point = uiState.selectedPoint!!,
                availableTags = availableTags,
                categoryTags = categoryTagsMap,
                onSave = { updated ->
                    viewModel.updatePoint(updated)
                    showPointEditor = false
                },
                onDelete = {
                    viewModel.deletePoint(uiState.selectedPoint!!.id)
                    showPointEditor = false
                },
                onDismiss = {
                    showPointEditor = false
                    viewModel.deselectPoint()
                }
            )
        }
    }

    // ─── Snackbar errore ───────────────────────────────────────────────────────
    uiState.error?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            viewModel.clearError()
        }
    }
}

@Composable
private fun ColorPointRow(
    point: ColorPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val composeColor = remember(point.color.hex) {
        try { Color(android.graphics.Color.parseColor(point.color.hex)) } catch (e: Exception) { Color.Gray }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(composeColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.color.hex.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (point.label.isNotBlank()) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = point.color.rgbString,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Elimina",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ColorPointEditorSheet(
    point: ColorPoint,
    availableTags: List<String>,
    categoryTags: Map<String, List<String>> = emptyMap(),
    onSave: (ColorPoint) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onNewTagAdded: (String) -> Unit = {}
) {
    var label by remember { mutableStateOf(point.label) }
    var note by remember { mutableStateOf(point.note) }
    // materialInfo riutilizzato come codice condizione luce: "shadow" | "mid" | "light"
    var lightCondition by remember {
        mutableStateOf(
            when (point.materialInfo) {
                "shadow" -> 0f
                "light"  -> 2f
                else     -> 1f   // "mid" o vuoto
            }
        )
    }
    var selectedTags by remember { mutableStateOf(point.tags) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Header colore con tutti i formati ────────────────────────────────
        ColorDetailSheet(
            color = point.color,
            label = label,
            note = note
        )

        HorizontalDivider()

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Etichetta") },
            placeholder = { Text("es. Parete principale") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            placeholder = { Text("es. visto di mattina, cielo coperto") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        // ─── Slider condizione luminosa ───────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Condizione luminosa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = lightCondition,
                onValueChange = { lightCondition = it },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val items = listOf("🌑 Ombra", "⛅ Medio", "☀️ Luce")
                items.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Label attivo
            val conditionLabel = when (lightCondition.toInt()) {
                0 -> "Zona in ombra"
                2 -> "Luce diretta"
                else -> "Luce diffusa / media"
            }
            Text(
                text = conditionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            )
        }

        // ─── Tag con autocompletamento ────────────────────────────────────────
        TagInputField(
            selectedTags = selectedTags,
            availableTags = availableTags,
            categoryTags = categoryTags,
            onTagsChanged = { selectedTags = it },
            onNewTagAdded = onNewTagAdded,
            label = "Tag cantiere"
        )

        // ─── Azioni ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Elimina")
            }
            Button(
                onClick = {
                    val lightStr = when (lightCondition.toInt()) {
                        0 -> "shadow"
                        2 -> "light"
                        else -> "mid"
                    }
                    onSave(
                        point.copy(
                            label = label.trim(),
                            note = note.trim(),
                            materialInfo = lightStr,
                            tags = selectedTags
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Salva")
            }
        }
    }
}

// ─── LLM Prompt Sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmPromptSheet(
    prompt: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Testo editabile — inizializzato col prompt generato, modificabile liberamente
    var editablePrompt by remember { mutableStateOf(prompt) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ask AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Edit, then copy or share",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset al prompt originale
                    if (editablePrompt != prompt) {
                        IconButton(
                            onClick = { editablePrompt = prompt },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Restore,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider()

            // ── Prompt editabile ─────────────────────────────────────────
            OutlinedTextField(
                value = editablePrompt,
                onValueChange = { editablePrompt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )

            // ── Azioni ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Copy to clipboard
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(editablePrompt))
                        copied = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (copied)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copied) "Copied!" else "Copy")
                }

                // Share / Open in another app
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, editablePrompt)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "Open in…")
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}
