package com.example.colorgptstudio.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.colorgptstudio.domain.model.ColorPalette

/**
 * Card che mostra una [ColorPalette] estratta automaticamente.
 * Include il titolo, la riga di swatches e (opzionale) i dettagli al click.
 *
 * @param palette          La palette da visualizzare
 * @param onColorSelected  Callback con il ColorData selezionato
 * @param isLoading        Se true, mostra uno skeleton animato (analisi in corso)
 */
@Composable
fun PaletteCard(
    palette: ColorPalette?,
    onColorSelected: (com.example.colorgptstudio.domain.model.ColorData) -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedHex by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<com.example.colorgptstudio.domain.model.ColorData?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Header ──────────────────────────────────────────────────────
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

            // ─── Swatches ─────────────────────────────────────────────────────
            if (isLoading) {
                PaletteSkeletonRow()
            } else if (palette != null && palette.colors.isNotEmpty()) {
                ColorSwatchRow(
                    colors = palette.colors,
                    selectedHex = selectedHex,
                    onColorClick = { colorData ->
                        selectedHex = if (selectedHex == colorData.hex) null else colorData.hex
                        selectedColor = if (selectedHex != null) colorData else null
                        if (selectedHex != null) onColorSelected(colorData)
                    },
                    swatchHeight = 64.dp
                )
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

            // ─── Dettaglio colore selezionato ─────────────────────────────────
            AnimatedVisibility(
                visible = selectedColor != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                selectedColor?.let { color ->
                    ColorDetailRow(colorData = color)
                }
            }
        }
    }
}

@Composable
private fun ColorDetailRow(
    colorData: com.example.colorgptstudio.domain.model.ColorData
) {
    val composeColor = remember(colorData.hex) {
        try { Color(android.graphics.Color.parseColor(colorData.hex)) } catch (e: Exception) { Color.Gray }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(composeColor)
        )
        Column {
            Text(
                text = colorData.hex.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${colorData.rgbString}  •  ${colorData.hslString}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
