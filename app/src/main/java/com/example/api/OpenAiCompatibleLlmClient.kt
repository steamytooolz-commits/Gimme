package com.example.api

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.GamePhase
import com.example.data.model.LlmEndpointConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiMessage(
    val role: String,
    val content: String
)

class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float,
    val max_tokens: Int,
    val stream: Boolean
)

class OpenAiCompatibleLlmClient : LlmClient {

    private val tag = "OpenAiLlmClient"
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Delegate for Gemini API fallback
    private val geminiClient = GeminiClient()

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>,
        config: LlmEndpointConfig,
        apiKey: String
    ): Flow<LlmStreamEvent> = flow {
        val url = if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions"
        
        val openAiMessages = messages.map { msg ->
            val role = if (msg.sender == "Player") "user" else "assistant"
            OpenAiMessage(role = role, content = msg.text)
        }

        val requestPayload = OpenAiChatRequest(
            model = config.modelName,
            messages = openAiMessages,
            temperature = config.temperature,
            max_tokens = config.maxTokens,
            stream = true
        )

        val requestBodyJson = moshi.adapter(OpenAiChatRequest::class.java).toJson(requestPayload)
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(mediaType))

        if (config.requiresApiKey && apiKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(requestBuilder.build()).execute()
            }

            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown error"
                emit(LlmStreamEvent.StreamError("HTTP ${response.code}: $errorMsg"))
                response.close()
                return@flow
            }

            val source = response.body?.source()
            if (source == null) {
                emit(LlmStreamEvent.StreamError("Response body is empty"))
                response.close()
                return@flow
            }

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        emit(LlmStreamEvent.StreamEnd)
                        break
                    }
                    if (data.isNotEmpty()) {
                        try {
                            val responseJson = moshi.adapter(Map::class.java).fromJson(data)
                            val choices = responseJson?.get("choices") as? List<*>
                            val firstChoice = choices?.firstOrNull() as? Map<*, *>
                            val delta = firstChoice?.get("delta") as? Map<*, *>
                            val content = delta?.get("content") as? String
                            
                            if (content != null) {
                                emit(LlmStreamEvent.ContentChunk(content))
                            }
                        } catch (e: Exception) {
                            // Ignore malformed streaming chunks gracefully
                        }
                    }
                }
            }
            response.close()
        } catch (e: Exception) {
            emit(LlmStreamEvent.StreamError(e.localizedMessage ?: "Unknown connection error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun validateConnection(config: LlmEndpointConfig, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        // For G4F, we skip real network validation as it frequently triggers rate limits or returns 500
        // on validation/models endpoints. We allow the user to proceed with their manual config.
        if (config.providerName == "G4F") {
            return@withContext Result.success(listOf(config.modelName))
        }

        // First, attempt to fetch models from standard GET /v1/models endpoint
        val modelsUrl = if (config.baseUrl.endsWith("/")) "${config.baseUrl}models" else "${config.baseUrl}/models"
        val requestBuilder = Request.Builder().url(modelsUrl).get()
        
        if (config.requiresApiKey && apiKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val responseJson = moshi.adapter(Map::class.java).fromJson(bodyString)
                    val dataArray = responseJson?.get("data") as? List<*>
                    val models = dataArray?.mapNotNull { item ->
                        val itemMap = item as? Map<*, *>
                        itemMap?.get("id") as? String
                    } ?: emptyList()
                    
                    if (models.isNotEmpty()) {
                        return@withContext Result.success(models)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "GET /models call failed or unsupported. Retrying via dummy completion call.", e)
        }

        return@withContext performDummyCompletionValidation(config, apiKey)
    }

    private suspend fun performDummyCompletionValidation(config: LlmEndpointConfig, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        // Fallback: Make a cheap, tiny 1-token completion call to validate credentials and config
        val completionsUrl = if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions"
        val fallbackPayload = OpenAiChatRequest(
            model = config.modelName,
            messages = listOf(OpenAiMessage("user", "Hello")),
            temperature = 0.7f,
            max_tokens = 1,
            stream = false
        )
        val requestBodyJson = moshi.adapter(OpenAiChatRequest::class.java).toJson(fallbackPayload)
        val completionRequestBuilder = Request.Builder()
            .url(completionsUrl)
            .post(requestBodyJson.toRequestBody(mediaType))

        if (config.requiresApiKey && apiKey.isNotEmpty()) {
            completionRequestBuilder.header("Authorization", "Bearer $apiKey")
        }

        return@withContext try {
            client.newCall(completionRequestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(listOf(config.modelName))
                } else {
                    val errorMsg = response.body?.string() ?: "Verification failed"
                    Result.failure(IOException("Server returned code ${response.code}: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateGameResponse(
        systemInstruction: String,
        history: List<Pair<String, String>>,
        currentPhase: GamePhase,
        config: LlmEndpointConfig?,
        apiKey: String
    ): GeminiResponse = withContext(Dispatchers.IO) {
        // Fallback to local Gemini client if BYOK config is not set or empty
        if (config == null || config.providerName.isEmpty() || config.baseUrl.isEmpty()) {
            return@withContext geminiClient.generateGameResponse(systemInstruction, history, currentPhase)
        }

        val url = if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions"
        
        // Unify standard OpenAI format
        val messagesList = mutableListOf<OpenAiMessage>()
        
        // Add system instruction as system message
        messagesList.add(OpenAiMessage(role = "system", content = systemInstruction))
        
        // Add conversation history
        for ((role, text) in history) {
            val openAiRole = if (role == "user" || role == "Player") "user" else "assistant"
            messagesList.add(OpenAiMessage(role = openAiRole, content = text))
        }

        val requestPayload = OpenAiChatRequest(
            model = config.modelName,
            messages = messagesList,
            temperature = config.temperature,
            max_tokens = config.maxTokens,
            stream = false
        )

        val requestBodyJson = moshi.adapter(OpenAiChatRequest::class.java).toJson(requestPayload)
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(mediaType))

        if (config.requiresApiKey && apiKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(tag, "API error: Code ${response.code}: $errorMsg")
                    return@withContext GeminiResponse(
                        textResponse = "Error connecting to Custom LLM provider: Code ${response.code}. Please verify settings.",
                        parsedToolCalls = emptyList()
                    )
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = moshi.adapter(Map::class.java).fromJson(bodyString)
                val choices = responseJson?.get("choices") as? List<*>
                val firstChoice = choices?.firstOrNull() as? Map<*, *>
                val messageObj = firstChoice?.get("message") as? Map<*, *>
                val rawText = messageObj?.get("content") as? String ?: ""

                Log.d(tag, "Response from ${config.providerName}: $rawText")
                return@withContext parseResponseText(rawText, currentPhase)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during custom LLM call", e)
            return@withContext GeminiResponse(
                textResponse = "Failed to connect to provider (${config.providerName}): ${e.localizedMessage}. Check connection/settings.",
                parsedToolCalls = emptyList()
            )
        }
    }

    private fun parseResponseText(rawText: String, currentPhase: GamePhase): GeminiResponse {
        val toolCalls = mutableListOf<ToolCall>()
        var cleanedText = rawText

        // Look for tool calls like [TOOL_CALL: {"name": "...", "args": {...}}]
        val regex = Regex("\\[TOOL_CALL:\\s*(\\{.*?\\})\\s*\\]")
        val matches = regex.findAll(rawText)

        for (match in matches) {
            val jsonStr = match.groups[1]?.value ?: continue
            try {
                val parsed = moshi.adapter(Map::class.java).fromJson(jsonStr) as? Map<*, *>
                if (parsed != null) {
                    val name = parsed["name"] as? String ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val argsMap = parsed["args"] as? Map<String, Any> ?: emptyMap()

                    if (isToolAllowedInPhase(name, currentPhase)) {
                        toolCalls.add(ToolCall(name, argsMap))
                    } else {
                        Log.w(tag, "Blocked tool '$name' not allowed in $currentPhase")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse tool call JSON: $jsonStr", e)
            }
        }

        // Clean tool calls from the displayed text
        cleanedText = regex.replace(cleanedText, "").trim()

        return GeminiResponse(cleanedText, toolCalls)
    }

    private fun isToolAllowedInPhase(toolName: String, phase: GamePhase): Boolean {
        // Give the AI API full, unrestricted control to execute any tool or event in any phase!
        return true
    }
}
