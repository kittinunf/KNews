package com.github.kittinunf.hackernews.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun createHttpClient(clientConfig: HttpClientConfig<*>.() -> Unit): HttpClient
