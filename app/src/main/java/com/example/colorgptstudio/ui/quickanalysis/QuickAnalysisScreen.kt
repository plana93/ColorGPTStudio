package com.example.colorgptstudio.ui.quickanalysis

import android.graphics.Bitmap
import android.net.Uri
import com.example.colorgptstudio.util.extractColorAtRatio
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.colorgptstudio.domain.model.ColorData
import com.example.colorgptstudio.domain.model.ColorPoint
import com.example.colorgptstudio.ui.components.ColorDetailSheet
import com.example.colorgptstudio.ui.components.copyPaletteToClipboard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAnalysisScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuickAnalysisViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.onImageSelected(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { _: Bitmap? -> /* TODO: salva bitmap temporaneo */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analisi Rapida") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Copia tutti i colori se ce ne sono
                    if (uiState.colorPoints.isNotEmpty()) {
                        IconButton(onClick = {
                            copyPaletteToClipboard(
                                context,
                                uiState.colorPoints.map { it.color },
                                "Analisi Rapida"
                            )
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copia tutti")
                        }
                        IconButton(onClick = viewModel::clearPoints) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Pulisci punti")
                        }
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
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.imageUri == null) {
                ImageSourceSelector(
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onCameraClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Canvas interattivo (55% schermo)
                InteractiveImageCanvas(
                    imageUri = uiState.imageUri!!,
                    colorPoints = uiState.colorPoints,
                    selectedPointId = uiState.selectedPoint?.id,
                    onTap = { xRatio, yRatio ->
                        val colorData = extractColorAtRatio(
                            context = context,
                            uri = uiState.imageUri!!,
                            xRatio = xRatio,
                            yRatio = yRatio
                        )
                        if (colorData != null) {
                            viewModel.onColorPicked(xRatio, yRatio, colorData)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                )

                // Lista punti colore scrollabile (45% schermo)
                if (uiState.colorPoints.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            items = uiState.colorPoints,
                            key = { it.id }
                        ) { point ->
                            ColorPointListItem(
                                point = point,
                                isSelected = point.id == uiState.selectedPoint?.id,
                                onClick = { viewModel.selectPoint(point) },
                                onDelete = { viewModel.removePoint(point.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // BottomSheet dettaglio colore selezionato
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (uiState.selectedPoint != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.deselectPoint() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ColorDetailSheet(
                color = uiState.selectedPoint!!.color,
                label = uiState.selectedPoint!!.label,
                note = uiState.selectedPoint!!.note,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ColorPointListItem(
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
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(composeColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${point.color.hexClean}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = point.color.rgbString,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // RAL approssimato
            Text(
                text = "RAL ${point.color.ralApprox.first}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Rimuovi",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ImageSourceSelector(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scegli un'immagine",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tocca un punto per estrarne il colore",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onGalleryClick,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galleria")
            }
            Button(
                onClick = onCameraClick,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Fotocamera")
            }
        }
    }
}

@Composable
private fun InteractiveImageCanvas(
    imageUri: Uri,
    colorPoints: List<ColorPoint>,
    selectedPointId: Long?,
    onTap: (xRatio: Float, yRatio: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Immagine da analizzare",
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { imageSize = it.size }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (imageSize != IntSize.Zero) {
                            onTap(
                                (offset.x / imageSize.width).coerceIn(0f, 1f),
                                (offset.y / imageSize.height).coerceIn(0f, 1f)
                            )
                        }
                    }
                },
            contentScale = ContentScale.Fit
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            colorPoints.forEach { point ->
                val cx = point.xRatio * size.width
                val cy = point.yRatio * size.height
                val isSelected = point.id == selectedPointId
                val pointColor = try {
                    Color(android.graphics.Color.parseColor(point.color.hex))
                } catch (e: Exception) { Color.Gray }

                if (isSelected) {
                    drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 22.dp.toPx(), center = Offset(cx, cy))
                }
                drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(cx, cy), style = Stroke(2.5.dp.toPx()))
                drawCircle(color = pointColor, radius = 11.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}
