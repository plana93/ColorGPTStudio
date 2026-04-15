package com.example.colorgptstudio.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.colorgptstudio.domain.model.ColorData
import com.example.colorgptstudio.domain.model.ColorPalette

/**
 * Card che mostra una [ColorPalette] estratta automaticamente.
 * - Tap su swatch → mostra [ColorDetailSheet] con tutti i formati
 * - Long-press su swatch → fullscreen monocromatico con HEX centrato
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaletteCard(
    palette: ColorPalette?,
    onColorSelected: (ColorData) -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember { mutableStateOf<ColorData?>(null) }
    var fullscreenColor by remember { mutableStateOf<ColorData?>(null) }

    // ─── Fullscreen preview ───────────────────────────────────────────────────
    fullscreenColor?.let { c ->
        val bg = remember(c.hex) {
            try { Color(android.graphics.Color.parseColor(c.hex)) } catch (e: Exception) { Color.Gray }
        }
        Dialog(
            onDismissRequest = { fullscreenColor = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .combinedClickable(
                        onClick = { fullscreenColor = null },
                        onLongClick = { fullscreenColor = null }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLight = (c.r * 0.299 + c.g * 0.587 + c.b * 0.114) > 186
                    val textColor = if (isLight) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
                    Text(
                        text = c.hex.uppercase(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = textColor
                        )
                    )
                    Text(
                        text = c.rgbString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "RAL ${c.ralApprox.first} — ${c.ralApprox.second}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "tocca per chiudere",
                        style = MaterialTheme.typography.labelSmall.copy(color = textColor.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = palette?.label ?: "Palette automatica",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Swatches
            if (isLoading) {
                PaletteSkeletonRow()
            } else if (palette != null && palette.colors.isNotEmpty()) {
                // Row di swatches con tap + long-press
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    palette.colors.forEach { colorData ->
                        val bg = remember(colorData.hex) {
                            try { Color(android.graphics.Color.parseColor(colorData.hex)) } catch (e: Exception) { Color.Gray }
                        }
                        val isSelected = selectedColor?.hex == colorData.hex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(bg)
                                .combinedClickable(
                                    onClick = {
                                        selectedColor = if (isSelected) null else colorData
                                        if (!isSelected) onColorSelected(colorData)
                                    },
                                    onLongClick = { fullscreenColor = colorData }
                                )
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-6).dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna palette estratta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dettaglio completo colore selezionato (tutti i formati)
            AnimatedVisibility(
                visible = selectedColor != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                selectedColor?.let { color ->
                    ColorDetailSheet(
                        color = color,
                        label = "",
                        note = "",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** Skeleton animato mostrato mentre la palette è in fase di estrazione */
@Composable
private fun PaletteSkeletonRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}


/**
 * Card che mostra una [ColorPalette] estratta automaticamente.
 * Include il titolo, la riga di swatches e (opzionale) i dettagli al click.
 *
 * @param palette          La palette da visualizzare
 * @param onColorSelected  Callback con il ColorData selezionato
 * @param isLoading        Se true, mostra uno skeleton animato (analisi in corso)
 */
