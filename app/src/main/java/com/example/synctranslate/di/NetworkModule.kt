package com.example.synctranslate.di

import com.example.synctranslate.data.remote.http.ApiConfig
import com.example.synctranslate.data.remote.http.ApiService
import com.example.synctranslate.data.remote.webrtc.SignalingClient
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson, apiConfig: ApiConfig): Retrofit {
        return Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides @Singleton
    fun provideSignalingClient(okHttpClient: OkHttpClient, gson: Gson): SignalingClient {
        return SignalingClient(okHttpClient, gson)
    }
}