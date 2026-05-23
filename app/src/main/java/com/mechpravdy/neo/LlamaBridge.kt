package com.mechpravdy.neo

import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("Ищу модель...")
            val modelPath = findModelFile()
            if (modelPath == null) {
                onProgress("Модель не найдена. Проверял папки:")
                val paths = getSearchPaths()
                for (p in paths) {
                    onProgress("  ${p}")
                    val dir = File(p)
                    if (dir.exists()) {
                        val allFiles = dir.listFiles()
                        if (allFiles != null) {
                            for (f in allFiles) {
                                onProgress("    ${f.name}")
                            }
                        }
                    } else {
                        onProgress("    (папка не существует)")
                    }
                }
                onProgress("Положите файл .gguf в MyDocuments/for fone")
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
