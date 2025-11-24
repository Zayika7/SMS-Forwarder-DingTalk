package com.example.myapplication // <--- 确认包名

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!GlobalState.isRunning.value) return

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = sms.originatingAddress ?: "Unknown"
                    val msgBody = sms.messageBody ?: ""

                    GlobalState.addLog("收到短信: 来自 $sender")

                    // 调用 GlobalState 里的公共发送方法
                    GlobalState.sendToDingTalk(sender, msgBody)
                }
            }
        }
    }
}