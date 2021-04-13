package com.github.kittinunf.hackernews.util

import kotlinx.coroutines.CoroutineScope

expect fun <T> runBlockingTest(block: suspend CoroutineScope.() -> T): T
