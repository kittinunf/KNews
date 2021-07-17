//package com.github.kittinunf.hackernews.util
//
//sealed class Result<out V : Any?, out E : Throwable> {
//
//    open operator fun component1(): V? = null
//    open operator fun component2(): E? = null
//
//    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X = when (this) {
//        is Success -> success(value)
//        is Failure -> failure(error)
//    }
//
//    abstract fun get(): V
//
//    class Success<out V : Any?>(val value: V) : Result<V, Nothing>() {
//
//        override fun component1(): V? = value
//
//        override fun get(): V = value
//
//        override fun toString() = "[Success: $value]"
//
//        override fun hashCode(): Int = value.hashCode()
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            return other is Success<*> && value == other.value
//        }
//    }
//
//    class Failure<out E : Throwable>(val error: E) : Result<Nothing, E>() {
//
//        override fun component2(): E = error
//
//        override fun get() = throw error
//
//        override fun toString() = "[Failure: $error]"
//
//        override fun hashCode(): Int = error.hashCode()
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            return other is Failure<*> && error == other.error
//        }
//    }
//
//    companion object {
//        // Factory methods
//        fun <E : Throwable> error(ex: E) = Failure(ex)
//        fun <V> success(v: V) = Success(v)
//
//        fun <V, E : Throwable> of(f: () -> V?): Result<V, E> = try {
//            success(f()) as Result<V, E>
//        } catch (e: Throwable) {
//            @Suppress("UNCHECKED_CAST")
//            error(e as E)
//        }
//    }
//}
//
//inline fun <V> Result<V, *>.success(f: (V) -> Unit) = fold(f, {})
//
//inline fun <E : Throwable> Result<*, E>.failure(f: (E) -> Unit) = fold({}, f)
//
//fun Result<*, *>.isSuccess() = this is Result.Success
//
//fun Result<*, *>.isFailure() = this is Result.Failure
//
//inline fun <T : Any?, U : Any?, reified E : Throwable> Result<T, *>.map(transform: (T) -> U): Result<U, *> = try {
//    when (this) {
//        is Result.Success -> Result.Success(transform(value))
//        is Result.Failure -> Result.Failure(error)
//    }
//} catch (ex: Exception) {
//    when (ex) {
//        is E -> Result.error(ex)
//        else -> throw ex
//    }
//}
//
//inline fun <reified E : Throwable, reified EE : Throwable> Result<*, E>.mapError(transform: (E) -> EE): Result<*, EE> =
//    when (this) {
//        is Result.Success -> Result.Success(value)
//        is Result.Failure -> Result.Failure(transform(error))
//    }
//
//
//inline fun <V : Any?, U : Any?, reified E : Throwable> Result<V, E>.flatMap(transform: (V) -> Result<U, E>): Result<U, E> = try {
//    when (this) {
//        is Result.Success -> transform(value)
//        is Result.Failure -> Result.Failure(error)
//    }
//} catch (ex: Exception) {
//    when (ex) {
//        is E -> Result.error(ex)
//        else -> throw ex
//    }
//}
//
//inline fun <V : Any?, reified E : Throwable> List<Result<V, E>>.lift(): Result<List<V>, E> {
//    return fold(Result.success(mutableListOf<V>()) as Result<MutableList<V>, E>) { acc, result ->
//        acc.flatMap { combine ->
//            result.map<V, MutableList<V>, E> {
//                combine.apply { add(it) }
//            } as Result<MutableList<V>, E>
//        }
//    }
//}
