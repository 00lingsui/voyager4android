package com.agent.voyager;

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
