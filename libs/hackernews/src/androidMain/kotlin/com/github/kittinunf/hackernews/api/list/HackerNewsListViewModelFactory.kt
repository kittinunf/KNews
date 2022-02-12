package com.github.kittinunf.hackernews.api.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kittinunf.hackernews.repository.HackerNewsService

class HackerNewsListViewModelFactory(private val service: HackerNewsService) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.getConstructor(HackerNewsService::class.java).newInstance(service)
}
