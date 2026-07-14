package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.db.SareeItem
import com.example.db.TransactionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExportUtils {

    suspend fun exportInventoryToPdfUri(context: Context, items: List<SareeItem>, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                
                var page = document.startPage(pageInfo)
                var canvas = page.canvas
                val paint = Paint()
                paint.textSize = 12f
                paint.color = Color.BLACK
                
                val headerPaint = Paint()
                headerPaint.textSize = 14f
                headerPaint.isFakeBoldText = true
                headerPaint.color = Color.BLACK

                var yPosition = 50f
                canvas.drawText("Inventory Report", 50f, yPosition, headerPaint)
                yPosition += 30f
                
                // Table headers
                canvas.drawText("Model Name", 50f, yPosition, headerPaint)
                canvas.drawText("SKU", 200f, yPosition, headerPaint)
                canvas.drawText("Qty", 350f, yPosition, headerPaint)
                canvas.drawText("Value", 450f, yPosition, headerPaint)
                yPosition += 20f
                
                for (item in items) {
                    if (yPosition > 800f) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                    }
                    canvas.drawText(item.modelName.take(20), 50f, yPosition, paint)
                    canvas.drawText(item.sku.take(20), 200f, yPosition, paint)
                    canvas.drawText(item.pieceCount.toString(), 350f, yPosition, paint)
                    canvas.drawText(String.format(java.util.Locale.US, "%.2f", item.totalValue), 450f, yPosition, paint)
                    yPosition += 20f
                }
                
                document.finishPage(page)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
                document.close()
                
                Result.success(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun exportTransactionsToPdfUri(context: Context, logs: List<TransactionLog>, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                
                var page = document.startPage(pageInfo)
                var canvas = page.canvas
                val paint = Paint()
                paint.textSize = 10f
                paint.color = Color.BLACK
                
                val headerPaint = Paint()
                headerPaint.textSize = 12f
                headerPaint.isFakeBoldText = true
                headerPaint.color = Color.BLACK

                var yPosition = 50f
                canvas.drawText("Transactions Report", 50f, yPosition, headerPaint)
                yPosition += 30f
                
                // Table headers
                canvas.drawText("Date", 50f, yPosition, headerPaint)
                canvas.drawText("Type", 120f, yPosition, headerPaint)
                canvas.drawText("Model", 180f, yPosition, headerPaint)
                canvas.drawText("Qty", 350f, yPosition, headerPaint)
                canvas.drawText("Amount", 420f, yPosition, headerPaint)
                yPosition += 20f
                
                for (log in logs) {
                    if (yPosition > 800f) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                    }
                    canvas.drawText(log.dateString.take(10), 50f, yPosition, paint)
                    canvas.drawText(log.type, 120f, yPosition, paint)
                    canvas.drawText(log.modelName.take(20), 180f, yPosition, paint)
                    canvas.drawText(log.quantity.toString(), 350f, yPosition, paint)
                    canvas.drawText(String.format(java.util.Locale.US, "%.2f", log.totalAmount), 420f, yPosition, paint)
                    yPosition += 20f
                }
                
                document.finishPage(page)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
                document.close()
                
                Result.success(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
