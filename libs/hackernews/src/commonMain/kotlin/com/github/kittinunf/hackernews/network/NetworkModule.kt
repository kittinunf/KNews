package com.github.kittinunf.hackernews.network

import com.github.kittinunf.hackernews.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType

class NetworkModule(val client: HttpClient) {

    suspend inline fun <reified T> get(
        endpoint: String,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, List<String>> = emptyMap()
    ): Result<T, Throwable> =
        try {
            val value = client.get<T> {
                url.encodedPath = endpoint
                params.forEach { entry ->
                    parameter(entry.key, entry.value.toString())
                }
                headers.forEach { entry ->
                    header(entry.key, entry.value)
                }
            }
            Result.success(value)
        } catch (t: Throwable) {
            Result.error(t)
        }

    suspend inline fun <R : Any, reified T> post(
        endpoint: String,
        body: R,
        contentType: String = ContentType.Application.Json.toString(),
        headers: Map<String, List<String>> = emptyMap()
    ): Result<T, Throwable> =
        try {
            val value = client.post<T> {
                url.encodedPath = endpoint
                this.body = body
                contentType(ContentType.parse(contentType))
                headers.forEach { entry ->
                    header(entry.key, entry.value)
                }
            }
            Result.success(value)
        } catch (t: Throwable) {
            Result.error(t)
        }
}
