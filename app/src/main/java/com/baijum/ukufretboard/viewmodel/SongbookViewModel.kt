package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.ChordProParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SongSortOrder {
    LAST_MODIFIED,
    DATE_ADDED,
    TITLE,
    ARTIST,
}

/**
 * ViewModel for managing the songbook (list of chord sheets).
 */
class SongbookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChordSheetRepository(application)

    private val _allSheets = MutableStateFlow<List<ChordSheet>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SongSortOrder.LAST_MODIFIED)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder.asStateFlow()

    private val _selectedLabels = MutableStateFlow<Set<String>>(emptySet())
    val selectedLabels: StateFlow<Set<String>> = _selectedLabels.asStateFlow()

    private val _allLabels = MutableStateFlow<Set<String>>(emptySet())
    /** All distinct labels used across every saved song. */
    val allLabels: StateFlow<Set<String>> = _allLabels.asStateFlow()

    private val _sheets = MutableStateFlow<List<ChordSheet>>(emptyList())

    /** Observable list of filtered and sorted chord sheets. */
    val sheets: StateFlow<List<ChordSheet>> = _sheets.asStateFlow()

    /** The currently open sheet (for viewing/editing). */
    private val _currentSheet = MutableStateFlow<ChordSheet?>(null)
    val currentSheet: StateFlow<ChordSheet?> = _currentSheet.asStateFlow()

    /** Whether the editor is open. */
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _allSheets.value = repository.getAll()
        _allLabels.value = _allSheets.value.flatMap { it.labels }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)
        applyFilterAndSort()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilterAndSort()
    }

    fun setSortOrder(order: SongSortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    fun toggleLabelFilter(label: String) {
        val current = _selectedLabels.value
        _selectedLabels.value = if (label in current) current - label else current + label
        applyFilterAndSort()
    }

    fun clearLabelFilter() {
        _selectedLabels.value = emptySet()
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val query = _searchQuery.value.trim().lowercase()
        val activeLabels = _selectedLabels.value

        var filtered = _allSheets.value

        if (query.isNotEmpty()) {
            filtered = filtered.filter { sheet ->
                sheet.title.lowercase().contains(query) ||
                    sheet.artist.lowercase().contains(query)
            }
        }

        if (activeLabels.isNotEmpty()) {
            filtered = filtered.filter { sheet ->
                sheet.labels.containsAll(activeLabels)
            }
        }

        _sheets.value = when (_sortOrder.value) {
            SongSortOrder.LAST_MODIFIED -> filtered.sortedByDescending { it.updatedAt }
            SongSortOrder.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
            SongSortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SongSortOrder.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
        }
    }

    fun openSheet(sheet: ChordSheet) {
        _currentSheet.value = sheet
        _isEditing.value = false
    }

    fun closeSheet() {
        _currentSheet.value = null
        _isEditing.value = false
    }

    fun startEditing(sheet: ChordSheet? = null) {
        _currentSheet.value = sheet ?: ChordSheet(title = "", content = "")
        _isEditing.value = true
    }

    fun saveSheet(
        title: String,
        artist: String,
        content: String,
        key: String = "",
        strumPatternName: String = "",
        labels: List<String> = emptyList(),
    ) {
        val existing = _currentSheet.value
        val sheet = if (existing != null && existing.title.isNotEmpty()) {
            existing.copy(
                title = title,
                artist = artist,
                content = content,
                key = key,
                strumPatternName = strumPatternName,
                labels = labels,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            ChordSheet(
                title = title,
                artist = artist,
                content = content,
                key = key,
                strumPatternName = strumPatternName,
                labels = labels,
            )
        }
        repository.save(sheet)
        _currentSheet.value = sheet
        _isEditing.value = false
        refresh()
    }

    fun updateLabels(labels: List<String>) {
        val sheet = _currentSheet.value ?: return
        val updated = sheet.copy(
            labels = labels,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        _currentSheet.value = updated
        refresh()
    }

    fun applyTranspose(semitones: Int) {
        if (semitones == 0) return
        val sheet = _currentSheet.value ?: return
        val transposedContent = ChordSheetTranspose.transpose(sheet.content, semitones)
        val updated = sheet.copy(
            content = transposedContent,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        _currentSheet.value = updated
        refresh()
    }

    fun updateStrumPattern(patternName: String) {
        val sheet = _currentSheet.value ?: return
        val updated = sheet.copy(
            strumPatternName = patternName,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        _currentSheet.value = updated
        refresh()
    }

    fun deleteSheet(id: String) {
        repository.delete(id)
        if (_currentSheet.value?.id == id) {
            _currentSheet.value = null
        }
        refresh()
    }

    /**
     * Imports a song from ChordPro-formatted text.
     *
     * Parses the content, saves the resulting [ChordSheet], and opens it.
     *
     * @param content The raw ChordPro text.
     * @param filename Optional filename used as a fallback title.
     */
    fun importChordPro(content: String, filename: String? = null) {
        val defaultTitle = filename
            ?.substringBeforeLast(".")
            ?.replace("_", " ")
            ?: "Imported Song"
        val sheet = ChordProParser.parse(content, defaultTitle)
        repository.save(sheet)
        _currentSheet.value = sheet
        _isEditing.value = false
        refresh()
    }

    /**
     * Imports a song from plain text (non-ChordPro).
     *
     * Wraps the content in a new [ChordSheet] and saves it.
     *
     * @param content The raw text content (may contain [ChordName] markers).
     * @param filename Optional filename used as the title.
     */
    fun importPlainText(content: String, filename: String? = null) {
        val title = filename
            ?.substringBeforeLast(".")
            ?.replace("_", " ")
            ?: "Imported Song"
        val sheet = ChordSheet(title = title, content = content.trim())
        repository.save(sheet)
        _currentSheet.value = sheet
        _isEditing.value = false
        refresh()
    }
}
