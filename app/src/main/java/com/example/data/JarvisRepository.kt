package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val db: JarvisDatabase) {
    val conversationDao = db.conversationDao()
    val messageDao = db.messageDao()
    val memoryDao = db.memoryDao()
    val researchDao = db.researchDao()
    val preferenceDao = db.preferenceDao()

    // Conversations
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    suspend fun getConversation(id: String) = conversationDao.getConversationById(id)
    suspend fun insertConversation(conv: ConversationEntity) = conversationDao.insertConversation(conv)
    suspend fun deleteConversation(id: String) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversation(id)
    }

    // Messages
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForConversation(convId)
    suspend fun getRecentMessages(convId: String, limit: Int = 10) = messageDao.getRecentMessages(convId, limit)
    suspend fun insertMessage(msg: MessageEntity) = messageDao.insertMessage(msg)

    // Memories
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> = memoryDao.getMemoriesByCategory(category)
    suspend fun getMemoryByKey(key: String) = memoryDao.getMemoryByKey(key)
    suspend fun getMemoryByCategoryAndKey(category: String, key: String) = memoryDao.getMemoryByCategoryAndKey(category, key)
    suspend fun searchMemories(query: String) = memoryDao.searchMemories(query)
    suspend fun saveMemory(memory: MemoryEntity) = memoryDao.insertMemory(memory)
    suspend fun updateMemory(memory: MemoryEntity) = memoryDao.updateMemory(memory)
    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)
    suspend fun clearAllMemories() = memoryDao.clearAllMemories()

    val totalMemoryCount: Flow<Int> = memoryDao.getMemoryCount()
    fun getMemoryCountByCategory(cat: String): Flow<Int> = memoryDao.getMemoryCountByCategory(cat)

    // Research
    val allResearch: Flow<List<ResearchTopicEntity>> = researchDao.getAllResearch()
    suspend fun findResearch(topic: String) = researchDao.findResearch(topic)
    suspend fun saveResearch(topic: ResearchTopicEntity) = researchDao.insertResearch(topic)
    suspend fun deleteResearch(id: Long) = researchDao.deleteResearch(id)
    val totalResearchCount: Flow<Int> = researchDao.getResearchCount()

    // User Preferences
    val allPreferences: Flow<List<UserPreferenceEntity>> = preferenceDao.getAllPreferences()
    suspend fun getPreference(key: String) = preferenceDao.getPreference(key)
    suspend fun setPreference(key: String, value: String) {
        preferenceDao.setPreference(UserPreferenceEntity(key = key, value = value))
    }

    // Export & Import
    suspend fun resetAllLearnedData() {
        memoryDao.clearAllMemories()
        researchDao.clearAllResearch()
        preferenceDao.clearAllPreferences()
    }
}
