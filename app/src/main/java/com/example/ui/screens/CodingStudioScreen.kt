package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CodeProjectFile
import com.example.data.model.CodingLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.THREE_JS_LOCAL_SCRIPT_URL
import com.example.ui.components.createThreeJsCodingChromeClient
import com.example.ui.components.createThreeJsCodingWebViewClient
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.PythonMiniInterpreter

private class SimpleSyntaxVisualTransformation : VisualTransformation {
    private val keywords = setOf(
        "def", "fun", "val", "var", "if", "elif", "else", "for", "while", "in",
        "return", "class", "public", "static", "void", "int", "float", "print",
        "println", "printf", "const", "let", "function", "import", "include", "true", "false"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val styled = buildAnnotatedString {
            append(raw)
            // Strings
            Regex("\"[^\"]*\"|'[^']*'").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF34D399)), m.range.first, m.range.last + 1)
            }
            // Numbers
            Regex("\\b\\d+(\\.\\d+)?\\b").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFFFBBF24)), m.range.first, m.range.last + 1)
            }
            // Keywords
            Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b").findAll(raw).forEach { m ->
                if (m.value in keywords) {
                    addStyle(
                        SpanStyle(color = Color(0xFFC084FC), fontWeight = FontWeight.Bold),
                        m.range.first,
                        m.range.last + 1
                    )
                }
            }
            // Comments (# or //)
            Regex("(#|//).*").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF94A3B8)), m.range.first, m.range.last + 1)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodingStudioScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onSaveFile: (CodeProjectFile) -> Unit,
    onDeleteFile: (String) -> Unit,
    onShareFile: (CodeProjectFile) -> Unit,
    onAnalyzeCodeWithAi: (CodingLanguage, String, String) -> Unit
) {
    BackHandler { onBack() }

    var activeFileId by remember { mutableStateOf<String?>(null) }
    var selectedLang by remember { mutableStateOf(CodingLanguage.PYTHON) }
    var fileName by remember { mutableStateOf("ornek.py") }
    var codeText by remember { mutableStateOf(CodingLanguage.PYTHON.starterTemplate) }
    var localConsoleOutput by remember { mutableStateOf<String?>(null) }
    var webHtmlToRender by remember { mutableStateOf<String?>(null) }
    var webRunCounter by remember { mutableIntStateOf(0) }
    val syntaxTransformation = remember { SimpleSyntaxVisualTransformation() }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Kodlama & Proje Stüdyosu",
                subtitle = "${selectedLang.displayName} • ${selectedLang.runtimeBadge}",
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = "${state.studioState.codeFiles.size} Dosya"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CodingLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLang == lang,
                                onClick = {
                                    selectedLang = lang
                                    if (activeFileId == null) {
                                        fileName = "proje.${lang.extension}"
                                        codeText = lang.starterTemplate
                                    }
                                    localConsoleOutput = null
                                    webHtmlToRender = null
                                },
                                label = { Text(lang.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    GlassCard(
                        cornerRadius = 20.dp,
                        accentBorder = ElectricBlueSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (selectedLang.isLiveRunnable) SuccessGreen.copy(alpha = 0.18f) else WarningAmber.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, if (selectedLang.isLiveRunnable) SuccessGreen else WarningAmber),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = selectedLang.runtimeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedLang.isLiveRunnable) SuccessGreen else WarningAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            activeFileId = null
                                            selectedLang = CodingLanguage.HTML
                                            fileName = "sahne_threejs.html"
                                            codeText = """
                                                <!DOCTYPE html><html><body style="margin:0;background:#070b18;overflow:hidden;">
                                                <script src="$THREE_JS_LOCAL_SCRIPT_URL"></script>
                                                <script>
                                                  const s=new THREE.Scene(),c=new THREE.PerspectiveCamera(60,innerWidth/innerHeight,0.1,50);
                                                  const r=new THREE.WebGLRenderer({antialias:true});r.setSize(innerWidth,innerHeight);document.body.appendChild(r.domElement);
                                                  const m=new THREE.Mesh(new THREE.TorusKnotGeometry(0.9,0.28,90,16),new THREE.MeshNormalMaterial());
                                                  s.add(m);c.position.z=3.4;
                                                  function loop(){requestAnimationFrame(loop);m.rotation.x+=0.015;m.rotation.y+=0.02;r.render(s,c);}
                                                  loop();
                                                </script></body></html>
                                            """.trimIndent()
                                            localConsoleOutput = "Three.js 3B şablonu yüklendi. 'Çalıştır'a basın."
                                            webHtmlToRender = null
                                        }
                                    ) {
                                        Text("Three.js 3B")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            activeFileId = null
                                            fileName = "yeni.${selectedLang.extension}"
                                            codeText = selectedLang.starterTemplate
                                            localConsoleOutput = null
                                            webHtmlToRender = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Yeni")
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                label = { Text("Dosya Adı") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = codeText,
                                onValueChange = { codeText = it },
                                label = { Text("${selectedLang.displayName} Kod Editörü") },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE2E8F0)
                                ),
                                visualTransformation = syntaxTransformation,
                                minLines = 8,
                                maxLines = 16,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("code_editor_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeonGradientButton(
                                    text = if (selectedLang.isLiveRunnable) "Çalıştır" else "AI Kuru Çalıştır",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = {
                                        when (selectedLang) {
                                            CodingLanguage.PYTHON -> {
                                                webHtmlToRender = null
                                                val res = PythonMiniInterpreter.execute(codeText)
                                                localConsoleOutput = buildString {
                                                    if (res.outputLines.isNotEmpty()) {
                                                        append(res.outputLines.joinToString("\n"))
                                                    }
                                                    if (!res.errorMessage.isNullOrBlank()) {
                                                        if (isNotEmpty()) append("\n")
                                                        append("Hata: ${res.errorMessage}")
                                                    }
                                                    if (isEmpty()) append("(Çıktı üretilmedi)")
                                                }
                                            }
                                            CodingLanguage.HTML -> {
                                                localConsoleOutput = "WebView Canlı HTML Önizleme güncellendi."
                                                webHtmlToRender = codeText
                                                webRunCounter++
                                            }
                                            CodingLanguage.CSS -> {
                                                localConsoleOutput = "WebView Canlı CSS Önizleme güncellendi."
                                                webHtmlToRender = "<!DOCTYPE html><html><head><style>$codeText</style></head><body style='background:#0B1120;padding:16px'><div class='card'><h3>MrStudy 4.0 CSS Kartı</h3><p>CSS stiliniz canlı olarak uygulandı.</p></div></body></html>"
                                                webRunCounter++
                                            }
                                            CodingLanguage.JAVASCRIPT -> {
                                                localConsoleOutput = "WebView JS Konsol çalıştırılıyor..."
                                                val escaped = codeText
                                                webHtmlToRender = """
                                                    <!DOCTYPE html><html><body style='background:#0B1120;color:#22D3EE;font-family:monospace;padding:12px'>
                                                    <div id='out'></div>
                                                    <script>
                                                    const _out = document.getElementById('out');
                                                    const log = (...args) => { _out.innerHTML += args.join(' ') + '<br/>'; console.log(args.join(' ')); };
                                                    console.log = log;
                                                    try { $escaped } catch (e) { log('Hata: ' + e.message); }
                                                    </script></body></html>
                                                """.trimIndent()
                                                webRunCounter++
                                            }
                                            else -> {
                                                webHtmlToRender = null
                                                localConsoleOutput = "Yerel derleyici yerine şeffaf 'AI Kuru Çalıştırma / Analiz' başlatıldı."
                                                onAnalyzeCodeWithAi(selectedLang, codeText, "Kuru Çalıştır & Çıktı")
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("run_code_button")
                                )

                                OutlinedButton(
                                    onClick = {
                                        val file = CodeProjectFile(
                                            id = activeFileId ?: java.util.UUID.randomUUID().toString(),
                                            name = fileName.ifBlank { "proje.${selectedLang.extension}" },
                                            language = selectedLang,
                                            code = codeText
                                        )
                                        activeFileId = file.id
                                        onSaveFile(file)
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kaydet")
                                }

                                OutlinedButton(
                                    onClick = {
                                        onShareFile(
                                            CodeProjectFile(
                                                name = fileName.ifBlank { "kod.${selectedLang.extension}" },
                                                language = selectedLang,
                                                code = codeText
                                            )
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dışa Aktar")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAnalyzeCodeWithAi(selectedLang, codeText, "Kodu Satır Satır Açıkla") },
                                    enabled = !state.isAnalyzingCode,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Açıkla")
                                }
                                Button(
                                    onClick = { onAnalyzeCodeWithAi(selectedLang, codeText, "Hata Bul ve Düzelt") },
                                    enabled = !state.isAnalyzingCode,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hata Düzelt")
                                }
                            }
                        }
                    }
                }

                if (!localConsoleOutput.isNullOrBlank()) {
                    item {
                        GlassCard(accentBorder = SuccessGreen, cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Çalıştırma Çıktısı / Konsol", style = MaterialTheme.typography.labelLarge, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    text = localConsoleOutput.orEmpty(),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                val html = webHtmlToRender
                if (html != null) {
                    item {
                        GlassCard(accentBorder = CyberCyanTertiary, cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Sandboxed WebView Canlı Önizleme", style = MaterialTheme.typography.labelLarge, color = CyberCyanTertiary)
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.allowFileAccess = false
                                            settings.allowContentAccess = false
                                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                            webViewClient = createThreeJsCodingWebViewClient(ctx)
                                            webChromeClient = createThreeJsCodingChromeClient { msg ->
                                                localConsoleOutput = (localConsoleOutput.orEmpty() + "\n> " + msg).trim()
                                            }
                                            loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
                                        }
                                    },
                                    update = { view ->
                                        val unused = webRunCounter
                                        view.loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }
                }

                if (state.isAnalyzingCode) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberCyanTertiary)
                            Text("AI kod asistanı inceliyor...")
                        }
                    }
                }

                if (!state.codeAiOutput.isNullOrBlank()) {
                    item {
                        GlassCard(accentBorder = NeonPurplePrimary, cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("AI Kod Analizi / Kuru Çalıştırma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                Text(state.codeAiOutput.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (state.studioState.codeFiles.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Kod Projelerin (${state.studioState.codeFiles.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.studioState.codeFiles, key = { it.id }) { file ->
                        GlassCard(
                            onClick = {
                                activeFileId = file.id
                                selectedLang = file.language
                                fileName = file.name
                                codeText = file.code
                                localConsoleOutput = null
                                webHtmlToRender = null
                            },
                            accentBorder = ElectricBlueSecondary,
                            cornerRadius = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${file.language.displayName} • ${file.code.lines().size} satır", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                                }
                                IconButton(onClick = { onDeleteFile(file.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Dosyayı Sil")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
