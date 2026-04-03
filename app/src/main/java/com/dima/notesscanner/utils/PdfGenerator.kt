package com.dima.notesscanner.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException

class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 1240
        private const val PAGE_HEIGHT = 1754
        private const val MARGIN_PERCENT = 0.05f
        private const val JPEG_QUALITY = 70
    }

    /**
     * Сохраняет PDF в Downloads
     */
    suspend fun savePdf(photoFiles: List<File>, fileName: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = generatePdfDocument(photoFiles)
            val uri = saveToDownloads(document, fileName)
            document.close()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Создаёт PDF для отправки (во временной папке)
     */
    suspend fun createShareablePdf(photoFiles: List<File>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = generatePdfDocument(photoFiles)
            val uri = saveToCache(document)
            document.close()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Единый метод генерации PDF документа
     */
    private suspend fun generatePdfDocument(photoFiles: List<File>): PdfDocument = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        photoFiles.forEachIndexed { index, file ->
            val bitmap = loadOptimizedBitmap(file)

            if (bitmap != null) {
                val page = createPageWithMargins(document, bitmap, index + 1)
                document.finishPage(page)
                bitmap.recycle()
            }
        }

        return@withContext document
    }

    /**
     * Загружает фото с оптимизацией (размер + RGB_565)
     */
    private fun loadOptimizedBitmap(file: File): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load(file)
                .override(PAGE_WIDTH, PAGE_HEIGHT)
                .format(DecodeFormat.PREFER_RGB_565)
                .submit()
                .get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
            null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Сохраняет PDF в Downloads
     */
    private fun saveToDownloads(document: PdfDocument, fileName: String): Uri {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw Exception("Не удалось создать файл")

        resolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
        }

        return uri
    }

    /**
     * Сохраняет PDF в cache для отправки
     */
    private fun saveToCache(document: PdfDocument): Uri {
        val pdfFile = File(context.cacheDir, "notes_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(pdfFile))
        pdfFile.deleteOnExit()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }

    private fun createPageWithMargins(
        document: PdfDocument,
        bitmap: Bitmap,
        pageNumber: Int
    ): PdfDocument.Page {
        val marginX = (PAGE_WIDTH * MARGIN_PERCENT).toInt()
        val marginY = (PAGE_HEIGHT * MARGIN_PERCENT).toInt()

        val availableWidth = PAGE_WIDTH - (marginX * 2)
        val availableHeight = PAGE_HEIGHT - (marginY * 2)

        val scaledBitmap = scaleBitmapToFit(bitmap, availableWidth, availableHeight)
        val compressedBitmap = compressBitmap(scaledBitmap, JPEG_QUALITY)

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)

        val left = marginX + (availableWidth - compressedBitmap.width) / 2
        val top = marginY + (availableHeight - compressedBitmap.height) / 2

        page.canvas.drawBitmap(compressedBitmap, left.toFloat(), top.toFloat(), null)

        // Очищаем память
        if (compressedBitmap != scaledBitmap) compressedBitmap.recycle()
        if (scaledBitmap != bitmap) scaledBitmap.recycle()

        return page
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

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