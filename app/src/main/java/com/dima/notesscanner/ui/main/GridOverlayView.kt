package com.dima.notesscanner.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        alpha = 150
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val stepX = width / 3
        val stepY = height / 3

        // Вертикальные линии
        for (i in 1..2) {
            val x = stepX * i
            canvas.drawLine(x, 0f, x, height, paint)
        }

        // Горизонтальные линии
        for (i in 1..2) {
            val y = stepY * i
            canvas.drawLine(0f, y, width, y, paint)
        }
    }
}