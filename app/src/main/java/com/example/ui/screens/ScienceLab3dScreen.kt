package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ScienceLabModule
import com.example.ui.components.Engine3d
import com.example.ui.components.Engine3d.drawShadedMesh
import com.example.ui.components.Engine3d.drawShadedSphere
import com.example.ui.components.GlassCard
import com.example.ui.components.HitTarget3d
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.ThreeJsSceneParams
import com.example.ui.components.ThreeJsScienceLabViewport
import com.example.ui.components.Vec3
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScienceLab3dScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onQuizCompleted: (subject: String, correct: Int, total: Int) -> Unit
) {
    BackHandler { onBack() }

    var activeModule by remember { mutableStateOf(ScienceLabModule.SOLAR_SYSTEM) }
    var yaw by remember { mutableFloatStateOf(0.45f) }
    var pitch by remember { mutableFloatStateOf(0.32f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var simTime by remember { mutableFloatStateOf(0f) }
    var selectedPartInfo by remember { mutableStateOf<String?>(null) }

    // Module-specific interactive controls
    var orbitSpeed by remember { mutableFloatStateOf(1.0f) }
    var eclipseAngle by remember { mutableFloatStateOf(0.0f) } // 0 = Güneş Tutulması çizgisi
    var forceNewton by remember { mutableFloatStateOf(20f) }
    var massKg by remember { mutableFloatStateOf(4f) }
    var circuitClosed by remember { mutableStateOf(true) }
    var voltageV by remember { mutableFloatStateOf(12f) }
    var resistanceOhm by remember { mutableFloatStateOf(4f) }
    var temperatureC by remember { mutableFloatStateOf(25f) }
    var geometryShapeIdx by remember { mutableIntStateOf(0) } // 0 Cube, 1 Pyramid, 2 Prism, 3 Cone
    var quizAnswerIdx by remember { mutableIntStateOf(-1) }
    var useThreeJsEngine by remember(state.studioState.lowPower3dMode) {
        mutableStateOf(!state.studioState.lowPower3dMode)
    }
    var threeJsFallbackMessage by remember { mutableStateOf<String?>(null) }
    var cameraResetCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeModule, orbitSpeed, state.studioState.lowPower3dMode) {
        quizAnswerIdx = -1
        selectedPartInfo = null
        if (state.studioState.lowPower3dMode) return@LaunchedEffect
        while (true) {
            delay(32L)
            simTime = (simTime + 0.035f * orbitSpeed) % 62.83f
        }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "3B Bilim Laboratuvarı",
                subtitle = activeModule.title,
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = when {
                    state.studioState.lowPower3dMode -> "2D Tasarruf"
                    useThreeJsEngine -> "Three.js 3B"
                    else -> "Yerel 3B"
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(18.dp))) {
                        Image(
                            painter = painterResource(R.drawable.img_science_lab_banner),
                            contentDescription = "Bilim Laboratuvarı",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.72f
                        )
                        Surface(color = Color(0xAA080D1A), modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
                                Text("Etkileşimli 3B Fen, Fizik & Geometri Sahnesi", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Parmağınla döndür • Yakınlaştır • Parçaya dokunarak incele", color = CyberCyanTertiary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScienceLabModule.entries.forEach { mod ->
                            FilterChip(
                                selected = activeModule == mod,
                                onClick = { activeModule = mod },
                                label = { Text(mod.title) },
                                modifier = Modifier.testTag("lab_mod_${mod.name}")
                            )
                        }
                    }
                }

                // 3D Viewport
                item {
                    GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = useThreeJsEngine,
                                    onClick = {
                                        threeJsFallbackMessage = null
                                        useThreeJsEngine = true
                                    },
                                    label = { Text("Three.js WebGL 3B") },
                                    modifier = Modifier.testTag("lab_engine_threejs")
                                )
                                FilterChip(
                                    selected = !useThreeJsEngine,
                                    onClick = { useThreeJsEngine = false },
                                    label = { Text("Yerel Hafif 3B") },
                                    modifier = Modifier.testTag("lab_engine_canvas")
                                )
                            }

                            if (!threeJsFallbackMessage.isNullOrBlank()) {
                                Text(
                                    text = threeJsFallbackMessage.orEmpty(),
                                    color = WarningAmber,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            if (useThreeJsEngine && !state.studioState.lowPower3dMode) {
                                ThreeJsScienceLabViewport(
                                    params = ThreeJsSceneParams(
                                        module = activeModule,
                                        zoom = zoom,
                                        orbitSpeed = orbitSpeed,
                                        eclipseAngle = eclipseAngle,
                                        forceNewton = forceNewton,
                                        massKg = massKg,
                                        circuitClosed = circuitClosed,
                                        voltageV = voltageV,
                                        resistanceOhm = resistanceOhm,
                                        temperatureC = temperatureC,
                                        geometryShapeIdx = geometryShapeIdx,
                                        resetCounter = cameraResetCounter
                                    ),
                                    onPartSelected = { selectedPartInfo = it },
                                    onWebGlError = { err ->
                                        threeJsFallbackMessage = "WebGL başlatılamadığı için Yerel 3B görünümüne geçildi: $err"
                                        useThreeJsEngine = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(275.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("threejs_science_viewport")
                                )
                            } else {
                                var hitTargets by remember { mutableStateOf<List<HitTarget3d>>(emptyList()) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(275.dp)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, drag ->
                                                change.consume()
                                                yaw += drag.x * 0.01f
                                                pitch = (pitch + drag.y * 0.01f).coerceIn(-1.35f, 1.35f)
                                            }
                                        }
                                        .pointerInput(hitTargets) {
                                            detectTapGestures { tap ->
                                                val hit = Engine3d.findHitTarget(tap, hitTargets)
                                                if (hit != null) selectedPartInfo = hit
                                            }
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                    val targets = mutableListOf<HitTarget3d>()
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val fov = minOf(size.width, size.height) * 0.42f

                                    when (activeModule) {
                                        ScienceLabModule.SOLAR_SYSTEM -> {
                                            val sunP = Engine3d.project(Vec3(0f, 0f, 0f), yaw, pitch, cx, cy, fov, zoom)
                                            drawShadedSphere(Offset(sunP.x, sunP.y), (sunP.scale * 0.45f), Color(0xFFF59E0B), Color(0xFFFBBF24))
                                            targets.add(HitTarget3d("Güneş: Yüzey sıcaklığı ~5500°C, sistemin kütle merkezidir.", Offset(sunP.x, sunP.y), sunP.scale * 0.45f, sunP.zDepth))
                                            val planets = listOf(
                                                Triple(1.05f, Color(0xFF94A3B8), "Merkür: Güneş'e en yakın, atmosferi en ince gezegen."),
                                                Triple(1.48f, Color(0xFF38BDF8), "Dünya: Yaşam barındıran, %71'i su kaplı 3. gezegen."),
                                                Triple(1.92f, Color(0xFFEF4444), "Mars: Demir oksitten dolayı Kızıl Gezegen olarak bilinir."),
                                                Triple(2.45f, Color(0xFFD97706), "Jüpiter: Güneş Sistemi'nin en büyük gaz devi.")
                                            )
                                            planets.forEachIndexed { idx, (rOrbit, col, info) ->
                                                val angle = simTime * (1.5f / (idx + 1)) + idx * 1.4f
                                                val pos = Vec3(cos(angle) * rOrbit, 0f, sin(angle) * rOrbit)
                                                val p = Engine3d.project(pos, yaw, pitch, cx, cy, fov, zoom)
                                                drawCircle(Color.White.copy(alpha = 0.14f), radius = rOrbit * fov * zoom * 0.22f, center = Offset(sunP.x, sunP.y), style = Stroke(1.4f))
                                                val rad = p.scale * (0.14f + idx * 0.03f)
                                                drawShadedSphere(Offset(p.x, p.y), rad, col)
                                                targets.add(HitTarget3d(info, Offset(p.x, p.y), rad, p.zDepth))
                                            }
                                        }
                                        ScienceLabModule.ECLIPSE_SIM -> {
                                            val sunP = Engine3d.project(Vec3(-1.9f, 0f, 0f), yaw, pitch, cx, cy, fov, zoom)
                                            val earthP = Engine3d.project(Vec3(0.4f, 0f, 0f), yaw, pitch, cx, cy, fov, zoom)
                                            val moonPos = Vec3(0.4f + cos(eclipseAngle) * 1.15f, sin(eclipseAngle * 0.5f) * 0.3f, sin(eclipseAngle) * 1.15f)
                                            val moonP = Engine3d.project(moonPos, yaw, pitch, cx, cy, fov, zoom)
                                            drawShadedSphere(Offset(sunP.x, sunP.y), sunP.scale * 0.48f, Color(0xFFFBBF24))
                                            drawShadedSphere(Offset(earthP.x, earthP.y), earthP.scale * 0.30f, ElectricBlueSecondary)
                                            drawShadedSphere(Offset(moonP.x, moonP.y), moonP.scale * 0.14f, Color(0xFFE2E8F0))
                                            targets.add(HitTarget3d("Ay, Dünya ile Güneş arasına girdiğinde Güneş Tutulması olur!", Offset(moonP.x, moonP.y), 30f, moonP.zDepth))
                                        }
                                        ScienceLabModule.FORCE_MOTION -> {
                                            val accel = forceNewton / massKg.coerceAtLeast(1f)
                                            val shiftX = (sin(simTime * (0.4f + accel * 0.1f)) * 1.4f)
                                            drawLine(Color(0xFF475569), Offset(40f, cy + 40f), Offset(size.width - 40f, cy + 40f), 6f)
                                            val cubeFaces = Engine3d.cubeFaces(0.45f + (massKg / 25f), listOf(NeonPurplePrimary, ElectricBlueSecondary, CyberCyanTertiary))
                                                .map { f -> f.copy(vertices = f.vertices.map { it + Vec3(shiftX, 0f, 0f) }) }
                                            drawShadedMesh(cubeFaces, yaw, pitch, zoom, fov)
                                        }
                                        ScienceLabModule.PLANT_CELL, ScienceLabModule.HUMAN_ORGANS -> {
                                            val isCell = activeModule == ScienceLabModule.PLANT_CELL
                                            val shell = Engine3d.cubeFaces(1.1f, listOf(if (isCell) SuccessGreen.copy(alpha = 0.25f) else DangerRed.copy(alpha = 0.25f)))
                                                .map { it.copy(wireframe = true) }
                                            drawShadedMesh(shell, yaw, pitch, zoom, fov)
                                            val items = if (isCell) {
                                                listOf(
                                                    Triple(Vec3(0f, 0f, 0f), NeonPurplePrimary, "Çekirdek: Hücrenin yönetim ve DNA merkezidir."),
                                                    Triple(Vec3(0.55f, 0.3f, 0.2f), WarningAmber, "Mitokondri: Oksijenli solunumla ATP (enerji) üretir."),
                                                    Triple(Vec3(-0.55f, -0.25f, 0.3f), SuccessGreen, "Kloroplast: Bitkilerde fotosentez yaparak besin üretir.")
                                                )
                                            } else {
                                                listOf(
                                                    Triple(Vec3(0f, -0.6f, 0f), Color(0xFFF472B6), "Beyin: Sinir sisteminin komuta merkezidir."),
                                                    Triple(Vec3(-0.35f, 0.15f, 0.2f), DangerRed, "Kalp: Kanı vücuda pompalayan kaslı organdır."),
                                                    Triple(Vec3(0.45f, 0.1f, -0.2f), CyberCyanTertiary, "Akciğerler: Oksijen ve karbondioksit gaz alışverişini sağlar.")
                                                )
                                            }
                                            items.forEach { (pos, col, info) ->
                                                val p = Engine3d.project(pos, yaw, pitch, cx, cy, fov, zoom)
                                                drawShadedSphere(Offset(p.x, p.y), p.scale * 0.24f, col)
                                                targets.add(HitTarget3d(info, Offset(p.x, p.y), p.scale * 0.24f, p.zDepth))
                                            }
                                        }
                                        ScienceLabModule.ELECTRIC_CIRCUIT -> {
                                            val currentAmp = if (circuitClosed) voltageV / resistanceOhm.coerceAtLeast(1f) else 0f
                                            val wireCol = if (circuitClosed) CyberCyanTertiary else Color(0xFF64748B)
                                            drawRect(wireCol, topLeft = Offset(cx - 120f, cy - 70f), size = androidx.compose.ui.geometry.Size(240f, 140f), style = Stroke(width = 6f))
                                            val bulbGlow = (currentAmp / 12f).coerceIn(0.15f, 1f)
                                            drawShadedSphere(Offset(cx, cy - 70f), 28f, if (circuitClosed) Color(0xFFFBBF24).copy(alpha = bulbGlow) else Color(0xFF475569))
                                            targets.add(HitTarget3d("Ampul: Akım I = V/R = ${String.format(java.util.Locale.US, "%.2f", currentAmp)} A", Offset(cx, cy - 70f), 32f, 0f))
                                        }
                                        ScienceLabModule.MATTER_STATES -> {
                                            val agitation = ((temperatureC + 30f) / 160f).coerceIn(0.05f, 1f)
                                            for (gx in -1..1) for (gy in -1..1) {
                                                val spread = 0.45f + agitation * 0.9f
                                                val jx = sin(simTime * (2f + agitation * 8f) + gx) * agitation * 0.22f
                                                val jy = cos(simTime * (2f + agitation * 8f) + gy) * agitation * 0.22f
                                                val p = Engine3d.project(Vec3(gx * spread + jx, gy * spread + jy, 0f), yaw, pitch, cx, cy, fov, zoom)
                                                drawShadedSphere(Offset(p.x, p.y), p.scale * 0.16f, if (temperatureC > 100f) DangerRed else if (temperatureC > 0f) ElectricBlueSecondary else CyberCyanTertiary)
                                            }
                                        }
                                        ScienceLabModule.GEOMETRY_3D -> {
                                            val pal = listOf(NeonPurplePrimary, ElectricBlueSecondary, CyberCyanTertiary, SuccessGreen, WarningAmber)
                                            val faces = when (geometryShapeIdx) {
                                                0 -> Engine3d.cubeFaces(0.95f, pal)
                                                1 -> Engine3d.pyramidFaces(1.05f, 1.5f, pal)
                                                2 -> Engine3d.prismFaces(0.85f, 0.85f, 1.15f, 6, pal)
                                                else -> Engine3d.coneFaces(1.0f, 1.6f, 12, pal)
                                            }
                                            drawShadedMesh(faces, yaw, pitch, zoom, fov)
                                        }
                                    }
                                        hitTargets = targets
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Yakınlaştırma: ${String.format(java.util.Locale.US, "%.1fx", zoom)}", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                                Button(
                                    onClick = {
                                        yaw = 0.45f
                                        pitch = 0.32f
                                        zoom = 1.0f
                                        cameraResetCounter++
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Sıfırla", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.6f..1.8f)

                            if (!selectedPartInfo.isNullOrBlank()) {
                                Surface(shape = RoundedCornerShape(12.dp), color = CyberCyanTertiary.copy(alpha = 0.16f), border = BorderStroke(1.dp, CyberCyanTertiary)) {
                                    Text(selectedPartInfo.orEmpty(), color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                    }
                }

                // Parameter Controls per Module
                item {
                    GlassCard(accentBorder = ElectricBlueSecondary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Deney Parametreleri & Formül", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            when (activeModule) {
                                ScienceLabModule.SOLAR_SYSTEM -> {
                                    Text("Yörünge Hızı: ${String.format(java.util.Locale.US, "%.1fx", orbitSpeed)}", color = CyberCyanTertiary)
                                    Slider(value = orbitSpeed, onValueChange = { orbitSpeed = it }, valueRange = 0f..3f)
                                }
                                ScienceLabModule.ECLIPSE_SIM -> {
                                    val isSolarEclipse = kotlin.math.abs(cos(eclipseAngle) + 1f) < 0.25f
                                    Text(if (isSolarEclipse) "Konum: Güneş Tutulması Hizası!" else "Ay Yörünge Açısı", color = CyberCyanTertiary)
                                    Slider(value = eclipseAngle, onValueChange = { eclipseAngle = it }, valueRange = 0f..6.28f)
                                }
                                ScienceLabModule.FORCE_MOTION -> {
                                    val a = forceNewton / massKg.coerceAtLeast(1f)
                                    Text("Kuvvet F=${forceNewton.toInt()} N  |  Kütle m=${massKg.toInt()} kg  →  İvme a=${String.format(java.util.Locale.US, "%.2f", a)} m/s²", color = CyberCyanTertiary)
                                    Slider(value = forceNewton, onValueChange = { forceNewton = it }, valueRange = 5f..60f)
                                    Slider(value = massKg, onValueChange = { massKg = it }, valueRange = 1f..20f)
                                }
                                ScienceLabModule.ELECTRIC_CIRCUIT -> {
                                    val iAmp = if (circuitClosed) voltageV / resistanceOhm else 0f
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Anahtar: ${if (circuitClosed) "Kapalı (Akım Var)" else "Açık"}", color = Color.White)
                                        Switch(checked = circuitClosed, onCheckedChange = { circuitClosed = it })
                                    }
                                    Text("V = ${voltageV.toInt()}V, R = ${resistanceOhm.toInt()}Ω → I = ${String.format(java.util.Locale.US, "%.2f", iAmp)} A", color = CyberCyanTertiary)
                                    Slider(value = voltageV, onValueChange = { voltageV = it }, valueRange = 3f..24f)
                                    Slider(value = resistanceOhm, onValueChange = { resistanceOhm = it }, valueRange = 1f..12f)
                                }
                                ScienceLabModule.MATTER_STATES -> {
                                    val stateName = when {
                                        temperatureC < 0f -> "Katı (Buz) • Sıkı titreşim"
                                        temperatureC < 100f -> "Sıvı (Su) • Akışkan bağlar"
                                        else -> "Gaz (Buhar) • Hızlı ve serbest hareket"
                                    }
                                    Text("Sıcaklık: ${temperatureC.toInt()} °C → $stateName", color = CyberCyanTertiary)
                                    Slider(value = temperatureC, onValueChange = { temperatureC = it }, valueRange = -25f..140f)
                                }
                                ScienceLabModule.GEOMETRY_3D -> {
                                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Küp (6 yüz)", "Piramit (5 yüz)", "Altıgen Prizma", "Koni").forEachIndexed { idx, label ->
                                            FilterChip(selected = geometryShapeIdx == idx, onClick = { geometryShapeIdx = idx }, label = { Text(label) })
                                        }
                                    }
                                }
                                else -> Text("Yukarıdaki 3B modeldeki kürelere dokunarak organ/organel bilgisini oku.", color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }

                // Module Mini Quiz
                item {
                    val (qText, opts, ans) = remember(activeModule) { moduleMiniQuiz(activeModule) }
                    GlassCard(accentBorder = SuccessGreen, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Text("Laboratuvar Kontrol Sorusu (+25 XP)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(qText, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodyMedium)
                            opts.forEachIndexed { idx, opt ->
                                val isSel = quizAnswerIdx == idx
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) (if (idx == ans) SuccessGreen.copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f)) else Color(0xFF172038),
                                    border = BorderStroke(1.dp, if (isSel && idx == ans) SuccessGreen else Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = quizAnswerIdx == -1) {
                                        quizAnswerIdx = idx
                                        onQuizCompleted(activeModule.subject, if (idx == ans) 1 else 0, 1)
                                    }
                                ) {
                                    Text("${'A' + idx}) $opt", color = Color.White, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun moduleMiniQuiz(mod: ScienceLabModule): Triple<String, List<String>, Int> = when (mod) {
    ScienceLabModule.SOLAR_SYSTEM -> Triple("Güneş Sistemindeki en büyük gezegen hangisidir?", listOf("Mars", "Venüs", "Jüpiter", "Merkür"), 2)
    ScienceLabModule.ECLIPSE_SIM -> Triple("Güneş tutulması gerçekleşirken hangisi ortada bulunur?", listOf("Dünya", "Ay", "Güneş", "Mars"), 1)
    ScienceLabModule.FORCE_MOTION -> Triple("20 N kuvvet 5 kg kütleye etki ederse ivme kaç m/s² olur?", listOf("2", "4", "10", "25"), 1)
    ScienceLabModule.PLANT_CELL -> Triple("Bitki hücresinde fotosentez hangi organelde yapılır?", listOf("Kloroplast", "Golgi", "Ribozom", "Sentrozom"), 0)
    ScienceLabModule.HUMAN_ORGANS -> Triple("Vücuda kan pompalamaktan sorumlu ana organ hangisidir?", listOf("Mide", "Akciğer", "Kalp", "Böbrek"), 2)
    ScienceLabModule.ELECTRIC_CIRCUIT -> Triple("Ohm Yasasına göre akım şiddeti formülü hangisidir?", listOf("I = V · R", "I = V / R", "V = I / R", "R = V · I"), 1)
    ScienceLabModule.MATTER_STATES -> Triple("Tanecikleri yalnızca titreşim hareketi yapan hâl hangisidir?", listOf("Katı", "Sıvı", "Gaz", "Plazma"), 0)
    ScienceLabModule.GEOMETRY_3D -> Triple("Bir küpün toplam kaç yüzü vardır?", listOf("4", "5", "6", "8"), 2)
}
