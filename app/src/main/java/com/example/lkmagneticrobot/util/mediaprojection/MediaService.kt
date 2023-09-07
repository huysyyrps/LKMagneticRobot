package com.example.lkmagneticrobot.util.mediaprojection

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat


class MediaService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "com.tencent.trtc.apiexample.MediaService"
    private val NOTIFICATION_CHANNEL_NAME = "com.tencent.trtc.apiexample.channel_name"
    private val NOTIFICATION_CHANNEL_DESC = "com.tencent.trtc.apiexample.channel_desc"
    override fun onCreate() {
        super.onCreate()
        startNotification()
    }

    private fun startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Call Start foreground with notification
            val notificationIntent = Intent(this, MediaService::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//                    .setLargeIcon(
//                        BitmapFactory.decodeResource(
//                            getResources()
////                            R.drawable.ic_launcher_foreground
//                        )
//                    )
//                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Starting Service")
                    .setContentText("Starting monitoring service")
                    .setContentIntent(pendingIntent)
            val notification: Notification = notificationBuilder.build()
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = NOTIFICATION_CHANNEL_DESC
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager!!.createNotificationChannel(channel)
            startForeground(
                1,
                notification
            ) //必须使用此方法显示通知，不能使用notificationManager.notify，否则还是会报上面的错误
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }
}