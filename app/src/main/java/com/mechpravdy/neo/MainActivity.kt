package com.mechpravdy.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    private lateinit var btnMistral3b: MaterialButton
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

    private lateinit var llamaBridge: LlamaBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setListeners()

        llamaBridge = LlamaBridge(this)

        checkPermissions()
        addChatMessage("⚡ Меч Правды загружен")
        addChatMessage("✅ Нажмите МИСТРАЛЬ 3Б и выберите .gguf файл")
    }

    private fun initViews() {
        btnMistral3b = findViewById(R.id.btnMistral3b)
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
        btnMistral3b.setOnClickListener { loadModel() }
        generateButton.setOnClickListener { generateToken() }
        sendButton.setOnClickListener { sendMessage() }
        cameraButton.setOnClickListener { openCamera() }
        checkButton.setOnClickListener { checkStatus() }
        capsuleButton.setOnClickListener { saveCapsule() }
        attachButton.setOnClickListener { pickFile() }
        voiceButton.setOnClickListener { startVoiceInput() }
    }

    private fun loadModel() {
        llamaBridge.loadModel(
            onProgress = { message -> addChatMessage(message) },
            onDone = { success ->
                if (success) {
                    addChatMessage("🎉 Модель загружена! Можно задавать вопросы.")
                } else {
                    addChatMessage("❌ Ошибка загрузки модели")
                }
            }
        )
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            updateStatus("Готов")
        }
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

        if (!llamaBridge.isLoaded) {
            addChatMessage("❌ Сначала загрузите модель (кнопка МИСТРАЛЬ 3Б)")
            return
        }

        addChatMessage("👤 $question")
        messageInput.text.clear()
        updateStatus("Думаю...")

        llamaBridge.generate(question,
            onToken = { answer -> addChatMessage("🤖 $answer") },
            onDone = { updateStatus("Готов") }
        )
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
        if (llamaBridge.isLoaded) addChatMessage("✅ Модель загружена")
        else addChatMessage("❌ Модель не загружена. Нажмите МИСТРАЛЬ 3Б")
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

    @Deprecated("Deprecated in Java")
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
    }

    private fun updateStatus(text: String) {
        statusText.text = text
        val color = if (text == "Готов") android.graphics.Color.GREEN else android.graphics.Color.GRAY
        statusDot.setColorFilter(color)
    }
}
