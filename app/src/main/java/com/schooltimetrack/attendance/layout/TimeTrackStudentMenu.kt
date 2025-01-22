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
import io.appwrite.Query
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import android.graphics.Bitmap
import android.util.Log
import android.util.TypedValue
import android.widget.Space
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class TimeTrackStudentMenu : Fragment() {
    private lateinit var navController: NavController
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var databases: Databases
    private var userDocument: UserDocument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
            Log.d("TimeTrackStudentMenu", "UserDocument: $userDocument")
        }
    }

    // int as dp
    private fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_menu, container, false)

        client = (activity as MainActivity).client
        storage = Storage(client)
        databases = (activity as MainActivity).databases

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
        val weeklyAttendanceView = view.findViewById<WeeklyAttendanceView>(R.id.weeklyAttendanceView)
        weeklyAttendanceView.apply {
            setGrade(userDocument?.grade ?: "")
            setSection(userDocument?.section ?: "")
            setUserId(userDocument?.userId ?: "")
        }
        val monthYearTextView = view.findViewById<TextView>(R.id.monthYearTextView)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)

            // get the ?attr/actionBarSize attribute
            val actionBarSize = TypedValue().apply {
                context?.theme?.resolveAttribute(android.R.attr.actionBarSize, this, true) ?: 0
            }.getDimension(resources.displayMetrics).toInt()

            insets
        }

        toolbar.setNavigationIcon(R.drawable.ic_chevron_left_24_filled)
        toolbar.setNavigationOnClickListener {
            exit()
        }

        // on back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            exit()
        }

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
                                R.id.action_timeTrackStudentMenu_to_userInfo,
                                Bundle().apply {
                                    putParcelable("UserDocument", userDocument)
                                    putBoolean("isTimeTrack", true)
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

        // Set the onDateChangeListener to update the month and year TextView
        weeklyAttendanceView.setOnDateChangeListener { date ->
            val localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            val monthYear = localDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + localDate.year
            monthYearTextView.text = monthYear
        }

        return view
    }

    private fun exit(){
        // confirm exit
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit the time tracking?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                navController.popBackStack()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}