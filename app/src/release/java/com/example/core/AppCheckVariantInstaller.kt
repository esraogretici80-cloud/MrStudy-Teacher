package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private const val TAG = "AppCheckReleaseInit"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val LAST_BUILD_TOKEN_KEY = "com.example.mrstudy.LAST_BUILD_APPCHECK_TOKEN"

internal fun installVariantAppCheckProvider(context: Context, intent: Intent?): AppCheckProviderMode {
    // Clean up any debug token store leftover if upgrading from a debug build
    runCatching {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        if (prefs.contains(DEBUG_SECRET_KEY) || prefs.contains(LAST_BUILD_TOKEN_KEY)) {
            prefs.edit()
                .remove(DEBUG_SECRET_KEY)
                .remove(LAST_BUILD_TOKEN_KEY)
                .commit()
        }
    }

    val appCheck = FirebaseAppCheck.getInstance()
    appCheck.setTokenAutoRefreshEnabled(true)
    appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    Log.i(TAG, "PlayIntegrityAppCheckProviderFactory installed for RELEASE build.")
    return AppCheckProviderMode.PLAY_INTEGRITY
}
