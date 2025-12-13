# GitHub部署指南 - 智谱AutoGLM Android应用

## 🚀 快速开始

本指南将帮助您将智谱AutoGLM项目部署到GitHub并自动构建APK文件。

## 📋 部署前准备

1. **GitHub账户**：确保您有GitHub账户
2. **项目文件**：确保所有项目文件已准备就绪
3. **Git客户端**：安装Git用于代码提交

## 🔧 部署步骤

### 步骤1：创建GitHub仓库

1. 登录GitHub
2. 点击右上角"+" → "New repository"
3. 填写仓库信息：
   - Repository name: `autoglm-android` (或其他名称)
   - Description: 智谱AutoGLM Android应用
   - Public/Private: 根据需求选择
   - 勾选"Add a README file"
   - 点击"Create repository"

### 步骤2：上传项目代码

```bash
# 进入项目目录
cd "c:\Users\mi\WPSDrive\386071991\WPS云盘\c记录•e个人文件\智谱autoglm本地部署"

# 初始化Git仓库
git init
git add .
git commit -m "初始提交：智谱AutoGLM Android应用"

# 连接到GitHub仓库
git remote add origin https://github.com/你的用户名/autoglm-android.git

# 推送代码
git branch -M main
git push -u origin main
```

### 步骤3：启用GitHub Actions

1. 进入GitHub仓库页面
2. 点击顶部"Actions"标签
3. 点击"I understand my workflows, go ahead and enable them"
4. 系统会自动检测到`.github/workflows/android-build.yml`文件

### 步骤4：触发首次构建

**方法一：推送代码触发**
```bash
# 修改任意文件（如README）
echo "# 更新说明" >> README.md

git add .
git commit -m "触发首次构建"
git push origin main
```

**方法二：创建Release触发（推荐）**
1. 进入GitHub仓库"Releases"页面
2. 点击"Create a new release"
3. 填写版本信息：
   - Tag version: `v1.0.0`
   - Release title: `v1.0.0 初始版本`
   - Description: 智谱AutoGLM Android应用初始版本
4. 点击"Publish release"

## 📱 获取APK文件

### 方法一：从GitHub Actions下载
1. 进入"Actions"页面
2. 点击最新的构建记录
3. 在"Artifacts"部分下载`AutoGLM-Release-APK`

### 方法二：从GitHub Releases下载
1. 进入"Releases"页面
2. 点击最新版本
3. 下载`AutoGLM-v1.0.0.apk`文件

## 🔍 构建状态监控

### 查看构建日志
1. 进入"Actions"页面
2. 点击构建记录
3. 查看"build-android"作业的详细日志

### 常见构建问题
- **构建失败**：检查Gradle配置和依赖版本
- **权限问题**：确保GitHub Actions有足够权限
- **缓存问题**：清除GitHub Actions缓存重新构建

## 📊 构建优化建议

### 缓存配置
项目已配置Gradle缓存，可显著提升构建速度：
```yaml
- name: Cache Gradle packages
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

### 构建矩阵（可选）
如需测试不同Android版本，可添加构建矩阵：
```yaml
strategy:
  matrix:
    api-level: [29, 30, 31, 32, 33, 34]
```

## 🔄 持续集成流程

### 自动触发条件
- **推送代码**：推送到main分支时自动构建
- **创建Release**：发布新版本时自动构建并上传APK
- **Pull Request**：创建PR时进行构建测试

### 构建产物管理
- **APK文件**：自动上传到GitHub Releases
- **构建日志**：保留30天构建历史
- **缓存管理**：自动管理Gradle缓存

## 📞 技术支持

### 常见问题解决
1. **构建超时**：GitHub Actions有6小时限制，通常足够
2. **依赖下载失败**：检查网络连接和镜像源
3. **签名问题**：Release版本需要签名配置

### 获取帮助
- 查看GitHub Actions文档
- 检查构建日志中的错误信息
- 在GitHub Issues中提问

## 🎯 下一步操作

1. **测试APK**：下载APK安装到小米手机测试
2. **功能验证**：验证AI助手功能是否正常
3. **用户反馈**：收集用户反馈进行优化
4. **版本迭代**：根据反馈发布新版本

---

**提示**：首次构建可能需要较长时间（约15-30分钟），后续构建会因缓存而更快。