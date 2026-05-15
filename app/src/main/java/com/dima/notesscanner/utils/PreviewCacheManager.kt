package com.dima.notesscanner.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PreviewCacheManager {

    // Размеры превью в пикселях (124x175 dp преобразуются в пиксели при вызове)
    suspend fun getPreview(context: Context, pdfFile: File, reqWidthDp: Int = 124, reqHeightDp: Int = 175): String? = withContext(Dispatchers.IO) {
        val reqWidth = dpToPx(context, reqWidthDp)
        val reqHeight = dpToPx(context, reqHeightDp)

        val dbHelper = PreviewCacheHelper(context)
        val db = dbHelper.readableDatabase

        // 1. Проверяем БД
        val cursor = db.query(
            PreviewCacheHelper.TABLE_NAME,
            arrayOf(PreviewCacheHelper.COL_PREVIEW_PATH),
            "${PreviewCacheHelper.COL_PDF_PATH} = ?",
            arrayOf(pdfFile.absolutePath),
            null, null, null
        )

        var previewPath: String? = null
        if (cursor.moveToFirst()) {
            previewPath = cursor.getString(0)
            cursor.close()
        } else {
            cursor.close()
        }

        // 2. Если есть и файл существует → возвращаем
        if (previewPath != null && File(previewPath).exists()) {
            return@withContext previewPath
        }

        // 3. Генерируем новое превью
        val newPreviewFile = File(context.cacheDir, "preview_${System.currentTimeMillis()}.jpg")
        val bitmap = generatePreview(pdfFile, reqWidth, reqHeight) ?: return@withContext null

        FileOutputStream(newPreviewFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        bitmap.recycle()

        // 4. Сохраняем в БД
        val values = ContentValues().apply {
            put(PreviewCacheHelper.COL_PDF_PATH, pdfFile.absolutePath)
            put(PreviewCacheHelper.COL_PREVIEW_PATH, newPreviewFile.absolutePath)
        }
        val writableDb = dbHelper.writableDatabase
        writableDb.replace(PreviewCacheHelper.TABLE_NAME, null, values)
        writableDb.close()
        dbHelper.close()

        return@withContext newPreviewFile.absolutePath
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun generatePreview(pdfFile: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val pageWidth = page.width
                    val pageHeight = page.height

                    // Вычисляем область без отступов (как в PdfGenerator, MARGIN_PERCENT = 0.05)
                    val marginPercent = 0.05f
                    val marginX = (pageWidth * marginPercent).toInt()
                    val marginY = (pageHeight * marginPercent).toInt()

                    val contentLeft = marginX
                    val contentTop = marginY
                    val contentRight = pageWidth - marginX
                    val contentBottom = pageHeight - marginY
                    val srcWidth = contentRight - contentLeft
                    val srcHeight = contentBottom - contentTop

                    // Создаём Bitmap нужного размера
                    val bitmap = createBitmap(reqWidth, reqHeight, Bitmap.Config.ARGB_8888)

                    // Рассчитываем область для вписывания с сохранением пропорций
                    val ratio = minOf(reqWidth.toFloat() / srcWidth, reqHeight.toFloat() / srcHeight)
                    val dstWidth = (srcWidth * ratio).toInt()
                    val dstHeight = (srcHeight * ratio).toInt()
                    val dstLeft = (reqWidth - dstWidth) / 2
                    val dstTop = (reqHeight - dstHeight) / 2
                    val dstRight = dstLeft + dstWidth
                    val dstBottom = dstTop + dstHeight

                    // Создаём матрицу для трансформации
                    val srcRect = android.graphics.Rect(contentLeft, contentTop, contentRight, contentBottom)
                    val dstRect = android.graphics.Rect(dstLeft, dstTop, dstRight, dstBottom)

                    // Создаём матрицу масштабирования
                    val transform = android.graphics.Matrix()
                    transform.setRectToRect(
                        android.graphics.RectF(srcRect),
                        android.graphics.RectF(dstRect),
                        android.graphics.Matrix.ScaleToFit.FILL
                    )

                    // Используем правильную сигнатуру render
                    page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deletePreview(context: Context, pdfFile: File) = withContext(Dispatchers.IO) {
        val dbHelper = PreviewCacheHelper(context)
        val db = dbHelper.writableDatabase

        val cursor = db.query(
            PreviewCacheHelper.TABLE_NAME,
            arrayOf(PreviewCacheHelper.COL_PREVIEW_PATH),
            "${PreviewCacheHelper.COL_PDF_PATH} = ?",
            arrayOf(pdfFile.absolutePath),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            val previewPath = cursor.getString(0)
            File(previewPath).delete()
            cursor.close()
        }

        db.delete(PreviewCacheHelper.TABLE_NAME, "${PreviewCacheHelper.COL_PDF_PATH} = ?", arrayOf(pdfFile.absolutePath))
        db.close()
        dbHelper.close()
    }
}