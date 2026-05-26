package com.mechpravdy.neo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class LlamaBridge(private val activity: AppCompatActivity) {

    var isLoaded = false
        private set
    
    private var modelPath: String? = null
    
    private val filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Сохраняем доступ
                activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addProgress("📁 Файл выбран: ${uri.lastPathSegment}")
                
                val path = getRealPathFromUri(uri)
                if (path != null) {
                    modelPath = path
                    addProgress("📂 Путь: $path")
                    
                    val loadResult = llamaLoadModel(path)
                    if (loadResult) {
                        isLoaded = true
                        addProgress("🟢 МОДЕЛЬ ЗАГРУЖЕНА УСПЕШНО!")
                        addProgress("⚡ 5 Вольт в норме")
                        onDoneCallback?.invoke(true)
                    } else {
                        addProgress("❌ ОШИБКА ЗАГРУЗКИ МОДЕЛИ")
                        onDoneCallback?.invoke(false)
                    }
                } else {
                    addProgress("❌ Не удалось получить путь к файлу")
                    onDoneCallback?.invoke(false)
                }
            }
        } else {
            addProgress("❌ Файл не выбран")
            onDoneCallback?.invoke(false)
        }
    }
    
    private var onProgressCallback: ((String) -> Unit)? = null
    private var onDoneCallback: ((Boolean) -> Unit)? = null
    
    private fun addProgress(msg: String) {
        onProgressCallback?.invoke(msg)
    }
    
    init {
        try {
            System.loadLibrary("llama")
            addProgress("✅ Библиотека LLaMA загружена")
        } catch (e: Exception) {
            addProgress("❌ Ошибка загрузки библиотеки: ${e.message}")
        }
    }
    
    private external fun llamaLoadModel(modelPath: String): Boolean
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgressCallback = onProgress
        onDoneCallback = onDone
        
        addProgress("📁 Нажмите и выберите файл .gguf")
        addProgress("Откроется окно проводника")
        
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "model/gguf"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/storage/emulated/0/Download"))
                } catch (e: Exception) {
                    // Игнорируем
                }
            }
        }
        filePickerLauncher.launch(intent)
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
            onToken("[НЕО] Модель не загружена. Нажми МИСТРАЛЬ 3Б и выбери .gguf файл.")
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
        } catch (e: Exception) { }
        isLoaded = false
    }
}
