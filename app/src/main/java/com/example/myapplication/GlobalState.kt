package com.example.myapplication // <--- 确认包名

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

object GlobalState {
    val consoleLogs = mutableStateListOf<String>()
    val isRunning = MutableStateFlow(false)
    val webhookUrl = MutableStateFlow("")

    // 【新增】配置状态
    val autoStartApp = MutableStateFlow(false) // 打开APP自动开启
    val autoStartBoot = MutableStateFlow(false) // 手机开机自启

    fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        if (consoleLogs.size > 200) {
            consoleLogs.removeAt(0)
        }
        consoleLogs.add("[$time] $message")
    }

    fun sendToDingTalk(sender: String, content: String) {
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
                markdown.put("text", "### 📩 新短信通知\n\n**来源:** $sender\n\n**内容:**\n> $content")
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