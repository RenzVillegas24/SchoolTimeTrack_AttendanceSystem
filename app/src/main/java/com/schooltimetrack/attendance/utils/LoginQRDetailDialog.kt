package com.schooltimetrack.attendance.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.widget.ImageView
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
    private val onConfirm: () -> Unit
) {
    fun show() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_login_qr_detail, null)
        
        val imageView = dialogView.findViewById<ImageView>(R.id.profileImage)

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

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setTitle("Confirm Login")
            .setMessage("""
                User Type: ${jsonData.getString("userType").uppercase(Locale.ROOT)}
                User Name: $name
                Email: ${jsonData.getString("email")}
                Address: $address
                Grade: ${jsonData.getString("grade")}
                Section: ${jsonData.getString("section")}
            """.trimIndent())
            .setPositiveButton("Login") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}