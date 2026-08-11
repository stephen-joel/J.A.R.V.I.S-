package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(viewModel: JarvisViewModel) {
    val conversations by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()

    val availableVoices by viewModel.voiceEngine.availableVoices.collectAsState()
    val selectedVoiceName by viewModel.voiceEngine.selectedVoiceName.collectAsState()
    val pitch by viewModel.voiceEngine.pitch.collectAsState()
    val speechRate by viewModel.voiceEngine.speechRate.collectAsState()

    var sliderPitch by remember(pitch) { mutableFloatStateOf(pitch) }
    var sliderRate by remember(speechRate) { mutableFloatStateOf(speechRate) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SETTINGS & VOICE CONTROLS",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyanGlow,
            letterSpacing = 1.5.sp
        )

        // Voice Controls Section
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyanGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JARVIS VOICE ENGINE",
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TEST VOICE Button
                Button(
                    onClick = { viewModel.testVoice() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_voice_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.background)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TEST VOICE", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Selector Dropdown
                Text("Installed TTS Voice:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                        .clickable { voiceDropdownExpanded = true }
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedVoiceName ?: "Default Voice (Auto Male / Fallback)",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    DropdownMenu(
                        expanded = voiceDropdownExpanded,
                        onDismissRequest = { voiceDropdownExpanded = false }
                    ) {
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.name, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.voiceEngine.selectVoice(voice.name)
                                    voiceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pitch Slider
                Text("Pitch: ${String.format("%.1f", sliderPitch)}", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = sliderPitch,
                    onValueChange = {
                        sliderPitch = it
                        viewModel.voiceEngine.setPitch(it)
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanGlow,
                        activeTrackColor = CyanGlow,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speech Rate Slider
                Text("Speech Rate: ${String.format("%.1f", sliderRate)}", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = sliderRate,
                    onValueChange = {
                        sliderRate = it
                        viewModel.voiceEngine.setSpeechRate(it)
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberGlow,
                        activeTrackColor = AmberGlow,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )
            }
        }

        // Conversation History Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AmberGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONVERSATION HISTORY",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = CyanGlow)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (conversations.isEmpty()) {
                    Text("No saved conversations.", fontSize = 12.sp, color = TextMuted)
                } else {
                    conversations.take(6).forEach { conv ->
                        val isSelected = conv.id == activeConvId
                        Surface(
                            color = if (isSelected) CyanGlow.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.selectConversation(conv.id) }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = conv.title,
                                    color = if (isSelected) CyanGlow else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanGlow)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Conversation History")
                }
            }
        }
    }
}
