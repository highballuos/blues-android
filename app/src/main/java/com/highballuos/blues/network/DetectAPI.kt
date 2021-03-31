package com.highballuos.blues.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DetectAPI {
    @Headers("Content-Type: application/json")
    @POST("v1/models/model:predict")   // API 상세 경로, format : json
    suspend fun getValues(
        @Body body: HashMap<String, List<String>>
    ): Response<Result>

    companion object {
        // Header에 들어갈 정보 설정
        private const val BASE_URL_DEMO_API = "https://gsc.oasisfores.com/"    // API 기본 경로

        fun create(): DetectAPI {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            val headerInterceptor = Interceptor {
                val request = it.request()
                    .newBuilder()
                    .build()
                return@Interceptor it.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL_DEMO_API)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DetectAPI::class.java)
        }
    }
}