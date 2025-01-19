import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.schooltimetrack.attendance.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import io.appwrite.services.Databases
import io.appwrite.Query
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.google.android.material.color.MaterialColors
import android.content.res.ColorStateList
import android.graphics.Color

class WeeklyAttendanceAdapter(
    private val onCheckInListener: () -> Unit,
    private val onCheckOutListener: () -> Unit,
    private val databases: Databases,
    private val grade: String,
    private val section: String,
    private val userId: String
) : RecyclerView.Adapter<WeeklyAttendanceAdapter.AttendanceViewHolder>() {

    private val items = mutableListOf<AttendanceDay>()

    init {
        GlobalScope.launch {
            loadScheduleData()
        }
    }

    suspend fun loadScheduleData() {
        // Load schedule data from Appwrite
        items.clear()
        val currentDate = LocalDate.now()

        coroutineScope {
            try {
                val schedules = databases.listDocuments(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    queries = listOf(
                        Query.equal("grade", grade),
                        Query.equal("section", section),
                        Query.equal("type", "schedule"),
                        Query.limit(1)
                    )
                )

                val attendance = databases.getDocument(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    documentId = userId
                )

                val scheduleTargets =
                    (schedules.documents.firstOrNull()?.data?.get("targetInOut") as List<*>).associate {
                        val stringSep = it.toString().split(",")
                        val date = LocalDate.ofEpochDay(stringSep[0].toLong())
                        val timeIn = LocalTime.parse(stringSep[1], DateTimeFormatter.ISO_LOCAL_TIME)
                        val timeOut = LocalTime.parse(stringSep[2], DateTimeFormatter.ISO_LOCAL_TIME)
                        date to Pair(timeIn, timeOut)
                    }

                val attendanceTargets = (
                    attendance.data["targetInOut"] as List<*>).associate {
                        val stringSep = it.toString().split(",")
                        val date = LocalDate.ofEpochDay(stringSep[0].toLong())
                        val timeIn = LocalTime.parse(stringSep[1], DateTimeFormatter.ISO_LOCAL_TIME)
                        if (stringSep[2] == "null") {
                            date to Pair(timeIn, null)
                        } else {
                            val timeOut = LocalTime.parse(stringSep[2], DateTimeFormatter.ISO_LOCAL_TIME)
                            date to Pair(timeIn, timeOut)
                        }
                    }


                for (i in -30 until 30) {
                    val date = currentDate.plusDays(i.toLong())
                    val type = if (i < 0) {
                        ViewType.PREVIOUS
                    } else if (i > 0) {
                        ViewType.FUTURE
                    } else {
                        ViewType.CURRENT
                    }

                    scheduleTargets[date]?.let { (targetTimeIn, targetTimeOut) ->
                        attendanceTargets[date]?.let { (timeIn, timeOut) ->
                            items.add(
                                AttendanceDay(
                                    date,
                                    type,
                                    targetTimeIn = targetTimeIn,
                                    targetTimeOut = targetTimeOut,
                                    timeIn = timeIn,
                                    timeOut = timeOut
                                )
                            )
                        } ?: items.add(
                            AttendanceDay(
                                date,
                                type,
                                targetTimeIn = targetTimeIn,
                                targetTimeOut = targetTimeOut
                            )
                        )
                    } ?: items.add(
                        AttendanceDay(
                            date,
                            ViewType.NONE
                        )
                    )
                }

                Handler(Looper.getMainLooper()).post {
                    notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    fun getDateAtPosition(position: Int): String {
        return items[position].date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    suspend fun updateAttendance(isCheckIn: Boolean) {
        try {
            // Get current document
            val attendance = databases.getDocument(
                databaseId = "6774d5c500013f347412",
                collectionId = "6785debf002943b87bb1",
                documentId = userId
            )



            // Get current attendance records
            val currentRecords = (attendance.data["targetInOut"] as List<*>).map {
            val parts = it.toString().split(",")
                AttendanceRecord(
                    date = LocalDate.ofEpochDay(parts[0].toLong()),
                    timeIn = if (parts[1] != "null") LocalTime.parse(parts[1]) else null,
                    timeOut = if (parts[2] != "null") LocalTime.parse(parts[2]) else null
                )
            }.sortedBy { it.date }.toMutableList()


            // Update record for today
            val today = LocalDate.now()
            val currentTime = LocalTime.now()
            val recordIndex = currentRecords.indexOfLast { it.date == today }

            if (recordIndex != -1) {
                currentRecords[recordIndex] = when {
                    isCheckIn -> currentRecords[recordIndex].copy(timeIn = currentTime)
                    else -> currentRecords[recordIndex].copy(timeOut = currentTime)
                }
            } else {
                if (isCheckIn) {
                    currentRecords.add(
                        AttendanceRecord(
                            date = today,
                            timeIn = currentTime,
                            timeOut = null
                        )
                    )
                } else {
                    Log.e("WeeklyAttendanceAdapter", "Cannot check out without checking in first")
                    return
                }
            }

            // Convert back to database format
            val updatedRecords = currentRecords.map { record ->
                "${record.date.toEpochDay()},${record.timeIn?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "null"},${record.timeOut?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "null"}"
            }


            // Update database
            databases.updateDocument(
                databaseId = "6774d5c500013f347412",
                collectionId = "6785debf002943b87bb1",
                documentId = userId,
                data = mapOf("targetInOut" to updatedRecords)
            )

            // Reload data to refresh UI
            loadScheduleData()
        } catch (e: Exception) {
            Log.e("WeeklyAttendanceAdapter", "Error updating attendance", e)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val layoutId = when (viewType) {
            ViewType.PREVIOUS.ordinal -> R.layout.item_previous_attendance_day
            ViewType.CURRENT.ordinal -> R.layout.item_current_attendance_day
            ViewType.FUTURE.ordinal -> R.layout.item_future_attendance_day
            else -> R.layout.item_no_schedule_day
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(attendanceDay: AttendanceDay) {
            val card = itemView.findViewById<MaterialCardView>(R.id.card)
            val dateText = itemView.findViewById<MaterialTextView>(R.id.dateText)
            val dayOfWeek = itemView.findViewById<MaterialTextView>(R.id.dayOfWeek)

            dateText.text = attendanceDay.date.format(DateTimeFormatter.ofPattern("d"))
            dayOfWeek.text = attendanceDay.date.format(DateTimeFormatter.ofPattern("EEEE"))

            when (attendanceDay.type) {
                ViewType.CURRENT -> bindCurrentDay(attendanceDay)
                ViewType.PREVIOUS -> bindPreviousDay(attendanceDay)
                ViewType.FUTURE -> bindFutureDay(attendanceDay)
                ViewType.NONE -> bindNoScheduleDay(attendanceDay)
            }
        }

        private fun Int.toDp(): Int = (this * itemView.resources.displayMetrics.density).toInt()

        private fun getStatusColor(status: String): Int {
            return when {
                status.contains("Late") || status.contains("Early") || status == "Absent" -> 
                    MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorError, Color.RED)
                status.contains("On Time") || status.contains("Overtime") || status.contains("Just In Time") ->
                    MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorPrimary, Color.GREEN)
                status == "Checked In" -> 
                    MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
                else -> 
                    MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorOutline, Color.GRAY)
            }
        }

        private fun bindCurrentDay(attendanceDay: AttendanceDay) {
            // Bind current day specific views
            itemView.findViewById<MaterialButton>(R.id.checkInButton)?.setOnClickListener { 
                onCheckInListener() 
            }
            itemView.findViewById<MaterialButton>(R.id.checkOutButton)?.setOnClickListener {
                onCheckOutListener() 
            }

            val targetTimeInText = itemView.findViewById<MaterialTextView>(R.id.targetTimeInText)
            val targetTimeOutText = itemView.findViewById<MaterialTextView>(R.id.targetTimeOutText)
            val statusChip = itemView.findViewById<Chip>(R.id.statusChip)

            targetTimeInText.text = attendanceDay.targetTimeIn.format(DateTimeFormatter.ofPattern("hh:mm a"))
            targetTimeOutText.text = attendanceDay.targetTimeOut.format(DateTimeFormatter.ofPattern("hh:mm a"))

            if (attendanceDay.timeIn == null) {
                itemView.findViewById<MaterialButton>(R.id.checkInButton)?.visibility = View.VISIBLE
                itemView.findViewById<MaterialButton>(R.id.checkOutButton)?.visibility = View.GONE
            } else if (attendanceDay.timeOut != null) {
                itemView.findViewById<MaterialCardView>(R.id.buttonContainer)?.visibility = View.GONE
            } else {
                itemView.findViewById<MaterialButton>(R.id.checkInButton)?.visibility = View.GONE
                itemView.findViewById<MaterialButton>(R.id.checkOutButton)?.visibility = View.VISIBLE
            }

            itemView.findViewById<MaterialTextView>(R.id.timeInText)?.text = attendanceDay.timeIn?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: ""
            itemView.findViewById<MaterialTextView>(R.id.timeOutText)?.text = attendanceDay.timeOut?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: ""

            val status = calculateStatusCurrent(attendanceDay.timeIn, attendanceDay.targetTimeIn, attendanceDay.timeOut, attendanceDay.targetTimeOut)
            statusChip.apply {
                text = status
                val color = getStatusColor(status)
                setChipBackgroundColorResource(android.R.color.transparent)
                setTextColor(color)
                chipStrokeWidth = 1f
                chipStrokeColor = ColorStateList.valueOf(color)
            }
        }

        private fun bindPreviousDay(attendanceDay: AttendanceDay) {
            // Bind previous day specific views

            val timeInText = itemView.findViewById<MaterialTextView>(R.id.timeInText)
            val timeOutText = itemView.findViewById<MaterialTextView>(R.id.timeOutText)
            val statusChip = itemView.findViewById<Chip>(R.id.statusChip)

            timeInText.text = attendanceDay.targetTimeIn.format(DateTimeFormatter.ofPattern("hh:mm a"))
            timeOutText.text = attendanceDay.targetTimeOut.format(DateTimeFormatter.ofPattern("hh:mm a"))

           if (attendanceDay.timeIn == null && attendanceDay.timeOut == null) {
                itemView.findViewById<MaterialButton>(R.id.checkInButton)?.visibility = View.GONE
                itemView.findViewById<MaterialButton>(R.id.checkOutButton)?.visibility = View.GONE
           } else {
               val targetTimeInText = itemView.findViewById<MaterialTextView>(R.id.targetTimeInText)
               val targetTimeOutText = itemView.findViewById<MaterialTextView>(R.id.targetTimeOutText)

               targetTimeOutText.text = attendanceDay.timeOut?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: ""
               targetTimeInText.text = attendanceDay.timeIn?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: ""

               if (attendanceDay.timeIn == null) {
                   itemView.findViewById<LinearLayout>(R.id.targetInContainer)?.visibility = View.GONE
               } else if (attendanceDay.timeOut == null) {
                   itemView.findViewById<LinearLayout>(R.id.targetOutContainer)?.visibility = View.GONE
               }

            }

            val status = calculateStatusPrevious(attendanceDay.timeIn, attendanceDay.targetTimeIn, attendanceDay.timeOut, attendanceDay.targetTimeOut)
            statusChip.apply {
                text = status
                val color = getStatusColor(status)
                setChipBackgroundColorResource(android.R.color.transparent)
                setTextColor(color)
                chipStrokeWidth = 1f
                chipStrokeColor = ColorStateList.valueOf(color)
            }
        }

        private fun bindFutureDay(attendanceDay: AttendanceDay) {
            // Bind future day specific views

            val targetTimeInText = itemView.findViewById<MaterialTextView>(R.id.targetTimeInText)
            val targetTimeOutText = itemView.findViewById<MaterialTextView>(R.id.targetTimeOutText)

            targetTimeInText.text = attendanceDay.targetTimeIn.format(DateTimeFormatter.ofPattern("hh:mm a"))
            targetTimeOutText.text = attendanceDay.targetTimeOut.format(DateTimeFormatter.ofPattern("hh:mm a"))

        }

        private fun bindNoScheduleDay(attendanceDay: AttendanceDay) {
            if (attendanceDay.date == LocalDate.now()){
                itemView.findViewById<MaterialTextView>(R.id.todayText)?.visibility = View.VISIBLE
                itemView.layoutParams.height = 110.toDp()
            } else {
                itemView.findViewById<MaterialTextView>(R.id.todayText)?.visibility = View.GONE
                itemView.layoutParams.height = 100.toDp()
            }


        }


        private fun calculateStatusCurrent(timeIn: LocalTime?, targetTimeIn: LocalTime, timeOut: LocalTime?, targetTimeOut: LocalTime): String {
            val currentTime = LocalTime.now()
            return when {
                timeIn == null && currentTime.isAfter(targetTimeIn) -> "Late"
                timeIn == null && timeOut == null -> "Absent"
                timeIn != null && timeOut == null -> "Checked In"
                timeIn != null && timeOut != null -> {
                    val inStatus = if (timeIn.isBefore(targetTimeIn)) "On Time" else if (timeIn == targetTimeIn) "Just In Time" else "Late"
                    val outStatus = if (timeOut.isAfter(targetTimeOut)) "Overtime" else if (timeOut == targetTimeOut) "Just In Time" else "Early"
                    "$inStatus / $outStatus"
                }
                else -> "Unknown"
            }
        }


        private fun calculateStatusPrevious(timeIn: LocalTime?, targetTimeIn: LocalTime, timeOut: LocalTime?, targetTimeOut: LocalTime): String {
            return when {
                timeIn == null && timeOut == null -> "Absent"
                timeIn != null && timeOut == null -> "Checked In"
                timeIn != null && timeOut != null -> {
                    val inStatus = if (timeIn.isBefore(targetTimeIn)) "On Time" else if (timeIn == targetTimeIn) "Just In Time" else "Late"
                    val outStatus = if (timeOut.isAfter(targetTimeOut)) "Overtime" else if (timeOut == targetTimeOut) "Just In Time" else "Early"

                    if (inStatus == outStatus)
                        inStatus
                    else
                        "$inStatus / $outStatus"
                }
                else -> "Unknown"
            }
        }
    }


}