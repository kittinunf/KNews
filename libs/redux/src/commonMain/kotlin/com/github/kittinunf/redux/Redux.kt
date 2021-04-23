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

fun interface Reducer<S : State> {

    fun reduce(currentState: S, action: Any): S
}

class NoopReducer<S : State> : Reducer<S> {

    override fun reduce(currentState: S, action: Any): S = currentState
}

enum class Order {
    Before,
    After
}

interface Middleware<S : State, E : Environment> {
    val environment: E

    fun process(order: Order, store: StoreType<S, E>, state: S, action: Any) {}
}

interface Environment

interface StoreType<S : State, E : Environment> {

    val states: StateFlow<S>

    val currentState: S

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
): StoreType<S, Nothing> = Store(scope, initialState, reducer, mutableListOf())

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S>,
    middleware: Middleware<S, E>
): StoreType<S, E> {
    return Store(scope, initialState, reducer, mutableListOf(middleware))
}

fun <S : State, E : Environment> createStore(
    scope: CoroutineScope = GlobalScope,
    initialState: S,
    reducer: Reducer<S>,
    vararg middlewares: Middleware<S, E>
): StoreType<S, E> {
    return Store(scope, initialState, reducer, middlewares.toMutableList())
}

interface StateScanner<S : State, E : Environment> {

    suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S
}

private class DefaultStateScanner<S : State, E : Environment>(private val reducer: Reducer<S>, private val middlewares: List<Middleware<S, E>>) : StateScanner<S, E> {

    override suspend fun scan(storeType: StoreType<S, E>, state: S, action: Any): S {
        middlewares.onEach { it.process(Order.Before, storeType, state, action) }
        val nextState = reducer.reduce(state, action)
        middlewares.onEach { it.process(Order.After, storeType, nextState, action) }
        return nextState
    }
}

class Store<S : State, E : Environment> internal constructor(
    scope: CoroutineScope,
    initialState: S,
    reducer: Reducer<S> = NoopReducer(),
    private val middlewares: MutableList<Middleware<S, E>> = mutableListOf(),
    scanner: StateScanner<S, E> = DefaultStateScanner(reducer, middlewares)
) : StoreType<S, E> {

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
        states = _actions
            .scan(initialState to NoAction as Any) { (state, _), action ->
                val nextState = scanner.scan(this, state, action)
                nextState to action
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

    override fun removeMiddleware(middleware: Middleware<S, E>): Boolean = middlewares.remove(middleware)
}

fun <S : State> combineReducers(reducers: List<Reducer<S>>): Reducer<S> = CompositeReducer(reducers)

fun <S : State> combineReducers(vararg reducers: Reducer<S>): Reducer<S> = CompositeReducer(reducers.asList())

private class CompositeReducer<S : State>(private val reducers: List<Reducer<S>>) : Reducer<S> {

    override fun reduce(currentState: S, action: Any): S = reducers.fold(currentState) { state, reducer ->
        reducer.reduce(state, action)
    }
}
