package com.voyager.gemini.enhancer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

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

        // 1. Hook Activity 的生命周期，注入 QuotaMonitorBar (顶部胶囊额度条) 与 悬浮球抽屉
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

        XposedHelpers.findAndHookMethod(
                Activity.class,
                "onDestroy",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        FloatingQuotaView.dismiss();
                    }
                }
        );

        // 2. Hook 网络层 HttpURLConnection，实时捕获 RateLimit / Quota 响应头
        try {
            XposedHelpers.findAndHookMethod(
                    HttpURLConnection.class,
                    "getHeaderFields",
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            HttpURLConnection conn = (HttpURLConnection) param.thisObject;
                            if (conn == null || conn.getURL() == null) return;
                            String url = conn.getURL().toString();
                            if (url.contains("google") || url.contains("bard") || url.contains("gemini")) {
                                Map<String, List<String>> headers = (Map<String, List<String>>) param.getResult();
                                if (headers != null) {
                                    QuotaParser.getInstance(null).parseHttpHeaders(headers, null);
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " => Hook HttpURLConnection 提示: " + t.getMessage());
        }

        // 3. Hook WebViewClient 注入前端额度侦测与数据反查
        try {
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
                            if (url != null && (url.contains("gemini.google.com") || url.contains("bard.google.com"))) {
                                // 注入前端轻量提取脚本
                                String jsProbe = "(function() {" +
                                        "  try {" +
                                        "    var info = window.WIZ_global_data || {};" +
                                        "    console.log('[VoyagerQuota] Probe finished');" +
                                        "  } catch(e) {}" +
                                        "})();";
                                webView.post(() -> webView.evaluateJavascript(jsProbe, null));
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " => Hook WebViewClient 提示: " + t.getMessage());
        }
    }

    /**
     * 将 Voyager 原生增强浮窗/侧边栏与 QuotaMonitorBar 注入到目标 Activity 的顶级 DecorView 中
     */
    private void injectVoyagerNativeOverlay(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            try {
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

                // 1. 首选使用 WindowManager 挂载独立顶部悬浮胶囊 (FloatingQuotaView)
                FloatingQuotaView floatingQuota = FloatingQuotaView.show(activity);
                if (floatingQuota == null && decorView.findViewWithTag("GEMINI_QUOTA_BAR_TAG") == null) {
                    XposedBridge.log(TAG + " => 正在挂载 Gemini 额度与刷新监控条 (QuotaMonitorBar DecorView 模式)");
                    QuotaMonitorBar quotaBar = new QuotaMonitorBar(activity);
                    quotaBar.setTag("GEMINI_QUOTA_BAR_TAG");

                    int statusBarHeight = getStatusBarHeight(activity);
                    FrameLayout.LayoutParams quotaLp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    quotaLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    quotaLp.topMargin = statusBarHeight + dp(activity, 8); // 避开状态栏

                    decorView.addView(quotaBar, quotaLp);
                }

                // 2. 注入 Voyager 原生抽屉与悬浮球 (VoyagerOverlayView)
                if (decorView.findViewWithTag("VOYAGER_OVERLAY_ROOT") == null) {
                    XposedBridge.log(TAG + " => 正在为 Activity [" + activity.getClass().getSimpleName() + "] 挂载 Voyager 原生侧边栏");

                    VoyagerOverlayView overlayView = new VoyagerOverlayView(activity);
                    overlayView.setTag("VOYAGER_OVERLAY_ROOT");

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    decorView.addView(overlayView, lp);
                    XposedBridge.log(TAG + " => Voyager 原生增强挂载成功！");
                }

            } catch (Throwable t) {
                XposedBridge.log(TAG + " => 注入原生 Overlay 异常: " + t.getMessage());
            }
        });
    }

    private static int getStatusBarHeight(Activity activity) {
        int result = 0;
        try {
            int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = activity.getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Throwable ignored) {}
        return result > 0 ? result : dp(activity, 24);
    }

    private static int dp(Activity activity, int val) {
        return (int) (val * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
