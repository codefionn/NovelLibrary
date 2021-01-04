package io.github.gmathi.novellibrary.util

import android.util.Log
import io.github.gmathi.novellibrary.BuildConfig
import java.lang.Exception

object Logs {

    fun debug(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(tag ?: "", message ?: "")
        }
    }

    fun info(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.i(tag ?: "", message ?: "")
        }
    }

    fun warning(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.w(tag ?: "", message ?: "")
        } else {
            logToFirebase("W", tag, message)
        }
    }

    fun warning(tag: String?, message: String?, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        } else {
            logToFirebase("W", tag, message, throwable)
        }
    }

    fun error(tag: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message ?: "")
        } else {
            logToFirebase("E", tag, message)
        }
    }

    fun error(tag: String?, message: String?, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        } else {
            logToFirebase("E", tag, message)
        }
    }

    private fun logToFirebase(logLevel: String, tag: String?, message: String?, throwable: Throwable? = null) {
    }

}