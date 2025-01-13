package com.schooltimetrack.attendance.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.schooltimetrack.attendance.databinding.ViewWeeklyAttendanceBinding
import com.schooltimetrack.attendance.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// WeeklyAttendanceView.kt
class WeeklyAttendanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {


    private var binding: ViewWeeklyAttendanceBinding? = null
    private val currentDate = LocalDate.now()


    private var onCheckInListener: (() -> Unit)? = null
    private var onCheckOutListener: (() -> Unit)? = null

    fun setOnCheckInListener(listener: () -> Unit) {
        onCheckInListener = listener
    }

    fun setOnCheckOutListener(listener: () -> Unit) {
        onCheckOutListener = listener
    }

    data class AttendanceData(
        val date: LocalDate,
        val timeIn: LocalTime?,
        val timeOut: LocalTime?,
        val targetTimeIn: LocalTime,
        val targetTimeOut: LocalTime,
        val isAttended: Boolean = false,
        val isLate: Boolean = false
    )

    init {
        binding = ViewWeeklyAttendanceBinding.inflate(LayoutInflater.from(context), this)
        initializeAttributes(attrs)
        setupView()
    }

    private fun initializeAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WeeklyAttendanceView,
            0, 0
        ).apply {
            try {
                // set the padding
                val padding = getDimensionPixelSize(R.styleable.WeeklyAttendanceView_android_padding, 0)
                setPadding(padding, padding, padding, padding)
            } finally {
                recycle()
            }
        }
    }

    init {
        binding = ViewWeeklyAttendanceBinding.inflate(LayoutInflater.from(context), this)
        initializeAttributes(attrs)
        setupView()
    }



    private fun setupView() {
        binding?.apply {

            // Setup previous days
            val previousDays = setupPreviousDays()
            previousDaysContainer.removeAllViews()
            previousDays.forEach { dayView ->
                previousDaysContainer.addView(dayView)
                // add space if not the last view
                if (previousDays.indexOf(dayView) != previousDays.size - 1) {
                    val space = Space(context)
                    space.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 8.dpToPx())
                    previousDaysContainer.addView(space)
                }
            }

            // Setup current day
            setupCurrentDay()

            // Setup future days
            val futureDays = setupFutureDays()
            futureDaysContainer.removeAllViews()
            futureDays.forEach { dayView ->
                futureDaysContainer.addView(dayView)
                // add space if not the last view
                if (previousDays.indexOf(dayView) != previousDays.size - 1) {
                    val space = Space(context)
                    space.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 8.dpToPx())
                    futureDaysContainer.addView(space)
                }

            }
        }
    }

    private fun setupPreviousDays(): List<View> {
        val days = mutableListOf<View>()
        for (i in 20 downTo 1) {
            val date = currentDate.minusDays(i.toLong())
            val dayView = createDayView(date, ViewType.PREVIOUS)
            days.add(dayView)
        }
        return days
    }

    private fun setupCurrentDay() {
        binding?.apply {
            currentDayContainer.removeAllViews()
            val currentDayView = createDayView(currentDate, ViewType.CURRENT)
            currentDayContainer.addView(currentDayView)
        }
    }

    private fun setupFutureDays(): List<View> {
        val days = mutableListOf<View>()
        for (i in 1..20) {
            val date = currentDate.plusDays(i.toLong())
            val dayView = createDayView(date, ViewType.FUTURE)
            days.add(dayView)
        }
        return days
    }

    private fun isNoScheduleDay(date: LocalDate): Boolean {
        return date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }

    private fun createDayView(date: LocalDate, type: ViewType): View {
        val layoutId = when {
            isNoScheduleDay(date) -> R.layout.item_no_schedule_day
            else -> when (type) {
                ViewType.PREVIOUS -> R.layout.item_previous_attendance_day
                ViewType.CURRENT -> R.layout.item_attendance_day
                ViewType.FUTURE -> R.layout.item_future_attendance_day
                ViewType.NONE -> R.layout.item_no_schedule_day
            }
        }
        
        val view = LayoutInflater.from(context).inflate(layoutId, null)

        // Find common views
        val card = view.findViewById<MaterialCardView>(R.id.card)
        val dateText = view.findViewById<MaterialTextView>(R.id.dateText)
        val dayOfWeek = view.findViewById<MaterialTextView>(R.id.dayOfWeek)
        dateText.text = date.format(DateTimeFormatter.ofPattern("d"))
        dayOfWeek.text = date.format(DateTimeFormatter.ofPattern("EEEE"))

        if (isNoScheduleDay(date)) {
            card.setCardBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceDim))
            return view
        }
        // Update text or visibility according to type
        val mockData = getMockAttendanceData(date)

        when (type) {
            ViewType.CURRENT -> {
                // Find views
                val timeInText = view.findViewById<MaterialTextView>(R.id.timeInText)
                val timeOutText = view.findViewById<MaterialTextView>(R.id.timeOutText)
                val statusChip = view.findViewById<Chip>(R.id.statusChip)
                val targetInText = view.findViewById<MaterialTextView>(R.id.targetTimeInText)
                val targetOutText = view.findViewById<MaterialTextView>(R.id.targetTimeOutText)
                val checkInButton = view.findViewById<MaterialButton>(R.id.checkInButton)
                val checkOutButton = view.findViewById<MaterialButton>(R.id.checkOutButton)

                card.setCardBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimaryContainer))

                // Set actual times if already checked in/out
                if (mockData.timeIn != null) {
                    timeInText.text = mockData.timeIn.format(DateTimeFormatter.ofPattern("h:mm a"))
                    checkInButton.isEnabled = false
                }
                if (mockData.timeOut != null) {
                    timeOutText.text = mockData.timeOut.format(DateTimeFormatter.ofPattern("h:mm a"))
                    checkOutButton.isEnabled = false
                }

                // Set status chip
                if (mockData.isAttended) {
                    statusChip.apply {
                        text = if (mockData.isLate) "Late" else "On Time"
                        setChipBackgroundColorResource(
                            if (mockData.isLate) R.color.material_warning_container
                            else R.color.material_success_container
                        )
                    }
                } else {
                    statusChip.apply {
                        text = "Absent"
                        setChipBackgroundColorResource(R.color.material_error_container)
                    }
                }

                // Set target times
                targetInText.text = mockData.targetTimeIn.format(DateTimeFormatter.ofPattern("h:mm a"))
                targetOutText.text = mockData.targetTimeOut.format(DateTimeFormatter.ofPattern("h:mm a"))

                checkInButton.setOnClickListener { onCheckInListener?.invoke() }
                checkOutButton.setOnClickListener { onCheckOutListener?.invoke() }
            }
            ViewType.PREVIOUS -> {
                val timeInText = view.findViewById<MaterialTextView>(R.id.timeInText)
                val timeOutText = view.findViewById<MaterialTextView>(R.id.timeOutText)
                val statusChip = view.findViewById<Chip>(R.id.statusChip)

                if (mockData.isAttended) {
                    card.setCardBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorTertiaryContainer))
                    timeInText.text = mockData.timeIn?.format(DateTimeFormatter.ofPattern("h:mm a"))
                    timeOutText.text = mockData.timeOut?.format(DateTimeFormatter.ofPattern("h:mm a"))
                    statusChip.apply {
                        text = if (mockData.isLate) "Late" else "On Time"
                        setChipBackgroundColorResource(
                            if (mockData.isLate) R.color.material_warning_container
                            else R.color.material_success_container
                        )
                    }
                } else {
                    card.setCardBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorErrorContainer))
                    timeInText.text = "Not attended"
                    timeOutText.visibility = GONE
                    statusChip.apply {
                        text = "Absent"
                        setChipBackgroundColorResource(R.color.material_error_container)
                    }
                }
            }
            ViewType.FUTURE -> {
                val targetInText = view.findViewById<MaterialTextView>(R.id.targetTimeInText)
                val targetOutText = view.findViewById<MaterialTextView>(R.id.targetTimeOutText)

                targetInText.text = mockData.targetTimeIn.format(DateTimeFormatter.ofPattern("h:mm a"))
                targetOutText.text = mockData.targetTimeOut.format(DateTimeFormatter.ofPattern("h:mm a"))
                
                card.setCardBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceVariant))
            }

            ViewType.NONE -> {
            }
        }

        return view
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


    private fun getMockAttendanceData(date: LocalDate): AttendanceData {
        // This would normally come from your data source
        return AttendanceData(
            date = date,
            timeIn = LocalTime.of(8, 30),
            timeOut = LocalTime.of(17, 0),
            targetTimeIn = LocalTime.of(8, 0),
            targetTimeOut = LocalTime.of(17, 0),
            isAttended = true,
            isLate = false
        )
    }

    enum class ViewType {
        PREVIOUS, CURRENT, FUTURE, NONE
    }
}