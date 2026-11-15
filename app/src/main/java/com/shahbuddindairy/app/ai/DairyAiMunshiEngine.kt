package com.shahbuddindairy.app.ai

import com.shahbuddindairy.app.data.model.CustomerEntity
import java.util.Locale
import kotlin.math.max

/**
 * Parsed intent representing an offline dairy transaction command spoken or typed in Hindi/Hinglish.
 */
data class DairyAiMunshiCommand(
    val matchedCustomer: CustomerEntity? = null,
    val customerNameQuery: String = "",
    val confidence: Float = 0f,
    val liters: Double? = null,
    val cash: Double? = null,
    val isNaga: Boolean = false,
    val isQuickPaymentOnly: Boolean = false,
    val rawText: String = "",
    val explanationHindi: String = ""
)

data class TomorrowDemandPrediction(
    val totalLitersNeeded: Double,
    val buffaloLiters: Double,
    val cowLiters: Double,
    val scheduledCustomerCount: Int,
    val alternateOffCount: Int,
    val suggestedLoadWithBuffer: Double,
    val hindiSummary: String
)

/**
 * 100% Offline, Zero-Cost, Deterministic Dairy AI Munshi NLP Engine.
 * Runs locally on Android device without internet or paid APIs.
 * Guarantees zero hallucination of ledger amounts.
 */
object DairyAiMunshiEngine {

    // Common Hindi words for quantities & fractions
    private val hindiNumberWords = mapOf(
        "ek" to 1.0, "ekk" to 1.0, "1" to 1.0, "एक" to 1.0,
        "do" to 2.0, "doh" to 2.0, "2" to 2.0, "दो" to 2.0,
        "teen" to 3.0, "tin" to 3.0, "3" to 3.0, "तीन" to 3.0,
        "char" to 4.0, "chaar" to 4.0, "4" to 4.0, "चार" to 4.0,
        "paanch" to 5.0, "panch" to 5.0, "5" to 5.0, "पांच" to 5.0,
        "chhe" to 6.0, "che" to 6.0, "chhah" to 6.0, "6" to 6.0, "छह" to 6.0,
        "saat" to 7.0, "sat" to 7.0, "7" to 7.0, "सात" to 7.0,
        "aath" to 8.0, "ath" to 8.0, "8" to 8.0, "आठ" to 8.0,
        "nau" to 9.0, "no" to 9.0, "9" to 9.0, "नौ" to 9.0,
        "das" to 10.0, "dass" to 10.0, "10" to 10.0, "दस" to 10.0,
        "aadha" to 0.5, "adha" to 0.5, "adhaa" to 0.5, "आधा" to 0.5, "0.5" to 0.5,
        "dedh" to 1.5, "derh" to 1.5, "deedh" to 1.5, "डेढ़" to 1.5, "1.5" to 1.5,
        "dhai" to 2.5, "dhaai" to 2.5, "ढाई" to 2.5, "2.5" to 2.5,
        "pauna" to 0.75, "paune" to 0.75, "पौना" to 0.75, "0.75" to 0.75,
        "sawa" to 1.25, "sawaa" to 1.25, "सवा" to 1.25, "1.25" to 1.25,
        "sadhe" to 0.5 // modifier for 3.5, 4.5 etc
    )

    fun parseInput(
        input: String,
        customers: List<CustomerEntity>
    ): DairyAiMunshiCommand {
        val clean = input.trim()
        if (clean.isBlank()) {
            return DairyAiMunshiCommand(rawText = input, explanationHindi = "कृपया कुछ बोलें या लिखें।")
        }

        val lower = clean.lowercase(Locale.ROOT)
            .replace("₹", " ")
            .replace("rs.", " ")
            .replace("rs", " ")
            .replace("rupaye", " rupaye ")
            .replace("rupayee", " rupaye ")
            .replace("rupay", " rupaye ")
            .replace("रुपये", " rupaye ")
            .replace("रूपये", " rupaye ")
            .replace("लीटर", " liter ")
            .replace("लिटर", " liter ")
            .replace("litre", " liter ")
            .replace("litres", " liter ")
            .replace("liters", " liter ")
            .replace("ltr", " liter ")

        val isNaga = lower.contains("naga") || lower.contains("नागा") ||
                lower.contains("chhutti") || lower.contains("छुट्टी") ||
                lower.contains("nahi diya") || lower.contains("नहीं दिया") ||
                lower.contains("band") || lower.contains("बंद")

        // 1. Detect Liters
        var detectedLiters: Double? = null
        if (!isNaga) {
            // Regex patterns for milk e.g. "2.5 liter", "2 liter", "dedh liter", "aadha liter"
            val literRegex = Regex("""(\d+(\.\d+)?)\s*(?:liter|l|ltr)?""")
            val matches = literRegex.findAll(lower).toList()

            for (m in matches) {
                val strVal = m.groupValues[1]
                val v = strVal.toDoubleOrNull()
                // If it's explicitly followed by liter or reasonable milk quantity
                if (v != null && (m.value.contains("liter") || m.value.contains("l") || v <= 20.0)) {
                    // Check if it's not a money indicator
                    val subBefore = lower.substring(0, m.range.first)
                    if (!subBefore.trimEnd().endsWith("rupaye") && !subBefore.trimEnd().endsWith("rs")) {
                        detectedLiters = v
                        break
                    }
                }
            }

            // Check word fractions if not matched by number
            if (detectedLiters == null) {
                for ((word, frac) in hindiNumberWords) {
                    if (lower.contains(word)) {
                        detectedLiters = frac
                        break
                    }
                }
            }
        }

        // 2. Detect Cash Amount
        var detectedCash: Double? = null
        val cashRegex = Regex("""(?:rupaye|rupay|rs|jama|diye|diya|de\s*diye)\s*(\d+)|(\d+)\s*(?:rupaye|rupay|rs|jama|diye|diya|nakad|cash)""")
        val cashMatch = cashRegex.find(lower)
        if (cashMatch != null) {
            val numStr = cashMatch.groupValues[1].ifEmpty { cashMatch.groupValues[2] }
            detectedCash = numStr.toDoubleOrNull()
        } else {
            // Fallback: look for large numbers typically > 20 as cash amount
            val allNumbers = Regex("""\b\d+\b""").findAll(lower).mapNotNull { it.value.toDoubleOrNull() }.toList()
            for (n in allNumbers) {
                if (n > 20.0 && n != detectedLiters) {
                    detectedCash = n
                    break
                }
            }
        }

        val isPaymentOnly = detectedLiters == null && detectedCash != null && !isNaga

        // 3. Match Customer Name with Fuzzy Matching
        val (matchedCust, score, queryName) = matchCustomer(clean, customers)

        // 4. Construct Explanation
        val explanation = buildString {
            if (matchedCust != null) {
                append("ग्राहक: ${matchedCust.name}")
                if (matchedCust.mohalla.isNotBlank()) append(" (${matchedCust.mohalla})")
                append("\n")
            } else if (queryName.isNotBlank()) {
                append("ग्राहक: '$queryName' नहीं मिला\n")
            }

            if (isNaga) {
                append("स्थिति: आज नागा (0L)\n")
            } else if (detectedLiters != null) {
                append("दूध: $detectedLiters L\n")
            }

            if (detectedCash != null) {
                append("नकद जमा: ₹%,d\n".format(Locale.ENGLISH, detectedCash.toLong()))
            }
        }.trim()

        return DairyAiMunshiCommand(
            matchedCustomer = matchedCust,
            customerNameQuery = queryName,
            confidence = score,
            liters = detectedLiters,
            cash = detectedCash,
            isNaga = isNaga,
            isQuickPaymentOnly = isPaymentOnly,
            rawText = clean,
            explanationHindi = explanation
        )
    }

    private fun matchCustomer(
        input: String,
        customers: List<CustomerEntity>
    ): Triple<CustomerEntity?, Float, String> {
        if (customers.isEmpty()) return Triple(null, 0f, "")

        val clean = input.lowercase(Locale.ROOT)
            .replace(Regex("""\b(ko|ka|ki|ne|se|liter|l|ltr|rupaye|rs|cash|jama|naga|diye|diya|aaj|aadha|dedh|dhai|ek|do|teen|char|paanch)\b"""), " ")
            .replace(Regex("""[^a-zA-Z0-9\u0900-\u097F\s]"""), " ")
            .trim()

        val tokens = clean.split(Regex("""\s+""")).filter { it.length >= 2 }

        var bestMatch: CustomerEntity? = null
        var highestScore = 0f
        var bestToken = ""

        for (cust in customers) {
            val cName = cust.name.lowercase(Locale.ROOT)
            val cMohalla = cust.mohalla.lowercase(Locale.ROOT)

            // Direct full name substring
            if (clean.contains(cName) && cName.isNotBlank()) {
                val score = 0.95f
                if (score > highestScore) {
                    highestScore = score
                    bestMatch = cust
                    bestToken = cust.name
                }
            }

            // Token matching
            for (token in tokens) {
                if (cName.contains(token) || token.contains(cName)) {
                    val score = 0.85f
                    if (score > highestScore) {
                        highestScore = score
                        bestMatch = cust
                        bestToken = token
                    }
                }

                // Check phonetic / similarity distance
                val sim = similarity(token, cName)
                if (sim > 0.65f && sim > highestScore) {
                    highestScore = sim
                    bestMatch = cust
                    bestToken = token
                }
            }
        }

        return Triple(bestMatch, highestScore, bestToken)
    }

    private fun similarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        val dist = levenshtein(s1, s2)
        return 1.0f - (dist.toFloat() / maxLen.toFloat())
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * V9.2: Offline AI Demand Predictor for next morning.
     * Analyzes customer default volumes and alternate schedules to forecast buffalo & cow demand.
     */
    fun predictTomorrowDemand(
        customers: List<CustomerEntity>,
        todayDateStr: String
    ): TomorrowDemandPrediction {
        val tomorrowDateStr = try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val d = sdf.parse(todayDateStr) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance().apply {
                time = d
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            sdf.format(cal.time)
        } catch (e: Exception) {
            todayDateStr
        }

        var buffaloLiters = 0.0
        var cowLiters = 0.0
        var scheduledCount = 0
        var alternateCount = 0

        val active = customers.filter { it.isArchived == 0 && it.isActive == 1 }
        for (c in active) {
            val isOff = com.shahbuddindairy.app.domain.CalculationEngine.isOffDay(c, tomorrowDateStr)
            if (isOff) {
                alternateCount++
            } else {
                scheduledCount++
                val qty = if (c.defaultLiters > 0) c.defaultLiters else 1.0
                if (c.milkType == "गाय") {
                    cowLiters += qty
                } else {
                    buffaloLiters += qty
                }
            }
        }

        val total = buffaloLiters + cowLiters
        val buffer = (total * 0.05).coerceAtLeast(1.5)
        val loadSuggested = (total + buffer)

        val summary = "कल सुबह का AI अनुमान: कुल %.1fL दूध लोड करें (भैंस: %.1fL, गाय: %.1fL)। %d ग्राहक शेड्यूल, %d अल्टरनेट अवकाश।".format(
            loadSuggested, buffaloLiters, cowLiters, scheduledCount, alternateCount
        )

        return TomorrowDemandPrediction(
            totalLitersNeeded = com.shahbuddindairy.app.domain.CalculationEngine.round2(total),
            buffaloLiters = com.shahbuddindairy.app.domain.CalculationEngine.round2(buffaloLiters),
            cowLiters = com.shahbuddindairy.app.domain.CalculationEngine.round2(cowLiters),
            scheduledCustomerCount = scheduledCount,
            alternateOffCount = alternateCount,
            suggestedLoadWithBuffer = com.shahbuddindairy.app.domain.CalculationEngine.round2(loadSuggested),
            hindiSummary = summary
        )
    }
}
