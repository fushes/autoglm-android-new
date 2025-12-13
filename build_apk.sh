#!/bin/bash

# 智谱AutoGLM Android应用构建脚本
# 适用于本地开发和GitHub Actions环境

echo "🚀 开始构建智谱AutoGLM Android应用..."

# 设置Gradle可执行权限
if [ ! -x "./gradlew" ]; then
    echo "🔧 设置Gradle可执行权限..."
    chmod +x ./gradlew
fi

# 清理构建缓存
echo "🧹 清理构建缓存..."
./gradlew clean

# 构建Release版本
echo "🔨 构建Release版本APK..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "✅ Release版本构建成功!"
    RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
    if [ -f "$RELEASE_APK" ]; then
        cp "$RELEASE_APK" "AutoGLM-release.apk"
        echo "📱 Release APK: AutoGLM-release.apk"
        
        # 显示APK文件信息
        APK_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        echo "📦 APK大小: $APK_SIZE"
    fi
else
    echo "❌ Release版本构建失败"
    exit 1
fi

# 生成构建报告
echo ""
echo "🎉 智谱AutoGLM构建完成!"
echo "==============================""
echo "📱 应用信息:"
echo "   包名: com.autoglm.android"
echo "   版本: 1.0"
echo "   SDK: Android 8.0+"
echo ""
echo "📦 构建产物:"
echo "   - AutoGLM-release.apk (发布版)"
echo ""
echo "🔧 安装说明:"
echo "1. 将APK文件传输到手机"
echo "2. 在手机上启用'未知来源应用'安装权限"
echo "3. 点击APK文件进行安装"
echo "4. 按照应用提示完成权限配置"
echo "==============================""

exit 0