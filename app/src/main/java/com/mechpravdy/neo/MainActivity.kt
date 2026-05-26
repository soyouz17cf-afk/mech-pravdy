package com.mechpravdy.neo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "Меч Правды\n\nПриложение работает, если ты это видишь."
        textView.setTextSize(20f)
        textView.setPadding(50, 50, 50, 50)
        
        setContentView(textView)
    }
}
