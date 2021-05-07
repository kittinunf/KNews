package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.cored.Identifiable

abstract class LoadAction<T>(val payload: T? = null) : Identifiable

open class ResultAction<A : Any, T>(val fromAction: A, val result: Result<T, Throwable>) : Identifiable {

    override val identifier: String = "ResultAction"
}
