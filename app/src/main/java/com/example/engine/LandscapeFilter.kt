package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Landscape, Scenery & Architectural Detail Enhancement Engine.
 * Intelligently detects and boosts sky tones (polarizer effect), lush tree foliage, and crisp architectural micro-contrast.
 */
object LandscapeFilter {

    fun enhanceLandscape(
        input: Bitmap,
        skyEnhance: Float,              // 0.0f .. 1.0f (deep cobalt sky & cloud clarity)
        foliagePop: Float,              // 0.0f .. 1.0f (lush tree greens vibrancy)
        architecturalClarity: Float,    // 0.0f .. 1.0f (structural edge micro-contrast)
        smartHdr: Float                 // 0.0f .. 1.0f (dynamic range tone expansion)
    ): Bitmap {
        if (skyEnhance <= 0.01f && foliagePop <= 0.01f && architecturalClarity <= 0.01f && smartHdr <= 0.01f) {
            return input
        }

        val width = input.width
        val height = input.height
        val total = width * height
        val pixels = IntArray(total)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        // Precompute high-pass / structural clarity map if architecturalClarity > 0
        val highPass = if (architecturalClarity > 0.02f) {
            computeStructuralClarityMap(pixels, width, height)
        } else {
            null
        }

        for (y in 0 until height) {
            val rowOffset = y * width
            val normY = y.toFloat() / height

            for (x in 0 until width) {
                val idx = rowOffset + x
                val p = pixels[idx]

                var r = Color.red(p).toFloat()
                var g = Color.green(p).toFloat()
                var b = Color.blue(p).toFloat()

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f
                val lum = (r * 0.299f + g * 0.587f + b * 0.114f)

                // 1. SKY ENHANCER (Sky blue & cloud polarizer)
                // Sky condition: upper 70% of image, blue dominant or bright cloud highlights
                if (skyEnhance > 0.01f && normY < 0.70f) {
                    val isBlueSky = (b > r * 1.08f && b > g * 0.98f && b > 70f)
                    val isCloud = (normY < 0.45f && lum > 175f && sat < 0.35f)

                    if (isBlueSky) {
                        // Deepen sky blues: increase blue saturation, slightly pull red down (polarizing filter effect)
                        val skyFactor = skyEnhance * (1.1f - normY * 0.5f)
                        b = (b * (1.0f + 0.38f * skyFactor)).coerceAtMost(255f)
                        r = (r * (1.0f - 0.20f * skyFactor)).coerceAtLeast(0f)
                        g = (g * (1.0f + 0.08f * skyFactor)).coerceAtMost(255f)
                    } else if (isCloud) {
                        // Enhance cloud volumetric clarity & dynamic range
                        val cloudContrast = skyEnhance * 0.22f
                        if (lum > 210f) {
                            // Preserve cloud highlights without clipping
                            r = (r * (1.0f - cloudContrast * 0.08f)).coerceAtLeast(0f)
                            g = (g * (1.0f - cloudContrast * 0.08f)).coerceAtLeast(0f)
                            b = (b * (1.0f - cloudContrast * 0.08f)).coerceAtLeast(0f)
                        } else {
                            // Deepen shadows beneath clouds for 3D depth
                            val cloudShadow = (1.0f - cloudContrast * 0.15f)
                            r *= cloudShadow
                            g *= cloudShadow
                            b *= cloudShadow
                        }
                    }
                }

                // 2. FOLIAGE & TREE POP
                // Chlorophyll condition: green dominant (G > R and G > B)
                // Crucially protects human skin tones (where R is highest, R > G > B)
                if (foliagePop > 0.01f) {
                    val isFoliage = (g > r * 1.04f && g > b * 1.08f && g > 45f)
                    if (isFoliage) {
                        val greenDiff = (g - max(r, b)) / 255f
                        val pop = foliagePop * (0.45f + greenDiff * 0.65f)

                        // Boost emerald/lime vibrance while adding rich depth
                        g = (g * (1.0f + pop * 0.42f)).coerceAtMost(255f)
                        r = (r * (1.0f - pop * 0.12f)).coerceAtLeast(0f)
                        b = (b * (1.0f - pop * 0.08f)).coerceAtLeast(0f)
                    }
                }

                // 3. ARCHITECTURAL CLARITY & MICRO-CONTRAST
                // Enhances local structural edge details (stone, brick, glass, frame lines)
                if (highPass != null && architecturalClarity > 0.01f) {
                    val edge = highPass[idx] // -128f .. +128f
                    val edgePop = edge * architecturalClarity * 1.35f
                    r += edgePop
                    g += edgePop
                    b += edgePop
                }

                // 4. SMART HDR (tone mapping extremes)
                if (smartHdr > 0.01f) {
                    if (lum < 90f) {
                        // Lift dark shadow foliage and architectural details
                        val lift = (90f - lum) / 90f * smartHdr * 28f
                        r += lift
                        g += lift
                        b += lift
                    } else if (lum > 200f) {
                        // Recover blown sky highlights
                        val drop = (lum - 200f) / 55f * smartHdr * 22f
                        r -= drop
                        g -= drop
                        b -= drop
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

    /**
     * Extracts high-frequency edge micro-contrast using a fast 3x3 local filter.
     */
    private fun computeStructuralClarityMap(pixels: IntArray, width: Int, height: Int): FloatArray {
        val total = width * height
        val result = FloatArray(total)

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val centerLum = lum(pixels[idx])
                val top = lum(pixels[idx - width])
                val bottom = lum(pixels[idx + width])
                val left = lum(pixels[idx - 1])
                val right = lum(pixels[idx + 1])

                val localAvg = (top + bottom + left + right) / 4f
                val delta = centerLum - localAvg

                // Clamp to prevent harsh digital noise
                result[idx] = delta.coerceIn(-35f, 35f)
            }
        }

        return result
    }

    private fun lum(p: Int): Float {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return r * 0.299f + g * 0.587f + b * 0.114f
    }
}
