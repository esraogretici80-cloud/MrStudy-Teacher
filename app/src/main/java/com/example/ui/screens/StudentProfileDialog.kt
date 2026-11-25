package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectCatalog
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentProfileDialog(
    profile: StudentProfile,
    isOnboarding: Boolean,
    onDismiss: () -> Unit,
    onSave: (displayName: String, gradeLevel: String, targetGoal: String, dailyGoalMinutes: Long, darkThemeMode: String) -> Unit,
    onSignOut: () -> Unit,
    onOpenAppCheckSetup: () -> Unit = {}
) {
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var selectedGrade by remember(profile) { mutableStateOf(profile.gradeLevel) }
    var targetGoal by remember(profile) { mutableStateOf(profile.targetGoal) }
    var goalMinutes by remember(profile) { mutableFloatStateOf(profile.dailyGoalMinutes.toFloat().coerceIn(15f, 240f)) }
    var themeMode by remember(profile) { mutableStateOf(profile.darkThemeMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isOnboarding) "MrStudy Öğrenci Profilini Kur" else "Öğrenci Profili ve Ayarlar",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "MrStudy öğretmenin ders anlatımını, sınav zorluğunu ve çalışma planını sınıfına ve hedefine göre uyarlar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Adın / Hitap Şekli") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Text(text = "Sınıf Seviyen", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubjectCatalog.gradeLevels.forEach { grade ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = { selectedGrade = grade },
                            label = { Text(grade, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = { Text("Akademik Hedefin (örn. LGS %1, YKS Tıp)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_goal_input")
                )

                Column {
                    Text(
                        text = "Günlük Çalışma Hedefi: ${goalMinutes.roundToLong()} dakika",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = goalMinutes,
                        onValueChange = { goalMinutes = it },
                        valueRange = 15f..240f,
                        steps = 14
                    )
                }

                Text(text = "Görünüm Teması", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu").forEach { (key, label) ->
                        FilterChip(
                            selected = themeMode == key,
                            onClick = { themeMode = key },
                            label = { Text(label) }
                        )
                    }
                }

                if (!isOnboarding) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onOpenAppCheckSetup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_appcheck_setup_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text("  Güvenlik ve App Check Durumu")
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Text("  Hesaptan Çıkış Yap")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayName,
                        selectedGrade,
                        targetGoal,
                        goalMinutes.roundToLong(),
                        themeMode
                    )
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
