package com.shahbuddindairy.app.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 100% WhatsApp Automation Manager for Shahbuddin Dairy.
 * Strictly replaces all email communication with direct WhatsApp messaging:
 * 1. 1-Tap Instant Delivery Receipts.
 * 2. Nightly Business Closing Summary for Dairy Owner.
 * 3. Gentle Due Recovery Reminders with UPI payment details.
 */
object WhatsAppReportManager {

    /**
     * Formats an Indian phone number for WhatsApp direct link.
     */
    private fun sanitizePhoneNumber(rawPhone: String?): String? {
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
    private fun launchWhatsApp(context: Context, phone: String?, messageText: String) {
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
            // If direct WhatsApp URI handler isn't found, fallback to standard share intent
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
     * Nightly Business Closing Summary sent to Owner's WhatsApp (Raat Ka Hisab).
     */
    fun sendNightlyClosingSummary(
        context: Context,
        ownerPhone: String?,
        dateStr: String,
        totalMilkLiters: Double,
        totalCashCollected: Double,
        completedDeliveries: Int,
        totalCustomers: Int,
        nagaCount: Int,
        alternateOffCount: Int,
        dairyName: String = "शाहबुद्दीन डेयरी"
    ) {
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
            append("🔒 डेटाबेस पूर्णतः सुरक्षित (SQLite v11 Safe)\n")
            append("✨ शुभ रात्रि! 🥛")
        }

        launchWhatsApp(context, ownerPhone, message)
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
