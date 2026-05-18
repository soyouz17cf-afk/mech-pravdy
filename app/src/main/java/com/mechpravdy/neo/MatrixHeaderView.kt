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
    private val fontSize = 24f // было 18, увеличили на 30%
    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 100 // было 60, увеличили для заметности
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = 50f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#555555")
        textSize = 22f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply {
        color = Color.WHITE
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

        // Белый фон
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Падающие символы — обновляются каждый 3-й кадр (было каждый кадр)
        // Это замедляет поток в ~3 раза относительно предыдущего
        if (frame % 3 == 0) {
            for (i in 0 until columns) {
                val char = chars[Random.nextInt(chars.length)]
                val x = i * fontSize
                val y = drops[i] * fontSize

                // Головной символ ярче
                if (i == frame / 3 % columns || i == (frame / 3 + columns / 2) % columns) {
                    matrixPaint.alpha = 200
                } else {
                    matrixPaint.alpha = 100
                }

                canvas.drawText(char.toString(), x, y, matrixPaint)
            }
        }

        // Обновление позиций — медленнее: каждый символ падает на 1 строку раз в 3 кадра
        if (frame % 3 == 0) {
            for (i in 0 until columns) {
                drops[i]++
                if (drops[i] * fontSize > h && Random.nextFloat() > 0.97f) {
                    drops[i] = 0
                }
            }
        }

        // Полупрозрачная белая плашка под текст
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#DDFFFFFF")
        }
        canvas.drawRect(0f, h * 0.1f, w, h * 0.9f, overlayPaint)

        // Заголовок SBER
        canvas.drawText("SBER", w / 2, h * 0.55f, titlePaint)

        // Подзаголовок GigaChat API
        canvas.drawText("GigaChat API", w / 2, h * 0.82f, subtitlePaint)

        frame++
        postInvalidateDelayed(160) // было 80, теперь 160 — в 2 раза медленнее обновление экрана
    }
}
