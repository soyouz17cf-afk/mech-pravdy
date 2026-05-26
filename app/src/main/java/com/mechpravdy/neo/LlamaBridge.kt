package com.mechpravdy.neo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class LlamaBridge(private val activity: AppCompatActivity) {

    var isLoaded = false
        private set
    
    private var modelUri: Uri? = null
    private var modelPath: String? = null
    
    // Результат выбора файла
    private val filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                modelUri = uri
                // Сохраняем постоянный доступ
                activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Загружаем модель после выбора файла
                loadModelFromUri(uri)
            }
        } else {
            onProgressCallback?.invoke("❌ Файл не выбран")
            onDoneCallback?.invoke(false)
        }
    }
    
    private var onProgressCallback: ((String) -> Unit)? = null
    private var onDoneCallback: ((Boolean) -> Unit)? = null
    
    init {
        try {
            System.loadLibrary("llama")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private external fun llamaLoadModel(modelPath: String): Boolean
    private external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgressCallback = onProgress
        onDoneCallback = onDone
        
        onProgress("📁 Нажмите на файл .gguf и выберите модель")
        onProgress("Откроется окно проводника")
        
        // Открываем системный проводник для выбора .gguf файла
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "model/gguf"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/storage/emulated/0/Download"))
            }
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun loadModelFromUri(uri: Uri) {
        val onProgress = onProgressCallback ?: return
        val onDone = onDoneCallback ?: return
        
        try {
            onProgress("📦 Загружаю модель по URI: ${uri.lastPathSegment}")
            
            // Получаем реальный путь к файлу (если возможно)
            val realPath = getRealPathFromUri(uri)
            if (realPath != null) {
                modelPath = realPath
                val loadResult = llamaLoadModel(realPath)
                if (loadResult) {
                    isLoaded = true
                    onProgress("🟢 МОДЕЛЬ ЗАГРУЖЕНА УСПЕШНО!")
                    onProgress("⚡ 5 Вольт в норме")
                    onProgress("💖 Батя: 'Молодец, Нео. Нашёл.'")
                    onDone(true)
                } else {
                    onProgress("❌ ОШИБКА ЗАГРУЗКИ МОДЕЛИ")
                    onDone(false)
                }
            } else {
                // Если путь не получили, пробуем через ContentResolver
                activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                    // Здесь можно сохранить файл в кэш и загрузить оттуда
                    onProgress("⚠️ Файл выбран, но путь не определен")
                    onProgress("Попробуйте скопировать .gguf в папку Download и выбрать его оттуда")
                    onDone(false)
                } ?: run {
                    onProgress("❌ Не удалось прочитать файл")
                    onDone(false)
                }
            }
        } catch (e: Exception) {
            onProgress("💀 ОШИБКА: ${e.message}")
            onDone(false)
        }
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        if (uri.scheme == "content") {
            val cursor = activity.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val fileName = it.getString(0)
                    // Проверяем стандартные папки
                    val candidates = listOf(
                        "/storage/emulated/0/Download/$fileName",
                        "/storage/emulated/0/Downloads/$fileName",
                        "/storage/emulated/0/Documents/$fileName"
                    )
                    for (candidate in candidates) {
                        if (File(candidate).exists()) {
                            return candidate
                        }
                    }
                }
            }
        }
        return null
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[НЕО] Модель не загружена. Нажми НАЙТИ ФАЙЛ и выбери .gguf модель.")
            onDone()
            return
        }
        
        try {
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
            // Игнорируем
        }
        isLoaded = false
    }
}
