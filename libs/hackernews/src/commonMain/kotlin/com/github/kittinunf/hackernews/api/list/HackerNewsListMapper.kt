package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.util.Mapper
import io.ktor.http.Url
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal val listUiRowStateMapper = object : Mapper<Story, ListUiRowState> {
    override fun map(t: Story): ListUiRowState {
        val now = Clock.System.now()
        val diff = now.epochSeconds - t.time
        return ListUiRowState(
            id = t.id,
            title = t.title,
            url = Url(t.url),
            score = t.score,
            by = t.by,
            fromNow = diff,
            fromNowText = diff.toHumanConsumableText(),
            commentIds = t.kids,
            descendants = t.descendants
        )
    }
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
