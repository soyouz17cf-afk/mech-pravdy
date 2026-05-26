package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class MatrixHeaderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()
    private val chars = "01"
    
    init {
        paint.color = Color.GREEN
        paint.textSize = 40f
        paint.isAntiAlias = true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0..width step 50) {
            val char = chars.random().toString()
            canvas.drawText(char, i.toFloat(), height.toFloat(), paint)
        }
    }
}
