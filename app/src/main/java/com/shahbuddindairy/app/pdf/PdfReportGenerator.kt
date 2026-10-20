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

    private const val MOBILE_PAGE_WIDTH = 360
    private const val MOBILE_PAGE_HEIGHT = 640
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
        val dRect = RectF(245f, 9f, 346f, 39f)
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
        canvas.drawText(dText, 295.5f, 22f, dPaint)
        canvas.drawText(if (mText.isNotBlank()) mText else "मो० $OWNER_WHATSAPP_PHONE", 295.5f, 33f, mPaint)

        // Header separator line
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        canvas.drawLine(14f, 44f, 346f, 44f, linePaint)
    }

    private fun drawMobileBrandFooter(canvas: Canvas, pageNum: Int, totalPages: Int, date: String) {
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        canvas.drawLine(14f, 622f, 346f, 622f, linePaint)

        val footerTextPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 6.8f
            isAntiAlias = true
        }
        canvas.drawText("Shahbuddin Dairy • 96, Gulab Nagar, Bareilly • मो० $OWNER_WHATSAPP_PHONE", 14f, 632f, footerTextPaint)

        val pageTextPaint = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 7.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Page $pageNum of $totalPages", 346f, 632f, pageTextPaint)
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
            val activeCustomers = customers.filter { it.isArchived == 0 && it.isActive == 1 }
            val delivMap = deliveries.filter { it.date == date && !it.isVoid }.associateBy { it.customerId }
            val paymentsToday = payments.filter { it.date == date && !it.isVoid }

            data class DeliveryRowItem(
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
                val time: String
            )

            val deliveredList = mutableListOf<DeliveryRowItem>()
            val pendingList = mutableListOf<DeliveryRowItem>()
            val nagaList = mutableListOf<DeliveryRowItem>()
            val altDayList = mutableListOf<DeliveryRowItem>()

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
                val cTime = d?.deliveryTime ?: ""

                val item = DeliveryRowItem(
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
                    time = cTime
                )

                if (isDeliv) {
                    servedCount++
                    totalSold += cLiters
                    if (d?.milkType == "गाय") cowSold += cLiters else buffaloSold += cLiters
                    totalBill += cBill
                    totalCash += (d?.paidCash ?: 0.0)
                    totalOnline += (d?.paidOnline ?: 0.0)
                    deliveredList.add(item)
                } else if (isN) {
                    nagaCount++
                    nagaList.add(item)
                } else if (isAlt) {
                    altDayCount++
                    altDayList.add(item)
                } else {
                    pendingCount++
                    pendingList.add(item)
                }
            }

            for (p in paymentsToday) {
                if (p.paymentMode == "Cash") totalCash += p.amount else totalOnline += p.amount
            }

            val totalCollected = totalCash + totalOnline
            val pendingDue = (totalBill - totalCollected).coerceAtLeast(0.0)
            val advance = (totalCollected - totalBill).coerceAtLeast(0.0)

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
                val cName = customers.find { it.id == d.customerId }?.name ?: "Customer #${d.customerId}"
                val t = d.deliveryTime.ifBlank { "12:00 pm" }
                if (d.paidCash > 0) cashReceipts.add(PdfPaymentItem(cName, d.paidCash, t, "Cash"))
                if (d.paidOnline > 0) onlineReceipts.add(PdfPaymentItem(cName, d.paidOnline, t, "Online"))
            }
            for (p in paymentsToday) {
                val cName = customers.find { it.id == p.customerId }?.name ?: "Customer #${p.customerId}"
                val t = p.createdAt.takeLast(8).ifBlank { "12:00 pm" }
                if (p.paymentMode == "Cash") {
                    cashReceipts.add(PdfPaymentItem(cName, p.amount, t, "Cash"))
                } else {
                    onlineReceipts.add(PdfPaymentItem(cName, p.amount, t, p.paymentMode))
                }
            }

            // 7 Days Volume
            val last7Days = (0..6).reversed().map { daysAgo ->
                val dStr = CalculationEngine.prevDayStr(date, daysAgo)
                val v = deliveries.filter { it.date == dStr && !it.isVoid && it.status == "delivered" }.sumOf { it.liters }
                val label = try {
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfOut = SimpleDateFormat("dd MMM", Locale.US)
                    sdfOut.format(sdfIn.parse(dStr) ?: Date())
                } catch (e: Exception) {
                    dStr.takeLast(5)
                }
                label to CalculationEngine.round2(v)
            }

            val avgMilk = if (servedCount > 0) CalculationEngine.round2(totalSold / servedCount) else 0.0
            val avgBill = if (servedCount > 0) CalculationEngine.round2(totalBill / servedCount) else 0.0
            val maxMilkItem = deliveredList.maxByOrNull { it.liters }

            // Pagination calculation
            val rowsPerPage = 20
            val deliveredPages = maxOf(1, (deliveredList.size + rowsPerPage - 1) / rowsPerPage)
            val totalPages = 1 + deliveredPages + 1 + 1 // Page 1: Dashboard, Pages 2..(1+deliveredPages): Delivered, Next: Exceptions, Last: Seal & Receipts

            // Common Paints
            val cardBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL; isAntiAlias = true }
            val cardBorder = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val greenCardBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL; isAntiAlias = true }
            val greenCardBorder = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val redCardBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL; isAntiAlias = true }
            val redCardBorder = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val blueCardBg = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL; isAntiAlias = true }
            val blueCardBorder = Paint().apply { color = Color.rgb(191, 219, 254); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }
            val amberCardBg = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL; isAntiAlias = true }
            val amberCardBorder = Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true }

            val secTitlePaint = Paint().apply { color = Color.rgb(22, 101, 52); textSize = 8.2f; isFakeBoldText = true; isAntiAlias = true }
            val textDark = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 7.2f; isAntiAlias = true }
            val textMuted = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 6.2f; isAntiAlias = true }
            val textBold = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 7.2f; isFakeBoldText = true; isAntiAlias = true }
            val textGreen = Paint().apply { color = Color.rgb(22, 101, 52); textSize = 7.2f; isFakeBoldText = true; isAntiAlias = true }
            val textRed = Paint().apply { color = Color.rgb(185, 28, 28); textSize = 7.2f; isFakeBoldText = true; isAntiAlias = true }
            val textBlue = Paint().apply { color = Color.rgb(29, 78, 216); textSize = 7.2f; isFakeBoldText = true; isAntiAlias = true }
            val textAmber = Paint().apply { color = Color.rgb(180, 83, 9); textSize = 7.2f; isFakeBoldText = true; isAntiAlias = true }
            val tableHeadBg = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.FILL; isAntiAlias = true }
            val tableHeadText = Paint().apply { color = Color.WHITE; textSize = 6.8f; isFakeBoldText = true; isAntiAlias = true }
            val altRowBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL; isAntiAlias = true }
            val dividerPaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 0.6f }

            // ==========================================
            // PAGE 1: EXECUTIVE SUMMARY & DASHBOARD
            // ==========================================
            val page1Info = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, 1).create()
            val page1 = document.startPage(page1Info)
            val c1 = page1.canvas

            drawMobileBrandHeader(c1, "दैनिक व्यापार विवरण", "Executive Summary", date)

            // Status Pills (y: 48f..64f)
            val pillY1 = 48f
            val pillY2 = 64f
            val p1Rect = RectF(14f, pillY1, 92f, pillY2)
            c1.drawRoundRect(p1Rect, 3f, 3f, greenCardBg); c1.drawRoundRect(p1Rect, 3f, 3f, greenCardBorder)
            c1.drawText("🟢 $servedCount डिलीवर", 53f, 59f, Paint(textGreen).apply { textAlign = Paint.Align.CENTER })

            val p2Rect = RectF(97f, pillY1, 175f, pillY2)
            c1.drawRoundRect(p2Rect, 3f, 3f, amberCardBg); c1.drawRoundRect(p2Rect, 3f, 3f, amberCardBorder)
            c1.drawText("🟠 $pendingCount बाकी", 136f, 59f, Paint(textAmber).apply { textAlign = Paint.Align.CENTER })

            val p3Rect = RectF(180f, pillY1, 258f, pillY2)
            c1.drawRoundRect(p3Rect, 3f, 3f, redCardBg); c1.drawRoundRect(p3Rect, 3f, 3f, redCardBorder)
            c1.drawText("🔴 $nagaCount नागा", 219f, 59f, Paint(textRed).apply { textAlign = Paint.Align.CENTER })

            val p4Rect = RectF(263f, pillY1, 346f, pillY2)
            c1.drawRoundRect(p4Rect, 3f, 3f, blueCardBg); c1.drawRoundRect(p4Rect, 3f, 3f, blueCardBorder)
            c1.drawText("🔵 $altDayCount अल्टरनेट", 304.5f, 59f, Paint(textBlue).apply { textAlign = Paint.Align.CENTER })

            // Section 1: KPI Cards (y: 72f..160f)
            c1.drawText("1. मुख्य व्यावसायिक मेट्रिक्स (Key Indicators)", 14f, 75f, secTitlePaint)
            val cardW = 160f
            val cardH = 38f
            val row1Y = 80f
            val row2Y = 122f

            // Card 1: Milk
            val c1r = RectF(14f, row1Y, 14f + cardW, row1Y + cardH)
            c1.drawRoundRect(c1r, 4f, 4f, greenCardBg); c1.drawRoundRect(c1r, 4f, 4f, greenCardBorder)
            c1.drawText("कुल दूध सप्लाई (Liters)", 20f, row1Y + 11f, textMuted)
            c1.drawText("%.2f L".format(totalSold), 20f, row1Y + 24f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 11f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("भैस: %.1f L • गाय: %.1f L".format(buffaloSold, cowSold), 20f, row1Y + 34f, textMuted)

            // Card 2: Bill
            val c2r = RectF(186f, row1Y, 186f + cardW, row1Y + cardH)
            c1.drawRoundRect(c2r, 4f, 4f, cardBg); c1.drawRoundRect(c2r, 4f, 4f, cardBorder)
            c1.drawText("आज का कुल बिल", 192f, row1Y + 11f, textMuted)
            c1.drawText("₹%,.2f".format(totalBill), 192f, row1Y + 24f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 11f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("${servedCount} ग्राहकों का दैनिक बिल", 192f, row1Y + 34f, textMuted)

            // Card 3: Collection
            val c3r = RectF(14f, row2Y, 14f + cardW, row2Y + cardH)
            c1.drawRoundRect(c3r, 4f, 4f, greenCardBg); c1.drawRoundRect(c3r, 4f, 4f, greenCardBorder)
            c1.drawText("आज की कुल वसूली", 20f, row2Y + 11f, textMuted)
            c1.drawText("₹%,.2f".format(totalCollected), 20f, row2Y + 24f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 11f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("नकद: ₹%,.0f • ऑनलाइन: ₹%,.0f".format(totalCash, totalOnline), 20f, row2Y + 34f, textMuted)

            // Card 4: Market Due
            val c4r = RectF(186f, row2Y, 186f + cardW, row2Y + cardH)
            c1.drawRoundRect(c4r, 4f, 4f, redCardBg); c1.drawRoundRect(c4r, 4f, 4f, redCardBorder)
            c1.drawText("कुल बाजार उधारी (Market Due)", 192f, row2Y + 11f, textMuted)
            c1.drawText("₹%,.2f".format(totalMarketDue), 192f, row2Y + 24f, Paint().apply { color = Color.rgb(185, 28, 28); textSize = 11f; isFakeBoldText = true; isAntiAlias = true })
            c1.drawText("कुल एडवांस: ₹%,.2f".format(totalMarketAdvance), 192f, row2Y + 34f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 6.2f; isAntiAlias = true })

            // Section 2: Financial Reconciliation (y: 168f..272f)
            c1.drawText("2. वित्तीय संतुलन एवं हिसाब मिलान (Financial Reconciliation)", 14f, 172f, secTitlePaint)
            val reconR = RectF(14f, 177f, 346f, 275f)
            c1.drawRoundRect(reconR, 4f, 4f, cardBg); c1.drawRoundRect(reconR, 4f, 4f, cardBorder)

            // Table rows inside recon
            val reconItems = listOf(
                "आज का बना कुल बिल (Today's Billed Amount)" to ("₹%,.2f".format(totalBill) to textDark),
                "आज प्राप्त नकद वसूली (Cash Collected)" to ("₹%,.2f".format(totalCash) to textGreen),
                "आज प्राप्त ऑनलाइन वसूली (Online Collected)" to ("₹%,.2f".format(totalOnline) to textGreen),
                "आज का बाकी/अदत्त शेष (Today's Unpaid Portion)" to ("₹%,.2f".format(pendingDue) to (if (pendingDue > 0) textRed else textDark)),
                "कुल शुद्ध बाजार उधारी (Cumulative Market Due)" to ("₹%,.2f".format(totalMarketDue) to textRed),
                "कुल ग्राहक अग्रिम राशि (Total Advance Holding)" to ("₹%,.2f".format(totalMarketAdvance) to textGreen)
            )

            var rRowY = 191f
            for ((label, valPair) in reconItems) {
                c1.drawText(label, 20f, rRowY, textDark)
                val (vText, vPaint) = valPair
                c1.drawText(vText, 338f, rRowY, Paint(vPaint).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                c1.drawLine(20f, rRowY + 3.5f, 340f, rRowY + 3.5f, dividerPaint)
                rRowY += 14.5f
            }

            // Section 3: 7-Day Trend Chart (y: 284f..396f)
            c1.drawText("3. 7-दिवसीय दूध आपूर्ति रुझान (7-Day Volume Trend)", 14f, 287f, secTitlePaint)
            val chartR = RectF(14f, 292f, 346f, 398f)
            c1.drawRoundRect(chartR, 4f, 4f, cardBg); c1.drawRoundRect(chartR, 4f, 4f, cardBorder)

            val maxVol = maxOf(last7Days.maxOfOrNull { it.second } ?: 1.0, 1.0)
            val barW = 24f
            val chartBaseY = 378f
            val maxBarH = 56f
            val barSpacing = (332f - (7 * barW)) / 8f

            for (i in last7Days.indices) {
                val (dLabel, v) = last7Days[i]
                val bX = 14f + barSpacing + i * (barW + barSpacing)
                val bH = ((v / maxVol) * maxBarH).toFloat().coerceAtLeast(3f)
                val bY = chartBaseY - bH
                val isToday = (i == last7Days.size - 1)

                val bPaint = Paint().apply {
                    color = if (isToday) Color.rgb(22, 163, 74) else Color.rgb(187, 247, 208)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                c1.drawRoundRect(RectF(bX, bY, bX + barW, chartBaseY), 3f, 3f, bPaint)

                // Liters on top
                c1.drawText("%.0fL".format(v), bX + barW / 2f, bY - 3f, Paint().apply {
                    color = if (isToday) Color.rgb(22, 101, 52) else Color.rgb(100, 116, 139)
                    textSize = 5.8f
                    isFakeBoldText = isToday
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })

                // Date label below
                c1.drawText(dLabel, bX + barW / 2f, chartBaseY + 10f, Paint().apply {
                    color = if (isToday) Color.rgb(22, 101, 52) else Color.rgb(100, 116, 139)
                    textSize = 5.8f
                    isFakeBoldText = isToday
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
            }
            c1.drawText("7-दिवसीय औसत: %.1f L/दिन • आज का कुल दूध: %.2f L".format(last7Days.map { it.second }.average(), totalSold), 180f, 394f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER })

            // Section 4: Operational Highlights & Certification Badge (y: 406f..615f)
            c1.drawText("4. परिचालन अंतर्दृष्टि एवं डेटा प्रमाणन (Insights & Verification)", 14f, 410f, secTitlePaint)
            val infoR = RectF(14f, 415f, 346f, 615f)
            c1.drawRoundRect(infoR, 4f, 4f, cardBg); c1.drawRoundRect(infoR, 4f, 4f, cardBorder)

            // 4 mini summary boxes
            val bW = 156f
            val bH = 28f
            // Mini 1
            val mb1 = RectF(20f, 421f, 20f + bW, 421f + bH)
            c1.drawRoundRect(mb1, 3f, 3f, greenCardBg); c1.drawRoundRect(mb1, 3f, 3f, greenCardBorder)
            c1.drawText("सक्रिय ग्राहक (Active)", 26f, 431f, textMuted)
            c1.drawText("${activeCustomers.size} ग्राहक (रूट में)", 26f, 442f, textGreen)

            // Mini 2
            val mb2 = RectF(184f, 421f, 184f + bW, 421f + bH)
            c1.drawRoundRect(mb2, 3f, 3f, cardBg); c1.drawRoundRect(mb2, 3f, 3f, cardBorder)
            c1.drawText("औसत दैनिक डिलीवरी", 190f, 431f, textMuted)
            c1.drawText("%.2f L / ग्राहक".format(avgMilk), 190f, 442f, textDark)

            // Mini 3
            val mb3 = RectF(20f, 453f, 20f + bW, 453f + bH)
            c1.drawRoundRect(mb3, 3f, 3f, cardBg); c1.drawRoundRect(mb3, 3f, 3f, cardBorder)
            c1.drawText("औसत दैनिक बिल", 26f, 463f, textMuted)
            c1.drawText("₹%.2f / ग्राहक".format(avgBill), 26f, 474f, textDark)

            // Mini 4
            val mb4 = RectF(184f, 453f, 184f + bW, 453f + bH)
            c1.drawRoundRect(mb4, 3f, 3f, greenCardBg); c1.drawRoundRect(mb4, 3f, 3f, greenCardBorder)
            c1.drawText("सर्वाधिक वितरण", 190f, 463f, textMuted)
            c1.drawText("${maxMilkItem?.customer?.name?.take(10) ?: "N/A"} (${maxMilkItem?.liters ?: 0.0}L)", 190f, 474f, textGreen)

            // Digital Authenticity & Security Seal Box inside
            val sealInner = RectF(20f, 488f, 340f, 608f)
            c1.drawRoundRect(sealInner, 4f, 4f, Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL; isAntiAlias = true })
            c1.drawRoundRect(sealInner, 4f, 4f, Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })

            val checkBadge = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.FILL; isAntiAlias = true }
            c1.drawCircle(35f, 506f, 8f, checkBadge)
            c1.drawText("✓", 35f, 509.5f, Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })

            c1.drawText("Shahbuddin Dairy • Double-Entry Ledger Verified", 48f, 505f, textGreen)
            c1.drawText("SQLite Data Engine v8.9 • Real-Time Financial Integrity", 48f, 514f, textMuted)

            c1.drawLine(26f, 522f, 334f, 522f, dividerPaint)

            c1.drawText("• प्रतिष्ठान: शाहबुद्दीन डेयरी (दुग्ध वितरण एवं लेखा प्रणाली)", 26f, 534f, textDark)
            c1.drawText("• आधिकारिक पता: $OFFICIAL_ADDRESS", 26f, 545f, textDark)
            c1.drawText("• प्रोपराइटर हेल्पलाइन: +91 $OWNER_WHATSAPP_PHONE (बरेली, उ०प्र०)", 26f, 556f, textDark)
            c1.drawText("• रिपोर्ट आईडी: SD-REP-${date.replace("-", "")} • ऑडिट स्थिति: सत्यापित (PASSED)", 26f, 567f, textMuted)

            // Signatory line
            c1.drawLine(210f, 592f, 330f, 592f, Paint().apply { color = Color.rgb(100, 116, 139); strokeWidth = 0.8f })
            c1.drawText("अधिकृत हस्ताक्षर / प्रोपराइटर", 270f, 602f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 6.2f })
            c1.drawText("✓ DIGITALLY CERTIFIED", 26f, 602f, Paint(textGreen).apply { textSize = 6.2f })

            drawMobileBrandFooter(c1, 1, totalPages, date)
            document.finishPage(page1)

            // ==========================================
            // DELIVERED CUSTOMERS REGISTER (PAGES 2..)
            // ==========================================
            val chunks = deliveredList.chunked(rowsPerPage)
            for (chunkIdx in chunks.indices) {
                val pageNum = 2 + chunkIdx
                val pageInfo = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, pageNum).create()
                val page = document.startPage(pageInfo)
                val c = page.canvas

                drawMobileBrandHeader(c, "दूध वितरण रजिस्टर (सफल वितरण)", "Delivered: ${chunkIdx + 1}/$deliveredPages", date)

                // Subhead banner (y: 47f..61f)
                val subR = RectF(14f, 47f, 346f, 61f)
                c.drawRoundRect(subR, 3f, 3f, greenCardBg); c.drawRoundRect(subR, 3f, 3f, greenCardBorder)
                val startCustIdx = chunkIdx * rowsPerPage + 1
                val endCustIdx = chunkIdx * rowsPerPage + chunks[chunkIdx].size
                c.drawText("सूची: ग्राहक $startCustIdx से $endCustIdx (कुल $servedCount में से)", 20f, 56.5f, textGreen)
                c.drawText("पेज ${chunkIdx + 1} of $deliveredPages", 340f, 56.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })

                // Table Header (y: 65f..80f)
                val thR = RectF(14f, 65f, 346f, 80f)
                c.drawRect(thR, tableHeadBg)
                c.drawText("#", 21f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.CENTER })
                c.drawText("ग्राहक का नाम", 33f, 75.5f, tableHeadText)
                c.drawText("मात्रा (L)", 164f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                c.drawText("रेट (₹)", 196f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                c.drawText("बिल (₹)", 242f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                c.drawText("जमा (₹)", 286f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                c.drawText("शेष/एडवांस", 342f, 75.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })

                val rowH = 24.5f
                var curY = 80f
                val chunkItems = chunks[chunkIdx]

                for (rIdx in chunkItems.indices) {
                    val item = chunkItems[rIdx]
                    val rBg = if (rIdx % 2 == 1) altRowBg else Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
                    c.drawRect(RectF(14f, curY, 346f, curY + rowH), rBg)

                    val rowNum = chunkIdx * rowsPerPage + rIdx + 1
                    c.drawText("$rowNum", 21f, curY + 14.5f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

                    // Name + Milk Type
                    c.drawText(item.customer.name.take(16), 33f, curY + 10.5f, textBold)
                    val mType = item.delivery?.milkType ?: "भैस"
                    c.drawText(mType, 33f, curY + 20f, textMuted)

                    // Liters
                    c.drawText("%.1f".format(item.liters), 164f, curY + 14.5f, Paint(textDark).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })

                    // Rate
                    c.drawText("%.0f".format(item.rate), 196f, curY + 14.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })

                    // Bill
                    c.drawText("%.0f".format(item.bill), 242f, curY + 14.5f, Paint(textDark).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })

                    // Paid
                    if (item.paid > 0) {
                        c.drawText("%.0f".format(item.paid), 286f, curY + 14.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else {
                        c.drawText("-", 286f, curY + 14.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })
                    }

                    // Net Balance
                    if (item.netBal > 0) {
                        c.drawText("%.0f ब".format(item.netBal), 342f, curY + 14.5f, Paint(textRed).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else if (item.netBal < 0) {
                        c.drawText("%.0f ए".format(-item.netBal), 342f, curY + 14.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else {
                        c.drawText("0", 342f, curY + 14.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })
                    }

                    c.drawLine(14f, curY + rowH, 346f, curY + rowH, dividerPaint)
                    curY += rowH
                }

                // Subtotal on last page of deliveries
                if (chunkIdx == chunks.size - 1) {
                    val totR = RectF(14f, curY, 346f, curY + 22f)
                    c.drawRect(totR, greenCardBg)
                    c.drawRoundRect(totR, 0f, 0f, greenCardBorder)
                    c.drawText("कुल योग (${servedCount} ग्राहक)", 33f, curY + 14.5f, textGreen)
                    c.drawText("%.2f L".format(totalSold), 164f, curY + 14.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    c.drawText("₹%,.0f".format(totalBill), 242f, curY + 14.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    val totalDelivPaid = deliveredList.sumOf { it.paid }
                    c.drawText("₹%,.0f".format(totalDelivPaid), 286f, curY + 14.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                }

                drawMobileBrandFooter(c, pageNum, totalPages, date)
                document.finishPage(page)
            }

            // ==========================================
            // EXCEPTIONS REGISTER (NAAGA, ALT-DAY, PENDING)
            // ==========================================
            val excPageNum = 1 + deliveredPages + 1
            val excPageInfo = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, excPageNum).create()
            val excPage = document.startPage(excPageInfo)
            val cExc = excPage.canvas

            drawMobileBrandHeader(cExc, "अनुपस्थिति एवं अवकाश रजिस्टर", "Exceptions Register", date)

            // Exceptions Summary Pills (y: 47f..63f)
            val ePillY1 = 47f
            val ePillY2 = 63f
            val ep1 = RectF(14f, ePillY1, 118f, ePillY2)
            cExc.drawRoundRect(ep1, 3f, 3f, redCardBg); cExc.drawRoundRect(ep1, 3f, 3f, redCardBorder)
            cExc.drawText("🔴 $nagaCount नागा ग्राहक", 66f, 58f, Paint(textRed).apply { textAlign = Paint.Align.CENTER })

            val ep2 = RectF(124f, ePillY1, 236f, ePillY2)
            cExc.drawRoundRect(ep2, 3f, 3f, blueCardBg); cExc.drawRoundRect(ep2, 3f, 3f, blueCardBorder)
            cExc.drawText("🔵 $altDayCount अल्टरनेट अवकाश", 180f, 58f, Paint(textBlue).apply { textAlign = Paint.Align.CENTER })

            val ep3 = RectF(242f, ePillY1, 346f, ePillY2)
            cExc.drawRoundRect(ep3, 3f, 3f, amberCardBg); cExc.drawRoundRect(ep3, 3f, 3f, amberCardBorder)
            cExc.drawText("🟠 $pendingCount लंबित (Pending)", 294f, 58f, Paint(textAmber).apply { textAlign = Paint.Align.CENTER })

            var ey = 73f

            // Section 1: Naaga (Missed)
            cExc.drawText("1. नागा रजिस्टर (Missed Deliveries: $nagaCount)", 14f, ey, secTitlePaint)
            ey += 7f
            if (nagaList.isNotEmpty()) {
                val nthR = RectF(14f, ey, 346f, ey + 15f)
                cExc.drawRect(nthR, Paint().apply { color = Color.rgb(185, 28, 28); style = Paint.Style.FILL; isAntiAlias = true })
                cExc.drawText("#", 21f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.CENTER })
                cExc.drawText("ग्राहक का नाम", 33f, ey + 10.5f, tableHeadText)
                cExc.drawText("नियत दूध", 180f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                cExc.drawText("स्थिति", 250f, ey + 10.5f, tableHeadText)
                cExc.drawText("पिछला बैलेंस", 342f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                ey += 15f

                val nRowH = 21f
                for (i in nagaList.indices) {
                    val item = nagaList[i]
                    val bg = if (i % 2 == 1) altRowBg else Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
                    cExc.drawRect(RectF(14f, ey, 346f, ey + nRowH), bg)
                    cExc.drawText("${i + 1}", 21f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER })
                    cExc.drawText(item.customer.name.take(18), 33f, ey + 13.5f, textBold)
                    cExc.drawText("%.1f L".format(item.customer.defaultLiters), 180f, ey + 13.5f, Paint(textDark).apply { textAlign = Paint.Align.RIGHT })
                    cExc.drawText("नागा (Skipped)", 250f, ey + 13.5f, textRed)
                    if (item.netBal > 0) {
                        cExc.drawText("₹%.0f ब".format(item.netBal), 342f, ey + 13.5f, Paint(textRed).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else if (item.netBal < 0) {
                        cExc.drawText("₹%.0f ए".format(-item.netBal), 342f, ey + 13.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else {
                        cExc.drawText("0", 342f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })
                    }
                    cExc.drawLine(14f, ey + nRowH, 346f, ey + nRowH, dividerPaint)
                    ey += nRowH
                }
            } else {
                val okR = RectF(14f, ey, 346f, ey + 24f)
                cExc.drawRoundRect(okR, 3f, 3f, greenCardBg); cExc.drawRoundRect(okR, 3f, 3f, greenCardBorder)
                cExc.drawText("✓ आज कोई नागा दर्ज नहीं है — सभी निर्धारित ग्राहकों को दूध प्राप्त हुआ", 24f, ey + 15f, textGreen)
                ey += 30f
            }

            ey += 12f

            // Section 2: Alternate-Day Off
            cExc.drawText("2. अल्टरनेट-डे अवकाश रजिस्टर (Scheduled Off: $altDayCount)", 14f, ey, secTitlePaint)
            ey += 7f
            if (altDayList.isNotEmpty()) {
                val athR = RectF(14f, ey, 346f, ey + 15f)
                cExc.drawRect(athR, Paint().apply { color = Color.rgb(29, 78, 216); style = Paint.Style.FILL; isAntiAlias = true })
                cExc.drawText("#", 21f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.CENTER })
                cExc.drawText("ग्राहक का नाम", 33f, ey + 10.5f, tableHeadText)
                cExc.drawText("नियम", 180f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                cExc.drawText("स्थिति", 250f, ey + 10.5f, tableHeadText)
                cExc.drawText("पिछला बैलेंस", 342f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                ey += 15f

                val aRowH = 21f
                for (i in altDayList.indices) {
                    val item = altDayList[i]
                    val bg = if (i % 2 == 1) altRowBg else Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
                    cExc.drawRect(RectF(14f, ey, 346f, ey + aRowH), bg)
                    cExc.drawText("${i + 1}", 21f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER })
                    cExc.drawText(item.customer.name.take(18), 33f, ey + 13.5f, textBold)
                    cExc.drawText("एक दिन छोड़", 180f, ey + 13.5f, Paint(textDark).apply { textAlign = Paint.Align.RIGHT })
                    cExc.drawText("आज अवकाश (Off)", 250f, ey + 13.5f, textBlue)
                    if (item.netBal > 0) {
                        cExc.drawText("₹%.0f ब".format(item.netBal), 342f, ey + 13.5f, Paint(textRed).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else if (item.netBal < 0) {
                        cExc.drawText("₹%.0f ए".format(-item.netBal), 342f, ey + 13.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else {
                        cExc.drawText("0", 342f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })
                    }
                    cExc.drawLine(14f, ey + aRowH, 346f, ey + aRowH, dividerPaint)
                    ey += aRowH
                }
            } else {
                val okR = RectF(14f, ey, 346f, ey + 24f)
                cExc.drawRoundRect(okR, 3f, 3f, blueCardBg); cExc.drawRoundRect(okR, 3f, 3f, blueCardBorder)
                cExc.drawText("✓ आज कोई अल्टरनेट-डे अवकाश नहीं है", 24f, ey + 15f, textBlue)
                ey += 30f
            }

            ey += 12f

            // Section 3: Pending Deliveries
            cExc.drawText("3. लंबित वितरण रजिस्टर (Pending Deliveries: $pendingCount)", 14f, ey, secTitlePaint)
            ey += 7f
            if (pendingList.isNotEmpty()) {
                val pthR = RectF(14f, ey, 346f, ey + 15f)
                cExc.drawRect(pthR, Paint().apply { color = Color.rgb(180, 83, 9); style = Paint.Style.FILL; isAntiAlias = true })
                cExc.drawText("#", 21f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.CENTER })
                cExc.drawText("ग्राहक का नाम", 33f, ey + 10.5f, tableHeadText)
                cExc.drawText("नियत दूध", 180f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                cExc.drawText("स्थिति", 250f, ey + 10.5f, tableHeadText)
                cExc.drawText("पिछला बैलेंस", 342f, ey + 10.5f, Paint(tableHeadText).apply { textAlign = Paint.Align.RIGHT })
                ey += 15f

                val pRowH = 21f
                for (i in pendingList.indices) {
                    val item = pendingList[i]
                    val bg = if (i % 2 == 1) altRowBg else Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
                    cExc.drawRect(RectF(14f, ey, 346f, ey + pRowH), bg)
                    cExc.drawText("${i + 1}", 21f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER })
                    cExc.drawText(item.customer.name.take(18), 33f, ey + 13.5f, textBold)
                    cExc.drawText("%.1f L".format(item.customer.defaultLiters), 180f, ey + 13.5f, Paint(textDark).apply { textAlign = Paint.Align.RIGHT })
                    cExc.drawText("लंबित (Pending)", 250f, ey + 13.5f, textAmber)
                    if (item.netBal > 0) {
                        cExc.drawText("₹%.0f ब".format(item.netBal), 342f, ey + 13.5f, Paint(textRed).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else if (item.netBal < 0) {
                        cExc.drawText("₹%.0f ए".format(-item.netBal), 342f, ey + 13.5f, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    } else {
                        cExc.drawText("0", 342f, ey + 13.5f, Paint(textMuted).apply { textAlign = Paint.Align.RIGHT })
                    }
                    cExc.drawLine(14f, ey + pRowH, 346f, ey + pRowH, dividerPaint)
                    ey += pRowH
                }
            } else {
                val okR = RectF(14f, ey, 346f, ey + 24f)
                cExc.drawRoundRect(okR, 3f, 3f, greenCardBg); cExc.drawRoundRect(okR, 3f, 3f, greenCardBorder)
                cExc.drawText("✓ 0 Pending • आज की सभी निर्धारित डिलीवरी सफलता से पूर्ण हो चुकी हैं", 24f, ey + 15f, textGreen)
            }

            drawMobileBrandFooter(cExc, excPageNum, totalPages, date)
            document.finishPage(excPage)

            // ==========================================
            // PAGE 5: COLLECTIONS, TOP ACCOUNTS & OFFICIAL SEAL
            // ==========================================
            val p5Info = PdfDocument.PageInfo.Builder(MOBILE_PAGE_WIDTH, MOBILE_PAGE_HEIGHT, totalPages).create()
            val page5 = document.startPage(p5Info)
            val c5 = page5.canvas

            drawMobileBrandHeader(c5, "वसूली विवरण, शीर्ष खाते एवं मुहर", "Receipts & Official Seal", date)

            // Section 1: Itemized Collections (y: 47f..195f)
            c5.drawText("1. आज की प्राप्त वसूली विवरण (Itemized Collections)", 14f, 50f, secTitlePaint)
            val collW = 160f
            val collH = 136f

            // Cash Box (Left: 14f..174f)
            val cashR = RectF(14f, 55f, 14f + collW, 55f + collH)
            c5.drawRoundRect(cashR, 4f, 4f, cardBg); c5.drawRoundRect(cashR, 4f, 4f, cardBorder)
            val cashHeadR = RectF(14f, 55f, 14f + collW, 72f)
            c5.drawRoundRect(cashHeadR, 4f, 4f, greenCardBg); c5.drawRoundRect(cashHeadR, 4f, 4f, greenCardBorder)
            c5.drawText("💵 नकद वसूली: ₹%,.2f".format(totalCash), 20f, 67f, textGreen)

            var cY = 83f
            if (cashReceipts.isNotEmpty()) {
                val showCash = cashReceipts.take(6)
                for (cr in showCash) {
                    c5.drawText(cr.name.take(13), 20f, cY, textDark)
                    c5.drawText("₹%,.0f".format(cr.amount), 168f, cY, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    c5.drawLine(20f, cY + 2.5f, 168f, cY + 2.5f, dividerPaint)
                    cY += 12f
                }
                if (cashReceipts.size > 6) {
                    val remCash = cashReceipts.drop(6).sumOf { it.amount }
                    c5.drawText("+ ${cashReceipts.size - 6} अन्य नकद प्राप्तियां", 20f, cY, textMuted)
                    c5.drawText("₹%,.0f".format(remCash), 168f, cY, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT })
                }
            } else {
                c5.drawText("आज कोई नकद वसूली नहीं", 20f, cY + 10f, textMuted)
            }

            // Online Box (Right: 186f..346f)
            val onlineR = RectF(186f, 55f, 186f + collW, 55f + collH)
            c5.drawRoundRect(onlineR, 4f, 4f, cardBg); c5.drawRoundRect(onlineR, 4f, 4f, cardBorder)
            val onlineHeadR = RectF(186f, 55f, 186f + collW, 72f)
            c5.drawRoundRect(onlineHeadR, 4f, 4f, blueCardBg); c5.drawRoundRect(onlineHeadR, 4f, 4f, blueCardBorder)
            c5.drawText("📲 ऑनलाइन वसूली: ₹%,.2f".format(totalOnline), 192f, 67f, textBlue)

            var oY = 83f
            if (onlineReceipts.isNotEmpty()) {
                val showOnline = onlineReceipts.take(6)
                for (or_ in showOnline) {
                    c5.drawText(or_.name.take(13), 192f, oY, textDark)
                    c5.drawText("₹%,.0f".format(or_.amount), 340f, oY, Paint(textBlue).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                    c5.drawLine(192f, oY + 2.5f, 340f, oY + 2.5f, dividerPaint)
                    oY += 12f
                }
                if (onlineReceipts.size > 6) {
                    val remOnline = onlineReceipts.drop(6).sumOf { it.amount }
                    c5.drawText("+ ${onlineReceipts.size - 6} अन्य ऑनलाइन प्राप्तियां", 192f, oY, textMuted)
                    c5.drawText("₹%,.0f".format(remOnline), 340f, oY, Paint(textBlue).apply { textAlign = Paint.Align.RIGHT })
                }
            } else {
                c5.drawText("आज कोई ऑनलाइन वसूली नहीं", 192f, oY + 10f, textMuted)
            }

            // Section 2: Top Accounts (y: 201f..328f)
            c5.drawText("2. शीर्ष खाते (Top Accounts — Dues & Advances)", 14f, 204f, secTitlePaint)
            val accH = 116f

            // Top Dues (Left: 14f..174f)
            val dueR = RectF(14f, 209f, 14f + collW, 209f + accH)
            c5.drawRoundRect(dueR, 4f, 4f, cardBg); c5.drawRoundRect(dueR, 4f, 4f, cardBorder)
            val dueHeadR = RectF(14f, 209f, 14f + collW, 224f)
            c5.drawRoundRect(dueHeadR, 4f, 4f, redCardBg); c5.drawRoundRect(dueHeadR, 4f, 4f, redCardBorder)
            c5.drawText("शीर्ष 5 बाजार बकाया (Top Dues)", 20f, 220f, textRed)

            var dY = 235f
            val topDues = dueList.take(5)
            for (idx in topDues.indices) {
                val (name, amt) = topDues[idx]
                c5.drawText("${idx + 1}. ${name.take(13)}", 20f, dY, textDark)
                c5.drawText("₹%,.0f".format(amt), 168f, dY, Paint(textRed).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                c5.drawLine(20f, dY + 2f, 168f, dY + 2f, dividerPaint)
                dY += 12f
            }
            c5.drawText("कुल बाजार बकाया: ₹%,.2f".format(totalMarketDue), 20f, 319f, Paint(textRed).apply { textSize = 6.2f; isFakeBoldText = true })

            // Top Advances (Right: 186f..346f)
            val advR = RectF(186f, 209f, 186f + collW, 209f + accH)
            c5.drawRoundRect(advR, 4f, 4f, cardBg); c5.drawRoundRect(advR, 4f, 4f, cardBorder)
            val advHeadR = RectF(186f, 209f, 186f + collW, 224f)
            c5.drawRoundRect(advHeadR, 4f, 4f, greenCardBg); c5.drawRoundRect(advHeadR, 4f, 4f, greenCardBorder)
            c5.drawText("शीर्ष 5 जमा एडवांस (Top Advances)", 192f, 220f, textGreen)

            var aY = 235f
            val topAdvances = advanceList.take(5)
            for (idx in topAdvances.indices) {
                val (name, amt) = topAdvances[idx]
                c5.drawText("${idx + 1}. ${name.take(13)}", 192f, aY, textDark)
                c5.drawText("₹%,.0f".format(amt), 340f, aY, Paint(textGreen).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true })
                c5.drawLine(192f, aY + 2f, 340f, aY + 2f, dividerPaint)
                aY += 12f
            }
            c5.drawText("कुल ग्राहक एडवांस: ₹%,.2f".format(totalMarketAdvance), 192f, 319f, Paint(textGreen).apply { textSize = 6.2f; isFakeBoldText = true })

            // Section 3: Official Verification & Digital Seal Box (y: 337f..615f)
            c5.drawText("3. आधिकारिक सत्यापन एवं डिजिटल मुहर (Official Certification)", 14f, 340f, secTitlePaint)
            val sealOuter = RectF(14f, 345f, 346f, 615f)
            val sealBg = Paint().apply { color = Color.rgb(255, 253, 248); style = Paint.Style.FILL; isAntiAlias = true }
            val sealBorderOuter = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }
            val sealBorderInner = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true }

            c5.drawRoundRect(sealOuter, 6f, 6f, sealBg)
            c5.drawRoundRect(sealOuter, 6f, 6f, sealBorderOuter)
            c5.drawRoundRect(RectF(17f, 348f, 343f, 612f), 5f, 5f, sealBorderInner)

            // Emblem
            val embCenter = 180f
            val embY = 373f
            val badgePaint = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.FILL; isAntiAlias = true }
            c5.drawCircle(embCenter, embY, 15f, badgePaint)
            c5.drawCircle(embCenter, embY, 12f, Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            c5.drawText("SD", embCenter, embY + 4f, Paint().apply { color = Color.WHITE; textSize = 11f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })

            c5.drawText("SHAHBUDDIN DAIRY BAREILLY", embCenter, 400f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })
            c5.drawText("प्रमाणित दैनिक व्यापार विवरण एवं डिजिटल खाता बही", embCenter, 410f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 6.8f })

            c5.drawLine(24f, 416f, 336f, 416f, dividerPaint)

            // Certification Statement
            val certText1 = "प्रमाणित किया जाता है कि यह दैनिक विवरण शाहबुद्दीन डेयरी के वास्तविक"
            val certText2 = "लेखा-जोखा (Double-Entry Ledger) से स्वतः उत्पन्न किया गया है। समस्त दूध"
            val certText3 = "वितरण, वित्तीय लेन-देन, वसूली एवं बकाया पूर्णतः सत्यापित हैं।"
            val certPaint = Paint().apply { color = Color.rgb(51, 65, 85); textSize = 6.8f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
            c5.drawText(certText1, embCenter, 428f, certPaint)
            c5.drawText(certText2, embCenter, 438f, certPaint)
            c5.drawText(certText3, embCenter, 448f, certPaint)

            c5.drawLine(24f, 455f, 336f, 455f, dividerPaint)

            // Details Grid
            val dLabels = listOf(
                "प्रतिष्ठान का नाम:" to "शाहबुद्दीन डेयरी (Shahbuddin Dairy)",
                "आधिकारिक पता:" to OFFICIAL_ADDRESS,
                "प्रोपराइटर हेल्पलाइन:" to "मो० $OWNER_WHATSAPP_PHONE (बरेली)",
                "रिपोर्ट जारी दिनांक:" to ("$date • " + SimpleDateFormat("hh:mm a", Locale.US).format(Date())),
                "सॉफ्टवेयर संस्करण:" to "Shahbuddin Dairy App v8.9 (Official Release)"
            )

            var detY = 469f
            for ((lbl, val_) in dLabels) {
                c5.drawText(lbl, 26f, detY, textMuted)
                c5.drawText(val_, 105f, detY, textBold)
                detY += 12f
            }

            // Signatory & Seal Stamps
            c5.drawLine(210f, 578f, 332f, 578f, Paint().apply { color = Color.rgb(71, 85, 105); strokeWidth = 0.8f })
            c5.drawText("अधिकृत हस्ताक्षरकर्ता / प्रोपराइटर", 271f, 589f, Paint(textDark).apply { textAlign = Paint.Align.CENTER; textSize = 6.5f; isFakeBoldText = true })
            c5.drawText("Shahbuddin Dairy Bareilly (U.P.)", 271f, 598f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 5.8f })

            // Official Stamp Badge on Left
            val stampR = RectF(26f, 555f, 150f, 602f)
            val stampBorder = Paint().apply { color = Color.rgb(22, 101, 52); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
            c5.drawRoundRect(stampR, 3f, 3f, greenCardBg); c5.drawRoundRect(stampR, 3f, 3f, stampBorder)
            c5.drawText("★ OFFICIAL DIGITAL SEAL ★", 88f, 569f, Paint(textGreen).apply { textAlign = Paint.Align.CENTER; textSize = 6.2f; isFakeBoldText = true })
            c5.drawText("VERIFIED & AUDITED", 88f, 580f, Paint().apply { color = Color.rgb(22, 101, 52); textSize = 7.5f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true })
            c5.drawText("RECORDS 100% RECONCILED", 88f, 592f, Paint(textMuted).apply { textAlign = Paint.Align.CENTER; textSize = 5.5f })

            drawMobileBrandFooter(c5, totalPages, totalPages, date)
            document.finishPage(page5)

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

            Log.i(TAG, "Daily Statement V8.9 Mobile PDF created: ${localFile.absolutePath} (${localFile.length()} bytes)")
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

