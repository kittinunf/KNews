package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.hackernews.util.Result

open class ResultAction<T : Any?, E : Throwable>(val result: Result<T, E>)
