package com.example.smartreceiptsplitter.ui.presentation.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewCropGeometryTest {

    // portrait photo 300 x 400 in a 1000 x 1000 view: scaled 2.5 to 750 x 1000, bands of 125 px left and right
    private val fit = FitMapping(imageWidth = 300, imageHeight = 400, viewWidth = 1000f, viewHeight = 1000f)

    @Test
    fun cornersAreDrawnOnThePhotoNotOnTheBands() {
        assertEquals(125f to 0f, fit.toView(NormalizedPoint(0f, 0f)))
        assertEquals(875f to 1000f, fit.toView(NormalizedPoint(1f, 1f)))
        assertEquals(500f to 500f, fit.toView(NormalizedPoint(0.5f, 0.5f)))
    }

    @Test
    fun fingerPositionIsConvertedBackToThePhoto() {
        assertEquals(NormalizedPoint(0.5f, 0.25f), fit.toImage(500f, 250f))
        // a finger on the band or outside the view stops at the photo's edge
        assertEquals(NormalizedPoint(0f, 0f), fit.toImage(40f, -30f))
        assertEquals(NormalizedPoint(1f, 1f), fit.toImage(990f, 1200f))
    }

    @Test
    fun insetLeavesRoomAroundThePhoto() {
        // 1000 x 1000 view with 100 px inset: the photo fits 800 x 800 -> 600 x 800, from (200, 100)
        val inset = FitMapping(300, 400, 1000f, 1000f, inset = 100f)
        assertEquals(200f to 100f, inset.toView(NormalizedPoint(0f, 0f)))
        assertEquals(800f to 900f, inset.toView(NormalizedPoint(1f, 1f)))
        assertEquals(NormalizedPoint(0.5f, 0.5f), inset.toImage(500f, 500f))
    }

    @Test
    fun touchNearAHandlePicksTheClosestCorner() {
        val corners = DocumentCorners.fullFrame()
        assertEquals(DocumentCorner.TOP_LEFT, corners.cornerAt(140f, 20f, fit, maxDistance = 50f))
        assertEquals(DocumentCorner.BOTTOM_RIGHT, corners.cornerAt(860f, 980f, fit, maxDistance = 50f))
        // the middle of the photo is far from every handle
        assertNull(corners.cornerAt(500f, 500f, fit, maxDistance = 50f))
    }
}