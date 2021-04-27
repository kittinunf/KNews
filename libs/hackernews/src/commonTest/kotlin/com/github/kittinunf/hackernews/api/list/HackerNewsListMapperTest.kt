package com.github.kittinunf.hackernews.api.list

import com.github.kittinunf.hackernews.model.Story
import com.github.kittinunf.hackernews.repository.createRandomStory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class HackerNewsListMapperTest {

    private val mapper = ::listUiRowStateMapper

    @Test
    fun `should map to the correct ListUiRowState`() {
        val story = createRandomStory(1)
        val state = mapper(story)

        assertEquals(1, state.id)
        assertEquals("Story1", state.title)
        assertEquals("http://1.com", state.url.toString())
    }

    @Test
    fun `should map the timeAgo for ListUiRowState correctly`() {
        val now = Clock.System.now()
        val tenSecondsAgo = (now - 10.toDuration(DurationUnit.SECONDS)).epochSeconds
        val secondsAgoStory = Story(1, "Story$1", "http://1.com", 100, "Ann$1", tenSecondsAgo.toInt(), null, 10)

        assertEquals("10 seconds ago", mapper(secondsAgoStory).fromNowText)

        val tenMinutesAgo = (now - (10.toDuration(DurationUnit.MINUTES))).epochSeconds

        val tenMinutesAgoStory = Story(1, "Story$1", "http://1.com", 100, "Ann$1", tenMinutesAgo.toInt(), null, 10)

        assertEquals("10 minutes ago", mapper(tenMinutesAgoStory).fromNowText)

        val tenHoursAgo = (now - 10.toDuration(DurationUnit.HOURS)).epochSeconds
        val tenHoursAgoStory = Story(1, "Story$1", "http://1.com", 100, "Ann$1", tenHoursAgo.toInt(), null, 10)

        assertEquals("10 hours ago", mapper(tenHoursAgoStory).fromNowText)
    }
}
