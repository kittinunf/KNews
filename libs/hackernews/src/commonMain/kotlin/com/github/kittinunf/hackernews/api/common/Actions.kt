package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.hackernews.util.Result

open class LoadAction<T>(val payload: T? = null)
open class ResultAction<A : Any, T>(val fromAction: A, val result: Result<T, Throwable>)
