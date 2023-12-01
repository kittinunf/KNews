package com.github.kittinunf.hackernews.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual open class ViewModel {

    actual open val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    actual val defaultDispatchers: CoroutineDispatcher = Dispatchers.Default

    actual fun cancel() {
        scope.cancel()
    }
}
