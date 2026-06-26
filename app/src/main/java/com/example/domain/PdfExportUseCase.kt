package com.example.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfExportUseCase(private val repository: GameRepository) {

    suspend fun exportCaseLogToPdf(context: Context, caseId: String): File = withContext(Dispatchers.IO) {
        val groundTruth = repository.getGroundTruth(caseId)
        val npcs = repository.getNpcsDirect()
        val evidence = repository.getAllEvidenceDirect()
        val messages = repository.getAllMessagesDirect()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val textPaint = TextPaint()
        textPaint.color = Color.BLACK
        textPaint.textSize = 10f
        textPaint.isAntiAlias = true

        val titlePaint = TextPaint()
        titlePaint.color = Color.BLACK
        titlePaint.textSize = 16f
        titlePaint.isFakeBoldText = true
        titlePaint.isAntiAlias = true

        var yPosition = 50f
        val margin = 50f
        val usableWidth = pageInfo.pageWidth - (2 * margin)

        fun drawText(text: String, isTitle: Boolean = false) {
            val paint = if (isTitle) titlePaint else textPaint
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, usableWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            if (yPosition + staticLayout.height > pageInfo.pageHeight - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin
            }

            canvas.save()
            canvas.translate(margin, yPosition)
            staticLayout.draw(canvas)
            canvas.restore()

            yPosition += staticLayout.height + (if (isTitle) 20f else 10f)
        }

        drawText("Court of Themis - Official Case Log", isTitle = true)
        drawText("Case ID: $caseId", isTitle = true)

        if (groundTruth != null) {
            drawText("Core Crime:", isTitle = true)
            drawText(groundTruth.coreCrime)
        }

        drawText("Characters Involved:", isTitle = true)
        npcs.forEach { npc ->
            drawText("- ${npc.name} (${npc.role}): ${npc.profile}")
        }

        drawText("Evidence Collected:", isTitle = true)
        if (evidence.isEmpty()) drawText("No evidence collected.")
        evidence.forEach { ev ->
            drawText("- ${ev.name}: ${ev.physicalDescription} (Admissibility: ${ev.admissibilityStatus.name})")
        }

        drawText("Action & Event Log:", isTitle = true)
        if (messages.isEmpty()) drawText("No logs found.")
        messages.forEach { msg ->
            val prefix = if (msg.isSystem) "[SYSTEM]" else if (msg.isToolCall) "[AI ACTION: ${msg.toolName}]" else "[${msg.sender}]"
            drawText("$prefix: ${msg.text}")
        }
        
        drawText("System Diagnostics & Error Logs:", isTitle = true)
        val errors = messages.filter { 
            (it.isSystem || it.isToolCall) && 
            (it.text.contains("error", ignoreCase = true) || 
             it.text.contains("fail", ignoreCase = true) || 
             it.text.contains("exception", ignoreCase = true)) 
        }
        
        if (errors.isEmpty()) {
            drawText("No critical errors or anomalies detected during the simulation.")
        } else {
            drawText("WARNING: The following potential errors or system anomalies were detected:")
            errors.forEach { err ->
                drawText("- [${err.timestamp}] ${err.text}")
            }
        }

        pdfDocument.finishPage(page)

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "Case_Export_${caseId}_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        file
    }
}
