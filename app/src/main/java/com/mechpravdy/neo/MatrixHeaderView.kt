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
    private val lineHeight = fontSize * 1.05f

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео", "СБЕР", "ГигаЧат"
    )

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 120
    }
    private val titlePaint = Paint().apply {
        color = Color.WHITE; textSize = 72f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#CCFFCC"); textSize = 26f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }

    private var columns = 0
    private var maxRows = 0
    private lateinit var lines: Array<String>
    private lateinit var lineY: FloatArray
    private lateinit var printedCount: IntArray
    private lateinit var speeds: FloatArray

    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        maxRows = (h / lineHeight).toInt() + 2
        lines = Array(maxRows) { generateLine() }
        lineY = FloatArray(maxRows) { i -> h + i * lineHeight }
        printedCount = IntArray(maxRows) { 0 }
        speeds = FloatArray(maxRows) { 4.0f + Random.nextFloat() * 4.0f }

        val logoWidth = w * 0.55f; val logoHeight = h * 0.75f
        val left = (w - logoWidth) / 2f; val top = (h - logoHeight) / 2f
        logoRect = RectF(left, top, left + logoWidth, top + logoHeight)
    }

    private fun generateLine() = if (Random.nextFloat() < 0.2f) {
        val word = easterEggs[Random.nextInt(easterEggs.size)]
        val pre = CharArray(Random.nextInt(0, columns - word.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        val suf = CharArray((columns - pre.length - word.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        pre + word + suf
    } else {
        CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (i in 0 until maxRows) {
            val speed = speeds[i]
            val charsToAdd = speed.toInt().coerceAtLeast(4)
            if (printedCount[i] < lines[i].length) {
                printedCount[i] = (printedCount[i] + charsToAdd).coerceAtMost(lines[i].length)
            }
            lineY[i] -= speed

            if (lineY[i] < -lineHeight && printedCount[i] >= lines[i].length) {
                lines[i] = generateLine()
                lineY[i] = h + lineHeight
                printedCount[i] = 0
                speeds[i] = 4.0f + Random.nextFloat() * 4.0f
            }
        }

        for (i in 0 until maxRows) {
            val y = lineY[i]
            if (y > h + lineHeight || y < -lineHeight) continue
            val limit = printedCount[i].coerceAtMost(lines[i].length)
            for (c in 0 until limit) {
                val x = c * fontSize
                if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
                canvas.drawText(lines[i][c].toString(), x, y, matrixPaint)
            }
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)
        postInvalidateDelayed(50)
    }
}
