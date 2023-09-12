package com.tcxplot.exerciselogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class ExerciseLoggingReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "ExerciseLoggingReceiver"
    private const val SENDER_PACKAGE_NAME = FileSender.SENDER_PACKAGE_NAME
  }

  override fun onReceive(context: Context, intent: Intent) {
    Log.d(TAG, "Received intent: ${intent.action} in receiver with hashCode: ${this.hashCode()}")
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED -> {
        // The device has finished booting
        Log.d(TAG, "Received BOOT_COMPLETED action")
        startExerciseLoggingService(context, intent)
      }
      "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STARTED" -> {
        // Extract the exerciseType from the intent extras
        val exerciseType = intent.getStringExtra("exerciseType")

        // Create the service intent with the exerciseType extra
        val serviceIntent =
          Intent(context, ExerciseLoggingService::class.java)
            .setAction(intent.action)
            .putExtra("exerciseType", exerciseType)

        Log.d(TAG, "Starting ExerciseLoggingService as ForegroundService")
        // Start the service
        context.startForegroundService(serviceIntent)
      }
      "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STOPPED" -> {
        val serviceIntent =
          Intent(context, ExerciseLoggingService::class.java).setAction(intent.action)

        Log.d(
          TAG,
          "Stopping ExerciseLoggingService as ForegroundService with action: ${intent.action}"
        )
        context.startForegroundService(serviceIntent)
      }
      else -> {
        Log.w(TAG, "Received unexpected action: ${intent.action}")
      }
    }
  }

  private fun startExerciseLoggingService(context: Context, intent: Intent) {
    // Create the service intent
    val serviceIntent = Intent(context, ExerciseLoggingService::class.java)
    // Start the service
    context.startForegroundService(serviceIntent)
  }

  private fun sendLogUriToSender(context: Context, logUri: Uri?) {
    Log.i(TAG, "sendLogUriToSender, logUri: ${logUri?.toString()}")

    logUri.let {
      val intent: Intent =
        Intent().apply {
          action = "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_LOGS"
          setPackage("${SENDER_PACKAGE_NAME}")
          putExtra("logFileUri", it!!.toString())
          type = "text/plain"
          flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
      Log.i(TAG, "sending intent: ${intent}")
      context.sendBroadcast(intent)
    }
  }
}
