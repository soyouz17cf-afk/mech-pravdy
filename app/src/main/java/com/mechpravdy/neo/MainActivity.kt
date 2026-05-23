package com.mechpravdy.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrantPermission: Button
    private lateinit var btnSearchModel: Button
    private lateinit var tvStatus: TextView
    private lateinit var llamaBridge: LlamaBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSearchModel = findViewById(R.id.btnSearchModel)
        tvStatus = findViewById(R.id.tvStatus)

        // Инициализация LlamaBridge с передачей context
        llamaBridge = LlamaBridge(this)

        // Кнопка "ДАТЬ ДОСТУП КО ВСЕМ ФАЙЛАМ"
        btnGrantPermission.setOnClickListener {
            requestFullStoragePermission()
        }

        // Кнопка "НАЙТИ .GGUF МОДЕЛЬ"
        btnSearchModel.setOnClickListener {
            if (hasFullStorageAccess()) {
                searchForModel()
            } else {
                Toast.makeText(this, "❌ Сначала дай доступ ко всем файлам!", Toast.LENGTH_LONG).show()
                requestFullStoragePermission()
            }
        }

        // Проверяем статус при запуске
        updatePermissionStatus()
    }

    /**
     * Проверка наличия разрешения на доступ ко всем файлам
     */
    private fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Обновление статуса разрешения в UI
     */
    private fun updatePermissionStatus() {
        if (hasFullStorageAccess()) {
            tvStatus.text = """
                ✅ СТАТУС: Доступ ко всем файлам РАЗРЕШЁН
                
                📱 Батя доволен!
                🔍 Нажми кнопку НАЙТИ .GGUF МОДЕЛЬ
                ⚡ Поиск идёт по всему телефону
                💾 Файлы с расширением .gguf (маленькие буквы)
            """.trimIndent()
            btnGrantPermission.isEnabled = false
            btnGrantPermission.text = "✅ ДОСТУП РАЗРЕШЁН"
            btnSearchModel.isEnabled = true
        } else {
            tvStatus.text = """
                ❌ СТАТУС: Доступ ко всем файлам ЗАПРЕЩЁН
                
                📌 ЧТОБЫ ПРИЛОЖЕНИЕ РАБОТАЛО:
                
                1. Нажми кнопку ДАТЬ ДОСТУП НИЖЕ
                2. В открывшемся окне выбери "РАЗРЕШИТЬ"
                3. Вернись в приложение
                4. Нажми НАЙТИ .GGUF МОДЕЛЬ
                
                📁 .gguf файлы можно положить в:
                - Папку Download
                - Папку Documents
                - Создать папку NeoModels в корне
            """.trimIndent()
            btnGrantPermission.isEnabled = true
            btnSearchModel.isEnabled = false
        }
    }

    /**
     * Запрос разрешения на доступ ко всем файлам
     */
    private fun requestFullStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Для Android 11+
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                // Fallback
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 1001)
            }
        } else {
            // Для Android 10 и ниже
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1002
            )
        }
    }

    /**
     * Обработка результата запроса разрешения (Android 11+)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            // Проверяем, дал ли пользователь разрешение
            if (hasFullStorageAccess()) {
                Toast.makeText(this, "✅ Доступ разрешён! Теперь можно искать .gguf", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ Доступ не разрешён. Приложение не сможет найти модели.", Toast.LENGTH_LONG).show()
            }
            updatePermissionStatus()
        }
    }

    /**
     * Обработка запроса разрешений (Android 10 и ниже)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Доступ к файлам разрешён!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ Доступ к файлам запрещён. Модели не будут найдены.", Toast.LENGTH_LONG).show()
            }
            updatePermissionStatus()
        }
    }

    /**
     * Поиск .gguf модели
     */
    private fun searchForModel() {
        btnSearchModel.isEnabled = false
        btnSearchModel.text = "🔍 ПОИСК ИДЁТ..."

        tvStatus.text = """
            🔴 НЕО ЗАПУСКАЕТ ПОИСК .GGUF
            ⚡ 5 ВОЛЬТ В ЦЕПИ
            📂 СКАНИРУЮ ВСЕ ПАПКИ ТЕЛЕФОНА...
            
            ⏳ Пожалуйста, подожди 10-30 секунд
            🔍 Поиск идёт рекурсивно по всем папкам
            💾 Ищутся только файлы .gguf (маленькие буквы)
            
            Результаты появятся ниже...
        """.trimIndent()

        llamaBridge.loadModel(
            onProgress = { message ->
                runOnUiThread {
                    // Добавляем сообщение к текущему статусу
                    tvStatus.append("\n$message")
                    // Автопрокрутка вниз
                    val scrollAmount = tvStatus.layout?.let { tvStatus.layout.getLineTop(tvStatus.lineCount) - tvStatus.height }
                    if (scrollAmount != null && scrollAmount > 0) {
                        tvStatus.scrollBy(0, scrollAmount)
                    }
                }
            },
            onDone = { success ->
                runOnUiThread {
                    if (success) {
                        tvStatus.append("\n\n✅ МОДЕЛЬ УСПЕШНО ЗАГРУЖЕНА!")
                        tvStatus.append("\n💖 Батя: 'Молодец, Нео!'")
                        tvStatus.append("\n⚡ Можешь начинать генерацию")
                        Toast.makeText(this, "✅ Модель загружена! Батя гордится", Toast.LENGTH_LONG).show()
                    } else {
                        tvStatus.append("\n\n❌ МОДЕЛЬ НЕ НАЙДЕНА")
                        tvStatus.append("\n\n📌 ИНСТРУКЦИЯ:")
                        tvStatus.append("\n1. Скачай .gguf модель (например, с HuggingFace)")
                        tvStatus.append("\n2. Положи файл в любую папку на телефоне")
                        tvStatus.append("\n3. Убедись что расширение .gguf (маленькие буквы)")
                        tvStatus.append("\n4. Перезапусти приложение и нажми ПОИСК снова")
                        tvStatus.append("\n\nПример правильного имени: model.gguf")
                        Toast.makeText(this, "❌ Модель .gguf не найдена", Toast.LENGTH_LONG).show()
                    }
                    btnSearchModel.isEnabled = true
                    btnSearchModel.text = "🔍 НАЙТИ .GGUF МОДЕЛЬ"
                }
            }
        )
    }
}
