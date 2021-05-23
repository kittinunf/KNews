package com.github.kittinunf.hackernews.api

sealed class Data<out V : Any?, out E : Any> {

    open operator fun component1(): V? = null
    open operator fun component2(): E? = null

    open fun get(): V? = null
    open fun getOrNull(): V? = null

    object Initial : Data<Nothing, Nothing>(), Incomplete

    class Loading<out V>(val value: V? = null) : Data<V, Nothing>(), Incomplete {

        override fun get(): V? = value
        override fun getOrNull(): V? = value
    }

    class Success<out V>(val value: V?) : Data<V, Nothing>(), Complete {

        override fun component1(): V? = value

        override fun get(): V? = value
        override fun getOrNull(): V? = value

        override fun toString() = "[Success: $value]"
    }

    class Failure<out E : Any>(val error: E) : Data<Nothing, E>(), Complete {

        override fun component2(): E = error

        override fun get() = throw IllegalStateException(toString())
        override fun getOrNull() = null

        override fun toString(): String = "[Failure: $error]"
    }

    interface Complete

    interface Incomplete

    val isInitial
        get() = this is Initial

    val isLoading
        get() = this is Loading

    val isSuccess
        get() = this is Success

    val isFailure
        get() = this is Failure

    val isComplete
        get() = this is Complete

    val isIncomplete
        get() = this is Incomplete
}

fun <V : Any?, U : Any?, E : Any> Data<V, E>.map(transform: (V?) -> U): Data<U, E> = mapBoth(transform, { it })

fun <V : Any?, E : Any, EE : Any> Data<V, E>.mapError(transform: (E) -> EE): Data<V, EE> = mapBoth({ it }, transform)

private inline fun <V : Any?, E : Any, U : Any?, EE : Any> Data<V, E>.mapBoth(
    transform: (V?) -> U?,
    transformError: (E) -> EE
): Data<U, EE> =
    when (this) {
        Data.Initial -> Data.Initial
        is Data.Loading -> Data.Loading(transform(value))
        is Data.Success -> Data.Success(transform(value))
        is Data.Failure -> Data.Failure(transformError(error))
    }
