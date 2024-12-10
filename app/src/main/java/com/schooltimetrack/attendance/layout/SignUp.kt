package com.schooltimetrack.attendance.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.schooltimetrack.attendance.R

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        val etFirstName = view.findViewById<EditText>(R.id.etFirstName)
        val etMiddleName = view.findViewById<EditText>(R.id.etMiddleName)
        val etLastName = view.findViewById<EditText>(R.id.etLastName)
        val etSuffixName = view.findViewById<EditText>(R.id.etSuffixName)
        val etGrade = view.findViewById<EditText>(R.id.etGrade)
        val etSection = view.findViewById<EditText>(R.id.etSection)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val etAddress = view.findViewById<EditText>(R.id.etAddress)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)

        btnSignUp.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val middleName = etMiddleName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val suffixName = etSuffixName.text.toString().trim()
            val grade = etGrade.text.toString().trim()
            val section = etSection.text.toString().trim()
            val age = etAge.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && grade.isNotEmpty() && section.isNotEmpty() && age.isNotEmpty() && address.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid
                            val database = FirebaseDatabase.getInstance().reference

                            val userMap = hashMapOf(
                                "firstName" to firstName,
                                "middleName" to middleName,
                                "lastName" to lastName,
                                "suffixName" to suffixName,
                                "grade" to grade,
                                "section" to section,
                                "age" to age,
                                "address" to address,
                                "email" to email
                            )

                            userId?.let {
                                database.child("users").child(it).setValue(userMap)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Toast.makeText(context, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Database Error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_login)
        }

        return view
    }
}