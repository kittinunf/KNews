package com.github.kittinunf.hackernews.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual open class NativeViewModel {

    private val job = SupervisorJob()
    actual val scope: CoroutineScope = CoroutineScope(job + Dispatchers.Main)

    actual fun cancel() {
        job.cancel()
    }
}
