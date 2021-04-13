package com.github.kittinunf.hackernews.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HackNewsModelSerializerTest {

    private val format = Json {
        serializersModule = hackerNewsSerializersModule
        ignoreUnknownKeys = true
    }

    @Test
    fun `should serialize and deserialize user model`() {
        val str = """
            {
              "about" : "This is a test",
              "created" : 1173923446,
              "delay" : 0,
              "id" : "jl",
              "karma" : 2937,
              "submitted" : [ 8265435, 8168423, 8090946, 8090326, 7699907, 7637962, 7596179, 7596163, 7594569]
            }
        """.trimIndent()
        val user = format.decodeFromString<User>(str)
        assertEquals("jl", user.id)
        assertEquals("This is a test", user.about)
        assertEquals(2937, user.karma)
    }

    @Test
    fun `should serialize and deserialize item wih story`() {
        val str = """
            {
              "by" : "dhouston",
              "descendants" : 71,
              "id" : 8863,
              "kids" : [ 8952, 9224, 8917, 8884, 8887, 8943, 8869, 8958, 9005 ],
              "score" : 111,
              "time" : 1175714200,
              "title" : "My YC app: Dropbox - Throw away your USB drive",
              "type" : "story",
              "descendants" : 10,
              "url" : "http://www.getdropbox.com/u/2/screencast.html"
            }
        """.trimIndent()
        val story = format.decodeFromString<Story>(str)
        assertEquals(8863, story.id)
        assertEquals("dhouston", story.by)
        assertEquals(9, story.kids!!.size)
        assertEquals(10, story.descendants)
        assertEquals("My YC app: Dropbox - Throw away your USB drive", story.title)
    }

    @Test
    fun `should serialize and deserialize item wih story with missing fields`() {
        val str = """
            {
              "by" : "dhouston",
              "descendants" : 71,
              "id" : 8863,
              "score" : 111,
              "time" : 1175714200,
              "title" : "My YC app: Dropbox - Throw away your USB drive",
              "type" : "story",
              "url" : "http://www.getdropbox.com/u/2/screencast.html"
            }
        """.trimIndent()
        val story = format.decodeFromString<Story>(str)
        assertEquals(8863, story.id)
        assertEquals("dhouston", story.by)
        assertEquals(0, story.kids?.size ?: 0)
        assertEquals(71, story.descendants ?: 0)
        assertEquals("My YC app: Dropbox - Throw away your USB drive", story.title)
    }

    @Test
    fun `should serialize and deserialize item with comment`() {
        val str = """
            {
              "by" : "norvig",
              "id" : 2921983,
              "kids" : [ 2922097, 2922429, 2924562, 2922709, 2922573, 2922140, 2922141 ],
              "parent" : 2921506,
              "text" : "Aw shucks, guys ... you make me blush with your compliments.<p>Tell you what, Ill make a deal: I'll keep writing if you keep reading. K?",
              "time" : 1314211127,
              "type" : "comment"
            }
        """.trimIndent()
        val comment = format.decodeFromString<Comment>(str)
        assertEquals(2921983, comment.id)
        assertEquals("norvig", comment.by)
        assertEquals(7, comment.kids!!.size)
        assertEquals("Aw shucks, guys ... you make me blush with your compliments.<p>Tell you what, Ill make a deal: I'll keep writing if you keep reading. K?", comment.text)
    }

    @Test
    fun `should serialize and deserialize item with comment that is flagged as deleted`() {
        val str = """
            {
              "deleted": true,
              "id": 26689556,
              "parent": 26688965,
              "time": 1617545722,
              "type": "comment"
            }
        """.trimIndent()
        val comment = format.decodeFromString<Comment>(str)
        assertEquals(26689556, comment.id)
        assertEquals(26688965, comment.parent)
        assertEquals(1617545722, comment.time)
    }
}
