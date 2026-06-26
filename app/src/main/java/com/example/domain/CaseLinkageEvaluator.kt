package com.example.domain

import com.example.api.LlmClient
import com.example.data.model.ColdCaseDigest
import com.example.data.model.GamePhase
import com.example.data.repository.GameRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class CaseLinkageEvaluator(
    private val repository: GameRepository,
    private val llmClient: LlmClient
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun createDigestForClosedCase(caseId: String): ColdCaseDigest? {
        val groundTruth = repository.getGroundTruth(caseId) ?: return null
        val report = repository.getEvaluationReport(caseId)
        
        val prompt = """
            You are an expert archivist in the Court of Themis.
            Please summarize the following closed/cold case into a Cold Case Digest.
            
            Case ID: ${groundTruth.caseId}
            Core Crime: ${groundTruth.coreCrime}
            Ground Truth: ${groundTruth.absoluteTruth}
            Outcome: ${report?.overallGrade ?: "Unresolved"}
            
            Return ONLY a valid JSON object matching this schema:
            {
                "originalCaseId": "${groundTruth.caseId}",
                "summary": "Brief summary of the case and its outcome",
                "keyPlayers": ["Name 1", "Name 2"],
                "unresolvedThreads": ["Thread 1", "Thread 2"]
            }
            
            Respond ONLY with a valid raw JSON block. No markdown wrappers.
        """.trimIndent()
        
        return try {
            val apiKey = repository.getWorldState("llm_api_key") ?: ""
            // Use config if needed, otherwise null
            val response = llmClient.generateGameResponse(prompt, emptyList(), GamePhase.COLD, null, apiKey)
            
            var cleanText = response.textResponse.trim()
            if (cleanText.startsWith("```json")) {
                cleanText = cleanText.substringAfter("```json").substringBeforeLast("```")
            } else if (cleanText.startsWith("```")) {
                cleanText = cleanText.substringAfter("```").substringBeforeLast("```")
            }
            cleanText = cleanText.trim()
            
            val adapter = moshi.adapter(ColdCaseDigest::class.java)
            val digest = adapter.fromJson(cleanText)
            
            if (digest != null) {
                repository.insertColdCaseDigest(digest)
            }
            digest
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
