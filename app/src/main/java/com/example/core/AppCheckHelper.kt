package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                // Synchronous commit() is required before installing DebugAppCheckProviderFactory
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured from launch Intent.")
            } else if (prefs.getString(DEBUG_SECRET_KEY, null).isNullOrBlank()) {
                // Pre-generate and synchronously persist a stable UUID for sideloaded debug installs
                val generated = UUID.randomUUID().toString()
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, generated)
                    .commit()
                Log.w(TAG, "No debug token in Intent extra; generated persistent local debug secret.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

fun getLocalAppCheckDebugToken(context: Context): String? {
    if (!BuildConfig.DEBUG) return null
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val existing = prefs.getString(DEBUG_SECRET_KEY, null)?.takeIf { it.isNotBlank() }
        if (existing != null) {
            existing
        } else {
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(DEBUG_SECRET_KEY, generated).commit()
            generated
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read local debug token", e)
        null
    }
}

fun setCustomAppCheckDebugToken(context: Context, token: String): Boolean {
    if (!BuildConfig.DEBUG) return false
    val cleaned = token.trim()
    if (cleaned.length < 8) return false
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        prefs.edit().putString(DEBUG_SECRET_KEY, cleaned).commit()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update custom debug token", e)
        false
    }
}

suspend fun testAppCheckConnection(forceRefresh: Boolean = true): Result<String> {
    return try {
        val result = withTimeout(10_000L) {
            FirebaseAppCheck.getInstance().getAppCheckToken(forceRefresh).await()
        }
        if (result.token.isNotBlank()) {
            Result.success(result.token)
        } else {
            Result.failure(IllegalStateException("App Check token boş döndü."))
        }
    } catch (e: TimeoutCancellationException) {
        Result.failure(IllegalStateException("App Check 10 saniyede yanıt vermedi. İnternetinizi kontrol edin.", e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun isAppCheckErrorMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    return message.contains("App Check", ignoreCase = true) ||
        message.contains("appcheck", ignoreCase = true) ||
        message.contains("attestation", ignoreCase = true) ||
        message.contains("Exchange failed", ignoreCase = true) ||
        message.contains("403", ignoreCase = true)
}
