package com.example.myapplication // <--- 确认你的包名

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 读取配置
            val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
            val isBootStartEnabled = prefs.getBoolean("boot_start", false)

            if (isBootStartEnabled) {
                // 启动服务
                val serviceIntent = Intent(context, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 这里我们无法直接写 GlobalState.addLog，因为 App 可能还没完全启动，
                // 但服务启动后会在通知栏显示。
            }
        }
    }
}