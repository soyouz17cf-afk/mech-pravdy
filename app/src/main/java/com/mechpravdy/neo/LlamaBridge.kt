package com.mechpravdy.neo

import android.content.Context
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    private var libraryLoaded = false

    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun ensureLibraryLoaded(context: Context) {
        if (!libraryLoaded) {
            val libPath = File(context.filesDir, "libllama.so")
            if (libPath.exists()) {
                System.load(libPath.absolutePath)
                libraryLoaded = true
            } else {
                throw RuntimeException("Библиотека libllama.so не найдена в песочнице")
            }
        }
    }

    fun loadModelFromPath(path: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgress("Файл: ${File(path).name}")
        val sizeMB = File(path).length() / (1024 * 1024)
        onProgress("Размер: $sizeMB МБ")
        onProgress("Загружаю модель...")
        try {
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
