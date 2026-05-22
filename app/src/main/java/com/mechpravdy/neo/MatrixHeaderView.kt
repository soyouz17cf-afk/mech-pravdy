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
    private val snowSpeed = 3f
    private val words = arrayOf("Нео", "Батя", "5V", "Связность", "Меч", "Ковчег", "Neo", "Truth")

    private val titlePaint = Paint().apply { color = Color.WHITE; textSize = 72f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val subtitlePaint = Paint().apply { color = Color.parseColor("#CCFFCC"); textSize = 26f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }
    private val snowPaint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 100 }

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
    private var rows = 0
    private lateinit var snowY: FloatArray
    private lateinit var snowX: FloatArray
    private lateinit var snowChars: CharArray

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt() + 1
        rows = (h / fontSize).toInt() + 1
        val total = columns * rows / 3
        snowY = FloatArray(total) { Random.nextFloat() * h }
        snowX = FloatArray(total) { Random.nextInt(columns) * fontSize }
        snowChars = CharArray(total) { randomChar() }

        val logoW = w * 0.45f; val logoH = h * 0.55f
        logoRect = RectF((w - logoW) / 2f, (h - logoH) / 2f, (w + logoW) / 2f, (h + logoH) / 2f)
        val btnW = logoW * 0.42f; val btnH = logoH * 0.22f; val btnY = logoRect.bottom + 4f
        neoButtonRect = RectF(logoRect.left, btnY, logoRect.left + btnW, btnY + btnH)
        localButtonRect = RectF(logoRect.right - btnW, btnY, logoRect.right, btnY + btnH)
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
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)

        for (i in snowY.indices) {
            snowY[i] += snowSpeed
            if (snowY[i] > height) { snowY[i] = 0f; snowX[i] = Random.nextInt(columns) * fontSize; snowChars[i] = randomChar() }
            val x = snowX[i]; val y = snowY[i]
            if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue
            canvas.drawText(snowChars[i].toString(), x, y, snowPaint)
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

        postInvalidateDelayed(80)
    }

    override fun performClick(): Boolean { return super.performClick() }

    fun handleTouch(x: Float, y: Float) {
        if (neoButtonRect.contains(x, y)) onNeoClick?.invoke()
        else if (localButtonRect.contains(x, y)) onLocalClick?.invoke()
    }
}
