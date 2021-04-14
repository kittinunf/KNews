package com.github.kittinunf.hackernews.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.ios.Ios
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

actual fun createHttpClient(clientConfig: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Ios) {
    clientConfig(this)
    engine {
        handleChallenge { _, _, challenge, completionHandler ->
            if (challenge.protectionSpace.serverTrust == null) completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            else completionHandler(NSURLSessionAuthChallengeUseCredential, NSURLCredential.credentialForTrust(challenge.protectionSpace.serverTrust))
        }
    }
}
