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

    override fun doInBackground(vararg p0: Void?): Boolean {
        return try {
            val url = URL(MODEL_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.connect()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                errorMessage = "HTTP ${conn.responseCode}"
                return false
            }

            val total = conn.contentLength
            val input = conn.inputStream
            val output = FileOutputStream(file)
            val buf = ByteArray(8192)
            var downloaded: Long = 0
            var read: Int
            var lastPercent = -1

            while (input.read(buf).also { read = it } != -1) {
                output.write(buf, 0, read)
                downloaded += read
                if (total > 0) {
                    val p = (downloaded * 100 / total).toInt()
                    if (p > lastPercent) {
                        lastPercent = p
                        publishProgress(p)
                    }
                }
            }

            output.flush()
            output.close()
            input.close()
            conn.disconnect()

            file.exists() && file.length() > 0
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ошибка сети"
            if (file.exists()) file.delete()
            false
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        values[0]?.let { onProgressUpdate(it) }
    }

    override fun onPostExecute(result: Boolean) {
        if (result) onDone() else onError(errorMessage ?: "Неизвестная ошибка")
    }
}
