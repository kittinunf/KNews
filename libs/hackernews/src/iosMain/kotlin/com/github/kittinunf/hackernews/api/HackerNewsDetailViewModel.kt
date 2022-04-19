package com.github.kittinunf.hackernews.api

import com.github.kittinunf.hackernews.api.detail.DetailUiState
import com.github.kittinunf.hackernews.api.detail.HackerNewsDetailViewModel
import com.github.kittinunf.hackernews.repository.HackerNewsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun HackerNewsDetailViewModel(state: DetailUiState, service: HackerNewsService) =
    HackerNewsDetailViewModel(state, CoroutineScope(SupervisorJob() + Dispatchers.Main), service)
