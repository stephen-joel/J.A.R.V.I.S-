package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val sender: String, // "user" or "jarvis"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAction: Boolean = false,
    val actionDetails: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "personal", "preference", "alias", "correction", "knowledge", "experience", "research", "routine"
    val key: String,
    val value: String,
    val confidence: Float = 0.9f,
    val source: String = "CONVERSATION", // "USER_TAUGHT", "USER_CORRECTION", "CONVERSATION", "RESEARCH", "SYSTEM"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "research_topics")
data class ResearchTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val summary: String,
    val sourceUrl: String? = null,
    val confidence: Float = 0.85f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
