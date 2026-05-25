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

    // UI элементы
    private lateinit var inputField: EditText
    private lateinit var askButton: Button
    private lateinit var answerText: TextView

    // Флаг, что модель загружена
    private var isModelReady = false

    // Блок загрузки нативной библиотеки
    init {
        try {
            System.loadLibrary("llama")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    // Объявление нативной функции (та самая связь с libllama.so)
    private external fun llamaComplete(prompt: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Привязка UI элементов
        inputField = findViewById(R.id.editTextQuestion)
        askButton = findViewById(R.id.buttonAsk)
        answerText = findViewById(R.id.textViewAnswer)

        // Кнопка "Спросить"
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

        // Запрос разрешений при запуске
        checkAndRequestPermissions()
    }

    // Проверка и запрос разрешений для Android 14
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Для Android 13+ нужно новое разрешение на чтение медиа
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
            // Разрешения уже есть — запускаем модель
            initModel()
        }
    }

    // Обработка ответа пользователя на запрос разрешений
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
                // Всё равно пробуем запустить модель, но без некоторых функций
                initModel()
            }
        }
    }

    // Инициализация модели (после получения разрешений)
    private fun initModel() {
        isModelReady = true
        Toast.makeText(this, "Модель LLaMA загружена. Можно задавать вопросы.", Toast.LENGTH_SHORT).show()
        // Здесь можно добавить дополнительную инициализацию, если нужно
    }

    // Вызов модели и отображение ответа
    private fun askModel(question: String) {
        answerText.text = "Думаю..."
        askButton.isEnabled = false

        // Запускаем в отдельном потоке, чтобы не блокировать UI
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
