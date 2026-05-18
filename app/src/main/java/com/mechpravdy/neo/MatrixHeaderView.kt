package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val chars = "アイウエオカキクケコサシスセソタチツテトナニヌネノ0123456789"
    private val fontSize = 18f
    private val paint = Paint().apply {
        color = Color.parseColor("#009900")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#888888")
        textSize = 18f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0A0A0A")
    }

    private var columns = 0
    private lateinit var drops: IntArray
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt()
        drops = IntArray(columns) { Random.nextInt(-h.toInt() / fontSize.toInt(), 0) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Фон
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Падающие символы
        for (i in 0 until columns) {
            val char = chars[Random.nextInt(chars.length)]
            val x = i * fontSize
            val y = drops[i] * fontSize
            val alpha = if (y > h * 0.6f && Random.nextFloat() > 0.95f) 255 else 150
            paint.alpha = alpha
            canvas.drawText(char.toString(), x, y, paint)
            if (y > h && Random.nextFloat() > 0.975f) {
                drops[i] = 0
            }
            drops[i]++
        }

        // Полупрозрачная плашка под текст
        val textBgPaint = Paint().apply {
            color = Color.parseColor("#CC0A0A0A")
        }
        canvas.drawRect(0f, h * 0.15f, w, h * 0.85f, textBgPaint)

        // Заголовок SBER
        canvas.drawText("SBER", w / 2, h * 0.52f, titlePaint)

        // Подзаголовок GigaChat API
        canvas.drawText("GigaChat API", w / 2, h * 0.78f, subtitlePaint)

        // Перерисовка каждые 80 мс
        frame++
        postInvalidateDelayed(80)
    }
}
