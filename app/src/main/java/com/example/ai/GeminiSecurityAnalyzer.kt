package com.example.ai

import com.example.BuildConfig
import com.example.data.model.NetworkConnection
import com.example.data.model.SecurityThreatAssessment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiSecurityAnalyzer {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    suspend fun analyzeConnection(connection: NetworkConnection): Result<SecurityThreatAssessment> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        val prompt = """
            Analyze the following network connection detected on an Android mobile device for security threats, data leaks, or suspicious behavior.
            
            CONNECTION METADATA:
            - Application: ${connection.appName} (${connection.packageName}, Process UID: ${connection.uid})
            - Protocol: ${connection.protocol}
            - Local Endpoint: ${connection.localAddress}:${connection.localPort}
            - Remote Destination: ${connection.displayDestination}:${connection.remotePort} (${connection.serviceName})
            - Network Transport: ${connection.networkType} (Interface: ${connection.interfaceName})
            - Socket State: ${connection.state}
            - Observed Heuristic: ${connection.riskDetails}
            
            Evaluate the connection and provide a response in valid JSON with exactly the following structure:
            {
              "threatLevel": "CLEAN" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "headline": "Short 1-sentence risk summary",
              "summary": "Detailed explanation of what this connection is doing, whether it is normal or anomalous, and potential security implications.",
              "threatVectors": ["Vector 1", "Vector 2"],
              "recommendations": ["Actionable Recommendation 1", "Actionable Recommendation 2"]
            }
        """.trimIndent()

        callGeminiApi(apiKey, prompt)
    }

    suspend fun analyzeAuditLogs(logs: List<NetworkConnection>): Result<SecurityThreatAssessment> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        val sampleLogs = logs.take(25)
        val logsFormatted = sampleLogs.joinToString("\n") { conn ->
            "- [${conn.protocol}] ${conn.appName} (${conn.packageName}) -> ${conn.displayDestination}:${conn.remotePort} (${conn.serviceName}, ${conn.networkType}) | State: ${conn.state} | Alert: ${conn.alertLevel.name}"
        }

        val prompt = """
            Perform a comprehensive Network Security Threat Audit on the following connection logs captured from an Android device:
            
            AUDIT LOGS (${sampleLogs.size} sample entries):
            $logsFormatted
            
            Evaluate for patterns of suspicious activity (such as cleartext data transmission, background tracking, unexpected outbound telemetry, or unusual destination ports).
            Provide a response in valid JSON with exactly the following structure:
            {
              "threatLevel": "CLEAN" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "headline": "Executive threat audit headline",
              "summary": "Comprehensive security audit analysis summarizing overall device network hygiene, risk posture, and anomalous activity.",
              "threatVectors": ["Key identified risk 1", "Key identified risk 2"],
              "recommendations": ["Security recommendation 1", "Security recommendation 2", "Security recommendation 3"]
            }
        """.trimIndent()

        callGeminiApi(apiKey, prompt)
    }

    private fun callGeminiApi(apiKey: String, userPrompt: String): Result<SecurityThreatAssessment> {
        return try {
            val url = "$BASE_URL?key=$apiKey"

            val systemInstruction = "You are an elite cyber threat intelligence analyst and network security engineer for Android devices. Provide rigorous, objective risk assessments and actionable mitigation steps in clean JSON format."

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)

                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                }
                put("generationConfig", genConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                return Result.failure(Exception("Gemini API request failed (${response.code}): $errorBody"))
            }

            val responseBody = response.body?.string().orEmpty()
            val parsedAssessment = parseGeminiResponse(responseBody)
            Result.success(parsedAssessment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeminiResponse(jsonResponse: String): SecurityThreatAssessment {
        val root = JSONObject(jsonResponse)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textPart = parts?.optJSONObject(0)?.optString("text").orEmpty()

        // Clean up markdown block if present
        val cleanJson = textPart.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = JSONObject(cleanJson)
            val threatLevel = obj.optString("threatLevel", "LOW")
            val headline = obj.optString("headline", "Security Assessment Complete")
            val summary = obj.optString("summary", "Analysis completed without findings.")

            val vectorsList = mutableListOf<String>()
            val vectorsArray = obj.optJSONArray("threatVectors")
            if (vectorsArray != null) {
                for (i in 0 until vectorsArray.length()) {
                    vectorsList.add(vectorsArray.optString(i))
                }
            }

            val recsList = mutableListOf<String>()
            val recsArray = obj.optJSONArray("recommendations")
            if (recsArray != null) {
                for (i in 0 until recsArray.length()) {
                    recsList.add(recsArray.optString(i))
                }
            }

            SecurityThreatAssessment(
                threatLevel = threatLevel,
                headline = headline,
                summary = summary,
                threatVectors = vectorsList,
                recommendations = recsList,
                fullReport = cleanJson
            )
        } catch (e: Exception) {
            SecurityThreatAssessment(
                threatLevel = "NOTICE",
                headline = "AI Threat Assessment Result",
                summary = textPart.ifBlank { "Analysis returned non-structured text." },
                threatVectors = emptyList(),
                recommendations = listOf("Review destination IP and connection security certificate."),
                fullReport = textPart
            )
        }
    }
}
