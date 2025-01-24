package com.schooltimetrack.attendance.layout

import UserDocument
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Space
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.bottomsheet.FaceVerificationBottomSheet
import com.schooltimetrack.attendance.bottomsheet.ImageCropBottomSheet
import com.schooltimetrack.attendance.bottomsheet.LoginEmailBottomSheet
import com.schooltimetrack.attendance.bottomsheet.LoginQRBottomSheet
import com.schooltimetrack.attendance.utils.LoadingDialog
import com.schooltimetrack.attendance.utils.LoginQRDetailDialog
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class Welcome : Fragment() {

  private lateinit var client: Client
  private lateinit var account: Account
  private lateinit var storage: Storage
  private lateinit var databases: Databases

  private lateinit var loadingDialog: LoadingDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    client = (activity as MainActivity).client
    account = (activity as MainActivity).account
    storage = (activity as MainActivity).storage
    databases = (activity as MainActivity).databases

    loadingDialog = LoadingDialog(requireContext())
  }
  private fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_welcome, container, false)

    val btnLogin = view.findViewById<Button>(R.id.btnLogin)
    val btnLoginQR = view.findViewById<Button>(R.id.btnLoginQR)
    val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)
    val btnAutoLogin = view.findViewById<Button>(R.id.btnAutoLogin)


    val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
    val ctlToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.ctlToolbar)
    val ivHeader = view.findViewById<View>(R.id.ivHeader)

    // add an event to check if the header collapsed
    ablToolbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
      if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
        // set opacity of the header image to 0, animated
        ivHeader.animate().alpha(0f).setDuration(200).start()
      } else {
        // set opacity of the header image to 1, animated
        ivHeader.animate().alpha(1f).setDuration(200).start()
      }
    }

    val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
      val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
      val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

      toolbar.layoutParams.height = statusBar.top + 90.toDp()

      // set margin for the header image
      insets
    }

    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
    enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

    btnLogin.setOnClickListener {
      LoginEmailBottomSheet { bottomSheet, email, password ->
        login(email, password) {
          bottomSheet.dismiss()
        }
      }.show(parentFragmentManager, "LoginEmailBottomSheet")
    }

    btnLoginQR.setOnClickListener {
      LoginQRBottomSheet(
        onDecryptedDataReceived = { jsonData, encryptedScanner, bottomSheet ->

          // Handle the decrypted JSON data
          val email = jsonData.getString("email")
          val password = jsonData.getString("password")
          encryptedScanner.pauseScanning()

          // Show material dialog with the user data
          LoginQRDetailDialog(
            context = view.context,
            jsonData = jsonData,
            storage = storage,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onConfirm = {
              login(email, password) {
                bottomSheet.dismiss()
              }
              encryptedScanner.resumeScanning()
            },
            onCancel = {
              encryptedScanner.resumeScanning()
            }
          ).show()
        }
      ).show(parentFragmentManager, "LoginQRBottomSheet")
    }


    btnAutoLogin.setOnClickListener {
      findNavController().navigate(R.id.action_welcome_to_timeTrackLoggingQR)
    }

    btnSignUp.setOnClickListener {
      findNavController().navigate(R.id.action_welcome_to_signUp)
    }

    return view
  }

  private fun login(email: String, password: String, onLoginSuccess: () -> Unit) {
    loadingDialog.show("Loading face data...")

    viewLifecycleOwner.lifecycleScope.launch {
      try {
        // find the email in database
        val emailQuery = databases.listDocuments(
          databaseId = "6774d5c500013f347412",
          collectionId = "677f45d0003a18299bdc",
          queries = listOf(Query.equal("email", email))
        )

        emailQuery.documents.firstOrNull()?.let { doc ->

          val userDocument = UserDocument(
            userId = doc.id,
            userType = doc.data["userType"].toString(),
            name = (doc.data["name"] as ArrayList<String>),
            grade = doc.data["grade"].toString(),
            subject = doc.data["subject"].toString(),
            section = doc.data["section"].toString(),
            age = doc.data["age"].toString().toInt(),
            address = doc.data["address"] as ArrayList<String>,
            addressId = doc.data["addressId"].toString(),
            birthday = doc.data["birthday"].toString(),
            gender = doc.data["gender"].toString(),
            profileImageId = doc.data["profileImageId"].toString(),
            email = doc.data["email"].toString(),
            contactNumber = doc.data["contactNumber"] as ArrayList<String>
          )

          val name = doc.data["name"]
          val docId = doc.id
          val embedding =
            (doc.data["embedding"] as ArrayList<Double>).map { it.toFloat() }.toFloatArray()

          loadingDialog.hide()

          FaceVerificationBottomSheet(Triple(embedding, docId, name),
            { bottomSheet, results ->
            Toast.makeText(context, "Face verified", Toast.LENGTH_SHORT).show()
            viewLifecycleOwner.lifecycleScope.launch {
              try {
                loadingDialog.show("Logging in...")

                // check if the user is already logged in, then log out
                if (checkExistingSession()) {
                  account.deleteSessions()
                }

                // Create an email session
                val session = account.createEmailPasswordSession(
                  email = email,
                  password = password
                )

                // Get account details to verify login
                val user = account.get()

                // Store the user document in the MainActivity
                (activity as MainActivity).userDocument = userDocument

                // Call the onLoginSuccess callback
                onLoginSuccess()

                // Navigate to the appropriate menu
                when (emailQuery.documents[0].data["userType"]?.toString()
                ) {
                  "student" -> findNavController().navigate(R.id.action_welcome_to_studentMenu,
                    Bundle().apply {
                      putParcelable("UserDocument", userDocument)
                    })

                  "teacher" -> findNavController().navigate(R.id.action_welcome_to_teacherMenu,
                    Bundle().apply {
                      putParcelable("UserDocument", userDocument)
                    })
                }

                withContext(Dispatchers.Main) {
                  Toast.makeText(
                    context,
                    "Authentication Successful: ${user.name}",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                  Toast.makeText(
                    context,
                    "Authentication Failed: ${e.message}",
                    Toast.LENGTH_SHORT
                  ).show()
                }

              }
              loadingDialog.hide()

            }

            bottomSheet.dismiss()
          }).show(childFragmentManager, "FaceVerificationBottomSheet")

        } ?: run {
          withContext(Dispatchers.Main) {
            Toast.makeText(
              context,
              "No user found with this email",
              Toast.LENGTH_SHORT
            ).show()
          }
        }


      } catch (e: Exception) {
        Toast.makeText(
          context,
          "Authentication Failed: ${e.message}",
          Toast.LENGTH_SHORT
        )
          .show()
      }
    }
  }

  // Optional: Check if user is already logged in
  private suspend fun checkExistingSession(): Boolean {
    return try {
      account.get()
      true
    } catch (e: Exception) {
      false
    }
  }



}