package com.dima.notesscanner.utils

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File

object ImageProcessor {

    // ==================== ПУБЛИЧНЫЕ ФУНКЦИИ ====================

    fun autoEnhanceBrightnessContrast(context: Context, photoFile: File): Bitmap? {
        return try {
            val originalBitmap = Glide.with(context)
                .asBitmap()
                .load(photoFile)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit()
                .get()

            val srcMat = Mat()
            Utils.bitmapToMat(originalBitmap, srcMat)

            // Адаптивный подбор параметров
            val (alpha, beta) = computeAdaptiveParams(srcMat)

            val dstMat = Mat()
            srcMat.convertTo(dstMat, -1, alpha, beta)

            val resultBitmap = Bitmap.createBitmap(
                dstMat.cols(),
                dstMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(dstMat, resultBitmap)

            srcMat.release()
            dstMat.release()
            originalBitmap.recycle()

            resultBitmap
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "Error in adaptive brightness/contrast", e)
            null
        }
    }

    private fun computeAdaptiveParams(srcMat: Mat): Pair<Double, Double> {
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_RGBA2GRAY)

        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(gray, mean, stddev)

        val meanVal = mean.get(0, 0)[0]
        val stdVal = stddev.get(0, 0)[0]

        gray.release()
        mean.release()
        stddev.release()

        val targetMean = 120.0
        val targetStd = 65.0

        var beta = targetMean - meanVal
        beta = beta.coerceIn(-40.0, 40.0)

        var alpha = if (stdVal > 0) targetStd / stdVal else 1.0
        alpha = alpha.coerceIn(0.7, 1.5)

        return Pair(alpha, beta)
    }



    fun autoEnhancePerspective(context: Context, photoFile: File): Bitmap? {
        // Пытаемся выпрямить документ
        val corrected = correctPerspective(context, photoFile)
        return corrected
    }

    // ==================== ОСНОВНАЯ ФУНКЦИЯ ВЫПРЯМЛЕНИЯ ====================

    fun correctPerspective(context: Context, photoFile: File): Bitmap? {
        val originalBitmap = loadBitmapWithOrientation(context, photoFile) ?: return null

        val srcMat = Mat()
        Utils.bitmapToMat(originalBitmap, srcMat)

        // Уменьшаем изображение для детекции (высота = 500)
        val ratio = srcMat.height() / 500.0
        val resizedMat = Mat()
        val resizedHeight = 500
        val resizedWidth = (srcMat.width() * resizedHeight / srcMat.height()).toInt()
        Imgproc.resize(srcMat, resizedMat, Size(resizedWidth.toDouble(), resizedHeight.toDouble()))

        // Детекция границ
        val gray = Mat()
        Imgproc.cvtColor(resizedMat, gray, Imgproc.COLOR_RGBA2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val edged = Mat()
        Imgproc.Canny(blurred, edged, 75.0, 200.0)

        // Поиск контуров
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Берём топ-5 контуров по площади
        val sortedContours = contours.sortedByDescending { Imgproc.contourArea(it) }.take(5)

        // Ищем четырёхугольник
        var screenPoints: Array<Point>? = null
        for (contour in sortedContours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx, 0.01 * peri, true)

            if (approx.total() == 4L) {
                // Масштабируем точки обратно к оригинальному размеру
                val pts = approx.toArray().map { Point(it.x * ratio, it.y * ratio) }.toTypedArray()
                screenPoints = orderPoints(pts)
                break
            }
        }

        // Освобождаем промежуточные матрицы
        resizedMat.release()
        gray.release()
        blurred.release()
        edged.release()
        hierarchy.release()

        if (screenPoints == null) {
            srcMat.release()
            originalBitmap.recycle()
            return null
        }

        // Вычисляем размеры выходного изображения
        val widthA = distance(screenPoints[1], screenPoints[0])
        val widthB = distance(screenPoints[2], screenPoints[3])
        val maxWidth = maxOf(widthA, widthB).toInt()
        val heightA = distance(screenPoints[1], screenPoints[2])
        val heightB = distance(screenPoints[0], screenPoints[3])
        val maxHeight = maxOf(heightA, heightB).toInt()

        val dstSize = Size(maxWidth.toDouble(), maxHeight.toDouble())
        val warpedMat = warpPerspective(srcMat, screenPoints, dstSize)

        // Конвертируем результат в Bitmap
        val resultBitmap = Bitmap.createBitmap(warpedMat.cols(), warpedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedMat, resultBitmap)

        srcMat.release()
        warpedMat.release()
        originalBitmap.recycle()

        return resultBitmap
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

    private fun loadBitmapWithOrientation(context: Context, file: File): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load(file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit()
                .get()
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "loadBitmap error", e)
            null
        }
    }

    private fun orderPoints(pts: Array<Point>): Array<Point> {
        val rect = arrayOfNulls<Point>(4)
        val sum = DoubleArray(4)
        for (i in 0..3) {
            sum[i] = pts[i].x + pts[i].y
        }
        rect[0] = pts[sum.indices.minByOrNull { sum[it] }!!]      // top-left
        rect[2] = pts[sum.indices.maxByOrNull { sum[it] }!!]      // bottom-right

        val diff = DoubleArray(4)
        for (i in 0..3) {
            diff[i] = pts[i].y - pts[i].x
        }
        rect[1] = pts[diff.indices.minByOrNull { diff[it] }!!]     // top-right
        rect[3] = pts[diff.indices.maxByOrNull { diff[it] }!!]     // bottom-left

        return rect.requireNoNulls()
    }

    private fun warpPerspective(srcMat: Mat, srcPoints: Array<Point>, dstSize: Size): Mat {
        val src = MatOfPoint2f(*srcPoints)
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(dstSize.width, 0.0),
            Point(dstSize.width, dstSize.height),
            Point(0.0, dstSize.height)
        )
        val transform = Imgproc.getPerspectiveTransform(src, dst)
        val warped = Mat()
        Imgproc.warpPerspective(srcMat, warped, transform, dstSize)
        src.release()
        dst.release()
        transform.release()
        return warped
    }

    private fun distance(p1: Point, p2: Point): Double {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y)
    }
}