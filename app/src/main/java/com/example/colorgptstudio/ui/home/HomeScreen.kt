package com.example.colorgptstudio.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import com.example.colorgptstudio.ui.theme.AccentBlue
import com.example.colorgptstudio.ui.theme.AccentBlueDark

/**
 * Schermata di ingresso principale.
 * Presenta due macro-azioni: "Analisi Rapida" e "I miei Progetti".
 */
@Composable
fun HomeScreen(
    onQuickAnalysisClick: () -> Unit,
    onProjectsClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Logo / Titolo app ────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Placeholder palette decorativa
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .height(8.dp)
                            .width(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            Color(0xFFE8C9A0),
                            Color(0xFF5C8A6B),
                            Color(0xFF3D5A80),
                            Color(0xFFC45B5B),
                            Color(0xFFF0E4C8)
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "ColorGPT Studio",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Analisi colore professionale\nper ogni progetto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── Card: Analisi Rapida ─────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 150)) +
                        slideInVertically(tween(700, delayMillis = 150)) { 60 }
            ) {
                HomeActionCard(
                    title = "Analisi Rapida",
                    description = "Scatta o carica una foto,\nseleziona i colori all'istante.\nNessun salvataggio.",
                    icon = Icons.Outlined.FlashOn,
                    gradient = Brush.linearGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    accentColor = AccentBlue,
                    onClick = onQuickAnalysisClick
                )
            }

            // ─── Card: I miei Progetti ────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 300)) +
                        slideInVertically(tween(700, delayMillis = 300)) { 60 }
            ) {
                HomeActionCard(
                    title = "I miei Progetti",
                    description = "Crea una scheda progetto,\naggiungi più immagini,\nanota materiali e palette.",
                    icon = Icons.Outlined.FolderOpen,
                    gradient = Brush.linearGradient(
                        colors = listOf(AccentBlueDark.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onProjectsClick
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: Brush,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
