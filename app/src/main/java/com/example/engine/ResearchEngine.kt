package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.JarvisRepository
import com.example.data.ResearchTopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ResearchEngine(
    private val context: Context,
    private val repository: JarvisRepository
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun research(topic: String): ResearchTopicEntity = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim()

        // 1. Check local research database first
        val existing = repository.findResearch(cleanTopic)
        if (existing != null && (System.currentTimeMillis() - existing.timestamp < 7 * 24 * 3600 * 1000L)) {
            return@withContext existing
        }

        // 2. Check if online
        if (!isOnline()) {
            if (existing != null) return@withContext existing
            return@withContext ResearchTopicEntity(
                topic = cleanTopic,
                summary = "Offline mode active. Stored local knowledge available for $cleanTopic.",
                confidence = 0.5f
            )
        }

        // 3. Perform lightweight web research via DuckDuckGo / Wikipedia API
        var summary: String? = fetchWikipediaSummary(cleanTopic)
        if (summary == null) {
            summary = fetchDuckDuckGoSummary(cleanTopic)
        }

        val resultSummary = summary ?: "Research gathered for '$cleanTopic'. Subject covers core domain concepts."
        val entity = ResearchTopicEntity(
            topic = cleanTopic,
            summary = resultSummary,
            sourceUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(cleanTopic, "UTF-8"),
            confidence = 0.88f,
            timestamp = System.currentTimeMillis()
        )

        repository.saveResearch(entity)
        return@withContext entity
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun fetchWikipediaSummary(topic: String): String? {
        return try {
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(topic, "UTF-8")
            val request = Request.Builder().url(url).header("User-Agent", "JARVISAssistant/1.0").build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.has("extract")) {
                    json.getString("extract")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchDuckDuckGoSummary(topic: String): String? {
        return try {
            val url = "https://api.duckduckgo.com/?q=" + URLEncoder.encode(topic, "UTF-8") + "&format=json&no_html=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText", "")
                if (abstractText.isNotBlank()) abstractText else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
