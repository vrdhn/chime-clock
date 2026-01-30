package vrdhn.chimeclock

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val swHourly = findViewById<Switch>(R.id.switch_hourly)
        val swHalfHourly = findViewById<Switch>(R.id.switch_half_hourly)
        val swLongChime = findViewById<Switch>(R.id.switch_long_chime)
        val seekDay = findViewById<SeekBar>(R.id.seek_day_volume)
        val seekNight = findViewById<SeekBar>(R.id.seek_night_volume)
        val btnNightStart = findViewById<Button>(R.id.btn_night_start)
        val btnNightEnd = findViewById<Button>(R.id.btn_night_end)
        val btnTest = findViewById<Button>(R.id.btn_test)

        var nightStart = prefs.getInt("night_start", 23)
        var nightEnd = prefs.getInt("night_end", 6)
        
        btnNightStart.text = "Start: %02d:00".format(nightStart)
        btnNightEnd.text = "End: %02d:00".format(nightEnd)

        swHourly.isChecked = prefs.getBoolean("hourly", true)
        swHalfHourly.isChecked = prefs.getBoolean("half_hourly", true)
        swLongChime.isChecked = prefs.getBoolean("long_chime", false)
        seekDay.progress = (prefs.getFloat("day_volume", 1.0f) * 100).toInt()
        seekNight.progress = (prefs.getFloat("night_volume", 0.2f) * 100).toInt()

        val saveListener = {
            prefs.edit().apply {
                putBoolean("hourly", swHourly.isChecked)
                putBoolean("half_hourly", swHalfHourly.isChecked)
                putBoolean("long_chime", swLongChime.isChecked)
                putFloat("day_volume", seekDay.progress / 100f)
                putFloat("night_volume", seekNight.progress / 100f)
                putInt("night_start", nightStart)
                putInt("night_end", nightEnd)
                apply()
            }
            ChimeReceiver.scheduleNext(this)
        }

        btnNightStart.setOnClickListener {
            TimePickerDialog(this, { _, h, _ ->
                nightStart = h
                btnNightStart.text = "Start: %02d:00".format(nightStart)
                saveListener()
            }, nightStart, 0, true).show()
        }

        btnNightEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, _ ->
                nightEnd = h
                btnNightEnd.text = "End: %02d:00".format(nightEnd)
                saveListener()
            }, nightEnd, 0, true).show()
        }

        swHourly.setOnCheckedChangeListener { _, _ -> saveListener() }
        swHalfHourly.setOnCheckedChangeListener { _, _ -> saveListener() }
        swLongChime.setOnCheckedChangeListener { _, _ -> saveListener() }
        
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { saveListener() }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }
        seekDay.setOnSeekBarChangeListener(seekBarListener)
        seekNight.setOnSeekBarChangeListener(seekBarListener)

        btnTest.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val intent = Intent(this, ChimeReceiver::class.java)
                intent.putExtra("test_hour", h)
                intent.putExtra("test_minute", m)
                sendBroadcast(intent)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }

        ChimeReceiver.scheduleNext(this)
    }
}
