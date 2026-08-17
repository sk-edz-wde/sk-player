package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val audioUrl: String,
    val imageUrl: String = "",
    val durationMs: Long = 0L,
    val category: String = "All", // Dynamic from Firebase
    val isVipOnly: Boolean = false,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocal: Boolean = false,
    val localUri: String? = null,
    val waveformPoints: List<Float> = emptyList(),
    val playCount: Long = 0L,
    val tags: List<String> = emptyList()
) {
    val coverUrl: String
        get() = imageUrl

    val isProOnly: Boolean
        get() = isVipOnly
    val durationFormatted: String
        get() {
            val totalMs = if (durationMs > 0L) {
                if (durationMs < 10000L) durationMs * 1000L else durationMs
            } else 210000L
            val totalSeconds = totalMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * Checks if this song matches a given category or tag based on database fields.
     */
    fun hasCategory(targetCategory: String): Boolean {
        val target = targetCategory.trim()
        if (target.isBlank() || target.equals("All", ignoreCase = true)) return true

        // 1. Direct case-insensitive matching on main category
        if (category.trim().equals(target, ignoreCase = true)) {
            return true
        }

        // 2. Direct case-insensitive matching against song tags
        if (tags.any { it.trim().equals(target, ignoreCase = true) }) {
            return true
        }

        // 3. Separated multi-category parts (e.g., "K-Pop, Pop" or "Lo-Fi / Chill")
        val categoryParts = category.split(",", "/", "|", "#", ";").map { it.trim() }
        if (categoryParts.any { it.equals(target, ignoreCase = true) }) {
            return true
        }

        // 4. Clean alphanumeric normalized comparison (e.g., "K-Pop" matches "K-Pop" or "kpop", "Hip-Hop" matches "hip-hop")
        fun norm(s: String): String = s.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val targetNorm = norm(target)
        if (targetNorm.isNotEmpty()) {
            if (norm(category) == targetNorm || categoryParts.any { norm(it) == targetNorm }) {
                return true
            }
            if (tags.any { norm(it) == targetNorm }) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts all unique categories and tags associated with this song from the database.
     */
    fun getAllCategoriesAndTags(): List<String> {
        val set = linkedSetOf<String>()
        if (category.isNotBlank() && !category.equals("All", ignoreCase = true) && !category.equals("Unknown", ignoreCase = true)) {
            category.split(",", "/", "|", "#", ";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotBlank() && !trimmed.equals("All", ignoreCase = true)) {
                    set.add(trimmed)
                }
            }
        }
        tags.forEach { tag ->
            val trimmed = tag.trim()
            if (trimmed.isNotBlank() && !trimmed.equals("All", ignoreCase = true)) {
                set.add(trimmed)
            }
        }
        return set.toList()
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val coverUrl: String = "",
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReportLog(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val songId: String = "",
    val songTitle: String = "",
    val message: String = "",
    val status: String = "Pending",
    val isVip: Boolean = false,
    val plan: String = "Free",
    val priority: String = "Normal",
    val vipDaysRemaining: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppNotification(
    val id: String = "",
    val title: String = "Admin Announcement",
    val message: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val songId: String = "",
    val type: String = "announcement",
    val priority: String = "normal",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class UserProfile(
    val uid: String = "sk_guest_01",
    val displayName: String = "Cyber Voyager",
    val email: String = "voyager@skedz.vip",
    val isPro: Boolean = false,
    val proExpiresAt: Long = 0L,
    val avatarUrl: String = "",
    val hasUsedFreeTrial: Boolean = false
) {
    val isProActive: Boolean
        get() = isPro && proExpiresAt > System.currentTimeMillis()

    val daysRemaining: Int
        get() {
            if (!isProActive) return 0
            val diff = proExpiresAt - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        }
}

data class VipKey(
    val key: String,
    val durationDays: Int = 30,
    val isClaimed: Boolean = false,
    val claimedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)


