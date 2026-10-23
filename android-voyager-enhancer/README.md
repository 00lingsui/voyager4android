# Gemini Voyager Enhancer (安卓客户端原生增强插件)

专门针对 **Google Gemini 原生安卓手机客户端 (`com.google.android.apps.bard`)** 开发的 LSPosed 插件。
完全基于 Android 原生 View 层级注入，不受 WebView 或前端渲染限制，在 Gemini 官方 App 中完美复刻 PC 浏览器插件 **Voyager** 的核心功能！

## 🌟 核心特性

1. **原生悬浮召唤球 (📁 Voyager)**：
   - 打开官方 Gemini 安卓 App，屏幕左侧边缘自动挂载原生悬浮球。
   - 点击平滑滑出暗黑风格侧边栏。

2. **对话分类文件夹 (Folders)**：
   - 随心建立「工作」、「编程」、「学术」等文件夹，给对话分门别类。

3. **常用 Prompt 库 (一键自动填入)**：
   - 内置高质量 Prompt 模版，支持自定义新增。
   - **点击任意 Prompt，插件自动遍历当前 Gemini 页面，精准找到提问输入框并自动填入文字聚焦**，无需反复复制粘贴！

## 🚀 GitHub Actions 自动云端编译 APK

1. 在 GitHub 新建仓库（如 `gemini-voyager-enhancer`）。
2. 将本工程的所有文件（含 `.github`）上传。
3. 点击 **Actions** 选项卡，等待 1 分钟自动编译完毕。
4. 在 **Artifacts** 区域直接下载 `Gemini-Voyager-Enhancer-APK.zip`，解压即为可直接在手机安装的 `.apk` 安装包！
