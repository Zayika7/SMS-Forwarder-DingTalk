package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 1. 配色定义 (保持你的黑客风格) ---
val HackerBlack = Color(0xFF121212)
val HackerGreen = Color(0xFF00FF41)
val HackerGray = Color(0xFF2E2E2E)
val HackerRed = Color(0xFFFF4444)
val HackerYellow = Color(0xFFFDD835)

class MainActivity : ComponentActivity() {

    // 权限请求启动器
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                GlobalState.addLog("权限: 已获取所有必要权限")
            } else {
                GlobalState.addLog("警告: 部分权限被拒绝，可能无法工作")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化配置 (调用你 GlobalState 中的方法)
        GlobalState.reloadConfig(this)

        // 2. 自动开启逻辑
        if (GlobalState.autoStartApp.value) {
            checkAndRequestPermissions()
            startForwardingService()
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = HackerBlack,
                    primary = HackerGreen,
                    onBackground = HackerGreen,
                    surface = HackerGray
                )
            ) {
                MainScreen(
                    onSaveConfig = { url ->
                        // 保存配置到 SharedPreferences
                        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
                        prefs.edit().putString("webhook", url).apply()
                        GlobalState.webhookUrl.value = url
                        GlobalState.addLog("配置: Webhook 已保存")
                        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
                    },
                    onToggleSetting = { key, value ->
                        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean(key, value).apply()
                    },
                    checkPermissions = { checkAndRequestPermissions() },
                    startService = { startForwardingService() },
                    stopService = { stopForwardingService() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        // Android 13 (API 33) 及以上需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startForwardingService() {
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopForwardingService() {
        val intent = Intent(this, KeepAliveService::class.java)
        stopService(intent)
    }
}

@Composable
fun MainScreen(
    onSaveConfig: (String) -> Unit,
    onToggleSetting: (String, Boolean) -> Unit,
    checkPermissions: () -> Unit,
    startService: () -> Unit,
    stopService: () -> Unit
) {
    // 监听 GlobalState 的 Flow 数据
    val isRunning by GlobalState.isRunning.collectAsState()
    val webhookUrl by GlobalState.webhookUrl.collectAsState()
    val autoApp by GlobalState.autoStartApp.collectAsState()
    val autoBoot by GlobalState.autoStartBoot.collectAsState()

    var tempUrl by remember { mutableStateOf(webhookUrl) }

    // ✅ 关键点 1：定义两个独立的滚动状态
    val mainScrollState = rememberScrollState() // 用于上面的配置区
    val logScrollState = rememberLazyListState() // 用于下面的日志区

    // 自动滚动日志到底部
    LaunchedEffect(GlobalState.consoleLogs.size) {
        if (GlobalState.consoleLogs.isNotEmpty()) {
            logScrollState.animateScrollToItem(GlobalState.consoleLogs.size - 1)
        }
    }
    // 同步 URL 显示
    LaunchedEffect(webhookUrl) {
        if (tempUrl.isEmpty() && webhookUrl.isNotEmpty()) {
            tempUrl = webhookUrl
        }
    }

    // 根布局：固定不动的容器
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HackerBlack)
            .padding(16.dp)
    ) {
        // --- 标题 (固定) ---
        Text(
            text = "SMS // 转发终端_V1.3",
            color = HackerGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // ==========================================================
        // ✅ 关键点 2：上半部分 (配置区)
        // 使用 weight(1f) 占据剩余空间 + verticalScroll 允许内容过多时滑动
        // ==========================================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(mainScrollState) // 这里让按钮部分可以滑动
                .padding(bottom = 8.dp)
        ) {
            // --- 状态栏 ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, HackerGray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(text = "运行状态: ", color = Color.White, fontFamily = FontFamily.Monospace)
                Text(
                    text = if (isRunning) "运行中 [ACTIVE]" else "已停止 [INACTIVE]",
                    color = if (isRunning) HackerGreen else HackerRed,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isRunning) HackerGreen else HackerRed, CircleShape)
                )
            }

            // --- 配置输入框 ---
            Text("配置设定 / CONFIG", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = { Text("钉钉机器人 Webhook 地址") },
                textStyle = androidx.compose.ui.text.TextStyle(color = HackerGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HackerGreen,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = HackerGreen,
                    focusedLabelColor = HackerGreen,
                    focusedTextColor = HackerGreen,
                    unfocusedTextColor = HackerGreen
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSaveConfig(tempUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = HackerGray),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, HackerGreen, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = HackerGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存配置 (SAVE CONFIG)", color = HackerGreen, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 开关选项 ---
            Column(modifier = Modifier.fillMaxWidth().border(1.dp, HackerGray, RoundedCornerShape(4.dp)).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("启动APP时自动开启转发", color = HackerGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Switch(
                        checked = autoApp,
                        onCheckedChange = {
                            GlobalState.autoStartApp.value = it
                            onToggleSetting("app_start", it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = HackerGreen, checkedTrackColor = HackerGray)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("手机开机后自动启动服务", color = HackerGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Switch(
                        checked = autoBoot,
                        onCheckedChange = {
                            GlobalState.autoStartBoot.value = it
                            onToggleSetting("boot_start", it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = HackerGreen, checkedTrackColor = HackerGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 测试按钮 (Markdown 优化版) ---
            Button(
                onClick = {
                    GlobalState.addLog("测试: 正在执行模拟推送...")

                    val sender = "模拟测试"
                    val content = "这是一条测试消息，用来验证排版是否清晰。"

                    // ✅ 关键点 3：在这里构建美化后的 Markdown 字符串
                    // 你的 GlobalState.sendToDingTalk 接收字符串并放入 "text" 字段，
                    // 所以我们在这里把 Markdown 格式组装好传进去即可。
                    val formattedMarkdown = """
                        ## <font color="#007FFF">📩 新短信转发</font>
                        ---
                        **发送方：** ` $sender `
                        **接收时间：** ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}
                        ---
                        **内容详情：**
                        > $content
                    """.trimIndent()

                    GlobalState.sendToDingTalk(sender, formattedMarkdown)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HackerGray),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, HackerYellow, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = HackerYellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试推送 (TEST PUSH)", color = HackerYellow, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 服务控制按钮 ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        checkPermissions()
                        startService()
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isRunning) Color.Gray else HackerGreen,
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Text(" 开启服务", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        stopService()
                    },
                    enabled = isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(!isRunning) Color.Gray else HackerRed,
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black)
                    Text(" 停止服务", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        } // --- 上半部分配置区结束 ---

        // ==========================================================
        // ✅ 关键点 4：下半部分 (日志区)
        // 使用 height(180.dp) 固定高度，确保永远在底部显示且不被挤出
        // ==========================================================
        Text("控制台日志 (SYSTEM LOG) >_", color = HackerGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp) // 固定高度，不再使用 fillMaxSize
                .background(Color(0xFF0A0A0A))
                .border(1.dp, HackerGreen.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            LazyColumn(state = logScrollState) {
                items(GlobalState.consoleLogs) { log ->
                    Text(text = log, color = HackerGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}