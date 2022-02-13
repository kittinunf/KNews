package com.github.kittinunf.hackernews.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

actual open class NativeViewModel : ViewModel() {

    actual val scope: CoroutineScope = viewModelScope
    actual val defaultDispatchers: CoroutineDispatcher = Dispatchers.IO

    actual fun cancel() {
        // do nothing for Android implementation as it is already covered by base viewmodel from lifecycle's library
    }
}
