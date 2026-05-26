package com.mechpravdy.neo

import android.os.AsyncTask
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadModelTask(
    private val file: File,
    private val onProgressUpdate: (Int) -> Unit,
    private val onDone: () -> Unit,
    private val onError: (String) -> Unit
) : AsyncTask<Void, Int, Boolean>() {

    private var errorMessage: String? = null

    companion object {
        const val MODEL_URL = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf"
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        return try {
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                errorMessage = "HTTP ${connection.responseCode}"
                return false
            }

            val fileSize = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(8192)
            var downloaded: Long = 0
            var bytesRead: Int
            var lastProgress = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead

                if (fileSize > 0) {
                    val progress = (downloaded * 100 / fileSize).toInt()
                    if (progress > lastProgress) {
                        lastProgress = progress
                        publishProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            file.exists() && file.length() > 0
        } catch (e: Exception) {
            errorMessage = e.message ?: "Неизвестная ошибка сети"
            if (file.exists()) file.delete()
            false
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        values[0]?.let { onProgressUpdate(it) }
    }

    override fun onPostExecute(result: Boolean) {
        if (result) {
            onDone()
        } else {
            onError(errorMessage ?: "Неизвестная ошибка")
        }
    }
}
