package com.autoglm.android

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ModelManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_URL = "https://huggingface.co/THUDM/autoglm-phone-9b/resolve/main/model.tflite"
        private const val MODEL_FILENAME = "autoglm_phone_9b.tflite"
        private const val MODEL_SIZE = 3500000000L // 约3.5GB
        private const val EXPECTED_MD5 = "a1b2c3d4e5f67890" // 示例MD5，实际需要从官方获取
        
        @Volatile
        private var instance: ModelManager? = null
        
        fun getInstance(context: Context): ModelManager {
            return instance ?: synchronized(this) {
                instance ?: ModelManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val httpClient = OkHttpClient()
    
    /**
     * 检查模型文件是否存在且完整
     */
    fun isModelAvailable(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > MODEL_SIZE * 0.9
    }
    
    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return modelFile.exists()
    }
    
    /**
     * 获取模型文件
     */
    fun getModelFile(): File {
        return File(context.filesDir, MODEL_FILENAME)
    }
    
    /**
     * 下载模型文件
     */
    suspend fun downloadModel(onProgress: (progress: Int) -> Unit): Boolean {
        return try {
            val request = Request.Builder()
                .url(MODEL_URL)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "下载失败: ${response.code}")
                return false
            }
            
            val modelFile = File(context.filesDir, MODEL_FILENAME)
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.temp")
            
            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        
                        // 更新进度
                        val progress = ((totalBytes.toDouble() / MODEL_SIZE) * 100).toInt()
                        onProgress(progress.coerceAtMost(100))
                    }
                }
            }
            
            // 验证文件完整性
            if (validateModelFile(tempFile)) {
                // 重命名为正式文件
                tempFile.renameTo(modelFile)
                Log.d(TAG, "模型下载完成")
                true
            } else {
                Log.e(TAG, "模型文件验证失败")
                tempFile.delete()
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "下载过程中出错: ${e.message}")
            false
        }
    }
    
    /**
     * 验证模型文件完整性
     */
    private fun validateModelFile(file: File): Boolean {
        return try {
            // 检查文件大小
            if (file.length() < MODEL_SIZE * 0.9) {
                Log.w(TAG, "文件大小异常: ${file.length()}")
                return false
            }
            
            // 计算MD5校验和（可选，需要从官方获取正确的MD5）
            val md5 = calculateMD5(file)
            Log.d(TAG, "文件MD5: $md5")
            
            // 如果提供了正确的MD5，进行验证
            if (EXPECTED_MD5.isNotBlank() && md5 != EXPECTED_MD5) {
                Log.w(TAG, "MD5校验失败")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "验证文件时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 计算文件的MD5值
     */
    private fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 获取模型文件路径
     */
    fun getModelFilePath(): String {
        return File(context.filesDir, MODEL_FILENAME).absolutePath
    }
    
    /**
     * 删除模型文件
     */
    fun deleteModel(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            true
        }
    }
    
    /**
     * 获取模型文件大小
     */
    fun getModelFileSize(): Long {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (modelFile.exists()) modelFile.length() else 0
    }
}