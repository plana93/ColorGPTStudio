package com.example.colorgptstudio.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.colorgptstudio.domain.model.ColorData

/**
 * Card espandibile che mostra tutti i formati di un colore con bottone "Copia".
 * Usata sia in QuickAnalysis che in AnalysisScreen.
 *
 * @param color     Il [ColorData] da mostrare
 * @param label     Etichetta opzionale del punto colore
 * @param note      Nota opzionale
 * @param modifier  Modifier esterno
 */
@Composable
fun ColorDetailSheet(
    color: ColorData,
    label: String = "",
    note: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showCopiedFeedback by remember { mutableStateOf(false) }

    val composeColor = remember(color.hex) {
        try { Color(android.graphics.Color.parseColor(color.hex)) } catch (e: Exception) { Color.Gray }
    }

    // Feedback "Copiato!" auto-dismiss
    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            kotlinx.coroutines.delay(2000)
            showCopiedFeedback = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ─── Header: swatch + HEX + pulsanti ─────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(composeColor)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${color.hexClean}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (label.isNotBlank()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = color.rgbString,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bottone Copia
                FilledTonalButton(
                    onClick = {
                        copyColorToClipboard(context, color, label, note)
                        showCopiedFeedback = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (showCopiedFeedback) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copia",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showCopiedFeedback) "Copiato!" else "Copia",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Freccia espandi
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Comprimi" else "Espandi",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ─── Dettaglio espanso ────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

                    ColorFormatRow(label = "HEX",  value = "#${color.hexClean}")
                    ColorFormatRow(label = "RGB",  value = "${color.r},  ${color.g},  ${color.b}")
                    ColorFormatRow(label = "CMYK", value = color.cmykString)
                    ColorFormatRow(label = "HSL",  value = "${color.h.toInt()}°  ${(color.s*100).toInt()}%  ${(color.l*100).toInt()}%")
                    ColorFormatRow(label = "RAL",  value = color.ralString)
                    ColorFormatRow(label = "NCS",  value = color.ncsApprox)

                    if (note.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorFormatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Copia il colore negli appunti con tutti i formati. */
fun copyColorToClipboard(context: Context, color: ColorData, label: String = "", note: String = "") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = color.toClipboardString(label, note)
    val clip = ClipData.newPlainText("Colore ColorGPT", text)
    clipboard.setPrimaryClip(clip)
}

/** Copia una lista di colori come testo strutturato. */
fun copyPaletteToClipboard(context: Context, colors: List<ColorData>, paletteLabel: String = "Palette") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = buildString {
        appendLine("=== $paletteLabel ===")
        colors.forEachIndexed { i, c ->
            appendLine()
            appendLine("[Colore ${i + 1}]")
            append(c.toClipboardString())
            appendLine()
        }
    }.trimEnd()
    val clip = ClipData.newPlainText("Palette ColorGPT", text)
    clipboard.setPrimaryClip(clip)
}
