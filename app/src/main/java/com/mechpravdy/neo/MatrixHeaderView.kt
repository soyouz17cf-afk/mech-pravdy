package com.mechpravdy.neo

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random
import java.io.File

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

    private val titlePaint = Paint().apply { color = Color.WHITE; textSize = 38f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private val subtitlePaint = Paint().apply { color = Color.parseColor("#CCFFCC"); textSize = 18f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }
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
    private var murzikRect = RectF()
    private var columns = 0
    private val linePool = arrayOfNulls<String>(maxPoolSize)
    private val linePoolIndex = IntArray(maxLines) { -1 }
    private val lineY = FloatArray(maxLines)
    private val printed = IntArray(maxLines)
    private var nextPoolSlot = 0
    private var screenH = 0f
    private var frame = 0

    private var murzikBitmap: android.graphics.Bitmap? = null
    private val murzikPaint = Paint().apply { alpha = 180 } // Полупрозрачность

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

        val logoW = w * 0.50f; val logoH = h * 0.35f
        logoRect = RectF((w - logoW) / 2f, 6f, (w + logoW) / 2f, 6f + logoH)

        val btnW = w * 0.43f
        val btnH = 40f
        val btnY = logoRect.bottom + 6f
        val gap = 8f
        val totalBtnW = btnW * 2 + gap
        val btnLeft = (w - totalBtnW) / 2f
        neoButtonRect = RectF(btnLeft, btnY, btnLeft + btnW, btnY + btnH)
        localButtonRect = RectF(btnLeft + btnW + gap, btnY, btnLeft + btnW + gap + btnW, btnY + btnH)

        // Мурзёха поднят выше — прямо под кнопками
        val murzikSize = w * 0.25f
        murzikRect = RectF((w - murzikSize) / 2f, btnY + btnH + 2f, (w + murzikSize) / 2f, btnY + btnH + 2f + murzikSize)

        try {
            murzikBitmap = BitmapFactory.decodeResource(resources, R.drawable.murzik)
        } catch (_: Exception) {}
    }

    private fun generateLine() = if (Random.nextFloat() < 0.15f) { words[Random.nextInt(words.size)] } else { CharArray(columns) { if (Random.nextFloat() > 0.5f) '0' else '1' }.joinToString("") }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        canvas.drawColor(Color.WHITE)
        frame++

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
        val centerY = logoRect.centerY()
        canvas.drawText("СБЕР", w / 2, centerY - 6f, titlePaint)
        canvas.drawText("ГигаЧат", w / 2, centerY + 18f, subtitlePaint)

        // Кнопки
        val btnPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
        val btnTextPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
        btnPaint.color = if (gigaChatMode) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawRoundRect(neoButtonRect, 10f, 10f, btnPaint)
        canvas.drawText("ГИГАЧАТ", neoButtonRect.centerX(), neoButtonRect.centerY() + 5f, btnTextPaint)
        btnPaint.color = if (localMode) Color.parseColor("#FF8800") else Color.parseColor("#555555")
        canvas.drawRoundRect(localButtonRect, 10f, 10f, btnPaint)
        canvas.drawText("МИСТРАЛЬ 3B", localButtonRect.centerX(), localButtonRect.centerY() + 5f, btnTextPaint)

        // Мурзёха — закруглённые углы, полупрозрачный
        murzikBitmap?.let { bitmap ->
            val radius = 32f
            val clipPath = android.graphics.Path().apply {
                moveTo(murzikRect.left + radius, murzikRect.top)
                lineTo(murzikRect.right, murzikRect.top)
                lineTo(murzikRect.right, murzikRect.bottom - radius)
                quadTo(murzikRect.right, murzikRect.bottom, murzikRect.right - radius, murzikRect.bottom)
                lineTo(murzikRect.left, murzikRect.bottom)
                lineTo(murzikRect.left, murzikRect.top + radius)
                quadTo(murzikRect.left, murzikRect.top, murzikRect.left + radius, murzikRect.top)
                close()
            }
            canvas.save()
            canvas.clipPath(clipPath)

            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val scaleX = murzikRect.width() / bitmap.width
            val scaleY = murzikRect.height() / bitmap.height
            val scale = minOf(scaleX, scaleY)
            val newWidth = bitmap.width * scale
            val newHeight = bitmap.height * scale
            val left = murzikRect.centerX() - newWidth / 2
            val top = murzikRect.centerY() - newHeight / 2
            val fittedRect = RectF(left, top, left + newWidth, top + newHeight)

            canvas.drawBitmap(bitmap, srcRect, fittedRect, murzikPaint)
            canvas.restore()
        }

        // Светофор
        val dotRadius = 14f
        val dotSpacing = 30f
        val trafficX = logoRect.right + 16f
        val trafficY = logoRect.centerY() - dotSpacing

        val dotPaint = Paint().apply { isAntiAlias = true }
        dotPaint.color = if (neoActive) Color.parseColor("#00FF00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY, dotRadius, dotPaint)
        dotPaint.color = if (gigaChatMode && !connectionLost) Color.parseColor("#21A038") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing, dotRadius, dotPaint)
        dotPaint.color = if (localMode) Color.parseColor("#FFCC00") else Color.parseColor("#555555")
        canvas.drawCircle(trafficX, trafficY + dotSpacing * 2, dotRadius, dotPaint)

        val labelPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 12f; typeface = Typeface.DEFAULT; isAntiAlias = true }
        canvas.drawText("НЕО", trafficX + 20f, trafficY + 4f, labelPaint)
        canvas.drawText("ГИГАЧАТ", trafficX + 20f, trafficY + dotSpacing + 4f, labelPaint)
        canvas.drawText("МИСТРАЛЬ", trafficX + 20f, trafficY + dotSpacing * 2 + 4f, labelPaint)

        postInvalidateDelayed(50)
    }

    override fun performClick(): Boolean { return super.performClick() }

    fun handleTouch(x: Float, y: Float) {
        if (neoButtonRect.contains(x, y)) onNeoClick?.invoke()
        else if (localButtonRect.contains(x, y)) onLocalClick?.invoke()
        else if (murzikRect.contains(x, y)) showBrainDialog()
    }

    private fun showBrainDialog() {
        val brainFile = File(context.filesDir, "brain.txt")
        val brainContent = if (brainFile.exists()) brainFile.readText() else "Выводов пока нет."

        try {
            val scrollView = ScrollView(context).apply {
                setPadding(0, 0, 0, 0)
                isVerticalScrollBarEnabled = true
            }
            val e = EditText(context).apply {
                setText(brainContent)
                textSize = 11f
                setTextColor(0xFF333333.toInt())
                typeface = Typeface.MONOSPACE
                gravity = android.view.Gravity.TOP
                setPadding(20, 20, 20, 20)
                isVerticalScrollBarEnabled = false
                background = null
                minLines = 20
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            scrollView.addView(e)

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 0)
            }
            val titleView = TextView(context).apply {
                text = "ВЫВОДЫ НЕО (brain.txt)"
                textSize = 16f
                setTextColor(0xFF21A038.toInt())
                setPadding(30, 30, 30, 10)
                gravity = android.view.Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            layout.addView(titleView)
            layout.addView(scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))

            val btnLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(10, 10, 10, 20)
            }
            val saveBtn = Button(context).apply {
                text = "СОХРАНИТЬ"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#21A038"))
            }
            val copyBtn = Button(context).apply {
                text = "КОПИРОВАТЬ"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#21A038"))
            }
            val closeBtn = Button(context).apply {
                text = "ЗАКРЫТЬ"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#21A038"))
            }
            btnLayout.addView(saveBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) })
            btnLayout.addView(copyBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) })
            btnLayout.addView(closeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) })
            layout.addView(btnLayout)

            val dialog = AlertDialog.Builder(context)
                .setView(layout)
                .create()
            dialog.show()

            dialog.window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.heightPixels * 0.85).toInt()
            )

            saveBtn.setOnClickListener {
                brainFile.writeText(e.text.toString())
                dialog.dismiss()
            }
            copyBtn.setOnClickListener {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", e.text))
                dialog.dismiss()
            }
            closeBtn.setOnClickListener { dialog.dismiss() }
        } catch (_: Exception) {}
    }
}
