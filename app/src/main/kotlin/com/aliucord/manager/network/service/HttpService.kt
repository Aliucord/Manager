package com.aliucord.manager.network.service

import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.network.utils.*
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
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

                if (T::class == String::class) {
                    return ApiResponse.Success(body as T)
                }

                ApiResponse.Success(json.decodeFromString<T>(body))
            } else {
                body = try {
                    response.bodyAsText()
                } catch (t: Throwable) {
                    null
                }

                Log.e(BuildConfig.TAG, "Failed to fetch: API error, http status: ${response.status}, body: $body")
                ApiResponse.Error(ApiError(response.status, body))
            }
        } catch (t: Throwable) {
            Log.e(BuildConfig.TAG, "Failed to fetch: error: $t, body: $body")
            ApiResponse.Failure(ApiFailure(t, body))
        }
        return response
    }
}
