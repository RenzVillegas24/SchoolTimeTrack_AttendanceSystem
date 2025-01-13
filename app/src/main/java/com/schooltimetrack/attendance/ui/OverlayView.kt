package com.schooltimetrack.attendance.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.android.material.color.MaterialColors

// OverlayView.kt
class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var centerBoundsX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var centerBoundsY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    private var size: Float = 0f
    private var left: Float = 0f
    private var top: Float = 0f
    private var right: Float = 0f
    private var bottom: Float = 0f
    var boundingBox: RectF = RectF()
        private set

    private val paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = MaterialColors.getColor(this@OverlayView, com.google.android.material.R.attr.colorPrimary)
        strokeWidth = 5f
    }
    private val gradientPaint = Paint()
    var gradientRadius: Float = 0.6f
        set(value) {
            field = value
            invalidate()
        }
    private var gradientShader: Shader? = null
    private var path: Path = Path()

    init {
        setWillNotDraw(false)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width
        val h = height

        size = w.coerceAtMost(h) * 0.8f
        left = (w - size) / 2 + centerBoundsX
        top = (h - size) / 2 + centerBoundsY
        right = left + size
        bottom = top + size
        boundingBox = RectF(left, top, right, bottom)

        gradientShader = RadialGradient(
            w * 0.5f,
            h * 0.5f,
            w.coerceAtLeast(h).toFloat() * gradientRadius,
            Color.TRANSPARENT,
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface),
            Shader.TileMode.CLAMP
        )

        // Draw the gradient overlay excluding the crop rectangle
        gradientPaint.shader = gradientShader
        path.apply {
            reset()
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRoundRect(boundingBox, 20f, 20f, Path.Direction.CCW)
        }
        canvas.drawPath(path, gradientPaint)

        // Draw the crop rectangle with corner radius
        canvas.drawRoundRect(boundingBox, 20f, 20f, paint)
    }
}