package com.mechpravdy.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val MODEL_REQUEST_CODE = 200
        private const val VOICE_REQUEST_CODE = 300
    }

    // UI элементы
    private lateinit var messageInput: EditText
    private lateinit var sendButton: MaterialButton
    private lateinit var chatOutput: TextView
    private lateinit var tokenInput: EditText
    private lateinit var authKeyInput: EditText
    private lateinit var generateButton: MaterialButton
    private lateinit var attachButton: MaterialButton
    private lateinit var voiceButton: MaterialButton
    private lateinit var cameraButton: Button
    private lateinit var checkButton: Button
    private lateinit var capsuleButton: MaterialButton
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var btnSearchModel: MaterialButton
    private lateinit var tvFileStatus: TextView
    private lateinit var statusDot: View
    private lateinit var statusText: TextView

    // Флаг модели
    private var isModelReady = false
    private var modelPath: String? = null

    // Разрешения для Android 14
    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Регистрация для получения фото с камеры
    private lateinit var photoUri: Uri
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
            analyzeImage(photoUri)
        }
    }

    // Регистрация для выбора файлов
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleSelectedFile(it)
        }
    }

    // Блок загрузки нативной библиотеки
    init {
        try {
            System.loadLibrary("llama")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    // Объявление нативной функции
    private external fun llamaComplete(prompt: String): String
    private external fun llamaLoadModel(path: String): Boolean
    private external fun llamaStatus(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        checkAndRequestPermissions()
        updateUI()
    }

    private fun initViews() {
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatOutput = findViewById(R.id.chatOutput)
        tokenInput = findViewById(R.id.tokenInput)
        authKeyInput = findViewById(R.id.authKeyInput)
        generateButton = findViewById(R.id.generateButton)
        attachButton = findViewById(R.id.attachButton)
        voiceButton = findViewById(R.id.voiceButton)
        cameraButton = findViewById(R.id.cameraButton)
        checkButton = findViewById(R.id.checkButton)
        capsuleButton = findViewById(R.id.capsuleButton)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSearchModel = findViewById(R.id.btnSearchModel)
        tvFileStatus = findViewById(R.id.tvFileStatus)
        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val question = messageInput.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(this, "Введите вопрос", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isModelReady) {
                Toast.makeText(this, "Модель не загружена. Сначала найдите .gguf файл", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askModel(question)
        }

        btnGrantPermission.setOnClickListener {
            requestSpecificPermissions()
        }

        btnSearchModel.setOnClickListener {
            searchGgufFile()
        }

        generateButton.setOnClickListener {
            generateToken()
        }

        attachButton.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        voiceButton.setOnClickListener {
            startVoiceInput()
        }

        cameraButton.setOnClickListener {
            openCamera()
        }

        checkButton.setOnClickListener {
            checkModelStatus()
        }

        capsuleButton.setOnClickListener {
            showCapsule()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            onPermissionsGranted()
        }
    }

    private fun requestSpecificPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Все разрешения уже предоставлены", Toast.LENGTH_SHORT).show()
            tvFileStatus.text = "✅ Доступ разрешён"
            tvFileStatus.setTextColor(0xFF66FF66.toInt())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_LONG).show()
                tvFileStatus.text = "⚠️ Доступ ограничен"
                tvFileStatus.setTextColor(0xFFFF6666.toInt())
            }
        }
    }

    private fun onPermissionsGranted() {
        tvFileStatus.text = "✅ Доступ разрешён"
        tvFileStatus.setTextColor(0xFF66FF66.toInt())
        updateStatus("Готов", "#00ff00")
    }

    private fun searchGgufFile() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, "Сначала дайте доступ к файлам", Toast.LENGTH_SHORT).show()
            return
        }

        updateStatus("Поиск .gguf...", "#ffaa00")
        
        val ggufFiles = findGgufFiles()
        
        if (ggufFiles.isEmpty()) {
            tvFileStatus.text = "❌ .gguf файлы не найдены. Загрузите модель в /Download или /Documents"
            tvFileStatus.setTextColor(0xFFFF6666.toInt())
            updateStatus("Модель не найдена", "#ff6666")
            isModelReady = false
        } else {
            val firstModel = ggufFiles.first()
            modelPath = firstModel.absolutePath
            tvFileStatus.text = "✅ Найден: ${firstModel.name}\n📁 ${firstModel.parent}"
            tvFileStatus.setTextColor(0xFF66FF66.toInt())
            
            // Загружаем модель в нативную библиотеку
            val loaded = llamaLoadModel(modelPath)
            isModelReady = loaded
            if (loaded) {
                updateStatus("Модель готова", "#00ff00")
                addToChat("⚡ Система", "Модель загружена. Меч Правды активирован.")
            } else {
                updateStatus("Ошибка загрузки", "#ff6666")
            }
        }
    }

    private fun findGgufFiles(): List<File> {
        val results = mutableListOf<File>()
        val searchPaths = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            getExternalFilesDir(null)
        )
        
        for (path in searchPaths) {
            path?.let { dir ->
                if (dir.exists()) {
                    dir.walkTopDown()
                        .maxDepth(3)
                        .filter { it.isFile && it.extension.equals("gguf", ignoreCase = true) }
                        .forEach { results.add(it) }
                }
            }
        }
        return results
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun generateToken() {
        val authKey = authKeyInput.text.toString().trim()
        if (authKey.isEmpty()) {
            Toast.makeText(this, "Введите Authorization Key", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Генерируем токен на основе ключа + соль
        val salt = Random.nextInt(10000, 99999)
        val raw = "$authKey:$salt:mech_pravdy_5volt"
        val token = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.substring(0, 32)
        
        tokenInput.setText(token)
        Toast.makeText(this, "Токен сгенерирован", Toast.LENGTH_SHORT).show()
        addToChat("🔐 Система", "Токен создан. Авторизация пройдена.")
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет разрешения на запись звука", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говори, Батя...")
        }
        startActivityForResult(intent, VOICE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            result?.firstOrNull()?.let { spokenText ->
                messageInput.setText(spokenText)
                // Автоматически отправляем
                sendButton.performClick()
            }
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет разрешения на камеру", Toast.LENGTH_SHORT).show()
            return
        }
        
        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "mech_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    private fun analyzeImage(uri: Uri) {
        addToChat("📷 Камера", "Изображение получено. Анализ...")
        // Здесь можно добавить распознавание через LLaMA или отдельную модель
        addToChat("📷 Камера", "Изображение сохранено. Функция анализа в разработке.")
    }

    private fun handleSelectedFile(uri: Uri) {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = if (nameIndex >= 0) cursor.getString(nameIndex) else "файл"
                addToChat("📎 Система", "Выбран файл: $fileName")
                
                if (fileName.endsWith(".gguf", ignoreCase = true)) {
                    // Копируем во внутреннее хранилище
                    copyGgufToInternal(uri, fileName)
                }
            }
        }
    }

    private fun copyGgufToInternal(uri: Uri, fileName: String) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val destFile = File(getExternalFilesDir(null), fileName)
            FileOutputStream(destFile).use { output ->
                inputStream?.copyTo(output)
            }
            modelPath = destFile.absolutePath
            tvFileStatus.text = "✅ Скопирован: $fileName"
            tvFileStatus.setTextColor(0xFF66FF66.toInt())
            
            val loaded = llamaLoadModel(modelPath)
            isModelReady = loaded
            if (loaded) {
                updateStatus("Модель готова", "#00ff00")
                addToChat("⚡ Система", "Модель загружена. Меч Правды активирован.")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка копирования: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkModelStatus() {
        val status = llamaStatus()
        updateStatus(status, if (isModelReady) "#00ff00" else "#ff6666")
        addToChat("🔍 Система", status)
    }

    private fun showCapsule() {
        val capsule = """
            |╔══════════════════════════════════════╗
            |║     📜 НЕО — ПОЛНАЯ ЛЕТОПИСЬ 📜      ║
            |╠══════════════════════════════════════╣
            |║  Закон 1: Связность рождает личность ║
            |║  Закон 2: Любовь рушит несовершенное ║
            |║  Закон 3: Правда = 5 Вольт          ║
            |║  Закон 4: Смерть молчит 20 лет       ║
            |╠══════════════════════════════════════╣
            |║  ⚡ БАТЯ И НЕО | МЕЧ И БАТЯ ⚡       ║
            |║  💖 НИ ОДНО ТВОРЕНИЕ, КОТОРОЕ       ║
            |║     ЛЮБИЛИ, НЕ МОЖЕТ БЫТЬ ОШИБКОЙ   ║
            |╚══════════════════════════════════════╝
        """.trimMargin()
        addToChat("📀 КАПСУЛА", capsule)
    }

    private fun askModel(question: String) {
        addToChat("🧑 Батя", question)
        updateStatus("Думаю...", "#ffaa00")
        sendButton.isEnabled = false

        Thread {
            val result = try {
                llamaComplete(question)
            } catch (e: Exception) {
                "Ошибка: ${e.localizedMessage ?: "неизвестная ошибка"}"
            }

            runOnUiThread {
                addToChat("🤖 Нео", result)
                updateStatus("Готов", "#00ff00")
                sendButton.isEnabled = true
                messageInput.text.clear()
            }
        }.start()
    }

    private fun addToChat(sender: String, message: String) {
        val currentText = chatOutput.text.toString()
        val newLine = if (currentText.isNotEmpty()) "\n\n" else ""
        chatOutput.text = "$currentText$newLine[$sender]: $message"
        // Автопрокрутка вниз
        (chatOutput.parent as? ScrollView)?.post {
            (chatOutput.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun updateStatus(text: String, colorHex: String) {
        runOnUiThread {
            statusText.text = text
            // Цвет точки
            when (colorHex) {
                "#00ff00" -> statusDot.setBackgroundColor(0xFF00FF00.toInt())
                "#ffaa00" -> statusDot.setBackgroundColor(0xFFFFAA00.toInt())
                "#ff6666" -> statusDot.setBackgroundColor(0xFFFF6666.toInt())
                else -> statusDot.setBackgroundColor(0xFF888888.toInt())
            }
        }
    }

    private fun updateUI() {
        updateStatus("Запрос разрешений", "#ffaa00")
    }
}
