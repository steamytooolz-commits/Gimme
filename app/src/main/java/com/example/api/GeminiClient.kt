package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GamePhase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ToolCall(
    val name: String,
    val args: Map<String, Any>
)

data class GeminiResponse(
    val textResponse: String,
    val parsedToolCalls: List<ToolCall>
)

class GeminiClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateGameResponse(
        systemInstruction: String,
        history: List<Pair<String, String>>, // sender name ("user" or "model") to message text
        currentPhase: GamePhase
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Return a fallback mock response if no API key is set
            return@withContext getFallbackMockResponse(history, currentPhase)
        }

        // Format history into Gemini's format: List of objects with role and parts
        val contentsList = mutableListOf<Map<String, Any>>()
        for ((role, text) in history) {
            val geminiRole = if (role == "user" || role == "Player") "user" else "model"
            contentsList.add(
                mapOf(
                    "role" to geminiRole,
                    "parts" to listOf(mapOf("text" to text))
                )
            )
        }

        val requestMap = mutableMapOf<String, Any>(
            "contents" to contentsList,
            "generationConfig" to mapOf(
                "temperature" to 0.8f,
                "topP" to 0.95f,
                "maxOutputTokens" to 4096
            ),
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            )
        )

        val requestJson = moshi.adapter(Map::class.java).toJson(requestMap)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiClient", "API call failed with code ${response.code}: $errBody")
                    return@withContext getErrorFallbackResponse("Network error: Code ${response.code}. Please ensure your Gemini API key is configured correctly.")
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = moshi.adapter(Map::class.java).fromJson(bodyString)
                
                val candidates = responseJson?.get("candidates") as? List<*>
                val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
                val content = firstCandidate?.get("content") as? Map<*, *>
                val parts = content?.get("parts") as? List<*>
                val firstPart = parts?.firstOrNull() as? Map<*, *>
                val rawText = firstPart?.get("text") as? String ?: ""

                Log.d("GeminiClient", "Raw response: $rawText")
                return@withContext parseResponseText(rawText, currentPhase)
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Exception during API call", e)
            return@withContext getErrorFallbackResponse("Connection error: ${e.localizedMessage}. Check internet or secret configuration.")
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
                @Suppress("UNCHECKED_CAST")
                val parsed = moshi.adapter(Map::class.java).fromJson(jsonStr) as? Map<String, Any>
                if (parsed != null) {
                    val name = parsed["name"] as? String ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val args = parsed["args"] as? Map<String, Any> ?: emptyMap()

                    // --- Hallucination Guardrail ---
                    if (isToolAllowedInPhase(name, currentPhase)) {
                        toolCalls.add(ToolCall(name, args))
                    } else {
                        Log.w("GeminiClient", "Guardrail Blocked tool call: '$name' is not allowed during $currentPhase phase.")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiClient", "Failed to parse tool call JSON: $jsonStr", e)
            }
        }

        // Clean tool calls from the displayed text
        cleanedText = regex.replace(cleanedText, "").trim()

        return GeminiResponse(cleanedText, toolCalls)
    }

    private fun isToolAllowedInPhase(toolName: String, phase: GamePhase): Boolean {
        val normalized = toolName.lowercase()
        return when (phase) {
            GamePhase.INVESTIGATION -> {
                // Allow investigative, forensics, and interrogation tools. Block verdict/objection tools.
                !normalized.contains("verdict") && 
                !normalized.contains("objection") && 
                !normalized.contains("ruling") && 
                !normalized.contains("sentence")
            }
            GamePhase.COURTROOM -> {
                // Allow court, trial, and ruling tools. Block raw raid/warrant/field gather tools.
                !normalized.contains("warrant") && 
                !normalized.contains("raid") && 
                !normalized.contains("gather_raw")
            }
            GamePhase.COLD -> {
                // Cold case: only view/query allowed
                normalized.contains("query") || normalized.contains("view") || normalized.contains("read")
            }
        }
    }

    private fun getErrorFallbackResponse(errorMessage: String): GeminiResponse {
        return GeminiResponse(errorMessage, emptyList())
    }

    private fun getFallbackMockResponse(history: List<Pair<String, String>>, phase: GamePhase): GeminiResponse {
        val lastUserMsg = history.lastOrNull()?.second?.lowercase() ?: ""
        
        if (phase == GamePhase.INVESTIGATION) {
            return when {
                lastUserMsg.contains("beatrice") || lastUserMsg.contains("widow") -> {
                    GeminiResponse(
                        "Lady Beatrice turns away, her hands trembling slightly. 'Magistrate, I have already told you everything. I was in the garden. If you suspect me, show me proof! Otherwise, please let me grieve in peace.'",
                        listOf(ToolCall("modify_npc_stress", mapOf("npc_id" to "beatrice", "delta" to 5)))
                    )
                }
                lastUserMsg.contains("gideon") || lastUserMsg.contains("apothecary") -> {
                    GeminiResponse(
                        "Gideon stammers and wipes cold sweat from his forehead. 'I-I swear on my life, Magistrate! I prepared the sleeping draft precisely as usual. But Chancellor Vance... he visited my chambers the night before! He had plenty of time to steal the Mandrake root!'",
                        listOf(
                            ToolCall("modify_npc_stress", mapOf("npc_id" to "gideon", "delta" to 10)),
                            ToolCall("update_world_state", mapOf("key" to "gideon_suspicions", "value" to "true"))
                        )
                    )
                }
                lastUserMsg.contains("vance") || lastUserMsg.contains("chancellor") -> {
                    GeminiResponse(
                        "Chancellor Vance smiles smoothly, brushing dust off his velvet doublet. 'A tragic loss, indeed. But we must be professional. Gideon's alchemical chest holds enough poison to slaughter the entire court. I suggest you arrest him before he flees.'",
                        listOf(ToolCall("modify_npc_stress", mapOf("npc_id" to "vance", "delta" to -2)))
                    )
                }
                lastUserMsg.contains("search") || lastUserMsg.contains("investigate") || lastUserMsg.contains("look around") -> {
                    if (lastUserMsg.contains("desk") || lastUserMsg.contains("vance")) {
                        GeminiResponse(
                            "You search Lord Vance's private desk in the Chancellor's office. Behind a false bottom, you discover a hidden ledger!",
                            listOf(
                                ToolCall("add_evidence", mapOf(
                                    "id" to "forged_ledger",
                                    "name" to "The Forged Ledger",
                                    "description" to "A hidden ledger detailing illegal chemical transactions, with a poorly forged signature of Lady Beatrice purchasing Nightshade toxin.",
                                    "isAdmissible" to true,
                                    "proceduralViolations" to ""
                                )),
                                ToolCall("advance_time", mapOf("hours" to 2, "reason" to "Searching Chancellor's desk"))
                            )
                        )
                    } else if (lastUserMsg.contains("garden") || lastUserMsg.contains("beatrice")) {
                        GeminiResponse(
                            "You search Lady Beatrice's private chambers. Tucked inside a jewelry box, you find a torn letter!",
                            listOf(
                                ToolCall("add_evidence", mapOf(
                                    "id" to "torn_letter",
                                    "name" to "The Torn Letter",
                                    "description" to "A torn note from Lord Vance to Beatrice, reading: 'Do as instructed at the banquet, or your debts will be exposed to the entire Duchy.'",
                                    "isAdmissible" to true,
                                    "proceduralViolations" to ""
                                )),
                                ToolCall("advance_time", mapOf("hours" to 2, "reason" to "Searching Lady Beatrice's chambers"))
                            )
                        )
                    } else {
                        GeminiResponse(
                            "You search the palace banquet hall. The servants are cleansing the floors, but you find some spilled wine near the Duke's seat showing traces of alchemical powder. (Tip: Try searching Vance's desk or Beatrice's garden/chambers to find critical clues!)",
                            listOf(ToolCall("advance_time", mapOf("hours" to 1, "reason" to "Searching banquet hall")))
                        )
                    }
                }
                else -> {
                    GeminiResponse(
                        "The palace is quiet, filled with tension. You can ask suspects like Beatrice, Gideon, or Vance questions directly, or try commands like '/search vance desk' or '/search beatrice chambers' to discover evidence. What is your next move?",
                        emptyList()
                    )
                }
            }
        } else { // COURTROOM PHASE
            return when {
                lastUserMsg.contains("present") || lastUserMsg.contains("evidence") -> {
                    if (lastUserMsg.contains("ledger")) {
                        GeminiResponse(
                            "You present 'The Forged Ledger' in court. Lord Vance gasps, but his defense attorney immediately objects! 'Objection! This ledger was obtained without a proper court warrant, violating search procedures!' (Magistrate, should you SUSTAIN or OVERRULE the objection?)",
                            listOf(ToolCall("trigger_objection", mapOf("type" to "HEARSAY", "target_witness" to "Lord Vance")))
                        )
                    } else if (lastUserMsg.contains("letter")) {
                        GeminiResponse(
                            "You present 'The Torn Letter' linking Lord Vance's blackmail to Lady Beatrice. Beatrice breaks into tears, admitting: 'It's true! He forced me! He threatened to ruin my family!' Vance turns pale.",
                            listOf(ToolCall("modify_npc_stress", mapOf("npc_id" to "vance", "delta" to 30)))
                        )
                    } else {
                        GeminiResponse(
                            "You present 'The Golden Goblet'. The Court Alchemist confirms it is filled with Mandrake root. Gideon shakes violently in the witness stand.",
                            listOf(ToolCall("modify_npc_stress", mapOf("npc_id" to "gideon", "delta" to 15)))
                        )
                    }
                }
                lastUserMsg.contains("question") || lastUserMsg.contains("interrogate") -> {
                    GeminiResponse(
                        "You interrogate the witness on the stand. Their voice echoes in the high-ceilinged stone chamber of the Court of Themis. Lord Vance's defense attorney raises an objection for leading questions!",
                        listOf(ToolCall("trigger_objection", mapOf("type" to "LEADING", "target_witness" to "Gideon")))
                    )
                }
                else -> {
                    GeminiResponse(
                        "The Courtroom trial is in session. Present evidence to break their defense, question the witnesses, or use the gavel to deliver your verdict! Type '/gavel convict vance' or '/gavel acquit beatrice' to issue your judgment.",
                        emptyList()
                    )
                }
            }
        }
    }
}
