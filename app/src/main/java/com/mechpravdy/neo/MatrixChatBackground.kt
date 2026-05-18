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

    private val chars = charArrayOf('0', '1')
    private val fontSize = 14f
    private val paint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 25
    }
    private val bgPaint = Paint().apply { color = Color.parseColor("#0A0A0A") }

    private var columns = 0
    private lateinit var drops: IntArray
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        drops = IntArray(columns) { Random.nextInt(-h.toInt() / fontSize.toInt(), 0) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (frame % 15 == 0) {
            for (i in 0 until columns) {
                drops[i]++
                if (drops[i] * fontSize > h && Random.nextFloat() > 0.97f) {
                    drops[i] = 0
                }
            }
        }

        for (i in 0 until columns) {
            val x = i * fontSize
            val y = drops[i] * fontSize
            val char = chars[Random.nextInt(2)]
            canvas.drawText(char.toString(), x, y, paint)
        }

        frame++
        postInvalidateDelayed(400)
    }
}
