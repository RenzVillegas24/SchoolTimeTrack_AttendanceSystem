package com.schooltimetrack.attendance.layout

import UserDocument
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.MainActivity
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
import android.widget.Space
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis

class StudentMenu : Fragment() {
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
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_menu, container, false)


        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
//        val sBottom = view.findViewById<Space>(R.id.sBottom)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)
//            sBottom.layoutParams.height = navBar.bottom
            insets
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

        return view
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