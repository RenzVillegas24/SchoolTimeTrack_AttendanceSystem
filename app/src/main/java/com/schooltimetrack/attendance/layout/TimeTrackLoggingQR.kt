package com.schooltimetrack.attendance.layout

import UserDocument
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Space
import android.widget.Toast
import androidx.activity.addCallback
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import com.schooltimetrack.attendance.qr.EncryptedScanner
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LoginQR : Fragment() {

  private lateinit var encryptedScanner: EncryptedScanner
  private lateinit var previewView: PreviewView

  private lateinit var client: Client
  private lateinit var account: Account
  private lateinit var storage: Storage
  private lateinit var databases: Databases

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Initialize Appwrite
    client = (activity as MainActivity).client
    account = (activity as MainActivity).account
    storage = (activity as MainActivity).storage
    databases = (activity as MainActivity).databases
  }

  override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.fragment_login_qr, container, false)


    val btnExit = view.findViewById<Button>(R.id.btnExit)
    btnExit.setOnClickListener {
        exit()
    }

    val btnSwitchCamera = view.findViewById<Button>(R.id.btnSwitchCamera)
    btnSwitchCamera.setOnClickListener {
        btnSwitchCamera.animate()
            .rotationBy(180f)
            .setDuration(700)
            .setInterpolator(PathInterpolator(0.3f, 1.5f, 0.25f, 1f))
            .start()
        encryptedScanner.switchCamera()
    }

    val bottomContainer = view.findViewById<FrameLayout>(R.id.bottomContainer)

    val qrGenerator = EncryptedGenerator()

    val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
      val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
      val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      ablToolbar.setPadding(0, statusBar.top, 0, 0)
        // set bottom margin to the bottom container
      val params = bottomContainer.layoutParams as ViewGroup.MarginLayoutParams
      params.bottomMargin = navBar.bottom
      bottomContainer.layoutParams = params
      insets
    }

    val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)

    toolbar.setNavigationOnClickListener {
        exit()
    }

      // on back press
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        exit()
    }

    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
    enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

    previewView = view.findViewById(R.id.viewFinder)
    encryptedScanner =
      EncryptedScanner(
        context = view.context,
        lifecycleOwner = viewLifecycleOwner,
        previewView = previewView,
        qrGenerator = qrGenerator,
        onDecryptedDataReceived = { jsonData, encryptedScanner ->
          Log.d("LoginQR", "Decrypted data: $jsonData")

          // Handle the decrypted JSON data
          val userType = jsonData.getString("userType")
          val name = jsonData.getJSONArray("name")
          val grade = jsonData.getString("grade")
          val section = jsonData.getString("section")
          val age = jsonData.getString("age")
          val address = jsonData.getJSONArray("address")
          val addressId = jsonData.getString("addressId")
          val email = jsonData.getString("email")
          val birthday = jsonData.getString("birthday")
          val gender = jsonData.getString("gender")
          val contactNumber = jsonData.getString("contactNumber")
          val password = jsonData.getString("password")
          //                val embedding = jsonData.getJSONArray("embedding")

            val addressString = address.join(", ").replace("\"", "")
            val nameString = name.join(" ").replace("\"", "")
          encryptedScanner.pauseScanning()

          // Show material dialog with the user data
          MaterialAlertDialogBuilder(view.context)
            .setTitle("Confirm Login")
            .setMessage("""
                User Type: ${userType.uppercase(Locale.ROOT)}
                User Name: $nameString
                Email: $email
                Address: $addressString
                Grade: $grade
                Section: $section
            """.trimIndent())
            .setPositiveButton("Login") { dialog, _ ->
              dialog.dismiss()
              viewLifecycleOwner.lifecycleScope.launch {
                try {
                  // find the email in database
                  val emailQuery = databases.listDocuments(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "677f45d0003a18299bdc",
                    queries = listOf(Query.equal("email", email))
                  )

                  emailQuery.documents.firstOrNull()?.let { doc ->
                    viewLifecycleOwner.lifecycleScope.launch {
                      try {

                        // check if the user is already logged in,
                        // then log out
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

                      Log.d("LoginQR", "User: $user")

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

                        (activity as MainActivity).userDocument = userDocument


                        // Navigate to the appropriate menu
                        when (emailQuery.documents[0].data["userType"]?.toString()
                        ) {"student" -> findNavController().navigate(R.id.action_loginQR_to_studentMenu,
                                Bundle().apply {
                                    putParcelable("UserDocument", userDocument)
                                })
                          "teacher" ->  findNavController().navigate(R.id.action_loginQR_to_teacherMenu,
                                Bundle().apply {
                                  putParcelable("UserDocument", userDocument)
                                })
                        }

                        withContext(Dispatchers.Main) {
                          Toast.makeText(
                                          context,
                                          "Authentication Successful: ${user.name}",
                                          Toast.LENGTH_SHORT
                                  )
                                  .show()
                        }
                      } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                          Toast.makeText(
                                          context,
                                          "Authentication Failed: ${e.message}",
                                          Toast.LENGTH_SHORT
                                  )
                                  .show()
                        }
                      }
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

              encryptedScanner.resumeScanning()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
              dialog.dismiss()
              encryptedScanner.resumeScanning()
            }
            .setOnDismissListener { encryptedScanner.resumeScanning() }
            .show()
        },
        onPermissionDenied = {
          // Handle permission denied
          Toast.makeText(view.context, "Camera permission denied", Toast.LENGTH_SHORT)
                  .show()
        }
      )



    return view
  }

    private fun exit() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exit Confirmation")
            .setMessage("Are you sure you want to exit the TimeTrack Mode?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

  override fun onDestroy() {
    super.onDestroy()
    encryptedScanner.release()
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
