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

        // Защита от нулевых размеров
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Используем точное деление на 3
        val stepX = w / 3f
        val stepY = h / 3f

        // Вертикальные линии (от верхнего края до нижнего)
        canvas.drawLine(stepX, 0f, stepX, h, paint)
        canvas.drawLine(stepX * 2, 0f, stepX * 2, h, paint)

        // Горизонтальные линии (от левого края до правого)
        canvas.drawLine(0f, stepY, w, stepY, paint)
        canvas.drawLine(0f, stepY * 2, w, stepY * 2, paint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Запрашиваем перерисовку после того, как View прикрепился к окну
        post {
            requestLayout()
            invalidate()
        }
    }
}