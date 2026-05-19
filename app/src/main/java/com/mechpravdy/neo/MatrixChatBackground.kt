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

    private val paint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    // Три строки одновременно для заполнения экрана
    private val activeLines = arrayOfNulls<String>(3)
    private val lineY = FloatArray(3)
    private val printedCount = IntArray(3)
    private val isPrinting = BooleanArray(3) { true }
    private val floatSpeed = FloatArray(3)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until 3) {
            spawnNewLine(i, h)
            lineY[i] = h * (0.3f + i * 0.3f)
        }
    }

    private fun spawnNewLine(index: Int, h: Float) {
        activeLines[index] = if (Random.nextFloat() < 0.15f) {
            val word = easterEggs[Random.nextInt(easterEggs.size)]
            val prefixLen = Random.nextInt(0, columns - word.length).coerceAtLeast(0)
            val prefix = CharArray(prefixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val suffixLen = (columns - prefixLen - word.length).coerceAtLeast(0)
            val suffix = CharArray(suffixLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            prefix + word + suffix
        } else {
            CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        }
        printedCount[index] = 0
        isPrinting[index] = true
        floatSpeed[index] = 0.15f + Random.nextFloat() * 0.3f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (i in 0 until 3) {
            val line = activeLines[i] ?: continue

            if (isPrinting[i]) {
                printedCount[i]++
                if (printedCount[i] >= line.length) {
                    isPrinting[i] = false
                }
            } else {
                lineY[i] -= floatSpeed[i]
                if (lineY[i] < -lineHeight) {
                    lineY[i] = h + lineHeight
                    spawnNewLine(i, h)
                }
            }

            val y = lineY[i]
            paint.alpha = 45
            for (c in 0 until printedCount[i].coerceAtMost(line.length)) {
                val x = c * fontSize
                canvas.drawText(line[c].toString(), x, y, paint)
            }
        }

        postInvalidateDelayed(60)
    }
}
