package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.cored.reducer
import com.github.kittinunf.cored.store.Store
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.result.Result
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
) {
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

internal class SetInitialStory(val state: DetailUiStoryState)

internal object LoadStory
internal class LoadStoryResult(result: Result<DetailUiStoryState, DetailError>) : ResultAction<DetailUiStoryState, DetailError>(result)

internal object LoadStoryComments
internal class LoadStoryCommentsResult(result: Result<List<DetailUiCommentRowState>?, DetailError>) : ResultAction<List<DetailUiCommentRowState>?, DetailError>(result)

@Suppress("FunctionName")
internal fun SetInitialStoryReducer() = reducer { currentState: DetailUiState, action: SetInitialStory ->
    currentState.copy(storyId = action.state.id, story = Data.Success(action.state))
}

@Suppress("FunctionName")
internal fun LoadStoryReducer() = reducer { currentState: DetailUiState, _: LoadStory ->
    currentState.copy(story = Data.Loading(currentState.story.getOrNull()))
}

@Suppress("FunctionName")
internal fun LoadStoryResultReducer() = reducer { currentState: DetailUiState, action: LoadStoryResult ->
    currentState.copy(story = action.result.toData())
}

@Suppress("FunctionName")
internal fun LoadStoryCommentsReducer() = reducer { currentState: DetailUiState, _: LoadStoryComments ->
    currentState.copy(comments = Data.Loading(currentState.comments.getOrNull()))
}

@Suppress("FunctionName")
internal fun LoadStoryCommentsResultReducer() = reducer { currentState: DetailUiState, action: LoadStoryCommentsResult ->
    currentState.copy(comments = action.result.toData())
}

internal class DetailEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository)

@Suppress("FunctionName")
internal fun DetailStore(initialState: DetailUiState = DetailUiState(), scope: CoroutineScope, environment: DetailEnvironment): Store<DetailUiState> {
    return Store(
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
