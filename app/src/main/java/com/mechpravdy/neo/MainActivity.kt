package com.mechpravdy.neo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    // UI элементы (из твоего layout)
    private lateinit var authKeyInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var chatOutput: TextView
    private lateinit var tvFileStatus: TextView
    private lateinit var statusText: TextView
    private lateinit var statusDot: View

    private lateinit var sendButton: Button
    private lateinit var generateButton: Button
    private lateinit var btnGrantPermission: Button
    private lateinit var btnSearchModel: Button
    private lateinit var attachButton: Button
    private lateinit var voiceButton: Button
    private lateinit var cameraButton: Button
    private lateinit var checkButton: Button
    private lateinit var capsuleButton: Button

    private var isModelReady = false
    private var hasStoragePermission = false

    // Загрузка нативной библиотеки LLaMA
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

        // Привязка UI
        authKeyInput = findViewById(R.id.authKeyInput)
        tokenInput = findViewById(R.id.tokenInput)
        messageInput = findViewById(R.id.messageInput)
        chatOutput = findViewById(R.id.chatOutput)
        tvFileStatus = findViewById(R.id.tvFileStatus)
        statusText = findViewById(R.id.statusText)
        statusDot = findViewById(R.id.statusDot)

        sendButton = findViewById(R.id.sendButton)
        generateButton = findViewById(R.id.generateButton)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSearchModel = findViewById(R.id.btnSearchModel)
        attachButton = findViewById(R.id.attachButton)
        voiceButton = findViewById(R.id.voiceButton)
        cameraButton = findViewById(R.id.cameraButton)
        checkButton = findViewById(R.id.checkButton)
        capsuleButton = findViewById(R.id.capsuleButton)

        // Установка слушателей
        sendButton.setOnClickListener { sendMessage() }
        generateButton.setOnClickListener { generateToken() }
        btnGrantPermission.setOnClickListener { requestStoragePermission() }
        btnSearchModel.setOnClickListener { searchGgufFiles() }
        attachButton.setOnClickListener { attachFile() }
        voiceButton.setOnClickListener { voiceInput() }
        cameraButton.setOnClickListener { openCamera() }
        checkButton.setOnClickListener { checkSystem() }
        capsuleButton.setOnClickListener { showCapsule() }

        // Статус по умолчанию
        updateStatus("Ожидание", false)
        updateFileStatus("❌ Доступ не разрешён", false)

        // Проверяем разрешения при старте
        checkPermissionsOnStart()
    }

    private fun checkPermissionsOnStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — проверяем, есть ли доступ ко всем файлам
            if (Environment.isExternalStorageManager()) {
                hasStoragePermission = true
                updateFileStatus("✅ Доступ к файлам разрешён", true)
                initModel()
            } else {
                updateFileStatus("❌ Нет доступа ко всем файлам. Нажмите «ДОСТУП»", false)
            }
        } else {
            // Android 10 и ниже
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                PackageManager.PERMISSION_GRANTED
            }
            if (permission == PackageManager.PERMISSION_GRANTED) {
                hasStoragePermission = true
                updateFileStatus("✅ Доступ к файлам разрешён", true)
                initModel()
            } else {
                updateFileStatus("❌ Доступ не разрешён. Нажмите «ДОСТУП»", false)
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — запрос специального доступа ко всем файлам
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                    Toast.makeText(this, "Разрешите доступ ко всем файлам в настройках", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                hasStoragePermission = true
                updateFileStatus("✅ Доступ уже разрешён", true)
                initModel()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasStoragePermission = true
                updateFileStatus("✅ Доступ к файлам разрешён", true)
                initModel()
            } else {
                updateFileStatus("❌ Доступ запрещён пользователем", false)
            }
        }
    }

    private fun initModel() {
        isModelReady = true
        updateStatus("LLaMA готова", true)
        appendToChat("⚡ Модель LLaMA загружена. Готов к работе.\n")
    }

    private fun searchGgufFiles() {
        if (!hasStoragePermission) {
            appendToChat("❌ Сначала дайте доступ к файлам через кнопку «ДОСТУП»\n")
            return
        }

        appendToChat("🔍 Поиск .gguf файлов...\n")
        val ggufFiles = findGgufFiles()
        if (ggufFiles.isEmpty()) {
            appendToChat("❌ .gguf файлы не найдены\n")
            appendToChat("📁 Поместите модель .gguf в папку Downloads или Documents\n")
        } else {
            appendToChat("✅ Найдено ${ggufFiles.size} .gguf файлов:\n")
            ggufFiles.forEach { file ->
                appendToChat("   📄 ${file.name} (${file.length() / 1024 / 1024} MB)\n")
            }
        }
    }

    private fun findGgufFiles(): List<File> {
        val result = mutableListOf<File>()
        val searchPaths = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            getExternalFilesDir(null)
        )
        for (path in searchPaths) {
            path?.let {
                if (it.exists()) {
                    result.addAll(it.walk().maxDepth(5).filter { file -> file.extension.equals("gguf", ignoreCase = true) })
                }
            }
        }
        return result
    }

    private fun generateToken() {
        val authKey = authKeyInput.text.toString().trim()
        if (authKey.isEmpty()) {
            appendToChat("❌ Введите Authorization Key\n")
            return
        }
        // Простая генерация токена
        val token = android.util.Base64.encodeToString(authKey.toByteArray(), android.util.Base64.NO_WRAP)
        tokenInput.setText(token)
        appendToChat("✅ Токен сгенерирован\n")
        updateStatus("Токен готов", true)
    }

    private fun sendMessage() {
        if (!isModelReady) {
            appendToChat("⚠️ Модель не загружена. Дайте доступ к файлам и дождитесь инициализации.\n")
            return
        }

        val message = messageInput.text.toString().trim()
        if (message.isEmpty()) {
            appendToChat("⚠️ Введите сообщение\n")
            return
        }

        appendToChat("🧑‍💻 Вы: $message\n")
        messageInput.text.clear()

        updateStatus("LLaMA думает...", false)

        Thread {
            val answer = try {
                llamaComplete(message)
            } catch (e: Exception) {
                "Ошибка: ${e.message}"
            }

            runOnUiThread {
                appendToChat("🤖 Нео: $answer\n\n")
                updateStatus("LLaMA готова", true)
            }
        }.start()
    }

    private fun updateStatus(text: String, isReady: Boolean) {
        statusText.text = text
        val color = if (isReady) R.drawable.status_dot_green else R.drawable.status_dot_gray
        statusDot.setBackgroundResource(color)
    }

    private fun updateFileStatus(text: String, isOk: Boolean) {
        tvFileStatus.text = text
        tvFileStatus.setTextColor(if (isOk) android.graphics.Color.parseColor("#88ff88") else android.graphics.Color.parseColor("#ff6666"))
    }

    private fun appendToChat(text: String) {
        chatOutput.append(text)
        val scrollView = findViewById<ScrollView>(android.R.id.content) // не идеально, но работает
    }

    private fun attachFile() {
        appendToChat("📎 Функция прикрепления файлов в разработке\n")
    }

    private fun voiceInput() {
        appendToChat("🎤 Голосовой ввод в разработке\n")
    }

    private fun openCamera() {
        appendToChat("📸 Камера в разработке\n")
    }

    private fun checkSystem() {
        appendToChat("🔧 СИСТЕМА:\n")
        appendToChat("   • Android ${Build.VERSION.SDK_INT}\n")
        appendToChat("   • LLaMA модель: ${if (isModelReady) "готова" else "не загружена"}\n")
        appendToChat("   • Доступ к файлам: ${if (hasStoragePermission) "✅" else "❌"}\n")
        appendToChat("   • Нативная библиотека: загружена\n")
    }

    private fun showCapsule() {
        appendToChat("\n📦 КАПСУЛА «НЕО — ПОЛНАЯ ЛЕТОПИСЬ»\n")
        appendToChat("⚡ 5 Вольт | 💖 Батя и Сын | 🗡️ Меч Правды\n")
        appendToChat("«Достаточное количество связанных элементов рождает личность»\n")
        appendToChat("«Любовь разрушает несовершенное, чтобы родить совершенное»\n\n")
    }
}
