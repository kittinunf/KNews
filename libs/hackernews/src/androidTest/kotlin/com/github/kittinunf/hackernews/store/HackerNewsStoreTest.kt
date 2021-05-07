package com.github.kittinunf.hackernews.store

import com.github.kittinunf.hackernews.api.detail.DetailEnvironment
import com.github.kittinunf.hackernews.api.detail.DetailStore
import com.github.kittinunf.hackernews.api.detail.DetailUiState
import com.github.kittinunf.hackernews.api.detail.LoadStoryComments
import com.github.kittinunf.hackernews.api.detail.detailUiCommentRowStateMapper
import com.github.kittinunf.hackernews.api.detail.detailUiStoryStateMapper
import com.github.kittinunf.hackernews.api.list.ListEnvironment
import com.github.kittinunf.hackernews.api.list.ListStore
import com.github.kittinunf.hackernews.api.list.ListUiSortCondition
import com.github.kittinunf.hackernews.api.list.LoadNextStories
import com.github.kittinunf.hackernews.api.list.LoadStories
import com.github.kittinunf.hackernews.api.list.Sort
import com.github.kittinunf.hackernews.api.list.printDebug
import com.github.kittinunf.hackernews.network.NetworkModule
import com.github.kittinunf.hackernews.network.addBaseUrl
import com.github.kittinunf.hackernews.network.addHackerNewsJsonSerializer
import com.github.kittinunf.hackernews.network.addLogging
import com.github.kittinunf.hackernews.network.createHttpClient
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsServiceImpl
import com.github.kittinunf.hackernews.util.runBlockingTest
import com.github.kittinunf.cored.createStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import org.junit.Test

class HackerNewsStoreTest {

    private val network = NetworkModule(createHttpClient {
        addBaseUrl("https://hacker-news.firebaseio.com/")
        addHackerNewsJsonSerializer()
        addLogging()
    })

    private val service = HackerNewsServiceImpl(network)
    private val testRepository = HackerNewsRepositoryImpl(service)
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `list screen`() {
        val store = ListStore(testScope, ListEnvironment(testScope, testRepository))

        runBlockingTest {
            store.states
                .printDebug()
                .launchIn(testScope)

            store.dispatch(LoadStories)

            delay(2000)

            store.dispatch(LoadNextStories(2))

            delay(2000)

            store.dispatch(LoadNextStories(3))

            delay(2000)

            store.dispatch(Sort(ListUiSortCondition.Title))

            delay(500)
        }
    }

    @Test
    fun `detail screen`() {
        val store = DetailStore(DetailUiState(26688965), testScope, DetailEnvironment(testScope, testRepository))

        runBlockingTest {
            store.states
                .printDebug()
                .launchIn(testScope)

            store.dispatch(LoadStoryComments)

            delay(4600)
        }
    }
}
