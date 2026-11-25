package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3): Vec3 = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )
    fun normalized(): Vec3 {
        val len = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-5f)
        return Vec3(x / len, y / len, z / len)
    }
}

data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val zDepth: Float,
    val scale: Float
)

data class Face3d(
    val vertices: List<Vec3>,
    val color: Color,
    val id: String = "",
    val wireframe: Boolean = false
)

data class HitTarget3d(
    val id: String,
    val centerScreen: Offset,
    val radiusPx: Float,
    val depth: Float
)

object Engine3d {

    private val lightDir = Vec3(0.45f, -0.65f, -0.62f).normalized()

    fun rotate(v: Vec3, yawRad: Float, pitchRad: Float): Vec3 {
        val cy = cos(yawRad)
        val sy = sin(yawRad)
        val x1 = v.x * cy + v.z * sy
        val z1 = -v.x * sy + v.z * cy

        val cp = cos(pitchRad)
        val sp = sin(pitchRad)
        val y2 = v.y * cp - z1 * sp
        val z2 = v.y * sp + z1 * cp
        return Vec3(x1, y2, z2)
    }

    fun project(
        v: Vec3,
        yawRad: Float,
        pitchRad: Float,
        centerX: Float,
        centerY: Float,
        fovScale: Float,
        zoom: Float = 1f,
        cameraDist: Float = 4.6f
    ): ProjectedPoint {
        val r = rotate(v, yawRad, pitchRad)
        val denom = (cameraDist + r.z).coerceAtLeast(0.6f)
        val perspective = (fovScale * zoom) / denom
        return ProjectedPoint(
            x = centerX + r.x * perspective,
            y = centerY + r.y * perspective,
            zDepth = r.z,
            scale = perspective
        )
    }

    fun shadeColor(base: Color, normal: Vec3): Color {
        val n = normal.normalized()
        val lambert = (-n.dot(lightDir)).coerceIn(0.18f, 1.0f)
        val factor = 0.35f + 0.65f * lambert
        return Color(
            red = (base.red * factor).coerceIn(0f, 1f),
            green = (base.green * factor).coerceIn(0f, 1f),
            blue = (base.blue * factor).coerceIn(0f, 1f),
            alpha = base.alpha
        )
    }

    fun DrawScope.drawShadedMesh(
        faces: List<Face3d>,
        yawRad: Float,
        pitchRad: Float,
        zoom: Float,
        fovScale: Float = minOf(size.width, size.height) * 0.45f,
        cameraDist: Float = 4.6f
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        data class RenderableFace(
            val points: List<ProjectedPoint>,
            val avgDepth: Float,
            val shadedColor: Color,
            val wireframe: Boolean
        )

        val sorted = faces.mapNotNull { face ->
            if (face.vertices.size < 3) return@mapNotNull null
            val rotated = face.vertices.map { rotate(it, yawRad, pitchRad) }
            val avgZ = rotated.map { it.z }.average().toFloat()
            val edge1 = rotated[1] - rotated[0]
            val edge2 = rotated[2] - rotated[0]
            val normal = edge1.cross(edge2)
            val projected = rotated.map { r ->
                val denom = (cameraDist + r.z).coerceAtLeast(0.6f)
                val persp = (fovScale * zoom) / denom
                ProjectedPoint(cx + r.x * persp, cy + r.y * persp, r.z, persp)
            }
            RenderableFace(
                points = projected,
                avgDepth = avgZ,
                shadedColor = shadeColor(face.color, normal),
                wireframe = face.wireframe
            )
        }.sortedByDescending { it.avgDepth }

        sorted.forEach { rf ->
            val path = Path().apply {
                moveTo(rf.points.first().x, rf.points.first().y)
                for (i in 1 until rf.points.size) {
                    lineTo(rf.points[i].x, rf.points[i].y)
                }
                close()
            }
            if (!rf.wireframe) {
                drawPath(path = path, color = rf.shadedColor)
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = if (rf.wireframe) 0.72f else 0.25f),
                style = Stroke(width = 1.6f)
            )
        }
    }

    fun DrawScope.drawShadedSphere(
        center: Offset,
        radius: Float,
        baseColor: Color,
        glowColor: Color = baseColor,
        selected: Boolean = false
    ) {
        val safeR = max(radius, 4f)
        if (selected) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = safeR + 5f,
                center = center,
                style = Stroke(width = 2.6f)
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.34f), Color.Transparent),
                center = center,
                radius = safeR * 1.65f
            ),
            radius = safeR * 1.65f,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    baseColor,
                    Color(
                        red = baseColor.red * 0.32f,
                        green = baseColor.green * 0.32f,
                        blue = baseColor.blue * 0.38f
                    )
                ),
                center = Offset(center.x - safeR * 0.28f, center.y - safeR * 0.32f),
                radius = safeR * 1.25f
            ),
            radius = safeR,
            center = center
        )
    }

    fun findHitTarget(tap: Offset, targets: List<HitTarget3d>): String? {
        return targets
            .sortedBy { it.depth }
            .firstOrNull { target ->
                val dx = tap.x - target.centerScreen.x
                val dy = tap.y - target.centerScreen.y
                sqrt(dx * dx + dy * dy) <= (target.radiusPx + 18f)
            }?.id
    }

    fun cubeFaces(halfSize: Float = 0.95f, palette: List<Color>): List<Face3d> {
        val s = halfSize
        val v0 = Vec3(-s, -s, -s)
        val v1 = Vec3(s, -s, -s)
        val v2 = Vec3(s, s, -s)
        val v3 = Vec3(-s, s, -s)
        val v4 = Vec3(-s, -s, s)
        val v5 = Vec3(s, -s, s)
        val v6 = Vec3(s, s, s)
        val v7 = Vec3(-s, s, s)
        fun col(i: Int) = palette[i % palette.size]
        return listOf(
            Face3d(listOf(v0, v1, v2, v3), col(0), "front"),
            Face3d(listOf(v5, v4, v7, v6), col(1), "back"),
            Face3d(listOf(v4, v0, v3, v7), col(2), "left"),
            Face3d(listOf(v1, v5, v6, v2), col(3), "right"),
            Face3d(listOf(v4, v5, v1, v0), col(4), "top"),
            Face3d(listOf(v3, v2, v6, v7), col(5), "bottom")
        )
    }

    fun pyramidFaces(halfBase: Float = 1.0f, height: Float = 1.45f, palette: List<Color>): List<Face3d> {
        val apex = Vec3(0f, -height * 0.55f, 0f)
        val by = height * 0.45f
        val b0 = Vec3(-halfBase, by, -halfBase)
        val b1 = Vec3(halfBase, by, -halfBase)
        val b2 = Vec3(halfBase, by, halfBase)
        val b3 = Vec3(-halfBase, by, halfBase)
        fun col(i: Int) = palette[i % palette.size]
        return listOf(
            Face3d(listOf(b0, b1, b2, b3), col(0), "base"),
            Face3d(listOf(b0, b1, apex), col(1), "f1"),
            Face3d(listOf(b1, b2, apex), col(2), "f2"),
            Face3d(listOf(b2, b3, apex), col(3), "f3"),
            Face3d(listOf(b3, b0, apex), col(4), "f4")
        )
    }

    fun prismFaces(rx: Float = 0.9f, ry: Float = 0.9f, halfLen: Float = 1.15f, sides: Int = 6, palette: List<Color>): List<Face3d> {
        val frontPts = (0 until sides).map { i ->
            val a = (2.0 * Math.PI * i / sides).toFloat()
            Vec3(cos(a) * rx, sin(a) * ry, -halfLen)
        }
        val backPts = frontPts.map { it.copy(z = halfLen) }
        val faces = mutableListOf<Face3d>()
        faces.add(Face3d(frontPts, palette[0], "front_cap"))
        faces.add(Face3d(backPts.reversed(), palette[1 % palette.size], "back_cap"))
        for (i in 0 until sides) {
            val next = (i + 1) % sides
            faces.add(
                Face3d(
                    listOf(frontPts[i], frontPts[next], backPts[next], backPts[i]),
                    palette[(i + 2) % palette.size],
                    "side_$i"
                )
            )
        }
        return faces
    }

    fun coneFaces(radius: Float = 1.0f, height: Float = 1.6f, segments: Int = 12, palette: List<Color>): List<Face3d> {
        val apex = Vec3(0f, -height * 0.55f, 0f)
        val by = height * 0.45f
        val ring = (0 until segments).map { i ->
            val a = (2.0 * Math.PI * i / segments).toFloat()
            Vec3(cos(a) * radius, by, sin(a) * radius)
        }
        val faces = mutableListOf<Face3d>()
        faces.add(Face3d(ring, palette[0], "base"))
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            faces.add(Face3d(listOf(ring[i], ring[next], apex), palette[(i + 1) % palette.size], "tri_$i"))
        }
        return faces
    }
}
