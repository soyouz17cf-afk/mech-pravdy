package com.mechpravdy.neo

import android.content.Context
import java.io.File
import dalvik.system.PathClassLoader

class LlamaBridge {

    var isLoaded = false
        private set

    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModelFromPath(context: Context, modelPath: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgress("Файл: ${File(modelPath).name}")
        val sizeMB = File(modelPath).length() / (1024 * 1024)
        onProgress("Размер: $sizeMB МБ")

        try {
            // Загружаем библиотеку через системный ClassLoader
            val libPath = context.applicationInfo.nativeLibDir + "/libllama.so"
            onProgress("Загружаю библиотеку...")
            try {
                System.load(libPath)
            } catch (e: Exception) {
                // Если не вышло через System.load, пробуем через ClassLoader
                val classLoader = PathClassLoader(libPath, ClassLoader.getSystemClassLoader())
                Thread.currentThread().contextClassLoader = classLoader
            }

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
