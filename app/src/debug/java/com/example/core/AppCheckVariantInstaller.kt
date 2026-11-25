package com.example.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private const val TAG = "AppCheckDebugInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val LAST_BUILD_TOKEN_KEY = "com.example.mrstudy.LAST_BUILD_APPCHECK_TOKEN"

private fun isRealPhysicalDevice(): Boolean {
    val fingerprint = Build.FINGERPRINT.orEmpty()
    val model = Build.MODEL.orEmpty()
    val hardware = Build.HARDWARE.orEmpty()
    val product = Build.PRODUCT.orEmpty()
    val isEmulator = fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        model.contains("google_sdk", ignoreCase = true) ||
        model.contains("Emulator", ignoreCase = true) ||
        model.contains("Android SDK built for", ignoreCase = true) ||
        hardware.contains("goldfish", ignoreCase = true) ||
        hardware.contains("ranchu", ignoreCase = true) ||
        product.contains("sdk_gphone", ignoreCase = true) ||
        product.contains("vbox", ignoreCase = true)
    return !isEmulator
}

internal fun installVariantAppCheckProvider(context: Context, intent: Intent?): AppCheckProviderMode {
    val persistenceKey = FirebaseApp.getInstance().persistenceKey
    val prefs = context.getSharedPreferences(
        "$DEBUG_STORE_PREFIX$persistenceKey",
        Context.MODE_PRIVATE
    )

    // Purge any legacy hardcoded/invalid shared token or malformed secret stored previously
    val existingSecret = prefs.getString(DEBUG_SECRET_KEY, null)
    if (!existingSecret.isNullOrBlank() && !isValidDebugTokenUuid(existingSecret)) {
        prefs.edit()
            .remove(DEBUG_SECRET_KEY)
            .remove(LAST_BUILD_TOKEN_KEY)
            .commit()
        Log.w(TAG, "Removed invalid legacy App Check debug secret from device preferences.")
    }

    val intentToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
    val buildToken = BuildConfig.FIREBASE_APPCHECK_DEBUG_TOKEN.trim()

    val appCheck = FirebaseAppCheck.getInstance()
    appCheck.setTokenAutoRefreshEnabled(true)

    return when {
        isValidDebugTokenUuid(intentToken) -> {
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, intentToken)
                .commit()
            Log.d(TAG, "App Check debug secret applied synchronously from launch intent.")
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            AppCheckProviderMode.DEBUG
        }
        isValidDebugTokenUuid(buildToken) -> {
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, buildToken)
                .putString(LAST_BUILD_TOKEN_KEY, buildToken)
                .commit()
            Log.d(TAG, "App Check debug secret applied synchronously from build configuration.")
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            AppCheckProviderMode.DEBUG
        }
        isRealPhysicalDevice() -> {
            // Real physical user device: Automatically use Google Play Integrity!
            try {
                appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                Log.i(TAG, "PlayIntegrityAppCheckProviderFactory installed automatically on physical device.")
                AppCheckProviderMode.PLAY_INTEGRITY
            } catch (e: Exception) {
                Log.w(TAG, "Play Integrity install failed on physical device, falling back to debug", e)
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                AppCheckProviderMode.DEBUG
            }
        }
        else -> {
            Log.d(TAG, "Emulator detected; installing DebugAppCheckProviderFactory.")
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            AppCheckProviderMode.DEBUG
        }
    }
}
