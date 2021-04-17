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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

interface State

interface Reducer<S : State> {

    fun reduce(currentState: S, action: Any): S
}

enum class Order {
    BeforeReducingState,
    AfterReducingState
}

interface Middleware<S : State, E : Environment> {
    val environment: E

    fun process(order: Order, store: StoreType<S, E>, state: S, action: Any) {}
}

interface Environment

interface NoEnvironment : Environment

interface StoreType<S : State, E : Environment> {

    val states: StateFlow<S>

    val currentState: S

    var replaceReducer: (S, Any) -> S

    suspend fun dispatch(action: Any)

    fun tryDispatch(action: Any): Boolean

    suspend fun dispatch(actions: Flow<Any>)

    fun addMiddleware(middleware: Middleware<S, E>)

    fun removeMiddleware(middleware: Middleware<S, E>): Boolean
}

fun <S : State> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S>,
): StoreType<S, NoEnvironment> {
    return Store(scope, initialState, reducer)
}

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S>,
    middleware: Middleware<S, E>
): StoreType<S, E> {
    return Store<S, E>(scope, initialState, reducer).apply { addMiddleware(middleware) }
}

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S>,
    vararg middlewares: Middleware<S, E>
): StoreType<S, E> {
    return Store<S, E>(scope, initialState, reducer).apply { middlewares.toList().forEach(::addMiddleware) }
}

class Store<S : State, E : Environment> internal constructor(scope: CoroutineScope, initialState: S, reducer: Reducer<S>) : StoreType<S, E> {

    // seed action
    private object NoAction

    private val _actions = MutableSharedFlow<Any>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val states: StateFlow<S>
    override val currentState: S
        get() = states.value

    private val middlewares = mutableListOf<Middleware<S, E>>()

    // By default, this is doing nothing, just passing the reduced state
    override var replaceReducer: (S, Any) -> S = { reducedState, _ -> reducedState }

    init {
        states = _actions
            .scan(initialState to NoAction as Any) { (state, _), action ->
                middlewares.onEach { it.process(Order.BeforeReducingState, this, state, action) }
                val reducedState = reducer.reduce(state, action)
                val nextState = replaceReducer(reducedState, action)
                nextState to action
            }
            .onEach { (nextState, latestAction) ->
                if (latestAction != NoAction) {
                    middlewares.onEach { @Suppress("UNCHECKED_CAST") it.process(Order.AfterReducingState, this, nextState, latestAction) }
                }
            }
            .map { it.first }
            .stateIn(scope, SharingStarted.Eagerly, initialState)
    }

    override suspend fun dispatch(action: Any) {
        _actions.emit(action)
    }

    override fun tryDispatch(action: Any): Boolean = _actions.tryEmit(action)

    override suspend fun dispatch(actions: Flow<Any>) {
        actions.collect(_actions::emit)
    }

    override fun addMiddleware(middleware: Middleware<S, E>) {
        middlewares.add(middleware)
    }

    override fun removeMiddleware(middleware: Middleware<S, E>) = middlewares.remove(middleware)
}

fun <S : State> combineReducers(vararg reducers: Reducer<S>): Reducer<S> = CompositeReducer(reducers.asList())

private class CompositeReducer<S : State>(private val reducers: List<Reducer<S>>) : Reducer<S> {

    override fun reduce(currentState: S, action: Any): S = reducers.fold(currentState) { state, reducer ->
        reducer.reduce(state, action)
    }
}
