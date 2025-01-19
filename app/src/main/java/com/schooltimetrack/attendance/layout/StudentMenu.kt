package com.schooltimetrack.attendance.layout

import UserDocument
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

class StudentAttendanceMenu : Fragment() {
    private lateinit var navController: NavController
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var databases: Databases
    private var userDocument: UserDocument? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
        }
    }

    // int as dp
    private fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_attendance_menu, container, false)

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
        val weeklyAttendanceView = view.findViewById<WeeklyAttendanceView>(R.id.weeklyAttendanceView)
        val monthYearTextView = view.findViewById<TextView>(R.id.monthYearTextView)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)

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
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_attendance -> {
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

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(userDocument: UserDocument) =
            StudentAttendanceMenu().apply {
                arguments = Bundle().apply {
                    putParcelable("UserDocument", userDocument)
                }
            }
    }
}