package com.github.kittinunf.hackernews.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> runBlockingTest(block: suspend CoroutineScope.() -> T): T = runBlocking { block() }
