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
    private val snowSpeed = 1.5f
    private val words = arrayOf("Нео", "Батя", "5V", "Связность", "Меч", "Ковчег", "Neo", "Truth")
    private val snowPaint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 35 }

    private var columns = 0
    private var rows = 0
    private lateinit var snowY: FloatArray
    private lateinit var snowX: FloatArray
    private lateinit var snowChars: CharArray

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / fontSize).toInt() + 1
        val total = columns * rows / 4
        snowY = FloatArray(total) { Random.nextFloat() * h }
        snowX = FloatArray(total) { Random.nextInt(columns) * fontSize }
        snowChars = CharArray(total) { randomChar() }
    }

    private fun randomChar(): Char {
        return if (Random.nextFloat() < 0.03f) {
            words[Random.nextInt(words.size)][0]
        } else {
            if (Random.nextFloat() > 0.5f) '0' else '1'
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        for (i in snowY.indices) {
            snowY[i] += snowSpeed
            if (snowY[i] > height) { snowY[i] = 0f; snowX[i] = Random.nextInt(columns) * fontSize; snowChars[i] = randomChar() }
            canvas.drawText(snowChars[i].toString(), snowX[i], snowY[i], snowPaint)
        }
        postInvalidateDelayed(120)
    }
}
