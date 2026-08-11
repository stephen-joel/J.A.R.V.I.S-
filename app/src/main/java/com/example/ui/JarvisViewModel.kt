package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationEntity
import com.example.data.JarvisDatabase
import com.example.data.JarvisRepository
import com.example.data.MemoryEntity
import com.example.data.MessageEntity
import com.example.data.ResearchTopicEntity
import com.example.data.UserPreferenceEntity
import com.example.engine.ConversationEngine
import com.example.engine.SpeechRecognitionEngine
import com.example.engine.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    val repository = JarvisRepository(db)

    val voiceEngine = VoiceEngine(application)
    val speechEngine = SpeechRecognitionEngine(application)
    val conversationEngine = ConversationEngine(application, repository, voiceEngine)

    private val _activeConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { id -> repository.getMessagesForConversation(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val researchTopics: StateFlow<List<ResearchTopicEntity>> = repository.allResearch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<List<UserPreferenceEntity>> = repository.allPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics for Brain Dashboard
    val memoryCount: StateFlow<Int> = repository.totalMemoryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val researchCount: StateFlow<Int> = repository.totalResearchCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val aliasesCount: StateFlow<Int> = repository.getMemoryCountByCategory("alias")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val preferencesCount: StateFlow<Int> = repository.getMemoryCountByCategory("preference")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val correctionsCount: StateFlow<Int> = memories.combine(memories) { mems, _ ->
        mems.count { it.source == "USER_CORRECTION" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sendMessage(userText: String, speak: Boolean = true) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                conversationEngine.processUserMessage(_activeConversationId.value, userText, speakResponse = speak)
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun startListening() {
        speechEngine.startListening { recognizedText ->
            if (recognizedText.isNotBlank()) {
                sendMessage(recognizedText, speak = true)
            }
        }
    }

    fun stopListening() {
        speechEngine.stopListening()
    }

    fun createNewConversation() {
        _activeConversationId.value = UUID.randomUUID().toString()
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                createNewConversation()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.conversationDao.clearAllConversations()
            repository.messageDao.clearAllMessages()
            createNewConversation()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun resetLearnedData() {
        viewModelScope.launch {
            repository.resetAllLearnedData()
            _statusMessage.value = "Learned data reset successfully"
        }
    }

    fun exportBrainJson(): String {
        val currentMemories = memories.value
        val currentPreferences = userPreferences.value
        val currentResearch = researchTopics.value

        val root = JSONObject()
        val memArray = JSONArray()
        for (m in currentMemories) {
            val obj = JSONObject().apply {
                put("category", m.category)
                put("key", m.key)
                put("value", m.value)
                put("confidence", m.confidence)
                put("source", m.source)
            }
            memArray.put(obj)
        }
        root.put("memories", memArray)
        root.put("exportedAt", System.currentTimeMillis())
        return root.toString(2)
    }

    fun importBrainJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)
                if (root.has("memories")) {
                    val memArray = root.getJSONArray("memories")
                    for (i in 0 until memArray.length()) {
                        val obj = memArray.getJSONObject(i)
                        repository.saveMemory(
                            MemoryEntity(
                                category = obj.getString("category"),
                                key = obj.getString("key"),
                                value = obj.getString("value"),
                                confidence = obj.optDouble("confidence", 0.9).toFloat(),
                                source = obj.optString("source", "IMPORT")
                            )
                        )
                    }
                }
                _statusMessage.value = "Brain imported successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.localizedMessage}"
            }
        }
    }

    fun testVoice() {
        voiceEngine.testVoice()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
        speechEngine.stopListening()
    }
}
