package com.example.network

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FirebaseTransactionDto(
    val id: String,
    val amount: Double,
    val type: String,
    val category: String,
    val descriptionEncrypted: String,
    val timestamp: Long,
    val bankName: String
)

interface FirebaseApiService {
    @PUT("users/{userId}/transactions/{id}.json")
    suspend fun uploadTransaction(
        @Path("userId") userId: String,
        @Path("id") id: String,
        @Body transaction: FirebaseTransactionDto
    ): Response<FirebaseTransactionDto>

    @DELETE("users/{userId}/transactions/{id}.json")
    suspend fun deleteTransaction(
        @Path("userId") userId: String,
        @Path("id") id: String
    ): Response<ResponseBody>

    @GET("users/{userId}/transactions.json")
    suspend fun getAllTransactions(
        @Path("userId") userId: String
    ): Response<Map<String, FirebaseTransactionDto>?>
}

object FirebaseClient {
    private var currentBaseUrl = "https://uangku-aistudio-default-rtdb.firebaseio.com/"
    private var cachedService: FirebaseApiService? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getService(customUrl: String? = null): FirebaseApiService {
        val url = if (!customUrl.isNullOrEmpty()) {
            var formatted = customUrl.trim()
            if (!formatted.endsWith("/")) formatted += "/"
            if (!formatted.startsWith("http")) formatted = "https://$formatted"
            formatted
        } else {
            currentBaseUrl
        }

        if (cachedService != null && currentBaseUrl == url) {
            return cachedService!!
        }

        currentBaseUrl = url
        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(FirebaseApiService::class.java)
        cachedService = service
        return service
    }
}
