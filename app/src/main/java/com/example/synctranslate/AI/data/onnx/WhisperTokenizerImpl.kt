package com.example.synctranslate.AI.data.onnx

import android.content.Context
import org.json.JSONObject
import java.io.File

class WhisperTokenizerImpl(context: Context) : WhisperTokenizer {

    private val vocab: Map<String, Int>
    private val merges: List<Pair<String, String>>
    private val bpeRanks: Map<Pair<String, String>, Int>
    private val cache = mutableMapOf<String, List<String>>()

    init {
        val vocabFile = File(context.filesDir, "whisper-tiny/vocab.json")
        val mergesFile = File(context.filesDir, "whisper-tiny/merges.txt")

        vocab = JSONObject(vocabFile.readText()).let { json ->
            json.keys().asSequence().associateWith { json.getInt(it) }
        }

        merges = mergesFile.readLines()
            .drop(1) // первая строка — версия
            .map { it.split(" ") }
            .filter { it.size == 2 }
            .map { it[0] to it[1] }

        bpeRanks = merges.withIndex().associate { it.value to it.index }
    }

    override fun getInitialToken(): IntArray = intArrayOf(vocab["<|startoftranscript|>"] ?: 50257)

    fun encode(text: String): IntArray {
        val words = text.trim().split(" ")
        val tokens = mutableListOf<Int>()

        tokens.addAll(getInitialToken().toList())

        for (word in words) {
            val pieces = bpe(word)
            pieces.forEach {
                vocab[it]?.let { id -> tokens.add(id) }
            }
        }

        return tokens.toIntArray()
    }

    private fun bpe(token: String): List<String> {
        if (cache.containsKey(token)) return cache[token]!!

        var word = token.toCharArray().map { it.toString() }.toMutableList()
        var pairs = getPairs(word)

        while (true) {
            val bigram = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (!bpeRanks.containsKey(bigram)) break

            val (first, second) = bigram
            val newWord = mutableListOf<String>()
            var i = 0

            while (i < word.size) {
                val j = word.indexOfFirst { it == first && i < word.lastIndex && word[i + 1] == second }
                if (j == -1) {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }
                newWord.addAll(word.subList(i, j))
                newWord.add(first + second)
                i = j + 2
            }

            word = newWord
            pairs = getPairs(word)
        }

        cache[token] = word
        return word
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until word.size - 1) {
            pairs.add(word[i] to word[i + 1])
        }
        return pairs
    }

    override fun decode(tokenIds: List<Int>): String {
        val reverseVocab = vocab.entries.associateBy({ it.value }, { it.key })
        return tokenIds.mapNotNull { reverseVocab[it] }
            .filterNot { it.startsWith("<|") } // убираем спец. токены
            .joinToString(" ")
    }
}