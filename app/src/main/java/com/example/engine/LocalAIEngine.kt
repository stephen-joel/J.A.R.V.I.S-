package com.example.engine

import com.example.data.MemoryEntity
import java.util.Locale

sealed class LocalAIResponse {
    data class Conversation(val text: String) : LocalAIResponse()
    data class Action(
        val actionType: String,
        val target: String,
        val preText: String,
        val postText: String? = null
    ) : LocalAIResponse()
    data class Research(val topic: String, val preText: String) : LocalAIResponse()
}

class LocalAIEngine {

    suspend fun generateResponse(
        userText: String,
        resolvedQuery: ResolvedQuery,
        memories: List<MemoryEntity>,
        learnedAlias: String?
    ): LocalAIResponse {
        val rawLower = userText.trim().lowercase(Locale.ROOT)
        val resolvedLower = resolvedQuery.resolvedText.lowercase(Locale.ROOT)
        val targetEntity = resolvedQuery.targetEntity

        // 1. Check for Action Intent vs Conversation Intent
        if (isActionIntent(rawLower, resolvedLower)) {
            val appTarget = learnedAlias ?: targetEntity ?: extractActionTarget(resolvedQuery.resolvedText)
            return LocalAIResponse.Action(
                actionType = "OPEN_APP",
                target = appTarget,
                preText = "Sure, opening $appTarget."
            )
        }

        // 2. System Status Action Intent
        if (rawLower.contains("battery") || rawLower.contains("system status") || rawLower.contains("storage space") || rawLower.contains("what time is it")) {
            return LocalAIResponse.Action(
                actionType = "SYSTEM_INFO",
                target = "system",
                preText = "Checking system status..."
            )
        }

        // 3. Web Search Action Intent
        if (rawLower.startsWith("search for ") || rawLower.startsWith("google ")) {
            val query = userText.substringAfter("search for ").substringAfter("google ").trim()
            return LocalAIResponse.Action(
                actionType = "SEARCH_WEB",
                target = query,
                preText = "Searching web for '$query'..."
            )
        }

        // 4. Research Intent: "research X", "tell me research on X"
        if (rawLower.startsWith("research ") || rawLower.contains("research about ")) {
            val topic = userText.replace(Regex("(?i)research (about )?"), "").trim()
            return LocalAIResponse.Research(
                topic = topic,
                preText = "Initiating research on '$topic'..."
            )
        }

        // 5. Memory Query Intent: "what browser do I prefer?", "what is my favorite X?", "what do you remember about me?"
        if (rawLower.contains("what do you remember") || rawLower.contains("what have you learned")) {
            if (memories.isEmpty()) {
                return LocalAIResponse.Conversation("I don't have any saved memories about you yet. As we talk, I will automatically remember your preferences and terminology!")
            }
            val memoryList = memories.take(5).joinToString("\n• ") { "${it.category.uppercase()}: ${it.key} = ${it.value}" }
            return LocalAIResponse.Conversation("Here is what I currently remember about you:\n• $memoryList")
        }

        if (rawLower.contains("browser") && (rawLower.contains("prefer") || rawLower.contains("favorite") || rawLower.contains("my"))) {
            val prefBrowser = memories.find { it.key == "favorite_browser" }?.value
            if (prefBrowser != null) {
                return LocalAIResponse.Conversation(prefBrowser)
            } else {
                return LocalAIResponse.Conversation("You haven't told me your preferred browser yet. If you tell me 'My favorite browser is Firefox', I will remember it!")
            }
        }

        if (rawLower.contains("favorite") || rawLower.contains("prefer")) {
            for (m in memories) {
                if (m.category == "preference" && rawLower.contains(m.key.replace("favorite_", ""))) {
                    return LocalAIResponse.Conversation(m.value)
                }
            }
        }

        // 6. Greetings
        if (rawLower == "hey jarvis" || rawLower == "hey" || rawLower == "hello jarvis" || rawLower == "hi jarvis" || rawLower == "hello" || rawLower == "hi") {
            return LocalAIResponse.Conversation("Hey. What's going on?")
        }

        // 7. Casual Conversation - "I'm bored"
        if (rawLower.contains("i'm bored") || rawLower.contains("im bored") || rawLower.contains("bored")) {
            return LocalAIResponse.Conversation("Want something interesting to do? Alright. I can give you a challenge, tell you something interesting, help you build something, or we can just chat.")
        }

        // 8. Casual Conversation - "Tell me something interesting"
        if (rawLower.contains("something interesting")) {
            val interestingFacts = listOf(
                "Did you know that quantum computers leverage superposition, allowing qubits to exist as both 0 and 1 simultaneously? This enables parallel calculations that classical supercomputers would take millennia to solve.",
                "Honey never spoils. Archaeologists have found 3,000-year-old jars of honey in Egyptian tombs that are still perfectly edible due to its low moisture content and acidic pH.",
                "Voyager 1 is over 15 billion miles from Earth and is currently traveling through interstellar space at approximately 38,000 miles per hour while continuing to transmit data back to Earth."
            )
            return LocalAIResponse.Conversation(interestingFacts.random())
        }

        // 9. Entity Specific Answers (Minecraft, YouTube, etc.)
        if (targetEntity != null) {
            val entityLower = targetEntity.lowercase()
            if (entityLower.contains("minecraft")) {
                if (resolvedLower.contains("play") && (resolvedLower.contains("phone") || resolvedLower.contains("mobile"))) {
                    return LocalAIResponse.Conversation("Yes! Minecraft is available on phones as Minecraft Bedrock / Pocket Edition for Android and iOS.")
                }
                return LocalAIResponse.Conversation("Minecraft is a popular 3D sandbox game created by Mojang Studios where players build, explore, craft, and survive in blocky procedurally generated worlds.")
            }

            if (entityLower.contains("youtube")) {
                if (resolvedLower.contains("why") && resolvedLower.contains("popular")) {
                    return LocalAIResponse.Conversation("YouTube is popular because of its vast diversity of creator content, seamless recommendation algorithms, global community, and accessibility across all mobile and smart devices.")
                }
                return LocalAIResponse.Conversation("YouTube is a global video-sharing platform owned by Google where users create, upload, watch, and interact with video content across all subjects.")
            }

            if (entityLower.contains("firefox")) {
                return LocalAIResponse.Conversation("Firefox is a open-source web browser developed by Mozilla, known for its privacy features, fast Gecko rendering engine, and user customization.")
            }

            if (entityLower.contains("tiktok")) {
                return LocalAIResponse.Conversation("TikTok is a popular short-form video streaming application featuring algorithmically curated content, viral music trends, and creator tools.")
            }
        }

        // 10. General conversational fallback
        if (rawLower.contains("who are you") || rawLower.contains("what are you")) {
            return LocalAIResponse.Conversation("I am JARVIS, your conversational, adaptive, local-first personal AI assistant. I remember your preferences, learn your terminology, research information, and run locally without requiring any external cloud APIs.")
        }

        if (rawLower.contains("what can you do") || rawLower.contains("help")) {
            return LocalAIResponse.Conversation("I can converse with you, remember your preferences and aliases, launch your installed apps, perform web searches, research topics, check system status, and adapt to how you speak—all locally on your phone.")
        }

        // Conversational default response for open queries
        return LocalAIResponse.Conversation("I understand. Tell me more about what you'd like to do or explore.")
    }

    private fun isActionIntent(rawLower: String, resolvedLower: String): Boolean {
        // Explicit open commands
        if (rawLower.startsWith("open ") || rawLower.startsWith("launch ") || rawLower.startsWith("start ")) {
            return true
        }

        if (resolvedLower.startsWith("open ") || resolvedLower.startsWith("launch ")) {
            return true
        }

        // "Open it" or "Launch it"
        if (rawLower == "open it" || rawLower == "launch it" || rawLower == "start it") {
            return true
        }

        return false
    }

    private fun extractActionTarget(text: String): String {
        val trimmed = text.trim()
        val regex = Regex("""(?i)(open|launch|start)\s+([a-z0-9_ ]+)""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[2].trim()
        }
        return trimmed.replace(Regex("""(?i)^(open|launch|start)\s*"""), "").trim()
    }
}
