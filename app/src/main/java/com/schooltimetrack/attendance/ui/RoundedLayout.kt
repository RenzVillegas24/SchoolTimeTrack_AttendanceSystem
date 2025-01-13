package com.schooltimetrack.attendance.ui

import com.schooltimetrack.attendance.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class RoundedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var topLeftCornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var topRightCornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var bottomLeftCornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var bottomRightCornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val path = Path()

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView,
            0,
            0
        )

        // Get the default values from the attrs
        topLeftCornerRadius = typedArray.getDimension(
            R.styleable.RoundedView_topLeftCornerRadius,
            0f
        )
        topRightCornerRadius = typedArray.getDimension(
            R.styleable.RoundedView_topRightCornerRadius,
            0f
        )
        bottomLeftCornerRadius = typedArray.getDimension(
            R.styleable.RoundedView_bottomLeftCornerRadius,
            0f
        )
        bottomRightCornerRadius = typedArray.getDimension(
            R.styleable.RoundedView_bottomRightCornerRadius,
            0f
        )

        typedArray.recycle()
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val count = canvas.save()

        path.reset()

        val cornerDimensions = floatArrayOf(
            topLeftCornerRadius, topLeftCornerRadius,
            topRightCornerRadius, topRightCornerRadius,
            bottomRightCornerRadius, bottomRightCornerRadius,
            bottomLeftCornerRadius, bottomLeftCornerRadius
        )

        path.addRoundRect(
            RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()),
            cornerDimensions,
            Path.Direction.CW
        )

        canvas.clipPath(path)

        super.dispatchDraw(canvas)
        canvas.restoreToCount(count)
    }
}
