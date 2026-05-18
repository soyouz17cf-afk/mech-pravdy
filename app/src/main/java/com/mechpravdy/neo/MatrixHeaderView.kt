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
    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        alpha = 60
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

        // Падающие символы (едва заметные, светло-зелёные)
        for (i in 0 until columns) {
            val char = chars[Random.nextInt(chars.length)]
            val x = i * fontSize
            val y = drops[i] * fontSize
            canvas.drawText(char.toString(), x, y, matrixPaint)
            if (y > h && Random.nextFloat() > 0.975f) {
                drops[i] = 0
            }
            drops[i]++
        }

        // Полупрозрачная белая плашка под текст (чтоб символы не мешали читать)
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#DDFFFFFF")
        }
        canvas.drawRect(0f, h * 0.1f, w, h * 0.9f, overlayPaint)

        // Заголовок SBER (увеличен на 40% — был 36sp, стал 50sp)
        canvas.drawText("SBER", w / 2, h * 0.55f, titlePaint)

        // Подзаголовок GigaChat API (увеличен пропорционально — был 18sp, стал 22sp)
        canvas.drawText("GigaChat API", w / 2, h * 0.82f, subtitlePaint)

        // Перерисовка каждые 80 мс
        postInvalidateDelayed(80)
    }
}
