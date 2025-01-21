package com.schooltimetrack.attendance.layout

import UserDocument
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.bottomsheet.GeneratedQRBottomSheet
import com.schooltimetrack.attendance.utils.LoadingDialog
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import java.time.format.DateTimeFormatter
import java.util.Locale

class UserInfo : Fragment() {
    private lateinit var navController: NavController
    private var userDocument: UserDocument? = null
    private lateinit var client: Client
    private lateinit var storage: Storage
    private lateinit var account: Account
    private lateinit var databases: Databases
    private lateinit var toolbar: MaterialToolbar

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (activity as MainActivity).navController

        arguments?.let {
            userDocument = it.getParcelable("UserDocument")
        }
        client = (activity as MainActivity).client
        storage = (activity as MainActivity).storage
        account = (activity as MainActivity).account
        databases = (activity as MainActivity).databases

        loadingDialog = LoadingDialog(requireContext())
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

        toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_chevron_left_24_filled)
        toolbar.setNavigationIconTint(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, "colorOnSurface"))
        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }

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


            // Generate Login QR Code
            view.findViewById<MaterialButton>(R.id.btnGenerateLoginQR).setOnClickListener {
                lifecycleScope.launch {
                    databases.listDocuments(
                        databaseId = "6774d5c500013f347412",
                        collectionId = "677f45d0003a18299bdc",
                        queries = listOf(
                            Query.equal("email", user.email)
                        )
                    ).documents.firstOrNull()?.let { userDoc ->

                        //  material alert dialog with password input
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("Enter Password")
                            setMessage("Please enter your password to continue.")
                            val view = layoutInflater.inflate(R.layout.dialog_password_field, null)
                            val etPassword = view.findViewById<EditText>(R.id.etPassword)
                            etPassword.requestFocus()
                            setView(view)
                            setPositiveButton("OK") { dialog, _ ->
                                val password = etPassword.text.toString()

                                lifecycleScope.launch {

                                    if (password == userDoc.data["password"].toString()) {

                                        dialog.dismiss()

                                        val data = mapOf(
                                            "userId" to userDoc.id,
                                            "userType" to userDoc.data["userType"].toString(),
                                            "name" to (userDoc.data["name"] as ArrayList<String>).toTypedArray(),
                                            "grade" to userDoc.data["grade"].toString(),
                                            "subject" to userDoc.data["subject"].toString(),
                                            "section" to userDoc.data["section"].toString(),
                                            "age" to userDoc.data["age"].toString().toInt(),
                                            "birthday" to userDoc.data["birthday"].toString(),
                                            "address" to (userDoc.data["address"] as ArrayList<String>).toTypedArray(),
                                            "addressId" to userDoc.data["addressId"].toString(),
                                            "birthday" to userDoc.data["birthday"].toString(),
                                            "gender" to userDoc.data["gender"].toString(),
                                            "profileImageId" to userDoc.data["profileImageId"].toString(),
                                            "embedding" to (userDoc.data["embedding"] as ArrayList<Double>).map { it.toFloat() }.toFloatArray(),
                                            "email" to userDoc.data["email"].toString(),
                                            "password" to password,
                                            "contactNumber" to (userDoc.data["contactNumber"] as ArrayList<String>).toArray(arrayOfNulls<String>(0))
                                        )

                                        Log.d("QR Data", data.toString())
                                        GeneratedQRBottomSheet(data).show(parentFragmentManager, "GeneratedQRBottomSheet")
                                    } else {
                                        MaterialAlertDialogBuilder(requireContext()).apply {
                                            setTitle("Incorrect Password")
                                            setMessage("The password you entered is incorrect.")
                                            setPositiveButton("OK") { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                        }.show()
                                    }

                                }

                            }
                            setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                        }.show()

                    }

                }
            }



            // logout button
            view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle("Logout")
                    setMessage("Are you sure you want to logout?")
                    setPositiveButton("Yes") { _, _ ->
                        loadingDialog.show("Logging out...")
                        lifecycleScope.launch {
                            val userTypeTmp = user.userType
                            try {
                                account.deleteSessions()
                                (activity as MainActivity).userDocument = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                // show confirmation dialog
                                loadingDialog.hide()
                                MaterialAlertDialogBuilder(requireContext()).apply {
                                    setTitle("Logout")
                                    setMessage("You have been logged out.")
                                    setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        navController.navigate(
                                            R.id.action_userInfo_to_welcome,
                                            Bundle().apply { putParcelable("UserDocument", userDocument) },
                                            androidx.navigation.NavOptions.Builder()
                                                .setPopUpTo(if (userTypeTmp == "student") R.id.studentMenu else R.id.teacherMenu, true)
                                                .build()
                                        )
                                    }
                                    setCancelable(false)
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