package com.schooltimetrack.attendance.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.model.AttendanceDay
import com.schooltimetrack.attendance.model.ViewType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimeSettingsBottomSheet : BottomSheetDialogFragment() {
    private var timeSettingsListener: TimeSettingsListener? = null
    private lateinit var selectedDate: LocalDate

    interface TimeSettingsListener {
        fun onTimeSettingsSaved(attendanceDay: AttendanceDay)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_time_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDate = arguments?.getString(ARG_DATE)?.let {
            LocalDate.parse(it)
        } ?: LocalDate.now()

        view.findViewById<TextView>(R.id.dateText).text = selectedDate.format(
            DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        )

        val timeInPicker = view.findViewById<TimePicker>(R.id.timeInPicker)
        val timeOutPicker = view.findViewById<TimePicker>(R.id.timeOutPicker)

        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val timeIn = LocalTime.of(timeInPicker.hour, timeInPicker.minute)
            val timeOut = LocalTime.of(timeOutPicker.hour, timeOutPicker.minute)

            val attendanceDay = AttendanceDay(
                date = selectedDate,
                type = ViewType.CURRENT,
                targetTimeIn = timeIn,
                targetTimeOut = timeOut
            )

            timeSettingsListener?.onTimeSettingsSaved(attendanceDay)
            dismiss()
        }
    }

    fun setTimeSettingsListener(listener: TimeSettingsListener) {
        timeSettingsListener = listener
    }

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: LocalDate) = TimeSettingsBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_DATE, date.toString())
            }
        }
    }
}