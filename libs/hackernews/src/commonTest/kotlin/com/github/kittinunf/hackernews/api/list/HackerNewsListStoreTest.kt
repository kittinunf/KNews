package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsStoryFailureMockService
import com.github.kittinunf.hackernews.repository.HackerNewsStoryNextPageFailureMockService
import com.github.kittinunf.hackernews.repository.HackerNewsSuccessfulMockService
import com.github.kittinunf.hackernews.util.runBlockingTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HackerNewsListStoreTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val testRepository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService((1..10).toList(), 0))
    private val store = ListStore(testScope, ListEnvironment(testScope, testRepository))

    @Test
    fun `should update the stories state with loading then ended with success`() {
        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> {
                            assertEquals(Data.Initial, state.stories)
                            assertEquals(Data.Initial, state.nextStories)
                        }
                        1 -> {
                            assertTrue(state.stories is Data.Loading)
                            assertEquals(Data.Initial, state.nextStories)
                        }
                        2 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
        }
    }

    @Test
    fun `should update the next stories state with loading then ended with success`() {
        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)
                        }
                        3 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)

                            assertTrue(state.nextStories is Data.Loading)
                        }
                        4 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(10, value?.size ?: 0)

                            val (nextStories, _) = state.nextStories
                            assertTrue(state.nextStories is Data.Success)
                            assertEquals(5, nextStories?.size ?: 0)
                        }
                        5 -> {
                            assertTrue(state.nextStories is Data.Loading)
                        }
                        6 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(10, value?.size ?: 0) // it is still 10 because we don't have anymore data
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
            store.dispatch(LoadNextStories(2))
            store.dispatch(LoadNextStories(3))
        }
    }

    @Test
    fun `should update the stories state with loading then ended with failure`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsStoryFailureMockService())

        val store = ListStore(testScope, ListEnvironment(testScope, repository))

        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (_, err) = state.stories
                            assertTrue(state.stories is Data.Failure)
                            assertTrue(err is LoadStoriesError)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
        }
    }

    @Test
    fun `should update the next stories state with loading then ended with failure`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsStoryFailureMockService())

        val store = ListStore(testScope, ListEnvironment(testScope, repository))

        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (_, err) = state.stories
                            assertTrue(state.stories is Data.Failure)
                            assertTrue(err is LoadStoriesError)
                        }
                        3 -> {
                            assertTrue(state.nextStories is Data.Loading)
                        }
                        4 -> {
                            val (_, err) = state.nextStories
                            assertTrue(state.nextStories is Data.Failure)
                            assertTrue(err is LoadNextStoriesError)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
            store.dispatch(LoadNextStories(2))
        }
    }

    @Test
    fun `should update the next stories state with loading then end with failure while the stories is intact`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsStoryNextPageFailureMockService())

        val store = ListStore(testScope, ListEnvironment(testScope, repository))

        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)
                        }
                        3 -> {
                            assertTrue(state.nextStories is Data.Loading)
                        }
                        4 -> {
                            val (_, err) = state.nextStories
                            assertTrue(state.nextStories is Data.Failure)
                            assertTrue(err is LoadNextStoriesError)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
            store.dispatch(LoadNextStories(2))
        }
    }

    @Test
    fun `should sort the stories according to new sorting condition`() {
        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)
                        }
                        3 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)

                            assertTrue(state.nextStories is Data.Loading)
                        }
                        4 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(10, value?.size ?: 0)

                            val (nextStories, _) = state.nextStories
                            assertTrue(state.nextStories is Data.Success)
                            assertEquals(5, nextStories?.size ?: 0)
                        }
                        5 -> {
                            val (value, _) = state.stories
                            assertNotNull(value)
                            assertEquals(1000, value[0].score)
                            assertEquals(900, value[1].score)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStories)
            store.dispatch(LoadNextStories(2))
            store.dispatch(Sort(ListUiSortCondition.Score))
        }
    }

    @Test
    fun `should sort the stories according to the current sorting condition`() {
        runBlockingTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            val sortCondition = state.sortCondition
                            assertEquals(ListUiSortCondition.Score, sortCondition)
                        }
                        3 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)

                            assertNotNull(value)
                            assertEquals(500, value[0].score)
                            assertEquals(400, value[1].score)
                        }
                        4 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(5, value?.size ?: 0)

                            assertTrue(state.nextStories is Data.Loading)
                        }
                        5 -> {
                            val (value, _) = state.stories
                            assertTrue(state.stories is Data.Success)
                            assertEquals(10, value?.size ?: 0)

                            val (nextStories, _) = state.nextStories
                            assertTrue(state.nextStories is Data.Success)
                            assertEquals(5, nextStories?.size ?: 0)
                        }
                        6 -> {
                            val (value, _) = state.stories
                            assertNotNull(value)
                            assertEquals(1000, value[0].score)
                            assertEquals(900, value[1].score)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(Sort(ListUiSortCondition.Score))
            store.dispatch(LoadStories)
            store.dispatch(LoadNextStories(2))
        }
    }
}

fun <T> Flow<T>.printDebug() = onEach { println(it) }
