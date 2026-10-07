package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorTab
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time interactive preview canvas with:
 * - Draggable split Before / After comparison slider
 * - Hold-to-compare instant toggle
 * - Tap-to-focus reticle (DSLR / iPhone camera focus square)
 * - Depth mask heatmap visualization mode
 * - Draggable virtual studio light direction handle
 */
@Composable
fun BeforeAfterImage(
    originalBitmap: Bitmap?,
    processedBitmap: Bitmap?,
    depthVisualBitmap: Bitmap?,
    showDepthMask: Boolean,
    focalPoint: Offset,
    onFocalPointChanged: (Offset) -> Unit,
    lightAngleDeg: Float,
    onLightAngleChanged: (Float) -> Unit,
    currentTab: EditorTab,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.50f) }
    var isHoldingCompare by remember { mutableStateOf(false) }
    var showFocusReticle by remember { mutableStateOf(false) }
    val reticleScale = remember { Animatable(1.4f) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("preview_canvas_box")
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        if (originalBitmap != null) {
            // Draw photo and comparison overlay
            val origImage = remember(originalBitmap) { originalBitmap.asImageBitmap() }
            val procImage = remember(processedBitmap) { processedBitmap?.asImageBitmap() }
            val depthImage = remember(depthVisualBitmap) { depthVisualBitmap?.asImageBitmap() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photo_canvas")
                    .pointerInput(originalBitmap) {
                        detectTapGestures(
                            onPress = {
                                isHoldingCompare = true
                                val released = tryAwaitRelease()
                                isHoldingCompare = false
                            },
                            onTap = { tapOffset ->
                                // Compute normalized photo coordinates
                                val normX = (tapOffset.x / canvasWidth).coerceIn(0.05f, 0.95f)
                                val normY = (tapOffset.y / canvasHeight).coerceIn(0.05f, 0.95f)
                                onFocalPointChanged(Offset(normX, normY))

                                showFocusReticle = true
                                scope.launch {
                                    reticleScale.snapTo(1.5f)
                                    reticleScale.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaFraction = dragAmount.x / canvasWidth
                            splitFraction = (splitFraction + deltaFraction).coerceIn(0.02f, 0.98f)
                        }
                    }
            ) {
                val dstSize = IntSize(size.width.toInt(), size.height.toInt())

                if (showDepthMask && depthImage != null) {
                    // Render AI Depth Map Heatmap
                    drawImage(
                        image = depthImage,
                        dstSize = dstSize
                    )
                } else if (isHoldingCompare || procImage == null) {
                    // Full Before / Original
                    drawImage(
                        image = origImage,
                        dstSize = dstSize
                    )
                } else {
                    val splitX = size.width * splitFraction

                    // Draw Processed on the entire canvas
                    drawImage(
                        image = procImage,
                        dstSize = dstSize
                    )

                    // Clip and draw Original on the left of the split divider
                    clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                        drawImage(
                            image = origImage,
                            dstSize = dstSize
                        )
                    }

                    // Draw vertical divider line
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Draw center slider grip circle
                    val centerY = size.height * 0.5f
                    drawCircle(
                        color = Color.White,
                        radius = 16.dp.toPx(),
                        center = Offset(splitX, centerY)
                    )
                    drawCircle(
                        color = Color(0xFF16161F),
                        radius = 14.dp.toPx(),
                        center = Offset(splitX, centerY)
                    )
                }

                // Draw Camera Tap-to-Focus Reticle
                if (!showDepthMask) {
                    val reticleX = focalPoint.x * size.width
                    val reticleY = focalPoint.y * size.height
                    val baseReticleSize = 56.dp.toPx() * reticleScale.value
                    val halfR = baseReticleSize / 2f

                    // Yellow focus brackets (like iPhone portrait or DSLR auto-focus)
                    val bracketColor = StudioAmber
                    val stroke = Stroke(width = 2.dp.toPx())
                    val cornerLen = 14.dp.toPx()

                    // Top-Left
                    drawLine(bracketColor, Offset(reticleX - halfR, reticleY - halfR), Offset(reticleX - halfR + cornerLen, reticleY - halfR), strokeWidth = stroke.width)
                    drawLine(bracketColor, Offset(reticleX - halfR, reticleY - halfR), Offset(reticleX - halfR, reticleY - halfR + cornerLen), strokeWidth = stroke.width)

                    // Top-Right
                    drawLine(bracketColor, Offset(reticleX + halfR, reticleY - halfR), Offset(reticleX + halfR - cornerLen, reticleY - halfR), strokeWidth = stroke.width)
                    drawLine(bracketColor, Offset(reticleX + halfR, reticleY - halfR), Offset(reticleX + halfR, reticleY - halfR + cornerLen), strokeWidth = stroke.width)

                    // Bottom-Left
                    drawLine(bracketColor, Offset(reticleX - halfR, reticleY + halfR), Offset(reticleX - halfR + cornerLen, reticleY + halfR), strokeWidth = stroke.width)
                    drawLine(bracketColor, Offset(reticleX - halfR, reticleY + halfR), Offset(reticleX - halfR, reticleY + halfR - cornerLen), strokeWidth = stroke.width)

                    // Bottom-Right
                    drawLine(bracketColor, Offset(reticleX + halfR, reticleY + halfR), Offset(reticleX + halfR - cornerLen, reticleY + halfR), strokeWidth = stroke.width)
                    drawLine(bracketColor, Offset(reticleX + halfR, reticleY + halfR), Offset(reticleX + halfR, reticleY + halfR - cornerLen), strokeWidth = stroke.width)

                    // Small center dot
                    drawCircle(bracketColor, radius = 2.5.dp.toPx(), center = Offset(reticleX, reticleY))
                }

                // If in Lighting mode, draw virtual light direction beam
                if (currentTab == EditorTab.LIGHTING && !showDepthMask) {
                    val rad = Math.toRadians(lightAngleDeg.toDouble())
                    val fX = focalPoint.x * size.width
                    val fY = focalPoint.y * size.height
                    val lightDist = 110.dp.toPx()
                    val lX = fX + (cos(rad) * lightDist).toFloat()
                    val lY = fY + (sin(rad) * lightDist).toFloat()

                    // Dotted line connecting focal point to virtual key light
                    drawLine(
                        color = StudioCyan.copy(alpha = 0.75f),
                        start = Offset(fX, fY),
                        end = Offset(lX, lY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Luminous virtual light source bulb
                    drawCircle(
                        color = StudioCyan,
                        radius = 8.dp.toPx(),
                        center = Offset(lX, lY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(lX, lY)
                    )
                }
            }

            // Top Badges overlay: BEFORE vs AFTER indicators
            if (!showDepthMask && !isHoldingCompare) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "BEFORE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "AFTER",
                            color = StudioCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else if (showDepthMask) {
                Surface(
                    color = Color.Black.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "AI DEPTH BUFFER MAP",
                        color = StudioAmber,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Bottom Floating Hint: "Hold canvas to compare • Drag divider"
            Surface(
                color = Color.Black.copy(alpha = 0.60f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Compare slider",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isHoldingCompare) "VIEWING ORIGINAL" else "Tap to focus • Hold to compare",
                        color = if (isHoldingCompare) StudioAmber else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
