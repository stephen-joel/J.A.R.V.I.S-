package com.example.engine

import com.example.data.JarvisRepository
import com.example.data.MemoryEntity
import java.util.Locale

data class ExtractedMemoryItem(
    val category: String,
    val key: String,
    val value: String,
    val confidence: Float = 0.95f,
    val source: String = "CONVERSATION",
    val isCorrection: Boolean = false
)

class MemoryEngine(private val repository: JarvisRepository) {

    suspend fun processInputForLearning(userText: String): ExtractedMemoryItem? {
        val text = userText.trim()
        val lower = text.lowercase(Locale.ROOT)

        // 1. Correction patterns: "No, when I say X I mean Y" or "No, X means Y"
        if (lower.startsWith("no,") || lower.startsWith("no ") || lower.contains("i meant")) {
            val aliasMatch = regexAliasMatch(lower)
            if (aliasMatch != null) {
                val item = aliasMatch.copy(
                    source = "USER_CORRECTION",
                    confidence = 1.0f,
                    isCorrection = true
                )
                saveOrUpdateMemory(item)
                return item
            }
        }

        // 2. Explicit Alias patterns: "When I say X, I mean Y", "X means Y"
        val aliasMatch = regexAliasMatch(lower)
        if (aliasMatch != null) {
            val item = aliasMatch.copy(source = "USER_TAUGHT", confidence = 0.98f)
            saveOrUpdateMemory(item)
            return item
        }

        // 3. Favorite/Preference patterns: "My favorite X is Y", "I prefer X"
        if (lower.contains("favorite") || lower.contains("prefer") || lower.contains("my browser is")) {
            val prefMatch = regexPreferenceMatch(userText, lower)
            if (prefMatch != null) {
                saveOrUpdateMemory(prefMatch)
                return prefMatch
            }
        }

        // 4. Personal facts: "My name is X", "I am a Y"
        if (lower.startsWith("my name is ") || lower.startsWith("i am a ") || lower.startsWith("i live in ")) {
            val factMatch = regexPersonalFactMatch(userText, lower)
            if (factMatch != null) {
                saveOrUpdateMemory(factMatch)
                return factMatch
            }
        }

        return null
    }

    private fun regexAliasMatch(lower: String): ExtractedMemoryItem? {
        // Pattern: "when i say [x] i mean [y]" or "when i say [x], i mean [y]" or "when i say [x] it means [y]"
        val regex1 = Regex("""when i say\s+['"]?([^,'"]+)['"]?\s*,?\s*i mean\s+['"]?([^.'"]+)['"]?""")
        val match1 = regex1.find(lower)
        if (match1 != null) {
            val key = match1.groupValues[1].trim()
            val value = match1.groupValues[2].trim()
            return ExtractedMemoryItem(category = "alias", key = key, value = value)
        }

        // Pattern: "[x] means [y]"
        val regex2 = Regex("""['"]?([^,'"]+)['"]?\s+means\s+['"]?([^.'"]+)['"]?""")
        val match2 = regex2.find(lower)
        if (match2 != null) {
            val rawKey = match2.groupValues[1].trim()
            val rawValue = match2.groupValues[2].trim()
            if (!rawKey.contains("what") && !rawKey.contains("this") && !rawKey.contains("that")) {
                val key = rawKey.replace("no,", "").trim()
                return ExtractedMemoryItem(category = "alias", key = key, value = rawValue)
            }
        }

        return null
    }

    private fun regexPreferenceMatch(originalText: String, lower: String): ExtractedMemoryItem? {
        // "My favorite browser is Firefox" -> key = "favorite_browser", value = "Firefox"
        val favRegex = Regex("""my favorite\s+([a-z0-9_ ]+)\s+is\s+([a-z0-9_ ]+)""", RegexOption.IGNORE_CASE)
        val match = favRegex.find(originalText)
        if (match != null) {
            val topic = match.groupValues[1].trim().lowercase()
            val value = match.groupValues[2].trim()
            return ExtractedMemoryItem(
                category = "preference",
                key = "favorite_$topic",
                value = value,
                source = "CONVERSATION"
            )
        }

        if (lower.contains("my browser is") || lower.contains("my preferred browser")) {
            val parts = originalText.split("is")
            if (parts.size >= 2) {
                val valStr = parts[1].trim().replace(".", "")
                return ExtractedMemoryItem(
                    category = "preference",
                    key = "favorite_browser",
                    value = valStr,
                    source = "CONVERSATION"
                )
            }
        }

        return null
    }

    private fun regexPersonalFactMatch(originalText: String, lower: String): ExtractedMemoryItem? {
        if (lower.startsWith("my name is ")) {
            val name = originalText.substring("my name is ".length).trim().replace(".", "")
            return ExtractedMemoryItem(
                category = "personal",
                key = "user_name",
                value = name,
                source = "USER_TAUGHT"
            )
        }
        return null
    }

    private suspend fun saveOrUpdateMemory(item: ExtractedMemoryItem) {
        val existing = repository.getMemoryByCategoryAndKey(item.category, item.key)
        if (existing != null) {
            repository.updateMemory(
                existing.copy(
                    value = item.value,
                    confidence = item.confidence,
                    source = item.source,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            repository.saveMemory(
                MemoryEntity(
                    category = item.category,
                    key = item.key,
                    value = item.value,
                    confidence = item.confidence,
                    source = item.source
                )
            )
        }
    }

    suspend fun getAliasFor(keyword: String): String? {
        val mem = repository.getMemoryByCategoryAndKey("alias", keyword.lowercase().trim())
        return mem?.value
    }

    suspend fun getPreferenceFor(key: String): String? {
        val mem = repository.getMemoryByCategoryAndKey("preference", key.lowercase().trim())
        return mem?.value
    }
}
