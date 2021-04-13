package com.github.kittinunf.hackernews.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(clientConfig: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp) {
    clientConfig(this)
}
