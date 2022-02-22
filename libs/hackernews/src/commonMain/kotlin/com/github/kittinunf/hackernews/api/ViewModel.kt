package com.github.kittinunf.hackernews.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

expect open class ViewModel() {

    open val scope: CoroutineScope
    val defaultDispatchers: CoroutineDispatcher

    fun cancel()
}
