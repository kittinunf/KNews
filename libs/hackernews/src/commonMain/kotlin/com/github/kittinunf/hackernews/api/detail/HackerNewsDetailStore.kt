package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.cored.Environment
import com.github.kittinunf.cored.Identifiable
import com.github.kittinunf.cored.Reducer
import com.github.kittinunf.cored.State
import com.github.kittinunf.cored.StoreType
import com.github.kittinunf.cored.createStore
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Result
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class DetailUiCommentRowState(val text: String, val by: String, val fromNow: Long, val fromNowText: String)

data class DetailUiStoryState(val id: Int, val title: String, val url: Url, val commentIds: List<Int>?, val descendants: Int?)

data class DetailUiState(
    val storyId: Int = -1,
    val story: Data<DetailUiStoryState, DetailError> = Data.Initial,
    val comments: Data<List<DetailUiCommentRowState>?, DetailError> = Data.Initial
) : State {
    constructor(
        id: Int,
        title: String,
        url: Url,
        commentIds: List<Int>?,
        descendants: Int?
    ) : this(id, Data.Success(DetailUiStoryState(id, title, url, commentIds, descendants)))
}

sealed class DetailError(message: String) : Throwable(message)
class LoadStoryError(val error: String) : DetailError(error)
class LoadStoryCommentsError(val error: String) : DetailError(error)

internal class SetInitialStory(val state: DetailUiStoryState) : Identifiable

internal object LoadStory : Identifiable
internal class LoadStoryResult(result: Result<DetailUiStoryState, DetailError>) : ResultAction<DetailUiStoryState, DetailError>(result), Identifiable

internal object LoadStoryComments : Identifiable
internal class LoadStoryCommentsResult(result: Result<List<DetailUiCommentRowState>?, DetailError>) : ResultAction<List<DetailUiCommentRowState>?, DetailError>(result),
    Identifiable

@Suppress("FunctionName")
internal fun SetInitialStoryReducer() = "SetInitialStory" to Reducer { currentState: DetailUiState, action: SetInitialStory ->
    currentState.copy(storyId = action.state.id, story = Data.Success(action.state))
}

@Suppress("FunctionName")
internal fun LoadStoryReducer() = "LoadStory" to Reducer { currentState: DetailUiState, _: LoadStory ->
    currentState.copy(story = Data.Loading(currentState.story.getOrNull()))
}

@Suppress("FunctionName")
internal fun LoadStoryResultReducer() = "LoadStoryResult" to Reducer { currentState: DetailUiState, action: LoadStoryResult ->
    currentState.copy(story = action.result.toData())
}

@Suppress("FunctionName")
internal fun LoadStoryCommentsReducer() = "LoadStoryComments" to Reducer { currentState: DetailUiState, _: LoadStoryComments ->
    currentState.copy(comments = Data.Loading(currentState.comments.getOrNull()))
}

@Suppress("FunctionName")
internal fun LoadStoryCommentsResultReducer() = "LoadStoryCommentsResult" to Reducer { currentState: DetailUiState, action: LoadStoryCommentsResult ->
    currentState.copy(comments = action.result.toData())
}

internal class DetailEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

internal typealias Store = StoreType<DetailUiState, DetailEnvironment>
@Suppress("FunctionName")
internal fun DetailStore(initialState: DetailUiState = DetailUiState(), scope: CoroutineScope, environment: DetailEnvironment): Store {
    return createStore(
        scope = scope,
        initialState = initialState,
        reducers = mapOf(
            SetInitialStoryReducer(),
            LoadStoryReducer(),
            LoadStoryResultReducer(),
            LoadStoryCommentsReducer(),
            LoadStoryCommentsResultReducer(),
        ),
        middlewares = mapOf(
            LoadStoryEffect(environment, ::detailUiStoryStateMapper),
            LoadStoryCommentsEffect(environment, ::detailUiCommentRowStateMapper)
        )
    )
}
