package com.mechpravdy.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val VOICE_REQUEST_CODE = 200
        private const val FILE_PICK_REQUEST = 300
    }

    // UI элементы
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var btnSearchModel: MaterialButton
    private lateinit var authKeyInput: EditText
    private lateinit var generateButton: MaterialButton
    private lateinit var tokenInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var sendButton: MaterialButton
    private lateinit var cameraButton: MaterialButton
    private lateinit var checkButton: MaterialButton
    private lateinit var capsuleButton: MaterialButton
    private lateinit var attachButton: MaterialButton
    private lateinit var voiceButton: MaterialButton
    private lateinit var chatOutput: TextView
    private lateinit var statusText: TextView
    private lateinit var statusDot: ImageView

    private var isModelReady = false
    private var authToken = ""
    private var accessToken = ""

    // Загрузка нативной библиотеки
    init {
        try {
            System.loadLibrary("llama")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
            addChatMessage("❌ Ошибка загрузки libllama.so: ${e.message}")
        }
    }

    private external fun llamaComplete(prompt: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setListeners()
        checkAndRequestPermissions()
    }

    private fun initViews() {
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSearchModel = findViewById(R.id.btnSearchModel)
        authKeyInput = findViewById(R.id.authKeyInput)
        generateButton = findViewById(R.id.generateButton)
        tokenInput = findViewById(R.id.tokenInput)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        cameraButton = findViewById(R.id.cameraButton)
        checkButton = findViewById(R.id.checkButton)
        capsuleButton = findViewById(R.id.capsuleButton)
        attachButton = findViewById(R.id.attachButton)
        voiceButton = findViewById(R.id.voiceButton)
        chatOutput = findViewById(R.id.chatOutput)
        statusText = findViewById(R.id.statusText)
        statusDot = findViewById(R.id.statusDot)
    }

    private fun setListeners() {
        btnGrantPermission.setOnClickListener { requestStoragePermission() }
        btnSearchModel.setOnClickListener { searchGgufFiles() }
        generateButton.setOnClickListener { generateToken() }
        sendButton.setOnClickListener { sendMessage() }
        cameraButton.setOnClickListener { openCamera() }
        checkButton.setOnClickListener { checkStatus() }
        capsuleButton.setOnClickListener { saveCapsule() }
        attachButton.setOnClickListener { pickFile() }
        voiceButton.setOnClickListener { startVoiceInput() }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Для Android 11+ нужно специальное разрежение для доступа ко всем файлам
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            updateStatus("Готов", "#00ff00")
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Доступ уже разрешён", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    private fun searchGgufFiles() {
        addChatMessage("🔍 Поиск .gguf файлов...")
        val ggufFiles = findGgufFiles()
        if (ggufFiles.isEmpty()) {
            addChatMessage("❌ .gguf файлы не найдены. Загрузите модель в папку Downloads или Documents")
        } else {
            addChatMessage("✅ Найдено ${ggufFiles.size} .gguf файлов:")
            ggufFiles.forEach { addChatMessage("   📁 ${it.name} (${it.length() / 1024 / 1024} MB)") }
        }
    }

    private fun findGgufFiles(): List<File> {
        val result = mutableListOf<File>()
        val directories = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            getExternalFilesDir(null)
        )
        directories.forEach { dir ->
            dir?.listFiles()?.filter { it.extension.equals("gguf", ignoreCase = true) }?.let { result.addAll(it) }
        }
        return result
    }

    private fun generateToken() {
        val key = authKeyInput.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, "Введите Authorization Key", Toast.LENGTH_SHORT).show()
            return
        }
        // Простая генерация токена (можно заменить на реальную)
        accessToken = key.take(8) + System.currentTimeMillis().toString().takeLast(6)
        tokenInput.setText(accessToken)
        authToken = key
        addChatMessage("✅ Токен сгенерирован: $accessToken")
        updateStatus("Авторизован", "#00ff00")
        isModelReady = true
    }

    private fun sendMessage() {
        val question = messageInput.text.toString().trim()
        if (question.isEmpty()) {
            Toast.makeText(this, "Введите вопрос", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isModelReady) {
            Toast.makeText(this, "Сначала авторизуйтесь (ТОКЕН)", Toast.LENGTH_SHORT).show()
            return
        }

        addChatMessage("👤 Вы: $question")
        messageInput.text.clear()
        updateStatus("Думаю...", "#ffaa00")

        Thread {
            val result = try {
                llamaComplete(question)
            } catch (e: Exception) {
                "Ошибка: ${e.message}"
            }

            runOnUiThread {
                addChatMessage("🤖 НЕО: $result")
                updateStatus("Готов", "#00ff00")
            }
        }.start()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return
        }
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Камера не доступна", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStatus() {
        val status = when {
            !isModelReady -> "❌ Не авторизован"
            accessToken.isEmpty() -> "❌ Нет токена"
            else -> "✅ Система готова. Модель: LLaMA"
        }
        addChatMessage("📊 СТАТУС: $status")
    }

    private fun saveCapsule() {
        val chatHistory = chatOutput.text.toString()
        if (chatHistory.isEmpty()) {
            Toast.makeText(this, "Нет данных для сохранения", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val file = File(getExternalFilesDir(null), "capsule_${System.currentTimeMillis()}.txt")
            file.writeText(chatHistory)
            addChatMessage("💾 Капсула сохранена: ${file.absolutePath}")
            Toast.makeText(this, "Капсула сохранена", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_PICK_REQUEST)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        startActivityForResult(intent, VOICE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VOICE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = result?.get(0) ?: ""
                    messageInput.setText(spokenText)
                    sendMessage()
                }
            }
            FILE_PICK_REQUEST -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    addChatMessage("📎 Файл прикреплён: ${uri?.path?.takeLast(30)}")
                }
            }
        }
    }

    private fun addChatMessage(msg: String) {
        val current = chatOutput.text.toString()
        chatOutput.text = if (current.isEmpty()) msg else "$current\n\n$msg"
        // Автопрокрутка вниз
        (chatOutput.parent as? android.widget.ScrollView)?.fullScroll(android.view.View.FOCUS_DOWN)
    }

    private fun updateStatus(text: String, colorHex: String) {
        statusText.text = text
        // Цвет точки можно установить через Drawable, но для простоты оставим зелёный/серый
        val color = if (colorHex == "#00ff00") android.graphics.Color.GREEN else android.graphics.Color.GRAY
        statusDot.setColorFilter(color)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                updateStatus("Готов", "#00ff00")
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
            } else {
                updateStatus("Нет прав", "#ff0000")
                Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_LONG).show()
            }
        }
    }
}
