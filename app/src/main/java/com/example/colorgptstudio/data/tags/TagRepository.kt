package com.example.colorgptstudio.data.tags

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Gestisce i tag disponibili nell'app:
 * - Tag predefiniti letti da assets/tags_preset.json
 * - Tag custom aggiunti dall'utente, persistiti in SharedPreferences
 *
 * I tag vengono esposti come StateFlow per aggiornamento reattivo della UI.
 */
class TagRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("colorgpt_tags", Context.MODE_PRIVATE)
    }

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    private val _presetCategories = MutableStateFlow<List<TagCategory>>(emptyList())
    val presetCategories: StateFlow<List<TagCategory>> = _presetCategories.asStateFlow()

    data class TagCategory(val name: String, val tags: List<String>)

    init {
        loadTags()
    }

    private fun loadTags() {
        val preset = loadPresetTags()
        val custom = loadCustomTags()
        _presetCategories.value = preset
        val presetFlat = preset.flatMap { it.tags }
        _allTags.value = (presetFlat + custom).distinct().sorted()
    }

    private fun loadPresetTags(): List<TagCategory> = try {
        val json = context.assets.open("tags_preset.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("categories")
        (0 until arr.length()).map { i ->
            val cat = arr.getJSONObject(i)
            val tagsArr = cat.getJSONArray("tags")
            val tags = (0 until tagsArr.length()).map { j -> tagsArr.getString(j) }
            TagCategory(cat.getString("name"), tags)
        }
    } catch (e: Exception) {
        Timber.e(e, "TagRepository: errore caricamento tags_preset.json")
        emptyList()
    }

    private fun loadCustomTags(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_TAGS, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    /** Aggiunge un tag custom se non esiste già. */
    suspend fun addCustomTag(tag: String) = withContext(Dispatchers.IO) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return@withContext
        val current = _allTags.value
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return@withContext

        val customTags = loadCustomTags().toMutableList()
        customTags.add(trimmed)
        prefs.edit().putString(KEY_CUSTOM_TAGS, customTags.joinToString(SEPARATOR)).apply()
        _allTags.update { (it + trimmed).distinct().sorted() }
        Timber.d("TagRepository: tag custom aggiunto → $trimmed")
    }

    /** Suggerisce tag che iniziano con il prefisso dato (case-insensitive). */
    fun suggest(prefix: String): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        return _allTags.value.filter { it.startsWith(lower, ignoreCase = true) }.take(8)
    }

    companion object {
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val SEPARATOR = "|||"
    }
}
