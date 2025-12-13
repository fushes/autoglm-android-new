# 智谱AutoGLM Android应用

## 🚀 项目概述

智谱AutoGLM是一个基于智谱AI开源模型的Android AI助手应用，可以在小米手机上实现本地AI助手功能，支持跨应用操作和智能自动化。

## ✨ 核心特性

- **🤖 本地AI模型**：集成智谱AutoGLM 9B参数视觉-语言模型
- **📱 跨应用操作**：支持自动点击、滑动、输入等操作
- **👁️ 屏幕分析**：实时分析屏幕内容并智能响应
- **🔒 离线运行**：所有AI推理在设备本地完成，无需网络连接
- **🛡️ 隐私保护**：数据不会上传到云端，保护用户隐私

## 📥 快速安装 - GitHub在线构建

### 🚀 GitHub Actions自动构建（推荐）
项目已配置完整的GitHub Actions工作流，支持自动构建和APK发布：

**完整步骤：**
1. **Fork项目**：点击GitHub页面右上角的"Fork"按钮
2. **启用Actions**：进入项目的"Actions"页面，点击"I understand my workflows, go ahead and enable them"
3. **创建Release（可选）**：在"Releases"页面创建新版本标签，自动触发构建
4. **查看构建状态**：在"Actions"页面查看构建进度
5. **下载APK**：构建完成后，在"Actions"页面下载APK文件或从"Releases"页面下载

**构建特性：**
- ✅ **自动构建**：每次推送到main分支或创建Release时自动构建
- ✅ **缓存优化**：使用Gradle缓存加速构建过程
- ✅ **APK发布**：自动上传APK到GitHub Releases
- ✅ **构建产物**：在Actions页面可下载构建的APK文件

### 📱 安装到小米手机
1. 从GitHub下载APK文件
2. 在小米手机上启用"未知来源应用"安装权限：
   - 设置 > 安全 > 更多安全设置 > 安装未知应用
   - 授权文件管理器应用允许安装未知应用
3. 使用文件管理器打开下载的APK文件进行安装
4. 按照应用提示完成权限配置

## 🔧 本地构建（备选方案）

如果需要在本地构建，可以使用提供的构建脚本：
```bash
# 构建Release版本APK
./build_apk.sh

# 或直接使用Gradle
./gradlew assembleRelease
```

## 🏗️ 本地开发构建

### 环境要求
- Java Development Kit 8+
- Android SDK 34+
- Android Studio 2022.3+

### 构建命令
```bash
# 克隆项目
git clone <项目地址>
cd 智谱autoglm本地部署

# 构建APK
./build_apk.sh

# 或使用Gradle直接构建
./gradlew assembleRelease
```

### 安装到设备
```bash
# 连接Android设备
adb devices

# 安装APK
adb install AutoGLM-release.apk
```

## 📱 应用使用

### 首次使用配置
1. **权限授予**：授予存储、无障碍、屏幕捕获等权限
2. **模型下载**：首次启动会自动下载AI模型文件
3. **服务启动**：点击"启动服务"开始使用AI助手

### 功能使用
- **自然语言输入**：在输入框中输入任务指令
- **跨应用操作**：支持打开应用、发送消息、设置闹钟等
- **实时日志**：查看AI分析和操作过程

## 🛠️ 技术架构

### 核心技术栈
- **Android**：Kotlin + Android SDK 34
- **AI模型**：TensorFlow Lite + AutoGLM-Phone-9B
- **权限管理**：Dexter权限库
- **网络请求**：OkHttp + Retrofit

### 项目结构
```
├── app/src/main/java/com/autoglm/android/
│   ├── MainActivity.kt           # 主界面
│   ├── AutoGLMService.kt         # AI服务
│   ├── ScreenCaptureService.kt   # 屏幕捕获
│   └── ModelManager.kt           # 模型管理
├── app/src/main/res/             # 资源文件
├── .github/workflows/            # CI/CD配置
└── scripts/                      # 构建脚本
```

## 🔍 故障排除

### 常见问题
1. **构建失败**：检查Android SDK和Java环境
2. **安装失败**：确保允许未知来源应用安装
3. **权限问题**：在设置中手动授予所有权限
4. **模型加载失败**：检查网络连接和存储空间

### 调试模式
启用调试日志查看详细错误信息：
```bash
adb logcat | grep AutoGLM
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！

### 开发流程
1. Fork本项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 📄 许可证

本项目基于Apache 2.0许可证开源。

## 🔗 相关链接

- [智谱AI官网](https://www.zhipu.ai/)
- [AutoGLM论文](https://arxiv.org/abs/2411.00820)
- [GitHub仓库](https://github.com/zai-org/Open-AutoGLM)

---

**注意**：本应用为开源项目，仅供学习和研究使用。请遵守相关法律法规，合理使用AI技术。