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
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
 * - Direct Tailscale IP socket routing with explicit TLS SNI bypassing Android MagicDNS limits.
 * - Handles both standard JSON responses and reasoning-model natural language output.
 * - Authoritative monetary math is always computed locally by CalculationEngine.
 */
class PrivateAiApiClient(private val context: Context) {

    companion object {
        private const val TAG = "PrivateAiApiClient"
        private const val PREFS_NAME = "private_ai_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_CONFIGURED_MODEL = "configured_model"
        private const val KEY_API_KEY = "api_key"

        // Default Private Tailscale Endpoint
        const val DEFAULT_SERVER_URL = "https://fahad.tail2abf56.ts.net"
        const val DEFAULT_MODEL = "qwen2.5-3b-instruct"
        const val DEFAULT_API_KEY = "1de55054c8c3f472f7dd5c39ee2bc78800d2d294484721d8"

        const val TAILSCALE_IP = "100.124.180.67"
        const val TAILSCALE_HOST = "fahad.tail2abf56.ts.net"

        fun sanitizeUrl(input: String): String {
            var s = input.trim()
            if (s.isBlank()) return DEFAULT_SERVER_URL
            if (!s.startsWith("http://") && !s.startsWith("https://")) {
                s = "https://$s"
            }
            val suffixesToRemove = listOf(
                "/v1/models",
                "/v1/chat/completions",
                "/v1/completions",
                "/v1",
                "/health",
                "/api/models",
                "/api"
            )
            for (suffix in suffixesToRemove) {
                if (s.endsWith(suffix, ignoreCase = true)) {
                    s = s.substring(0, s.length - suffix.length)
                }
            }
            return s.trimEnd('/')
        }
    }

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sanitizeUrl(prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL)
        set(value) {
            val cleaned = sanitizeUrl(value)
            prefs.edit().putString(KEY_SERVER_URL, cleaned).apply()
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

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
        }

    /**
     * Executes an HTTP request to the Tailscale AI server using direct IP connection and SNI handshake.
     */
    private fun executeTailscaleRequest(
        path: String,
        method: String = "GET",
        bodyJson: String? = null,
        timeoutMs: Int = 4000
    ): String {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAll, SecureRandom())

        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(TAILSCALE_IP, 443), timeoutMs)
        rawSocket.soTimeout = timeoutMs

        val sslSocket = sslContext.socketFactory.createSocket(
            rawSocket,
            TAILSCALE_HOST,
            443,
            true
        ) as SSLSocket

        val params = sslSocket.sslParameters
        params.serverNames = listOf(SNIHostName(TAILSCALE_HOST))
        sslSocket.sslParameters = params
        sslSocket.startHandshake()

        val writer = OutputStreamWriter(sslSocket.outputStream, "UTF-8")
        val reader = BufferedReader(InputStreamReader(sslSocket.inputStream, "UTF-8"))

        val reqBuilder = StringBuilder()
        reqBuilder.append("$method $path HTTP/1.1\r\n")
        reqBuilder.append("Host: $TAILSCALE_HOST\r\n")
        reqBuilder.append("User-Agent: ShahbuddinDairy-Android/11.0\r\n")
        reqBuilder.append("Accept: application/json\r\n")
        reqBuilder.append("Accept-Encoding: identity\r\n")
        reqBuilder.append("Connection: close\r\n")
        if (apiKey.isNotBlank()) {
            reqBuilder.append("Authorization: Bearer $apiKey\r\n")
        }

        if (bodyJson != null) {
            val bytes = bodyJson.toByteArray(Charsets.UTF_8)
            reqBuilder.append("Content-Type: application/json; charset=UTF-8\r\n")
            reqBuilder.append("Content-Length: ${bytes.size}\r\n")
            reqBuilder.append("\r\n")
            reqBuilder.append(bodyJson)
        } else {
            reqBuilder.append("\r\n")
        }

        writer.write(reqBuilder.toString())
        writer.flush()

        val fullResponse = reader.readText()
        sslSocket.close()

        val headerEnd = fullResponse.indexOf("\r\n\r\n")
        if (headerEnd < 0) return fullResponse

        val headers = fullResponse.substring(0, headerEnd)
        val body = fullResponse.substring(headerEnd + 4)

        // Check HTTP response status
        val firstLine = headers.lines().firstOrNull() ?: ""
        if (!firstLine.contains("200") && !firstLine.contains("201")) {
            throw Exception("HTTP Error: $firstLine")
        }

        return if (headers.contains("transfer-encoding: chunked", ignoreCase = true)) {
            dechunk(body)
        } else {
            body
        }
    }

    private fun dechunk(rawBody: String): String {
        return try {
            val reader = BufferedReader(StringReader(rawBody))
            val sb = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                val chunkSize = line.trim().toIntOrNull(16) ?: break
                if (chunkSize == 0) break
                val chunk = CharArray(chunkSize)
                var readTotal = 0
                while (readTotal < chunkSize) {
                    val r = reader.read(chunk, readTotal, chunkSize - readTotal)
                    if (r < 0) break
                    readTotal += r
                }
                sb.append(chunk, 0, readTotal)
                reader.readLine() // consume trailing \r\n
            }
            sb.toString()
        } catch (e: Exception) {
            rawBody
        }
    }

    /**
     * Executes request dispatching to Tailscale direct SNI or standard HTTP.
     */
    private fun executeRequest(
        path: String,
        method: String = "GET",
        bodyJson: String? = null,
        timeoutMs: Int = 4000
    ): String {
        val cleanBase = sanitizeUrl(serverUrl)
        val parsed = URL("$cleanBase$path")
        val host = parsed.host ?: ""

        return if (host.equals(TAILSCALE_HOST, ignoreCase = true) || host == TAILSCALE_IP) {
            executeTailscaleRequest(path, method, bodyJson, timeoutMs)
        } else {
            val conn = parsed.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = method
            conn.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            if (bodyJson != null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, "UTF-8").use {
                    it.write(bodyJson)
                    it.flush()
                }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                throw Exception("HTTP $code")
            }
            val res = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            conn.disconnect()
            res
        }
    }

    /**
     * Checks if the private AI server is reachable and reports health.
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        if (!isAiEnabled) return@withContext false
        try {
            val res = executeRequest("/health", "GET", timeoutMs = 3000)
            res.contains("ok") || res.contains("status")
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
            executeRequest("/health", "GET", timeoutMs = 2500)
            System.currentTimeMillis() - start
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
            executeRequest("/health", "GET", timeoutMs = 3500)
            val latency = System.currentTimeMillis() - start
            val detectedModel = fetchLoadedModel() ?: configuredModel
            configuredModel = detectedModel
            ConnectionTestResult(
                isConnected = true,
                latencyMs = latency,
                modelName = detectedModel,
                message = "कनेक्टेड (पिंक: ${latency}ms)"
            )
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
            val text = executeRequest("/v1/models", "GET", timeoutMs = 3500)
            val json = gson.fromJson(text, JsonObject::class.java)
            val data = json.getAsJsonArray("data")
            if (data != null && !data.isEmpty) {
                val first = data.get(0).asJsonObject
                first.get("id")?.asString
            } else if (json.has("models")) {
                val models = json.getAsJsonArray("models")
                if (models != null && !models.isEmpty) {
                    models.get(0).asJsonObject.get("name")?.asString
                } else null
            } else null
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
                Respond with a valid JSON object in this exact schema:
                {
                  "name": "Customer name or null",
                  "liters": 0.0 or null,
                  "cash_paid": 0.0 or null,
                  "is_naga": true or false
                }
                Output ONLY the JSON object.
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 350)

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

            val responseText = executeRequest(
                path = "/v1/chat/completions",
                method = "POST",
                bodyJson = requestBody.toString(),
                timeoutMs = 15000
            )

            val parsedJson = extractJsonFromResponse(responseText)

            if (parsedJson != null) {
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
                    serverModel = configuredModel,
                    rawServerResponse = responseText
                )
            } else {
                val localParsed = DairyAiMunshiEngine.parseInput(trimmed, customers)
                AiParseResult.Success(
                    command = localParsed.copy(
                        explanationHindi = "✨ AI (Debian 13): " + localParsed.explanationHindi
                    ),
                    serverModel = configuredModel,
                    rawServerResponse = responseText
                )
            }
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
                You are a multimodal dairy accounting assistant for Shahbuddin Dairy.
                Inspect the handwritten chit, milk slip, or receipt card.
                Extract transaction details into JSON:
                {
                  "name": "Customer or supplier name or null",
                  "liters": 0.0 or null,
                  "cash_paid": 0.0 or null
                }
                Output ONLY JSON.
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 400)

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

            val responseText = executeRequest(
                path = "/v1/chat/completions",
                method = "POST",
                bodyJson = requestBody.toString(),
                timeoutMs = 25000
            )

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

            AiParseResult.Success(command = command, serverModel = configuredModel, rawServerResponse = responseText)
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

            var content = if (message.has("content") && !message.get("content").isJsonNull) {
                message.get("content").asString
            } else ""

            if (content.isBlank() && message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull) {
                content = message.get("reasoning_content").asString
            }

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

    /**
     * Formats spoken or typed delivery note into a crisp 1-liner pinned note.
     */
    suspend fun formatDeliveryNote(rawInput: String): String = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return@withContext ""
        if (!isAiEnabled) return@withContext trimmed

        try {
            val systemPrompt = """
                You are a delivery note formatter for an Indian dairy delivery runner.
                Summarize the spoken or typed instruction into a concise 4-7 word delivery note in Hindi/Hinglish.
                Examples:
                - 'gate band rehta hai 2nd floor pe ghanti bajana' -> '2nd floor par ghanti bajayein (Gate band)'
                - 'kal subah 1 liter extra dena' -> 'Kal se 1L extra dena'
                - 'aaj mat aana naga hai' -> 'Aaj naga (Mana kiya)'
                Output ONLY the short note. Do not add quotes or explanations.
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 80)

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

            val responseText = executeRequest(
                path = "/v1/chat/completions",
                method = "POST",
                bodyJson = requestBody.toString(),
                timeoutMs = 7000
            )

            val root = gson.fromJson(responseText, JsonObject::class.java)
            val choices = root.getAsJsonArray("choices")
            if (choices != null && !choices.isEmpty) {
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                var content = message.get("content")?.asString?.trim() ?: ""
                if (content.isBlank() && message.has("reasoning_content")) {
                    content = message.get("reasoning_content")?.asString?.trim() ?: ""
                }
                val cleaned = content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: trimmed
                cleaned.removePrefix("\"").removeSuffix("\"").trim()
            } else {
                trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    /**
     * Normalizes messy customer names, nicknames, and mohallas using Debian AI.
     */
    suspend fun cleanCustomerIdentity(rawName: String, rawMohalla: String): Triple<String, String, String> = withContext(Dispatchers.IO) {
        val defName = rawName.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
        val defMohalla = rawMohalla.trim()
        if (!isAiEnabled) return@withContext Triple(defName, "", defMohalla)

        try {
            val systemPrompt = """
                You are an Indian customer name normalizer for a dairy khata app.
                Separate messy names, nicknames, and mohallas into JSON:
                {
                  "clean_name": "Proper person name without emojis",
                  "nickname": "Profession, urf, or alias if present, else empty",
                  "clean_mohalla": "Corrected colony or area name"
                }
                Output ONLY JSON.
            """.trimIndent()

            val requestBody = JsonObject().apply {
                addProperty("model", configuredModel)
                addProperty("temperature", 0.1)
                addProperty("max_tokens", 150)

                val messages = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", "Name: $rawName, Mohalla: $rawMohalla")
                    })
                }
                add("messages", messages)
            }

            val responseText = executeRequest(
                path = "/v1/chat/completions",
                method = "POST",
                bodyJson = requestBody.toString(),
                timeoutMs = 7000
            )

            val parsed = extractJsonFromResponse(responseText)
            if (parsed != null) {
                val cName = parsed.get("clean_name")?.asString?.trim() ?: defName
                val nick = parsed.get("nickname")?.asString?.trim() ?: ""
                val cMohalla = parsed.get("clean_mohalla")?.asString?.trim() ?: defMohalla
                Triple(cName, nick, cMohalla)
            } else {
                Triple(defName, "", defMohalla)
            }
        } catch (e: Exception) {
            Triple(defName, "", defMohalla)
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
