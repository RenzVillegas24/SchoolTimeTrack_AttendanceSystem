package com.schooltimetrack.attendance.layout

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.android.material.transition.MaterialSharedAxis
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.ai.FaceRecognition
import com.schooltimetrack.attendance.ui.FaceVerificationBottomSheet
import com.schooltimetrack.attendance.ui.GeneratedQRBottomSheet
import com.schooltimetrack.attendance.ui.ImageCropBottomSheet
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Welcome : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

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
            findNavController().navigate(R.id.action_welcome_to_login)
        }

        btnLoginQR.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_loginQR)
        }

        btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_signUp)
        }


//        GeneratedQRBottomSheet(
//            mapOf(
//                "userType" to "student",
//                "email" to "temporaryacct4now@gmail.com",
//                "password" to "password",
//                "name" to arrayOf("John", "Doe"),
//                "age" to "18",
//                "address" to arrayOf("1234", "Main St", "City", "State", "Country", "Zip"),
//                "addressId" to ID.unique(),
//                "section" to "A",
//                "grade" to "12",
//                "contactNumber" to "09123456789",
//                "embedding" to arrayListOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
//            )
//        ).show(parentFragmentManager, "GeneratedQRBottomSheet")


//        startActivityForResult(
//            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                type = "image/*"
//            },
//            1)


        return view
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream = context?.contentResolver?.openInputStream(uri)
                inputStream?.let {
                    Log.i("ImageCropBottomSheet", "Image URI: $uri")
                    ImageCropBottomSheet(it,
                        { croppedBitmap ->
                            Log.i("ImageCropBottomSheet", "Cropped Bitmap: $croppedBitmap")
                            FaceVerificationBottomSheet(croppedBitmap)
                                { bottomSheet, results ->
                                    bottomSheet.dismiss()
                                }.show(childFragmentManager, "FaceVerificationBottomSheet")


                        },
                        {}, requirePerson = true, centerCropY = -200f)
                }?.show(parentFragmentManager, "ImageCropBottomSheet")

            }
        }
    }


}