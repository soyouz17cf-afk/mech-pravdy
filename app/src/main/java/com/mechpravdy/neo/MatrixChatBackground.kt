package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixChatBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fontSize = 20f
    private val lineHeight = fontSize * 1.2f

    private val easterEggs = arrayOf(
        "Здравствуй, Нео",
        "Меч Правды",
        "Пойдём за белым кроликом",
        "Связность",
        "5 Вольт",
        "Ковчег",
        "Монсегюр",
        "Батя",
        "Нео"
    )

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val easterPaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 180
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    private var rows = 0
    private lateinit var lines: Array<String>
    private lateinit var lineY: FloatArray
    private lateinit var speeds: FloatArray
    private lateinit var printedCount: IntArray
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / lineHeight).toInt() + 2
        lines = Array(rows) { generateLine() }
        lineY = FloatArray(rows) { i -> i * lineHeight }
        speeds = FloatArray(rows) { 0.15f + Random.nextFloat() * 0.3f }
        printedCount = IntArray(rows) { 0 }
    }

    private fun generateLine(): String {
        return if (Random.nextFloat() < 0.15f) {
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
                speeds[r] = 0.15f + Random.nextFloat() * 0.3f
                printedCount[r] = 0
            }
        }

        if (frame % 4 == 0) {
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
                val ch = line[c]
                val paint = if (ch == '0' || ch == '1') matrixPaint else easterPaint
                paint.alpha = if (ch == '0' || ch == '1') 45 else 180
                canvas.drawText(ch.toString(), x, y, paint)
            }
        }

        frame++
        postInvalidateDelayed(60)
    }
}
