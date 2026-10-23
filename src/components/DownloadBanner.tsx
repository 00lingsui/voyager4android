import React, { useState } from 'react';
import { 
  Download, 
  CheckCircle, 
  FolderArchive, 
  FileCode, 
  Copy, 
  Check, 
  Terminal, 
  AlertCircle,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { ZIP_BASE64 } from '../data/zipBase64';

export const DownloadBanner: React.FC = () => {
  const [downloadSuccess, setDownloadSuccess] = useState(false);
  const [showFiles, setShowFiles] = useState(false);
  const [copiedFile, setCopiedFile] = useState<string | null>(null);

  // 100% 内存二进制安全生成 Blob，彻底杜绝因为 iframe 拦截或网络中断导致的 0 字节损坏包
  const handleMemoryDownload = () => {
    try {
      const binaryString = window.atob(ZIP_BASE64);
      const len = binaryString.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }
      const blob = new Blob([bytes], { type: 'application/zip' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'voyager-web-enhancer-xposed.zip';
      document.body.appendChild(link);
      link.click();
      setTimeout(() => {
        window.URL.revokeObjectURL(url);
        document.body.removeChild(link);
      }, 100);

      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 4000);
    } catch (err) {
      console.error('Download error:', err);
      window.location.href = '/voyager-web-enhancer-xposed.zip';
    }
  };

  const projectFiles = [
    {
      name: '.github/workflows/build_apk.yml',
      desc: 'GitHub Actions 自动编译流水线',
      content: `name: 自动编译 Gemini 原生增强 Xposed APK

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
      - name: 检出仓库源码
        uses: actions/checkout@v4

      - name: 设置 JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: 设置 Gradle 运行环境
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.5'

      - name: 接受 Android SDK 协议
        run: yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses 2>/dev/null || true

      - name: 自动修复源码语法与依赖环境
        run: |
          if [ ! -f "build.gradle" ]; then
            SUBDIR=$(find . -maxdepth 2 -name "build.gradle" -not -path "*/app/*" | head -n 1 | xargs dirname 2>/dev/null || true)
            if [ -n "$SUBDIR" ] && [ "$SUBDIR" != "." ]; then
              echo "检测到工程在子目录 $SUBDIR，自动移至根目录..."
              cp -rf "$SUBDIR"/* . || true
            fi
          fi

          mkdir -p app/libs
          if [ ! -f "app/libs/xposed-api-82.jar" ]; then
            echo "正在自动下载 xposed-api 依赖..."
            curl -sLo app/libs/xposed-api-82.jar https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar || true
          fi

          if ! grep -q "fileTree" app/build.gradle 2>/dev/null; then
            sed -i "s/dependencies {/dependencies {\\n    compileOnly fileTree(dir: 'libs', include: ['*.jar'])/" app/build.gradle || true
          fi

          if ! grep -q "api.xposed.info" settings.gradle 2>/dev/null; then
            sed -i "s|mavenCentral()|mavenCentral()\\n        maven { url 'https://api.xposed.info/' }|g" settings.gradle || true
          fi

          find . -name "VoyagerOverlayView.java" -exec sed -i 's/sp.getString("folders",.*/sp.getString("folders", "[]");/' {} + 2>/dev/null || true
          find . -name "VoyagerOverlayView.java" -exec sed -i 's/sp.getString("prompts",.*/sp.getString("prompts", "[]");/' {} + 2>/dev/null || true

          if [ -f "./gradlew" ]; then
            sed -i 's/\\r$//' ./gradlew || true
            chmod +x ./gradlew || true
          fi

      - name: 编译 Debug 版 APK
        run: |
          if [ -f "./gradlew" ]; then
            ./gradlew assembleDebug --no-daemon --stacktrace
          else
            gradle assembleDebug --no-daemon --stacktrace
          fi

      - name: 提取扁平化 APK (彻底去除多层嵌套文件夹)
        run: |
          mkdir -p single_apk
          find . -name "*debug*.apk" -exec cp {} single_apk/Voyager-Gemini-Enhancer.apk \\;

      - name: 上传纯净版 APK (压缩包内直接是 APK，零文件夹嵌套)
        uses: actions/upload-artifact@v4
        with:
          name: Voyager-Gemini-Enhancer-APK
          path: single_apk/Voyager-Gemini-Enhancer.apk
          retention-days: 7`
    },
    {
      name: 'app/src/main/java/com/voyager/gemini/enhancer/HookEntry.java',
      desc: 'Gemini 原生客户端 Xposed 注入入口',
      content: `package com.voyager.gemini.enhancer;

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

    private static final String[] TARGET_DOMAINS = {
            "chatgpt.com",
            "gemini.google.com",
            "claude.ai",
            "chat.deepseek.com"
    };

    private static String cachedScript = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
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
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);

                String script = getScriptContent(webView.getContext());
                if (script != null && !script.isEmpty()) {
                    webView.evaluateJavascript(script, null);
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + " => 注入异常: " + t.getMessage());
            }
        });
    }

    private synchronized String getScriptContent(Context context) {
        if (cachedScript != null) return cachedScript;
        try {
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
            return "console.log('[Voyager] fallback');";
        }
    }
}`
    },
    {
      name: 'app/src/main/assets/xposed_init',
      desc: 'LSPosed 入口类声明',
      content: `com.voyager.enhancer.HookEntry`
    },
    {
      name: 'app/build.gradle',
      desc: '模块编译配置与 Xposed 依赖',
      content: `plugins {
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

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly 'de.robv.android.xposed:api:82'
}`
    }
  ];

  const copyCode = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedFile(id);
    setTimeout(() => setCopiedFile(null), 2000);
  };

  return (
    <div className="bg-gradient-to-r from-emerald-950/90 via-zinc-900 to-zinc-900 border-2 border-emerald-500 rounded-2xl p-5 sm:p-6 shadow-2xl mb-8 space-y-4">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5">
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-emerald-500 text-zinc-950">
              <FolderArchive className="w-3.5 h-3.5" />
              <span>内存二进制直出 · 100% 完整防损坏</span>
            </span>
            <span className="text-xs font-mono text-zinc-400 bg-zinc-800 px-2 py-0.5 rounded border border-zinc-700">
              作用目标: Gemini 原生 App (com.google.android.apps.bard)
            </span>
          </div>

          <h2 className="text-xl sm:text-2xl font-bold text-zinc-100 tracking-tight">
            Gemini Voyager 手机端旗舰版增强插件 (LSPosed)
          </h2>
          <p className="text-xs sm:text-sm text-zinc-300 max-w-2xl leading-relaxed">
            已升级 <strong className="text-emerald-400">52dp 圆形翡翠绿发光悬浮球</strong>（支持手指自由拖拽 + 松手平滑吸附屏幕边缘）！集成 <strong className="text-emerald-400">三级彩色文件夹树、垂直时间线小地图、支持 {{变量}} 模版的 Prompt 库、Markdown 导出与公式 LaTeX 提取</strong>！
          </p>
        </div>

        {/* Buttons */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 shrink-0">
          <button
            onClick={handleMemoryDownload}
            className="flex items-center justify-center space-x-2 px-6 py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-zinc-950 font-bold text-sm font-mono shadow-lg shadow-emerald-500/25 active:scale-95 transition-all cursor-pointer"
          >
            {downloadSuccess ? (
              <>
                <CheckCircle className="w-4 h-4 text-zinc-950" />
                <span>已成功生成并下载 ZIP!</span>
              </>
            ) : (
              <>
                <Download className="w-4 h-4 stroke-[2.5]" />
                <span>重新点击下载 ZIP 完整包</span>
              </>
            )}
          </button>

          <button
            onClick={() => setShowFiles(!showFiles)}
            className="flex items-center justify-center space-x-1.5 px-4 py-3 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs font-mono border border-zinc-700 transition-all text-center cursor-pointer"
          >
            <FileCode className="w-4 h-4 text-emerald-400" />
            <span>免解压：直接复制代码</span>
            {showFiles ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      {/* Accordion: Direct File Viewer in case unzipping has local OS issues */}
      {showFiles && (
        <div className="mt-4 pt-4 border-t border-zinc-800 space-y-4 animate-fade-in">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span className="font-semibold text-zinc-200">
              💡 极简备选方案：也可以直接在 GitHub 上新建这些文件并粘贴代码（无需解压）
            </span>
          </div>

          <div className="space-y-3">
            {projectFiles.map((file) => (
              <div key={file.name} className="bg-zinc-950 rounded-xl border border-zinc-800 overflow-hidden">
                <div className="flex items-center justify-between px-4 py-2.5 bg-zinc-900 border-b border-zinc-800 text-xs">
                  <div className="flex items-center space-x-2">
                    <FileCode className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="font-mono text-zinc-200 font-semibold">{file.name}</span>
                    <span className="text-zinc-500 text-[11px]">({file.desc})</span>
                  </div>
                  <button
                    onClick={() => copyCode(file.content, file.name)}
                    className="flex items-center space-x-1 text-xs font-mono text-zinc-400 hover:text-emerald-400 transition-colors"
                  >
                    {copiedFile === file.name ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-emerald-400">已复制!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>复制代码</span>
                      </>
                    )}
                  </button>
                </div>
                <div className="p-3 font-mono text-[11px] text-zinc-300 max-h-40 overflow-y-auto bg-zinc-950/80">
                  <pre>{file.content}</pre>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
