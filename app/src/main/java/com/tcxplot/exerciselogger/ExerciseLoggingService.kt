package com.tcxplot.exerciselogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.*

class ExerciseLoggingService : Service() {

  companion object {
    private const val TAG = "ExerciseLoggingService"
    private const val SENDER_PACKAGE_NAME = FileSender.SENDER_PACKAGE_NAME
    private const val NOTIFICATION_CHANNEL_ID = "exercise_logging_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "Exercise Logging"
    private const val NOTIFICATION_ID = 1
  }

  private var exerciseType: String? = null
  private var startTime: Long? = null
  private val coroutineScope = CoroutineScope(Dispatchers.IO)
  private var heartbeatJob: Job? = null
  private var logFiles: MutableList<File> = mutableListOf()

  override fun onCreate() {
    super.onCreate()

    Log.i(TAG, "onCreate")
    createNotificationChannel()
    val notification = createNotification()
    startForeground(NOTIFICATION_ID, notification)
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    Log.i(TAG, "Received onStartCommand")

    when (intent?.action) {
      "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STARTED" -> {
        exerciseType = intent.getStringExtra("exerciseType") ?: "Unknown"
        startTime = System.currentTimeMillis()
        var logFile = createLogFile(this)
        logFile?.let {
          logFiles.add(it)
        }
        Log.i(TAG, "Received WEAR_EXERCISE_STARTED for exercise: ${exerciseType}")
        startLogging()
      }
      "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STOPPED" -> {
        Log.i(TAG, "Received WEAR_EXERCISE_STOPPED")
        stopLogging()
        for (logFile in logFiles) {
          FileSender.sendLogDataToSender(logFile, this)
        }
        stopSelf()
      }
    }

    // by returning START_STICKY, the service will be recreated if it gets killed by the system
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder? {
    // bind method is needed as it is an abstract method of Service, return null as we won't be
    // binding this service to any component
    return null
  }

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      )
    val manager = getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
  }

  private fun createNotification(): Notification {
    val builder =
      NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Exercise Logging")
        .setContentText("Logging exercise...")
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    return builder.build()
  }

  private fun startLogging() {
    Log.i(TAG, "Starting logging")
    heartbeatJob?.cancel() // Cancel the previous job if it exists
    heartbeatJob =
      coroutineScope.launch {
        while (true) {
          logHeartbeat()
          delay(1000L)
        }
      }
  }

  private fun stopLogging() {
    Log.i(TAG, "Stopping logging")
    heartbeatJob?.cancel()
    heartbeatJob = null
  }

  private fun logHeartbeat() {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(Date())
    val heartbeat = (60..100).random()
    val log = "$timestamp, bpm=$heartbeat\n"
    Log.i(TAG, log)
    logFiles.firstOrNull()?.appendText(log)
  }

  private fun createLogFile(context: Context): File? {
    val deviceName = android.os.Build.DEVICE
    val instant = Instant.now()
    val formatter = DateTimeFormatter.ISO_INSTANT
    val timestamp = formatter.format(instant)
    val fileName = "${deviceName}-${timestamp}.log"

    // Get the app's private storage directory
    val logsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    if (logsDir != null && logsDir.exists()) {
      val logFile = File(logsDir, fileName)
      Log.i(TAG, "Creating logfile: $fileName on folder: $logsDir")
      return logFile
    }

    Log.e(TAG, "Failed to create logfile: $fileName on folder: $logsDir")
    return null
  }
}
