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
    }

    suspend fun savePdf(photoFiles: List<File>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            photoFiles.forEachIndexed { index, file ->
                // Загружаем сразу нужного размера и сжимаем
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(file)
                    .override(PAGE_WIDTH, PAGE_HEIGHT) // сразу нужный размер
                    .format(DecodeFormat.PREFER_RGB_565) // меньше памяти
                    .submit()
                    .get()

                if (bitmap != null) {
                    val page = createPageWithMargins(document, bitmap, index + 1)
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }

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

    suspend fun createShareablePdf(photoFiles: List<File>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            photoFiles.forEachIndexed { index, file ->
                val bitmap = loadBitmapWithGlide(file)

                if (bitmap != null) {
                    val page = createPageWithMargins(document, bitmap, index + 1)
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }

            val pdfFile = File(context.cacheDir, "notes_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(pdfFile))
            document.close()

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
     * Загружает фото через Glide (учитывает EXIF-ориентацию)
     */
    private fun loadBitmapWithGlide(file: File): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load(file)
                .submit(PAGE_WIDTH, PAGE_HEIGHT) // Загружаем сразу нужного размера
                .get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
            null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            null
        }
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

        // СЖАТИЕ: конвертируем в JPEG с качеством 70%
        val compressedBitmap = compressBitmap(scaledBitmap, 70)

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)

        val left = marginX + (availableWidth - compressedBitmap.width) / 2
        val top = marginY + (availableHeight - compressedBitmap.height) / 2

        page.canvas.drawBitmap(compressedBitmap, left.toFloat(), top.toFloat(), null)

        // Очищаем память
        if (compressedBitmap != scaledBitmap) {
            compressedBitmap.recycle()
        }
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return page
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int = 70): Bitmap {
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