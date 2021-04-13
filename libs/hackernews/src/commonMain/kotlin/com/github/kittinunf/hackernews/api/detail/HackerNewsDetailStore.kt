package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.list.toHumanConsumableText
import com.github.kittinunf.hackernews.model.Comment
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
import com.github.kittinunf.hackernews.util.Result
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class DetailUiCommentRowState(val text: String, val by: String, val fromNow: Long, val fromNowText: String)

data class DetailUiStoryState(val id: Int, val title: String, val url: Url, val commentIds: List<Int>?, val descendants: Int?)

data class DetailUiState(
    val storyId: Int = -1,
    val story: Data<DetailUiStoryState, DetailError> = Data.Initial,
    val comments: Data<List<DetailUiCommentRowState>, DetailError> = Data.Initial
) : State

sealed class DetailError {
    class LoadStoryError(val error: String) : DetailError()
    class LoadCommentsError(val error: String) : DetailError()
}

sealed class DetailAction : Action {
    class SetInitialStory(val state: DetailUiStoryState) : DetailAction()
    object LoadStory : DetailAction()
    object LoadStoryComments : DetailAction()

    class Success<T>(val fromAction: Action, val value: T) : DetailAction()
    class Failure(val fromAction: Action, val error: String) : DetailAction()
}

class DetailReducer : Reducer<DetailUiState, DetailAction> {

    override fun reduce(currentState: DetailUiState, action: DetailAction): DetailUiState {
        return when (action) {
            is DetailAction.SetInitialStory -> {
                currentState.copy(storyId = action.state.id, story = Data.Success(action.state))
            }

            is DetailAction.LoadStory -> {
                currentState.copy(story = Data.Loading)
            }

            is DetailAction.LoadStoryComments -> {
                currentState.copy(comments = Data.Loading)
            }

            is DetailAction.Success<*> -> {
                require(action.fromAction is DetailAction.LoadStory || action.fromAction is DetailAction.LoadStoryComments)

                return with(currentState) {
                    if (action.fromAction is DetailAction.LoadStory) {
                        copy(story = Data.Success(action.value!! as DetailUiStoryState))
                    } else {
                        copy(comments = Data.Success((action.value as? List<DetailUiCommentRowState>) ?: emptyList()))
                    }
                }
            }

            is DetailAction.Failure -> {
                require(action.fromAction is DetailAction.LoadStory || action.fromAction is DetailAction.LoadStoryComments)

                return with(currentState) {
                    if (action.fromAction is DetailAction.LoadStory) {
                        copy(story = Data.Failure(DetailError.LoadStoryError(action.error)))
                    } else {
                        println(action.error)
                        copy(comments = Data.Failure(DetailError.LoadCommentsError(action.error)))
                    }
                }
            }
        }
    }
}

class DetailEnvironment(val scope: CoroutineScope, val repository: HackerNewsRepository) : Environment

class DetailDataMiddleware(
    override val environment: DetailEnvironment,
    private val detailUiStoryStateMapper: Mapper<Story, DetailUiStoryState>,
    private val detailUiCommentRowStateMapper: Mapper<Comment, DetailUiCommentRowState>
) : Middleware<DetailUiState, DetailAction, DetailEnvironment> {

    override fun process(order: Order, store: StoreType<DetailUiState, DetailAction, DetailEnvironment>, state: DetailUiState, action: DetailAction) {
        when (order) {
            Order.BeforeReducingState -> {
                when (action) {
                    is DetailAction.LoadStory -> {
                        // the current loading is already in-flight
                        if (state.story is Data.Loading) return

                        with(environment) {
                            scope.launch {
                                val result = repository.getStory(state.storyId)
                                result.fold(success = {
                                    store.dispatch(DetailAction.Success(action, detailUiStoryStateMapper.map(it)))
                                }, failure = {
                                    store.dispatch(DetailAction.Failure(action, it.message ?: "Unknown error"))
                                })
                            }
                        }
                    }

                    is DetailAction.LoadStoryComments -> {
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
                                    store.dispatch((DetailAction.Success(action, it?.map(detailUiCommentRowStateMapper::map))))
                                }, failure = {
                                    store.dispatch(DetailAction.Failure(action, it.message ?: "Unknown error"))
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

internal val detailUiStoryStateMapper = object : Mapper<Story, DetailUiStoryState> {
    override fun map(t: Story): DetailUiStoryState {
        return DetailUiStoryState(id = t.id, title = t.title, url = Url(t.url), commentIds = t.kids, descendants = t.descendants)
    }
}

@OptIn(ExperimentalTime::class)
internal val detailUiCommentRowStateMapper = object : Mapper<Comment, DetailUiCommentRowState> {

    override fun map(t: Comment): DetailUiCommentRowState {
        val now = Clock.System.now()
        val diff = now.epochSeconds - t.time
        return DetailUiCommentRowState(text = t.text, by = t.by, fromNow = diff, fromNowText = diff.toHumanConsumableText())
    }
}

typealias Store = StoreType<DetailUiState, DetailAction, DetailEnvironment>
