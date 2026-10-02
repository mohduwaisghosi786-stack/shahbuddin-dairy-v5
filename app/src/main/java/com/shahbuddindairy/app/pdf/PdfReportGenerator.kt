package com.shahbuddindairy.app.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.shahbuddindairy.app.data.model.CustomerEntity
import com.shahbuddindairy.app.data.model.DeliveryEntity
import com.shahbuddindairy.app.data.model.PaymentEntity
import com.shahbuddindairy.app.domain.CalculationEngine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {
    private const val TAG = "PdfReportGenerator"
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    data class PdfResult(
        val success: Boolean,
        val message: String,
        val uri: Uri? = null,
        val file: File? = null,
        val fileName: String? = null,
        val sizeBytes: Long = 0
    )

    fun generateCustomerKhataPdf(
        context: Context,
        customer: CustomerEntity,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): PdfResult {
        val document = PdfDocument()
        val now = Date()
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)
        val safeName = customer.name.replace("\\s+".toRegex(), "_").take(15)
        val fileName = "Khata_${customer.id}_${safeName}_$dateSuffix.pdf"

        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(27, 94, 32)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val rowTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                isAntiAlias = true
            }

            val altRowBgPaint = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.8f
            }

            fun drawHeader(c: Canvas) {
                // Dairy Brand Banner
                c.drawText("SHAHBUDDIN DAIRY — शाहबुद्दीन डेयरी", 40f, 45f, titlePaint)
                c.drawText("मो०: 9897259461 • कटरा, जलालाबाद • शुद्ध एवं ताजा दूध", 40f, 60f, subPaint)

                // Customer Info Box
                val boxPaint = Paint().apply {
                    color = Color.rgb(232, 245, 233)
                    style = Paint.Style.FILL
                }
                c.drawRect(40f, 75f, 555f, 130f, boxPaint)
                val borderPaint = Paint().apply {
                    color = Color.rgb(200, 230, 201)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                c.drawRect(40f, 75f, 555f, 130f, borderPaint)

                val infoPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val infoSub = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 9.5f
                    isAntiAlias = true
                }

                c.drawText("Customer #${customer.id}: ${customer.name}", 50f, 95f, infoPaint)
                c.drawText("Mohalla: ${customer.mohalla}  •  Phone: ${customer.phone.ifBlank { "N/A" }}", 50f, 112f, infoSub)
                val rateText = if (customer.ratePerLiter > 0) "Rate: ₹${customer.ratePerLiter}/L" else "Rate: Standard"
                c.drawText("Type: ${if (customer.billingType == "daily_cash") "Daily Cash" else "Monthly"}  •  $rateText  •  Default: ${customer.defaultLiters} L", 50f, 125f, infoSub)

                // Statement Date
                val printDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(now)
                c.drawText("Report Generated: $printDate", 380f, 45f, subPaint)
            }

            drawHeader(canvas)

            // Table Columns: Date (80), Type/Milk (80), Liters (50), Rate (50), Bill (65), Paid (65), Due (75), Note (50)
            val tableTop = 145f
            val rowHeight = 20f

            fun drawTableHeader(c: Canvas, topY: Float) {
                c.drawRect(40f, topY, 555f, topY + rowHeight, headerBgPaint)
                c.drawText("Date", 45f, topY + 14f, headerTextPaint)
                c.drawText("Item / Mode", 125f, topY + 14f, headerTextPaint)
                c.drawText("Milk", 215f, topY + 14f, headerTextPaint)
                c.drawText("Rate", 270f, topY + 14f, headerTextPaint)
                c.drawText("Bill", 325f, topY + 14f, headerTextPaint)
                c.drawText("Paid", 385f, topY + 14f, headerTextPaint)
                c.drawText("Net Due", 455f, topY + 14f, headerTextPaint)
                c.drawText("Status", 515f, topY + 14f, headerTextPaint)
            }

            drawTableHeader(canvas, tableTop)

            // Combine deliveries and payments chronologically
            data class TimelineEntry(
                val date: String,
                val item: String,
                val liters: Double,
                val rate: Double,
                val bill: Double,
                val paid: Double,
                val status: String,
                val isVoid: Boolean
            )

            val timeline = mutableListOf<TimelineEntry>()
            for (d in deliveries) {
                if (d.customerId == customer.id) {
                    timeline.add(
                        TimelineEntry(
                            date = d.date,
                            item = d.milkType,
                            liters = if (d.status == "delivered") d.liters else 0.0,
                            rate = d.rate,
                            bill = if (d.status == "delivered" && !d.isVoid) d.totalAmount else 0.0,
                            paid = if (!d.isVoid) d.totalPaid else 0.0,
                            status = if (d.isVoid) "VOID" else if (d.status == "skipped") "NAGA" else "DONE",
                            isVoid = d.isVoid
                        )
                    )
                }
            }
            for (p in payments) {
                if (p.customerId == customer.id) {
                    timeline.add(
                        TimelineEntry(
                            date = p.date,
                            item = "Payment (${p.paymentMode})",
                            liters = 0.0,
                            rate = 0.0,
                            bill = 0.0,
                            paid = if (!p.isVoid) p.amount else 0.0,
                            status = if (p.isVoid) "VOID" else "PAID",
                            isVoid = p.isVoid
                        )
                    )
                }
            }

            timeline.sortBy { it.date }

            var currentY = tableTop + rowHeight
            var runningBalance = 0.0
            var totalMilkLiters = 0.0
            var totalBilled = 0.0
            var totalPaidAmount = 0.0

            for (i in timeline.indices) {
                if (currentY > PAGE_HEIGHT - 60) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    drawTableHeader(canvas, tableTop)
                    currentY = tableTop + rowHeight
                }

                val entry = timeline[i]
                if (!entry.isVoid) {
                    totalMilkLiters += entry.liters
                    totalBilled += entry.bill
                    totalPaidAmount += entry.paid
                    runningBalance += (entry.bill - entry.paid)
                }

                if (i % 2 == 1) {
                    canvas.drawRect(40f, currentY, 555f, currentY + rowHeight, altRowBgPaint)
                }

                val rowColor = if (entry.isVoid) Color.GRAY else Color.BLACK
                rowTextPaint.color = rowColor

                canvas.drawText(CalculationEngine.formatDateDisplay(entry.date), 45f, currentY + 14f, rowTextPaint)
                canvas.drawText(entry.item.take(16), 125f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (entry.liters > 0) "%.2fL".format(entry.liters) else "—", 215f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (entry.rate > 0) "₹%.0f".format(entry.rate) else "—", 270f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (entry.bill > 0) "₹%.0f".format(entry.bill) else "—", 325f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (entry.paid > 0) "₹%.0f".format(entry.paid) else "—", 385f, currentY + 14f, rowTextPaint)
                canvas.drawText(CalculationEngine.formatMoney(runningBalance), 455f, currentY + 14f, rowTextPaint)

                val statusPaint = Paint().apply {
                    textSize = 8.5f
                    isFakeBoldText = true
                    color = when (entry.status) {
                        "DONE", "PAID" -> Color.rgb(46, 125, 50)
                        "NAGA", "VOID" -> Color.rgb(198, 40, 40)
                        else -> Color.DKGRAY
                    }
                }
                canvas.drawText(entry.status, 515f, currentY + 14f, statusPaint)

                canvas.drawLine(40f, currentY + rowHeight, 555f, currentY + rowHeight, linePaint)
                currentY += rowHeight
            }

            // Summary Footer Box
            currentY += 15f
            if (currentY > PAGE_HEIGHT - 90) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            val summaryBg = Paint().apply {
                color = Color.rgb(232, 245, 233)
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, currentY, 555f, currentY + 65f, summaryBg)
            val summaryBorder = Paint().apply {
                color = Color.rgb(46, 125, 50)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRect(40f, currentY, 555f, currentY + 65f, summaryBorder)

            val summaryTitlePaint = Paint().apply {
                color = Color.rgb(27, 94, 32)
                textSize = 11f
                isFakeBoldText = true
            }
            val summaryValPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isFakeBoldText = true
            }
            val dueValPaint = Paint().apply {
                color = if (runningBalance > 0) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50)
                textSize = 14f
                isFakeBoldText = true
            }

            canvas.drawText("SUMMARY TOTALS (खाता सारांश)", 50f, currentY + 20f, summaryTitlePaint)
            canvas.drawText("Total Milk: %.2f L".format(totalMilkLiters), 50f, currentY + 45f, summaryValPaint)
            canvas.drawText("Total Bill: ₹%,d".format(totalBilled.toLong()), 170f, currentY + 45f, summaryValPaint)
            canvas.drawText("Total Paid: ₹%,d".format(totalPaidAmount.toLong()), 290f, currentY + 45f, summaryValPaint)

            val balanceLabel = if (runningBalance >= 0) "Current Due: " else "Advance: "
            val balanceStr = balanceLabel + CalculationEngine.formatMoney(if (runningBalance >= 0) runningBalance else -runningBalance)
            canvas.drawText(balanceStr, 400f, currentY + 45f, dueValPaint)

            document.finishPage(page)

            // Save PDF physically to local cache & MediaStore
            val sharedDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val localFile = File(sharedDir, fileName)
            FileOutputStream(localFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
            }
            document.close()

            var externalUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { os ->
                        localFile.inputStream().use { it.copyTo(os) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    externalUri = itemUri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val target = File(downloadsDir, fileName)
                localFile.copyTo(target, overwrite = true)
                externalUri = Uri.fromFile(target)
            }

            val shareUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            } catch (e: Exception) {
                externalUri ?: Uri.fromFile(localFile)
            }

            Log.i(TAG, "PDF Khata created: ${localFile.absolutePath} (${localFile.length()} bytes)")
            return PdfResult(
                success = true,
                message = "Khata PDF saved: $fileName",
                uri = shareUri,
                file = localFile,
                fileName = fileName,
                sizeBytes = localFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "PDF generation error", e)
            try { document.close() } catch (_: Exception) {}
            return PdfResult(false, "PDF generation failed: ${e.message}")
        }
    }

    fun generateDailyReportPdf(
        context: Context,
        date: String,
        customers: List<CustomerEntity>,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): PdfResult {
        val document = PdfDocument()
        val fileName = "ShahbuddinDairy_DailyReport_${date.replace("-", "")}.pdf"

        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(27, 94, 32)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val rowTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                isAntiAlias = true
            }

            val altRowBgPaint = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.8f
            }

            canvas.drawText("SHAHBUDDIN DAIRY — DAILY REPORT", 40f, 45f, titlePaint)
            canvas.drawText("दैनिक दूध बिक्री एवं वसूली रिपोर्ट • Date: ${CalculationEngine.formatDateDisplay(date)}", 40f, 62f, subPaint)

            // Compute Daily Metrics
            var totalSold = 0.0
            var totalBill = 0.0
            var totalCash = 0.0
            var servedCount = 0
            var nagaCount = 0
            var pendingCount = 0

            val delivMap = deliveries.filter { it.date == date }.associateBy { it.customerId }

            for (c in customers) {
                if (c.isActive == 0 || c.isArchived == 1) continue
                val d = delivMap[c.id]
                if (d != null && !d.isVoid) {
                    if (d.status == "delivered") {
                        servedCount++
                        totalSold += d.liters
                        totalBill += d.totalAmount
                        totalCash += d.paidCash
                    } else if (d.status == "skipped") {
                        nagaCount++
                    }
                } else {
                    pendingCount++
                }
            }

            // Summary Header Box
            val boxPaint = Paint().apply {
                color = Color.rgb(232, 245, 233)
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, 75f, 555f, 130f, boxPaint)
            val borderPaint = Paint().apply {
                color = Color.rgb(200, 230, 201)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(40f, 75f, 555f, 130f, borderPaint)

            val summaryPaint = Paint().apply {
                color = Color.rgb(27, 94, 32)
                textSize = 10.5f
                isFakeBoldText = true
            }
            canvas.drawText("TOTAL MILK: %.2f L   •   TOTAL BILL: ₹%,d   •   CASH COLLECTED: ₹%,d".format(totalSold, totalBill.toLong(), totalCash.toLong()), 50f, 96f, summaryPaint)
            val countPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
            }
            canvas.drawText("Completed: $servedCount customers  •  Naga (Skipped): $nagaCount  •  Pending: $pendingCount", 50f, 115f, countPaint)

            // Table Header
            val tableTop = 145f
            val rowHeight = 20f

            fun drawTableHeader(c: Canvas, topY: Float) {
                c.drawRect(40f, topY, 555f, topY + rowHeight, headerBgPaint)
                c.drawText("#", 45f, topY + 14f, headerTextPaint)
                c.drawText("Customer Name", 75f, topY + 14f, headerTextPaint)
                c.drawText("Mohalla", 225f, topY + 14f, headerTextPaint)
                c.drawText("Milk", 330f, topY + 14f, headerTextPaint)
                c.drawText("Rate", 380f, topY + 14f, headerTextPaint)
                c.drawText("Today Bill", 430f, topY + 14f, headerTextPaint)
                c.drawText("Cash Paid", 495f, topY + 14f, headerTextPaint)
            }

            drawTableHeader(canvas, tableTop)

            var currentY = tableTop + rowHeight

            for (i in customers.indices) {
                val c = customers[i]
                if (c.isArchived == 1) continue

                if (currentY > PAGE_HEIGHT - 50) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawTableHeader(canvas, 40f)
                    currentY = 40f + rowHeight
                }

                if (i % 2 == 1) {
                    canvas.drawRect(40f, currentY, 555f, currentY + rowHeight, altRowBgPaint)
                }

                val d = delivMap[c.id]
                canvas.drawText("${c.id}", 45f, currentY + 14f, rowTextPaint)
                canvas.drawText(c.name.take(20), 75f, currentY + 14f, rowTextPaint)
                canvas.drawText(c.mohalla.take(15), 225f, currentY + 14f, rowTextPaint)

                if (d != null && !d.isVoid) {
                    if (d.status == "delivered") {
                        canvas.drawText("%.2fL".format(d.liters), 330f, currentY + 14f, rowTextPaint)
                        canvas.drawText("₹%.0f".format(d.rate), 380f, currentY + 14f, rowTextPaint)
                        canvas.drawText("₹%.0f".format(d.totalAmount), 430f, currentY + 14f, rowTextPaint)
                        canvas.drawText(if (d.paidCash > 0) "₹%.0f".format(d.paidCash) else "—", 495f, currentY + 14f, rowTextPaint)
                    } else {
                        canvas.drawText("NAGA", 330f, currentY + 14f, Paint().apply { color = Color.RED; textSize = 9f; isFakeBoldText = true })
                        canvas.drawText("—", 380f, currentY + 14f, rowTextPaint)
                        canvas.drawText("—", 430f, currentY + 14f, rowTextPaint)
                        canvas.drawText("—", 495f, currentY + 14f, rowTextPaint)
                    }
                } else {
                    canvas.drawText("Pending", 330f, currentY + 14f, Paint().apply { color = Color.rgb(230, 81, 0); textSize = 9f })
                    canvas.drawText("—", 380f, currentY + 14f, rowTextPaint)
                    canvas.drawText("—", 430f, currentY + 14f, rowTextPaint)
                    canvas.drawText("—", 495f, currentY + 14f, rowTextPaint)
                }

                canvas.drawLine(40f, currentY + rowHeight, 555f, currentY + rowHeight, linePaint)
                currentY += rowHeight
            }

            document.finishPage(page)

            val sharedDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val localFile = File(sharedDir, fileName)
            FileOutputStream(localFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
            }
            document.close()

            var externalUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { os ->
                        localFile.inputStream().use { it.copyTo(os) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    externalUri = itemUri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val target = File(downloadsDir, fileName)
                localFile.copyTo(target, overwrite = true)
                externalUri = Uri.fromFile(target)
            }

            val shareUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            } catch (e: Exception) {
                externalUri ?: Uri.fromFile(localFile)
            }

            Log.i(TAG, "Daily Report PDF created: ${localFile.absolutePath} (${localFile.length()} bytes)")
            return PdfResult(
                success = true,
                message = "Daily Report PDF saved: $fileName",
                uri = shareUri,
                file = localFile,
                fileName = fileName,
                sizeBytes = localFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Daily PDF generation error", e)
            try { document.close() } catch (_: Exception) {}
            return PdfResult(false, "Daily PDF generation failed: ${e.message}")
        }
    }
}
