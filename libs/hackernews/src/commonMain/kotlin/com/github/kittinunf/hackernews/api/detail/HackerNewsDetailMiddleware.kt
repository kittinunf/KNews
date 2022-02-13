package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.cored.Order
import com.github.kittinunf.cored.middleware
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
internal fun LoadStoryEffect(scope: CoroutineScope, dispatcher: CoroutineDispatcher, repository: HackerNewsRepository, mapper: Mapper<Story, DetailUiStoryState>) = middleware { order, store, state: DetailUiState, action: LoadStory ->
    // we only perform side effect in the beforeReduce order
    if (order == Order.AfterReduce) return@middleware
    // the current loading is already in-flight
    if (state.story is Data.Loading) return@middleware

    scope.launch {
        val result = withContext(dispatcher) { repository.getStory(state.storyId) }

        result.fold(success = {
            store.dispatch(LoadStoryResult(Result.success(mapper(it))))
        }, failure = {
            store.dispatch(LoadStoryResult(Result.failure(LoadStoryError(it.message ?: "Unknown error"))))
        })
    }
}

@Suppress("FunctionName")
internal fun LoadStoryCommentsEffect(scope: CoroutineScope, dispatcher: CoroutineDispatcher, repository: HackerNewsRepository, mapper: Mapper<Comment, DetailUiCommentRowState>) = middleware { order, store, state: DetailUiState, action: LoadStoryComments ->
    // we only perform side effect in the beforeReduce order
    if (order == Order.AfterReduce) return@middleware
    // the current loading is already in-flight
    if (state.comments is Data.Loading) return@middleware

    scope.launch {
        // check value we have a valid story object, or we fetch the story first to get all of the kids then fetch the comment
        val result = withContext(dispatcher) {
            if (state.story is Data.Success) {
                val ids = state.story.value?.commentIds
                if (ids == null) Result.success<List<Comment>?>(null) else repository.getComments(state.story.value.commentIds)
            } else repository.getStoryComments(state.storyId)
        }

        result.fold(success = {
            store.dispatch(LoadStoryCommentsResult(Result.success(it?.map(mapper::invoke))))
        }, failure = {
            store.dispatch(LoadStoryCommentsResult(Result.failure(LoadStoryCommentsError(it.message ?: "Unknown error"))))
        })
    }
}
