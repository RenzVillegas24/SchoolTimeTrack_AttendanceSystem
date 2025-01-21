package com.schooltimetrack.attendance.layout

import AttendanceDay
import UserDocument
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.ui.WeeklyAttendanceView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import com.google.android.material.imageview.ShapeableImageView
import android.widget.LinearLayout
import android.util.Log
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.ui.SegmentedControl
import com.shuhart.materialcalendarview.CalendarDay
import com.shuhart.materialcalendarview.MaterialCalendarView
import com.shuhart.materialcalendarview.OnDateSelectedListener
import com.shuhart.materialcalendarview.OnRangeSelectedListener
import com.shuhart.materialcalendarview.indicator.pager.CustomPager
import com.shuhart.materialcalendarview.indicator.pager.PagerContainer
import com.shuhart.materialcalendarview.indicator.pager.PagerIndicatorAdapter
import com.shuhart.materialcalendarview.format.DayFormatter
import io.appwrite.Query
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.properties.Delegates

class StudentMenu : Fragment() {
    private var currentMode = TeacherMenuMode.SCHEDULE
    private lateinit var weeklyAttendanceView: WeeklyAttendanceView
    private lateinit var calendarSelection: MaterialCalendarView
    private lateinit var currentAttendanceText: TextView
    private lateinit var currentListAttendance: LinearLayout
    private lateinit var selectedListDatesText: TextView
    private lateinit var monthYearTextView: TextView
    private lateinit var attendanceContainer: NestedScrollView
    private lateinit var bottomTextView: TextView
    private val scheduledDates = mutableMapOf<LocalDate, AttendanceDay>()
    private lateinit var navController: NavController
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var databases: Databases
    private var userDocument: UserDocument? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView


    private var colPrimary by Delegates.notNull<Int>()
    private var colSub by Delegates.notNull<Int>()
    private var colTextPrimary by Delegates.notNull<Int>()
    private var colTextSub by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController


        client = (activity as MainActivity).client
        storage = (activity as MainActivity).storage
        databases = (activity as MainActivity).databases

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
        }
    }

    // int as dp
    private fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_menu, container, false)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationIcon(R.drawable.ic_menu_rounded)

        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
        weeklyAttendanceView = view.findViewById(R.id.weeklyAttendanceView)
        monthYearTextView = view.findViewById(R.id.monthYearTextView)
        attendanceContainer = view.findViewById(R.id.attendanceContainer)
        bottomTextView = view.findViewById(R.id.bottomTextView)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        calendarSelection = view.findViewById(R.id.calendarSelection)
        currentAttendanceText = view.findViewById(R.id.currentAttendanceText)
        currentListAttendance = view.findViewById(R.id.currentListAttendance)
        selectedListDatesText = view.findViewById(R.id.selectedListDatesText)



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

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)

            // get the ?attr/actionBarSize attribute
            val actionBarSize = TypedValue().apply {
                context?.theme?.resolveAttribute(android.R.attr.actionBarSize, this, true) ?: 0
            }.getDimension(resources.displayMetrics).toInt()

            navView.getHeaderView(0)?.setPadding(16.toDp(), statusBar.top + 32.toDp(), 16.toDp(), 16.toDp())
            insets
        }


        // Set user profile image as menu icon
        lifecycleScope.launch {
            try {

//                weeklyAttendanceView.loadScheduleData()

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

                    // Update the header with user info
                    navView.getHeaderView(0)?.let { header ->
                        header.findViewById<TextView>(R.id.nav_header_name)?.text = 
                            userDocument?.name?.filter { it.isNotEmpty() }?.joinToString(" ")
                        header.findViewById<TextView>(R.id.nav_header_email)?.text = userDocument?.email
                        val imageView = header.findViewById<ShapeableImageView>(R.id.nav_header_image)
                        imageView.setImageBitmap(bitmap)
                    }
                }

                // Set click listener
                toolbar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.userInfo -> {
                            navController.navigate(
                                R.id.action_studentMenu_to_userInfo,
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

        // Set up navigation drawer
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

        // Set the onDateChangeListener to update the month and year TextView
        weeklyAttendanceView.setOnDateChangeListener { date ->
            val localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            val monthYear = localDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + localDate.year
            monthYearTextView.text = monthYear
        }

        // Setup calendar
        calendarSelection.apply {
            selectRange(CalendarDay.today(), CalendarDay.today())

            addOnDateChangedListener(object: OnDateSelectedListener {
                override fun onDateSelected(
                    widget: MaterialCalendarView,
                    date: CalendarDay,
                    selected: Boolean
                ) {
                    selectedListDatesText.text =
                        dateGroup(widget.selectedDates.map { it.date.time })

                    val selectedDates = widget.selectedDates.map { dateToDay(it.date.time) }

                    // If no dates selected, hide attendance views and return
                    if (selectedDates.isEmpty()) {
                        currentAttendanceText.visibility = View.GONE
                        currentListAttendance.removeAllViews()
                        return
                    } else {
                        if (selected && currentMode == TeacherMenuMode.ATTENDANCE) {
                            showAttendance()
                        }
                    }
                }
            })

            addOnRangeSelectedListener(object: OnRangeSelectedListener {
                override fun onRangeSelected(widget: MaterialCalendarView, dates: List<CalendarDay>) {
                    selectedListDatesText.text =
                        dateGroup(dates.map { it.date.time })

                    val selectedDates = widget.selectedDates.map { dateToDay(it.date.time) }

                    // If no dates selected, hide attendance views and return
                    if (selectedDates.isEmpty()) {
                        currentAttendanceText.visibility = View.GONE
                        currentListAttendance.removeAllViews()
                        return
                    } else {
                        if (currentMode == TeacherMenuMode.ATTENDANCE) {
                            showAttendance()
                        }
                    }
                }
            })
        }


        val segSelectionType = view.findViewById<SegmentedControl>(R.id.segSelectionType)
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


        return view
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


    private fun getConsecutiveNumbers(srcList: List<Long>): List<List<Long>> {
        return srcList.fold(mutableListOf<MutableList<Long>>()) { acc, i ->
            if (acc.isEmpty() || acc.last().last() + 86400000 != i) {
                acc.add(mutableListOf(i))
            } else acc.last().add(i)
            acc
        }
    }

    private fun updateUIForMode() {
        when (currentMode) {
            TeacherMenuMode.SCHEDULE -> {
                weeklyAttendanceView.visibility = View.VISIBLE
                attendanceContainer.visibility = View.GONE
                calendarSelection.visibility = View.GONE
                monthYearTextView.visibility = View.VISIBLE
                bottomTextView.visibility = View.VISIBLE
                currentAttendanceText.visibility = View.GONE
                currentListAttendance.visibility = View.GONE
                selectedListDatesText.visibility = View.GONE
            }
            TeacherMenuMode.ATTENDANCE -> {
                weeklyAttendanceView.visibility = View.GONE
                attendanceContainer.visibility = View.VISIBLE
                calendarSelection.visibility = View.VISIBLE
                monthYearTextView.visibility = View.GONE
                bottomTextView.visibility = View.GONE
                currentAttendanceText.visibility = View.VISIBLE
                currentListAttendance.visibility = View.VISIBLE
                selectedListDatesText.visibility = View.VISIBLE
                resetCalendarToToday()
                loadTeacherSchedules()
            }
        }
//        updateNavigationDrawer()
    }

    private fun loadTeacherSchedules() {
        lifecycleScope.launch {
            try {
                val schedules = databases.listDocuments(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "6785debf002943b87bb1",
                    queries = listOf(
                        Query.equal("grade", userDocument?.grade ?: ""),
                        Query.equal("section", userDocument?.section ?: ""),
                        Query.equal("type", "schedule")
                    )
                )

                Log.d("StudentMenu", "Schedules: ${schedules.documents}")

                schedules.documents.forEach { schedule ->
                    (schedule.data["targetInOut"] as? List<*>)?.forEach { date ->
                        val parts = date.toString().split(",")
                        val day = parts[0].toLong()
                        scheduledDates[LocalDate.ofEpochDay(day)] = AttendanceDay(
                            date = LocalDate.ofEpochDay(day),
                            type = ViewType.CURRENT,
                            targetTimeIn = LocalTime.parse(parts[1]),
                            targetTimeOut = LocalTime.parse(parts[2])
                        )
                    }
                }


                // Step 1: Add DayFormatter to calendar setup in onCreateView
                calendarSelection.setDayFormatter(object : DayFormatter {
                    override fun format(day: CalendarDay): String {
                        if (scheduledDates.containsKey(dateToDay(day.date.time))) {
                            return day.day.toString() + "\n•"
                        }
                        return day.day.toString() + "\n"
                    }
                })

                showAttendance()
            } catch (e: Exception) {
                Log.e("StudentMenu", "Error loading schedules", e)
            }
        }
    }

    fun dateToDay(date: Long): LocalDate {
        return Instant.ofEpochMilli(date).atZone(
            ZoneId.systemDefault()).toLocalDate()
    }

    private fun showAttendance() {
        // Get selected dates
        val selectedDates = calendarSelection.selectedDates.map { dateToDay(it.date.time) }
        
        // If no dates selected, hide attendance views and return
        if (selectedDates.isEmpty()) {
            currentAttendanceText.visibility = View.GONE
            currentListAttendance.removeAllViews()
            return
        } else {
            lifecycleScope.launch {
                try {
                    val attendance = databases.getDocument(
                        databaseId = "6774d5c500013f347412",
                        collectionId = "6785debf002943b87bb1",
                        documentId = userDocument?.userId ?: ""
                    )

                    currentListAttendance.removeAllViews()

                    // Create attendance map
                    val attendanceMap = (attendance.data["targetInOut"] as? List<*>)?.associate {
                        val parts = it.toString().split(",")
                        LocalDate.ofEpochDay(parts[0].toLong()) to Triple(
                            LocalDate.ofEpochDay(parts[0].toLong()),
                            if (parts[1] != "null") LocalTime.parse(parts[1]) else null,
                            if (parts[2] != "null") LocalTime.parse(parts[2]) else null
                        )
                    } ?: emptyMap()

                    val today = LocalDate.now()

                    // Step 2: Update attendance record creation in showStudentAttendance()
                    val records = selectedDates.mapNotNull { date ->
                        scheduledDates[date]?.let { schedule ->
                            // Use existing attendance record or create absent record
                            attendanceMap[date] ?: Triple(date, null, null)
                        }
                    }.sortedBy { it.first }

                    currentAttendanceText.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE

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
                            Log.d("StudentMenu", "Date: $date, TimeIn: $timeIn, TimeOut: $timeOut")

                            scheduledDates[date]?.let { schedule ->
                                val recordView = layoutInflater.inflate(
                                    R.layout.item_student_attendance_record,
                                    monthLayout,
                                    false
                                )

                                recordView.apply {
                                    findViewById<TextView>(R.id.dateText).text = date.dayOfMonth.toString()
                                    findViewById<TextView>(R.id.dayOfWeek).text = date.dayOfWeek.toString()
                                        .lowercase().replaceFirstChar { it.uppercase() }

                                    findViewById<TextView>(R.id.targetTimeInText).text =
                                        schedule.targetTimeIn.format(DateTimeFormatter.ofPattern("h:mm a"))
                                    findViewById<TextView>(R.id.targetTimeOutText).text =
                                        schedule.targetTimeOut.format(DateTimeFormatter.ofPattern("h:mm a"))

                                    findViewById<TextView>(R.id.timeInText).text = timeIn?.format(
                                        DateTimeFormatter.ofPattern("h:mm a")
                                    ) ?: "-"
                                    findViewById<TextView>(R.id.timeOutText).text = timeOut?.format(
                                        DateTimeFormatter.ofPattern("h:mm a")
                                    ) ?: "-"

                                    // Step 3: Update status chip logic in recordView.apply block
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
                                }

                                monthLayout.addView(recordView)
                            }
                        }

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 16.toDp())
                        }
                        currentListAttendance.addView(monthCard, params)
                    }
                } catch (e: Exception) {
                    Log.e("StudentMenu", "Error showing attendance", e)
                    currentAttendanceText.visibility = View.GONE
                    currentListAttendance.removeAllViews()
                }
            }

        }
    }


    private fun resetCalendarToToday() {
        calendarSelection.clearSelection()
        val today = CalendarDay.today()
        calendarSelection.selectRange(today, today)
        calendarSelection.setCurrentDate(today)
    }

    companion object {
        @JvmStatic
        fun newInstance(userDocument: UserDocument) =
            StudentMenu().apply {
                arguments = Bundle().apply {
                    putParcelable("UserDocument", userDocument)
                }
            }
    }
}