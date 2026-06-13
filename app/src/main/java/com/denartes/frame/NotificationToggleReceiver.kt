package com.denartes.frame

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            OverlayService.ACTION_PAUSE, OverlayService.ACTION_RESUME -> {
                context.startService(
                    Intent(context, OverlayService::class.java).also { it.action = intent.action }
                )
            }
        }
    }
}
