package com.mechpravdy.neo

import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    private var libraryLoaded = false

    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    private fun ensureLibraryLoaded() {
        if (!libraryLoaded) {
            System.loadLibrary("llama")
            libraryLoaded = true
        }
    }

    fun loadModelFromPath(path: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgress("Файл: ${File(path).name}")
        val sizeMB = File(path).length() / (1024 * 1024)
        onProgress("Размер: $sizeMB МБ")
        onProgress("Загружаю...")
        try {
            ensureLibraryLoaded()
            val result = llamaLoadModel(path)
            if (result) {
                isLoaded = true
                onProgress("Модель загружена! Готов к бою!")
                onDone(true)
            } else {
                onProgress("Ошибка загрузки модели")
                onDone(false)
            }
        } catch (e: Exception) {
            onProgress("Ошибка: ${e.message}")
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[NEO] Модель не загружена.")
            onDone()
            return
        }
        try {
            val response = llamaComplete(prompt)
            onToken(response)
        } catch (e: Exception) {
            onToken("[NEO] Ошибка: ${e.message}")
        }
        onDone()
    }

    fun unload() {
        try { llamaStop() } catch (e: Exception) { e.printStackTrace() }
        isLoaded = false
    }
}
