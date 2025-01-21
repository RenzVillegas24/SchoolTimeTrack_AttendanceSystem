package com.schooltimetrack.attendance.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.Toast
import androidx.camera.view.PreviewView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import com.schooltimetrack.attendance.qr.EncryptedScanner
import com.schooltimetrack.attendance.qr.Scanner
import org.json.JSONObject

class LoginQRBottomSheet(
    private val onDecryptedDataReceived: ((json: JSONObject, encryptedScanner: Scanner, bottomSheet: LoginQRBottomSheet) -> Unit)? = null
) : BottomSheetDialogFragment() {


    private lateinit var encryptedScanner: EncryptedScanner
    private lateinit var previewView: PreviewView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_login_qr, container, false)
        
        previewView = view.findViewById(R.id.viewFinder)
        val btnSwitchCamera = view.findViewById<Button>(R.id.btnSwitchCamera)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val qrGenerator = EncryptedGenerator()

        encryptedScanner = EncryptedScanner(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = previewView,
            qrGenerator = qrGenerator,
            onDecryptedDataReceived = { json, scanner ->
                onDecryptedDataReceived?.invoke(json, scanner, this)
            },
            onPermissionDenied = {
                Toast.makeText(view.context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        )

        btnCancel.setOnClickListener { dismiss() }

        btnSwitchCamera.setOnClickListener {
            btnSwitchCamera.animate()
            .rotationBy(180f)
            .setDuration(700)
            .setInterpolator(PathInterpolator(0.3f, 1.5f, 0.25f, 1f))
            .start()
        
            encryptedScanner.switchCamera()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as? ViewGroup
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
        encryptedScanner.release()
    }



}