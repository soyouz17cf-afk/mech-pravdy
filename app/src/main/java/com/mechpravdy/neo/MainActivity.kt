package com.mechpravdy.neo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Меч Правды\n\nПриложение работает"
        tv.setTextSize(20f)
        tv.setPadding(50, 50, 50, 50)
        setContentView(tv)
    }
}
