package com.mechpravdy.neo

import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("Ищу модель .gguf в памяти телефона...")
            val modelPath = findModelFile(onProgress)
            if (modelPath == null) {
                onProgress("Модель не найдена.")
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

    private fun findModelFile(onProgress: (String) -> Unit): String? {
        val root = File("/storage/emulated/0")
        return searchInDir(root, onProgress)
    }

    private fun searchInDir(dir: File, onProgress: (String) -> Unit): String? {
        if (!dir.exists()) return null
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.name.endsWith(".gguf")) {
                return file.absolutePath
            }
        }
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".") && !file.name.equals("Android", ignoreCase = true)) {
                val result = searchInDir(file, onProgress)
                if (result != null) return result
            }
        }
        return null
    }
}
