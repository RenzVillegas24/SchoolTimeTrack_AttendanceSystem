package com.schooltimetrack.attendance.ui

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.ai.FaceRecognition
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class FaceVerificationBottomSheet(
    private val profileImage: Any,
    private val onVerificationSuccess: (FaceVerificationBottomSheet, List<Triple<String, List<String>, Float>>) -> Unit,
) : BottomSheetDialogFragment() {

    private lateinit var faceVerificationView: FaceVerificationView
    private lateinit var profileImageView: ImageView
    private lateinit var profileCardView: MaterialCardView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnPause: MaterialButton
    private lateinit var btnResume: MaterialButton


    private lateinit var fade: ObjectAnimator

    // create an gradient radius animation from face verification view
    private lateinit var gradientRadiusAnimator: ObjectAnimator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_face_verification, container, false)
        faceVerificationView = view.findViewById(R.id.faceVerificationView)
        profileImageView = view.findViewById(R.id.profileImageView)
        profileCardView = view.findViewById(R.id.profileCardView)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnPause = view.findViewById(R.id.btnPause)
        btnResume = view.findViewById(R.id.btnResume)

        val fadeKeyframe1 = Keyframe.ofFloat(0f, 0f)
        val fadeKeyframe2 = Keyframe.ofFloat(0.5f, 1f)
        val fadeKeyframe3 = Keyframe.ofFloat(1f, 0f)
        val fadeProperty = PropertyValuesHolder.ofKeyframe("alpha", fadeKeyframe1, fadeKeyframe2, fadeKeyframe3)
        fade = ObjectAnimator.ofPropertyValuesHolder(profileCardView, fadeProperty).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
        }

        gradientRadiusAnimator = ObjectAnimator.ofFloat(faceVerificationView, "gradientRadius", 0.2f)

        // get the type of profile image
        Log.d("FaceVerificationBottomSheet", "Profile image type: ${profileImage::class.simpleName}")
        Log.d("FaceVerificationBottomSheet", "Profile: $profileImage")

        if (profileImage is Bitmap) {
            faceVerificationView.initializeCamera(profileImage, "", emptyList())
        } else if (profileImage is Triple<*, *, *>) {
            if (profileImage.first is Bitmap)
                faceVerificationView.initializeCamera(
                    profileImage.first as Bitmap,
                    profileImage.second as String,
                    profileImage.third as List<String>)
            else if (profileImage.first is FloatArray)
                faceVerificationView.initializeCamera(
                    profileImage.first as FloatArray,
                    profileImage.second as String,
                    profileImage.third as List<String>)
        } else if (profileImage is List<*>) {
            if (profileImage.isNotEmpty()) {
                val profile = profileImage[0]
                if (profile is Bitmap) {
                    faceVerificationView.initializeCamera(profile, "", emptyList())
                } else if (profile is Triple<*, *, *>) {
                    if (profile.second is String && profile.third is List<*>) {
                        if (profile.first is Bitmap)
                            faceVerificationView.initializeCamera(
                                profile.first as Bitmap,
                                profile.second as String,
                                profile.third as List<String>
                            )
                        else if (profile.first is FloatArray)
                            faceVerificationView.initializeCamera(
                                profile.first as FloatArray,
                                profile.second as String,
                                profile.third as List<String>
                            )
                    }
                }
            }
        }

        faceVerificationView.setOnFaceConfirmedListener { results ->
            faceVerificationView.freezeCameraPreview()
            onVerificationSuccess(this@FaceVerificationBottomSheet, results)
        }

        faceVerificationView.setOnDetectListener {
            gradientRadiusAnimator.apply {
                setFloatValues(faceVerificationView.gradientRadius, 0.2f)
                duration = 3500
                cancel()
                start()
            }
        }


        faceVerificationView.setOnFaceNotDetectedListener {
            // revert the gradient radius animation
            gradientRadiusAnimator.apply {
                setFloatValues(0.6f, faceVerificationView.gradientRadius)
                duration = 750
                cancel()
                reverse()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnPause.setOnClickListener {
            faceVerificationView.pauseDetection()
        }

        btnResume.setOnClickListener {
            faceVerificationView.resumeDetection()
        }

        Log.d("FaceVerificationBottomSheet", "View created and camera should start")

        return view
    }




    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? ViewGroup
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.isDraggable = false
            }
        }
        // Keep the status bar color unchanged
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        dialog.window?.statusBarColor = Color.TRANSPARENT
        dialog.window?.navigationBarColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, "colorSurface")

        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        faceVerificationView.cameraExecutor?.shutdown()
    }

}