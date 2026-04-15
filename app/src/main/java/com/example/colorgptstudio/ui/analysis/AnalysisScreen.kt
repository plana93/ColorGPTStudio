package com.example.colorgptstudio.ui.analysis

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.colorgptstudio.domain.model.ColorPoint
import com.example.colorgptstudio.ui.components.ColorDetailSheet
import com.example.colorgptstudio.ui.components.PaletteCard
import com.example.colorgptstudio.ui.components.TagInputField
import com.example.colorgptstudio.ui.components.copyColorToClipboard
import com.example.colorgptstudio.util.extractColorAtRatio
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
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showPointEditor by remember { mutableStateOf(false) }

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
            // ─── Canvas interattivo (occupa il 60% dello schermo) ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageFile = File(uiState.imageLocalPath)
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "Immagine da analizzare",
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { imageSize = it.size }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (imageSize != IntSize.Zero) {
                                        val xRatio = (offset.x / imageSize.width).coerceIn(0f, 1f)
                                        val yRatio = (offset.y / imageSize.height).coerceIn(0f, 1f)
                                        val colorData = extractColorAtRatio(
                                            context = context,
                                            uri = Uri.fromFile(imageFile),
                                            xRatio = xRatio,
                                            yRatio = yRatio
                                        )
                                        colorData?.let { viewModel.onColorPicked(xRatio, yRatio, it) }
                                    }
                                }
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                // Overlay punti colore
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.colorPoints.forEach { point ->
                        val cx = point.xRatio * size.width
                        val cy = point.yRatio * size.height
                        val isSelected = point.id == uiState.selectedPoint?.id

                        val colorArgb = try {
                            android.graphics.Color.parseColor(point.color.hex)
                        } catch (e: Exception) { android.graphics.Color.GRAY }
                        val pointColor = Color(colorArgb)

                        // Alone per il punto selezionato
                        if (isSelected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = 22.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                        // Bordo bianco
                        drawCircle(
                            color = Color.White,
                            radius = 14.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        // Colore estratto
                        drawCircle(
                            color = pointColor,
                            radius = 11.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Indicatore "Estrazione in corso..."
                if (uiState.isExtractingPalette) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Analisi colori in corso…",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ─── Pannello inferiore (palette + punti colore) ──────────────────
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

                // Lista punti colore
                if (uiState.colorPoints.isNotEmpty()) {
                    Text(
                        text = "Punti colore (${uiState.colorPoints.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    uiState.colorPoints.forEach { point ->
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

    // ─── Bottom sheet: editor del punto colore selezionato ────────────────────
    val availableTags by viewModel.tagRepository.allTags.collectAsState()

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
    onSave: (ColorPoint) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onNewTagAdded: (String) -> Unit = {}
) {
    var label by remember { mutableStateOf(point.label) }
    var note by remember { mutableStateOf(point.note) }
    var materialInfo by remember { mutableStateOf(point.materialInfo) }
    var selectedTags by remember { mutableStateOf(point.tags) }

    val composeColor = remember(point.color.hex) {
        try { Color(android.graphics.Color.parseColor(point.color.hex)) } catch (e: Exception) { Color.Gray }
    }

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
            placeholder = { Text("es. Luce naturale fredda, mattina") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = materialInfo,
            onValueChange = { materialInfo = it },
            label = { Text("Prodotto / Codice vernice / Fornitore") },
            placeholder = { Text("es. Sto Ivos Bianco Antico 0102-Y") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ─── Tag con autocompletamento ────────────────────────────────────────
        TagInputField(
            selectedTags = selectedTags,
            availableTags = availableTags,
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
                    onSave(
                        point.copy(
                            label = label.trim(),
                            note = note.trim(),
                            materialInfo = materialInfo.trim(),
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
