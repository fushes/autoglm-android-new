package com.autoglm.android

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
    }
    
    private var imageReader: ImageReader? = null
    private var onImageAvailableListener: ((Bitmap) -> Unit)? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ScreenCaptureService created")
        sendStatusUpdate("started", "屏幕捕获服务已启动")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Log.d(TAG, "onStartCommand called")
            
            // 检查必要的权限
            if (!hasRequiredPermissions()) {
                Log.e(TAG, "缺少必要的屏幕捕获权限")
                sendStatusUpdate("error", "缺少屏幕捕获权限，请检查权限设置")
                return START_NOT_STICKY
            }
            
            sendStatusUpdate("started", "屏幕捕获服务运行中")
            Log.d(TAG, "屏幕捕获服务启动成功")
            
            return START_STICKY
            
        } catch (e: Exception) {
            Log.e(TAG, "服务启动失败: ${e.message}", e)
            sendStatusUpdate("error", "服务启动失败: ${e.message}")
            return START_NOT_STICKY
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        // 检查屏幕捕获权限
        // 这里可以添加具体的权限检查逻辑
        return true // 暂时返回true，实际实现需要检查具体权限
    }
    
    fun setupImageReader(width: Int, height: Int, density: Int): Boolean {
        return try {
            imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.RGB_565, 2)
            
            imageReader?.setOnImageAvailableListener({ reader ->
                serviceScope.launch {
                    processImage(reader)
                }
            }, null)
            
            Log.d(TAG, "ImageReader created: ${width}x${height}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ImageReader", e)
            false
        }
    }
    
    fun getImageReader(): ImageReader? = imageReader
    
    private suspend fun processImage(reader: ImageReader) {
        withContext(Dispatchers.IO) {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                image?.let { img ->
                    val bitmap = imageToBitmap(img)
                    
                    // 保存调试图片
                    saveDebugImage(bitmap)
                    
                    // 通知监听器
                    onImageAvailableListener?.invoke(bitmap)
                    
                    // 发送给AutoGLM服务
                    sendToAutoGLMService(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
            } finally {
                image?.close()
            }
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.RGB_565
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        return bitmap
    }
    
    private fun saveDebugImage(bitmap: Bitmap) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AutoGLM_${timestamp}.png"
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "AutoGLM"
            )
            
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Debug image saved: ${file.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save debug image", e)
        }
    }
    
    private fun sendToAutoGLMService(bitmap: Bitmap) {
        try {
            val intent = Intent("com.autoglm.SCREEN_CAPTURE")
            intent.putExtra("bitmap", bitmap)
            sendBroadcast(intent)
            
            Log.d(TAG, "Screen capture sent to AutoGLM service")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send screen capture to AutoGLM service", e)
        }
    }
    
    private fun sendStatusUpdate(status: String, message: String) {
        try {
            val intent = Intent("com.autoglm.SERVICE_STATUS_UPDATE")
            intent.putExtra("status", status)
            intent.putExtra("message", message)
            sendBroadcast(intent)
            
            Log.d(TAG, "Service status updated: $status - $message")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send service status update", e)
        }
    }
    
    fun setOnImageAvailableListener(listener: (Bitmap) -> Unit) {
        onImageAvailableListener = listener
    }
    
    override fun onDestroy() {
        try {
            Log.d(TAG, "ScreenCaptureService destroyed")
            
            // 关闭ImageReader
            try {
                imageReader?.close()
                Log.d(TAG, "ImageReader已关闭")
            } catch (e: Exception) {
                Log.w(TAG, "关闭ImageReader失败: ${e.message}")
            }
            
            // 取消协程作用域
            try {
                serviceScope.cancel()
                Log.d(TAG, "协程作用域已取消")
            } catch (e: Exception) {
                Log.w(TAG, "取消协程作用域失败: ${e.message}")
            }
            
            // 发送服务停止状态
            sendStatusUpdate("stopped", "屏幕捕获服务已停止")
            
            super.onDestroy()
            
        } catch (e: Exception) {
            Log.e(TAG, "服务销毁过程中发生错误: ${e.message}", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}