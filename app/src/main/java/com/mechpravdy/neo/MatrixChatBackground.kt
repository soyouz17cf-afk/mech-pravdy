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
    // 5 строк для заполнения экрана
    private val lines = arrayOfNulls<String>(5)
    private val lineY = FloatArray(5)
    private val printedCount = IntArray(5)
    private val isPrinting = BooleanArray(5) { true }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until 5) {
            lines[i] = generateLine()
            lineY[i] = h + i * lineHeight
            printedCount[i] = 0
            isPrinting[i] = true
        }
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

        // Печатаем самую нижнюю
        if (isPrinting[0]) {
            printedCount[0] += 3
            if (printedCount[0] >= lines[0]!!.length) isPrinting[0] = false
        }

        // Сдвиг вверх
        if (!isPrinting[0]) {
            for (i in 4 downTo 1) {
                lines[i] = lines[i - 1]
                lineY[i] = lineY[i - 1]
                printedCount[i] = printedCount[i - 1]
                isPrinting[i] = isPrinting[i - 1]
            }
            lines[0] = generateLine()
            lineY[0] = h + lineHeight
            printedCount[0] = 0
            isPrinting[0] = true
        }

        // Рисуем
        for (i in 0 until 5) {
            val line = lines[i] ?: continue
            val y = lineY[i]
            if (y > h + lineHeight || y < -lineHeight) continue
            paint.alpha = 45
            val limit = printedCount[i].coerceAtMost(line.length)
            for (c in 0 until limit) {
                val x = c * fontSize
                canvas.drawText(line[c].toString(), x, y, paint)
            }
        }
        postInvalidateDelayed(60)
    }
}
