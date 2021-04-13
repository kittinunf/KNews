package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.map
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.redux.Action
import com.github.kittinunf.redux.Environment
import com.github.kittinunf.redux.Middleware
import com.github.kittinunf.redux.Order
import com.github.kittinunf.redux.Reducer
import com.github.kittinunf.redux.State
import com.github.kittinunf.redux.StoreType
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
    val nextStories: Data<List<ListUiRowState>, ListError> = Data.Initial
) : State

sealed class ListError {
    class LoadStoriesError(val error: String) : ListError()
    class LoadNextStoriesError(val error: String) : ListError()
}

sealed class ListAction : Action {
    object LoadStories : ListAction()
    class LoadNextStories(val page: Int) : ListAction()
    class Sort(val sortCondition: ListUiSortCondition) : ListAction()

    class Success(val fromAction: ListAction, val stories: List<ListUiRowState>?) : ListAction()
    class Failure(val fromAction: ListAction, val error: String) : ListAction()
}

class ListReducer : Reducer<ListUiState, ListAction> {

    override fun reduce(currentState: ListUiState, action: ListAction): ListUiState {
        when (action) {
            is ListAction.LoadStories -> {
                return currentState.copy(stories = Data.Loading)
            }

            is ListAction.LoadNextStories -> {
                return currentState.copy(nextStories = Data.Loading)
            }

            is ListAction.Sort -> {
                val newSortCondition = action.sortCondition
                val newSortComparator = action.sortCondition.comparator

                if (newSortCondition == currentState.sortCondition) return currentState

                return currentState.copy(sortCondition = newSortCondition, stories = currentState.stories.map {
                    if (newSortComparator != null) it.sortedWith(newSortComparator) else it
                })
            }

            is ListAction.Success -> {
                require(action.fromAction is ListAction.LoadStories || action.fromAction is ListAction.LoadNextStories)

                return if (action.fromAction is ListAction.LoadStories) {
                    val sortedStories = if (currentState.sortCondition != ListUiSortCondition.None) {
                        val list = action.stories?.toMutableList()
                        list?.sortWith(currentState.sortCondition.comparator!!)
                        list
                    } else action.stories
                    currentState.copy(stories = Data.Success(sortedStories.orEmpty()))
                } else {
                    // when loadNextStories success we need to append our data into the stories
                    val stories = currentState.stories
                    val newStories = if (stories is Data.Success) {
                        val list = stories.value.toMutableList()
                        list.addAll(action.stories.orEmpty())

                        if (currentState.sortCondition != ListUiSortCondition.None) {
                            list.sortWith(currentState.sortCondition.comparator!!)
                        }

                        Data.Success(list)
                    } else stories
                    currentState.copy(stories = newStories, nextStories = Data.Success(action.stories.orEmpty()))
                }
            }

            is ListAction.Failure -> {
                require(action.fromAction is ListAction.LoadStories || action.fromAction is ListAction.LoadNextStories)

                return if (action.fromAction is ListAction.LoadStories) {
                    currentState.copy(stories = Data.Failure(ListError.LoadStoriesError(action.error)))
                } else {
                    currentState.copy(nextStories = Data.Failure(ListError.LoadNextStoriesError(action.error)))
                }
            }
        }
    }
}

class ListEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

class ListDataMiddleware(override val environment: ListEnvironment, private val mapper: Mapper<Story, ListUiRowState>) : Middleware<ListUiState, ListAction, ListEnvironment> {

    override fun process(order: Order, store: StoreType<ListUiState, ListAction, ListEnvironment>, state: ListUiState, action: ListAction) {
        when (order) {
            Order.BeforeReducingState -> {
                when (action) {
                    ListAction.LoadStories -> {
                        // the current loading is already in-flight
                        if (state.stories is Data.Loading) return

                        with(environment) {
                            scope.launch {
                                val result = repository.getTopStories()
                                result.fold(success = {
                                    store.dispatch(ListAction.Success(action, it?.map(mapper::map)))
                                }, failure = {
                                    store.dispatch(ListAction.Failure(action, it.message ?: "Unknown error"))
                                })
                            }
                        }
                    }
                    is ListAction.LoadNextStories -> {
                        // the current loading is already in-flight and the main list is not done yet
                        if (state.nextStories is Data.Loading) return
                        if (state.stories.isSuccess.not()) {
                            with(environment) {
                                scope.launch { store.dispatch(ListAction.Failure(action, "Data inconsistency, not loading next page")) }
                            }
                            return
                        }

                        with(environment) {
                            scope.launch {
                                val result = repository.getTopStories(action.page)
                                result.fold(success = {
                                    store.dispatch(ListAction.Success(action, it?.map(mapper::map)))
                                }, failure = {
                                    store.dispatch(ListAction.Failure(action, it.message ?: "Unknown error"))
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

@OptIn(ExperimentalTime::class)
internal val listUiRowStateMapper = object : Mapper<Story, ListUiRowState> {
    override fun map(t: Story): ListUiRowState {
        val now = Clock.System.now()
        val diff = now.epochSeconds - t.time
        return ListUiRowState(
            id = t.id,
            title = t.title,
            url = Url(t.url),
            score = t.score,
            by = t.by,
            fromNow = diff,
            fromNowText = diff.toHumanConsumableText(),
            commentIds = t.kids,
            descendants = t.descendants
        )
    }
}

// Int here represent the diff in seconds
internal fun Long.toHumanConsumableText(): String {
    if (this < 0) return "Unknown ago"
    return when (this) {
        in 0..59 -> "$this seconds ago"
        in 60..3599 -> "${this / 60} minutes ago"
        in 3600..86399 -> "${this / (60 * 60)} hours ago"
        else -> "${this / (60 * 60 * 24)} days ago"
    }
}

typealias Store = StoreType<ListUiState, ListAction, ListEnvironment>
