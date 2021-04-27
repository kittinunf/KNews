package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.model.Story
import io.ktor.http.Url
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun listUiRowStateMapper(story: Story): ListUiRowState {
    val now = Clock.System.now()
    val diff = now.epochSeconds - story.time
    return ListUiRowState(
        id = story.id,
        title = story.title,
        url = Url(story.url),
        score = story.score,
        by = story.by,
        fromNow = diff,
        fromNowText = diff.toHumanConsumableText(),
        commentIds = story.kids,
        descendants = story.descendants
    )
}

// Int here represent the diff in seconds
internal fun Long.toHumanConsumableText(): String {
    if (this < 0) return "Unknown ago"
    return when (this) {
        in 0..59 -> "$this seconds ago"
        in 60..3599 -> "${this / 60} minutes ago"
        in 3600..86399 -> "${this / (60 * 60)} hours ago"
        else -> "${this / (60 * 60 * 24)} days ago"
    }
}
