package com.schooltimetrack.attendance.bottomsheet

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.model.StudentInfo
import com.schooltimetrack.attendance.model.TeacherInfo
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import java.io.Serializable

class GeneratedQRBottomSheet(
  private val json: Map<String, Serializable?>
) : BottomSheetDialogFragment() {

  private lateinit var qrGenerator: EncryptedGenerator
  private lateinit var qrImage: ImageView
  private lateinit var btnExit: MaterialButton
  private lateinit var btnExport: MaterialButton

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.bottom_sheet_generated_qr, container, false)
    btnExit = view.findViewById(R.id.btnExit)
    btnExport = view.findViewById(R.id.btnExport)
    qrImage = view.findViewById(R.id.ivImageQR)

    qrGenerator = EncryptedGenerator()

    var qrData = if (json["userType"] as String == "student") {
      qrGenerator.generateForStudent(
        StudentInfo(
          email = json["email"] as String,
          password = json["password"] as String,
          name = json["name"] as Array<String>,
          age = json["age"] as Int,
          address = json["address"] as Array<String>,
          addressId = json["addressId"] as String,
          section = json["section"] as String,
          grade = json["grade"] as String,
          contactNumber = json["contactNumber"] as Array<String>,
          birthday = json["birthday"] as String,
          gender = json["gender"] as String,
          embedding = json["embedding"] as FloatArray,
          profileImageId = json["profileImageId"] as String
        ),
        Color.TRANSPARENT,
        MaterialColors.getColor(
          context,
          com.google.android.material.R.attr.colorPrimary,
          "colorPrimary"
        )
      )
    } else {
      qrGenerator.generateForTeacher(
        TeacherInfo(
          email = json["email"] as String,
          password = json["password"] as String,
          name = json["name"] as Array<String>,
          age = json["age"] as Int,
          address = json["address"] as Array<String>,
          addressId = json["addressId"] as String,
          grade = json["grade"] as String,
          section = json["section"] as String,
          subject = json["subject"] as String,
          contactNumber = json["contactNumber"] as Array<String>,
          birthday = json["birthday"] as String,
          gender = json["gender"] as String,
          embedding = json["embedding"] as FloatArray,
          profileImageId = json["profileImageId"] as String
        ),
        Color.TRANSPARENT,
        MaterialColors.getColor(
          context,
          com.google.android.material.R.attr.colorPrimary,
          "colorPrimary"
        )
      )
    }

    // show QR code on dialog
    val qrBitmap = BitmapFactory.decodeByteArray(qrData, 0, qrData.size)

    qrImage.setImageBitmap(qrBitmap)

    btnExit.setOnClickListener {
      dismiss()
    }

    btnExport.setOnClickListener {
      qrData = if (json["userType"] as String == "student") {
        qrGenerator.generateForStudent(
          StudentInfo(
            email = json["email"] as String,
            password = json["password"] as String,
            name = json["name"] as Array<String>,
            age = json["age"] as Int,
            address = json["address"] as Array<String>,
            addressId = json["addressId"] as String,
            section = json["section"] as String,
            grade = json["grade"] as String,
            contactNumber = json["contactNumber"] as Array<String>,
            birthday = json["birthday"] as String,
            gender = json["gender"] as String,
            embedding = json["embedding"] as FloatArray,
            profileImageId = json["profileImageId"] as String
          ),
          Color.TRANSPARENT,
          Color.BLACK
        )
      } else {
        qrGenerator.generateForTeacher(
          TeacherInfo(
            email = json["email"] as String,
            password = json["password"] as String,
            name = json["name"] as Array<String>,
            age = json["age"] as Int,
            address = json["address"] as Array<String>,
            addressId = json["addressId"] as String,
            grade = json["grade"] as String,
            section = json["section"] as String,
            subject = json["subject"] as String,
            contactNumber = json["contactNumber"] as Array<String>,
            birthday = json["birthday"] as String,
            gender = json["gender"] as String,
            embedding = json["embedding"] as FloatArray,
            profileImageId = json["profileImageId"] as String
          ),
          Color.TRANSPARENT,
          Color.BLACK
        )
      }

      val qrBitmap = BitmapFactory.decodeByteArray(qrData, 0, qrData.size)

      // add text to QR code
      val bitmap =
        Bitmap.createBitmap(qrBitmap.width + 50, qrBitmap.height + 200, Bitmap.Config.ARGB_8888)
          .apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.WHITE)
            val paint = android.graphics.Paint().apply {
              color = Color.BLACK
              textSize = 40f
              textAlign = android.graphics.Paint.Align.CENTER
            }
            val xPos = canvas.width / 2
            val yPos = 80
            canvas.drawText("Scan the QR to be able to login to the",
              xPos.toFloat(), yPos.toFloat(), paint.apply {
                textSize = 25f
              })
            canvas.drawText("SCHOOL TIME TRACK app",
              xPos.toFloat(), yPos + 44f, paint.apply {
                textSize = 37f
                isFakeBoldText = true
              })
            canvas.drawBitmap(qrBitmap, 25f, 170f, null)
          }

      // save the bitmap to gallery
      val saved = android.provider.MediaStore.Images.Media.insertImage(
        context?.contentResolver,
        bitmap,
        "QR Code",
        "QR Code for School Time Track"
      )
      if (saved != null) {
        android.widget.Toast.makeText(
          context,
          "QR code saved to gallery",
          android.widget.Toast.LENGTH_SHORT
        ).show()
      } else {
        android.widget.Toast.makeText(
          context,
          "Failed to save QR code",
          android.widget.Toast.LENGTH_SHORT
        ).show()
      }
    }

    return view
  }


  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
    dialog.setOnShowListener {
      val bottomSheet =
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? ViewGroup
      bottomSheet?.let {
        // set the height as maximum height minus the height of the status bar
        val displayMetrics = resources.displayMetrics
        val height = displayMetrics.heightPixels

        // get the status bar height
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        it.layoutParams = it.layoutParams.apply {
          this.height = height - statusBarHeight
        }

        BottomSheetBehavior.from(it).apply {
          state = BottomSheetBehavior.STATE_EXPANDED
          skipCollapsed = true
          isDraggable = true
          peekHeight = height - statusBarHeight
        }
      }
    }

    // Keep the status bar color unchanged
    dialog.window?.apply {
      addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      statusBarColor = Color.TRANSPARENT
      navigationBarColor = Color.TRANSPARENT
    }

    return dialog
  }

  override fun onDestroy() {
    super.onDestroy()
  }

}