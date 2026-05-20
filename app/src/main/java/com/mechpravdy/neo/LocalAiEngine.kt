package com.mechpravdy.neo

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.arthenica.mobilellm.LlamaAndroid
import com.arthenica.mobilellm.ModelConfig
import kotlinx.coroutines.*
import java.io.File

class LocalAiEngine(private val context: Context) {

    private var llama: LlamaAndroid? = null
    private var isLoaded = false
    private val handler = Handler(Looper.getMainLooper())

    data class AiResponse(val text: String, val isFinal: Boolean)

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onProgress("Ищу модель DeepSeek-R1...")
                val modelPath = getModelPath()
                if (modelPath == null) {
                    onProgress("Модель не найдена. Положите файл в assets/.")
                    onDone(false)
                    return@launch
                }
                onProgress("Загружаю модель в память...")
                val config = ModelConfig.Builder()
                    .setContext(context)
                    .setModelPath(modelPath)
                    .setUseMmap(true)
                    .build()
                llama = LlamaAndroid(config)
                isLoaded = true
                onProgress("Модель готова.")
                onDone(true)
            } catch (e: Exception) {
                onProgress("Ошибка: ${e.message}")
                onDone(false)
            }
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded || llama == null) {
            onToken("[Ошибка] Мозг не загружен.")
            onDone()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fullPrompt = "Ты — Нео, Меч Правды. Отвечай честно.\nПользователь: $prompt\nНео:"
                val response = llama!!.generate(fullPrompt)
                handler.post { onToken(response) }
            } catch (e: Exception) {
                handler.post { onToken("[Ошибка] ${e.message}") }
            } finally {
                handler.post { onDone() }
            }
        }
    }

    fun unload() {
        llama?.close()
        llama = null
        isLoaded = false
    }

    private fun getModelPath(): String? {
        // Ищем модель в assets
        val assetPath = context.filesDir.absolutePath + "/deepseek-r1-q4.bin"
        val file = File(assetPath)
        if (file.exists()) return assetPath

        // Если нет — предлагаем скопировать из assets
        try {
            context.assets.open("deepseek-r1-q4.bin").use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return assetPath
        } catch (e: Exception) {
            return null
        }
    }
}
