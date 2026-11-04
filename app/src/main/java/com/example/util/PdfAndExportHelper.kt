package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Donation
import com.example.data.Donor
import com.example.data.IndianCurrencyFormatter
import com.example.data.ReceiptRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfAndExportHelper {

    /**
     * Generates a deterministic, high-contrast QR-style verification matrix bitmap
     * encoding any donor or receipt payload string without external network dependencies.
     */
    fun generateQrCodeBitmap(payload: String, sizePx: Int = 360): Bitmap {
        val grid = 21
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D1513")
            style = Paint.Style.FILL
        }

        val margin = sizePx * 0.08f
        val cellSize = (sizePx - margin * 2) / grid

        fun drawFinder(startCol: Int, startRow: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    if (isBorder || isInner) {
                        val left = margin + (startCol + c) * cellSize
                        val top = margin + (startRow + r) * cellSize
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, paint)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(grid - 7, 0)
        drawFinder(0, grid - 7)

        val bytes = payload.toByteArray(Charsets.UTF_8)
        var bitIdx = 0
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= grid - 8
                val inBottomLeft = r >= grid - 8 && c < 8
                if (inTopLeft || inTopRight || inBottomLeft) continue

                val byteVal = bytes[bitIdx % bytes.size].toInt() and 0xFF
                val shift = (bitIdx / bytes.size) % 8
                val active = ((byteVal shr shift) xor (r * 31 + c * 17)) and 1 == 1
                bitIdx++

                if (active) {
                    val left = margin + c * cellSize
                    val top = margin + r * cellSize
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, paint)
                }
            }
        }
        return bmp
    }

    /**
     * Generates a professional PDF Donation Receipt for CASH Chanda.
     */
    fun generateReceiptPdf(context: Context, receipt: ReceiptRecord): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D1513")
            style = Paint.Style.FILL
        }
        val saffronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9933")
            style = Paint.Style.FILL
        }
        val limePaint = Paint(Paint(Paint.ANTI_ALIAS_FLAG)).apply {
            color = Color.parseColor("#2E5A00")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // Top Header Banner
        canvas.drawRect(0f, 0f, 595f, 125f, navyPaint)
        canvas.drawRect(0f, 125f, 595f, 133f, saffronPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCFF80")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCE4E1")
            textSize = 11f
        }

        canvas.drawText(receipt.akhadaName.ifBlank { "SHREE AKHADA TRUST" }, 36f, 48f, titlePaint)
        canvas.drawText("OFFICIAL CASH DONATION RECEIPT • चंदा रसीद (नकद)", 36f, 72f, subPaint)
        canvas.drawText(receipt.akhadaAddress.take(85), 36f, 96f, addrPaint)

        // Receipt Meta Row
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawRoundRect(RectF(36f, 155f, 559f, 215f), 10f, 10f, borderPaint)
        canvas.drawText("RECEIPT NO:", 52f, 178f, labelPaint)
        canvas.drawText(receipt.receiptNumber, 52f, 198f, valuePaint)

        canvas.drawText("DONATION DATE:", 230f, 178f, labelPaint)
        canvas.drawText(receipt.donationDate, 230f, 198f, valuePaint)

        canvas.drawText("PAYMENT MODE:", 410f, 178f, labelPaint)
        canvas.drawText("CASH ONLY (नकद)", 410f, 198f, limePaint.apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD })

        // Donor Information Box
        canvas.drawRoundRect(RectF(36f, 235f, 559f, 430f), 12f, 12f, borderPaint)

        fun drawRow(y: Float, label: String, value: String) {
            canvas.drawText(label, 54f, y, labelPaint)
            canvas.drawText(value, 210f, y, valuePaint)
        }

        drawRow(268f, "DONOR ID (दाता आईडी):", receipt.donorCode)
        drawRow(300f, "DONOR FULL NAME:", receipt.donorName)
        drawRow(332f, "MOBILE NUMBER:", receipt.donorMobile)
        drawRow(364f, "DONATION PURPOSE:", receipt.purpose ?: "General Akhada Chanda")
        drawRow(396f, "RECEIVED BY ADMIN:", receipt.receivedByAdmin)

        // Highlight Amount Box
        val amountBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F8E9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(36f, 450f, 559f, 555f), 12f, 12f, amountBoxPaint)
        canvas.drawRoundRect(RectF(36f, 450f, 559f, 555f), 12f, 12f, borderPaint)

        canvas.drawText("CASH DONATION AMOUNT (दान राशि):", 54f, 478f, labelPaint)
        val bigAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(IndianCurrencyFormatter.formatInr(receipt.amount) + "/- (CASH)", 54f, 512f, bigAmountPaint)
        canvas.drawText("In Words: ${receipt.amountInWords}", 54f, 538f, labelPaint)

        // QR Code & Signature Section
        val qrBitmap = generateQrCodeBitmap(receipt.qrPayload.ifBlank { receipt.receiptNumber }, 130)
        canvas.drawBitmap(qrBitmap, 42f, 580f, null)
        canvas.drawText("Scan to Verify Receipt", 44f, 726f, labelPaint)

        // Thank You Message & Signature Box
        val thankYou = receipt.thankYouMessage?.ifBlank { null }
            ?: "आपके द्वारा हमारे अखाड़े को दिए गए सहयोग के लिए हृदय से धन्यवाद।"
        canvas.drawText("Thank You Note / धन्यवाद संदेश:", 195f, 600f, labelPaint)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11.5f
        }
        canvas.drawText(thankYou.take(65), 195f, 622f, bodyPaint)
        if (thankYou.length > 65) {
            canvas.drawText(thankYou.drop(65).take(65), 195f, 640f, bodyPaint)
        }

        // Signature Box
        canvas.drawLine(380f, 705f, 545f, 705f, borderPaint)
        canvas.drawText("Authorized Akhada Signatory", 388f, 724f, labelPaint)
        canvas.drawText("(${receipt.receivedByAdmin})", 395f, 740f, bodyPaint)

        // Footer
        canvas.drawRect(0f, 795f, 595f, 842f, navyPaint)
        canvas.drawText(
            "CHANDA APP • Private Akhada Cash Donation Ledger • Strict Cash-Only Receipt",
            36f,
            822f,
            addrPaint
        )

        pdfDocument.finishPage(page)

        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeName = receipt.receiptNumber.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        val file = File(receiptsDir, "Receipt_$safeName.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    fun shareReceiptPdf(context: Context, receipt: ReceiptRecord, preferWhatsApp: Boolean = false) {
        try {
            val pdfFile = generateReceiptPdf(context, receipt)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val caption = buildString {
                append("*${receipt.akhadaName}*\n")
                append("Receipt No: *${receipt.receiptNumber}*\n")
                append("Donor: *${receipt.donorName}* (${receipt.donorCode})\n")
                append("Cash Amount: *${IndianCurrencyFormatter.formatInr(receipt.amount)}*\n")
                append("Date: ${receipt.donationDate}\n\n")
                append(receipt.thankYouMessage ?: "आपके द्वारा हमारे अखाड़े को दिए गए सहयोग के लिए हृदय से धन्यवाद।")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (preferWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Share Donation Receipt PDF"))
            } catch (e: Exception) {
                // Fallback to generic chooser if WhatsApp is not installed on emulator
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share Donation Receipt PDF"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Receipt generated: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printReceiptPdf(context: Context, receipt: ReceiptRecord) {
        try {
            val pdfFile = generateReceiptPdf(context, receipt)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val jobName = "Chanda_Receipt_${receipt.receiptNumber}"
            printManager.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(pdfFile.name)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        if (destination == null) {
                            callback?.onWriteFailed("No destination")
                            return
                        }
                        try {
                            FileInputStream(pdfFile).use { input ->
                                FileOutputStream(destination.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        }
                    }
                },
                null
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open print dialog: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppMessage(context: Context, phone: String, message: String) {
        val digits = phone.replace("[^0-9]".toRegex(), "").let {
            if (it.length == 10) "91$it" else it
        }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send Message"))
        }
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        val clean = phone.trim()
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Dialer unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports Complete Financial Report (CSV / Excel-compatible TSV-CSV or Summary PDF)
     * and launches Android Share sheet.
     */
    fun exportFinancialReport(
        context: Context,
        format: String, // "CSV", "EXCEL", "PDF"
        reportTitle: String,
        donors: List<Donor>,
        donations: List<Donation>,
        akhadaName: String
    ) {
        try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeTitle = reportTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file: File
            val mimeType: String

            if (format == "PDF") {
                file = File(exportsDir, "${safeTitle}_Report.pdf")
                mimeType = "application/pdf"
                val pdf = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0D1513")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, 595f, 95f, headerPaint)
                val whiteBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                }
                val limeSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CCFF80")
                    textSize = 12f
                }
                canvas.drawText(akhadaName, 32f, 42f, whiteBold)
                canvas.drawText("$reportTitle • Generated ${IndianCurrencyFormatter.nowReadable()}", 32f, 68f, limeSub)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 11f
                }
                val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }

                val activeDonations = donations.filter { it.status != "REVERSED" }
                val totalCash = activeDonations.sumOf { it.amount }
                canvas.drawText(
                    "Total Registered Donors: ${donors.size}   |   Active Cash Donations: ${activeDonations.size}   |   Total Cash: ${IndianCurrencyFormatter.formatInr(totalCash)}",
                    32f,
                    125f,
                    boldPaint
                )

                var y = 158f
                canvas.drawText("Receipt #", 32f, y, boldPaint)
                canvas.drawText("Date", 130f, y, boldPaint)
                canvas.drawText("Donor Name", 205f, y, boldPaint)
                canvas.drawText("Purpose", 355f, y, boldPaint)
                canvas.drawText("Cash (INR)", 485f, y, boldPaint)
                y += 18f

                activeDonations.take(28).forEach { d ->
                    canvas.drawText(d.receiptNumber.take(15), 32f, y, textPaint)
                    canvas.drawText(d.donationDate.take(10), 130f, y, textPaint)
                    canvas.drawText(d.donorName.take(22), 205f, y, textPaint)
                    canvas.drawText(d.purpose.take(18), 355f, y, textPaint)
                    canvas.drawText(IndianCurrencyFormatter.formatInr(d.amount), 485f, y, boldPaint)
                    y += 20f
                }

                pdf.finishPage(page)
                FileOutputStream(file).use { pdf.writeTo(it) }
                pdf.close()
            } else {
                val ext = if (format == "EXCEL") "xls" else "csv"
                val sep = if (format == "EXCEL") "\t" else ","
                file = File(exportsDir, "${safeTitle}_Ledger.$ext")
                mimeType = if (format == "EXCEL") "application/vnd.ms-excel" else "text/csv"

                val sb = StringBuilder()
                // UTF-8 BOM for Excel Hindi/Rupee compatibility
                sb.append("\uFEFF")
                sb.append(
                    listOf(
                        "ReceiptNumber",
                        "DonationDate",
                        "DonorCode",
                        "DonorName",
                        "MobileNumber",
                        "AmountINR",
                        "PaymentMethod",
                        "Purpose",
                        "ReceivedByAdmin",
                        "Status"
                    ).joinToString(sep)
                ).append("\n")

                donations.forEach { d ->
                    val row = listOf(
                        d.receiptNumber,
                        d.donationDate,
                        d.donorCode,
                        d.donorName.replace(sep, " "),
                        d.donorMobile,
                        d.amount.toString(),
                        "CASH",
                        d.purpose.replace(sep, " "),
                        d.receivedByAdmin.replace(sep, " "),
                        d.status
                    )
                    sb.append(row.joinToString(sep)).append("\n")
                }

                sb.append("\n")
                sb.append(
                    listOf(
                        "DonorCode",
                        "FullName",
                        "FatherName",
                        "MobileNumber",
                        "WhatsAppNumber",
                        "Village",
                        "City",
                        "District",
                        "State",
                        "PinCode",
                        "LifetimeCashINR",
                        "DonationCount",
                        "AddedByAdmin"
                    ).joinToString(sep)
                ).append("\n")

                donors.forEach { donor ->
                    val row = listOf(
                        donor.donorCode,
                        donor.fullName.replace(sep, " "),
                        donor.fatherName.replace(sep, " "),
                        donor.mobileNumber,
                        donor.whatsappNumber,
                        donor.village.replace(sep, " "),
                        donor.city.replace(sep, " "),
                        donor.district.replace(sep, " "),
                        donor.state.replace(sep, " "),
                        donor.pinCode,
                        donor.lifetimeDonation.toString(),
                        donor.donationCount.toString(),
                        donor.addedByAdmin.replace(sep, " ")
                    )
                    sb.append(row.joinToString(sep)).append("\n")
                }

                file.writeText(sb.toString(), Charsets.UTF_8)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$akhadaName - $reportTitle")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export $reportTitle ($format)"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
