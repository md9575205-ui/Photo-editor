package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Intelligent Depth and Subject Saliency Estimator.
 * Computes a normalized depth map [0.0 = foreground subject, 1.0 = distant background]
 * based on focus gradient, center saliency, high-frequency edge density, and user focal point.
 */
object DepthEstimator {

    /**
     * Estimates depth buffer for an image bitmap.
     * Returns a FloatArray of size (width * height), with values in [0.0f, 1.0f].
     */
    fun estimateDepthMap(
        bitmap: Bitmap,
        userFocalPoint: Offset = Offset(0.5f, 0.45f)
    ): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val depthMap = FloatArray(totalPixels)

        // 1. Compute sharpness / edge gradient map using a 3x3 Laplacian filter
        val sharpness = FloatArray(totalPixels)
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val c = lum(pixels[idx])
                val top = lum(pixels[idx - width])
                val bottom = lum(pixels[idx + width])
                val left = lum(pixels[idx - 1])
                val right = lum(pixels[idx + 1])

                val lap = abs(4 * c - (top + bottom + left + right))
                sharpness[idx] = min(1.0f, lap / 80.0f)
            }
        }

        // 2. Synthesize depth buffer
        val focusX = userFocalPoint.x.coerceIn(0f, 1f) * width
        val focusY = userFocalPoint.y.coerceIn(0f, 1f) * height
        val maxDist = sqrt((width * width + height * height).toDouble()).toFloat()

        for (y in 0 until height) {
            val rowOffset = y * width
            val normY = y.toFloat() / height

            for (x in 0 until width) {
                val idx = rowOffset + x
                val normX = x.toFloat() / width

                val dx = (x - focusX) / width
                val dy = (y - focusY) / height
                val distFromFocal = sqrt(dx * dx + dy * dy)

                // Vertical perspective prior: upper part of photo is typically sky/background, lower part is ground
                val verticalPrior = (1.0f - normY) * 0.40f

                // Sharpness cue: sharp pixels are at focal plane; blurry pixels are background
                val sharpCue = (1.0f - sharpness[idx]) * 0.35f

                // Color sky cue: high blue chromaticity at the top is very distant sky (depth -> 1.0)
                val pixel = pixels[idx]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val skyCue = if (normY < 0.55f && b > r * 1.15f && b > g * 1.05f) 0.35f else 0.0f

                // Distance from focal point contributes to depth distance
                val radialDistanceCue = distFromFocal * 0.85f

                // Combined depth: 0.0 is exact focal plane subject, 1.0 is far background
                val rawDepth = (radialDistanceCue + verticalPrior + sharpCue + skyCue)
                depthMap[idx] = rawDepth.coerceIn(0.0f, 1.0f)
            }
        }

        // 3. Fast box-blur smoothing of depth map to avoid hard noise edges
        return smoothDepthMap(depthMap, width, height)
    }

    private fun smoothDepthMap(input: FloatArray, width: widthInt, height: heightInt): FloatArray {
        val output = FloatArray(input.size)
        val radius = 3
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                for (dy in -radius..radius) {
                    val py = y + dy
                    if (py in 0 until height) {
                        val pOffset = py * width
                        for (dx in -radius..radius) {
                            val px = x + dx
                            if (px in 0 until width) {
                                sum += input[pOffset + px]
                                count++
                            }
                        }
                    }
                }
                output[rowOffset + x] = sum / count
            }
        }
        return output
    }

    /**
     * Renders a visual representation of the depth buffer (e.g. heatmap or grayscale)
     * so the user can inspect the AI depth estimation in real time.
     */
    fun createDepthVisualBitmap(depthMap: FloatArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in depthMap.indices) {
            val d = depthMap[i].coerceIn(0f, 1f)
            // Heatmap color gradient: 0.0 (Subject) = Warm Amber/Yellow -> 0.5 = Cyan/Green -> 1.0 (Deep Background) = Deep Violet
            val r = (sinDeg(d * 180f) * 220f + (1f - d) * 255f * 0.5f).toInt().coerceIn(0, 255)
            val g = (sinDeg(d * 160f) * 200f).toInt().coerceIn(0, 255)
            val b = (d * 240f + 15f).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun lum(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun sinDeg(deg: Float): Float {
        return kotlin.math.sin(Math.toRadians(deg.toDouble())).toFloat().coerceIn(0f, 1f)
    }
}

private typealias widthInt = Int
private typealias heightInt = Int
