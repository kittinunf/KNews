package com.github.kittinunf.hackernews.api.detail

import com.github.kittinunf.hackernews.model.Comment
import com.github.kittinunf.hackernews.repository.createRandomComment
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class HackerNewsDetailMapperTest {

    private val mapper = ::detailUiCommentRowStateMapper

    @Test
    fun `should map to the correct ListUiRowState`() {
        val comment = createRandomComment(2)
        val state = mapper(comment)

        assertEquals("Comment2", state.text)
        assertEquals("Ann2", state.by)
    }

    @Test
    fun `should map the timeAgo for ListUiRowState correctly`() {
        val now = Clock.System.now()
        val tenSecondsAgo = (now - 10.toDuration(DurationUnit.SECONDS)).epochSeconds
        val secondsAgoComment = Comment(2, 10, "Comment2", "Ann2", tenSecondsAgo.toInt(), null)

        assertEquals("10 seconds ago", mapper(secondsAgoComment).fromNowText)

        val tenMinutesAgo = (now - (10.toDuration(DurationUnit.MINUTES))).epochSeconds

        val tenMinutesAgoComment = Comment(2, 10, "Comment2", "Ann2", tenMinutesAgo.toInt(), null)

        assertEquals("10 minutes ago", mapper(tenMinutesAgoComment).fromNowText)

        val tenHoursAgo = (now - 10.toDuration(DurationUnit.HOURS)).epochSeconds
        val tenHoursAgoComment = Comment(2, 10, "Comment2", "Ann2", tenHoursAgo.toInt(), null)

        assertEquals("10 hours ago", mapper(tenHoursAgoComment).fromNowText)
    }
}
