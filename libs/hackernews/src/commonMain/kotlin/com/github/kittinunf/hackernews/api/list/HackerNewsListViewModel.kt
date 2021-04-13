package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.NativeViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsService
import com.github.kittinunf.redux.createStore
import kotlinx.coroutines.launch

class HackerNewsListViewModel(private val service: HackerNewsService) : NativeViewModel() {

    private val store: Store by lazy {
        createStore(
            scope = scope,
            initialState = ListUiState(),
            reducer = ListReducer(),
            middleware = ListDataMiddleware(ListEnvironment(scope, HackerNewsRepositoryImpl(service = service)), listUiRowStateMapper)
        )
    }

    @Suppress("Unused")
    val currentState
        get() = store.currentState

    val states = store.states

    private var currentPage = 1

    fun loadStories() {
        currentPage = 1
        scope.launch {
            store.dispatch(ListAction.LoadStories)
        }
    }

    fun loadNextStories() {
        currentPage += 1
        scope.launch {
            store.dispatch(ListAction.LoadNextStories(currentPage))
        }
    }

    fun sortBy(sortCondition: ListUiSortCondition) {
        scope.launch {
            store.dispatch(ListAction.Sort(sortCondition))
        }
    }
}
