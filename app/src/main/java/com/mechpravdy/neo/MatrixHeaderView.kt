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
    private val speed = 7f
    private val words = arrayOf("Нео", "Батя", "Меч Правды", "Ковчег", "Иди за белым кроликом")

    private val titlePaint = Paint().apply { color = Color.WHITE; textSize = 72f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val subtitlePaint = Paint().apply { color = Color.parseColor("#CCFFCC"); textSize = 26f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }
    private val matrixPaint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 120 }

    var neoActive = false
    var gigaChatMode = false
    var localMode = false
    var connectionLost = false

    private var neoButtonRect = RectF()
    private var localButtonRect = RectF()
    var onNeoClick: (() -> Unit)? = null
    var onLocalClick: (() -> Unit)? = null

    private var logoRect = RectF()
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
        val logoW = w * 0.45f; val logoH = h * 0.55f
        logoRect = RectF((w - logoW) / 2f, (h - logoH) / 2f, (w + logoW) / 2f, (h + logoH) / 2f)
        val btnW = logoW * 0.42f; val btnH = logoH * 0.22f; val btnY = logoRect.bottom + 4f
        neoButtonRect = RectF(logoRect.left, btnY, logoRect.left + btnW, btnY + btnH)
        localButtonRect = RectF(logoRect.right - btnW, btnY, logoRect.right, btnY + btnH)
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
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)
        frame++

        when (state) {
            0 -> {
                if (frame % 2 == 0) printed += 2
                if (printed >= currentLine.length) state = 1
            }
            1 -> {
                cursorY -= speed
                if (cursorY < -lineHeight) spawnLine()
            }
        }

        val limit = printed.coerceAtMost(currentLine.length)
        for (c in 0 until limit) {
            val x = c * fontSize; val y = cursorY
            if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
            canvas.drawText(currentLine[c].toString(), x, y, matrixPaint)
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)

        val btnPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val btnTextPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        btnPaint.color = if (gigaChatMode) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawRoundRect(neoButtonRect, 8f, 8f, btnPaint)
        canvas.drawText("ГИГАЧАТ", neoButtonRect.centerX(), neoButtonRect.centerY() + 6f, btnTextPaint)
        btnPaint.color = if (localMode) Color.parseColor("#FF8800") else Color.parseColor("#555555")
        canvas.drawRoundRect(localButtonRect, 8f, 8f, btnPaint)
        canvas.drawText("ДИПСИК", localButtonRect.centerX(), localButtonRect.centerY() + 6f, btnTextPaint)

        val trafficX = logoRect.right + 20f; val trafficY = logoRect.top + logoRect.height() * 0.15f
        val dotRadius = 14f; val dotSpacing = 30f
        val dotPaint = Paint().apply { isAntiAlias = true }
        dotPaint.color = if (neoActive) Color.parseColor("#00FF00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY, dotRadius, dotPaint)
        dotPaint.color = if (gigaChatMode && !connectionLost) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing, dotRadius, dotPaint)
        dotPaint.color = if (localMode) Color.parseColor("#FFCC00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing * 2, dotRadius, dotPaint)
        val labelPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 13f; typeface = Typeface.DEFAULT; isAntiAlias = true }
        canvas.drawText("НЕО", trafficX + 20f, trafficY + 5f, labelPaint)
        canvas.drawText("ГИГАЧАТ", trafficX + 20f, trafficY + dotSpacing + 5f, labelPaint)
        canvas.drawText("ДИПСИК", trafficX + 20f, trafficY + dotSpacing * 2 + 5f, labelPaint)

        postInvalidateDelayed(50)
    }

    override fun performClick(): Boolean { return super.performClick() }

    fun handleTouch(x: Float, y: Float) {
        if (neoButtonRect.contains(x, y)) onNeoClick?.invoke()
        else if (localButtonRect.contains(x, y)) onLocalClick?.invoke()
    }
}
