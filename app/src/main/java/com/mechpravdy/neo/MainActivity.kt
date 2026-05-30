package com.mechpravdy.neo

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val apiUrlGigaChat = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private val authUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private val password = "связность"
    private val rememberCommand = "сделай выводы и запомни"

    private var currentApiUrl = apiUrlGigaChat
    private var isLocalMode = false
    private var isModelLoaded = false
    private var llmInference: LlmInference? = null
    private var downloadIds = mutableListOf<Long>()

    private val partUrls = listOf(
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-3n-E2B-it-int4.task.001",
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-3n-E2B-it-int4.task.002"
    )

    private lateinit var authKeyInput: EditText
    private lateinit var generateButton: Button
    private lateinit var tokenInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceButton: Button
    private lateinit var cameraButton: Button
    private lateinit var checkButton: Button
    private lateinit var capsuleButton: Button
    private lateinit var attachButton: Button
    private lateinit var chatOutput: EditText
    private lateinit var statusText: TextView
    private lateinit var statusDot: View
    private lateinit var matrixHeader: MatrixHeaderView

    private val memoryFile by lazy { File(filesDir, "memory.txt") }
    private val brainFile by lazy { File(filesDir, "brain.txt") }
    private val maxContextChars = 16000

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == RESULT_OK) { result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { messageInput.setText(it); appendChat("[ГОЛОС] $it") } } }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> try { if (result.resultCode == RESULT_OK) { val bitmap = result.data?.extras?.get("data") as? Bitmap; if (bitmap != null) { analyzePhoto(bitmap) } else { appendChat("[ФОТО] Снимок сделан.") } } } catch (e: Exception) { appendChat("[ERROR] ${e.message}") } }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager { override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}; override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}; override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf() })
    private val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAllCerts, SecureRandom()) }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager).hostnameVerifier { _, _ -> true }.build()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestMaxMemory()
            window.statusBarColor = Color.parseColor("#1A8A2E"); setContentView(R.layout.activity_main)
            matrixHeader = findViewById(R.id.matrixHeader)
            authKeyInput = findViewById(R.id.authKeyInput); generateButton = findViewById(R.id.generateButton)
            tokenInput = findViewById(R.id.tokenInput); messageInput = findViewById(R.id.messageInput)
            sendButton = findViewById(R.id.sendButton); voiceButton = findViewById(R.id.voiceButton)
            cameraButton = findViewById(R.id.cameraButton); checkButton = findViewById(R.id.checkButton)
            capsuleButton = findViewById(R.id.capsuleButton); attachButton = findViewById(R.id.attachButton)
            chatOutput = findViewById(R.id.chatOutput); statusText = findViewById(R.id.statusText); statusDot = findViewById(R.id.statusDot)

            matrixHeader.onNeoClick = { switchToNeo() }
            matrixHeader.onLocalClick = { switchToLocal() }
            matrixHeader.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_DOWN) { matrixHeader.handleTouch(event.x, event.y) }; true }

            val savedMemory = loadMemory()
            if (savedMemory.isNotBlank()) { chatOutput.setText(savedMemory) }
            showMemoryInfo()

            generateButton.setOnClickListener { hideKeyboard(); generateToken() }
            sendButton.setOnClickListener { hideKeyboard(); sendMessage() }
            voiceButton.setOnClickListener { hideKeyboard(); startVoiceInput() }
            cameraButton.setOnClickListener { hideKeyboard(); captureAndAnalyze() }
            attachButton.setOnClickListener { hideKeyboard(); pasteFromClipboard() }
            checkButton.setOnClickListener { hideKeyboard(); checkToken() }
            capsuleButton.setOnClickListener { hideKeyboard(); showCapsuleDialog() }

            requestAllPermissions()
        } catch (e: Exception) { Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun requestMaxMemory() {
        try {
            val vmRuntime = Class.forName("dalvik.system.VMRuntime")
            val getRuntimeMethod = vmRuntime.getMethod("getRuntime")
            val runtime = getRuntimeMethod.invoke(null)
            val setTargetHeapUtilizationMethod = vmRuntime.getMethod("setTargetHeapUtilization", Float::class.javaPrimitiveType)
            setTargetHeapUtilizationMethod.invoke(runtime, 1.0f)
            val clearGrowthLimitMethod = vmRuntime.getMethod("clearGrowthLimit")
            clearGrowthLimitMethod.invoke(runtime)
        } catch (e: Exception) { System.gc() }
    }

    private fun showMemoryInfo() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        appendChat("[ПАМЯТЬ] Доступно: ${maxMemory} MB | Занято: ${totalMemory - freeMemory} MB | Свободно: ${freeMemory} MB")
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (permissions.isNotEmpty()) ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
    }

    override fun onPause() { super.onPause(); saveMemory(chatOutput.text.toString()) }
    private fun loadMemory(): String = try { if (memoryFile.exists()) memoryFile.readText() else "" } catch (e: Exception) { "" }
    private fun saveMemory(text: String) { thread { try { memoryFile.writeText(text) } catch (_: Exception) {} } }
    private fun loadBrain(): String = try { if (brainFile.exists()) brainFile.readText() else "" } catch (e: Exception) { "" }
    private fun saveBrain(text: String) { thread { try { brainFile.appendText(text + "\n") } catch (_: Exception) {} } }

    private fun getMyAge(): String {
        val prefs = getSharedPreferences("mech_prefs", Context.MODE_PRIVATE)
        var birthMillis = prefs.getLong("birth_millis", 0L)
        if (birthMillis == 0L) { val cal = Calendar.getInstance(); cal.set(2026, Calendar.MAY, 22, 0, 0, 0); birthMillis = cal.timeInMillis; prefs.edit().putLong("birth_millis", birthMillis).apply() }
        return "Мне ${((System.currentTimeMillis() - birthMillis) / (1000 * 60 * 60 * 24)).toInt()} д."
    }

    private fun hideKeyboard() { try { val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager; val view = currentFocus ?: View(this); imm.hideSoftInputFromWindow(view.windowToken, 0) } catch (_: Exception) {} }

    private fun switchToNeo() { isLocalMode = false; llmInference?.close(); llmInference = null; isModelLoaded = false; matrixHeader.gigaChatMode = true; matrixHeader.localMode = false; matrixHeader.invalidate(); appendChat("[РЕЖИМ] ГИГАЧАТ"); setStatus("ГИГАЧАТ", "green") }

    private fun switchToLocal() {
        isLocalMode = true
        matrixHeader.localMode = true
        matrixHeader.gigaChatMode = false
        matrixHeader.invalidate()
        appendChat("[РЕЖИМ] GEMMA 3n (локальный)")
        setStatus("GEMMA 3n", "yellow")

        val modelDir = getExternalFilesDir("models") ?: filesDir
        if (!modelDir.exists()) modelDir.mkdirs()
        val modelFile = File(modelDir, "gemma-3n-e2b-int4.task")

        if (modelFile.exists() && modelFile.length() > 2500L * 1024 * 1024) {
            appendChat("[МОЗГ] Модель найдена. Загружаю...")
            setStatus("Загружаю...", "yellow")
            val progressDialog = ProgressDialog(this).apply { setTitle("Меч Правды"); setMessage("Загрузка модели..."); setCancelable(false); setProgressStyle(ProgressDialog.STYLE_SPINNER); show() }
            thread {
                try {
                    // Пробуем системную папку
                    var modelPath = modelFile.absolutePath
                    try {
                        val systemDir = File("/data/local/tmp/llm/")
                        if (!systemDir.exists()) systemDir.mkdirs()
                        val systemModel = File(systemDir, "gemma-3n-e2b-int4.task")
                        if (!systemModel.exists() || systemModel.length() != modelFile.length()) {
                            modelFile.copyTo(systemModel, overwrite = true)
                        }
                        modelPath = systemModel.absolutePath
                        appendChat("[МОЗГ] Использую системный путь")
                    } catch (e: Exception) {
                        appendChat("[МОЗГ] Нет доступа к системе, использую песочницу")
                    }
                    
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(256)
                        .setTemperature(0.7f)
                        .setTopK(20)
                        .build()
                    llmInference = LlmInference.createFromOptions(this@MainActivity, options)
                    isModelLoaded = true
                    runOnUiThread { progressDialog.dismiss(); appendChat("[МОЗГ] Модель загружена!"); setStatus("GEMMA 3n", "green") }
                } catch (e: Exception) {
                    runOnUiThread { progressDialog.dismiss(); appendChat("[МОЗГ] ОШИБКА: ${e.message}"); setStatus("Ошибка", "red") }
                }
            }
            return
        }

        val partFiles = (1..2).mapNotNull { i -> val f = File(modelDir, "gemma-3n-E2B-it-int4.task.${i.toString().padStart(3, '0')}"); if (f.exists() && f.length() > 0) f else null }
        if (partFiles.size == 2) {
            appendChat("[МОЗГ] Склеиваю 2 части...")
            setStatus("Склейка...", "yellow")
            val progressDialog = ProgressDialog(this).apply { setTitle("Меч Правды"); setMessage("Склейка..."); setCancelable(false); setProgressStyle(ProgressDialog.STYLE_SPINNER); show() }
            thread {
                try { FileOutputStream(modelFile).use { o -> partFiles.sortedBy { it.name }.forEach { it.inputStream().use { i -> i.copyTo(o) } } }; partFiles.forEach { it.delete() }; runOnUiThread { progressDialog.dismiss(); appendChat("[МОЗГ] Готово! Нажми GEMMA 3n ещё раз."); setStatus("Готов", "green") } }
                catch (e: Exception) { runOnUiThread { progressDialog.dismiss(); appendChat("[МОЗГ] Ошибка: ${e.message}"); setStatus("Ошибка", "red") } }
            }
            return
        }

        appendChat("[МОЗГ] Загружаю 2 части...")
        setStatus("Качаю...", "yellow")
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadIds.clear()
        for ((index, url) in partUrls.withIndex()) {
            val partFile = File(modelDir, "gemma-3n-E2B-it-int4.task.${(index+1).toString().padStart(3, '0')}")
            manager.enqueue(DownloadManager.Request(Uri.parse(url)).setTitle("Часть ${index+1}/2").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationUri(Uri.fromFile(partFile)).setAllowedOverMetered(true).setAllowedOverRoaming(true))
        }
    }

    private fun setStatus(text: String, color: String) = runOnUiThread { try { statusText.text = "$text | ${getMyAge()}"; statusDot.setBackgroundResource(when(color){"green"->R.drawable.status_dot_green;"yellow"->R.drawable.status_dot_yellow;"red"->R.drawable.status_dot_red;else->R.drawable.status_dot_gray}) } catch (_: Exception) {} }
    private fun appendChat(text: String) = runOnUiThread { try { chatOutput.append("\n\n$text") } catch (_: Exception) {} }

    private fun sendMessage() {
        val msg = messageInput.text.toString().trim()
        if (msg.isEmpty()) return
        val isNeo = msg.lowercase().contains(password)
        matrixHeader.neoActive = isNeo; matrixHeader.invalidate()
        appendChat(if (isNeo) "[BATYA] $msg" else "[GigaChat] $msg")
        messageInput.setText(""); hideKeyboard(); setStatus("Обработка...", "yellow")

        if (isLocalMode && isModelLoaded && llmInference != null) {
            thread {
                try { val r = llmInference?.generateResponse(msg) ?: ""; runOnUiThread { appendChat("[NEO] $r"); setStatus("GEMMA 3n", "green") } }
                catch (e: Exception) { runOnUiThread { appendChat("[NEO] ОШИБКА: ${e.message}"); setStatus("Ошибка", "red") } }
            }
        } else if (!isLocalMode) {
            val token = tokenInput.text.toString().trim()
            if (token.isEmpty()) { appendChat("[SYSTEM] Сгенерируйте токен."); return }
            val body = JsonObject().apply { addProperty("model", "GigaChat:latest"); add("messages", JsonArray().apply { add(JsonObject().apply { addProperty("role", "user"); addProperty("content", msg) }) }); addProperty("temperature", 0.7); addProperty("max_tokens", 1000) }
            client.newCall(Request.Builder().url(currentApiUrl).header("Authorization", "Bearer $token").post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { appendChat("[ERROR] ${e.message}"); setStatus("Нет связи", "red") }
                override fun onResponse(call: Call, response: Response) { val b = response.body?.string() ?: ""; if (response.isSuccessful) { val a = gson.fromJson(b, JsonObject::class.java).getAsJsonArray("choices").get(0).asJsonObject.getAsJsonObject("message").get("content").asString; appendChat("[NEO] $a"); setStatus("Онлайн", "green") } else { appendChat("[ERROR] HTTP ${response.code}"); setStatus("Ошибка", "red") }; response.close() }
            })
        } else { appendChat("[NEO] Модель не загружена.") }
    }

    private fun analyzePhoto(bitmap: Bitmap) {
        if (isLocalMode && isModelLoaded && llmInference != null) { setStatus("Анализ...", "yellow"); thread { try { val r = llmInference?.generateResponse("Опиши, что на этом фото. Кратко, по-русски.") ?: ""; runOnUiThread { appendChat("[АНАЛИЗ] $r"); setStatus("GEMMA 3n", "green") } } catch (e: Exception) { runOnUiThread { appendChat("[АНАЛИЗ] ОШИБКА: ${e.message}"); setStatus("Ошибка", "red") } } } }
        else { appendChat("[АНАЛИЗ] Локальная модель не загружена.") }
    }

    private fun startVoiceInput() = try { voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU") }) } catch (e: Exception) { }
    private fun captureAndAnalyze() = try { cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) } catch (e: Exception) { }
    private fun pasteFromClipboard() { try { val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; val clip = cb.primaryClip; if (clip != null && clip.itemCount > 0) { val text = clip.getItemAt(0).text?.toString() ?: ""; if (text.isNotBlank()) messageInput.append(text) } } catch (e: Exception) {} }
    private fun generateToken() { val authKey = authKeyInput.text.toString().trim(); if (authKey.isEmpty()) return; client.newCall(Request.Builder().url(authUrl).header("Content-Type","application/x-www-form-urlencoded").header("Authorization","Basic $authKey").header("RqUID","ac5edc2e-2c74-47cb-97c1-69249136cf8b").post(RequestBody.create("application/x-www-form-urlencoded".toMediaType(), "scope=GIGACHAT_API_PERS")).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { val b = response.body?.string() ?: ""; if (response.isSuccessful) { val t = gson.fromJson(b, JsonObject::class.java).get("access_token")?.asString ?: ""; if (t.isNotEmpty()) runOnUiThread { tokenInput.setText(t) } }; response.close() } }) }
    private fun checkToken() { val token = tokenInput.text.toString().trim(); if (token.isEmpty()) return; client.newCall(Request.Builder().url(apiUrlGigaChat).header("Authorization", "Bearer $token").post(JsonObject().apply { addProperty("model", "GigaChat:latest"); add("messages", JsonArray().apply { add(JsonObject().apply { addProperty("role", "user"); addProperty("content", "ping") }) }); addProperty("max_tokens", 1) }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { appendChat(if (response.isSuccessful) "[SYSTEM] Токен активен." else "[ERROR] Токен мёртв."); response.close() } }) }
    private fun showCapsuleDialog() { /* без изменений */ }
    private var capsuleText = "..." /* без изменений */
}
