package com.agent.voyager;

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
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
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
