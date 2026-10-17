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
import com.shahbuddindairy.app.receipt.ReceiptData
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

            // Table Header (Mobile Portrait Optimized with Prev Due & Advance)
            val tableTop = 145f
            val rowHeight = 20f

            fun drawTableHeader(c: Canvas, topY: Float) {
                c.drawRect(40f, topY, 555f, topY + rowHeight, headerBgPaint)
                c.drawText("#", 44f, topY + 14f, headerTextPaint)
                c.drawText("Customer", 68f, topY + 14f, headerTextPaint)
                c.drawText("Milk", 192f, topY + 14f, headerTextPaint)
                c.drawText("Bill", 236f, topY + 14f, headerTextPaint)
                c.drawText("Prev Due", 280f, topY + 14f, headerTextPaint)
                c.drawText("Advance", 340f, topY + 14f, headerTextPaint)
                c.drawText("Cash", 398f, topY + 14f, headerTextPaint)
                c.drawText("Net Bal", 452f, topY + 14f, headerTextPaint)
                c.drawText("Status", 512f, topY + 14f, headerTextPaint)
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
                val prevBal = CalculationEngine.calculatePrevDueBefore(c.id, date, deliveries, payments)
                val prevDueVal = if (prevBal > 0) prevBal else 0.0
                val advanceVal = if (prevBal < 0) -prevBal else 0.0

                val billAmt = if (d != null && !d.isVoid && d.status == "delivered") d.totalAmount else 0.0
                val cashAmt = if (d != null && !d.isVoid) d.paidCash else 0.0
                val netBal = prevBal + billAmt - cashAmt

                canvas.drawText("${c.id}", 44f, currentY + 14f, rowTextPaint)
                canvas.drawText(c.name.take(16), 68f, currentY + 14f, rowTextPaint)

                if (d != null && !d.isVoid) {
                    if (d.status == "delivered") {
                        canvas.drawText("%.2fL".format(d.liters), 192f, currentY + 14f, rowTextPaint)
                        canvas.drawText("₹%.0f".format(billAmt), 236f, currentY + 14f, rowTextPaint)
                    } else {
                        canvas.drawText("NAGA", 192f, currentY + 14f, Paint().apply { color = Color.RED; textSize = 9f; isFakeBoldText = true })
                        canvas.drawText("—", 236f, currentY + 14f, rowTextPaint)
                    }
                } else {
                    canvas.drawText("Pending", 192f, currentY + 14f, Paint().apply { color = Color.rgb(230, 81, 0); textSize = 8.5f })
                    canvas.drawText("—", 236f, currentY + 14f, rowTextPaint)
                }

                canvas.drawText(if (prevDueVal > 0) "₹%.0f".format(prevDueVal) else "—", 280f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (advanceVal > 0) "₹%.0f".format(advanceVal) else "—", 340f, currentY + 14f, rowTextPaint)
                canvas.drawText(if (cashAmt > 0) "₹%.0f".format(cashAmt) else "—", 398f, currentY + 14f, rowTextPaint)

                // Net Balance
                val netBalText = if (netBal > 0) "₹%.0f".format(netBal) else if (netBal < 0) "+₹%.0f".format(-netBal) else "₹0"
                val netBalColor = if (netBal > 0) Color.rgb(180, 0, 0) else if (netBal < 0) Color.rgb(27, 94, 32) else Color.BLACK
                canvas.drawText(netBalText, 452f, currentY + 14f, Paint().apply { color = netBalColor; textSize = 8.5f; isFakeBoldText = netBal != 0.0 })

                // Status tag
                val statusText = if (d != null && !d.isVoid) {
                    if (d.status == "delivered") "DONE ✓" else "NAGA"
                } else "PEND"
                val statusColor = if (d != null && !d.isVoid) {
                    if (d.status == "delivered") Color.rgb(27, 94, 32) else Color.RED
                } else Color.rgb(230, 81, 0)
                canvas.drawText(statusText, 512f, currentY + 14f, Paint().apply { color = statusColor; textSize = 8f; isFakeBoldText = true })

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

    /**
     * V8.0: Generates a dedicated single delivery / payment receipt PDF.
     */
    fun generateSingleReceiptPdf(
        context: Context,
        receipt: ReceiptData
    ): PdfResult {
        val document = PdfDocument()
        val now = Date()
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)
        val safeName = receipt.customerName.replace("\\s+".toRegex(), "_").take(15)
        val fileName = "Receipt_${receipt.customerId}_${safeName}_$dateSuffix.pdf"

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background Card
            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, 20f, (PAGE_WIDTH - 20).toFloat(), (PAGE_HEIGHT - 20).toFloat(), bgPaint)

            // Header Banner
            val headerPaint = Paint().apply {
                color = Color.rgb(74, 20, 140)
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, 20f, (PAGE_WIDTH - 20).toFloat(), 130f, headerPaint)

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("शाहबुद्दीन डेयरी (Shahbuddin Dairy)", (PAGE_WIDTH / 2).toFloat(), 60f, titlePaint)

            val subTitlePaint = Paint().apply {
                color = Color.rgb(225, 190, 231)
                textSize = 13f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("वितरण व भुगतान रसीद • V8.0", (PAGE_WIDTH / 2).toFloat(), 88f, subTitlePaint)

            val phonePaint = Paint().apply {
                color = Color.rgb(209, 196, 233)
                textSize = 11.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("संपर्क: ${receipt.businessPhone} ${if (receipt.upiId.isNotBlank()) "• UPI: ${receipt.upiId}" else ""}", (PAGE_WIDTH / 2).toFloat(), 112f, phonePaint)

            var y = 165f
            val labelPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 12f
            }
            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 12.5f
            }
            val greenPaint = Paint().apply {
                color = Color.rgb(22, 101, 52)
                textSize = 13.5f
                isFakeBoldText = true
            }
            val redPaint = Paint().apply {
                color = Color.rgb(185, 28, 28)
                textSize = 13.5f
                isFakeBoldText = true
            }
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            // Customer Details Box
            canvas.drawText("ग्राहक (Customer):", 45f, y, labelPaint)
            canvas.drawText("#${receipt.customerId} ${receipt.customerName}", 180f, y, boldPaint)
            y += 24f

            canvas.drawText("दिनांक (Date):", 45f, y, labelPaint)
            canvas.drawText(CalculationEngine.formatDateDisplay(receipt.date), 180f, y, textPaint)
            y += 24f

            if (receipt.receiptNumber.isNotBlank()) {
                canvas.drawText("रसीद संख्या (Receipt #):", 45f, y, labelPaint)
                canvas.drawText(receipt.receiptNumber, 180f, y, textPaint)
                y += 24f
            }

            y += 8f
            canvas.drawLine(45f, y, (PAGE_WIDTH - 45).toFloat(), y, linePaint)
            y += 26f

            fun drawReceiptRow(label: String, value: String, valPaint: Paint = boldPaint) {
                canvas.drawText(label, 45f, y, labelPaint)
                val w = valPaint.measureText(value)
                canvas.drawText(value, (PAGE_WIDTH - 45 - w), y, valPaint)
                y += 26f
            }

            if (receipt.milkLiters > 0.0) {
                drawReceiptRow("दूध मात्रा (Milk Quantity):", "${CalculationEngine.formatLiters(receipt.milkLiters)} (${receipt.milkType})")
                drawReceiptRow("दर (Rate / Liter):", CalculationEngine.formatMoney(receipt.rate) + " / L", textPaint)
                drawReceiptRow("आज का बिल (Today's Bill):", CalculationEngine.formatMoney(receipt.billAmount))
            }

            drawReceiptRow("पिछला बकाया (Previous Due):", CalculationEngine.formatMoney(receipt.previousDue))
            if (receipt.pbsPending > 0.0) {
                drawReceiptRow("PBS पुरानी बही बकाया:", CalculationEngine.formatMoney(receipt.pbsPending))
            }

            val totalReq = receipt.billAmount + receipt.previousDue + receipt.pbsPending
            y += 4f
            canvas.drawLine(45f, y, (PAGE_WIDTH - 45).toFloat(), y, linePaint)
            y += 24f
            drawReceiptRow("कुल देय हिसाब (Total Required):", CalculationEngine.formatMoney(totalReq))

            if (receipt.cashPaid > 0.0) {
                drawReceiptRow("नकद प्राप्त (Cash Paid):", CalculationEngine.formatMoney(receipt.cashPaid), greenPaint)
            }
            if (receipt.onlinePaid > 0.0) {
                drawReceiptRow("ऑनलाइन प्राप्त (Online Paid):", CalculationEngine.formatMoney(receipt.onlinePaid), greenPaint)
            }
            if (receipt.totalPaid > 0.0 && (receipt.cashPaid > 0.0 && receipt.onlinePaid > 0.0)) {
                drawReceiptRow("कुल जमा (Total Paid):", CalculationEngine.formatMoney(receipt.totalPaid), greenPaint)
            }

            y += 4f
            canvas.drawLine(45f, y, (PAGE_WIDTH - 45).toFloat(), y, linePaint)
            y += 26f

            if (receipt.remainingDue > 0.0) {
                drawReceiptRow("शेष बकाया (Remaining Due):", CalculationEngine.formatMoney(receipt.remainingDue), redPaint)
            } else {
                drawReceiptRow("शेष बकाया (Remaining Due):", "₹0.00 (खाता चुकता ✓)", greenPaint)
            }

            if (receipt.advance > 0.0) {
                drawReceiptRow("पेशगी / जमा (Advance):", CalculationEngine.formatMoney(receipt.advance), greenPaint)
            }

            // Footer
            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 10.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("शाहबुद्दीन डेयरी सेवा में सदैव समर्पित • व्यावसायिक शुद्धता गारंटी", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 45).toFloat(), footerPaint)

            document.finishPage(page)

            // Save PDF
            val localFile = File(context.cacheDir, fileName)
            FileOutputStream(localFile).use { out -> document.writeTo(out) }
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
                    resolver.openOutputStream(itemUri)?.use { os -> localFile.inputStream().use { it.copyTo(os) } }
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

            return PdfResult(
                success = true,
                message = "रसीद PDF सुरक्षित सहेजी गई: $fileName",
                uri = shareUri,
                file = localFile,
                fileName = fileName,
                sizeBytes = localFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Receipt PDF generation error", e)
            try { document.close() } catch (_: Exception) {}
            return PdfResult(false, "रसीद PDF निर्माण असफल: ${e.message}")
        }
    }

    /**
     * V8.0: Generates a dedicated Monthly Customer Statement PDF.
     */
    fun generateMonthlyStatementPdf(
        context: Context,
        customer: CustomerEntity,
        monthStr: String, // e.g. "2026-10"
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>
    ): PdfResult {
        val document = PdfDocument()
        val safeName = customer.name.replace("\\s+".toRegex(), "_").take(15)
        val fileName = "Statement_${customer.id}_${safeName}_${monthStr.replace("-", "")}.pdf"

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            val headerPaint = Paint().apply {
                color = Color.rgb(74, 20, 140)
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, 20f, (PAGE_WIDTH - 20).toFloat(), 110f, headerPaint)

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("शाहबुद्दीन डेयरी — मासिक ग्राहक विवरण (Monthly Statement)", (PAGE_WIDTH / 2).toFloat(), 55f, titlePaint)

            val subTitlePaint = Paint().apply {
                color = Color.rgb(225, 190, 231)
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("माह: $monthStr • ग्राहक: #${customer.id} ${customer.name}", (PAGE_WIDTH / 2).toFloat(), 82f, subTitlePaint)

            var y = 135f
            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11.5f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 11f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            // Month Deliveries & Payments
            val monthDeliveries = deliveries.filter { it.date.startsWith(monthStr) && (it.status.equals("delivered", ignoreCase = true)) && !it.isVoid }
            val monthPayments = payments.filter { it.date.startsWith(monthStr) && !it.isVoid }

            val totalLiters = monthDeliveries.sumOf { it.liters }
            val totalBilled = monthDeliveries.sumOf { it.totalAmount }
            val totalPaid = monthPayments.sumOf { it.amount }

            // Summary row
            canvas.drawText("कुल वितरण: ${CalculationEngine.formatLiters(totalLiters)}", 40f, y, boldPaint)
            canvas.drawText("कुल बिल: ${CalculationEngine.formatMoney(totalBilled)}", 190f, y, boldPaint)
            canvas.drawText("कुल जमा: ${CalculationEngine.formatMoney(totalPaid)}", 340f, y, boldPaint)
            y += 20f
            canvas.drawLine(40f, y, (PAGE_WIDTH - 40).toFloat(), y, linePaint)
            y += 18f

            // Table Header
            canvas.drawText("दिनांक (Date)", 40f, y, boldPaint)
            canvas.drawText("विवरण (Type)", 140f, y, boldPaint)
            canvas.drawText("मात्रा (Qty)", 250f, y, boldPaint)
            canvas.drawText("बिल (Bill)", 350f, y, boldPaint)
            canvas.drawText("जमा (Paid)", 460f, y, boldPaint)
            y += 8f
            canvas.drawLine(40f, y, (PAGE_WIDTH - 40).toFloat(), y, linePaint)
            y += 16f

            // Merge entries for table
            data class StmtRow(val date: String, val type: String, val qty: String, val bill: String, val paid: String)
            val rows = mutableListOf<StmtRow>()

            monthDeliveries.forEach { d ->
                rows.add(StmtRow(d.date, "दूध (${d.milkType})", CalculationEngine.formatLiters(d.liters), CalculationEngine.formatMoney(d.totalAmount), "-"))
            }
            monthPayments.forEach { p ->
                rows.add(StmtRow(p.date, "भुगतान (${p.paymentMode})", "-", "-", CalculationEngine.formatMoney(p.amount)))
            }
            rows.sortBy { it.date }

            rows.take(24).forEach { row ->
                canvas.drawText(row.date, 40f, y, textPaint)
                canvas.drawText(row.type, 140f, y, textPaint)
                canvas.drawText(row.qty, 250f, y, textPaint)
                canvas.drawText(row.bill, 350f, y, textPaint)
                canvas.drawText(row.paid, 460f, y, textPaint)
                y += 18f
            }

            document.finishPage(page)

            val localFile = File(context.cacheDir, fileName)
            FileOutputStream(localFile).use { out -> document.writeTo(out) }
            document.close()

            val shareUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            } catch (e: Exception) {
                Uri.fromFile(localFile)
            }

            return PdfResult(
                success = true,
                message = "मासिक विवरण PDF तैयार: $fileName",
                uri = shareUri,
                file = localFile,
                fileName = fileName,
                sizeBytes = localFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Monthly statement error", e)
            try { document.close() } catch (_: Exception) {}
            return PdfResult(false, "मासिक विवरण PDF असफल: ${e.message}")
        }
    }
}

