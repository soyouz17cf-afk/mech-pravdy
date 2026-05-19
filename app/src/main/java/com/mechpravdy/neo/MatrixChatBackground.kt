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

    private val fontSize = 18f
    private val lineHeight = fontSize * 1.2f

    private val paint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    private var rows = 0
    private lateinit var lines: Array<CharArray>
    private lateinit var lineY: FloatArray
    private lateinit var speeds: FloatArray
    private lateinit var printedCount: IntArray
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / lineHeight).toInt() + 2
        lines = Array(rows) { CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' } }
        lineY = FloatArray(rows) { i -> i * lineHeight }
        speeds = FloatArray(rows) { 0.15f + Random.nextFloat() * 0.3f }
        printedCount = IntArray(rows) { 0 }
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
                for (c in 0 until columns) {
                    lines[r][c] = if (Random.nextFloat() > 0.5f) '0' else '1'
                }
                speeds[r] = 0.15f + Random.nextFloat() * 0.3f
                printedCount[r] = 0
            }
        }

        // Эффект печати: каждые 4 кадра добавляем символ
        if (frame % 4 == 0) {
            for (r in 0 until rows) {
                if (printedCount[r] < columns) {
                    printedCount[r]++
                }
            }
        }

        for (r in 0 until rows) {
            val y = lineY[r]
            if (y > h || y < -lineHeight) continue
            for (c in 0 until columns) {
                if (c >= printedCount[r]) continue
                val x = c * fontSize
                paint.alpha = 45
                canvas.drawText(lines[r][c].toString(), x, y, paint)
            }
        }

        frame++
        postInvalidateDelayed(60)
    }
}
