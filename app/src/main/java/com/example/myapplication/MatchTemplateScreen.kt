package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 赛事预设库页面
 *
 * 管理和存档多场比赛的配置。
 * 点击列表中的某一个存档，立即携带该配置数据进入主计时器页面。
 *
 * 状态管理：
 * - 列表由内部 mutableStateListOf 驱动，增删直接操作列表，不走路由导航
 * - 新增/删除均带 AnimatedVisibility 动画
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MatchTemplateScreen(
    initialTemplates: List<MatchTemplate>,
    templateManager: MatchTemplateManager,
    onNavigateBack: () -> Unit,
    onTemplateSelected: (MatchTemplate) -> Unit,
) {
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    val templateList = remember { mutableStateListOf<MatchTemplate>().also { it.addAll(initialTemplates) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_match_templates),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewTemplateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_add_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_new_template))
            }
        }
    ) { innerPadding ->
        if (templateList.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wrench),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.msg_no_templates),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.msg_no_templates_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = templateList,
                    key = { it.id },
                ) { template ->
                    AnimatedTemplateCard(
                        template = template,
                        onClick = { onTemplateSelected(template) },
                        onDelete = {
                            templateManager.deleteTemplate(template.id)
                            templateList.remove(template)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(300)
                        )
                    )
                }
                // 底部留白给 FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    // 新建预设弹窗
    if (showNewTemplateDialog) {
        NewTemplateDialog(
            onDismiss = { showNewTemplateDialog = false },
            onConfirm = { template ->
                templateManager.saveTemplate(template)
                templateList.add(0, template)
                showNewTemplateDialog = false
            }
        )
    }
}

/**
 * 带删除动画的赛事预设卡片
 *
 * 使用 AnimatedVisibility + fadeOut + shrinkVertically 实现平滑退出动画，
 * 配合 LazyColumn 的 animateItemPlacement 让剩余卡片自然上移。
 */
@Composable
private fun AnimatedTemplateCard(
    template: MatchTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            delay(300) // 等动画播完再真正删除
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDeleted,
        modifier = modifier,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit = fadeOut(tween(300)) + shrinkVertically(
            animationSpec = tween(300),
            shrinkTowards = Alignment.Top
        )
    ) {
        TemplateCard(
            template = template,
            onClick = onClick,
            onDelete = {
                isDeleted = true
            }
        )
    }
}

/**
 * 单个赛事预设卡片
 */
@Composable
private fun TemplateCard(
    template: MatchTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 顶部：赛事名称 + 删除
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_delete_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 中部：主队 vs 客队（带色块）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // 主队色块 + 名称
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(template.homeTeamColor))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = template.homeTeamName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // VS
                Text(
                    text = " ${stringResource(R.string.label_vs)} ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )

                // 客队名称 + 色块
                Text(
                    text = template.awayTeamName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(template.awayTeamColor))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部：半场时长
            Text(
                text = stringResource(R.string.label_duration) + ": " +
                    stringResource(R.string.label_duration_fmt, template.halfTimeMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 新建赛事预设弹窗
 */
@Composable
private fun NewTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (MatchTemplate) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var homeTeamName by remember { mutableStateOf("") }
    var awayTeamName by remember { mutableStateOf("") }
    var halfTimeMinutes by remember { mutableIntStateOf(45) }
    var homeColor by remember { mutableIntStateOf(0xFF1565C0.toInt()) }
    var awayColor by remember { mutableIntStateOf(0xFFC62828.toInt()) }

    // 预设颜色列表
    val presetColors = listOf(
        0xFF1565C0.toInt(), // 蓝
        0xFFC62828.toInt(), // 红
        0xFF2E7D32.toInt(), // 绿
        0xFFF9A825.toInt(), // 黄
        0xFFFFFFFF.toInt(), // 白
        0xFF000000.toInt(), // 黑
        0xFF6A1B9A.toInt(), // 紫
        0xFFE65100.toInt(), // 橙
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(R.string.btn_new_template),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 赛事名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.hint_template_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // 主队名称
                OutlinedTextField(
                    value = homeTeamName,
                    onValueChange = { homeTeamName = it },
                    label = { Text(stringResource(R.string.hint_team_name) + " (${stringResource(R.string.label_home_team)})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // 客队名称
                OutlinedTextField(
                    value = awayTeamName,
                    onValueChange = { awayTeamName = it },
                    label = { Text(stringResource(R.string.hint_team_name) + " (${stringResource(R.string.label_away_team)})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // 半场时长
                Text(
                    text = "${stringResource(R.string.label_half_duration)}: $halfTimeMinutes ${stringResource(R.string.unit_min_half)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = halfTimeMinutes.toFloat(),
                    onValueChange = { halfTimeMinutes = it.toInt() },
                    valueRange = 5f..45f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 主队颜色选择
                Text(
                    text = stringResource(R.string.label_home_team),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { homeColor = color }
                                .then(
                                    if (color == homeColor) {
                                        Modifier.background(Color.Transparent)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == homeColor) {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_check_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isLightColor(color)) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // 客队颜色选择
                Text(
                    text = stringResource(R.string.label_away_team),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { awayColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == awayColor) {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_check_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isLightColor(color)) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && homeTeamName.isNotBlank() && awayTeamName.isNotBlank()) {
                        onConfirm(
                            MatchTemplate(
                                name = name,
                                homeTeamName = homeTeamName,
                                awayTeamName = awayTeamName,
                                halfTimeMinutes = halfTimeMinutes,
                                homeTeamColor = homeColor,
                                awayTeamColor = awayColor,
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && homeTeamName.isNotBlank() && awayTeamName.isNotBlank(),
            ) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

/**
 * 判断颜色是否为浅色（用于决定勾选图标的颜色）
 */
private fun isLightColor(color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
    return luminance > 186
}
