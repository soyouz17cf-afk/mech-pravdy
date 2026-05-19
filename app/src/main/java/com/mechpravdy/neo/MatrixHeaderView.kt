package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fontSize = 18f
    private val lineHeight = fontSize * 1.15f

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 120
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
    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#EEFFFFFF") }
    private val logoBorderPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    private var columns = 0
    private var rows = 0
    // Массив строк: каждая строка знает, сколько символов уже "напечатано"
    private lateinit var lines: Array<CharArray>
    private lateinit var lineY: FloatArray
    private lateinit var speeds: FloatArray
    // Сколько символов уже видно в каждой строке (эффект печати)
    private lateinit var printedCount: IntArray
    private var frame = 0

    // Границы закруглённого прямоугольника вокруг текста
    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / lineHeight).toInt() + 2
        lines = Array(rows) { CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' } }
        lineY = FloatArray(rows) { i -> i * lineHeight }
        speeds = FloatArray(rows) { 0.3f + Random.nextFloat() * 0.5f }
        printedCount = IntArray(rows) { 0 }

        val rect = Rect()
        titlePaint.getTextBounds("СБЕР", 0, 4, rect)
        val textCenterY = h * 0.50f
        val left = w / 2f - rect.width() / 2f - 35f
        val top = textCenterY - titlePaint.textSize * 0.7f - 15f
        val right = w / 2f + rect.width() / 2f + 35f
        val bottom = textCenterY + titlePaint.textSize * 0.3f + 45f
        logoRect = RectF(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Двигаем строки вверх
        for (r in 0 until rows) {
            lineY[r] -= speeds[r]
            if (lineY[r] < -lineHeight) {
                // Строка ушла за верх — сбрасываем вниз
                lineY[r] = h + lineHeight
                for (c in 0 until columns) {
                    lines[r][c] = if (Random.nextFloat() > 0.5f) '0' else '1'
                }
                speeds[r] = 0.3f + Random.nextFloat() * 0.5f
                printedCount[r] = 0
            }
        }

        // Эффект печати: каждые 3 кадра увеличиваем счётчик напечатанных символов
        if (frame % 3 == 0) {
            for (r in 0 until rows) {
                if (printedCount[r] < columns) {
                    printedCount[r]++
                }
            }
        }

        // Рисуем строки
        for (r in 0 until rows) {
            val y = lineY[r]
            if (y > h || y < -lineHeight) continue

            for (c in 0 until columns) {
                // Рисуем только "напечатанные" символы
                if (c >= printedCount[r]) continue

                val x = c * fontSize

                // Пропускаем зону логотипа
                if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue

                canvas.drawText(lines[r][c].toString(), x, y, matrixPaint)
            }
        }

        // Закруглённый прямоугольник позади текста
        canvas.drawRoundRect(logoRect, 20f, 20f, logoBgPaint)
        canvas.drawRoundRect(logoRect, 20f, 20f, logoBorderPaint)

        val textCenterY = h * 0.50f
        canvas.drawText("СБЕР", w / 2, textCenterY + 10f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, h * 0.85f, subtitlePaint)

        frame++
        postInvalidateDelayed(50)
    }
}
