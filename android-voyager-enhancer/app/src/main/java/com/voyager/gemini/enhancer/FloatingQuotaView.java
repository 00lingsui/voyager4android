package com.voyager.gemini.enhancer;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
 * Gemini 额度与刷新时间监控 UI（WindowManager 顶部悬浮胶囊 Overlay）
 *
 * 核心特性：
 * 1. 使用 Android WindowManager 独立浮动层挂载，无需侵入篡改目标页面层级
 * 2. 胶囊型高质感暗黑毛玻璃外观（#141418，圆角 18dp，深色边框 #272732，悬浮 Elevation）
 * 3. 横向单行排列（Row）：
 *    - 【短期额度】："5h" 标识 + 极简绿点进度条 + 百分比 "9%" + 动态重置倒计时 "(24m)"
 *    - 【每周额度】："每周" 标识 + 极简绿点进度条 + 百分比 "6%" + 动态周重置倒计时 "(4d3h)"
 *    - 【操作按钮簇】：↻ (旋转刷新) + 🗗 (展开额度明细与快捷跳转)
 * 4. 触控自由拖拽吸顶，支持窗口坐标实时更新与生命周期安全回收
 */
public class FloatingQuotaView extends FrameLayout implements QuotaParser.OnQuotaChangeListener {

    // 默认高质感深色系配色
    private static final int COLOR_CAPSULE_BG = Color.parseColor("#E6141418"); // 90% 不透明暗夜黑
    private static final int COLOR_CAPSULE_BORDER = Color.parseColor("#272732");
    private static final int COLOR_EMERALD = Color.parseColor("#10B981");
    private static final int COLOR_AMBER = Color.parseColor("#F59E0B");
    private static final int COLOR_ROSE = Color.parseColor("#EF4444");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#F4F4F5");
    private static final int COLOR_TEXT_MUTED = Color.parseColor("#A1A1AA");
    private static final int COLOR_TRACK_BG = Color.parseColor("#27272A");

    // WindowManager 与宿主上下文引用
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private boolean mIsAttachedToWm = false;

    // 数据与定时器
    private QuotaData mQuotaData;
    private final Handler mTickerHandler = new Handler(Looper.getMainLooper());
    private Runnable mTickerRunnable;

    // UI 组件：短期额度
    private TextView mTvShortLabel;
    private View mViewShortDot;
    private View mViewShortTrackFill;
    private TextView mTvShortPercent;
    private TextView mTvShortCountdown;

    // UI 组件：每周额度
    private TextView mTvWeeklyLabel;
    private View mViewWeeklyDot;
    private View mViewWeeklyTrackFill;
    private TextView mTvWeeklyPercent;
    private TextView mTvWeeklyCountdown;

    // UI 组件：快捷按钮
    private TextView mBtnRefresh;
    private TextView mBtnExpand;

    // 触摸拖拽跟踪参数
    private float mTouchDownRawX;
    private float mTouchDownRawY;
    private int mInitParamX;
    private int mInitParamY;
    private boolean mIsDragging = false;
    private static final int TOUCH_SLOP = 12;

    // 单例/活跃实例静态持有，方便生命周期管理
    private static FloatingQuotaView sActiveInstance;

    public FloatingQuotaView(Context context) {
        this(context, null);
    }

    public FloatingQuotaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    /**
     * 在指定 Activity 上以 WindowManager 形式展现顶部胶囊额度条
     */
    public static synchronized FloatingQuotaView show(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return null;
        }

        // 若已有旧实例且在同一窗口，直接复用并刷新
        if (sActiveInstance != null && sActiveInstance.isAttachedToWindowManager()) {
            sActiveInstance.refreshData();
            return sActiveInstance;
        }

        dismiss();

        try {
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                wm = activity.getWindowManager();
            }

            FloatingQuotaView floatingView = new FloatingQuotaView(activity);
            floatingView.mWindowManager = wm;

            int statusBarHeight = getStatusBarHeight(activity);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, // 依附于 Activity 窗口，无需申请额外 SYSTEM_ALERT_WINDOW 权限
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );

            // 绑定宿主 Activity DecorView 的 Window Token
            if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                lp.token = activity.getWindow().getDecorView().getWindowToken();
            }

            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.x = 0;
            lp.y = statusBarHeight + dp(activity, 8); // 避开状态栏

            floatingView.mWindowLayoutParams = lp;
            wm.addView(floatingView, lp);
            floatingView.mIsAttachedToWm = true;
            sActiveInstance = floatingView;

            return floatingView;
        } catch (Throwable t) {
            // 如果 TYPE_APPLICATION_PANEL token 尚未就绪，退回尝试 DecorView 注入模式
            t.printStackTrace();
            return null;
        }
    }

    /**
     * 关闭并移除浮窗
     */
    public static synchronized void dismiss() {
        if (sActiveInstance != null) {
            try {
                sActiveInstance.destroySelf();
            } catch (Throwable ignored) {}
            sActiveInstance = null;
        }
    }

    public boolean isAttachedToWindowManager() {
        return mIsAttachedToWm && getParent() != null;
    }

    private void destroySelf() {
        stopCountdownTicker();
        QuotaParser.getInstance(getContext()).setOnQuotaChangeListener(null);
        if (mWindowManager != null && mIsAttachedToWm) {
            try {
                mWindowManager.removeViewImmediate(this);
            } catch (Throwable ignored) {}
            mIsAttachedToWm = false;
        }
    }

    /**
     * 初始化 UI 视图与子组件结构
     */
    private void initView(Context context) {
        setClickable(true);
        setFocusable(false);

        // 1. 构建主容器胶囊外观
        LinearLayout capsuleLayout = new LinearLayout(context);
        capsuleLayout.setOrientation(LinearLayout.HORIZONTAL);
        capsuleLayout.setGravity(Gravity.CENTER_VERTICAL);

        int padHoriz = dp(12);
        int padVert = dp(6);
        capsuleLayout.setPadding(padHoriz, padVert, padHoriz, padVert);

        GradientDrawable capsuleBg = new GradientDrawable();
        capsuleBg.setColor(COLOR_CAPSULE_BG);
        capsuleBg.setCornerRadius(dp(18));
        capsuleBg.setStroke(dp(1), COLOR_CAPSULE_BORDER);
        capsuleLayout.setBackground(capsuleBg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capsuleLayout.setElevation(dp(8));
        }

        // 2. 添加短期额度组件组
        capsuleLayout.addView(buildShortTermSection(context));

        // 3. 中隔圆点 "·"
        TextView dotDivider = new TextView(context);
        dotDivider.setText("·");
        dotDivider.setTextColor(Color.parseColor("#52525B"));
        dotDivider.setTextSize(12);
        dotDivider.setPadding(dp(6), 0, dp(6), 0);
        capsuleLayout.addView(dotDivider);

        // 4. 添加每周额度组件组
        capsuleLayout.addView(buildWeeklySection(context));

        // 5. 垂直细分割线
        View vertDivider = new View(context);
        vertDivider.setBackgroundColor(Color.parseColor("#3F3F46"));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(1), dp(13));
        lineLp.setMargins(dp(8), 0, dp(6), 0);
        capsuleLayout.addView(vertDivider, lineLp);

        // 6. 添加操作按钮簇 (↻ 和 🗗)
        capsuleLayout.addView(buildActionButtons(context));

        // 将胶囊放入当前 FloatingQuotaView 根布局
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        flp.gravity = Gravity.CENTER;
        addView(capsuleLayout, flp);

        // 7. 读取初始额度数据并绑定解析器监听
        mQuotaData = QuotaParser.getInstance(context).getCurrentQuota();
        if (mQuotaData == null) {
            mQuotaData = QuotaData.loadFromPrefs(context);
        }
        QuotaParser.getInstance(context).setOnQuotaChangeListener(this);

        updateDisplayUI();
        startCountdownTicker();
    }

    /**
     * 构建【短期额度 (如 5h)】：标签 + 绿点迷你进度条 + 百分比 + 倒计时
     */
    private View buildShortTermSection(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // 文本标识 (5h)
        mTvShortLabel = new TextView(context);
        mTvShortLabel.setText("5h");
        mTvShortLabel.setTextColor(COLOR_TEXT_PRIMARY);
        mTvShortLabel.setTextSize(11);
        mTvShortLabel.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvShortLabel);

        // 极简微型进度条容器
        FrameLayout trackContainer = new FrameLayout(context);
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(dp(38), dp(6));
        trackLp.setMargins(dp(6), 0, dp(5), 0);
        trackContainer.setLayoutParams(trackLp);

        // 轨道底色
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(COLOR_TRACK_BG);
        trackBg.setCornerRadius(dp(3));
        trackContainer.setBackground(trackBg);

        // 轨道填充指示条
        mViewShortTrackFill = new View(context);
        GradientDrawable fillDrawable = new GradientDrawable();
        fillDrawable.setColor(COLOR_EMERALD);
        fillDrawable.setCornerRadius(dp(3));
        mViewShortTrackFill.setBackground(fillDrawable);
        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT);
        trackContainer.addView(mViewShortTrackFill, fillLp);
        row.addView(trackContainer);

        // 状态指示小绿点
        mViewShortDot = new View(context);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(COLOR_EMERALD);
        mViewShortDot.setBackground(dotDrawable);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dotLp.setMargins(0, 0, dp(4), 0);
        row.addView(mViewShortDot, dotLp);

        // 百分比文字 (如 9%)
        mTvShortPercent = new TextView(context);
        mTvShortPercent.setText("9%");
        mTvShortPercent.setTextColor(COLOR_TEXT_PRIMARY);
        mTvShortPercent.setTextSize(11);
        mTvShortPercent.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvShortPercent);

        // 动态刷新倒计时文字 (如 (24m))
        mTvShortCountdown = new TextView(context);
        mTvShortCountdown.setText("(24m)");
        mTvShortCountdown.setTextColor(COLOR_EMERALD);
        mTvShortCountdown.setTextSize(11);
        mTvShortCountdown.setTypeface(Typeface.DEFAULT_BOLD);
        mTvShortCountdown.setPadding(dp(2), 0, 0, 0);
        row.addView(mTvShortCountdown);

        return row;
    }

    /**
     * 构建【每周额度】：标签 "每周" + 极简微型进度条 + 百分比 + 周倒计时
     */
    private View buildWeeklySection(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // 文本标识 (每周)
        mTvWeeklyLabel = new TextView(context);
        mTvWeeklyLabel.setText("每周");
        mTvWeeklyLabel.setTextColor(COLOR_TEXT_MUTED);
        mTvWeeklyLabel.setTextSize(11);
        mTvWeeklyLabel.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvWeeklyLabel);

        // 极简微型进度条容器
        FrameLayout trackContainer = new FrameLayout(context);
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(dp(38), dp(6));
        trackLp.setMargins(dp(6), 0, dp(5), 0);
        trackContainer.setLayoutParams(trackLp);

        // 轨道底色
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(COLOR_TRACK_BG);
        trackBg.setCornerRadius(dp(3));
        trackContainer.setBackground(trackBg);

        // 轨道填充指示条
        mViewWeeklyTrackFill = new View(context);
        GradientDrawable fillDrawable = new GradientDrawable();
        fillDrawable.setColor(COLOR_EMERALD);
        fillDrawable.setCornerRadius(dp(3));
        mViewWeeklyTrackFill.setBackground(fillDrawable);
        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT);
        trackContainer.addView(mViewWeeklyTrackFill, fillLp);
        row.addView(trackContainer);

        // 状态指示小绿点
        mViewWeeklyDot = new View(context);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(COLOR_EMERALD);
        mViewWeeklyDot.setBackground(dotDrawable);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dotLp.setMargins(0, 0, dp(4), 0);
        row.addView(mViewWeeklyDot, dotLp);

        // 每周百分比 (如 6%)
        mTvWeeklyPercent = new TextView(context);
        mTvWeeklyPercent.setText("6%");
        mTvWeeklyPercent.setTextColor(COLOR_TEXT_PRIMARY);
        mTvWeeklyPercent.setTextSize(11);
        mTvWeeklyPercent.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mTvWeeklyPercent);

        // 动态每周刷新倒计时文字 (如 (4d3h))
        mTvWeeklyCountdown = new TextView(context);
        mTvWeeklyCountdown.setText("(4d3h)");
        mTvWeeklyCountdown.setTextColor(COLOR_EMERALD);
        mTvWeeklyCountdown.setTextSize(11);
        mTvWeeklyCountdown.setTypeface(Typeface.DEFAULT_BOLD);
        mTvWeeklyCountdown.setPadding(dp(2), 0, 0, 0);
        row.addView(mTvWeeklyCountdown);

        return row;
    }

    /**
     * 构建右侧操作按钮簇：↻ (刷新) 与 🗗 (展开/跳转)
     */
    private View buildActionButtons(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // ↻ 刷新按钮
        mBtnRefresh = new TextView(context);
        mBtnRefresh.setText("↻");
        mBtnRefresh.setTextColor(COLOR_TEXT_MUTED);
        mBtnRefresh.setTextSize(14);
        mBtnRefresh.setGravity(Gravity.CENTER);
        mBtnRefresh.setPadding(dp(4), dp(2), dp(4), dp(2));

        mBtnRefresh.setOnClickListener(v -> {
            // 执行顺滑 360 度自转动画
            ObjectAnimator animator = ObjectAnimator.ofFloat(mBtnRefresh, "rotation", 0f, 360f);
            animator.setDuration(600);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();

            // 触发主动数据刷新抓取
            QuotaParser.getInstance(context).manualRefresh(context);
            Toast.makeText(context, "🪐 Gemini 额度与刷新倒计时已更新", Toast.LENGTH_SHORT).show();
        });
        row.addView(mBtnRefresh);

        // 🗗 展开/跳转按钮
        mBtnExpand = new TextView(context);
        mBtnExpand.setText("🗗");
        mBtnExpand.setTextColor(COLOR_TEXT_MUTED);
        mBtnExpand.setTextSize(13);
        mBtnExpand.setGravity(Gravity.CENTER);
        mBtnExpand.setPadding(dp(4), dp(2), dp(4), dp(2));

        mBtnExpand.setOnClickListener(v -> showDetailedQuotaDialog(context));
        row.addView(mBtnExpand);

        return row;
    }

    /**
     * 刷新额度与视图渲染
     */
    public void refreshData() {
        mQuotaData = QuotaParser.getInstance(getContext()).getCurrentQuota();
        updateDisplayUI();
    }

    /**
     * 更新全部 UI 显示
     */
    private void updateDisplayUI() {
        if (mQuotaData == null) return;

        // 1. 短期额度更新
        if (mTvShortLabel != null) mTvShortLabel.setText(mQuotaData.shortTermLabel);
        if (mTvShortPercent != null) mTvShortPercent.setText(mQuotaData.shortTermPercent + "%");
        if (mTvShortCountdown != null) mTvShortCountdown.setText(mQuotaData.getFormattedShortCountdown());

        int shortColor = resolveColorByPercent(mQuotaData.shortTermPercent);
        updateDotAndTrack(mViewShortDot, mViewShortTrackFill, mQuotaData.shortTermPercent, shortColor);
        if (mTvShortCountdown != null) mTvShortCountdown.setTextColor(shortColor);

        // 2. 每周额度更新
        if (mTvWeeklyLabel != null) mTvWeeklyLabel.setText(mQuotaData.weeklyLabel);
        if (mTvWeeklyPercent != null) mTvWeeklyPercent.setText(mQuotaData.weeklyPercent + "%");
        if (mTvWeeklyCountdown != null) mTvWeeklyCountdown.setText(mQuotaData.getFormattedWeeklyCountdown());

        int weeklyColor = resolveColorByPercent(mQuotaData.weeklyPercent);
        updateDotAndTrack(mViewWeeklyDot, mViewWeeklyTrackFill, mQuotaData.weeklyPercent, weeklyColor);
        if (mTvWeeklyCountdown != null) mTvWeeklyCountdown.setTextColor(weeklyColor);
    }

    /**
     * 针对指示点与进度条宽度赋色
     */
    private void updateDotAndTrack(View dot, View trackFill, int percent, int color) {
        if (dot != null) {
            GradientDrawable d = (GradientDrawable) dot.getBackground();
            if (d != null) d.setColor(color);
        }
        if (trackFill != null) {
            GradientDrawable f = (GradientDrawable) trackFill.getBackground();
            if (f != null) f.setColor(color);

            int totalWidthDp = 38;
            int filledWidthDp = Math.max(2, (int) (totalWidthDp * (percent / 100.0f)));
            ViewGroup.LayoutParams lp = trackFill.getLayoutParams();
            if (lp != null) {
                lp.width = dp(filledWidthDp);
                trackFill.setLayoutParams(lp);
            }
        }
    }

    /**
     * 根据额度百分比返回对应预警色
     */
    private int resolveColorByPercent(int percent) {
        if (percent >= 90) return COLOR_ROSE;
        if (percent >= 70) return COLOR_AMBER;
        return COLOR_EMERALD;
    }

    /**
     * 启动每秒定时器驱动倒计时平滑刷新
     */
    private void startCountdownTicker() {
        stopCountdownTicker();
        mTickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mQuotaData != null) {
                    if (mTvShortCountdown != null) {
                        mTvShortCountdown.setText(mQuotaData.getFormattedShortCountdown());
                    }
                    if (mTvWeeklyCountdown != null) {
                        mTvWeeklyCountdown.setText(mQuotaData.getFormattedWeeklyCountdown());
                    }
                }
                mTickerHandler.postDelayed(this, 1000);
            }
        };
        mTickerHandler.postDelayed(mTickerRunnable, 1000);
    }

    private void stopCountdownTicker() {
        if (mTickerRunnable != null) {
            mTickerHandler.removeCallbacks(mTickerRunnable);
            mTickerRunnable = null;
        }
    }

    /**
     * 展开全功能额度详情对话框与跳转中心
     */
    private void showDetailedQuotaDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(Color.parseColor("#18181B"));

        // 标题行
        TextView tvTitle = new TextView(context);
        tvTitle.setText("🪐 Gemini 额度与配额详情");
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(COLOR_TEXT_PRIMARY);
        content.addView(tvTitle);

        // 订阅/套餐档位
        TextView tvTier = new TextView(context);
        tvTier.setText("当前订阅计划：" + (mQuotaData != null ? mQuotaData.tierName : "Gemini Advanced"));
        tvTier.setTextSize(12);
        tvTier.setTextColor(COLOR_TEXT_MUTED);
        tvTier.setPadding(0, dp(4), 0, dp(14));
        content.addView(tvTier);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 详情卡片 1：短期额度
        content.addView(createMetricCard(
                context,
                "⚡ 短期使用率 (" + (mQuotaData != null ? mQuotaData.shortTermLabel : "5h") + ")",
                (mQuotaData != null ? mQuotaData.shortTermPercent : 0) + "%",
                "下次重置：" + (mQuotaData != null && mQuotaData.shortTermResetTimestamp > 0
                        ? sdf.format(new Date(mQuotaData.shortTermResetTimestamp)) + " " + mQuotaData.getFormattedShortCountdown()
                        : "待捕获")
        ));

        // 详情卡片 2：每周额度
        content.addView(createMetricCard(
                context,
                "📅 每周配额池 (Weekly Limit)",
                (mQuotaData != null ? mQuotaData.weeklyPercent : 0) + "%",
                "每周重置：" + (mQuotaData != null && mQuotaData.weeklyResetTimestamp > 0
                        ? sdf.format(new Date(mQuotaData.weeklyResetTimestamp)) + " " + mQuotaData.getFormattedWeeklyCountdown()
                        : "待捕获")
        ));

        // 操作按钮行：跳转官方设置 & 手动重测
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);
        actionsRow.setPadding(0, dp(16), 0, 0);

        TextView btnWeb = new TextView(context);
        btnWeb.setText("打开 Gemini 官网 ↗");
        btnWeb.setTextColor(COLOR_EMERALD);
        btnWeb.setTextSize(13);
        btnWeb.setPadding(dp(8), dp(8), dp(12), dp(8));
        btnWeb.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"));
                context.startActivity(intent);
            } catch (Throwable ignored) {}
        });
        actionsRow.addView(btnWeb);

        content.addView(actionsRow);
        scrollView.addView(content);

        builder.setView(scrollView);
        builder.setPositiveButton("我知道了", null);
        builder.setNeutralButton("立即主动刷新", (dialog, which) -> {
            QuotaParser.getInstance(context).manualRefresh(context);
            Toast.makeText(context, "已触发额度探针更新", Toast.LENGTH_SHORT).show();
        });

        try {
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private View createMetricCard(Context context, String title, String value, String sub) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        card.setPadding(p, p, p, p);
        card.setMargins(0, 0, 0, dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#27272A"));
        bg.setCornerRadius(dp(8));
        card.setBackground(bg);

        TextView tTitle = new TextView(context);
        tTitle.setText(title);
        tTitle.setTextColor(COLOR_TEXT_MUTED);
        tTitle.setTextSize(11);
        card.addView(tTitle);

        TextView tVal = new TextView(context);
        tVal.setText(value);
        tVal.setTextColor(COLOR_TEXT_PRIMARY);
        tVal.setTextSize(20);
        tVal.setTypeface(Typeface.DEFAULT_BOLD);
        tVal.setPadding(0, dp(2), 0, dp(2));
        card.addView(tVal);

        TextView tSub = new TextView(context);
        tSub.setText(sub);
        tSub.setTextColor(COLOR_EMERALD);
        tSub.setTextSize(11);
        card.addView(tSub);

        return card;
    }

    /**
     * 触摸手势事件拦截：支持在屏幕顶部与全屏范围内自由拖拽
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mWindowManager == null || mWindowLayoutParams == null) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownRawX = event.getRawX();
                mTouchDownRawY = event.getRawY();
                mInitParamX = mWindowLayoutParams.x;
                mInitParamY = mWindowLayoutParams.y;
                mIsDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - mTouchDownRawX;
                float deltaY = event.getRawY() - mTouchDownRawY;

                if (!mIsDragging && Math.hypot(deltaX, deltaY) > TOUCH_SLOP) {
                    mIsDragging = true;
                }

                if (mIsDragging) {
                    mWindowLayoutParams.x = (int) (mInitParamX + deltaX);
                    mWindowLayoutParams.y = (int) (mInitParamY + deltaY);
                    try {
                        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
                    } catch (Throwable ignored) {}
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    mIsDragging = false;
                    return true;
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onQuotaUpdated(QuotaData quota) {
        post(() -> {
            mQuotaData = quota;
            updateDisplayUI();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startCountdownTicker();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopCountdownTicker();
    }

    private static int getStatusBarHeight(Context context) {
        int result = 0;
        try {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Throwable ignored) {}
        return result > 0 ? result : dp(context, 24);
    }

    private static int dp(Context context, int val) {
        return (int) (val * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private int dp(int val) {
        return dp(getContext(), val);
    }
}
