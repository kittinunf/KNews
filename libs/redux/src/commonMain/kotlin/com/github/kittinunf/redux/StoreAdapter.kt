package com.github.kittinunf.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

private class StoreAdapter<S : State, E : Environment>(private val store: Store<S, E>) : StoreType<S, E> by store {

    override fun addMiddleware(middleware: Middleware<S, E>) = error("Not implemented")

    override fun removeMiddleware(middleware: Middleware<S, E>): Boolean = error("Not implemented")
}

interface Identifiable {

    val identifier: String
        get() = this::class.qualifiedName!!
}

fun interface StrictReducer<S : State, in A : Any> {

    fun reduce(currentState: S, action: A): S
}

interface StrictMiddleware<S : State, in A: Any, E : Environment> {

    fun process(order: Order, store: StoreType<S, E>, state: S, action: A) {}
}

fun <S : State, A: Any> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducers: Map<String, StrictReducer<S, A>>
): StoreType<S, Nothing> {
    return StoreAdapter(Store(scope, initialState, scanner = StoreAdapterStateScanner(reducers.toMutableMap(), mutableMapOf())))
}

fun <S : State, A: Any, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducers: Map<String, StrictReducer<S, A>>,
    middlewares: Map<String, StrictMiddleware<S, A, E>>
): StoreType<S, E> {
    return StoreAdapter(Store(scope, initialState, scanner = StoreAdapterStateScanner(reducers.toMutableMap(), middlewares.toMutableMap())))
}

private class StoreAdapterStateScanner<S : State, A: Any, E : Environment>(
    val reducerMap: MutableMap<String, StrictReducer<S, A>>,
    val middlewareMap: MutableMap<String, StrictMiddleware<S, A, E>>
) : StateScanner<S, E> {

    override suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S {
        val id = checkNotNull(action as? Identifiable)
        val action = checkNotNull(action as? A)

        val middleware = middlewareMap[id.identifier]
        val reducer = reducerMap.getValue(id.identifier)

        middleware?.process(Order.Before, storeType, state, action)
        val nextState = reducer.reduce(state, action)
        middleware?.process(Order.After, storeType, nextState, action)
        return nextState
    }
}
