package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.list.toHumanConsumableText
import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.util.Mapper
import io.ktor.http.Url
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

internal val detailUiStoryStateMapper = object : Mapper<Story, DetailUiStoryState> {
    override fun map(t: Story): DetailUiStoryState {
        return DetailUiStoryState(id = t.id, title = t.title, url = Url(t.url), commentIds = t.kids, descendants = t.descendants)
    }
}

@OptIn(ExperimentalTime::class)
internal val detailUiCommentRowStateMapper = object : Mapper<Comment, DetailUiCommentRowState> {
    override fun map(t: Comment): DetailUiCommentRowState {
        val now = Clock.System.now()
        val diff = now.epochSeconds - t.time
        return DetailUiCommentRowState(text = t.text, by = t.by, fromNow = diff, fromNowText = diff.toHumanConsumableText())
    }
}
