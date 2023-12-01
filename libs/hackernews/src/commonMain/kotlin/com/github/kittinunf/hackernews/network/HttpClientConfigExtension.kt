package com.github.kittinunf.hackernews.network

import com.github.kittinunf.hackernews.model.hackerNewsSerializersModule
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun HttpClientConfig<*>.addBaseUrl(urlString: String) {
    install(DefaultRequest) {
        host = urlString
        headers {
            set(HttpHeaders.ContentType, ContentType.Application.Json.contentSubtype)
        }
    }
}

internal fun HttpClientConfig<*>.addHackerNewsJsonSerializer() {
    install(ContentNegotiation) {
        json(
            json = Json {
                serializersModule = hackerNewsSerializersModule
                ignoreUnknownKeys = true
            }
        )
    }
}

internal fun HttpClientConfig<*>.addLogging() {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}
