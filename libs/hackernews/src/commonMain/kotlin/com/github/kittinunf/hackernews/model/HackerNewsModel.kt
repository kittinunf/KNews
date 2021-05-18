package com.github.kittinunf.hackernews.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class User(
    val id: String,
    val about: String,
    val karma: Int
)

internal interface Item {
    val id: Int
    val by: String
    val time: Int
    val kids: List<Int>?
}

@Serializable
@SerialName("story")
data class Story(
    override val id: Int,
    val title: String,
    val url: String = "", // this is based on the real-world testing that it can be missing from the json object
    val score: Int,
    override val by: String,
    override val time: Int,
    override val kids: List<Int>? = null,
    val descendants: Int? = null
) : Item

@Serializable
@SerialName("comment")
data class Comment(
    override val id: Int,
    val parent: Int,
    val text: String = "", // this is based on the real-world testing that it can be missing from the json object
    override val by: String = "", // this is based on the real-world testing that it can be missing from the json object
    override val time: Int,
    override val kids: List<Int>? = null
) : Item

internal val hackerNewsSerializersModule = SerializersModule {
    polymorphic(Item::class) {
        subclass(Story::class, Story.serializer())
        subclass(Comment::class, Comment.serializer())
    }
}
