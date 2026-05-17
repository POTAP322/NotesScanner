package com.dima.notesscanner.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class YandexDiskClient(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "YandexDiskClient"
    }
    /**
     * Получает URL для загрузки файла
     */
    suspend fun getUploadUrl(filePath: String, overwrite: Boolean = true): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "getUploadUrl: Getting upload URL for: $filePath")
            // Используем app:/ для папки приложения
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val url = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=app:/$encodedPath&overwrite=$overwrite"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "OAuth $token")
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "getUploadUrl: Response code: ${response.code}")

            if (response.isSuccessful) {
                val json = response.body?.string()
                val jsonObject = JSONObject(json)
                val href = jsonObject.getString("href")
                Log.d(TAG, "getUploadUrl: Got upload URL: ${href.take(50)}...")
                href
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "getUploadUrl: Failed, error: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUploadUrl: Error", e)
            null
        }
    }

    /**
     * Загружает файл на Яндекс.Диск
     */
    suspend fun uploadFile(file: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uploadUrl = getUploadUrl(remotePath)
            if (uploadUrl == null) {
                Log.e(TAG, "uploadFile: Failed to get upload URL")
                return@withContext false
            }

            val fileData = file.readBytes()
            val request = Request.Builder()
                .url(uploadUrl)
                .put(fileData.toRequestBody("application/pdf".toMediaTypeOrNull()))
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful
            Log.d(TAG, "uploadFile: Upload response code: ${response.code}, success: $isSuccess")
            response.close()
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "uploadFile: Error", e)
            false
        }
    }

    /**
     * Удаляем файл на Я диске
     */
    suspend fun deleteFile(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "deleteFile: Deleting file: $remotePath")
            val encodedPath = java.net.URLEncoder.encode(remotePath, "UTF-8")
            val url = "https://cloud-api.yandex.net/v1/disk/resources?path=app:/$encodedPath&permanently=true"

            val request = Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "OAuth $token")
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful
            Log.d(TAG, "deleteFile: Response code: ${response.code}, success: $isSuccess")

            if (!isSuccess) {
                val errorBody = response.body?.string()
                Log.e(TAG, "deleteFile: Failed, error: $errorBody")
            }
            response.close()
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "deleteFile: Error", e)
            false
        }
    }
}