package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SkPlayerApplication
import com.example.audio.EqualizerBand
import com.example.audio.EqualizerPreset
import com.example.audio.RepeatMode
import com.example.audio.SkAudioEngine
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.UserProfile
import com.example.data.model.VipKey
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SkPlayerApplication
    val repository = app.musicRepository
    val audioEngine = SkAudioEngine(application)

    // UI Tab Navigation: 0 = Home, 1 = Search, 2 = Playlists, 3 = Local (Offline), 4 = Profile
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Fullscreen Player Sheet state
    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    // Active Category Filter for Home Screen
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Home Live Search Query
    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    // Search Query for Search Tab
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow("All")
    val searchCategory: StateFlow<String> = _searchCategory.asStateFlow()

    // Active Playlist View Details
    private val _activePlaylist = MutableStateFlow<Playlist?>(null)
    val activePlaylist: StateFlow<Playlist?> = _activePlaylist.asStateFlow()

    // Dialog & Sheet States
    private val _showVipModal = MutableStateFlow(false)
    val showVipModal: StateFlow<Boolean> = _showVipModal.asStateFlow()

    private val _showEqualizerSheet = MutableStateFlow(false)
    val showEqualizerSheet: StateFlow<Boolean> = _showEqualizerSheet.asStateFlow()

    private val _showAudioQualityDialog = MutableStateFlow(false)
    val showAudioQualityDialog: StateFlow<Boolean> = _showAudioQualityDialog.asStateFlow()

    private val _selectedAudioQuality = MutableStateFlow("320 kbps (Ultra HD)")
    val selectedAudioQuality: StateFlow<String> = _selectedAudioQuality.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _selectedSongForOptions = MutableStateFlow<Song?>(null)
    val selectedSongForOptions: StateFlow<Song?> = _selectedSongForOptions.asStateFlow()

    private val _showAddToPlaylistDialog = MutableStateFlow(false)
    val showAddToPlaylistDialog: StateFlow<Boolean> = _showAddToPlaylistDialog.asStateFlow()

    private val _songForPlaylist = MutableStateFlow<Song?>(null)
    val songForPlaylist: StateFlow<Song?> = _songForPlaylist.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    // Auth Loading State
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Snackbar / Toast Event Stream
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // Realtime Firebase Data Streams
    val currentUser: StateFlow<FirebaseUser?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.firebase.auth.currentUser)

    val allSongs: StateFlow<List<Song>> = repository.allCloudSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Most Played / Top Streamed Songs sorted in real-time by playCount
    val mostPlayedSongs: StateFlow<List<Song>> = allSongs.map { songs ->
        songs.sortedWith(
            compareByDescending<Song> { it.playCount }
                .thenByDescending { it.durationMs }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val vipKeys: StateFlow<List<VipKey>> = repository.allVipKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userReports: StateFlow<List<com.example.data.model.ReportLog>> = repository.userReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<com.example.data.model.AppNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deletedNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    private val _seenNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    val visibleNotifications: StateFlow<List<com.example.data.model.AppNotification>> = combine(
        notifications,
        _deletedNotificationIds
    ) { notifs, deletedIds ->
        val context = getApplication<android.app.Application>()
        notifs.filter { notif ->
            !deletedIds.contains(notif.id) && !com.example.util.NotificationHelper.isNotificationDeleted(context, notif.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = combine(
        visibleNotifications,
        _seenNotificationIds
    ) { notifs, seenIds ->
        val context = getApplication<android.app.Application>()
        notifs.count { notif ->
            !seenIds.contains(notif.id) && !com.example.util.NotificationHelper.isNotificationSeen(context, notif.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _showNotificationsDialog = MutableStateFlow(false)
    val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

    fun setShowNotificationsDialog(show: Boolean) {
        _showNotificationsDialog.value = show
        if (show) {
            markAllNotificationsAsRead()
        }
    }

    fun markAllNotificationsAsRead() {
        val current = visibleNotifications.value
        if (current.isNotEmpty()) {
            val ids = current.map { it.id }.filter { it.isNotBlank() }
            com.example.util.NotificationHelper.markAllNotificationsSeen(getApplication(), ids)
            _seenNotificationIds.value = _seenNotificationIds.value + ids
            viewModelScope.launch {
                repository.saveNotificationSeen(ids)
            }
        }
    }

    fun deleteNotification(notifId: String) {
        if (notifId.isBlank()) return
        com.example.util.NotificationHelper.deleteNotification(getApplication(), notifId)
        _deletedNotificationIds.value = _deletedNotificationIds.value + notifId
        _seenNotificationIds.value = _seenNotificationIds.value + notifId
        viewModelScope.launch {
            repository.saveNotificationDeleted(listOf(notifId))
        }
        showToast("Notification removed")
    }

    fun clearAllNotifications() {
        val current = visibleNotifications.value
        if (current.isNotEmpty()) {
            val ids = current.map { it.id }.filter { it.isNotBlank() }
            com.example.util.NotificationHelper.clearAllNotifications(getApplication(), ids)
            _deletedNotificationIds.value = _deletedNotificationIds.value + ids
            _seenNotificationIds.value = _seenNotificationIds.value + ids
            viewModelScope.launch {
                repository.saveNotificationDeleted(ids)
            }
            showToast("All notifications cleared")
        }
    }

    fun markNotificationRead(notifId: String) {
        com.example.util.NotificationHelper.markNotificationSeen(getApplication(), notifId)
        _seenNotificationIds.value = _seenNotificationIds.value + notifId
        viewModelScope.launch {
            repository.saveNotificationSeen(listOf(notifId))
        }
    }

    // Realtime synced Current Playing Song with immediate favorite status
    val currentSong: StateFlow<Song?> = combine(
        audioEngine.currentSong,
        repository.favoriteIds.onStart { emit(emptySet()) }
    ) { song, favIds ->
        if (song == null) null
        else {
            val isFav = favIds.contains(song.id)
            song.copy(isFavorite = isFav)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), audioEngine.currentSong.value)

    val playlistQueue: StateFlow<List<Song>> = audioEngine.playlistQueueFlow

    val allPlaylistSongIds: StateFlow<Set<String>> = repository.allPlaylistSongIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Dynamically available categories extracted purely from live Firebase Firestore 'categories' and songs
    val dynamicCategories: StateFlow<List<String>> = combine(
        repository.allCategories,
        allSongs
    ) { fbCats, songs ->
        val songCats = mutableSetOf<String>()
        songs.forEach { song ->
            song.getAllCategoriesAndTags().forEach { cat ->
                val trimmed = cat.trim()
                if (trimmed.isNotBlank() && !trimmed.equals("All", ignoreCase = true) && !trimmed.equals("General", ignoreCase = true) && !trimmed.equals("Music", ignoreCase = true)) {
                    songCats.add(trimmed)
                }
            }
        }
        val cleanFbCats = fbCats.filter {
            val t = it.trim()
            t.isNotBlank() && !t.equals("All", ignoreCase = true) && !t.equals("General", ignoreCase = true) && !t.equals("Music", ignoreCase = true)
        }
        
        // Deduplicate using a map to ensure case-insensitive uniqueness (keeps first encountered casing)
        val uniqueMap = mutableMapOf<String, String>()
        (cleanFbCats + songCats).forEach { cat ->
            val key = cat.lowercase()
            if (!uniqueMap.containsKey(key)) {
                uniqueMap[key] = cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
        
        val combined = uniqueMap.values.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        if (combined.isEmpty()) listOf("All") else listOf("All") + combined
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // Local / Device audio state
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _isScanningLocal = MutableStateFlow(false)
    val isScanningLocal: StateFlow<Boolean> = _isScanningLocal.asStateFlow()

    private fun songMatchesQuery(song: Song, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        // Single search term check
        val inTitle = song.title.lowercase().contains(q)
        val inArtist = song.artist.lowercase().contains(q)
        val inAlbum = song.album.lowercase().contains(q)
        val inCategory = song.category.lowercase().contains(q)
        val inTags = song.tags.any { it.lowercase().contains(q) }
        if (inTitle || inArtist || inAlbum || inCategory || inTags) return true

        // Multi-word keyword matching (e.g. if user types "anirudh master" or related terms)
        val tokens = q.split(" ").filter { it.isNotBlank() }
        if (tokens.size > 1) {
            val combinedText = "${song.title} ${song.artist} ${song.album} ${song.category} ${song.tags.joinToString(" ")}".lowercase()
            return tokens.all { token -> combinedText.contains(token) }
        }
        return false
    }

    // Filtered songs for Home screen (category + homeSearchQuery matching title, artist, album, category & tags)
    val homeFilteredSongs: StateFlow<List<Song>> = combine(allSongs, selectedCategory, _homeSearchQuery) { songs, category, query ->
        val isCatAll = category.isBlank() || category.equals("All", ignoreCase = true)
        val isQueryBlank = query.isBlank()
        if (isCatAll && isQueryBlank) {
            songs
        } else {
            songs.filter { song ->
                val matchCat = if (isCatAll) true else song.hasCategory(category)
                val matchQuery = if (isQueryBlank) true else songMatchesQuery(song, query)
                matchCat && matchQuery
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Filtered Songs (including tags search)
    val searchResults: StateFlow<List<Song>> = combine(allSongs, searchQuery, searchCategory) { songs, query, cat ->
        val isCatAll = cat.isBlank() || cat.equals("All", ignoreCase = true)
        val isQueryBlank = query.isBlank()
        if (isCatAll && isQueryBlank) {
            songs
        } else {
            songs.filter { song ->
                val matchCat = if (isCatAll) true else song.hasCategory(cat)
                val matchQuery = if (isQueryBlank) true else songMatchesQuery(song, query)
                matchCat && matchQuery
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Songs for currently active playlist
    private val _activePlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val activePlaylistSongs: StateFlow<List<Song>> = _activePlaylistSongs.asStateFlow()

    private val _randomCategory = MutableStateFlow<String?>(null)
    val randomCategory: StateFlow<String?> = _randomCategory.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultData()
        }
        viewModelScope.launch {
            dynamicCategories.collect { cats ->
                if (_randomCategory.value == null && cats.isNotEmpty()) {
                    val nonAll = cats.filter { !it.equals("All", ignoreCase = true) }
                    _randomCategory.value = nonAll.shuffled().firstOrNull()
                }
            }
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (!profile.isProActive) {
                    audioEngine.toggleGoldenBass(false)
                    audioEngine.set8dAudio(false)
                }
            }
        }
        viewModelScope.launch {
            repository.userNotificationsState.collect { (remoteSeen, remoteDeleted) ->
                if (remoteSeen.isNotEmpty()) {
                    _seenNotificationIds.value = _seenNotificationIds.value + remoteSeen
                }
                if (remoteDeleted.isNotEmpty()) {
                    _deletedNotificationIds.value = _deletedNotificationIds.value + remoteDeleted
                }
            }
        }
        viewModelScope.launch {
            notifications.collect { notifs ->
                val deleted = _deletedNotificationIds.value
                for (n in notifs) {
                    if (!deleted.contains(n.id)) {
                        com.example.util.NotificationHelper.showAdminNotification(getApplication(), n)
                    }
                }
            }
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setHomeSearchQuery(query: String) {
        _homeSearchQuery.value = query
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategory(category: String) {
        _searchCategory.value = category
    }

    fun playSongFromList(songs: List<Song>, song: Song) {
        val user = userProfile.value
        if (song.isVipOnly && !user.isProActive) {
            _showVipModal.value = true
            showToast("This track is VIP Pro exclusive. Activate a VIP Key in Firebase to stream.")
            return
        }
        val targetList = if (songs.any { it.id == song.id }) {
            songs
        } else {
            val all = allSongs.value
            if (all.any { it.id == song.id }) all else listOf(song) + songs
        }
        val index = targetList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        audioEngine.playQueue(targetList, index)
        viewModelScope.launch {
            repository.recordSongPlay(song.id)
        }
    }

    fun togglePlayPause() {
        val current = audioEngine.currentSong.value
        if (current == null) {
            val list = allSongs.value
            if (list.isNotEmpty()) {
                playSongFromList(list, list.first())
            }
        } else {
            audioEngine.togglePlayPause()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
            val action = if (song.isFavorite) "Removed from Liked Songs" else "Added to Liked Songs ❤️"
            _uiEvent.emit(action)
        }
    }

    private var playlistSongsJob: kotlinx.coroutines.Job? = null

    fun openPlaylist(playlist: Playlist) {
        _activePlaylist.value = playlist
        playlistSongsJob?.cancel()
        playlistSongsJob = viewModelScope.launch {
            if (playlist.id == "favorites_smart_pl" || playlist.id == "favorites" || playlist.name.contains("Liked", ignoreCase = true)) {
                favoriteSongs.collect { songs ->
                    _activePlaylistSongs.value = songs
                }
            } else {
                repository.getSongsForPlaylist(playlist.id).collect { songs ->
                    _activePlaylistSongs.value = songs
                }
            }
        }
    }

    fun closePlaylist() {
        playlistSongsJob?.cancel()
        _activePlaylist.value = null
        _activePlaylistSongs.value = emptyList()
    }

    fun createPlaylist(name: String, coverUrl: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = repository.createPlaylist(name, coverUrl)
            result.onSuccess { id ->
                _showCreatePlaylistDialog.value = false
                _uiEvent.emit("Playlist \"$name\" synced to Firebase!")
                val songToAdd = _selectedSongForOptions.value
                if (songToAdd != null) {
                    repository.addSongToPlaylist(id, songToAdd.id)
                    _uiEvent.emit("Added \"${songToAdd.title}\" to playlist")
                }
            }.onFailure {
                _uiEvent.emit("Failed to create playlist: ${it.message}")
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            closePlaylist()
            _uiEvent.emit("Playlist deleted from Firebase")
        }
    }

    fun addSongToExistingPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.id)
            _showAddToPlaylistDialog.value = false
            _uiEvent.emit("Added \"${song.title}\" to playlist in Firebase")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _uiEvent.emit("Song removed from playlist")
        }
    }


    fun submitReport(songId: String, songTitle: String, message: String) {
        viewModelScope.launch {
            val result = repository.submitReport(songId, songTitle, message)
            if (result.isSuccess) {
                showToast("Your report reached successfully")
            } else {
                showToast("Failed to submit report")
            }
        }
    }

    private val _showReportLog = MutableStateFlow(false)
    val showReportLog: StateFlow<Boolean> = _showReportLog.asStateFlow()

    private val _longPressedSong = MutableStateFlow<Song?>(null)
    val longPressedSong: StateFlow<Song?> = _longPressedSong.asStateFlow()

    private val _songForReport = MutableStateFlow<Song?>(null)
    val songForReport: StateFlow<Song?> = _songForReport.asStateFlow()

    fun setLongPressedSong(song: Song?) {
        _longPressedSong.value = song
    }

    fun setShowReportLog(show: Boolean) {
        _showReportLog.value = show
    }

    fun setSongForReport(song: Song?) {
        _songForReport.value = song
    }

    fun activateVipKey(key: String) {
        viewModelScope.launch {
            val result = repository.activateVipKey(key)
            result.onSuccess { msg ->
                _showVipModal.value = false
                _uiEvent.emit("🌟 $msg")
            }.onFailure { err ->
                _uiEvent.emit("❌ ${err.message}")
            }
        }
    }

    fun activateFreeTrial() {
        viewModelScope.launch {
            val result = repository.activateFreeTrial()
            result.onSuccess { msg ->
                _uiEvent.emit("🌟 $msg")
            }.onFailure { err ->
                _uiEvent.emit("❌ ${err.message}")
            }
        }
    }

    // --- FIREBASE AUTH ACTIONS ---
    fun loginWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            showToast("Please enter both email and password")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.firebase.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            result.onSuccess {
                _uiEvent.emit("Welcome back, ${it.email}!")
            }.onFailure {
                _uiEvent.emit("Login failed: ${it.message}")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.firebase.signInWithGoogle(context)
            _isAuthLoading.value = false
            result.onSuccess {
                _uiEvent.emit("Google Sign-In successful! Welcome, ${it.displayName ?: it.email}!")
            }.onFailure {
                if (it.message?.contains("cancel", ignoreCase = true) != true) {
                    _uiEvent.emit("Google Sign-In: ${it.message}")
                }
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, displayName: String) {
        if (email.isBlank() || pass.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.firebase.signUpWithEmail(email, pass, displayName)
            _isAuthLoading.value = false
            result.onSuccess {
                _uiEvent.emit("Account created & synced to Firebase!")
            }.onFailure {
                _uiEvent.emit("Registration failed: ${it.message}")
            }
        }
    }

    fun quickDemoLogin() {
        val demoEmail = "demo_user@skplayer.app"
        val demoPass = "SkPlayer#2026"
        viewModelScope.launch {
            _isAuthLoading.value = true
            val loginRes = repository.firebase.signInWithEmail(demoEmail, demoPass)
            if (loginRes.isSuccess) {
                _isAuthLoading.value = false
                _uiEvent.emit("Logged in as ${loginRes.getOrNull()?.email}")
            } else {
                // Auto-register demo account if not exists
                val regRes = repository.firebase.signUpWithEmail(demoEmail, demoPass, "Sk Player Explorer")
                _isAuthLoading.value = false
                if (regRes.isSuccess) {
                    _uiEvent.emit("Demo account created & logged in!")
                } else {
                    _uiEvent.emit("Login error: ${loginRes.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun signOutUser() {
        togglePlayPause()
        audioEngine.toggleGoldenBass(false)
        audioEngine.set8dAudio(false)
        repository.firebase.signOut()
        showToast("Signed out of Firebase")
    }

    fun setShowAudioQualityDialog(show: Boolean) {
        _showAudioQualityDialog.value = show
    }

    fun setSelectedAudioQuality(quality: String) {
        _selectedAudioQuality.value = quality
        showToast("Audio Engine Quality set to $quality")
    }

    fun addPickedAudioUris(uris: List<android.net.Uri>, context: Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanningLocal.value = true
            val currentList = _localSongs.value.toMutableList()
            var addedCount = 0

            for (uri in uris) {
                try {
                    val metadataRetriever = android.media.MediaMetadataRetriever()
                    try {
                        metadataRetriever.setDataSource(context, uri)
                    } catch (e: Exception) {
                        // ignore and use fallback
                    }

                    var title = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    val album = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Local Music"
                    val durationStr = metadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 210000L

                    metadataRetriever.release()

                    if (title.isNullOrBlank()) {
                        // Resolve display name from content resolver
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    title = it.getString(nameIndex)
                                }
                            }
                        }
                    }

                    if (title.isNullOrBlank()) {
                        title = uri.lastPathSegment ?: "Audio Track ${currentList.size + 1}"
                    }

                    val cleanTitle = title!!.replace(Regex("\\.[a-zA-Z0-9]+$"), "")
                    val songId = "local_file_" + uri.toString().hashCode()

                    if (currentList.none { it.id == songId || it.localUri == uri.toString() }) {
                        val newSong = Song(
                            id = songId,
                            title = cleanTitle,
                            artist = artist,
                            album = album,
                            audioUrl = uri.toString(),
                            imageUrl = "",
                            localUri = uri.toString(),
                            durationMs = durationMs,
                            category = "Local",
                            isLocal = true,
                            waveformPoints = listOf(0.4f, 0.7f, 0.5f, 0.8f, 0.6f, 0.9f, 0.5f, 0.7f)
                        )
                        currentList.add(newSong)
                        addedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error loading picked audio uri", e)
                }
            }

            _localSongs.value = currentList
            _isScanningLocal.value = false
            _uiEvent.emit("Added $addedCount local track(s)")
        }
    }

    fun scanSelectedFolder(treeUri: android.net.Uri, context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isScanningLocal.value = true
            val currentList = _localSongs.value.toMutableList()
            var addedCount = 0

            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

                val projection = arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val documentId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: ""
                        val mime = cursor.getString(mimeCol) ?: ""

                        val isAudio = mime.startsWith("audio/") ||
                                name.endsWith(".mp3", ignoreCase = true) ||
                                name.endsWith(".m4a", ignoreCase = true) ||
                                name.endsWith(".wav", ignoreCase = true) ||
                                name.endsWith(".aac", ignoreCase = true) ||
                                name.endsWith(".flac", ignoreCase = true)

                        if (isAudio) {
                            val documentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                            val songId = "local_folder_" + documentUri.toString().hashCode()
                            val cleanTitle = name.replace(Regex("\\.[a-zA-Z0-9]+$"), "")

                            if (currentList.none { it.id == songId || it.localUri == documentUri.toString() }) {
                                val newSong = Song(
                                    id = songId,
                                    title = cleanTitle,
                                    artist = "Folder Audio",
                                    album = "Imported Folder",
                                    audioUrl = documentUri.toString(),
                                    imageUrl = "",
                                    localUri = documentUri.toString(),
                                    durationMs = 210000L,
                                    category = "Local",
                                    isLocal = true,
                                    waveformPoints = listOf(0.3f, 0.8f, 0.6f, 0.9f, 0.5f, 0.7f, 0.4f)
                                )
                                currentList.add(newSong)
                                addedCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error scanning folder: ${e.message}", e)
            }

            _localSongs.value = currentList
            _isScanningLocal.value = false
            _uiEvent.emit("Imported $addedCount audio file(s) from folder")
        }
    }

    fun clearLocalSongs() {
        _localSongs.value = emptyList()
        showToast("Cleared selected local files")
    }

    fun scanLocalAudio() {
        viewModelScope.launch {
            _isScanningLocal.value = true
            repository.scanDeviceAudio().collect { localList ->
                _localSongs.value = localList
                _isScanningLocal.value = false
                _uiEvent.emit("Found ${localList.size} device audio track(s)")
            }
        }
    }

    fun showSongOptions(song: Song) {
        _selectedSongForOptions.value = song
    }

    fun closeSongOptions() {
        _selectedSongForOptions.value = null
    }

    fun setShowVipModal(show: Boolean) {
        _showVipModal.value = show
    }

    fun setShowEqualizerSheet(show: Boolean) {
        _showEqualizerSheet.value = show
    }

    fun setShowCreatePlaylistDialog(show: Boolean) {
        _showCreatePlaylistDialog.value = show
    }

    fun setShowAddToPlaylistDialog(show: Boolean, song: Song? = null) {
        _showAddToPlaylistDialog.value = show
        if (show && song != null) {
            _songForPlaylist.value = song
        } else if (!show) {
            _songForPlaylist.value = null
        }
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _showSleepTimerDialog.value = show
    }

    val bassLevel: StateFlow<Float> = audioEngine.bassLevel

    fun setBassLevel(level: Float) {
        if (!userProfile.value.isProActive) {
            _showVipModal.value = true
            showToast("Adjusting Bass Booster requires VIP Pro activation.")
            return
        }
        audioEngine.setBassLevel(level)
    }

    fun toggleGoldenBass(enabled: Boolean) {
        if (enabled && !userProfile.value.isProActive) {
            _showVipModal.value = true
            showToast("Golden Bass Booster requires VIP Pro activation.")
            audioEngine.toggleGoldenBass(false)
            return
        }
        audioEngine.toggleGoldenBass(enabled)
    }

    fun toggle8dAudio(enabled: Boolean) {
        if (enabled && !userProfile.value.isProActive) {
            _showVipModal.value = true
            showToast("8D Spatial Surround Audio requires VIP Pro activation.")
            audioEngine.set8dAudio(false)
            return
        }
        audioEngine.set8dAudio(enabled)
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(message)
        }
    }
}
