package com.schooltimetrack.attendance.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.face.Face
import com.schooltimetrack.attendance.R

class PersonSelectionAdapter(
    private val faces: List<Any>, // Changed to Any to handle both Face and SkeletonFace
    private val bitmap: Bitmap,
    private val onFaceSelected: (Face) -> Unit
) : RecyclerView.Adapter<PersonSelectionAdapter.FaceViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION
    val realItemCount = faces.size
    private val duplicateFactor = 1000 // Large enough for smooth scrolling but not too large
    private val initialPosition = (duplicateFactor / 2) * realItemCount // Start from middle

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_face, parent, false)
        return FaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaceViewHolder, position: Int) {
        val actualPosition = position % realItemCount
        val face = faces[actualPosition]
        holder.bind(face)
    }

    override fun getItemCount() = if (realItemCount == 0) 0 else realItemCount * duplicateFactor

    fun getInitialPosition() = initialPosition

    fun selectFace(position: Int) {
        val normalizedPosition = position % realItemCount
        if (normalizedPosition != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = normalizedPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            if (faces[normalizedPosition] is Face) {
                onFaceSelected(faces[normalizedPosition] as Face)
            }
        }
    }

    inner class FaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivFace)

        fun bind(face: Any) {
            if (face is Face) {
                val faceBounds = face.boundingBox
                val padding = (faceBounds.width() * 0.1).toInt()
                val adjustedBounds = Rect(
                    faceBounds.left - padding,
                    faceBounds.top - padding,
                    faceBounds.right + padding,
                    faceBounds.bottom + padding
                )

                val faceBitmap = Bitmap.createBitmap(
                    adjustedBounds.width(),
                    adjustedBounds.height(),
                    Bitmap.Config.ARGB_8888
                )

                Canvas(faceBitmap).apply {
                    drawBitmap(bitmap, adjustedBounds, Rect(0, 0, faceBitmap.width, faceBitmap.height), null)
                }

                imageView.setImageBitmap(faceBitmap)
            } else if (face is SkeletonFace) {
                imageView.setImageResource(R.drawable.skeleton_placeholder)
            }
        }
    }
}
