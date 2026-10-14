package com.example.test.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** App-private Drive storage. No refresh token, app server or broad Drive permission. */
class GoogleDriveBackup(private val token: String) {
    fun upload(bytes: ByteArray): String {
        require(bytes.size <= BackupStore.MAX_BYTES)
        val boundary = "hero_day_${System.nanoTime()}"
        val meta = JSONObject().put("name", FILE_NAME).put("parents", org.json.JSONArray().put("appDataFolder"))
        val start = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n".toByteArray()
        val end = "\r\n--$boundary--\r\n".toByteArray()
        val result = request("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id", "POST", start + bytes + end, "multipart/related; boundary=$boundary")
        return JSONObject(result.toString(Charsets.UTF_8)).getString("id")
    }

    fun downloadLatest(): ByteArray {
        val query = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val list = request("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$query&orderBy=modifiedTime%20desc&pageSize=1&fields=files(id)")
        val files = JSONObject(list.toString(Charsets.UTF_8)).getJSONArray("files")
        require(files.length() > 0) { "No backup in this account" }
        val id = files.getJSONObject(0).getString("id")
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,160}")))
        return request("https://www.googleapis.com/drive/v3/files/$id?alt=media")
    }

    private fun request(url: String, method: String = "GET", body: ByteArray? = null, type: String = "application/json"): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", type)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            check(connection.responseCode in 200..299) { "Drive request failed (${connection.responseCode})" }
            return connection.inputStream.use(BackupStore::readLimited)
        } finally { connection.disconnect() }
    }
    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val FILE_NAME = "hero-day-backup.json"
    }
}
