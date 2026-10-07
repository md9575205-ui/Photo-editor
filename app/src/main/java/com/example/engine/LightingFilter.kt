package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.example.model.StudioLightingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Advanced Studio Lighting & iPhone 17 Portrait Relighting Engine.
 * Simulates virtual key lights, contour cheekbone highlights, stage spotlights, rim lighting, and tone mapping.
 */
object LightingFilter {

    fun applyLighting(
        input: Bitmap,
        depthMap: FloatArray,
        mode: StudioLightingMode,
        lightIntensity: Float,          // 0.0f .. 2.0f
        lightAngleDeg: Float,           // 0 .. 360 degrees
        rimIntensity: Float,            // 0.0f .. 1.0f
        exposure: Float,                // -1.0f .. 1.0f
        highlights: Float,              // -1.0f .. 1.0f
        shadows: Float,                 // -1.0f .. 1.0f
        warmth: Float,                  // -1.0f .. 1.0f
        vignette: Float,                // 0.0f .. 1.0f
        focalPoint: Offset = Offset(0.5f, 0.45f)
    ): Bitmap {
        val width = input.width
        val height = input.height
        val total = width * height
        val pixels = IntArray(total)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val angleRad = Math.toRadians(lightAngleDeg.toDouble())
        val lightDirX = cos(angleRad).toFloat()
        val lightDirY = sin(angleRad).toFloat()

        val centerX = focalPoint.x * width
        val centerY = focalPoint.y * height
        val maxDist = sqrt((width * width + height * height).toDouble()).toFloat()

        val isHighKey = (mode == StudioLightingMode.HIGH_KEY_NOIR)

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                val pixel = pixels[idx]
                val d = depthMap[idx] // 0.0 = subject, 1.0 = background

                var r = Color.red(pixel).toFloat()
                var g = Color.green(pixel).toFloat()
                var b = Color.blue(pixel).toFloat()

                // 1. Studio Relighting simulation
                val isSubject = (d < 0.45f)
                val subjectFactor = (1.0f - (d / 0.45f).coerceIn(0f, 1f))

                when (mode) {
                    StudioLightingMode.NATURAL -> {
                        // Subtle balanced subject lift
                        val lift = 1.0f + (lightIntensity - 1.0f) * 0.25f * subjectFactor
                        r *= lift
                        g *= lift
                        b *= lift
                    }

                    StudioLightingMode.STUDIO_KEY -> {
                        // Key light illumination: vector dot product relative to light source
                        val dx = (x - centerX) / width
                        val dy = (y - centerY) / height
                        val dot = (dx * lightDirX + dy * lightDirY)
                        val lightFalloff = (1.0f + dot * 0.45f).coerceIn(0.7f, 1.35f)

                        val keyMultiplier = 1.0f + (lightIntensity * 0.40f * subjectFactor * lightFalloff)
                        // Background slightly toned down for studio separation
                        val bgMultiplier = 1.0f - (d * 0.12f * (lightIntensity - 0.8f).coerceAtLeast(0f))

                        val mult = keyMultiplier * bgMultiplier
                        r *= mult
                        g *= mult
                        b *= mult
                    }

                    StudioLightingMode.CONTOUR -> {
                        // Sculpted highlights on subject with contrast gradient
                        val dx = (x - centerX) / width
                        val dy = (y - centerY) / height
                        val dist = sqrt(dx * dx + dy * dy)
                        val contourFactor = (1.2f - dist * 1.5f).coerceIn(0.6f, 1.4f)

                        val contourMultiplier = 1.0f + (lightIntensity * 0.50f * subjectFactor * (contourFactor - 1.0f))
                        val bgDim = 1.0f - (d * 0.22f)
                        val mult = contourMultiplier * bgDim
                        r *= mult
                        g *= mult
                        b *= mult
                    }

                    StudioLightingMode.STAGE_LIGHT -> {
                        // Spotlight on subject, background dramatically darkened
                        val dx = (x - centerX) / (width * 0.45f)
                        val dy = (y - centerY) / (height * 0.45f)
                        val spotDist = sqrt(dx * dx + dy * dy)
                        val spotMask = (1.0f - spotDist).coerceIn(0.0f, 1.0f)

                        // Spotlight illumination
                        val spotGlow = (spotMask * subjectFactor * lightIntensity * 0.5f)
                        val bgDrop = (1.0f - (d * 0.85f * (lightIntensity * 0.8f))).coerceIn(0.12f, 1.0f)

                        r = (r * bgDrop) * (1.0f + spotGlow)
                        g = (g * bgDrop) * (1.0f + spotGlow)
                        b = (b * bgDrop) * (1.0f + spotGlow)
                    }

                    StudioLightingMode.GOLDEN_HOUR -> {
                        // Warm sunlight beam with amber highlights
                        val dx = (x - centerX) / width
                        val dy = (y - centerY) / height
                        val dot = (dx * lightDirX + dy * lightDirY)
                        val sunBeam = (1.0f + dot * 0.55f).coerceIn(0.65f, 1.5f)

                        val glow = lightIntensity * 0.35f * sunBeam
                        r = r * (1.0f + glow * 1.25f) + glow * 25f
                        g = g * (1.0f + glow * 0.95f) + glow * 12f
                        b = b * (1.0f + glow * 0.55f)
                    }

                    StudioLightingMode.HIGH_KEY_NOIR -> {
                        // Clean monochrome studio portrait
                        val lum = (r * 0.299f + g * 0.587f + b * 0.114f)
                        val highContrastLum = if (lum > 128f) {
                            128f + (lum - 128f) * 1.35f
                        } else {
                            128f - (128f - lum) * 1.25f
                        }
                        val finalLum = (highContrastLum * lightIntensity).coerceIn(0f, 255f)
                        r = finalLum
                        g = finalLum
                        b = finalLum
                    }
                }

                // 2. Rim Light separation (boosts edge perimeter around subject)
                if (rimIntensity > 0.05f) {
                    // Depth gradient threshold: where subject transitions to background (d ~ 0.3 to 0.6)
                    val edgeTransition = (1.0f - abs(d - 0.42f) / 0.18f).coerceIn(0f, 1f)
                    if (edgeTransition > 0.1f) {
                        val rimBoost = rimIntensity * edgeTransition * 45f
                        r += rimBoost * 1.1f
                        g += rimBoost * 1.0f
                        b += rimBoost * 0.9f
                    }
                }

                // 3. Global Tone Adjustments (Exposure, Highlights, Shadows, Warmth)
                if (!isHighKey) {
                    // Exposure
                    if (exposure != 0f) {
                        val expFactor = 1.0f + exposure * 0.55f
                        r *= expFactor
                        g *= expFactor
                        b *= expFactor
                    }

                    // Highlights recovery & Shadow lift
                    val lum = (r * 0.299f + g * 0.587f + b * 0.114f)
                    if (highlights != 0f && lum > 140f) {
                        val factor = (lum - 140f) / 115f
                        val shift = highlights * factor * 35f
                        r += shift
                        g += shift
                        b += shift
                    }
                    if (shadows != 0f && lum < 115f) {
                        val factor = (115f - lum) / 115f
                        val shift = shadows * factor * 40f
                        r += shift
                        g += shift
                        b += shift
                    }

                    // Warmth color temperature
                    if (warmth != 0f) {
                        r += warmth * 28f
                        b -= warmth * 24f
                    }
                }

                // 4. Optical Vignette (lens falloff at image corners)
                if (vignette > 0.02f) {
                    val nx = (x - width * 0.5f) / (width * 0.5f)
                    val ny = (y - height * 0.5f) / (height * 0.5f)
                    val distFromCenter = sqrt(nx * nx + ny * ny)
                    if (distFromCenter > 0.55f) {
                        val vigFactor = 1.0f - (distFromCenter - 0.55f) * vignette * 0.95f
                        val clampedVig = vigFactor.coerceIn(0.15f, 1.0f)
                        r *= clampedVig
                        g *= clampedVig
                        b *= clampedVig
                    }
                }

                pixels[idx] = Color.rgb(
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                )
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
