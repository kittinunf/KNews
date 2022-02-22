package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.ViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HackerNewsListViewModel(override val scope: CoroutineScope, private val service: HackerNewsService) : ViewModel() {

    private val store by lazy { ListStore(scope = scope, dispatcher = defaultDispatchers, repository = HackerNewsRepositoryImpl(service)) }

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
