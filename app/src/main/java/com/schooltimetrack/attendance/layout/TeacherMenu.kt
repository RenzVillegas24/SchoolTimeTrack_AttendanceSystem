package com.schooltimetrack.attendance.layout

import AttendanceDay
import UserDocument
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.AutoCompleteTextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.imageview.ShapeableImageView
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.bottomsheet.TimeSettingsBottomSheet
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
import io.appwrite.Query
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
import kotlin.properties.Delegates

enum class TeacherMenuMode {
    SCHEDULE,
    ATTENDANCE
}

class TeacherScheduleMenu : Fragment() {
    private lateinit var selectedDatesText: TextView
    private lateinit var selectedListDatesText: TextView
    private lateinit var currentScheduleAttendanceText: TextView
    private lateinit var currentListSchedule: LinearLayout
    private lateinit var setTimeButton: MaterialButton
    private lateinit var calendarSelection: MaterialCalendarView
    private var userDocument: UserDocument? = null
    private lateinit var navController: NavController
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var databases: Databases
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var currentMode = TeacherMenuMode.SCHEDULE
    private lateinit var studentSelection: LinearLayout

    private var colPrimary by Delegates.notNull<Int>()
    private var colSub by Delegates.notNull<Int>()
    private var colTextPrimary by Delegates.notNull<Int>()
    private var colTextSub by Delegates.notNull<Int>()
    
    private val scheduledDates = mutableMapOf<LocalDate, AttendanceDay>()

    // Add new properties
    private var selectedStudentId: String? = null
    private lateinit var studentSpinner: AutoCompleteTextView

    // Add at the top with other properties
    private var students: List<Map<String, Any>> = emptyList()
    private var isCalendarEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController


        // Initialize Appwrite
        client = (activity as MainActivity).client
        storage = (activity as MainActivity).storage
        databases = (activity as MainActivity).databases

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

    private fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teacher_menu, container, false)

        calendarSelection = view.findViewById<MaterialCalendarView>(R.id.calendarSelection)
        selectedListDatesText = view.findViewById<TextView>(R.id.selectedListDatesText)
        val segSelectionType = try {
            view.findViewById<SegmentedControl>(R.id.segSelectionType)
        } catch (e: Exception) {
            TODO("Not yet implemented")
        }

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
        currentScheduleAttendanceText = view.findViewById(R.id.currentScheduleAttendanceText)
        currentListSchedule = view.findViewById(R.id.currentListSchedule)
        setTimeButton = view.findViewById(R.id.setTimeButton)

        studentSpinner = view.findViewById(R.id.studentSpinner)

        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        studentSelection = view.findViewById(R.id.studentSelection)

        val sBottom = view.findViewById<Space>(R.id.sBottom)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)
            navView.getHeaderView(0)?.setPadding(16.toDp(), statusBar.top + 32.toDp(), 16.toDp(), 16.toDp())
            sBottom.layoutParams.height = navBar.bottom

            insets
        }

        // Setup navigation drawer
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Handle navigation item selection
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_schedules -> {
                    currentMode = TeacherMenuMode.SCHEDULE
                    updateUIForMode()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_attendance -> {
                    currentMode = TeacherMenuMode.ATTENDANCE
                    updateUIForMode() 
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        calendarSelection.selectRange(CalendarDay.today(), CalendarDay.today())
        showSchedules(calendarSelection.selectedDates)
        calendarSelection.addOnRangeSelectedListener(object: OnRangeSelectedListener {
            override fun onRangeSelected(widget: MaterialCalendarView, dates: List<CalendarDay>) {
                if (!isCalendarEnabled) return
                selectedListDatesText.text = dateGroup(dates.map { itt -> itt.date.time})
                setTimeButton.isEnabled = dates.isNotEmpty()

                if (currentMode == TeacherMenuMode.ATTENDANCE && selectedStudentId != null) {
                    showStudentAttendance(selectedStudentId!!)
                } else {
                    showSchedules(dates)
                }
            }
        })

        calendarSelection.addOnDateChangedListener(object: OnDateSelectedListener {
            override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
                if (!isCalendarEnabled) return
                selectedListDatesText.text = dateGroup(widget.selectedDates.map { itt -> itt.date.time})
                setTimeButton.isEnabled = selected

                if (currentMode == TeacherMenuMode.ATTENDANCE && selectedStudentId != null) {
                    showStudentAttendance(selectedStudentId!!)
                } else {
                    showSchedules(widget.selectedDates) 
                }
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


                    // Update the header with user info
                    navView.getHeaderView(0)?.let { header ->
                        header.findViewById<TextView>(R.id.nav_header_name)?.text = userDocument?.name?.filter { it.isNotEmpty() }?.joinToString(" ")
                        header.findViewById<TextView>(R.id.nav_header_email)?.text = userDocument?.email
                        val imageView = header.findViewById<ShapeableImageView>(R.id.nav_header_image)
                        imageView.setImageBitmap(bitmap)
                    }


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

        // Initialize UI for default mode
        updateUIForMode()

        // Add after navView initialization
        updateNavigationDrawer()

        return view
    }

    private fun showSchedules(dates: List<CalendarDay>) {
        if (scheduledDates.isNotEmpty()) {
            val selectedDates = dates.map { dateToDay(it.date.time) }
            Log.d("TeacherMenu", "Selected dates: $selectedDates")
            scheduledDates.filterKeys { it in selectedDates }
                .map { it.value }
                .sortedBy { it.date }
                .let { days ->
                    currentScheduleAttendanceText.visibility = if (days.isNotEmpty()) View.VISIBLE else View.GONE

                    currentListSchedule.removeAllViews()
                    
                    
                val grouped = days.groupBy { it.date.month }
                grouped.forEach { (month, items) ->
                   val monthCard = MaterialCardView(requireContext()).apply {
                        setContentPadding(16.toDp(), 16.toDp(), 16.toDp(), 16.toDp())
                        cardElevation = 0f
                        radius = 16.toDp().toFloat()
                        setCardBackgroundColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, Color.WHITE))
                       clipToPadding = false
                        clipChildren = false
                       strokeWidth = 0
                    }
                    val monthLinearLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    monthCard.addView(monthLinearLayout)

                    val monthTitle = TextView(requireContext()).apply {
                        text = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
                        textSize = 20f
                        setTextColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
                        setPadding(0, 0, 0, 12.toDp())
                    }
                    monthLinearLayout.addView(monthTitle)

                    items.sortedBy { it.date }.forEach { day ->
                        val dayView = layoutInflater.inflate(R.layout.item_normal_attendance_day, null).apply {
                            findViewById<TextView>(R.id.dateText).text = day.date.dayOfMonth.toString()
                            findViewById<TextView>(R.id.dayOfWeek).text = day.date.dayOfWeek.name.lowercase()
                                .replaceFirstChar { ch -> ch.uppercaseChar() }
                            findViewById<TextView>(R.id.targetTimeInText).text =
                                DateTimeFormatter.ofPattern("h:mm a").format(day.targetTimeIn)
                            findViewById<TextView>(R.id.targetTimeOutText).text =
                                DateTimeFormatter.ofPattern("h:mm a").format(day.targetTimeOut)

                            // set margin for the day view if not the last item
                            if (day != items.last()) {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 0, 0, 8.toDp())
                                }
                            }
                        }
                        monthLinearLayout.addView(dayView)
                    }

                    // set margin for the month card if not the last item
                    if (month != grouped.keys.last()) {
                        monthCard.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 16.toDp())
                        }
                    }

                    currentListSchedule.addView(monthCard)
                }
                }
        } else {
            currentScheduleAttendanceText.visibility = View.GONE
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

    private fun updateNavigationDrawer() {
        // Uncheck all items first
        navView.menu.forEach { item ->
            item.isChecked = false
        }
        
        // Check the current mode's menu item
        when (currentMode) {
            TeacherMenuMode.SCHEDULE -> {
                navView.menu.findItem(R.id.nav_schedules).isChecked = true
            }
            TeacherMenuMode.ATTENDANCE -> {
                navView.menu.findItem(R.id.nav_attendance).isChecked = true
            }
        }
    }

    // Add this function to handle calendar state
    private fun updateCalendarState() {
        when (currentMode) {
            TeacherMenuMode.SCHEDULE -> {
                calendarSelection.isEnabled = true
                isCalendarEnabled = true
            }
            TeacherMenuMode.ATTENDANCE -> {
                calendarSelection.isEnabled = selectedStudentId != null
                isCalendarEnabled = selectedStudentId != null
            }
        }
    }

    // Modify updateUIForMode()
    private fun updateUIForMode() {
        when (currentMode) {
            TeacherMenuMode.SCHEDULE -> {
                studentSelection.visibility = View.GONE
                setTimeButton.visibility = View.VISIBLE
                currentScheduleAttendanceText.text = "Current Schedules"
                updateCalendarState()
                // Reset calendar to today and refresh schedules
                resetCalendarToToday()
                updateScheduleDates()
            }
            TeacherMenuMode.ATTENDANCE -> {
                studentSelection.visibility = View.VISIBLE
                setTimeButton.visibility = View.GONE
                currentScheduleAttendanceText.text = "Current Attendance"
                // Reset calendar to today before loading student list
                resetCalendarToToday()
                loadStudentList()
                updateCalendarState()
            }
        }
        // Update navigation drawer state
        updateNavigationDrawer()
    }

    // Add new helper function to reset calendar
    private fun resetCalendarToToday() {
        // Clear any existing selections
        calendarSelection.clearSelection()
        // Select today's date
        val today = CalendarDay.today()
        calendarSelection.selectRange(today, today)
        // Ensure calendar shows current month
        calendarSelection.setCurrentDate(today)
    }

    // Replace loadStudentList() with this version
    private fun loadStudentList() {
        lifecycleScope.launch {
            try {
                val studentsResult = databases.listDocuments(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    queries = listOf(
                        Query.equal("grade", userDocument?.grade ?: ""),
                        Query.equal("section", userDocument?.section ?: ""),
                        Query.equal("type", "attendance")
                    )
                )

                // Store full student data
                students = studentsResult.documents.map { student ->
                    val studentDoc = databases.getDocument(
                        databaseId = "6774d5c500013f347412",
                        collectionId = "677f45d0003a18299bdc",
                        documentId = student.id
                    )
                    student.data + studentDoc.data
                }

                val studentAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    students.map { student ->
                        (student["name"] as? List<*>)?.filterNotNull()?.joinToString(" ") ?: ""
                    }
                )

                studentSpinner.setAdapter(studentAdapter)

                // Auto-select first student if available
                if (selectedStudentId == null && students.isNotEmpty()) {
                    selectedStudentId = studentsResult.documents.first().id
                    studentSpinner.setText(studentAdapter.getItem(0), false)
                    updateCalendarState()
                    if (calendarSelection.selectedDates.isNotEmpty()) {
                        showStudentAttendance(selectedStudentId!!)
                    }
                }

                studentSpinner.setOnItemClickListener { _, _, position, _ ->
                    selectedStudentId = studentsResult.documents[position].id
                    updateCalendarState()
                    if (calendarSelection.selectedDates.isNotEmpty()) {
                        showStudentAttendance(selectedStudentId!!)
                    }
                }

            } catch (e: Exception) {
                Log.e("TeacherMenu", "Error loading students", e)
            }
        }
    }

    private fun showStudentAttendance(studentId: String) {
        lifecycleScope.launch {
            try {
                val attendance = databases.getDocument(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    documentId = studentId
                )

                currentListSchedule.removeAllViews()
                val selectedDates = calendarSelection.selectedDates.map { dateToDay(it.date.time) }

                // Create a map of existing attendance records
                val attendanceMap = (attendance.data["targetInOut"] as? List<*>)?.associate {
                    val parts = it.toString().split(",")
                    LocalDate.ofEpochDay(parts[0].toLong()) to Triple(
                        LocalDate.ofEpochDay(parts[0].toLong()),
                        if (parts[1] != "null") LocalTime.parse(parts[1]) else null,
                        if (parts[2] != "null") LocalTime.parse(parts[2]) else null
                    )
                } ?: emptyMap()

                val today = LocalDate.now()
                
                // Filter out future dates when creating records
                val records = selectedDates.mapNotNull { date ->
                    // Only include dates up to today
                    // if (date.isAfter(today)) {
                    //     null
                    // } else {
                        scheduledDates[date]?.let { schedule ->
                            // Use existing attendance record or create absent record
                            attendanceMap[date] ?: Triple(date, null, null)
                        }
                    // }
                }.sortedBy { it.first }

                // Hide text if no records
                currentScheduleAttendanceText.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE

                val grouped = records.groupBy { it.first.month }
                grouped.forEach { (month, monthRecords) ->
                    val monthCard = MaterialCardView(requireContext()).apply {
                        setContentPadding(16.toDp(), 16.toDp(), 16.toDp(), 16.toDp())
                        cardElevation = 0f
                        radius = 16.toDp().toFloat()
                        strokeWidth = 0
                        clipChildren = false
                        clipToPadding = false
                        setCardBackgroundColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, Color.WHITE))
                    }

                    val monthLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    monthCard.addView(monthLayout)

                    val monthTitle = TextView(requireContext()).apply {
                        text = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
                        textSize = 20f
                        setTextColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
                        setPadding(0, 0, 0, 12.toDp())
                    }
                    monthLayout.addView(monthTitle)

                    monthRecords.forEach { (date, timeIn, timeOut) ->
                        scheduledDates[date]?.let { schedule ->
                            val recordView = layoutInflater.inflate(
                                R.layout.item_student_attendance_record,
                                monthLayout,
                                false
                            )

                            recordView.apply {
                                // Set date info
                                findViewById<TextView>(R.id.dateText).text = date.dayOfMonth.toString()
                                findViewById<TextView>(R.id.dayOfWeek).text = date.dayOfWeek.toString()
                                    .lowercase().replaceFirstChar { it.uppercase() }

                                // Set target times
                                findViewById<TextView>(R.id.targetTimeInText).text = 
                                    schedule.targetTimeIn.format(DateTimeFormatter.ofPattern("h:mm a"))
                                findViewById<TextView>(R.id.targetTimeOutText).text = 
                                    schedule.targetTimeOut.format(DateTimeFormatter.ofPattern("h:mm a"))

                                // Set actual attendance times
                                findViewById<TextView>(R.id.timeInText).text = timeIn?.format(
                                    DateTimeFormatter.ofPattern("h:mm a")
                                ) ?: "-"
                                findViewById<TextView>(R.id.timeOutText).text = timeOut?.format(
                                    DateTimeFormatter.ofPattern("h:mm a")
                                ) ?: "-"

                                // Set status chip
                                val (status, color) = when {
                                    date.isAfter(today) -> 
                                        Pair("Upcoming", MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, Color.GRAY))
                                    timeIn == null && timeOut == null -> 
                                        Pair("Absent", MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED))
                                    timeIn != null && timeOut == null -> 
                                        Pair("Checked In", MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLUE))
                                    timeIn != null && timeOut != null -> {
                                        val isLate = timeIn.isAfter(schedule.targetTimeIn)
                                        val isEarlyOut = timeOut.isBefore(schedule.targetTimeOut)
                                        when {
                                            isLate && isEarlyOut -> 
                                                Pair("Late & Early Out", MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED))
                                            isLate -> 
                                                Pair("Late", MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED))
                                            isEarlyOut -> 
                                                Pair("Early Out", MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED))
                                            else -> 
                                                Pair("Present", MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.GREEN))
                                        }
                                    }
                                    else -> Pair("Unknown", MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED))
                                }

                                findViewById<Chip>(R.id.statusChip).apply {
                                    text = status
                                    setChipBackgroundColorResource(android.R.color.transparent)
                                    setTextColor(color)
                                    chipStrokeColor = ColorStateList.valueOf(color)
                                }

                                // Add margin if not last item
                                if (date != monthRecords.last().first) {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(0, 0, 0, 8.toDp())
                                    }
                                }
                            }

                            monthLayout.addView(recordView)
                        }
                    }

                    if (month != grouped.keys.last()) {
                        monthCard.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 16.toDp())
                        }
                    }

                    currentListSchedule.addView(monthCard)
                }
            } catch (e: Exception) {
                Log.e("TeacherMenu", "Error showing attendance", e)
                currentScheduleAttendanceText.visibility = View.GONE
            }
        }
    }
}