package com.schooltimetrack.attendance.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.color.MaterialColors
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class Scanner(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onQrCodeDetected: (String, Scanner) -> Unit,
    private val onPermissionDenied: () -> Unit = {},
    private val hasOverlay: Boolean = true
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var processCameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null
    private var isFlashEnabled = false
    private var cameraSelector: CameraSelector

    // Camera settings
    private val resolution = Size(1280, 720)

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = MaterialColors.getColor(previewView, com.google.android.material.R.attr.colorPrimary)
        strokeWidth = 5f
    }

    private val gradientPaint = Paint()

    var gradientRadius: Float = 0.6f
        set(value) {
            field = value
        }

    init {
        // Get saved camera preference
        val sharedPrefs = context.getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)
        val useFrontCamera = sharedPrefs.getBoolean("use_front_camera", false)
        cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        if (context is ComponentActivity) {
            requestCameraPermission(context)
            if (hasOverlay) {
                setupOverlay()
            }
        } else {
            throw IllegalArgumentException("Context must be a ComponentActivity")
        }
    }

    private fun setupOverlay() {
        previewView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (hasOverlay) {
                previewView.overlay?.clear()
                drawScannerOverlay()
            }
        }
    }

    private fun drawScannerOverlay() {
        val overlayBitmap = Bitmap.createBitmap(
            previewView.width,
            previewView.height,
            Bitmap.Config.ARGB_8888
        )
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE)

        val canvas = Canvas(overlayBitmap)

        val width = canvas.width
        val height = canvas.height
        val size = Math.min(width, height) * 0.7f
        val left = (width - size) / 2
        val top = (height - size) / 2
        val right = left + size
        val bottom = top + size
        val cropRect = RectF(left, top, right, bottom)

        // Draw the gradient overlay excluding the crop rectangle
        val gradientShader = RadialGradient(
            canvas.width * 0.5f,  // centerX
            canvas.height * 0.5f,  // centerY
            canvas.width.coerceAtLeast(canvas.height).toFloat() * gradientRadius,  // radius
            Color.TRANSPARENT,
            MaterialColors.getColor(previewView, com.google.android.material.R.attr.colorSurface),
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradientShader

        val path = Path().apply {
            addRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Path.Direction.CW)
            addRoundRect(cropRect, 20f, 20f, Path.Direction.CCW)
        }
        canvas.drawPath(path, gradientPaint)

        // Draw the crop rectangle with corner radius
        canvas.drawRoundRect(cropRect, 20f, 20f, paint)

        // Add the overlay to the PreviewView
        val drawable = object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: Canvas) {
                canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        previewView.overlay?.add(drawable)
    }

    private fun requestCameraPermission(activity: ComponentActivity) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                val requestPermissionLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        startCamera()
                    } else {
                        onPermissionDenied()
                    }
                }
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            processCameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .setTargetResolution(resolution)
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Image analysis use case
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                processCameraProvider?.unbindAll()

                // Use current cameraSelector instead of hardcoded back camera
                val camera = processCameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector, // Use variable instead of DEFAULT_BACK_CAMERA
                    preview,
                    imageAnalyzer
                )

                cameraControl = camera?.cameraControl

            } catch (exc: Exception) {
                Toast.makeText(context, "Failed to bind camera use cases", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )

            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.let { barcode ->
                        if (barcode.valueType == Barcode.TYPE_TEXT) {
                            barcode.rawValue?.let { qrContent ->
                                onQrCodeDetected(qrContent, this)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?: imageProxy.close()
    }

    // Public methods for controlling the scanner

    fun toggleFlash() {
        isFlashEnabled = !isFlashEnabled
        cameraControl?.enableTorch(isFlashEnabled)
    }

    fun isFlashOn() = isFlashEnabled

    fun pauseScanning() {
        processCameraProvider?.unbindAll()
    }

    fun resumeScanning() {
        startCamera()
    }

    fun release() {
        processCameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // Save camera preference
        context.getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("use_front_camera", isUsingFrontCamera())
            .apply()
            
        startCamera()
    }

    fun isUsingFrontCamera(): Boolean {
        return cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }
}