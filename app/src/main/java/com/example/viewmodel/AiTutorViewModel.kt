package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val responseText: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiTutorViewModel : ViewModel() {
    private val TAG = "AiTutorViewModel"

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "cosmos_welcome",
                text = "Welcome to the Cosmos! Whether you're untangling Physics equations, debugging Python, or gearing up for a 50-mark Boss Fight—drop your challenge below and let's solve it together!",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendMessage(prompt: String, attachedImage: Bitmap? = null) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() && attachedImage == null) return

        val displayPrompt = if (attachedImage != null && cleanPrompt.isBlank()) {
            "[Attached problem image] Please solve this step by step."
        } else if (attachedImage != null) {
            "[Image attached] $cleanPrompt"
        } else {
            cleanPrompt
        }

        val userMsg = ChatMessage(text = displayPrompt, isUser = true)
        val replyId = java.util.UUID.randomUUID().toString()
        _messages.value = _messages.value + userMsg
        _uiState.value = AiUiState.Loading

        viewModelScope.launch {
            try {
                val systemDirective = "You are the Cosmos AI Companion—an expert, encouraging Indo-Futuristic study mentor. Give crystal-clear step-by-step explanations, highlight key formulas, and format any code inside fenced ``` blocks."
                val requestContent = content {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text("$systemDirective\n\nStudent query: ${cleanPrompt.ifBlank { "Explain and solve what is shown in the attached study image step by step." }}")
                }

                val builder = StringBuilder()
                var placeholderAdded = false

                withTimeout(60_000L) {
                    model.generateContentStream(requestContent).collect { chunk ->
                        val part = chunk.text
                        if (!part.isNullOrEmpty()) {
                            builder.append(part)
                            val updatedText = builder.toString()
                            if (!placeholderAdded) {
                                placeholderAdded = true
                                _messages.value = _messages.value + ChatMessage(
                                    id = replyId,
                                    text = updatedText,
                                    isUser = false
                                )
                            } else {
                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == replyId) msg.copy(text = updatedText) else msg
                                }
                            }
                        }
                    }
                }

                val finalText = builder.toString().ifBlank {
                    "I couldn't generate an answer right now. Try rephrasing your challenge!"
                }
                if (!placeholderAdded) {
                    _messages.value = _messages.value + ChatMessage(id = replyId, text = finalText, isUser = false)
                }
                _uiState.value = AiUiState.Success(finalText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                val errorMsg = ChatMessage(text = "Cosmos link timed out after 60 seconds. Please try again.", isUser = false)
                _messages.value = _messages.value + errorMsg
                _uiState.value = AiUiState.Error("Timed out.")
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                val errorMsg = ChatMessage(
                    text = "Unable to reach the Cosmos mentor right now: ${e.localizedMessage ?: "Please check connection and retry."}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMsg
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed.")
            }
        }
    }
}
