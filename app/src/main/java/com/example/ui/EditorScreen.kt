package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.QuickPreset
import com.example.ui.components.BeforeAfterImage
import com.example.ui.components.ControlSliders
import com.example.ui.components.PresetCarousel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDivider
import com.example.ui.theme.StudioSurface

@Composable
fun EditorScreen(
    viewModel: EditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val originalBitmap by viewModel.originalFullBitmap.collectAsState()
    val previewBaseBitmap by viewModel.previewBaseBitmap.collectAsState()
    val processedBitmap by viewModel.processedBitmap.collectAsState()
    val depthVisualBitmap by viewModel.depthVisualBitmap.collectAsState()
    val editParams by viewModel.editParams.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val showDepthMask by viewModel.showDepthMask.collectAsState()
    val sceneAnalysis by viewModel.sceneAnalysis.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isAiEnhancing by viewModel.isAiEnhancing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSampleMenu by remember { mutableStateOf(false) }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadCustomPhoto(uri)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                sceneDescription = sceneAnalysis.sceneType,
                showDepthMask = showDepthMask,
                isSaving = isSaving,
                isAiEnhancing = isAiEnhancing,
                onAutoAiClick = { viewModel.autoEditWithAi() },
                onToggleDepthMask = { viewModel.toggleDepthMask() },
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSelectSampleClick = { showSampleMenu = true },
                onSaveClick = { viewModel.saveEditedPhotoToGallery() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dropdown Menu for Sample Photos
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DropdownMenu(
                    expanded = showSampleMenu,
                    onDismissRequest = { showSampleMenu = false },
                    modifier = Modifier.background(StudioCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Object Close-up (iPhone 17 Blur)", color = Color.White) },
                        onClick = {
                            viewModel.loadSamplePhoto(EditorViewModel.SampleType.OBJECT)
                            showSampleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Portrait Sample", color = Color.White) },
                        onClick = {
                            viewModel.loadSamplePhoto(EditorViewModel.SampleType.PORTRAIT)
                            showSampleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Landscape & Trees Sample", color = Color.White) },
                        onClick = {
                            viewModel.loadSamplePhoto(EditorViewModel.SampleType.LANDSCAPE)
                            showSampleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Architecture Sample", color = Color.White) },
                        onClick = {
                            viewModel.loadSamplePhoto(EditorViewModel.SampleType.ARCHITECTURE)
                            showSampleMenu = false
                        }
                    )
                }
            }

            // Real-Time Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            ) {
                BeforeAfterImage(
                    originalBitmap = previewBaseBitmap,
                    processedBitmap = processedBitmap,
                    depthVisualBitmap = depthVisualBitmap,
                    showDepthMask = showDepthMask,
                    focalPoint = editParams.focalPoint,
                    onFocalPointChanged = { viewModel.setFocalPoint(it) },
                    lightAngleDeg = editParams.lightAngleDegrees,
                    onLightAngleChanged = {
                        viewModel.updateParams(editParams.copy(lightAngleDegrees = it))
                    },
                    currentTab = currentTab
                )

                // Floating One-Click AI Auto-Edit Button
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, StudioCyan.copy(alpha = 0.9f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .clickable(enabled = !isAiEnhancing && !isSaving, onClick = { viewModel.autoEditWithAi() })
                        .testTag("btn_one_click_ai_auto_edit")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "One click AI Auto-Edit",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "AI Auto-Edit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // AI Scanning & Auto-Tuning Overlay
                if (isAiEnhancing) {
                    Surface(
                        color = Color(0xFF14141E).copy(alpha = 0.92f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, StudioCyan),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            CircularProgressIndicator(
                                color = StudioCyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "✨ AI Auto-Tuning...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Analyzing depth, bokeh & studio light",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Save in progress overlay
                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StudioCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rendering High-Resolution Master...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // One-Tap Presets Carousel
            PresetCarousel(
                activePreset = editParams.activePreset,
                onPresetSelected = { viewModel.selectPreset(it) }
            )

            // Manual Fine-Tuning Control Sliders (Tabbed)
            ControlSliders(
                params = editParams,
                onParamsChange = { viewModel.updateParams(it) },
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun TopAppBar(
    sceneDescription: String,
    showDepthMask: Boolean,
    isSaving: Boolean,
    isAiEnhancing: Boolean,
    onAutoAiClick: () -> Unit,
    onToggleDepthMask: () -> Unit,
    onPickPhoto: () -> Unit,
    onSelectSampleClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & AI Scene Chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StudioCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "LuminaLens",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "LuminaLens",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = sceneDescription,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StudioCyan,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // One-Click AI Auto-Edit Button
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StudioCyan),
                    modifier = Modifier
                        .clickable(enabled = !isAiEnhancing && !isSaving, onClick = onAutoAiClick)
                        .testTag("btn_top_ai_auto_edit")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                                )
                            )
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "One click AI Auto-Edit",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Edit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Sample Photo Switcher
                Surface(
                    color = StudioCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StudioDivider),
                    modifier = Modifier
                        .clickable(onClick = onSelectSampleClick)
                        .testTag("btn_sample_selector")
                ) {
                    Text(
                        text = "Samples",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Gallery Import button
                IconButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_pick_photo")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Import Photo",
                        tint = Color.White
                    )
                }

                // Depth Mask view toggle button
                IconButton(
                    onClick = onToggleDepthMask,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_toggle_depth_mask")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Depth Map View",
                        tint = if (showDepthMask) StudioAmber else Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Save / Export button
                Button(
                    onClick = onSaveClick,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_save_export")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
