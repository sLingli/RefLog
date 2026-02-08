package com.example.myapplication // 🔥 改成你的包名

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert

// 🔥 UI 专用的数据模型 (为了不跟你的 MatchRecord 混淆)
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

        // 1. 初始化数据管理器
        recordManager = MatchRecordManager(this)

        // 2. 获取所有原始记录
        val rawRecords = recordManager.getAllRecords()

        // 3. 🔥 翻译数据：把 MatchRecord 转成 MatchHistoryUiModel
        val uiRecords = rawRecords.map { record ->
            MatchHistoryUiModel(
                id = record.id,
                date = record.date, // 直接用
                duration = "${record.halfTimeMinutes}分钟/半场", // 拼接字符串
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

// ... (下面的 Compose 代码完全不用动，保持原样即可) ...
// ... (包括 HistoryScreen 和 HistoryItemCard) ...

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    initialRecords: List<MatchHistoryUiModel>, // 🔥 这里也改了类型
    onClose: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteOne: (MatchHistoryUiModel) -> Unit // 🔥 这里也改了类型
) {
    val records = remember { mutableStateListOf<MatchHistoryUiModel>().apply { addAll(initialRecords) } }
    val listState = rememberScalingLazyListState()

    // 控制删除确认弹窗
    var recordToDelete by remember { mutableStateOf<MatchHistoryUiModel?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            anchorType = androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType.ItemStart
        ) {
            // 1. 顶部图标
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
                        text = "暂无比赛记录",
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

                    // 关闭 (绿)
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676)),
                        modifier = Modifier.size(50.dp).padding(start = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(painterResource(id = R.drawable.outline_close_24), null, tint = Color.White)
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
            title = { Text("清空所有历史?", textAlign = TextAlign.Center) },
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

// 🔥 单个历史记录卡片
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    record: MatchHistoryUiModel, // 🔥 类型已修改
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
            // 第一行：日期 + 时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔥 这里 Text 就不会报错了，因为 record 是我们新定义的类，它一定有 date 字段
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

            // 第二行：补时
            Text(
                text = record.stoppage,
                color = Color(0xFFFF9800),
                fontSize = 12.sp
            )

            // 第三行：事件
            Text(
                text = record.events,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@androidx.compose.ui.tooling.preview.Preview(
    device = androidx.wear.tooling.preview.devices.WearDevices.SMALL_ROUND,
    showSystemUi = true
)
@Composable
fun HistoryScreenPreview() {
    // 1. 搞几条假数据，方便看效果
    val mockRecords = listOf(
        MatchHistoryUiModel(1, "2024-02-08", "45分钟/半场", "补时: 上+2 / 下+3", "进球: 2  红牌: 0"),
        MatchHistoryUiModel(2, "2024-02-07", "45分钟/半场", "补时: 上+1 / 下+4", "进球: 1  红牌: 1"),
        MatchHistoryUiModel(3, "2024-02-06", "90分钟/全场", "补时: 上+0 / 下+2", "无事件"),
        MatchHistoryUiModel(4, "2024-02-05", "15分钟/加时", "补时: 上+1 / 下+1", "进球: 1")
    )

    MaterialTheme {
        HistoryScreen(
            initialRecords = mockRecords,
            onClose = {},     // 预览里不需要真的关闭
            onClearAll = {},  // 预览里不需要真的清空
            onDeleteOne = {}  // 预览里不需要真的删除
        )
    }
}