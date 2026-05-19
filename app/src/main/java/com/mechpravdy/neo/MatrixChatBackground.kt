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
    private val printSpeed = 2

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
    private var currentLine = ""
    private var currentY = 0f
    private var printedCount = 0
    private var isPrinting = true
    private var floatSpeed = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        spawnLine(h)
    }

    private fun spawnLine(h: Float) {
        currentLine = if (Random.nextFloat() < 0.15f) {
            val word = easterEggs[Random.nextInt(easterEggs.size)]
            val preLen = Random.nextInt(0, columns - word.length).coerceAtLeast(0)
            val pre = CharArray(preLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val sufLen = (columns - preLen - word.length).coerceAtLeast(0)
            val suf = CharArray(sufLen) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            pre + word + suf
        } else {
            CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        }
        currentY = h + lineHeight
        printedCount = 0
        isPrinting = true
        floatSpeed = 0.3f + Random.nextFloat() * 0.4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (isPrinting) {
            printedCount = (printedCount + printSpeed).coerceAtMost(currentLine.length)
            if (printedCount >= currentLine.length) isPrinting = false
        } else {
            currentY -= floatSpeed
            if (currentY < -lineHeight) spawnLine(h)
        }

        paint.alpha = 45
        val limit = printedCount.coerceAtMost(currentLine.length)
        for (c in 0 until limit) {
            val x = c * fontSize
            canvas.drawText(currentLine[c].toString(), x, currentY, paint)
        }
        postInvalidateDelayed(60)
    }
}
