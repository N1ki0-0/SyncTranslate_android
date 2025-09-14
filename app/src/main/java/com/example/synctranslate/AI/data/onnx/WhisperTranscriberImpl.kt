package com.example.synctranslate.AI.data.onnx

import android.content.Context
import ai.onnxruntime.*
import android.util.Log
import com.example.synctranslate.AI.data.audio.AudioPreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import javax.inject.Inject

class WhisperTranscriberImpl @Inject constructor(
    private val context: Context
) : WhisperTranscriber {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val sessionEncoder: OrtSession
    private val sessionDecoder: OrtSession
    private val sessionDecoderWithPast: OrtSession
    private val tokenizer = WhisperTokenizerImpl(context)

    init {
        val modelDir = File(context.filesDir, "whisper-tiny")
        sessionEncoder = ortEnv.createSession(File(modelDir, "encoder_model.onnx").absolutePath, OrtSession.SessionOptions())
        sessionDecoder = ortEnv.createSession(File(modelDir, "decoder_model.onnx").absolutePath, OrtSession.SessionOptions())
        sessionDecoderWithPast = ortEnv.createSession(File(modelDir, "decoder_with_past_model.onnx").absolutePath, OrtSession.SessionOptions())
    }

    override suspend fun transcribe(audioFilePath: String): String = withContext(Dispatchers.Default) {
        val preprocessor = AudioPreprocessor(context)
        val wavPath = preprocessor.convertToWav(audioFilePath)
        val audioFile = File(wavPath)
        val melSpectrogram = WhisperPreprocessing.audioToMel(audioFile)

        val inputTensor = OnnxTensor.createTensor(ortEnv, melSpectrogram)
        val inputName = sessionEncoder.inputNames.iterator().next()

        val encoderOutput: Array<Array<FloatArray>>
        sessionEncoder.run(mapOf(inputName to inputTensor)).use { encoderResult ->
            encoderOutput = (encoderResult[0] as OnnxTensor).value as Array<Array<FloatArray>>
        }

        val tokens = mutableListOf<Int>()
        val encodedPrompt = tokenizer.encode("translate Russian to English")
        var inputIds = encodedPrompt
        var pastKeyValues: List<OnnxValue>? = null
        var isFinished = false
        var step = 0

        tokens.addAll(encodedPrompt.toList())

        while (tokens.size < 50) {
            val inputTensorIds = OnnxTensor.createTensor(ortEnv, arrayOf(inputIds.map { it.toLong() }.toLongArray()))

            if (step == 0) {
                val encoderTensor = OnnxTensor.createTensor(ortEnv, encoderOutput)

                val inputs = mapOf(
                    "input_ids" to inputTensorIds,
                    "encoder_hidden_states" to encoderTensor
                )



                sessionDecoder.run(inputs).use { result ->
                    val logits = (result[0] as OnnxTensor).value as Array<Array<FloatArray>>
                    val nextToken = logits[0][0].indices.maxByOrNull { logits[0][0][it] } ?: 50258

                    if (nextToken == 50258) {
                        isFinished = true
                    } else {
                        tokens.add(nextToken)
                        inputIds = intArrayOf(nextToken)
                        pastKeyValues = (1 until result.size()).map { result[it] }
                    }
                }

                encoderTensor.close()
            } else {
                if (pastKeyValues != null && pastKeyValues!!.size == 16) {
                    val cachePositionTensor = OnnxTensor.createTensor(ortEnv, longArrayOf(step.toLong()))

                    val inputs = mutableMapOf<String, OnnxTensorLike>(
                        "input_ids" to inputTensorIds,
                        "cache_position" to cachePositionTensor
                    )

                    pastKeyValues!!.chunked(4).forEachIndexed { layer, tensors ->
                        inputs["past_key_values.${layer}.decoder.key"] = tensors[0] as OnnxTensor
                        inputs["past_key_values.${layer}.decoder.value"] = tensors[1] as OnnxTensor
                        inputs["past_key_values.${layer}.encoder.key"] = tensors[2] as OnnxTensor
                        inputs["past_key_values.${layer}.encoder.value"] = tensors[3] as OnnxTensor
                    }

                    sessionDecoderWithPast.run(inputs).use { result ->
                        val logits = (result[0] as OnnxTensor).value as Array<Array<FloatArray>>
                        val nextToken = logits[0][0].indices.maxByOrNull { logits[0][0][it] } ?: 50258

                        if (nextToken == 50258) {
                            isFinished = true
                        } else {
                            tokens.add(nextToken)
                            inputIds = intArrayOf(nextToken)
                            pastKeyValues = (1 until result.size()).map { result[it] }
                        }
                    }

                    cachePositionTensor.close()
                } else {
                    break
                }
            }

            inputTensorIds.close()
            step++
        }

        return@withContext tokenizer.decode(tokens)
    }
}