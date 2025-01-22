package com.schooltimetrack.attendance.qr


import android.content.Context
import android.view.View
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject

class EncryptedScanner(
  context: Context,
  lifecycleOwner: LifecycleOwner,
  previewView: PreviewView,
  private val qrGenerator: EncryptedGenerator,
  private val onDecryptedDataReceived: (JSONObject, Scanner) -> Unit,
  onPermissionDenied: () -> Unit = {},
  hasOverlay: Boolean = true
) : Scanner(
  context = context,
  lifecycleOwner = lifecycleOwner,
  previewView = previewView,
  onQrCodeDetected = { encryptedData, scanner ->
    try {
      val decryptedData = qrGenerator.decrypt(encryptedData)
      onDecryptedDataReceived(JSONObject(decryptedData), scanner)
    } catch (e: Exception) {
      // Handle decryption error
      e.printStackTrace()
    }
  },
  onPermissionDenied = onPermissionDenied,
  hasOverlay = hasOverlay
)
