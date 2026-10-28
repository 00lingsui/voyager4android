package com.voyager.gemini.enhancer;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Gemini 额度与刷新时间监控 UI（StatusBar/Bar 胶囊样式）
 * 横向单行排列 (Row)：
 * 【5h 🟩 9% (24m)】 · 【每周 🟩 6% (4d3h)】 | [↻] [🗗]
 */
public class QuotaMonitorBar extends LinearLayout implements QuotaParser.OnQuotaChangeListener {

    // 颜色配置
    private static final int COLOR_BG = Color.parseColor("#141418");
    private static final int COLOR_BORDER = Color.parseColor("#272732");
    private static final int COLOR_GREEN = Color.parseColor("#10B981");
    private static final int COLOR_YELLOW = Color.parseColor("#F59E0B");
    private static final int COLOR_RED = Color.parseColor("#EF4444");
    private static final int COLOR_TEXT_MAIN = Color.parseColor("#E4E4E7");
    private static final int COLOR_TEXT_MUTED = Color.parseColor("#9CA3AF");
    private static final int COLOR_TRACK_BG = Color.parseColor("#27272A");

    private QuotaData mQuotaData;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimerRunnable;

    // UI 组件引用
    private TextView mTvShortLabel;
    private View mViewShortDot;
    private View mViewShortTrackFill;
    private TextView mTvShortPercent;
    private TextView mTvShortCountdown;

    private TextView mTvWeeklyLabel;
    private View mViewWeeklyDot;
    private View mViewWeeklyTrackFill;
    private TextView mTvWeeklyPercent;
    private TextView mTvWeeklyCountdown;

    private TextView mBtnRefresh;
    private TextView mBtnExpand;

    // 拖拽手势
    private float mDownX, mDownY;
    private float mLastRawX, mLastRawY;
    private boolean mIsDragging = false;

    public QuotaMonitorBar(Context context) {
        this(context, null);
    }

    public QuotaMonitorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);

        // 1. 设置胶囊外观 (圆角扁平顶部悬浮条)
        int paddingHoriz = dp(12);
        int paddingVert = dp(6);
        setPadding(paddingHoriz, paddingVert, paddingHoriz, paddingVert);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_BG);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), COLOR_BORDER);
        setBackground(bg);
        setElevation(dp(8));

        // 2. 构建【短期额度 (5h)】部分
        buildShortTermSection(context);

        // 3. 构建中隔圆点
        TextView dotDivider = new TextView(context);
        dotDivider.setText("·");
        dotDivider.setTextColor(Color.parseColor("#4B5563"));
        dotDivider.setTextSize(12);
        dotDivider.setPadding(dp(6), 0, dp(6), 0);
        addView(dotDivider);

        // 4. 构建【每周额度】部分
        buildWeeklySection(context);

        // 5. 构建右侧分割线
        View vertLine = new View(context);
        vertLine.setBackgroundColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(1), dp(14));
        lineLp.setMargins(dp(8), 0, dp(6), 0);
        addView(vertLine, lineLp);

        // 6. 构建右侧操作按钮区 (↻ 和 🗗)
        buildActionsSection(context);

        // 7. 加载数据与注册监听
        mQuotaData = QuotaParser.getInstance(context).getCurrentQuota();
        QuotaParser.getInstance(context).setOnQuotaChangeListener(this);
        updateViews();

        // 8. 启动动态倒计时轮询 (每秒更新一次倒计时)
        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdownsOnly();
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(mTimerRunnable);

        // 9. 设置触摸拖拽吸附支持
        setupDragTouch();
    }

    /**
     * 构建短期额度：5h 🟩 9% (24m)
     */
    private void buildShortTermSection(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        mTvShortLabel = new TextView(context);
        mTvShortLabel.setText("5h");
        mTvShortLabel.setTextColor(COLOR_TEXT_MAIN);
        mTvShortLabel.setTextSize(11);
        mTvShortLabel.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvShortLabel);

        // 进度小条 (32dp x 3dp) 与 绿色小点
        FrameLayout progressContainer = new FrameLayout(context);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(dp(30), dp(4));
        progLp.setMargins(dp(5), 0, dp(5), 0);
        progressContainer.setLayoutParams(progLp);

        // 轨道底色
        View trackBg = new View(context);
        GradientDrawable trackBgGd = new GradientDrawable();
        trackBgGd.setColor(COLOR_TRACK_BG);
        trackBgGd.setCornerRadius(dp(2));
        trackBg.setBackground(trackBgGd);
        progressContainer.addView(trackBg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 进度填充色
        mViewShortTrackFill = new View(context);
        GradientDrawable fillGd = new GradientDrawable();
        fillGd.setColor(COLOR_GREEN);
        fillGd.setCornerRadius(dp(2));
        mViewShortTrackFill.setBackground(fillGd);
        progressContainer.addView(mViewShortTrackFill, new FrameLayout.LayoutParams(dp(6), ViewGroup.LayoutParams.MATCH_PARENT));

        row.addView(progressContainer);

        // 绿色圆点指示器 (🟩)
        mViewShortDot = new View(context);
        GradientDrawable dotGd = new GradientDrawable();
        dotGd.setColor(COLOR_GREEN);
        dotGd.setShape(GradientDrawable.OVAL);
        mViewShortDot.setBackground(dotGd);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(5), dp(5));
        dotLp.setMargins(0, 0, dp(4), 0);
        row.addView(mViewShortDot, dotLp);

        // 百分比
        mTvShortPercent = new TextView(context);
        mTvShortPercent.setText("9%");
        mTvShortPercent.setTextColor(COLOR_TEXT_MAIN);
        mTvShortPercent.setTextSize(11);
        mTvShortPercent.setTypeface(Typeface.MONOSPACE);
        row.addView(mTvShortPercent);

        // 倒计时
        mTvShortCountdown = new TextView(context);
        mTvShortCountdown.setText("(24m)");
        mTvShortCountdown.setTextColor(COLOR_TEXT_MUTED);
        mTvShortCountdown.setTextSize(10);
        mTvShortCountdown.setPadding(dp(3), 0, 0, 0);
        row.addView(mTvShortCountdown);

        addView(row);
    }

    /**
     * 构建每周额度：每周 🟩 6% (4d3h)
     */
    private void buildWeeklySection(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        mTvWeeklyLabel = new TextView(context);
        mTvWeeklyLabel.setText("每周");
        mTvWeeklyLabel.setTextColor(COLOR_TEXT_MAIN);
        mTvWeeklyLabel.setTextSize(11);
        mTvWeeklyLabel.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvWeeklyLabel);

        // 进度小条 (30dp x 4dp)
        FrameLayout progressContainer = new FrameLayout(context);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(dp(30), dp(4));
        progLp.setMargins(dp(5), 0, dp(5), 0);
        progressContainer.setLayoutParams(progLp);

        View trackBg = new View(context);
        GradientDrawable trackBgGd = new GradientDrawable();
        trackBgGd.setColor(COLOR_TRACK_BG);
        trackBgGd.setCornerRadius(dp(2));
        trackBg.setBackground(trackBgGd);
        progressContainer.addView(trackBg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mViewWeeklyTrackFill = new View(context);
        GradientDrawable fillGd = new GradientDrawable();
        fillGd.setColor(COLOR_GREEN);
        fillGd.setCornerRadius(dp(2));
        mViewWeeklyTrackFill.setBackground(fillGd);
        progressContainer.addView(mViewWeeklyTrackFill, new FrameLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        row.addView(progressContainer);

        // 绿色小点 (🟩)
        mViewWeeklyDot = new View(context);
        GradientDrawable dotGd = new GradientDrawable();
        dotGd.setColor(COLOR_GREEN);
        dotGd.setShape(GradientDrawable.OVAL);
        mViewWeeklyDot.setBackground(dotGd);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(5), dp(5));
        dotLp.setMargins(0, 0, dp(4), 0);
        row.addView(mViewWeeklyDot, dotLp);

        // 百分比
        mTvWeeklyPercent = new TextView(context);
        mTvWeeklyPercent.setText("6%");
        mTvWeeklyPercent.setTextColor(COLOR_TEXT_MAIN);
        mTvWeeklyPercent.setTextSize(11);
        mTvWeeklyPercent.setTypeface(Typeface.MONOSPACE);
        row.addView(mTvWeeklyPercent);

        // 倒计时
        mTvWeeklyCountdown = new TextView(context);
        mTvWeeklyCountdown.setText("(4d3h)");
        mTvWeeklyCountdown.setTextColor(COLOR_TEXT_MUTED);
        mTvWeeklyCountdown.setTextSize(10);
        mTvWeeklyCountdown.setPadding(dp(3), 0, 0, 0);
        row.addView(mTvWeeklyCountdown);

        addView(row);
    }

    /**
     * 构建右侧快捷按钮：↻ (刷新) 与 🗗 (展开/跳转)
     */
    private void buildActionsSection(Context context) {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        // 1. ↻ 刷新按钮
        mBtnRefresh = new TextView(context);
        mBtnRefresh.setText("↻");
        mBtnRefresh.setTextColor(COLOR_TEXT_MUTED);
        mBtnRefresh.setTextSize(14);
        mBtnRefresh.setPadding(dp(5), dp(2), dp(5), dp(2));
        mBtnRefresh.setClickable(true);
        mBtnRefresh.setOnClickListener(v -> handleManualRefresh());
        actions.addView(mBtnRefresh);

        // 2. 🗗 展开/跳转按钮
        mBtnExpand = new TextView(context);
        mBtnExpand.setText("🗗");
        mBtnExpand.setTextColor(COLOR_TEXT_MUTED);
        mBtnExpand.setTextSize(13);
        mBtnExpand.setPadding(dp(4), dp(2), dp(2), dp(2));
        mBtnExpand.setClickable(true);
        mBtnExpand.setOnClickListener(v -> showDetailedQuotaDialog());
        actions.addView(mBtnExpand);

        addView(actions);
    }

    /**
     * 手动触发刷新：播放 360 度平滑旋转动画，并调用 QuotaParser 触发更新
     */
    private void handleManualRefresh() {
        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(mBtnRefresh, "rotation", 0f, 360f);
        rotateAnim.setDuration(600);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.start();

        Toast.makeText(getContext(), "正在刷新 Gemini 额度与重置时间...", Toast.LENGTH_SHORT).show();
        QuotaParser.getInstance(getContext()).manualRefresh(getContext());
    }

    /**
     * 弹出详细额度与刷新时间面板 (🗗)
     */
    private void showDetailedQuotaDialog() {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));
        content.setBackgroundColor(Color.parseColor("#18181B"));

        TextView titleTv = new TextView(context);
        titleTv.setText("🪐 Gemini 额度与用量监控中心");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(16);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, dp(12));
        content.addView(titleTv);

        // 卡片 1：短期额度
        content.addView(createDetailCard("短期滚动窗口 (5h)", 
                mQuotaData.shortTermPercent + "% 已用 (健康)",
                "下一次重置时间",
                formatExactDate(mQuotaData.shortTermResetTimestamp) + " " + mQuotaData.getFormattedShortCountdown(),
                COLOR_GREEN));

        // 卡片 2：每周额度
        content.addView(createDetailCard("每周额度配额 (Weekly)", 
                mQuotaData.weeklyPercent + "% 已用 (充裕)",
                "周配额重置时间",
                formatExactDate(mQuotaData.weeklyResetTimestamp) + " " + mQuotaData.getFormattedWeeklyCountdown(),
                COLOR_GREEN));

        // 卡片 3：账号状态
        content.addView(createDetailCard("当前账号与订阅等级", 
                mQuotaData.tierName,
                "Google 官方订阅",
                "包含 Gemini Advanced 2.0 Flash / Pro 完整模型调用特权",
                Color.parseColor("#38BDF8")));

        builder.setView(content);
        builder.setPositiveButton("前往 Google One 订阅", (d, w) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://one.google.com/explore-plan"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Throwable ignored) {}
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private View createDetailCard(String title, String val1, String subTitle, String val2, int accentColor) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#27272A"));
        gd.setCornerRadius(dp(8));
        card.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tTv = new TextView(getContext());
        tTv.setText(title);
        tTv.setTextColor(COLOR_TEXT_MUTED);
        tTv.setTextSize(11);
        card.addView(tTv);

        TextView vTv = new TextView(getContext());
        vTv.setText(val1);
        vTv.setTextColor(accentColor);
        vTv.setTextSize(14);
        vTv.setTypeface(Typeface.DEFAULT_BOLD);
        vTv.setPadding(0, dp(2), 0, dp(4));
        card.addView(vTv);

        TextView sTv = new TextView(getContext());
        sTv.setText(subTitle + ": " + val2);
        sTv.setTextColor(Color.parseColor("#94A3B8"));
        sTv.setTextSize(11);
        card.addView(sTv);

        return card;
    }

    /**
     * 刷新全部数据与进度条
     */
    public void updateViews() {
        if (mQuotaData == null) return;

        // 短期 5h
        mTvShortLabel.setText(mQuotaData.shortTermLabel);
        mTvShortPercent.setText(mQuotaData.shortTermPercent + "%");
        mTvShortCountdown.setText(mQuotaData.getFormattedShortCountdown());

        int shortColor = getQuotaColor(mQuotaData.shortTermPercent);
        applyColorToDotAndFill(mViewShortDot, mViewShortTrackFill, shortColor, mQuotaData.shortTermPercent);

        // 每周
        mTvWeeklyLabel.setText(mQuotaData.weeklyLabel);
        mTvWeeklyPercent.setText(mQuotaData.weeklyPercent + "%");
        mTvWeeklyCountdown.setText(mQuotaData.getFormattedWeeklyCountdown());

        int weeklyColor = getQuotaColor(mQuotaData.weeklyPercent);
        applyColorToDotAndFill(mViewWeeklyDot, mViewWeeklyTrackFill, weeklyColor, mQuotaData.weeklyPercent);
    }

    /**
     * 仅轻量刷新倒计时文本 (高频执行，节约电量)
     */
    private void updateCountdownsOnly() {
        if (mQuotaData != null) {
            mTvShortCountdown.setText(mQuotaData.getFormattedShortCountdown());
            mTvWeeklyCountdown.setText(mQuotaData.getFormattedWeeklyCountdown());
        }
    }

    private void applyColorToDotAndFill(View dot, View fill, int color, int percent) {
        if (dot != null && dot.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) dot.getBackground()).setColor(color);
        }
        if (fill != null) {
            if (fill.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) fill.getBackground()).setColor(color);
            }
            // 动态调节进度条宽度 (总宽 30dp)
            int targetWidth = Math.max(dp(3), (int) (dp(30) * (percent / 100.0f)));
            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            if (lp != null) {
                lp.width = targetWidth;
                fill.setLayoutParams(lp);
            }
        }
    }

    private int getQuotaColor(int percent) {
        if (percent >= 90) return COLOR_RED;
        if (percent >= 70) return COLOR_YELLOW;
        return COLOR_GREEN;
    }

    private String formatExactDate(long timestampMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestampMs));
    }

    @Override
    public void onQuotaUpdated(QuotaData quota) {
        post(() -> {
            mQuotaData = quota;
            updateViews();
        });
    }

    /**
     * 悬浮拖拽手势
     */
    private void setupDragTouch() {
        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getRawX();
                    mDownY = event.getRawY();
                    mLastRawX = mDownX;
                    mLastRawY = mDownY;
                    mIsDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - mLastRawX;
                    float dy = event.getRawY() - mLastRawY;
                    if (Math.hypot(event.getRawX() - mDownX, event.getRawY() - mDownY) > dp(5)) {
                        mIsDragging = true;
                    }
                    if (mIsDragging) {
                        setX(getX() + dx);
                        setY(getY() + dy);
                    }
                    mLastRawX = event.getRawX();
                    mLastRawY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!mIsDragging) {
                        // 单击弹窗
                        showDetailedQuotaDialog();
                    }
                    return true;
            }
            return false;
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mTimerRunnable);
    }

    private int dp(int val) {
        return (int) (val * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
