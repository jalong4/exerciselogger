package com.tcxplot.exerciselogger

import android.app.Activity
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View

class ExerciseLoggingActivity : Activity() {
  companion object {
    private const val TAG = "ExerciseLoggingActivity"
  }

  private lateinit var receiver: ExerciseLoggingReceiver

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(View(this))
    Log.d(TAG, "onCreate")
    registerExerciseLoggingReceiver()
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(receiver)
  }

  private fun registerExerciseLoggingReceiver() {
    // Initialize the receiver
    receiver = ExerciseLoggingReceiver()

    // Register the receiver
    val intentFilter =
      IntentFilter().apply {
        addAction("${FileSender.SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STARTED")
        addAction("${FileSender.SENDER_PACKAGE_NAME}.WEAR_EXERCISE_STOPPED")
      }
    registerReceiver(receiver, intentFilter)
  }
}
