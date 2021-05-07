package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.LoadAction
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.api.map
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.hackernews.util.map
import com.github.kittinunf.cored.EffectType
import com.github.kittinunf.cored.Environment
import com.github.kittinunf.cored.Identifiable
import com.github.kittinunf.cored.Middleware
import com.github.kittinunf.cored.Order
import com.github.kittinunf.cored.Reducer
import com.github.kittinunf.cored.State
import com.github.kittinunf.cored.StoreType
import com.github.kittinunf.cored.createStore
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

enum class ListUiSortCondition(val comparator: Comparator<ListUiRowState>?) {
    None(null),
    Recent(compareBy { it.fromNow }),
    Title(compareBy { it.title }),
    Score(compareByDescending { it.score });
}

@OptIn(ExperimentalTime::class)
data class ListUiRowState(
    val id: Int,
    val title: String,
    val url: Url,
    val score: Int,
    val by: String,
    val fromNow: Long,
    val fromNowText: String,
    val commentIds: List<Int>?,
    val descendants: Int?
)

data class ListUiState(
    val sortCondition: ListUiSortCondition = ListUiSortCondition.None,
    val stories: Data<List<ListUiRowState>, ListError> = Data.Initial,
    val nextStories: Data<List<ListUiRowState>?, ListError> = Data.Initial
) : State

sealed class ListError(message: String) : Throwable(message)
class LoadStoriesError(val error: String) : ListError(error)
class LoadNextStoriesError(val error: String) : ListError(error)

internal typealias ListAction = Any

internal object LoadStories : LoadAction<Nothing>() {
    override val identifier: String = "LoadStories"
}

internal class LoadNextStories(val page: Int) : LoadAction<Int>(page) {
    override val identifier: String = "LoadNextStories"
}

internal class Sort(val sortCondition: ListUiSortCondition) : ListAction(), Identifiable {
    override val identifier: String = "Sort"
}

@Suppress("FunctionName")
internal fun LoadStoriesReducer() = "LoadStories" to Reducer { currentState: ListUiState, _: LoadStories ->
    currentState.copy(stories = Data.Loading)
}

@Suppress("FunctionName")
internal fun LoadNextStoriesReducer() = "LoadNextStories" to Reducer { currentState: ListUiState, _: LoadNextStories ->
    currentState.copy(nextStories = Data.Loading)
}

@Suppress("FunctionName")
internal fun SortReducer() = "Sort" to Reducer { currentState: ListUiState, action: Sort ->
    val newSortCondition = action.sortCondition
    val newSortComparator = action.sortCondition.comparator

    if (newSortCondition == currentState.sortCondition) return@Reducer currentState

    currentState.copy(sortCondition = newSortCondition, stories = currentState.stories.map {
        if (newSortComparator != null) it.sortedWith(newSortComparator) else it
    })
}

@Suppress("FunctionName")
internal fun ResultActionReducer() = "ResultAction" to Reducer { currentState: ListUiState, action: ResultAction<*, *> ->
    require(action.fromAction is LoadStories || action.fromAction is LoadNextStories)

    with(currentState) {
        if (action.fromAction is LoadStories) {
            val result = action.result as Result<List<ListUiRowState>, ListError>

            val sortedResult = result.map<List<ListUiRowState>, List<ListUiRowState>, ListError> {
                val comparator = currentState.sortCondition.comparator
                if (comparator != null) {
                    val list = it.toMutableList()
                    list.sortWith(currentState.sortCondition.comparator)
                    list
                } else it
            }

            copy(stories = sortedResult.toData() as Data<List<ListUiRowState>, ListError>)
        } else {
            // when loadNextStories success we need to append our data into the stories
            val result = action.result as Result<List<ListUiRowState>?, ListError>
            val stories = currentState.stories
            val sortedStories = stories.map {
                val list = it.toMutableList()

                val (value, _) = result
                list.addAll(value.orEmpty())

                val comparator = currentState.sortCondition.comparator
                if (comparator != null) {
                    list.sortWith(comparator)
                }
                list
            }
            copy(stories = sortedStories, nextStories = result.toData())
        }
    }
}

internal class ListEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

internal typealias Store = StoreType<ListUiState, ListEnvironment>
internal typealias Effect<A> = EffectType<ListUiState, A, ListEnvironment>

@Suppress("FunctionName")
internal fun LoadStoriesEffect(environment: ListEnvironment, mapper: Mapper<Story, ListUiRowState>): Effect<LoadStories> =
    "LoadStories" to object : Middleware<ListUiState, LoadStories, ListEnvironment> {
        override fun process(order: Order, store: Store, state: ListUiState, action: LoadStories) {
            // we only perform side effect in the beforeReduce order
            if (order == Order.AfterReduced) return

            // the current loading is already in-flight
            if (state.stories is Data.Loading) return

            with(environment) {
                scope.launch {
                    val result = repository.getTopStories()
                    result.fold(success = {
                        store.dispatch(ResultAction(action, Result.success(it?.map(mapper::invoke))))
                    }, failure = {
                        store.dispatch(ResultAction(action, Result.error(LoadStoriesError(it.message ?: "Unknown error"))))
                    })
                }
            }
        }

        override val environment: ListEnvironment = environment
    }

@Suppress("FunctionName")
internal fun LoadNextStoriesEffect(environment: ListEnvironment, mapper: Mapper<Story, ListUiRowState>): Effect<LoadNextStories> =
    "LoadNextStories" to object : Middleware<ListUiState, LoadNextStories, ListEnvironment> {
        override fun process(order: Order, store: StoreType<ListUiState, ListEnvironment>, state: ListUiState, action: LoadNextStories) {
            // we only perform side effect in the beforeReduce order
            if (order == Order.AfterReduced) return

            // the current loading is already in-flight and the main list is not done yet
            if (state.nextStories is Data.Loading) return
            if (state.stories.isSuccess.not()) {
                with(environment) {
                    scope.launch { store.dispatch(ResultAction(action, Result.error(LoadNextStoriesError("Data inconsistency, not loading next page")))) }
                }
                return
            }

            with(environment) {
                scope.launch {
                    val result = repository.getTopStories(action.page)
                    result.fold(success = {
                        store.dispatch(ResultAction(action, Result.success(it?.map(mapper::invoke))))
                    }, failure = {
                        store.dispatch(ResultAction(action, Result.error(LoadNextStoriesError(it.message ?: "Unknown error"))))
                    })
                }
            }
        }

        override val environment: ListEnvironment = environment
    }

@Suppress("FunctionName")
internal fun ListStore(scope: CoroutineScope, environment: ListEnvironment): Store {
    return createStore(
        scope = scope,
        initialState = ListUiState(),
        reducers = mapOf(
            LoadNextStoriesReducer(),
            LoadStoriesReducer(),
            SortReducer(),
            ResultActionReducer()
        ),
        middlewares = mapOf(
            LoadStoriesEffect(environment, ::listUiRowStateMapper),
            LoadNextStoriesEffect(environment, ::listUiRowStateMapper)
        )
    )
}
