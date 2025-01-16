package com.schooltimetrack.attendance.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.schooltimetrack.attendance.R
import java.time.LocalDate

class AttendanceAdapter(
    private val onCheckInListener: () -> Unit,
    private val onCheckOutListener: () -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    private val dates = mutableListOf<LocalDate>()
    private val currentDate = LocalDate.now()

    init {
        // Add dates from 20 days ago to 20 days in future
        (-20..20).forEach { offset ->
            dates.add(currentDate.plusDays(offset.toLong()))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CURRENT -> R.layout.item_attendance_day
            VIEW_TYPE_NO_SCHEDULE -> R.layout.item_no_schedule_day
            VIEW_TYPE_PREVIOUS -> R.layout.item_previous_attendance_day
            else -> R.layout.item_future_attendance_day
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        holder.bind(date, date == currentDate)
    }

    override fun getItemCount() = dates.size

    override fun getItemViewType(position: Int): Int {
        val date = dates[position]
        return when {
            date.dayOfWeek.value > 5 -> VIEW_TYPE_NO_SCHEDULE
            date == currentDate -> VIEW_TYPE_CURRENT
            date.isBefore(currentDate) -> VIEW_TYPE_PREVIOUS
            else -> VIEW_TYPE_FUTURE
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(date: LocalDate, isCurrent: Boolean) {
            // Reuse existing binding logic from WeeklyAttendanceView
            if (isCurrent) {
                itemView.findViewById<View>(R.id.checkInButton)?.setOnClickListener { 
                    onCheckInListener() 
                }
                itemView.findViewById<View>(R.id.checkOutButton)?.setOnClickListener { 
                    onCheckOutListener() 
                }
            }
        }
    }

    companion object {
        const val VIEW_TYPE_CURRENT = 0
        const val VIEW_TYPE_PREVIOUS = 1
        const val VIEW_TYPE_FUTURE = 2
        const val VIEW_TYPE_NO_SCHEDULE = 3
    }
}