package com.example.data.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import java.util.UUID

/**
 * Manages unique, device-isolated Guest IDs (similar to Free Fire / mobile games).
 * Each physical device or app installation maintains its own unique local guest storage,
 * completely isolated so actions on one device NEVER affect or leak onto another device.
 */
class GuestIdentityManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sk_guest_identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "GuestIdentityMgr"
        private const val KEY_LOCAL_DEVICE_GUEST_ID = "key_local_device_guest_id_v2"
    }

    /**
     * Gets or generates the unique local guest user ID for this specific device.
     * Tied permanently to this device installation only.
     */
    fun getGuestId(): String {
        val cached = prefs.getString(KEY_LOCAL_DEVICE_GUEST_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }

        // Generate permanent unique local device ID
        @SuppressLint("HardwareIds")
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }

        val randomToken = UUID.randomUUID().toString().replace("-", "").take(12)
        val deviceBase = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            "${androidId.take(8)}_$randomToken"
        } else {
            randomToken
        }

        val newGuestId = "guest_dev_$deviceBase"
        prefs.edit().putString(KEY_LOCAL_DEVICE_GUEST_ID, newGuestId).apply()
        Log.d(TAG, "Generated new device-local guest ID: $newGuestId")
        return newGuestId
    }

    /**
     * Backward-compatible helper that returns the local device guest ID.
     */
    suspend fun refreshGuestIdWithPublicIp(): String {
        return getGuestId()
    }
}
