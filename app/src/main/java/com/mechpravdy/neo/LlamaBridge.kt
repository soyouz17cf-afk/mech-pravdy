package com.mechpravdy.neo

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

    private val filePickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onProgressCallback?.invoke("📁 Выбран: ${uri.lastPathSegment}")

                val path = getRealPathFromUri(uri)
                if (path != null) {
                    modelPath = path
                    val loadResult = llamaLoadModel(path)
                    if (loadResult) {
                        isLoaded = true
                        onProgressCallback?.invoke("🟢 МОДЕЛЬ ЗАГРУЖЕНА!")
                        onDoneCallback?.invoke(true)
                    } else {
                        onProgressCallback?.invoke("❌ ОШИБКА ЗАГРУЗКИ")
                        onDoneCallback?.invoke(false)
                    }
                } else {
                    onProgressCallback?.invoke("❌ Не удалось получить путь")
                    onDoneCallback?.invoke(false)
                }
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
    external fun llamaComplete(prompt: String): String
    private external fun llamaStop()

    fun loadModel(onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgressCallback = onProgress
        onDoneCallback = onDone

        onProgress("📁 Выбери файл .gguf")

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("/storage/emulated/0/Download")
                )
            }
        }
        filePickerLauncher.launch(intent)
    }

    fun loadModelFromPath(path: String, onProgress: (String) -> Unit, onDone: (Boolean) -> Unit) {
        onProgressCallback = onProgress
        onDoneCallback = onDone

        modelPath = path
        onProgress("🔍 Файл: ${File(path).name}")

        if (!File(path).exists()) {
            onProgress("❌ Файл не найден")
            onDone(false)
            return
        }

        val sizeMB = File(path).length() / (1024 * 1024)
        onProgress("📦 Размер: $sizeMB МБ")
        onProgress("⏳ Загружаю...")

        val result = llamaLoadModel(path)
        if (result) {
            isLoaded = true
            onProgress("🟢 ГОТОВ!")
            onDone(true)
        } else {
            onProgress("❌ ОШИБКА")
            onDone(false)
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        val cursor = activity.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(0)
                val dirs = listOf(
                    "/storage/emulated/0/Download",
                    "/storage/emulated/0/Downloads",
                    "/storage/emulated/0/Documents"
                )
                for (dir in dirs) {
                    val f = File(dir, name)
                    if (f.exists()) return f.absolutePath
                }
            }
        }
        return null
    }

    fun generate(prompt: String, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (!isLoaded) {
            onToken("[НЕО] Модель не загружена. Нажми МИСТРАЛЬ 3Б.")
            onDone()
            return
        }

        try {
            val response = llamaComplete(prompt)
            onToken(response)
        } catch (e: Exception) {
            onToken("[НЕО] Ошибка: ${e.message}")
        }

        onDone()
    }

    fun unload() {
        try {
            llamaStop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoaded = false
    }
}
