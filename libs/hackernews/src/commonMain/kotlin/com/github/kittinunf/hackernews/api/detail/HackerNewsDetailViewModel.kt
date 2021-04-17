package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.NativeViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsService
import com.github.kittinunf.redux.createStore
import kotlinx.coroutines.launch

class HackerNewsDetailViewModel(private val service: HackerNewsService) : NativeViewModel() {

    private val store: Store by lazy {
        createStore(
            scope = scope,
            initialState = DetailUiState(),
            reducer = DetailReducer(),
            middleware = DetailDataMiddleware(DetailEnvironment(scope, HackerNewsRepositoryImpl(service = service)), detailUiStoryStateMapper, detailUiCommentRowStateMapper)
        )
    }

    @Suppress("Unused")
    val currentState
        get() = store.currentState

    val states = store.states

    fun setInitialStory(state: DetailUiStoryState) {
        scope.launch {
            store.dispatch(SetInitialStory(state))
        }
    }

    fun loadStory() {
        scope.launch {
            store.dispatch(LoadStory)
        }
    }

    fun loadStoryComments() {
        scope.launch {
            store.dispatch(LoadStoryComments)
        }
    }
}
