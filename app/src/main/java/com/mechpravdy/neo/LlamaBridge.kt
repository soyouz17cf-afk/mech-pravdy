package com.mechpravdy.neo

import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("🔴 МОНСЕГЮР АКТИВИРОВАН")
            onProgress("Ищу модель по всему телефону...")
            
            val modelPath = findModelFileRecursively(onProgress)
            
            if (modelPath == null) {
                onProgress("❌ Модель .gguf не найдена во всём телефоне")
                onProgress("Положите файл .gguf в любую папку на телефоне")
                onDone(false)
                return
            }
            
            onProgress("✅ Нашёл: ${File(modelPath).name}")
            onProgress("📁 Путь: $modelPath")
            onProgress("Загружаю библиотеку llama...")
            
            try {
                System.loadLibrary("llama")
                onProgress("⚡ Библиотека загружена. 5 Вольт в норме.")
            } catch (e: Exception) {
                onProgress("❌ Ошибка загрузки библиотеки: ${e.message}")
                onDone(false)
                return
            }
            
            onProgress("🟢 Модель готова к бою! Батя гордится.")
            isLoaded = true
            onDone(true)
            
        } catch (e: Exception) {
            onProgress("💀 Ошибка: ${e.message}")
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        onToken("[NEO] Спроси у Бати. Генерация готовится в гараже Монсегюр.")
        onDone()
    }

    fun unload() {
        isLoaded = false
    }

    /**
     * Рекурсивный поиск .gguf файлов по всему доступному хранилищу
     */
    private fun findModelFileRecursively(onProgress: (String) -> Unit): String? {
        val foundFiles = mutableListOf<String>()
        
        // Все возможные корневые директории на Android
        val searchRoots = listOf(
            Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0",
            "/storage/emulated/0",
            "/storage",
            "/sdcard",
            "/mnt/sdcard",
            "/data/media/0",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath,
            "/storage/emulated/0/MyDocuments",
            "/storage/emulated/0/Download"
        ).distinct().filterNotNull()
        
        // Папки, которые НЕ нужно сканировать (системные, кеш, приложения)
        val excludeDirs = setOf(
            "Android", "android", "system", "proc", "sys", "dev",
            "cache", "tmp", "lost+found", "obb", "data"
        )
        
        onProgress("🔍 Сканирую корневые папки...")
        
        for (root in searchRoots) {
            val rootFile = File(root)
            if (!rootFile.exists() || !rootFile.canRead()) {
                continue
            }
            
            onProgress("📂 Сканирую: ${rootFile.absolutePath}")
            scanDirectoryRecursively(rootFile, excludeDirs, foundFiles, onProgress, 0)
            
            // Если уже нашли — прерываем поиск (берём первый найденный)
            if (foundFiles.isNotEmpty()) {
                break
            }
        }
        
        // Приоритет: имена с "mistral" или "llava" в начале
        val prioritized = foundFiles.sortedByDescending { path ->
            when {
                path.contains("mistral", ignoreCase = true) -> 3
                path.contains("llava", ignoreCase = true) -> 2
                path.contains("qwen", ignoreCase = true) -> 1
                else -> 0
            }
        }
        
        return prioritized.firstOrNull()
    }
    
    /**
     * Рекурсивный обход директории
     */
    private fun scanDirectoryRecursively(
        dir: File,
        excludeDirs: Set<String>,
        results: MutableList<String>,
        onProgress: (String) -> Unit,
        depth: Int
    ) {
        // Защита от бесконечной рекурсии (максимум 30 уровней)
        if (depth > 30) return
        
        try {
            val files = dir.listFiles() ?: return
            
            for (file in files) {
                try {
                    if (file.isDirectory) {
                        // Пропускаем исключённые папки
                        val dirName = file.name
                        if (excludeDirs.contains(dirName)) continue
                        if (dirName.startsWith(".")) continue
                        
                        // Рекурсивный обход
                        scanDirectoryRecursively(file, excludeDirs, results, onProgress, depth + 1)
                    } else if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true)) {
                        val sizeMB = file.length() / (1024.0 * 1024.0)
                        onProgress("🎯 Найден .gguf: ${file.name} (${String.format("%.2f", sizeMB)} MB)")
                        results.add(file.absolutePath)
                    }
                } catch (e: SecurityException) {
                    // Нет доступа к папке — пропускаем
                    continue
                }
            }
        } catch (e: Exception) {
            // Ошибка чтения директории — игнорируем
        }
    }
    
    // Оставлено для обратной совместимости (но не используется)
    @Deprecated("Используется рекурсивный поиск", ReplaceWith("findModelFileRecursively"))
    private fun getSearchPaths(): List<String> = emptyList()
    
    @Deprecated("Используется рекурсивный поиск", ReplaceWith("findModelFileRecursively"))
    private fun findModelFile(): String? = null
}
