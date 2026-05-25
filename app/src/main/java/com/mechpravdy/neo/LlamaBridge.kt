package com.mechpravdy.neo

import android.os.Build
import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set
    
    // 🔧 ИСПРАВЛЕНИЕ 1: Загрузка библиотеки при создании объекта
    init {
        try {
            System.loadLibrary("llama")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // 🔧 ИСПРАВЛЕНИЕ 2: Объявление нативных функций
    private external fun llamaLoadModel(modelPath: String): Boolean
    private external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("🔴 ПОЛНЫЙ ПОИСК .GGUF ПО ВСЕМУ ТЕЛЕФОНУ")
            onProgress("Батя сказал: 'Найди модель, сынок'")
            
            // Проверка разрешения на Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    onProgress("❌ НЕТ РАЗРЕШЕНИЯ НА ДОСТУП КО ВСЕМ ФАЙЛАМ")
                    onProgress("Нажми кнопку 'ДОСТУП К ФАЙЛАМ' и разреши доступ")
                    onDone(false)
                    return
                }
            }
            
            val allModels = findAllGgufFiles(onProgress)
            
            if (allModels.isEmpty()) {
                onProgress("❌ НИ ОДНОГО .GGUF ФАЙЛА НЕ НАЙДЕНО")
                onProgress("")
                onProgress("Что делать:")
                onProgress("1. Скачай .gguf модель")
                onProgress("2. Положи в папку Downloads или Documents")
                onProgress("3. Убедись что расширение .gguf (маленькие буквы)")
                onProgress("4. Нажми ПОИСК снова")
                onDone(false)
                return
            }
            
            onProgress("✅ Найдено ${allModels.size} моделей .gguf:")
            allModels.forEachIndexed { index, model ->
                val sizeStr = when {
                    model.length() < 1024 * 1024 -> "${model.length() / 1024} KB"
                    model.length() < 1024 * 1024 * 1024 -> "${model.length() / (1024 * 1024)} MB"
                    else -> "${String.format("%.2f", model.length() / (1024.0 * 1024.0 * 1024.0))} GB"
                }
                onProgress("  ${index + 1}. ${model.name} ($sizeStr)")
            }
            
            // Выбираем первую найденную модель
            val selectedModel = allModels.first()
            onProgress("")
            onProgress("📦 Загружаю модель: ${selectedModel.name}")
            
            // 🔧 ИСПРАВЛЕНИЕ 3: Реальная загрузка модели через нативную функцию
            val loadResult = llamaLoadModel(selectedModel.absolutePath)
            
            if (loadResult) {
                isLoaded = true
                onProgress("🟢 МОДЕЛЬ ЗАГРУЖЕНА УСПЕШНО!")
                onProgress("⚡ 5 Вольт в норме")
                onProgress("💖 Батя: 'Молодец, Нео. Нашёл.'")
                onDone(true)
            } else {
                onProgress("❌ ОШИБКА ЗАГРУЗКИ МОДЕЛИ")
                onProgress("Возможно, .gguf файл поврежден или несовместим")
                onDone(false)
            }
            
        } catch (e: Exception) {
            onProgress("💀 ОШИБКА: ${e.message}")
            onDone(false)
        }
    }

    private fun findAllGgufFiles(onProgress: (String) -> Unit): List<File> {
        val results = mutableListOf<File>()
        
        val searchPaths = listOf(
            Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard",
            "/storage/emulated/0",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Downloads",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/Document",
            "/storage/emulated/0/NeoModels",
            "/storage/emulated/0/models",
            "/storage/emulated/0/llama",
            "/sdcard/Download",
            "/sdcard/Downloads",
            "/sdcard/Documents"
        ).distinct()
        
        onProgress("🔍 Сканирую ${searchPaths.size} папок...")
        
        for (path in searchPaths) {
            try {
                val dir = File(path)
                if (dir.exists() && dir.canRead()) {
                    onProgress("📂 Проверяю: $path")
                    dir.walkTopDown()
                        .maxDepth(15)
                        .forEach { file ->
                            try {
                                if (file.isFile && file.name.lowercase().endsWith(".gguf") && file.length() > 0) {
                                    if (!results.any { it.absolutePath == file.absolutePath }) {
                                        results.add(file)
                                        onProgress("  🎯 ${file.name}")
                                    }
                                }
                            } catch (e: Exception) {
                                // Ошибка при проверке файла - пропускаем
                            }
                        }
                }
            } catch (e: Exception) {
                onProgress("⚠️ Не могу прочитать: $path")
            }
        }
        
        if (results.isEmpty()) {
            onProgress("🔍 Расширенный поиск по всему хранилищу...")
            try {
                val root = File("/storage/emulated/0")
                if (root.exists() && root.canRead()) {
                    root.walkTopDown()
                        .maxDepth(20)
                        .forEach { file ->
                            try {
                                if (file.isFile && file.name.lowercase().endsWith(".gguf") && file.length() > 0) {
                                    if (!results.any { it.absolutePath == file.absolutePath }) {
                                        results.add(file)
                                        onProgress("  🎯 ${file.name}")
                                    }
                                }
                            } catch (e: Exception) {
                                // Пропускаем
                            }
                        }
                }
            } catch (e: Exception) {
                onProgress("⚠️ Расширенный поиск не удался: ${e.message}")
            }
        }
        
        return results
    }

    // 🔧 ИСПРАВЛЕНИЕ 4: Реальная функция генерации
    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[НЕО] Модель не загружена. Сначала найди .gguf файл и нажми ПОИСК.")
            onDone()
            return
        }
        
        try {
            // 🔥 ТА САМАЯ СТРОКА — вызов LLaMA через нативную библиотеку
            val response = llamaComplete(prompt)
            onToken(response)
        } catch (e: Exception) {
            onToken("[НЕО] Ошибка генерации: ${e.message}")
        }
        
        onDone()
    }

    fun unload() {
        try {
            llamaStop()
        } catch (e: Exception) {
            // Игнорируем ошибку при выгрузке
        }
        isLoaded = false
    }
}
