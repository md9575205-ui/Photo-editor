package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.example.model.EditParameters
import com.example.model.QuickPreset
import com.example.model.SceneAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Unified Photo Processor orchestrating depth-of-field, lighting, and landscape filters.
 */
class PhotoProcessor {

    private var cachedBaseBitmap: Bitmap? = null
    private var cachedFocalPoint: Offset? = null
    private var cachedDepthMap: FloatArray? = null
    private var cachedDepthVisual: Bitmap? = null
    private var cachedSceneAnalysis: SceneAnalysis? = null

    /**
     * Prepares and caches the depth map for a given bitmap and focal point.
     */
    suspend fun prepareDepthMap(bitmap: Bitmap, focalPoint: Offset): Pair<FloatArray, Bitmap> = withContext(Dispatchers.Default) {
        val sameBitmap = (cachedBaseBitmap == bitmap)
        val sameFocal = (cachedFocalPoint == focalPoint)

        if (sameBitmap && sameFocal && cachedDepthMap != null && cachedDepthVisual != null) {
            return@withContext Pair(cachedDepthMap!!, cachedDepthVisual!!)
        }

        val depthMap = DepthEstimator.estimateDepthMap(bitmap, focalPoint)
        val depthVisual = DepthEstimator.createDepthVisualBitmap(depthMap, bitmap.width, bitmap.height)

        cachedBaseBitmap = bitmap
        cachedFocalPoint = focalPoint
        cachedDepthMap = depthMap
        cachedDepthVisual = depthVisual
        cachedSceneAnalysis = analyzeScene(bitmap, depthMap)

        Pair(depthMap, depthVisual)
    }

    /**
     * Executes the complete processing pipeline on an image with the given parameters.
     */
    suspend fun processPhoto(
        bitmap: Bitmap,
        params: EditParameters
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Ensure depth map is computed
        val (depthMap, _) = prepareDepthMap(bitmap, params.focalPoint)

        // Step 2: Landscape, Sky & Architectural Enhancements
        val landscapeEnhanced = if (params.skyEnhance > 0.01f || params.foliagePop > 0.01f ||
            params.architecturalClarity > 0.01f || params.smartHdr > 0.01f
        ) {
            LandscapeFilter.enhanceLandscape(
                bitmap,
                params.skyEnhance,
                params.foliagePop,
                params.architecturalClarity,
                params.smartHdr
            )
        } else {
            bitmap
        }

        // Step 3: Optical Bokeh & Depth of Field Blur
        val bokehApplied = if (params.blurIntensity > 0.01f && params.apertureStop < 15.0f) {
            BokehFilter.applyDepthOfField(
                landscapeEnhanced,
                depthMap,
                params.blurIntensity,
                params.apertureStop,
                params.focalDepth,
                params.depthFalloffSoftness,
                params.objectIsolation,
                params.bokehShape,
                params.bokehHighlights
            )
        } else {
            landscapeEnhanced
        }

        // Step 4: Studio Lighting & Relighting simulation
        val finalResult = LightingFilter.applyLighting(
            bokehApplied,
            depthMap,
            params.lightingMode,
            params.lightIntensity,
            params.lightAngleDegrees,
            params.rimLightIntensity,
            params.exposure,
            params.highlights,
            params.shadows,
            params.warmth,
            params.vignette,
            params.focalPoint
        )

        finalResult
    }

    /**
     * Fast downscaled thumbnail processing for real-time preview responsiveness.
     */
    fun createPreviewBitmap(source: Bitmap, maxDim: Int = 720): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) {
            return source
        }

        val scale = maxDim.toFloat() / max(width, height)
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    /**
     * Intelligent scene analysis based on image color distribution, edges, and depth cues.
     */
    fun getSceneAnalysis(bitmap: Bitmap): SceneAnalysis {
        return cachedSceneAnalysis ?: analyzeScene(bitmap, cachedDepthMap ?: FloatArray(bitmap.width * bitmap.height) { 0.5f })
    }

    private fun analyzeScene(bitmap: Bitmap, depthMap: FloatArray): SceneAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var blueSkyPixels = 0
        var foliagePixels = 0
        var centerSubjectContrast = 0

        val upperPixels = (height * 0.45f).toInt() * width
        for (i in 0 until upperPixels) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            if (b > r * 1.15f && b > g * 1.05f) {
                blueSkyPixels++
            }
        }

        for (i in 0 until total) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            if (g > r * 1.06f && g > b * 1.08f) {
                foliagePixels++
            }
        }

        val skyRatio = blueSkyPixels.toFloat() / (upperPixels.coerceAtLeast(1))
        val foliageRatio = foliagePixels.toFloat() / total

        return when {
            skyRatio > 0.20f && foliageRatio > 0.15f -> {
                SceneAnalysis(
                    sceneType = "Landscape & Nature",
                    detectedSubjectDescription = "Lush foliage & open sky detected. Sky and tree pop recommended.",
                    hasSky = true,
                    hasFoliage = true,
                    estimatedApertureRecommendation = 8.0f
                )
            }
            skyRatio > 0.25f -> {
                SceneAnalysis(
                    sceneType = "Architecture & Urban",
                    detectedSubjectDescription = "Structural lines and sky detected. Micro-contrast clarity recommended.",
                    hasSky = true,
                    hasFoliage = false,
                    estimatedApertureRecommendation = 5.6f
                )
            }
            else -> {
                SceneAnalysis(
                    sceneType = "Portrait / Subject",
                    detectedSubjectDescription = "Foreground subject detected. 85mm f/1.2 or iPhone 17 depth recommended.",
                    hasSky = skyRatio > 0.10f,
                    hasFoliage = foliageRatio > 0.08f,
                    estimatedApertureRecommendation = 1.4f
                )
            }
        }
    }

    /**
     * One-click AI automatic editing algorithm.
     * Evaluates image metrics, scene semantics, depth distribution, and dynamic range
     * to formulate a tailored pro DSLR / iPhone 17 / landscape edit.
     */
    fun computeOptimalAiEdit(bitmap: Bitmap): EditParameters {
        val analysis = getSceneAnalysis(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalLum = 0L
        var warmScore = 0L
        var coolScore = 0L
        for (i in 0 until total step 4) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val l = (r * 299 + g * 587 + b * 114) / 1000
            totalLum += l
            if (r > b + 25) warmScore++
            if (b > r + 25) coolScore++
        }
        val sampledCount = (total / 4).coerceAtLeast(1)
        val avgLum = totalLum / sampledCount
        val isUnderexposed = avgLum < 100
        val isGoldenWarm = warmScore > coolScore * 1.5

        return when {
            analysis.hasFoliage && analysis.hasSky -> {
                EditParameters(
                    blurIntensity = 0.0f,
                    apertureStop = 11.0f,
                    focalDepth = 0.70f,
                    lightingMode = com.example.model.StudioLightingMode.NATURAL,
                    lightIntensity = 1.05f,
                    exposure = if (isUnderexposed) 0.12f else 0.02f,
                    highlights = -0.32f,
                    shadows = 0.35f,
                    warmth = if (isGoldenWarm) 0.10f else 0.04f,
                    vignette = 0.08f,
                    skyEnhance = 0.85f,
                    foliagePop = 0.82f,
                    architecturalClarity = 0.40f,
                    smartHdr = 0.75f,
                    activePreset = QuickPreset.AI_MAGIC,
                    aiSummaryReason = "Landscape Scene: Sky Polarizer +85%, Forest Greens +82%, Smart HDR +75%"
                )
            }
            analysis.hasSky && analysis.sceneType.contains("Architecture") -> {
                EditParameters(
                    blurIntensity = 0.0f,
                    apertureStop = 8.0f,
                    focalDepth = 0.60f,
                    lightingMode = com.example.model.StudioLightingMode.NATURAL,
                    lightIntensity = 1.10f,
                    exposure = if (isUnderexposed) 0.10f else 0.04f,
                    highlights = -0.38f,
                    shadows = 0.38f,
                    warmth = 0.0f,
                    vignette = 0.14f,
                    skyEnhance = 0.60f,
                    foliagePop = 0.25f,
                    architecturalClarity = 0.92f,
                    smartHdr = 0.70f,
                    activePreset = QuickPreset.AI_MAGIC,
                    aiSummaryReason = "Architecture Scene: Facade Clarity +92%, Sky Polarizer +60%, Dynamic Tone"
                )
            }
            isGoldenWarm -> {
                EditParameters(
                    blurIntensity = 0.75f,
                    apertureStop = 1.4f,
                    focalDepth = 0.35f,
                    bokehShape = com.example.model.BokehShape.SWIRLY,
                    bokehHighlights = 0.80f,
                    lightingMode = com.example.model.StudioLightingMode.GOLDEN_HOUR,
                    lightIntensity = 1.40f,
                    lightAngleDegrees = 45f,
                    rimLightIntensity = 0.65f,
                    exposure = 0.10f,
                    highlights = -0.15f,
                    shadows = 0.25f,
                    warmth = 0.35f,
                    vignette = 0.20f,
                    skyEnhance = 0.30f,
                    foliagePop = 0.40f,
                    architecturalClarity = 0.20f,
                    smartHdr = 0.45f,
                    activePreset = QuickPreset.AI_MAGIC,
                    aiSummaryReason = "Golden Hour: 45° Sunbeam Relight, f/1.4 Creamy Bokeh, Amber Rim Light"
                )
            }
            else -> {
                EditParameters(
                    blurIntensity = 0.72f,
                    apertureStop = 1.4f,
                    focalDepth = 0.32f,
                    depthFalloffSoftness = 0.45f,
                    bokehShape = com.example.model.BokehShape.CREAMY_IPHONE,
                    bokehHighlights = 0.60f,
                    lightingMode = com.example.model.StudioLightingMode.STUDIO_KEY,
                    lightIntensity = 1.30f,
                    lightAngleDegrees = 50f,
                    rimLightIntensity = 0.42f,
                    exposure = if (isUnderexposed) 0.14f else 0.06f,
                    highlights = -0.20f,
                    shadows = 0.24f,
                    warmth = 0.08f,
                    vignette = 0.16f,
                    skyEnhance = 0.20f,
                    foliagePop = 0.30f,
                    architecturalClarity = 0.25f,
                    smartHdr = 0.40f,
                    activePreset = QuickPreset.AI_MAGIC,
                    aiSummaryReason = "Portrait Scene: f/1.4 Optical Bokeh, Studio Key Relight +1.3x, Skin Protection"
                )
            }
        }
    }
}
