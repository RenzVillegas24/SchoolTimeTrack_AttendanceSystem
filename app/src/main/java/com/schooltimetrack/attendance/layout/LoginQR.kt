package com.schooltimetrack.attendance.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        previewView = view.findViewById(R.id.viewFinder)


        val encryptedScanner = EncryptedScanner(
            context = view.context,
            lifecycleOwner = viewLifecycleOwner,
            previewView = previewView,
            qrGenerator = qrGenerator,
            onDecryptedDataReceived = { jsonData, encryptedScanner ->
                // Handle the decrypted JSON data
                val userType = jsonData.getString("userType")
                val email = jsonData.getString("email")
                val password = jsonData.getString("password")
                val firstName = jsonData.getString("firstName")
                val lastName = jsonData.getString("lastName")
                val middleName = jsonData.getString("middleName")
                val suffixName = jsonData.getString("suffixName")
                val address = jsonData.getString("databases")
                val section = jsonData.getString("section")
                val age = jsonData.getString("age")


                encryptedScanner.pauseScanning()

                // Show material dialog with the user data
                MaterialAlertDialogBuilder(view.context)
                    .setTitle("User Data")
                    .setMessage(
                        "User Type: $userType\n" +
                                "Email: $email\n" +
                                "Password: $password\n" +
                                "First Name: $firstName\n" +
                                "Middle Name: $middleName\n" +
                                "Last Name: $lastName\n" +
                                "Suffix Name: $suffixName\n" +
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