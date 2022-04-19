package com.github.kittinunf.hackernews.api

import com.github.kittinunf.hackernews.api.list.HackerNewsListViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun HackerNewsListViewModel(service: HackerNewsService) =
    HackerNewsListViewModel(CoroutineScope(SupervisorJob() + Dispatchers.Main), service)
