package com.example.colorgptstudio.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.colorgptstudio.domain.model.Project
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Long) -> Unit,
    viewModel: ProjectListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "I miei Progetti",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Nuovo progetto")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.projects.isEmpty() -> {
                    EmptyProjectsPlaceholder(
                        onCreateClick = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.projects,
                            key = { _, project -> project.id }
                        ) { index, project ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 2 }
                            ) {
                                ProjectCard(
                                    project = project,
                                    onClick = { onProjectClick(project.id) },
                                    onLongClick = { projectToRename = project }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) } // spazio per FAB
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onConfirm = { name, description ->
                viewModel.createProject(name, description)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // ─── Dialog rinomina ──────────────────────────────────────────────────────
    projectToRename?.let { project ->
        ProjectContextMenu(
            project = project,
            onRename = { newName ->
                viewModel.renameProject(project.id, newName)
                projectToRename = null
            },
            onDelete = {
                projectToRename = null
                projectToDelete = project
            },
            onDismiss = { projectToRename = null }
        )
    }

    // ─── Dialog conferma eliminazione ─────────────────────────────────────────
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Elimina progetto") },
            text = { Text("Eliminare \"${project.name}\"?\nTutti i dati e le immagini verranno rimossi definitivamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("Annulla") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copertina immagine
            val coverPath = project.coverImage?.localPath
            if (coverPath != null && File(coverPath).exists()) {
                AsyncImage(
                    model = File(coverPath),
                    contentDescription = "Copertina ${project.name}",
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Mini palette se disponibile
                    if (project.palette != null && project.palette.colors.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            project.palette.colors.take(5).forEach { color ->
                                val composeColor = androidx.compose.ui.graphics.Color(
                                    android.graphics.Color.parseColor(color.hex)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(composeColor)
                                )
                            }
                        }
                    }
                }
            }

            // Info progetto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Text(
                    text = "${project.images.size} ${if (project.images.size == 1) "immagine" else "immagini"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProjectContextMenu(
    project: Project,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf("menu") } // "menu" | "rename"
    var nameText by remember { mutableStateOf(project.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == "rename") "Rinomina progetto" else project.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            if (mode == "rename") {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nuovo nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { mode = "rename" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rinomina", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Elimina", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            if (mode == "rename") {
                Button(
                    onClick = { if (nameText.isNotBlank()) onRename(nameText) },
                    enabled = nameText.isNotBlank()
                ) { Text("Rinomina") }
            }
        },
        dismissButton = {
            TextButton(onClick = if (mode == "rename") { { mode = "menu" } } else onDismiss) {
                Text(if (mode == "rename") "Indietro" else "Chiudi")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun EmptyProjectsPlaceholder(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Nessun progetto ancora",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Crea il tuo primo progetto\nper iniziare ad analizzare i colori",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onCreateClick) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nuovo Progetto")
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Progetto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome progetto") },
                    placeholder = { Text("es. Cucina Rossi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (opzionale)") },
                    placeholder = { Text("es. Restyling cucina, cliente vuole toni chiari") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
