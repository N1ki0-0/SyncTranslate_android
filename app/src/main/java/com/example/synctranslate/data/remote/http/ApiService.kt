package com.example.synctranslate.data.remote.http

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/train/voice")
    suspend fun uploadSetupAudio(@Part file: MultipartBody.Part): Response<Unit>
}
