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
 * 100% WhatsApp Automation Manager for Shahbuddin Dairy V8.5.
 * Strictly replaces all email communication with direct WhatsApp messaging:
 * 1. 1-Tap Instant Delivery Receipts to customers.
 * 2. Monthly Khata Statement directly to customer's WhatsApp.
 * 3. Nightly Business Closing Summary for Dairy Owner (Default: 9258263340).
 * 4. Gentle Due Recovery Reminders with UPI payment details.
 */
object WhatsAppReportManager {

    const val DEFAULT_OWNER_PHONE = "9258263340"
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
        dairyName: String = "शाहबुद्दीन डेयरी"
    ) {
        val message = buildString {
            append("🥛 *").append(dairyName).append("*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📅 दिनांक: ").append(dateStr).append("\n")
            append("👤 ग्राहक: *").append(customerName).append("*\n")
            append("🍶 दूध: *").append(if (liters == liters.toLong().toDouble()) "${liters.toLong()}" else "%.1f".format(liters)).append("L* (").append(milkType).append(")\n")
            append("⚡ दर (Rate): ₹%.0f/L\n".format(rate))
            append("💵 आज का बिल: *₹%.0f*\n".format(todayBill))
            if (cashPaid > 0) {
                append("💰 नकद प्राप्त: *₹%.0f*\n".format(cashPaid))
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (totalRemainingDue > 0) {
                append("📊 *कुल बकाया राशि: ₹%.0f*\n".format(totalRemainingDue))
            } else if (totalRemainingDue < 0) {
                append("🎁 *जमा एडवांस: ₹%.0f*\n".format(-totalRemainingDue))
            } else {
                append("✅ *हिसाब बराबर (No Due)*\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("धन्यवाद! आपका दिन शुभ हो। 🙏")
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
        dairyName: String = "शाहबुद्दीन डेयरी"
    ) {
        val message = buildString {
            append("📋 *").append(dairyName).append(" — खाता विवरण*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("👤 ग्राहक: *").append(customerName).append("*\n")
            append("📅 अवधि: ").append(periodStr).append("\n")
            if (openingBalance != 0.0) {
                append("🔄 पुराना बकाया: ₹%.0f\n".format(openingBalance))
            }
            append("🥛 कुल दूध: *%.1f Liters*\n".format(totalMilkLiters))
            append("💵 कुल दूध बिल: *₹%.0f*\n".format(totalMilkBill))
            append("💰 कुल जमा (भुगतान): *₹%.0f*\n".format(totalPaid))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (closingBalance > 0) {
                append("🚨 *अंतिम देय बकाया: ₹%.0f*\n".format(closingBalance))
                append("कृपया बकाया राशि का भुगतान करें।\n")
            } else if (closingBalance < 0) {
                append("🎁 *अग्रिम जमा (Advance): ₹%.0f*\n".format(-closingBalance))
            } else {
                append("✅ *खाता पूर्णतः चुकता (Nil Balance)*\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("किसी भी प्रश्न के लिए संपर्क करें। धन्यवाद! 🙏")
        }

        launchWhatsApp(context, customerPhone, message)
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
        dairyName: String = "शाहबुद्दीन डेयरी"
    ) {
        val targetOwnerPhone = ownerPhone?.ifBlank { getOwnerReportPhone(context) } ?: getOwnerReportPhone(context)
        val message = buildString {
            append("🌙 *").append(dairyName).append(" — रात का बंद हिसाब*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📅 तारीख: ").append(dateStr).append("\n")
            append("🥛 कुल दूध वितरण: *%.1f Liters*\n".format(totalMilkLiters))
            append("💵 कुल नकद संग्रह: *₹%,d*\n".format(totalCashCollected.toLong()))
            append("👥 पूर्ण डिलीवरी: *").append(completedDeliveries).append(" / ").append(totalCustomers).append(" ग्राहक*\n")
            if (nagaCount > 0) {
                append("❌ कुल नागा (0L): *").append(nagaCount).append("*\n")
            }
            if (alternateOffCount > 0) {
                append("⏸️ आज बंद (Alternate Off): *").append(alternateOffCount).append("*\n")
            }
            val pending = (totalCustomers - completedDeliveries - nagaCount - alternateOffCount).coerceAtLeast(0)
            if (pending > 0) {
                append("⏳ शेष बाकी: *").append(pending).append("*\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🔒 डेटाबेस पूर्णतः सुरक्षित (SQLite v8.5 Safe)\n")
            append("✨ शुभ रात्रि! 🥛")
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
        payeeName: String = "शाहबुद्दीन डेयरी"
    ) {
        val message = buildString {
            append("🙏 *सादर प्रणाम ").append(customerName).append(" जी,*\n\n")
            append("यह ").append(payeeName).append(" की तरफ से एक विनम्र सूचना है।\n")
            append("आपकी वर्तमान कुल बकाया राशि *₹%.0f* है।\n\n".format(dueAmount))
            append("कृपया अपनी सुविधानुसार भुगतान करने का कष्ट करें।\n")
            if (!upiId.isNullOrBlank()) {
                append("📲 *UPI ID:* `").append(upiId.trim()).append("`\n")
                val upiLink = "upi://pay?pa=${upiId.trim()}&pn=${URLEncoder.encode(payeeName, "UTF-8")}&am=%.2f&cu=INR".format(dueAmount)
                append("🔗 *Direct Pay Link:* ").append(upiLink).append("\n")
            }
            append("\nधन्यवाद! 🙏")
        }

        launchWhatsApp(context, customerPhone, message)
    }
}
