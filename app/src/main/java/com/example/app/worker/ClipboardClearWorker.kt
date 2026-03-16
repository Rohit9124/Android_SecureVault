package com.example.app.worker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ClipboardClearWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            val clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            // On Android 13+ (API 33), apps cannot clear the clipboard unless they are the default handler
            // or if they own the content. However, calling clearPrimaryClip() is deprecated/restricted.
            // But requirement says "Clear clipboard using ClipboardManager".
            // We will try to clear it. For older versions it works fine.
            // For newer versions, we might need to overwrite with empty data if clear doesn't work,
            // but strict clearing might be limited by OS.
            // We will stick to standard clearPrimaryClip().
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                // For older versions, setting null or empty clip
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            
            Log.d(TAG, "Clipboard cleared successfully by Worker")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing clipboard", e)
            return Result.failure()
        }
    }

    companion object {
        private const val TAG = "ClipboardClearWorker"
    }
}
