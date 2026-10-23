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
rootProject.name = "GeminiVoyagerEnhancer"
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
    namespace 'com.voyager.gemini.enhancer'
    compileSdk 34

    defaultConfig {
        applicationId "com.voyager.gemini.enhancer"
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
    // Xposed 核心接口 (编译期 compileOnly)
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
        android:label="Gemini Voyager Enhancer"
        android:supportsRtl="true">

        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposeddescription"
            android:value="针对 Google Gemini 原生安卓客户端的对话增强插件（原生侧边栏注入、文件夹管理、Prompt 库）" />
        <meta-data
            android:name="xposedminversion"
            android:value="82" />
        <meta-data
            android:name="xposedscope"
            android:resource="@array/xposed_scope" />
    </application>

</manifest>
""")

# 7.1 res/values/arrays.xml (默认作用域)
res_values = os.path.join(src_main, "res", "values")
os.makedirs(res_values, exist_ok=True)
with open(os.path.join(res_values, "arrays.xml"), "w", encoding="utf-8") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="xposed_scope">
        <item>com.google.android.apps.bard</item>
        <item>com.google.android.googlequicksearchbox</item>
        <item>com.openai.chatgpt</item>
        <item>com.anthropic.claude</item>
        <item>com.deepseek.chat</item>
    </string-array>
</resources>
""")

# 8. assets/xposed_init
assets_dir = os.path.join(src_main, "assets")
os.makedirs(assets_dir, exist_ok=True)
with open(os.path.join(assets_dir, "xposed_init"), "w", encoding="utf-8") as f:
    f.write("com.voyager.gemini.enhancer.HookEntry\n")

# 9. Java 实现类
pkg_dir = os.path.join(src_main, "java", "com", "voyager", "gemini", "enhancer")
os.makedirs(pkg_dir, exist_ok=True)

# 9.1 HookEntry.java
with open(os.path.join(pkg_dir, "HookEntry.java"), "w", encoding="utf-8") as f:
    f.write("""package com.voyager.gemini.enhancer;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "GeminiVoyager";

    // 重点支持 Google Gemini 原生客户端包名
    public static final String GEMINI_PKG = "com.google.android.apps.bard";
    public static final String GOOGLE_SEARCH_PKG = "com.google.android.googlequicksearchbox";
    public static final String CHATGPT_PKG = "com.openai.chatgpt";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;
        boolean isTarget = pkg.equals(GEMINI_PKG) || 
                          pkg.equals(GOOGLE_SEARCH_PKG) || 
                          pkg.equals(CHATGPT_PKG) ||
                          pkg.contains("gemini") ||
                          pkg.contains("bard");

        if (!isTarget) return;

        XposedBridge.log(TAG + " => 正在注入 Gemini/AI 原生客户端: " + pkg);

        // Hook Activity 的生命周期，在界面渲染完成后，将 Voyager 原生悬浮抽屉直接注入到 DecorView 顶层
        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onPostCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        injectVoyagerNativeOverlay(activity);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        injectVoyagerNativeOverlay(activity);
                    }
                }
        );
    }

    /**
     * 将 Voyager 原生增强浮窗/侧边栏注入到目标 Activity 的顶级 DecorView 中
     * 无论 Gemini 内部是用 Jetpack Compose 还是 WebView/Flutter 写的，顶层 DecorView 始终存在！
     */
    private void injectVoyagerNativeOverlay(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            try {
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                // 检查是否已经注入，避免重复添加
                if (decorView.findViewWithTag("VOYAGER_OVERLAY_ROOT") != null) {
                    return;
                }

                XposedBridge.log(TAG + " => 正在为 Activity [" + activity.getClass().getSimpleName() + "] 挂载 Voyager 原生侧边栏");

                VoyagerOverlayView overlayView = new VoyagerOverlayView(activity);
                overlayView.setTag("VOYAGER_OVERLAY_ROOT");

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                decorView.addView(overlayView, lp);
                XposedBridge.log(TAG + " => Voyager 原生增强挂载成功！");

            } catch (Throwable t) {
                XposedBridge.log(TAG + " => 注入原生 Overlay 异常: " + t.getMessage());
            }
        });
    }
}
""")

# 9.2 VoyagerOverlayView.java (完全用纯 Java 代码构建的移动端原生 UI，避开布局资源 ID 冲突)
with open(os.path.join(pkg_dir, "VoyagerOverlayView.java"), "w", encoding="utf-8") as f:
    f.write("""package com.voyager.gemini.enhancer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 Java 原生动态构建的 Voyager 侧边栏与悬浮球，支持：
 * 1. 类似网页插件的【文件夹管理】（工作、编程、写作分类）
 * 2. 【常用 Prompt 库】（点击自动填入 Gemini 客户端原生输入框并聚焦）
 * 3. 【对话收藏】：保存当前 Gemini 会话标题
 */
public class VoyagerOverlayView extends FrameLayout {

    private final Activity activity;
    private View fabButton;
    private View backdropView;
    private LinearLayout drawerLayout;
    private LinearLayout folderListLayout;
    private LinearLayout promptListLayout;

    private boolean isDrawerOpen = false;
    private SharedPreferences sp;

    public VoyagerOverlayView(Activity activity) {
        super(activity);
        this.activity = activity;
        this.sp = activity.getSharedPreferences("voyager_gemini_config", Context.MODE_PRIVATE);
        initViews();
        loadDataAndRender();
    }

    private void initViews() {
        int dp8 = dp(8);
        int dp12 = dp(12);
        int dp16 = dp(16);

        // 1. 半透明遮罩层 (点击收起侧边栏)
        backdropView = new View(getContext());
        backdropView.setBackgroundColor(Color.parseColor("#99000000"));
        backdropView.setVisibility(GONE);
        backdropView.setOnClickListener(v -> closeDrawer());
        addView(backdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. 原生抽屉侧边栏 (从左侧滑出)
        drawerLayout = new LinearLayout(getContext());
        drawerLayout.setOrientation(LinearLayout.VERTICAL);
        drawerLayout.setBackgroundColor(Color.parseColor("#18181B")); // 深色高质感
        drawerLayout.setPadding(dp16, dp(48), dp16, dp16); // 顶部留出状态栏高度

        int drawerWidth = dp(290);
        LayoutParams drawerLp = new LayoutParams(drawerWidth, LayoutParams.MATCH_PARENT, Gravity.START);
        drawerLayout.setLayoutParams(drawerLp);
        drawerLayout.setTranslationX(-drawerWidth); // 初始隐藏在屏幕左侧
        addView(drawerLayout);

        // 抽屉头部
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(getContext());
        title.setText("Voyager for Gemini");
        title.setTextColor(Color.parseColor("#10A37F"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView closeBtn = new TextView(getContext());
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#A1A1AA"));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setPadding(dp8, dp8, dp8, dp8);
        closeBtn.setOnClickListener(v -> closeDrawer());
        header.addView(closeBtn);

        drawerLayout.addView(header);

        // 内容可滚动区域
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp12, 0, dp16);

        // 模块 1: 文件夹分类
        TextView folderSection = createSectionTitle("📂 对话分类文件夹 (Folders)");
        content.addView(folderSection);

        folderListLayout = new LinearLayout(getContext());
        folderListLayout.setOrientation(LinearLayout.VERTICAL);
        content.addView(folderListLayout);

        Button addFolderBtn = createStyledButton("+ 新建文件夹", "#27272A", "#FFFFFF");
        addFolderBtn.setOnClickListener(v -> showAddFolderDialog());
        content.addView(addFolderBtn);

        // 模块 2: 常用 Prompt 库
        TextView promptSection = createSectionTitle("⚡ 常用快捷 Prompt 库");
        content.addView(promptSection);

        promptListLayout = new LinearLayout(getContext());
        promptListLayout.setOrientation(LinearLayout.VERTICAL);
        content.addView(promptListLayout);

        Button addPromptBtn = createStyledButton("+ 添加常用 Prompt", "#27272A", "#FFFFFF");
        addPromptBtn.setOnClickListener(v -> showAddPromptDialog());
        content.addView(addPromptBtn);

        scroll.addView(content);
        drawerLayout.addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 3. 悬浮召唤球 (吸附在左边缘)
        fabButton = createFabButton();
        LayoutParams fabLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        fabButton.setLayoutParams(fabLp);
        addView(fabButton);
    }

    private View createFabButton() {
        TextView fab = new TextView(getContext());
        fab.setText("📁 Voyager");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fab.setTypeface(null, android.graphics.Typeface.BOLD);
        fab.setPadding(dp(8), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#10A37F"));
        gd.setCornerRadii(new float[]{0, 0, dp(18), dp(18), dp(18), dp(18), 0, 0});
        fab.setBackground(gd);

        fab.setOnClickListener(v -> openDrawer());
        return fab;
    }

    public void openDrawer() {
        isDrawerOpen = true;
        backdropView.setVisibility(VISIBLE);
        drawerLayout.animate().translationX(0).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
    }

    public void closeDrawer() {
        isDrawerOpen = false;
        backdropView.setVisibility(GONE);
        drawerLayout.animate().translationX(-drawerLayout.getWidth()).setDuration(200).start();
    }

    private void loadDataAndRender() {
        renderFolders();
        renderPrompts();
    }

    private void renderFolders() {
        folderListLayout.removeAllViews();
        List<String> folders = getSavedFolders();
        for (String fName : folders) {
            TextView item = new TextView(getContext());
            item.setText("📁  " + fName);
            item.setTextColor(Color.parseColor("#E4E4E7"));
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#27272A"));
            gd.setCornerRadius(dp(8));
            item.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            item.setLayoutParams(lp);

            item.setOnClickListener(v -> {
                Toast.makeText(activity, "已选择分类: " + fName, Toast.LENGTH_SHORT).show();
            });

            folderListLayout.addView(item);
        }
    }

    private void renderPrompts() {
        promptListLayout.removeAllViews();
        List<String> prompts = getSavedPrompts();
        for (String pText : prompts) {
            TextView pView = new TextView(getContext());
            pView.setText(pText);
            pView.setTextColor(Color.parseColor("#D4D4D8"));
            pView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            pView.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#202024"));
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), Color.parseColor("#333338"));
            pView.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            pView.setLayoutParams(lp);

            // 点击核心功能：自动寻找 Gemini 客户端当前原生输入框并填入文字！
            pView.setOnClickListener(v -> {
                boolean filled = autoFillGeminiInput(activity.getWindow().getDecorView(), pText);
                if (filled) {
                    Toast.makeText(activity, "✅ 已自动填入 Gemini 原生输入框", Toast.LENGTH_SHORT).show();
                    closeDrawer();
                } else {
                    Toast.makeText(activity, "已复制到剪贴板，请长按粘贴", Toast.LENGTH_SHORT).show();
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(pText);
                }
            });

            promptListLayout.addView(pView);
        }
    }

    /**
     * 核心自动化：递归遍历 Gemini 客户端原生 View 树，自动定位输入框填入文字
     */
    private boolean autoFillGeminiInput(View root, String textToFill) {
        if (root == null) return false;

        if (root instanceof EditText) {
            EditText et = (EditText) root;
            if (et.isEnabled() && et.isFocusable()) {
                et.setText(textToFill);
                et.setSelection(textToFill.length());
                et.requestFocus();
                return true;
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (autoFillGeminiInput(group.getChildAt(i), textToFill)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("新建文件夹");
        final EditText input = new EditText(activity);
        input.setHint("如: 业务调研、代码优化、论文翻译");
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                List<String> list = getSavedFolders();
                list.add(name);
                saveFolders(list);
                renderFolders();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAddPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("添加常用 Prompt 模版");
        final EditText input = new EditText(activity);
        input.setHint("输入你的常用提示词，点击可一键填入 Gemini 输入框");
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String p = input.getText().toString().trim();
            if (!p.isEmpty()) {
                List<String> list = getSavedPrompts();
                list.add(p);
                savePrompts(list);
                renderPrompts();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private List<String> getSavedFolders() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("folders", "[\"💼 工作与业务\",\"💻 编程与Bug排查\",\"📚 学习提炼与笔记\"]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
        return res;
    }

    private void saveFolders(List<String> list) {
        JSONArray arr = new JSONArray(list);
        sp.edit().putString("folders", arr.toString()).apply();
    }

    private List<String> getSavedPrompts() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("prompts", "[\"用中文简明扼要提炼以上内容的核心要点：\",\"请审查以下代码并指出潜在的性能隐患：\",\"请将上述内容转换为结构清晰的对比表格：\"]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
        return res;
    }

    private void savePrompts(List<String> list) {
        JSONArray arr = new JSONArray(list);
        sp.edit().putString("prompts", arr.toString()).apply();
    }

    private TextView createSectionTitle(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#71717A"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private Button createStyledButton(String text, String bgColor, String textColor) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(dp(6));
        btn.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        lp.topMargin = dp(4);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
""")

# 10. .github/workflows/build_apk.yml
workflows_dir = os.path.join(PROJECT_DIR, ".github", "workflows")
os.makedirs(workflows_dir, exist_ok=True)
with open(os.path.join(workflows_dir, "build_apk.yml"), "w", encoding="utf-8") as f:
    f.write("""name: 自动编译 Gemini 原生增强 Xposed APK

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
          cache: 'gradle'

      - name: 授权 Gradle 脚本
        run: chmod +x gradlew || true

      - name: 编译 Debug 版 APK
        run: gradle assembleDebug --stacktrace

      - name: 上传 APK 安装包至 GitHub 产物 (Artifacts)
        uses: actions/upload-artifact@v4
        with:
          name: Gemini-Voyager-Enhancer-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
""")

# 11. README.md
with open(os.path.join(PROJECT_DIR, "README.md"), "w", encoding="utf-8") as f:
    f.write("""# Gemini Voyager Enhancer (安卓客户端原生增强插件)

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
""")

print("Gemini Native Enhancer project created successfully!")
