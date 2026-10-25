package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.security.SecurePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class ToneTransformation(val label: String, val prompt: String) {
    SMOOTH(
        "Smooth 🌟",
        "Transform this text into a playful, highly charismatic, and charming banter response for a dating app conversation. Keep it confident, short, and natural."
    ),
    WITTY(
        "Witty 🔥",
        "Rewrite this text to be a fast, clever, humorous, and sharp comeback or reply. Keep it punchy."
    ),
    CHILL(
        "Chill 🧊",
        "Rewrite this text to sound completely relaxed, low-pressure, natural, and effortlessly casual."
    ),
    VICTORIAN(
        "Victorian 📜",
        "Translate and rewrite this text into highly formal, eloquent, dramatic 19th-century Victorian English prose. Use sophisticated vocabulary and classical structural grammar."
    ),
    FIX_GRAMMAR(
        "Fix Grammar ✍️",
        "Act as a strict professional editor. Correct any spelling mistakes, fix grammatical errors, and rephrase the sentence purely for maximum clarity and structural polish without changing the core meaning or adding slang."
    ),
    GEN_Z(
        "Gen Z 🧢",
        "Transform this text into hyper-modern American Gen Z internet slang. Heavily use text shorthand abbreviations (such as idk, ngl, pmo, tbh, fr) and internet/subculture slang words (such as moots, foids, cooked, rizz, no cap, unc, pressed). Make it sound exactly like an online chat room or social media comment."
    )
}

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String, val isApiKeyMissing: Boolean = false) : AiResult<Nothing>()
}

class AiWorkflowEngine(private val context: Context) {

    private val prefs = SecurePreferencesManager.getInstance(context)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun transformText(tone: ToneTransformation, text: String): AiResult<String> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext AiResult.Error("Offline: Please check your internet connection")
        }

        val apiKey = prefs.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error("API key missing. Tap to configure in RizzBoard Settings.", isApiKeyMissing = true)
        }

        val provider = prefs.getAiProvider()

        val fullSystemPrompt = "${tone.prompt}\n\nIMPORTANT: Output ONLY the rewritten text directly. Do not include quotes, greetings, explanations, or introductory text."

        return@withContext try {
            val responseText = if (provider.equals("openai", ignoreCase = true) || apiKey.startsWith("sk-")) {
                callOpenAi(apiKey, fullSystemPrompt, text)
            } else {
                callGemini(apiKey, fullSystemPrompt, text)
            }
            val cleaned = cleanAiOutput(responseText)
            if (cleaned.isNotBlank()) {
                AiResult.Success(cleaned)
            } else {
                AiResult.Error("AI returned an empty response. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in transformText", e)
            AiResult.Error(resolveErrorMessage(e))
        }
    }

    suspend fun generatePasteAndRizzReplies(chatPartnerMessage: String): AiResult<List<String>> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext AiResult.Error("Offline: Please check your internet connection")
        }

        val apiKey = prefs.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error("API key missing. Tap to configure in RizzBoard Settings.", isApiKeyMissing = true)
        }

        val provider = prefs.getAiProvider()

        val systemPrompt = "Analyze this incoming text message from a chat conversation. Generate exactly 3 distinct, short, high-EQ, and highly engaging response options to keep the chemistry alive.\n\nFormatting rule: Return strictly 3 lines, with one response per line. Do NOT prefix with numbers, dashes, or bullet points. Do NOT include markdown or quotes."

        return@withContext try {
            val responseText = if (provider.equals("openai", ignoreCase = true) || apiKey.startsWith("sk-")) {
                callOpenAi(apiKey, systemPrompt, "Incoming Message from chat partner: \"$chatPartnerMessage\"")
            } else {
                callGemini(apiKey, systemPrompt, "Incoming Message from chat partner: \"$chatPartnerMessage\"")
            }

            val suggestions = parseSuggestions(responseText)
            if (suggestions.isNotEmpty()) {
                AiResult.Success(suggestions)
            } else {
                AiResult.Error("Could not generate replies. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generatePasteAndRizzReplies", e)
            AiResult.Error(resolveErrorMessage(e))
        }
    }

    private fun callGemini(apiKey: String, systemPrompt: String, userText: String): String {
        // Models priority: gemini-2.5-flash or gemini-1.5-flash
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$systemPrompt\n\nOriginal Text:\n$userText")
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.75)
                        put("maxOutputTokens", 256)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        if (response.code in 400..404 && model != models.last()) {
                            // Try next model if this one failed
                            lastException = IOException("Gemini API error ${response.code}: $responseBodyString")
                            return@use
                        }
                        throw IOException("Gemini API error ${response.code}: ${parseErrorDetail(responseBodyString)}")
                    }

                    val responseJson = JSONObject(responseBodyString)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text", "")
                        }
                    }
                    throw IOException("Invalid response structure from Gemini API")
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("API error 404") == true) {
                    continue
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IOException("Failed to connect to Gemini API")
    }

    private fun callOpenAi(apiKey: String, systemPrompt: String, userText: String): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            })
            put("temperature", 0.75)
            put("max_tokens", 256)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("OpenAI API error ${response.code}: ${parseErrorDetail(responseBodyString)}")
            }

            val responseJson = JSONObject(responseBodyString)
            val choices = responseJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return message?.optString("content", "") ?: ""
            }
            throw IOException("Invalid response structure from OpenAI API")
        }
    }

    private fun parseSuggestions(rawText: String): List<String> {
        val lines = rawText.split("\n")
            .map { cleanSuggestionLine(it) }
            .filter { it.isNotBlank() }

        return if (lines.size >= 3) {
            lines.take(3)
        } else if (lines.isNotEmpty()) {
            lines
        } else {
            listOf(cleanAiOutput(rawText))
        }
    }

    private fun cleanSuggestionLine(line: String): String {
        var trimmed = line.trim()
        // Strip common numbering like "1. ", "1) ", "- ", "• "
        trimmed = trimmed.replace(Regex("^(\\d+[.)]|[-*•])\\s*"), "")
        return cleanAiOutput(trimmed)
    }

    private fun cleanAiOutput(text: String): String {
        var res = text.trim()
        if (res.startsWith("\"") && res.endsWith("\"") && res.length >= 2) {
            res = res.substring(1, res.length - 1).trim()
        }
        if (res.startsWith("“") && res.endsWith("”") && res.length >= 2) {
            res = res.substring(1, res.length - 1).trim()
        }
        return res
    }

    private fun parseErrorDetail(errorBody: String): String {
        return try {
            val json = JSONObject(errorBody)
            if (json.has("error")) {
                val err = json.optJSONObject("error")
                err?.optString("message", "") ?: json.optString("error", errorBody)
            } else {
                errorBody.take(120)
            }
        } catch (_: Exception) {
            errorBody.take(120)
        }
    }

    private fun resolveErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("401", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "Invalid API Key or forbidden. Please update in settings."
            msg.contains("429", ignoreCase = true) ->
                "Rate limit exceeded. Please wait a few seconds."
            e is IOException ->
                "Network timeout or unreachable. Check your connection."
            else ->
                "Error: ${msg.take(80)}"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val TAG = "AiWorkflowEngine"
    }
}
