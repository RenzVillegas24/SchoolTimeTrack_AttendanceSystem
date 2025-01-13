package com.schooltimetrack.attendance.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.color.MaterialColors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.schooltimetrack.attendance.R

class ImageCropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var centerCropX: Float = 0f
    private var centerCropY: Float = 0f
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = MaterialColors.getColor(this@ImageCropView, com.google.android.material.R.attr.colorPrimary)
        strokeWidth = 5f
    }
    private var cropRect: RectF
    private var bitmap: Bitmap? = null
    private var scaleFactor = 1.0f
    private var lastX = 0f
    private var lastY = 0f
    private var posX = 0f
    private var posY = 0f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var requirePerson = false
    private var onCancel: (() -> Unit)? = null
    var faces: List<Face> = emptyList()
        private set

    private val imageView: ImageView
    private var overlayView: OverlayView

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ImageCropView,
            0, 0
        ).apply {
            try {
                centerCropX = getDimension(R.styleable.ImageCropView_centerCropX, 0f)
                centerCropY = getDimension(R.styleable.ImageCropView_centerCropY, 0f)
            } finally {
                recycle()
            }
        }

        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.MATRIX
        }
        addView(imageView)

        overlayView = OverlayView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        cropRect = overlayView.boundingBox
        addView(overlayView)
    }

    fun setImageBitmap(bitmap: Bitmap, requirePerson: Boolean, onCancel: () -> Unit, centerCropX: Float = 0f, centerCropY: Float = 0f) {
        this.bitmap = bitmap
        this.requirePerson = requirePerson
        this.onCancel = onCancel
        this.centerCropX = centerCropX
        this.centerCropY = centerCropY
        overlayView.centerBoundsX = centerCropX
        overlayView.centerBoundsY = centerCropY
        imageView.setImageBitmap(bitmap)
        if (requirePerson) {
            detectFaces(bitmap) { faces ->
                if (faces.isNotEmpty()) {
                    centerCropOnFace(faces[0])
                }
            }
        } else {
            reset()
        }
    }

    fun detectFaces(bitmap: Bitmap, onFacesDetected: (List<Face>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { detectedFaces ->
                if (detectedFaces.isNotEmpty()) {
                    faces = detectedFaces
                    onFacesDetected(faces)
                } else {
                    Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
                    onCancel?.invoke()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Face detection failed", Toast.LENGTH_SHORT).show()
                onCancel?.invoke()
            }
    }

    fun centerCropOnFace(face: Face) {
        bitmap?.let {
            val faceBounds = face.boundingBox
            cropRect = overlayView.boundingBox

            // Calculate padding based on the crop factor
            val cropFactor = Math.min(cropRect.width() / faceBounds.width(), cropRect.height() / faceBounds.height())
            val padding = (1000 / cropFactor).coerceAtLeast(25f) // Ensure minimum padding

            val faceWidthWithPadding = faceBounds.width() + padding
            val faceHeightWithPadding = faceBounds.height() + padding

            var targetScaleFactor: Float
            val targetPosX: Float
            val targetPosY: Float

            // Scale the image to fit the face within the cropRect
            val scaleX = cropRect.width() / faceWidthWithPadding
            val scaleY = cropRect.height() / faceHeightWithPadding
            targetScaleFactor = Math.max(scaleX, scaleY)

            // Ensure the scale factor does not go below the size of the cropRect
            val minScaleFactor = Math.max(cropRect.width() / it.width, cropRect.height() / it.height)
            targetScaleFactor = Math.max(minScaleFactor, targetScaleFactor)

            targetPosX = cropRect.left - (faceBounds.left - padding / 2) * targetScaleFactor
            targetPosY = cropRect.top - (faceBounds.top - padding / 3) * targetScaleFactor

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 750 // Animation duration in milliseconds
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    posX += (targetPosX - posX) * animatedValue
                    posY += (targetPosY - posY) * animatedValue
                    scaleFactor += (targetScaleFactor - scaleFactor) * animatedValue
                    adjustPosition()
                    updateImageMatrix()
                }
            }
            animator.start()
        }
    }

    private fun reset(hasAnimation: Boolean = true) {
        bitmap?.let {
            val size = Math.min(width, height) * 0.8f
            val left = (width - size) / 2 + centerCropX
            val top = (height - size) / 2 + centerCropY
            val right = left + size
            val bottom = top + size
            cropRect.set(left, top, right, bottom)

            val scaleX = cropRect.width() / it.width
            val scaleY = cropRect.height() / it.height
            val targetScaleFactor = Math.max(scaleX, scaleY)

            val targetPosX = cropRect.left - (it.width * targetScaleFactor - cropRect.width()) / 2
            val targetPosY = cropRect.top - (it.height * targetScaleFactor - cropRect.height()) / 2

            if (hasAnimation) {
                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 750 // Animation duration in milliseconds
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animation ->
                        val animatedValue = animation.animatedValue as Float
                        posX += (targetPosX - posX) * animatedValue
                        posY += (targetPosY - posY) * animatedValue
                        scaleFactor += (targetScaleFactor - scaleFactor) * animatedValue
                        adjustPosition()
                        updateImageMatrix()
                    }
                }
                animator.start()
            } else {
                posX = targetPosX
                posY = targetPosY
                scaleFactor = targetScaleFactor
                adjustPosition()
                updateImageMatrix()
            }
        }
    }

    fun resetScaling() {
        reset()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        reset(hasAnimation = false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        overlayView.invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                posX += dx
                posY += dy
                adjustPosition()
                lastX = event.x
                lastY = event.y
                updateImageMatrix()
            }
        }
        return true
    }

    private fun adjustPosition() {
        bitmap?.let {
            val minPosX = cropRect.right - it.width * scaleFactor
            val maxPosX = cropRect.left
            val minPosY = cropRect.bottom - it.height * scaleFactor
            val maxPosY = cropRect.top

            if (posX < minPosX) posX = minPosX
            if (posX > maxPosX) posX = maxPosX
            if (posY < minPosY) posY = minPosY
            if (posY > maxPosY) posY = maxPosY
        }
    }

    private fun updateImageMatrix() {
        val matrix = Matrix()
        matrix.postTranslate(posX, posY)
        matrix.postScale(scaleFactor, scaleFactor, posX, posY)
        imageView.imageMatrix = matrix
        invalidate()
    }

    fun getCroppedBitmap(): Bitmap? {
        bitmap?.let {
            val scale = 1 / scaleFactor
            val cropX = ((cropRect.left - posX) * scale).toInt()
            val cropY = ((cropRect.top - posY) * scale).toInt()
            val cropWidth = (cropRect.width() * scale).toInt()
            val cropHeight = (cropRect.height() * scale).toInt()
            return Bitmap.createBitmap(it, cropX, cropY, cropWidth, cropHeight)
        }
        return null
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactorChange = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Calculate the new scale factor
            scaleFactor *= scaleFactorChange

            // Ensure the scale factor does not go below the size of the cropRect
            val minScaleFactor = Math.max(cropRect.width() / bitmap!!.width, cropRect.height() / bitmap!!.height)
            scaleFactor = Math.max(minScaleFactor, Math.min(scaleFactor, 5.0f))

            // Adjust the position to keep the focus point in place
            posX = focusX - (focusX - posX) * scaleFactorChange
            posY = focusY - (focusY - posY) * scaleFactorChange

            // Ensure the image stays within the cropRect
            adjustPosition()

            updateImageMatrix()
            return true
        }
    }
}