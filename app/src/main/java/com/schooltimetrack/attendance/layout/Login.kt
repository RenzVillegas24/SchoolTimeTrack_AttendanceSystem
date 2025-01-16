package com.schooltimetrack.attendance.layout

import UserDocument
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Space
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import io.appwrite.Client
import io.appwrite.services.Account
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.ai.FaceRecognition
import com.schooltimetrack.attendance.ui.FaceVerificationBottomSheet
import io.appwrite.Query
import io.appwrite.services.Databases
import io.appwrite.services.Storage

class Login : Fragment() {

    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var storage: Storage
    private lateinit var databases: Databases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Appwrite
        client = Client(requireContext())
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6773c26a001612edc5fb")
        account = Account(client)
        databases = Databases(client)
        storage = Storage(client)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnLoginQR = view.findViewById<Button>(R.id.btnLoginQR)
        val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)

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

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // find the email in database
                        val emailQuery = databases.listDocuments(
                            databaseId = "6774d5c500013f347412",
                            collectionId = "677f45d0003a18299bdc",
                            queries = listOf(
                                Query.equal("email", email)
                            )
                        )

                        emailQuery.documents.firstOrNull()?.let { doc ->

                            // get the profileImageId from the emailQuery
                            val profileImageId = doc.data["profileImageId"]?.toString()
                            val name = doc.data["name"]
                            val docId = doc.id
                            val embedding = (doc.data["embedding"] as ArrayList<Double>).map { it.toFloat() }.toFloatArray()

                            if (profileImageId == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Profile image not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }

                            FaceVerificationBottomSheet(Triple(embedding, docId, name)) { bottomSheet, results ->
                                Toast.makeText(context, "Face verified", Toast.LENGTH_SHORT).show()
                                viewLifecycleOwner.lifecycleScope.launch {
                                    try {
                                        // Create an email session
                                        val session = account.createEmailPasswordSession(
                                            email = email,
                                            password = password
                                        )

                                        // Get account details to verify login
                                        val user = account.get()

                                        val userDocument = UserDocument(
                                            userId = docId,
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


                                        // Navigate to the appropriate menu
                                        when (emailQuery.documents[0].data["userType"]?.toString()) {
                                            "student" -> findNavController().navigate(R.id.action_login_to_studentMenu,
                                                Bundle().apply {
                                                    putParcelable("UserDocument", userDocument)
                                                })
                                            "teacher" -> findNavController().navigate(R.id.action_login_to_teacherMenu,
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
                                }

                                bottomSheet.dismiss()
                            }.show(childFragmentManager, "FaceVerificationBottomSheet")

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
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error during login: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnLoginQR.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_loginQR)
        }

        btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signUp)
        }

        return view
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