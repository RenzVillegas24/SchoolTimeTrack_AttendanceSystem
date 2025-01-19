package com.schooltimetrack.attendance.layout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Space
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import com.schooltimetrack.attendance.qr.EncryptedScanner


class LoginQR : Fragment() {

    private lateinit var encryptedScanner: EncryptedScanner
    private lateinit var previewView: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login_qr, container, false)

        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)
        val qrGenerator = EncryptedGenerator()

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

        previewView = view.findViewById(R.id.viewFinder)
        encryptedScanner = EncryptedScanner(
            context = view.context,
            lifecycleOwner = viewLifecycleOwner,
            previewView = previewView,
            qrGenerator = qrGenerator,
            onDecryptedDataReceived = { jsonData, encryptedScanner ->

                Log.d("LoginQR", "Decrypted data: $jsonData")

                // Handle the decrypted JSON data
                val userType = jsonData.getString("userType")
                val name = jsonData.getJSONArray("name")
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

                encryptedScanner.pauseScanning()

                // Show material dialog with the user data
                MaterialAlertDialogBuilder(view.context)
                    .setTitle("User Data")
                    .setMessage(
                        "User Type: $userType\n" +
                        "Email: $email\n" +
                        "Password: $password\n" +
                        "Address: $address\n" +
                        "Section: $section\n" +
                        "Age: $age"
                    )
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        encryptedScanner.resumeScanning()
                    }
                    .setOnDismissListener {
                        encryptedScanner.resumeScanning()
                    }
                    .show()



            },
            onPermissionDenied = {
                // Handle permission denied
                Toast.makeText(view.context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        )

        btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginQR_to_signUp)
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        encryptedScanner.release()
    }
}