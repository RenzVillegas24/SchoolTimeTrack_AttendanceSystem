package com.schooltimetrack.attendance.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearSnapHelper
import com.google.android.material.button.MaterialButton
import com.schooltimetrack.attendance.R
import com.google.mlkit.vision.face.Face
import com.schooltimetrack.attendance.adapter.PersonSelectionAdapter

class PersonSelectionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val btnPrevious: MaterialButton
    private val btnNext: MaterialButton
    private val rvFaces: RecyclerView
    private val selectionIndicator: View
    private lateinit var adapter: PersonSelectionAdapter
    private val snapHelper = LinearSnapHelper()
    private var isScrolling = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_person_selection, this, true)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        rvFaces = findViewById(R.id.rvFaces)
        selectionIndicator = findViewById(R.id.selectionIndicator)

        setupRecyclerView()
        setupScrollButtons()
        setupScrollListener()

        // Initially set skeleton faces and disable buttons
        setSkeletonFaces()
        btnPrevious.isEnabled = false
        btnNext.isEnabled = false

        alpha = 0f

    }

    private fun setupRecyclerView() {
        val layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
            override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
                val smoothScroller = object : LinearSmoothScroller(context) {
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                        return 150f / displayMetrics.densityDpi // Slower scroll
                    }

                    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
                    }
                }
                smoothScroller.targetPosition = position
                startSmoothScroll(smoothScroller)
            }
        }
        rvFaces.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(rvFaces)
    }

    private fun smoothScrollToPosition(position: Int) {
        if (isScrolling) return

        val currentPosition = (rvFaces.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        if (currentPosition == RecyclerView.NO_POSITION) return

        val targetPosition = when {
            position < 0 -> currentPosition + adapter.realItemCount - 1
            position >= adapter.itemCount -> currentPosition - adapter.realItemCount + 1
            else -> position
        }

        rvFaces.smoothScrollToPosition(targetPosition)
    }

    private fun setupScrollButtons() {
        btnPrevious.setOnClickListener {
            val position = (rvFaces.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            if (position != RecyclerView.NO_POSITION) {
                smoothScrollToPosition(position - 1)
            }
        }

        btnNext.setOnClickListener {
            val position = (rvFaces.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            if (position != RecyclerView.NO_POSITION) {
                smoothScrollToPosition(position + 1)
            }
        }
    }

    private fun setupScrollListener() {
        rvFaces.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        isScrolling = false
                        snapHelper.findSnapView(rvFaces.layoutManager)?.let { snapView ->
                            val position = (rvFaces.layoutManager as LinearLayoutManager).getPosition(snapView)
                            val actualPosition = position % adapter.realItemCount
                            adapter.selectFace(actualPosition)

                            if (position >= adapter.itemCount - 2) {
                                (rvFaces.layoutManager as LinearLayoutManager).scrollToPosition(position - adapter.realItemCount)
                            } else if (position <= 1) {
                                (rvFaces.layoutManager as LinearLayoutManager).scrollToPosition(position + adapter.realItemCount)
                            }
                        }
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING -> {
                        isScrolling = true
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateItemsTransformation()
            }
        })
    }

    private fun updateItemsTransformation() {
        val layoutManager = rvFaces.layoutManager as LinearLayoutManager
        val centerX = rvFaces.width / 2f

        for (i in 0 until rvFaces.childCount) {
            val child = rvFaces.getChildAt(i) ?: continue
            val childCenter = (child.left + child.right) / 2f
            val distanceFromCenter = Math.abs(centerX - childCenter)
            val maxDistance = rvFaces.width / 2f

            val scale = lerp(0.5f, 1.0f, (1f - (distanceFromCenter / maxDistance)).coerceIn(0f, 1f))
            val alpha = lerp(0.3f, 1.0f, (1f - (distanceFromCenter / maxDistance)).coerceIn(0f, 1f))

            child.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(0)
                .start()
        }
    }

    private fun generateSkeletonFaces(count: Int): List<SkeletonFace> {
        return List(count) { SkeletonFace(it) }
    }

    private fun lerp(start: Float, end: Float, fraction: Float) = start + (end - start) * fraction

    private fun setSkeletonFaces() {
        val skeletonFaces = generateSkeletonFaces(5) // Adjust the count as needed
        adapter = PersonSelectionAdapter(skeletonFaces, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) { }
        rvFaces.adapter = adapter
        rvFaces.isEnabled = false

        rvFaces.doOnLayout {
            centerFirstItem()
            updateItemsTransformation()
        }
    }

    fun setFaces(faces: List<Face>, bitmap: Bitmap, onFaceSelected: (Face) -> Unit) {
        adapter = PersonSelectionAdapter(faces, bitmap, onFaceSelected)
        rvFaces.adapter = adapter
        rvFaces.isEnabled = true
        btnPrevious.isEnabled = true
        btnNext.isEnabled = true


        rvFaces.doOnLayout {
            animate().alpha(1f).setDuration(300).start()
            centerFirstItem()
            updateItemsTransformation()
        }
    }

    private fun centerFirstItem() {
        val initialPosition = adapter.getInitialPosition()
        rvFaces.post {
            (rvFaces.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                initialPosition,
                (rvFaces.width - (rvFaces.layoutManager?.getChildAt(0)?.width ?: 0)) / 2
            )

            val position = (rvFaces.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            smoothScrollToPosition(position + 1)
        }
    }
}