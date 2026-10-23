import os
import shutil

ROOT_DIR = "/android-voyager-agent"
os.makedirs(ROOT_DIR, exist_ok=True)

# 1. root settings.gradle
with open(os.path.join(ROOT_DIR, "settings.gradle"), "w", encoding="utf-8") as f:
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
rootProject.name = "VoyagerAgent"
include ':app'
""")

# 2. root build.gradle
with open(os.path.join(ROOT_DIR, "build.gradle"), "w", encoding="utf-8") as f:
    f.write("""plugins {
    id 'com.android.application' version '8.2.2' apply false
}
""")

# 3. gradle.properties
with open(os.path.join(ROOT_DIR, "gradle.properties"), "w", encoding="utf-8") as f:
    f.write("""org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
""")

# 4. gradle-wrapper.properties
wrapper_dir = os.path.join(ROOT_DIR, "gradle", "wrapper")
os.makedirs(wrapper_dir, exist_ok=True)
with open(os.path.join(wrapper_dir, "gradle-wrapper.properties"), "w", encoding="utf-8") as f:
    f.write("""distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""")

# 5. gradlew shell wrapper (simplified for CI)
with open(os.path.join(ROOT_DIR, "gradlew"), "w", encoding="utf-8") as f:
    f.write("""#!/bin/sh
gradle "$@"
""")
os.chmod(os.path.join(ROOT_DIR, "gradlew"), 0o755)

# 6. app/build.gradle
app_dir = os.path.join(ROOT_DIR, "app")
os.makedirs(app_dir, exist_ok=True)
with open(os.path.join(app_dir, "build.gradle"), "w", encoding="utf-8") as f:
    f.write("""plugins {
    id 'com.android.application'
}

android {
    namespace 'com.agent.voyager'
    compileSdk 34

    defaultConfig {
        applicationId "com.agent.voyager"
        minSdk 26
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
    // Xposed API (必须 compileOnly)
    compileOnly 'de.robv.android.xposed:api:82'
    // 内置轻量 HTTP Server
    implementation 'org.nanohttpd:nanohttpd:2.3.1'
}
""")

# 7. app/src/main/AndroidManifest.xml
src_main = os.path.join(app_dir, "src", "main")
os.makedirs(src_main, exist_ok=True)
with open(os.path.join(src_main, "AndroidManifest.xml"), "w", encoding="utf-8") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="VoyagerAgent"
        android:supportsRtl="true">

        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposeddescription"
            android:value="Voyager LLM In-Process Automation Agent" />
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
    f.write("com.agent.voyager.HookEntry\n")

# 9. java source files
pkg_dir = os.path.join(src_main, "java", "com", "agent", "voyager")
os.makedirs(pkg_dir, exist_ok=True)

with open(os.path.join(pkg_dir, "HookEntry.java"), "w", encoding="utf-8") as f:
    f.write("""package com.agent.voyager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "VoyagerAgent";
    // 目标 App 包名（可以在编译前修改为您需要控制的 App）
    public static final String TARGET_PACKAGE = "com.example.targetapp";
    public static final int AGENT_PORT = 28888; // 选用高位独占端口，避免与 8080 冲突

    public static Context appContext = null;
    public static Activity currentActivity = null;
    private static AgentHttpServer server = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 过滤非目标应用
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + " => 成功注入目标应用: " + lpparam.packageName);

        // Hook Application.onCreate，获取 Context 并启动后台通信 Server
        XposedHelpers.findAndHookMethod(
                Application.class,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        appContext = (Application) param.thisObject;
                        XposedBridge.log(TAG + " => 宿主 Application 初始化完成，正在启动 Agent 服务");

                        // 启动进程内 HTTP 服务 (28888 独占端口)
                        startAgentServer(AGENT_PORT);

                        // 追踪可见 Activity
                        ((Application) appContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                            @Override
                            public void onActivityResumed(Activity activity) {
                                currentActivity = activity;
                                XposedBridge.log(TAG + " => 当前前台 Activity: " + activity.getClass().getName());
                            }
                            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
                            @Override public void onActivityStarted(Activity activity) {}
                            @Override public void onActivityPaused(Activity activity) {}
                            @Override public void onActivityStopped(Activity activity) {}
                            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
                            @Override
                            public void onActivityDestroyed(Activity activity) {
                                if (currentActivity == activity) currentActivity = null;
                            }
                        });
                    }
                }
        );

        // 示例：全局拦截 View.performClick
        XposedHelpers.findAndHookMethod(
                View.class,
                "performClick",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View v = (View) param.thisObject;
                        XposedBridge.log(TAG + " => [UI事件] performClick, View ID: " + v.getId());
                    }
                }
        );
    }

    private static synchronized void startAgentServer(int port) {
        if (server != null) return;
        try {
            server = new AgentHttpServer(port);
            server.start();
            XposedBridge.log(TAG + " => AgentHttpServer 启动成功: http://127.0.0.1:" + port);
        } catch (Exception e) {
            XposedBridge.log(TAG + " => AgentHttpServer 启动异常: " + e.getMessage());
        }
    }
}
""")

with open(os.path.join(pkg_dir, "AgentHttpServer.java"), "w", encoding="utf-8") as f:
    f.write("""package com.agent.voyager;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AgentHttpServer extends NanoHTTPD {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AgentHttpServer(int port) {
        super("127.0.0.1", port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // 1. 状态探测接口
            if ("/status".equals(uri)) {
                JSONObject res = new JSONObject();
                res.put("status", "ok");
                res.put("package", HookEntry.TARGET_PACKAGE);
                res.put("currentActivity", HookEntry.currentActivity != null ?
                        HookEntry.currentActivity.getClass().getName() : "None");
                return newFixedLengthResponse(Response.Status.OK, "application/json", res.toString());
            }

            // 2. 环境感知接口：Dump 界面 View 树文本
            if ("/dump".equals(uri)) {
                JSONObject result = new JSONObject();
                if (HookEntry.currentActivity != null) {
                    View decorView = HookEntry.currentActivity.getWindow().getDecorView();
                    JSONArray viewsArray = new JSONArray();
                    traverseViews(decorView, viewsArray);
                    result.put("activity", HookEntry.currentActivity.getClass().getSimpleName());
                    result.put("views", viewsArray);
                } else {
                    result.put("error", "No active activity");
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());
            }

            // 3. 执行动作接口
            if ("/action".equals(uri) && Method.POST.equals(method)) {
                Map<String, String> body = new HashMap<>();
                session.parseBody(body);
                String postData = body.get("postData");
                JSONObject req = new JSONObject(postData != null ? postData : "{}");

                String action = req.optString("action");
                String text = req.optString("text");

                if ("toast".equals(action)) {
                    mainHandler.post(() -> {
                        if (HookEntry.appContext != null) {
                            Toast.makeText(HookEntry.appContext, "[Agent] " + text, Toast.LENGTH_SHORT).show();
                        }
                    });
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "{\\"success\\":true}");
                }

                if ("click".equals(action)) {
                    final boolean[] clicked = {false};
                    mainHandler.post(() -> {
                        if (HookEntry.currentActivity != null) {
                            View decor = HookEntry.currentActivity.getWindow().getDecorView();
                            clicked[0] = findAndClickText(decor, text);
                        }
                    });
                    Thread.sleep(150);
                    JSONObject resp = new JSONObject();
                    resp.put("action", "click");
                    resp.put("target", text);
                    resp.put("success", clicked[0]);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", resp.toString());
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
        }
    }

    private void traverseViews(View view, JSONArray array) {
        if (view == null || view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.length() > 0) {
                JSONObject item = new JSONObject();
                try {
                    item.put("type", view.getClass().getSimpleName());
                    item.put("text", text.toString());
                    item.put("clickable", view.isClickable());
                    array.put(item);
                } catch (Exception ignored) {}
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseViews(group.getChildAt(i), array);
            }
        }
    }

    private boolean findAndClickText(View view, String target) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().contains(target)) {
                View clickableTarget = view.isClickable() ? view : (View) view.getParent();
                if (clickableTarget != null) return clickableTarget.performClick();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (findAndClickText(group.getChildAt(i), target)) return true;
            }
        }
        return false;
    }
}
""")

# 10. .github/workflows/build_apk.yml (GitHub Actions 自动化编译脚本)
workflow_dir = os.path.join(ROOT_DIR, ".github", "workflows")
os.makedirs(workflow_dir, exist_ok=True)
with open(os.path.join(workflow_dir, "build_apk.yml"), "w", encoding="utf-8") as f:
    f.write("""name: 自动编译 Xposed 模块 APK

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
      - name: 检出代码仓库
        uses: actions/checkout@v4

      - name: 设置 JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: 赋予 Gradle 执行权限
        run: chmod +x gradlew || true

      - name: 编译 Debug 版 APK
        run: |
          if [ -f "./gradlew" ] && grep -q "gradle" ./gradlew; then
            gradle assembleDebug --stacktrace
          else
            gradle assembleDebug --stacktrace
          fi

      - name: 上传 APK 产物到 GitHub Artifacts (供直接下载)
        uses: actions/upload-artifact@v4
        with:
          name: VoyagerAgent-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
""")

# 11. README.md
with open(os.path.join(ROOT_DIR, "README.md"), "w", encoding="utf-8") as f:
    f.write("""# Voyager-Android LSPosed Agent

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
""")

# 12. test_agent.py
with open(os.path.join(ROOT_DIR, "test_agent.py"), "w", encoding="utf-8") as f:
    f.write("""import json
import urllib.request

BASE_URL = "http://127.0.0.1:28888"

def get_status():
    try:
        with urllib.request.urlopen(f"{BASE_URL}/status") as resp:
            print("当前状态:", resp.read().decode())
    except Exception as e:
        print("连接失败，请确认是否执行了 'adb forward tcp:28888 tcp:28888':", e)

def send_toast(msg):
    req = urllib.request.Request(
        f"{BASE_URL}/action",
        data=json.dumps({"action": "toast", "text": msg}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        print("Toast 结果:", resp.read().decode())

def click_button(btn_text):
    req = urllib.request.Request(
        f"{BASE_URL}/action",
        data=json.dumps({"action": "click", "text": btn_text}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        print("点击结果:", resp.read().decode())

if __name__ == "__main__":
    print("1. 正在检查宿主状态...")
    get_status()
    print("2. 发送测试 Toast...")
    send_toast("Hello from Voyager Agent!")
""")

# 13. Package into public/voyager-android-agent.zip
public_dir = "/public"
os.makedirs(public_dir, exist_ok=True)
zip_output = os.path.join(public_dir, "voyager-android-agent")
shutil.make_archive(zip_output, "zip", ROOT_DIR)

print("Project generated and zipped successfully!")
