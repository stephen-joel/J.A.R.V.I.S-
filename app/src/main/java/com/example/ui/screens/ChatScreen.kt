package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanGlowDim
import com.example.ui.theme.JarvisBubbleBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBg

@Composable
fun ChatScreen(viewModel: JarvisViewModel) {
    val messages by viewModel.activeMessages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsState()
    val isListening by viewModel.speechEngine.isListening.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Futuristic Glowing Header Visualizer
        JarvisHeaderVisualizer(
            isProcessing = isProcessing,
            isSpeaking = isSpeaking,
            isListening = isListening
        )

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatGreeting()
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(msg = msg)
                }
            }
        }

        // Input Controls Section
        Surface(
            color = JarvisSurface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AmberGlow else CyanGlow.copy(alpha = 0.2f))
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color.Black else CyanGlow
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Talk with JARVIS...", color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendMessage(textToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) CyanGlow else CyanGlowDim.copy(alpha = 0.3f))
                        .testTag("send_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.Black else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JarvisHeaderVisualizer(
    isProcessing: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (isSpeaking || isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 400 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = JarvisSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                    val color = when {
                        isSpeaking -> CyanGlow
                        isListening -> AmberGlow
                        isProcessing -> Color(0xFF9D4EDD)
                        else -> CyanGlowDim
                    }
                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = color,
                        radius = size.minDimension / 3,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = color,
                        radius = size.minDimension / 6
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JARVIS AI ASSISTANT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CyanGlow,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = when {
                        isSpeaking -> "VOCALIZING RESPONSE"
                        isListening -> "LISTENING TO VOICE..."
                        isProcessing -> "PROCESSING LOCAL INTENTION..."
                        else -> "ONLINE • 100% LOCAL & PERSISTENT"
                    },
                    fontSize = 12.sp,
                    color = if (isSpeaking || isListening) AmberGlow else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isSpeaking) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speaking",
                    tint = CyanGlow,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(msg: MessageEntity) {
    val isUser = msg.sender == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bubbleBg = if (isUser) UserBubbleBg else JarvisBubbleBg
    val textColor = TextPrimary

    Column(
        horizontalAlignment = align,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "JARVIS",
                    tint = CyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("JARVIS", fontSize = 11.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
            } else {
                Text("YOU", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleBg)
                .border(
                    1.dp,
                    if (isUser) CyanGlow.copy(alpha = 0.4f) else JarvisCardBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = msg.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                if (msg.isAction && !msg.actionDetails.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ${msg.actionDetails}",
                        fontSize = 11.sp,
                        color = AmberGlow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "JARVIS",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyanGlow,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Conversational • Adaptive • Local-First",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = JarvisSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Try saying or typing:",
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• \"Hey JARVIS, I'm bored.\"", fontSize = 13.sp, color = TextPrimary)
                    Text("• \"My favorite browser is Firefox.\"", fontSize = 13.sp, color = TextPrimary)
                    Text("• \"When I say YT, I mean YouTube.\"", fontSize = 13.sp, color = TextPrimary)
                    Text("• \"Open YouTube\" or \"Open YT\"", fontSize = 13.sp, color = TextPrimary)
                    Text("• \"What is Minecraft?\" -> \"Can I play it on my phone?\"", fontSize = 13.sp, color = TextPrimary)
                }
            }
        }
    }
}
