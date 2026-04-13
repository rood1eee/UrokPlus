package com.example.urokplus

import android.content.Context
import android.media.MediaPlayer

class AudioPlayer {
    private var player: MediaPlayer? = null

    fun playFile(context: Context, path: String) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener { stop() }
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
}
