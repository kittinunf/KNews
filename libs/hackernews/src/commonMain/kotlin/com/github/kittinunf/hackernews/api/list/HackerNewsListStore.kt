package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.LoadAction
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.api.detail.LoadStoryError
import com.github.kittinunf.hackernews.api.map
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.hackernews.util.map
import com.github.kittinunf.redux.Environment
import com.github.kittinunf.redux.Middleware
import com.github.kittinunf.redux.Order
import com.github.kittinunf.redux.Reducer
import com.github.kittinunf.redux.State
import com.github.kittinunf.redux.StoreType
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

typealias ListAction = Any

object LoadStories : LoadAction<Nothing>()
class LoadNextStories(val page: Int) : LoadAction<Int>(page)
class Sort(val sortCondition: ListUiSortCondition) : ListAction()

class ListReducer : Reducer<ListUiState> {

    override fun reduce(currentState: ListUiState, action: ListAction): ListUiState {
        return when (action) {
            is LoadStories -> {
                currentState.copy(stories = Data.Loading)
            }

            is LoadNextStories -> {
                currentState.copy(nextStories = Data.Loading)
            }

            is Sort -> {
                val newSortCondition = action.sortCondition
                val newSortComparator = action.sortCondition.comparator

                if (newSortCondition == currentState.sortCondition) return currentState

                currentState.copy(sortCondition = newSortCondition, stories = currentState.stories.map {
                    if (newSortComparator != null) it.sortedWith(newSortComparator) else it
                })
            }

            is ResultAction<*, *> -> {
                require(action.fromAction is LoadStories || action.fromAction is LoadNextStories)

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

                    currentState.copy(stories = sortedResult.toData() as Data<List<ListUiRowState>, ListError>)
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
                    currentState.copy(stories = sortedStories, nextStories = result.toData())
                }
            }

            else -> currentState
        }
    }
}

class ListEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

class ListDataMiddleware(override val environment: ListEnvironment, private val mapper: Mapper<Story, ListUiRowState>) : Middleware<ListUiState, ListEnvironment> {

    override fun process(order: Order, store: StoreType<ListUiState, ListEnvironment>, state: ListUiState, action: ListAction) {
        when (order) {
            Order.BeforeReducingState -> {
                when (action) {
                    is LoadStories -> {
                        // the current loading is already in-flight
                        if (state.stories is Data.Loading) return

                        with(environment) {
                            scope.launch {
                                val result = repository.getTopStories()
                                result.fold(success = {
                                    store.dispatch(ResultAction(action, Result.success(it?.map(mapper::map))))
                                }, failure = {
                                    store.dispatch(ResultAction(action, Result.error(LoadStoriesError(it.message ?: "Unknown error"))))
                                })
                            }
                        }
                    }
                    is LoadNextStories -> {
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
                                    store.dispatch(ResultAction(action, Result.success(it?.map(mapper::map))))
                                }, failure = {
                                    store.dispatch(ResultAction(action, Result.error(LoadNextStoriesError(it.message ?: "Unknown error"))))
                                })
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
            Order.AfterReducingState -> {
            }
        }
    }
}

typealias Store = StoreType<ListUiState, ListEnvironment>
