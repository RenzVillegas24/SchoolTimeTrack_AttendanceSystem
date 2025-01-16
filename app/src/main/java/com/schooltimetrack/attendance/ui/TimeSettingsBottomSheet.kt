package com.schooltimetrack.attendance.layout

import AttendanceDay
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.schooltimetrack.attendance.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimeSettingsBottomSheet : BottomSheetDialogFragment() {
    private var timeSettingsListener: TimeSettingsListener? = null
    private var selectedDates: List<LocalDate> = emptyList()

    interface TimeSettingsListener {
        fun onTimeSettingsSaved(attendanceDays: List<AttendanceDay>)
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

        arguments?.getLongArray(ARG_DATES)?.forEach {
            Log.d("TimeSettingsBottomSheet", "Date: $it")
        }

        selectedDates = arguments?.getLongArray(ARG_DATES)
            ?.map {  Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ?: emptyList()

        Log.d("TimeSettingsBottomSheet", "Selected dates: $selectedDates")

        val timeInPicker = view.findViewById<TimePicker>(R.id.timeInPicker)
        val timeOutPicker = view.findViewById<TimePicker>(R.id.timeOutPicker)

        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val timeIn = LocalTime.of(timeInPicker.hour, timeInPicker.minute)
            val timeOut = LocalTime.of(timeOutPicker.hour, timeOutPicker.minute)

            val attendanceDays = selectedDates.map { date ->
                Log.d("TimeSettingsBottomSheet", "Setting time settings for $date")
                AttendanceDay(
                    date = date,
                    type = ViewType.CURRENT,
                    targetTimeIn = timeIn,
                    targetTimeOut = timeOut
                )
            }

            timeSettingsListener?.onTimeSettingsSaved(attendanceDays)
            dismiss()
        }
    }

    fun setTimeSettingsListener(listener: TimeSettingsListener) {
        timeSettingsListener = listener
    }

    companion object {
        private const val ARG_DATES = "dates"

        fun newInstance(dates: List<Long>) = TimeSettingsBottomSheet().apply {
            arguments = Bundle().apply {
                putLongArray(ARG_DATES, dates.toLongArray())
            }
        }
    }
}