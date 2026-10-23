# Voyager-Android LSPosed Agent

基于 LSPosed / Xposed 框架的进程内自动化代理插件，内嵌 NanoHTTPD 轻量服务，支持大语言模型（LLM）进行环境感知与动作执行。

## 🚀 如何在 GitHub 上一键自动编译 APK？

本项目已经配置了完整的 **GitHub Actions 自动化编译工作流**（`.github/workflows/build_apk.yml`），无需在电脑上安装几个 G 的 Android Studio！

### 操作方法（仅需 2 步）：

1. **新建 GitHub 仓库**：
   - 打开 GitHub，点击右上角 **New repository**，输入任意仓库名（如 `voyager-xposed-agent`），选择 **Public** 或 **Private**，点击创建。

2. **上传本项目文件**：
   - 将本项目的所有文件（包括隐藏文件夹 `.github`）直接上传或者推送（`git push`）到该 GitHub 仓库。
   - 上传成功后，点击 GitHub 仓库顶部的 **Actions** 标签页。
   - 你会看到 `自动编译 Xposed 模块 APK` 工作流已自动触发！
   - 等待约 1~2 分钟编译完成后，进入该次任务，在 **Artifacts** 区域即可直接下载编译生成的 `VoyagerAgent-debug-apk.zip`（解压即可得到 `.apk` 安装包）！

## 📱 安装与调试

1. 手机需已 Root 并安装 LSPosed (Zygisk)。
2. 安装编译生成的 APK，打开 LSPosed 管理器，启用该模块并勾选目标 App。
3. 强制停止目标 App 并重新打开。
4. PC 运行 `adb forward tcp:28888 tcp:28888`。
5. 执行 `python test_agent.py` 或 `curl http://127.0.0.1:28888/status` 测试通信！
