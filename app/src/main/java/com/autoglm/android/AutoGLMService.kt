package com.autoglm.android

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class AutoGLMService : Service() {
    
    companion object {
        private const val TAG = "AutoGLMService"
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
    }
    
    private var interpreter: Interpreter? = null
    private lateinit var imageProcessor: ImageProcessor
    private var isModelLoaded = false
    private var isRunning = false
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val screenCaptureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.autoglm.SCREEN_CAPTURE") {
                val bitmap = intent.getParcelableExtra<Bitmap>("bitmap")
                bitmap?.let {
                    serviceScope.launch {
                        processScreenCapture(it)
                    }
                }
            }
        }
    }
    
    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoglm.SERVICE_STATUS" -> {
                    val status = intent.getStringExtra("status")
                    val message = intent.getStringExtra("message")
                    Log.d(TAG, "收到服务状态更新: $status - $message")
                    
                    // 通知主界面更新状态
                    sendStatusToMainActivity(status, message)
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoGLM服务创建")
        
        // 注册屏幕捕获接收器
        val filter = IntentFilter("com.autoglm.SCREEN_CAPTURE")
        registerReceiver(screenCaptureReceiver, filter)
        
        // 注册服务状态广播接收器
        val statusFilter = IntentFilter("com.autoglm.SERVICE_STATUS")
        registerReceiver(serviceStatusReceiver, statusFilter)
        
        initializeImageProcessor()
        loadModel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Log.d(TAG, "AutoGLM服务启动")
            
            // 注册屏幕捕获接收器
            val filter = IntentFilter("com.autoglm.SCREEN_CAPTURE")
            registerReceiver(screenCaptureReceiver, filter)
            Log.d(TAG, "屏幕捕获接收器注册成功")
            
            // 注册服务状态接收器
            val statusFilter = IntentFilter("com.autoglm.SERVICE_STATUS")
            registerReceiver(serviceStatusReceiver, statusFilter)
            Log.d(TAG, "服务状态接收器注册成功")
            
            // 初始化图像处理器
            initializeImageProcessor()
            Log.d(TAG, "图像处理器初始化成功")
            
            // 加载模型
            if (!isModelLoaded) {
                loadModel()
            }
            
            if (!isRunning && isModelLoaded) {
                // 启动模型推理协程
                startInferenceLoop()
                isRunning = true
                Log.d(TAG, "模型推理循环已启动")
            }
            
            // 通知主界面服务已启动
            sendStatusToMainActivity("started", "AutoGLM服务已启动并运行")
            
            return START_STICKY
            
        } catch (e: Exception) {
            Log.e(TAG, "服务启动失败: ${e.message}", e)
            sendStatusToMainActivity("error", "服务启动失败: ${e.message}")
            return START_NOT_STICKY
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        try {
            super.onDestroy()
            Log.d(TAG, "AutoGLM服务销毁")
            isRunning = false
            
            // 关闭模型解释器
            interpreter?.close()
            Log.d(TAG, "模型解释器已关闭")
            
            // 注销接收器
            try {
                unregisterReceiver(screenCaptureReceiver)
                Log.d(TAG, "屏幕捕获接收器已注销")
            } catch (e: Exception) {
                Log.w(TAG, "注销屏幕捕获接收器失败: ${e.message}")
            }
            
            try {
                unregisterReceiver(serviceStatusReceiver)
                Log.d(TAG, "服务状态接收器已注销")
            } catch (e: Exception) {
                Log.w(TAG, "注销服务状态接收器失败: ${e.message}")
            }
            
            // 取消协程作用域
            serviceScope.cancel()
            Log.d(TAG, "协程作用域已取消")
            
            // 通知主界面服务已停止
            sendStatusToMainActivity("stopped", "AutoGLM服务已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "服务销毁过程中发生错误: ${e.message}", e)
        }
    }
    
    private fun initializeImageProcessor() {
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // 归一化到[0,1]
            .build()
    }
    
    private fun loadModel() {
        try {
            // 检查模型文件是否存在
            val modelManager = ModelManager.getInstance(this)
            if (!modelManager.isModelDownloaded()) {
                Log.e(TAG, "模型文件不存在，请先下载模型")
                isModelLoaded = false
                return
            }
            
            // 从文件系统加载模型
            val modelFile = modelManager.getModelFile()
            val model = loadModelFile(modelFile)
            
            val options = Interpreter.Options()
            options.setUseNNAPI(true) // 使用神经网络API加速
            options.setNumThreads(4) // 使用4个线程
            
            interpreter = Interpreter(model, options)
            isModelLoaded = true
            Log.d(TAG, "AutoGLM模型加载成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "模型加载失败: ${e.message}")
            isModelLoaded = false
        }
    }
    
    private fun loadModelFile(modelFile: File): ByteBuffer {
        val fileInputStream = FileInputStream(modelFile)
        val fileChannel = fileInputStream.channel
        
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            modelFile.length()
        )
        
        modelBuffer.order(ByteOrder.nativeOrder())
        
        fileInputStream.close()
        
        return modelBuffer
    }
    
    private fun startInferenceLoop() {
        serviceScope.launch {
            while (isRunning && isModelLoaded) {
                try {
                    // 每200毫秒执行一次推理
                    delay(200)
                    
                    // 这里应该获取最新的屏幕截图并进行推理
                    // 目前使用模拟数据
                    val mockBitmap = createMockBitmap()
                    processScreenCapture(mockBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "推理错误: ${e.message}")
                }
            }
        }
    }
    
    private fun processScreenCapture(bitmap: Bitmap) {
        try {
            // 预处理图像
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            // 准备输入输出缓冲区
            val inputBuffer = processedImage.buffer
            val outputBuffer = Array(1) { FloatArray(512) } // 假设输出为512维向量
            
            // 执行推理
            interpreter?.run(inputBuffer, outputBuffer)
            
            // 解析输出结果
            val action = parseOutput(outputBuffer[0])
            
            // 执行动作
            executeAction(action)
            
        } catch (e: Exception) {
            Log.e(TAG, "处理屏幕捕获失败", e)
        }
    }
    
    private fun createMockBitmap(): Bitmap {
        return Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
    }
    
    private fun parseOutput(output: FloatArray): AutoGLMAction {
        // 解析模型输出为具体动作
        // 这里需要根据AutoGLM的实际输出格式进行解析
        
        // 示例解析逻辑：找到最大概率的动作
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        val confidence = output[maxIndex]
        
        return when (maxIndex) {
            0 -> AutoGLMAction.Click(confidence, 0.5f, 0.5f) // 点击屏幕中心
            1 -> AutoGLMAction.Swipe(confidence, 0.2f, 0.5f, 0.8f, 0.5f) // 从左到右滑动
            2 -> AutoGLMAction.Input(confidence, "Hello AutoGLM") // 输入文本
            else -> AutoGLMAction.Wait(confidence) // 等待
        }
    }
    
    private fun executeAction(action: AutoGLMAction) {
        when (action) {
            is AutoGLMAction.Click -> {
                Log.d(TAG, "执行点击操作: (${action.x}, ${action.y}), 置信度: ${action.confidence}")
                performClick(action.x, action.y)
            }
            is AutoGLMAction.Swipe -> {
                Log.d(TAG, "执行滑动操作: (${action.startX}, ${action.startY}) -> (${action.endX}, ${action.endY}), 置信度: ${action.confidence}")
                performSwipe(action.startX, action.startY, action.endX, action.endY)
            }
            is AutoGLMAction.Input -> {
                Log.d(TAG, "执行输入操作: ${action.text}, 置信度: ${action.confidence}")
                performInput(action.text)
            }
            is AutoGLMAction.Wait -> {
                Log.d(TAG, "等待操作, 置信度: ${action.confidence}")
                // 不执行任何操作
            }
        }
    }
    
    private fun performClick(x: Float, y: Float) {
        // 通过无障碍服务执行点击
        // 实际实现需要与AutoGLMAccessibilityService配合
        Log.d(TAG, "执行点击: ($x, $y)")
    }
    
    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        // 通过无障碍服务执行滑动
        Log.d(TAG, "执行滑动: ($startX, $startY) -> ($endX, $endY)")
    }
    
    private fun performInput(text: String) {
        // 通过无障碍服务执行输入
        Log.d(TAG, "执行输入: $text")
    }
    
    private fun sendStatusToMainActivity(status: String?, message: String?) {
        try {
            val intent = Intent("com.autoglm.SERVICE_STATUS_UPDATE")
            intent.putExtra("status", status)
            intent.putExtra("message", message)
            sendBroadcast(intent)
            Log.d(TAG, "发送服务状态到主界面: $status - $message")
        } catch (e: Exception) {
            Log.e(TAG, "发送服务状态失败", e)
        }
    }
}

// AutoGLM动作类型定义
sealed class AutoGLMAction(val confidence: Float) {
    data class Click(val confidence: Float, val x: Float, val y: Float) : AutoGLMAction(confidence)
    data class Swipe(val confidence: Float, val startX: Float, val startY: Float, val endX: Float, val endY: Float) : AutoGLMAction(confidence)
    data class Input(val confidence: Float, val text: String) : AutoGLMAction(confidence)
    data class Wait(val confidence: Float) : AutoGLMAction(confidence)
}