package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.redux.Identifiable

open class LoadAction<T>(val payload: T? = null) : Identifiable

open class ResultAction<A : Any, T>(val fromAction: A, val result: Result<T, Throwable>) : Identifiable {

    override val identifier: String = "ResultAction"
}
