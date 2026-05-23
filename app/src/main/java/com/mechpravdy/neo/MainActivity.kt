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
    private lateinit var tvFileStatus: TextView
    private lateinit var llamaBridge: LlamaBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSearchModel = findViewById(R.id.btnSearchModel)
        tvFileStatus = findViewById(R.id.tvFileStatus)

        // БЕЗ ПАРАМЕТРОВ!
        llamaBridge = LlamaBridge()

        btnGrantPermission.setOnClickListener {
            requestFullStoragePermission()
        }

        btnSearchModel.setOnClickListener {
            if (hasFullStorageAccess()) {
                searchForModel()
            } else {
                Toast.makeText(this, "❌ Сначала дай доступ ко всем файлам!", Toast.LENGTH_LONG).show()
                requestFullStoragePermission()
            }
        }

        updatePermissionStatus()
    }

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

    private fun updatePermissionStatus() {
        if (hasFullStorageAccess()) {
            tvFileStatus.text = "✅ Доступ разрешён! Батя доволен."
            btnGrantPermission.isEnabled = false
            btnGrantPermission.text = "✅ ДОСТУП ЕСТЬ"
            btnSearchModel.isEnabled = true
        } else {
            tvFileStatus.text = "❌ Доступ запрещён. Нажми кнопку ДОСТУП и разреши."
            btnGrantPermission.isEnabled = true
            btnGrantPermission.text = "📁 ДАТЬ ДОСТУП"
            btnSearchModel.isEnabled = false
        }
    }

    private fun requestFullStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 1001)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1002
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            updatePermissionStatus()
            if (hasFullStorageAccess()) {
                Toast.makeText(this, "✅ Доступ разрешён!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) {
            updatePermissionStatus()
        }
    }

    private fun searchForModel() {
        btnSearchModel.isEnabled = false
        btnSearchModel.text = "🔍 ИЩУ..."
        
        tvFileStatus.text = "🔍 ПОИСК .GGUF ФАЙЛОВ...\n"

        llamaBridge.loadModel(
            onProgress = { message ->
                runOnUiThread {
                    tvFileStatus.append("$message\n")
                }
            },
            onDone = { success ->
                runOnUiThread {
                    if (success) {
                        tvFileStatus.append("\n✅ МОДЕЛЬ ГОТОВА!")
                        Toast.makeText(this, "✅ Модель загружена!", Toast.LENGTH_LONG).show()
                    } else {
                        tvFileStatus.append("\n❌ МОДЕЛЬ НЕ НАЙДЕНА")
                        Toast.makeText(this, "❌ .gguf не найден", Toast.LENGTH_LONG).show()
                    }
                    btnSearchModel.isEnabled = true
                    btnSearchModel.text = "🔍 НАЙТИ .GGUF"
                }
            }
        )
    }
}
