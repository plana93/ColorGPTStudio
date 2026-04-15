package com.example.colorgptstudio.ui.project

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.colorgptstudio.domain.model.ProjectImage
import com.example.colorgptstudio.ui.components.PaletteCard
import com.example.colorgptstudio.ui.components.copyPaletteToClipboard
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onImageClick: (projectId: Long, imageId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectDetailViewModel = koinViewModel(parameters = { parametersOf(projectId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEditNotes by remember { mutableStateOf(false) }

    // Launcher galleria per aggiungere immagini
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addImage(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.project?.name ?: "Progetto",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Copia palette globale
                    if (uiState.globalPalette != null) {
                        IconButton(onClick = {
                            copyPaletteToClipboard(
                                context,
                                uiState.globalPalette!!.colors,
                                uiState.project?.name ?: "Progetto"
                            )
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copia palette")
                        }
                    }
                    IconButton(onClick = { showEditNotes = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifica note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { galleryLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Aggiungi immagine")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.project == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Progetto non trovato", color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                val project = uiState.project!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ─── Note progetto ────────────────────────────────────────
                    if (project.description.isNotBlank()) {
                        item {
                            ProjectNotesCard(
                                description = project.description,
                                onEditClick = { showEditNotes = true }
                            )
                        }
                    }

                    // ─── Palette globale progetto ─────────────────────────────
                    item {
                        PaletteCard(
                            palette = uiState.globalPalette,
                            isLoading = uiState.isLoading
                        )
                    }

                    // ─── Griglia immagini ─────────────────────────────────────
                    item {
                        Text(
                            text = "Immagini (${project.images.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (project.images.isEmpty()) {
                        item {
                            EmptyImagesPlaceholder(
                                onAddClick = { galleryLauncher.launch("image/*") }
                            )
                        }
                    } else {
                        items(project.images, key = { it.id }) { image ->
                            ImageCard(
                                image = image,
                                onClick = { onImageClick(project.id, image.id) },
                                onDelete = { viewModel.deleteImage(image.id) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ─── Dialog modifica note progetto ────────────────────────────────────────
    if (showEditNotes) {
        EditNotesDialog(
            initialText = uiState.project?.description ?: "",
            onConfirm = { text ->
                viewModel.updateProjectNotes(text)
                showEditNotes = false
            },
            onDismiss = { showEditNotes = false }
        )
    }
}

@Composable
private fun ProjectNotesCard(description: String, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.StickyNote2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Modifica",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImageCard(
    image: ProjectImage,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Thumbnail
            val file = File(image.localPath)
            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = image.label.ifBlank { image.fileName },
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (image.label.isNotBlank()) {
                        Text(
                            text = image.label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (image.notes.isNotBlank()) {
                        Text(
                            text = image.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini palette se disponibile
                    if (image.palette != null && image.palette.colors.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .height(16.dp)
                                .width(80.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            image.palette.colors.take(5).forEach { colorData ->
                                val c = try {
                                    Color(android.graphics.Color.parseColor(colorData.hex))
                                } catch (e: Exception) { Color.Gray }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(c))
                            }
                        }
                    } else {
                        Text(
                            text = "${image.colorPoints.size} punti",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
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
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina immagine") },
            text = { Text("Questa azione è irreversibile. L'immagine e tutti i punti colore associati verranno eliminati.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun EmptyImagesPlaceholder(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Nessuna immagine nel progetto",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onAddClick) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Aggiungi immagine")
        }
    }
}

@Composable
private fun EditNotesDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note progetto") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note") },
                placeholder = { Text("es. Stile rustico, cliente preferisce toni caldi…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.trim()) }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
