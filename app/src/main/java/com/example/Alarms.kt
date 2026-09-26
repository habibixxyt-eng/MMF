package com.example

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object Alarms {
    const val CHANNEL = "mmf_alarms"

    fun channels(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL,
            "Alarms and reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Locally scheduled personal reminders"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun exact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    private fun pending(c: Context, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            c,
            id.hashCode(),
            Intent(c, AlarmReceiver::class.java)
                .setData(Uri.parse("mmf://alarm/$id"))
                .putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun next(d: JSONObject, now: Long = System.currentTimeMillis()): Long? {
        if (!d.optBoolean("enabled", true)) return null
        val days = d.optString("days").split(",").mapNotNull { it.trim().toIntOrNull() }
        val interval = d.optInt("interval", 0)
        val base = d.optLong("when", 0L)
        if (base <= 0L) return null
        if (days.isEmpty() && interval == 0) return base.takeIf { it > now }

        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(base).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        for (i in 0..3700) {
            val date = today.plusDays(i.toLong())
            val elapsed = ChronoUnit.DAYS.between(start.toLocalDate(), date)
            val match = if (days.isNotEmpty()) {
                date.dayOfWeek.value in days
            } else {
                elapsed >= 0 && elapsed % interval == 0L
            }
            if (match && !date.isBefore(start.toLocalDate())) {
                val candidate = date.atTime(start.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
                if (candidate > now) return candidate
            }
        }
        return null
    }

    fun schedule(c: Context, item: Item, at: Long? = null) {
        val manager = c.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pending(c, item.id))
        val time = at ?: next(item.data) ?: return
        val pi = pending(c, item.id)
        if (exact(c)) {
            val show = PendingIntent.getActivity(
                c,
                0,
                Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(time, show), pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }

    fun cancel(c: Context, id: String) {
        c.getSystemService(AlarmManager::class.java)?.cancel(pending(c, id))
    }

    fun restore(c: Context) {
        val s = Store(c)
        try {
            s.all().filter { it.kind == "alarm" || it.kind == "activity" }.forEach {
                if (it.kind == "alarm" || it.data.optLong("when", 0) > 0) {
                    runCatching { schedule(c, it) }
                }
            }
        } finally {
            s.close()
        }
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val result = goAsync()
        Thread {
            try {
                Alarms.restore(c)
            } finally {
                result.finish()
            }
        }.start()
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val s = Store(c)
        try {
            val item = s.get(id) ?: return
            if (!item.data.optBoolean("enabled", true)) return
            val service = Intent(c, RingService::class.java).putExtra("id", id)
            if (Alarms.exact(c)) {
                try {
                    c.startForegroundService(service)
                } catch (e: RuntimeException) {
                    RingService.notifyOnly(c, item)
                }
            } else {
                RingService.notifyOnly(c, item)
            }
            Alarms.schedule(c, item)
        } finally {
            s.close()
        }
    }
}

class RingService : Service() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var activeId: String? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val id = i?.getStringExtra("id") ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val s = Store(this)
        val item = try {
            s.get(id)
        } finally {
            s.close()
        }
        if (item == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (i.action == "dismiss" || i.action == "snooze") {
            if (i.action == "snooze") {
                Alarms.schedule(this, item, System.currentTimeMillis() + 300000L)
            }
            getSystemService(NotificationManager::class.java)?.cancel(id.hashCode())
            if (activeId == null || activeId == id) stopSelf()
            return START_NOT_STICKY
        }
        activeId = id
        Alarms.channels(this)
        startForeground(id.hashCode(), notification(this, item))
        player?.release()
        val uri = item.data.optString("sound").takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@RingService, uri)
                isLooping = true
                prepare()
                start()
            }
        }
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, 600000L)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        fun notification(c: Context, item: Item): Notification {
            val open = PendingIntent.getActivity(
                c,
                0,
                Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            fun action(name: String) = PendingIntent.getService(
                c,
                name.hashCode() xor item.id.hashCode(),
                Intent(c, RingService::class.java)
                    .setAction(name)
                    .setData(Uri.parse("mmf://$name/${item.id}"))
                    .putExtra("id", item.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = item.data.optString("name", "MMF Formula Reminder")
            val notes = item.data.optString("notes", "Open app to view details")

            return NotificationCompat.Builder(c, Alarms.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(if (notes.isNotBlank()) notes else "Offline reminder")
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .addAction(0, "Dismiss", action("dismiss"))
                .addAction(0, "Snooze 5 min", action("snooze"))
                .build()
        }

        fun notifyOnly(c: Context, item: Item) {
            Alarms.channels(c)
            runCatching {
                c.getSystemService(NotificationManager::class.java)?.notify(item.id.hashCode(), notification(c, item))
            }
        }
    }
}
