package com.github.kittinunf.hackernews.network

import com.github.kittinunf.hackernews.model.hackerNewsSerializersModule
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.Json
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json as KotlinxSerializationJson

internal fun HttpClientConfig<*>.addBaseUrl(urlString: String) {
    defaultRequest {
        URLBuilder(urlString).run {
            url.protocol = protocol
            url.host = host
        }
    }
}

internal fun HttpClientConfig<*>.addHackerNewsJsonSerializer() {
    Json {
        serializer = KotlinxSerializer(KotlinxSerializationJson {
            serializersModule = hackerNewsSerializersModule
            ignoreUnknownKeys = true
        })
    }
}

internal fun HttpClientConfig<*>.addLogging() {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}
