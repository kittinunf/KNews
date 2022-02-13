package com.github.kittinunf.hackernews.api

import com.github.kittinunf.hackernews.network.HttpClient
import com.github.kittinunf.hackernews.network.NetworkModule
import com.github.kittinunf.hackernews.network.addBaseUrl
import com.github.kittinunf.hackernews.network.addHackerNewsJsonSerializer
import com.github.kittinunf.hackernews.network.addLogging

object Dependency {

    val networkModule = NetworkModule(HttpClient {
        addBaseUrl("https://hacker-news.firebaseio.com/")
        addHackerNewsJsonSerializer()
        addLogging()
    })
}
