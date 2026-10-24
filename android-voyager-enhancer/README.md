# 🪐 Voyager for Gemini (Android LSPosed 原生增强插件)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Framework-LSPosed_/_Xposed-8A2BE2?style=for-the-badge" alt="Framework" />
  <img src="https://img.shields.io/badge/Target-Google_Gemini_App-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Target" />
  <img src="https://img.shields.io/badge/Build-GitHub_Actions_CI-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI" />
</p>

> 深度复刻 GitHub 顶级浏览器插件 **[voyager-crew/voyager](https://github.com/voyager-crew/voyager)** 的核心体验。  
> 专为 **Google Gemini 官方安卓客户端 (`com.google.android.apps.bard`)** 打造，脱离浏览器与网页限制，直接运行在原生 App 进程内，打造手机端终极 AI 生产力工作台！

---

## ✨ 核心特性矩阵

### 1. 🔘 自由拖拽 + 磁吸贴边的圆形悬浮球
- **完美圆形外观**：52dp 翡翠绿圆形发光球体（🪐），搭配浅绿高亮边框与原生立体投影（Elevation）。
- **全屏自由拖拽**：在屏幕任何位置均可手指按住移动，不再死板固定。
- **松手自动靠边吸附**：松开手指后，悬浮球根据屏幕中线自动平滑滑行并吸附到左边缘或右边缘，完全不遮挡正文。
- **闲置智能淡化**：3.5 秒不操作时自动淡化至 55% 半透明，屏幕更清爽；轻触瞬间恢复 100% 亮度。
- **坐标永久记忆**：自动记录你上次拖拽停留的位置，重启 App 后依然停留在你最习惯的操作区域。

### 2. 🌓 深色与浅色模式自动适配
- **无感主题切换**：自动检测宿主 Gemini 客户端或 Android 系统当前的配色模式。
- **暗黑模式 (Dark Mode)**：极具质感的 `#121216` 深灰背景、暗夜卡片与低对比度辅助文字。
- **明亮模式 (Light Mode)**：清爽高雅的浅灰白背景（`#F8FAFC`）、纯白卡片与深石板灰文字。

### 3. 📂 真实多级彩色文件夹系统 (树状直观折叠 + 点击直接进入会话)
- **类似 PC 端 Voyager 的树状折叠体验**：主界面直观展示分类文件夹，点击文件夹标题栏即可平滑展开/折叠内部会话，告别繁琐的多层弹窗。
- **点击条目直接进入对应会话**：在文件夹中点击任一已归档对话，系统自动关闭侧边栏并**直接进入并切换到相对应对话中**！
  - **4 重自适应无缝驱动**：
    1. **WebView DOM & Router 驱动**：自动检索 Gemini 内部侧边栏锚点并触发原生点击跳转，或重定向至会话 URL；
    2. **原生 View 树智能检索**：遍历宿主原生历史列表，精准匹配标题并模拟点击；
    3. **Deep Link 唤醒**：通过 Android 显式 Intent 直接路由至 Gemini 对应会话 ID；
    4. **浮动智能续接条 (Floating Resume HUD)**：若为跨设备离线记录，顶部浮现贴心小胶囊，支持一键填入输入框续聊与全文快照离线回看！
- **完整离线问答快照**：每条归档对话均完整保留问答快照，点击条目右侧的「📖」图标随时随地离线查看，点击「✕」一键移出。

### 3.1 🔑 永久固定签名机制（免卸载无缝更新）
- **告别卸载重装**：项目已内置全局唯一固定的签名秘钥证书（`app/voyager-release.jks`）。
- **无缝覆盖安装**：无论是 GitHub Actions 自动编译推送的 Release APK，还是本地编译的安装包，**签名证书完全一致，每次发布新版本直接点击安装即可无缝覆盖更新**，绝不会触发 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，亦无需在 LSPosed 中反复重新勾选模块！

### 4. 📍 实时时间线导航小地图 (Timeline)
- **实时探针抓取**：打开抽屉时动态下发探针，实时提取当前聊天屏幕上真实的 Q1, Q2, Q3 用户提问。
- **一键平滑跳转**：点击任意节点，屏幕**真正平滑滚动居中定位至该消息，并伴随半秒翠绿色高亮闪烁提醒**！
- **💬 一键引用回复**：点击节点右侧的「引用」按钮，自动格式化为 Markdown 引用块（`> 问题内容\n\n针对上述问题：`）并直接填入 Gemini 提问框并聚焦！

### 5. 💾 真实对话全文导出工具箱
- **Markdown 导出**：实时抓取整场对话的全部真实问答文本，生成带完整格式、代码块与时间戳的标准 `.md` 文件。
- **结构化 JSON 导出**：方便备份与二次分析。
- **一键全文复制**：自动唤起 Android 系统分享面板，直接发送到微信、QQ、Notion、邮件或本地文件管理。

### 6. 💡 智能提示词工程库
- **分类标签体系**：`💻 编程`、`📊 分析`、`📝 润色`、`🌐 翻译`。
- **支持 `{{变量}}` 模版**：遇到含动态变量的 Prompt（如 `{{目标代码}}`），点击自动弹出输入对话框，填写后**自动合成并精准填入 Gemini 原生输入框，光标定位到末尾并自动唤起软键盘**！

### 7. 🛠️ 效率黑科技
- 📐 **公式多格式提取**：点击回答中的数学公式，立即复制为 **LaTeX**、Word 专用的 **MathML** 或 **Notion** 格式。
- 🚫 **防强制自动滚底 (Scroll Lock)**：拦截发送新消息后强制滚动到底部的内置行为，保持当前阅读视角。
- 📱 **输入框快速收起**：长篇阅读时一键折叠隐藏底部输入框，直接释放 **35% 屏幕可视空间**。
- 🧜‍♀️ **Mermaid 图表渲染增强**：流程图与时序图可视化。

### 8. ☁️ 配置一键云备份与多端同步
- 点击「📤 备份所有文件夹与会话记录为 JSON」，一键打包所有自定义分类与提示词。
- 点击「📥 从 JSON 文本还原配置」，多台手机间无缝同步迁移。

---

## 📲 安装与使用指南

### 前提条件
- 手机已获取 Root 权限，并安装了 **Magisk / KernelSU / APatch**。
- 已刷入并激活 **LSPosed (Zygisk 版)**。
- 手机上安装了官方 **Google Gemini** 客户端（包名：`com.google.android.apps.bard`）。

### 极速安装步骤
1. 打开本仓库右侧的 **[Releases](../../releases)** 页面；
2. 直接下载最新版本的 **`Voyager-Gemini-Enhancer.apk`** 并安装到手机（**无需解压**）；
3. 打开 **LSPosed 管理器**，在“模块”列表中启用 **`Voyager for Gemini`**；
4. 勾选作用域：**Google Gemini (`com.google.android.apps.bard`)**；
5. 强制停止并重新打开 Gemini 官方客户端；
6. 屏幕边缘即会出现翡翠绿的 **🪐 圆形悬浮球**，点击即可开始使用！

---

## 🛠️ GitHub Actions 自动化在线编译流程

本项目已配置完整的 CI/CD 云端构建流（位于 `.github/workflows/build_apk.yml`）：
- **无需本地 Android Studio / SDK 环境**；
- 每次将代码推送到 GitHub（Push / PR / 手动触发），云端将自动调用 Ubuntu + JDK 17 + Gradle 8.5 进行编译；
- 编译完成后，会自动在仓库的 **Releases** 生成以版本号命名的发布页面，并直接附带单文件免解压的 `.apk` 安装包！

---

## 🔒 隐私与安全性

- **100% 纯本地运行**：本插件不包含任何第三方远程统计、上报或分析代码。
- **无额外隐私权限**：不申请网络权限（所有导出均基于 Android 原生系统 IPC Share 机制）。
- **数据本地存储**：所有文件夹、Prompt 模版与配置均保存在应用自身的沙盒私有存储中。

## 📝 版本更新日志 (Changelog)

### 🪐 Voyager for Gemini 1.00
- 🔘 **圆形发光悬浮球**：升级为 52dp 翡翠绿圆形发光悬浮球，支持全屏自由拖拽、松手平滑自动贴边吸附、闲置自动半透明淡化。
- 🔖 **会话书签与极速直达**：新增星标会话书签功能，一键收藏重点对话；点击书签卡片即刻直达相对应对话记录与完整问答详情页面。
- 📂 **三级分类文件夹**：精准捕获并收录当前会话，支持树状文件夹归类管理与点击回溯问答记录。
- 📍 **对话时间线导航**：自动提炼聊天提问节点为可视化小地图，点击平滑滚动并高亮定位当前对话。
- 💾 **多格式数据导出**：一键导出包含标准代码高亮渲染的 Markdown / JSON 格式文本，自动唤起系统级分享。
- 💡 **提示词库与变量模版**：内置 Prompt 词库，支持 `{{变量}}` 占位符一键填入输入框并聚焦。
- 📐 **数学公式多平台复制**：支持一键提取公式并转换为 LaTeX、MathML (专为 Word 优化) 及 Notion 兼容格式。
- 🔑 **永久固定签名**：内置统一 Release 签名配置，后续版本覆盖更新无需重复卸载安装。

---

## 📄 开源许可证
本项目基于 [MIT License](LICENSE) 开源。
致敬 [voyager-crew/voyager](https://github.com/voyager-crew/voyager) 团队所创造的卓越产品理念！
