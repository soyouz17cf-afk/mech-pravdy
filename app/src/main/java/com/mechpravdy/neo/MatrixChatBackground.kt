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
    private val speed = 4f
    private val words = arrayOf("Нео", "Батя", "Меч Правды", "Ковчег", "Иди за белым кроликом")

    private val paint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 45 }

    private var columns = 0
    private var currentLine = ""
    private var cursorY = 0f
    private var printed = 0
    private var state = 0
    private var screenH = 0f
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenH = h.toFloat()
        columns = (w / fontSize).toInt() + 1
        spawnLine()
    }

    private fun spawnLine() {
        currentLine = if (Random.nextFloat() < 0.15f) {
            words[Random.nextInt(words.size)]
        } else {
            CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        }
        cursorY = screenH + lineHeight
        printed = 0
        state = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        frame++

        when (state) {
            0 -> {
                if (frame % 3 == 0) printed += 2
                if (printed >= currentLine.length) state = 1
            }
            1 -> {
                cursorY -= speed
                if (cursorY < -lineHeight) spawnLine()
            }
        }

        val limit = printed.coerceAtMost(currentLine.length)
        for (c in 0 until limit) {
            canvas.drawText(currentLine[c].toString(), c * fontSize, cursorY, paint)
        }
        postInvalidateDelayed(100)
    }
}
