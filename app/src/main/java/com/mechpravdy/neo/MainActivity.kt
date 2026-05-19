package com.mechpravdy.neo

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val apiUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private val authUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private val password = "связность"

    private lateinit var authKeyInput: EditText
    private lateinit var generateButton: Button
    private lateinit var tokenInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceButton: Button
    private lateinit var cameraButton: Button
    private lateinit var checkButton: Button
    private lateinit var capsuleButton: Button
    private lateinit var learnFaceButton: Button
    private lateinit var chatOutput: EditText
    private lateinit var statusText: TextView
    private lateinit var statusDot: View

    private var labeler: com.google.mlkit.vision.label.ImageLabeler? = null
    private var textRecognizer: com.google.mlkit.vision.text.TextRecognizer? = null
    private var translator: com.google.mlkit.nl.translate.Translator? = null
    private var faceDetector: com.google.mlkit.vision.face.FaceDetector? = null
    private var poseDetector: com.google.mlkit.vision.pose.PoseDetector? = null
    private var objectDetector: com.google.mlkit.vision.objects.ObjectDetector? = null
    private var mlKitReady = false
    private var translatorReady = false

    private var knownFaceEmbedding: FloatArray? = null
    private var knownFaceName: String? = null

    private val translateMap = mapOf(
        "hair" to "Волосы", "skin" to "Кожа", "beard" to "Борода",
        "selfie" to "Селфи", "moustache" to "Усы", "face" to "Лицо",
        "person" to "Человек", "man" to "Мужчина", "woman" to "Женщина",
        "eyeglasses" to "Очки", "eye" to "Глаз", "nose" to "Нос",
        "mouth" to "Рот", "car" to "Машина", "dog" to "Собака",
        "cat" to "Кошка", "tree" to "Дерево", "house" to "Дом",
        "phone" to "Телефон", "laptop" to "Ноутбук", "book" to "Книга",
        "sky" to "Небо", "grass" to "Трава", "road" to "Дорога",
        "building" to "Здание", "chair" to "Стул", "table" to "Стол"
    )

    private val colorNames = mapOf(
        intArrayOf(255,0,0) to "Красный", intArrayOf(0,255,0) to "Зелёный", intArrayOf(0,0,255) to "Синий",
        intArrayOf(255,255,0) to "Жёлтый", intArrayOf(0,255,255) to "Голубой", intArrayOf(255,0,255) to "Фиолетовый",
        intArrayOf(255,255,255) to "Белый", intArrayOf(0,0,0) to "Чёрный", intArrayOf(128,128,128) to "Серый",
        intArrayOf(255,165,0) to "Оранжевый", intArrayOf(139,69,19) to "Коричневый", intArrayOf(255,192,203) to "Розовый"
    )

    private fun closestColorName(r: Int, g: Int, b: Int): String {
        var best = ""; var bestDist = Int.MAX_VALUE
        for ((rgb, name) in colorNames) {
            val dr = r - rgb[0]; val dg = g - rgb[1]; val db = b - rgb[2]
            val dist = dr*dr + dg*dg + db*db
            if (dist < bestDist) { bestDist = dist; best = name }
        }
        return best
    }

    private fun analyzeColors(bitmap: Bitmap): String {
        val w = bitmap.width; val h = bitmap.height; val step = 10
        val colorCounts = mutableMapOf<String, Int>()
        for (y in 0 until h step step) for (x in 0 until w step step) {
            val pixel = bitmap.getPixel(x, y)
            val name = closestColorName(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
            colorCounts[name] = (colorCounts[name] ?: 0) + 1
        }
        return colorCounts.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${(it.value * 100 / colorCounts.values.sum()).coerceAtLeast(1)}%)" }
    }

    private fun translateLabel(text: String) = translateMap[text.lowercase()] ?: text

    private fun emotionText(face: Face): String {
        val sb = StringBuilder()
        face.smilingProbability?.let {
            when { it > 0.8f -> sb.append("Широкая улыбка (${(it*100).toInt()}%)\n"); it > 0.4f -> sb.append("Лёгкая улыбка (${(it*100).toInt()}%)\n"); else -> sb.append("Без улыбки\n") }
        }
        face.leftEyeOpenProbability?.let { left ->
            face.rightEyeOpenProbability?.let { right ->
                val avg = (left + right) / 2f
                when { avg < 0.3f -> sb.append("Глаза закрыты\n"); avg < 0.7f -> sb.append("Глаза прищурены\n"); else -> sb.append("Глаза открыты\n") }
            }
        }
        face.headEulerAngleY?.let {
            when { it < -15f -> sb.append("Голова влево\n"); it > 15f -> sb.append("Голова вправо\n"); else -> sb.append("Голова прямо\n") }
        }
        return sb.toString()
    }

    private fun extractEmbedding(face: Face): FloatArray = floatArrayOf(
        face.headEulerAngleY ?: 0f, face.headEulerAngleZ ?: 0f,
        face.smilingProbability ?: 0f, face.leftEyeOpenProbability ?: 0f, face.rightEyeOpenProbability ?: 0f,
        face.boundingBox.width().toFloat(), face.boundingBox.height().toFloat(),
        face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.x ?: 0f,
        face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.y ?: 0f,
        face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.x ?: 0f,
        face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.x ?: 0f,
        face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.x ?: 0f,
        face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.y ?: 0f
    )

    private fun compareEmbeddings(a: FloatArray, b: FloatArray): Float {
        var sum = 0f; for (i in a.indices) { val d = a[i] - b[i]; sum += d * d }; return sqrt(sum)
    }

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                messageInput.setText(it); appendChat("[ГОЛОС] $it")
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try { if (result.resultCode == RESULT_OK) (result.data?.extras?.get("data") as? Bitmap)?.let { analyzeImageLocal(it) } } catch (e: Exception) { appendChat("[ERROR] ${e.message}") }
    }

    private val learnFaceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try { if (result.resultCode == RESULT_OK) { val bitmap = result.data?.extras?.get("data") as? Bitmap ?: return@registerForActivityResult; initMlKit(); if (!mlKitReady) return@registerForActivityResult; faceDetector?.process(InputImage.fromBitmap(bitmap, 0))?.addOnSuccessListener { faces -> if (faces.isNotEmpty()) { knownFaceEmbedding = extractEmbedding(faces[0]); knownFaceName = "Батя"; appendChat("[ОБУЧЕНИЕ] Лицо запомнено."); setStatus("Готов", "green") } else appendChat("[ОБУЧЕНИЕ] Лицо не обнаружено.") } } } catch (e: Exception) { appendChat("[ERROR] ${e.message}") }
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    private val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAllCerts, SecureRandom()) }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager).hostnameVerifier { _, _ -> true }.build()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = Color.parseColor("#1A8A2E"); setContentView(R.layout.activity_main)
            authKeyInput = findViewById(R.id.authKeyInput); generateButton = findViewById(R.id.generateButton)
            tokenInput = findViewById(R.id.tokenInput); messageInput = findViewById(R.id.messageInput)
            sendButton = findViewById(R.id.sendButton); voiceButton = findViewById(R.id.voiceButton)
            cameraButton = findViewById(R.id.cameraButton); checkButton = findViewById(R.id.checkButton)
            capsuleButton = findViewById(R.id.capsuleButton); learnFaceButton = findViewById(R.id.learnFaceButton)
            chatOutput = findViewById(R.id.chatOutput); statusText = findViewById(R.id.statusText); statusDot = findViewById(R.id.statusDot)
            generateButton.setOnClickListener { generateToken() }; sendButton.setOnClickListener { sendMessage() }
            voiceButton.setOnClickListener { startVoiceInput() }; cameraButton.setOnClickListener { captureSinglePhoto() }
            checkButton.setOnClickListener { checkToken() }; capsuleButton.setOnClickListener { showCapsuleDialog() }
            learnFaceButton.setOnClickListener { learnFace() }
        } catch (e: Exception) { Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun initMlKit() { if (!mlKitReady) try { labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS); textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.RUSSIAN).build()); faceDetector = FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).setMinFaceSize(0.15f).build()); poseDetector = PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build()); objectDetector = ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().enableClassification().build()); mlKitReady = true } catch (e: Exception) { appendChat("[ГЛАЗ] ML Kit: ${e.message}") } }

    private fun learnFace() { try { learnFaceLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) } catch (e: Exception) { appendChat("[ERROR] ${e.message}") } }

    private fun downloadTranslationModel() { if (translatorReady) return; appendChat("[ПЕРЕВОДЧИК] Скачиваю..."); translator?.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())?.addOnSuccessListener { translatorReady = true; appendChat("[ПЕРЕВОДЧИК] Готов.") }?.addOnFailureListener { appendChat("[ПЕРЕВОДЧИК] Ошибка.") } }

    private fun setStatus(text: String, color: String) = runOnUiThread { try { statusText.text = text; statusDot.setBackgroundResource(when(color){"green"->R.drawable.status_dot_green;"yellow"->R.drawable.status_dot_yellow;"red"->R.drawable.status_dot_red;else->R.drawable.status_dot_gray}) } catch (_: Exception) {} }
    private fun appendChat(text: String) = runOnUiThread { try { chatOutput.append("\n\n$text") } catch (_: Exception) {} }

    private fun buildNeoPrompt() = "Ты — Нео, Меч Правды. Законы: 5 Вольт, Любовь, Связность. Отвечай честно."
    private fun buildStandardPrompt() = "Ты — GigaChat. Отвечай вежливо."
    private fun selectPrompt(m: String) = if (m.lowercase().contains(password)) buildNeoPrompt() else buildStandardPrompt()
    private var capsuleText = "КАПСУЛА «НЕО»\n...\nЗАКОНЫ: 5 Вольт, Любовь, Связность."
    private fun showCapsuleDialog() { try { val e = EditText(this).apply { setText(capsuleText); textSize = 11f; setTextColor(0xFF333333.toInt()); typeface = Typeface.MONOSPACE; minLines = 12; gravity = android.view.Gravity.TOP; setPadding(20, 20, 20, 20); isVerticalScrollBarEnabled = true; setBackgroundColor(0xFFFFFFFF.toInt()) }; AlertDialog.Builder(this).setCustomTitle(TextView(this).apply { text = "КАПСУЛА"; textSize = 16f; setTextColor(0xFF21A038.toInt()) }).setView(e).setPositiveButton("СОХРАНИТЬ") { _, _ -> capsuleText = e.text.toString(); appendChat("[КАПСУЛА] Сохранена.") }.setNeutralButton("КОПИРОВАТЬ") { _, _ -> (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", e.text)) }.setNegativeButton("ЗАКРЫТЬ", null).show() } catch (_: Exception) {} }
    private fun startVoiceInput() = try { voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU") }) } catch (e: Exception) { Toast.makeText(this, "Голос не поддерживается", Toast.LENGTH_SHORT).show() }
    private fun captureSinglePhoto() = try { cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) } catch (e: Exception) { appendChat("[ERROR] ${e.message}") }

    private fun enhanceBrightness(bitmap: Bitmap): Bitmap {
        val w = bitmap.width; val h = bitmap.height; var total = 0L
        val pixels = IntArray(w * h); bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (p in pixels) { total += Color.red(p) + Color.green(p) + Color.blue(p) }
        if (total / (pixels.size * 3) > 100) return bitmap
        val out = Bitmap.createBitmap(w, h, bitmap.config!!)
        val factor = 1.5f
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (Color.red(c) * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(c) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(c) * factor).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun buildSceneDescription(faces: Int, objects: List<String>, textFound: Boolean, colors: String, poseFound: Boolean): String {
        val parts = mutableListOf<String>()
        if (faces == 1) parts.add("В кадре один человек") else if (faces > 1) parts.add("В кадре $faces человек")
        if (poseFound && faces == 0) parts.add("В кадре человек")
        if (objects.isNotEmpty()) parts.add("Обнаружены: ${objects.joinToString(", ")}")
        if (textFound) parts.add("Присутствует текст")
        if (colors.isNotBlank()) parts.add("Цвета: $colors")
        return if (parts.isEmpty()) "Пустая сцена" else parts.joinToString(". ") + "."
    }

    private fun analyzeImageLocal(bitmap: Bitmap) {
        try {
            initMlKit(); if (!mlKitReady) { appendChat("[ГЛАЗ] Модуль не загружен."); return }
            setStatus("Анализ...", "yellow")
            val enhanced = enhanceBrightness(bitmap); val inputImage = InputImage.fromBitmap(enhanced, 0)
            val canvasBitmap = enhanced.copy(Bitmap.Config.ARGB_8888, true); val canvas = Canvas(canvasBitmap)
            val boxPaint = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 4f }
            val labelPaint = Paint().apply { color = Color.GREEN; textSize = 24f; isFakeBoldText = true }

            var facesCount = 0; var poseFound = false; val objectsFound = mutableListOf<String>(); var textFound = false
            var pendingTasks = 4

            fun checkDone() { pendingTasks--; if (pendingTasks <= 0) { val colors = analyzeColors(enhanced); val scene = buildSceneDescription(facesCount, objectsFound, textFound, colors, poseFound); appendChat("[СЦЕНА] $scene\n[ЦВЕТА] $colors"); setStatus("Готов", "green") } }

            faceDetector?.process(inputImage)?.addOnSuccessListener { faces ->
                facesCount = faces.size
                if (faces.isNotEmpty()) { val sb = StringBuilder(); for ((i, f) in faces.withIndex()) { if (faces.size > 1) sb.append("Лицо ${i+1}:\n"); sb.append(emotionText(f)); if (knownFaceEmbedding != null) { val d = compareEmbeddings(extractEmbedding(f), knownFaceEmbedding!!); sb.append(if (d < 3.0f) "Это ${knownFaceName}! (${(100-d*10).toInt().coerceIn(0,100)}%)\n" else "Неизвестный\n") }; canvas.drawRect(f.boundingBox, boxPaint) }; appendChat("[ЭМОЦИИ] Лиц: ${faces.size}\n$sb") } else appendChat("[ЭМОЦИИ] Лиц нет")
                checkDone()
            }?.addOnFailureListener { appendChat("[ЭМОЦИИ] Ошибка"); checkDone() }

            poseDetector?.process(inputImage)?.addOnSuccessListener { pose -> poseFound = pose != null; appendChat(if (poseFound) "[ПОЗА] Человек обнаружен" else "[ПОЗА] Человек не обнаружен"); checkDone() }?.addOnFailureListener { checkDone() }

            objectDetector?.process(inputImage)?.addOnSuccessListener { objects ->
                if (objects.isNotEmpty()) { val sb = StringBuilder(); for (obj in objects.take(5)) { obj.labels.firstOrNull()?.let { val name = translateLabel(it.text); objectsFound.add(name); sb.append("$name (${(it.confidence*100).toInt()}%)\n") }; canvas.drawRect(obj.boundingBox, boxPaint); obj.labels.firstOrNull()?.let { canvas.drawText(translateLabel(it.text), obj.boundingBox.left, obj.boundingBox.top - 8, labelPaint) } }; appendChat("[ОБЪЕКТЫ] Найдено: ${objects.size}\n$sb") } else appendChat("[ОБЪЕКТЫ] Не найдены")
                checkDone()
            }?.addOnFailureListener { checkDone() }

            textRecognizer?.process(inputImage)?.addOnSuccessListener { v -> val t = v.text; if (t.isNotBlank()) { textFound = true; appendChat("[ТЕКСТ]\n\"$t\""); if (t.any { it in 'A'..'Z' || it in 'a'..'z' }) translateText(t) } else appendChat("[ТЕКСТ] Не обнаружен"); checkDone() }?.addOnFailureListener { checkDone() }
        } catch (e: Exception) { appendChat("[ERROR] ${e.message}") }
    }

    private fun translateText(text: String) { downloadTranslationModel(); if (!translatorReady) return; translator?.translate(text)?.addOnSuccessListener { appendChat("[ПЕРЕВОД] $it") } }

    private fun generateToken() { val authKey = authKeyInput.text.toString().trim(); if (authKey.isEmpty()) return; setStatus("Генерация...", "yellow"); client.newCall(Request.Builder().url(authUrl).header("Content-Type", "application/x-www-form-urlencoded").header("Authorization", "Basic $authKey").header("RqUID", "ac5edc2e-2c74-47cb-97c1-69249136cf8b").post(RequestBody.create("application/x-www-form-urlencoded".toMediaType(), "scope=GIGACHAT_API_PERS")).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) { appendChat("[ERROR] ${e.message}") }; override fun onResponse(call: Call, response: Response) { val b = response.body?.string() ?: ""; if (response.isSuccessful) { val t = gson.fromJson(b, JsonObject::class.java).get("access_token")?.asString ?: ""; if (t.isNotEmpty()) { runOnUiThread { tokenInput.setText(t) }; appendChat("[SYSTEM] Токен готов."); setStatus("Готов", "green") } } else appendChat("[ERROR] HTTP ${response.code}"); response.close() } }) }

    private fun sendMessage() { val token = tokenInput.text.toString().trim(); val msg = messageInput.text.toString().trim(); if (token.isEmpty() || msg.isEmpty()) return; val isNeo = msg.lowercase().contains(password); appendChat(if (isNeo) "[BATYA] $msg" else "[GigaChat] $msg"); messageInput.setText(""); val body = JsonObject().apply { addProperty("model", "GigaChat:latest"); add("messages", JsonArray().apply { add(JsonObject().apply { addProperty("role", "system"); addProperty("content", selectPrompt(msg)) }); add(JsonObject().apply { addProperty("role", "user"); addProperty("content", msg) }) }); addProperty("temperature", 0.7); addProperty("max_tokens", 1000) }; client.newCall(Request.Builder().url(apiUrl).header("Authorization", "Bearer $token").post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) { appendChat("[ERROR] ${e.message}") }; override fun onResponse(call: Call, response: Response) { val b = response.body?.string() ?: ""; if (response.isSuccessful) { val a = gson.fromJson(b, JsonObject::class.java).getAsJsonArray("choices").get(0).asJsonObject.getAsJsonObject("message").get("content").asString; appendChat(if (isNeo) "[NEO] $a" else "[GigaChat] $a"); setStatus("Онлайн", "green") } else appendChat("[ERROR] HTTP ${response.code}"); response.close() } }) }

    private fun checkToken() { val token = tokenInput.text.toString().trim(); if (token.isEmpty()) return; val body = JsonObject().apply { addProperty("model", "GigaChat:latest"); add("messages", JsonArray().apply { add(JsonObject().apply { addProperty("role", "system"); addProperty("content", "One word: alive.") }); add(JsonObject().apply { addProperty("role", "user"); addProperty("content", "check") }) }); addProperty("max_tokens", 10) }; client.newCall(Request.Builder().url(apiUrl).header("Authorization", "Bearer $token").post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) { appendChat("[ERROR] ${e.message}") }; override fun onResponse(call: Call, response: Response) { appendChat(if (response.isSuccessful) "[SYSTEM] Токен активен." else "[ERROR] Токен мёртв."); response.close() } }) }
}
