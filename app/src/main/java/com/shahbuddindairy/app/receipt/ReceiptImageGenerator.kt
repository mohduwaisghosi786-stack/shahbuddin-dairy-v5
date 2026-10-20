package com.shahbuddindairy.app.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.shahbuddindairy.app.domain.CalculationEngine
import java.io.File
import java.io.FileOutputStream

object ReceiptImageGenerator {

    /**
     * Generates a high-quality receipt image bitmap from ReceiptData.
     */
    fun createReceiptBitmap(
        data: ReceiptData,
        qrBitmap: Bitmap? = null
    ): Bitmap {
        val width = 720
        val height = if (qrBitmap != null) 1150 else 920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.rgb(250, 250, 252))

        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat())
        canvas.drawRoundRect(cardRect, 24f, 24f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = Color.rgb(220, 224, 230)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Header Background
        val headerPaint = Paint().apply {
            color = Color.rgb(74, 20, 140) // Purple
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val headerRect = RectF(20f, 20f, (width - 20).toFloat(), 170f)
        canvas.drawRoundRect(headerRect, 24f, 24f, headerPaint)
        // Rectify bottom corners of header
        canvas.drawRect(20f, 150f, (width - 20).toFloat(), 170f, headerPaint)

        // Header text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("शाहबुद्दीन डेयरी (Shahbuddin Dairy)", (width / 2).toFloat(), 75f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = Color.rgb(225, 190, 231)
            textSize = 20f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("वितरण व भुगतान रसीद • V8.9", (width / 2).toFloat(), 115f, subTitlePaint)

        val phonePaint = Paint().apply {
            color = Color.rgb(209, 196, 233)
            textSize = 17f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("संपर्क: ${data.businessPhone} ${if (data.upiId.isNotBlank()) "• UPI: ${data.upiId}" else ""}", (width / 2).toFloat(), 148f, phonePaint)

        var y = 220f
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 21f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 19f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val greenPaint = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Customer Info Row
        canvas.drawText("ग्राहक (Customer):", 50f, y, labelPaint)
        canvas.drawText("#${data.customerId} ${data.customerName}", 260f, y, boldPaint)
        y += 38f

        canvas.drawText("दिनांक (Date):", 50f, y, labelPaint)
        canvas.drawText(CalculationEngine.formatDateDisplay(data.date), 260f, y, textPaint)
        y += 38f

        if (data.receiptNumber.isNotBlank()) {
            canvas.drawText("रसीद संख्या (Receipt #):", 50f, y, labelPaint)
            canvas.drawText(data.receiptNumber, 260f, y, textPaint)
            y += 38f
        }

        // Divider
        y += 10f
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1.5f
        }
        canvas.drawLine(50f, y, (width - 50).toFloat(), y, linePaint)
        y += 35f

        // Table items
        fun drawRow(label: String, value: String, valuePaint: Paint = boldPaint) {
            canvas.drawText(label, 50f, y, labelPaint)
            val vWidth = valuePaint.measureText(value)
            canvas.drawText(value, (width - 50 - vWidth), y, valuePaint)
            y += 36f
        }

        if (data.milkLiters > 0.0) {
            drawRow("आज का दूध (Milk Qty):", "${CalculationEngine.formatLiters(data.milkLiters)} (${data.milkType})")
            drawRow("दर (Rate / Liter):", CalculationEngine.formatMoney(data.rate) + " / L", textPaint)
            drawRow("आज का बिल (Today's Bill):", CalculationEngine.formatMoney(data.billAmount))
        }

        drawRow("पिछला बकाया (Previous Due):", CalculationEngine.formatMoney(data.previousDue))
        if (data.pbsPending > 0.0) {
            drawRow("PBS पुरानी बही बकाया:", CalculationEngine.formatMoney(data.pbsPending))
        }

        // Subtotal Line
        val totalReq = data.billAmount + data.previousDue + data.pbsPending
        y += 6f
        canvas.drawLine(50f, y, (width - 50).toFloat(), y, linePaint)
        y += 32f
        drawRow("कुल देय हिसाब (Total Required):", CalculationEngine.formatMoney(totalReq))

        // Payments
        if (data.cashPaid > 0.0) {
            drawRow("नकद प्राप्त (Cash Paid):", CalculationEngine.formatMoney(data.cashPaid), greenPaint)
        }
        if (data.onlinePaid > 0.0) {
            drawRow("ऑनलाइन प्राप्त (Online Paid):", CalculationEngine.formatMoney(data.onlinePaid), greenPaint)
        }
        if (data.totalPaid > 0.0 && (data.cashPaid > 0.0 && data.onlinePaid > 0.0)) {
            drawRow("कुल जमा (Total Paid):", CalculationEngine.formatMoney(data.totalPaid), greenPaint)
        }

        // Final Balances
        y += 6f
        canvas.drawLine(50f, y, (width - 50).toFloat(), y, linePaint)
        y += 34f

        if (data.remainingDue > 0.0) {
            drawRow("शेष बकाया (Remaining Due):", CalculationEngine.formatMoney(data.remainingDue), redPaint)
        } else {
            drawRow("शेष बकाया (Remaining Due):", "₹0.00 (खाता चुकता ✓)", greenPaint)
        }

        if (data.advance > 0.0) {
            drawRow("पेशगी / जमा (Advance):", CalculationEngine.formatMoney(data.advance), greenPaint)
        }

        // Optional QR Code
        if (qrBitmap != null) {
            y += 15f
            canvas.drawLine(50f, y, (width - 50).toFloat(), y, linePaint)
            y += 25f

            val qrSize = 170
            val qrLeft = ((width - qrSize) / 2).toFloat()
            val qrDst = RectF(qrLeft, y, qrLeft + qrSize, y + qrSize)
            canvas.drawBitmap(qrBitmap, null, qrDst, null)
            y += qrSize + 22f

            val qrLabelPaint = Paint().apply {
                color = Color.rgb(74, 20, 140)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("QR स्कैन कर सीधे UPI भुगतान करें", (width / 2).toFloat(), y, qrLabelPaint)
            y += 24f
        }

        // Footer note
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 15f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("शाहबुद्दीन डेयरी सेवा में सदैव समर्पित • सुरक्षित व्यावसायिक रिकॉर्ड", (width / 2).toFloat(), (height - 35).toFloat(), footerPaint)

        return bitmap
    }

    /**
     * Saves receipt bitmap to a temporary file and returns content Uri.
     */
    fun saveReceiptImage(context: Context, bitmap: Bitmap, customerId: Long): File {
        val cacheDir = File(context.cacheDir, "receipt_images").apply { mkdirs() }
        val timeStamp = System.currentTimeMillis()
        val file = File(cacheDir, "Receipt_${customerId}_$timeStamp.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
