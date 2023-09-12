package com.tcxplot.exerciselogger

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest

class FileSender(private val file: File) {

  companion object {
    private const val TAG = "FileSender"
    const val SENDER_PACKAGE_NAME = "com.tcxplot.tcxfitness"
    private const val CHUNK_SIZE = 64 * 1024 // 64 KB chunk size

    private fun sendFileChunk(
      context: Context,
      chunk: ByteArray,
      filename: String,
      chunkChecksum: String,
      chunkNumber: Long,
      filesize: Long,
      fileChecksum: String,
      isTransferComplete: Boolean
    ) {
      val intent: Intent =
        Intent().apply {
          action = "${SENDER_PACKAGE_NAME}.WEAR_EXERCISE_LOGS"
          setPackage("${SENDER_PACKAGE_NAME}")
          putExtra("filename", filename)
          putExtra("chunk", chunk)
          putExtra("chunkNumber", chunkNumber)
          putExtra("chunkChecksum", chunkChecksum)
          putExtra("fileChecksum", fileChecksum)
          putExtra("fileSize", filesize)
          putExtra("isTransferComplete", isTransferComplete)
        }

      Log.i(TAG, "sending intent: ${intent}")
      context.sendBroadcast(intent)
    }

    fun sendLogDataToSender(logFile: File?, context: Context) {
      Log.i(TAG, "sendLogUriToSender")

      logFile?.let { file ->
        val chunkProcessor = FileSender(file)
        chunkProcessor.resetCurrentChunk() // Reset the chunk position to start from the beginning

        var chunkNumber = 0L
        var chunk = chunkProcessor.processNextChunk()
        while (chunk != null) {
          // Debug log to check if the block is being entered
          Log.d(TAG, "Processing chunk number: $chunkNumber")
          val chunkChecksum = chunkProcessor.getChunkChecksum()
          val filesize = chunkProcessor.getFileSize()
          val fileChecksum = chunkProcessor.getFileChecksum()
          val isTransferComplete = chunkProcessor.isTransferComplete()

          Log.d(TAG, "FileChunk extras:")
          Log.d(TAG, "Filename: $file.name")
          Log.d(TAG, "Chunk Number: $chunkNumber")
          Log.d(TAG, "Chunk Checksum: $chunkChecksum")
          Log.d(TAG, "File Checksum: $fileChecksum")
          Log.d(TAG, "File Size: $filesize")
          Log.d(TAG, "Is Transfer Complete: $isTransferComplete")
          sendFileChunk(
            context = context,
            chunk = chunk,
            filename = file.name,
            chunkChecksum = chunkChecksum,
            chunkNumber = chunkNumber,
            filesize = filesize,
            fileChecksum = fileChecksum,
            isTransferComplete = isTransferComplete
          )

          chunkNumber++
          chunk = chunkProcessor.processNextChunk()
        }
      }
    }

  }

  private var currentChunk = 0
  private var fileChecksum = ""
  private var currentChunkChecksum = ""
  private var chunkChecksums = mutableListOf<String>()
  private val fileInputStream: FileInputStream = FileInputStream(file)

  fun getFileSize(): Long {
    return file.length()
  }

  fun resetCurrentChunk() {
    currentChunk = 0
    fileChecksum = ""
    chunkChecksums.clear()
  }

  fun getNextChunk(): ByteArray? {

    val chunk = ByteArray(CHUNK_SIZE)
    val bytesRead = fileInputStream.read(chunk)

    if (bytesRead == -1) {
      return null // Reached end of file
    }

    currentChunk++
    return chunk.copyOf(bytesRead)
  }

  fun calculateChecksum(chunk: ByteArray, bytesRead: Int): String {
    val md5Digest = MessageDigest.getInstance("MD5")
    md5Digest.update(chunk, 0, bytesRead)
    val md5Checksum = md5Digest.digest()
    return bytesToHex(md5Checksum)
  }

  fun getChunkChecksum(): String {
    return currentChunkChecksum
  }

  fun getFileChecksum(): String {
    return fileChecksum
  }

  private fun bytesToHex(bytes: ByteArray): String {
    val bigInteger = BigInteger(1, bytes)
    return bigInteger.toString(16).padStart(bytes.size * 2, '0')
  }

  fun generateFileChecksum(checksums: List<String>): String {
    val concatenatedChecksums = checksums.joinToString("")
    val md5Digest = MessageDigest.getInstance("MD5")
    val md5Checksum = md5Digest.digest(concatenatedChecksums.toByteArray())
    return bytesToHex(md5Checksum)
  }

  fun isTransferComplete(): Boolean {
    val totalChunks = Math.ceil(file.length() / CHUNK_SIZE.toDouble()).toInt()
    return currentChunk == totalChunks
  }

  fun processNextChunk(): ByteArray? {
    currentChunkChecksum = ""
    val chunk = getNextChunk()
    if (chunk != null) {
      Log.d(TAG, "Updating checksums")
      currentChunkChecksum = calculateChecksum(chunk, chunk.size)
      chunkChecksums.add(currentChunkChecksum)
      fileChecksum = generateFileChecksum(chunkChecksums)
    }
    return chunk
  }
}
