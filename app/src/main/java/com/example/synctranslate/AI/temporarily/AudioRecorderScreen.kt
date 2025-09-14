package com.example.synctranslate.AI.temporarily

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.synctranslate.R
import java.io.File
import java.io.FileOutputStream
import java.util.Vector

@Composable
fun AudioRecorderUI() {
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }

    Column {
        if (isRecording) {
            // Кнопки во время записи
            Row  {
                IconButton(
                    onClick = {
                        isPaused = !isPaused
                        mediaRecorder?.apply {
                            if (isPaused) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    pause()
                                }
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    resume()
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.pause),
                        contentDescription = "Pause",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(
                    onClick = {
                        stopRecording(mediaRecorder)
                        isRecording = false
                        mediaRecorder = null
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.stop),
                        contentDescription = "Stop",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp))
                }
            }
        } else {
            // Стартовая кнопка
            IconButton(
                onClick = {
                    mediaRecorder = startRecording(context)
                    isRecording = true
                }
            ) {
                 Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.mic),
                    contentDescription = "Record",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

private fun startRecording(context: Context): MediaRecorder {
    return MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        val outputFile = File(context.cacheDir, "temp_audio.mp4")
        setOutputFile(FileOutputStream(outputFile).fd)

        prepare()
        start()
    }
}

private fun stopRecording(mediaRecorder: MediaRecorder?) {
    mediaRecorder?.apply {
        stop()
        release()
    }


}

@Preview
@Composable
fun prev(){
    AudioRecorderUI()
}