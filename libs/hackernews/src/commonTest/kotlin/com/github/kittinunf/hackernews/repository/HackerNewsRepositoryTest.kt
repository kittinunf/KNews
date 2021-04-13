package com.github.kittinunf.hackernews.repository

import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.util.Result
import com.github.kittinunf.hackernews.util.runBlockingTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HackerNewsRepositoryTest {

    private val repository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService((1..10).toList()))

    @Test
    fun `should return a story`() {
        val result = runBlockingTest {
            repository.getStory(1)
        }

        assertTrue(result is Result.Success)
        val story = result.get()
        assertEquals(1, story.id)
        assertEquals("Story1", story.title)
        assertEquals("http://1.com", story.url)
    }

    @Test
    fun `should not return a story`() {
        val result = runBlockingTest {
            repository.getStory(1000)
        }

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should return a comment`() {
        val result = runBlockingTest {
            repository.getComment(1)
        }

        assertTrue(result is Result.Success)
        val comment = result.get()
        assertEquals(1, comment.id)
        assertEquals(11, comment.parent)
        assertEquals("Comment1", comment.text)
    }

    @Test
    fun `should not return a comment`() {
        val result = runBlockingTest {
            repository.getStory(1000)
        }

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should return top stories in a consumable format with page`() {
        val result = runBlockingTest { repository.getTopStories(1) }
        assertTrue(result is Result.Success)

        val stories1 = result.get()

        val anotherResult = runBlockingTest { repository.getTopStories(2) }
        assertTrue(anotherResult is Result.Success)

        val stories2 = anotherResult.get()

        assertNotNull(stories1)
        assertNotNull(stories2)

        stories1.forEachIndexed { index, story ->
            assertEquals(index + 1, story.id)
            assertEquals(index + 101, story.time)
        }
        stories2.forEachIndexed { index, story ->
            assertEquals(index + 6, story.id)
            assertEquals(index + 106, story.time)
        }
    }

    @Test
    fun `should return null top stories if the page is over the available items`() {
        val result = runBlockingTest { repository.getTopStories(3) }

        assertTrue(result is Result.Success)

        val stories = result.get()

        assertNull(stories)
    }

    @Test
    fun `should return failure if the top stories is failure to fetch`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsTopStoriesFailureMockService())

        val result = runBlockingTest { repository.getTopStories(1) }

        assertTrue(result is Result.Failure)
        assertTrue(result.error is NotImplementedError)
    }

    @Test
    fun `should return failure if the individual story is failure to fetch`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsStoryFailureMockService())

        val result = runBlockingTest { repository.getTopStories(1) }

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should return comments of a story`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService(((1..10).toList())))

        val result = runBlockingTest { repository.getStoryComments(1) }

        assertTrue(result is Result.Success)

        val (value, _) = result
        assertNotNull(value)
        assertEquals(11, value[0].id)
        assertEquals("Comment11", value[0].text)
        assertEquals(21, value[1].id)
        assertEquals(221, value[1].time)
        assertEquals(31, value[2].id)
    }

    @Test
    fun `should return comments of a story with a list of ids`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService(((1..10).toList())))

        val result = runBlockingTest { repository.getComments(listOf(11, 21, 31)) }

        assertTrue(result is Result.Success)

        val (value, _) = result
        assertNotNull(value)

        assertEquals(11, value[0].id)
        assertEquals("Comment11", value[0].text)
        assertEquals(21, value[1].id)
        assertEquals(221, value[1].time)
        assertEquals(31, value[2].id)
    }

    @Test
    fun `should return null comments of a story without the comments`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService(((1..10).toList())))

        val result = runBlockingTest { repository.getStoryComments(100) }

        assertTrue(result is Result.Success)

        val (value, _) = result
        assertNull(value)
    }

    @Test
    fun `should return failure if the individual comment is not found`() {
        val repository = HackerNewsRepositoryImpl(HackerNewsSuccessfulMockService(((1..10).toList())))

        val result = runBlockingTest { repository.getStoryComments(101) }

        assertTrue(result is Result.Failure)

        val (_, error) = result
        assertNotNull(error)
    }
}

class HackerNewsStoryFailureMockService : HackerNewsService {

    override suspend fun getTopStories(): Result<List<Int>, Throwable> = Result.success(listOf(1, 2, 3, 4, 5, 6))

    override suspend fun getStory(id: Int): Result<Story, Throwable> = Result.error(NotImplementedError())

    override suspend fun getComment(id: Int): Result<Comment, Throwable> = Result.error(NotImplementedError())
}

class HackerNewsTopStoriesFailureMockService : HackerNewsService {

    override suspend fun getTopStories(): Result<List<Int>, Throwable> = Result.error(NotImplementedError())

    override suspend fun getStory(id: Int): Result<Story, Throwable> = Result.error(NotImplementedError())

    override suspend fun getComment(id: Int): Result<Comment, Throwable> = Result.error(NotImplementedError())
}

class HackerNewsSuccessfulMockService(private val list: List<Int>, private val withDelayMillis: Long = 0) : HackerNewsService {

    private val inMemoryStories = hashMapOf<Int, Story>().apply {
        list.forEach { put(it, createRandomStory(it)) }

        // set up no kids story
        put(100, Story(100, "Story100", "http://100.com", 100 * 100, "Ann100", 200, null, 10))
    }

    private val inMemoryComments = hashMapOf<Int, Comment>().apply {
        list.forEach { put(it, createRandomComment(it)) }

        // set up some comment with parent
        put(11, createRandomComment(11))
        put(21, createRandomComment(21))
        put(31, createRandomComment(31))
    }

    override suspend fun getTopStories(): Result<List<Int>, Throwable> {
        delay(withDelayMillis)
        return Result.success(list)
    }

    override suspend fun getStory(id: Int): Result<Story, Throwable> {
        val story = inMemoryStories[id] ?: return Result.error(IllegalArgumentException("No story found"))
        return Result.success(story)
    }

    override suspend fun getComment(id: Int): Result<Comment, Throwable> {
        val comment = inMemoryComments[id] ?: return Result.error(IllegalArgumentException("No comment found"))
        return Result.success(comment)
    }
}

// 1*10 + 1 = 11, 21, 31 | 12, 22, 32
internal fun createRandomStory(id: Int) = Story(id, "Story$id", "http://$id.com", 100 * id, "Ann$id", 100 + id, (1..3).map { id + 10 * it }, 10)

internal fun createRandomComment(id: Int) = Comment(id, 10 + id, "Comment$id", "Ann$id", 200 + id, (0 until 3).map { id + 10 * it })
