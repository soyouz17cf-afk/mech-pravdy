package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fontSize = 15f
    private val lineHeight = fontSize * 1.15f

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = 88f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#555555")
        textSize = 32f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }

    private var columns = 0
    private var rows = 0
    private lateinit var lines: Array<CharArray>
    private lateinit var lineY: FloatArray
    private lateinit var speeds: FloatArray
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / lineHeight).toInt() + 2
        lines = Array(rows) { CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' } }
        lineY = FloatArray(rows) { i -> i * lineHeight }
        speeds = FloatArray(rows) { 0.3f + Random.nextFloat() * 0.5f }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Чёрный фон на весь холст
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val textCenterY = h * 0.50f
        val textTop = textCenterY - titlePaint.textSize * 0.7f
        val textBottom = textCenterY + titlePaint.textSize * 0.3f

        // Двигаем строки вверх
        for (r in 0 until rows) {
            lineY[r] -= speeds[r]
            if (lineY[r] < -lineHeight) {
                lineY[r] = h + lineHeight
                for (c in 0 until columns) {
                    lines[r][c] = if (Random.nextFloat() > 0.5f) '0' else '1'
                }
                speeds[r] = 0.3f + Random.nextFloat() * 0.5f
            }
        }

        // Рисуем строки
        for (r in 0 until rows) {
            val y = lineY[r]
            if (y > h || y < -lineHeight) continue

            // Пропускаем зону текста
            if (y > textTop - 10f && y < textBottom + 10f) continue

            for (c in 0 until columns) {
                val x = c * fontSize
                matrixPaint.alpha = 140
                matrixPaint.textSize = fontSize
                canvas.drawText(lines[r][c].toString(), x, y, matrixPaint)
            }
        }

        // Полупрозрачная плашка под текст
        val overlayPaint = Paint().apply { color = Color.parseColor("#CC000000") }
        canvas.drawRect(0f, textTop - 15f, w, textBottom + 45f, overlayPaint)

        canvas.drawText("СБЕР", w / 2, textCenterY + 10f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, h * 0.85f, subtitlePaint)

        frame++
        postInvalidateDelayed(50)
    }
}
