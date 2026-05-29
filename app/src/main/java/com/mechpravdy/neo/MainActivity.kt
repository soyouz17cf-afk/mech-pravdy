private fun switchToLocal() {
    isLocalMode = true
    matrixHeader.localMode = true
    matrixHeader.gigaChatMode = false
    matrixHeader.connectionLost = false
    matrixHeader.invalidate()
    appendChat("[РЕЖИМ] МИСТРАЛЬ 3B (локальный)")
    setStatus("МИСТРАЛЬ", "yellow")

    val modelDir = getExternalFilesDir("models") ?: filesDir
    if (!modelDir.exists()) modelDir.mkdirs()
    val modelFile = File(modelDir, "gemma-2b-it-gpu-int8.bin")

    // Если модель уже готова и склеена — загружаем через MediaPipe
    if (modelFile.exists() && modelFile.length() > 500L * 1024 * 1024) {
        appendChat("[МОЗГ] Модель готова. Загружаю...")
        setStatus("Загружаю...", "yellow")
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Меч Правды")
            setMessage("Загрузка модели через MediaPipe...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }
        thread {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setTemperature(0.7f)
                    .setTopK(40)
                    .build()
                llmInference = LlmInference.createFromOptions(this@MainActivity, options)
                isModelLoaded = true
                runOnUiThread {
                    progressDialog.dismiss()
                    appendChat("[МОЗГ] Модель загружена! Готов к бою!")
                    setStatus("МИСТРАЛЬ", "green")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    appendChat("[МОЗГ] Ошибка: ${e.message}")
                    setStatus("Ошибка", "red")
                }
            }
        }
        return
    }

    // Проверяем, есть ли 5 скачанных частей (.001, .002, .003, .004, .005)
    val partFiles = (1..5).mapNotNull { i ->
        val f = File(modelDir, "gemma-2b-it-cpu-int8.${
            i.toString().padStart(3, '0')
        }")
        if (f.exists() && f.length() > 0) f else null
    }

    // Если все 5 частей на месте — склеиваем
    if (partFiles.size == 5) {
        appendChat("[МОЗГ] Найдены все 5 частей. Склеиваю...")
        setStatus("Склейка...", "yellow")
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Меч Правды")
            setMessage("Склейка частей в модель...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }
        thread {
            try {
                FileOutputStream(modelFile).use { output ->
                    for (partFile in partFiles.sortedBy { it.name }) {
                        partFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
                // Удаляем части после склейки
                partFiles.forEach { it.delete() }

                runOnUiThread {
                    progressDialog.dismiss()
                    appendChat("[МОЗГ] Склейка завершена! Нажми МИСТРАЛЬ 3B ещё раз.")
                    setStatus("Готов", "green")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    appendChat("[МОЗГ] Ошибка склейки: ${e.message}")
                    setStatus("Ошибка", "red")
                }
            }
        }
        return
    }

    // Если частей нет — начинаем загрузку по твоим новым ссылкам
    appendChat("[МОЗГ] Запускаю загрузку 5 частей Gemma 2B...")
    appendChat("[МОЗГ] Смотри прогресс в шторке уведомлений.")
    appendChat("[МОЗГ] Когда все скачаются — нажми МИСТРАЛЬ 3B ещё раз.")
    setStatus("Качаю...", "yellow")

    val newPartUrls = listOf(
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-2b-it-cpu-int8.001",
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-2b-it-cpu-int8.002",
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-2b-it-cpu-int8.003",
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-2b-it-cpu-int8.004",
        "https://github.com/soyouz17cf-afk/mech-pravdy/releases/download/v1.0/gemma-2b-it-cpu-int8.005"
    )

    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadIds.clear()
    for ((index, url) in newPartUrls.withIndex()) {
        val partFile = File(modelDir, "gemma-2b-it-cpu-int8.${(index+1).toString().padStart(3, '0')}")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Меч Правды: часть ${index+1}/5")
            .setDescription("Gemma 2B (500 МБ)")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(partFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        downloadIds.add(manager.enqueue(request))
    }
}
