package com.mechpravdy.neo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class MatrixChatBackground(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()
    
    init {
        paint.color = Color.parseColor("#0a330a")
        paint.style = Paint.Style.FILL
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
