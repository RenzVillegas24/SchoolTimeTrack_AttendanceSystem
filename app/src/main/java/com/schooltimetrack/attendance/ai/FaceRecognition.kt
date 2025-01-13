package com.schooltimetrack.attendance.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaceRecognition(
    context: Context,
    private val faceNet: FaceNet,
    private val faceSpoofDetector: FaceSpoofDetector
) {

    private val client = Client(context)
        .setEndpoint("https://cloud.appwrite.io/v1")
        .setProject("6773c26a001612edc5fb")
    private val database = Databases(client)

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .enableTracking()
        .setMinFaceSize(0.15f)
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .build()
    private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)



    // Detect face + run FaceSpoof check + embed
    suspend fun detectFace(bitmap: Bitmap): Result<Pair<Bitmap, FloatArray>> =
        withContext(Dispatchers.IO) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(faceDetector.process(inputImage))
            if (faces.size != 1)
                return@withContext Result.failure(
                    Throwable("Require exactly one face. Found: ${faces.size}")
                )
            val cropped = cropFace(bitmap, faces[0])

            val spoofResult = faceSpoofDetector.detectSpoof(bitmap, faces[0].boundingBox)
            if (spoofResult.isSpoof)
                return@withContext Result.failure(
                    Throwable("Spoof detected!")
                )
            val embedding = faceNet.getFaceEmbedding(cropped)
            Result.success(
                Pair(cropped, embedding)
            )
        }

//    // Store embedding for a person
//    suspend fun storeEmbedding(personId: String, personName: String, embedding: FloatArray): Result<Boolean> {
//        return runCatching {
//            val docData = mapOf(
//                "personName" to personName,
//                "embedding" to embedding
//            )
//            database.createDocument(
//                databaseId = "6774d5c500013f347412",
//                collectionId = "677c2df70002803b4fa8",
//                documentId = personId,
//                docData)
//            true
//        }
//    }

    suspend fun getUsers(selectedUsers: List<String>? = null) = run {
        selectedUsers?.mapNotNull { userId ->
            runCatching {
                database.getDocument(
                    databaseId = "6774d5c500013f347412",
                    collectionId = "677c2df70002803b4fa8",
                    documentId = userId
                )
            }.getOrNull()
        } ?: runCatching {
            database.listDocuments(
                databaseId = "6774d5c500013f347412",
                collectionId = "677c2df70002803b4fa8"
            )
        }.getOrNull()?.documents ?: return emptyList<Document<Map<String, Any>>>()
    }

    // Match embedding, optionally restricting to a certain target user ids
    suspend fun matchEmbedding(embedding: FloatArray, targetUserIds: String, docs: List<Document<Map<String, Any>>>): List<Pair<Array<String>, Float>> {
        val results = mutableListOf<Pair<Array<String>, Float>>()
        for (doc in docs) {
            if (doc.data["name"] != targetUserIds) continue
            val name = doc.data["name"] as? Array<String> ?: continue
            val storedEmb = doc.data["embedding"] as? FloatArray ?: continue
            val score = cosineSimilarity(embedding, storedEmb)
            results.add(Pair(name, score))
        }
        return results.sortedByDescending { it.second }
   }

    suspend fun matchEmbedding(embedding: FloatArray, targetEmbeddings: List<FloatArray>): List<Pair<String, Float>> {
        val results = mutableListOf<Pair<String, Float>>()
        for (storedEmb in targetEmbeddings) {
            val score = cosineSimilarity(embedding, storedEmb)
            results.add(Pair("", score))
        }
        return results.sortedByDescending { it.second }
    }

    private fun cropFace(orig: Bitmap, face: Face): Bitmap {
        val box = face.boundingBox
        val width = box.width().coerceAtMost(orig.width - box.left)
        val height = box.height().coerceAtMost(orig.height - box.top)
        return Bitmap.createBitmap(orig, box.left, box.top, width, height)
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var mag1 = 0f
        var mag2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            mag1 += v1[i] * v1[i]
            mag2 += v2[i] * v2[i]
        }
        return dot / (kotlin.math.sqrt(mag1) * kotlin.math.sqrt(mag2))
    }
}