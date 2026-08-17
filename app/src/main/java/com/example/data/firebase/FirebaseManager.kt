package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.UserProfile
import com.example.data.model.VipKey
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseManager {

    private val TAG = "FirebaseManager"
    val databaseUrl = "https://skedz-p-default-rtdb.firebaseio.com"
    val googleWebClientId = "799073385490-fc95oito6p9kg6jqdeclhjg9mfvd4k1j.apps.googleusercontent.com"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val realtimeDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(databaseUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FirebaseDatabase instance with URL", e)
            FirebaseDatabase.getInstance()
        }
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Realtime Database References
    private val rootRef get() = realtimeDb.reference
    private val playerRef get() = realtimeDb.getReference("player")
    private val songsRef get() = realtimeDb.getReference("songs")
    private val musicRef get() = realtimeDb.getReference("music")
    private val tracksRef get() = realtimeDb.getReference("tracks")
    private val playlistsRef get() = realtimeDb.getReference("playlists")
    private val userPlaylistsRef get() = realtimeDb.getReference("user_playlists")
    private val playlistSongsRef get() = realtimeDb.getReference("playlist_songs")
    private val userPlaylistSongsRef get() = realtimeDb.getReference("user_playlist_songs")
    private val vipKeysRef get() = realtimeDb.getReference("vip_keys")
    private val keysRef get() = realtimeDb.getReference("keys")
    private val usersRef get() = realtimeDb.getReference("users")
    private val favoritesRef get() = realtimeDb.getReference("favorites")
    private val reportsRef get() = realtimeDb.getReference("reports")

    // --- AUTH FLOW ---
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(googleWebClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("Google Sign-In returned empty user")

                // Update / sync user profile in Realtime Database
                val userSnap = usersRef.child(user.uid).get().await()
                if (!userSnap.exists()) {
                    val profile = mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "User"),
                        "email" to (user.email ?: ""),
                        "avatarUrl" to (user.photoUrl?.toString() ?: ""),
                        "isPro" to false,
                        "proExpiresAt" to 0L,
                        "createdAt" to System.currentTimeMillis()
                    )
                    usersRef.child(user.uid).setValue(profile).await()
                } else {
                    user.photoUrl?.let { photo ->
                        usersRef.child(user.uid).child("avatarUrl").setValue(photo.toString()).await()
                    }
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Unsupported credential type received from Google."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In cancelled"))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("Authentication returned empty user")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("User creation failed")
            // Create user profile in Realtime Database
            val profile = mapOf(
                "uid" to user.uid,
                "displayName" to displayName.ifBlank { email.substringBefore("@") },
                "email" to (user.email ?: email),
                "isPro" to false,
                "proExpiresAt" to 0L,
                "createdAt" to System.currentTimeMillis()
            )
            usersRef.child(user.uid).setValue(profile).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Anonymous sign-in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // --- REALTIME SONGS LISTENER (OBSERVES FIRESTORE 'songs' & 'player' + RTDB) ---
    fun observeRealtimeSongs(): Flow<List<Song>> = callbackFlow {
        var playerSnapshot: DataSnapshot? = null
        var songsSnapshot: DataSnapshot? = null
        var musicSnapshot: DataSnapshot? = null
        var tracksSnapshot: DataSnapshot? = null
        var firestoreSongs: List<Song> = emptyList()
        var firestorePlayerSongs: List<Song> = emptyList()

        fun emitCombined() {
            val resultList = mutableListOf<Song>()
            val seen = mutableSetOf<String>()

            // 1. Process Firestore 'songs' collection (Primary Firestore Data)
            for (fsSong in firestoreSongs) {
                val dedupe = (fsSong.audioUrl.ifBlank { fsSong.id }).lowercase()
                if (seen.add(dedupe)) {
                    resultList.add(fsSong)
                }
            }

            // 2. Process Firestore 'player' collection
            for (fsSong in firestorePlayerSongs) {
                val dedupe = (fsSong.audioUrl.ifBlank { fsSong.id }).lowercase()
                if (seen.add(dedupe)) {
                    resultList.add(fsSong)
                }
            }

            // 3. Process Realtime DB player node
            playerSnapshot?.let { collectSongsFromSnapshot(it, resultList, seen) }
            // 4. Process Realtime DB songs node
            songsSnapshot?.let { collectSongsFromSnapshot(it, resultList, seen) }
            // 5. Process Realtime DB music & tracks node
            musicSnapshot?.let { collectSongsFromSnapshot(it, resultList, seen) }
            tracksSnapshot?.let { collectSongsFromSnapshot(it, resultList, seen) }

            Log.d(TAG, "Realtime songs updated: ${resultList.size} songs loaded from Firestore & RTDB")
            trySend(resultList)
        }

        val playerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playerSnapshot = snapshot
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "player listener cancelled: ${error.message}")
            }
        }

        val songsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                songsSnapshot = snapshot
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "songs listener cancelled: ${error.message}")
            }
        }

        val musicListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                musicSnapshot = snapshot
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        val tracksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tracksSnapshot = snapshot
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        playerRef.addValueEventListener(playerListener)
        songsRef.addValueEventListener(songsListener)
        musicRef.addValueEventListener(musicListener)
        tracksRef.addValueEventListener(tracksListener)

        // 1. Listen to Firestore 'songs' collection (Matches user's Firestore Screenshot)
        val firestoreSongsReg = try {
            firestore.collection("songs").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.w(TAG, "Firestore 'songs' listener error: ${e?.message}")
                    return@addSnapshotListener
                }
                val parsed = mutableListOf<Song>()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    parseSongFromMap(doc.id, data)?.let { parsed.add(it) }
                }
                firestoreSongs = parsed
                emitCombined()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore 'songs' listener", e)
            null
        }

        // 2. Listen to Firestore 'player' collection
        val firestorePlayerReg = try {
            firestore.collection("player").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val parsed = mutableListOf<Song>()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    parseSongFromMap(doc.id, data)?.let { parsed.add(it) }
                }
                firestorePlayerSongs = parsed
                emitCombined()
            }
        } catch (e: Exception) {
            null
        }

        awaitClose {
            playerRef.removeEventListener(playerListener)
            songsRef.removeEventListener(songsListener)
            musicRef.removeEventListener(musicListener)
            tracksRef.removeEventListener(tracksListener)
            firestoreSongsReg?.remove()
            firestorePlayerReg?.remove()
        }
    }

    private fun resolveCategoryName(
        rawCategory: String?,
        tags: List<String>,
        title: String,
        artist: String,
        album: String
    ): String {
        if (!rawCategory.isNullOrBlank() && !rawCategory.equals("all", ignoreCase = true) && !rawCategory.equals("unknown", ignoreCase = true)) {
            val trimmed = rawCategory.trim()
            // Format nicely if needed (e.g. "hip-hop" -> "Hip-hop", "brakeup" -> "Brakeup")
            return trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // Use tag if category field is empty
        if (tags.isNotEmpty()) {
            val firstTag = tags.firstOrNull { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            if (firstTag != null) {
                return firstTag.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        return "General"
    }

    private fun parseSongFromMap(docId: String, data: Map<String, Any>): Song? {
        try {
            if (docId.startsWith("rep_") || docId.startsWith("notif_") || docId.startsWith("ann_") || docId == "reports" || docId == "feedback" || docId == "notifications") {
                return null
            }
            val type = data["type"]?.toString()
            if (type in listOf("song_issue", "general_feedback", "report", "feedback", "announcement", "notification")) {
                return null
            }

            val id = data["id"]?.toString()?.ifBlank { null } ?: docId
            val title = data["title"]?.toString()
                ?: data["name"]?.toString()
                ?: data["songName"]?.toString()
                ?: data["song_name"]?.toString()
                ?: data["trackName"]?.toString()
                ?: data["track_name"]?.toString()
                ?: data["songTitle"]?.toString()
                ?: "Untitled Track"

            val audioUrl = data["audioUrl"]?.toString()
                ?: data["audio_url"]?.toString()
                ?: data["url"]?.toString()
                ?: data["songUrl"]?.toString()
                ?: data["song_url"]?.toString()
                ?: data["link"]?.toString()
                ?: data["streamUrl"]?.toString()
                ?: data["src"]?.toString()
                ?: data["source"]?.toString()
                ?: data["audio"]?.toString()
                ?: data["mp3"]?.toString()
                ?: data["file"]?.toString()
                ?: ""

            val rawImageUrl = data["imageUrl"]?.toString()
                ?: data["image_url"]?.toString()
                ?: data["image"]?.toString()
                ?: data["coverUrl"]?.toString()
                ?: data["cover_url"]?.toString()
                ?: data["cover"]?.toString()
                ?: data["poster"]?.toString()
                ?: data["posterUrl"]?.toString()
                ?: data["thumbnail"]?.toString()
                ?: data["photo"]?.toString()
                ?: data["art"]?.toString()
                ?: ""

            val imageUrl = if (rawImageUrl.contains("unsplash.com", ignoreCase = true)) "" else rawImageUrl

            val artist = data["artist"]?.toString()
                ?: data["singer"]?.toString()
                ?: data["artistName"]?.toString()
                ?: data["singerName"]?.toString()
                ?: data["author"]?.toString()
                ?: "Tamil & EDM Artist"

            val album = data["album"]?.toString()
                ?: data["movie"]?.toString()
                ?: data["film"]?.toString()
                ?: data["collection"]?.toString()
                ?: "SK Edz Music"

            val isVip = data["isVipOnly"] as? Boolean
                ?: data["is_vip_only"] as? Boolean
                ?: data["vip"] as? Boolean
                ?: data["isVip"] as? Boolean
                ?: data["pro"] as? Boolean
                ?: data["isPro"] as? Boolean
                ?: false

            val durationMs: Long = when (val dur = data["durationMs"] ?: data["duration_ms"] ?: data["duration"] ?: data["length"]) {
                is Number -> dur.toLong()
                is String -> {
                    if (dur.contains(":")) {
                        val parts = dur.split(":")
                        val m = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                        val s = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                        (m * 60 + s) * 1000L
                    } else dur.toLongOrNull() ?: 210000L
                }
                else -> 210000L
            }

            val playCount: Long = (data["playCount"] as? Number)?.toLong()
                ?: (data["play_count"] as? Number)?.toLong()
                ?: (data["plays"] as? Number)?.toLong()
                ?: (data["streams"] as? Number)?.toLong()
                ?: (data["views"] as? Number)?.toLong()
                ?: 0L

            val tagsList = mutableListOf<String>()
            val rawTags = data["tags"]
            if (rawTags is List<*>) {
                rawTags.forEach { if (it != null && it.toString().isNotBlank()) tagsList.add(it.toString().trim()) }
            } else if (rawTags is String) {
                rawTags.split(",", "#", ";", " ").forEach { if (it.isNotBlank()) tagsList.add(it.trim()) }
            }
            val tagField = data["tag"]?.toString() ?: data["keywords"]?.toString()
            if (!tagField.isNullOrBlank()) {
                tagField.split(",", "#", ";", " ").forEach { if (it.isNotBlank() && !tagsList.contains(it.trim())) tagsList.add(it.trim()) }
            }

            // Also check categories / genres list
            val rawCategoriesList = data["categories"] ?: data["genres"] ?: data["languages"] ?: data["moods"]
            if (rawCategoriesList is List<*>) {
                rawCategoriesList.forEach {
                    if (it != null && it.toString().isNotBlank() && !tagsList.contains(it.toString().trim())) {
                        tagsList.add(it.toString().trim())
                    }
                }
            }

            // Direct category ID fields
            val catId = data["categoryId"]?.toString() ?: data["category_id"]?.toString() ?: data["catId"]?.toString()
            if (!catId.isNullOrBlank() && !tagsList.contains(catId.trim())) {
                tagsList.add(catId.trim())
            }

            val rawCategory = data["category"]?.toString()
                ?: data["genre"]?.toString()
                ?: data["mood"]?.toString()
                ?: data["language"]?.toString()
                ?: data["lang"]?.toString()
                ?: data["categoryName"]?.toString()
                ?: data["category_name"]?.toString()
                ?: data["mood_name"]?.toString()
                ?: data["genre_name"]?.toString()
                ?: data["type"]?.toString()
                ?: data["track_type"]?.toString()
                ?: data["section"]?.toString()
                ?: data["folder"]?.toString()
                ?: (if (tagsList.isNotEmpty()) tagsList.first() else null)

            if (!rawCategory.isNullOrBlank()) {
                rawCategory.split(",", "/", "|", "#", ";").forEach {
                    val trimmed = it.trim()
                    if (trimmed.isNotBlank() && !tagsList.any { t -> t.equals(trimmed, ignoreCase = true) }) {
                        tagsList.add(trimmed)
                    }
                }
            }

            val category = resolveCategoryName(rawCategory, tagsList, title, artist, album)

            val waveformCsv = data["waveformCsv"]?.toString() ?: data["waveform"]?.toString() ?: ""

            // Strict Audio Check: A song must have a valid stream/audio URL
            if (audioUrl.isNotBlank()) {
                return Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    audioUrl = audioUrl,
                    imageUrl = imageUrl,
                    durationMs = durationMs,
                    category = category,
                    isVipOnly = isVip,
                    playCount = playCount,
                    tags = tagsList.distinct(),
                    waveformPoints = parseWaveform(waveformCsv)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firestore song document $docId", e)
        }
        return null
    }

    private fun collectSongsFromSnapshot(
        snapshot: DataSnapshot,
        targetList: MutableList<Song>,
        seenKeys: MutableSet<String>
    ) {
        if (!snapshot.exists()) return

        // Skip root reports / feedback / notifications branches
        val rootKey = snapshot.key?.lowercase() ?: ""
        if (rootKey.contains("report") || rootKey.contains("feedback") || rootKey.contains("notif") || rootKey.contains("announc") || rootKey.contains("user") || rootKey.contains("vip_key")) {
            return
        }

        // Check if snapshot itself is a single song object
        val directSong = parseSongFromSnapshot(snapshot)
        if (directSong != null) {
            val dedupe = (directSong.audioUrl.ifBlank { directSong.id }).lowercase()
            if (seenKeys.add(dedupe)) {
                targetList.add(directSong)
            }
            return
        }

        // Iterate through all direct children
        for (child in snapshot.children) {
            val childKey = child.key?.lowercase() ?: ""
            if (childKey.contains("report") || childKey.contains("feedback") || childKey.contains("notif") || childKey.contains("announc") || childKey.contains("user") || childKey.contains("vip_key")) {
                continue
            }
            val childSong = parseSongFromSnapshot(child)
            if (childSong != null) {
                val dedupe = (childSong.audioUrl.ifBlank { childSong.id }).lowercase()
                if (seenKeys.add(dedupe)) {
                    targetList.add(childSong)
                }
            } else if (child.hasChildren()) {
                // If child is a sub-array or sub-map (e.g. nested lists or category groupings)
                for (subChild in child.children) {
                    val subKey = subChild.key?.lowercase() ?: ""
                    if (subKey.contains("report") || subKey.contains("feedback") || subKey.contains("notif")) {
                        continue
                    }
                    val subSong = parseSongFromSnapshot(subChild)
                    if (subSong != null) {
                        val dedupe = (subSong.audioUrl.ifBlank { subSong.id }).lowercase()
                        if (seenKeys.add(dedupe)) {
                            targetList.add(subSong)
                        }
                    }
                }
            }
        }
    }

    private fun parseSongFromSnapshot(child: DataSnapshot): Song? {
        try {
            val key = child.key ?: ""
            if (key.startsWith("rep_") || key.startsWith("notif_") || key.startsWith("ann_") || key == "reports" || key == "feedback" || key == "notifications" || key == "announcements") {
                return null
            }
            val parentKey = child.ref.parent?.key ?: ""
            if (parentKey in listOf("reports", "report_logs", "feedback", "issues", "song_reports", "notifications", "announcements", "alerts", "broadcasts", "users", "profiles", "vip_keys", "admins")) {
                return null
            }
            val type = child.child("type").getValue(String::class.java)
            if (type in listOf("song_issue", "general_feedback", "report", "feedback", "announcement", "notification")) {
                return null
            }

            val id = child.child("id").getValue(String::class.java)
                ?: child.child("songId").getValue(String::class.java)
                ?: child.key
                ?: UUID.randomUUID().toString()

            val audioUrl = getStringField(child, "audioUrl", "audio_url", "url", "songUrl", "song_url", "link", "streamUrl", "stream_url", "musicUrl", "music_url", "src", "source", "mp3", "audio", "media", "file", "download_url", "downloadUrl")
                ?: ""

            // Strict Audio Check: If there's no audio URL, this is NOT a playable song
            if (audioUrl.isBlank()) {
                return null
            }

            val title = getStringField(child, "title", "name", "song_name", "songName", "track_name", "trackName", "track", "label", "song", "song_title", "songTitle")
                ?: child.key
                ?: "Untitled Track"

            val rawImageUrl = getStringField(child, "imageUrl", "image_url", "image", "coverUrl", "cover_url", "cover", "poster", "posterUrl", "thumbnail", "thumb", "art", "artwork", "photo", "pic", "img", "banner")
                ?: ""

            val imageUrl = if (rawImageUrl.contains("unsplash.com", ignoreCase = true)) "" else rawImageUrl

            val artist = getStringField(child, "artist", "singer", "artist_name", "artistName", "singer_name", "singerName", "author", "creator", "channel", "music_director")
                ?: "Tamil & EDM Artist"

            val album = getStringField(child, "album", "album_name", "albumName", "movie", "film", "collection")
                ?: "SK Edz Music"

            val isVipOnly = child.child("isVipOnly").getValue(Boolean::class.java)
                ?: child.child("is_vip_only").getValue(Boolean::class.java)
                ?: child.child("vip").getValue(Boolean::class.java)
                ?: child.child("isVip").getValue(Boolean::class.java)
                ?: child.child("pro").getValue(Boolean::class.java)
                ?: child.child("isPro").getValue(Boolean::class.java)
                ?: false

            val durationMs: Long = when (val durVal = child.child("durationMs").value
                ?: child.child("duration_ms").value
                ?: child.child("duration").value
                ?: child.child("length").value) {
                is Long -> durVal
                is Int -> durVal.toLong()
                is Double -> durVal.toLong()
                is String -> {
                    if (durVal.contains(":")) {
                        val parts = durVal.split(":")
                        if (parts.size == 2) {
                            val m = parts[0].trim().toLongOrNull() ?: 0L
                            val s = parts[1].trim().toLongOrNull() ?: 0L
                            (m * 60 + s) * 1000L
                        } else 210000L
                    } else durVal.toLongOrNull() ?: 210000L
                }
                else -> 210000L
            }

            val playCount: Long = when (val playVal = child.child("playCount").value
                ?: child.child("play_count").value
                ?: child.child("plays").value
                ?: child.child("streams").value
                ?: child.child("views").value
                ?: child.child("listens").value) {
                is Long -> playVal
                is Int -> playVal.toLong()
                is Double -> playVal.toLong()
                is String -> playVal.toLongOrNull() ?: 0L
                else -> 0L
            }

            val tagsList = mutableListOf<String>()
            // Check tags as child array/map
            val tagsSnap = child.child("tags")
            if (tagsSnap.exists()) {
                for (t in tagsSnap.children) {
                    val str = t.getValue(String::class.java)?.trim()
                    if (!str.isNullOrBlank()) tagsList.add(str)
                }
                if (tagsList.isEmpty() && tagsSnap.value is String) {
                    tagsSnap.value.toString().split(",", "#", " ").forEach {
                        if (it.isNotBlank()) tagsList.add(it.trim())
                    }
                }
            }
            val rawTagString = getStringField(child, "tag", "keywords", "genre", "genre_name", "keywords_list")
            if (!rawTagString.isNullOrBlank()) {
                rawTagString.split(",", "#", " ").forEach {
                    val trimmed = it.trim()
                    if (trimmed.isNotBlank() && !tagsList.contains(trimmed)) {
                        tagsList.add(trimmed)
                    }
                }
            }

            val rawCategory = getStringField(
                child,
                "category", "genre", "mood", "language", "lang", "categoryName", "category_name",
                "mood_name", "genre_name", "tag", "type", "track_type", "section", "playlist", "folder"
            ) ?: if (parentKey.isNotBlank() && parentKey != "player" && parentKey != "music" && parentKey != "tracks" && parentKey != "songs" && parentKey != "user_playlists") {
                parentKey
            } else {
                null
            }

            if (!rawCategory.isNullOrBlank()) {
                rawCategory.split(",", "/", "|", "#").forEach {
                    val trimmed = it.trim()
                    if (trimmed.isNotBlank() && !tagsList.any { t -> t.equals(trimmed, ignoreCase = true) }) {
                        tagsList.add(trimmed)
                    }
                }
            }

            val category = resolveCategoryName(rawCategory, tagsList, title, artist, album)

            val waveformCsv = getStringField(child, "waveformCsv", "waveform", "amplitudes") ?: ""

            // Strict Audio Check: Item MUST have a valid audioUrl to be a playable track
            if (audioUrl.isNotBlank()) {
                return Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    audioUrl = audioUrl,
                    imageUrl = imageUrl,
                    durationMs = durationMs,
                    category = category,
                    isVipOnly = isVipOnly,
                    waveformPoints = parseWaveform(waveformCsv),
                    playCount = playCount,
                    tags = tagsList.distinct()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing song from snapshot child: ${child.key}", e)
        }
        return null
    }

    private fun getStringField(snapshot: DataSnapshot, vararg keys: String): String? {
        for (k in keys) {
            val v = snapshot.child(k).value
            if (v != null) {
                val str = v.toString().trim()
                if (str.isNotBlank() && str != "null") return str
            }
        }
        return null
    }

    // --- REALTIME CATEGORIES LISTENER (OBSERVES FIRESTORE 'categories' COLLECTION & RTDB) ---
    fun observeRealtimeCategories(): Flow<List<String>> = callbackFlow {
        var firestoreCats: List<String> = emptyList()
        var rtdbCats: List<String> = emptyList()

        fun emitCombined() {
            val combined = (firestoreCats + rtdbCats)
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
                .distinct()
            trySend(combined)
        }

        // 1. Listen to Firestore 'categories' collection (hip-hop, k-pop, lo-fi, love, malayalam, melody, mood, tamil, xxx-tentacion)
        val firestoreReg = try {
            firestore.collection("categories").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.w(TAG, "Firestore categories error: ${e?.message}")
                    return@addSnapshotListener
                }
                val parsed = mutableListOf<String>()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name")
                        ?: doc.data?.get("name")?.toString()
                        ?: doc.data?.get("title")?.toString()
                        ?: doc.id.replace("-", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    if (name.isNotBlank()) {
                        parsed.add(name)
                    }
                }
                firestoreCats = parsed
                emitCombined()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore categories listener", e)
            null
        }

        // 2. Listen to RTDB 'categories' node
        val categoriesRef = realtimeDb.getReference("categories")
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parsed = mutableListOf<String>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java)
                        ?: child.getValue(String::class.java)
                        ?: child.key
                    if (!name.isNullOrBlank()) {
                        parsed.add(name)
                    }
                }
                rtdbCats = parsed
                emitCombined()
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        categoriesRef.addValueEventListener(rtdbListener)

        awaitClose {
            firestoreReg?.remove()
            categoriesRef.removeEventListener(rtdbListener)
        }
    }

    // --- USER-ISOLATED REALTIME PLAYLISTS LISTENER ---
    fun observeUserPlaylists(uid: String): Flow<List<Playlist>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var rtdbPlaylists = mutableListOf<Playlist>()
        var firestorePlaylists = mutableListOf<Playlist>()

        fun emitCombined() {
            val map = mutableMapOf<String, Playlist>()
            for (p in rtdbPlaylists) map[p.id] = p
            for (p in firestorePlaylists) map[p.id] = p
            trySend(map.values.toList())
        }

        val userPlRef = userPlaylistsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Playlist>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: "Playlist"
                        val coverUrl = child.child("coverUrl").getValue(String::class.java) ?: ""
                        val isCustom = child.child("isCustom").getValue(Boolean::class.java) ?: true
                        val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val songCount = child.child("songCount").getValue(Int::class.java) ?: 0

                        list.add(
                            Playlist(
                                id = id,
                                name = name,
                                coverUrl = coverUrl,
                                isCustom = isCustom,
                                createdAt = createdAt,
                                songCount = songCount
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing playlist: ${child.key}", e)
                    }
                }
                rtdbPlaylists = list
                emitCombined()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeUserPlaylists cancelled: ${error.message}")
            }
        }

        userPlRef.addValueEventListener(listener)

        // Listen to Firestore user-specific playlists: users/{uid}/playlists
        val firestoreReg = try {
            firestore.collection("users").document(uid).collection("playlists").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = mutableListOf<Playlist>()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val id = doc.id
                    val name = data["name"]?.toString() ?: "Playlist"
                    val coverUrl = data["coverUrl"]?.toString() ?: ""
                    val isCustom = data["isCustom"] as? Boolean ?: true
                    val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val songCount = (data["songCount"] as? Number)?.toInt() ?: 0

                    list.add(
                        Playlist(
                            id = id,
                            name = name,
                            coverUrl = coverUrl,
                            isCustom = isCustom,
                            createdAt = createdAt,
                            songCount = songCount
                        )
                    )
                }
                firestorePlaylists = list
                emitCombined()
            }
        } catch (e: Exception) {
            null
        }

        awaitClose {
            userPlRef.removeEventListener(listener)
            firestoreReg?.remove()
        }
    }

    fun observeAllPlaylistSongIds(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }

        val ref = userPlaylistSongsRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = mutableSetOf<String>()
                for (playlistNode in snapshot.children) {
                    for (songNode in playlistNode.children) {
                        val key = songNode.key
                        if (!key.isNullOrBlank()) {
                            ids.add(key)
                        }
                    }
                }
                trySend(ids)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeAllPlaylistSongIds cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- REALTIME PLAYLIST SONG IDS (USER ISOLATED) ---
    fun observePlaylistSongIds(uid: String, playlistId: String): Flow<List<String>> = callbackFlow {
        if (uid.isBlank() || playlistId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val ref = userPlaylistSongsRef.child(uid).child(playlistId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = mutableListOf<String>()
                for (child in snapshot.children) {
                    ids.add(child.key ?: "")
                }
                trySend(ids.filter { it.isNotBlank() })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observePlaylistSongIds cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- REALTIME VIP KEYS ---
    fun observeRealtimeVipKeys(): Flow<List<VipKey>> = callbackFlow {
        var rtdbKeys = mutableListOf<VipKey>()
        var firestoreKeys = mutableListOf<VipKey>()

        fun emitCombined() {
            val map = mutableMapOf<String, VipKey>()
            for (k in rtdbKeys) map[k.key] = k
            for (k in firestoreKeys) map[k.key] = k
            trySend(map.values.toList())
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<VipKey>()
                for (child in snapshot.children) {
                    try {
                        val key = child.child("key").getValue(String::class.java) ?: child.key ?: ""
                        val durationDays = child.child("durationDays").getValue(Int::class.java) ?: 30
                        val isClaimed = child.child("isClaimed").getValue(Boolean::class.java) ?: false
                        val claimedBy = child.child("claimedBy").getValue(String::class.java)
                        val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        list.add(
                            VipKey(
                                key = key,
                                durationDays = durationDays,
                                isClaimed = isClaimed,
                                claimedBy = claimedBy,
                                createdAt = createdAt
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing VIP key: ${child.key}", e)
                    }
                }
                rtdbKeys = list
                emitCombined()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeRealtimeVipKeys cancelled: ${error.message}")
            }
        }

        vipKeysRef.addValueEventListener(listener)

        // Listen to Firestore 'keys' collection
        val firestoreKeysReg = try {
            firestore.collection("keys").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = mutableListOf<VipKey>()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val key = data["key"]?.toString() ?: doc.id
                    val durationDays = (data["durationDays"] as? Number)?.toInt()
                        ?: (data["days"] as? Number)?.toInt()
                        ?: (data["duration"] as? Number)?.toInt()
                        ?: 30
                    val isClaimed = data["isClaimed"] as? Boolean
                        ?: data["isUsed"] as? Boolean
                        ?: data["used"] as? Boolean
                        ?: false
                    val claimedBy = data["claimedBy"]?.toString() ?: data["usedBy"]?.toString()
                    val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                    list.add(
                        VipKey(
                            key = key,
                            durationDays = durationDays,
                            isClaimed = isClaimed,
                            claimedBy = claimedBy,
                            createdAt = createdAt
                        )
                    )
                }
                firestoreKeys = list
                emitCombined()
            }
        } catch (e: Exception) {
            null
        }

        awaitClose {
            vipKeysRef.removeEventListener(listener)
            firestoreKeysReg?.remove()
        }
    }

    // --- REALTIME USER PROFILE ---
    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(UserProfile(uid = "guest", displayName = "Guest User", email = "guest@skedz.app"))
            awaitClose { }
            return@callbackFlow
        }

        var rtdbProfile: UserProfile? = null
        var firestoreProfile: UserProfile? = null

        fun emitMerged() {
            val r = rtdbProfile
            val f = firestoreProfile
            if (r == null && f == null) return

            val isPro = (r?.isPro == true) || (f?.isPro == true)
            val proExpiresAt = maxOf(r?.proExpiresAt ?: 0L, f?.proExpiresAt ?: 0L)
            val displayName = when {
                !f?.displayName.isNullOrBlank() && f?.displayName != "SK User" -> f!!.displayName
                !r?.displayName.isNullOrBlank() && r?.displayName != "SK User" -> r!!.displayName
                else -> auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "SK User"
            }
            val email = f?.email?.ifBlank { r?.email } ?: auth.currentUser?.email ?: ""
            val avatarUrl = f?.avatarUrl?.ifBlank { r?.avatarUrl } ?: ""

            val hasUsedFreeTrial = r?.hasUsedFreeTrial == true || f?.hasUsedFreeTrial == true
            trySend(
                UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    isPro = isPro,
                    proExpiresAt = proExpiresAt,
                    avatarUrl = avatarUrl,
                    hasUsedFreeTrial = hasUsedFreeTrial
                )
            )
        }

        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val displayName = snapshot.child("displayName").getValue(String::class.java)
                        ?: snapshot.child("name").getValue(String::class.java)
                        ?: "SK User"
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val isPro = (snapshot.child("isPro").getValue(Boolean::class.java) == true)
                        || (snapshot.child("vip").getValue(Boolean::class.java) == true)
                        || (snapshot.child("isVip").getValue(Boolean::class.java) == true)
                        || (snapshot.child("plan").getValue(String::class.java)?.lowercase() in listOf("vip", "pro", "premium"))
                    val proExpiresAt = when (val exp = snapshot.child("proExpiresAt").value
                        ?: snapshot.child("vipExpiresAt").value
                        ?: snapshot.child("expiresAt").value) {
                        is Long -> exp
                        is Number -> exp.toLong()
                        is String -> exp.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java) ?: ""
                    val hasUsedFreeTrial = snapshot.child("hasUsedFreeTrial").getValue(Boolean::class.java) ?: false

                    rtdbProfile = UserProfile(
                        uid = uid,
                        displayName = displayName,
                        email = email,
                        isPro = isPro || (proExpiresAt > System.currentTimeMillis()),
                        proExpiresAt = proExpiresAt,
                        avatarUrl = avatarUrl,
                        hasUsedFreeTrial = hasUsedFreeTrial
                    )
                    emitMerged()
                } else {
                    val currentAuthUser = auth.currentUser
                    val newProfile = UserProfile(
                        uid = uid,
                        displayName = currentAuthUser?.displayName ?: currentAuthUser?.email?.substringBefore("@") ?: "SK User",
                        email = currentAuthUser?.email ?: "",
                        isPro = false,
                        proExpiresAt = 0L
                    )
                    ref.setValue(
                        mapOf(
                            "uid" to newProfile.uid,
                            "displayName" to newProfile.displayName,
                            "email" to newProfile.email,
                            "isPro" to false,
                            "proExpiresAt" to 0L
                        )
                    )
                    rtdbProfile = newProfile
                    emitMerged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeUserProfile cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)

        // Also listen to Firestore users collection
        val firestoreUserReg = try {
            firestore.collection("users").document(uid).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val data = snapshot.data ?: return@addSnapshotListener
                val displayName = data["displayName"]?.toString()
                    ?: data["name"]?.toString()
                    ?: data["username"]?.toString()
                    ?: "SK User"
                val email = data["email"]?.toString() ?: ""
                val planStr = data["plan"]?.toString()?.lowercase() ?: data["role"]?.toString()?.lowercase() ?: ""
                val isPro = (data["isPro"] as? Boolean == true)
                    || (data["vip"] as? Boolean == true)
                    || (data["isVip"] as? Boolean == true)
                    || (data["vip_active"] as? Boolean == true)
                    || (planStr in listOf("vip", "pro", "premium", "admin"))
                val proExpiresAt = when (val exp = data["proExpiresAt"] ?: data["vipExpiresAt"] ?: data["expiresAt"]) {
                    is Number -> exp.toLong()
                    is com.google.firebase.Timestamp -> exp.toDate().time
                    is String -> exp.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val avatarUrl = data["avatarUrl"]?.toString() ?: data["photo"]?.toString() ?: ""
                val hasUsedFreeTrial = data["hasUsedFreeTrial"] as? Boolean ?: false

                firestoreProfile = UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    isPro = isPro || (proExpiresAt > System.currentTimeMillis()),
                    proExpiresAt = proExpiresAt,
                    avatarUrl = avatarUrl,
                    hasUsedFreeTrial = hasUsedFreeTrial
                )
                emitMerged()
            }
        } catch (e: Exception) {
            null
        }

        awaitClose {
            ref.removeEventListener(listener)
            firestoreUserReg?.remove()
        }
    }

    // --- REALTIME FAVORITES ---
    fun observeUserFavorites(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }

        val ref = favoritesRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favIds = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val isFav = child.getValue(Boolean::class.java) ?: true
                    if (isFav) {
                        child.key?.let { favIds.add(it) }
                    }
                }
                trySend(favIds)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeUserFavorites cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- WRITE OPERATIONS ---

    suspend fun saveSongToFirebase(song: Song): Result<Unit> {
        return try {
            val songId = song.id.ifBlank { "song_" + UUID.randomUUID().toString().take(8) }
            val songData = mapOf(
                "id" to songId,
                "title" to song.title,
                "name" to song.title,
                "artist" to song.artist,
                "album" to song.album,
                "audioUrl" to song.audioUrl,
                "url" to song.audioUrl,
                "imageUrl" to song.imageUrl,
                "image" to song.imageUrl,
                "durationMs" to song.durationMs,
                "category" to song.category,
                "isVipOnly" to song.isVipOnly,
                "waveformCsv" to (if (song.waveformPoints.isNotEmpty()) song.waveformPoints.joinToString(",") else "0.3,0.7,0.5,0.9,0.4,0.8"),
                "updatedAt" to System.currentTimeMillis()
            )

            // Write to both player and songs in Realtime Database for complete compatibility
            playerRef.child(songId).setValue(songData).await()
            songsRef.child(songId).setValue(songData).await()

            try {
                firestore.collection("songs").document(songId).set(songData).await()
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore mirror skipped: ${fe.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveSongToFirebase failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSongFromFirebase(songId: String): Result<Unit> {
        return try {
            playerRef.child(songId).removeValue().await()
            songsRef.child(songId).removeValue().await()
            try {
                firestore.collection("songs").document(songId).delete().await()
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore delete skipped: ${fe.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteSongFromFirebase failed", e)
            Result.failure(e)
        }
    }

    suspend fun createPlaylistInFirebase(uid: String, name: String, coverUrl: String): Result<String> {
        if (uid.isBlank()) return Result.failure(Exception("User ID is required"))
        return try {
            val id = "pl_" + UUID.randomUUID().toString().take(8)
            val data = mapOf(
                "id" to id,
                "name" to name,
                "coverUrl" to coverUrl,
                "isCustom" to true,
                "userId" to uid,
                "ownerUid" to uid,
                "createdAt" to System.currentTimeMillis(),
                "songCount" to 0
            )
            // Save to user's private node
            userPlaylistsRef.child(uid).child(id).setValue(data).await()

            // Also mirror to user's private Firestore collection
            try {
                firestore.collection("users").document(uid).collection("playlists").document(id).set(data).await()
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore user playlist mirror skipped: ${fe.message}")
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylistFromFirebase(uid: String, playlistId: String): Result<Unit> {
        if (uid.isBlank() || playlistId.isBlank()) return Result.failure(Exception("Invalid arguments"))
        return try {
            userPlaylistsRef.child(uid).child(playlistId).removeValue().await()
            userPlaylistSongsRef.child(uid).child(playlistId).removeValue().await()
            try {
                firestore.collection("users").document(uid).collection("playlists").document(playlistId).delete().await()
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore playlist delete skipped: ${fe.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSongToPlaylistInFirebase(uid: String, playlistId: String, songId: String): Result<Unit> {
        if (uid.isBlank() || playlistId.isBlank() || songId.isBlank()) return Result.failure(Exception("Invalid arguments"))
        return try {
            userPlaylistSongsRef.child(uid).child(playlistId).child(songId).setValue(true).await()
            val snapshot = userPlaylistSongsRef.child(uid).child(playlistId).get().await()
            val count = snapshot.childrenCount.toInt()
            userPlaylistsRef.child(uid).child(playlistId).child("songCount").setValue(count).await()
            try {
                firestore.collection("users").document(uid).collection("playlists").document(playlistId).update("songCount", count).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeSongFromPlaylistInFirebase(uid: String, playlistId: String, songId: String): Result<Unit> {
        if (uid.isBlank() || playlistId.isBlank() || songId.isBlank()) return Result.failure(Exception("Invalid arguments"))
        return try {
            userPlaylistSongsRef.child(uid).child(playlistId).child(songId).removeValue().await()
            val snapshot = userPlaylistSongsRef.child(uid).child(playlistId).get().await()
            val count = snapshot.childrenCount.toInt()
            userPlaylistsRef.child(uid).child(playlistId).child("songCount").setValue(count).await()
            try {
                firestore.collection("users").document(uid).collection("playlists").document(playlistId).update("songCount", count).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteInFirebase(uid: String, songId: String, isCurrentlyFav: Boolean): Result<Unit> {
        if (uid.isBlank() || songId.isBlank()) return Result.failure(Exception("Invalid user or song"))
        return try {
            if (isCurrentlyFav) {
                favoritesRef.child(uid).child(songId).removeValue().await()
                try {
                    firestore.collection("users").document(uid).collection("favorites").document(songId).delete().await()
                } catch (_: Exception) {}
            } else {
                favoritesRef.child(uid).child(songId).setValue(true).await()
                try {
                    firestore.collection("users").document(uid).collection("favorites").document(songId).set(
                        mapOf("songId" to songId, "addedAt" to System.currentTimeMillis())
                    ).await()
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateVipKeyInFirebase(key: String, durationDays: Int): Result<Unit> {
        return try {
            val normalizedKey = formatToVipPattern(key)
            val keyData = mapOf(
                "key" to normalizedKey,
                "durationDays" to durationDays,
                "isClaimed" to false,
                "claimedBy" to null,
                "createdAt" to System.currentTimeMillis()
            )
            vipKeysRef.child(normalizedKey).setValue(keyData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats any 9-character string or standard key into the required XXX-XXX-XXX pattern
     */
    fun formatToVipPattern(rawInput: String): String {
        val sanitized = rawInput.replace("-", "").replace(" ", "").trim().uppercase()
        return if (sanitized.length == 9) {
            "${sanitized.substring(0, 3)}-${sanitized.substring(3, 6)}-${sanitized.substring(6, 9)}"
        } else {
            rawInput.trim().uppercase()
        }
    }

    suspend fun incrementSongPlayCount(songId: String) {
        try {
            val playerSnap = playerRef.child(songId).child("playCount").get().await()
            val current = (playerSnap.getValue(Long::class.java) ?: playerSnap.getValue(Int::class.java)?.toLong() ?: 0L)
            val newCount = current + 1
            playerRef.child(songId).child("playCount").setValue(newCount).await()
            try {
                songsRef.child(songId).child("playCount").setValue(newCount).await()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment play count: ${e.message}")
        }
    }

    suspend fun activateFreeTrialInFirebase(uid: String): Result<String> {
        return try {
            val userRef = realtimeDb.getReference("users/$uid")
            val snap = userRef.get().await()
            val hasUsed = snap.child("hasUsedFreeTrial").getValue(Boolean::class.java) ?: false
            if (hasUsed) {
                return Result.failure(Exception("You have already used your 1-day free trial."))
            }

            // Grant 1 day (86,400,000 ms)
            val currentExp = snap.child("proExpiresAt").getValue(Long::class.java) ?: 0L
            val now = System.currentTimeMillis()
            val baseTime = if (currentExp > now) currentExp else now
            val newExp = baseTime + (24L * 60L * 60L * 1000L)

            val updates = mapOf(
                "isPro" to true,
                "proExpiresAt" to newExp,
                "hasUsedFreeTrial" to true
            )
            userRef.updateChildren(updates).await()
            Result.success("1-Day Free Trial activated!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun activateVipKeyInFirebase(uid: String, keyString: String): Result<String> {
        val rawInput = keyString.trim().uppercase()
        val targetKey = formatToVipPattern(rawInput)
        val cleanTarget = rawInput.replace("-", "").replace(" ", "")

        return try {
            var targetRef = vipKeysRef
            var snapshot: DataSnapshot? = null
            var matchedNodeKey: String? = null

            // 1. Check in vip_keys ref
            val directVip = vipKeysRef.child(targetKey).get().await()
            if (directVip.exists()) {
                snapshot = directVip
                matchedNodeKey = targetKey
                targetRef = vipKeysRef
            } else {
                val altVip = vipKeysRef.child(rawInput).get().await()
                if (altVip.exists()) {
                    snapshot = altVip
                    matchedNodeKey = rawInput
                    targetRef = vipKeysRef
                }
            }

            // 2. Check in keys ref if not found yet
            if (snapshot == null || !snapshot.exists()) {
                val directKey = keysRef.child(targetKey).get().await()
                if (directKey.exists()) {
                    snapshot = directKey
                    matchedNodeKey = targetKey
                    targetRef = keysRef
                } else {
                    val altKey = keysRef.child(rawInput).get().await()
                    if (altKey.exists()) {
                        snapshot = altKey
                        matchedNodeKey = rawInput
                        targetRef = keysRef
                    }
                }
            }

            // 3. Fallback: Search all children in both vip_keys and keys nodes
            if (snapshot == null || !snapshot.exists()) {
                val allVipSnap = vipKeysRef.get().await()
                for (child in allVipSnap.children) {
                    val k = (child.child("key").getValue(String::class.java) ?: child.key ?: "").replace("-", "").replace(" ", "").uppercase()
                    if (k == cleanTarget || child.key.equals(rawInput, ignoreCase = true) || child.key.equals(targetKey, ignoreCase = true)) {
                        snapshot = child
                        matchedNodeKey = child.key
                        targetRef = vipKeysRef
                        break
                    }
                }
            }

            if (snapshot == null || !snapshot.exists()) {
                val allKeysSnap = keysRef.get().await()
                for (child in allKeysSnap.children) {
                    val k = (child.child("key").getValue(String::class.java) ?: child.key ?: "").replace("-", "").replace(" ", "").uppercase()
                    if (k == cleanTarget || child.key.equals(rawInput, ignoreCase = true) || child.key.equals(targetKey, ignoreCase = true)) {
                        snapshot = child
                        matchedNodeKey = child.key
                        targetRef = keysRef
                        break
                    }
                }
            }

            // 4. Firestore Check (keys and vip_keys collection)
            var firestoreDocId: String? = null
            var firestoreCollection: String? = null
            var firestoreData: Map<String, Any>? = null

            if (snapshot == null || !snapshot.exists() || matchedNodeKey == null) {
                try {
                    val fsDirect = firestore.collection("keys").document(targetKey).get().await()
                    if (fsDirect.exists()) {
                        firestoreDocId = targetKey
                        firestoreCollection = "keys"
                        firestoreData = fsDirect.data
                    } else {
                        val fsVip = firestore.collection("vip_keys").document(targetKey).get().await()
                        if (fsVip.exists()) {
                            firestoreDocId = targetKey
                            firestoreCollection = "vip_keys"
                            firestoreData = fsVip.data
                        } else {
                            val allFsKeys = firestore.collection("keys").get().await()
                            for (doc in allFsKeys.documents) {
                                val d = doc.data ?: continue
                                val k = (d["key"]?.toString() ?: doc.id).replace("-", "").replace(" ", "").uppercase()
                                if (k == cleanTarget || doc.id.equals(rawInput, ignoreCase = true) || doc.id.equals(targetKey, ignoreCase = true)) {
                                    firestoreDocId = doc.id
                                    firestoreCollection = "keys"
                                    firestoreData = d
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore key check error: ${e.message}")
                }
            }

            if (snapshot == null && firestoreData == null) {
                return Result.failure(Exception("Invalid VIP License Key: '$targetKey' not found in database."))
            }

            val durationDays: Int
            val now = System.currentTimeMillis()

            if (snapshot != null && matchedNodeKey != null) {
                // Check if already used or claimed (Strict 1-Time Use)
                val isClaimed = snapshot.child("isClaimed").getValue(Boolean::class.java) ?: false
                val isUsed = snapshot.child("isUsed").getValue(Boolean::class.java) ?: false
                val used = snapshot.child("used").getValue(Boolean::class.java) ?: false
                val status = snapshot.child("status").getValue(String::class.java) ?: ""

                if (isClaimed || isUsed || used || status.equals("used", ignoreCase = true) || status.equals("claimed", ignoreCase = true)) {
                    return Result.failure(Exception("This VIP License Key has already been used (1-time use only)."))
                }

                durationDays = when (val durVal = snapshot.child("durationDays").value
                    ?: snapshot.child("duration_days").value
                    ?: snapshot.child("days").value
                    ?: snapshot.child("duration").value
                    ?: snapshot.child("validity").value) {
                    is Long -> durVal.toInt()
                    is Int -> durVal
                    is Double -> durVal.toInt()
                    is String -> durVal.toIntOrNull() ?: 30
                    else -> 30
                }

                // Mark key as permanently used in Realtime Database
                targetRef.child(matchedNodeKey).updateChildren(
                    mapOf(
                        "isClaimed" to true,
                        "isUsed" to true,
                        "used" to true,
                        "status" to "used",
                        "claimedBy" to uid,
                        "usedBy" to uid,
                        "claimedAt" to now,
                        "usedAt" to now
                    )
                ).await()
            } else if (firestoreData != null && firestoreDocId != null && firestoreCollection != null) {
                val isClaimed = firestoreData["isClaimed"] as? Boolean ?: firestoreData["isUsed"] as? Boolean ?: firestoreData["used"] as? Boolean ?: false
                val status = firestoreData["status"]?.toString() ?: ""
                if (isClaimed || status.equals("used", ignoreCase = true) || status.equals("claimed", ignoreCase = true)) {
                    return Result.failure(Exception("This VIP License Key has already been used (1-time use only)."))
                }

                durationDays = (firestoreData["durationDays"] as? Number)?.toInt()
                    ?: (firestoreData["days"] as? Number)?.toInt()
                    ?: (firestoreData["duration"] as? Number)?.toInt()
                    ?: 30

                firestore.collection(firestoreCollection).document(firestoreDocId).update(
                    mapOf(
                        "isClaimed" to true,
                        "isUsed" to true,
                        "used" to true,
                        "status" to "used",
                        "claimedBy" to uid,
                        "usedBy" to uid,
                        "claimedAt" to now,
                        "usedAt" to now
                    )
                ).await()
            } else {
                durationDays = 30
            }

            val addedDurationMs = durationDays.toLong() * 24 * 60 * 60 * 1000L

            val userSnap = usersRef.child(uid).get().await()
            val currentExpiry = userSnap.child("proExpiresAt").getValue(Long::class.java) ?: 0L
            val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
            val newExpiry = baseTime + addedDurationMs

            // Update user status in RTDB
            usersRef.child(uid).updateChildren(
                mapOf(
                    "isPro" to true,
                    "proExpiresAt" to newExpiry
                )
            ).await()

            // Update user status in Firestore
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "isPro" to true,
                        "proExpiresAt" to newExpiry
                    )
                ).await()
            } catch (_: Exception) {}

            Result.success("VIP Pro Activated! +$durationDays Days of Master Access.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REPORTS & FEEDBACK (SYNCED ACROSS FIRESTORE & REALTIME DB ADMIN PANELS) ---
    suspend fun submitReport(report: com.example.data.model.ReportLog): Result<String> {
        return try {
            val reportId = report.id.ifBlank {
                "rep_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6)
            }
            val now = System.currentTimeMillis()
            val createdTime = if (report.createdAt > 0) report.createdAt else now
            val isSongReport = report.songId.isNotBlank() && report.songId != "general_feedback"
            val isVipUser = report.isVip
            val planName = if (isVipUser) "VIP PRO" else report.plan.ifBlank { "Free" }
            val priority = if (isVipUser) "HIGH" else report.priority.ifBlank { "NORMAL" }
            val userType = if (isVipUser) "VIP Member 👑" else if (report.userId.startsWith("guest_")) "Guest Device" else "Free User"

            val reportData = mapOf(
                "id" to reportId,
                "reportId" to reportId,
                "userId" to report.userId,
                "uid" to report.userId,
                "userEmail" to report.userEmail,
                "email" to report.userEmail,
                "userName" to report.userName,
                "name" to report.userName,
                "displayName" to report.userName,
                "isVip" to isVipUser,
                "isPro" to isVipUser,
                "isVipUser" to isVipUser,
                "vip" to isVipUser,
                "vip_active" to isVipUser,
                "vipStatus" to if (isVipUser) "VIP PRO" else "FREE",
                "plan" to planName,
                "userPlan" to planName,
                "userType" to userType,
                "role" to if (isVipUser) "VIP" else "User",
                "userRole" to if (isVipUser) "VIP" else "User",
                "priority" to priority,
                "reportPriority" to priority,
                "vipDaysRemaining" to report.vipDaysRemaining,
                "songId" to (if (isSongReport) report.songId else ""),
                "trackId" to (if (isSongReport) report.songId else ""),
                "songTitle" to (if (isSongReport) report.songTitle else "General Feedback"),
                "attachedSong" to (if (isSongReport) report.songTitle else "None"),
                "message" to report.message,
                "reportMessage" to report.message,
                "description" to report.message,
                "details" to report.message,
                "issue" to report.message,
                "status" to report.status.ifBlank { "Pending" },
                "createdAt" to createdTime,
                "timestamp" to createdTime,
                "type" to if (isSongReport) "song_issue" else "general_feedback"
            )

            // 1. Write ONLY to Realtime Database /reports and /report_logs
            try {
                reportsRef.child(reportId).setValue(reportData).await()
                realtimeDb.getReference("report_logs").child(reportId).setValue(reportData).await()
                realtimeDb.getReference("feedback").child(reportId).setValue(reportData).await()
            } catch (rtdbErr: Exception) {
                Log.w(TAG, "RTDB reports write warning: ${rtdbErr.message}")
            }

            // 2. Write ONLY to Firestore collections 'reports' and 'report_logs'
            try {
                firestore.collection("reports").document(reportId).set(reportData).await()
                firestore.collection("report_logs").document(reportId).set(reportData).await()
                firestore.collection("feedback").document(reportId).set(reportData).await()
                firestore.collection("issues").document(reportId).set(reportData).await()
                if (isSongReport) {
                    firestore.collection("song_reports").document(reportId).set(reportData).await()
                }
            } catch (fsErr: Exception) {
                Log.w(TAG, "Firestore reports write warning: ${fsErr.message}")
            }

            Log.d(TAG, "Report $reportId submitted successfully to both Firestore and Realtime Database (VIP: $isVipUser)")
            Result.success("Report submitted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "submitReport failed", e)
            Result.failure(e)
        }
    }

    fun observeUserReports(uid: String): Flow<List<com.example.data.model.ReportLog>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var rtdbReports: List<com.example.data.model.ReportLog> = emptyList()
        var firestoreReports: List<com.example.data.model.ReportLog> = emptyList()

        fun emitCombined() {
            val seen = mutableSetOf<String>()
            val combined = mutableListOf<com.example.data.model.ReportLog>()

            for (r in (firestoreReports + rtdbReports)) {
                if (r.id.isNotBlank() && seen.add(r.id)) {
                    combined.add(r)
                } else if (r.id.isBlank()) {
                    val key = "${r.createdAt}_${r.songId}_${r.message}"
                    if (seen.add(key)) {
                        combined.add(r)
                    }
                }
            }
            trySend(combined.sortedByDescending { it.createdAt })
        }

        // Listen to Realtime Database reports
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.example.data.model.ReportLog>()
                for (child in snapshot.children) {
                    val id = child.key ?: child.child("id").getValue(String::class.java) ?: ""
                    val reportUid = child.child("userId").getValue(String::class.java)
                        ?: child.child("uid").getValue(String::class.java) ?: ""
                    val email = child.child("userEmail").getValue(String::class.java)
                        ?: child.child("email").getValue(String::class.java) ?: ""
                    val name = child.child("userName").getValue(String::class.java)
                        ?: child.child("displayName").getValue(String::class.java)
                        ?: child.child("name").getValue(String::class.java) ?: ""
                    val sId = child.child("songId").getValue(String::class.java)
                        ?: child.child("trackId").getValue(String::class.java) ?: ""
                    val sTitle = child.child("songTitle").getValue(String::class.java)
                        ?: child.child("songName").getValue(String::class.java)
                        ?: child.child("title").getValue(String::class.java) ?: ""
                    val msg = child.child("message").getValue(String::class.java)
                        ?: child.child("description").getValue(String::class.java)
                        ?: child.child("details").getValue(String::class.java) ?: ""
                    val stat = child.child("status").getValue(String::class.java) ?: "Pending"
                    val isVip = (child.child("isVip").getValue(Boolean::class.java) == true)
                        || (child.child("isPro").getValue(Boolean::class.java) == true)
                        || (child.child("vip").getValue(Boolean::class.java) == true)
                        || (child.child("plan").getValue(String::class.java)?.lowercase()?.contains("vip") == true)
                    val plan = child.child("plan").getValue(String::class.java)
                        ?: child.child("userPlan").getValue(String::class.java) ?: if (isVip) "VIP PRO" else "Free"
                    val prio = child.child("priority").getValue(String::class.java) ?: if (isVip) "HIGH" else "Normal"
                    val vipDays = (child.child("vipDaysRemaining").getValue(Long::class.java) ?: 0L).toInt()
                    val cTime = child.child("createdAt").getValue(Long::class.java)
                        ?: child.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (reportUid == uid || uid == "all" || reportUid.isBlank()) {
                        list.add(
                            com.example.data.model.ReportLog(
                                id = id,
                                userId = reportUid,
                                userEmail = email,
                                userName = name,
                                songId = sId,
                                songTitle = sTitle,
                                message = msg,
                                status = stat,
                                isVip = isVip,
                                plan = plan,
                                priority = prio,
                                vipDaysRemaining = vipDays,
                                createdAt = cTime
                            )
                        )
                    }
                }
                rtdbReports = list
                emitCombined()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB reports listener cancelled: ${error.message}")
            }
        }
        reportsRef.addValueEventListener(rtdbListener)

        // Listen to Firestore reports collection
        val firestoreReg = try {
            firestore.collection("reports")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) {
                        Log.w(TAG, "Firestore reports listener error: ${e?.message}")
                        return@addSnapshotListener
                    }
                    val list = mutableListOf<com.example.data.model.ReportLog>()
                    for (doc in snapshot.documents) {
                        val reportUid = doc.getString("userId") ?: doc.getString("uid") ?: ""
                        if (reportUid == uid || uid == "all") {
                            val id = doc.id
                            val email = doc.getString("userEmail") ?: doc.getString("email") ?: ""
                            val name = doc.getString("userName") ?: doc.getString("displayName") ?: doc.getString("name") ?: ""
                            val sId = doc.getString("songId") ?: doc.getString("trackId") ?: ""
                            val sTitle = doc.getString("songTitle") ?: doc.getString("songName") ?: doc.getString("title") ?: ""
                            val msg = doc.getString("message") ?: doc.getString("description") ?: doc.getString("details") ?: ""
                            val stat = doc.getString("status") ?: "Pending"
                            val isVip = (doc.getBoolean("isVip") == true)
                                || (doc.getBoolean("isPro") == true)
                                || (doc.getBoolean("vip") == true)
                                || (doc.getString("plan")?.lowercase()?.contains("vip") == true)
                            val plan = doc.getString("plan") ?: doc.getString("userPlan") ?: if (isVip) "VIP PRO" else "Free"
                            val prio = doc.getString("priority") ?: if (isVip) "HIGH" else "Normal"
                            val vipDays = doc.getLong("vipDaysRemaining")?.toInt() ?: 0
                            val cTime = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: 0L
                            list.add(
                                com.example.data.model.ReportLog(
                                    id = id,
                                    userId = reportUid,
                                    userEmail = email,
                                    userName = name,
                                    songId = sId,
                                    songTitle = sTitle,
                                    message = msg,
                                    status = stat,
                                    isVip = isVip,
                                    plan = plan,
                                    priority = prio,
                                    vipDaysRemaining = vipDays,
                                    createdAt = cTime
                                )
                            )
                        }
                    }
                    firestoreReports = list
                    emitCombined()
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore reports registration failed: ${e.message}")
            null
        }

        awaitClose {
            reportsRef.removeEventListener(rtdbListener)
            firestoreReg?.remove()
        }
    }

    // --- ADMIN NOTIFICATIONS & BROADCASTS (REAL-TIME FIRESTORE & RTDB OBSERVER) ---
    fun observeAdminNotifications(): Flow<List<com.example.data.model.AppNotification>> = callbackFlow {
        var rtdbNotifs = listOf<com.example.data.model.AppNotification>()
        var firestoreNotifs = listOf<com.example.data.model.AppNotification>()

        fun emitMerged() {
            val seen = mutableSetOf<String>()
            val combined = mutableListOf<com.example.data.model.AppNotification>()
            for (n in (firestoreNotifs + rtdbNotifs)) {
                val key = n.id.ifBlank { "${n.timestamp}_${n.title}_${n.message}" }
                if (seen.add(key)) {
                    combined.add(n)
                }
            }
            trySend(combined.sortedByDescending { it.timestamp })
        }

        val notifsRef = realtimeDb.getReference("notifications")
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.example.data.model.AppNotification>()
                for (child in snapshot.children) {
                    val id = child.key ?: child.child("id").getValue(String::class.java) ?: UUID.randomUUID().toString()
                    val title = getStringField(child, "title", "header", "subject", "heading", "name") ?: "Admin Announcement"
                    val msg = getStringField(child, "message", "body", "desc", "description", "text", "content", "msg") ?: ""
                    val img = getStringField(child, "imageUrl", "image", "banner", "bannerUrl", "pic") ?: ""
                    val sId = getStringField(child, "songId", "trackId", "song_id") ?: ""
                    val prio = getStringField(child, "priority", "importance") ?: "normal"
                    val time = child.child("timestamp").getValue(Long::class.java)
                        ?: child.child("createdAt").getValue(Long::class.java)
                        ?: child.child("time").getValue(Long::class.java)
                        ?: System.currentTimeMillis()
                    if (title.isNotBlank() || msg.isNotBlank()) {
                        list.add(
                            com.example.data.model.AppNotification(
                                id = id,
                                title = title,
                                message = msg,
                                body = msg,
                                imageUrl = img,
                                songId = sId,
                                priority = prio,
                                timestamp = time
                            )
                        )
                    }
                }
                rtdbNotifs = list
                emitMerged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB notifications listener cancelled: ${error.message}")
            }
        }
        notifsRef.addValueEventListener(rtdbListener)

        val firestoreReg = try {
            firestore.collection("notifications")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = mutableListOf<com.example.data.model.AppNotification>()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val title = doc.getString("title") ?: doc.getString("header") ?: doc.getString("subject") ?: "Admin Announcement"
                        val msg = doc.getString("message") ?: doc.getString("body") ?: doc.getString("description") ?: doc.getString("text") ?: ""
                        val img = doc.getString("imageUrl") ?: doc.getString("image") ?: doc.getString("banner") ?: ""
                        val sId = doc.getString("songId") ?: doc.getString("trackId") ?: ""
                        val prio = doc.getString("priority") ?: "normal"
                        val time = doc.getLong("timestamp") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                        if (title.isNotBlank() || msg.isNotBlank()) {
                            list.add(
                                com.example.data.model.AppNotification(
                                    id = id,
                                    title = title,
                                    message = msg,
                                    body = msg,
                                    imageUrl = img,
                                    songId = sId,
                                    priority = prio,
                                    timestamp = time
                                )
                            )
                        }
                    }
                    firestoreNotifs = list
                    emitMerged()
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore notifications listener failed: ${e.message}")
            null
        }

        awaitClose {
            notifsRef.removeEventListener(rtdbListener)
            firestoreReg?.remove()
        }
    }

    // --- USER NOTIFICATION PERSISTENCE (SYNC ACROSS REINSTALLS & LOGINS) ---
    fun observeUserNotificationsState(uid: String): Flow<Pair<Set<String>, Set<String>>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(Pair(emptySet(), emptySet()))
            awaitClose { }
            return@callbackFlow
        }

        val ref = usersRef.child(uid).child("notification_state")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val seenSet = mutableSetOf<String>()
                val deletedSet = mutableSetOf<String>()

                for (sChild in snapshot.child("seen").children) {
                    val k = sChild.key
                    if (!k.isNullOrBlank()) seenSet.add(k)
                }
                for (dChild in snapshot.child("deleted").children) {
                    val k = dChild.key
                    if (!k.isNullOrBlank()) deletedSet.add(k)
                }

                trySend(Pair(seenSet, deletedSet))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeUserNotificationsState cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveNotificationSeenToCloud(uid: String, notifIds: List<String>) {
        if (uid.isBlank() || notifIds.isEmpty()) return
        try {
            val updateMap = mutableMapOf<String, Any>()
            notifIds.forEach { id ->
                if (id.isNotBlank()) {
                    updateMap["seen/$id"] = true
                }
            }
            if (updateMap.isNotEmpty()) {
                usersRef.child(uid).child("notification_state").updateChildren(updateMap).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveNotificationSeenToCloud failed: ${e.message}")
        }
    }

    suspend fun saveNotificationDeletedToCloud(uid: String, notifIds: List<String>) {
        if (uid.isBlank() || notifIds.isEmpty()) return
        try {
            val updateMap = mutableMapOf<String, Any>()
            notifIds.forEach { id ->
                if (id.isNotBlank()) {
                    updateMap["deleted/$id"] = true
                    updateMap["seen/$id"] = true
                }
            }
            if (updateMap.isNotEmpty()) {
                usersRef.child(uid).child("notification_state").updateChildren(updateMap).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveNotificationDeletedToCloud failed: ${e.message}")
        }
    }

    private fun parseWaveform(csv: String): List<Float> {
        if (csv.isBlank()) return (1..32).map { (20..95).random() / 100f }
        return csv.split(",").mapNotNull { it.trim().toFloatOrNull() }
    }
}
