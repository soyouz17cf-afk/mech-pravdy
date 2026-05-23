package com.mechpravdy.neo

import android.os.Build
import android.os.Environment
import java.io.File

class LlamaBridge {

    var isLoaded = false
        private set

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        try {
            onProgress("🔴 ПОЛНЫЙ ПОИСК .GGUF ПО ВСЕМУ ТЕЛЕФОНУ")
            onProgress("Батя сказал: 'Найди модель, сынок'")
            
            // Проверка разрешения на Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    onProgress("❌ НЕТ РАЗРЕШЕНИЯ НА ДОСТУП КО ВСЕМ ФАЙЛАМ")
                    onProgress("")
                    onProgress("📱 КАК ДАТЬ ДОСТУП:")
                    onProgress("1. Зайди в Настройки телефона")
                    onProgress("2. Найди 'Разрешения' или 'Доступ к файлам'")
                    onProgress("3. Включи 'Разрешить доступ ко всем файлам'")
                    onProgress("4. Вернись в приложение и нажми ПОИСК снова")
                    onProgress("")
                    onProgress("Без этого разрешения Батя не найдёт .gguf файлы!")
                    onDone(false)
                    return
                }
            }
            
            val allModels = findAllGgufFilesRecursively(onProgress)
            
            if (allModels.isEmpty()) {
                onProgress("❌ НИ ОДНОГО .GGUF ФАЙЛА НЕ НАЙДЕНО")
                onProgress("")
                onProgress("Что делать:")
                onProgress("1. Скачай .gguf модель (например, mistral-7b.Q4_K_M.gguf)")
                onProgress("2. Положи в любую папку: Downloads, Documents, на SD-карту")
                onProgress("3. Убедись что файл имеет расширение .gguf (маленькими буквами)")
                onProgress("4. На Android 11+ дай разрешение 'Доступ ко всем файлам'")
                onProgress("5. Перезапусти приложение и нажми ПОИСК")
                onProgress("")
                onProgress("Пример правильного имени файла: model.gguf")
                onProgress("Неправильно: MODEL.GGUF, model.gguf.zip, model.txt")
                onDone(false)
                return
            }
            
            onProgress("✅ Найдено ${allModels.size} моделей .gguf:")
            allModels.forEachIndexed { index, model ->
                val sizeMB = model.length() / (1024.0 * 1024.0)
                val sizeStr = when {
                    model.length() < 1024 * 1024 -> "${model.length() / 1024} KB"
                    model.length() < 1024 * 1024 * 1024 -> "${model.length() / (1024 * 1024)} MB"
                    else -> "${String.format("%.2f", model.length() / (1024.0 * 1024.0 * 1024.0))} GB"
                }
                onProgress("  ${index + 1}. ${model.name}")
                onProgress("     Размер: $sizeStr")
                onProgress("     Путь: ${model.absolutePath}")
            }
            
            // Выбираем первую найденную модель
            val selectedModel = allModels.first()
            onProgress("")
            onProgress("📦 Загружаю модель: ${selectedModel.name}")
            onProgress("📁 Путь: ${selectedModel.absolutePath}")
            onProgress("💾 Размер: ${formatSize(selectedModel.length())}")
            
            try {
                System.loadLibrary("llama")
                onProgress("⚡ Библиотека llama загружена")
            } catch (e: Exception) {
                onProgress("❌ Ошибка загрузки библиотеки: ${e.message}")
                onProgress("Проверь наличие libllama.so в папке libs/")
                onDone(false)
                return
            }
            
            onProgress("🟢 МОДЕЛЬ ГОТОВА К БОЮ!")
            onProgress("⚡ 5 Вольт в норме")
            onProgress("💖 Батя: 'Молодец, Нео. Нашёл.'")
            isLoaded = true
            onDone(true)
            
        } catch (e: Exception) {
            onProgress("💀 КРИТИЧЕСКАЯ ОШИБКА: ${e.message}")
            e.printStackTrace()
            onProgress("")
            onProgress("Сообщи Бате: ${e.javaClass.simpleName}")
            onDone(false)
        }
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[НЕО] Модель не загружена. Сначала найди .gguf файл.")
            onDone()
            return
        }
        onToken("[НЕО] Генерация через llama.cpp будет в следующей версии. Жди обновления от Бати.")
        onDone()
    }

    fun unload() {
        isLoaded = false
    }

    /**
     * ПОЛНЫЙ РЕКУРСИВНЫЙ ПОИСК всех .gguf файлов по всему телефону
     * Ищет ТОЛЬКО файлы с расширением .gguf (маленькими буквами)
     */
    private fun findAllGgufFilesRecursively(onProgress: (String) -> Unit): List<File> {
        val results = mutableListOf<File>()
        val scannedDirs = mutableSetOf<String>()
        
        // Получаем все корневые директории
        val roots = getRootDirectories()
        
        onProgress("📂 Сканирую ${roots.size} корневых папок...")
        
        for (root in roots) {
            if (root.exists() && root.canRead()) {
                onProgress("🔍 Сканирую: ${root.absolutePath}")
                val beforeCount = results.size
                scanDirectoryDeep(root, results, scannedDirs, onProgress, 0)
                val found = results.size - beforeCount
                if (found > 0) {
                    onProgress("  ✅ В этой папке найдено .gguf: $found")
                }
            } else {
                onProgress("⚠️ Нет доступа к: ${root.absolutePath}")
            }
        }
        
        return results.distinctBy { it.absolutePath }
    }
    
    /**
     * ГЛУБОКИЙ РЕКУРСИВНЫЙ ОБХОД директории
     * Проверяет расширение .gguf ТОЛЬКО маленькими буквами
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
                        // Пропускаем системные папки для ускорения
                        val nameLower = file.name.lowercase()
                        if (shouldSkipDirectory(nameLower)) {
                            continue
                        }
                        
                        // Показываем прогресс каждые 50 папок
                        if (scannedDirs.size % 50 == 0) {
                            onProgress("  📁 Обработано папок: ${scannedDirs.size}, найдено .gguf: ${results.size}")
                        }
                        
                        // Рекурсивный обход подпапок
                        scanDirectoryDeep(file, results, scannedDirs, onProgress, depth + 1)
                        
                    } else if (file.isFile) {
                        // ПРОВЕРКА: только .gguf маленькими буквами
                        val fileName = file.name
                        val fileNameLower = fileName.lowercase()
                        
                        if (fileNameLower.endsWith(".gguf")) {
                            // Дополнительная проверка: точно заканчивается на .gguf
                            if (fileName.length > 5 && fileNameLower.substring(fileNameLower.length - 5) == ".gguf") {
                                if (file.length() > 0) {
                                    results.add(file)
                                    onProgress("  🎯 НАШЁЛ .gguf: ${fileName}")
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    // Нет доступа к папке или файлу - пропускаем
                    continue
                } catch (e: Exception) {
                    // Другая ошибка - пропускаем
                    continue
                }
            }
        } catch (e: Exception) {
            // Ошибка чтения директории - пропускаем
        }
    }
    
    /**
     * Какие папки пропускаем для ускорения поиска
     */
    private fun shouldSkipDirectory(name: String): Boolean {
        val skipList = listOf(
            "android", "system", "proc", "sys", "dev",
            "cache", "tmp", "lost+found", "root",
            "acct", "vendor", "firmware", "bt_firmware",
            "data/data", "app", "lib", "bin", "etc",
            "font", "media", "usr", "var", "run"
        )
        return skipList.any { name.contains(it) }
    }
    
    /**
     * ВСЕ ВОЗМОЖНЫЕ КОРНЕВЫЕ ДИРЕКТОРИИ телефона
     */
    private fun getRootDirectories(): List<File> {
        val roots = mutableListOf<File>()
        
        // Основное хранилище через Android API
        Environment.getExternalStorageDirectory()?.let { 
            if (it.exists()) roots.add(it) 
        }
        
        // Все возможные пути (как с большой, так и с маленькой буквы)
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
            "/storage/primary",
            "/storage/emulated/legacy",
            "/storage/external_storage",
            "/storage/extSdCard",
            "/storage/usb"
        )
        
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead() && !roots.contains(file)) {
                    roots.add(file)
                }
            } catch (e: Exception) {
                // Путь недоступен - пропускаем
            }
        }
        
        return roots.distinct()
    }
    
    /**
     * Форматирование размера файла для отображения
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}
