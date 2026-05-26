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

    private lateinit var btnSelectFolder: MaterialButton
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
    private lateinit var tvFileStatus: TextView

    private lateinit var llamaBridge: LlamaBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setListeners()

        llamaBridge = LlamaBridge(this)

        checkPermissions()
        addChatMessage("⚡ Меч Правды загружен")
        addChatMessage("✅ Нажми «ВЫБРАТЬ ПАПКУ С МОДЕЛЬЮ» для скачивания мозга")
    }

    private fun initViews() {
        btnSelectFolder = findViewById(R.id.btnSelectFolder)
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
        tvFileStatus = findViewById(R.id.tvFileStatus)
    }

    private fun setListeners() {
        btnSelectFolder.setOnClickListener { loadModel() }
        btnGrantPermission.setOnClickListener { grantPermission() }
        btnSearchModel.setOnClickListener { searchModel() }
        generateButton.setOnClickListener { generateToken() }
        sendButton.setOnClickListener { sendMessage() }
        cameraButton.setOnClickListener { openCamera() }
        checkButton.setOnClickListener { checkStatus() }
        capsuleButton.setOnClickListener { saveCapsule() }
        attachButton.setOnClickListener { pickFile() }
        voiceButton.setOnClickListener { startVoiceInput() }
    }

    private fun loadModel() {
        val modelDir: File = getExternalFilesDir("models") ?: filesDir
        if (!modelDir.exists()) modelDir.mkdirs()
        val modelFile = File(modelDir, "mistral-7b-instruct-v0.2.Q4_K_M.gguf")

        if (modelFile.exists()) {
            addChatMessage("✅ Модель найдена. Загружаю...")
            updateStatus("Загружаю...")
            tvFileStatus.text = "🟢 Файл: ${modelFile.name}"
            llamaBridge.loadModelFromPath(
                path = modelFile.absolutePath,
                onProgress = { msg: String -> addChatMessage(msg) },
                onDone = { success: Boolean ->
                    if (success) {
                        addChatMessage("🎉 Модель загружена!")
                        updateStatus("Готов")
                        tvFileStatus.text = "🟢 Модель готова к работе"
                    } else {
                        addChatMessage("❌ Ошибка загрузки модели")
                        updateStatus("Ошибка")
                        tvFileStatus.text = "❌ Ошибка загрузки"
                    }
                }
            )
        } else {
            addChatMessage("📥 Скачиваю Mistral 7B (4.1 ГБ). Жди...")
            updateStatus("Качаю...")
            tvFileStatus.text = "📥 Скачивание..."

            val task = DownloadModelTask(
                file = modelFile,
                onProgressUpdate = { percent: Int ->
                    runOnUiThread {
                        addChatMessage("📥 $percent%")
                        updateStatus("Качаю $percent%")
                        tvFileStatus.text = "📥 Скачано: $percent%"
                    }
                },
                onDone = {
                    runOnUiThread {
                        addChatMessage("✅ Скачано. Загружаю...")
                        updateStatus("Загружаю...")
                        tvFileStatus.text = "🟢 Скачано. Загружаю..."
                        llamaBridge.loadModelFromPath(
                            path = modelFile.absolutePath,
                            onProgress = { msg: String -> addChatMessage(msg) },
                            onDone = { success: Boolean ->
                                if (success) {
                                    addChatMessage("🎉 Модель загружена!")
                                    updateStatus("Готов")
                                    tvFileStatus.text = "🟢 Модель готова к работе"
                                } else {
                                    addChatMessage("❌ Ошибка загрузки")
                                    updateStatus("Ошибка")
                                    tvFileStatus.text = "❌ Ошибка загрузки"
                                }
                            }
                        )
                    }
                },
                onError = { error: String ->
                    runOnUiThread {
                        addChatMessage("❌ Ошибка: $error")
                        updateStatus("Ошибка сети")
                        tvFileStatus.text = "❌ $error"
                    }
                }
            )
            task.execute()
        }
    }

    private fun grantPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
                tvFileStatus.text = "📁 Открыты настройки доступа"
            } catch (e: Exception) {
                tvFileStatus.text = "❌ Ошибка открытия настроек"
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
            } else {
                tvFileStatus.text = "✅ Доступ уже разрешён"
            }
        }
    }

    private fun searchModel() {
        val modelDir: File = getExternalFilesDir("models") ?: filesDir
        if (!modelDir.exists()) modelDir.mkdirs()
        val modelFile = File(modelDir, "mistral-7b-instruct-v0.2.Q4_K_M.gguf")

        if (modelFile.exists()) {
            val sizeMB = modelFile.length() / (1024 * 1024)
            tvFileStatus.text = "✅ Найден: ${modelFile.name} ($sizeMB МБ)"
            addChatMessage("✅ Найден .gguf: $sizeMB МБ")
        } else {
            tvFileStatus.text = "❌ Файл не найден. Нажми «ВЫБРАТЬ ПАПКУ» для скачивания"
            addChatMessage("❌ .gguf не найден в песочнице")
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            updateStatus("Готов")
            tvFileStatus.text = "✅ Доступ разрешён"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 || requestCode == 101) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                updateStatus("Готов")
                tvFileStatus.text = "✅ Доступ разрешён"
            } else {
                tvFileStatus.text = "❌ Доступ не разрешён"
            }
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
            addChatMessage("❌ Сначала загрузи модель (кнопка «ВЫБРАТЬ ПАПКУ С МОДЕЛЬЮ»)")
            return
        }

        addChatMessage("👤 $question")
        messageInput.text.clear()
        updateStatus("Думаю...")

        llamaBridge.generate(
            prompt = question,
            onToken = { answer: String -> addChatMessage("🤖 $answer") },
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
        if (llamaBridge.isLoaded) {
            addChatMessage("✅ Модель загружена")
            tvFileStatus.text = "🟢 Модель готова"
        } else {
            addChatMessage("❌ Модель не загружена. Нажми «ВЫБРАТЬ ПАПКУ С МОДЕЛЬЮ»")
            tvFileStatus.text = "❌ Модель не загружена"
        }
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
