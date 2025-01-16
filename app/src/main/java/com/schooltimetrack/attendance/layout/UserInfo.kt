package com.schooltimetrack.attendance.layout

import UserDocument
import android.graphics.BitmapFactory
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.schooltimetrack.attendance.R
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.address.AddressManager
import com.schooltimetrack.attendance.address.Barangay
import com.schooltimetrack.attendance.address.CityMun
import com.schooltimetrack.attendance.address.Province
import com.schooltimetrack.attendance.address.Region
import com.schooltimetrack.attendance.ui.SegmentedControl
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Storage
import java.time.format.DateTimeFormatter
import java.util.Locale

class UserInfo : Fragment() {
    private lateinit var navController: NavController
    private var userDocument: UserDocument? = null
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var account: Account



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
        }
        client = (activity as MainActivity).client
        storage = Storage(client)
        account = (activity as MainActivity).account
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)


        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
        val sBottom = view.findViewById<Space>(R.id.sBottom)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)
            sBottom.layoutParams.height = navBar.bottom
            insets
        }


        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)


        userDocument?.let { user ->

            Log.d("Address", user.addressId)

            // Display user information
            view.findViewById<ImageView>(R.id.ivProfileImage).let { imageView ->
                lifecycleScope.launch {
                    try {

                        val result = storage.getFileDownload(
                            bucketId = "6774d59e001b225502c9",
                            fileId = user.profileImageId
                        )

                        // Convert to bitmap
                        val bitmap = BitmapFactory.decodeByteArray(result, 0, result.size)

                        view.findViewById<ImageView>(R.id.ivProfileImage).setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            view.findViewById<TextInputLayout>(R.id.tilSubject).visibility = if (user.userType == "student") View.GONE else View.VISIBLE
            view.findViewById<TextView>(R.id.etFirstName).text = user.name[0]
            view.findViewById<TextView>(R.id.etMiddleName).text = user.name[1]
            view.findViewById<TextView>(R.id.etLastName).text = user.name[2]
            view.findViewById<TextView>(R.id.etSuffixName).text = user.name[3]
            view.findViewById<TextView>(R.id.etSubject).text = user.subject
            view.findViewById<TextView>(R.id.etGrade).text = user.grade
            view.findViewById<TextView>(R.id.etSection).text = user.section
            view.findViewById<TextView>(R.id.etAge).text = user.age.toString()
            DateTimeFormatter.ISO_DATE_TIME.parse(user.birthday) { date ->
                view.findViewById<TextView>(R.id.etBirthday).text = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(date)
            }
            view.findViewById<TextView>(R.id.etGender).text = user.gender
            view.findViewById<TextView>(R.id.etEmail).text = user.email
            view.findViewById<TextView>(R.id.etCountryCode).text = user.contactNumber[0]
            view.findViewById<TextView>(R.id.etContactNumber).text = user.contactNumber[1]
            view.findViewById<TextView>(R.id.etRegion).text = user.address[0]
            view.findViewById<TextView>(R.id.etProvince).text = user.address[1]
            view.findViewById<TextView>(R.id.etCityMun).text = user.address[2]
            view.findViewById<TextView>(R.id.etBrgy).text = user.address[3]
            view.findViewById<TextView>(R.id.etStreet).text = user.address[4]

            view.findViewById<TextView>(R.id.tvAccountType).text = "${user.userType.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString()
            }} Information"



            // logout button
            view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle("Logout")
                    setMessage("Are you sure you want to logout?")
                    setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                account.deleteSessions()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                // show confirmation dialog
                                MaterialAlertDialogBuilder(requireContext()).apply {
                                    setTitle("Logout")
                                    setMessage("You have been logged out.")
                                    setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        navController.popBackStack(R.id.welcome, true)
                                        navController.navigate(R.id.welcome)
                                    }
                                    show()
                                }
                            }
                        }
                    }
                    setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }


        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(userDocument: UserDocument) =
            UserInfo().apply {
                arguments = Bundle().apply {
                    putParcelable("UserDocument", userDocument)
                }
            }
    }
}