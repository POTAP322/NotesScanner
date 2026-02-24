package com.dima.notesscanner.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfGenerator(private val context: Context) {

    /**
     * Сохраняет PDF во внутреннюю память приложения
     */
    suspend fun savePdf(photoFiles: List<File>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            photoFiles.forEachIndexed { index, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width,
                        bitmap.height,
                        index + 1
                    ).create()

                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }

            // Сохраняем в Downloads
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "notes_${System.currentTimeMillis()}.pdf")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
                document.close()
                Result.success(it)
            } ?: Result.failure(Exception("Не удалось создать файл"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Создаёт PDF для отправки (во временной папке)
     */
    suspend fun createShareablePdf(photoFiles: List<File>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            photoFiles.forEachIndexed { index, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width,
                        bitmap.height,
                        index + 1
                    ).create()

                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }

            // Сохраняем во временную папку приложения (cache)
            val pdfFile = File(context.cacheDir, "notes_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(pdfFile))
            document.close()

            // Создаём Uri через FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Создаёт Intent для отправки PDF
     */
    fun createShareIntent(pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}