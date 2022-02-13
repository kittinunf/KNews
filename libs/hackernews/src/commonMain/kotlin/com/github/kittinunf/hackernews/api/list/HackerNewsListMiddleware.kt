package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.cored.Order
import com.github.kittinunf.cored.middleware
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.HackerNewsRepository
import com.github.kittinunf.hackernews.util.Mapper
import com.github.kittinunf.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
internal fun LoadStoriesEffect(scope: CoroutineScope, dispatcher: CoroutineDispatcher, repository: HackerNewsRepository, mapper: Mapper<Story, ListUiRowState>) = middleware { order, store, state: ListUiState, action: LoadStories ->
    // we only perform side effect in the beforeReduce order
    if (order == Order.AfterReduce) return@middleware
    // the current loading is already in-flight
    if (state.stories is Data.Loading) return@middleware

    scope.launch {
        val result = withContext(dispatcher) { repository.getTopStories() }
        result.fold(success = {
            store.dispatch(LoadStoriesResult(Result.success(it?.map(mapper::invoke).orEmpty())))
        }, failure = {
            store.dispatch(LoadStoriesResult(Result.failure(LoadStoriesError(it.message ?: "Unknown error"))))
        })
    }
}

@Suppress("FunctionName")
internal fun LoadNextStoriesEffect(scope: CoroutineScope, dispatcher: CoroutineDispatcher, repository: HackerNewsRepository, mapper: Mapper<Story, ListUiRowState>) = middleware { order, store, state: ListUiState, action: LoadNextStories ->
    // we only perform side effect in the beforeReduce order
    if (order == Order.AfterReduce) return@middleware
    // the current loading is already in-flight and the main list is not done yet
    if (state.nextStories is Data.Loading) return@middleware

    if (state.stories.isSuccess.not()) {
        scope.launch { store.dispatch(LoadNextStoriesResult(Result.failure(LoadNextStoriesError("Data inconsistency, not loading next page")))) }
        return@middleware
    }

    scope.launch {
        val result = withContext(dispatcher) { repository.getTopStories(action.page) }
        result.fold(success = {
            store.dispatch(LoadNextStoriesResult(Result.success(it?.map(mapper::invoke))))
        }, failure = {
            store.dispatch(LoadNextStoriesResult(Result.failure(LoadNextStoriesError(it.message ?: "Unknown error"))))
        })
    }
}
