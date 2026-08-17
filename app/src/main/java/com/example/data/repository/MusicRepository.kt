package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.GuestIdentityManager
import com.example.data.local.AppDatabase
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.UserProfile
import com.example.data.model.VipKey
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepository(
    private val context: Context,
    private val database: AppDatabase,
    val firebase: FirebaseManager = FirebaseManager()
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val guestIdentityManager = GuestIdentityManager(context)

    fun getEffectiveUid(): String {
        return firebase.auth.currentUser?.uid ?: guestIdentityManager.getGuestId()
    }

    // Firebase Auth User Flow
    val currentUser: Flow<FirebaseUser?> = firebase.authStateFlow()

    // Current User Profile synced with Firebase Realtime DB
    val userProfile: Flow<UserProfile> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeUserProfile(uid).map { profile ->
            profile ?: UserProfile(
                uid = uid,
                displayName = user?.displayName ?: (user?.email?.substringBefore("@") ?: "Guest Explorer"),
                email = user?.email ?: "guest_${uid.takeLast(6)}@skedz.app",
                isPro = false,
                proExpiresAt = 0L
            )
        }
    }

    // User's Realtime Favorite Song IDs (isolated by IP for guest users)
    val favoriteIds: Flow<Set<String>> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeUserFavorites(uid)
    }

    // Realtime Cloud Songs from Firebase (with favorites)
    val allCloudSongs: Flow<List<Song>> = combine(
        firebase.observeRealtimeSongs(),
        favoriteIds
    ) { songs: List<Song>, favSet: Set<String> ->
        songs.map { song ->
            song.copy(
                isFavorite = favSet.contains(song.id)
            )
        }
    }

    // Realtime Playlists from Firebase (User Private & Isolated)
    val allPlaylists: Flow<List<Playlist>> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeUserPlaylists(uid)
    }

    // A map of all playlist IDs to their song IDs
    val allPlaylistSongIds: Flow<Set<String>> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeAllPlaylistSongIds(uid)
    }

    // Realtime Categories from Firebase (Firestore 'categories' + RTDB + Song categories)
    val allCategories: Flow<List<String>> = combine(
        firebase.observeRealtimeCategories(),
        firebase.observeRealtimeSongs()
    ) { explicitCats: List<String>, songs: List<Song> ->
        val songCats = songs.flatMap { it.getAllCategoriesAndTags() }
        (explicitCats + songCats)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) && !it.equals("General", ignoreCase = true) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    // Realtime Favorite Songs
    val favoriteSongs: Flow<List<Song>> = combine(
        allCloudSongs,
        favoriteIds
    ) { songs: List<Song>, favSet: Set<String> ->
        songs.filter { favSet.contains(it.id) }
    }

    // Realtime VIP Keys from Firebase
    val allVipKeys: Flow<List<VipKey>> = firebase.observeRealtimeVipKeys()

    // Realtime Admin Notifications from Firebase
    val allNotifications: Flow<List<com.example.data.model.AppNotification>> = firebase.observeAdminNotifications()

    // Realtime User-synced Notification State (seenIds, deletedIds)
    val userNotificationsState: Flow<Pair<Set<String>, Set<String>>> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeUserNotificationsState(uid)
    }

    suspend fun saveNotificationSeen(notifIds: List<String>) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.saveNotificationSeenToCloud(uid, notifIds)
    }

    suspend fun saveNotificationDeleted(notifIds: List<String>) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.saveNotificationDeletedToCloud(uid, notifIds)
    }

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        // Resolve guest IP asynchronously on startup
        guestIdentityManager.refreshGuestIdWithPublicIp()
    }

    // --- PLAY COUNT TRACKING IN FIREBASE ---
    suspend fun recordSongPlay(songId: String) = withContext(Dispatchers.IO) {
        if (songId.isNotBlank()) {
            firebase.incrementSongPlayCount(songId)
        }
    }

    // --- FAVORITES IN FIREBASE ---
    suspend fun toggleFavorite(songId: String, currentFavStatus: Boolean) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.toggleFavoriteInFirebase(uid, songId, currentFavStatus)
    }

    // --- PLAYLISTS IN FIREBASE (USER PRIVATE & ISOLATED) ---
    suspend fun createPlaylist(name: String, coverUrl: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        val defaultCover = if (coverUrl.isNotBlank() && !coverUrl.contains("unsplash.com", ignoreCase = true)) coverUrl else ""
        firebase.createPlaylistInFirebase(uid, name, defaultCover)
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.deletePlaylistFromFirebase(uid, playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.addSongToPlaylistInFirebase(uid, playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.removeSongFromPlaylistInFirebase(uid, playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        if (playlistId == "favorites_smart_pl" || playlistId == "favorites" || playlistId.equals("liked", ignoreCase = true)) {
            return favoriteSongs
        }
        val uid = getEffectiveUid()
        return combine(
            allCloudSongs,
            firebase.observePlaylistSongIds(uid, playlistId)
        ) { songs, ids ->
            val idSet = ids.toSet()
            songs.filter { idSet.contains(it.id) }
        }
    }

    // --- VIP ACTIVATION IN FIREBASE ---
    suspend fun activateVipKey(keyString: String): Result<String> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        firebase.activateVipKeyInFirebase(uid, keyString)
    }

    suspend fun activateFreeTrial(): Result<String> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUid()
        if (uid.startsWith("guest_")) return@withContext Result.failure(Exception("Must be signed in"))
        firebase.activateFreeTrialInFirebase(uid)
    }

    // --- LOCAL MEDIA SCANNER ---
    fun scanDeviceAudio(): Flow<List<Song>> = flow {
        val localAudioList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Track"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Local Storage"
                    val duration = it.getLong(durationCol).coerceAtLeast(1000L)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    localAudioList.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            audioUrl = contentUri.toString(),
                            imageUrl = "",
                            durationMs = duration,
                            category = "Local",
                            isVipOnly = false,
                            isDownloaded = true,
                            isLocal = true,
                            localUri = contentUri.toString(),
                            waveformPoints = (1..32).map { (20..95).random() / 100f }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emit(localAudioList)
    }.flowOn(Dispatchers.IO)

    // --- ADMIN ACTIONS (DIRECT TO FIREBASE) ---
    suspend fun adminAddSong(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        firebase.saveSongToFirebase(song)
    }

    suspend fun adminDeleteSong(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        firebase.deleteSongFromFirebase(songId)
    }

    val userReports: Flow<List<com.example.data.model.ReportLog>> = currentUser.flatMapLatest { user ->
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        firebase.observeUserReports(uid)
    }

    suspend fun submitReport(songId: String, songTitle: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        val user = currentUser.first()
        val uid = user?.uid ?: guestIdentityManager.getGuestId()
        val email = user?.email ?: "Guest"
        val profile = try {
            userProfile.first()
        } catch (e: Exception) {
            com.example.data.model.UserProfile()
        }
        val isVip = profile.isProActive
        val planName = if (isVip) "VIP PRO" else if (user != null) "Free Member" else "Guest"
        val priority = if (isVip) "HIGH" else "NORMAL"
        val report = com.example.data.model.ReportLog(
            userId = uid,
            userEmail = email,
            userName = profile.displayName,
            songId = songId,
            songTitle = songTitle,
            message = message,
            isVip = isVip,
            plan = planName,
            priority = priority,
            vipDaysRemaining = profile.daysRemaining
        )
        firebase.submitReport(report)
    }

    suspend fun adminGenerateVipKey(days: Int): Result<String> = withContext(Dispatchers.IO) {
        val part1 = (('A'..'Z') + ('0'..'9')).shuffled().take(3).joinToString("")
        val part2 = (100..999).random().toString()
        val part3 = (('A'..'Z') + ('0'..'9')).shuffled().take(3).joinToString("")
        val key = "$part1-$part2-$part3"
        val result = firebase.generateVipKeyInFirebase(key, days)
        if (result.isSuccess) {
            Result.success(key)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to generate VIP key"))
        }
    }
}
