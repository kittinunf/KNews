package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.NativeViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsService
import kotlinx.coroutines.launch

class HackerNewsListViewModel(private val service: HackerNewsService) : NativeViewModel() {

    private val store: Store by lazy { ListStore(scope, ListEnvironment(scope, HackerNewsRepositoryImpl(service))) }

    @Suppress("Unused")
    val currentState
        get() = store.currentState

    val states = store.states

    private var currentPage = 1

    fun loadStories() {
        currentPage = 1
        scope.launch {
            store.dispatch(LoadStories)
        }
    }

    fun loadNextStories() {
        currentPage += 1
        scope.launch {
            store.dispatch(LoadNextStories(currentPage))
        }
    }

    fun sortBy(sortCondition: ListUiSortCondition) {
        scope.launch {
            store.dispatch(Sort(sortCondition))
        }
    }
}
