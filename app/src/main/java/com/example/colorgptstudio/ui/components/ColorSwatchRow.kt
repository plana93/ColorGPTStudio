package com.example.colorgptstudio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.colorgptstudio.domain.model.ColorData

/**
 * Riga orizzontale di swatches colore, con animazione di entrata.
 * Usata nella PaletteCard e nella schermata di Analisi.
 *
 * @param colors      Lista di [ColorData] da mostrare
 * @param selectedHex HEX del colore correntemente selezionato (opzionale)
 * @param onColorClick Callback al click su uno swatch
 * @param swatchHeight Altezza di ogni swatch (default 56.dp)
 */
@Composable
fun ColorSwatchRow(
    colors: List<ColorData>,
    selectedHex: String? = null,
    onColorClick: (ColorData) -> Unit = {},
    swatchHeight: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(swatchHeight)
            .clip(RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        colors.forEachIndexed { index, colorData ->
            val isSelected = colorData.hex.uppercase() == selectedHex?.uppercase()
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = tween(200),
                label = "swatch_scale_$index"
            )

            val composeColor = remember(colorData.hex) {
                try {
                    Color(android.graphics.Color.parseColor(colorData.hex))
                } catch (e: Exception) {
                    Color.Gray
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .scale(scale)
                    .background(composeColor)
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(0.dp)
                        ) else Modifier
                    )
                    .clickable { onColorClick(colorData) },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (isSelected) {
                    Text(
                        text = colorData.hexClean,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
