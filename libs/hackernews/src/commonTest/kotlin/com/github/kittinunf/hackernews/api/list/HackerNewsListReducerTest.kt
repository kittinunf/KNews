package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.common.ResultAction
import com.github.kittinunf.hackernews.repository.createRandomStory
import com.github.kittinunf.hackernews.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HackerNewsListReducerTest {

    @Test
    fun `should bring uiState to Load state when load action is dispatched`() {
        val currentState = ListUiState()
        val (_, reducer) = LoadStoriesReducer()
        val state = reducer(currentState, LoadStories)

        assertEquals(Data.Loading, state.stories)
    }

    @Test
    fun `should bring uiState to Success state when load action is ended with success`() {
        val currentState = ListUiState(stories = Data.Loading)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(currentState, ResultAction(LoadStories, Result.success(listOf(createRandomStory(1), createRandomStory(2)).map(::listUiRowStateMapper))))

        assertTrue(state.stories.isSuccess)
        val (value, _) = state.stories
        assertNotNull(value)
        assertEquals(2, value.size)
        assertEquals(1, value[0].id)
        assertEquals("Story1", value[0].title)
        assertEquals(2, value[1].id)
        assertEquals("Story2", value[1].title)
    }

    @Test
    fun `should bring uiState to Failure state when load action is ended with failure`() {
        val currentState = ListUiState(stories = Data.Loading)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(currentState, ResultAction(LoadStories, Result.error(LoadStoriesError("Cannot load stories"))))

        assertTrue(state.stories.isFailure)
        val (_, err) = state.stories
        assertNotNull(err)
        assertTrue(err is LoadStoriesError)
        assertEquals("Cannot load stories", err.error)
    }

    @Test
    fun `should bring uiState to LoadNext state when load next action is dispatched`() {
        val currentState = ListUiState()
        val (_, reducer) = LoadNextStoriesReducer()
        val state = reducer(currentState, LoadNextStories(2))

        assertEquals(Data.Loading, state.nextStories)
    }

    @Test
    fun `should bring uiState to Success state when load next action is ended with success`() {
        val currentState = ListUiState(nextStories = Data.Loading)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(currentState, ResultAction(LoadNextStories(2), Result.success(listOf(createRandomStory(3), createRandomStory(4)).map(::listUiRowStateMapper))))

        assertTrue(state.nextStories.isSuccess)
        val (value, _) = state.nextStories
        assertNotNull(value)
        assertEquals(2, value.size)
        assertEquals(3, value[0].id)
        assertEquals(4, value[1].id)
    }

    @Test
    fun `should bring uiState to Failure state when load next action is ended with failure`() {
        val currentState = ListUiState(nextStories = Data.Loading)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(currentState, ResultAction(LoadNextStories(2), Result.error(LoadNextStoriesError("Cannot load next stories"))))

        assertTrue(state.nextStories.isFailure)
        val (_, err) = state.nextStories
        assertNotNull(err)
        assertTrue(err is LoadNextStoriesError)
        assertEquals("Cannot load next stories", err.error)
    }

    @Test
    fun `should add the stories into the main stories list after load next action is ended with success`() {
        val currentState = ListUiState(stories = Data.Success(listOf(createRandomStory(1), createRandomStory(2)).map(::listUiRowStateMapper)))
        val (_, reducer) = ResultActionReducer()
        val state = reducer(
            currentState,
            ResultAction(LoadNextStories(page = 2), Result.success(listOf(createRandomStory(3), createRandomStory(4), createRandomStory(5)).map(::listUiRowStateMapper)))
        )

        assertTrue(state.stories.isSuccess)
        val (value, _) = state.stories
        assertNotNull(value)
        assertEquals(5, value.size)
    }

    @Test
    fun `should not do anything with the main stories list after load next action is ended with failure`() {
        val currentState = ListUiState(stories = Data.Initial)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(currentState, ResultAction(LoadNextStories(page = 2), Result.success(listOf(createRandomStory(1)).map(::listUiRowStateMapper))))

        val (value, error) = state.stories
        assertNull(value)
        assertNull(error)
    }

    @Test
    fun `should sort according to the current sorting condition`() {
        val currentState = ListUiState(sortCondition = ListUiSortCondition.Score, stories = Data.Initial)
        val (_, reducer) = ResultActionReducer()
        val state = reducer(
            currentState, ResultAction(LoadStories, Result.success(listOf(createRandomStory(1), createRandomStory(2), createRandomStory(4)).map(::listUiRowStateMapper)))
        )

        val (value, _) = state.stories
        assertNotNull(value)
        assertEquals(400, value[0].score)
        assertEquals(200, value[1].score)
        assertEquals(100, value[2].score)
    }

    @Test
    fun `should sort according to the update sorting condition`() {
        val currentState = ListUiState()
        val (_, reducer) = ResultActionReducer()
        val state0 = reducer(
            currentState, ResultAction(LoadStories, Result.success(listOf(createRandomStory(1), createRandomStory(2), createRandomStory(4)).map(::listUiRowStateMapper)))
        )

        val (value0, _) = state0.stories
        assertNotNull(value0)
        assertEquals(100, value0[0].score)
        assertEquals(200, value0[1].score)
        assertEquals(400, value0[2].score)

        val (_, sortReducer) = SortReducer()
        val state1 = sortReducer(state0, Sort(ListUiSortCondition.Score))

        val (value1, _) = state1.stories
        assertNotNull(value1)
        assertEquals(400, value1[0].score)
        assertEquals(200, value1[1].score)
        assertEquals(100, value1[2].score)
    }

    @Test
    fun `should not do sort for stories state other than success`() {
        val currentState = ListUiState()
        val (_, reducer) = ResultActionReducer()

        val state0 = reducer(currentState, ResultAction(LoadStories, Result.error(LoadNextStoriesError("Error loading stories"))))
        assertEquals(state0.sortCondition, ListUiSortCondition.None)

        val (_, sortReducer) = SortReducer()
        val state1 = sortReducer(state0, Sort(ListUiSortCondition.Recent))

        assertTrue(state1.stories.isFailure)
        assertEquals(state1.sortCondition, ListUiSortCondition.Recent)
    }

    @Test
    fun `should also sort the next page that has included in the stories`() {
        val currentState = ListUiState(stories = Data.Success(listOf(createRandomStory(1), createRandomStory(2)).map(::listUiRowStateMapper)))
        val (_, reducer) = ResultActionReducer()

        val state0 = reducer(
            currentState,
            ResultAction(LoadNextStories(page = 2), Result.success(listOf(createRandomStory(3), createRandomStory(4), createRandomStory(5)).map(::listUiRowStateMapper)))
        )

        val (_, sortReducer) = SortReducer()
        val state1 = sortReducer(state0, Sort(ListUiSortCondition.Score))

        val (value1, _) = state1.stories
        assertNotNull(value1)
        assertEquals(5, value1.size)
        assertEquals(500, value1[0].score)
        assertEquals(400, value1[1].score)
        assertEquals(300, value1[2].score)
    }
}
