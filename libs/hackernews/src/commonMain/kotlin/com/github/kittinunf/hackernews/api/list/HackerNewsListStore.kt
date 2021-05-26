package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.cored.Environment
import com.github.kittinunf.cored.Identifiable
import com.github.kittinunf.cored.Reducer
import com.github.kittinunf.cored.State
import com.github.kittinunf.cored.StoreType
import com.github.kittinunf.cored.createStore
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.api.map
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.hackernews.util.map
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
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
    val url: Url?,
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

internal class Sort(val sortCondition: ListUiSortCondition) : Identifiable

internal object LoadStories : Identifiable
internal class LoadStoriesResult(result: Result<List<ListUiRowState>, ListError>) : ResultAction<List<ListUiRowState>, ListError>(result), Identifiable

internal class LoadNextStories(val page: Int) : Identifiable
internal class LoadNextStoriesResult(result: Result<List<ListUiRowState>?, ListError>) : ResultAction<List<ListUiRowState>?, ListError>(result), Identifiable

@Suppress("FunctionName")
internal fun LoadStoriesReducer() = "LoadStories" to Reducer { currentState: ListUiState, _: LoadStories ->
    currentState.copy(stories = Data.Loading(currentState.stories.getOrNull()))
}

@Suppress("FunctionName")
internal fun LoadStoriesResultReducer() = "LoadStoriesResult" to Reducer { currentState: ListUiState, action: LoadStoriesResult ->
    with(currentState) {
        val sortedResult = action.result.map<List<ListUiRowState>, List<ListUiRowState>, ListError> {
            val comparator = currentState.sortCondition.comparator
            if (comparator != null) {
                val list = it.toMutableList()
                list.sortWith(currentState.sortCondition.comparator)
                list
            } else it
        }

        copy(stories = sortedResult.toData() as Data<List<ListUiRowState>, ListError>)
    }
}

@Suppress("FunctionName")
internal fun LoadNextStoriesReducer() = "LoadNextStories" to Reducer { currentState: ListUiState, _: LoadNextStories ->
    currentState.copy(nextStories = Data.Loading())
}

@Suppress("FunctionName")
internal fun LoadNextStoriesResultReducer() = "LoadNextStoriesResult" to Reducer { currentState: ListUiState, action: LoadNextStoriesResult ->
    with(currentState) {
        // when loadNextStories success we need to append our data into the stories
        val result = action.result
        val stories = currentState.stories
        val sortedStories = stories.map {
            val list = it?.toMutableList() ?: mutableListOf()

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

@Suppress("FunctionName")
internal fun SortReducer() = "Sort" to Reducer { currentState: ListUiState, action: Sort ->
    with(currentState) {
        val newSortCondition = action.sortCondition
        val newSortComparator = action.sortCondition.comparator

        if (newSortCondition == sortCondition) return@Reducer currentState

        copy(sortCondition = newSortCondition, stories = currentState.stories.map {
            it ?: return@map emptyList()
            if (newSortComparator != null) it.sortedWith(newSortComparator) else it
        })
    }
}

internal class ListEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

internal typealias Store = StoreType<ListUiState, ListEnvironment>
@Suppress("FunctionName")
internal fun ListStore(scope: CoroutineScope, environment: ListEnvironment): Store {
    return createStore(
        scope = scope,
        initialState = ListUiState(),
        reducers = mapOf(
            LoadStoriesReducer(),
            LoadStoriesResultReducer(),
            LoadNextStoriesReducer(),
            LoadNextStoriesResultReducer(),
            SortReducer(),
        ),
        middlewares = mapOf(
            LoadStoriesEffect(environment, ::listUiRowStateMapper),
            LoadNextStoriesEffect(environment, ::listUiRowStateMapper)
        )
    )
}
