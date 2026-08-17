package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val audioUrl: String,
    val imageUrl: String,
    val durationMs: Long,
    val category: String,
    val isVipOnly: Boolean = false,
    val isDownloaded: Boolean = false,
    val localUri: String? = null,
    val waveformCsv: String = ""
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String = "",
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vip_keys")
data class VipKeyEntity(
    @PrimaryKey val key: String,
    val durationDays: Int = 30,
    val isClaimed: Boolean = false,
    val claimedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val isPro: Boolean,
    val proExpiresAt: Long,
    val avatarUrl: String
)
