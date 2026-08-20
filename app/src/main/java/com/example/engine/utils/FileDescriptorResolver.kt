package com.example.engine.utils

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * Utility to safely open and manage Android ParcelFileDescriptors from file paths or content URIs,
 * providing zero-copy file descriptors for mmap native engines.
 */
object FileDescriptorResolver {
  private const val TAG = "FileDescriptorResolver"

  /**
   * Opens a read-only ParcelFileDescriptor from a given file path or content URI.
   */
  fun openReadOnlyDescriptor(uriOrPath: String, context: Context?): ParcelFileDescriptor? {
    if (uriOrPath.isBlank()) return null

    return try {
      if (uriOrPath.startsWith("content://") && context != null) {
        context.contentResolver.openFileDescriptor(Uri.parse(uriOrPath), "r")
      } else {
        val file = java.io.File(uriOrPath)
        if (file.exists() && file.canRead()) {
          ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
          null
        }
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Error opening descriptor for: $uriOrPath", e)
      null
    }
  }
}
