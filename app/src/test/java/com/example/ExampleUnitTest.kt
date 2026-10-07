package com.example

import com.example.model.EditParameters
import com.example.model.QuickPreset
import com.example.model.StudioLightingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPresetInitialization() {
        val iphone17 = EditParameters.defaultForPreset(QuickPreset.IPHONE_17_PORTRAIT)
        assertEquals(QuickPreset.IPHONE_17_PORTRAIT, iphone17.activePreset)
        assertEquals(StudioLightingMode.STUDIO_KEY, iphone17.lightingMode)
        assertTrue(iphone17.blurIntensity > 0.5f)

        val landscape = EditParameters.defaultForPreset(QuickPreset.VIBRANT_LANDSCAPE)
        assertEquals(QuickPreset.VIBRANT_LANDSCAPE, landscape.activePreset)
        assertTrue(landscape.skyEnhance > 0.7f)
        assertTrue(landscape.foliagePop > 0.7f)

        val architecture = EditParameters.defaultForPreset(QuickPreset.ARCHITECTURE_HDR)
        assertEquals(QuickPreset.ARCHITECTURE_HDR, architecture.activePreset)
        assertTrue(architecture.architecturalClarity > 0.8f)

        val dslr = EditParameters.defaultForPreset(QuickPreset.DSLR_85MM)
        assertEquals(1.2f, dslr.apertureStop, 0.01f)
        assertTrue(dslr.blurIntensity > 0.8f)

        val aiMagic = EditParameters.defaultForPreset(QuickPreset.AI_MAGIC)
        assertEquals(QuickPreset.AI_MAGIC, aiMagic.activePreset)
        assertTrue(aiMagic.blurIntensity > 0.5f)
        assertTrue(aiMagic.lightIntensity > 1.0f)
        assertTrue(aiMagic.aiSummaryReason.isNotEmpty())

        val objectPreset = EditParameters.defaultForPreset(QuickPreset.IPHONE_17_OBJECT)
        assertEquals(QuickPreset.IPHONE_17_OBJECT, objectPreset.activePreset)
        assertEquals(1.0f, objectPreset.objectIsolation, 0.01f)
        assertTrue(objectPreset.blurIntensity > 0.8f)
    }
}
