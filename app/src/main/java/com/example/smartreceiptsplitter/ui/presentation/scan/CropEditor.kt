package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint

/*
* The review photo with the receipt's outline and a handle on each corner
* A drag that starts near a handle moves that corner, the handle its distance to the finger instead of jumping
* under it and the corner stays inside the photo
* The outline turns red while the corners can't be used as a crop
* */

@Composable
internal fun CropEditor(
    preview: Bitmap,
    corners: DocumentCorners,
    onCornerMoved: (DocumentCorner, NormalizedPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val image = remember(preview) { preview.asImageBitmap() }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val currentCorners by rememberUpdatedState(corners)
    val currentOnCornerMoved by rememberUpdatedState(onCornerMoved)

    val density = LocalDensity.current
    val inset = with(density) { HANDLE_INSET.toPx() }
    val touchRadius = with(density) { TOUCH_RADIUS.toPx() }
    val lineColor = if (corners.isUsableCrop()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Box(modifier = modifier.onSizeChanged { viewSize = it }) {
        Image(
            bitmap = image,
            contentDescription = "Receipt photo, drag the corners to the receipt's edges",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(HANDLE_INSET)
        )
        if (viewSize.width > 0 && viewSize.height > 0) {
            val mapping = FitMapping(preview.width, preview.height, viewSize.width.toFloat(), viewSize.height.toFloat(), inset)
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(mapping) {
                    var dragged: DocumentCorner? = null
                    var grabOffset = Offset.Zero
                    detectDragGestures(
                        onDragStart = { start ->
                            dragged = currentCorners.cornerAt(start.x, start.y, mapping, touchRadius)
                            dragged?.let { corner ->
                                val (handleX, handleY) = mapping.toView(currentCorners[corner])
                                grabOffset = Offset(handleX - start.x, handleY - start.y)
                            }
                        },
                        onDrag = { change, _ ->
                            dragged?.let { corner ->
                                change.consume()
                                val target = change.position + grabOffset
                                currentOnCornerMoved(corner, mapping.toImage(target.x, target.y))
                            }
                        },
                        onDragEnd = { dragged = null },
                        onDragCancel = { dragged = null }
                    )
                }
            ) {
                val points = DocumentCorner.entries.map { corner -> mapping.toView(corners[corner]) }
                val outline = Path().apply {
                    points.forEachIndexed { index, (x,y) -> if (index == 0) moveTo(x,y) else lineTo(x,y) }
                    close()
                }
                drawPath(outline, color = lineColor.copy(alpha = 0.15f))
                drawPath(outline, color = lineColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                points.forEach { (x,y) ->
                    drawCircle(Color.White, radius = HANDLE_RADIUS.toPx(), center = Offset(x, y))
                    drawCircle(lineColor, radius = HANDLE_RADIUS.toPx(), center = Offset(x, y), style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
    }
}

private val HANDLE_RADIUS = 12.dp
private val HANDLE_INSET = 24.dp // room around photo
private val TOUCH_RADIUS = 40.dp // a drag starting this close to a handle moves it