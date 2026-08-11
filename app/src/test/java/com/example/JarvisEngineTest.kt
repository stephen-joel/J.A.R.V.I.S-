package com.example

import com.example.engine.ContextEngine
import com.example.engine.ExtractedMemoryItem
import com.example.engine.LocalAIEngine
import com.example.engine.LocalAIResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisEngineTest {

    @Test
    fun testAnaphoraResolutionForMinecraft() {
        val contextEngine = ContextEngine()
        contextEngine.updateContextWithUserMessage("What is Minecraft?", emptyList())

        val query = contextEngine.resolveAnaphoraAndIntent("Can I play it on my phone?")
        assertEquals("Minecraft", query.targetEntity)
        assertTrue(query.resolvedText.contains("Minecraft"))
    }

    @Test
    fun testAnaphoraResolutionForYouTube() {
        val contextEngine = ContextEngine()
        contextEngine.updateContextWithUserMessage("Tell me about YouTube.", emptyList())

        val query = contextEngine.resolveAnaphoraAndIntent("Open it.")
        assertEquals("YouTube", query.targetEntity)
        assertTrue(query.resolvedText.lowercase().contains("youtube"))
    }

    @Test
    fun testLocalAIActionVsConversation() = runBlocking {
        val aiEngine = LocalAIEngine()
        val contextEngine = ContextEngine()

        // 1. Conversation test: "What is YouTube?"
        val resolved1 = contextEngine.resolveAnaphoraAndIntent("What is YouTube?")
        val response1 = aiEngine.generateResponse("What is YouTube?", resolved1, emptyList(), null)
        assertTrue(response1 is LocalAIResponse.Conversation)

        // 2. Action test: "Open YouTube"
        val resolved2 = contextEngine.resolveAnaphoraAndIntent("Open YouTube")
        val response2 = aiEngine.generateResponse("Open YouTube", resolved2, emptyList(), null)
        assertTrue(response2 is LocalAIResponse.Action)
        val action = response2 as LocalAIResponse.Action
        assertEquals("OPEN_APP", action.actionType)
        assertEquals("YouTube", action.target)
    }

    @Test
    fun testCasualConversationGreetings() = runBlocking {
        val aiEngine = LocalAIEngine()
        val contextEngine = ContextEngine()

        val resolved = contextEngine.resolveAnaphoraAndIntent("Hey JARVIS")
        val response = aiEngine.generateResponse("Hey JARVIS", resolved, emptyList(), null) as LocalAIResponse.Conversation
        assertTrue(response.text.contains("Hey") || response.text.contains("going on"))
    }
}
