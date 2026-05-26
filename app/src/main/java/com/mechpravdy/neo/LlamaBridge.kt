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
     * Старый метод — ищет .gguf в папках (заглушка)
     */
    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("Ищу модель...")
            val foundPath = findModelFile()
            if (foundPath == null) {
                onProgress("Модель не найдена. Положите .gguf в MyDocuments/for fone")
                onDone(false)
                return
            }
            onProgress("Нашёл: ${File(foundPath).name}")
            loadModelFromPath(foundPath, onProgress, onDone)
        } catch (e: Exception) {
            onProgress("Ошибка: ${e.message}")
            onDone(false)
        }
    }

    /**
     * Новый метод — прямая загрузка из песочницы
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

    private fun getSearchPaths(): List<String> {
        return listOf(
            "/storage/emulated/0/MyDocuments/for fone",
            "/storage/emulated/0/MyDocuments/for phone",
            "/storage/emulated/0/MyDocuments",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents"
        )
    }

    private fun findModelFile(): String? {
        val paths = getSearchPaths()
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                val files = dir.listFiles { f -> f.name.endsWith(".gguf") }
                if (!files.isNullOrEmpty()) {
                    val found = files.firstOrNull {
                        it.name.contains("mistral", ignoreCase = true) ||
                        it.name.contains("llava", ignoreCase = true)
                    }
                    if (found != null) return found.absolutePath
                }
            }
        }
        return null
    }
}
