package com.github.kittinunf.hackernews.api

import kotlinx.coroutines.CoroutineScope

expect open class NativeViewModel() {
    val scope: CoroutineScope

    fun cancel()
}
