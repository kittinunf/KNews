package com.github.kittinunf.hackernews.repository

import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.network.NetworkModule
import com.github.kittinunf.hackernews.util.Result

interface HackerNewsService {

    suspend fun getTopStories(): Result<List<Int>, Throwable>

    suspend fun getStory(id: Int): Result<Story, Throwable>

    suspend fun getComment(id: Int): Result<Comment, Throwable>
}

class HackerNewsServiceImpl(private val api: NetworkModule) : HackerNewsService {

    companion object {
        const val topStories = "topstories"
        const val item = "item"
    }

    override suspend fun getTopStories(): Result<List<Int>, Throwable> {
        return api.get("/v0/$topStories.json")
    }

    override suspend fun getStory(id: Int): Result<Story, Throwable> {
        return api.get("/v0/$item/$id.json")
    }

    override suspend fun getComment(id: Int): Result<Comment, Throwable> {
        return api.get("/v0/$item/$id.json")
    }
}
