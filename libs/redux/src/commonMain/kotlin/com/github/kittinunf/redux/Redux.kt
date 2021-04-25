package com.github.kittinunf.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

interface State

typealias AnyReducer<S> = Reducer<S, Any>

fun interface Reducer<S : State, in A : Any> {

    fun reduce(currentState: S, action: A): S
}

class NoopReducer<S : State> : AnyReducer<S> {

    override fun reduce(currentState: S, action: Any): S = currentState
}

enum class Order {
    BeforeReduce,
    AfterReduced
}

typealias AnyMiddleware<S, E> = Middleware<S, Any, E>

interface Middleware<S : State, in A : Any, E : Environment> {

    val environment: E

    fun process(order: Order, store: StoreType<S, E>, state: S, action: A) {}
}

interface Environment

interface StoreType<S : State, E : Environment> {

    val states: StateFlow<S>

    val currentState: S

    suspend fun dispatch(action: Any)

    fun tryDispatch(action: Any): Boolean

    suspend fun dispatch(actions: Flow<Any>)

    fun addMiddleware(middleware: AnyMiddleware<S, E>)

    fun removeMiddleware(middleware: AnyMiddleware<S, E>): Boolean
}

fun <S : State> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: AnyReducer<S>,
): StoreType<S, Nothing> = Store(scope, initialState, DefaultEngine(reducer, mutableListOf()))

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: AnyReducer<S>,
    middleware: AnyMiddleware<S, E>
): StoreType<S, E> {
    return Store(scope, initialState, DefaultEngine(reducer, mutableListOf(middleware)))
}

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: AnyReducer<S>,
    vararg middlewares: AnyMiddleware<S, E>
): StoreType<S, E> {
    return Store(scope, initialState, DefaultEngine(reducer, middlewares.toMutableList()))
}

interface StateScannerEngine<S : State, E : Environment> {

    var reducer: AnyReducer<S>
    val middlewares: MutableList<AnyMiddleware<S, E>>

    suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S
}

private class DefaultEngine<S : State, E : Environment>(override var reducer: AnyReducer<S>, override val middlewares: MutableList<AnyMiddleware<S, E>>) :
    StateScannerEngine<S, E> {

    override suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S {
        middlewares.onEach { it.process(Order.BeforeReduce, storeType, state, action) }
        val nextState = reducer.reduce(state, action)
        middlewares.onEach { it.process(Order.AfterReduced, storeType, nextState, action) }
        return nextState
    }
}

class Store<S : State, E : Environment> internal constructor(scope: CoroutineScope, initialState: S, val engine: StateScannerEngine<S, E>) : StoreType<S, E> {

    companion object {
        const val defaultBufferCapacity = 16
    }

    // seed action
    private object NoAction

    private val _actions = MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = defaultBufferCapacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val states: StateFlow<S>

    override val currentState: S
        get() = states.value

    init {
        states = _actions.scan(initialState to NoAction as Any) { (state, _), action ->
            val nextState = engine.scan(this, state, action)
            nextState to action
        }.map { it.first }
        .stateIn(scope, SharingStarted.Eagerly, initialState)
    }

    override suspend fun dispatch(action: Any) {
        _actions.emit(action)
    }

    override fun tryDispatch(action: Any): Boolean = _actions.tryEmit(action)

    override suspend fun dispatch(actions: Flow<Any>) {
        actions.collect(_actions::emit)
    }

    override fun addMiddleware(middleware: AnyMiddleware<S, E>) {
        engine.middlewares.add(middleware)
    }

    override fun removeMiddleware(middleware: AnyMiddleware<S, E>): Boolean = engine.middlewares.remove(middleware)
}

fun <S : State> combineReducers(reducers: List<AnyReducer<S>>): AnyReducer<S> = CompositeReducer(reducers)

fun <S : State> combineReducers(vararg reducers: AnyReducer<S>): AnyReducer<S> = CompositeReducer(reducers.asList())

private class CompositeReducer<S : State>(private val reducers: List<AnyReducer<S>>) : AnyReducer<S> {

    override fun reduce(currentState: S, action: Any): S = reducers.fold(currentState) { state, reducer ->
        reducer.reduce(state, action)
    }
}
