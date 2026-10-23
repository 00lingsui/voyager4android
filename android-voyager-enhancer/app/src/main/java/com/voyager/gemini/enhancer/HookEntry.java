package com.voyager.gemini.enhancer;

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
