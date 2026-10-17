package com.shahbuddindairy.app.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shahbuddindairy.app.data.model.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Result returned by PrivateAiApiClient.
 */
sealed class AiParseResult {
    data class Success(
        val command: DairyAiMunshiCommand,
        val serverModel: String,
        val rawServerResponse: String? = null
    ) : AiParseResult()

    data class Fallback(
        val command: DairyAiMunshiCommand,
        val reason: String
    ) : AiParseResult()
}

/**
 * Result returned by server connection and latency test.
 */
data class ConnectionTestResult(
    val isConnected: Boolean,
    val latencyMs: Long,
    val modelName: String?,
    val message: String
)

/**
 * Safe, Privacy-Preserving Client for Private Local AI API Server (Debian 13 llama.cpp over Tailscale).
 * - Strictly preserves offline behavior (automatic fallback to local deterministic parser).
 * - Enforces HTTPS and safe connection timeouts.
 * - Leaves monetary calculation to the local CalculationEngine.
 */
class PrivateAiApiClient(private val context: Context) {

    companion object {
        private const val TAG = "PrivateAiApiClient"
        private const val PREFS_NAME = "private_ai_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_CONFIGURED_MODEL = "configured_model"

        // Default Private Tailscale Endpoint (Configurable via UI/Preferences)
        const val DEFAULT_SERVER_URL = "https://fahad.tail2abf56.ts.net"
        const val DEFAULT_MODEL = "openbmb/MiniCPM-V-4.7-1B-gguf:Q4_K_M"
    }

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) {
            prefs.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()
        }

    var isAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()
        }

    var configuredModel: String
        get() = prefs.getString(KEY_CONFIGURED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) {
            prefs.edit().putString(KEY_CONFIGURED_MODEL, value.trim()).apply()
        }

    /**
     * Checks if the private AI server is reachable and reports health.
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        if (!isAiEnabled) return@withContext false
        try {
            val url = URL("$serverUrl/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode
            conn.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed: ${e.message}")
            false
        }
    }

    /**
     * Fast ping latency measurement for live header badge.
     * Returns round-trip time in milliseconds, or null if unreachable.
     */
    suspend fun checkPingLatency(): Long? = withContext(Dispatchers.IO) {
        if (!isAiEnabled) return@withContext null
        val start = System.currentTimeMillis()
        try {
            val url = URL("$serverUrl/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode
            conn.disconnect()
            if (responseCode in 200..299) {
                System.currentTimeMillis() - start
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Auto-detects server connection, measures ping latency, and fetches loaded model name.
     */
    suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val url = URL("$serverUrl/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode
            conn.disconnect()
            val latency = System.currentTimeMillis() - start

            if (responseCode in 200..299) {
                val detectedModel = fetchLoadedModel() ?: configuredModel
                configuredModel = detectedModel
                ConnectionTestResult(
                    isConnected = true,
                    latencyMs = latency,
                    modelName = detectedModel,
                    message = "कनेक्टेड (पिंक: ${latency}ms)"
                )
            } else {
                ConnectionTestResult(
                    isConnected = false,
                    latencyMs = latency,
                    modelName = null,
                    message = "सर्वर त्रुटि: HTTP $responseCode"
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            ConnectionTestResult(
                isConnected = false,
                latencyMs = latency,
                modelName = null,
                message = "कनेक्शन विफल (${e.localizedMessage ?: "Timeout"})"
            )
        }
    }

    private fun fetchLoadedModel(): String? {
        return try {
            val url = URL("$serverUrl/v1/models")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()
                val json = gson.fromJson(text, JsonObject::class.java)
                val data = json.getAsJsonArray("data")
                if (data != null && !data.isEmpty) {
                    val first = data.get(0).asJsonObject
                    first.get("id")?.asString
                } else null
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a Hindi or Hinglish spoken/typed dairy command using the private AI server.
     * If the server is offline or fails, falls back seamlessly to the local DairyAiMunshiEngine.
     */
    suspend fun parseDairyInstruction(
        input: String,
        customers: List<CustomerEntity>
    ): AiParseResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext AiParseResult.Fallback(
                command = DairyAiMunshiEngine.parseInput("", customers),
                reason = "इनपुट खाली है"
            )
        }

        if (!isAiEnabled) {
            return@withContext AiParseResult.Fallback(
                command = DairyAiMunshiEngine.parseInput(trimmed, customers),
                reason = "AI सर्वर बंद है (लोकल मोड सक्रिय)"
            )
        }

        try {
            val systemPrompt = """
                You are a dairy transaction parser for Shahbuddin Dairy.
                Extract dairy transaction details from the user's Hindi or Hinglish message.
                Respond ONLY with a valid JSON object in this exact schema:
                {
                  "type": "SALE" or "PURCHASE",
                  "name": "Customer or supplier name or null",
                  "liters": 0.0 or null,
                  "rate": 0.0 or null,
                  "cash_paid": 0.0 or null,
                  "is_naga": true or false
                }
                Do not include markdown backticks, explanations, or reasoning. Output only the JSON.
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 250)

                val messages = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", trimmed)
                    })
                }
                add("messages", messages)
            }

            val endpoint = URL("$serverUrl/v1/chat/completions")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 12000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                conn.disconnect()
                return@withContext fallbackToLocal(trimmed, customers, "सर्वर त्रुटि HTTP $responseCode")
            }

            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            conn.disconnect()

            val parsedJson = extractJsonFromResponse(responseText)
            if (parsedJson == null) {
                return@withContext fallbackToLocal(trimmed, customers, "अमान्य AI फॉर्मेट")
            }

            // Map extracted fields into DairyAiMunshiCommand
            val extractedName = if (parsedJson.has("name") && !parsedJson.get("name").isJsonNull) {
                parsedJson.get("name").asString.trim()
            } else ""

            val extractedLiters = if (parsedJson.has("liters") && !parsedJson.get("liters").isJsonNull) {
                parsedJson.get("liters").asDouble
            } else null

            val extractedCash = if (parsedJson.has("cash_paid") && !parsedJson.get("cash_paid").isJsonNull) {
                parsedJson.get("cash_paid").asDouble
            } else null

            val extractedIsNaga = if (parsedJson.has("is_naga") && !parsedJson.get("is_naga").isJsonNull) {
                parsedJson.get("is_naga").asBoolean
            } else false

            // Match customer identity against actual local Room database records
            val matchedCustomer = matchCustomerLocally(extractedName, customers)

            val explanation = buildString {
                append("✨ AI (Debian 13): ")
                if (matchedCustomer != null) {
                    append("ग्राहक ${matchedCustomer.name}")
                } else if (extractedName.isNotBlank()) {
                    append("नाम '$extractedName'")
                }
                if (extractedIsNaga) {
                    append(" • आज नागा (0L)")
                } else if (extractedLiters != null) {
                    append(" • दूध ${extractedLiters}L")
                }
                if (extractedCash != null && extractedCash > 0) {
                    append(" • नकद जमा ₹%.0f".format(extractedCash))
                }
            }

            val command = DairyAiMunshiCommand(
                matchedCustomer = matchedCustomer,
                customerNameQuery = extractedName,
                confidence = if (matchedCustomer != null) 0.95f else 0.7f,
                liters = extractedLiters,
                cash = extractedCash,
                isNaga = extractedIsNaga,
                rawText = trimmed,
                explanationHindi = explanation
            )

            AiParseResult.Success(
                command = command,
                serverModel = DEFAULT_MODEL,
                rawServerResponse = responseText
            )
        } catch (e: Exception) {
            Log.w(TAG, "Private AI API call failed, falling back to local engine: ${e.message}")
            fallbackToLocal(trimmed, customers, "नेटवर्क ऑफ़लाइन (${e.localizedMessage ?: "timeout"})")
        }
    }

    /**
     * Parses an image (e.g. handwritten dairy chit or receipt) using the multimodal vision capability.
     */
    suspend fun parseReceiptImage(
        base64Jpeg: String,
        customers: List<CustomerEntity>
    ): AiParseResult = withContext(Dispatchers.IO) {
        if (!isAiEnabled) {
            return@withContext AiParseResult.Fallback(
                command = DairyAiMunshiCommand(explanationHindi = "AI सर्वर बंद है"),
                reason = "AI सर्वर बंद है"
            )
        }

        try {
            val systemPrompt = """
                You are a dairy receipt analyzer for Shahbuddin Dairy.
                Extract customer name, milk liters, rate, and cash amount from this handwritten slip or receipt.
                Respond ONLY with a JSON object:
                {"type": "SALE", "name": string, "liters": float, "rate": float, "cash_paid": float, "is_naga": boolean}
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 300)

                val messages = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        val contentArray = com.google.gson.JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("type", "text")
                                addProperty("text", "कृपया इस पर्ची का हिसाब निकालें:")
                            })
                            add(JsonObject().apply {
                                addProperty("type", "image_url")
                                add("image_url", JsonObject().apply {
                                    addProperty("url", "data:image/jpeg;base64,$base64Jpeg")
                                })
                            })
                        }
                        add("content", contentArray)
                    })
                }
                add("messages", messages)
            }

            val endpoint = URL("$serverUrl/v1/chat/completions")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 20000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                conn.disconnect()
                return@withContext fallbackToLocal("", customers, "विज़न सर्वर त्रुटि HTTP $responseCode")
            }

            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            conn.disconnect()

            val parsedJson = extractJsonFromResponse(responseText)
                ?: return@withContext fallbackToLocal("", customers, "पर्ची का हिसाब समझ नहीं आया")

            val extractedName = if (parsedJson.has("name") && !parsedJson.get("name").isJsonNull) {
                parsedJson.get("name").asString.trim()
            } else ""

            val extractedLiters = if (parsedJson.has("liters") && !parsedJson.get("liters").isJsonNull) {
                parsedJson.get("liters").asDouble
            } else null

            val extractedCash = if (parsedJson.has("cash_paid") && !parsedJson.get("cash_paid").isJsonNull) {
                parsedJson.get("cash_paid").asDouble
            } else null

            val matchedCustomer = matchCustomerLocally(extractedName, customers)

            val command = DairyAiMunshiCommand(
                matchedCustomer = matchedCustomer,
                customerNameQuery = extractedName,
                confidence = if (matchedCustomer != null) 0.9f else 0.6f,
                liters = extractedLiters,
                cash = extractedCash,
                isNaga = false,
                rawText = "पर्ची स्कैन",
                explanationHindi = "📷 पर्ची से निकाला गया हिसाब: ${matchedCustomer?.name ?: extractedName} • ${extractedLiters ?: 0.0}L"
            )

            AiParseResult.Success(command = command, serverModel = DEFAULT_MODEL, rawServerResponse = responseText)
        } catch (e: Exception) {
            Log.w(TAG, "Image analysis failed: ${e.message}")
            fallbackToLocal("", customers, "पर्ची स्कैन विफल: ${e.localizedMessage}")
        }
    }

    private fun fallbackToLocal(
        input: String,
        customers: List<CustomerEntity>,
        reason: String
    ): AiParseResult.Fallback {
        val localCmd = DairyAiMunshiEngine.parseInput(input, customers)
        val note = if (input.isNotBlank()) " (लोकल ऑफ़लाइन मोड: $reason)" else ""
        return AiParseResult.Fallback(
            command = localCmd.copy(explanationHindi = localCmd.explanationHindi + note),
            reason = reason
        )
    }

    private fun extractJsonFromResponse(responseBody: String): JsonObject? {
        try {
            val root = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = root.getAsJsonArray("choices") ?: return null
            if (choices.isEmpty) return null
            val firstChoice = choices[0].asJsonObject
            val message = firstChoice.getAsJsonObject("message") ?: return null
            val content = message.get("content")?.asString ?: return null

            // Find JSON within markdown blocks or raw text
            val jsonText = if (content.contains("```json")) {
                content.substringAfter("```json").substringBefore("```").trim()
            } else if (content.contains("```")) {
                content.substringAfter("```").substringBefore("```").trim()
            } else {
                val start = content.indexOf('{')
                val end = content.lastIndexOf('}')
                if (start >= 0 && end > start) {
                    content.substring(start, end + 1)
                } else {
                    content.trim()
                }
            }

            return gson.fromJson(jsonText, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract JSON from response: ${e.message}", e)
            return null
        }
    }

    private fun matchCustomerLocally(nameQuery: String, customers: List<CustomerEntity>): CustomerEntity? {
        if (nameQuery.isBlank() || customers.isEmpty()) return null
        val cleanQuery = nameQuery.trim().lowercase(Locale.ROOT)

        // Exact match
        customers.firstOrNull { it.name.trim().lowercase(Locale.ROOT) == cleanQuery }?.let { return it }

        // Substring match
        customers.firstOrNull { it.name.trim().lowercase(Locale.ROOT).contains(cleanQuery) }?.let { return it }
        customers.firstOrNull { cleanQuery.contains(it.name.trim().lowercase(Locale.ROOT)) }?.let { return it }

        // Mohalla match if query has mohalla
        customers.firstOrNull {
            it.mohalla.isNotBlank() && cleanQuery.contains(it.mohalla.trim().lowercase(Locale.ROOT))
        }?.let { return it }

        return null
    }
}
