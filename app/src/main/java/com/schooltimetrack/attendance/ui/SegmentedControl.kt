package com.schooltimetrack.attendance.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.PathInterpolator
import androidx.annotation.RequiresApi
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.R

@RequiresApi(Build.VERSION_CODES.S)
class SegmentedControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var selectedIndex = 0
    private var currentIndicatorX = 0f
    private var segmentWidth = 0f
    private var animator: ValueAnimator? = null
    private var indicatorPadding = 12f
    private var cornerRadius = 8f

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorRect = RectF()
    private val backgroundRect = RectF()

    init {
        orientation = HORIZONTAL

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SegmentedControl,
            0, 0).apply {
            try {
                indicatorPadding = getDimension(R.styleable.SegmentedControl_indicatorPadding, 12f)
                cornerRadius = getDimension(R.styleable.SegmentedControl_cornerRadius, 24f)
                selectedIndex = getInt(R.styleable.SegmentedControl_selectedIndex, 0)
            } finally {
                recycle()
            }
        }
        setBackgroundColors()
        clipChildren = false
        clipToPadding = false

        if (paddingStart == 0 && paddingEnd == 0 && paddingTop == 0 && paddingBottom == 0) {
            val defaultPadding = context.resources.getDimensionPixelSize(
                android.R.dimen.app_icon_size) / 8
            setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setupChildViews()
    }

    private fun setupChildViews() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val params = LinearLayout.LayoutParams(
                0,  // width of 0 means it will be calculated by weight
                LayoutParams.MATCH_PARENT
            )
            params.weight = 1f  // equal weight for each child
            child.layoutParams = params

            val textColor = if (i == selectedIndex) {
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
            } else {
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
            }
            (child as? TextView)?.setTextColor(textColor)

             
            // Add click listener
            child.setOnClickListener {
                val index = indexOfChild(it)
                if (index != selectedIndex) {
                    animateToIndex(index)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setBackgroundColors() {
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant)
        val primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

        backgroundPaint.color = surfaceColor
        indicatorPaint.color = primaryColor

        indicatorPaint.setShadowLayer(
            4f,
            0f,
            2f,
            0x1A000000
        )
        setRenderEffect(null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        segmentWidth = (width - paddingLeft - paddingRight).toFloat() / childCount
        currentIndicatorX = paddingLeft + selectedIndex * segmentWidth
        updateRects()
    }

    private fun updateRects() {
        backgroundRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            width - paddingRight.toFloat(),
            height - paddingBottom.toFloat()
        )

        indicatorRect.set(
            currentIndicatorX + indicatorPadding,
            paddingTop + indicatorPadding,
            currentIndicatorX + segmentWidth - indicatorPadding,
            height - paddingBottom - indicatorPadding
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)
        canvas.drawRoundRect(indicatorRect, cornerRadius - indicatorPadding, cornerRadius - indicatorPadding, indicatorPaint)
        super.dispatchDraw(canvas)
    }

    // override fun onTouchEvent(event: MotionEvent): Boolean {
    //     when (event.action) {
    //         MotionEvent.ACTION_DOWN,
    //         MotionEvent.ACTION_MOVE -> {
    //             val x = event.x - paddingLeft
    //             val index = (x / segmentWidth).toInt()
    //                 .coerceIn(0, childCount - 1)
    //             if (index != selectedIndex) {
    //                 animateToIndex(index)
    //             }
    //             return true
    //         }
    //     }
    //     return super.onTouchEvent(event)
    // }

    private fun animateToIndex(index: Int) {
        animator?.cancel()

        val startX = currentIndicatorX
        val endX = paddingLeft + index * segmentWidth

        animator = ValueAnimator.ofFloat(startX, endX).apply {
            duration = 500
            interpolator = PathInterpolator(0.3f, 1.2f, 0.25f, 1f)

            addUpdateListener { animation ->
                currentIndicatorX = animation.animatedValue as Float
                updateRects()
                invalidate()
            }

            start()
        }

        animateTextColorChange(selectedIndex, index)
        selectedIndex = index
        onSegmentSelectedListener?.onSegmentSelected(index)
    }

    private fun animateTextColorChange(oldIndex: Int, newIndex: Int) {
        val oldTextView = getChildAt(oldIndex) as? TextView
        val newTextView = getChildAt(newIndex) as? TextView

        val oldColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
        val newColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

        ValueAnimator.ofObject(ArgbEvaluator(), oldColor, newColor).apply {
            duration = 250
            addUpdateListener { animator ->
                oldTextView?.setTextColor(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(ArgbEvaluator(), newColor, oldColor).apply {
            duration = 250
            addUpdateListener { animator ->
                newTextView?.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    interface OnSegmentSelectedListener {
        fun onSegmentSelected(index: Int)
    }

    private var onSegmentSelectedListener: OnSegmentSelectedListener? = null

    fun setOnSegmentSelectedListener(listener: OnSegmentSelectedListener) {
        onSegmentSelectedListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setBackgroundColors()
    }

    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    fun setSelectedIndex(index: Int) {
        animateToIndex(index)
    }
}