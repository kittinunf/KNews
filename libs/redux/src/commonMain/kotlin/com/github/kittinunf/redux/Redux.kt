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

interface Action

interface State

interface Reducer<S : State, A : Action> {

    fun reduce(currentState: S, action: A): S
}

enum class Order {
    BeforeReducingState,
    AfterReducingState
}

interface Middleware<S : State, A : Action, E : Environment> {
    val environment: E

    fun process(order: Order, store: StoreType<S, A, E>, state: S, action: A) {}
}

interface Environment

interface NoEnvironment : Environment

interface StoreType<S : State, A : Action, E : Environment> {

    val states: StateFlow<S>

    val currentState: S

    var replaceReducer: (S, A) -> S

    suspend fun dispatch(action: A)

    fun tryDispatch(action: A): Boolean

    suspend fun dispatch(actions: Flow<A>)

    fun addMiddleware(middleware: Middleware<S, A, E>)

    fun removeMiddleware(middleware: Middleware<S, A, E>): Boolean
}

fun <S : State, A : Action> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S, A>,
): StoreType<S, A, NoEnvironment> {
    return Store(scope, initialState, reducer)
}

fun <S : State, A : Action, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S, A>,
    middleware: Middleware<S, A, E>
): StoreType<S, A, E> {
    return Store<S, A, E>(scope, initialState, reducer).apply { addMiddleware(middleware) }
}

fun <S : State, A : Action, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S, A>,
    vararg middlewares: Middleware<S, A, E>
): StoreType<S, A, E> {
    return Store<S, A, E>(scope, initialState, reducer).apply { middlewares.toList().forEach(::addMiddleware) }
}

class Store<S : State, A : Action, E : Environment> internal constructor(scope: CoroutineScope, initialState: S, reducer: Reducer<S, A>) : StoreType<S, A, E> {

    // seed action
    private object NoAction : Action

    private val _actions = MutableSharedFlow<A>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val states: StateFlow<S>
    override val currentState: S
        get() = states.value

    private val middlewares = mutableListOf<Middleware<S, A, E>>()

    // By default, this is doing nothing, just passing the reduced state
    override var replaceReducer: (S, A) -> S = { reducedState, _ -> reducedState }

    init {
        states = _actions
            .scan(initialState to NoAction as Action) { (state, _), action ->
                middlewares.onEach { it.process(Order.BeforeReducingState, this, state, action) }
                val reducedState = reducer.reduce(state, action)
                val nextState = replaceReducer(reducedState, action)
                nextState to action
            }
            .onEach { (nextState, latestAction) ->
                if (latestAction != NoAction) {
                    middlewares.onEach { @Suppress("UNCHECKED_CAST") it.process(Order.AfterReducingState, this, nextState, latestAction as A) }
                }
            }
            .map { it.first }
            .stateIn(scope, SharingStarted.Eagerly, initialState)
    }

    override suspend fun dispatch(action: A) {
        _actions.emit(action)
    }

    override fun tryDispatch(action: A): Boolean = _actions.tryEmit(action)

    override suspend fun dispatch(actions: Flow<A>) {
        actions.collect(_actions::emit)
    }

    override fun addMiddleware(middleware: Middleware<S, A, E>) {
        middlewares.add(middleware)
    }

    override fun removeMiddleware(middleware: Middleware<S, A, E>) = middlewares.remove(middleware)
}

fun <S : State, A : Action> combineReducers(reducers: List<Reducer<S, A>>): Reducer<S, A> = CompositeReducer(reducers)

private class CompositeReducer<S : State, A : Action>(private val reducers: List<Reducer<S, A>>) : Reducer<S, A> {

    override fun reduce(currentState: S, action: A): S = reducers.fold(currentState) { state, reducer ->
        reducer.reduce(state, action)
    }
}
