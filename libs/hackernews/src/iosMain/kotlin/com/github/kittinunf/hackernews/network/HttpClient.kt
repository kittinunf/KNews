package com.github.kittinunf.hackernews.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

//@OptIn(ExperimentalForeignApi::class)
//actual fun createHttpClient(clientConfig: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) {
//    engine {
//        handleChallenge { _, _, challenge, completionHandler ->
//            if (challenge.protectionSpace.serverTrust == null) completionHandler(
//                NSURLSessionAuthChallengeCancelAuthenticationChallenge,
//                null
//            )
//            else completionHandler(
//                NSURLSessionAuthChallengeUseCredential,
//                NSURLCredential.credentialForTrust(challenge.protectionSpace.serverTrust)
//            )
//        }
//    }
//    clientConfig(this)
//}
