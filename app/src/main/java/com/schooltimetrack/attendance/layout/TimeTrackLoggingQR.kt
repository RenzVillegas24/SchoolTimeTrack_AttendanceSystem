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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.bottomsheet.FaceVerificationBottomSheet
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import com.schooltimetrack.attendance.qr.EncryptedScanner
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

class TimeTrackLoggingQR : Fragment() {

  private lateinit var encryptedScanner: EncryptedScanner
  private lateinit var previewView: PreviewView

  private lateinit var client: Client
  private lateinit var account: Account
  private lateinit var storage: Storage
  private lateinit var databases: Databases

  private lateinit var loadingDialog: LoadingDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Initialize Appwrite
    client = (activity as MainActivity).client
    account = (activity as MainActivity).account
    storage = (activity as MainActivity).storage
    databases = (activity as MainActivity).databases

    loadingDialog = LoadingDialog(requireContext())
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.fragment_time_track_logging_qr, container, false)


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
          // Handle the decrypted JSON data
          val email = jsonData.getString("email")
          val password = jsonData.getString("password")

          // if the user was a teacher then do not proceed
          if (jsonData.getString("userType") == "teacher") {
            MaterialAlertDialogBuilder(requireContext())
              .setTitle("Teacher Login")
              .setMessage("Teacher login is not allowed in TimeTrack Mode")
              .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                encryptedScanner.resumeScanning()
              }
              .setOnDismissListener { encryptedScanner.resumeScanning() }
              .show()
            return@EncryptedScanner
          }

          encryptedScanner.pauseScanning()

          // Show material dialog with the user data
          LoginQRDetailDialog(
            context = view.context,
            jsonData = jsonData,
            storage = storage,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onConfirm = {
              viewLifecycleOwner.lifecycleScope.launch {
                try {
                  // find the email in database
                  val emailQuery = databases.listDocuments(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "677f45d0003a18299bdc",
                    queries = listOf(Query.equal("email", email))
                  )

                  emailQuery.documents.firstOrNull()?.let { doc ->
                    val embedding =
                      (doc.data["embedding"] as ArrayList<Double>).map { it.toFloat() }
                        .toFloatArray()

                    loadingDialog.hide()


                    FaceVerificationBottomSheet(Triple(embedding, doc.id, doc.data["name"]),
                      { bottomSheet, results ->
                        Toast.makeText(context, "Face verified", Toast.LENGTH_SHORT).show()

                        // Navigate to the next screen
                        findNavController().navigate(R.id.action_timeTrackLoggingQR_to_timeTrackStudentMenu,
                          Bundle().apply {
                            putParcelable(
                              "UserDocument", UserDocument(
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
                            )
                          })

                        bottomSheet.dismiss()
                      },
                      { _ ->
                        encryptedScanner.resumeScanning()
                      }
                    ).show(childFragmentManager, "FaceVerificationBottomSheet")

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

              encryptedScanner.resumeScanning()
            },
            onCancel = {
              encryptedScanner.resumeScanning()
            },
            autoConfirm = true
          ).show()
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
