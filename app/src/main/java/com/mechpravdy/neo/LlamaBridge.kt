package com.mechpravdy.neo

import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    private var modelPath: String? = null

    init {
        try {
            System.loadLibrary("llama")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    /**
     * Прямая загрузка модели из песочницы (без поиска по папкам)
     */
    fun loadModelFromPath(path: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        modelPath = path
        onProgress("🔍 Файл: ${File(path).name}")

        if (!File(path).exists()) {
            onProgress("❌ Файл не найден: $path")
            onDone(false)
            return
        }

        val sizeMB = File(path).length() / (1024 * 1024)
        onProgress("📦 Размер: $sizeMB МБ")
        onProgress("⏳ Загружаю в память...")

        try {
            val result = llamaLoadModel(path)
            if (result) {
                isLoaded = true
                onProgress("🟢 Модель загружена! Готов к бою!")
                onDone(true)
            } else {
                onProgress("❌ Ошибка загрузки модели")
                onDone(false)
            }
        } catch (e: Exception) {
            onProgress("❌ Ошибка: ${e.message}")
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[NEO] Модель не загружена. Скачайте мозг через МИСТРАЛЬ 3B.")
            onDone()
            return
        }

        try {
            val response = llamaComplete(prompt)
            onToken(response)
        } catch (e: Exception) {
            onToken("[NEO] Ошибка генерации: ${e.message}")
        }

        onDone()
    }

    fun unload() {
        try {
            llamaStop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoaded = false
    }
}
