package com.schooltimetrack.attendance.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import com.google.android.material.color.MaterialColors
import com.schooltimetrack.attendance.model.StudentInfo
import com.schooltimetrack.attendance.model.TeacherInfo


class EncryptedGenerator {
    companion object {
        // Must be 32 bytes for AES-256
        private val ENCRYPTION_KEY = byteArrayOf(117, -26, 112, 111, -127, 45, -27, 121, 104, -82, -50, 96, -127, 100, -38, 62, -17, 3, 60, -18, -54, -99, 102, -98, 114, -86, 6, 11, -18, -122, -25, 17)
        // Must be 16 bytes
        private val INITIALIZATION_VECTOR = byteArrayOf(85, 59, -110, -48, 111, -69, -76, -109, -119, -7, -63, -9, 94, 40, -3, -53)
        // QR code dimension in pixels
        private const val QR_DIMENSION = 512
    }
    private val secretKey: SecretKey = SecretKeySpec(ENCRYPTION_KEY, "AES")
    private val iv = IvParameterSpec(INITIALIZATION_VECTOR)

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(encryptedData: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
        return String(decryptedBytes)
    }

    fun generateForStudent(info: StudentInfo, colorBlack: Int = Color.BLACK, colorWhite: Int = Color.WHITE): ByteArray {
        val jsonData = JSONObject().apply {
            put("userType", info.userType)
            put("email", info.email)
            put("password", info.password)
            put("name", info.name)
            put("age", info.age)
            put("address", info.address)
            put("addressId", info.addressId)
            put("section", info.section)
            put("grade", info.grade)
            put("contactNumber", info.contactNumber)
            put("embedding", info.embedding)
        }

        return generateCode(encrypt(jsonData.toString()), colorBlack, colorWhite)
    }

    fun generateForTeacher(info: TeacherInfo, colorBlack: Int = Color.BLACK, colorWhite: Int = Color.WHITE): ByteArray {
        val jsonData = JSONObject().apply {
            put("userType", info.userType)
            put("email", info.email)
            put("password", info.password)
            put("name", info.name)
            put("age", info.age)
            put("address", info.address)
            put("addressId", info.addressId)
            put("section", info.section)
            put("subject", info.subject)
            put("contactNumber", info.contactNumber)
            put("embedding", info.embedding)
        }

        return generateCode(encrypt(jsonData.toString()), colorBlack, colorWhite)
    }

    private fun generateCode(data: String, colorBlack: Int = Color.BLACK, colorWhite: Int = Color.WHITE): ByteArray {
        // Initialize QR encoder with dimension
        val qrgEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, QR_DIMENSION)

        // Set QR code color to black
        qrgEncoder.colorBlack = colorBlack
        qrgEncoder.colorWhite = colorWhite

        // Generate QR code as bitmap
        val bitmap = qrgEncoder.getBitmap()

        // Convert bitmap to ByteArray
        return bitmap.toByteArray()
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        // Compress with high quality PNG for better scanning
        this.compress(CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun saveToFile(qrData: ByteArray, filePath: String) {
        FileOutputStream(File(filePath)).use { fos ->
            fos.write(qrData)
            fos.flush()
        }
    }
}
