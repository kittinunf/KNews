package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.repository.createRandomComment
import com.github.kittinunf.hackernews.repository.createRandomStory
import com.github.kittinunf.hackernews.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HackerNewsDetailReducerTest {

    private val mockStoryId = 10

    @Test
    fun `should bring uiState to initial success story state when set initial story action is dispatched`() {
        val currentState = DetailUiState(10)
        val (_, reducer) = SetInitialStoryReducer()
        val state = reducer.reduce(currentState, SetInitialStory(detailUiStoryStateMapper.map(createRandomStory(10))))

        val (value, _) = state.story
        assertNotNull(value)
        assertEquals(10, value.id)
        assertEquals("Story10", value.title)
    }

    @Test
    fun `should bring uiState to Load state when load story action is dispatched`() {
        val currentState = DetailUiState(mockStoryId)
        val (_, reducer) = LoadStoryReducer()
        val state = reducer.reduce(currentState, LoadStory)

        assertEquals(Data.Loading, state.story)
    }

    @Test
    fun `should bring uiState to Load state when load comment action is dispatched`() {
        val currentState = DetailUiState(mockStoryId)
        val (_, reducer) = LoadStoryCommentsReducer()
        val state = reducer.reduce(currentState, LoadStoryComments)
        assertEquals(Data.Loading, state.comments)
    }

    @Test
    fun `should bring uiState to Success state when load story action is ended with success`() {
        val currentState = DetailUiState(mockStoryId)
        val (_, reducer) = ResultActionReducer()
        val state = reducer.reduce(currentState, ResultAction(LoadStory, Result.success(detailUiStoryStateMapper.map(createRandomStory(10)))))

        assertTrue(state.story.isSuccess)

        val (value, _) = state.story
        assertNotNull(value)
        assertEquals(10, value.id)
        assertEquals("Story10", value.title)
    }

    @Test
    fun `should bring uiState to Success state when load comment action is ended with success`() {
        val currentState = DetailUiState(mockStoryId)
        val (_, reducer) = ResultActionReducer()
        val state = reducer.reduce(currentState, ResultAction(LoadStoryComments, Result.success((1..3).map(::createRandomComment).map(detailUiCommentRowStateMapper::map))))

        assertTrue(state.comments.isSuccess)

        val (value, _) = state.comments
        assertNotNull(value)
        assertEquals("Comment1", value[0].text)
        assertEquals("Comment2", value[1].text)
    }

    @Test
    fun `should bring uiState to Failure state when load story action is ended with failure`() {
        val currentState = DetailUiState(mockStoryId)
        val (_,reducer) = ResultActionReducer()
        val state = reducer.reduce(currentState, ResultAction(LoadStory, Result.error(LoadStoryError("Cannot load story"))))

        assertTrue(state.story.isFailure)

        val (_, err) = state.story
        assertNotNull(err)
        assertTrue(err is LoadStoryError)
        assertEquals("Cannot load story", err.error)
    }

    @Test
    fun `should bring uiState to Failure state when load comment action is ended with failure`() {
        val currentState = DetailUiState(mockStoryId)
        val (_, reducer) = ResultActionReducer()
        val state = reducer.reduce(currentState, ResultAction(LoadStoryComments, Result.error(LoadStoryCommentsError("Cannot load comments"))))

        assertTrue(state.comments.isFailure)

        val (_, err) = state.comments
        assertNotNull(err)
        assertTrue(err is LoadStoryCommentsError)
        assertEquals("Cannot load comments", err.error)
    }
}
