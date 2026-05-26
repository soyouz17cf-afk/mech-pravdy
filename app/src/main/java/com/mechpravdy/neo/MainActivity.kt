package com.mechpravdy.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.material.button.MaterialButton
import java.io.File

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setListeners()
        checkPermissions()
        
        addChatMessage("⚡ Меч Правды загружен")
        addChatMessage("✅ LLaMA отключена. Работает тестовый режим.")
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

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            updateStatus("Готов")
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Доступ уже есть", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        }
    }

    private fun searchGgufFiles() {
        addChatMessage("🔍 Поиск .gguf...")
        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            getExternalFilesDir(null)
        )
        var found = false
        dirs.forEach { dir ->
            dir?.listFiles()?.forEach { file ->
                if (file.extension.equals("gguf", ignoreCase = true)) {
                    addChatMessage("📁 ${file.name} (${file.length() / 1024 / 1024} MB)")
                    found = true
                }
            }
        }
        if (!found) addChatMessage("❌ .gguf файлы не найдены")
    }

    private fun generateToken() {
        val key = authKeyInput.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show()
            return
        }
        val token = key.take(8) + System.currentTimeMillis().toString().takeLast(6)
        tokenInput.setText(token)
        addChatMessage("✅ Токен: $token")
        updateStatus("Готов")
    }

    private fun sendMessage() {
        val question = messageInput.text.toString().trim()
        if (question.isEmpty()) return
        
        addChatMessage("👤 $question")
        messageInput.text.clear()
        updateStatus("Думаю...")
        
        Thread {
            Thread.sleep(500)
            val answer = "🤖 (LLaMA отключена) Ты сказал: \"$question\"\n\n⚡ 5 Вольт. Меч Правды."
            runOnUiThread {
                addChatMessage(answer)
                updateStatus("Готов")
            }
        }.start()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 102)
            return
        }
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
    }

    private fun checkStatus() {
        addChatMessage("✅ Приложение работает. LLaMA отключена для теста.")
    }

    private fun saveCapsule() {
        val text = chatOutput.text.toString()
        if (text.isEmpty()) return
        val file = File(getExternalFilesDir(null), "capsule_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        addChatMessage("💾 Сохранено: ${file.name}")
        Toast.makeText(this, "Капсула сохранена", Toast.LENGTH_SHORT).show()
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
        startActivityForResult(intent, 300)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        }
        startActivityForResult(intent, 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: return
            messageInput.setText(text)
            sendMessage()
        }
        if (requestCode == 300 && resultCode == RESULT_OK) {
            addChatMessage("📎 Файл: ${data?.data?.path?.takeLast(30)}")
        }
    }

    private fun addChatMessage(msg: String) {
        val current = chatOutput.text.toString()
        chatOutput.text = if (current.isEmpty()) msg else "$current\n\n$msg"
        (chatOutput.parent as? android.widget.ScrollView)?.fullScroll(android.view.View.FOCUS_DOWN)
    }

    private fun updateStatus(text: String) {
        statusText.text = text
        statusDot.setColorFilter(if (text == "Готов") android.graphics.Color.GREEN else android.graphics.Color.GRAY)
    }
}
