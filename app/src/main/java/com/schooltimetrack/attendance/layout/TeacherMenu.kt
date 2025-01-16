package com.schooltimetrack.attendance.layout

import AttendanceDay
import UserDocument
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.ui.SegmentedControl
import com.shuhart.materialcalendarview.CalendarDay
import com.shuhart.materialcalendarview.MaterialCalendarView
import com.shuhart.materialcalendarview.OnDateSelectedListener
import com.shuhart.materialcalendarview.OnRangeSelectedListener
import com.shuhart.materialcalendarview.format.DayFormatter
import com.shuhart.materialcalendarview.indicator.pager.CustomPager
import com.shuhart.materialcalendarview.indicator.pager.PagerContainer
import com.shuhart.materialcalendarview.indicator.pager.PagerIndicatorAdapter
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.properties.Delegates

class TeacherMenu : Fragment() {
    private lateinit var selectedDatesText: TextView
    private lateinit var selectedListDatesText: TextView
    private lateinit var currentScheduledText: TextView
    private lateinit var currentListSchedule: LinearLayout
    private lateinit var setTimeButton: MaterialButton
    private lateinit var calendarSelection: MaterialCalendarView
    private var userDocument: UserDocument? = null
    private lateinit var navController: NavController
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var databases: Databases


    private var colPrimary by Delegates.notNull<Int>()
    private var colSub by Delegates.notNull<Int>()
    private var colTextPrimary by Delegates.notNull<Int>()
    private var colTextSub by Delegates.notNull<Int>()
    
    private val scheduledDates = mutableMapOf<LocalDate, AttendanceDay>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController


        // Initialize Appwrite
        client = Client(requireContext())
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6773c26a001612edc5fb")
        databases = Databases(client)
        storage = Storage(client)

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
        }

    }


    fun updateScheduleDates(){
        scheduledDates.clear()

        lifecycleScope.launch {
            val targetDates = async {
                databases.getDocument(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    documentId = userDocument?.userId ?: ""
                )
            }



            targetDates.await().let {

                val data = it.data
                (data["targetInOut"] as List<*>).forEach { date ->
                    // seperate the in and out time (string) seperated by ,
                    val dateString = date.toString().split(",")
                    // parse time date epoch
                    val day = dateString[0].toLong()
                    val dateIn = dateString[1]
                    val dateOut = dateString[2]

                    // add the date to the selected dates
                    val attendanceDay = AttendanceDay(
                        date = LocalDate.ofEpochDay(day),
                        type = ViewType.CURRENT,
                        targetTimeIn = LocalTime.parse(dateIn),
                        targetTimeOut = LocalTime.parse(dateOut)
                    )

                    scheduledDates[attendanceDay.date] = attendanceDay
                }

                showSchedules(calendarSelection.selectedDates)

            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_menu, container, false)

        calendarSelection = view.findViewById<MaterialCalendarView>(R.id.calendarSelection)
        selectedListDatesText = view.findViewById<TextView>(R.id.selectedListDatesText)
        val segSelectionType = view.findViewById<SegmentedControl>(R.id.segSelectionType)

        client = (activity as MainActivity).client
        storage = Storage(client)
        databases = (activity as MainActivity).databases


        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)



        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationIcon(R.drawable.ic_menu_rounded)

        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        colPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary  , Color.BLACK)
        colSub = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSecondaryContainer, Color.BLACK)
        colTextPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.BLACK)
        colTextSub = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK)
        val pager = ((((calendarSelection.children.elementAt(0) as PagerContainer).children.first() as CustomPager).adapter) as PagerIndicatorAdapter)


        // set the color of the pager
        pager.defaultButtonBackgroundColor = colSub
        pager.selectedButtonBackgroundColor = colPrimary
        pager.defaultButtonTextColor = colTextSub
        pager.selectedButtonTextColor = colTextPrimary

        selectedDatesText = view.findViewById(R.id.selectedDatesText)
        currentScheduledText = view.findViewById(R.id.currentScheduledText)
        currentListSchedule = view.findViewById(R.id.currentListSchedule)
        setTimeButton = view.findViewById(R.id.setTimeButton)



        calendarSelection.selectRange(CalendarDay.today(), CalendarDay.today())
        showSchedules(calendarSelection.selectedDates)
        calendarSelection.addOnRangeSelectedListener(object: OnRangeSelectedListener {
            override fun onRangeSelected(widget: MaterialCalendarView, dates: List<CalendarDay>) {
                selectedListDatesText.text = dateGroup(calendarSelection.selectedDates.map { itt -> itt.date.time})
                setTimeButton.isEnabled = dates.isNotEmpty()

                showSchedules(dates)
            }
        })

        calendarSelection.addOnDateChangedListener(object: OnDateSelectedListener {
            override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
                selectedListDatesText.text = dateGroup(calendarSelection.selectedDates.map { itt -> itt.date.time})
                setTimeButton.isEnabled = selected

                showSchedules(widget.selectedDates)
            }
        })


        segSelectionType.setOnSegmentSelectedListener(object : SegmentedControl.OnSegmentSelectedListener {
            override fun onSegmentSelected(position: Int) {
                // Handle segment selection
                if (position == 0) {
                    calendarSelection.selectionMode = MaterialCalendarView.SELECTION_MODE_SINGLE
                } else if (position == 1) {
                    calendarSelection.selectionMode = MaterialCalendarView.SELECTION_MODE_RANGE
                } else {
                    calendarSelection.selectionMode = MaterialCalendarView.SELECTION_MODE_MULTIPLE
                }
            }
        })

        updateScheduleDates()

        // Set user profile image as menu icon
        lifecycleScope.launch {
            try {
                userDocument?.let { user ->
                    // Get profile image
                    val result = storage.getFilePreview(
                        bucketId = "6774d59e001b225502c9",
                        fileId = user.profileImageId
                    )

                    // Convert to bitmap
                    val bitmap = BitmapFactory.decodeByteArray(result, 0, result.size)

                    // Create circular bitmap drawable
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(
                        resources,
                        Bitmap.createScaledBitmap(bitmap, 128, 128, true)
                    ).apply {
                        isCircular = true
                    }

                    // Set as menu item icon
                    toolbar.menu.findItem(R.id.userInfo)?.icon = circularBitmapDrawable
                }

                // Set click listener
                toolbar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.userInfo -> {
                            navController.navigate(
                                R.id.action_teacherMenu_to_userInfo,
                                Bundle().apply {
                                    putParcelable("UserDocument", userDocument)
                                }
                            )
                            true
                        }
                        else -> false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setTimeButton.setOnClickListener {
            Log.d("TeacherMenu", "Selected dates: ${calendarSelection.selectedDates}")
            showTimeSettingsDialog(calendarSelection.selectedDates.map { it.date.time })
        }

        return view
    }

    private fun showSchedules(dates: List<CalendarDay>) {
        if (scheduledDates.isNotEmpty()) {
            val selectedDates = dates.map { dateToDay(it.date.time) }
            Log.d("TeacherMenu", "Selected dates: $selectedDates")
            scheduledDates.filterKeys { it in selectedDates }
                .map { it.value }
                .let { days ->
                    currentScheduledText.visibility = if (days.isNotEmpty()) View.VISIBLE else View.GONE

                    currentListSchedule.removeAllViews()
                    // add the item future schedule
                    days.forEach { day ->
                        layoutInflater.inflate(R.layout.item_normal_attendance_day, currentListSchedule, false).let { it ->
                            it.findViewById<TextView>(R.id.dateText).text = day.date.dayOfMonth.toString()
                            // Word case
                            it.findViewById<TextView>(R.id.dayOfWeek).text = day.date.dayOfWeek.toString()
                                .lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                            it.findViewById<TextView>(R.id.targetTimeInText).text = DateTimeFormatter.ofPattern("h:mm a").format(day.targetTimeIn)
                            it.findViewById<TextView>(R.id.targetTimeOutText).text = DateTimeFormatter.ofPattern("h:mm a").format(day.targetTimeOut)
                            currentListSchedule.addView(it)


                            if (day != days.last()) {
                                View(context).let {
                                    it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 32)
                                    currentListSchedule.addView(it)
                                }
                            }
                        }

                    }
                }
        } else {
            currentScheduledText.visibility = View.GONE
            currentListSchedule.removeAllViews()
        }


        calendarSelection.setDayFormatter(object : DayFormatter {
            override fun format(day: CalendarDay): String {
                // check if the current day was on the scheduled dates
                Log.d("TeacherMenu", "Day: ${day.date}")
                if (scheduledDates.containsKey(dateToDay(day.date.time))) {
                    return day.day.toString() + "\n•"
                }
                return day.day.toString() + "\n"
            }
        })
    }

    private fun getConsecutiveNumbers(srcList: List<Long>): List<List<Long>> {
        return srcList.fold(mutableListOf<MutableList<Long>>()) { acc, i ->
            if (acc.isEmpty() || acc.last().last() + 86400000 != i) {
                acc.add(mutableListOf(i))
            } else acc.last().add(i)
            acc
        }
    }

    fun dateToDay(date: Long): LocalDate {
        return Instant.ofEpochMilli(date).atZone(
            ZoneId.systemDefault()).toLocalDate()
    }

    fun dateGroup(lst:  List<Long>): String {
        var datesString = ""
        // get the consecutive dates
        getConsecutiveNumbers(lst).forEach {
            // check if there are more than one date
            if (it.size > 1) {
                // check if the dates are in the same month
                if (SimpleDateFormat("MMMM").format(it[0]) == SimpleDateFormat("MMMM").format(it[it.size - 1]))
                    datesString += SimpleDateFormat("MMMM d").format(it[0]) + " - " + SimpleDateFormat("d, yyyy").format(it[it.size - 1]) + "\n"
                // check if the dates are in the same year
                else if (SimpleDateFormat("yyyy").format(it[0]) == SimpleDateFormat("yyyy").format(it[it.size - 1]))
                    datesString += SimpleDateFormat("MMM d").format(it[0]) + " - " + SimpleDateFormat("MMM d, yyyy").format(it[it.size - 1]) + "\n"
                // otherwise, the dates are in different years
                else
                    datesString += SimpleDateFormat("MMM d, yyyy").format(it[0]) + " - " + SimpleDateFormat("MMM d, yyyy").format(it[it.size - 1]) + "\n"
                // check if there is only one date
            } else {
                datesString += SimpleDateFormat("MMMM d, yyyy").format(it[0]) + "\n"
            }

        }

        return datesString.trim()
    }

    private fun showTimeSettingsDialog(dates: List<Long>) {
        TimeSettingsBottomSheet.newInstance(dates).apply {
            setTimeSettingsListener(object : TimeSettingsBottomSheet.TimeSettingsListener {
                override fun onTimeSettingsSaved(attendanceDays: List<AttendanceDay>) {
                    attendanceDays.forEach { day ->
                        scheduledDates[day.date] = day
                    }

                    lifecycleScope.launch {
                        databases.updateDocument(
                            databaseId = "6774d5c500013f347412",
                            collectionId = "6785debf002943b87bb1",
                            documentId = userDocument?.userId ?: "",
                            data = mapOf(
                                "targetInOut" to scheduledDates.map {
                                    val timeDay = it.key.toEpochDay()
                                    val timeIn = LocalTime.of(it.value.targetTimeIn.hour, it.value.targetTimeIn.minute)
                                    val timeOut = LocalTime.of(it.value.targetTimeOut.hour, it.value.targetTimeOut.minute)
                                    "${timeDay},${timeIn},${timeOut}"
                                }
                            )
                        )
                    }
                    showSchedules(calendarSelection.selectedDates)
                }
            })
        }.show(childFragmentManager, "TimeSettings")
    }
}