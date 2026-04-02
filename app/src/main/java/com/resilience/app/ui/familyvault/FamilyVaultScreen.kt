package com.resilience.app.ui.familyvault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import com.resilience.app.ui.theme.SafeReachDarkGray

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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Family Safety Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Encrypted · Local only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFF1B5E20).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Encrypted",
                                color = Color(0xFF66BB6A),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // -------- Family Members Section --------
            item {
                SectionHeader(
                    title = "Family Members",
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

            items(uiState.members, key = { it.id }) { member ->
                FamilyMemberCard(
                    member = member,
                    onEdit = { viewModel.openAddMemberSheet(member) },
                    onDelete = { viewModel.deleteMember(member) }
                )
            }

            // -------- Meeting Points Section --------
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    title = "Meeting Points",
                    icon = Icons.Default.LocationOn,
                    onAdd = { viewModel.openAddMeetingPointSheet() }
                )
            }

            if (uiState.meetingPoints.isEmpty()) {
                item {
                    EmptyState(
                        message = "No meeting points set yet.",
                        icon = Icons.Default.AddLocation
                    )
                }
            }

            items(uiState.meetingPoints, key = { it.id }) { point ->
                MeetingPointCard(
                    point = point,
                    onDelete = { viewModel.deleteMeetingPoint(point) }
                )
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

// ---------------------------------------------------------------------------
// Section header with + button
// ---------------------------------------------------------------------------
@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.onBackground)
        }
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
    val avatarColor = remember(member.id) {
        val colors = listOf(0xFFE53935, 0xFF8E24AA, 0xFF1E88E5, 0xFF43A047, 0xFFFF8F00)
        Color(colors[(member.id % colors.size).toInt()])
    }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = avatarColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(member.relationship, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.08f))
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
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", color = Color(0xFFEF5350), style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = Color.White, style = MaterialTheme.typography.labelMedium)
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
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Meeting Point Card
// ---------------------------------------------------------------------------
@Composable
private fun MeetingPointCard(point: MeetingPointEntity, onDelete: () -> Unit) {
    val priorityColor = when (point.priority) {
        1 -> Color(0xFF43A047)
        2 -> Color(0xFFFF8F00)
        else -> Color(0xFFE53935)
    }
    val priorityLabel = when (point.priority) {
        1 -> "Primary"
        2 -> "Secondary"
        else -> "Tertiary"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = priorityColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = priorityColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(point.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Surface(color = priorityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(priorityLabel, color = priorityColor, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.5f, %.5f".format(point.latitude, point.longitude),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall
                )
                if (point.plainTextDirections.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(point.plainTextDirections, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty State
// ---------------------------------------------------------------------------
@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = SafeReachDarkGray.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SafeReachDarkGray) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (initial == null) "Add Family Member" else "Edit ${initial.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Save Member", color = Color.Black, fontWeight = FontWeight.Bold)
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SafeReachDarkGray) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Meeting Point", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Save Point", color = Color.Black, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            focusedLabelColor = Color.White.copy(alpha = 0.7f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
            cursorColor = Color.White
        )
    )
}
