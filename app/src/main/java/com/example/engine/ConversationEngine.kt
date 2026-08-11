package com.example.engine

import android.content.Context
import com.example.data.ConversationEntity
import com.example.data.JarvisRepository
import com.example.data.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class ConversationEngine(
    private val context: Context,
    private val repository: JarvisRepository,
    val voiceEngine: VoiceEngine
) {

    val toolEngine = ToolEngine(context)
    val memoryEngine = MemoryEngine(repository)
    val researchEngine = ResearchEngine(context, repository)
    val contextEngine = ContextEngine()
    val localAIEngine = LocalAIEngine()

    suspend fun processUserMessage(
        conversationId: String,
        userText: String,
        speakResponse: Boolean = true
    ): MessageEntity = withContext(Dispatchers.IO) {

        val trimmedText = userText.trim()

        // 1. Ensure conversation entity exists
        var conv = repository.getConversation(conversationId)
        if (conv == null) {
            conv = ConversationEntity(
                id = conversationId,
                title = trimmedText.take(30)
            )
            repository.insertConversation(conv)
        } else {
            repository.insertConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }

        // 2. Save User Message
        val userMsg = MessageEntity(
            conversationId = conversationId,
            sender = "user",
            text = trimmedText,
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(userMsg)

        // 3. Update multi-turn context & resolve anaphora ("it", "that")
        val recentMessages = repository.getRecentMessages(conversationId, 10)
        contextEngine.updateContextWithUserMessage(trimmedText, recentMessages)
        val resolvedQuery = contextEngine.resolveAnaphoraAndIntent(trimmedText)

        // 4. Extract automatic learning item (preferences, aliases, corrections)
        val learnedItem = memoryEngine.processInputForLearning(trimmedText)

        // 5. Fetch existing memories for context
        val memories = repository.allMemories.first()
        val learnedAlias = memoryEngine.getAliasFor(resolvedQuery.resolvedText.lowercase())
            ?: memoryEngine.getAliasFor(trimmedText.lowercase())

        // 6. Generate AI response or action intent
        val response = localAIEngine.generateResponse(
            userText = trimmedText,
            resolvedQuery = resolvedQuery,
            memories = memories,
            learnedAlias = learnedAlias
        )

        val jarvisMessageText: String
        var isAction = false
        var actionDetails: String? = null

        when (response) {
            is LocalAIResponse.Conversation -> {
                jarvisMessageText = if (learnedItem != null && learnedItem.isCorrection) {
                    "Got it. I've updated my memory: when you say '${learnedItem.key}', you mean '${learnedItem.value}'."
                } else if (learnedItem != null && learnedItem.category == "alias") {
                    "Understood. I will remember that '${learnedItem.key}' means '${learnedItem.value}'."
                } else if (learnedItem != null && learnedItem.category == "preference") {
                    "I've remembered your preference: ${learnedItem.key.replace("_", " ")} is ${learnedItem.value}."
                } else {
                    response.text
                }
            }

            is LocalAIResponse.Action -> {
                isAction = true
                when (response.actionType) {
                    "OPEN_APP" -> {
                        val result = toolEngine.executeAppLaunch(response.target, learnedAlias)
                        jarvisMessageText = result.message
                        actionDetails = result.details
                    }
                    "SYSTEM_INFO" -> {
                        val result = toolEngine.getSystemInfo()
                        jarvisMessageText = result.message
                        actionDetails = result.details
                    }
                    "SEARCH_WEB" -> {
                        val result = toolEngine.executeWebSearch(response.target)
                        jarvisMessageText = result.message
                        actionDetails = result.details
                    }
                    "CALCULATE" -> {
                        val result = toolEngine.evaluateMath(response.target)
                        jarvisMessageText = result.message
                        actionDetails = result.details
                    }
                    else -> {
                        jarvisMessageText = response.preText
                    }
                }
            }

            is LocalAIResponse.Research -> {
                isAction = true
                val researchTopic = researchEngine.research(response.topic)
                jarvisMessageText = "Research Summary for '${researchTopic.topic}':\n\n${researchTopic.summary}"
                actionDetails = "Source: ${researchTopic.sourceUrl ?: "Local Stored Knowledge"}"
            }
        }

        // 7. Save JARVIS response message
        val jarvisMsg = MessageEntity(
            conversationId = conversationId,
            sender = "jarvis",
            text = jarvisMessageText,
            timestamp = System.currentTimeMillis(),
            isAction = isAction,
            actionDetails = actionDetails
        )
        repository.insertMessage(jarvisMsg)

        // 8. Speak response if enabled
        if (speakResponse) {
            voiceEngine.speak(jarvisMessageText)
        }

        return@withContext jarvisMsg
    }
}
