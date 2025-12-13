package com.autoglm.android

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class MediaProjectionService : Service() {
    
    companion object {
        private const val TAG = "MediaProjectionService"
        const val RESULT_CODE = "result_code"
        const val RESULT_INTENT = "result_intent"
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaProjectionService created")
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        
        intent?.let { 
            val resultCode = it.getIntExtra(RESULT_CODE, -1)
            val resultIntent = it.getParcelableExtra<Intent>(RESULT_INTENT)
            
            if (resultCode != -1 && resultIntent != null) {
                startMediaProjection(resultCode, resultIntent)
            }
        }
        
        return START_STICKY
    }
    
    private fun startMediaProjection(resultCode: Int, data: Intent) {
        try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    stopSelf()
                }
            }, null)
            
            Log.d(TAG, "MediaProjection started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media projection", e)
            stopSelf()
        }
    }
    
    fun createVirtualDisplay(width: Int, height: Int, density: Int): Boolean {
        return try {
            imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.RGB_565, 2)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AutoGLM_ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            virtualDisplay != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create virtual display", e)
            false
        }
    }
    
    fun getImageReader(): ImageReader? = imageReader
    
    fun isProjectionActive(): Boolean {
        return mediaProjection != null && virtualDisplay != null
    }
    
    override fun onDestroy() {
        Log.d(TAG, "MediaProjectionService destroyed")
        
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        serviceScope.cancel()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}