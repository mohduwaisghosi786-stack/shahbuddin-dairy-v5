package com.shahbuddindairy.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.ui.components.AppTopHeader
import com.shahbuddindairy.app.ui.components.EmptyState
import com.shahbuddindairy.app.ui.theme.DairyGreenDark
import com.shahbuddindairy.app.ui.theme.StatusAmber
import com.shahbuddindairy.app.ui.theme.StatusRed
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateToCustomerKhata: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDraft by viewModel.activeDraft.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var deleteConfirmNoteId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP HEADER
            AppTopHeader(
                title = "डेयरी नोट्स (Notes)",
                subtitle = "कुल ${uiState.allNotes.size} नोट्स • वॉइस व मीडिया सुरक्षित"
            )

            // 2. SEARCH BAR
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search title, content, customer, tags...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. CATEGORY FILTER CHIPS
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All (${uiState.allNotes.size})",
                    "CUSTOMER" to "Customer (${uiState.allNotes.count { it.note.customerId > 0 }})",
                    "GENERAL" to "General (${uiState.allNotes.count { it.note.customerId == 0L }})",
                    "PINNED" to "Pinned 📌 (${uiState.allNotes.count { it.note.isPinned }})",
                    "VOICE" to "Voice 🎙️ (${uiState.allNotes.count { it.note.audioUri.isNotBlank() }})",
                    "MEDIA" to "Photos 📷 (${uiState.allNotes.count { it.note.mediaUris.isNotBlank() }})",
                    "REMINDERS" to "Reminders ⏰ (${uiState.allNotes.count { it.note.reminderDateTime.isNotBlank() }})"
                ).forEach { (catKey, label) ->
                    val isSel = uiState.activeFilterCategory == catKey
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.onFilterCategoryChanged(catKey) },
                        label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // 4. TAGS FILTER ROW (IF AVAILABLE)
            if (uiState.allTags.isNotEmpty()) {
                val tagScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tagScrollState)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.allTags.forEach { tag ->
                        val isSel = uiState.selectedTagFilter == tag
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSel) colorScheme.primary else colorScheme.outlineVariant),
                            modifier = Modifier.clickable { viewModel.onTagFilterSelected(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 5. NOTES LIST
            if (uiState.filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "कोई नोट नहीं मिला (No Notes)",
                        message = if (uiState.searchQuery.isNotBlank()) "No notes match '${uiState.searchQuery}'" else "Tap '+ New Note' to create your first note or voice memo."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredNotes, key = { it.note.id }) { item ->
                        NoteCardItem(
                            item = item,
                            isPlaying = uiState.activeAudioPlayingId == item.note.id && uiState.isPlayingAudio,
                            playbackProgress = if (uiState.activeAudioPlayingId == item.note.id) uiState.audioPlaybackProgress else 0f,
                            onPlayAudio = { viewModel.playAudio(item.note.id, item.note.audioUri) },
                            onTogglePin = { viewModel.togglePin(item.note) },
                            onEdit = { viewModel.editNote(item.note) },
                            onDelete = { deleteConfirmNoteId = item.note.id },
                            onCustomerClick = {
                                if (item.note.customerId > 0) {
                                    onNavigateToCustomerKhata(item.note.customerId)
                                }
                            },
                            onImageClick = { previewImagePath = it }
                        )
                    }
                }
            }
        }

        // FLOATING ACTION BUTTONS (+ New Note)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.startNewNote(0L) },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Note", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("नया नोट", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    deleteConfirmNoteId?.let { noteId ->
        AlertDialog(
            onDismissRequest = { deleteConfirmNoteId = null },
            title = { Text("नोट हटाएं (Delete Note)", fontWeight = FontWeight.Bold) },
            text = { Text("क्या आप इस नोट और इसके अटैचमेंट्स को हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(noteId)
                        deleteConfirmNoteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("हटाएं (Delete)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmNoteId = null }) {
                    Text("रद्द करें")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    // PHOTO PREVIEW DIALOG
    previewImagePath?.let { path ->
        Dialog(onDismissRequest = { previewImagePath = null }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bmp = remember(path) {
                        try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("चित्र लोड करने में असमर्थ", color = StatusRed)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { previewImagePath = null },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("बंद करें")
                    }
                }
            }
        }
    }

    // ACTIVE NOTE DRAFT EDITOR DIALOG
    activeDraft?.let { draft ->
        NoteEditorDialog(
            draft = draft,
            customers = uiState.customers,
            isRecording = uiState.isRecording,
            recordingDurationSec = uiState.recordingDurationSec,
            onDismiss = { viewModel.dismissDraft() },
            onSave = { viewModel.saveActiveDraft() },
            onTitleChange = { viewModel.updateDraftTitle(it) },
            onContentChange = { viewModel.updateDraftContent(it) },
            onCustomerChange = { viewModel.updateDraftCustomer(it) },
            onCategoryChange = { viewModel.updateDraftCategory(it) },
            onTagsChange = { viewModel.updateDraftTags(it) },
            onPriorityChange = { viewModel.updateDraftPriority(it) },
            onReminderChange = { viewModel.updateDraftReminder(it) },
            onTogglePin = { viewModel.toggleDraftPinned() },
            onAddMediaUri = { uri -> viewModel.addMediaUriToDraft(uri, context) },
            onRemoveMedia = { path -> viewModel.removeMediaFromDraft(path) },
            onStartRecording = { viewModel.startVoiceRecording(context) },
            onStopRecording = { viewModel.stopVoiceRecording() },
            onCancelRecording = { viewModel.cancelRecording() },
            onRemoveAudio = { viewModel.removeAudioFromDraft() }
        )
    }
}

@Composable
fun NoteCardItem(
    item: NoteWithCustomer,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayAudio: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCustomerClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val note = item.note
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (note.isPinned) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) colorScheme.primaryContainer.copy(alpha = 0.25f) else colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Customer Badge / Category + Pin + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (item.customer != null) {
                        Surface(
                            color = colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onCustomerClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "#${item.customer.id} ${item.customer.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "सामान्य नोट (General)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (note.priority != "Normal") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (note.priority == "High") StatusRed.copy(alpha = 0.15f) else Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = note.priority,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (note.priority == "High") StatusRed else Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Title (if present)
            if (note.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = colorScheme.onSurface
                )
            }

            // Note Content
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colorScheme.onSurface
                )
            }

            // VOICE NOTE AUDIO PLAYER BAR
            if (note.audioUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🎙️ Voice Memo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                val durSec = (note.audioDurationMs / 1000).toInt()
                                Text("%d:%02d".format(durSec / 60, durSec % 60), fontSize = 10.5.sp, color = colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { playbackProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = colorScheme.primary,
                                trackColor = colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // ATTACHED IMAGES ROW
            if (note.mediaUris.isNotBlank()) {
                val mediaPaths = note.mediaUris.split("|").filter { it.isNotBlank() }
                if (mediaPaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        mediaPaths.forEach { path ->
                            val bmp = remember(path) {
                                try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onImageClick(path) }
                                )
                            }
                        }
                    }
                }
            }

            // REMINDER OR TAGS FOOTER
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags
                if (note.tags.isNotBlank()) {
                    Text(
                        text = note.tags.split(",", " ").filter { it.isNotBlank() }.joinToString(" ") { "#${it.removePrefix("#")}" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Reminder / Timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.reminderDateTime.isNotBlank()) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = note.reminderDateTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = note.createdAt.take(10),
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    draft: NoteDraft,
    customers: List<CustomerEntity>,
    isRecording: Boolean,
    recordingDurationSec: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCustomerChange: (Long) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onReminderChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onAddMediaUri: (Uri) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRemoveAudio: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var showCustomerPicker by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddMediaUri(it) }
    }

    // Camera Capture Launcher (Takes high-res photo via temporary file)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            onAddMediaUri(tempCameraUri!!)
        }
    }

    // Speech-To-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val spokenMatches = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenMatches?.firstOrNull() ?: ""
            if (recognizedText.isNotBlank()) {
                val updatedContent = if (draft.content.isBlank()) recognizedText else "${draft.content} $recognizedText"
                onContentChange(updatedContent)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (draft.content.isBlank() && draft.title.isBlank()) "नया नोट (New Note)" else "नोट संपादित करें (Edit Note)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (draft.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (draft.isPinned) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer Selector Field
                Box {
                    val selCustomer = customers.find { it.id == draft.customerId }
                    OutlinedButton(
                        onClick = { showCustomerPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selCustomer != null) "ग्राहक: #${selCustomer.id} ${selCustomer.name} (${selCustomer.mohalla})" else "लिंक्ड ग्राहक: कोई नहीं (General Note)",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    DropdownMenu(
                        expanded = showCustomerPicker,
                        onDismissRequest = { showCustomerPicker = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("कोई ग्राहक नहीं (General Note)") },
                            onClick = {
                                onCustomerChange(0L)
                                showCustomerPicker = false
                            }
                        )
                        HorizontalDivider()
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("#${c.id} ${c.name} (${c.mohalla})") },
                                onClick = {
                                    onCustomerChange(c.id)
                                    showCustomerPicker = false
                                }
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = { Text("शीर्षक (Title / Summary)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Multiline Content Input with Speech Dictation Button
                Box {
                    OutlinedTextField(
                        value = draft.content,
                        onValueChange = onContentChange,
                        label = { Text("नोट विवरण (Notes in Hindi / English)", fontSize = 12.sp) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Speech Dictation Button
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए (Speak your note)...")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice recognizer not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                // Voice Recording Attachment Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("वॉइस रिकॉर्डिंग (Audio Memo)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                            if (draft.audioUri.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val durSec = (draft.audioDurationMs / 1000).toInt()
                                    Text("✓ रिकॉर्डेड (%d:%02d)".format(durSec / 60, durSec % 60), fontSize = 11.sp, color = DairyGreenDark, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = onRemoveAudio, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = StatusRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isRecording) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StatusRed)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("रिकॉर्डिंग चल रही है... (%d:%02d)".format(recordingDurationSec / 60, recordingDurationSec % 60), color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onStopRecording,
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("रोकें", fontSize = 11.sp)
                                }
                            }
                        } else if (draft.audioUri.isBlank()) {
                            Button(
                                onClick = onStartRecording,
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("आवाज़ रिकॉर्ड करें (Record Audio)", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Photo Attachments Box
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("फ़ोटो संलग्न करें (Photos)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val tempFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                        tempCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Camera launch error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कैमरा", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("गैलरी", fontSize = 11.sp)
                            }
                        }
                    }

                    if (draft.mediaUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            draft.mediaUris.forEach { path ->
                                Box(modifier = Modifier.size(60.dp)) {
                                    val bmp = remember(path) {
                                        try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveMedia(path) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Tags & Priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft.tags,
                        onValueChange = onTagsChange,
                        label = { Text("टैग (Tags: due, urgent)", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    var showPriorityMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(0.9f)) {
                        OutlinedButton(
                            onClick = { showPriorityMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("प्राथमिकता: ${draft.priority}", fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = showPriorityMenu,
                            onDismissRequest = { showPriorityMenu = false }
                        ) {
                            listOf("Low", "Normal", "High").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        onPriorityChange(p)
                                        showPriorityMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Reminder Date / Time Input
                OutlinedTextField(
                    value = draft.reminderDateTime,
                    onValueChange = onReminderChange,
                    label = { Text("रिमाइंडर (e.g. 25 Oct 2026, 08:00 AM)", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("सुरक्षित करें (Save Note)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("रद्द करें")
            }
        },
        shape = RoundedCornerShape(14.dp),
        containerColor = colorScheme.surface
    )
}
