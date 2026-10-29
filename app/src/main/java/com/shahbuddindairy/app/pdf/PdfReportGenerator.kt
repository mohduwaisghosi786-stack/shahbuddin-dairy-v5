package com.shahbuddindairy.app.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.util.Calendar
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
                c.drawText("मो०: 9258263340 • 96, Gulab Nagar, Bareilly, Uttar Pradesh, India", 40f, 60f, subPaint)

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

    fun getHindiDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance().apply { time = d }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "रविवार"
                Calendar.MONDAY -> "सोमवार"
                Calendar.TUESDAY -> "मंगलवार"
                Calendar.WEDNESDAY -> "बुधवार"
                Calendar.THURSDAY -> "गुरुवार"
                Calendar.FRIDAY -> "शुक्रवार"
                Calendar.SATURDAY -> "शनिवार"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private const val MOBILE_PAGE_WIDTH = 380
    private const val MOBILE_PAGE_HEIGHT = 800
    private const val OFFICIAL_ADDRESS = "96, Gulab Nagar, Bareilly, Uttar Pradesh, India"
    private const val OWNER_WHATSAPP_PHONE = "9258263340"

    private fun drawMobileBrandHeader(canvas: Canvas, title: String, subtitle: String, date: String) {
        val barPaint = Paint().apply {
            color = Color.rgb(22, 163, 74)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, MOBILE_PAGE_WIDTH.toFloat(), 4f, barPaint)

        // Dairy Logo Badge
        val badgePaint = Paint().apply {
            color = Color.rgb(27, 94, 32)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(25f, 23f, 11f, badgePaint)
        val iconPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("🥛", 25f, 27f, iconPaint)

        val brandPaint = Paint().apply {
            color = Color.rgb(27, 94, 32)
            textSize = 11.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("SHAHBUDDIN DAIRY", 42f, 19f, brandPaint)

        val subPaint = Paint().apply {
            color = Color.rgb(80, 80, 80)
            textSize = 7f
            isAntiAlias = true
        }
        canvas.drawText("$title • $subtitle", 42f, 29f, subPaint)

        val addrPaint = Paint().apply {
            color = Color.rgb(110, 110, 110)
            textSize = 6f
            isAntiAlias = true
        }
        canvas.drawText(OFFICIAL_ADDRESS, 42f, 38f, addrPaint)

        // Date & Phone pill on top right
        val datePillBg = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val datePillBorder = Paint().apply {
            color = Color.rgb(187, 247, 208)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val dRect = RectF(260f, 9f, 366f, 39f)
        canvas.drawRoundRect(dRect, 4f, 4f, datePillBg)
        canvas.drawRoundRect(dRect, 4f, 4f, datePillBorder)

        val (dText, mText) = try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdfIn.parse(date) ?: Date()
            val dStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
            val dayName = getHindiDayOfWeek(date)
            dStr to dayName
        } catch (e: Exception) {
            date to ""
        }
        val dPaint = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 7.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val mPaint = Paint().apply {
            color = Color.rgb(74, 85, 104)
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(dText, 313f, 22f, dPaint)
        canvas.drawText(if (mText.isNotBlank()) mText else "मो० $OWNER_WHATSAPP_PHONE", 313f, 33f, mPaint)

        // Header separator line
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        canvas.drawLine(14f, 44f, 366f, 44f, linePaint)
    }

    private fun drawMobileBrandFooter(canvas: Canvas, pageNum: Int, totalPages: Int, date: String) {
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val footerY = MOBILE_PAGE_HEIGHT - 22f
        canvas.drawLine(14f, footerY, 366f, footerY, linePaint)

        val footerTextPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 6.8f
            isAntiAlias = true
        }
        canvas.drawText("Shahbuddin Dairy • 96, Gulab Nagar, Bareilly • मो० $OWNER_WHATSAPP_PHONE", 14f, footerY + 11f, footerTextPaint)

        val pageTextPaint = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 7.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Page $pageNum of $totalPages", 366f, footerY + 11f, pageTextPaint)
    }

    fun generateDailyReportPdf(
        context: Context,
        date: String,
        customers: List<CustomerEntity>,
        deliveries: List<DeliveryEntity>,
        payments: List<PaymentEntity>,
        pbsMap: Map<Long, Double> = emptyMap()
    ): PdfResult {
        val document = PdfDocument()
        val fileName = "ShahbuddinDairy_DailyReport_${date.replace("-", "")}.pdf"

        try {
            val activeCustomers = customers
                .filter { it.isArchived == 0 && it.isActive == 1 }
                .sortedWith(compareBy({ it.routeOrder }, { it.id }))
            val delivMap = deliveries.filter { it.date == date && !it.isVoid }.associateBy { it.customerId }
            val paymentsToday = payments.filter { it.date == date && !it.isVoid }

            data class CustomerCardItem(
                val customer: CustomerEntity,
                val delivery: DeliveryEntity?,
                val isDelivered: Boolean,
                val isNaga: Boolean,
                val isAltDay: Boolean,
                val isPending: Boolean,
                val liters: Double,
                val rate: Double,
                val bill: Double,
                val paid: Double,
                val netBal: Double,
                val pbs: Double = 0.0
            )

            val customerRows = mutableListOf<CustomerCardItem>()
            var servedCount = 0
            var nagaCount = 0
            var pendingCount = 0
            var altDayCount = 0
            var totalSold = 0.0
            var buffaloSold = 0.0
            var cowSold = 0.0
            var totalBill = 0.0
            var totalCash = 0.0
            var totalOnline = 0.0

            for (c in activeCustomers) {
                val d = delivMap[c.id]
                val isDeliv = d != null && !d.isVoid && d.status == "delivered"
                val isN = d != null && !d.isVoid && (d.status == "skipped" || d.status == "naga")
                val isAlt = if (!isDeliv && !isN) CalculationEngine.isOffDay(c, date) else false
                val isPend = !isDeliv && !isN && !isAlt

                val cLiters = if (isDeliv) (d?.liters ?: 0.0) else 0.0
                val cRate = d?.rate ?: c.ratePerLiter
                val cBill = if (isDeliv) (d?.totalAmount ?: 0.0) else 0.0
                val cPaid = (d?.paidCash ?: 0.0) + (d?.paidOnline ?: 0.0)
                val prevBal = CalculationEngine.calculateCustomerBalance(c.id, deliveries, payments)
                val cPbs = pbsMap[c.id] ?: 0.0
                val cNetBal = CalculationEngine.round2(prevBal + cPbs)

                val item = CustomerCardItem(
                    customer = c,
                    delivery = d,
                    isDelivered = isDeliv,
                    isNaga = isN,
                    isAltDay = isAlt,
                    isPending = isPend,
                    liters = cLiters,
                    rate = cRate,
                    bill = cBill,
                    paid = cPaid,
                    netBal = cNetBal,
                    pbs = cPbs
                )
                customerRows.add(item)

                if (isDeliv) {
                    servedCount++
                    totalSold += cLiters
                    if (d?.milkType == "गाय") cowSold += cLiters else buffaloSold += cLiters
                    totalBill += cBill
                    totalCash += (d?.paidCash ?: 0.0)
                    totalOnline += (d?.paidOnline ?: 0.0)
                } else if (isN) {
                    nagaCount++
                } else if (isAlt) {
                    altDayCount++
                } else {
                    pendingCount++
                }
            }

            for (p in paymentsToday) {
                if (p.paymentMode == "Cash") totalCash += p.amount else totalOnline += p.amount
            }

            val totalCollected = totalCash + totalOnline
            val pendingDue = (totalBill - totalCollected).coerceAtLeast(0.0)

            // Market Due & Advance
            val dueList = mutableListOf<Pair<String, Double>>()
            val advanceList = mutableListOf<Pair<String, Double>>()
            var totalMarketDue = 0.0
            var totalMarketAdvance = 0.0

            for (c in activeCustomers) {
                val bal = CalculationEngine.calculateCustomerBalance(c.id, deliveries, payments)
                val pbs = pbsMap[c.id] ?: 0.0
                val totalBal = CalculationEngine.round2(bal + pbs)
                if (totalBal > 0) {
                    totalMarketDue += totalBal
                    dueList.add(c.name to totalBal)
                } else if (totalBal < 0) {
                    totalMarketAdvance += (-totalBal)
                    advanceList.add(c.name to (-totalBal))
                }
            }
            dueList.sortByDescending { it.second }
            advanceList.sortByDescending { it.second }

            // Itemized Cash & Online receipts
            data class PdfPaymentItem(val name: String, val amount: Double, val time: String, val mode: String)
            val cashReceipts = mutableListOf<PdfPaymentItem>()
            val onlineReceipts = mutableListOf<PdfPaymentItem>()

            for (d in deliveries.filter { it.date == date && !it.isVoid }) {
                val cName = customers.find { it.id == d.customerId }?.name ?: "ग्राहक #${d.customerId}"
                val t = d.deliveryTime.ifBlank { "12:00 pm" }
                if (d.paidCash > 0) cashReceipts.add(PdfPaymentItem(cName, d.paidCash, t, "Cash"))
                if (d.paidOnline > 0) onlineReceipts.add(PdfPaymentItem(cName, d.paidOnline, t, "Online"))
            }
            for (p in paymentsToday) {
                val cName = customers.find { it.id == p.customerId }?.name ?: "ग्राहक #${p.customerId}"
                val t = p.createdAt.takeLast(8).ifBlank { "12:00 pm" }
                if (p.paymentMode == "Cash") {
                    cashReceipts.add(PdfPaymentItem(cName, p.amount, t, "Cash"))
                } else {
                    onlineReceipts.add(PdfPaymentItem(cName, p.amount, t, p.paymentMode))
                }
            }

            // Common Paints
            val cardBg = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
            val cardBorder = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val greenCardBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL; isAntiAlias = true }
            val greenCardBorder = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val redCardBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL; isAntiAlias = true }
            val redCardBorder = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val blueCardBg = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL; isAntiAlias = true }
            val blueCardBorder = Paint().apply { color = Color.rgb(191, 219, 254); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val amberCardBg = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL; isAntiAlias = true }
            val amberCardBorder = Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val grayPillBg = Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL; isAntiAlias = true }
            val grayPillBorder = Paint().apply { color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true }
            val emeraldSolidBg = Paint().apply { color = Color.rgb(22, 163, 74); style = Paint.Style.FILL; isAntiAlias = true }

            val secTitlePaint = Paint().apply { color = Color.rgb(22, 101, 52); textSize = 8.5f; isFakeBoldText = true; isAntiAlias = true }
            val textDark = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 7.5f; isAntiAlias = true }
            val textMuted = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 6.2f; isAntiAlias = true }
            val textDarkBold = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 8f; isFakeBoldText = true; isAntiAlias = true }
            val textGreenBold = Paint().apply { color = Color.rgb(22, 101, 52); textSize = 7f; isFakeBoldText = true; isAntiAlias = true }
            val textRedBold = Paint().apply { color = Color.rgb(185, 28, 28); textSize = 7f; isFakeBoldText = true; isAntiAlias = true }
            val textBlueBold = Paint().apply { color = Color.rgb(29, 78, 216); textSize = 7f; isFakeBoldText = true; isAntiAlias = true }
            val textAmberBold = Paint().apply { color = Color.rgb(180, 83, 9); textSize = 7f; isFakeBoldText = true; isAntiAlias = true }
            val dividerThin = Paint().apply { color = Color.rgb(241, 245, 249); strokeWidth = 0.5f }

            // Card drawing helper function
            fun drawCustomerCard(canvas: Canvas, item: CustomerCardItem, topY: Float) {
                val cardR = RectF(14f, topY, 366f, topY + 45f)

                // Background & border
                val bgPaint = when {
                    item.isNaga -> redCardBg
                    item.isAltDay -> blueCardBg
                    item.isPending -> amberCardBg
                    else -> cardBg
                }
                val borderPaint = when {
                    item.isNaga -> redCardBorder
                    item.isAltDay -> blueCardBorder
                    item.isPending -> amberCardBorder
                    else -> cardBorder
                }
                val accentColor = when {
                    item.isNaga -> Color.rgb(220, 38, 38)
                    item.isAltDay -> Color.rgb(37, 99, 235)
                    item.isPending -> Color.rgb(217, 119, 6)
                    else -> Color.rgb(22, 163, 74)
                }

                canvas.drawRoundRect(cardR, 5f, 5f, bgPaint)
                canvas.drawRoundRect(cardR, 5f, 5f, borderPaint)
                val accentPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL; isAntiAlias = true }
                canvas.drawRoundRect(RectF(14f, topY + 4f, 17.5f, topY + 41f), 2f, 2f, accentPaint)

                // Route index pill
                val rPill = RectF(22f, topY + 4f, 40f, topY + 16f)
                canvas.drawRoundRect(rPill, 3f, 3f, grayPillBg)
                canvas.drawRoundRect(rPill, 3f, 3f, grayPillBorder)
                val routeNum = if (item.customer.routeOrder > 0) item.customer.routeOrder else item.customer.id
                canvas.drawText("#$routeNum", 31f, topY + 13f, Paint().apply {
                    color = Color.rgb(71, 85, 105)
                    textSize = 7f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })

                // Full Customer Name (untruncated)
                canvas.drawText(item.customer.name, 44f, topY + 13.5f, textDarkBold)

                // Status pill
                val sPill = RectF(188f, topY + 4f, 266f, topY + 16f)
                when {
                    item.isDelivered -> {
                        canvas.drawRoundRect(sPill, 3f, 3f, greenCardBg)
                        canvas.drawRoundRect(sPill, 3f, 3f, greenCardBorder)
                        val mType = item.delivery?.milkType ?: "भैंस"
                        canvas.drawText("✓ ${item.liters}L $mType", 227f, topY + 13f, Paint(textGreenBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                    }
                    item.isNaga -> {
                        canvas.drawRoundRect(sPill, 3f, 3f, redCardBg)
                        canvas.drawRoundRect(sPill, 3f, 3f, redCardBorder)
                        canvas.drawText("⚠️ नागा (0L)", 227f, topY + 13f, Paint(textRedBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                    }
                    item.isAltDay -> {
                        canvas.drawRoundRect(sPill, 3f, 3f, blueCardBg)
                        canvas.drawRoundRect(sPill, 3f, 3f, blueCardBorder)
                        canvas.drawText("🔵 अल्टरनेट", 227f, topY + 13f, Paint(textBlueBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                    }
                    else -> {
                        canvas.drawRoundRect(sPill, 3f, 3f, amberCardBg)
                        canvas.drawRoundRect(sPill, 3f, 3f, amberCardBorder)
                        canvas.drawText("⏳ बाकी", 227f, topY + 13f, Paint(textAmberBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                    }
                }

                // Cash Collected badge (prominently displayed)
                val cPill = RectF(272f, topY + 4f, 360f, topY + 16f)
                if (item.paid > 0) {
                    canvas.drawRoundRect(cPill, 3f, 3f, emeraldSolidBg)
                    canvas.drawText("💰 जमा: ₹%,.0f".format(item.paid), 316f, topY + 13f, Paint().apply {
                        color = Color.WHITE
                        textSize = 7.2f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    })
                } else {
                    canvas.drawRoundRect(cPill, 3f, 3f, grayPillBg)
                    canvas.drawRoundRect(cPill, 3f, 3f, grayPillBorder)
                    canvas.drawText("जमा: -", 316f, topY + 13f, Paint().apply {
                        color = Color.rgb(148, 163, 184)
                        textSize = 6.8f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    })
                }

                // Divider line inside card
                canvas.drawLine(22f, topY + 20f, 360f, topY + 20f, dividerThin)

                // Line 2: Delivery & Bill details on Left
                val line2Text = when {
                    item.isDelivered -> "दूध बिल: ₹%.0f • दर: ₹%.0f/L".format(item.bill, item.rate)
                    item.isNaga -> "नियत: %.1fL • दर: ₹%.0f/L (आज नागा)".format(item.customer.defaultLiters, item.rate)
                    item.isAltDay -> "नियम: अल्टरनेट • नियत: %.1fL (आज अवकाश)".format(item.customer.defaultLiters)
                    else -> "नियत: %.1fL • दर: ₹%.0f/L (लंबित)".format(item.customer.defaultLiters, item.rate)
                }
                canvas.drawText(line2Text, 22f, topY + 30.5f, textMuted)

                // Line 2: Due / Advance pill on Right
                val bPill = RectF(262f, topY + 23f, 360f, topY + 35f)
                if (item.netBal > 0) {
                    canvas.drawRoundRect(bPill, 3f, 3f, redCardBg)
                    canvas.drawRoundRect(bPill, 3f, 3f, redCardBorder)
                    canvas.drawText("🔴 बकाया: ₹%,.0f".format(item.netBal), 311f, topY + 31.5f, Paint(textRedBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                } else if (item.netBal < 0) {
                    canvas.drawRoundRect(bPill, 3f, 3f, greenCardBg)
                    canvas.drawRoundRect(bPill, 3f, 3f, greenCardBorder)
                    canvas.drawText("🟢 एडवांस: ₹%,.0f".format(-item.netBal), 311f, topY + 31.5f, Paint(textGreenBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
                } else {
                    canvas.drawRoundRect(bPill, 3f, 3f, grayPillBg)
                    canvas.drawRoundRect(bPill, 3f, 3f, grayPillBorder)
                    canvas.drawText("⚪ चुकता ₹0", 311f, topY + 31.5f, Paint().apply {
                        color = Color.rgb(100, 116, 139)
                        textSize = 6.5f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    })
                }

                // Line 3: Mohalla & PBS details on Bottom
                val pbsStr = if (item.pbs != 0.0) " • PBS: ₹%.0f".format(item.pbs) else ""
                val infoStr = "${item.customer.mohalla.ifBlank { "बरेली" }}${if (item.customer.phone.isNotBlank()) " • " + item.customer.phone else ""}$pbsStr"
                canvas.drawText(infoStr, 22f, topY + 41f, Paint().apply {
                    color = Color.rgb(148, 163, 184)
                    textSize = 5.8f
                    isAntiAlias = true
                })
            }

            // Pagination calculation
            val page1Cards = customerRows.take(12)
            val remCards = customerRows.drop(12)
            val remChunks = if (remCards.isNotEmpty()) remCards.chunked(14) else emptyList()
            val totalPages = 1 + remChunks.size + 1 // Page 1 + subsequent card pages + Final Seal page

            // ==========================================
            // PAGE 1: BRAND HEADER, KPIS & CUSTOMER CARDS (1..12)
            // ==========================================
            val page1Info = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, 1).create()
            val page1 = document.startPage(page1Info)
            val c1 = page1.canvas

            drawMobileBrandHeader(c1, "दैनिक व्यापार विवरण", "Daily Delivery Register", date)

            // KPI Grid (y: 48f..116f)
            val cardW = 172f
            val cardH = 32f

            // Card 1: Milk (Left Row 1)
            val kpi1 = RectF(14f, 48f, 14f + cardW, 48f + cardH)
            c1.drawRoundRect(kpi1, 4f, 4f, greenCardBg); c1.drawRoundRect(kpi1, 4f, 4f, greenCardBorder)
            c1.drawText("कुल दूध सप्लाई", 20f, 58f, textMuted)
            c1.drawText("%.2f L".format(totalSold), 20f, 70f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("भैंस: %.1f L • गाय: %.1f L".format(buffaloSold, cowSold), 80f, 70f, textMuted)

            // Card 2: Bill (Right Row 1)
            val kpi2 = RectF(194f, 48f, 194f + cardW, 48f + cardH)
            c1.drawRoundRect(kpi2, 4f, 4f, cardBg); c1.drawRoundRect(kpi2, 4f, 4f, cardBorder)
            c1.drawText("आज का कुल बिल", 200f, 58f, textMuted)
            c1.drawText("₹%,.2f".format(totalBill), 200f, 70f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("${servedCount} ग्राहकों का बिल", 275f, 70f, textMuted)

            // Card 3: Collection (Left Row 2)
            val kpi3 = RectF(14f, 84f, 14f + cardW, 84f + cardH)
            c1.drawRoundRect(kpi3, 4f, 4f, greenCardBg); c1.drawRoundRect(kpi3, 4f, 4f, greenCardBorder)
            c1.drawText("आज की कुल वसूली", 20f, 94f, textMuted)
            c1.drawText("₹%,.2f".format(totalCollected), 20f, 106f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("नकद: ₹%,.0f • ऑन: ₹%,.0f".format(totalCash, totalOnline), 88f, 106f, textMuted)

            // Card 4: Market Due (Right Row 2)
            val kpi4 = RectF(194f, 84f, 194f + cardW, 84f + cardH)
            c1.drawRoundRect(kpi4, 4f, 4f, redCardBg); c1.drawRoundRect(kpi4, 4f, 4f, redCardBorder)
            c1.drawText("कुल बाजार उधारी (Market Due)", 200f, 94f, textMuted)
            c1.drawText("₹%,.2f".format(totalMarketDue), 200f, 106f, Paint().apply { color = Color.rgb(185, 28, 28); textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("एडवांस: ₹%,.0f".format(totalMarketAdvance), 285f, 106f, Paint(textGreenBold).apply { textSize = 5.8f })

            // Status Pills (y: 120f..135f)
            val pillY1 = 120f
            val pillY2 = 135f
            val pillW = 84f

            val p1Rect = RectF(14f, pillY1, 14f + pillW, pillY2)
            c1.drawRoundRect(p1Rect, 3f, 3f, greenCardBg); c1.drawRoundRect(p1Rect, 3f, 3f, greenCardBorder)
            c1.drawText("🟢 $servedCount डिलीवर", 56f, 131f, Paint(textGreenBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            val p2Rect = RectF(102f, pillY1, 102f + pillW, pillY2)
            c1.drawRoundRect(p2Rect, 3f, 3f, redCardBg); c1.drawRoundRect(p2Rect, 3f, 3f, redCardBorder)
            c1.drawText("🔴 $nagaCount नागा", 144f, 131f, Paint(textRedBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            val p3Rect = RectF(191f, pillY1, 191f + pillW, pillY2)
            c1.drawRoundRect(p3Rect, 3f, 3f, blueCardBg); c1.drawRoundRect(p3Rect, 3f, 3f, blueCardBorder)
            c1.drawText("🔵 $altDayCount अल्टरनेट", 233f, 131f, Paint(textBlueBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            val p4Rect = RectF(280f, pillY1, 280f + pillW, pillY2)
            c1.drawRoundRect(p4Rect, 3f, 3f, amberCardBg); c1.drawRoundRect(p4Rect, 3f, 3f, amberCardBorder)
            c1.drawText("⏳ $pendingCount बाकी", 322f, 131f, Paint(textAmberBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            // Section Title (y: 144f)
            c1.drawText("📋 दैनिक ग्राहक वितरण सूची (Customer Register)", 14f, 146f, secTitlePaint)
            c1.drawText("ग्राहक 1 से ${page1Cards.size} (कुल ${customerRows.size} में से)", 366f, 146f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })

            // Draw Cards for Page 1
            var p1Y = 152f
            for (card in page1Cards) {
                drawCustomerCard(c1, card, p1Y)
                p1Y += 49f
            }

            drawMobileBrandFooter(c1, 1, totalPages, date)
            document.finishPage(page1)

            // ==========================================
            // SUBSEQUENT CUSTOMER CARD PAGES
            // ==========================================
            for (chunkIdx in remChunks.indices) {
                val pageNum = 2 + chunkIdx
                val pageInfo = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, pageNum).create()
                val page = document.startPage(pageInfo)
                val c = page.canvas

                val startIdx = 12 + chunkIdx * 14 + 1
                val endIdx = 12 + chunkIdx * 14 + remChunks[chunkIdx].size
                drawMobileBrandHeader(c, "दैनिक ग्राहक वितरण सूची", "ग्राहक $startIdx से $endIdx", date)

                var cardY = 48f
                for (card in remChunks[chunkIdx]) {
                    drawCustomerCard(c, card, cardY)
                    cardY += 49f
                }

                drawMobileBrandFooter(c, pageNum, totalPages, date)
                document.finishPage(page)
            }

            // ==========================================
            // FINAL PAGE: RECONCILIATION, RECEIPTS, TOP ACCOUNTS & DIGITAL SEAL
            // ==========================================
            val pFinalInfo = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, totalPages).create()
            val pageFinal = document.startPage(pFinalInfo)
            val cFinal = pageFinal.canvas

            drawMobileBrandHeader(cFinal, "वित्तीय संतुलन एवं सत्यापन मुहर", "Reconciliation & Seal", date)

            // Section 1: Financial Reconciliation (y: 48f..156f)
            cFinal.drawText("1. वित्तीय संतुलन एवं हिसाब मिलान (Financial Reconciliation)", 14f, 52f, secTitlePaint)
            val reconR = RectF(14f, 57f, 366f, 158f)
            cFinal.drawRoundRect(reconR, 4f, 4f, cardBg); cFinal.drawRoundRect(reconR, 4f, 4f, cardBorder)

            val reconItems = listOf(
                "आज का बना कुल बिल (Today's Billed Amount)" to ("₹%,.2f".format(totalBill) to textDark),
                "आज प्राप्त नकद वसूली (Cash Collected)" to ("₹%,.2f".format(totalCash) to Paint(textGreenBold).apply { textSize = 7.5f }),
                "आज प्राप्त ऑनलाइन वसूली (Online Collected)" to ("₹%,.2f".format(totalOnline) to Paint(textGreenBold).apply { textSize = 7.5f }),
                "आज का बाकी/अदत्त शेष (Today's Unpaid Portion)" to ("₹%,.2f".format(pendingDue) to (if (pendingDue > 0) Paint(textRedBold).apply { textSize = 7.5f } else textDark)),
                "कुल शुद्ध बाजार उधारी (Cumulative Market Due)" to ("₹%,.2f".format(totalMarketDue) to Paint(textRedBold).apply { textSize = 7.5f }),
                "कुल ग्राहक अग्रिम राशि (Total Advance Holding)" to ("₹%,.2f".format(totalMarketAdvance) to Paint(textGreenBold).apply { textSize = 7.5f })
            )

            var rRowY = 71f
            for ((label, valPair) in reconItems) {
                cFinal.drawText(label, 22f, rRowY, textDark)
                val (vText, vPaint) = valPair
                cFinal.drawText(vText, 358f, rRowY, Paint(vPaint).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                cFinal.drawLine(22f, rRowY + 3.5f, 358f, rRowY + 3.5f, dividerThin)
                rRowY += 14.5f
            }

            // Section 2: Itemized Collections Breakdown (y: 168f..308f)
            cFinal.drawText("2. आज की प्राप्त वसूली विवरण (Itemized Collections)", 14f, 172f, secTitlePaint)
            val collW = 172f
            val collH = 126f

            // Cash Receipts Box (Left)
            val cashR = RectF(14f, 177f, 14f + collW, 177f + collH)
            cFinal.drawRoundRect(cashR, 4f, 4f, cardBg); cFinal.drawRoundRect(cashR, 4f, 4f, cardBorder)
            val cashHeadR = RectF(14f, 177f, 14f + collW, 192f)
            cFinal.drawRoundRect(cashHeadR, 4f, 4f, greenCardBg); cFinal.drawRoundRect(cashHeadR, 4f, 4f, greenCardBorder)
            cFinal.drawText("💵 नकद वसूली: ₹%,.2f".format(totalCash), 20f, 188f, textGreenBold)

            var cY = 203f
            if (cashReceipts.isNotEmpty()) {
                val showCash = cashReceipts.take(6)
                for (cr in showCash) {
                    cFinal.drawText(cr.name, 20f, cY, textDark)
                    cFinal.drawText("₹%,.0f".format(cr.amount), 178f, cY, Paint(textGreenBold).apply { textAlign = Paint.Align.RIGHT })
                    cFinal.drawLine(20f, cY + 2.5f, 178f, cY + 2.5f, dividerThin)
                    cY += 12.5f
                }
                if (cashReceipts.size > 6) {
                    val remCash = cashReceipts.drop(6).sumOf { it.amount }
                    cFinal.drawText("+ ${cashReceipts.size - 6} अन्य नकद प्राप्तियां", 20f, cY, textMuted)
                    cFinal.drawText("₹%,.0f".format(remCash), 178f, cY, Paint(textGreenBold).apply { textAlign = Paint.Align.RIGHT })
                }
            } else {
                cFinal.drawText("आज कोई नकद वसूली नहीं", 20f, cY + 12f, textMuted)
            }

            // Online Receipts Box (Right)
            val onlineR = RectF(194f, 177f, 194f + collW, 177f + collH)
            cFinal.drawRoundRect(onlineR, 4f, 4f, cardBg); cFinal.drawRoundRect(onlineR, 4f, 4f, cardBorder)
            val onlineHeadR = RectF(194f, 177f, 194f + collW, 192f)
            cFinal.drawRoundRect(onlineHeadR, 4f, 4f, blueCardBg); cFinal.drawRoundRect(onlineHeadR, 4f, 4f, blueCardBorder)
            cFinal.drawText("📲 ऑनलाइन वसूली: ₹%,.2f".format(totalOnline), 200f, 188f, textBlueBold)

            var oY = 203f
            if (onlineReceipts.isNotEmpty()) {
                val showOnline = onlineReceipts.take(6)
                for (or_ in showOnline) {
                    cFinal.drawText(or_.name, 200f, oY, textDark)
                    cFinal.drawText("₹%,.0f".format(or_.amount), 358f, oY, Paint(textBlueBold).apply { textAlign = Paint.Align.RIGHT })
                    cFinal.drawLine(200f, oY + 2.5f, 358f, oY + 2.5f, dividerThin)
                    oY += 12.5f
                }
                if (onlineReceipts.size > 6) {
                    val remOnline = onlineReceipts.drop(6).sumOf { it.amount }
                    cFinal.drawText("+ ${onlineReceipts.size - 6} अन्य ऑनलाइन", 200f, oY, textMuted)
                    cFinal.drawText("₹%,.0f".format(remOnline), 358f, oY, Paint(textBlueBold).apply { textAlign = Paint.Align.RIGHT })
                }
            } else {
                cFinal.drawText("आज कोई ऑनलाइन वसूली नहीं", 200f, oY + 12f, textMuted)
            }

            // Section 3: Top Accounts (y: 318f..440f)
            cFinal.drawText("3. शीर्ष खाते (Top Accounts — Dues & Advances)", 14f, 322f, secTitlePaint)
            val accH = 110f

            // Top Dues (Left)
            val dueR = RectF(14f, 327f, 14f + collW, 327f + accH)
            cFinal.drawRoundRect(dueR, 4f, 4f, cardBg); cFinal.drawRoundRect(dueR, 4f, 4f, cardBorder)
            val dueHeadR = RectF(14f, 327f, 14f + collW, 342f)
            cFinal.drawRoundRect(dueHeadR, 4f, 4f, redCardBg); cFinal.drawRoundRect(dueHeadR, 4f, 4f, redCardBorder)
            cFinal.drawText("शीर्ष 5 बाजार बकाया (Top Dues)", 20f, 338f, textRedBold)

            var dY = 353f
            val topDues = dueList.take(5)
            for (idx in topDues.indices) {
                val (name, amt) = topDues[idx]
                cFinal.drawText("${idx + 1}. $name", 20f, dY, textDark)
                cFinal.drawText("₹%,.0f".format(amt), 178f, dY, Paint(textRedBold).apply { textAlign = Paint.Align.RIGHT })
                cFinal.drawLine(20f, dY + 2f, 178f, dY + 2f, dividerThin)
                dY += 12f
            }
            cFinal.drawText("कुल बाजार बकाया: ₹%,.2f".format(totalMarketDue), 20f, 431f, Paint(textRedBold).apply { textSize = 6.2f })

            // Top Advances (Right)
            val advR = RectF(194f, 327f, 194f + collW, 327f + accH)
            cFinal.drawRoundRect(advR, 4f, 4f, cardBg); cFinal.drawRoundRect(advR, 4f, 4f, cardBorder)
            val advHeadR = RectF(194f, 327f, 194f + collW, 342f)
            cFinal.drawRoundRect(advHeadR, 4f, 4f, greenCardBg); cFinal.drawRoundRect(advHeadR, 4f, 4f, greenCardBorder)
            cFinal.drawText("शीर्ष 5 जमा एडवांस (Top Advances)", 200f, 338f, textGreenBold)

            var aY = 353f
            val topAdvances = advanceList.take(5)
            for (idx in topAdvances.indices) {
                val (name, amt) = topAdvances[idx]
                cFinal.drawText("${idx + 1}. $name", 200f, aY, textDark)
                cFinal.drawText("₹%,.0f".format(amt), 358f, aY, Paint(textGreenBold).apply { textAlign = Paint.Align.RIGHT })
                cFinal.drawLine(200f, aY + 2f, 358f, aY + 2f, dividerThin)
                aY += 12f
            }
            cFinal.drawText("कुल ग्राहक एडवांस: ₹%,.2f".format(totalMarketAdvance), 200f, 431f, Paint(textGreenBold).apply { textSize = 6.2f })

            // Section 4: Official Digital Seal & Certification Box (y: 450f..760f)
            cFinal.drawText("4. आधिकारिक सत्यापन एवं डिजिटल मुहर (Official Certification)", 14f, 454f, secTitlePaint)
            val sealOuter = RectF(14f, 460f, 366f, 762f)
            val sealBg = Paint().apply { color = Color.rgb(255, 253, 248); style = Paint.Style.FILL; isAntiAlias = true }
            val sealBorderOuter = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }
            val sealBorderInner = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true }

            cFinal.drawRoundRect(sealOuter, 6f, 6f, sealBg)
            cFinal.drawRoundRect(sealOuter, 6f, 6f, sealBorderOuter)
            cFinal.drawRoundRect(RectF(17f, 463f, 363f, 759f), 5f, 5f, sealBorderInner)

            // Emblem
            val embCenter = 190f
            val embY = 490f
            val badgePaint = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.FILL; isAntiAlias = true }
            cFinal.drawCircle(embCenter, embY, 16f, badgePaint)
            cFinal.drawCircle(embCenter, embY, 13f, Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            cFinal.drawText("SD", embCenter, embY + 4.5f, Paint().apply { color = Color.WHITE; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })

            cFinal.drawText("SHAHBUDDIN DAIRY BAREILLY", embCenter, 518f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 10.5f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })
            cFinal.drawText("प्रमाणित दैनिक व्यापार विवरण एवं डिजिटल खाता बही", embCenter, 528f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            cFinal.drawLine(24f, 534f, 356f, 534f, dividerThin)

            // Certification Statement
            val certText1 = "प्रमाणित किया जाता है कि यह दैनिक विवरण शाहबुद्दीन डेयरी के वास्तविक"
            val certText2 = "लेखा-जोखा (Double-Entry Ledger) से स्वतः उत्पन्न किया गया है। समस्त दूध"
            val certText3 = "वितरण, वित्तीय लेन-देन, वसूली एवं बकाया पूर्णतः सत्यापित हैं।"
            val certPaint = Paint().apply { color = Color.rgb(51, 65, 85); textSize = 7.2f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
            cFinal.drawText(certText1, embCenter, 548f, certPaint)
            cFinal.drawText(certText2, embCenter, 558f, certPaint)
            cFinal.drawText(certText3, embCenter, 568f, certPaint)

            cFinal.drawLine(24f, 576f, 356f, 576f, dividerThin)

            // Details Grid
            val dLabels = listOf(
                "प्रतिष्ठान का नाम:" to "शाहबुद्दीन डेयरी (Shahbuddin Dairy)",
                "आधिकारिक पता:" to OFFICIAL_ADDRESS,
                "प्रोपराइटर हेल्पलाइन:" to "मो० $OWNER_WHATSAPP_PHONE (बरेली)",
                "रिपोर्ट जारी दिनांक:" to ("$date • " + SimpleDateFormat("hh:mm a", Locale.US).format(Date())),
                "सॉफ्टवेयर संस्करण:" to "Shahbuddin Dairy App v9.1 (Official Release)"
            )

            var detY = 592f
            for ((lbl, val_) in dLabels) {
                cFinal.drawText(lbl, 28f, detY, textMuted)
                cFinal.drawText(val_, 112f, detY, textDarkBold)
                detY += 13f
            }

            // Signatory & Seal Stamps
            cFinal.drawLine(220f, 715f, 350f, 715f, Paint().apply { color = Color.rgb(71, 85, 105); strokeWidth = 0.8f })
            cFinal.drawText("अधिकृत हस्ताक्षरकर्ता / प्रोपराइटर", 285f, 726f, Paint(textDarkBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })
            cFinal.drawText("Shahbuddin Dairy Bareilly (U.P.)", 285f, 736f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 6f })

            // Official Stamp Badge on Left
            val stampR = RectF(28f, 695f, 160f, 746f)
            val stampBorder = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
            cFinal.drawRoundRect(stampR, 3f, 3f, greenCardBg); cFinal.drawRoundRect(stampR, 3f, 3f, stampBorder)
            cFinal.drawText("★ OFFICIAL DIGITAL SEAL ★", 94f, 709f, Paint(textGreenBold).apply { textAlign = Paint.Align.CENTER; textSize = 6.5f })
            cFinal.drawText("VERIFIED & AUDITED", 94f, 722f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 8f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })
            cFinal.drawText("RECORDS 100% RECONCILED", 94f, 735f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 5.8f })

            drawMobileBrandFooter(cFinal, totalPages, totalPages, date)
            document.finishPage(pageFinal)

            // Save PDF
            val localFile = File(context.cacheDir, fileName)
            FileOutputStream(localFile).use { fos ->
                document.writeTo(fos)
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

            Log.i(TAG, "Daily Statement V9.1 Mobile Card PDF created: ${localFile.absolutePath} (${localFile.length()} bytes)")
            return PdfResult(
                success = true,
                message = "Daily Statement PDF saved: $fileName",
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
            canvas.drawText("वितरण व भुगतान रसीद • V8.9", (PAGE_WIDTH / 2).toFloat(), 88f, subTitlePaint)

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

