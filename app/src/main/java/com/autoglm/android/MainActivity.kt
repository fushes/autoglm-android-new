package com.autoglm.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var taskInputEditText: EditText
    private lateinit var executeButton: Button
    private lateinit var logTextView: TextView
    
    private val modelManager by lazy { ModelManager(this) }
    private val scope = MainScope()
    
    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.autoglm.SERVICE_STATUS_UPDATE" -> {
                    val status = intent.getStringExtra("status")
                    val message = intent.getStringExtra("message")
                    
                    runOnUiThread {
                        when (status) {
                            "started" -> {
                                statusTextView.text = "AutoGLM服务运行中"
                                startButton.isEnabled = false
                                stopButton.isEnabled = true
                            }
                            "stopped" -> {
                                statusTextView.text = "AutoGLM服务已停止"
                                startButton.isEnabled = true
                                stopButton.isEnabled = false
                            }
                            "error" -> {
                                statusTextView.text = "服务错误: $message"
                            }
                        }
                        
                        if (message != null) {
                            addLog("服务状态: $message")
                        }
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
        checkModelStatus()
        checkPermissions()
        
        // 注册服务状态接收器
        val filter = IntentFilter("com.autoglm.SERVICE_STATUS_UPDATE")
        registerReceiver(serviceStatusReceiver, filter)
    }
    
    private fun initializeViews() {
        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        downloadButton = findViewById(R.id.downloadButton)
        progressBar = findViewById(R.id.progressBar)
        progressTextView = findViewById(R.id.progressTextView)
        taskInputEditText = findViewById(R.id.taskInputEditText)
        executeButton = findViewById(R.id.executeButton)
        logTextView = findViewById(R.id.logTextView)
    }
    
    private fun setupClickListeners() {
        startButton.setOnClickListener {
            startAutoGLMService()
        }
        
        stopButton.setOnClickListener {
            stopAutoGLMService()
        }
        
        downloadButton.setOnClickListener {
            downloadModel()
        }
        
        executeButton.setOnClickListener {
            executeTask()
        }
    }
    
    private fun checkModelStatus() {
        if (modelManager.isModelAvailable()) {
            val modelSize = modelManager.getModelFileSize()
            val sizeMB = String.format("%.2f", modelSize / (1024.0 * 1024.0))
            statusTextView.text = "模型已下载 ($sizeMB MB)"
            downloadButton.isEnabled = false
            startButton.isEnabled = true
        } else {
            statusTextView.text = "模型未下载，请先下载模型"
            downloadButton.isEnabled = true
            startButton.isEnabled = false
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }
        
        Dexter.withContext(this)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        addLog("权限已获取，准备启动服务")
                        requestAccessibilityService()
                    } else {
                        statusTextView.text = "部分权限未授权"
                        Toast.makeText(this@MainActivity, "请授权所有权限", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }
    
    private fun requestAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "请在无障碍设置中启用AutoGLM服务", Toast.LENGTH_LONG).show()
    }
    
    private fun downloadModel() {
        try {
            addLog("开始下载AutoGLM模型...")
            
            scope.launch {
                try {
                    downloadButton.isEnabled = false
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressTextView.visibility = TextView.VISIBLE
                    
                    val success = modelManager.downloadModel { progress ->
                        runOnUiThread {
                            progressBar.progress = progress
                            progressTextView.text = "下载进度: $progress%"
                            
                            if (progress == 100) {
                                addLog("模型下载完成")
                                checkModelStatus()
                            }
                        }
                    }
                    
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        progressTextView.visibility = TextView.GONE
                        
                        if (success) {
                            addLog("模型下载成功")
                            Toast.makeText(this@MainActivity, "模型下载成功", Toast.LENGTH_SHORT).show()
                        } else {
                            addLog("模型下载失败，请检查网络连接")
                            Toast.makeText(this@MainActivity, "模型下载失败，请检查网络连接", Toast.LENGTH_SHORT).show()
                            downloadButton.isEnabled = true
                        }
                    }
                    
                } catch (e: Exception) {
                    runOnUiThread {
                        addLog("模型下载过程中发生错误: ${e.message}")
                        progressBar.visibility = ProgressBar.GONE
                        progressTextView.visibility = TextView.GONE
                        downloadButton.isEnabled = true
                        Toast.makeText(this@MainActivity, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
        } catch (e: Exception) {
            addLog("启动下载任务失败: ${e.message}")
            Toast.makeText(this, "启动下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startAutoGLMService() {
        try {
            addLog("正在启动AutoGLM服务...")
            
            // 检查模型是否已下载
            if (!modelManager.isModelDownloaded()) {
                addLog("模型未下载，请先下载模型")
                Toast.makeText(this, "模型未下载，请先下载模型", Toast.LENGTH_LONG).show()
                return
            }
            
            // 启动AutoGLM服务
            val intent = Intent(this, AutoGLMService::class.java)
            startService(intent)
            
            // 启动屏幕捕获服务
            val screenIntent = Intent(this, ScreenCaptureService::class.java)
            startService(screenIntent)
            
            addLog("AutoGLM服务启动命令已发送")
            Toast.makeText(this, "正在启动AutoGLM服务", Toast.LENGTH_SHORT).show()
            
            // 禁用启动按钮，启用停止按钮
            startButton.isEnabled = false
            stopButton.isEnabled = true
            
        } catch (e: Exception) {
            addLog("启动AutoGLM服务失败: ${e.message}")
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            
            // 恢复按钮状态
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }
    
    private fun stopAutoGLMService() {
        try {
            addLog("正在停止AutoGLM服务...")
            
            // 停止AutoGLM服务
            val intent = Intent(this, AutoGLMService::class.java)
            stopService(intent)
            
            // 停止屏幕捕获服务
            val screenIntent = Intent(this, ScreenCaptureService::class.java)
            stopService(screenIntent)
            
            addLog("AutoGLM服务停止命令已发送")
            Toast.makeText(this, "正在停止AutoGLM服务", Toast.LENGTH_SHORT).show()
            
            // 禁用停止按钮，启用启动按钮
            startButton.isEnabled = true
            stopButton.isEnabled = false
            
        } catch (e: Exception) {
            addLog("停止AutoGLM服务失败: ${e.message}")
            Toast.makeText(this, "停止失败: ${e.message}", Toast.LENGTH_LONG).show()
            
            // 恢复按钮状态
            startButton.isEnabled = false
            stopButton.isEnabled = true
        }
    }
    
    private fun executeTask() {
        val task = taskInputEditText.text.toString().trim()
        if (task.isEmpty()) {
            Toast.makeText(this, "请输入任务指令", Toast.LENGTH_SHORT).show()
            return
        }
        
        addLog("执行任务: $task")
        
        // 禁用执行按钮，防止重复点击
        executeButton.isEnabled = false
        
        // 模拟任务执行过程
        scope.launch {
            delay(1000) // 模拟处理时间
            
            runOnUiThread {
                addLog("任务处理中...")
                
                // 模拟AI分析结果
                val analysisResult = when {
                    task.contains("微信") -> "检测到微信相关任务，将打开微信应用"
                    task.contains("浏览器") -> "检测到浏览器相关任务，将打开浏览器"
                    task.contains("发送") -> "检测到发送消息任务，将执行消息发送操作"
                    else -> "任务已接收，正在分析执行步骤..."
                }
                
                addLog("AI分析: $analysisResult")
                
                // 重新启用执行按钮
                executeButton.isEnabled = true
                
                // 清空输入框
                taskInputEditText.text.clear()
                
                Toast.makeText(this@MainActivity, "任务已提交处理", Toast.LENGTH_SHORT).show()
            }
        }
    }
        

    
    private fun addLog(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            val logEntry = "[$timestamp] $message\n"
            
            val currentText = logTextView.text.toString()
            if (currentText.length > 10000) {
                // 限制日志长度，避免内存问题
                logTextView.text = currentText.substring(currentText.length - 5000) + logEntry
            } else {
                logTextView.append(logEntry)
            }
            
            // 自动滚动到底部
            val scrollAmount = logTextView.layout.getLineTop(logTextView.lineCount) - logTextView.height
            if (scrollAmount > 0) {
                logTextView.scrollTo(0, scrollAmount)
            } else {
                logTextView.scrollTo(0, 0)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }
    
    private fun updateServiceStatus() {
        // 检查服务状态并更新UI
        // 这里可以添加服务状态检查逻辑
        statusTextView.text = "服务状态：待机"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        
        // 注销服务状态接收器
        try {
            unregisterReceiver(serviceStatusReceiver)
        } catch (e: Exception) {
            // 接收器可能未注册，忽略异常
        }
    }
}