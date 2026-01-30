package vrdhn.chimeclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import java.util.Calendar

class ChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
            scheduleNext(context)
            return
        }

        val testHour = intent.getIntExtra("test_hour", -1)
        val testMinute = intent.getIntExtra("test_minute", -1)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChimeClock:WakeLock")
        wakeLock.acquire(30000) // 30 seconds max

        playChime(context, testHour, testMinute) {
            if (wakeLock.isHeld) wakeLock.release()
            if (testHour == -1) scheduleNext(context)
        }
    }

    private fun playChime(context: Context, testHour: Int, testMinute: Int, onComplete: () -> Unit) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val now = Calendar.getInstance()
        val hour = if (testHour != -1) testHour else now.get(Calendar.HOUR_OF_DAY)
        val minute = if (testMinute != -1) testMinute else now.get(Calendar.MINUTE)

        val startH = prefs.getInt("night_start_h", 23)
        val startM = prefs.getInt("night_start_m", 0)
        val endH = prefs.getInt("night_end_h", 6)
        val endM = prefs.getInt("night_end_m", 0)
        
        val startTotal = startH * 60 + startM
        val endTotal = endH * 60 + endM
        val currentTotal = hour * 60 + minute

        val isNight = if (startTotal > endTotal) {
            currentTotal >= startTotal || currentTotal < endTotal
        } else {
            currentTotal in startTotal until endTotal
        }

        val volume = if (isNight) {
            prefs.getFloat("night_volume", 0.2f)
        } else {
            prefs.getFloat("day_volume", 1.0f)
        }

        val isHalfHour = minute >= 25 && minute <= 35
        val enabled = if (isHalfHour) prefs.getBoolean("half_hourly", true) else prefs.getBoolean("hourly", true)
        
        if (!enabled) {
            onComplete()
            return
        }

        val useLongChime = prefs.getBoolean("long_chime", false)
        val dingCount = if (isHalfHour) 1 else {
            val h = if (testHour != -1) testHour % 12 else now.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        val dingId = soundPool.load(context, R.raw.ding, 1)
        val longChimeId = if (useLongChime) soundPool.load(context, R.raw.long_chime, 1) else -1
        val loadedSamples = mutableSetOf<Int>()
        val totalToLoad = if (useLongChime) 2 else 1

        soundPool.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0) {
                loadedSamples.add(sampleId)
                if (loadedSamples.size == totalToLoad) {
                    val handler = Handler(Looper.getMainLooper())
                    
                    fun playDings(count: Int) {
                        if (count <= 0) {
                            handler.postDelayed({
                                sp.release()
                                onComplete()
                            }, 1000)
                            return
                        }
                        sp.play(dingId, volume, volume, 1, 0, 1f)
                        handler.postDelayed({ playDings(count - 1) }, 1500)
                    }

                    if (useLongChime) {
                        sp.play(longChimeId, volume, volume, 1, 0, 1f)
                        handler.postDelayed({ playDings(dingCount) }, 4000)
                    } else {
                        playDings(dingCount)
                    }
                }
            }
        }
    }

    companion object {
        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ChimeReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = Calendar.getInstance()
            val next = Calendar.getInstance()
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)

            if (next.get(Calendar.MINUTE) < 30) {
                next.set(Calendar.MINUTE, 30)
            } else {
                next.set(Calendar.MINUTE, 0)
                next.add(Calendar.HOUR_OF_DAY, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent)
            }
        }
    }
}
