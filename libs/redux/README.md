## Libs/Redux

### Motivation

The motivation behind creating this Redux implementation in KMP is to be able to use in KMP to
support building maintainable and scalable mobile apps. This implementation closely follow the 3
core principles of main Redux repos which are;

* Single source of truth
* State is read-only and never mutate
* Changes are only made by pure functions

### Implementation

While the core concept are borrowed by the main js redux repository, in our use case, we
use [Kotlinx.Coroutine](https://kotlinlang.org/docs/coroutines-overview.html) as a main mechanism
under the hood. The benefit by doing this is that we are leveraging the asynchronous or non-blocking
programming or structured concurrency provided by Kotlin Coroutines.

### State

```kotlin
interface State
```

Imagine a screen in your application can be described as a plain object. For example, a simple
counter app would be represent as something as simple as this;

```kotlin
data class CounterState(val counter: Int = 0) : State
```

In Kotlin, using a data class is a perfect representation for this. As This is a core concept of
Redux because State cannot be mutated and/or changed by directly setting the value. As you can see
that the counter is used with `val` keyword.

In our implementation, the `interface state` is used merely as a phantom type. It doesn't have any
other specific purpose other than making sure that our Store object is working with the right state
object.

### Action

```kotlin
interface Action
```

Whenever, you want to change something to your state, ie. to alter state of the screen in the
application. The only way to do that is to `dispatch` an Action. An Action, again is a plain
interface type to enforce, describe how the state in your application can be changed and described
how to do so. Some of the actions for our Counter app is the following;

```kotlin
class Increment(val by: Int) : CounterAction()
class Decrement(val by: Int) : CounterAction()
```

By doing this, you can have a clear understanding on how you can interact with your state by
enforcing this with type. In this example, there **are** only 2 ways to do that which is to
either `Increment` or `Decrement` by a `by` value.

### Reducer

```kotlin
fun reduce(currentState: State, action: Action): State
```

Next, to piece the whole picture together on how the `State` and `Action` interact to each other, we
use the `Reducer` function which is a pure function without side-effect to do so. This is described
as a function that takes the current state and given action, then we write the code to describe how
to generate new state.

The example of the counter app's reducer function should be something like;

```kotlin
    private val counterReducer = object : Reducer<CounterState, CounterAction> {
    override fun reduce(currentState: CounterState, action: CounterAction): CounterState {
        return with(currentState) {
            when (action) {
                is Increment -> copy(counter = counter + action.by)
                is Decrement -> copy(counter = counter - action.by)
            }
        }
    }
}
```

To reducer a state with the action to get the newState is something as easy as a function call like
the following;

```kotlin
val newState = counterReducer.reduce(oldState, Increment(10))
```

By enforcing this, we make sure that the change that made to the state is predictable and in a
controlled environment.

### Environment

```kotlin
interface Environment
```

Environment is a outside dependency that could be injected into the store. In the perfect world, we
probably don't want to interact anything but inside the Store only. However, this is not the case.
Sometimes, changing the state introduces some side-effects to the outside world. This could be
thought as interact with persistent layer like DB or calling the network. This could be used when we
interact with middleware.

Let's assume that we need to interact with a Repository class, we could create a class like this;

```kotlin
class CounterEnvironment(val repository: Repository)
```

### Middleware

Middleware is a class that interacts with side-effects. There are 2 steps one is before state is
reduced and after state is reduced which can be identified with Order object. One good example of
such use-case for middleware is interaction with DB.

```kotlin
enum class Order {
    BeforeReducingState,
    AfterReducingState
}
```

```kotlin
val saveToDBMiddleware = object : Middleware<CounterState, CounterAction, CounterEnvironment> {
    override fun process(
        order: Order,
        store: StoreType<CounterState, CounterAction, CounterEnvironment>,
        state: CounterState,
        action: CounterAction
    ) {
        if (order == Order.BeforeReducingState) {
        } else {
            environment.saveDataToDB()
        }
    }

    override val environment: CounterEnvironment = CounterEnvironment
}
```

### Store

After we have all of the components ready, we can create a store with a provided
function `createStore(...)`

```kotlin
val store = createStore(counterState, CounterEnvironment, counterReducer)
```

Then, we can observe the state that will be changed over time with `store.states` which represents
as `StateFlow<T>` like so;

```kotlin
val scope = CoroutineScope()
scope.launch {
    store.states
        .collect { state ->
            println(state)
        }
}
```
