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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuickPreset
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDivider

@Composable
fun PresetCarousel(
    activePreset: QuickPreset,
    onPresetSelected: (QuickPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        QuickPreset.AI_MAGIC,
        QuickPreset.IPHONE_17_OBJECT,
        QuickPreset.IPHONE_17_PORTRAIT,
        QuickPreset.DSLR_85MM,
        QuickPreset.VIBRANT_LANDSCAPE,
        QuickPreset.ARCHITECTURE_HDR,
        QuickPreset.GOLDEN_PORTRAIT,
        QuickPreset.STUDIO_CONTOUR,
        QuickPreset.NONE
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = StudioCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ONE-TAP PRO PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Text(
                text = activePreset.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = StudioCyan,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = (preset == activePreset)
                PresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: QuickPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (preset) {
        QuickPreset.AI_MAGIC -> Icons.Default.AutoAwesome
        QuickPreset.IPHONE_17_OBJECT -> Icons.Default.CenterFocusStrong
        QuickPreset.IPHONE_17_PORTRAIT -> Icons.Default.PhoneIphone
        QuickPreset.DSLR_85MM -> Icons.Default.CameraAlt
        QuickPreset.VIBRANT_LANDSCAPE -> Icons.Default.Park
        QuickPreset.ARCHITECTURE_HDR -> Icons.Default.LocationCity
        QuickPreset.GOLDEN_PORTRAIT -> Icons.Default.LightMode
        QuickPreset.STUDIO_CONTOUR -> Icons.Default.FilterHdr
        QuickPreset.NONE -> Icons.Default.RestartAlt
    }

    val accentGradient = when (preset) {
        QuickPreset.AI_MAGIC -> Brush.linearGradient(listOf(Color(0xFFFF007A), Color(0xFF7928CA), Color(0xFF00DFD8)))
        QuickPreset.IPHONE_17_OBJECT -> Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF10B981)))
        QuickPreset.IPHONE_17_PORTRAIT -> Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
        QuickPreset.DSLR_85MM -> Brush.linearGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5E62)))
        QuickPreset.VIBRANT_LANDSCAPE -> Brush.linearGradient(listOf(Color(0xFF00F260), Color(0xFF0575E6)))
        QuickPreset.ARCHITECTURE_HDR -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
        QuickPreset.GOLDEN_PORTRAIT -> Brush.linearGradient(listOf(Color(0xFFFFD194), Color(0xFFD1913C)))
        QuickPreset.STUDIO_CONTOUR -> Brush.linearGradient(listOf(Color(0xFFF09819), Color(0xFFEDDE5D)))
        QuickPreset.NONE -> Brush.linearGradient(listOf(Color(0xFF434343), Color(0xFF282828)))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF212130) else StudioCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) StudioCyan else StudioDivider
        ),
        modifier = Modifier
            .width(116.dp)
            .clickable(onClick = onClick)
            .testTag("preset_${preset.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = preset.label,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                ),
                maxLines = 1
            )

            Text(
                text = when (preset) {
                    QuickPreset.AI_MAGIC -> "Auto AI Optimize"
                    QuickPreset.IPHONE_17_OBJECT -> "Clear object, blur BG"
                    QuickPreset.IPHONE_17_PORTRAIT -> "Soft falloff & light"
                    QuickPreset.DSLR_85MM -> "f/1.2 optical bokeh"
                    QuickPreset.VIBRANT_LANDSCAPE -> "Vivid sky & forest"
                    QuickPreset.ARCHITECTURE_HDR -> "Micro-contrast"
                    QuickPreset.GOLDEN_PORTRAIT -> "Warm rim glow"
                    QuickPreset.STUDIO_CONTOUR -> "Sculpted lighting"
                    QuickPreset.NONE -> "Reset edits"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (isSelected) StudioCyan.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f)
                ),
                maxLines = 1
            )
        }
    }
}
