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
    // Очередь строк: [0] — самая нижняя (печатается или уже готова), [1] — выше, [2] — ещё выше
    private val lines = arrayOfNulls<String>(3)
    private val lineY = FloatArray(3)
    private val printedCount = IntArray(3)
    private val isPrinting = BooleanArray(3) { true }
    private var frame = 0

    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until 3) {
            lines[i] = generateLine()
            lineY[i] = h + i * lineHeight
            printedCount[i] = 0
            isPrinting[i] = true
        }
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

        // Печатаем самую нижнюю строку
        val bottomIdx = 0
        if (isPrinting[bottomIdx]) {
            printedCount[bottomIdx] += 3 // печатаем по 3 символа за кадр
            if (printedCount[bottomIdx] >= lines[bottomIdx]!!.length) {
                isPrinting[bottomIdx] = false
            }
        }

        // Если нижняя напечаталась полностью — сдвигаем все строки вверх
        if (!isPrinting[bottomIdx]) {
            // Сдвигаем все строки вверх на одну позицию
            for (i in 2 downTo 1) {
                lines[i] = lines[i - 1]
                lineY[i] = lineY[i - 1]
                printedCount[i] = printedCount[i - 1]
                isPrinting[i] = isPrinting[i - 1]
            }
            // Создаём новую строку снизу
            lines[0] = generateLine()
            lineY[0] = h + lineHeight
            printedCount[0] = 0
            isPrinting[0] = true
        }

        // Рисуем все строки
        for (i in 0 until 3) {
            val line = lines[i] ?: continue
            val y = lineY[i]
            if (y > h + lineHeight || y < -lineHeight) continue
            val limit = printedCount[i].coerceAtMost(line.length)
            for (c in 0 until limit) {
                val x = c * fontSize
                if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
                canvas.drawText(line[c].toString(), x, y, matrixPaint)
            }
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)
        postInvalidateDelayed(50)
    }
}
