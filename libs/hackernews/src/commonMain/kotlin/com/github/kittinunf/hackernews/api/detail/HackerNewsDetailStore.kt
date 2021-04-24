package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.LoadAction
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.repository.HackerNewsService
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.redux.EffectType
import com.github.kittinunf.redux.Environment
import com.github.kittinunf.redux.Identifiable
import com.github.kittinunf.redux.Middleware
import com.github.kittinunf.redux.Order
import com.github.kittinunf.redux.Reducer
import com.github.kittinunf.redux.State
import com.github.kittinunf.redux.StoreType
import com.github.kittinunf.redux.createStore
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class DetailUiCommentRowState(val text: String, val by: String, val fromNow: Long, val fromNowText: String)

data class DetailUiStoryState(val id: Int, val title: String, val url: Url, val commentIds: List<Int>?, val descendants: Int?)

data class DetailUiState(
    val storyId: Int = -1,
    val story: Data<DetailUiStoryState, DetailError> = Data.Initial,
    val comments: Data<List<DetailUiCommentRowState>, DetailError> = Data.Initial
) : State

sealed class DetailError(message: String) : Throwable(message)
class LoadStoryError(val error: String) : DetailError(error)
class LoadStoryCommentsError(val error: String) : DetailError(error)

internal typealias DetailAction = Any

internal class SetInitialStory(val state: DetailUiStoryState) : DetailAction(), Identifiable {
    override val identifier: String = "SetInitialStory"
}

internal object LoadStory : LoadAction<Nothing>(), Identifiable {
    override val identifier: String = "LoadStory"
}

internal object LoadStoryComments : LoadAction<Nothing>(), Identifiable {
    override val identifier: String = "LoadStoryComments"
}

internal fun SetInitialStoryReducer() = "SetInitialStory" to Reducer { currentState: DetailUiState, action: SetInitialStory ->
    currentState.copy(storyId = action.state.id, story = Data.Success(action.state))
}

internal fun LoadStoryReducer() = "LoadStory" to Reducer { currentState: DetailUiState, _: LoadStory ->
    currentState.copy(story = Data.Loading)
}

internal fun LoadStoryCommentsReducer() = "LoadStoryComments" to Reducer { currentState: DetailUiState, _: LoadStoryComments ->
    currentState.copy(comments = Data.Loading)
}

internal fun ResultActionReducer() = "ResultAction" to Reducer { currentState: DetailUiState, action: ResultAction<*, *> ->
    require(action.fromAction is LoadStory || action.fromAction is LoadStoryComments)

    with(currentState) {
        if (action.fromAction is LoadStory) {
            val result = action.result as Result<DetailUiStoryState, DetailError>
            copy(story = result.toData())
        } else {
            val result = action.result as Result<List<DetailUiCommentRowState>, DetailError>
            copy(comments = result.toData())
        }
    }
}

internal class DetailEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

internal typealias Store = StoreType<DetailUiState, DetailEnvironment>
internal typealias Effect<A> = EffectType<DetailUiState, A, DetailEnvironment>

internal fun LoadStoryEffect(environment: DetailEnvironment, mapper: Mapper<Story, DetailUiStoryState>): Effect<LoadStory> =
    "LoadStory" to object : Middleware<DetailUiState, LoadStory, DetailEnvironment> {
        override fun process(order: Order, store: StoreType<DetailUiState, DetailEnvironment>, state: DetailUiState, action: LoadStory) {
            if (order == Order.AfterReduced) return

            // the current loading is already in-flight
            if (state.story is Data.Loading) return

            with(environment) {
                scope.launch {
                    val result = repository.getStory(state.storyId)
                    result.fold(success = {
                        store.dispatch(ResultAction(action, Result.success(detailUiStoryStateMapper.map(it))))
                    }, failure = {
                        store.dispatch(ResultAction(action, Result.error(LoadStoryError(it.message ?: "Unknown error"))))
                    })
                }
            }
        }

        override val environment: DetailEnvironment = environment
    }

internal fun LoadStoryCommentsEffect(environment: DetailEnvironment, mapper: Mapper<Comment, DetailUiCommentRowState>): Effect<LoadStoryComments> =
    "LoadStoryComments" to object : Middleware<DetailUiState, LoadStoryComments, DetailEnvironment> {
        override fun process(order: Order, store: StoreType<DetailUiState, DetailEnvironment>, state: DetailUiState, action: LoadStoryComments) {
            if (order == Order.AfterReduced) return
            // the current loading is already in-flight
            if (state.comments is Data.Loading) return

            with(environment) {
                scope.launch {
                    // check value we have a valid story object, or we fetch the story first to get all of the kids then fetch the comment
                    val result = if (state.story is Data.Success) {
                        val ids = state.story.value.commentIds
                        if (ids == null) Result.success<List<Comment>?>(null) else repository.getComments(state.story.value.commentIds)
                    } else repository.getStoryComments(state.storyId)

                    result.fold(success = {
                        store.dispatch(ResultAction(action, Result.success(it?.map(detailUiCommentRowStateMapper::map))))
                    }, failure = {
                        store.dispatch(ResultAction(action, Result.Failure(LoadStoryCommentsError(it.message ?: "Unknown error"))))
                    })
                }
            }
        }

        override val environment: DetailEnvironment = environment
    }

@Suppress("FunctionName")
internal fun DetailStore(initialState: DetailUiState = DetailUiState(), scope: CoroutineScope, environment: DetailEnvironment): Store {
    return createStore(
        scope = scope,
        initialState = initialState,
        reducers = mapOf(
            SetInitialStoryReducer(),
            LoadStoryReducer(),
            LoadStoryCommentsReducer(),
            ResultActionReducer()
        ),
        middlewares = mapOf(
            LoadStoryEffect(environment, detailUiStoryStateMapper),
            LoadStoryCommentsEffect(environment, detailUiCommentRowStateMapper)
        )
    )
}
