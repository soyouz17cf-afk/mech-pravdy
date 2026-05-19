package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    private val fontSize = 36f
    private val lineHeight = fontSize * 1.05f // меньше расстояние между строками

    private val easterEggs = arrayOf(
        "Здравствуй, Нео",
        "Меч Правды",
        "Пойдём за белым кроликом",
        "Связность",
        "5 Вольт",
        "Ковчег",
        "Монсегюр",
        "Батя",
        "Нео",
        "СБЕР",
        "ГигаЧат"
    )

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 120
    }
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#CCFFCC")
        textSize = 26f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }

    private var columns = 0
    private var activeLine: String = ""
    private var activeLineY: Float = 0f
    private var printedCount = 0
    private var isPrinting = true
    private var floatSpeed = 0f

    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        spawnNewLine(h.toFloat())
        val logoWidth = w * 0.55f
        val logoHeight = h * 0.75f
        val left = (w - logoWidth) / 2f
        val top = (h - logoHeight) / 2f
        val right = left + logoWidth
        val bottom = top + logoHeight
        logoRect = RectF(left, top, right, bottom)
    }

    private fun spawnNewLine(h: Float) {
        activeLine = if (Random.nextFloat() < 0.2f) {
            val word = easterEggs[Random.nextInt(easterEggs.size)]
            val prefixLen = Random.nextInt(0, columns - word.length).coerceAtLeast(0)
            val prefix = CharArray(prefixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val suffixLen = (columns - prefixLen - word.length).coerceAtLeast(0)
            val suffix = CharArray(suffixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            prefix + word + suffix
        } else {
            CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        }
        activeLineY = h + lineHeight
        printedCount = 0
        isPrinting = true
        floatSpeed = 0.6f + Random.nextFloat() * 0.8f // быстрее плывёт
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (isPrinting) {
            // Печатаем быстрее — по 4 символа за кадр
            printedCount += 4
            if (printedCount >= activeLine.length) {
                isPrinting = false
            }
        } else {
            activeLineY -= floatSpeed
            if (activeLineY < -lineHeight) {
                // Новая строка рождается сразу, без паузы
                spawnNewLine(h + lineHeight)
            }
        }

        val y = activeLineY
        for (c in 0 until printedCount.coerceAtMost(activeLine.length)) {
            val x = c * fontSize
            if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
            canvas.drawText(activeLine[c].toString(), x, y, matrixPaint)
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)

        postInvalidateDelayed(50)
    }
}
