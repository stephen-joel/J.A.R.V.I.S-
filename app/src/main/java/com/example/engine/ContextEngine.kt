package com.example.engine

import com.example.data.MessageEntity

class ContextEngine {

    private var activeEntity: String? = null
    private var lastAction: String? = null
    private val entityHistory = mutableListOf<String>()

    fun updateContextWithUserMessage(userText: String, recentMessages: List<MessageEntity>) {
        val extractedEntity = extractEntityFromText(userText)
        if (extractedEntity != null) {
            activeEntity = extractedEntity
            if (!entityHistory.contains(extractedEntity)) {
                entityHistory.add(extractedEntity)
            }
        }
    }

    fun resolveAnaphoraAndIntent(userText: String): ResolvedQuery {
        val lower = userText.trim().lowercase()

        // Detect if query uses pronouns like "it", "that", "this"
        val containsPronoun = lower.contains(" it") || lower.endsWith(" it") || lower.startsWith("it ") || lower.contains(" that") || lower.contains(" this")

        var resolvedEntity = activeEntity

        if (containsPronoun && resolvedEntity != null) {
            val resolvedText = userText
                .replace(Regex("""\bit\b""", RegexOption.IGNORE_CASE), resolvedEntity)
                .replace(Regex("""\bthat\b""", RegexOption.IGNORE_CASE), resolvedEntity)

            return ResolvedQuery(
                originalText = userText,
                resolvedText = resolvedText,
                targetEntity = resolvedEntity,
                isAnaphoraResolved = true
            )
        }

        return ResolvedQuery(
            originalText = userText,
            resolvedText = userText,
            targetEntity = extractedOrActive(userText),
            isAnaphoraResolved = false
        )
    }

    private fun extractedOrActive(userText: String): String? {
        return extractEntityFromText(userText) ?: activeEntity
    }

    private fun extractEntityFromText(text: String): String? {
        val lower = text.lowercase()

        // Known common topics/apps
        val knownEntities = listOf(
            "minecraft", "youtube", "yt", "tiktok", "firefox", "chrome", "gmail",
            "spotify", "maps", "camera", "settings", "instagram", "twitter",
            "reddit", "whatsapp", "telegram", "facebook", "netflix", "discord"
        )

        for (e in knownEntities) {
            if (lower.contains(e)) {
                return when(e) {
                    "yt" -> "YouTube"
                    "minecraft" -> "Minecraft"
                    "youtube" -> "YouTube"
                    "tiktok" -> "TikTok"
                    "firefox" -> "Firefox"
                    "gmail" -> "Gmail"
                    "spotify" -> "Spotify"
                    else -> e.replaceFirstChar { it.uppercase() }
                }
            }
        }

        // Regex for "what is X", "tell me about X", "open X"
        val regexes = listOf(
            Regex("""what is ([a-z0-9_ ]+)\b""", RegexOption.IGNORE_CASE),
            Regex("""tell me about ([a-z0-9_ ]+)\b""", RegexOption.IGNORE_CASE),
            Regex("""open ([a-z0-9_ ]+)\b""", RegexOption.IGNORE_CASE),
            Regex("""search for ([a-z0-9_ ]+)\b""", RegexOption.IGNORE_CASE)
        )

        for (r in regexes) {
            val match = r.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (!candidate.contains("you") && !candidate.contains("it") && candidate.length > 2) {
                    return candidate.split(" ")[0].replaceFirstChar { it.uppercase() }
                }
            }
        }

        return null
    }

    fun setActiveEntity(entity: String) {
        activeEntity = entity
    }

    fun getActiveEntity(): String? = activeEntity
}

data class ResolvedQuery(
    val originalText: String,
    val resolvedText: String,
    val targetEntity: String?,
    val isAnaphoraResolved: Boolean
)
