package com.schooltimetrack.attendance.ui

import WeeklyAttendanceAdapter
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.schooltimetrack.attendance.R
import kotlin.math.abs
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.schooltimetrack.attendance.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WeeklyAttendanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

//    private var onCheckInListener: (() -> Unit)? = null
//    private var onCheckOutListener: (() -> Unit)? = null
    private var onDateChangeListener: ((String) -> Unit)? = null
    private var initialScrollDone = false
    private var currentCenterDate: String? = null

    init {
        setupRecyclerView()
        clipToPadding = false
        setPadding(
            resources.getDimensionPixelSize(R.dimen.weekly_attendance_padding),
            resources.getDimensionPixelSize(R.dimen.weekly_attendance_padding),
            resources.getDimensionPixelSize(R.dimen.weekly_attendance_padding),
            resources.getDimensionPixelSize(R.dimen.weekly_attendance_padding)
        )
        attachSnapHelper()
    }

    private fun setupRecyclerView() {
        layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
            override fun onLayoutChildren(recycler: Recycler?, state: State?) {
                super.onLayoutChildren(recycler, state)
                if (!initialScrollDone && childCount > 0) {
                    scrollToCenter()
                    initialScrollDone = true
                }
                updateItemsTransformation()
            }
        }

        this.adapter = WeeklyAttendanceAdapter(
            onCheckInListener = {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Check In")
                    .setMessage("Are you sure you want to check in?")
                    .setPositiveButton("Yes") { dialog, which ->
                        GlobalScope.launch {
                            (adapter as WeeklyAttendanceAdapter).updateAttendance(true)
                        }
                    }
                    .setNegativeButton("No") { dialog, which ->
                        // do nothing
                    }
                    .show()
            },
            onCheckOutListener = {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Check Out")
                    .setMessage("Are you sure you want to check out?")
                    .setPositiveButton("Yes") { dialog, which ->
                        GlobalScope.launch {
                            (adapter as WeeklyAttendanceAdapter).updateAttendance(false)
                        }
                    }
                    .setNegativeButton("No") { dialog, which ->
                        // do nothing
                    }
                    .show()
            },
            databases = (context as MainActivity).databases,
            grade = (context as MainActivity).userDocument.grade,
            section = (context as MainActivity).userDocument.section,
            userId = (context as MainActivity).userDocument.userId
        ).apply {
            registerAdapterDataObserver(object : AdapterDataObserver() {
                override fun onChanged() {
                    scrollToCenter()
                }
            })
        }

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateItemsTransformation()
                detectCenterDate()
            }
        })

    }

    suspend fun loadScheduleData() {
        (adapter as? WeeklyAttendanceAdapter)?.loadScheduleData()
    }


    private fun attachSnapHelper() {
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(this)
    }

    fun scrollToCenter() {
        val adapter = adapter as? WeeklyAttendanceAdapter ?: return
        val layoutManager = layoutManager as? LinearLayoutManager ?: return
        
        val centerPosition = adapter.itemCount / 2
        
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                val viewHeight = viewEnd - viewStart
                return boxStart + (boxEnd - boxStart - viewHeight) / 2 - viewStart
            }
    
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                // Adjust scroll speed - lower number = faster scroll
                return 50f / displayMetrics.densityDpi
            }
        }
    
        smoothScroller.targetPosition = centerPosition
        layoutManager.startSmoothScroll(smoothScroller)
    }


    private fun updateItemsTransformation() {
        val centerY = height / 2f
        val scaleDistanceThreshold = height / 2f
        val basePadding = resources.getDimensionPixelSize(R.dimen.attendance_item_padding).toFloat()

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenterY = (child.top + child.bottom) / 2f
            val distanceFromCenter = abs(centerY - childCenterY)
            
            val scale = when {
                distanceFromCenter <= scaleDistanceThreshold -> {
                    val fraction = 1 - (distanceFromCenter / scaleDistanceThreshold)
                    lerp(0.8f, 1.0f, fraction)
                }
                else -> 0.8f
            }

            val alpha = when {
                distanceFromCenter <= scaleDistanceThreshold -> {
                    val fraction = 1 - (distanceFromCenter / scaleDistanceThreshold)
                    lerp(0.5f, 1.0f, fraction)
                }
                else -> 0.5f
            }

            child.apply {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
//                requestLayout()
            }
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction.coerceIn(0f, 1f)
    }

    private fun detectCenterDate() {
        val centerView = findChildViewUnder(width / 2f, height / 2f) ?: return
        val position = getChildAdapterPosition(centerView)
        val adapter = adapter as? WeeklyAttendanceAdapter ?: return
        val date = adapter.getDateAtPosition(position)

        if (date != currentCenterDate) {
            currentCenterDate = date
            onDateChangeListener?.invoke(date)
        }
    }

//
//    fun setOnCheckInListener(listener: () -> Unit) {
//        onCheckInListener = listener
//    }
//
//    fun setOnCheckOutListener(listener: () -> Unit) {
//        onCheckOutListener = listener
//    }

    fun setOnDateChangeListener(listener: (String) -> Unit) {
        onDateChangeListener = listener
    }
}