package com.github.kittinunf.hackernews.util

fun interface Mapper<T : Any, U : Any> {
    operator fun invoke(t: T): U
}
