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
    private val maxLines = 6
    private val maxPoolSize = 12
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
    private val linePool = arrayOfNulls<String>(maxPoolSize)
    private val linePoolIndex = IntArray(maxLines) { -1 }
    private val lineY = FloatArray(maxLines)
    private val printed = IntArray(maxLines)
    private var nextPoolSlot = 0
    private var screenH = 0f
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenH = h.toFloat()
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until maxPoolSize) { linePool[i] = generateLine() }
        for (i in 0 until maxLines) {
            linePoolIndex[i] = i % maxPoolSize
            lineY[i] = i * lineHeight * 2f
            printed[i] = 0
        }
        nextPoolSlot = maxLines % maxPoolSize

        // Логотип
        val logoW = w * 0.50f; val logoH = h * 0.40f
        logoRect = RectF((w - logoW) / 2f, 6f, (w + logoW) / 2f, 6f + logoH)

        // Кнопки — фиксированные, не налазят
        val btnW = w * 0.43f
        val btnH = 46f
        val btnY = logoRect.bottom + 4f
        val gap = 8f // зазор между кнопками
        val totalBtnW = btnW * 2 + gap
        val btnLeft = (w - totalBtnW) / 2f
        neoButtonRect = RectF(btnLeft, btnY, btnLeft + btnW, btnY + btnH)
        localButtonRect = RectF(btnLeft + btnW + gap, btnY, btnLeft + btnW + gap + btnW, btnY + btnH)
    }

    private fun generateLine() = if (Random.nextFloat() < 0.15f) { words[Random.nextInt(words.size)] } else { CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("") }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)
        frame++

        // Матрица — без изменений
        for (i in 0 until maxLines) {
            val poolIdx = linePoolIndex[i]
            if (poolIdx < 0) continue
            val line = linePool[poolIdx] ?: continue
            if (frame % 2 == 0 && printed[i] < line.length) printed[i] += 2
            lineY[i] -= speed
        }
        if (lineY[0] < -lineHeight) {
            for (i in 0 until maxLines - 1) {
                linePoolIndex[i] = linePoolIndex[i + 1]
                lineY[i] = lineY[i + 1]
                printed[i] = printed[i + 1]
            }
            linePool[nextPoolSlot] = generateLine()
            linePoolIndex[maxLines - 1] = nextPoolSlot
            lineY[maxLines - 1] = lineY[maxLines - 2] + lineHeight
            printed[maxLines - 1] = 0
            nextPoolSlot = (nextPoolSlot + 1) % maxPoolSize
        }
        for (i in 0 until maxLines) {
            val poolIdx = linePoolIndex[i]
            if (poolIdx < 0) continue
            val line = linePool[poolIdx] ?: continue
            val y = lineY[i]
            if (y > screenH + lineHeight || y < -lineHeight) continue
            val limit = printed[i].coerceAtMost(line.length)
            for (c in 0 until limit) {
                val x = c * fontSize
                if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
                canvas.drawText(line[c].toString(), x, y, matrixPaint)
            }
        }

        // Логотип
        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)

        // Кнопки — не налазят, зазор 8dp
        val btnPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        val btnTextPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        btnPaint.color = if (gigaChatMode) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawRoundRect(neoButtonRect, 10f, 10f, btnPaint)
        canvas.drawText("ГИГАЧАТ", neoButtonRect.centerX(), neoButtonRect.centerY() + 6f, btnTextPaint)
        btnPaint.color = if (localMode) Color.parseColor("#FF8800") else Color.parseColor("#555555")
        canvas.drawRoundRect(localButtonRect, 10f, 10f, btnPaint)
        canvas.drawText("МИСТРАЛЬ 3B", localButtonRect.centerX(), localButtonRect.centerY() + 6f, btnTextPaint)

        // Светофор — нормальное расстояние
        val dotRadius = 16f
        val dotSpacing = 34f
        val trafficX = logoRect.right + 18f
        val trafficY = logoRect.centerY() - dotSpacing

        val dotPaint = Paint().apply { isAntiAlias = true }
        dotPaint.color = if (neoActive) Color.parseColor("#00FF00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY, dotRadius, dotPaint)
        dotPaint.color = if (gigaChatMode && !connectionLost) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing, dotRadius, dotPaint)
        dotPaint.color = if (localMode) Color.parseColor("#FFCC00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing * 2, dotRadius, dotPaint)

        val labelPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 13f; typeface = Typeface.DEFAULT; isAntiAlias = true }
        canvas.drawText("НЕО", trafficX + 22f, trafficY + 5f, labelPaint)
        canvas.drawText("ГИГАЧАТ", trafficX + 22f, trafficY + dotSpacing + 5f, labelPaint)
        canvas.drawText("МИСТРАЛЬ", trafficX + 22f, trafficY + dotSpacing * 2 + 5f, labelPaint)

        postInvalidateDelayed(50)
    }

    override fun performClick(): Boolean { return super.performClick() }

    fun handleTouch(x: Float, y: Float) {
        if (neoButtonRect.contains(x, y)) onNeoClick?.invoke()
        else if (localButtonRect.contains(x, y)) onLocalClick?.invoke()
    }
}
