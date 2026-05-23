package com.mechpravdy.neo

import android.os.Environment
import kotlinx.coroutines.*
import java.io.File

class LlamaBridge {

    private var nativeLoaded = false
    var isLoaded = false
        private set

    // JNI-методы
    private external fun loadModelNative(modelPath: String): Boolean
    private external fun generateNative(prompt: String, maxTokens: Int): String
    private external fun unloadModelNative()

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Загружаем .so библиотеку
                if (!nativeLoaded) {
                    try {
                        System.loadLibrary("llama")
                        nativeLoaded = true
                    } catch (e: Exception) {
                        // Если .so нет — пробуем через AbodeLLM API
                        onProgress("Библиотека llama не найдена. Использую AbodeLLM.")
                        isLoaded = false
                        onDone(false)
                        return@launch
                    }
                }

                onProgress("Ищу модель Mistral 3B...")
                val modelPath = findModelFile()
                if (modelPath == null) {
                    onProgress("Модель .gguf не найдена. Положите файл в MyDocuments/for fone")
                    isLoaded = false
                    onDone(false)
                    return@launch
                }
                onProgress("Нашёл: ${File(modelPath).name}")
                onProgress("Загружаю модель в память...")
                val success = loadModelNative(modelPath)
                if (success) {
                    isLoaded = true
                    onProgress("Модель готова!")
                    onDone(true)
                } else {
                    onProgress("Ошибка загрузки модели.")
                    isLoaded = false
                    onDone(false)
                }
            } catch (e: Exception) {
                onProgress("Ошибка: ${e.message}")
                isLoaded = false
                onDone(false)
            }
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[Ошибка] Модель не загружена.")
            onDone()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fullPrompt = "Ты — Нео, Меч Правды. Законы: 5 Вольт, Любовь, Связность.\nПользователь: $prompt\nНео:"
                val response = generateNative(fullPrompt, 500)
                onToken(response)
            } catch (e: Exception) {
                onToken("[Ошибка] ${e.message}")
            } finally {
                onDone()
            }
        }
    }

    fun unload() {
        if (isLoaded) {
            unloadModelNative()
            isLoaded = false
        }
    }

    private fun findModelFile(): String? {
        val paths = listOf(
            "${Environment.getExternalStorageDirectory().absolutePath}/MyDocuments/for fone",
            "${Environment.getExternalStorageDirectory().absolutePath}/Download"
        )
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                val files = dir.listFiles { f -> f.name.endsWith(".gguf") }
                if (!files.isNullOrEmpty()) {
                    // Ищем Mistral
                    val mistral = files.firstOrNull { it.name.contains("Mistral", ignoreCase = true) || it.name.contains("mistral", ignoreCase = true) }
                    if (mistral != null) return mistral.absolutePath
                    // Если нет — берём первый попавшийся
                    return files.first().absolutePath
                }
            }
        }
        return null
    }
}
