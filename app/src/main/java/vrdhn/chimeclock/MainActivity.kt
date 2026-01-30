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
        
        val spStartHour = findViewById<android.widget.Spinner>(R.id.spinner_night_start_hour)
        val spStartMin = findViewById<android.widget.Spinner>(R.id.spinner_night_start_minute)
        val spStartAmPm = findViewById<android.widget.Spinner>(R.id.spinner_night_start_ampm)
        
        val spEndHour = findViewById<android.widget.Spinner>(R.id.spinner_night_end_hour)
        val spEndMin = findViewById<android.widget.Spinner>(R.id.spinner_night_end_minute)
        val spEndAmPm = findViewById<android.widget.Spinner>(R.id.spinner_night_end_ampm)

        val btnTest = findViewById<Button>(R.id.btn_test)

        fun setSpinnerTime(h24: Int, m: Int, spH: android.widget.Spinner, spM: android.widget.Spinner, spA: android.widget.Spinner) {
            val h12 = when {
                h24 == 0 -> 12
                h24 > 12 -> h24 - 12
                h24 == 12 -> 12
                else -> h24
            }
            spH.setSelection(h12 - 1)
            spM.setSelection(if (m == 30) 1 else 0)
            spA.setSelection(if (h24 >= 12) 1 else 0)
        }

        fun getSpinnerTime(spH: android.widget.Spinner, spM: android.widget.Spinner, spA: android.widget.Spinner): Pair<Int, Int> {
            val h12 = spH.selectedItem.toString().toInt()
            val ampm = spA.selectedItem.toString()
            val m = if (spM.selectedItem.toString() == ":30") 30 else 0
            val h24 = when {
                ampm == "AM" && h12 == 12 -> 0
                ampm == "AM" -> h12
                ampm == "PM" && h12 == 12 -> 12
                else -> h12 + 12
            }
            return h24 to m
        }

        val startH = prefs.getInt("night_start_h", 23)
        val startM = prefs.getInt("night_start_m", 0)
        val endH = prefs.getInt("night_end_h", 6)
        val endM = prefs.getInt("night_end_m", 0)

        setSpinnerTime(startH, startM, spStartHour, spStartMin, spStartAmPm)
        setSpinnerTime(endH, endM, spEndHour, spEndMin, spEndAmPm)

        swHourly.isChecked = prefs.getBoolean("hourly", true)
        swHalfHourly.isChecked = prefs.getBoolean("half_hourly", true)
        swLongChime.isChecked = prefs.getBoolean("long_chime", false)
        seekDay.progress = (prefs.getFloat("day_volume", 1.0f) * 100).toInt()
        seekNight.progress = (prefs.getFloat("night_volume", 0.2f) * 100).toInt()

        val saveListener = {
            val (sH, sM) = getSpinnerTime(spStartHour, spStartMin, spStartAmPm)
            val (eH, eM) = getSpinnerTime(spEndHour, spEndMin, spEndAmPm)
            
            prefs.edit().apply {
                putBoolean("hourly", swHourly.isChecked)
                putBoolean("half_hourly", swHalfHourly.isChecked)
                putBoolean("long_chime", swLongChime.isChecked)
                putFloat("day_volume", seekDay.progress / 100f)
                putFloat("night_volume", seekNight.progress / 100f)
                putInt("night_start_h", sH)
                putInt("night_start_m", sM)
                putInt("night_end_h", eH)
                putInt("night_end_m", eM)
                apply()
            }
            ChimeReceiver.scheduleNext(this)
        }

        val spinnerListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: android.view.View?, p2: Int, p3: Long) { saveListener() }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        listOf(spStartHour, spStartMin, spStartAmPm, spEndHour, spEndMin, spEndAmPm).forEach {
            it.onItemSelectedListener = spinnerListener
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

        val spinnerHour = findViewById<android.widget.Spinner>(R.id.spinner_test_hour)
        val spinnerMinute = findViewById<android.widget.Spinner>(R.id.spinner_test_minute)
        val spinnerAmPm = findViewById<android.widget.Spinner>(R.id.spinner_test_ampm)

        btnTest.setOnClickListener {
            val h12 = spinnerHour.selectedItem.toString().toInt()
            val ampm = spinnerAmPm.selectedItem.toString()
            val m = if (spinnerMinute.selectedItem.toString() == ":30") 30 else 0
            
            val h24 = when {
                ampm == "AM" && h12 == 12 -> 0
                ampm == "AM" -> h12
                ampm == "PM" && h12 == 12 -> 12
                else -> h12 + 12
            }

            val intent = Intent(this, ChimeReceiver::class.java)
            intent.putExtra("test_hour", h24)
            intent.putExtra("test_minute", m)
            sendBroadcast(intent)
        }

        ChimeReceiver.scheduleNext(this)
    }
}
