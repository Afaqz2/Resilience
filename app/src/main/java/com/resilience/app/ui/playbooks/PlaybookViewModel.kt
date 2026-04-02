package com.resilience.app.ui.playbooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.db.entity.PlaybookEntity
import com.resilience.app.data.repository.PlaybookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybookUiState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val playbooks: List<PlaybookEntity> = emptyList(),
    val selectedPlaybook: PlaybookEntity? = null,
    /** Controls whether TTS is actively speaking */
    val isSpeaking: Boolean = false
)

@HiltViewModel
class PlaybookViewModel @Inject constructor(
    private val repository: PlaybookRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedPlaybook  = MutableStateFlow<PlaybookEntity?>(null)
    private val _isSpeaking        = MutableStateFlow(false)

    val uiState: StateFlow<PlaybookUiState> = combine(
        repository.getAllCategories(),
        _selectedCategory,
        _selectedPlaybook,
        _isSpeaking
    ) { categories, selectedCat, selectedBook, speaking ->
        PlaybookUiState(
            isLoading = false,
            categories = categories,
            selectedCategory = selectedCat,
            selectedPlaybook = selectedBook,
            isSpeaking = speaking
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaybookUiState()
    )

    /** Playbooks filtered by the currently selected category */
    val filteredPlaybooks: StateFlow<List<PlaybookEntity>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) repository.getAllPlaybooks()
            else repository.getPlaybooksByCategory(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Seed the JSON asset into Room on first launch
        viewModelScope.launch { repository.seedFromAssets() }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectPlaybook(playbook: PlaybookEntity?) {
        _selectedPlaybook.value = playbook
    }

    fun setSpeaking(speaking: Boolean) {
        _isSpeaking.value = speaking
    }
}
