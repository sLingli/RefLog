package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MatchHistoryUiModel(
    val id: Long,
    val date: String,
    val duration: String,
    val stoppage: String,
    val events: String
)

class HistoryActivity : ComponentActivity() {
    private lateinit var recordManager: MatchRecordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recordManager = MatchRecordManager(this)

        val rawRecords = recordManager.getAllRecords()

        val uiRecords = rawRecords.map { record ->
            MatchHistoryUiModel(
                id = record.id,
                date = record.date,
                duration = getString(R.string.label_duration_fmt),
                stoppage = "补时: 上+${record.firstHalfStoppage} / 下+${record.secondHalfStoppage}",
                events = "进球:${record.goalCount}  红牌:${record.redCount}  换人:${record.substitutionCount}"
            )
        }

        // Debug log: number of records read from storage
        Log.i("HistoryActivity", "Loaded ${uiRecords.size} history records from MatchRecordManager")

        // Ensure the activity window background is solid black so swipe-to-dismiss (reveal) won't show a white background
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        setContent {
            // Root Box fills the whole window and uses a black background to avoid any white edges during gestures
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                MaterialTheme {
                    HistoryScreen(
                        initialRecords = uiRecords,
                        fetchRecords = {
                            recordManager.getAllRecords().map { record ->
                                MatchHistoryUiModel(
                                    id = record.id,
                                    date = record.date,
                                    duration = "${record.halfTimeMinutes}${getString(R.string.unit_min_half)}",
                                    stoppage = "补时: 上+${record.firstHalfStoppage} / 下+${record.secondHalfStoppage}",
                                    events = "进球:${record.goalCount}  红牌:${record.redCount}  换人:${record.substitutionCount}"
                                )
                            }
                        },
                        onClose = { finish() },
                        onClearAll = {
                            // 真实清空数据库
                            recordManager.clearAllRecords()
                            Toast.makeText(this@HistoryActivity, getString(R.string.msg_history_cleared), Toast.LENGTH_SHORT).show()
                        },
                        onDeleteOne = { uiModel ->
                            // 真实删除单条
                            recordManager.deleteRecord(uiModel.id)
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    initialRecords: List<MatchHistoryUiModel> = emptyList(),
    fetchRecords: (() -> List<MatchHistoryUiModel>)? = null,
    onClose: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteOne: (MatchHistoryUiModel) -> Unit
) {
    val records = remember { mutableStateListOf<MatchHistoryUiModel>().apply { addAll(initialRecords) } }
    // If a fetchRecords lambda is provided, load latest records on composition start
    LaunchedEffect(fetchRecords) {
        fetchRecords?.let { loader ->
            val latest = loader()
            records.clear()
            records.addAll(latest)
        }
    }
    val listState: ScalingLazyListState = rememberScalingLazyListState()

    var showClearAllDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape)) {
        Scaffold(
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                scalingParams = ScalingLazyColumnDefaults.scalingParams()
            ) {
                item {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_history),
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp).padding(bottom = 8.dp)
                    )
                }

                if (records.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.msg_no_events),
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    items(records, key = { it.id }) { record ->
                        HistoryItemCard(
                            record = record,
                            onLongClick = { },
                            onDelete = {
                                onDeleteOne(record)
                                records.remove(record)
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { if (records.isNotEmpty()) showClearAllDialog = true },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30)),
                            modifier = Modifier.size(50.dp).padding(end = 8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.outline_delete_24), null)
                        }
                    }

                }
            }
        }
    }

    if (showClearAllDialog) {
        CircularAlert(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.msg_confirm_clear_all), textAlign = TextAlign.Center, color = Color.White) },
            confirmText = { Icon(painterResource(R.drawable.outline_delete_24), contentDescription = null, tint = Color.White) },
            onConfirm = {
                onClearAll()
                records.clear()
                showClearAllDialog = false
            },
            dismissText = { Icon(painterResource(R.drawable.outline_close_24), contentDescription = null, tint = Color.White) },
            onDismissAction = { showClearAllDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeReveal(
    modifier: Modifier = Modifier,
    revealWidth: Dp = 68.dp,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealPx = with(density) { revealWidth.toPx() }
    val screenWidthPx = with(density) { 300.dp.toPx() } // Sufficient to slide off screen
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // helper to animate to target with spring (bouncy)
    suspend fun settleTo(target: Float) {
        offset.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Background: right delete button only
        Box(
            modifier = Modifier
                .matchParentSize()
        ) {
            val cur = offset.value
            val fractionRight = ((-cur) / revealPx).coerceIn(0f, 1f)

            // Right red button container
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(revealWidth)
                    .fillMaxHeight()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                val alpha = fractionRight
                if (alpha > 0f) {
                    val translateRight = (1f - alpha) * revealPx
                    Button(
                        onClick = {
                            scope.launch {
                                // Slide out to the left (negative offset)
                                offset.animateTo(-screenWidthPx, animationSpec = spring())
                                onDelete()
                                // Reset offset for reused view item if necessary,
                                // but since it's deleted, it usually detaches.
                                // If list state recycles this view, we want to be safe,
                                // but setting it back immediately causes flicker if the removal isn't instant.
                                // For now, we assume the item is removed from composition.
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30)),
                        modifier = Modifier
                            .size(revealWidth)
                            .graphicsLayer {
                                translationX = translateRight
                                this.alpha = alpha
                            }
                    ) {
                        Icon(painterResource(id = R.drawable.outline_delete_24), contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            scope.launch {
                                val v = offset.value
                                when {
                                    v < -revealPx / 2f -> settleTo(-revealPx)
                                    else -> settleTo(0f)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { settleTo(0f) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val target = (offset.value + dragAmount).coerceIn(-revealPx, 0f)
                                offset.snapTo(target)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    record: MatchHistoryUiModel,
    onLongClick: () -> Unit,
    onDelete: ((MatchHistoryUiModel) -> Unit)? = null
) {
    SwipeReveal(
        revealWidth = 68.dp,
        onDelete = { onDelete?.invoke(record) }
    ) {
        Card(
            onClick = {},
            enabled = true,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 4.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongClick),
            backgroundPainter = CardDefaults.cardBackgroundPainter(Color(0xFF222222)),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.date,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.duration,
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


@androidx.compose.ui.tooling.preview.Preview(
    device = androidx.wear.tooling.preview.devices.WearDevices.SMALL_ROUND,
    showSystemUi = true
)
@Composable
fun HistoryScreenPreview() {
    val mockRecords = listOf(
        MatchHistoryUiModel(1, "2024-02-08", "45分钟/半场", "补时: 上+2 / 下+3", "进球: 2  红牌: 0"),
        MatchHistoryUiModel(2, "2024-02-07", "45分钟/半场", "补时: 上+1 / 下+4", "进球: 1  红牌: 1"),
        MatchHistoryUiModel(3, "2024-02-06", "90分钟/全场", "补时: 上+0 / 下+2", "无事件"),
        MatchHistoryUiModel(4, "2024-02-05", "15分钟/加时", "补时: 上+1 / 下+1", "进球: 1")
    )

    MaterialTheme {
        HistoryScreen(
            initialRecords = mockRecords,
            onClose = {},
            onClearAll = {},
            onDeleteOne = {}
        )
    }
}

@Composable
fun CircularAlert(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    confirmText: @Composable () -> Unit,
    onConfirm: () -> Unit,
    dismissText: (@Composable () -> Unit)? = null,
    onDismissAction: (() -> Unit)? = null
) {
    // 圆形弹窗 - 不填满整个屏幕，只显示圆形 Card
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismissRequest() })
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = {},
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 圆形内部的点击不会冒泡到背景
                    detectTapGestures(onTap = { /* 消费点击事件，不关闭 */ })
                },
            shape = CircleShape,
            backgroundPainter = CardDefaults.cardBackgroundPainter(Color(0xFF222222)),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    title()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissText != null && onDismissAction != null) {
                        Button(
                            onClick = onDismissAction,
                            colors = ButtonDefaults.secondaryButtonColors()
                        ) {
                            dismissText()
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30))
                    ) {
                        confirmText()
                    }
                }
            }
        }
    }
}
