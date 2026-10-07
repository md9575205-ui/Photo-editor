package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.engine.PhotoProcessor
import com.example.model.EditParameters
import com.example.model.EditorTab
import com.example.model.QuickPreset
import com.example.model.SceneAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val processor = PhotoProcessor()

    private val _originalFullBitmap = MutableStateFlow<Bitmap?>(null)
    val originalFullBitmap: StateFlow<Bitmap?> = _originalFullBitmap.asStateFlow()

    private val _previewBaseBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBaseBitmap: StateFlow<Bitmap?> = _previewBaseBitmap.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _depthVisualBitmap = MutableStateFlow<Bitmap?>(null)
    val depthVisualBitmap: StateFlow<Bitmap?> = _depthVisualBitmap.asStateFlow()

    private val _editParams = MutableStateFlow(EditParameters.defaultForPreset(QuickPreset.IPHONE_17_PORTRAIT))
    val editParams: StateFlow<EditParameters> = _editParams.asStateFlow()

    private val _currentTab = MutableStateFlow(EditorTab.PRESETS)
    val currentTab: StateFlow<EditorTab> = _currentTab.asStateFlow()

    private val _showDepthMask = MutableStateFlow(false)
    val showDepthMask: StateFlow<Boolean> = _showDepthMask.asStateFlow()

    private val _sceneAnalysis = MutableStateFlow(SceneAnalysis())
    val sceneAnalysis: StateFlow<SceneAnalysis> = _sceneAnalysis.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isAiEnhancing = MutableStateFlow(false)
    val isAiEnhancing: StateFlow<Boolean> = _isAiEnhancing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var previewRenderJob: Job? = null

    init {
        // Load default portrait sample on first launch
        loadSamplePhoto(SampleType.PORTRAIT)
    }

    enum class SampleType {
        OBJECT, PORTRAIT, LANDSCAPE, ARCHITECTURE
    }

    fun loadSamplePhoto(type: SampleType) {
        val resId = when (type) {
            SampleType.OBJECT -> R.drawable.sample_object
            SampleType.PORTRAIT -> R.drawable.sample_portrait
            SampleType.LANDSCAPE -> R.drawable.sample_landscape
            SampleType.ARCHITECTURE -> R.drawable.sample_architecture
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val fullBmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, resId)
            }
            if (fullBmp != null) {
                setupLoadedBitmap(fullBmp, when (type) {
                    SampleType.OBJECT -> QuickPreset.IPHONE_17_OBJECT
                    SampleType.PORTRAIT -> QuickPreset.IPHONE_17_PORTRAIT
                    SampleType.LANDSCAPE -> QuickPreset.VIBRANT_LANDSCAPE
                    SampleType.ARCHITECTURE -> QuickPreset.ARCHITECTURE_HDR
                })
            }
            _isProcessing.value = false
        }
    }

    fun loadCustomPhoto(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val fullBmp = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (fullBmp != null) {
                // Auto-detect scene and suggest preset
                setupLoadedBitmap(fullBmp, null)
            } else {
                _statusMessage.value = "Failed to load selected photo"
            }
            _isProcessing.value = false
        }
    }

    private suspend fun setupLoadedBitmap(fullBmp: Bitmap, defaultPreset: QuickPreset?) {
        _originalFullBitmap.value = fullBmp
        val previewBmp = processor.createPreviewBitmap(fullBmp, maxDim = 800)
        _previewBaseBitmap.value = previewBmp

        // Estimate scene
        val analysis = processor.getSceneAnalysis(previewBmp)
        _sceneAnalysis.value = analysis

        // Prepare initial depth map
        val initialFocal = analysis.estimatedSubjectCenter
        val (_, depthVisual) = processor.prepareDepthMap(previewBmp, initialFocal)
        _depthVisualBitmap.value = depthVisual

        // Choose initial preset
        val preset = defaultPreset ?: when {
            analysis.hasFoliage && analysis.hasSky -> QuickPreset.VIBRANT_LANDSCAPE
            analysis.hasSky && analysis.sceneType.contains("Architecture") -> QuickPreset.ARCHITECTURE_HDR
            else -> QuickPreset.IPHONE_17_PORTRAIT
        }

        val initialParams = EditParameters.defaultForPreset(preset).copy(focalPoint = initialFocal)
        _editParams.value = initialParams

        // Render preview
        renderPreviewInternal(previewBmp, initialParams)
    }

    fun updateParams(newParams: EditParameters) {
        _editParams.value = newParams
        triggerPreviewRender()
    }

    fun setFocalPoint(newPoint: Offset) {
        val updated = _editParams.value.copy(focalPoint = newPoint)
        _editParams.value = updated

        viewModelScope.launch {
            val preview = _previewBaseBitmap.value ?: return@launch
            val (_, depthVisual) = processor.prepareDepthMap(preview, newPoint)
            _depthVisualBitmap.value = depthVisual
            triggerPreviewRender()
        }
    }

    fun selectPreset(preset: QuickPreset) {
        val newParams = EditParameters.defaultForPreset(preset).copy(
            focalPoint = _editParams.value.focalPoint
        )
        _editParams.value = newParams
        triggerPreviewRender()
    }

    /**
     * One-click automatic AI photo enhancement.
     * Evaluates depth, subject, lighting, sky, and foliage to formulate optimal parameters.
     */
    fun autoEditWithAi() {
        val preview = _previewBaseBitmap.value ?: return
        viewModelScope.launch {
            _isAiEnhancing.value = true
            _statusMessage.value = "AI scanning scene depth & lighting..."
            delay(350)

            val optimal = processor.computeOptimalAiEdit(preview).copy(
                focalPoint = _editParams.value.focalPoint
            )
            _editParams.value = optimal
            renderPreviewInternal(preview, optimal)

            _isAiEnhancing.value = false
            _statusMessage.value = "✨ ${optimal.aiSummaryReason}"
        }
    }

    fun setTab(tab: EditorTab) {
        _currentTab.value = tab
    }

    fun toggleDepthMask() {
        _showDepthMask.value = !_showDepthMask.value
    }

    private fun triggerPreviewRender() {
        previewRenderJob?.cancel()
        previewRenderJob = viewModelScope.launch {
            delay(35) // Small debounce for ultra-smooth 60fps sliding
            val preview = _previewBaseBitmap.value ?: return@launch
            renderPreviewInternal(preview, _editParams.value)
        }
    }

    private suspend fun renderPreviewInternal(preview: Bitmap, params: EditParameters) {
        val result = processor.processPhoto(preview, params)
        _processedBitmap.value = result
    }

    fun saveEditedPhotoToGallery() {
        val fullSource = _originalFullBitmap.value ?: return
        val params = _editParams.value

        viewModelScope.launch {
            _isSaving.value = true
            _statusMessage.value = "Rendering high-resolution master..."

            val rendered = withContext(Dispatchers.Default) {
                // High-resolution processing
                processor.processPhoto(fullSource, params)
            }

            val saved = withContext(Dispatchers.IO) {
                saveBitmapToMediaStore(getApplication(), rendered)
            }

            _isSaving.value = false
            if (saved != null) {
                _statusMessage.value = "Saved to Gallery in full resolution!"
            } else {
                _statusMessage.value = "Failed to export image"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? {
        val filename = "LuminaLens_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LuminaLens")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }
}
