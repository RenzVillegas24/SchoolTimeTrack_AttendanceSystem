package com.schooltimetrack.attendance.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.ImageView
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.schooltimetrack.attendance.R
import io.appwrite.services.Storage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class LoginQRDetailDialog(
    private val context: Context,
    private val jsonData: JSONObject,
    private val storage: Storage,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onConfirm: () -> Unit,
    private val onCancel: (() -> Unit?)? = null,
    private val autoConfirm: Boolean = false
) {
    private var isAutoConfirmActive = true

    private fun cancelAutoConfirm() {
        isAutoConfirmActive = false
    }

    fun show() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_login_qr_detail, null)
        
            val imageView = dialogView.findViewById<ImageView>(R.id.profileImage)
            val nameText = dialogView.findViewById<TextView>(R.id.nameText)
            val detailsText = dialogView.findViewById<TextView>(R.id.detailsText)
    
            // Load profile image
            lifecycleScope.launch {
                try {
                    val result = storage.getFilePreview(
                        bucketId = "6774d59e001b225502c9",
                        fileId = jsonData.getString("profileImageId")
                    )
                    val bitmap = BitmapFactory.decodeByteArray(result, 0, result.size)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    
            val address = jsonData.getJSONArray("address").join(", ").replace("\"", "")
            val name = jsonData.getJSONArray("name").join(" ").replace("\"", "")
    
            // Set the name
            nameText.text = name
    
            // Create details message
            val details = SpannableStringBuilder().apply {
                // User Type
                append("User Type: ")
                setSpan(TextAppearanceSpan(context, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append("${jsonData.getString("userType").uppercase(Locale.ROOT)}\n")
    
                // Email
                append("Email: ")
                setSpan(TextAppearanceSpan(context, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium), length - 7, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append("${jsonData.getString("email")}\n")

                // Grade
                append("Grade: ")
                setSpan(TextAppearanceSpan(context, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium), length - 7, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append("${jsonData.getString("grade")}\n")
                
                // Section
                append("Section: ")
                setSpan(TextAppearanceSpan(context, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium), length - 9, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(jsonData.getString("section"))
            }
    
            detailsText.text = details
    
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setTitle("Confirm Login")
                .setPositiveButton("Login") { dialog, _ ->
                    cancelAutoConfirm()
                    dialog.dismiss()
                    onConfirm()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    cancelAutoConfirm()
                    dialog.dismiss()
                    onCancel?.invoke()
                }
                .setOnDismissListener { 
                    cancelAutoConfirm()
                    onCancel?.invoke() 
                }
                .create()

            dialog.show()

            if (autoConfirm) {
                var secondsLeft = 5
                lifecycleScope.launch {
                    while (secondsLeft > 0 && isAutoConfirmActive) {
                        dialog.setTitle("Confirm Login\n(Auto-confirming in ${secondsLeft}s)")
                        kotlinx.coroutines.delay(1000)
                        secondsLeft--
                    }
                    if (isAutoConfirmActive) {
                        dialog.dismiss()
                        onConfirm()
                    }
                }
            }
        }
}