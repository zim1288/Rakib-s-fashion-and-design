package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.db.SareeItem
import com.example.db.TransactionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

object CSVExportUtils {

    suspend fun exportInventoryToUri(context: Context, items: List<SareeItem>, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.append("Model Name,SKU,Color,Fabric Type,Brand Category,Unit Price,Quantity,Total Value\n")
                        for (item in items) {
                            writer.append("${escapeCSV(item.modelName)},${escapeCSV(item.sku)},${escapeCSV(item.color)},${escapeCSV(item.fabricType)},${escapeCSV(item.brandCategory)},${item.unitPrice},${item.pieceCount},${item.totalValue}\n")
                        }
                    }
                }
                Result.success(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun exportTransactionsToUri(context: Context, logs: List<TransactionLog>, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.append("Date,Type,Model Name,SKU,Color,Fabric Type,Quantity,Unit Price,Total Amount\n")
                        for (log in logs) {
                            writer.append("${log.dateString},${log.type},${escapeCSV(log.modelName)},${escapeCSV(log.sku)},${escapeCSV(log.color)},${escapeCSV(log.fabricType)},${log.quantity},${log.unitPrice},${log.totalAmount}\n")
                        }
                    }
                }
                Result.success(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun escapeCSV(value: String): String {
        var escaped = value
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = escaped.replace("\"", "\"\"")
            escaped = "\"$escaped\""
        }
        return escaped
    }
}
