package com.github.kittinunf.hackernews.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual open class NativeViewModel actual constructor() {

    private val job = SupervisorJob()
    actual val scope: CoroutineScope = CoroutineScope(job + Dispatchers.Main)
    actual val defaultDispatchers: CoroutineDispatcher = Dispatchers.Main

    actual fun cancel() {
        job.cancel()
    }
}
