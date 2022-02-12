package com.github.kittinunf.hackernews.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.addTimeInterval
import platform.Foundation.performBlock
import platform.Foundation.runUntilDate
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
actual fun <T> runBlockingTest(block: suspend CoroutineScope.() -> T): T {
    val expectation = Expectation<T>()

    GlobalScope.launch(MainRunLoopDispatcher) {
        expectation.fulfill(block())
    }

    return expectation.wait() ?: throw RuntimeException("runBlocking failed")
}

private object MainRunLoopDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        NSRunLoop.mainRunLoop().performBlock { block.run() }
    }
}

class Expectation<T> {
    private var waiting = true
    private var result: T? = null

    fun fulfill(result: T?) {
        waiting = false
        this.result = result
    }

    fun wait(): T? {
        while (waiting) {
            advanceRunLoop()
        }

        return result
    }
}

private fun advanceRunLoop() {
    val date = NSDate().addTimeInterval(1.0) as NSDate
    NSRunLoop.mainRunLoop.runUntilDate(date)
}
