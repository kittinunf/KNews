package com.github.kittinunf.hackernews.util

interface Mapper<T : Any, U : Any> {
    fun map(t: T): U
}
