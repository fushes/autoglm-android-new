package com.autoglm.android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoGLMAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AutoGLMAccessibilityService"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")
        
        // 配置服务信息
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        
        this.serviceInfo = info
        Log.d(TAG, "无障碍服务配置完成")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleViewTextChanged(event)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        Log.d(TAG, "窗口状态变化 - 包名: $packageName, 类名: $className")
        
        // 记录当前活动窗口信息
        // 可以用于任务上下文理解
    }
    
    private fun handleViewClicked(event: AccessibilityEvent) {
        val source = event.source
        if (source != null) {
            val viewInfo = getViewInfo(source)
            Log.d(TAG, "视图点击: $viewInfo")
        }
    }
    
    private fun handleViewFocused(event: AccessibilityEvent) {
        val source = event.source
        if (source != null) {
            val viewInfo = getViewInfo(source)
            Log.d(TAG, "视图获得焦点: $viewInfo")
        }
    }
    
    private fun handleViewTextChanged(event: AccessibilityEvent) {
        val text = event.text?.joinToString(", ")
        Log.d(TAG, "文本变化: $text")
    }
    
    // 执行点击操作
    fun performClick(x: Int, y: Int) {
        try {
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(
                    android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                    0L,
                    100L
                ))
                .build()
            
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "执行点击操作: ($x, $y)")
            
        } catch (e: Exception) {
            Log.e(TAG, "点击操作失败: ${e.message}")
        }
    }
    
    // 执行滑动操作
    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 500L) {
        try {
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(
                    android.graphics.Path().apply {
                        moveTo(startX.toFloat(), startY.toFloat())
                        lineTo(endX.toFloat(), endY.toFloat())
                    },
                    0L,
                    duration
                ))
                .build()
            
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "执行滑动操作: ($startX, $startY) -> ($endX, $endY)")
            
        } catch (e: Exception) {
            Log.e(TAG, "滑动操作失败: ${e.message}")
        }
    }
    
    // 执行文本输入
    fun performTextInput(text: String) {
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val focusedNode = findFocusedNode(rootNode)
                if (focusedNode != null && focusedNode.isEditable) {
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Log.d(TAG, "执行文本输入: $text")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "文本输入失败: ${e.message}")
        }
    }
    
    // 查找获得焦点的节点
    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val focused = findFocusedNode(child)
                if (focused != null) {
                    return focused
                }
            }
        }
        
        return null
    }
    
    // 获取视图信息
    private fun getViewInfo(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        return "类名: ${node.className}, 文本: ${node.text}, 描述: ${node.contentDescription}, " +
               "位置: [${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom}]"
    }
    
    // 获取当前屏幕的UI层次结构
    fun getScreenHierarchy(): String {
        val rootNode = rootInActiveWindow
        return if (rootNode != null) {
            traverseNodeHierarchy(rootNode, 0)
        } else {
            "无法获取屏幕层次结构"
        }
    }
    
    private fun traverseNodeHierarchy(node: AccessibilityNodeInfo, depth: Int): String {
        val indent = "  ".repeat(depth)
        var result = "$indent${node.className}: ${node.text}\n"
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                result += traverseNodeHierarchy(child, depth + 1)
            }
        }
        
        return result
    }
}