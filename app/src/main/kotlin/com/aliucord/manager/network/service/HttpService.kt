package com.aliucord.manager.network.service

import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.network.utils.ApiError
import com.aliucord.manager.network.utils.ApiFailure
import com.aliucord.manager.network.utils.ApiResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HttpService(
    val json: Json,
    val http: HttpClient,
) {
    suspend inline fun <reified T> request(builder: HttpRequestBuilder.() -> Unit = {}): ApiResponse<T> {
        var body: String? = null

        val response = try {
            val response = http.request(builder)

            if (response.status.isSuccess()) {
                body = response.bodyAsText()

                ApiResponse.Success(json.decodeFromString<T>(body))
            } else {
                body = try {
                    response.bodyAsText()
                } catch (t: Throwable) {
                    null
                }

                Log.e(BuildConfig.TAG, "Failed to fetch, api error, http: ${response.status}, body: $body")
                ApiResponse.Error(ApiError(response.status, body))
            }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to fetch. body: $body", t)
            ApiResponse.Failure(ApiFailure(t, body))
        }
        return response
    }
}
