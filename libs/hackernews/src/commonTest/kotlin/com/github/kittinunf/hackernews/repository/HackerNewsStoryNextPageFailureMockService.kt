package com.github.kittinunf.hackernews.repository

import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.result.Result

class HackerNewsStoryNextPageFailureMockService : HackerNewsService {

    override suspend fun getTopStories(): Result<List<Int>, Throwable> = Result.success(listOf(1, 2, 3, 4, 5, 6))

    override suspend fun getStory(id: Int): Result<Story, Throwable> {
        if (id == 6) return Result.failure(IllegalArgumentException("6 cannot be found"))
        return Result.success(createRandomStory(id))
    }

    override suspend fun getComment(id: Int): Result<Comment, Throwable> = Result.failure(NotImplementedError())
}

