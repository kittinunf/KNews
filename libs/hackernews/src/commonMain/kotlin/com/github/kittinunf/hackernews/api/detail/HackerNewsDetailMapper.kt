package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.api.list.toHumanConsumableText
import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.model.Story
import io.ktor.http.Url
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

fun detailUiStoryStateMapper(story: Story): DetailUiStoryState =
    DetailUiStoryState(id = story.id, title = story.title, url = Url(story.url!!), commentIds = story.kids, descendants = story.descendants)

@OptIn(ExperimentalTime::class)
fun detailUiCommentRowStateMapper(comment: Comment): DetailUiCommentRowState {
    val now = Clock.System.now()
    val diff = now.epochSeconds - comment.time
    return DetailUiCommentRowState(text = comment.text, by = comment.by, fromNow = diff, fromNowText = diff.toHumanConsumableText())
}
