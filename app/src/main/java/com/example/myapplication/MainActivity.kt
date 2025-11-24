package com.example.myapplication // <--- 确认包名

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
import androidx.compose.foundation.shape.RoundedCornerShape
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

// --- 配色保持不变 ---
val HackerBlack = Color(0xFF121212)
val HackerGreen = Color(0xFF00FF41)
val HackerGray = Color(0xFF2E2E2E)
val HackerRed = Color(0xFFFF4444)
val HackerYellow = Color(0xFFFDD835)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                GlobalState.addLog("权限: 已获取所有必要权限")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 读取所有配置
        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        GlobalState.webhookUrl.value = prefs.getString("webhook", "") ?: ""
        GlobalState.autoStartApp.value = prefs.getBoolean("app_start", false)
        GlobalState.autoStartBoot.value = prefs.getBoolean("boot_start", false)

        // 2. 检查是否需要“打开APP自动开启”
        if (GlobalState.autoStartApp.value) {
            startForwardingService()
            GlobalState.isRunning.value = true
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
                        prefs.edit().putString("webhook", url).apply()
                        GlobalState.webhookUrl.value = url
                        GlobalState.addLog("配置: Webhook 已保存")
                        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
                    },
                    onToggleSetting = { key, value ->
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
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS)
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
    onToggleSetting: (String, Boolean) -> Unit, // 新增：保存开关状态的回调
    checkPermissions: () -> Unit,
    startService: () -> Unit,
    stopService: () -> Unit
) {
    val isRunning by GlobalState.isRunning.collectAsState()
    val webhookUrl by GlobalState.webhookUrl.collectAsState()
    val autoApp by GlobalState.autoStartApp.collectAsState()
    val autoBoot by GlobalState.autoStartBoot.collectAsState()

    var tempUrl by remember { mutableStateOf(webhookUrl) }
    val scrollState = rememberLazyListState()

    LaunchedEffect(GlobalState.consoleLogs.size) {
        if (GlobalState.consoleLogs.isNotEmpty()) {
            scrollState.animateScrollToItem(GlobalState.consoleLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HackerBlack)
            .padding(16.dp)
    ) {
        // --- 标题 ---
        Text(
            text = "SMS // 转发终端_V1.1",
            color = HackerGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

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
                    .background(if (isRunning) HackerGreen else HackerRed, androidx.compose.foundation.shape.CircleShape)
            )
        }

        // --- Webhook 配置 ---
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

        // --- 新增：高级开关 ---
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, HackerGray, RoundedCornerShape(4.dp)).padding(8.dp)) {
            // 开关 1
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
            // 开关 2
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

        // --- 测试按钮 ---
        Button(
            onClick = {
                GlobalState.addLog("测试: 正在执行模拟推送...")
                GlobalState.sendToDingTalk("模拟测试", "这是一条测试消息，验证配置。")
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

        // --- 核心控制 ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    checkPermissions()
                    GlobalState.isRunning.value = true
                    startService()
                    GlobalState.addLog("系统: 监听服务已启动")
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
                    GlobalState.isRunning.value = false
                    stopService()
                    GlobalState.addLog("系统: 监听服务已停止")
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

        Spacer(modifier = Modifier.height(16.dp))

        // --- 日志 ---
        Text("控制台日志 (SYSTEM LOG) >_", color = HackerGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .border(1.dp, HackerGreen.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            LazyColumn(state = scrollState) {
                items(GlobalState.consoleLogs) { log ->
                    Text(text = log, color = HackerGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}