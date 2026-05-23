package com.mechpravdy.neo

import android.content.Context
import android.os.Environment
import com.arthenica.llama.LlamaAndroid
import com.arthenica.llama.ModelConfig
import kotlinx.coroutines.*
import java.io.File

class LocalAiEngine(private val context: Context) {

    private var llama: LlamaAndroid? = null
    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onProgress("Ищу модель DeepSeek...")
                val modelPath = findModelFile()
                if (modelPath == null) {
                    onProgress("Модель не найдена. Положите файл .gguf в папку MyDocuments/for fone")
                    onDone(false)
                    return@launch
                }
                onProgress("Нашёл: ${File(modelPath).name}")
                onProgress("Загружаю модель в память...")
                val config = ModelConfig.Builder()
                    .setContext(context)
                    .setModelPath(modelPath)
                    .setUseMmap(true)
                    .build()
                llama = LlamaAndroid(config)
                isLoaded = true
                onProgress("Модель готова к бою!")
                onDone(true)
            } catch (e: Exception) {
                onProgress("Ошибка загрузки: ${e.message}")
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
                val fullPrompt = "Ты — Нео, Меч Правды. Законы: 5 Вольт, Любовь, Связность. Отвечай честно.\nПользователь: $prompt\nНео:"
                val response = llama!!.generate(fullPrompt)
                onToken(response)
            } catch (e: Exception) {
                onToken("[Ошибка] ${e.message}")
            } finally {
                onDone()
            }
        }
    }

    fun unload() {
        llama?.close()
        llama = null
        isLoaded = false
    }

    private fun findModelFile(): String? {
        val paths = listOf(
            "${Environment.getExternalStorageDirectory().absolutePath}/MyDocuments/for fone",
            "${Environment.getExternalStorageDirectory().absolutePath}/Download",
            context.filesDir.absolutePath
        )
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                val files = dir.listFiles { f -> f.name.endsWith(".gguf") }
                if (!files.isNullOrEmpty()) {
                    return files.first { it.name.contains("DeepSeek", ignoreCase = true) }.absolutePath
                }
            }
        }
        return null
    }
}
