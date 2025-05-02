package ru.alexeyivannikov.lab21

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import ru.alexeyivannikov.lab21.databinding.ActivityMainBinding
import java.util.Timer
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var isPermission = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isPermission = true
            clickFun()
        } else {
            Toast.makeText(this, "Необходимо разрешение на уведомления", Toast.LENGTH_SHORT).show()
        }
    }

    private var timerService: TimerService? = null;

    private var timerState = MutableLiveData<TimerState>(TimerState.Stopped)

    private val connector = object : ServiceConnection {
        override fun onServiceConnected(
            p0: ComponentName?,
            p1: IBinder?
        ) {
            val binder = p1 as TimerService.LocalBinder
            timerService = binder.getService()
            timerService?.secondsLeft?.observe(this@MainActivity) {
                timerState.value = it
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            timerService = null;
        }
    }



    private fun updateTimer(secondsLeft: Int) {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val s = "${minutes}:${seconds.toString().padStart(2, '0')}"
        binding.tvTimerState.text = s
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        observeState()

        binding.btTimerControl.setOnClickListener {view ->
            if (!isPermission) {
                checkPermission()
            } else {
                clickFun()
            }
        }
    }

    private fun clickFun() {
        if (!validateFields()){
            return
        }
        if (timerState.value == TimerState.Stopped) {
            Toast.makeText(this, "Таймер запущен", Toast.LENGTH_SHORT).show()
            startTimer()
        } else {
            stopTimer()
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TimerService::class.java)
        bindService(intent, connector, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connector)
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                Toast.makeText(this, "Необходимо разрешение на уведомления", Toast.LENGTH_SHORT)
                    .show()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            isPermission = true
            startTimer()
        }
    }

    private fun observeState() {
        timerState.observe(this) {
            when (it) {
                is TimerState.Stopped -> {
                    binding.etSeconds.isEnabled = true
                    binding.etMinutes.isEnabled = true
                    binding.btTimerControl.text = "Пуск"
                    binding.tvTimerState.text =
                        resources.getText(R.string.stopped_timer_state)
                }

                is TimerState.InProgress -> {
                    updateTimer(it.secondsLeft)
                    binding.btTimerControl.text = "Стоп"
                    binding.etMinutes.isEnabled = false
                    binding.etSeconds.isEnabled = false
                }
            }
        }
    }


    private fun stopTimer() {
        timerService?.stopTimer()
    }

    private fun validateFields(): Boolean {
        with(binding) {
            if (etMinutes.text.toString().trim().isEmpty() || etMinutes.text.toString().trim().isEmpty()) {
                Toast.makeText(this@MainActivity, "Поля времени не должны быть пустыми", Toast.LENGTH_SHORT).show()
                return false;
            }
            try {
                etMinutes.getInt()
                val s = etSeconds.getInt()
                if (s > 59) {
                    Toast.makeText(this@MainActivity, "Секунды должны быть меньше 60", Toast.LENGTH_SHORT).show()
                    return false
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка при обработке полей", Toast.LENGTH_SHORT).show()

                return false
            }
        }
        return true
    }

    private fun startTimer() {
        if (timerState.value is TimerState.Stopped) {
            createNotificationChanel()
            val intent = Intent(this, TimerService::class.java).apply {
                putExtra(EXTRA_MINUTES, binding.etMinutes.getInt())
                putExtra(EXTRA_SECONDS, binding.etSeconds.getInt())
            }

            this.startService(intent)
        }
    }

    private fun createNotificationChanel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Таймер",
            NotificationManager.IMPORTANCE_HIGH
        )

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    fun EditText.getInt(): Int {
        return this.text.toString().toInt()
    }

    companion object {
        const val CHANNEL_ID = "lab21_channel_msg"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_SECONDS = "seconds"
        const val SERVICE_ID = 1
    }

}