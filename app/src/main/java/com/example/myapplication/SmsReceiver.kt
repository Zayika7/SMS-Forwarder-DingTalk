package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            // 这里不做去重，只管接收，去重交给 GlobalState 处理

            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as? Array<*> ?: return
                    val format = bundle.getString("format")

                    for (pdu in pdus) {
                        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            SmsMessage.createFromPdu(pdu as ByteArray, format)
                        } else {
                            SmsMessage.createFromPdu(pdu as ByteArray)
                        }

                        val sender = sms.originatingAddress ?: "Unknown"
                        val msgBody = sms.messageBody ?: ""

                        // 调用 GlobalState 发送（那里有去重逻辑）
                        GlobalState.sendToDingTalk(sender, msgBody)
                    }
                } catch (e: Exception) {
                    GlobalState.addLog("解析错误: ${e.message}")
                }
            }
        }
    }
}