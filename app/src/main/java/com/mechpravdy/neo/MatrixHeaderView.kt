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
    private val lineHeight = fontSize * 1.05f
    private val printSpeed = 2

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео", "СБЕР", "ГигаЧат"
    )

    private val matrixPaint = Paint().apply {
        color = Color.parseColor("#21A038")
        textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 120
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
    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }

    private var columns = 0

    private var currentLine = ""
    private var currentY = 0f
    private var printedCount = 0
    private var isPrinting = true
    private var floatSpeed = 0f

    private var logoRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        spawnLine(h.toFloat())
        val lw = w * 0.55f; val lh = h * 0.75f
        val left = (w - lw) / 2f; val top = (h - lh) / 2f
        logoRect = RectF(left, top, left + lw, top + lh)
    }

    private fun spawnLine(h: Float) {
        currentLine = if (Random.nextFloat() < 0.2f) {
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
        floatSpeed = 0.5f + Random.nextFloat() * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (isPrinting) {
            printedCount = (printedCount + printSpeed).coerceAtMost(currentLine.length)
            if (printedCount >= currentLine.length) {
                isPrinting = false
            }
        } else {
            currentY -= floatSpeed
            if (currentY < -lineHeight) {
                spawnLine(h)
            }
        }

        val limit = printedCount.coerceAtMost(currentLine.length)
        for (c in 0 until limit) {
            val x = c * fontSize
            if (x >= logoRect.left && x <= logoRect.right && currentY >= logoRect.top && currentY <= logoRect.bottom) continue
            canvas.drawText(currentLine[c].toString(), x, currentY, matrixPaint)
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)
        postInvalidateDelayed(50)
    }
}
