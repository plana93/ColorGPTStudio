# Copilot Instructions — ColorGPTStudio

## Tecnologie

- **Android** con **Jetpack Compose**
- **Kotlin**
- Architettura MVVM + Repository
- Immagini gestite con **Coil** (`AsyncImage`)
- Device target: MediaTek A142, Android, con **SELinux restrittivo**

---

## Regole critiche

### 1. Non usare `File` catturato da closure in Compose

**❌ SBAGLIATO — il `File` viene catturato al momento del primo render, quando il path può essere ancora vuoto:**

```kotlin
// FUORI dalla lambda — cattura il valore iniziale (può essere File("") → path="/")
val imageFile = java.io.File(uiState.imageLocalPath)
InteractiveImageCanvas(
    onTap = { xRatio, yRatio ->
        extractColorAtRatioFromFile(file = imageFile, ...)  // imageFile.absolutePath può essere "/"
    }
)
```

**✅ CORRETTO — costruire il `File` DENTRO la lambda, leggendo `uiState` al momento del tap:**

```kotlin
InteractiveImageCanvas(
    imageSource = if (uiState.imageLocalPath.isNotBlank()) java.io.File(uiState.imageLocalPath) else null,
    onTap = { xRatio, yRatio ->
        val currentPath = uiState.imageLocalPath   // ← letto al momento del tap
        if (currentPath.isNotBlank()) {
            val colorData = extractColorAtRatioFromFile(
                file = java.io.File(currentPath),  // ← costruito qui, non fuori
                xRatio = xRatio,
                yRatio = yRatio
            )
            colorData?.let { viewModel.onColorPicked(xRatio, yRatio, it) }
        }
    }
)
```

**Perché:** In Compose le lambda passate come parametri a un Composable figlio possono catturare una versione stale della `val` locale. Il `File` costruito fuori dalla lambda usa il path del primo render (spesso blank → `File("").absolutePath == "/"`). Leggere `uiState.imageLocalPath` direttamente dentro la lambda garantisce il valore aggiornato.

---

### 2. SELinux su MediaTek A142 — non usare `Uri.fromFile()` né `File.exists()`

**❌ SBAGLIATO:**

```kotlin
val uri = Uri.fromFile(imageFile)          // → SELinux denied
val exists = imageFile.exists()            // → sempre false su /data/user/0/
```

**✅ CORRETTO — usare `BitmapFactory.decodeFile()` direttamente:**

```kotlin
fun extractColorAtRatioFromFile(file: java.io.File, xRatio: Float, yRatio: Float): ColorData? = try {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)  // funziona anche se exists()=false
    if (bitmap != null) {
        val x = (xRatio * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (yRatio * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val argb = bitmap.getPixel(x, y)
        bitmap.recycle()
        ColorData.fromArgb(argb)
    } else null
} catch (e: Exception) { null }
```

**Perché:** Su questo device SELinux blocca le operazioni sul path `/data/user/0/` tramite `File` API, ma `BitmapFactory.decodeFile()` passa attraverso un canale diverso e funziona correttamente.

---

### 3. Gesture in Compose — un solo `pointerInput` per composable

**❌ SBAGLIATO — due `pointerInput(Unit)` in chain sullo stesso Modifier:**

```kotlin
.pointerInput(Unit) {
    detectTapGestures { offset -> onTap(...) }
}
.pointerInput(Unit) {
    awaitEachGesture { /* drag + pinch */ }
}
```

**Problema:** In Compose, i Modifier si applicano dall'esterno verso l'interno. Il secondo `pointerInput` nella lista viene eseguito **prima** nella pipeline eventi. Se `awaitEachGesture` consuma il `DOWN` event (anche con `requireUnconsumed = false`), `detectTapGestures` non lo riceve più e i tap non scattano mai.

**✅ CORRETTO — un unico `awaitEachGesture` che gestisce tap + drag + pinch:**

```kotlin
.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position
        var dragAccum = Offset.Zero
        var hasMoved = false

        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pointers = event.changes.filter { it.pressed }

            if (pointers.size == 1) {
                dragAccum += pointers[0].positionChange()
                if (!hasMoved && abs(dragAccum.x) + abs(dragAccum.y) > 10f) hasMoved = true
                if (hasMoved) {
                    // drag logic...
                    pointers[0].consume()
                }
            } else if (pointers.size >= 2) {
                hasMoved = true
                scale = (scale * event.calculateZoom()).coerceIn(0.5f, 8f)
                pan += event.calculatePan()
                pointers.forEach { it.consume() }
            }
        } while (pointers.any { it.pressed })

        if (!hasMoved) {
            // tap puro
            val (xR, yR) = offsetToRatio(startPos)
            onTap(xR, yR)
        }
    }
}
```

---

### 4. Camera — usare `FileProvider` + contratto `TakePicture`

- Non usare `ACTION_IMAGE_CAPTURE` con `Uri.fromFile()` → crash su Android 7+
- Usare sempre `FileProvider` con `getUriForFile()` e contratto `ActivityResultContracts.TakePicture()`
- Il file foto va salvato in `context.filesDir` (non `cacheDir`, non `externalFilesDir`)

---

### 5. `offsetToRatio` — non correggere manualmente il letterbox di `ContentScale.Fit`

```kotlin
// ❌ SBAGLIATO — sottrarre renderedImageOffset rompe le coordinate
fun offsetToRatio(offset: Offset): Pair<Float, Float> {
    val localX = (offset.x - pan.x - renderedImageOffset.x) / scale  // WRONG
    ...
}

// ✅ CORRETTO — ContentScale.Fit è gestito da Compose layout, non da una trasformazione manuale
fun offsetToRatio(offset: Offset): Pair<Float, Float> {
    if (imageSize == IntSize.Zero) return 0f to 0f
    val localX = (offset.x - pan.x) / scale
    val localY = (offset.y - pan.y) / scale
    return (localX / imageSize.width).coerceIn(0f, 1f) to
           (localY / imageSize.height).coerceIn(0f, 1f)
}
```

`renderedImageOffset` serve **solo** per posizionare i pallini overlay nel Canvas — non per `offsetToRatio`.
