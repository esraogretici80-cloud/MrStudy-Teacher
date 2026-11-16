package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.getLocalAppCheckDebugToken
import com.example.core.setCustomAppCheckDebugToken
import com.example.core.testAppCheckConnection
import kotlinx.coroutines.launch

private const val FIREBASE_APP_CHECK_CONSOLE_URL =
    "https://console.firebase.google.com/project/gen-lang-client-0683046785/appcheck/apps"

@Composable
fun AppCheckTokenQuickCard(
    debugToken: String,
    onOpenSetupDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(debugToken) { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = debugToken,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(debugToken))
                        copied = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_debug_token_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) "Kopyalandı" else "Token'ı Kopyala", maxLines = 1)
                }
                Button(
                    onClick = onOpenSetupDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_appcheck_setup_button")
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kur & Test Et", maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AppCheckSetupDialog(
    onDismiss: () -> Unit,
    onVerifiedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var currentToken by remember { mutableStateOf(getLocalAppCheckDebugToken(context).orEmpty()) }
    var copied by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<String?>(null) }
    var testFailure by remember { mutableStateOf<String?>(null) }
    var showCustomTokenInput by remember { mutableStateOf(false) }
    var customTokenInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("App Check Cihaz Doğrulama", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Telefonuna doğrudan kurulan Debug APK'da Firebase App Check, güvenlik nedeniyle her cihazın Debug Token'ını bir kez onaylamanı ister. Onayladıktan sonra Normal ve Canlı Ders modelleri hatasız çalışır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Bu Cihazın Debug Token'ı",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentToken.ifBlank { "Debug token oluşturulamadı (Release derleme)" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        if (currentToken.isNotBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentToken))
                                    copied = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_copy_debug_token_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (copied) "Token Kopyalandı ✓" else "1. Token'ı Kopyala")
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FIREBASE_APP_CHECK_CONSOLE_URL))
                            context.startActivity(intent)
                        }.onFailure { err ->
                            testFailure = "Tarayıcı açılamadı: ${err.localizedMessage}"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_firebase_console_appcheck_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2. Firebase Console > App Check'i Aç")
                }

                Text(
                    text = "Açılan sayfada:\n• MrStudy (com.aistudio.mrstudy.krtvxm) satırında üç nokta (⋮) → Manage debug tokens → Add debug token\n• Kopyaladığın token'ı yapıştırıp Save'e bas ve buraya dön.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testSuccess = null
                            testFailure = null
                            val res = testAppCheckConnection(forceRefresh = true)
                            isTesting = false
                            res.onSuccess {
                                testSuccess = "App Check doğrulandı! Artık Normal Öğretmen ve Gemini Live Canlı Ders hatasız çalışacaktır."
                                onVerifiedSuccess()
                            }.onFailure { err ->
                                testFailure = "Henüz onaylanmadı (403): ${err.localizedMessage ?: "Token reddedildi."}"
                            }
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_appcheck_button")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Doğrulama Test Ediliyor...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3. Doğrulamayı Test Et")
                    }
                }

                if (!testSuccess.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = testSuccess.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (!testFailure.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testFailure.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                TextButton(onClick = { showCustomTokenInput = !showCustomTokenInput }) {
                    Text(
                        if (showCustomTokenInput) "Kayıtlı Token Girişini Gizle" else "Daha önce kayıtlı bir token'ım var"
                    )
                }

                if (showCustomTokenInput) {
                    OutlinedTextField(
                        value = customTokenInput,
                        onValueChange = { customTokenInput = it },
                        label = { Text("Kayıtlı Debug Token (UUID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FilledTonalButton(
                        onClick = {
                            if (setCustomAppCheckDebugToken(context, customTokenInput)) {
                                currentToken = customTokenInput.trim()
                                customTokenInput = ""
                                copied = false
                                testFailure = null
                                testSuccess = "Yeni token cihaza kaydedildi. Şimdi 'Doğrulamayı Test Et'e dokun."
                            } else {
                                testFailure = "Geçerli bir UUID token girin."
                            }
                        },
                        enabled = customTokenInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Token'ı Uygula")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
