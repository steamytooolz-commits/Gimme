package com.example.api

import com.example.data.model.ChatMessage
import com.example.data.model.GamePhase
import com.example.data.model.LlmEndpointConfig
import kotlinx.coroutines.flow.Flow

data class ToolProperty(
    val type: String,
    val description: String
)

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

sealed interface LlmStreamEvent {
    data class ContentChunk(val text: String) : LlmStreamEvent
    data class ToolCallChunk(val name: String, val arguments: String) : LlmStreamEvent
    data object StreamEnd : LlmStreamEvent
    data class StreamError(val error: String) : LlmStreamEvent
}

interface LlmClient {
    // Streams text and tool calls via Server-Sent Events (SSE)
    suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>,
        config: LlmEndpointConfig,
        apiKey: String
    ): Flow<LlmStreamEvent>
    
    // Validates the connection and fetches available models
    suspend fun validateConnection(config: LlmEndpointConfig, apiKey: String): Result<List<String>>

    // Main non-streaming entry point for the VM
    suspend fun generateGameResponse(
        systemInstruction: String,
        history: List<Pair<String, String>>,
        currentPhase: GamePhase,
        config: LlmEndpointConfig?,
        apiKey: String
    ): GeminiResponse
}
