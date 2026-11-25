package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ScienceLabModule
import java.util.Locale

private const val LOCAL_ASSET_HOST = "appassets.androidplatform.net"
private const val SCIENCE_LAB_URL = "https://$LOCAL_ASSET_HOST/assets/three/science_lab_three.html"
const val THREE_JS_LOCAL_SCRIPT_URL = "https://$LOCAL_ASSET_HOST/assets/three/three.min.js"

data class ThreeJsSceneParams(
    val module: ScienceLabModule,
    val zoom: Float,
    val orbitSpeed: Float,
    val eclipseAngle: Float,
    val forceNewton: Float,
    val massKg: Float,
    val circuitClosed: Boolean,
    val voltageV: Float,
    val resistanceOhm: Float,
    val temperatureC: Float,
    val geometryShapeIdx: Int,
    val resetCounter: Int
) {
    fun toJsonString(): String = buildString {
        append('{')
        append("\"module\":\"").append(module.name).append("\",")
        append(String.format(Locale.US, "\"zoom\":%.4f,", zoom))
        append(String.format(Locale.US, "\"orbitSpeed\":%.4f,", orbitSpeed))
        append(String.format(Locale.US, "\"eclipseAngle\":%.4f,", eclipseAngle))
        append(String.format(Locale.US, "\"forceNewton\":%.4f,", forceNewton))
        append(String.format(Locale.US, "\"massKg\":%.4f,", massKg))
        append("\"circuitClosed\":").append(circuitClosed).append(',')
        append(String.format(Locale.US, "\"voltageV\":%.4f,", voltageV))
        append(String.format(Locale.US, "\"resistanceOhm\":%.4f,", resistanceOhm))
        append(String.format(Locale.US, "\"temperatureC\":%.4f,", temperatureC))
        append("\"geometryShapeIdx\":").append(geometryShapeIdx)
        append('}')
    }
}

private fun toJsQuotedString(raw: String): String =
    "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private class MrStudyThreeBridge(
    private val onPartSelectedCallback: (String) -> Unit,
    private val onWebGlErrorCallback: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPartSelected(info: String) {
        mainHandler.post { onPartSelectedCallback(info) }
    }

    @JavascriptInterface
    fun onWebGlError(error: String) {
        mainHandler.post { onWebGlErrorCallback(error) }
    }
}

fun interceptBundledThreeAsset(context: Context, request: WebResourceRequest?): WebResourceResponse? {
    val uri = request?.url ?: return null
    if (uri.host != LOCAL_ASSET_HOST) return null
    val path = uri.path?.removePrefix("/assets/")?.trimStart('/') ?: return null
    if (!path.startsWith("three/")) return null

    return try {
        val stream = context.assets.open(path)
        val mime = when {
            path.endsWith(".js", ignoreCase = true) -> "application/javascript"
            path.endsWith(".html", ignoreCase = true) -> "text/html"
            else -> "application/octet-stream"
        }
        WebResourceResponse(mime, "UTF-8", stream)
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeJsScienceLabViewport(
    params: ThreeJsSceneParams,
    onPartSelected: (String) -> Unit,
    onWebGlError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnPartSelected by rememberUpdatedState(onPartSelected)
    val currentOnWebGlError by rememberUpdatedState(onWebGlError)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    LaunchedEffect(params, pageReady) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        val jsonLiteral = toJsQuotedString(params.toJsonString())
        wv.evaluateJavascript("if (window.mrStudyUpdateScene) window.mrStudyUpdateScene($jsonLiteral);", null)
    }

    LaunchedEffect(params.resetCounter, pageReady) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (pageReady && params.resetCounter > 0) {
            wv.evaluateJavascript("if (window.mrStudyResetCamera) window.mrStudyResetCamera();", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                removeJavascriptInterface("MrStudyThreeBridge")
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(
                    MrStudyThreeBridge(
                        onPartSelectedCallback = { currentOnPartSelected(it) },
                        onWebGlErrorCallback = { currentOnWebGlError(it) }
                    ),
                    "MrStudyThreeBridge"
                )
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? = interceptBundledThreeAsset(ctx, request)

                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady = true
                        val jsonLiteral = toJsQuotedString(params.toJsonString())
                        view?.evaluateJavascript(
                            "if (window.mrStudyUpdateScene) window.mrStudyUpdateScene($jsonLiteral);",
                            null
                        )
                    }
                }
                loadUrl(SCIENCE_LAB_URL)
                webViewRef = this
            }
        },
        modifier = modifier
    )
}

fun createThreeJsCodingWebViewClient(context: Context): WebViewClient =
    object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? = interceptBundledThreeAsset(context, request)
    }

fun createThreeJsCodingChromeClient(onConsole: (String) -> Unit): WebChromeClient =
    object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            val msg = consoleMessage?.message().orEmpty()
            if (msg.isNotBlank()) onConsole(msg)
            return true
        }
    }
