package com.example.synctranslate.AI.data.audio

interface AudioPlayer {
    fun play(filePath: String)
    fun stop()
    fun isPlaying(): Boolean
}