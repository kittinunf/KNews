package com.github.kittinunf.hackernews.api

import com.github.kittinunf.hackernews.network.NetworkModule
import com.github.kittinunf.hackernews.network.addBaseUrl
import com.github.kittinunf.hackernews.network.addHackerNewsJsonSerializer
import com.github.kittinunf.hackernews.network.addLogging
import com.github.kittinunf.hackernews.network.createHttpClient

object HackerNewsDependency {

    val networkModule = NetworkModule(createHttpClient {
        addBaseUrl("https://hacker-news.firebaseio.com/")
        addHackerNewsJsonSerializer()
        addLogging()
    })
}
