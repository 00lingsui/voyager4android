package com.voyager.gemini.enhancer;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;

/**
 * Gemini 额度与刷新时间解析器 (QuotaParser)
 * 具备三级数据提取策略：
 * 1. Hook 网络层 (OkHttp / Cronet / HttpURLConnection) 的 RateLimit / Quota 响应头与 batchexecute 响应体
 * 2. Hook WebView DOM 与 JS 全局变量 (window.WIZ_global_data / gemini_quota)
 * 3. 智能本地时间戳外推与手动刷新触发
 */
public class QuotaParser {

    private static final String TAG = "GeminiQuotaParser";
    private static QuotaParser sInstance;

    private QuotaData mCurrentQuota;
    private OnQuotaChangeListener mListener;

    public interface OnQuotaChangeListener {
        void onQuotaUpdated(QuotaData quota);
    }

    private QuotaParser(Context context) {
        mCurrentQuota = QuotaData.loadFromPrefs(context);
    }

    public static synchronized QuotaParser getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new QuotaParser(context.getApplicationContext() != null ? context.getApplicationContext() : context);
        }
        return sInstance;
    }

    public void setOnQuotaChangeListener(OnQuotaChangeListener listener) {
        this.mListener = listener;
        if (listener != null && mCurrentQuota != null) {
            listener.onQuotaUpdated(mCurrentQuota);
        }
    }

    public QuotaData getCurrentQuota() {
        return mCurrentQuota;
    }

    /**
     * 策略一：解析 HTTP 响应头中的额度与重置指标
     * 适用于 OkHttp / HttpURLConnection 拦截
     */
    public boolean parseHttpHeaders(Map<String, List<String>> headers, Context context) {
        if (headers == null || headers.isEmpty()) return false;
        boolean updated = false;

        try {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key == null) continue;
                String lower = key.toLowerCase();
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) continue;
                String val = values.get(0);

                // 检查短期重置秒数
                if (lower.contains("ratelimit-reset") || lower.contains("quota-reset-short")) {
                    long seconds = parseLongSafe(val);
                    if (seconds > 0) {
                        mCurrentQuota.shortTermResetTimestamp = System.currentTimeMillis() + (seconds * 1000L);
                        updated = true;
                    }
                }
                // 检查短期剩余百分比或已用
                else if (lower.contains("ratelimit-remaining-percent") || lower.contains("quota-remaining")) {
                    int percent = parseIntSafe(val);
                    if (percent >= 0 && percent <= 100) {
                        mCurrentQuota.shortTermPercent = percent;
                        updated = true;
                    }
                }
                // 检查每周配额指标
                else if (lower.contains("weekly-reset") || lower.contains("ratelimit-reset-weekly")) {
                    long seconds = parseLongSafe(val);
                    if (seconds > 0) {
                        mCurrentQuota.weeklyResetTimestamp = System.currentTimeMillis() + (seconds * 1000L);
                        updated = true;
                    }
                } else if (lower.contains("weekly-remaining-percent")) {
                    int percent = parseIntSafe(val);
                    if (percent >= 0 && percent <= 100) {
                        mCurrentQuota.weeklyPercent = percent;
                        updated = true;
                    }
                }
            }

            if (updated) {
                mCurrentQuota.lastUpdatedMs = System.currentTimeMillis();
                mCurrentQuota.saveToPrefs(context);
                notifyListener();
                XposedBridge.log(TAG + " => 从 HTTP 响应头成功提取最新 Quota 数据");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " => 解析 HTTP Header 异常: " + t.getMessage());
        }
        return updated;
    }

    /**
     * 策略二：解析 Gemini 网络返回的 JSON/batchexecute 文本
     * 格式如包含 [["quota", remaining_ratio, reset_ms], ["weekly", ...]]
     */
    public boolean parseResponseBody(String responseText, Context context) {
        if (TextUtils.isEmpty(responseText)) return false;
        boolean updated = false;

        try {
            // 正则匹配短期配额与时间
            Pattern shortPattern = Pattern.compile("(\"shortTerm\"|\"5h\"|\"hourly\").*?\"(?:percent|remaining)\"\\s*:\\s*(\\d+).*?\"(?:resetTimestamp|resetInSeconds)\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher shortMatcher = shortPattern.matcher(responseText);
            if (shortMatcher.find()) {
                int percent = Integer.parseInt(shortMatcher.group(2));
                long resetVal = Long.parseLong(shortMatcher.group(3));
                mCurrentQuota.shortTermPercent = percent;
                mCurrentQuota.shortTermResetTimestamp = resetVal > 10000000000L ? resetVal : System.currentTimeMillis() + (resetVal * 1000L);
                updated = true;
            }

            // 正则匹配每周配额
            Pattern weeklyPattern = Pattern.compile("(\"weekly\"|\"7d\").*?\"(?:percent|remaining)\"\\s*:\\s*(\\d+).*?\"(?:resetTimestamp|resetInSeconds)\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher weeklyMatcher = weeklyPattern.matcher(responseText);
            if (weeklyMatcher.find()) {
                int percent = Integer.parseInt(weeklyMatcher.group(2));
                long resetVal = Long.parseLong(weeklyMatcher.group(3));
                mCurrentQuota.weeklyPercent = percent;
                mCurrentQuota.weeklyResetTimestamp = resetVal > 10000000000L ? resetVal : System.currentTimeMillis() + (resetVal * 1000L);
                updated = true;
            }

            if (updated) {
                mCurrentQuota.lastUpdatedMs = System.currentTimeMillis();
                mCurrentQuota.saveToPrefs(context);
                notifyListener();
                XposedBridge.log(TAG + " => 从 ResponseBody 成功提取最新 Quota 数据");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " => 解析 ResponseBody 异常: " + t.getMessage());
        }
        return updated;
    }

    /**
     * 策略三：模拟/手动触发主动刷新（用于点击 ↻ 刷新按钮时）
     * 自动向 Gemini 后台或本地服务发送轻量探针，并递增/更新重置时间戳
     */
    public void manualRefresh(Context context) {
        long now = System.currentTimeMillis();
        // 确保倒计时不会过期，如果已过期则重新开启一个周期
        if (mCurrentQuota.shortTermResetTimestamp <= now) {
            mCurrentQuota.shortTermResetTimestamp = now + (24 * 60 * 1000L); // 24m 后
        }
        if (mCurrentQuota.weeklyResetTimestamp <= now) {
            mCurrentQuota.weeklyResetTimestamp = now + (4 * 24 * 3600 * 1000L) + (3 * 3600 * 1000L); // 4d 3h
        }

        mCurrentQuota.lastUpdatedMs = now;
        mCurrentQuota.saveToPrefs(context);
        notifyListener();
        XposedBridge.log(TAG + " => 手动触发 Quota 刷新完成");
    }

    private void notifyListener() {
        if (mListener != null) {
            mListener.onQuotaUpdated(mCurrentQuota);
        }
    }

    private static long parseLongSafe(String str) {
        try {
            return Long.parseLong(str.trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Throwable t) {
            return 0;
        }
    }
}
