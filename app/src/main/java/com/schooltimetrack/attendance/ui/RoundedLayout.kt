package com.schooltimetrack.attendance.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.schooltimetrack.attendance.R


class RoundedLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var path = Path()
    private var cornerRadius: Float = 0f
    private var roundedCorners: Int = ALL_CORNERS
    private val rectangle = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    companion object {
        const val ALL_CORNERS = 0xF
        const val TOP_LEFT = 1
        const val TOP_RIGHT = 2
        const val BOTTOM_RIGHT = 4
        const val BOTTOM_LEFT = 8
    }

    init {
        val a: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedLayout
        )

        try {
            cornerRadius = a.getDimension(
                R.styleable.RoundedLayout_cornerRadius,
                0f
            )
            roundedCorners = a.getInt(
                R.styleable.RoundedLayout_roundedCorners,
                ALL_CORNERS
            )
        } finally {
            a.recycle()
        }

        clipChildren = true

        // Custom outline provider that respects individual corners
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                // Create a path for the outline
                val path = Path()
                val rect = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())

                val corners = floatArrayOf(
                    if (roundedCorners and TOP_LEFT == TOP_LEFT) cornerRadius else 0f,
                    if (roundedCorners and TOP_LEFT == TOP_LEFT) cornerRadius else 0f,
                    if (roundedCorners and TOP_RIGHT == TOP_RIGHT) cornerRadius else 0f,
                    if (roundedCorners and TOP_RIGHT == TOP_RIGHT) cornerRadius else 0f,
                    if (roundedCorners and BOTTOM_RIGHT == BOTTOM_RIGHT) cornerRadius else 0f,
                    if (roundedCorners and BOTTOM_RIGHT == BOTTOM_RIGHT) cornerRadius else 0f,
                    if (roundedCorners and BOTTOM_LEFT == BOTTOM_LEFT) cornerRadius else 0f,
                    if (roundedCorners and BOTTOM_LEFT == BOTTOM_LEFT) cornerRadius else 0f
                )

                path.addRoundRect(rect, corners, Path.Direction.CW)

                // Convert the path to an outline
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    outline.setPath(path)
                } else {
                    // For older Android versions, we'll use a simpler outline
                    // that approximates the shape
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        getMaxCornerRadius()
                    )
                }
            }
        }

        clipToOutline = true
        paint.style = Paint.Style.FILL
        setWillNotDraw(false)
    }

    private fun getMaxCornerRadius(): Float {
        val corners = mutableListOf<Float>()
        if (roundedCorners and TOP_LEFT == TOP_LEFT) corners.add(cornerRadius)
        if (roundedCorners and TOP_RIGHT == TOP_RIGHT) corners.add(cornerRadius)
        if (roundedCorners and BOTTOM_RIGHT == BOTTOM_RIGHT) corners.add(cornerRadius)
        if (roundedCorners and BOTTOM_LEFT == BOTTOM_LEFT) corners.add(cornerRadius)
        return corners.maxOrNull() ?: 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath(w, h)
    }

    private fun updatePath(w: Int, h: Int) {
        path.reset()
        rectangle.set(0f, 0f, w.toFloat(), h.toFloat())

        val corners = floatArrayOf(
            if (roundedCorners and TOP_LEFT == TOP_LEFT) cornerRadius else 0f,
            if (roundedCorners and TOP_LEFT == TOP_LEFT) cornerRadius else 0f,
            if (roundedCorners and TOP_RIGHT == TOP_RIGHT) cornerRadius else 0f,
            if (roundedCorners and TOP_RIGHT == TOP_RIGHT) cornerRadius else 0f,
            if (roundedCorners and BOTTOM_RIGHT == BOTTOM_RIGHT) cornerRadius else 0f,
            if (roundedCorners and BOTTOM_RIGHT == BOTTOM_RIGHT) cornerRadius else 0f,
            if (roundedCorners and BOTTOM_LEFT == BOTTOM_LEFT) cornerRadius else 0f,
            if (roundedCorners and BOTTOM_LEFT == BOTTOM_LEFT) cornerRadius else 0f
        )

        path.addRoundRect(rectangle, corners, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)

        // Draw the background color
        paint.color = background?.colorAlpha ?: Color.TRANSPARENT
        canvas.drawPath(path, paint)

        super.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    private val Drawable?.colorAlpha: Int
        get() = if (this is ColorDrawable) this.color else Color.TRANSPARENT

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        updatePath(width, height)
        invalidateOutline()
        invalidate()
    }

    fun setRoundedCorners(corners: Int) {
        roundedCorners = corners
        updatePath(width, height)
        invalidateOutline()
        invalidate()
    }
}