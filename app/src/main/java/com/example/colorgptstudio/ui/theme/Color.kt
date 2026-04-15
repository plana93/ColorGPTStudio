package com.example.colorgptstudio.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Filosofia: "Studio Digitale" ─────────────────────────────────────────────
// L'interfaccia usa toni neutri per non competere con i colori analizzati.
// Un solo colore accento (Blu Elettrico) guida l'occhio verso le azioni.

// ─── Accento Primario: Blu Elettrico ──────────────────────────────────────────
val AccentBlue        = Color(0xFF3D8BFF)  // Blu elettrico vibrante
val AccentBlueDark    = Color(0xFF1A6BE0)  // Variante scura per stati premuti
val AccentBlueLight   = Color(0xFF80B4FF)  // Variante chiara per sfondi tinted

// ─── Tema Dark (principale) ───────────────────────────────────────────────────
val DarkBackground    = Color(0xFF121214)  // Sfondo app principale
val DarkSurface       = Color(0xFF1E1E22)  // Card, bottom sheet
val DarkSurfaceVar    = Color(0xFF2A2A30)  // Input, chips, secondari
val DarkOutline       = Color(0xFF3A3A42)  // Bordi, divisori
val DarkOnBackground  = Color(0xFFF0F0F5)  // Testo principale
val DarkOnSurface     = Color(0xFFD0D0DA)  // Testo secondario
val DarkOnSurfaceVar  = Color(0xFF8A8A9A)  // Testo terziario, placeholder

// ─── Tema Light ───────────────────────────────────────────────────────────────
val LightBackground   = Color(0xFFF5F5F7)  // Sfondo app principale
val LightSurface      = Color(0xFFFFFFFF)  // Card, bottom sheet
val LightSurfaceVar   = Color(0xFFEEEEF2)  // Input, chips, secondari
val LightOutline      = Color(0xFFD0D0D8)  // Bordi, divisori
val LightOnBackground = Color(0xFF141416)  // Testo principale
val LightOnSurface    = Color(0xFF3A3A42)  // Testo secondario
val LightOnSurfaceVar = Color(0xFF7A7A8A)  // Testo terziario, placeholder

// ─── Colori Semantici ─────────────────────────────────────────────────────────
val ColorSuccess      = Color(0xFF34C759)  // Verde successo (iOS-like, universale)
val ColorWarning      = Color(0xFFFF9500)  // Arancione warning
val ColorError        = Color(0xFFFF3B30)  // Rosso errore
