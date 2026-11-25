package com.example.core

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "AppCheckInit"
private const val LEGACY_INVALID_SHARED_TOKEN = "MrStudy-Shared-Debug-Token-2026-AppCheck"
private const val PLACEHOLDER_DEBUG_TOKEN = "YOUR_FIREBASE_APPCHECK_DEBUG_TOKEN"
private val UUID_REGEX = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
)

enum class AppCheckProviderMode(val label: String) {
    PLAY_INTEGRITY("Play Integrity (Otomatik)"),
    DEBUG("Debug Provider (Geliştirme)")
}

enum class AppCheckFailureReason {
    NONE,
    NO_NETWORK,
    PLAY_INTEGRITY_ATTESTATION,
    DEBUG_ATTESTATION_UNREGISTERED,
    TIMEOUT,
    UNKNOWN
}

data class AppCheckHealthStatus(
    val isHealthy: Boolean,
    val providerMode: AppCheckProviderMode,
    val hasNetwork: Boolean,
    val reason: AppCheckFailureReason,
    val userMessage: String,
    val technicalDetail: String? = null
)

@Volatile
private var cachedAppContext: Context? = null

fun isValidDebugTokenUuid(token: String?): Boolean {
    val trimmed = token?.trim().orEmpty()
    if (trimmed.isEmpty()) return false
    if (trimmed.equals(LEGACY_INVALID_SHARED_TOKEN, ignoreCase = true)) return false
    if (trimmed.equals(PLACEHOLDER_DEBUG_TOKEN, ignoreCase = true)) return false
    return UUID_REGEX.matches(trimmed)
}

fun currentAppCheckProviderMode(): AppCheckProviderMode =
    if (BuildConfig.DEBUG) AppCheckProviderMode.DEBUG else AppCheckProviderMode.PLAY_INTEGRITY

fun hasActiveNetwork(context: Context?): Boolean {
    val ctx = context ?: cachedAppContext ?: return true
    return try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Throwable) {
        true
    }
}

fun configureAppCheck(context: Context, intent: Intent?) {
    cachedAppContext = context.applicationContext
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        installVariantAppCheckProvider(context.applicationContext, intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * Verifies that an App Check token is available and valid for outbound Firebase AI requests.
 * If cached token retrieval fails or returns empty, performs at most ONE forced SDK refresh
 * when network is active — never loops infinitely and never bypasses App Check.
 */
suspend fun verifyOrRefreshAppCheckToken(
    context: Context? = cachedAppContext,
    allowSingleForceRefresh: Boolean = true
): Result<String> {
    if (!hasActiveNetwork(context)) {
        return Result.failure(
            IllegalStateException("İnternet bağlantısı yok. App Check doğrulaması için ağ bağlantınızı açın.")
        )
    }

    val appCheck = try {
        FirebaseAppCheck.getInstance()
    } catch (e: Exception) {
        Log.e(TAG, "FirebaseAppCheck instance unavailable", e)
        return Result.failure(e)
    }

    return try {
        val initial = withTimeout(10_000L) {
            appCheck.getAppCheckToken(false).await()
        }
        if (initial.token.isNotBlank()) {
            Result.success(initial.token)
        } else if (allowSingleForceRefresh) {
            refreshOnce(appCheck)
        } else {
            Result.failure(IllegalStateException("App Check token boş döndü."))
        }
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Cached App Check token fetch failed, attempting single SDK refresh", e)
        if (allowSingleForceRefresh && hasActiveNetwork(context)) {
            refreshOnce(appCheck)
        } else {
            Result.failure(e)
        }
    }
}

private suspend fun refreshOnce(appCheck: FirebaseAppCheck): Result<String> {
    return try {
        val refreshed = withTimeout(10_000L) {
            appCheck.getAppCheckToken(true).await()
        }
        if (refreshed.token.isNotBlank()) {
            Result.success(refreshed.token)
        } else {
            Result.failure(IllegalStateException("App Check yenilemesi boş token döndürdü."))
        }
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
            Result.failure(e)
        } else {
            throw e
        }
    } catch (e: Exception) {
        Log.e(TAG, "Forced App Check token refresh failed", e)
        Result.failure(e)
    }
}

suspend fun runAppCheckDiagnostics(context: Context): AppCheckHealthStatus {
    cachedAppContext = context.applicationContext
    val mode = currentAppCheckProviderMode()
    val online = hasActiveNetwork(context)
    if (!online) {
        return AppCheckHealthStatus(
            isHealthy = false,
            providerMode = mode,
            hasNetwork = false,
            reason = AppCheckFailureReason.NO_NETWORK,
            userMessage = "İnternet bağlantısı algılanamadı. Doğrulama için ağa bağlanıp tekrar deneyin."
        )
    }

    val result = verifyOrRefreshAppCheckToken(context = context, allowSingleForceRefresh = true)
    return result.fold(
        onSuccess = {
            AppCheckHealthStatus(
                isHealthy = true,
                providerMode = mode,
                hasNetwork = true,
                reason = AppCheckFailureReason.NONE,
                userMessage = if (mode == AppCheckProviderMode.PLAY_INTEGRITY) {
                    "Play Integrity App Check doğrulaması aktif ve token başarıyla yenilendi."
                } else {
                    "Geliştirme (Debug) App Check token doğrulaması aktif."
                }
            )
        },
        onFailure = { err ->
            val (reason, msg) = classifyAppCheckError(err, context)
            AppCheckHealthStatus(
                isHealthy = false,
                providerMode = mode,
                hasNetwork = online,
                reason = reason,
                userMessage = msg,
                technicalDetail = err.localizedMessage
            )
        }
    )
}

fun classifyAppCheckError(
    error: Throwable,
    context: Context? = cachedAppContext
): Pair<AppCheckFailureReason, String> {
    val chain = generateSequence(error) { it.cause }
        .mapNotNull { it.message ?: it.localizedMessage }
        .joinToString(" | ")

    if (!hasActiveNetwork(context)) {
        return AppCheckFailureReason.NO_NETWORK to
            "İnternet bağlantısı olmadığı için Firebase App Check doğrulaması yapılamadı. Bağlantınızı kontrol edip tekrar deneyin."
    }

    if (error is TimeoutCancellationException || chain.contains("timed out", ignoreCase = true) || chain.contains("timeout", ignoreCase = true)) {
        return AppCheckFailureReason.TIMEOUT to
            "Firebase App Check doğrulaması zaman aşımına uğradı. İnternet bağlantınızı kontrol edip tekrar deneyin."
    }

    if (isAppCheckErrorMessage(chain)) {
        return if (BuildConfig.DEBUG) {
            AppCheckFailureReason.DEBUG_ATTESTATION_UNREGISTERED to
                "Geliştirme (Debug) sürümünde Firebase App Check doğrulaması reddedildi (403). Bu geliştirme ortamı için Firebase Console > App Check kaydı doğrulanamadı; son kullanıcı sürümünde (Release) Play Integrity otomatik çalışır."
        } else {
            AppCheckFailureReason.PLAY_INTEGRITY_ATTESTATION to
                "Play Integrity doğrulaması tamamlanamadı (403). Cihazda Google Play Hizmetleri güncel değilse veya Firebase Console'da Play Integrity / yayın SHA-256 kaydı eksikse bu hata oluşur."
        }
    }

    return AppCheckFailureReason.UNKNOWN to
        (error.localizedMessage ?: "Firebase App Check token alınamadı.")
}

fun formatAppCheckUserError(
    error: Throwable,
    context: Context? = cachedAppContext
): String = classifyAppCheckError(error, context).second

fun isAppCheckErrorMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    return message.contains("App Check", ignoreCase = true) ||
        message.contains("appcheck", ignoreCase = true) ||
        message.contains("App attestation failed", ignoreCase = true) ||
        message.contains("attestation", ignoreCase = true) ||
        message.contains("Exchange failed", ignoreCase = true) ||
        message.contains("Play Integrity", ignoreCase = true) ||
        message.contains("403", ignoreCase = true)
}
