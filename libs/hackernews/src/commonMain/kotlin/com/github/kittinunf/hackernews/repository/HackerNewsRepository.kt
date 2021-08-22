package com.github.kittinunf.hackernews.repository

import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.lift

interface HackerNewsRepository {

    suspend fun getTopStories(page: Int = 1): Result<List<Story>?, Throwable>

    suspend fun getStory(id: Int): Result<Story, Throwable>

    suspend fun getStoryComments(id: Int): Result<List<Comment>?, Throwable>

    suspend fun getComment(id: Int): Result<Comment, Throwable>

    suspend fun getComments(list: List<Int>): Result<List<Comment>, Throwable>
}

class HackerNewsRepositoryImpl(private val service: HackerNewsService) : HackerNewsRepository {

    class InitialStateException : Throwable("Initial State: Repository has no data yet")

    private var cacheTopStories: Result<List<Int>, Throwable> = Result.failure(InitialStateException())

    companion object {
        const val defaultPageSize = 5
    }

    /**
     *  Calculate manual paging here, let's assume that the topstories returns the following ids
     *  [1,2,3,4,5,6,7,8]
     *
     *  pageSize = 3
     *  page = 1 [0, 2]
     *  page = 2 [3, 5]
     *  page = 3 [6, 7]
     *
     *  (page - 1)*pageSize ... ((page - 1)*pageSize + pageSize) - 1
     */
    override suspend fun getTopStories(page: Int): Result<List<Story>?, Throwable> {
        if (page <= 0) return Result.failure(IllegalArgumentException("Page is not less than or equal to 0"))

        if (page == 1 || cacheTopStories is Result.Failure) {
            cacheTopStories = service.getTopStories()
        }
        // we need to caches the top stories id
        return cacheTopStories.flatMap {
            val startIndex = ((page - 1) * defaultPageSize).coerceAtMost(it.size)
            val endIndex = (startIndex + defaultPageSize - 1).coerceAtMost(it.size)

            // this means that we don't have valid items for this page anymore (if both of them is equal last index when have just one item left
            if (startIndex == endIndex && startIndex != it.lastIndex) return Result.success(null)

            getStories(it.subList(startIndex, endIndex + 1)) // sublist is [from, to)
        }
    }

    override suspend fun getStory(id: Int): Result<Story, Throwable> {
        return service.getStory(id)
    }

    override suspend fun getStoryComments(id: Int): Result<List<Comment>?, Throwable> {
        return service.getStory(id).flatMap {
            if (it.kids == null) return Result.success(null)

            // it.kids is a list of first level comment
            getComments(it.kids)
        }
    }

    override suspend fun getComment(id: Int): Result<Comment, Throwable> {
        return service.getComment(id)
    }

    override suspend fun getComments(list: List<Int>): Result<List<Comment>, Throwable> {
        return list.map { service.getComment(it) }.lift()
    }

    private suspend fun getStories(list: List<Int>): Result<List<Story>, Throwable> {
        return list.map { service.getStory(it) }.lift()
    }
}
