package com.example.urokplus.util

import android.util.Log

object AppLogger {
    fun d(tag: String, msg: String) { Log.d(tag, msg) }
    fun i(tag: String, msg: String) { Log.i(tag, msg) }
    fun w(tag: String, msg: String) { Log.w(tag, msg) }
    fun e(tag: String, msg: String, t: Throwable? = null) { Log.e(tag, msg, t) }
}
