package com.example.smartreceiptsplitter.ui.presentation.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveReceiptEdgesTest {

    private val edges = LiveReceiptEdges(
        corners = DocumentCorners(
            topLeft = NormalizedPoint(0f, 0f),
            topRight = NormalizedPoint(1f, 0f),
            bottomRight = NormalizedPoint(1f, 1f),
            bottomLeft = NormalizedPoint(0.5f, 0.5f),
            isConfident = true
        ),
        frameWidth = 300,
        frameHeight = 400
    )

    @Test
    fun frameWithTheViewsAspectRatioIsOnlyScaled() {
        assertEquals(
            listOf(0f to 0f, 600f to 0f, 600f to 800f, 300f to 400f),
            edges.pointsInView(viewWidth = 600f, viewHeight = 800f)
        )
    }

    @Test
    fun squareViewCropsTopAndBottomLikeFillCenter() {
        // scale 2 covers the 600 px width, the 800 px tall frame overflows by 100 px on each side
        assertEquals(
            listOf(0f to -100f, 600f to -100f, 600f to 700f, 300f to 300f),
            edges.pointsInView(viewWidth = 600f, viewHeight = 600f)
        )
    }

    @Test
    fun tallViewCropsLeftAndRight() {
        // scale 3 covers the 1200 px height, the 900 px wide frame overflows by 150 px on each side
        assertEquals(
            listOf(-150f to 0f, 750f to 0f, 750f to 1200f, 300f to 600f),
            edges.pointsInView(viewWidth = 600f, viewHeight = 1200f)
        )
    }

    @Test
    fun throttleLetsThroughAtMostOneFramePerInterval() {
        val throttle = FrameThrottle(minIntervalNanos = 200)
        assertTrue(throttle.shouldAnalyze(1_000))
        assertFalse(throttle.shouldAnalyze(1_100))
        assertFalse(throttle.shouldAnalyze(1_199))
        assertTrue(throttle.shouldAnalyze(1_200))
        assertFalse(throttle.shouldAnalyze(1_300))
    }

    @Test
    fun throttleRestartsWhenTimestampsGoBack() {
        // the camera was rebound and its clock started over
        val throttle = FrameThrottle(minIntervalNanos = 200)
        assertTrue(throttle.shouldAnalyze(5_000))
        assertTrue(throttle.shouldAnalyze(10))
        assertFalse(throttle.shouldAnalyze(100))
    }
}