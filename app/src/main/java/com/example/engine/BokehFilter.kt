package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.BokehShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Optical Bokeh and Depth of Field Simulation Engine.
 * Recreates DSLR large-aperture lens blur (e.g. 85mm f/1.2) and iPhone 17 computational portrait depth falloff.
 */
object BokehFilter {

    /**
     * Applies optical depth-of-field blur guided by the depth map.
     */
    fun applyDepthOfField(
        input: Bitmap,
        depthMap: FloatArray,
        blurIntensity: Float,           // 0.0f .. 1.0f
        apertureStop: Float,            // e.g. 1.2 to 16.0
        focalDepth: Float,              // 0.0f .. 1.0f
        falloffSoftness: Float,         // 0.1f .. 1.0f
        objectIsolation: Float,         // 0.0f .. 1.0f (Strict iPhone 17 object focus)
        shape: BokehShape,
        highlightBoost: Float           // Specular highlight boost (0.0f .. 1.0f)
    ): Bitmap {
        if (blurIntensity <= 0.01f || apertureStop >= 15.0f) {
            return input
        }

        val width = input.width
        val height = input.height
        val total = width * height
        val srcPixels = IntArray(total)
        input.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // 1. Calculate physical aperture radius scale
        // f/1.2 gives max radius ~24px on preview, f/2.8 gives ~10px, f/8 gives ~2px
        val apertureFactor = (1.8f / apertureStop.coerceAtLeast(1.0f))
        val baseRadius = (blurIntensity * apertureFactor * 22.0f).toInt().coerceIn(1, 35)

        // 2. Generate multi-scale blurred layers for smooth depth transitions
        val blurredHalf = fastBoxBlur(srcPixels, width, height, (baseRadius / 2).coerceAtLeast(1), shape)
        val blurredFull = fastBoxBlur(srcPixels, width, height, baseRadius, shape)

        // 3. Specular highlight extraction for bokeh balls (disc pop)
        val specularMap = if (highlightBoost > 0.05f) {
            extractBokehHighlights(srcPixels, width, height, baseRadius, highlightBoost)
        } else {
            null
        }

        // 4. Per-pixel depth-aware progressive blending
        val outPixels = IntArray(total)
        val ramp = max(0.05f, falloffSoftness)

        for (i in 0 until total) {
            val d = depthMap[i]
            // Distance in depth space from the focal plane
            val depthDiff = abs(d - focalDepth)

            // Strict object separation (iPhone 17 style):
            // When objectIsolation is high, pixels on the focused object remain strictly pin-sharp (blur = 0),
            // while background pixels receive boosted, creamy blur!
            val rawBlurWeight = ((depthDiff / ramp) * blurIntensity * (apertureFactor.coerceAtMost(2.0f)))
                .coerceIn(0.0f, 1.0f)

            val blurWeight = if (objectIsolation > 0.05f) {
                val objectTolerance = 0.16f * (1.1f - objectIsolation * 0.4f)
                if (depthDiff < objectTolerance) {
                    0.0f // Object is 100% pin-sharp and clear!
                } else {
                    val bgBoost = 1.0f + objectIsolation * 0.45f
                    (rawBlurWeight * bgBoost).coerceIn(0.0f, 1.0f)
                }
            } else {
                rawBlurWeight
            }

            if (blurWeight <= 0.05f) {
                outPixels[i] = srcPixels[i]
            } else {
                val origPixel = srcPixels[i]
                val fullPixel = blurredFull[i]
                val halfPixel = blurredHalf[i]

                // Two-tier smooth interpolation to avoid step artifacts
                val r: Int
                val g: Int
                val b: Int

                if (blurWeight < 0.5f) {
                    val t = blurWeight * 2.0f
                    r = lerp(Color.red(origPixel), Color.red(halfPixel), t)
                    g = lerp(Color.green(origPixel), Color.green(halfPixel), t)
                    b = lerp(Color.blue(origPixel), Color.blue(halfPixel), t)
                } else {
                    val t = (blurWeight - 0.5f) * 2.0f
                    r = lerp(Color.red(halfPixel), Color.red(fullPixel), t)
                    g = lerp(Color.green(halfPixel), Color.green(fullPixel), t)
                    b = lerp(Color.blue(halfPixel), Color.blue(fullPixel), t)
                }

                // Add specular bokeh highlights in blurred regions
                if (specularMap != null && blurWeight > 0.35f) {
                    val spec = specularMap[i]
                    val specWeight = (blurWeight - 0.35f) * 1.5f
                    val specR = (r + Color.red(spec) * specWeight).toInt().coerceIn(0, 255)
                    val specG = (g + Color.green(spec) * specWeight).toInt().coerceIn(0, 255)
                    val specB = (b + Color.blue(spec) * specWeight).toInt().coerceIn(0, 255)
                    outPixels[i] = Color.rgb(specR, specG, specB)
                } else {
                    outPixels[i] = Color.rgb(r, g, b)
                }
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Fast two-pass box blur with shape modulation (e.g. Anamorphic aspect ratio or Swirly).
     */
    private fun fastBoxBlur(
        src: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        shape: BokehShape
    ): IntArray {
        val total = width * height
        val temp = IntArray(total)
        val dst = IntArray(total)

        // Shape adaptations:
        // ANAMORPHIC: horizontal radius is 2x vertical radius
        val hRadius = when (shape) {
            BokehShape.ANAMORPHIC -> (radius * 1.8f).toInt().coerceAtLeast(1)
            else -> radius
        }
        val vRadius = when (shape) {
            BokehShape.ANAMORPHIC -> (radius * 0.7f).toInt().coerceAtLeast(1)
            else -> radius
        }

        // Horizontal pass
        for (y in 0 until height) {
            val rowOffset = y * width
            var rAcc = 0
            var gAcc = 0
            var bAcc = 0
            var count = 0

            for (x in -hRadius..hRadius) {
                if (x in 0 until width) {
                    val p = src[rowOffset + x]
                    rAcc += (p shr 16) and 0xFF
                    gAcc += (p shr 8) and 0xFF
                    bAcc += p and 0xFF
                    count++
                }
            }

            for (x in 0 until width) {
                temp[rowOffset + x] = Color.rgb(rAcc / count, gAcc / count, bAcc / count)

                // Slide window
                val removeX = x - hRadius
                if (removeX in 0 until width) {
                    val rp = src[rowOffset + removeX]
                    rAcc -= (rp shr 16) and 0xFF
                    gAcc -= (rp shr 8) and 0xFF
                    bAcc -= rp and 0xFF
                    count--
                }

                val addX = x + hRadius + 1
                if (addX in 0 until width) {
                    val ap = src[rowOffset + addX]
                    rAcc += (ap shr 16) and 0xFF
                    gAcc += (ap shr 8) and 0xFF
                    bAcc += ap and 0xFF
                    count++
                }
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            var rAcc = 0
            var gAcc = 0
            var bAcc = 0
            var count = 0

            for (y in -vRadius..vRadius) {
                if (y in 0 until height) {
                    val p = temp[y * width + x]
                    rAcc += (p shr 16) and 0xFF
                    gAcc += (p shr 8) and 0xFF
                    bAcc += p and 0xFF
                    count++
                }
            }

            for (y in 0 until height) {
                dst[y * width + x] = Color.rgb(rAcc / count, gAcc / count, bAcc / count)

                val removeY = y - vRadius
                if (removeY in 0 until height) {
                    val rp = temp[removeY * width + x]
                    rAcc -= (rp shr 16) and 0xFF
                    gAcc -= (rp shr 8) and 0xFF
                    bAcc -= rp and 0xFF
                    count--
                }

                val addY = y + vRadius + 1
                if (addY in 0 until height) {
                    val ap = temp[addY * width + x]
                    rAcc += (ap shr 16) and 0xFF
                    gAcc += (ap shr 8) and 0xFF
                    bAcc += ap and 0xFF
                    count++
                }
            }
        }

        return dst
    }

    /**
     * Extracts highlights that bloom into glowing circular bokeh balls in the background.
     */
    private fun extractBokehHighlights(
        pixels: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        boost: Float
    ): IntArray {
        val total = width * height
        val highlights = IntArray(total)

        // Threshold bright specular points
        val threshold = 195
        for (i in 0 until total) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = (r * 299 + g * 587 + b * 114) / 1000

            if (l > threshold) {
                val excess = (l - threshold).toFloat() / (255 - threshold)
                val factor = excess * boost * 1.5f
                highlights[i] = Color.rgb(
                    (r * factor).toInt().coerceIn(0, 255),
                    (g * factor).toInt().coerceIn(0, 255),
                    (b * factor).toInt().coerceIn(0, 255)
                )
            } else {
                highlights[i] = 0
            }
        }

        // Expand/dilate highlights into luminous bokeh discs
        val discRadius = (radius * 0.85f).toInt().coerceIn(2, 18)
        return fastBoxBlur(highlights, width, height, discRadius, BokehShape.CIRCULAR_DSLR)
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }
}
