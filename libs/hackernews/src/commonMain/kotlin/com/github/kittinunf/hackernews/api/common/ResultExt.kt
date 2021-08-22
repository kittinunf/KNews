package com.github.kittinunf.hackernews.api.common

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.result.Result

fun <V, E : Throwable> Result<V, E>.toData(): Data<V, E> = when (this) {
    is Result.Success -> Data.Success(value)
    is Result.Failure -> Data.Failure(error)
}
