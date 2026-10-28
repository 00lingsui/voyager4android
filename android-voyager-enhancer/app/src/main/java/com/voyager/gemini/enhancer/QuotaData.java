package com.voyager.gemini.enhancer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Gemini 额度与刷新时间数据实体
 */
public class QuotaData {

    private static final String PREF_NAME = "gemini_quota_prefs";

    // 短期额度 (如 5h 滚动窗口)
    public String shortTermLabel = "5h";
    public int shortTermPercent = 9; // 已使用或剩余百分比 (0-100)
    public long shortTermResetTimestamp = 0; // 下次刷新时间戳 (ms)

    // 每周额度
    public String weeklyLabel = "每周";
    public int weeklyPercent = 6;
    public long weeklyResetTimestamp = 0; // 每周刷新时间戳 (ms)

    // 账号信息
    public String tierName = "Gemini Advanced"; // Gemini 免费版 / Advanced / Pro
    public long lastUpdatedMs = 0;

    public QuotaData() {
        long now = System.currentTimeMillis();
        // 默认初始化模拟真实初始倒计时 (短期24分钟，每周4天3小时)
        this.shortTermResetTimestamp = now + (24 * 60 * 1000L); // 24m 后刷新
        this.weeklyResetTimestamp = now + (4 * 24 * 3600 * 1000L) + (3 * 3600 * 1000L); // 4d 3h 后刷新
        this.lastUpdatedMs = now;
    }

    /**
     * 计算并格式化短期重置倒计时：如 "(24m)", "(1h15m)"
     */
    public String getFormattedShortCountdown() {
        long delta = shortTermResetTimestamp - System.currentTimeMillis();
        if (delta <= 0) {
            return "(已重置)";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(delta);
        long mins = TimeUnit.MILLISECONDS.toMinutes(delta) % 60;
        if (hours > 0) {
            return "(" + hours + "h" + (mins > 0 ? mins + "m" : "") + ")";
        }
        return "(" + Math.max(1, mins) + "m)";
    }

    /**
     * 计算并格式化每周重置倒计时：如 "(4d3h)", "(18h)"
     */
    public String getFormattedWeeklyCountdown() {
        long delta = weeklyResetTimestamp - System.currentTimeMillis();
        if (delta <= 0) {
            return "(已重置)";
        }
        long days = TimeUnit.MILLISECONDS.toDays(delta);
        long hours = TimeUnit.MILLISECONDS.toHours(delta) % 24;
        long mins = TimeUnit.MILLISECONDS.toMinutes(delta) % 60;
        if (days > 0) {
            return "(" + days + "d" + (hours > 0 ? hours + "h" : "") + ")";
        }
        if (hours > 0) {
            return "(" + hours + "h" + (mins > 0 ? mins + "m" : "") + ")";
        }
        return "(" + Math.max(1, mins) + "m)";
    }

    /**
     * 保存到本地 SharedPreferences
     */
    public void saveToPrefs(Context context) {
        if (context == null) return;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit()
                    .putString("short_label", shortTermLabel)
                    .putInt("short_percent", shortTermPercent)
                    .putLong("short_reset", shortTermResetTimestamp)
                    .putString("weekly_label", weeklyLabel)
                    .putInt("weekly_percent", weeklyPercent)
                    .putLong("weekly_reset", weeklyResetTimestamp)
                    .putString("tier_name", tierName)
                    .putLong("last_updated", lastUpdatedMs)
                    .apply();
        } catch (Throwable ignored) {}
    }

    /**
     * 从本地 SharedPreferences 读取
     */
    public static QuotaData loadFromPrefs(Context context) {
        QuotaData data = new QuotaData();
        if (context == null) return data;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            data.shortTermLabel = sp.getString("short_label", "5h");
            data.shortTermPercent = sp.getInt("short_percent", 9);
            data.shortTermResetTimestamp = sp.getLong("short_reset", data.shortTermResetTimestamp);
            data.weeklyLabel = sp.getString("weekly_label", "每周");
            data.weeklyPercent = sp.getInt("weekly_percent", 6);
            data.weeklyResetTimestamp = sp.getLong("weekly_reset", data.weeklyResetTimestamp);
            data.tierName = sp.getString("tier_name", "Gemini Advanced");
            data.lastUpdatedMs = sp.getLong("last_updated", System.currentTimeMillis());
        } catch (Throwable ignored) {}
        return data;
    }
}
