package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.ViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HackerNewsDetailViewModel(state: DetailUiState, override val scope: CoroutineScope, private val service: HackerNewsService) : ViewModel() {

    private val store by lazy {
        DetailStore(
            initialState = state,
            scope = scope,
            dispatcher = defaultDispatchers,
            repository = HackerNewsRepositoryImpl(service)
        )
    }

    @Suppress("Unused")
    val currentState
        get() = store.currentState

    val states = store.states

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
