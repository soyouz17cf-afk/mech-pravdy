package com.mechpravdy.neo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var inputField: EditText
    private lateinit var askButton: Button
    private lateinit var answerText: TextView
    private var isModelReady = false

    init {
        try {
            System.loadLibrary("llama")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    private external fun llamaComplete(prompt: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.editTextQuestion)
        askButton = findViewById(R.id.buttonAsk)
        answerText = findViewById(R.id.textViewAnswer)

        askButton.setOnClickListener {
            if (!isModelReady) {
                Toast.makeText(this, "Модель ещё не загружена, подождите...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val question = inputField.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(this, "Введите вопрос", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askModel(question)
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initModel()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                initModel()
            } else {
                Toast.makeText(this, "Некоторые разрешения не предоставлены. Приложение может работать некорректно.", Toast.LENGTH_LONG).show()
                initModel()
            }
        }
    }

    private fun initModel() {
        isModelReady = true
        Toast.makeText(this, "Модель LLaMA загружена. Можно задавать вопросы.", Toast.LENGTH_SHORT).show()
    }

    private fun askModel(question: String) {
        answerText.text = "Думаю..."
        askButton.isEnabled = false

        Thread {
            val result = try {
                llamaComplete(question)
            } catch (e: Exception) {
                "Ошибка: ${e.localizedMessage ?: "неизвестная ошибка"}"
            }

            runOnUiThread {
                answerText.text = result
                askButton.isEnabled = true
            }
        }.start()
    }
}
