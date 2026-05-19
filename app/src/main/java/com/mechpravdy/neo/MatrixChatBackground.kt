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
    private val lineHeight = fontSize * 1.1f

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео"
    )

    private val paint = Paint().apply {
        color = Color.parseColor("#21A038"); textSize = fontSize
        typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 45
    }

    private var columns = 0
    private var currentLine = ""
    private var cursorY = 0f
    private var printed = 0
    private var floatSpeed = 0f
    private var state = 0
    private var h = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.h = h
        columns = (w / fontSize).toInt() + 1
        spawnLine()
    }

    private fun spawnLine() {
        currentLine = if (Random.nextFloat() < 0.15f) {
            val w = easterEggs[Random.nextInt(easterEggs.size)]
            val pre = CharArray(Random.nextInt(0, columns - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val suf = CharArray((columns - pre.length - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            pre + w + suf
        } else CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        cursorY = h + lineHeight
        printed = 0
        state = 0
        floatSpeed = 8f + Random.nextFloat() * 6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        when (state) {
            0 -> {
                printed += 2
                if (printed >= currentLine.length) state = 1
            }
            1 -> {
                cursorY -= floatSpeed
                if (cursorY < -lineHeight) spawnLine()
            }
        }

        for (c in 0 until printed.coerceAtMost(currentLine.length)) {
            canvas.drawText(currentLine[c].toString(), c * fontSize, cursorY, paint)
        }
        postInvalidateDelayed(60)
    }
}
