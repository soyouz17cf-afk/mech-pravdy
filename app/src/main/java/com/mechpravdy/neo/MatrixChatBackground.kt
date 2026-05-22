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
    private val speed = 7f
    private val maxLines = 6
    private val maxPoolSize = 15

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео"
    )

    private val paint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 45 }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    private val linePool = arrayOfNulls<String>(maxPoolSize)
    private val lineY = FloatArray(maxLines)
    private val printed = IntArray(maxLines)
    private var lineIndex = 0
    private var screenH = 0f
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenH = h.toFloat()
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until maxPoolSize) { linePool[i] = generateLine() }
        for (i in 0 until maxLines) { lineY[i] = screenH + i * lineHeight; printed[i] = 0 }
    }

    private fun generateLine() = if (Random.nextFloat() < 0.15f) { val w = easterEggs[Random.nextInt(easterEggs.size)]; val pre = CharArray(Random.nextInt(0, columns - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString(""); val suf = CharArray((columns - pre.length - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString(""); pre + w + suf } else CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        frame++

        for (i in 0 until maxLines) {
            if (frame % 3 == 0 && printed[i] < (linePool[(lineIndex + i) % maxPoolSize]?.length ?: 0)) printed[i] += 2
            lineY[i] -= speed
        }
        if (lineY[0] < -lineHeight) {
            for (i in 0 until maxLines - 1) { lineY[i] = lineY[i + 1]; printed[i] = printed[i + 1] }
            lineIndex = (lineIndex + maxLines) % maxPoolSize
            linePool[lineIndex] = generateLine()
            lineY[maxLines - 1] = lineY[maxLines - 2] + lineHeight
            printed[maxLines - 1] = 0
        }
        for (i in 0 until maxLines) {
            val line = linePool[(lineIndex + i) % maxPoolSize] ?: continue
            val y = lineY[i]
            if (y > screenH + lineHeight || y < -lineHeight) continue
            val limit = printed[i].coerceAtMost(line.length)
            for (c in 0 until limit) { canvas.drawText(line[c].toString(), c * fontSize, y, paint) }
        }
        postInvalidateDelayed(80) // медленнее для чата
    }
}
