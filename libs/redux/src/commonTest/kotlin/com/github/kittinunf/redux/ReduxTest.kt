package com.github.kittinunf.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

data class CounterState(val counter: Int = 0) : State

sealed class CounterAction : Action
class Increment(val by: Int) : CounterAction()
class Decrement(val by: Int) : CounterAction()
class Set(val value: Int) : CounterAction()

object CounterEnvironment : Environment

typealias CounterStore = StoreType<CounterState, CounterAction, CounterEnvironment>

class ReduxTest {

    private val counterState = CounterState()
    private val counterReducer = object : Reducer<CounterState, CounterAction> {
        override fun reduce(currentState: CounterState, action: CounterAction): CounterState {
            return with(currentState) {
                when (action) {
                    is Increment -> copy(counter = counter + action.by)
                    is Decrement -> copy(counter = counter - action.by)
                    is Set -> copy(counter = action.value)
                }
            }
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val counterStore = createStore<CounterState, CounterAction, CounterEnvironment>(testScope, counterState, counterReducer)

    @BeforeTest
    fun before() {
    }

    @Test
    fun `should increment state`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> assertEquals(0, state.counter)
                        1 -> assertEquals(1, state.counter)
                        2 -> assertEquals(3, state.counter)
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(1))
            counterStore.dispatch(Increment(2))
        }

        println(counterStore.states.value)
    }

    @Test
    fun `should decrement state`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> assertEquals(0, state.counter)
                        1 -> assertEquals(-2, state.counter)
                        2 -> assertEquals(-5, state.counter)
                        3 -> assertEquals(-10, state.counter)
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Decrement(2))
            counterStore.dispatch(Decrement(3))
            counterStore.dispatch(Decrement(5))
        }
    }

    @Test
    fun `should emit initial value`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    assertEquals(0, index)
                    assertEquals(0, state.counter)
                }
                .printDebug()
                .launchIn(testScope)
        }
    }

    @Test
    fun `should emit value even the state not changed`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> assertEquals(0, state.counter)
                        1 -> assertEquals(0, state.counter)
                        2 -> assertEquals(0, state.counter)
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(0))
            counterStore.dispatch(Decrement(0))
        }
    }

    @Test
    fun `should not emit same value if states used`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> assertEquals(0, state.counter)
                        1 -> assertEquals(1, state.counter)
                        else -> fail()
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(1))
            counterStore.dispatch(Decrement(0))
            counterStore.dispatch(Decrement(0))
        }
    }

    @Test
    fun `should dispatch multiple value from Flow initiator`() {
        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        4 -> assertEquals(0, state.counter)
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(flow {
                emit(Increment(10))
                emit(Decrement(10))
                emit(Increment(100))
                emit(Decrement(100))
            })
        }
    }

    @Test
    fun `should invoke middleware if set`() {
        data class SideEffectData(var value: Int)

        val sideEffectData = SideEffectData(100)

        val middleware = object : Middleware<CounterState, CounterAction, CounterEnvironment> {
            override fun process(order: Order, store: StoreType<CounterState, CounterAction, CounterEnvironment>, state: CounterState, action: CounterAction) {
                if (order == Order.BeforeReducingState) {
                    assertEquals(0, state.counter)
                    assertTrue(action is Increment)
                } else {
                    sideEffectData.value = sideEffectData.value + state.counter
                }
            }

            override val environment: CounterEnvironment = CounterEnvironment
        }

        counterStore.addMiddleware(middleware)

        runBlockingTest {
            counterStore.states
                .withIndex()
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(100))
        }

        assertEquals(200, sideEffectData.value)
    }

    @Test
    fun `should invoke middleware until remove`() {
        data class SideEffectData(var value: Int)

        val sideEffectData = SideEffectData(100)

        val middleware = object : Middleware<CounterState, CounterAction, CounterEnvironment> {
            override fun process(order: Order, store: StoreType<CounterState, CounterAction, CounterEnvironment>, state: CounterState, action: CounterAction) {
                if (order == Order.BeforeReducingState) {
                    assertEquals(0, state.counter)
                    assertTrue(action is Increment)
                } else {
                    sideEffectData.value = sideEffectData.value + state.counter
                }
            }

            override val environment: CounterEnvironment = CounterEnvironment
        }

        counterStore.addMiddleware(middleware)

        runBlockingTest {
            counterStore.states
                .withIndex()
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(100))
        }

        assertEquals(200, sideEffectData.value)

        counterStore.removeMiddleware(middleware)

        runBlockingTest {
            counterStore.dispatch(Increment(100))
            counterStore.dispatch(Decrement(100))
        }

        assertEquals(200, sideEffectData.value)
    }

    @Test
    fun `should invoke middleware in the correct order`() {
        val middleware = object : Middleware<CounterState, CounterAction, CounterEnvironment> {
            override fun process(order: Order, store: StoreType<CounterState, CounterAction, CounterEnvironment>, state: CounterState, action: CounterAction) {
                if (order == Order.BeforeReducingState) /**/{
                    assertEquals(0, state.counter)
                    assertTrue(action is Increment)
                } else {
                    assertEquals(100, state.counter)
                    assertTrue(action is Increment)
                }
            }

            override val environment: CounterEnvironment = CounterEnvironment
        }

        counterStore.addMiddleware(middleware)

        runBlockingTest {
            counterStore.states
                .withIndex()
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(100))
        }
    }

    @Test
    fun `should dispatch action from the middleware`() {
        val middleware = object : Middleware<CounterState, CounterAction, CounterEnvironment> {
            override fun process(order: Order, store: StoreType<CounterState, CounterAction, CounterEnvironment>, state: CounterState, action: CounterAction) {
                if (order == Order.BeforeReducingState) {
                    assertTrue(action is Increment)
                } else {
                    assertTrue(action is Increment)
                    if (state.counter == 100) {
                        // dispatch another action from middleware
                        runBlockingTest {
                            store.dispatch(Increment(10))
                        }
                        store.tryDispatch(Increment(200))
                    }
                }
            }

            override val environment: CounterEnvironment = CounterEnvironment
        }

        counterStore.addMiddleware(middleware)

        runBlockingTest {
            counterStore.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> assertEquals(110, state.counter)
                        3 -> assertEquals(310, state.counter)
                    }
                }
                .printDebug()
                .launchIn(testScope)

            counterStore.dispatch(Increment(100))
        }
    }
}

fun <T> Flow<T>.printDebug() = onEach { println(it) }
