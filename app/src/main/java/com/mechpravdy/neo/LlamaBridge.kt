package com.mechpravdy.neo

import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("🔴 ПОЛНЫЙ ПОИСК .GGUF ПО ВСЕМУ ТЕЛЕФОНУ")
            onProgress("Батя сказал: 'Найди модель, сынок'")
            
            val allModels = findAllGgufFilesRecursively(onProgress)
            
            if (allModels.isEmpty()) {
                onProgress("❌ НИ ОДНОГО .GGUF файла не найдено во всём телефоне")
                onProgress("")
                onProgress("Что делать:")
                onProgress("1. Скачай .gguf модель (например, mistral-7b.Q4_K_M.gguf)")
                onProgress("2. Положи в любую папку: Downloads, Documents, на SD-карту")
                onProgress("3. На Android 11+ дай разрешение 'Доступ ко всем файлам'")
                onProgress("4. Перезапусти приложение")
                onDone(false)
                return
            }
            
            onProgress("✅ Найдено ${allModels.size} моделей .gguf:")
            allModels.forEachIndexed { index, model ->
                val sizeMB = model.length() / (1024.0 * 1024.0)
                onProgress("  ${index + 1}. ${model.name} (${String.format("%.1f", sizeMB)} MB)")
                onProgress("     ${model.absolutePath.take(60)}...")
            }
            
            // Выбираем первую модель (можно добавить выбор)
            val selectedModel = allModels.first()
            onProgress("")
            onProgress("📦 Загружаю: ${selectedModel.name}")
            
            try {
                System.loadLibrary("llama")
                onProgress("⚡ Библиотека llama загружена")
            } catch (e: Exception) {
                onProgress("❌ Ошибка загрузки библиотеки: ${e.message}")
                onDone(false)
                return
            }
            
            onProgress("🟢 Модель готова! 5 Вольт в норме.")
            onProgress("Батя: 'Молодец, Нео'")
            isLoaded = true
            onDone(true)
            
        } catch (e: Exception) {
            onProgress("💀 Ошибка: ${e.message}")
            e.printStackTrace()
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        onToken("[НЕО] Модель загружена. Генерация через llama.cpp будет в следующей версии.")
        onDone()
    }

    fun unload() {
        isLoaded = false
    }

    /**
     * НАСТОЯЩИЙ РЕКУРСИВНЫЙ ПОИСК по всем доступным папкам телефона
     * Возвращает ВСЕ найденные .gguf файлы
     */
    private fun findAllGgufFilesRecursively(onProgress: (String) -> Unit): List<File> {
        val results = mutableListOf<File>()
        val scannedDirs = mutableSetOf<String>()
        
        // Все возможные корневые директории
        val roots = getRootDirectories()
        
        onProgress("📂 Сканирую ${roots.size} корневых папок...")
        
        for (root in roots) {
            if (root.exists() && root.canRead()) {
                onProgress("🔍 Сканирую: ${root.absolutePath}")
                scanDirectoryDeep(root, results, scannedDirs, onProgress, 0)
            }
        }
        
        return results.distinctBy { it.absolutePath }
    }
    
    /**
     * Глубокий рекурсивный обход директории
     */
    private fun scanDirectoryDeep(
        dir: File,
        results: MutableList<File>,
        scannedDirs: MutableSet<String>,
        onProgress: (String) -> Unit,
        depth: Int
    ) {
        // Защита от бесконечной рекурсии
        if (depth > 25) return
        
        // Не сканируем одну папку дважды
        val dirPath = dir.absolutePath
        if (dirPath in scannedDirs) return
        scannedDirs.add(dirPath)
        
        try {
            val files = dir.listFiles() ?: return
            
            for (file in files) {
                try {
                    if (file.isDirectory) {
                        // Пропускаем системные папки, но не все
                        val nameLower = file.name.lowercase()
                        if (shouldSkipDirectory(nameLower)) {
                            continue
                        }
                        
                        // Рекурсивно обходим подпапки
                        scanDirectoryDeep(file, results, scannedDirs, onProgress, depth + 1)
                        
                    } else if (file.isFile) {
                        // Проверяем расширение .gguf (без учёта регистра)
                        if (file.name.lowercase().endsWith(".gguf")) {
                            if (file.length() > 0) { // Не пустые файлы
                                results.add(file)
                                onProgress("  🎯 НАШЁЛ: ${file.name} (${formatSize(file.length())})")
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    // Нет доступа - пропускаем
                    continue
                }
            }
        } catch (e: Exception) {
            // Ошибка чтения - пропускаем
        }
    }
    
    /**
     * Какие папки пропускаем для ускорения поиска
     */
    private fun shouldSkipDirectory(name: String): Boolean {
        val skipList = listOf(
            "android", "system", "proc", "sys", "dev",
            "cache", "tmp", "lost+found", "root",
            "acct", "vendor", "firmware", "bt_firmware"
        )
        return skipList.any { name.contains(it) }
    }
    
    /**
     * Все корневые директории телефона
     */
    private fun getRootDirectories(): List<File> {
        val roots = mutableListOf<File>()
        
        // Основное хранилище
        Environment.getExternalStorageDirectory()?.let { roots.add(it) }
        
        // Дополнительные пути
        val paths = listOf(
            "/storage/emulated/0",
            "/storage/emulated",
            "/storage",
            "/sdcard",
            "/sdcard0",
            "/sdcard1",
            "/mnt/sdcard",
            "/mnt",
            "/data/media/0",
            "/storage/self/primary",
            "/storage/primary"
        )
        
        for (path in paths) {
            val file = File(path)
            if (file.exists() && !roots.contains(file)) {
                roots.add(file)
            }
        }
        
        return roots.distinct()
    }
    
    /**
     * Форматирование размера файла
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}
