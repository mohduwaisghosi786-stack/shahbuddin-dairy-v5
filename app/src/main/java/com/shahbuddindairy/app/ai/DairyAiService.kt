package com.shahbuddindairy.app.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Standard Supported AI Intents for Shahbuddin Dairy V9.0.
 */
enum class AiIntent {
    ADD_MILK_ENTRY,
    RECORD_PAYMENT,
    GET_DUE_REPORT,
    LIST_UNPAID_CUSTOMERS,
    FIX_CUSTOMER_NAME,
    SET_REMINDER,
    AMBIGUOUS_NEED_CLARIFICATION
}

/**
 * Structured AI Intent Result validated against the local SQLite database.
 * NOTE: All monetary and bill totals MUST be calculated in Kotlin via CalculationEngine,
 * NEVER by trusting LLM arithmetic.
 */
data class DairyAiIntentResult(
    val intent: AiIntent,
    val matchedCustomer: CustomerEntity? = null,
    val matchedCustomerId: Long? = null,
    val customerNameRaw: String = "",
    val quantityLiters: Double? = null,
    val ratePerLiter: Double? = null,
    val calculatedBill: Double? = null,
    val paymentAmount: Double? = null,
    val isNaga: Boolean = false,
    val timePeriod: String = "today",
    val requiresUserConfirmation: Boolean = true,
    val hindiReplyPreview: String = "",
    val clarificationQuestion: String? = null,
    val confidence: Float = 0.5f,
    val isFallbackLocal: Boolean = false
)

/**
 * High-Level Dedicated AI Service for Shahbuddin Dairy.
 * Encapsulates:
 * 1. Requesting Qwen2.5-3B-Instruct via Tailscale endpoint.
 * 2. Strict JSON schema extraction & parsing.
 * 3. Local CustomerEntity matching & validation.
 * 4. Deterministic financial calculations via CalculationEngine.
 * 5. Transparent fallback to local offline NLP if network/server is unavailable.
 */
class DairyAiService(private val context: Context) {

    companion object {
        private const val TAG = "DairyAiService"
    }

    private val apiClient = PrivateAiApiClient(context)
    private val gson = Gson()

    /**
     * Processes natural language spoken or typed Hindi/Hinglish instructions.
     */
    suspend fun processInstruction(
        input: String,
        customers: List<CustomerEntity>
    ): DairyAiIntentResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext DairyAiIntentResult(
                intent = AiIntent.AMBIGUOUS_NEED_CLARIFICATION,
                customerNameRaw = "",
                requiresUserConfirmation = false,
                hindiReplyPreview = "कृपया कुछ बोलें या लिखें।",
                clarificationQuestion = "कृपया अपनी बात बताएं।"
            )
        }

        // If AI is disabled or offline, use deterministic offline fallback
        if (!apiClient.isAiEnabled) {
            return@withContext parseFallbackLocal(trimmed, customers, "ऑफ़लाइन मोड")
        }

        try {
            val parseResult = apiClient.parseDairyInstruction(trimmed, customers)
            if (parseResult is AiParseResult.Success && parseResult.rawServerResponse != null) {
                val parsed = extractStructuredIntent(parseResult.rawServerResponse, customers)
                if (parsed != null) {
                    return@withContext parsed
                }
            }

            // Fallback to local heuristic parser
            parseFallbackLocal(trimmed, customers, "AI सर्वर से उत्तर पार्स नहीं हो सका")
        } catch (e: Exception) {
            Log.w(TAG, "DairyAiService error: ${e.message}")
            parseFallbackLocal(trimmed, customers, "सर्वर टाइमआउट (${e.localizedMessage ?: "timeout"})")
        }
    }

    private fun extractStructuredIntent(
        rawResponse: String,
        customers: List<CustomerEntity>
    ): DairyAiIntentResult? {
        try {
            val root = gson.fromJson(rawResponse, JsonObject::class.java)
            val choices = root.getAsJsonArray("choices") ?: return null
            if (choices.isEmpty) return null
            val message = choices[0].asJsonObject.getAsJsonObject("message") ?: return null

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

            val json = gson.fromJson(jsonText, JsonObject::class.java) ?: return null

            val rawIntentStr = if (json.has("intent") && !json.get("intent").isJsonNull) {
                json.get("intent").asString.uppercase(Locale.ROOT)
            } else "ADD_MILK_ENTRY"

            val intent = try {
                AiIntent.valueOf(rawIntentStr)
            } catch (e: Exception) {
                AiIntent.ADD_MILK_ENTRY
            }

            val customerNameRaw = if (json.has("customer_name_raw") && !json.get("customer_name_raw").isJsonNull) {
                json.get("customer_name_raw").asString.trim()
            } else if (json.has("name") && !json.get("name").isJsonNull) {
                json.get("name").asString.trim()
            } else ""

            val quantityLiters = if (json.has("quantity_liters") && !json.get("quantity_liters").isJsonNull) {
                json.get("quantity_liters").asDouble
            } else if (json.has("liters") && !json.get("liters").isJsonNull) {
                json.get("liters").asDouble
            } else null

            val paymentAmount = if (json.has("payment_amount") && !json.get("payment_amount").isJsonNull) {
                json.get("payment_amount").asDouble
            } else if (json.has("cash_paid") && !json.get("cash_paid").isJsonNull) {
                json.get("cash_paid").asDouble
            } else null

            val isNaga = if (json.has("is_naga") && !json.get("is_naga").isJsonNull) {
                json.get("is_naga").asBoolean
            } else false

            // Strict local customer matching & rate retrieval from SQLite
            val matchedCustomer = matchCustomer(customerNameRaw, customers)
            val effectiveRate = matchedCustomer?.ratePerLiter ?: if (json.has("rate_per_liter") && !json.get("rate_per_liter").isJsonNull) {
                json.get("rate_per_liter").asDouble
            } else 60.0

            // Mandatory local Kotlin math (Never trust LLM arithmetic for ledger calculations)
            val calculatedBill = if (quantityLiters != null && quantityLiters > 0.0 && !isNaga) {
                CalculationEngine.round2(quantityLiters * effectiveRate)
            } else if (isNaga) {
                0.0
            } else null

            val preview = if (json.has("hindi_reply_preview") && !json.get("hindi_reply_preview").isJsonNull) {
                json.get("hindi_reply_preview").asString
            } else {
                buildDefaultPreview(intent, matchedCustomer?.name ?: customerNameRaw, quantityLiters, effectiveRate, calculatedBill, paymentAmount, isNaga)
            }

            val clarif = if (json.has("clarification_question") && !json.get("clarification_question").isJsonNull) {
                json.get("clarification_question").asString
            } else null

            return DairyAiIntentResult(
                intent = intent,
                matchedCustomer = matchedCustomer,
                matchedCustomerId = matchedCustomer?.id,
                customerNameRaw = customerNameRaw,
                quantityLiters = if (isNaga) 0.0 else quantityLiters,
                ratePerLiter = effectiveRate,
                calculatedBill = calculatedBill,
                paymentAmount = paymentAmount,
                isNaga = isNaga,
                timePeriod = "today",
                requiresUserConfirmation = true,
                hindiReplyPreview = preview,
                clarificationQuestion = clarif,
                confidence = if (matchedCustomer != null) 0.95f else 0.65f,
                isFallbackLocal = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "extractStructuredIntent error: ${e.message}")
            return null
        }
    }

    private fun parseFallbackLocal(
        input: String,
        customers: List<CustomerEntity>,
        reason: String
    ): DairyAiIntentResult {
        val localCmd = DairyAiMunshiEngine.parseInput(input, customers)
        val customer = localCmd.matchedCustomer
        val rate = customer?.ratePerLiter ?: 60.0
        val bill = if (localCmd.isNaga) 0.0 else if (localCmd.liters != null) CalculationEngine.round2(localCmd.liters * rate) else null

        val intent = when {
            localCmd.isQuickPaymentOnly -> AiIntent.RECORD_PAYMENT
            localCmd.isNaga -> AiIntent.ADD_MILK_ENTRY
            localCmd.liters != null -> AiIntent.ADD_MILK_ENTRY
            else -> AiIntent.AMBIGUOUS_NEED_CLARIFICATION
        }

        return DairyAiIntentResult(
            intent = intent,
            matchedCustomer = customer,
            matchedCustomerId = customer?.id,
            customerNameRaw = localCmd.customerNameQuery,
            quantityLiters = if (localCmd.isNaga) 0.0 else localCmd.liters,
            ratePerLiter = rate,
            calculatedBill = bill,
            paymentAmount = localCmd.cash,
            isNaga = localCmd.isNaga,
            timePeriod = "today",
            requiresUserConfirmation = true,
            hindiReplyPreview = localCmd.explanationHindi + " ($reason)",
            clarificationQuestion = if (customer == null && localCmd.customerNameQuery.isNotBlank()) "ग्राहक की पुष्टि करें" else null,
            confidence = localCmd.confidence,
            isFallbackLocal = true
        )
    }

    private fun matchCustomer(rawName: String, customers: List<CustomerEntity>): CustomerEntity? {
        if (rawName.isBlank() || customers.isEmpty()) return null
        val clean = rawName.trim().lowercase(Locale.ROOT)
        // 1. Exact match
        customers.firstOrNull { it.name.trim().lowercase(Locale.ROOT) == clean }?.let { return it }
        // 2. Substring match
        customers.firstOrNull { it.name.trim().lowercase(Locale.ROOT).contains(clean) }?.let { return it }
        customers.firstOrNull { clean.contains(it.name.trim().lowercase(Locale.ROOT)) }?.let { return it }
        // 3. Normalized alphanumeric match
        val cleanAlpha = clean.replace(Regex("[^a-z0-9\u0900-\u097F]"), "")
        if (cleanAlpha.isNotBlank()) {
            customers.firstOrNull {
                it.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9\u0900-\u097F]"), "").contains(cleanAlpha)
            }?.let { return it }
        }
        return null
    }

    private fun buildDefaultPreview(
        intent: AiIntent,
        customerName: String,
        liters: Double?,
        rate: Double,
        bill: Double?,
        cash: Double?,
        isNaga: Boolean
    ): String {
        return when (intent) {
            AiIntent.ADD_MILK_ENTRY -> {
                if (isNaga) {
                    "$customerName: आज का नागा (0L) दर्ज करें?"
                } else {
                    "$customerName: ${liters ?: 0.0}L दूध (@ ₹$rate/L, बिल ₹${bill ?: 0.0}) दर्ज करें?"
                }
            }
            AiIntent.RECORD_PAYMENT -> {
                "$customerName: ₹${cash ?: 0.0} नकद भुगतान दर्ज करें?"
            }
            AiIntent.GET_DUE_REPORT -> {
                "$customerName का बकाया हिसाब देखें?"
            }
            AiIntent.LIST_UNPAID_CUSTOMERS -> {
                "बाकी उधारी वाले सभी ग्राहकों की सूची निकालें?"
            }
            AiIntent.FIX_CUSTOMER_NAME -> {
                "$customerName का नाम सुधारें?"
            }
            AiIntent.SET_REMINDER -> {
                "$customerName के लिए रिमाइंडर सेट करें?"
            }
            AiIntent.AMBIGUOUS_NEED_CLARIFICATION -> {
                "कमांड स्पष्ट नहीं है, कृपया दोबारा बताएं।"
            }
        }
    }
}
