package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.LoadAction
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.api.common.toData
import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.hackernews.util.Result
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

typealias DetailAction = Any

class SetInitialStory(val state: DetailUiStoryState) : DetailAction()
object LoadStory : LoadAction<Nothing>()
object LoadStoryComments : LoadAction<Nothing>()

class DetailReducer : Reducer<DetailUiState> {

    override fun reduce(currentState: DetailUiState, action: DetailAction): DetailUiState {
        return when (action) {
            is SetInitialStory -> {
                currentState.copy(storyId = action.state.id, story = Data.Success(action.state))
            }

            is LoadStory -> {
                currentState.copy(story = Data.Loading)
            }

            is LoadStoryComments -> {
                currentState.copy(comments = Data.Loading)
            }

            is ResultAction<*, *> -> {
                require(action.fromAction is LoadStory || action.fromAction is LoadStoryComments)

                return with(currentState) {
                    if (action.fromAction is LoadStory) {
                        val result = action.result as Result<DetailUiStoryState, DetailError>
                        copy(story = result.toData())
                    } else {
                        val result = action.result as Result<List<DetailUiCommentRowState>, DetailError>
                        copy(comments = result.toData())
                    }
                }
            }

            else -> currentState
        }
    }
}

class DetailEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

class DetailDataMiddleware(
    override val environment: DetailEnvironment,
    private val detailUiStoryStateMapper: Mapper<Story, DetailUiStoryState>,
    private val detailUiCommentRowStateMapper: Mapper<Comment, DetailUiCommentRowState>
) : Middleware<DetailUiState, DetailEnvironment> {

    override fun process(order: Order, store: StoreType<DetailUiState, DetailEnvironment>, state: DetailUiState, action: DetailAction) {
        when (order) {
            Order.Before -> {
                when (action) {
                    is LoadStory -> {
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

                    is LoadStoryComments -> {
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
                    else -> {
                    }
                }
            }
            Order.After -> {
            }
        }
    }
}

typealias Store = StoreType<DetailUiState, DetailEnvironment>
