package com.shahbuddindairy.app.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.shahbuddindairy.app.domain.CalculationEngine
import java.io.File
import java.io.FileOutputStream

/**
 * Universal, trusted business receipt data model for Shahbuddin Dairy V8.0.
 *
 * Feeds Image Receipt, PDF Receipt, and WhatsApp Receipt identically
 * to guarantee 100% financial consistency across all formats.
 */
data class ReceiptData(
    val businessName: String = "शाहबुद्दीन डेयरी (Shahbuddin Dairy)",
    val businessPhone: String = "9897259461",
    val upiId: String = "",
    val customerName: String,
    val customerId: Long,
    val date: String,
    val milkLiters: Double = 0.0,
    val milkType: String = "भैंस (Buffalo)",
    val rate: Double = 0.0,
    val billAmount: Double = 0.0,
    val previousDue: Double = 0.0,
    val cashPaid: Double = 0.0,
    val onlinePaid: Double = 0.0,
    val totalPaid: Double = 0.0,
    val advance: Double = 0.0,
    val remainingDue: Double = 0.0,
    val pbsPending: Double = 0.0,
    val receiptNumber: String = "",
    val note: String = ""
)

object UpiQrGenerator {

    /**
     * Builds a standard UPI payment URI.
     * Example: upi://pay?pa=dairy@upi&pn=Shahbuddin+Dairy&am=350.00&cu=INR&tn=Milk+Hisab
     */
    fun buildUpiUri(
        upiId: String,
        payeeName: String,
        amount: Double? = null,
        note: String = "Milk Hisab"
    ): String {
        val cleanUpi = upiId.trim()
        val encodedName = java.net.URLEncoder.encode(payeeName.trim(), "UTF-8")
        val encodedNote = java.net.URLEncoder.encode(note.trim(), "UTF-8")

        val base = "upi://pay?pa=$cleanUpi&pn=$encodedName&cu=INR&tn=$encodedNote"
        return if (amount != null && amount > 0.0) {
            val formattedAm = "%.2f".format(java.util.Locale.US, amount)
            "$base&am=$formattedAm"
        } else {
            base
        }
    }

    /**
     * Generates a QR code Bitmap using ZXing core offline without any network calls.
     */
    fun generateUpiQrBitmap(
        content: String,
        size: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? = generateQrBitmap(content, size, darkColor, lightColor)

    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves bitmap to a temporary cache file for Android sharing.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to start an Android Share Intent with a content Uri.
     */
    fun shareBitmap(context: Context, uri: android.net.Uri, title: String) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, title))
    }
}
