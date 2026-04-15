package com.example.colorgptstudio.domain.model

import kotlin.math.*

/**
 * Rappresenta tutti i formati di un singolo colore estratto.
 * Il campo [hex] è il primario, gli altri sono calcolati on-demand.
 */
data class ColorData(
    val hex: String,       // es. "#A3B2C1"
    val r: Int,
    val g: Int,
    val b: Int,
    val h: Float,          // HSL - Hue 0..360
    val s: Float,          // HSL - Saturation 0..1
    val l: Float,          // HSL - Lightness 0..1
    val a: Float = 1f      // Alpha 0..1
) {
    /** Stringa HEX compatta senza #, utile per display */
    val hexClean: String get() = hex.removePrefix("#").uppercase()

    /** Stringa RGB human-readable */
    val rgbString: String get() = "rgb($r, $g, $b)"

    /** Stringa HSL human-readable */
    val hslString: String get() = "hsl(${h.toInt()}°, ${(s * 100).toInt()}%, ${(l * 100).toInt()}%)"

    // ─── CMYK ─────────────────────────────────────────────────────────────────
    val cmyk: FloatArray get() {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val k = 1f - maxOf(rf, gf, bf)
        return if (k == 1f) floatArrayOf(0f, 0f, 0f, 1f)
        else floatArrayOf(
            (1f - rf - k) / (1f - k),
            (1f - gf - k) / (1f - k),
            (1f - bf - k) / (1f - k),
            k
        )
    }
    val cmykString: String get() {
        val c = cmyk
        return "cmyk(${(c[0]*100).roundToInt()}%, ${(c[1]*100).roundToInt()}%, ${(c[2]*100).roundToInt()}%, ${(c[3]*100).roundToInt()}%)"
    }

    // ─── RAL Classic approssimato ──────────────────────────────────────────────
    /** Restituisce il codice RAL Classic più vicino e il suo nome. */
    val ralApprox: Pair<String, String> get() = findClosestRal(r, g, b)

    val ralString: String get() = ralApprox.let { "RAL ${it.first} – ${it.second}" }

    // ─── NCS (Natural Colour System) approssimato ─────────────────────────────
    /**
     * Approssimazione NCS dal formato S-BBCC-HHNN.
     * Non è una conversione certificata ma dà un'indicazione utile al falegname.
     */
    val ncsApprox: String get() = approximateNcs(r, g, b)

    // ─── Stringa copia appunti completa ────────────────────────────────────────
    fun toClipboardString(label: String = "", note: String = ""): String = buildString {
        if (label.isNotBlank()) appendLine("• $label")
        appendLine("HEX:   #$hexClean")
        appendLine("RGB:   $r, $g, $b")
        appendLine("CMYK:  $cmykString")
        appendLine("HSL:   ${h.toInt()}°  ${(s*100).roundToInt()}%  ${(l*100).roundToInt()}%")
        appendLine("RAL:   $ralString")
        appendLine("NCS:   $ncsApprox")
        if (note.isNotBlank()) appendLine("Note:  $note")
    }.trimEnd()

    companion object {
        /** Crea un [ColorData] a partire da un intero ARGB (output di Bitmap.getPixel) */
        fun fromArgb(argb: Int): ColorData {
            val a = ((argb shr 24) and 0xFF) / 255f
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val hex = "#%02X%02X%02X".format(r, g, b)
            val (h, s, l) = rgbToHsl(r, g, b)
            return ColorData(hex = hex, r = r, g = g, b = b, h = h, s = s, l = l, a = a)
        }

        fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
            val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
            val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
            val delta = max - min
            val l = (max + min) / 2f
            val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
            val h = when {
                delta == 0f -> 0f
                max == rf   -> 60f * (((gf - bf) / delta) % 6)
                max == gf   -> 60f * (((bf - rf) / delta) + 2)
                else        -> 60f * (((rf - gf) / delta) + 4)
            }.let { if (it < 0) it + 360f else it }
            return Triple(h, s, l)
        }

        // ─── RAL Classic table (selezione rappresentativa per falegnami) ──────
        // Fonte: RAL Classic ufficiale. Lista ridotta con i colori più usati
        // nel settore legno/verniciatura d'interni.
        private val RAL_TABLE: List<Triple<String, IntArray, String>> = listOf(
            Triple("1000", intArrayOf(190, 189, 127), "Verde giallo"),
            Triple("1001", intArrayOf(194, 176, 120), "Beige"),
            Triple("1002", intArrayOf(198, 166, 100), "Giallo sabbia"),
            Triple("1013", intArrayOf(225, 220, 200), "Bianco perla"),
            Triple("1014", intArrayOf(221, 196, 152), "Avorio"),
            Triple("1015", intArrayOf(230, 210, 180), "Avorio chiaro"),
            Triple("1016", intArrayOf(241, 222, 24), "Giallo zolfo"),
            Triple("1019", intArrayOf(163, 143, 122), "Beige grigio"),
            Triple("1020", intArrayOf(162, 143, 96), "Giallo oliva"),
            Triple("1021", intArrayOf(243, 185, 11), "Giallo colza"),
            Triple("2001", intArrayOf(186, 83, 46), "Arancione rosso"),
            Triple("2004", intArrayOf(244, 100, 24), "Arancione puro"),
            Triple("3000", intArrayOf(162, 35, 29), "Rosso fiamma"),
            Triple("3001", intArrayOf(161, 35, 18), "Rosso segnale"),
            Triple("3002", intArrayOf(162, 35, 29), "Rosso carminio"),
            Triple("3009", intArrayOf(100, 40, 35), "Rosso ossido"),
            Triple("3011", intArrayOf(122, 32, 25), "Rosso ruggine"),
            Triple("3013", intArrayOf(151, 45, 34), "Rosso pomodoro"),
            Triple("3020", intArrayOf(196, 26, 22), "Rosso traffico"),
            Triple("4001", intArrayOf(128, 91, 145), "Lilla rosato"),
            Triple("4002", intArrayOf(143, 62, 74), "Viola rossastro"),
            Triple("5000", intArrayOf(42, 82, 122), "Blu violaceo"),
            Triple("5003", intArrayOf(27, 61, 100), "Blu zaffiro"),
            Triple("5005", intArrayOf(0, 83, 135), "Blu segnale"),
            Triple("5010", intArrayOf(0, 76, 124), "Blu genziana"),
            Triple("5011", intArrayOf(27, 45, 82), "Blu acciaio"),
            Triple("5013", intArrayOf(31, 52, 100), "Blu cobalto"),
            Triple("5015", intArrayOf(34, 113, 179), "Blu cielo"),
            Triple("5017", intArrayOf(6, 91, 143), "Blu traffico"),
            Triple("5024", intArrayOf(93, 155, 196), "Blu pastello"),
            Triple("6001", intArrayOf(54, 103, 43), "Verde smeraldo"),
            Triple("6002", intArrayOf(49, 102, 45), "Verde foglia"),
            Triple("6003", intArrayOf(82, 97, 58), "Verde oliva"),
            Triple("6005", intArrayOf(15, 76, 48), "Verde muschio"),
            Triple("6007", intArrayOf(38, 56, 37), "Verde bottiglia"),
            Triple("6010", intArrayOf(53, 105, 45), "Verde erba"),
            Triple("6011", intArrayOf(105, 130, 86), "Verde reseda"),
            Triple("6017", intArrayOf(82, 130, 72), "Verde maggio"),
            Triple("6018", intArrayOf(87, 166, 57), "Verde giallognolo"),
            Triple("6019", intArrayOf(188, 220, 176), "Verde bianco"),
            Triple("6021", intArrayOf(137, 172, 118), "Verde pallido"),
            Triple("6024", intArrayOf(48, 132, 70), "Verde traffico"),
            Triple("7000", intArrayOf(120, 133, 139), "Grigio scoiattolo"),
            Triple("7001", intArrayOf(138, 149, 151), "Grigio argento"),
            Triple("7004", intArrayOf(155, 155, 155), "Grigio segnale"),
            Triple("7006", intArrayOf(108, 100, 88), "Grigio beige"),
            Triple("7008", intArrayOf(106, 93, 72), "Grigio cachi"),
            Triple("7009", intArrayOf(77, 82, 75), "Grigio verde"),
            Triple("7010", intArrayOf(76, 81, 74), "Grigio tela"),
            Triple("7011", intArrayOf(67, 78, 84), "Grigio ferro"),
            Triple("7015", intArrayOf(79, 90, 100), "Grigio ardesia"),
            Triple("7016", intArrayOf(55, 63, 69), "Grigio antracite"),
            Triple("7021", intArrayOf(35, 40, 44), "Grigio nerastro"),
            Triple("7022", intArrayOf(62, 60, 57), "Grigio ombra"),
            Triple("7030", intArrayOf(149, 148, 138), "Grigio pietra"),
            Triple("7031", intArrayOf(92, 105, 113), "Grigio blu"),
            Triple("7032", intArrayOf(185, 180, 162), "Grigio ghiaia"),
            Triple("7035", intArrayOf(198, 200, 195), "Grigio chiaro"),
            Triple("7037", intArrayOf(127, 130, 126), "Grigio polvere"),
            Triple("7038", intArrayOf(176, 178, 166), "Grigio agata"),
            Triple("7039", intArrayOf(103, 100, 90), "Grigio quarzo"),
            Triple("7040", intArrayOf(152, 158, 162), "Grigio finestra"),
            Triple("7042", intArrayOf(142, 146, 145), "Grigio traffico A"),
            Triple("7043", intArrayOf(82, 89, 93), "Grigio traffico B"),
            Triple("7044", intArrayOf(182, 178, 168), "Grigio seta"),
            Triple("7045", intArrayOf(141, 148, 154), "Grigio telescopio"),
            Triple("7046", intArrayOf(120, 130, 140), "Grigio telemagenta"),
            Triple("7047", intArrayOf(200, 200, 200), "Grigio segnale (chiaro)"),
            Triple("8001", intArrayOf(141, 93, 45), "Marrone ocra"),
            Triple("8002", intArrayOf(99, 57, 47), "Marrone segnale"),
            Triple("8003", intArrayOf(115, 66, 34), "Marrone argilla"),
            Triple("8004", intArrayOf(142, 64, 42), "Marrone rame"),
            Triple("8007", intArrayOf(96, 57, 30), "Marrone cervo"),
            Triple("8008", intArrayOf(111, 72, 33), "Marrone oliva"),
            Triple("8011", intArrayOf(91, 52, 26), "Marrone noce"),
            Triple("8012", intArrayOf(89, 35, 27), "Marrone rosso"),
            Triple("8014", intArrayOf(56, 37, 22), "Marrone seppia"),
            Triple("8015", intArrayOf(94, 39, 27), "Marrone castagno"),
            Triple("8016", intArrayOf(76, 37, 22), "Marrone mogano"),
            Triple("8017", intArrayOf(68, 36, 24), "Marrone cioccolato"),
            Triple("8019", intArrayOf(61, 48, 44), "Marrone grigio"),
            Triple("8022", intArrayOf(26, 20, 18), "Marrone nerastro"),
            Triple("8023", intArrayOf(166, 91, 43), "Marrone arancio"),
            Triple("8024", intArrayOf(121, 77, 46), "Marrone beige"),
            Triple("8025", intArrayOf(117, 88, 66), "Marrone pallido"),
            Triple("8028", intArrayOf(78, 57, 39), "Marrone terra"),
            Triple("9001", intArrayOf(233, 228, 213), "Bianco crema"),
            Triple("9002", intArrayOf(215, 215, 204), "Bianco grigio"),
            Triple("9003", intArrayOf(237, 237, 237), "Bianco segnale"),
            Triple("9004", intArrayOf(40, 40, 40), "Nero segnale"),
            Triple("9005", intArrayOf(14, 14, 16), "Nero intenso"),
            Triple("9006", intArrayOf(166, 166, 166), "Bianco alluminio"),
            Triple("9007", intArrayOf(118, 118, 118), "Grigio alluminio"),
            Triple("9010", intArrayOf(246, 244, 235), "Bianco puro"),
            Triple("9011", intArrayOf(23, 23, 26), "Nero grafite"),
            Triple("9016", intArrayOf(246, 246, 243), "Bianco traffico"),
            Triple("9017", intArrayOf(25, 23, 24), "Nero traffico"),
            Triple("9018", intArrayOf(215, 220, 215), "Bianco papiro"),
        ).map { (code, rgb, name) -> Triple(code, rgb, name) }

        fun findClosestRal(r: Int, g: Int, b: Int): Pair<String, String> {
            var best = RAL_TABLE[0]
            var minDist = Double.MAX_VALUE
            for (entry in RAL_TABLE) {
                val rgb = entry.second
                val d = sqrt(
                    ((r - rgb[0]).toDouble().pow(2)) +
                    ((g - rgb[1]).toDouble().pow(2)) +
                    ((b - rgb[2]).toDouble().pow(2))
                )
                if (d < minDist) { minDist = d; best = entry }
            }
            return Pair(best.first, best.third)
        }

        /**
         * Approssimazione NCS (Natural Colour System).
         * Formato: S-BBCC-HHNNN dove BB=blackness, CC=chromaticness, HH=hue.
         * Non certificato — indicativo per comunicazione con fornitori.
         */
        fun approximateNcs(r: Int, g: Int, b: Int): String {
            val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
            val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
            val whiteness = min
            val blackness = 1f - max
            val chromaticness = max - min

            val blacknessPct = (blackness * 100).roundToInt().coerceIn(0, 90)
            val chromaticPct = (chromaticness * 100).roundToInt().coerceIn(0, 90)

            val hue = when {
                chromaticness < 0.05f -> return "S ${blacknessPct.toString().padStart(2,'0')}00-N"
                else -> {
                    val (h, _, _) = rgbToHsl(r, g, b)
                    when {
                        h < 30  -> "Y${(h * 10 / 3).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}R"
                        h < 60  -> "Y${((h-30)*10/3).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}R".let { "R${((60-h)*10/3).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}Y" }
                        h < 120 -> "Y${((h-60)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}G"
                        h < 180 -> "G${((h-120)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}B"
                        h < 240 -> "B${((h-180)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}G".let { "B${((240-h)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}G" }
                        h < 300 -> "B${((h-240)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}R"
                        else    -> "R${((360-h)*10/6).roundToInt().coerceIn(0,99).toString().padStart(2,'0')}B"
                    }
                }
            }
            return "S ${blacknessPct.toString().padStart(2,'0')}${chromaticPct.toString().padStart(2,'0')}-$hue"
        }
    }
}
