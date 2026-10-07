package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BokehShape
import com.example.model.EditParameters
import com.example.model.EditorTab
import com.example.model.StudioLightingMode
import com.example.ui.theme.FoliageGreen
import com.example.ui.theme.GoldenGlow
import com.example.ui.theme.SkyCobalt
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDivider
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun ControlSliders(
    params: EditParameters,
    onParamsChange: (EditParameters) -> Unit,
    currentTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        // Tab Row for selecting control category
        TabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = StudioSurface,
            contentColor = StudioCyan,
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioDivider)
                )
            },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                    color = StudioCyan,
                    height = 2.5.dp
                )
            }
        ) {
            EditorTab.values().forEach { tab ->
                val isSelected = (currentTab == tab)
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) StudioCyan else Color.White.copy(alpha = 0.65f)
                        )
                    },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (currentTab) {
                EditorTab.PRESETS -> {
                    // Handled above in top preset row, but provides quick recap & scene AI recommendation
                    PresetTabOverview(params, onParamsChange)
                }

                EditorTab.DEPTH_BOKEH -> {
                    DepthBokehControls(params, onParamsChange)
                }

                EditorTab.LIGHTING -> {
                    StudioLightingControls(params, onParamsChange)
                }

                EditorTab.LANDSCAPE -> {
                    LandscapeControls(params, onParamsChange)
                }
            }
        }
    }
}

@Composable
private fun PresetTabOverview(
    params: EditParameters,
    onParamsChange: (EditParameters) -> Unit
) {
    Column {
        Text(
            text = "Fine-tune any slider in the tabs above to customize your look.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryStatusCard(
                title = "Aperture & Bokeh",
                value = "f/${String.format("%.1f", params.apertureStop)} • ${(params.blurIntensity * 100).toInt()}% blur",
                accent = StudioCyan,
                modifier = Modifier.weight(1f)
            )

            SummaryStatusCard(
                title = "Studio Lighting",
                value = "${params.lightingMode.label} • ${(params.lightIntensity * 100).toInt()}%",
                accent = StudioAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryStatusCard(
                title = "Landscape & Trees",
                value = if (params.skyEnhance > 0f || params.foliagePop > 0f) "Sky +${(params.skyEnhance * 100).toInt()}% • Trees +${(params.foliagePop * 100).toInt()}%" else "Neutral",
                accent = FoliageGreen,
                modifier = Modifier.weight(1f)
            )

            SummaryStatusCard(
                title = "Clarity & HDR",
                value = if (params.architecturalClarity > 0f) "Clarity +${(params.architecturalClarity * 100).toInt()}%" else "Standard",
                accent = SkyCobalt,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStatusCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StudioDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = accent, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DepthBokehControls(
    params: EditParameters,
    onParamsChange: (EditParameters) -> Unit
) {
    Column {
        // Blur Intensity slider
        FineSlider(
            title = "Blur Intensity",
            valueText = "${(params.blurIntensity * 100).toInt()}%",
            value = params.blurIntensity,
            valueRange = 0.0f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.BlurCircular,
            onValueChange = { onParamsChange(params.copy(blurIntensity = it)) },
            tag = "slider_blur_intensity"
        )

        // Aperture Stops (Chips)
        Text(
            text = "Simulated Aperture",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        val apertures = listOf(1.2f, 1.4f, 1.8f, 2.8f, 4.0f, 8.0f, 16.0f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            apertures.forEach { fStop ->
                val isSelected = (params.apertureStop == fStop)
                Surface(
                    color = if (isSelected) StudioCyan else StudioCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) StudioCyan else StudioDivider),
                    modifier = Modifier
                        .clickable { onParamsChange(params.copy(apertureStop = fStop)) }
                        .testTag("aperture_${fStop}")
                ) {
                    Text(
                        text = "f/${if (fStop == 1.2f || fStop == 1.4f || fStop == 1.8f || fStop == 2.8f || fStop == 4.0f || fStop == 8.0f) fStop else 16}",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bokeh Shape Selection
        Text(
            text = "Bokeh Optical Character",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BokehShape.values().forEach { shape ->
                val isSelected = (params.bokehShape == shape)
                Surface(
                    color = if (isSelected) StudioAmber else StudioCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) StudioAmber else StudioDivider),
                    modifier = Modifier
                        .clickable { onParamsChange(params.copy(bokehShape = shape)) }
                        .testTag("bokeh_shape_${shape.name.lowercase()}")
                ) {
                    Text(
                        text = shape.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Focal Depth Plane Slider
        FineSlider(
            title = "Focal Plane Distance",
            valueText = "${(params.focalDepth * 100).toInt()}%",
            value = params.focalDepth,
            valueRange = 0.0f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.CenterFocusStrong,
            onValueChange = { onParamsChange(params.copy(focalDepth = it)) },
            tag = "slider_focal_depth"
        )

        // Object Isolation (iPhone 17 Mode: keeps only object clear, background blurred)
        FineSlider(
            title = "Object Isolation (iPhone 17 Mode)",
            valueText = "${(params.objectIsolation * 100).toInt()}%",
            value = params.objectIsolation,
            valueRange = 0.0f..1.0f,
            accentColor = StudioAmber,
            icon = Icons.Default.CenterFocusStrong,
            onValueChange = { onParamsChange(params.copy(objectIsolation = it)) },
            tag = "slider_object_isolation"
        )

        // Falloff Softness Slider
        FineSlider(
            title = "Depth Falloff Softness",
            valueText = "${(params.depthFalloffSoftness * 100).toInt()}%",
            value = params.depthFalloffSoftness,
            valueRange = 0.1f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.ShutterSpeed,
            onValueChange = { onParamsChange(params.copy(depthFalloffSoftness = it)) },
            tag = "slider_falloff_softness"
        )

        // Bokeh Highlights Disc Pop
        FineSlider(
            title = "Specular Highlights (Bokeh Balls)",
            valueText = "${(params.bokehHighlights * 100).toInt()}%",
            value = params.bokehHighlights,
            valueRange = 0.0f..1.0f,
            accentColor = GoldenGlow,
            icon = Icons.Default.Flare,
            onValueChange = { onParamsChange(params.copy(bokehHighlights = it)) },
            tag = "slider_bokeh_highlights"
        )
    }
}

@Composable
private fun StudioLightingControls(
    params: EditParameters,
    onParamsChange: (EditParameters) -> Unit
) {
    Column {
        // Lighting Mode Chips
        Text(
            text = "Studio Lighting Mode",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StudioLightingMode.values().forEach { mode ->
                val isSelected = (params.lightingMode == mode)
                Surface(
                    color = if (isSelected) StudioAmber else StudioCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) StudioAmber else StudioDivider),
                    modifier = Modifier
                        .clickable { onParamsChange(params.copy(lightingMode = mode)) }
                        .testTag("lighting_mode_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Light Intensity Slider
        FineSlider(
            title = "Light Intensity",
            valueText = "${(params.lightIntensity * 100).toInt()}%",
            value = params.lightIntensity,
            valueRange = 0.0f..2.0f,
            accentColor = StudioAmber,
            icon = Icons.Default.Lightbulb,
            onValueChange = { onParamsChange(params.copy(lightIntensity = it)) },
            tag = "slider_light_intensity"
        )

        // Light Angle Slider
        FineSlider(
            title = "Virtual Light Angle",
            valueText = "${params.lightAngleDegrees.roundToInt()}°",
            value = params.lightAngleDegrees,
            valueRange = 0.0f..360.0f,
            accentColor = StudioAmber,
            icon = Icons.Default.Highlight,
            onValueChange = { onParamsChange(params.copy(lightAngleDegrees = it)) },
            tag = "slider_light_angle"
        )

        // Rim Light Separation
        FineSlider(
            title = "Rim Light (Subject Separation)",
            valueText = "${(params.rimLightIntensity * 100).toInt()}%",
            value = params.rimLightIntensity,
            valueRange = 0.0f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.Flare,
            onValueChange = { onParamsChange(params.copy(rimLightIntensity = it)) },
            tag = "slider_rim_light"
        )

        // Warmth / Golden Hour color temperature
        FineSlider(
            title = "Color Warmth",
            valueText = if (params.warmth > 0f) "+${(params.warmth * 100).toInt()}%" else "${(params.warmth * 100).toInt()}%",
            value = params.warmth,
            valueRange = -1.0f..1.0f,
            accentColor = GoldenGlow,
            icon = Icons.Default.WbSunny,
            onValueChange = { onParamsChange(params.copy(warmth = it)) },
            tag = "slider_warmth"
        )

        // Exposure
        FineSlider(
            title = "Exposure",
            valueText = if (params.exposure > 0f) "+${(params.exposure * 100).toInt()}%" else "${(params.exposure * 100).toInt()}%",
            value = params.exposure,
            valueRange = -1.0f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.Camera,
            onValueChange = { onParamsChange(params.copy(exposure = it)) },
            tag = "slider_exposure"
        )

        // Optical Vignette
        FineSlider(
            title = "Vignette",
            valueText = "${(params.vignette * 100).toInt()}%",
            value = params.vignette,
            valueRange = 0.0f..1.0f,
            accentColor = Color.White,
            icon = Icons.Default.BlurCircular,
            onValueChange = { onParamsChange(params.copy(vignette = it)) },
            tag = "slider_vignette"
        )
    }
}

@Composable
private fun LandscapeControls(
    params: EditParameters,
    onParamsChange: (EditParameters) -> Unit
) {
    Column {
        Text(
            text = "AI Scenic & Structural Detail Enhancer",
            fontSize = 11.sp,
            color = StudioCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Sky Enhancer (Polarizer & Deep Cobalt)
        FineSlider(
            title = "Sky Blue & Cloud Polarizer",
            valueText = "+${(params.skyEnhance * 100).toInt()}%",
            value = params.skyEnhance,
            valueRange = 0.0f..1.0f,
            accentColor = SkyCobalt,
            icon = Icons.Default.FilterDrama,
            onValueChange = { onParamsChange(params.copy(skyEnhance = it)) },
            tag = "slider_sky_enhance"
        )

        // Foliage & Tree Pop (Chlorophyll vibrance)
        FineSlider(
            title = "Lush Trees & Foliage Pop",
            valueText = "+${(params.foliagePop * 100).toInt()}%",
            value = params.foliagePop,
            valueRange = 0.0f..1.0f,
            accentColor = FoliageGreen,
            icon = Icons.Default.Nature,
            onValueChange = { onParamsChange(params.copy(foliagePop = it)) },
            tag = "slider_foliage_pop"
        )

        // Architectural Clarity (Local micro-contrast)
        FineSlider(
            title = "Architectural Structure & Detail",
            valueText = "+${(params.architecturalClarity * 100).toInt()}%",
            value = params.architecturalClarity,
            valueRange = 0.0f..1.0f,
            accentColor = GoldenGlow,
            icon = Icons.Default.Flare,
            onValueChange = { onParamsChange(params.copy(architecturalClarity = it)) },
            tag = "slider_arch_clarity"
        )

        // Smart HDR Dynamic Range
        FineSlider(
            title = "Smart HDR Dynamic Range",
            valueText = "${(params.smartHdr * 100).toInt()}%",
            value = params.smartHdr,
            valueRange = 0.0f..1.0f,
            accentColor = StudioCyan,
            icon = Icons.Default.AutoAwesome,
            onValueChange = { onParamsChange(params.copy(smartHdr = it)) },
            tag = "slider_smart_hdr"
        )
    }
}

@Composable
private fun FineSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    icon: ImageVector,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Text(
                text = valueText,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF282838)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag(tag)
        )
    }
}
