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

    private val fontSize = 40f
    private val lineHeight = fontSize * 1.05f

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео"
    )

    private val paint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    private var maxRows = 0
    private lateinit var lines: Array<String>
    private lateinit var lineY: FloatArray
    private lateinit var printedCount: IntArray
    private lateinit var isPrinting: BooleanArray
    private lateinit var speeds: FloatArray

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        maxRows = (h / lineHeight).toInt() + 2
        lines = Array(maxRows) { generateLine() }
        lineY = FloatArray(maxRows) { i -> h + i * lineHeight }
        printedCount = IntArray(maxRows) { 0 }
        isPrinting = BooleanArray(maxRows) { true }
        speeds = FloatArray(maxRows) { 0.2f + Random.nextFloat() * 0.4f }
    }

    private fun generateLine() = if (Random.nextFloat() < 0.15f) {
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
            if (isPrinting[i]) {
                printedCount[i]++
                if (printedCount[i] >= lines[i].length) isPrinting[i] = false
            } else {
                lineY[i] -= speeds[i]
                if (lineY[i] < -lineHeight) {
                    lines[i] = generateLine()
                    lineY[i] = h + lineHeight
                    printedCount[i] = 0
                    isPrinting[i] = true
                    speeds[i] = 0.2f + Random.nextFloat() * 0.4f
                }
            }

            val y = lineY[i]
            if (y > h + lineHeight || y < -lineHeight) continue
            paint.alpha = 45
            val limit = printedCount[i].coerceAtMost(lines[i].length)
            for (c in 0 until limit) {
                val x = c * fontSize
                canvas.drawText(lines[i][c].toString(), x, y, paint)
            }
        }
        postInvalidateDelayed(60)
    }
}
