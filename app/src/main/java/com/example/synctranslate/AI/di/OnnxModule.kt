package com.example.synctranslate.AI.di

import android.content.Context
import com.example.synctranslate.AI.data.onnx.WhisperTranscriber
import com.example.synctranslate.AI.data.onnx.WhisperTranscriberImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnnxModule {

    @Provides
    @Singleton
    fun provideWhisperTranscriber(
        @ApplicationContext context: Context
    ): WhisperTranscriber {
        return WhisperTranscriberImpl(context)
    }
}