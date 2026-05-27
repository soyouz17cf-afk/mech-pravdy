package com.mechpravdy.neo

import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModelFromPath(modelPath: String, libPath: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgress("Файл: ${File(modelPath).name}")
        val sizeMB = File(modelPath).length() / (1024 * 1024)
        onProgress("Размер: $sizeMB МБ")

        try {
            onProgress("Загружаю библиотеку...")
            System.load(libPath)

            onProgress("Библиотека загружена. Загружаю модель...")
            try {
                Thread.sleep(500)
            } catch (_: Exception) {}

            val result = llamaLoadModel(modelPath)
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
