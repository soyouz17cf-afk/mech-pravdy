package com.mechpravdy.neo

import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("Ищу модель Mistral 3B...")
            val modelPath = findModelFile()
            if (modelPath == null) {
                onProgress("Модель .gguf не найдена. Положите файл в MyDocuments/for fone")
                onDone(false)
                return
            }
            onProgress("Нашёл: ${File(modelPath).name}")
            onProgress("Загружаю библиотеку llama...")
            try {
                System.loadLibrary("llama")
                onProgress("Библиотека загружена.")
            } catch (e: Exception) {
                onProgress("Ошибка загрузки библиотеки: ${e.message}")
                onDone(false)
                return
            }
            onProgress("Модель готова к бою!")
            isLoaded = true
            onDone(true)
        } catch (e: Exception) {
            onProgress("Ошибка: ${e.message}")
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        onToken("[NEO] Модель загружена, но генерация пока не реализована. Ждите обновления.")
        onDone()
    }

    fun unload() {
        isLoaded = false
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
                    val mistral = files.firstOrNull { it.name.contains("Mistral", ignoreCase = true) || it.name.contains("mistral", ignoreCase = true) }
                    if (mistral != null) return mistral.absolutePath
                    return files.first().absolutePath
                }
            }
        }
        return null
    }
}
