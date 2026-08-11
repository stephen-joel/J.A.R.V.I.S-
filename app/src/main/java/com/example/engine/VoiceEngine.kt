package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate

    private val prefs = context.getSharedPreferences("jarvis_voice_prefs", Context.MODE_PRIVATE)

    init {
        _pitch.value = prefs.getFloat("pitch", 1.0f)
        _speechRate.value = prefs.getFloat("speech_rate", 1.0f)
        _selectedVoiceName.value = prefs.getString("selected_voice", null)
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                setupTTS()
            } else {
                Log.e("VoiceEngine", "TTS Language missing or not supported")
            }
        } else {
            Log.e("VoiceEngine", "TTS Initialization failed with status: $status")
        }
    }

    private fun setupTTS() {
        val ttsInstance = tts ?: return

        ttsInstance.setPitch(_pitch.value)
        ttsInstance.setSpeechRate(_speechRate.value)

        // Load available voices
        val voices = try {
            ttsInstance.voices?.filter { !it.isNetworkConnectionRequired }?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        _availableVoices.value = voices

        // Voice selection logic according to specification:
        // Priority:
        // 1. User saved voice selection if valid
        // 2. Male voice if suitable one exists
        // 3. Fallback to current working default TTS voice (so speech NEVER fails!)
        val savedVoiceName = _selectedVoiceName.value
        var voiceToUse: Voice? = voices.find { it.name == savedVoiceName }

        if (voiceToUse == null) {
            // Search for a suitable male voice
            voiceToUse = voices.find { v ->
                val nameLower = v.name.lowercase()
                (nameLower.contains("male") || nameLower.contains("man") || nameLower.contains("en-us-x-sfg") || nameLower.contains("en-us-x-iom")) && !nameLower.contains("female")
            }
        }

        if (voiceToUse == null && voices.isNotEmpty()) {
            voiceToUse = ttsInstance.defaultVoice
        }

        if (voiceToUse != null) {
            try {
                ttsInstance.voice = voiceToUse
                _selectedVoiceName.value = voiceToUse.name
            } catch (e: Exception) {
                Log.w("VoiceEngine", "Failed to set preferred voice, keeping fallback default", e)
            }
        }

        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w("VoiceEngine", "TTS not initialized, attempting fallback recovery")
            onComplete?.invoke()
            return
        }

        val utteranceId = "JARVIS_MSG_${System.currentTimeMillis()}"
        tts?.setPitch(_pitch.value)
        tts?.setSpeechRate(_speechRate.value)

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        try {
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e("VoiceEngine", "Error invoking speak")
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Speech synthesis failed gracefully", e)
            onComplete?.invoke()
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error stopping TTS", e)
        }
    }

    fun testVoice() {
        speak("Hello. I am JARVIS. How can I help you?")
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch
        tts?.setPitch(pitch)
        prefs.edit().putFloat("pitch", pitch).apply()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun selectVoice(voiceName: String) {
        val voice = _availableVoices.value.find { it.name == voiceName }
        if (voice != null && tts != null) {
            try {
                tts?.voice = voice
                _selectedVoiceName.value = voiceName
                prefs.edit().putString("selected_voice", voiceName).apply()
            } catch (e: Exception) {
                Log.e("VoiceEngine", "Error switching voice to $voiceName", e)
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Shutdown error", e)
        }
    }
}
