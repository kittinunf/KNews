package com.github.kittinunf.hackernews.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

actual open class NativeViewModel : ViewModel() {
    actual val scope: CoroutineScope = viewModelScope

    actual fun cancel() {
        // do nothing for Android implementation as it is already covered by base viewmodel from lifecycle's library
    }
}
