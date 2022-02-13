package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.list.printDebug
import com.github.kittinunf.hackernews.repository.HackerNewsRepositoryImpl
import com.github.kittinunf.hackernews.repository.HackerNewsSuccessfulMockService
import com.github.kittinunf.hackernews.repository.createRandomStory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HackerNewsDetailStoreTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val testRepository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService((1..10).toList(), 0))
    private val store = DetailStore(DetailUiState(1), testScope, Dispatchers.Unconfined, testRepository)

    @Test
    fun `should update the story state with loading then ended with success`() {
        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        0 -> {
                            assertEquals(Data.Initial, state.story)
                            assertEquals(Data.Initial, state.comments)
                        }
                        1 -> {
                            assertTrue(state.story is Data.Loading)
                        }
                        2 -> {
                            val (value, _) = state.story
                            assertNotNull(value)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStory)
        }
    }

    @Test
    fun `should update the story state without loading`() {
        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            val (value, _) = state.story
                            assertNotNull(value)
                            assertEquals(state.storyId, value.id)
                            assertEquals(listOf(11, 21, 31), value.commentIds)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(SetInitialStory(detailUiStoryStateMapper(createRandomStory(1))))
        }
    }

    @Test
    fun `should update the comments state with loading then ended with success`() {
        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        3 -> {
                            assertTrue(state.comments is Data.Loading)
                        }
                        4 -> {
                            val (value, _) = state.comments
                            assertNotNull(value)
                            assertEquals(3, value.size)
                            assertEquals("Comment11", value[0].text)
                            assertEquals("Ann21", value[1].by)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStory)
            store.dispatch(LoadStoryComments)
        }
    }

    @Test
    fun `should update the comment state even the story itself is not in the success`() {
        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            assertTrue(state.comments is Data.Loading)
                        }
                        2 -> {
                            val (value, _) = state.comments
                            assertNotNull(value)
                            assertEquals(3, value.size)
                            assertEquals("Comment11", value[0].text)
                            assertEquals("Ann21", value[1].by)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(LoadStoryComments)
        }
    }

    @Test
    fun `should update the comment state when the story is already in success`() {
        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            val (value, _) = state.story
                            assertNotNull(value)
                            assertEquals(1, value.id)
                            assertEquals("http://1.com", value.url.toString())
                        }
                        2 -> {
                            assertTrue(state.comments is Data.Loading)
                        }
                        3 -> {
                            val (value, _) = state.comments
                            assertNotNull(value)
                            assertEquals(3, value.size)
                            assertEquals("Comment11", value[0].text)
                            assertEquals("Ann21", value[1].by)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            // load
            store.dispatch(SetInitialStory(detailUiStoryStateMapper(createRandomStory(1))))
            store.dispatch(LoadStoryComments)
        }
    }

    @Test
    fun `should update the story state with loading then ended with failure`() {
        val store = DetailStore(DetailUiState(101), testScope, Dispatchers.Unconfined, testRepository)

        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            assertTrue(state.story is Data.Loading)
                        }
                        2 -> {
                            val (_, err) = state.story
                            assertNotNull(err)
                            assertTrue(err is LoadStoryError)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            store.dispatch(LoadStory)
        }
    }

    @Test
    fun `should update the comments state with loading then ended with failure`() {
        val store = DetailStore(DetailUiState(5), testScope, Dispatchers.Unconfined, testRepository)

        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        2 -> {
                            val (_, err) = state.comments
                            assertNotNull(err)
                            assertTrue(err is LoadStoryCommentsError)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            store.dispatch(LoadStoryComments)
        }
    }

    @Test
    fun `should update the comment state with loading then ended with success for story without comments`() {
        val store = DetailStore(DetailUiState(100), testScope, Dispatchers.Unconfined, testRepository)

        runTest {
            store.states
                .withIndex()
                .onEach { (index, state) ->
                    when (index) {
                        1 -> {
                            assertTrue(state.story is Data.Loading)
                        }
                        2 -> {
                            val (value, _) = state.story
                            assertNotNull(value)

                            assertEquals(100, value.id)
                        }
                        3 -> {
                            assertTrue(state.comments is Data.Loading)
                        }
                        4 -> {
                            val (value, _) = state.comments
                            assertNull(value) // this represent story without comment
                            assertEquals(null, value?.size)
                        }
                    }
                }
                .printDebug()
                .launchIn(testScope)

            store.dispatch(LoadStory)
            store.dispatch(LoadStoryComments)
        }
    }
}
