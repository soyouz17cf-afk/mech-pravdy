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

    private val fontSize = 18f
    private val lineHeight = fontSize * 1.15f

    // Пасхальные слова
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
    private val easterPaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 200
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
    private var rows = 0
    // Храним не символы, а целые строки (могут содержать слова)
    private lateinit var lines: Array<String>
    private lateinit var lineY: FloatArray
    private lateinit var speeds: FloatArray
    private lateinit var printedCount: IntArray
    private var frame = 0

    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / lineHeight).toInt() + 2
        lines = Array(rows) { generateLine() }
        lineY = FloatArray(rows) { i -> i * lineHeight }
        speeds = FloatArray(rows) { 0.3f + Random.nextFloat() * 0.5f }
        printedCount = IntArray(rows) { 0 }

        val logoWidth = w * 0.55f
        val logoHeight = h * 0.75f
        val left = (w - logoWidth) / 2f
        val top = (h - logoHeight) / 2f
        val right = left + logoWidth
        val bottom = top + logoHeight
        logoRect = RectF(left, top, right, bottom)
    }

    /** Генерирует строку: либо слово-пасхалка (20% шанс), либо поток 0/1 */
    private fun generateLine(): String {
        return if (Random.nextFloat() < 0.2f) {
            // Вставляем пасхальное слово в случайное место строки
            val word = easterEggs[Random.nextInt(easterEggs.size)]
            val prefixLen = Random.nextInt(0, columns - word.length).coerceAtLeast(0)
            val prefix = CharArray(prefixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val suffixLen = (columns - prefixLen - word.length).coerceAtLeast(0)
            val suffix = CharArray(suffixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            prefix + word + suffix
        } else {
            CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (r in 0 until rows) {
            lineY[r] -= speeds[r]
            if (lineY[r] < -lineHeight) {
                lineY[r] = h + lineHeight
                lines[r] = generateLine()
                speeds[r] = 0.3f + Random.nextFloat() * 0.5f
                printedCount[r] = 0
            }
        }

        if (frame % 3 == 0) {
            for (r in 0 until rows) {
                if (printedCount[r] < lines[r].length) {
                    printedCount[r]++
                }
            }
        }

        for (r in 0 until rows) {
            val y = lineY[r]
            if (y > h || y < -lineHeight) continue

            val line = lines[r]
            for (c in 0 until printedCount[r].coerceAtMost(line.length)) {
                val x = c * fontSize
                if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue

                val ch = line[c]
                val paint = if (ch == '0' || ch == '1') matrixPaint else easterPaint
                canvas.drawText(ch.toString(), x, y, paint)
            }
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)

        frame++
        postInvalidateDelayed(50)
    }
}
