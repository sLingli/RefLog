package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.*

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
                duration = "${record.halfTimeMinutes}分钟/半场",
                stoppage = "补时: 上+${record.firstHalfStoppage} / 下+${record.secondHalfStoppage}",
                events = "进球:${record.goalCount}  红牌:${record.redCount}  换人:${record.substitutionCount}"
            )
        }

        setContent {
            MaterialTheme {
                HistoryScreen(
                    initialRecords = uiRecords,
                    onClose = { finish() },
                    onClearAll = {
                        // 真实清空数据库
                        recordManager.clearAllRecords()
                        Toast.makeText(this, "历史记录已清空", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteOne = { uiModel ->
                        // 真实删除单条
                        recordManager.deleteRecord(uiModel.id)
                        Toast.makeText(this, "记录已删除", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    initialRecords: List<MatchHistoryUiModel>, 
    onClose: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteOne: (MatchHistoryUiModel) -> Unit
) {
    val records = remember { mutableStateListOf<MatchHistoryUiModel>().apply { addAll(initialRecords) } }
    val listState: androidx.wear.compose.material.ScalingLazyListState = rememberScalingLazyListState()

    var recordToDelete by remember { mutableStateOf<MatchHistoryUiModel?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            scalingParams = ScalingLazyColumnDefaults.scalingParams()
        ) {
            // ...

            item {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history),
                    contentDescription = "History",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp).padding(bottom = 8.dp)
                )
            }

            // 2. 列表
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
                        onLongClick = { recordToDelete = record }
                    )
                }
            }

            // 3. 底部按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 清空 (红)
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

    // 删除单条弹窗
    if (recordToDelete != null) {
        Alert(
            title = { Text("删除这条记录?", textAlign = TextAlign.Center) },
            positiveButton = {
                Button(
                    onClick = {
                        onDeleteOne(recordToDelete!!)
                        records.remove(recordToDelete)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30))
                ) { Icon(painterResource(R.drawable.outline_delete_24), null) }
            },
            negativeButton = {
                Button(
                    onClick = { recordToDelete = null },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Icon(painterResource(R.drawable.outline_close_24), null) }
            }
        )
    }

    // 清空全部弹窗
    if (showClearAllDialog) {
        Alert(
            title = { Text(stringResource(R.string.msg_confirm_clear_all), textAlign = TextAlign.Center) },
            positiveButton = {
                Button(
                    onClick = {
                        onClearAll()
                        records.clear()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30))
                ) { Icon(painterResource(R.drawable.outline_delete_24), null) }
            },
            negativeButton = {
                Button(onClick = { showClearAllDialog = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(painterResource(R.drawable.outline_close_24), null)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    record: MatchHistoryUiModel,
    onLongClick: () -> Unit
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
        Column(modifier = Modifier.padding(4.dp)) {
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

            Spacer(modifier = Modifier.height(4.dp))


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