package com.resilience.app.ui.familyvault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import com.resilience.app.ui.components.TacticalCard
import com.resilience.app.ui.components.TacticalBottomStatusBar
import com.resilience.app.ui.components.TacticalIconButton
import com.resilience.app.ui.components.TacticalScannerOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyVaultScreen(
    onBack: () -> Unit,
    viewModel: FamilyVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FamilyVaultHeader(
                activeCount = uiState.members.size,
                onBack = onBack
            )
        },
        bottomBar = {
            TacticalBottomStatusBar(
                statusText = "COMMS ACTIVE",
                infoText = "${uiState.members.size} CONTACT${if (uiState.members.size == 1) "" else "S"}"
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle scanner for secondary screens
            TacticalScannerOverlay(isProminent = false)

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Box
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // -------- Meeting Points Section --------
                item {
                    SectionHeader(
                        title = "// RALLY POINTS",
                        icon = Icons.Default.LocationOn,
                        onAdd = { viewModel.openAddMeetingPointSheet() }
                    )
                }

                if (uiState.meetingPoints.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No rally points set yet.",
                            icon = Icons.Default.AddLocation
                        )
                    }
                }

                items(uiState.meetingPoints, key = { "pt_${it.id}" }) { point ->
                    MeetingPointCard(
                        point = point,
                        onDelete = { viewModel.deleteMeetingPoint(point) }
                    )
                }

                // -------- Family Members Section --------
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        title = "// CONTACTS",
                        icon = Icons.Default.Group,
                        onAdd = { viewModel.openAddMemberSheet() }
                    )
                }

                if (uiState.members.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No family members added yet.",
                            icon = Icons.Default.PersonAdd
                        )
                    }
                }

                items(uiState.members, key = { "member_${it.id}" }) { member ->
                    FamilyMemberCard(
                        member = member,
                        onEdit = { viewModel.openAddMemberSheet(member) },
                        onDelete = { viewModel.deleteMember(member) }
                    )
                }
            }
        }
    }

    // -------- Add / Edit Member Sheet --------
    if (uiState.showAddMemberSheet) {
        AddMemberBottomSheet(
            initial = uiState.editingMember,
            onSave = { viewModel.saveMember(it) },
            onDismiss = { viewModel.closeAddMemberSheet() }
        )
    }

    // -------- Add Meeting Point Sheet --------
    if (uiState.showAddMeetingPointSheet) {
        AddMeetingPointBottomSheet(
            onSave = { viewModel.saveMeetingPoint(it) },
            onDismiss = { viewModel.closeAddMeetingPointSheet() }
        )
    }
}

@Composable
private fun FamilyVaultHeader(
    activeCount: Int,
    onBack: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TacticalIconButton(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FAMILY PLAN",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Text(
                        text = "$activeCount ACTIVE",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header with + button
// ---------------------------------------------------------------------------
@Composable
private fun SectionHeader(title: String, icon: ImageVector, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        TacticalIconButton(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            onClick = onAdd,
            containerSize = 32.dp,
            iconSize = 16.dp,
            tint = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    }
}

// ---------------------------------------------------------------------------
// Family Member Card
// ---------------------------------------------------------------------------
@Composable
private fun FamilyMemberCard(
    member: FamilyMemberEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val initials = member.name.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }

    TacticalCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp).clickable { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tactical Avatar Square
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                    // Status dot overlay
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary // Assuming active/online for demo
                        ) {}
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                    Text(
                        member.relationship.uppercase(), 
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), 
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Now", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PING", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CONTACT", style = MaterialTheme.typography.labelMedium)
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))
                    MemberDetailRow(label = "Blood Group", value = member.bloodGroup, icon = Icons.Default.Bloodtype)
                    if (member.allergies.isNotBlank())
                        MemberDetailRow(label = "Allergies", value = member.allergies, icon = Icons.Default.Warning)
                    if (member.medications.isNotBlank())
                        MemberDetailRow(label = "Medications", value = member.medications, icon = Icons.Default.MedicalServices)
                    if (member.emergencyContactName.isNotBlank())
                        MemberDetailRow(label = "Emergency Contact", value = "${member.emergencyContactName} · ${member.emergencyContactPhone}", icon = Icons.Default.Phone)
                    if (member.notes.isNotBlank())
                        MemberDetailRow(label = "Notes", value = member.notes, icon = Icons.Default.Notes)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDelete, shape = RoundedCornerShape(4.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onEdit, shape = RoundedCornerShape(4.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberDetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label.uppercase(), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Meeting Point Card
// ---------------------------------------------------------------------------
@Composable
private fun MeetingPointCard(point: MeetingPointEntity, onDelete: () -> Unit) {
    val priorityColor = when (point.priority) {
        1 -> MaterialTheme.colorScheme.primary // Rust
        2 -> MaterialTheme.colorScheme.tertiary // Amber
        else -> MaterialTheme.colorScheme.secondary // Olive
    }
    val priorityLabel = when (point.priority) {
        1 -> "PRIMARY RALLY POINT"
        2 -> "SECONDARY RALLY POINT"
        else -> "TERTIARY RALLY POINT"
    }

    TacticalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = priorityColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(priorityLabel, color = priorityColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                TacticalIconButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    onClick = onDelete,
                    containerSize = 32.dp,
                    iconSize = 16.dp,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(point.label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "%.5f° N, %.5f° W".format(point.latitude, Math.abs(point.longitude)), // Dummy format mimicking mock
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha=0.2f), shape = RoundedCornerShape(2.dp)) {
                            Text("VERIFIED", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal=6.dp, vertical=2.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Last checked: 6h ago", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (point.plainTextDirections.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.02f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha=0.1f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Directions: ${point.plainTextDirections}", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty State
// ---------------------------------------------------------------------------
@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TacticalCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Add Member Bottom Sheet
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberBottomSheet(
    initial: FamilyMemberEntity?,
    onSave: (FamilyMemberEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name            by remember { mutableStateOf(initial?.name ?: "") }
    var relationship    by remember { mutableStateOf(initial?.relationship ?: "") }
    var bloodGroup      by remember { mutableStateOf(initial?.bloodGroup ?: "") }
    var allergies       by remember { mutableStateOf(initial?.allergies ?: "") }
    var medications     by remember { mutableStateOf(initial?.medications ?: "") }
    var contactName     by remember { mutableStateOf(initial?.emergencyContactName ?: "") }
    var contactPhone    by remember { mutableStateOf(initial?.emergencyContactPhone ?: "") }
    var notes           by remember { mutableStateOf(initial?.notes ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = if (initial == null) "ADD FAMILY MEMBER" else "EDIT ${initial.name.uppercase()}",
                onDismiss = onDismiss
            )
            VaultTextField("Full Name *", name, { name = it })
            VaultTextField("Relationship (e.g. Spouse, Child)", relationship, { relationship = it })
            VaultTextField("Blood Group (e.g. O+)", bloodGroup, { bloodGroup = it })
            VaultTextField("Allergies (comma-separated)", allergies, { allergies = it })
            VaultTextField("Medications (comma-separated)", medications, { medications = it })
            VaultTextField("Emergency Contact Name", contactName, { contactName = it })
            VaultTextField("Emergency Contact Phone", contactPhone, { contactPhone = it }, KeyboardType.Phone)
            VaultTextField("Notes", notes, { notes = it })

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(
                        FamilyMemberEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            relationship = relationship.trim(),
                            bloodGroup = bloodGroup.trim(),
                            allergies = allergies.trim(),
                            medications = medications.trim(),
                            emergencyContactName = contactName.trim(),
                            emergencyContactPhone = contactPhone.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.background)
                Spacer(Modifier.width(8.dp))
                Text("SAVE MEMBER", color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add Meeting Point Bottom Sheet
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeetingPointBottomSheet(
    onSave: (MeetingPointEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var label       by remember { mutableStateOf("") }
    var latitude    by remember { mutableStateOf("") }
    var longitude   by remember { mutableStateOf("") }
    var directions  by remember { mutableStateOf("") }
    var priority    by remember { mutableStateOf("1") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = "ADD MEETING POINT",
                onDismiss = onDismiss
            )
            VaultTextField("Label (e.g. Primary — School Gate) *", label, { label = it })
            VaultTextField("Latitude", latitude, { latitude = it }, KeyboardType.Decimal)
            VaultTextField("Longitude", longitude, { longitude = it }, KeyboardType.Decimal)
            VaultTextField("Plain-text directions", directions, { directions = it })
            VaultTextField("Priority (1=Primary, 2=Secondary, 3=Tertiary)", priority, { priority = it }, KeyboardType.Number)

            Button(
                onClick = {
                    if (label.isBlank()) return@Button
                    onSave(
                        MeetingPointEntity(
                            label = label.trim(),
                            latitude = latitude.toDoubleOrNull() ?: 0.0,
                            longitude = longitude.toDoubleOrNull() ?: 0.0,
                            plainTextDirections = directions.trim(),
                            priority = priority.toIntOrNull() ?: 1
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.background)
                Spacer(Modifier.width(8.dp))
                Text("SAVE POINT", color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared styled text field
// ---------------------------------------------------------------------------
@Composable
private fun VaultTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            focusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            cursorColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun SheetHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        TacticalIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            onClick = onDismiss,
            containerSize = 32.dp,
            iconSize = 16.dp
        )
    }
}
