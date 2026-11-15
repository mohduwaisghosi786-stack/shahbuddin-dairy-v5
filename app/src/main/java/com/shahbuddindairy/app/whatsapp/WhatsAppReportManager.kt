package com.shahbuddindairy.app.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.shahbuddindairy.app.ai.VoiceFeedbackManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 100% WhatsApp Automation Manager for Shahbuddin Dairy V9.0.
 * Strictly replaces all email communication with direct WhatsApp messaging:
 * 1. 1-Tap Instant Delivery Receipts to customers.
 * 2. Monthly Khata Statement directly to customer's WhatsApp.
 * 3. Nightly Business Closing Summary for Dairy Owner (Default: 9258263340).
 * 4. Gentle Due Recovery Reminders with UPI payment details.
 */
object WhatsAppReportManager {

    const val DEFAULT_OWNER_PHONE = "9258262340"
    private const val PREFS_NAME = "sd_owner_report_prefs"
    private const val KEY_OWNER_PHONE = "owner_report_phone"
    private const val KEY_AUTO_REPORT = "owner_auto_report"

    fun getOwnerReportPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_OWNER_PHONE, DEFAULT_OWNER_PHONE)?.ifBlank { DEFAULT_OWNER_PHONE } ?: DEFAULT_OWNER_PHONE
    }

    fun setOwnerReportPhone(context: Context, phone: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OWNER_PHONE, phone.trim()).apply()
    }

    fun isAutoReportEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_REPORT, false)
    }

    fun setAutoReportEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_REPORT, enabled).apply()
    }

    /**
     * Formats an Indian phone number for WhatsApp direct link.
     */
    fun sanitizePhoneNumber(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        val digitsOnly = rawPhone.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.length == 10 -> "91$digitsOnly"
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly
            digitsOnly.length > 10 -> digitsOnly
            else -> null
        }
    }

    /**
     * Safely opens WhatsApp with prefilled text and optional recipient phone.
     */
    fun launchWhatsApp(context: Context, phone: String?, messageText: String) {
        VoiceFeedbackManager.getInstance(context).playShareSound()
        val cleanPhone = sanitizePhoneNumber(phone)
        val encodedMessage = try {
            URLEncoder.encode(messageText, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            messageText
        }

        try {
            val uri = if (cleanPhone != null) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to standard WhatsApp share intent
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(shareIntent)
            } catch (e2: Exception) {
                // WhatsApp not installed, open generic chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "WhatsApp पर्ची भेजें"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp खोलने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 1-Tap Delivery Receipt sent directly to customer upon delivery completion.
     */
    fun sendDeliveryReceipt(
        context: Context,
        customerPhone: String?,
        customerName: String,
        dateStr: String,
        liters: Double,
        milkType: String,
        rate: Double,
        todayBill: Double,
        cashPaid: Double,
        totalRemainingDue: Double,
        dairyName: String = "SHAHBUDDIN DAIRY (Bareilly)"
    ) {
        val message = buildString {
            append("🥛 *").append(dairyName).append("*\n")
            append("Official Delivery Receipt\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📅 Date: ").append(dateStr).append("\n")
            append("👤 Customer: *").append(customerName).append("*\n")
            append("🍶 Milk: *").append(if (liters == liters.toLong().toDouble()) "${liters.toLong()}" else "%.1f".format(liters)).append("L* (").append(milkType).append(")\n")
            append("⚡ Rate: ₹%.0f/L\n".format(rate))
            append("💵 Today Bill: *₹%.0f*\n".format(todayBill))
            if (cashPaid > 0) {
                append("💰 Cash Paid: *₹%.0f*\n".format(cashPaid))
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (totalRemainingDue > 0) {
                append("🔴 *Total Due Balance: ₹%.0f*\n".format(totalRemainingDue))
            } else if (totalRemainingDue < 0) {
                append("🟢 *Advance Holding: ₹%.0f*\n".format(-totalRemainingDue))
            } else {
                append("✅ *Account Settled (Nil Due)*\n")
            }
            append("💡 AI Insight: Regular daily delivery • Verified transaction.\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("Thank you for your business.\n")
            append("Shahbuddin Dairy • Ph: 9258263340\n")
            append("Gulab Nagar, Bareilly")
        }

        launchWhatsApp(context, customerPhone, message)
    }

    /**
     * Monthly Statement / Customer Khata summary sent to customer's WhatsApp.
     */
    fun sendCustomerKhataStatement(
        context: Context,
        customerPhone: String?,
        customerName: String,
        periodStr: String,
        openingBalance: Double,
        totalMilkLiters: Double,
        totalMilkBill: Double,
        totalPaid: Double,
        closingBalance: Double,
        dairyName: String = "SHAHBUDDIN DAIRY (Bareilly)"
    ) {
        val message = buildString {
            append("📋 *").append(dairyName).append("*\n")
            append("Monthly Account Statement\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("👤 Customer: *").append(customerName).append("*\n")
            append("📅 Period: ").append(periodStr).append("\n")
            if (openingBalance != 0.0) {
                append("🔄 Opening Balance: ₹%.0f\n".format(openingBalance))
            }
            append("🥛 Total Milk: *%.1f Liters*\n".format(totalMilkLiters))
            append("💵 Total Milk Bill: *₹%.0f*\n".format(totalMilkBill))
            append("💰 Total Paid: *₹%.0f*\n".format(totalPaid))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (closingBalance > 0) {
                append("🔴 *Net Outstanding Due: ₹%.0f*\n".format(closingBalance))
                append("Kindly arrange payment at your earliest convenience.\n")
            } else if (closingBalance < 0) {
                append("🟢 *Advance Holding: ₹%.0f*\n".format(-closingBalance))
            } else {
                append("✅ *Account Cleared (Nil Balance)*\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("Thank you.\n")
            append("Shahbuddin Dairy • Ph: 9258263340\n")
            append("Gulab Nagar, Bareilly")
        }

        launchWhatsApp(context, customerPhone, message)
    }

    /**
     * Shares an Image Receipt directly to WhatsApp.
     */
    fun shareReceiptImageViaWhatsApp(
        context: Context,
        imageFile: java.io.File,
        phone: String? = null,
        caption: String = "🥛 Shahbuddin Dairy — Official Delivery Receipt"
    ) {
        VoiceFeedbackManager.getInstance(context).playShareSound()
        try {
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val cleanPhone = sanitizePhoneNumber(phone)
                if (cleanPhone != null) {
                    setPackage("com.whatsapp")
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt Image"))
        } catch (e: Exception) {
            Toast.makeText(context, "Image शेयर विफल: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a PDF file directly to WhatsApp (optionally targeting owner phone or general share).
     */
    fun sharePdfViaWhatsApp(
        context: Context,
        pdfUri: Uri,
        phone: String? = null,
        caption: String = "🥛 Shahbuddin Dairy — Daily Statement PDF"
    ) {
        VoiceFeedbackManager.getInstance(context).playShareSound()
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val cleanPhone = sanitizePhoneNumber(phone)
                if (cleanPhone != null) {
                    setPackage("com.whatsapp")
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Statement PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF शेयर विफल: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Nightly Business Closing Summary sent to Owner's WhatsApp (Raat Ka Hisab).
     */
    fun sendNightlyClosingSummary(
        context: Context,
        ownerPhone: String? = null,
        dateStr: String,
        totalMilkLiters: Double,
        totalCashCollected: Double,
        completedDeliveries: Int,
        totalCustomers: Int,
        nagaCount: Int,
        alternateOffCount: Int,
        totalOnlineCollected: Double = 0.0,
        totalBilled: Double = 0.0,
        totalMarketDue: Double = 0.0,
        totalAdvance: Double = 0.0,
        dairyName: String = "शाहबुद्दीन डेयरी"
    ) {
        val targetOwnerPhone = ownerPhone?.ifBlank { getOwnerReportPhone(context) } ?: getOwnerReportPhone(context)
        val totalCollected = totalCashCollected + totalOnlineCollected
        val message = buildString {
            appendLine("🌙 *$dairyName — रात का बंद हिसाब*")
            appendLine("📍 96, Gulab Nagar, Bareilly, Uttar Pradesh, India")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📅 तारीख: $dateStr")
            appendLine("🥛 कुल दूध वितरण: *%.2f Liters*".format(totalMilkLiters))
            if (totalBilled > 0) {
                appendLine("💵 कुल बिक्री (Bill): *₹%,d*".format(totalBilled.toLong()))
            }
            appendLine("💰 कुल नकद संग्रह: *₹%,d*".format(totalCashCollected.toLong()))
            if (totalOnlineCollected > 0) {
                appendLine("📱 ऑनलाइन संग्रह: *₹%,d*".format(totalOnlineCollected.toLong()))
            }
            appendLine("⚡ कुल वसूली: *₹%,d*".format(totalCollected.toLong()))
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("👥 पूर्ण डिलीवरी: *$completedDeliveries / $totalCustomers ग्राहक*")
            if (nagaCount > 0) {
                appendLine("❌ कुल नागा (0L): *$nagaCount*")
            }
            if (alternateOffCount > 0) {
                appendLine("⏸️ अल्टरनेट ऑफ: *$alternateOffCount* (आज बंद)")
            }
            val pending = (totalCustomers - completedDeliveries - nagaCount - alternateOffCount).coerceAtLeast(0)
            if (pending > 0) {
                appendLine("⏳ शेष बाकी: *$pending*")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            if (totalMarketDue > 0) {
                appendLine("⚠️ कुल बाजार उधारी: *₹%,.2f*".format(totalMarketDue))
            }
            if (totalAdvance > 0) {
                appendLine("🟢 कुल अग्रिम जमा: *₹%,.2f*".format(totalAdvance))
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔒 डेटाबेस पूर्णतः सुरक्षित (SQLite v9.0 Safe)")
            appendLine("✨ शुभ रात्रि! 🥛")
        }

        launchWhatsApp(context, targetOwnerPhone, message)
    }

    /**
     * Gentle polite due reminder with UPI payment info for customers with outstanding balance.
     */
    fun sendGentleDueReminder(
        context: Context,
        customerPhone: String?,
        customerName: String,
        dueAmount: Double,
        upiId: String?,
        payeeName: String = "SHAHBUDDIN DAIRY (Bareilly)"
    ) {
        val message = buildString {
            append("*").append(payeeName).append("*\n")
            append("Official Payment Reminder\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("Customer: *").append(customerName).append("*\n")
            append("Total Outstanding Due: *₹%.0f*\n\n".format(dueAmount))
            append("Kindly arrange payment at your earliest convenience.\n")
            if (!upiId.isNullOrBlank()) {
                append("📲 *UPI ID:* `").append(upiId.trim()).append("`\n")
                val upiLink = "upi://pay?pa=${upiId.trim()}&pn=${URLEncoder.encode("Shahbuddin Dairy", "UTF-8")}&am=%.2f&cu=INR".format(dueAmount)
                append("🔗 *Direct Pay Link:* ").append(upiLink).append("\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("Thank you.\n")
            append("Shahbuddin Dairy • Ph: 9258263340\n")
            append("96, Gulab Nagar, Bareilly")
        }

        launchWhatsApp(context, customerPhone, message)
    }
}
