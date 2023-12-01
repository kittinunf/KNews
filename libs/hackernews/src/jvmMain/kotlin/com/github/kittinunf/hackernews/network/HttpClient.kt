package com.github.kittinunf.hackernews.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

actual fun createHttpClient(clientConfig: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp) {
    val builder = OkHttpClient.Builder()
    engine {
        preconfigured = builder.build()
    }
    clientConfig(this)
}
