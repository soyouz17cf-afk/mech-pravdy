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
    private val maxLines = 5

    private val easterEggs = arrayOf(
        "Здравствуй, Нео", "Меч Правды", "Пойдём за белым кроликом",
        "Связность", "5 Вольт", "Ковчег", "Монсегюр", "Батя", "Нео", "СБЕР", "ГигаЧат"
    )

    private val paint = Paint().apply { color = Color.parseColor("#21A038"); textSize = fontSize; typeface = Typeface.MONOSPACE; isAntiAlias = true; alpha = 120 }
    private val titlePaint = Paint().apply { color = Color.WHITE; textSize = 72f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val subtitlePaint = Paint().apply { color = Color.parseColor("#CCFFCC"); textSize = 26f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val logoBgPaint = Paint().apply { color = Color.parseColor("#1A8A2E") }

    // Светофор
    private var neoActive = true
    private var localActive = false
    private var connectionLost = false

    private var columns = 0
    private val lines = arrayOfNulls<String>(maxLines)
    private val lineY = FloatArray(maxLines)
    private val printed = IntArray(maxLines)
    private var logoRect = RectF()
    private var screenH = 0f
    private var frame = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenH = h.toFloat()
        columns = (w / fontSize).toInt() + 1
        for (i in 0 until maxLines) { lines[i] = generateLine(); lineY[i] = screenH + i * lineHeight; printed[i] = 0 }
        val logoW = w * 0.55f; val logoH = h * 0.75f
        logoRect = RectF((w - logoW) / 2f, (h - logoH) / 2f, (w + logoW) / 2f, (h + logoH) / 2f)
    }

    private fun generateLine() = if (Random.nextFloat() < 0.2f) {
        val word = easterEggs[Random.nextInt(easterEggs.size)]
        val pre = CharArray(Random.nextInt(0, columns - word.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        val suf = CharArray((columns - pre.length - word.length).coerceAtLeast(0)) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")
        pre + word + suf
    } else CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)
        frame++

        for (i in 0 until maxLines) { if (frame % 2 == 0 && printed[i] < (lines[i]?.length ?: 0)) printed[i] += 2; lineY[i] -= speed }
        if (lineY[0] < -lineHeight) { for (i in 0 until maxLines - 1) { lines[i] = lines[i + 1]; lineY[i] = lineY[i + 1]; printed[i] = printed[i + 1] }; lines[maxLines - 1] = generateLine(); lineY[maxLines - 1] = lineY[maxLines - 2] + lineHeight; printed[maxLines - 1] = 0 }

        for (i in 0 until maxLines) {
            val line = lines[i] ?: continue; val y = lineY[i]
            if (y > screenH + lineHeight || y < -lineHeight) continue
            val limit = printed[i].coerceAtMost(line.length)
            for (c in 0 until limit) { val x = c * fontSize; if (x >= logoRect.left && x <= logoRect.right && y >= logoRect.top && y <= logoRect.bottom) continue; canvas.drawText(line[c].toString(), x, y, paint) }
        }

        canvas.drawRoundRect(logoRect, 16f, 16f, logoBgPaint)
        canvas.drawText("СБЕР", w / 2, logoRect.top + logoRect.height() * 0.45f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, logoRect.top + logoRect.height() * 0.75f, subtitlePaint)

        // Светофор
        val trafficX = logoRect.right + 20f
        val trafficY = logoRect.top + logoRect.height() * 0.3f
        val dotRadius = 10f
        val dotSpacing = 28f

        val neoColor = if (neoActive) Color.parseColor("#00FF00") else Color.parseColor("#555555")
        val localColor = if (localActive) Color.parseColor("#FFCC00") else Color.parseColor("#555555")
        val lostColor = if (connectionLost) Color.parseColor("#FF0000") else Color.parseColor("#555555")

        val dotPaint = Paint().apply { isAntiAlias = true }
        dotPaint.color = neoColor; canvas.drawCircle(trafficX, trafficY, dotRadius, dotPaint)
        dotPaint.color = localColor; canvas.drawCircle(trafficX, trafficY + dotSpacing, dotRadius, dotPaint)
        dotPaint.color = lostColor; canvas.drawCircle(trafficX, trafficY + dotSpacing * 2, dotRadius, dotPaint)

        val labelPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 16f; typeface = Typeface.DEFAULT; isAntiAlias = true }
        canvas.drawText("НЕО", trafficX + 18f, trafficY + 6f, labelPaint)
        canvas.drawText("ЛОК", trafficX + 18f, trafficY + dotSpacing + 6f, labelPaint)
        canvas.drawText("СВЯЗЬ", trafficX + 18f, trafficY + dotSpacing * 2 + 6f, labelPaint)

        postInvalidateDelayed(50)
    }

    fun setNeoActive(active: Boolean) { neoActive = active; localActive = !active; connectionLost = false; invalidate() }
    fun setLocalActive(active: Boolean) { localActive = active; neoActive = !active; connectionLost = false; invalidate() }
    fun setConnectionLost(lost: Boolean) { connectionLost = lost; invalidate() }
}
