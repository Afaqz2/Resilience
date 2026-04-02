package com.resilience.app.ui.familyvault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import com.resilience.app.data.repository.FamilyVaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyVaultUiState(
    val isLoading: Boolean = true,
    val members: List<FamilyMemberEntity> = emptyList(),
    val meetingPoints: List<MeetingPointEntity> = emptyList(),
    /** Member currently open in the add/edit sheet, null when sheet is closed */
    val editingMember: FamilyMemberEntity? = null,
    val showAddMemberSheet: Boolean = false,
    val showAddMeetingPointSheet: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class FamilyVaultViewModel @Inject constructor(
    private val repository: FamilyVaultRepository
) : ViewModel() {

    private val _showAddMember       = MutableStateFlow(false)
    private val _showAddMeetingPoint = MutableStateFlow(false)
    private val _editingMember       = MutableStateFlow<FamilyMemberEntity?>(null)
    private val _snackbarMessage     = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FamilyVaultUiState> = combine(
        repository.getAllMembers(),
        repository.getAllMeetingPoints(),
        _showAddMember,
        _showAddMeetingPoint,
        _editingMember
    ) { members, points, showMember, showPoint, editing ->
        FamilyVaultUiState(
            isLoading = false,
            members = members,
            meetingPoints = points,
            editingMember = editing,
            showAddMemberSheet = showMember,
            showAddMeetingPointSheet = showPoint
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FamilyVaultUiState()
    )

    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    /* ---------- Member actions ---------- */

    fun openAddMemberSheet(member: FamilyMemberEntity? = null) {
        _editingMember.value = member
        _showAddMember.value = true
    }

    fun closeAddMemberSheet() {
        _showAddMember.value = false
        _editingMember.value = null
    }

    fun saveMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.saveMember(member)
            _snackbarMessage.value = "Member saved"
            closeAddMemberSheet()
        }
    }

    fun deleteMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.deleteMember(member)
            _snackbarMessage.value = "Member removed"
        }
    }

    /* ---------- Meeting point actions ---------- */

    fun openAddMeetingPointSheet() { _showAddMeetingPoint.value = true }
    fun closeAddMeetingPointSheet() { _showAddMeetingPoint.value = false }

    fun saveMeetingPoint(point: MeetingPointEntity) {
        viewModelScope.launch {
            repository.saveMeetingPoint(point)
            _snackbarMessage.value = "Meeting point saved"
            closeAddMeetingPointSheet()
        }
    }

    fun deleteMeetingPoint(point: MeetingPointEntity) {
        viewModelScope.launch {
            repository.deleteMeetingPoint(point)
            _snackbarMessage.value = "Meeting point removed"
        }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }
}
