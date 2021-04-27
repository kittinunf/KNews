package com.github.kittinunf.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

private class StoreAdapter<S : State, E : Environment>(private val store: Store<S, E>) : StoreType<S, E> by store {

    override fun addMiddleware(middleware: AnyMiddleware<S, E>) = error("Not supported yet")

    override fun removeMiddleware(middleware: AnyMiddleware<S, E>): Boolean = error("Not supported yet")
}

interface Identifiable {

    val identifier: String
        get() = this::class.qualifiedName!!
}

typealias EffectType<S, A, E> = Pair<String, Middleware<S, A, E>>

fun <S : State, A : Any> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducers: Map<String, Reducer<S, A>>
): StoreType<S, Nothing> {
    return StoreAdapter(Store(scope, initialState, engine = StoreAdapterEngine(reducers.toMutableMap(), mutableMapOf())))
}

fun <S : State, A : Any, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducers: Map<String, Reducer<S, A>>,
    middlewares: Map<String, Middleware<S, A, E>>
): StoreType<S, E> {
    return StoreAdapter(Store(scope, initialState, engine = StoreAdapterEngine(reducers.toMutableMap(), middlewares.toMutableMap())))
}

private class StoreAdapterEngine<S : State, A : Any, E : Environment>(
    val reducerMap: MutableMap<String, Reducer<S, A>>,
    val middlewareMap: MutableMap<String, Middleware<S, A, E>>
) : StateScannerEngine<S, E> {

    override suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S {
        val id = checkNotNull(action as? Identifiable)

        val middleware = middlewareMap[id.identifier]
        val reducer = reducerMap.getValue(id.identifier)

        val typedAction = action as? A

        return if (typedAction == null) state else {
            middleware?.process(Order.BeforeReduce, storeType, state, typedAction)
            val nextState = reducer(state, typedAction)
            middleware?.process(Order.AfterReduced, storeType, nextState, typedAction)
            nextState
        }
    }

    // as we are using reducerMap instead, this pointing to NoopReducer()
    override var reducer: AnyReducer<S> = NoopReducer()

    override val middlewares: MutableList<AnyMiddleware<S, E>>
        get() = TODO("Not yet implemented")
}
