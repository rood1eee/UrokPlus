package com.example.urokplus.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.urokplus.util.showUrokNotification

class HomeworkAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: return
        val subject = intent?.getStringExtra(EXTRA_SUBJECT).orEmpty()
        val text = if (subject.isNotBlank()) "$subject: $title" else title
        showUrokNotification(context, "Срок задания", text)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUBJECT = "subject"
    }
}
