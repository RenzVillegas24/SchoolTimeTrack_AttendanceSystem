package com.schooltimetrack.attendance.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.schooltimetrack.attendance.R

class LoadingDialog(private val context: Context) {
    private var dialog: AlertDialog? = null
    private var dialogView: View? = null
    
    fun show(message: String = "Loading...") {
        if (dialog == null) {
            dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
            dialogView?.findViewById<MaterialTextView>(R.id.tvMessage)?.text = message
            
            dialog = MaterialAlertDialogBuilder(context, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create()
        } else {
            dialogView?.findViewById<MaterialTextView>(R.id.tvMessage)?.text = message
        }
        dialog?.show()
    }
    
    fun hide() {
        dialog?.dismiss()
    }
}