package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.result.Result

open class ResultAction<T : Any?, E : Throwable>(val result: Result<T, E>)
