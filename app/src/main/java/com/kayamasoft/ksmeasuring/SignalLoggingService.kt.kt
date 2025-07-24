package com.kayamasoft.ksmeasuring

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.*
import android.telephony.*
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SignalLoggingService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var connectivityManager: ConnectivityManager
    private var rsrpHistory = mutableListOf<Float>()
    private var isRunning = true
    private lateinit var logFile: File

    override fun onCreate() {
        super.onCreate()

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val logDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ksmeasuring")
        if (!logDir.exists()) logDir.mkdirs()
        logFile = File(logDir, "KSM_${timestamp}.csv")

        startForegroundNotification()

        Thread {
            var lastRx = TrafficStats.getTotalRxBytes()
            var lastTx = TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()
            var headerWritten = false

            while (isRunning) {
                val now = System.currentTimeMillis()
                val newRx = TrafficStats.getTotalRxBytes()
                val newTx = TrafficStats.getTotalTxBytes()
                val dlMbps = ((newRx - lastRx) * 8 / 1_000_000.0) / ((now - lastTime) / 1000.0)
                val ulMbps = ((newTx - lastTx) * 8 / 1_000_000.0) / ((now - lastTime) / 1000.0)

                lastRx = newRx
                lastTx = newTx
                lastTime = now

                val info = getSignalInfo(applicationContext, telephonyManager, connectivityManager, dlMbps, ulMbps, rsrpHistory)
                try {
                    FileOutputStream(logFile, true).use { fos ->
                        OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                            if (!headerWritten) {
                                writer.write("\uFEFF")
                                writer.append(info.joinToString(",") { it.first }).append("\n")
                                headerWritten = true
                            }
                            writer.append(info.joinToString(",") { (label, value) ->
                                val formattedValue = if (label == "Neighbor Cell") {
                                    value.replace("\n", "; ")
                                } else {
                                    value
                                }
                                escapeCsv(formattedValue)
                            }).append("\n")
                        }
                    }
                } catch (_: Exception) { }

                Thread.sleep(1000)
            }
        }.start()
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    override fun onDestroy() {
        isRunning = false

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "ks_logging_channel"
        val channelName = "KS Measuring Logging"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        // 通知タップでMainActivityを開く
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ログ取得中")
            .setContentText("KS Measuringがバックグラウンドでログを取得しています")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

}
