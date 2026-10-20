package com.shahbuddindairy.app.ui.screens

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahbuddindairy.app.DairyApplication
import com.shahbuddindairy.app.audio.SoundEffectManager
import com.shahbuddindairy.app.audio.SoundEvent
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.CustomerNoteEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NoteWithCustomer(
    val note: CustomerNoteEntity,
    val customer: CustomerEntity?
)

data class NotesUiState(
    val allNotes: List<NoteWithCustomer> = emptyList(),
    val filteredNotes: List<NoteWithCustomer> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val activeFilterCategory: String = "ALL", // ALL, GENERAL, CUSTOMER, PINNED, REMINDERS, VOICE, MEDIA
    val searchQuery: String = "",
    val selectedTagFilter: String? = null,
    val allTags: List<String> = emptyList(),
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val activeAudioPlayingId: String? = null,
    val isPlayingAudio: Boolean = false,
    val audioPlaybackProgress: Float = 0f,
    val isLoading: Boolean = false
)

data class NoteDraft(
    val id: String = UUID.randomUUID().toString(),
    val customerId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val category: String = "General", // General, Delivery, Payment, Preference, Voice
    val tags: String = "",
    val priority: String = "Normal", // Low, Normal, High
    val reminderDateTime: String = "",
    val mediaUris: List<String> = emptyList(),
    val audioUri: String = "",
    val audioDurationMs: Long = 0L,
    val isPinned: Boolean = false
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DairyApplication).repository
    private val soundManager = SoundEffectManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow("ALL")
    val filterCategory = _filterCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration = _recordingDuration.asStateFlow()

    private val _activePlayingNoteId = MutableStateFlow<String?>(null)
    val activePlayingNoteId = _activePlayingNoteId.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio = _isPlayingAudio.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    // Active Note Draft for Creating/Editing
    private val _activeDraft = MutableStateFlow<NoteDraft?>(null)
    val activeDraft = _activeDraft.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingTimerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackingJob: Job? = null

    val uiState: StateFlow<NotesUiState> = combine(
        repository.getAllNotes(),
        repository.getAllCustomers(),
        _searchQuery,
        _filterCategory,
        _selectedTag,
        _isRecording,
        _recordingDuration,
        _activePlayingNoteId,
        _isPlayingAudio,
        _playbackProgress
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val notes = args[0] as List<CustomerNoteEntity>
        @Suppress("UNCHECKED_CAST")
        val customers = args[1] as List<CustomerEntity>
        val query = args[2] as String
        val filter = args[3] as String
        val tagFilter = args[4] as? String
        val recording = args[5] as Boolean
        val duration = args[6] as Int
        val playingId = args[7] as? String
        val playing = args[8] as Boolean
        val progress = args[9] as Float

        val customerMap = customers.associateBy { it.id }
        val noteItems = notes.map { note ->
            NoteWithCustomer(
                note = note,
                customer = if (note.customerId > 0) customerMap[note.customerId] else null
            )
        }

        val allTags = notes.flatMap { it.tags.split(",", " ", ";") }
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val filtered = noteItems.filter { item ->
            val note = item.note
            val matchesFilter = when (filter) {
                "GENERAL" -> note.customerId == 0L
                "CUSTOMER" -> note.customerId > 0L
                "PINNED" -> note.isPinned
                "REMINDERS" -> note.reminderDateTime.isNotBlank()
                "VOICE" -> note.audioUri.isNotBlank() || note.category == "Voice"
                "MEDIA" -> note.mediaUris.isNotBlank()
                else -> true
            }

            val matchesTag = if (tagFilter == null) true else {
                note.tags.contains(tagFilter, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) true else {
                val q = query.trim()
                note.title.contains(q, ignoreCase = true) ||
                note.content.contains(q, ignoreCase = true) ||
                note.tags.contains(q, ignoreCase = true) ||
                (item.customer?.name?.contains(q, ignoreCase = true) == true) ||
                (item.customer?.mohalla?.contains(q, ignoreCase = true) == true) ||
                item.note.customerId.toString() == q
            }

            matchesFilter && matchesTag && matchesQuery
        }

        NotesUiState(
            allNotes = noteItems,
            filteredNotes = filtered,
            customers = customers,
            activeFilterCategory = filter,
            searchQuery = query,
            selectedTagFilter = tagFilter,
            allTags = allTags,
            isRecording = recording,
            recordingDurationSec = duration,
            activeAudioPlayingId = playingId,
            isPlayingAudio = playing,
            audioPlaybackProgress = progress,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotesUiState(isLoading = true)
    )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onFilterCategoryChanged(cat: String) {
        _filterCategory.value = cat
    }

    fun onTagFilterSelected(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun startNewNote(customerId: Long = 0L) {
        _activeDraft.value = NoteDraft(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            category = if (customerId > 0) "Customer" else "General"
        )
    }

    fun editNote(note: CustomerNoteEntity) {
        val mediaList = if (note.mediaUris.isBlank()) emptyList() else note.mediaUris.split("|").filter { it.isNotBlank() }
        _activeDraft.value = NoteDraft(
            id = note.id,
            customerId = note.customerId,
            title = note.title,
            content = note.content,
            category = note.category,
            tags = note.tags,
            priority = note.priority,
            reminderDateTime = note.reminderDateTime,
            mediaUris = mediaList,
            audioUri = note.audioUri,
            audioDurationMs = note.audioDurationMs,
            isPinned = note.isPinned
        )
    }

    fun dismissDraft() {
        stopAudioPlayback()
        cancelRecording()
        _activeDraft.value = null
    }

    fun updateDraftTitle(title: String) {
        _activeDraft.value = _activeDraft.value?.copy(title = title)
    }

    fun updateDraftContent(content: String) {
        _activeDraft.value = _activeDraft.value?.copy(content = content)
    }

    fun updateDraftCustomer(customerId: Long) {
        _activeDraft.value = _activeDraft.value?.copy(
            customerId = customerId,
            category = if (customerId > 0) "Customer" else "General"
        )
    }

    fun updateDraftCategory(category: String) {
        _activeDraft.value = _activeDraft.value?.copy(category = category)
    }

    fun updateDraftTags(tags: String) {
        _activeDraft.value = _activeDraft.value?.copy(tags = tags)
    }

    fun updateDraftPriority(priority: String) {
        _activeDraft.value = _activeDraft.value?.copy(priority = priority)
    }

    fun updateDraftReminder(reminder: String) {
        _activeDraft.value = _activeDraft.value?.copy(reminderDateTime = reminder)
    }

    fun toggleDraftPinned() {
        _activeDraft.value = _activeDraft.value?.let {
            it.copy(isPinned = !it.isPinned)
        }
    }

    fun addMediaUriToDraft(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaDir = File(context.filesDir, "notes_media")
                if (!mediaDir.exists()) mediaDir.mkdirs()

                val ext = "jpg"
                val targetFile = File(mediaDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    val currentList = _activeDraft.value?.mediaUris ?: emptyList()
                    _activeDraft.value = _activeDraft.value?.copy(mediaUris = currentList + targetFile.absolutePath)
                    soundManager.play(SoundEvent.ATTACHMENT_ADDED)
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Failed to copy note image: ${e.message}", e)
            }
        }
    }

    fun removeMediaFromDraft(path: String) {
        val currentList = _activeDraft.value?.mediaUris ?: return
        _activeDraft.value = _activeDraft.value?.copy(mediaUris = currentList - path)
        try {
            val f = File(path)
            if (f.exists()) f.delete()
        } catch (e: Exception) {
            // Ignore deletion error
        }
    }

    fun saveActiveDraft() {
        val draft = _activeDraft.value ?: return
        if (draft.content.isBlank() && draft.title.isBlank() && draft.audioUri.isBlank() && draft.mediaUris.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            val existing = repository.getNoteByIdDirect(draft.id)
            val createdAt = existing?.createdAt ?: nowStr

            val entity = CustomerNoteEntity(
                id = draft.id,
                customerId = draft.customerId,
                content = draft.content.trim(),
                category = draft.category,
                isPinned = draft.isPinned,
                createdAt = createdAt,
                updatedAt = nowStr,
                title = draft.title.trim(),
                tags = draft.tags.trim(),
                priority = draft.priority,
                reminderDateTime = draft.reminderDateTime,
                mediaUris = draft.mediaUris.joinToString("|"),
                audioUri = draft.audioUri,
                audioDurationMs = draft.audioDurationMs,
                isCompleted = false
            )

            repository.saveNote(entity)

            withContext(Dispatchers.Main) {
                soundManager.play(SoundEvent.NOTE_SAVED)
                soundManager.vibrate(40)
                _activeDraft.value = null
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getNoteByIdDirect(id)
            if (note != null) {
                if (note.audioUri.isNotBlank()) {
                    try { File(note.audioUri).delete() } catch (e: Exception) {}
                }
                if (note.mediaUris.isNotBlank()) {
                    note.mediaUris.split("|").forEach { path ->
                        try { File(path).delete() } catch (e: Exception) {}
                    }
                }
            }
            repository.deleteNote(id)
            withContext(Dispatchers.Main) {
                soundManager.play(SoundEvent.UNDO_COMPLETED)
            }
        }
    }

    fun togglePin(note: CustomerNoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(
                isPinned = !note.isPinned,
                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            )
            repository.saveNote(updated)
            withContext(Dispatchers.Main) {
                soundManager.vibrate(25)
            }
        }
    }

    // VOICE NOTE RECORDING
    fun startVoiceRecording(context: Context) {
        try {
            val audioDir = File(context.filesDir, "notes_audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            val recFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = recFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(recFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDuration.value = 0
            soundManager.play(SoundEvent.MIC_TOGGLE)
            soundManager.vibrate(40)

            recordingTimerJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingDuration.value += 1
                }
            }
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Failed to start audio recording: ${e.message}", e)
            _isRecording.value = false
            currentRecordingFile = null
        }
    }

    fun stopVoiceRecording() {
        try {
            recordingTimerJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false

            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                val durationMs = (_recordingDuration.value * 1000L).coerceAtLeast(1000L)
                _activeDraft.value = _activeDraft.value?.copy(
                    audioUri = file.absolutePath,
                    audioDurationMs = durationMs,
                    category = if (_activeDraft.value?.category == "General") "Voice" else _activeDraft.value?.category ?: "Voice"
                )
                soundManager.play(SoundEvent.ATTACHMENT_ADDED)
            }
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Error stopping recording: ${e.message}")
        } finally {
            _isRecording.value = false
            currentRecordingFile = null
        }
    }

    fun cancelRecording() {
        try {
            recordingTimerJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            currentRecordingFile?.delete()
            currentRecordingFile = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun removeAudioFromDraft() {
        val audioPath = _activeDraft.value?.audioUri ?: return
        try {
            File(audioPath).delete()
        } catch (e: Exception) {}
        _activeDraft.value = _activeDraft.value?.copy(audioUri = "", audioDurationMs = 0L)
    }

    // AUDIO PLAYBACK
    fun playAudio(noteId: String, audioPath: String) {
        if (_activePlayingNoteId.value == noteId && _isPlayingAudio.value) {
            pauseAudio()
            return
        }

        stopAudioPlayback()

        val file = File(audioPath)
        if (!file.exists()) return

        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }

            mediaPlayer = mp
            _activePlayingNoteId.value = noteId
            _isPlayingAudio.value = true

            mp.setOnCompletionListener {
                stopAudioPlayback()
            }

            progressTrackingJob = viewModelScope.launch {
                while (_isPlayingAudio.value) {
                    val current = mp.currentPosition.toFloat()
                    val total = mp.duration.toFloat().coerceAtLeast(1f)
                    _playbackProgress.value = (current / total).coerceIn(0f, 1f)
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Error playing audio note: ${e.message}")
            stopAudioPlayback()
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.pause()
            _isPlayingAudio.value = false
            progressTrackingJob?.cancel()
        } catch (e: Exception) {
            stopAudioPlayback()
        }
    }

    fun stopAudioPlayback() {
        try {
            progressTrackingJob?.cancel()
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        } finally {
            _activePlayingNoteId.value = null
            _isPlayingAudio.value = false
            _playbackProgress.value = 0f
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
        cancelRecording()
    }
}
