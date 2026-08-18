package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.ChordProParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.data.SongSortOrder
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.SongbookFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        val activeLabels = _selectedLabels.value

        var result = SongbookFilter.apply(
            songs = _allSheets.value,
            query = _searchQuery.value,
            label = activeLabels.firstOrNull(),
            sortOrder = _sortOrder.value,
            title = { it.title },
            artist = { it.artist },
            labels = { it.labels },
            updatedAt = { it.updatedAt },
            createdAt = { it.createdAt },
        )

        if (activeLabels.size > 1) {
            result = result.filter { sheet -> sheet.labels.containsAll(activeLabels) }
        }

        _sheets.value = result
    }

    private var viewOpenedAt: Long = 0L

    fun openSheet(sheet: ChordSheet) {
        val now = System.currentTimeMillis()
        val updated = sheet.copy(
            viewCount = sheet.viewCount + 1,
            lastViewedAt = now,
        )
        repository.save(updated)
        _currentSheet.value = updated
        _isEditing.value = false
        viewOpenedAt = now
        refresh()
    }

    fun closeSheet() {
        recordViewTime()
        _currentSheet.value = null
        _isEditing.value = false
    }

    private fun recordViewTime() {
        val sheet = _currentSheet.value ?: return
        if (viewOpenedAt <= 0L) return
        val elapsed = System.currentTimeMillis() - viewOpenedAt
        viewOpenedAt = 0L
        if (elapsed <= 0) return
        val updated = sheet.copy(totalViewTimeMs = sheet.totalViewTimeMs + elapsed)
        repository.save(updated)
        _currentSheet.value = updated
        refresh()
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

    fun duplicateSheet(sheet: ChordSheet) {
        val now = System.currentTimeMillis()
        val copy = ChordSheet(
            title = "${sheet.title} (Copy)",
            subtitle = sheet.subtitle,
            artist = sheet.artist,
            content = sheet.content,
            key = sheet.key,
            capo = sheet.capo,
            strumPatternName = sheet.strumPatternName,
            labels = sheet.labels,
            createdAt = now,
            updatedAt = now,
        )
        repository.save(copy)
        _currentSheet.value = copy
        _isEditing.value = false
        refresh()
    }

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleSelection(id: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
        if (_selectedIds.value.isEmpty()) _isSelectionMode.value = false
    }

    fun selectAll() {
        _selectedIds.value = _sheets.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun enterSelectionMode(id: String) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun deleteSelected() {
        _selectedIds.value.forEach { repository.delete(it) }
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
        refresh()
    }

    fun applyLabelsToSelected(labels: List<String>) {
        val ids = _selectedIds.value
        _allSheets.value.filter { it.id in ids }.forEach { sheet ->
            val merged = (sheet.labels + labels).distinct()
            val updated = sheet.copy(labels = merged, updatedAt = System.currentTimeMillis())
            repository.save(updated)
        }
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
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
        val sheet = ChordProParser.parse(content, titleFromFilename(filename))
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
        val sheet = ChordSheet(title = titleFromFilename(filename), content = content.trim())
        repository.save(sheet)
        _currentSheet.value = sheet
        _isEditing.value = false
        refresh()
    }

    /**
     * Derives a song title from an imported file name.
     *
     * Strips any path and Storage Access Framework document-ID prefix (e.g.
     * `primary:Download/`) before dropping the extension, so a URI or document ID
     * can never surface as the title (issue #500).
     *
     * @param filename The resolved file name, or `null` when unavailable.
     * @return A display title, falling back to [DEFAULT_IMPORT_TITLE].
     */
    private fun titleFromFilename(filename: String?): String =
        filename
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_IMPORT_TITLE

    private companion object {
        const val DEFAULT_IMPORT_TITLE = "Imported Song"
    }
}
