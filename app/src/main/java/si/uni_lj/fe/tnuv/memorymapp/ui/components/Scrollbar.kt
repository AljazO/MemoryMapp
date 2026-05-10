package si.uni_lj.fe.tnuv.memorymapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 6.dp
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var isScrolledRecently by remember { mutableStateOf(false) }
    
    val isScrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }
    val scrollValueState by remember { derivedStateOf { state.value } }

    LaunchedEffect(scrollValueState, isScrollInProgress) {
        if (isScrollInProgress || scrollValueState > 0) {
            isScrolledRecently = true
            delay(2000)
            if (!isScrollInProgress) {
                isScrolledRecently = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isScrollInProgress || isScrolledRecently) 1f else 0f,
        animationSpec = tween(durationMillis = if (isScrollInProgress) 150 else 500),
        label = "scrollbar_alpha"
    )

    this.drawWithContent {
        drawContent()

        val needScrollbar = state.maxValue > 0
        if (needScrollbar && alpha > 0f) {
            val visibleHeight = size.height
            val totalHeight = state.maxValue + visibleHeight
            val scrollValue = state.value.toFloat()
            
            val scrollbarHeight = (visibleHeight / totalHeight) * visibleHeight
            val scrollbarOffsetY = (scrollValue / totalHeight) * visibleHeight

            drawRect(
                color = Color.White.copy(alpha = alpha * 0.6f),
                topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight)
            )
        }
    }.pointerInput(state.maxValue) {
        if (state.maxValue > 0) {
            detectDragGestures(
                onDragStart = { _ -> },
                onDrag = { change, dragAmount ->
                    if (change.position.x >= size.width - width.toPx() * 3) {
                        val visibleHeight = size.height
                        val totalHeight = state.maxValue + visibleHeight
                        val delta = (dragAmount.y / visibleHeight) * totalHeight
                        scope.launch {
                            state.scrollTo((state.value + delta).toInt().coerceIn(0, state.maxValue))
                        }
                        change.consume()
                    }
                }
            )
        }
    }
}

@Composable
fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 6.dp
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var isScrolledRecently by remember { mutableStateOf(false) }
    
    val isScrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }
    val firstVisibleItemIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { state.firstVisibleItemScrollOffset } }

    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset, isScrollInProgress) {
        isScrolledRecently = true
        delay(2000)
        if (!isScrollInProgress) {
            isScrolledRecently = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isScrollInProgress || isScrolledRecently) 1f else 0f,
        animationSpec = tween(durationMillis = if (isScrollInProgress) 150 else 500),
        label = "scrollbar_alpha"
    )

    this.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isNotEmpty() && alpha > 0f) {
            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = visibleItemsInfo.size
            
            if (visibleItemsCount < totalItemsCount) {
                val scrollbarHeight = (visibleItemsCount.toFloat() / totalItemsCount) * size.height
                val scrollbarOffsetY = (state.firstVisibleItemIndex.toFloat() / totalItemsCount) * size.height + 
                    (state.firstVisibleItemScrollOffset.toFloat() / (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) * (size.height / totalItemsCount))

                drawRect(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight)
                )
            }
        }
    }.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            if (change.position.x >= size.width - width.toPx() * 3) {
                val layoutInfo = state.layoutInfo
                val totalItemsCount = layoutInfo.totalItemsCount
                if (totalItemsCount > 0) {
                    val scrollDeltaItems = (dragAmount.y / size.height) * totalItemsCount
                    scope.launch {
                        state.scrollToItem(
                            (state.firstVisibleItemIndex + scrollDeltaItems.toInt()).coerceIn(0, totalItemsCount - 1),
                            state.firstVisibleItemScrollOffset
                        )
                    }
                    change.consume()
                }
            }
        }
    }
}
