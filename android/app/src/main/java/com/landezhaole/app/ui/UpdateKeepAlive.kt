package com.landezhaole.app.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.landezhaole.app.R

/**
 * 更新下载期间的保活：按用户给的 DownloadKeepAlive 移植（前台服务 + 唤醒锁）。
 * 只在真的有下载在跑时开着，下载收尾就停服务，通知栏不留常驻通知。
 */
internal object UpdateKeepAlive {
    fun sync(context: Context, active: Int) {
        val app = context.applicationContext
        val intent = Intent(app, UpdateKeepingService::class.java)
        try {
            if (active > 0) app.startForegroundService(intent) else app.stopService(intent)
        } catch (e: Throwable) {
            android.util.Log.w("landezhaole-update", "更新保活没起来(下载照常):$e")
        }
    }
}

class UpdateKeepingService : Service() {
    private var wake: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 前台通知或唤醒锁任一环节抛异常都不能连累主进程闪退：起不来就停服务，下载本身照常跑
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Throwable) {
            android.util.Log.w("landezhaole-update", "前台通知起不来，停服务保下载:$e")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            holdCpu()
        } catch (e: Throwable) {
            android.util.Log.w("landezhaole-update", "唤醒锁拿不到，下载照常:$e")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseCpu()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "更新下载", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("正在下载更新")
            .setContentText("切到别的应用也会继续")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
        packageManager.getLaunchIntentForPackage(packageName)?.let { open ->
            builder.setContentIntent(
                PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE)
            )
        }
        return builder.build()
    }

    private fun holdCpu() {
        if (wake != null) return
        val power = getSystemService(PowerManager::class.java)
        wake = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseCpu() {
        wake?.let { if (it.isHeld) it.release() }
        wake = null
    }

    private companion object {
        const val CHANNEL_ID = "landezhaole-update"
        const val NOTIFICATION_ID = 0x4A1
        const val WAKE_TAG = "landezhaole:update"
    }
}
