import os
import shutil

PROJECT_DIR = "/android-voyager-enhancer"
os.makedirs(PROJECT_DIR, exist_ok=True)

# 1. settings.gradle
with open(os.path.join(PROJECT_DIR, "settings.gradle"), "w", encoding="utf-8") as f:
    f.write("""pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "VoyagerWebEnhancer"
include ':app'
""")

# 2. root build.gradle
with open(os.path.join(PROJECT_DIR, "build.gradle"), "w", encoding="utf-8") as f:
    f.write("""plugins {
    id 'com.android.application' version '8.2.2' apply false
}
""")

# 3. gradle.properties
with open(os.path.join(PROJECT_DIR, "gradle.properties"), "w", encoding="utf-8") as f:
    f.write("""org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
""")

# 4. gradle wrapper
wrapper_dir = os.path.join(PROJECT_DIR, "gradle", "wrapper")
os.makedirs(wrapper_dir, exist_ok=True)
with open(os.path.join(wrapper_dir, "gradle-wrapper.properties"), "w", encoding="utf-8") as f:
    f.write("""distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""")

# 5. gradlew
with open(os.path.join(PROJECT_DIR, "gradlew"), "w", encoding="utf-8") as f:
    f.write("""#!/bin/sh
gradle "$@"
""")
os.chmod(os.path.join(PROJECT_DIR, "gradlew"), 0o755)

# 6. app/build.gradle
app_dir = os.path.join(PROJECT_DIR, "app")
os.makedirs(app_dir, exist_ok=True)
with open(os.path.join(app_dir, "build.gradle"), "w", encoding="utf-8") as f:
    f.write("""plugins {
    id 'com.android.application'
}

android {
    namespace 'com.voyager.enhancer'
    compileSdk 34

    defaultConfig {
        applicationId "com.voyager.enhancer"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // 核心 Xposed API (必须是 compileOnly，不能打包进 APK)
    compileOnly 'de.robv.android.xposed:api:82'
}
""")

# 7. app/src/main/AndroidManifest.xml
src_main = os.path.join(app_dir, "src", "main")
os.makedirs(src_main, exist_ok=True)
with open(os.path.join(src_main, "AndroidManifest.xml"), "w", encoding="utf-8") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="Voyager AI Enhancer"
        android:supportsRtl="true">

        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposeddescription"
            android:value="AI 对话网页增强（文件夹管理与快捷Prompt侧边栏注入）" />
        <meta-data
            android:name="xposedminversion"
            android:value="82" />
    </application>

</manifest>
""")

# 8. app/src/main/assets/xposed_init
assets_dir = os.path.join(src_main, "assets")
os.makedirs(assets_dir, exist_ok=True)
with open(os.path.join(assets_dir, "xposed_init"), "w", encoding="utf-8") as f:
    f.write("com.voyager.enhancer.HookEntry\n")

# 9. app/src/main/assets/voyager_enhancer.js (完整的前端注入脚本)
with open(os.path.join(assets_dir, "voyager_enhancer.js"), "w", encoding="utf-8") as f:
    f.write("""(function() {
  if (window.__VOYAGER_ENHANCER_INITED__) return;
  window.__VOYAGER_ENHANCER_INITED__ = true;
  console.log('[Voyager] AI 手机端对话增强已成功注入当前页面');

  // 1. 注入适配手机的 CSS
  const style = document.createElement('style');
  style.id = 'voyager-mobile-style';
  style.textContent = `
    #voyager-fab-btn {
      position: fixed;
      left: 0;
      top: 40%;
      z-index: 2147483646;
      background: #10a37f;
      color: #fff;
      padding: 8px 10px 8px 6px;
      border-radius: 0 16px 16px 0;
      font-size: 14px;
      font-weight: bold;
      cursor: pointer;
      box-shadow: 2px 2px 12px rgba(0,0,0,0.35);
      user-select: none;
      opacity: 0.9;
    }
    #voyager-fab-btn:active { transform: scale(0.95); opacity: 1; }
    #voyager-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.6);
      z-index: 2147483646;
      display: none;
      backdrop-filter: blur(2px);
    }
    #voyager-backdrop.show { display: block; }
    #voyager-drawer {
      position: fixed;
      left: -290px;
      top: 0;
      width: 280px;
      height: 100vh;
      background: #18181b;
      color: #f4f4f5;
      z-index: 2147483647;
      box-shadow: 4px 0 24px rgba(0,0,0,0.5);
      transition: left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      flex-direction: column;
      font-family: system-ui, -apple-system, sans-serif;
    }
    #voyager-drawer.show { left: 0; }
    .v-header {
      padding: 16px;
      background: #121215;
      border-bottom: 1px solid #27272a;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .v-title { font-size: 15px; font-weight: bold; color: #10a37f; }
    .v-close { cursor: pointer; font-size: 18px; color: #a1a1aa; padding: 4px; }
    .v-content { flex: 1; overflow-y: auto; padding: 14px; }
    .v-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #71717a;
      margin-bottom: 8px;
    }
    .v-folder-item {
      padding: 10px 12px;
      margin-bottom: 6px;
      background: #27272a;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      cursor: pointer;
    }
    .v-folder-item:active { background: #3f3f46; }
    .v-btn {
      width: 100%;
      padding: 9px;
      background: #10a37f;
      color: #fff;
      border: none;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      margin-top: 8px;
    }
    .v-btn-secondary { background: #3f3f46; }
    .v-tag {
      display: inline-block;
      padding: 6px 10px;
      background: #27272a;
      border-radius: 6px;
      font-size: 12px;
      margin: 4px 4px 0 0;
      cursor: pointer;
    }
    .v-tag:active { background: #10a37f; color: #fff; }
  `;
  document.head.appendChild(style);

  // 2. 本地存储数据
  const STORAGE_KEY = 'voyager_ai_data';
  let appData = {
    folders: [
      { id: '1', name: '💼 工作与日常', chats: [] },
      { id: '2', name: '💻 代码开发与Debug', chats: [] },
      { id: '3', name: '📚 学习提炼与笔记', chats: [] }
    ],
    prompts: [
      '用中文简要提炼以上对话的核心结论：',
      '审查这段代码的潜在 Bug 并给出优化建议：',
      '请用更具条理的表格形式重新输出上述内容：'
    ]
  };

  try {
    const local = localStorage.getItem(STORAGE_KEY);
    if (local) appData = JSON.parse(local);
  } catch (e) {}

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appData));
  }

  // 3. 页面挂载 UI 结构
  const container = document.createElement('div');
  container.id = 'voyager-root';
  container.innerHTML = `
    <div id="voyager-fab-btn">📁</div>
    <div id="voyager-backdrop"></div>
    <div id="voyager-drawer">
      <div class="v-header">
        <span class="v-title">Voyager 对话增强</span>
        <span class="v-close" id="v-close">✕</span>
      </div>
      <div class="v-content">
        <div class="v-label">会话文件夹分类</div>
        <div id="v-folders"></div>
        <button class="v-btn" id="v-add-folder">+ 新建文件夹</button>

        <div class="v-label" style="margin-top:20px;">常用快捷 Prompt</div>
        <div id="v-prompts"></div>

        <div style="margin-top:20px;">
          <button class="v-btn v-btn-secondary" id="v-save-cur-chat">
            📌 将当前会话收藏至文件夹
          </button>
        </div>
      </div>
    </div>
  `;
  document.body.appendChild(container);

  const fab = document.getElementById('voyager-fab-btn');
  const drawer = document.getElementById('voyager-drawer');
  const backdrop = document.getElementById('voyager-backdrop');
  const closeBtn = document.getElementById('v-close');
  const folderBox = document.getElementById('v-folders');
  const promptBox = document.getElementById('v-prompts');

  const openDrawer = () => { drawer.classList.add('show'); backdrop.classList.add('show'); };
  const closeDrawer = () => { drawer.classList.remove('show'); backdrop.classList.remove('show'); };

  fab.onclick = openDrawer;
  closeBtn.onclick = closeDrawer;
  backdrop.onclick = closeDrawer;

  function render() {
    folderBox.innerHTML = '';
    appData.folders.forEach((f) => {
      const item = document.createElement('div');
      item.className = 'v-folder-item';
      item.innerHTML = `<span>${f.name}</span><span style="color:#71717a;font-size:11px;">(${f.chats.length})</span>`;
      item.onclick = () => {
        if (!f.chats.length) {
          alert(`文件夹 [${f.name}] 暂无记录，可点击下方按钮收藏当前页面`);
          return;
        }
        const summary = f.chats.map((c, i) => `${i + 1}. ${c.title}`).join('\\n');
        const pick = prompt(`【${f.name}】中的会话：\\n${summary}\\n\\n输入序号跳转:`);
        const idx = parseInt(pick) - 1;
        if (f.chats[idx]) window.location.href = f.chats[idx].url;
      };
      folderBox.appendChild(item);
    });

    promptBox.innerHTML = '';
    appData.prompts.forEach((p) => {
      const tag = document.createElement('span');
      tag.className = 'v-tag';
      tag.textContent = p.length > 15 ? p.substring(0, 15) + '...' : p;
      tag.onclick = () => {
        const input = document.querySelector('textarea') || 
                      document.querySelector('div[contenteditable="true"]') ||
                      document.querySelector('#prompt-textarea');
        if (input) {
          if (input.tagName === 'TEXTAREA') {
            input.value = p;
            input.dispatchEvent(new Event('input', { bubbles: true }));
          } else {
            input.innerText = p;
          }
          input.focus();
          closeDrawer();
        } else {
          alert('未找到当前网页的提问输入框');
        }
      };
      promptBox.appendChild(tag);
    });
  }

  document.getElementById('v-add-folder').onclick = () => {
    const name = prompt('请输入新文件夹名称:');
    if (name && name.trim()) {
      appData.folders.push({ id: Date.now().toString(), name: '📁 ' + name.trim(), chats: [] });
      save();
      render();
    }
  };

  document.getElementById('v-save-cur-chat').onclick = () => {
    const url = window.location.href;
    const title = document.title || '当前对话';
    const names = appData.folders.map((f, i) => `${i + 1}. ${f.name}`).join('\\n');
    const pick = prompt(`选择存入的文件夹编号：\\n${names}`);
    const idx = parseInt(pick) - 1;
    if (appData.folders[idx]) {
      appData.folders[idx].chats.push({ title, url, time: new Date().toLocaleDateString() });
      save();
      render();
      alert(`已成功将当前对话加入 [${appData.folders[idx].name}]！`);
    }
  };

  render();

  // 监听 SPA 路由变化（Next.js / Angular 单页面无刷新切换）
  const _pushState = history.pushState;
  history.pushState = function() {
    _pushState.apply(this, arguments);
    if (!document.getElementById('voyager-root')) {
      setTimeout(render, 500);
    }
  };
})();
""")

# 10. Java 实现类
pkg_dir = os.path.join(src_main, "java", "com", "voyager", "enhancer")
os.makedirs(pkg_dir, exist_ok=True)

with open(os.path.join(pkg_dir, "HookEntry.java"), "w", encoding="utf-8") as f:
    f.write("""package com.voyager.enhancer;

import android.content.Context;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "VoyagerEnhancer";

    // 目标域名白名单
    private static final String[] TARGET_DOMAINS = {
            "chatgpt.com",
            "gemini.google.com",
            "claude.ai",
            "chat.deepseek.com"
    };

    private static String cachedScript = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 全局 Hook WebViewClient 的 onPageFinished 方法
        XposedHelpers.findAndHookMethod(
                WebViewClient.class,
                "onPageFinished",
                WebView.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        WebView webView = (WebView) param.args[0];
                        String url = (String) param.args[1];

                        if (url == null || webView == null) return;

                        if (isTargetAiSite(url)) {
                            XposedBridge.log(TAG + " => 命中 AI 站点: " + url + "，执行前端增强脚本注入");
                            injectScript(webView);
                        }
                    }
                }
        );
    }

    private boolean isTargetAiSite(String url) {
        for (String domain : TARGET_DOMAINS) {
            if (url.contains(domain)) return true;
        }
        return false;
    }

    private void injectScript(WebView webView) {
        webView.post(() -> {
            try {
                // 开启基础权限
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);

                // 读取脚本
                String script = getScriptContent(webView.getContext());
                if (script != null && !script.isEmpty()) {
                    webView.evaluateJavascript(script, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            XposedBridge.log(TAG + " => 脚本注入反馈: " + value);
                        }
                    });
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + " => 注入异常: " + t.getMessage());
            }
        });
    }

    private synchronized String getScriptContent(Context context) {
        if (cachedScript != null) return cachedScript;
        try {
            // 从模块自身 assets 读取
            Context modContext = context.createPackageContext("com.voyager.enhancer", Context.CONTEXT_IGNORE_SECURITY);
            InputStream is = modContext.getAssets().open("voyager_enhancer.js");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\\n");
            }
            reader.close();
            cachedScript = sb.toString();
            return cachedScript;
        } catch (Exception e) {
            XposedBridge.log(TAG + " => 读取本地 assets 失败，使用内嵌 fallback 注入: " + e.getMessage());
            return getFallbackScript();
        }
    }

    private String getFallbackScript() {
        return "(function(){\\n" +
               "  if(window.__V_LOADED__) return; window.__V_LOADED__ = true;\\n" +
               "  var d = document.createElement('div');\\n" +
               "  d.id = 'voyager-fab-btn';\\n" +
               "  d.innerText = '📁';\\n" +
               "  d.style.cssText = 'position:fixed;left:0;top:40%;z-index:999999;background:#10a37f;color:#fff;padding:8px 10px;border-radius:0 12px 12px 0;font-size:16px;cursor:pointer;';\\n" +
               "  d.onclick = function() { alert('Voyager 移动端增强已就绪！可进入设置添加文件夹分类与常用 Prompt。'); };\\n" +
               "  document.body.appendChild(d);\\n" +
               "})();";
    }
}
""")

# 11. GitHub Actions 自动化编译流水线 (.github/workflows/build_apk.yml)
workflows_dir = os.path.join(PROJECT_DIR, ".github", "workflows")
os.makedirs(workflows_dir, exist_ok=True)
with open(os.path.join(workflows_dir, "build_apk.yml"), "w", encoding="utf-8") as f:
    f.write("""name: 自动编译 Voyager Xposed 增强模块 APK

on:
  push:
    branches: [ "**" ]
  pull_request:
    branches: [ "**" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: 检出仓库代码
        uses: actions/checkout@v4

      - name: 配置 JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: 赋予 Gradle 执行权限
        run: chmod +x gradlew || true

      - name: 云端编译 Debug APK
        run: gradle assembleDebug --stacktrace

      - name: 上传 APK 安装包至 GitHub 产物 (Artifacts)
        uses: actions/upload-artifact@v4
        with:
          name: Voyager-Web-Enhancer-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
""")

# 12. README.md
with open(os.path.join(PROJECT_DIR, "README.md"), "w", encoding="utf-8") as f:
    f.write("""# Voyager Web Enhancer (Android Xposed / LSPosed 插件)

专门针对手机端网页版 **Gemini / ChatGPT / Claude / DeepSeek** 的增强插件。
通过 Hook 系统 WebView 注入前端增强脚本，在手机网页左侧自动增加 **文件夹抽屉管理** 与 **常用 Prompt 库**。

## 🚀 如何在 GitHub 上一键免费在线编译 APK？

本项目已经配置好 **GitHub Actions**（无需安装任何本地 Android 开发环境）：

1. 打开 GitHub，点击右上角 **「New repository」** 新建一个仓库（例如 `voyager-web-enhancer`）。
2. 将本工程的所有文件（**包括隐藏的 `.github` 文件夹**）全部上传/推送到新仓库。
3. 点击仓库上方的 **「Actions」** 选项卡。
4. 你会看到名为 `自动编译 Voyager Xposed 增强模块 APK` 的任务已经在自动运行！
5. 约 1 分钟编译完毕后，点击任务详情，在下方的 **Artifacts** 直接下载编译好的 `Voyager-Web-Enhancer-APK.zip`，解压即可安装使用！

## 📱 手机端使用

1. 手机需安装 LSPosed。
2. 安装生成的 APK，打开 LSPosed，勾选启用此模块。
3. 在作用域中勾选你想增强的浏览器（如 Chrome、Edge、Via、X浏览器）或官方 App。
4. 使用该浏览器打开 `chatgpt.com`、`gemini.google.com` 或 `chat.deepseek.com`，屏幕左侧即可看到绿色文件夹悬浮球！
""")

print("Voyager Web Enhancer project created successfully!")
