package ru.alexeyivannikov.lab21

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.random.Random


class TimerService : LifecycleService() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private var _secondsLeft = MutableLiveData<TimerState>(TimerState.Stopped)
    val secondsLeft: LiveData<TimerState>
        get() = _secondsLeft

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    private var currentTimer: CountDownTimer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            val m = it.getIntExtra(MainActivity.EXTRA_MINUTES, 0)
            val s = it.getIntExtra(MainActivity.EXTRA_SECONDS, 0)
            val notification = createNotification(m, s)
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
            currentTimer = initTimer(m * 60 + s)
            currentTimer?.start()
        }
        return START_STICKY
    }

    fun stopTimer() {
        currentTimer?.cancel()
        currentTimer = null
        _secondsLeft.postValue(TimerState.Stopped)
        hideNotification()
    }

    private fun updateNotification(leftSeconds: Int) {
        val minutes = leftSeconds / 60
        val seconds = leftSeconds % 60
        val notification = createNotification(minutes, seconds)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun hideNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
    }


    private fun createNotification(minutes: Int, seconds: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setContentTitle("Timer")
                .setContentText("Time left: ${minutes}m ${seconds}s")
                .setSmallIcon(R.drawable.notification_bell)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)

        return builder.build()
    }

    private fun initTimer(seconds: Int): CountDownTimer {
        return object : CountDownTimer(seconds * 1000L, 1_000) {
            override fun onTick(leftSeconds: Long) {
                val left = (leftSeconds / 1000).toInt()
                _secondsLeft.postValue(TimerState.InProgress(left))
                updateNotification(left)
            }

            override fun onFinish() {
                _secondsLeft.postValue(TimerState.Stopped)
                hideNotification()
            }
        }
    }


    companion object {
        const val NOTIFICATION_ID = 123
    }
}