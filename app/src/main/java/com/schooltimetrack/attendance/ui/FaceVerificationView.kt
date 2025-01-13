package com.schooltimetrack.attendance.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import com.schooltimetrack.attendance.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FaceVerificationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val previewView: PreviewView
    private val overlayView: OverlayView
    private val imageCapture: ImageCapture
    private var imageAnalyzer: ImageAnalysis? = null
    val cameraExecutor: ExecutorService
    private var onDetect: ((Bitmap, FaceVerificationView) -> Unit)? = null
    private var onFaceConfirmed: ((Bitmap, FaceVerificationView) -> Unit)? = null
    private var onFaceNotDetected: ((FaceVerificationView) -> Unit)? = null
    private var isDetectionPaused = false
    private var isOverlayVisible: Boolean = true
    private var detectionDelay: Int = 3500 // Default to 2 seconds
    private var detectionHandler: Handler = Handler(Looper.getMainLooper())
    private var detectionRunnable: Runnable? = null
    private var faceBitmap: Bitmap? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var isCameraFrozen = false
    private var lastPreviewBitmap: Bitmap? = null

    private var currentTranslationX = 0f
    private var currentTranslationY = 0f
    private var currentScale = 1f
    private var animatorX: ValueAnimator? = null
    private var animatorY: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null

    // Constants for smooth tracking
    private val ANIMATION_DURATION = 300L
    private val SCALE_FACTOR = 0.4f
    private val MIN_FACE_SIZE = 0.4f
    private val MAX_FACE_SIZE = 1.0f

    private val DEFAULT_SCALE = 1f
    private val DEFAULT_TRANSLATION_X = 0f
    private val DEFAULT_TRANSLATION_Y = 0f


    init {
        // Get the overlay visibility and detection delay from attributes
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FaceCameraView,
            0, 0
        ).apply {
            try {
                isOverlayVisible = getBoolean(R.styleable.FaceCameraView_overlayVisible, isOverlayVisible)
                detectionDelay = getInt(R.styleable.FaceCameraView_detectionDelay, detectionDelay)
            } finally {
                recycle()
            }
        }


        previewView = PreviewView(context)
        previewView.clipToOutline = false
        previewView.clipChildren = false
        clipChildren = false
        clipToPadding = false
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE)

        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        overlayView = OverlayView(context)
        addView(overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    var gradientRadius : Float = 0.6f
        set(value) {
            field = value
            overlayView.gradientRadius = value
            invalidate()
        }

    fun startCamera(bitmap: Bitmap? = null) {
        faceBitmap = bitmap
        imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor, FaceAnalyzer())
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                this.findViewTreeLifecycleOwner()?.let {
                    camera = cameraProvider?.bindToLifecycle(
                        it,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )
                }
            } catch (exc: Exception) {
                Log.e("FaceCameraView", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setOverlayVisible(visible: Boolean) {
        isOverlayVisible = visible
        overlayView.visibility = if (visible) VISIBLE else GONE
    }

    fun isOverlayVisible(): Boolean = isOverlayVisible

    fun setOnDetectListener(listener: (Bitmap, FaceVerificationView) -> Unit) {
        onDetect = listener
    }

    fun setOnFaceConfirmedListener(listener: (Bitmap, FaceVerificationView) -> Unit) {
        onFaceConfirmed = listener
    }

    fun setOnFaceNotDetectedListener(listener: (FaceVerificationView) -> Unit) {
        onFaceNotDetected = listener
    }

    fun pauseDetection() {
        isDetectionPaused = true
        detectionRunnable?.let { detectionHandler.removeCallbacks(it) }
        detectionRunnable = null
    }

    fun resumeDetection() {
        isDetectionPaused = false
    }

    fun getBoundingBox(): RectF {
        return overlayView.boundingBox
    }


    fun freezeCameraPreview() {
        if (!isCameraFrozen) {
            isCameraFrozen = true

            // Capture the current preview as a bitmap
            lastPreviewBitmap = previewView.bitmap?.copy(Bitmap.Config.ARGB_8888, false)

            // Unbind all use cases to stop the camera feed
            this.findViewTreeLifecycleOwner()?.let { lifecycle ->
                try {
                    cameraProvider?.unbindAll()

                    // Create an ImageView to display the frozen frame
                    val frozenImageView = ImageView(context).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        lastPreviewBitmap?.let { bitmap ->
                            setImageBitmap(bitmap)
                        }
                        // Retain the current translation and scale values
                        translationX = previewView.translationX
                        translationY = previewView.translationY
                        scaleX = previewView.scaleX
                        scaleY = previewView.scaleY
                    }

                    // Remove the preview view temporarily and add the frozen image
                    removeView(previewView)
                    addView(frozenImageView, 0)
                } catch (exc: Exception) {
                    Log.e("FaceCameraView", "Failed to freeze camera preview", exc)
                }
            }

            // Pause face detection
            pauseDetection()
        }
    }

    fun unfreezeCameraPreview() {
        if (isCameraFrozen) {
            isCameraFrozen = false

            // Remove the frozen image view if it exists
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is ImageView) {
                    removeView(child)
                    break
                }
            }

            // Add back the preview view
            if (previewView.parent == null) {
                addView(previewView, 0)
            }

            // Restart the camera
            startCamera()

            // Resume face detection
            resumeDetection()

            // Clean up the stored bitmap
            lastPreviewBitmap?.recycle()
            lastPreviewBitmap = null
        }
    }

    fun resetCameraView() {
        // Cancel any ongoing animations
        animatorX?.cancel()
        animatorY?.cancel()
        scaleAnimator?.cancel()

        // Animate back to default values
        animatorX = ValueAnimator.ofFloat(currentTranslationX, DEFAULT_TRANSLATION_X).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentTranslationX = animation.animatedValue as Float
                previewView.translationX = currentTranslationX
            }
            start()
        }

        animatorY = ValueAnimator.ofFloat(currentTranslationY, DEFAULT_TRANSLATION_Y).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentTranslationY = animation.animatedValue as Float
                previewView.translationY = currentTranslationY
            }
            start()
        }

        scaleAnimator = ValueAnimator.ofFloat(currentScale, DEFAULT_SCALE).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                previewView.scaleX = currentScale
                previewView.scaleY = currentScale
            }
            start()
        }
    }


    private fun updateCameraView(face: Face, proxyWidth: Float, proxyHeight: Float) {
        val faceBounds = face.boundingBox
        val overlayBox = overlayView.boundingBox ?: return

        // Calculate preview dimensions
        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()
        val previewAspectRatio = previewWidth / previewHeight
        val actualPreviewAspectRatio = 3f / 4f

        // Calculate actual preview dimensions
        val actualPreviewWidth = if (previewAspectRatio > actualPreviewAspectRatio) {
            previewWidth
        } else {
            previewHeight * actualPreviewAspectRatio
        }

        val actualPreviewHeight = if (previewAspectRatio > actualPreviewAspectRatio) {
            previewWidth / actualPreviewAspectRatio
        } else {
            previewHeight
        }

        // Calculate scaling factors
        val previewScale = (actualPreviewWidth / proxyHeight).coerceAtMost(actualPreviewHeight / proxyWidth)

        // Calculate face size relative to preview
        val faceSize = face.boundingBox.width().toFloat() / proxyWidth

        // Dynamic scale calculation based on face size
        val targetScale = when {
            faceSize < MIN_FACE_SIZE -> SCALE_FACTOR * (MIN_FACE_SIZE / faceSize)
            faceSize > MAX_FACE_SIZE -> SCALE_FACTOR * (MAX_FACE_SIZE / faceSize)
            else -> SCALE_FACTOR
        }.coerceIn(1f, 2.5f)

        // Calculate target positions for centering
        var targetX = (faceBounds.centerX() * previewScale - overlayBox.centerX()) - (actualPreviewWidth - previewWidth) / 2
        var targetY = (overlayBox.centerY() - (faceBounds.centerY() * previewScale)) - (actualPreviewHeight - previewHeight) / 2

        // Consider the scale factor on the target positions
        targetX *= targetScale
        targetY *= targetScale

        // Animate translation
        animateTranslation(targetX, targetY, targetScale)

    }

    private fun animateTranslation(targetX: Float, targetY: Float, targetScale: Float) {
        // Cancel existing animations
        animatorX?.cancel()
        animatorY?.cancel()
        scaleAnimator?.cancel()

        // Animate X translation
        animatorX = ValueAnimator.ofFloat(currentTranslationX, targetX).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentTranslationX = animation.animatedValue as Float
                previewView.translationX = currentTranslationX
            }
            start()
        }

        // Animate Y translation
        animatorY = ValueAnimator.ofFloat(currentTranslationY, targetY).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentTranslationY = animation.animatedValue as Float
                previewView.translationY = currentTranslationY
            }
            start()
        }

        // Animate scale
        scaleAnimator = ValueAnimator.ofFloat(currentScale, targetScale).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                previewView.scaleX = currentScale
                previewView.scaleY = currentScale
            }
            start()
        }
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        private var lastProcessingTimeMs = 0L
        private val PROCESS_DELAY = 50L // Limit processing to 20 FPS

        private val faceDetectorOptions = FaceDetectorOptions.Builder()
            .enableTracking()
            .setMinFaceSize(0.15f)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
        private val faceNet: FaceNet = FaceNet(context)
        private val faceSpoofDetector: FaceSpoofDetector = FaceSpoofDetector(context)

        private var faceFloatArray: FloatArray? = null

        init {
            val image = InputImage.fromBitmap(faceBitmap!!, 0)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        CoroutineScope(Dispatchers.Default).launch {
                            val face = faces[0]
                            val croppedFace = cropFace(faceBitmap!!, face)
                            faceFloatArray = faceNet.getFaceEmbedding(croppedFace)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                }
        }

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessingTimeMs < PROCESS_DELAY) {
                imageProxy.close()
                return
            }
            lastProcessingTimeMs = currentTime

            if (isDetectionPaused) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)


            Log.d("FaceAnalyzer", "Processing image")

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        Log.d("FaceAnalyzer", "Face detected")
                        val face = faces[0]

                        updateCameraView(
                            face,
                            imageProxy.width.toFloat(),
                            imageProxy.height.toFloat()
                        )
//
//                        val croppedFace = cropFace(imageProxy.toBitmap(), face)
//
//                        CoroutineScope(Dispatchers.Default).launch {
//                            val spoofResult = faceSpoofDetector.detectSpoof(imageProxy.toBitmap(), face.boundingBox)
//                            val embedding = faceNet.getFaceEmbedding(croppedFace)
//
//                            val similarity = faceFloatArray?.let { faceFloatArray ->
//                                cosineSimilarity(faceFloatArray, embedding)
//                            } ?: 0f
//
//                            withContext(Dispatchers.Main) {
//                                if (similarity > 0.6 && !spoofResult.isSpoof) {
//                                    run {
//                                        Log.d("FaceAnalyzer", "Face matched, similarity: $similarity spoof: ${spoofResult.isSpoof}")
//                                        onDetect?.invoke(
//                                            previewView.bitmap ?: return@run,
//                                            this@FaceVerificationView
//                                        )
//
//                                        if (detectionRunnable == null) {
//                                            Log.d("FaceAnalyzer", "Face confirmed")
//                                            detectionRunnable = Runnable {
//                                                onFaceConfirmed?.invoke(
//                                                    previewView.bitmap ?: return@Runnable,
//                                                    this@FaceVerificationView
//                                                )
//                                                detectionRunnable = null
//                                            }
//                                            detectionHandler.postDelayed(
//                                                detectionRunnable!!,
//                                                detectionDelay.toLong()
//                                            )
//                                        }
//                                    }
//                                    imageProxy.close()
//                                } else {
//                                    Log.d("FaceAnalyzer", "Face not matched, similarity: $similarity spoof: ${spoofResult.isSpoof}")
//                                    resetCameraView()
//                                    onFaceNotDetected?.invoke(
//                                        this@FaceVerificationView
//                                    )
//                                    detectionRunnable?.let {
//                                        detectionHandler.removeCallbacks(it)
//                                    }
//                                    detectionRunnable = null
//                                }
//                            }
//                        }

                    } else {
                        Log.d("FaceAnalyzer", "Face not detected")
                        resetCameraView()
                        onFaceNotDetected?.invoke(
                            this@FaceVerificationView
                        )
                        detectionRunnable?.let {
                            detectionHandler.removeCallbacks(it)
                        }
                        detectionRunnable = null
                    }

                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                    imageProxy.close()
                }
        }

        private fun cropFace(orig: Bitmap, face: Face): Bitmap {
            val box = face.boundingBox
            val left = box.left.coerceAtLeast(0)
            val top = box.top.coerceAtLeast(0)
            val width = box.width().coerceAtMost(orig.width - left)
            val height = box.height().coerceAtMost(orig.height - top)
            return Bitmap.createBitmap(orig, left, top, width, height)
        }

        private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
            var dot = 0f
            var mag1 = 0f
            var mag2 = 0f
            for (i in v1.indices) {
                dot += v1[i] * v2[i]
                mag1 += v1[i] * v1[i]
                mag2 += v2[i] * v2[i]
            }
            return dot / (kotlin.math.sqrt(mag1) * kotlin.math.sqrt(mag2))
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animatorX?.cancel()
        animatorY?.cancel()
        scaleAnimator?.cancel()
        cameraExecutor.shutdown()
        lastPreviewBitmap?.recycle()
        lastPreviewBitmap = null
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

}
