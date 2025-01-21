package com.schooltimetrack.attendance.ui

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.R
import java.io.InputStream

class ImageCropBottomSheet : BottomSheetDialogFragment {

    private lateinit var imageCropView: ImageCropView
    private lateinit var personSelectionView: PersonSelectionView
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var btnCancel: Button
    private var bitmap: Bitmap? = null

    constructor(
        imageUri: InputStream,
        onImageCropped: (Bitmap) -> Unit,
        onCancel: () -> Unit,
        requirePerson: Boolean = false,
        centerCropX: Float = 0f,
        centerCropY: Float = 0f
    ) : this(BitmapFactory.decodeStream(imageUri), onImageCropped, onCancel, requirePerson, centerCropX, centerCropY)

    constructor(
        bitmap: Bitmap,
        onImageCropped: (Bitmap) -> Unit,
        onCancel: () -> Unit,
        requirePerson: Boolean = false,
        centerCropX: Float = 0f,
        centerCropY: Float = 0f
    ) {
        this.bitmap = bitmap
        this.onImageCropped = onImageCropped
        this.onCancel = onCancel
        this.requirePerson = requirePerson
        this.centerCropX = centerCropX
        this.centerCropY = centerCropY
    }

    private var onImageCropped: (Bitmap) -> Unit
    private var onCancel: () -> Unit
    private var requirePerson: Boolean = false
    private var centerCropX: Float = 0f
    private var centerCropY: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_image_crop, container, false)
        imageCropView = view.findViewById(R.id.imageCropView)
        personSelectionView = view.findViewById(R.id.personSelectionView)
        btnSave = view.findViewById(R.id.btnSave)
        btnReset = view.findViewById(R.id.btnReset)
        btnCancel = view.findViewById(R.id.btnCancel)

        personSelectionView.visibility = if (requirePerson) View.VISIBLE else View.GONE

        bitmap?.let {
            imageCropView.setImageBitmap(
                it,
                requirePerson, {
                    onCancel()
                    dismiss()
                },
                centerCropX,
                centerCropY
            )

            if (requirePerson) {
                imageCropView.detectFaces(it) { faces ->
                    personSelectionView.setFaces(faces, it) { face ->
                        imageCropView.centerCropOnFace(face)
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val croppedBitmap = imageCropView.getCroppedBitmap()
            croppedBitmap?.let {
                onImageCropped(it)
                dismiss()
            }
        }

        btnReset.setOnClickListener {
            imageCropView.resetScaling()
        }

        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? ViewGroup
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
                    isDraggable = false
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
}