package com.example.synctranslate.AI.data.audio

import android.media.MediaPlayer
import java.io.IOException
import javax.inject.Inject

class AudioPlayerImpl @Inject constructor() : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    override fun play(filePath: String) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
    }

    override fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}