package com.example.colorgptstudio.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.colorgptstudio.domain.model.ColorPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Canvas interattivo condiviso tra QuickAnalysisScreen e AnalysisScreen.
 *
 * Funzionalità:
 * - Tap rapido → callback onTap(xRatio, yRatio)
 * - Drag → mostra lente di ingrandimento 2.5x nell'angolo in alto a destra
 * - Pinch → zoom (0.5x … 8x) + pan
 * - I punti colore vengono scalati e traslati coerentemente con zoom/pan
 */
@Composable
fun InteractiveImageCanvas(
    imageSource: Any?,           // Uri, File, path String
    colorPoints: List<ColorPoint>,
    selectedPointId: Long? = null,
    onTap: (xRatio: Float, yRatio: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // ─── Stato zoom / pan ─────────────────────────────────────────────────────
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    // ─── Stato lente (drag) ───────────────────────────────────────────────────
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }      // posizione assoluta sul canvas
    var magnifierColor by remember { mutableStateOf<Color?>(null) }    // colore estratto in tempo reale

    val context = LocalContext.current
    val density = LocalDensity.current

    // Bitmap in memoria per la lente (caricato lazy una volta)
    var cachedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageSource) {
        withContext(Dispatchers.IO) {
            cachedBitmap = when (imageSource) {
                is Uri -> {
                    context.contentResolver.openInputStream(imageSource)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                is java.io.File -> BitmapFactory.decodeFile(imageSource.absolutePath)
                is String -> BitmapFactory.decodeFile(imageSource)
                else -> null
            }
        }
    }

    /** Converte offset assoluto sul canvas in (xRatio, yRatio) tenendo conto di zoom/pan */
    fun offsetToRatio(offset: Offset): Pair<Float, Float> {
        if (imageSize == IntSize.Zero) return 0f to 0f
        // Inverti la trasformazione zoom/pan per trovare il punto immagine
        val imageX = (offset.x - pan.x) / scale
        val imageY = (offset.y - pan.y) / scale
        return (imageX / imageSize.width).coerceIn(0f, 1f) to
               (imageY / imageSize.height).coerceIn(0f, 1f)
    }

    /** Estrae il colore dal bitmap al punto specificato */
    fun extractColorAt(xRatio: Float, yRatio: Float): Color? {
        val bmp = cachedBitmap ?: return null
        val px = (xRatio * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val py = (yRatio * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val argb = bmp.getPixel(px, py)
        return Color(argb)
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {

        // ─── Immagine con zoom/pan ────────────────────────────────────────────
        AsyncImage(
            model = imageSource,
            contentDescription = "Immagine da analizzare",
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { imageSize = it.size }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = pan.x,
                    translationY = pan.y
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // ── Attendi il primo dito ────────────────────────────
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastPointerCount = 1
                        var accZoom = 1f
                        var dragAccum = Offset.Zero
                        var hasMoved = false

                        // Posizione iniziale per decidere tap vs drag
                        val startPos = down.position

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pointers = event.changes.filter { it.pressed }
                            val pointerCount = pointers.size

                            if (pointerCount == 1) {
                                // ── Single finger: drag per lente ────────────
                                val change = pointers[0]
                                val delta = change.positionChange()
                                dragAccum += delta

                                if (!hasMoved && abs(dragAccum.x) + abs(dragAccum.y) > 8f) {
                                    hasMoved = true
                                }

                                if (hasMoved) {
                                    isDragging = true
                                    dragPosition = change.position
                                    val (xR, yR) = offsetToRatio(
                                        // La posizione del dito è in coordinate del Box,
                                        // ma imageSize è già del Box per ContentScale.Fit
                                        change.position
                                    )
                                    magnifierColor = extractColorAt(xR, yR)
                                    change.consume()
                                }
                            } else if (pointerCount >= 2) {
                                // ── Pinch: zoom + pan ────────────────────────
                                isDragging = false
                                hasMoved = true

                                val zoomFactor = event.calculateZoom()
                                val panDelta = event.calculatePan()

                                scale = (scale * zoomFactor).coerceIn(0.5f, 8f)
                                pan += panDelta
                                pointers.forEach { it.consume() }
                            }

                            lastPointerCount = pointerCount
                        } while (pointers.any { it.pressed })

                        // ── Rilascio ─────────────────────────────────────────
                        if (isDragging) {
                            // Al drag-release: campiona il colore finale
                            val (xR, yR) = offsetToRatio(dragPosition)
                            onTap(xR, yR)
                        } else if (!hasMoved && imageSize != IntSize.Zero) {
                            // Tap puro (nessun movimento)
                            val (xR, yR) = offsetToRatio(startPos)
                            onTap(xR, yR)
                        }
                        isDragging = false
                        magnifierColor = null
                    }
                },
            contentScale = ContentScale.Fit
        )

        // ─── Overlay punti colore (seguono zoom/pan tramite graphicsLayer) ────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = pan.x,
                    translationY = pan.y
                )
        ) {
            colorPoints.forEach { point ->
                val cx = point.xRatio * size.width
                val cy = point.yRatio * size.height
                val isSelected = point.id == selectedPointId
                val ptColor = try {
                    Color(android.graphics.Color.parseColor(point.color.hex))
                } catch (e: Exception) { Color.Gray }

                if (isSelected) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = 22.dp.toPx() / scale,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = 14.dp.toPx() / scale,
                    center = Offset(cx, cy),
                    style = Stroke(2.5.dp.toPx() / scale)
                )
                drawCircle(
                    color = ptColor,
                    radius = 11.dp.toPx() / scale,
                    center = Offset(cx, cy)
                )
            }
        }

        // ─── Lente di ingrandimento ───────────────────────────────────────────
        if (isDragging && imageSize != IntSize.Zero) {
            val lensSize = 120.dp
            val lensSizePx = with(density) { lensSize.toPx() }
            val zoomFactor = 2.8f

            // Posizionata in alto a destra, con offset fisso
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(lensSize)
                    .offset((-12).dp, 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .drawBehind {
                        // Ritaglio circolare già fatto da .clip(CircleShape)
                    }
            ) {
                // Disegniamo la lente con canvas: ritaglio dell'immagine zoomata
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bmp = cachedBitmap
                    if (bmp != null) {
                        val (xR, yR) = offsetToRatio(dragPosition)
                        val bmpX = (xR * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                        val bmpY = (yR * bmp.height).toInt().coerceIn(0, bmp.height - 1)

                        // Porzione del bitmap da zoomare
                        val half = (lensSizePx / zoomFactor / 2).toInt().coerceAtLeast(1)
                        val srcLeft = (bmpX - half).coerceIn(0, bmp.width - 1)
                        val srcTop = (bmpY - half).coerceIn(0, bmp.height - 1)
                        val srcRight = (bmpX + half).coerceIn(srcLeft + 1, bmp.width)
                        val srcBottom = (bmpY + half).coerceIn(srcTop + 1, bmp.height)

                        val srcBitmap = android.graphics.Bitmap.createBitmap(
                            bmp, srcLeft, srcTop, srcRight - srcLeft, srcBottom - srcTop
                        )
                        drawImage(
                            image = srcBitmap.asImageBitmap(),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )

                        // Mirino centrale
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))
                        drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - 14.dp.toPx(), cy), Offset(cx + 14.dp.toPx(), cy), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.7f), Offset(cx, cy - 14.dp.toPx()), Offset(cx, cy + 14.dp.toPx()), 1.dp.toPx())
                    }
                }

                // Bordo della lente
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(2.dp.toPx())
                    )
                }

                // Codice HEX nella lente (banda in basso)
                magnifierColor?.let { c ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .fillMaxSize(0.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        val hex = "#%02X%02X%02X".format(
                            (c.red * 255).roundToInt(),
                            (c.green * 255).roundToInt(),
                            (c.blue * 255).roundToInt()
                        )
                        Text(
                            text = hex,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
