package com.example.myapplication

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GlobalState {
    val consoleLogs = mutableStateListOf<String>()
    val isRunning = MutableStateFlow(false)
    val webhookUrl = MutableStateFlow("")
    val autoStartApp = MutableStateFlow(false)
    val autoStartBoot = MutableStateFlow(false)

    // --- 【关键】防重复发送变量 ---
    private var lastMsgContent: String = ""
    private var lastMsgTime: Long = 0

    fun reloadConfig(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        webhookUrl.value = prefs.getString("webhook", "") ?: ""
        autoStartApp.value = prefs.getBoolean("app_start", false)
        autoStartBoot.value = prefs.getBoolean("boot_start", false)
        addLog("系统: 配置已重载")
    }

    fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (consoleLogs.size > 200) {
            consoleLogs.removeAt(0)
        }
        consoleLogs.add("[$time] $message")
    }

    // --- 统一发送入口 ---
    fun sendToDingTalk(sender: String, content: String) {
        // 1. 去重检查：如果3秒内收到相同内容，直接丢弃
        val currentTime = System.currentTimeMillis()
        if (content == lastMsgContent && (currentTime - lastMsgTime) < 3000) {
            addLog("系统: 拦截到重复广播，已忽略")
            return
        }

        // 更新最后一条消息记录
        lastMsgContent = content
        lastMsgTime = currentTime

        val url = webhookUrl.value
        if (url.isBlank()) {
            addLog("错误: 未配置 Webhook URL")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject()
                json.put("msgtype", "markdown")
                val markdown = JSONObject()
                markdown.put("title", "短信转发通知")
                markdown.put("text", content)
                json.put("markdown", markdown)

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(requestBody).build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    addLog("发送成功: [$sender] 已转发")
                } else {
                    addLog("发送失败: 钉钉返回码 ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                addLog("异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}