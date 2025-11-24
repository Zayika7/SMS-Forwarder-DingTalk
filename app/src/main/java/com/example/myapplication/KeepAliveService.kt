package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class KeepAliveService : Service() {

    private var smsReceiver: SmsReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        GlobalState.reloadConfig(this)
        // 动态注册广播，作为第一道防线
        registerDynamicReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "CHANNEL_ID_SMS")
            .setContentTitle("SMS转发服务运行中")
            .setContentText("正在监听短信并转发至钉钉...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        GlobalState.isRunning.value = true
        GlobalState.addLog("系统: 服务已启动 (前台模式)")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterDynamicReceiver()
        GlobalState.isRunning.value = false
        GlobalState.addLog("系统: 服务已停止")
    }

    private fun registerDynamicReceiver() {
        if (smsReceiver == null) {
            smsReceiver = SmsReceiver()
            val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
            filter.priority = Int.MAX_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(smsReceiver, filter)
            }
            GlobalState.addLog("系统: 动态Receiver挂载成功")
        }
    }

    private fun unregisterDynamicReceiver() {
        smsReceiver?.let {
            unregisterReceiver(it)
            smsReceiver = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "CHANNEL_ID_SMS",
                "SMS Forward Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}