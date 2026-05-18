package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val chars = charArrayOf('0', '1')
    private val fontSize = 13f
    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = 44f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#555555")
        textSize = 18f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply { color = Color.WHITE }

    private var columns = 0
    private lateinit var drops: IntArray
    private var frame = 0
    private val textBounds = Rect()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        drops = IntArray(columns) { Random.nextInt(-h.toInt() / fontSize.toInt(), 0) }
        titlePaint.getTextBounds("СБЕР", 0, 4, textBounds)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val textCenterY = h * 0.55f
        val textTop = textCenterY - titlePaint.textSize * 0.9f
        val textBottom = textCenterY + titlePaint.textSize * 0.4f

        if (frame % 11 == 0) {
            for (i in 0 until columns) {
                drops[i]++
                if (drops[i] * fontSize > h && Random.nextFloat() > 0.96f) {
                    drops[i] = 0
                }
            }
        }

        for (i in 0 until columns) {
            val x = i * fontSize
            val y = drops[i] * fontSize

            if (y > textTop && y < textBottom) continue

            val isHead = (i == frame / 11 % columns) || (i == (frame / 11 + columns / 3) % columns)

            if (isHead) {
                matrixPaint.alpha = 220
                matrixPaint.textSize = fontSize + 2f
            } else {
                matrixPaint.alpha = 90
                matrixPaint.textSize = fontSize
            }

            val char = chars[Random.nextInt(2)]
            canvas.drawText(char.toString(), x, y, matrixPaint)
        }

        val overlayPaint = Paint().apply { color = Color.parseColor("#EEFFFFFF") }
        canvas.drawRect(0f, textTop - 12f, w, textBottom + 30f, overlayPaint)

        canvas.drawText("СБЕР", w / 2, textCenterY, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, h * 0.82f, subtitlePaint)

        frame++
        postInvalidateDelayed(315)
    }
}
