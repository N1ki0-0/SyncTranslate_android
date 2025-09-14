package com.example.synctranslate.AI.data.onnx

interface WhisperTokenizer {
    fun getInitialToken(): IntArray
    fun decode(tokenIds: List<Int>): String
}