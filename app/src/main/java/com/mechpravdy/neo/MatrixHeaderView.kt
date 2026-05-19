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
    private val lineHeight = fontSize * 1.1f

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео", "СБЕР", "ГигаЧат"
    )

    private val paint = Paint().apply {
        color = Color.parseColor("#21A038"); textSize = fontSize
        typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 120
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

    private var columns = 0
    private var currentLine = ""
    private var cursorY = 0f
    private var printed = 0
    private var state = 0
    private var logoRect = RectF()
    private var screenH = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.screenH = h.toFloat()
        columns = (w / fontSize).toInt() + 1
        spawnLine()
        val logoW = w * 0.55f; val logoH = h * 0.75f
        logoRect = RectF((w - logoW) / 2f, (h - logoH) / 2f, (w + logoW) / 2f, (h + logoH) / 2f)
    }

    private fun spawnLine() {
        currentLine = if (Random.nextFloat() < 0.2f) {
            val w = easterEggs[Random.nextInt(easterEggs.size)]
            val pre = CharArray(Random.nextInt(0, columns - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            val suf = CharArray((columns - pre.length - w.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
            pre + w + suf
        } else CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        cursorY = screenH + lineHeight
        printed = 0
        state = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)

        when (state) {
            0 -> {
                printed += 2
                if (printed >= currentLine.length) state = 1
            }
            1 -> {
                cursorY -= 7f
                if (cursorY < -lineHeight) spawnLine()
            }
        }

        for (c in 0 until printed.coerceAtMost(currentLine.length)) {
            val x = c * fontSize
            if (x >= logoRect.left && x <= logoRect.right && cursorY >= logoRect.top && cursorY <= logoRect.bottom) continue
            canvas.drawText(currentLine[c].toString(), x, cursorY, paint)
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, Paint().apply { color = Color.parseColor("#1A8A2E") })
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)
        postInvalidateDelayed(50)
    }
}
