package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.repository.createRandomStory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HackerNewsListReducerTest {

    private val reducer = ListReducer()

    @Test
    fun `should bring uiState to Load state when load action is dispatched`() {
        val currentState = ListUiState()
        val state = reducer.reduce(currentState, ListAction.LoadStories)

        assertEquals(Data.Loading, state.stories)
    }

    @Test
    fun `should bring uiState to Success state when load action is ended with success`() {
        val currentState = ListUiState(stories = Data.Loading)
        val state = reducer.reduce(currentState, ListAction.Success(ListAction.LoadStories, listOf(createRandomStory(1), createRandomStory(2)).map(listUiRowStateMapper::map)))

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
        val state = reducer.reduce(currentState, ListAction.Failure(ListAction.LoadStories, "Cannot load stories"))

        assertTrue(state.stories.isFailure)
        val (_, err) = state.stories
        assertNotNull(err)
        assertTrue(err is ListError.LoadStoriesError)
        assertEquals("Cannot load stories", err.error)
    }

    @Test
    fun `should bring uiState to LoadNext state when load next action is dispatched`() {
        val currentState = ListUiState()
        val state = reducer.reduce(currentState, ListAction.LoadNextStories(2))

        assertEquals(Data.Loading, state.nextStories)
    }

    @Test
    fun `should bring uiState to Success state when load next action is ended with success`() {
        val currentState = ListUiState(nextStories = Data.Loading)
        val state =
            reducer.reduce(currentState, ListAction.Success(ListAction.LoadNextStories(2), listOf(createRandomStory(3), createRandomStory(4)).map(listUiRowStateMapper::map)))

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
        val state = reducer.reduce(currentState, ListAction.Failure(ListAction.LoadNextStories(2), "Cannot load next stories"))

        assertTrue(state.nextStories.isFailure)
        val (_, err) = state.nextStories
        assertNotNull(err)
        assertTrue(err is ListError.LoadNextStoriesError)
        assertEquals("Cannot load next stories", err.error)
    }

    @Test
    fun `should add the stories into the main stories list after load next action is ended with success`() {
        val currentState = ListUiState(stories = Data.Success(listOf(createRandomStory(1), createRandomStory(2)).map(listUiRowStateMapper::map)))
        val state = reducer.reduce(
            currentState, ListAction.Success(
                ListAction.LoadNextStories(page = 2), listOf(createRandomStory(3), createRandomStory(4), createRandomStory(5)).map(listUiRowStateMapper::map)
            )
        )

        assertTrue(state.stories.isSuccess)
        val (value, _) = state.stories
        assertNotNull(value)
        assertEquals(5, value.size)
    }

    @Test
    fun `should not do anything with the main stories list after load next action is ended with failure`() {
        val currentState = ListUiState(stories = Data.Initial)
        val state = reducer.reduce(currentState, ListAction.Success(ListAction.LoadNextStories(page = 2), listOf(createRandomStory(1)).map(listUiRowStateMapper::map)))

        val (value, error) = state.stories
        assertNull(value)
        assertNull(error)
    }

    @Test
    fun `should sort according to the current sorting condition`() {
        val currentState = ListUiState(sortCondition = ListUiSortCondition.Score, stories = Data.Initial)
        val state = reducer.reduce(
            currentState, ListAction.Success(ListAction.LoadStories, listOf(createRandomStory(1), createRandomStory(2), createRandomStory(4)).map(listUiRowStateMapper::map))
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
        val state0 = reducer.reduce(
            currentState, ListAction.Success(ListAction.LoadStories, listOf(createRandomStory(1), createRandomStory(2), createRandomStory(4)).map(listUiRowStateMapper::map))
        )

        val (value0, _) = state0.stories
        assertNotNull(value0)
        assertEquals(100, value0[0].score)
        assertEquals(200, value0[1].score)
        assertEquals(400, value0[2].score)

        val state1 = reducer.reduce(state0, ListAction.Sort(ListUiSortCondition.Score))

        val (value1, _) = state1.stories
        assertNotNull(value1)
        assertEquals(400, value1[0].score)
        assertEquals(200, value1[1].score)
        assertEquals(100, value1[2].score)
    }

    @Test
    fun `should not do sort for stories state other than success`() {
        val currentState = ListUiState()

        val state0 = reducer.reduce(currentState, ListAction.Failure(ListAction.LoadStories, "Error loading stories"))
        assertEquals(state0.sortCondition, ListUiSortCondition.None)

        val state1 = reducer.reduce(state0, ListAction.Sort(ListUiSortCondition.Recent))

        assertTrue(state1.stories.isFailure)
        assertEquals(state1.sortCondition, ListUiSortCondition.Recent)
    }

    @Test
    fun `should also sort the next page that has included in the stories`() {
        val currentState = ListUiState(stories = Data.Success(listOf(createRandomStory(1), createRandomStory(2)).map(listUiRowStateMapper::map)))

        val state0 = reducer.reduce(
            currentState, ListAction.Success(
                ListAction.LoadNextStories(page = 2), listOf(createRandomStory(3), createRandomStory(4), createRandomStory(5)).map(listUiRowStateMapper::map)
            )
        )

        val state1 = reducer.reduce(state0, ListAction.Sort(ListUiSortCondition.Score))

        val (value1, _) = state1.stories
        assertNotNull(value1)
        assertEquals(5, value1.size)
        assertEquals(500, value1[0].score)
        assertEquals(400, value1[1].score)
        assertEquals(300, value1[2].score)
    }
}
