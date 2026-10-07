package com.example.model

import androidx.compose.ui.geometry.Offset

/**
 * Bokeh blur shapes simulated by the depth-of-field engine.
 */
enum class BokehShape(val label: String, val description: String) {
    CIRCULAR_DSLR("DSLR Prime", "Smooth circular aperture blades typical of 85mm f/1.2"),
    CREAMY_IPHONE("iPhone 17", "Buttery soft computational falloff with edge preservation"),
    ANAMORPHIC("Cine Oval", "Horizontal anamorphic bokeh popular in cinema"),
    HEXAGONAL("Vintage 6-Blade", "Classic vintage hexagonal aperture flares"),
    SWIRLY("Petzval Swirl", "Artistic vintage swirling bokeh at image periphery")
}

/**
 * Lighting presets simulating studio relighting and iPhone portrait lighting.
 */
enum class StudioLightingMode(val label: String, val description: String) {
    NATURAL("Natural", "Balanced ambient dynamic range"),
    STUDIO_KEY("Studio Key", "Soft directional face illumination"),
    CONTOUR("Contour", "Sculpted facial bone highlights and rim separation"),
    STAGE_LIGHT("Stage Spotlight", "Focused subject beam with dramatic dark background"),
    GOLDEN_HOUR("Golden Hour", "Warm dimensional sunbeam with amber rim light"),
    HIGH_KEY_NOIR("High-Key Mono", "High dynamic black & white studio finish")
}

/**
 * One-tap smart editing presets.
 */
enum class QuickPreset(val label: String, val iconName: String, val subtitle: String) {
    NONE("Original", "refresh", "Unedited base photo"),
    AI_MAGIC("AI Auto-Edit", "auto_awesome", "Intelligent scene & depth auto-tune"),
    IPHONE_17_OBJECT("Object Focus", "center_focus_strong", "Only object clear, background blurred"),
    IPHONE_17_PORTRAIT("iPhone 17 Pro", "smartphone", "Buttery falloff, studio relight & natural skin"),
    DSLR_85MM("DSLR f/1.2 Cine", "camera", "Creamy 85mm prime lens depth with specular bokeh"),
    VIBRANT_LANDSCAPE("Sky & Foliage", "forest", "Deep cobalt sky, lush trees & HDR cloud dynamics"),
    ARCHITECTURE_HDR("Architecture", "apartment", "Crisp structural clarity, fine micro-contrast"),
    GOLDEN_PORTRAIT("Golden Sun", "wb_sunny", "Warm sunset glow, dreamy background falloff"),
    STUDIO_CONTOUR("Studio Contour", "lightbulb", "Directional key light with crisp edge rim")
}

/**
 * Parameters controlling all aspects of the image pipeline.
 */
data class EditParameters(
    // Depth of field & Bokeh
    val blurIntensity: Float = 0.65f,         // 0.0f .. 1.0f
    val apertureStop: Float = 1.4f,            // f/1.2 to f/16.0
    val focalDepth: Float = 0.35f,             // 0.0f .. 1.0f (depth plane of focal point)
    val depthFalloffSoftness: Float = 0.45f,   // transition ramp width
    val objectIsolation: Float = 0.85f,        // 0.0f .. 1.0f (Strict iPhone 17 object separation: keeps only object clear)
    val bokehShape: BokehShape = BokehShape.CREAMY_IPHONE,
    val bokehHighlights: Float = 0.50f,        // Specular highlight boost in bokeh discs
    val focalPoint: Offset = Offset(0.5f, 0.45f), // Normalized x, y in photo

    // Pro Lighting & Relighting
    val lightingMode: StudioLightingMode = StudioLightingMode.STUDIO_KEY,
    val lightIntensity: Float = 1.20f,         // 0.0f .. 2.0f
    val lightAngleDegrees: Float = 45f,        // 0 .. 360 degrees
    val lightRadius: Float = 0.60f,            // Spread of light
    val rimLightIntensity: Float = 0.35f,      // Subject edge halo separation
    val exposure: Float = 0.05f,               // -1.0f .. 1.0f
    val highlights: Float = -0.10f,            // -1.0f .. 1.0f (highlight recovery)
    val shadows: Float = 0.15f,                // -1.0f .. 1.0f (shadow lift)
    val warmth: Float = 0.08f,                 // -1.0f .. 1.0f
    val vignette: Float = 0.15f,               // 0.0f .. 1.0f

    // Scenery & Landscape enhancements
    val skyEnhance: Float = 0.0f,              // 0.0f .. 1.0f (sky blue booster & polarizer)
    val foliagePop: Float = 0.0f,              // 0.0f .. 1.0f (tree chlorophyll vibrancy)
    val architecturalClarity: Float = 0.0f,    // 0.0f .. 1.0f (structural micro-contrast)
    val smartHdr: Float = 0.20f,               // 0.0f .. 1.0f

    // Active Preset identifier
    val activePreset: QuickPreset = QuickPreset.IPHONE_17_PORTRAIT,
    val aiSummaryReason: String = "AI Auto-Tuned"
) {
    companion object {
        fun defaultForPreset(preset: QuickPreset): EditParameters {
            return when (preset) {
                QuickPreset.AI_MAGIC -> EditParameters(
                    blurIntensity = 0.70f,
                    apertureStop = 1.4f,
                    focalDepth = 0.35f,
                    depthFalloffSoftness = 0.45f,
                    bokehShape = BokehShape.CREAMY_IPHONE,
                    bokehHighlights = 0.55f,
                    lightingMode = StudioLightingMode.STUDIO_KEY,
                    lightIntensity = 1.30f,
                    lightAngleDegrees = 45f,
                    rimLightIntensity = 0.40f,
                    exposure = 0.06f,
                    highlights = -0.20f,
                    shadows = 0.22f,
                    warmth = 0.08f,
                    vignette = 0.15f,
                    skyEnhance = 0.50f,
                    foliagePop = 0.50f,
                    architecturalClarity = 0.40f,
                    smartHdr = 0.50f,
                    activePreset = QuickPreset.AI_MAGIC,
                    aiSummaryReason = "AI Multi-Layer Optimization: Depth-of-Field, Studio Key Relight & Smart HDR"
                )

                QuickPreset.IPHONE_17_OBJECT -> EditParameters(
                    blurIntensity = 0.90f,
                    apertureStop = 1.2f,
                    focalDepth = 0.28f,
                    depthFalloffSoftness = 0.22f,
                    objectIsolation = 1.0f, // 100% strict object focus preservation
                    bokehShape = BokehShape.CREAMY_IPHONE,
                    bokehHighlights = 0.50f,
                    lightingMode = StudioLightingMode.STUDIO_KEY,
                    lightIntensity = 1.25f,
                    lightAngleDegrees = 45f,
                    rimLightIntensity = 0.45f,
                    exposure = 0.05f,
                    highlights = -0.15f,
                    shadows = 0.18f,
                    warmth = 0.05f,
                    vignette = 0.18f,
                    skyEnhance = 0.10f,
                    foliagePop = 0.20f,
                    architecturalClarity = 0.35f,
                    smartHdr = 0.30f,
                    activePreset = QuickPreset.IPHONE_17_OBJECT,
                    aiSummaryReason = "iPhone 17 Object Mode: Object Pin-Sharp, Background Full Creamy Bokeh"
                )

                QuickPreset.NONE -> EditParameters(
                    blurIntensity = 0.0f,
                    apertureStop = 16.0f,
                    focalDepth = 0.5f,
                    bokehHighlights = 0.0f,
                    lightingMode = StudioLightingMode.NATURAL,
                    lightIntensity = 1.0f,
                    rimLightIntensity = 0.0f,
                    exposure = 0.0f,
                    highlights = 0.0f,
                    shadows = 0.0f,
                    warmth = 0.0f,
                    vignette = 0.0f,
                    skyEnhance = 0.0f,
                    foliagePop = 0.0f,
                    architecturalClarity = 0.0f,
                    smartHdr = 0.0f,
                    activePreset = QuickPreset.NONE
                )

                QuickPreset.IPHONE_17_PORTRAIT -> EditParameters(
                    blurIntensity = 0.65f,
                    apertureStop = 1.8f,
                    focalDepth = 0.35f,
                    depthFalloffSoftness = 0.50f,
                    bokehShape = BokehShape.CREAMY_IPHONE,
                    bokehHighlights = 0.40f,
                    lightingMode = StudioLightingMode.STUDIO_KEY,
                    lightIntensity = 1.25f,
                    lightAngleDegrees = 50f,
                    rimLightIntensity = 0.30f,
                    exposure = 0.08f,
                    highlights = -0.15f,
                    shadows = 0.20f,
                    warmth = 0.06f,
                    vignette = 0.12f,
                    skyEnhance = 0.15f,
                    foliagePop = 0.25f,
                    architecturalClarity = 0.15f,
                    smartHdr = 0.35f,
                    activePreset = QuickPreset.IPHONE_17_PORTRAIT
                )

                QuickPreset.DSLR_85MM -> EditParameters(
                    blurIntensity = 0.88f,
                    apertureStop = 1.2f,
                    focalDepth = 0.32f,
                    depthFalloffSoftness = 0.35f,
                    bokehShape = BokehShape.CIRCULAR_DSLR,
                    bokehHighlights = 0.75f,
                    lightingMode = StudioLightingMode.STUDIO_KEY,
                    lightIntensity = 1.15f,
                    lightAngleDegrees = 40f,
                    rimLightIntensity = 0.45f,
                    exposure = 0.05f,
                    highlights = -0.10f,
                    shadows = 0.10f,
                    warmth = 0.04f,
                    vignette = 0.30f,
                    skyEnhance = 0.10f,
                    foliagePop = 0.20f,
                    architecturalClarity = 0.20f,
                    smartHdr = 0.25f,
                    activePreset = QuickPreset.DSLR_85MM
                )

                QuickPreset.VIBRANT_LANDSCAPE -> EditParameters(
                    blurIntensity = 0.0f,
                    apertureStop = 11.0f,
                    focalDepth = 0.70f,
                    bokehHighlights = 0.0f,
                    lightingMode = StudioLightingMode.NATURAL,
                    lightIntensity = 1.05f,
                    lightAngleDegrees = 90f,
                    rimLightIntensity = 0.0f,
                    exposure = 0.02f,
                    highlights = -0.30f,
                    shadows = 0.35f,
                    warmth = 0.05f,
                    vignette = 0.08f,
                    skyEnhance = 0.85f,     // Strong sky blue & cloud polarizer
                    foliagePop = 0.80f,     // Vivid forest trees pop
                    architecturalClarity = 0.45f,
                    smartHdr = 0.70f,
                    activePreset = QuickPreset.VIBRANT_LANDSCAPE
                )

                QuickPreset.ARCHITECTURE_HDR -> EditParameters(
                    blurIntensity = 0.0f,
                    apertureStop = 8.0f,
                    focalDepth = 0.60f,
                    bokehHighlights = 0.0f,
                    lightingMode = StudioLightingMode.NATURAL,
                    lightIntensity = 1.10f,
                    lightAngleDegrees = 60f,
                    rimLightIntensity = 0.10f,
                    exposure = 0.04f,
                    highlights = -0.40f,
                    shadows = 0.40f,
                    warmth = -0.02f,
                    vignette = 0.15f,
                    skyEnhance = 0.55f,
                    foliagePop = 0.30f,
                    architecturalClarity = 0.90f, // Maximum structural edge micro-contrast
                    smartHdr = 0.65f,
                    activePreset = QuickPreset.ARCHITECTURE_HDR
                )

                QuickPreset.GOLDEN_PORTRAIT -> EditParameters(
                    blurIntensity = 0.75f,
                    apertureStop = 1.4f,
                    focalDepth = 0.35f,
                    bokehShape = BokehShape.SWIRLY,
                    bokehHighlights = 0.80f,
                    lightingMode = StudioLightingMode.GOLDEN_HOUR,
                    lightIntensity = 1.40f,
                    lightAngleDegrees = 45f,
                    rimLightIntensity = 0.65f,
                    exposure = 0.12f,
                    highlights = -0.10f,
                    shadows = 0.25f,
                    warmth = 0.45f,
                    vignette = 0.22f,
                    skyEnhance = 0.35f,
                    foliagePop = 0.45f,
                    architecturalClarity = 0.15f,
                    smartHdr = 0.40f,
                    activePreset = QuickPreset.GOLDEN_PORTRAIT
                )

                QuickPreset.STUDIO_CONTOUR -> EditParameters(
                    blurIntensity = 0.60f,
                    apertureStop = 2.0f,
                    focalDepth = 0.35f,
                    bokehShape = BokehShape.CREAMY_IPHONE,
                    bokehHighlights = 0.30f,
                    lightingMode = StudioLightingMode.CONTOUR,
                    lightIntensity = 1.60f,
                    lightAngleDegrees = 75f,
                    rimLightIntensity = 0.70f,
                    exposure = 0.0f,
                    highlights = -0.20f,
                    shadows = 0.05f,
                    warmth = 0.0f,
                    vignette = 0.40f,
                    skyEnhance = 0.0f,
                    foliagePop = 0.10f,
                    architecturalClarity = 0.30f,
                    smartHdr = 0.30f,
                    activePreset = QuickPreset.STUDIO_CONTOUR
                )
            }
        }
    }
}

/**
 * Metadata about the active image and scene analysis.
 */
data class SceneAnalysis(
    val sceneType: String = "Portrait / Subject",
    val detectedSubjectDescription: String = "Prominent foreground subject detected",
    val hasSky: Boolean = true,
    val hasFoliage: Boolean = true,
    val estimatedApertureRecommendation: Float = 1.8f,
    val estimatedSubjectCenter: Offset = Offset(0.5f, 0.45f)
)

/**
 * Editor UI tab selection.
 */
enum class EditorTab(val title: String) {
    PRESETS("Presets"),
    DEPTH_BOKEH("Depth & Bokeh"),
    LIGHTING("Studio Light"),
    LANDSCAPE("Scenery & HDR")
}
