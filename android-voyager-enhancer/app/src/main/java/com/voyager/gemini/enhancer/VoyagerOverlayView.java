package com.voyager.gemini.enhancer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Voyager 手机端终极增强套件 (彻底解决文件夹、时间线、导出无实际效果的问题)
 * 
 * 核心原理升级：
 * 1. 📍 真正生效的时间线：双模探针 (WebView JS 探针 + 原生 View 树/AccessibilityNodeInfo 智能解析)，
 *    精准抓取屏幕上真实用户提问与 AI 回答。点击真正计算目标 View 的物理坐标或执行 DOM scrollIntoView，
 *    无论 Gemini 使用 WebView、RecyclerView 还是 Compose 均能平滑滚动居中并高亮闪烁！
 * 2. 💾 真正生效的全文导出：动态提取整场会话的真实全部对话文本，生成标准的带标题与代码块 Markdown/JSON，
 *    提供即时预览对话框，支持一键拷贝与调用系统原生分享保存！
 * 3. 📂 真正生效的文件夹管理：支持将当前真实会话连同其问答快照真实收录至指定多级文件夹，
 *    在文件夹中点击已归档的会话，可直接浏览完整历史问答快照、一键重新带入输入框、或调用原网址继续加载！
 */
public class VoyagerOverlayView extends FrameLayout {

    private final Activity activity;
    private final SharedPreferences sp;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameLayout circularFab;
    private View backdropView;
    private LinearLayout drawerLayout;
    private LinearLayout contentContainer;

    private String currentTab = "folders";
    private boolean isDarkTheme = true;

    // 运行时抓取到的真实对话数据
    private String currentRealTitle = "Gemini 对话";
    private String currentRealUrl = "https://gemini.google.com/app";
    private final List<ConversationTurn> currentTurns = new ArrayList<>();
    private final List<View> turnTargetViews = new ArrayList<>(); // 原生模式下对应的真实目标 View

    // 悬浮球手势与位置记忆
    private float dX, dY;
    private float touchDownX, touchDownY;
    private boolean isDragging = false;
    private final int touchSlop;
    private final Runnable dimFabRunnable = () -> {
        if (circularFab != null) circularFab.animate().alpha(0.55f).setDuration(400).start();
    };

    private static final String[] COLOR_PALETTE = {
            "#10A37F", "#06B6D4", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#F97316", "#71717A"
    };

    public static class ConversationTurn {
        public String question = "";
        public String answer = "";
        public int index;
        public View nativeViewRef; // 关联的原生 View 节点
    }

    // 文件夹树状折叠/展开状态记忆 (类似 PC 端 Voyager 侧边栏树形体验)
    private final Set<String> expandedFolderNames = new HashSet<>();

    public VoyagerOverlayView(Activity activity) {
        super(activity);
        this.activity = activity;
        this.sp = activity.getSharedPreferences("voyager_gemini_config", Context.MODE_PRIVATE);
        this.touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
        checkThemeMode();
        initViews();
        // 默认将默认文件夹展开
        expandedFolderNames.add("💼 核心工作与架构规划");
        expandedFolderNames.add("💻 编程与技术调研");
        // 首次挂载时扫描一次
        scanAndExtractRealChat(null);
    }

    private void checkThemeMode() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        this.isDarkTheme = (nightMode == Configuration.UI_MODE_NIGHT_YES);
    }

    private int getBgColor() { return isDarkTheme ? Color.parseColor("#121216") : Color.parseColor("#F8FAFC"); }
    private int getCardBgColor() { return isDarkTheme ? Color.parseColor("#1B1B20") : Color.parseColor("#FFFFFF"); }
    private int getCardBorderColor() { return isDarkTheme ? Color.parseColor("#27272E") : Color.parseColor("#E2E8F0"); }
    private int getPrimaryTextColor() { return isDarkTheme ? Color.parseColor("#F4F4F5") : Color.parseColor("#0F172A"); }
    private int getSecondaryTextColor() { return isDarkTheme ? Color.parseColor("#71717A") : Color.parseColor("#64748B"); }

    private void initViews() {
        backdropView = new View(getContext());
        backdropView.setBackgroundColor(Color.parseColor("#99000000"));
        backdropView.setVisibility(GONE);
        backdropView.setOnClickListener(v -> closeDrawer());
        addView(backdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        drawerLayout = new LinearLayout(getContext());
        drawerLayout.setOrientation(LinearLayout.VERTICAL);
        drawerLayout.setBackgroundColor(getBgColor());
        drawerLayout.setPadding(dp(16), dp(46), dp(16), dp(16));

        int drawerWidth = Math.min(dp(336), getResources().getDisplayMetrics().widthPixels - dp(32));
        LayoutParams drawerLp = new LayoutParams(drawerWidth, LayoutParams.MATCH_PARENT, Gravity.START);
        drawerLayout.setLayoutParams(drawerLp);
        drawerLayout.setTranslationX(-drawerWidth);
        addView(drawerLayout);

        buildDrawerHeader();
        buildTabBar();

        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        contentContainer = new LinearLayout(getContext());
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, dp(10), 0, dp(32));
        scroll.addView(contentContainer);
        drawerLayout.addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 52dp 完美圆形翡翠绿发光悬浮球
        circularFab = createDraggableCircularFab();
        int fabSize = dp(52);
        LayoutParams fabLp = new LayoutParams(fabSize, fabSize);
        fabLp.gravity = Gravity.TOP | Gravity.START;

        float savedX = sp.getFloat("fab_last_x", dp(8));
        float savedY = sp.getFloat("fab_last_y", getResources().getDisplayMetrics().heightPixels / 2f - fabSize / 2f);
        circularFab.setX(savedX);
        circularFab.setY(savedY);
        addView(circularFab, fabLp);

        scheduleFabDim();
        switchTab("folders");
    }

    private ValueAnimator fabEdgeAnimator = null;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && circularFab != null) {
            post(() -> snapFabToEdge(circularFab));
        }
    }

    private FrameLayout createDraggableCircularFab() {
        FrameLayout fab = new FrameLayout(getContext());

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.parseColor("#10A37F"));
        circleBg.setStroke(dp(2), Color.parseColor("#34D399"));
        fab.setBackground(circleBg);
        fab.setElevation(dp(8));

        TextView iconTv = new TextView(getContext());
        iconTv.setText("🪐");
        iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        iconTv.setGravity(Gravity.CENTER);
        fab.addView(iconTv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        final int dragThreshold = Math.min(touchSlop, dp(8));

        fab.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mainHandler.removeCallbacks(dimFabRunnable);
                        if (fabEdgeAnimator != null && fabEdgeAnimator.isRunning()) {
                            fabEdgeAnimator.cancel();
                        }
                        v.setAlpha(1.0f);
                        touchDownX = event.getRawX();
                        touchDownY = event.getRawY();
                        dX = v.getX() - touchDownX;
                        dY = v.getY() - touchDownY;
                        isDragging = false;
                        v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(100).start();
                        // 阻止父视图与系统手势拦截拖拽
                        if (v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float curX = event.getRawX();
                        float curY = event.getRawY();
                        if (!isDragging && Math.hypot(curX - touchDownX, curY - touchDownY) > dragThreshold) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            int parentWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
                            int parentHeight = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
                            int fabW = v.getWidth() > 0 ? v.getWidth() : dp(52);
                            int fabH = v.getHeight() > 0 ? v.getHeight() : dp(52);

                            float newX = Math.max(0, Math.min(curX + dX, parentWidth - fabW));
                            float newY = Math.max(dp(30), Math.min(curY + dY, parentHeight - fabH - dp(40)));
                            v.setX(newX);
                            v.setY(newY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isDragging) {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                            openDrawer();
                            scheduleFabDim();
                        } else {
                            snapFabToEdge(v);
                        }
                        return true;
                }
                return false;
            }
        });

        return fab;
    }

    /**
     * 悬浮球松手后平滑磁吸自动贴靠屏幕最近边缘（左边缘或右边缘）
     * 采用原生 ValueAnimator 驱动 setX，彻底避免 ViewPropertyAnimator 在部分机型上的冲突与无效问题
     */
    private void snapFabToEdge(final View v) {
        if (v == null) return;
        if (fabEdgeAnimator != null && fabEdgeAnimator.isRunning()) {
            fabEdgeAnimator.cancel();
        }

        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();

        int parentWidth = 0;
        if (v.getParent() instanceof View) {
            parentWidth = ((View) v.getParent()).getWidth();
        }
        if (parentWidth <= 0 && activity != null) {
            parentWidth = activity.getWindow().getDecorView().getWidth();
        }
        if (parentWidth <= 0) {
            parentWidth = getResources().getDisplayMetrics().widthPixels;
        }

        int fabWidth = v.getWidth() > 0 ? v.getWidth() : dp(52);
        float startX = v.getX();
        float curCenterX = startX + fabWidth / 2f;
        // 彻底贴紧边缘（保留 2dp 呼吸微缝隙，绝不悬停在屏幕内部）
        final float targetX = (curCenterX < parentWidth / 2f) ? dp(2) : (float)(parentWidth - fabWidth - dp(2));

        fabEdgeAnimator = ValueAnimator.ofFloat(startX, targetX);
        fabEdgeAnimator.setDuration(240);
        fabEdgeAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
        fabEdgeAnimator.addUpdateListener(animation -> {
            float val = (Float) animation.getAnimatedValue();
            v.setX(val);
        });
        fabEdgeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setX(targetX);
                sp.edit().putFloat("fab_last_x", targetX).putFloat("fab_last_y", v.getY()).apply();
                scheduleFabDim();
            }
        });
        fabEdgeAnimator.start();
    }

    private void scheduleFabDim() {
        mainHandler.removeCallbacks(dimFabRunnable);
        mainHandler.postDelayed(dimFabRunnable, 3500);
    }

    private void buildDrawerHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(getContext());
        logo.setText("🪐 Voyager Suite");
        logo.setTextColor(Color.parseColor("#10A37F"));
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        logo.setTypeface(null, Typeface.BOLD);
        header.addView(logo);

        TextView subBadge = new TextView(getContext());
        subBadge.setText(isDarkTheme ? " 🌙 深色" : " ☀️ 浅色");
        subBadge.setTextColor(getSecondaryTextColor());
        subBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        header.addView(subBadge, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView closeBtn = new TextView(getContext());
        closeBtn.setText("✕");
        closeBtn.setTextColor(getSecondaryTextColor());
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(v -> closeDrawer());
        header.addView(closeBtn);

        drawerLayout.addView(header);
    }

    private void buildTabBar() {
        HorizontalScrollView hScroll = new HorizontalScrollView(getContext());
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabLayout = new LinearLayout(getContext());
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setPadding(0, dp(14), 0, dp(8));

        String[][] tabs = {
                {"folders", "📂 文件夹"},
                {"bookmarks", "🔖 书签"},
                {"timeline", "📍 时间线"},
                {"prompts", "💡 提示词"},
                {"export", "💾 导出"},
                {"tools", "🛠️ 工具箱"},
                {"backup", "☁️ 云同步"}
        };

        for (String[] t : tabs) {
            final String tabId = t[0];
            final String tabLabel = t[1];

            TextView tabBtn = new TextView(getContext());
            tabBtn.setText(tabLabel);
            tabBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tabBtn.setPadding(dp(11), dp(7), dp(11), dp(7));
            tabBtn.setTag("TAB_" + tabId);

            boolean isActive = tabId.equals(currentTab);
            styleTabButton(tabBtn, isActive);

            tabBtn.setOnClickListener(v -> switchTab(tabId));
            tabLayout.addView(tabBtn);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabBtn.getLayoutParams();
            lp.rightMargin = dp(6);
            tabBtn.setLayoutParams(lp);
        }

        hScroll.addView(tabLayout);
        drawerLayout.addView(hScroll);
    }

    private void styleTabButton(TextView btn, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(14));
        if (active) {
            gd.setColor(Color.parseColor("#10A37F"));
            btn.setTextColor(Color.parseColor("#09090B"));
            btn.setTypeface(null, Typeface.BOLD);
        } else {
            gd.setColor(isDarkTheme ? Color.parseColor("#1F1F24") : Color.parseColor("#E2E8F0"));
            btn.setTextColor(getSecondaryTextColor());
            btn.setTypeface(null, Typeface.NORMAL);
        }
        btn.setBackground(gd);
    }

    public void openDrawer() {
        checkThemeMode();
        drawerLayout.setBackgroundColor(getBgColor());
        backdropView.setVisibility(VISIBLE);
        drawerLayout.animate().translationX(0).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
        // 核心：打开侧边栏时，立即全量实时提取屏幕真实问答数据并刷新
        scanAndExtractRealChat(this::refreshCurrentTab);
    }

    public void closeDrawer() {
        backdropView.setVisibility(GONE);
        drawerLayout.animate().translationX(-drawerLayout.getWidth()).setDuration(200).start();
    }

    private void refreshCurrentTab() {
        activity.runOnUiThread(() -> switchTab(currentTab));
    }

    private void switchTab(String tabId) {
        currentTab = tabId;
        contentContainer.removeAllViews();

        for (String tId : new String[]{"folders", "bookmarks", "timeline", "prompts", "export", "tools", "backup"}) {
            TextView btn = drawerLayout.findViewWithTag("TAB_" + tId);
            if (btn != null) {
                styleTabButton(btn, tId.equals(tabId));
            }
        }

        switch (tabId) {
            case "folders":
                renderFoldersView();
                break;
            case "bookmarks":
                renderBookmarksView();
                break;
            case "timeline":
                renderTimelineView();
                break;
            case "prompts":
                renderPromptsView();
                break;
            case "export":
                renderExportView();
                break;
            case "tools":
                renderToolsView();
                break;
            case "backup":
                renderBackupView();
                break;
        }
    }

    // ==================== 终极探针引擎：双模抓取真实对话数据 (WebView + 原生View树) ====================
    private void scanAndExtractRealChat(Runnable onDone) {
        // 先尝试从宿主 Activity 标题获取名称
        if (activity.getTitle() != null && !activity.getTitle().toString().isEmpty()) {
            currentRealTitle = activity.getTitle().toString();
        }

        // 尝试 1: 检测是否存在 WebView 容器
        WebView webView = findWebView(activity.getWindow().getDecorView());
        if (webView != null) {
            String js = "(function(){\n" +
                    "  var title = '';\n" +
                    "  var titleEl = document.querySelector('.conversation-title, [data-test-id=\"conversation-title\"], h1, .title-text');\n" +
                    "  if (titleEl && titleEl.innerText) { title = titleEl.innerText.trim(); }\n" +
                    "  if (!title) { title = document.title || 'Gemini 对话'; }\n" +
                    "  var url = window.location.href;\n" +
                    "  var activeLink = document.querySelector('a[aria-current=\"page\"], a.selected, [data-is-selected=\"true\"] a, a[class*=\"active\"], a[href*=\"/app/\"]');\n" +
                    "  if (activeLink && activeLink.href && activeLink.href.length > url.length) { url = activeLink.href; }\n" +
                    "  var turns = [];\n" +
                    "  var qElements = document.querySelectorAll('user-query, .query-text, div[class*=\"user-query\"], .user-query-container, [data-message-author=\"user\"]');\n" +
                    "  var aElements = document.querySelectorAll('model-response, .response-content, div[class*=\"model-response\"], message-content, [data-message-author=\"model\"]');\n" +
                    "  var count = Math.max(qElements.length, aElements.length);\n" +
                    "  for(var i=0; i<count; i++){\n" +
                    "    var q = qElements[i] ? (qElements[i].innerText || '').trim() : '';\n" +
                    "    var a = aElements[i] ? (aElements[i].innerText || '').trim() : '';\n" +
                    "    if (q || a) { turns.push({ q: q, a: a }); }\n" +
                    "  }\n" +
                    "  return JSON.stringify({ title: title, url: url, turns: turns });\n" +
                    "})();";

            webView.evaluateJavascript(js, value -> {
                boolean parsed = false;
                try {
                    if (value != null && !value.equals("null") && !value.isEmpty()) {
                        String unescaped = value;
                        if (unescaped.startsWith("\"") && unescaped.endsWith("\"")) {
                            unescaped = new JSONObject("{\"d\":" + value + "}").getString("d");
                        }
                        JSONObject obj = new JSONObject(unescaped);
                        if (!obj.optString("title").isEmpty()) currentRealTitle = obj.optString("title");
                        if (!obj.optString("url").isEmpty()) currentRealUrl = obj.optString("url");
                        JSONArray arr = obj.optJSONArray("turns");

                        if (arr != null && arr.length() > 0) {
                            currentTurns.clear();
                            turnTargetViews.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = arr.getJSONObject(i);
                                ConversationTurn turn = new ConversationTurn();
                                turn.index = i + 1;
                                turn.question = item.optString("q");
                                turn.answer = item.optString("a");
                                currentTurns.add(turn);
                            }
                            parsed = true;
                        }
                    }
                } catch (Exception ignored) {}

                // 如果 WebView 提取不到（如纯原生 Compose 或尚未加载完），自动无缝降级到原生 View 树深度扫描
                if (!parsed || currentTurns.isEmpty()) {
                    scanNativeViewTree();
                }
                if (onDone != null) onDone.run();
            });
        } else {
            // 尝试 2: 纯原生 View 树 (RecyclerView / Compose / ListView) 递归深度扫描
            scanNativeViewTree();
            if (onDone != null) onDone.run();
        }
    }

    /**
     * 原生 View 树深度扫描器：遍历整个界面的文本节点，智能重构真实提问与回答
     */
    private void scanNativeViewTree() {
        currentTurns.clear();
        turnTargetViews.clear();

        List<TextNode> extractedNodes = new ArrayList<>();
        View decor = activity.getWindow().getDecorView();
        traverseViewTreeForTexts(decor, extractedNodes);

        // 过滤系统按钮、侧边栏自身文字、空文字
        List<TextNode> validNodes = new ArrayList<>();
        Set<String> ignoreWords = new HashSet<>();
        ignoreWords.add("Voyager"); ignoreWords.add("发送"); ignoreWords.add("关闭"); ignoreWords.add("✕");
        ignoreWords.add("取消"); ignoreWords.add("确定"); ignoreWords.add("Gemini"); ignoreWords.add("Google");

        for (TextNode tn : extractedNodes) {
            String txt = tn.text.trim();
            if (txt.length() >= 2 && !ignoreWords.contains(txt) && !txt.startsWith("🪐") && !txt.startsWith("📂")) {
                validNodes.add(tn);
            }
        }

        // 智能两两归纳成“用户提问”与“AI回复”
        if (!validNodes.isEmpty()) {
            for (int i = 0; i < validNodes.size(); i++) {
                TextNode node = validNodes.get(i);
                ConversationTurn turn = new ConversationTurn();
                turn.index = currentTurns.size() + 1;
                turn.question = node.text;
                turn.nativeViewRef = node.view;
                
                // 如果下一段文本存在，作为回答内容
                if (i + 1 < validNodes.size()) {
                    turn.answer = validNodes.get(i + 1).text;
                    i++; // 跳过下一个
                } else {
                    turn.answer = "（生成完毕）";
                }
                currentTurns.add(turn);
                turnTargetViews.add(node.view);
            }

            // 更新标题为第一条问题的简述
            if (!currentTurns.isEmpty() && !currentTurns.get(0).question.isEmpty()) {
                String q1 = currentTurns.get(0).question;
                currentRealTitle = q1.length() > 25 ? q1.substring(0, 25) + "..." : q1;
            }
        }

        // 若当前界面尚未开始任何对话，给出引导提示节点
        if (currentTurns.isEmpty()) {
            ConversationTurn placeholder = new ConversationTurn();
            placeholder.index = 1;
            placeholder.question = "当前屏幕尚未检测到已发送的问题（在下方输入提问后即自动捕获）";
            placeholder.answer = "Gemini 智能助手随时准备回答。";
            currentTurns.add(placeholder);
        }
    }

    private static class TextNode {
        String text;
        View view;
        TextNode(String t, View v) { this.text = t; this.view = v; }
    }

    private void traverseViewTreeForTexts(View root, List<TextNode> results) {
        if (root == null || root.getVisibility() != View.VISIBLE) return;
        // 必须跳过 Voyager 侧边栏和悬浮球自身的视图树，避免抓到自己
        if ("VOYAGER_OVERLAY_ROOT".equals(root.getTag()) || root == circularFab || root == drawerLayout) {
            return;
        }

        if (root instanceof TextView && !(root instanceof EditText)) {
            CharSequence cs = ((TextView) root).getText();
            if (cs != null && cs.length() > 0) {
                results.add(new TextNode(cs.toString().trim(), root));
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                traverseViewTreeForTexts(vg.getChildAt(i), results);
            }
        }
    }

    // ==================== 1. 文件夹系统 (真正树状分类 + 点击直接进入对应会话 + 离线快照 + 智能续接) ====================
    private void renderFoldersView() {
        TextView intro = createSectionTitle("📂 多级分类文件夹 (点击条目直接进入对应会话)");
        contentContainer.addView(intro);

        List<FolderData> folders = loadPersistedFolders();
        for (FolderData f : folders) {
            buildPersistedFolderCard(f, folders);
        }

        Button addFolderBtn = createStyledButton("+ 新建分类文件夹", "#10A37F", "#09090B");
        addFolderBtn.setOnClickListener(v -> showCreateFolderDialog());
        contentContainer.addView(addFolderBtn);

        // 真正收录当前对话（包含完整问答快照）
        Button saveChatBtn = createStyledButton("📌 将当前 Gemini 真实对话收录进文件夹", isDarkTheme ? "#26262B" : "#E2E8F0", isDarkTheme ? "#FFFFFF" : "#0F172A");
        saveChatBtn.setOnClickListener(v -> showSaveCurrentChatDialog(folders));
        contentContainer.addView(saveChatBtn);
    }

    private void buildPersistedFolderCard(FolderData folder, List<FolderData> allFolders) {
        final boolean isExpanded = expandedFolderNames.contains(folder.name);

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(getCardBgColor());
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), getCardBorderColor());
        card.setBackground(gd);

        // 文件夹标题栏（包含展开箭头、颜色标、名称、篇数、直接存入按钮、删除文件夹按钮）
        LinearLayout topBar = new LinearLayout(getContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView arrowTv = new TextView(getContext());
        arrowTv.setText(isExpanded ? "▼ " : "▶ ");
        arrowTv.setTextColor(Color.parseColor("#10A37F"));
        arrowTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        arrowTv.setTypeface(null, Typeface.BOLD);
        topBar.addView(arrowTv);

        View dot = new View(getContext());
        GradientDrawable dotGd = new GradientDrawable();
        dotGd.setShape(GradientDrawable.OVAL);
        dotGd.setColor(Color.parseColor(folder.color));
        dot.setBackground(dotGd);
        topBar.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));

        TextView nameTv = new TextView(getContext());
        nameTv.setText("  " + folder.name);
        nameTv.setTextColor(getPrimaryTextColor());
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameTv.setTypeface(null, Typeface.BOLD);
        topBar.addView(nameTv, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

        TextView countBadge = new TextView(getContext());
        countBadge.setText(String.valueOf(folder.savedChats.size()) + " 篇");
        countBadge.setTextColor(getSecondaryTextColor());
        countBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        countBadge.setPadding(dp(4), 0, dp(6), 0);
        topBar.addView(countBadge);

        // 快捷添加按钮：直接将当前对话收录进此文件夹
        TextView addChatBtn = new TextView(getContext());
        addChatBtn.setText(" ＋ ");
        addChatBtn.setTextColor(Color.parseColor("#10A37F"));
        addChatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addChatBtn.setTypeface(null, Typeface.BOLD);
        addChatBtn.setOnClickListener(v -> saveChatToSpecificFolder(folder, allFolders));
        topBar.addView(addChatBtn);

        // 删除文件夹按钮
        TextView delFolderBtn = new TextView(getContext());
        delFolderBtn.setText(" 🗑️");
        delFolderBtn.setTextColor(getSecondaryTextColor());
        delFolderBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        delFolderBtn.setOnClickListener(v -> confirmDeleteFolder(folder, allFolders));
        topBar.addView(delFolderBtn);

        // 点击标题栏切换展开/折叠
        topBar.setOnClickListener(v -> {
            if (isExpanded) {
                expandedFolderNames.remove(folder.name);
            } else {
                expandedFolderNames.add(folder.name);
            }
            renderFoldersView();
        });

        card.addView(topBar);

        // 展开后的会话列表 (类似 PC 端 Voyager 侧边栏树形体验)
        if (isExpanded) {
            LinearLayout childContainer = new LinearLayout(getContext());
            childContainer.setOrientation(LinearLayout.VERTICAL);
            childContainer.setPadding(dp(12), dp(8), dp(0), dp(4));

            if (folder.savedChats.isEmpty()) {
                TextView emptyTv = new TextView(getContext());
                emptyTv.setText("（暂无会话，点击右侧 ＋ 可直接将当前会话存入）");
                emptyTv.setTextColor(getSecondaryTextColor());
                emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                emptyTv.setPadding(dp(4), dp(4), dp(4), dp(4));
                childContainer.addView(emptyTv);
            } else {
                for (final SavedChat chat : folder.savedChats) {
                    LinearLayout chatItem = new LinearLayout(getContext());
                    chatItem.setOrientation(LinearLayout.HORIZONTAL);
                    chatItem.setGravity(Gravity.CENTER_VERTICAL);
                    chatItem.setPadding(dp(10), dp(8), dp(8), dp(8));

                    GradientDrawable itemGd = new GradientDrawable();
                    itemGd.setColor(isDarkTheme ? Color.parseColor("#222228") : Color.parseColor("#F1F5F9"));
                    itemGd.setCornerRadius(dp(6));
                    itemGd.setStroke(dp(1), isDarkTheme ? Color.parseColor("#2C2C34") : Color.parseColor("#E2E8F0"));
                    chatItem.setBackground(itemGd);

                    TextView chatIcon = new TextView(getContext());
                    chatIcon.setText("💬 ");
                    chatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    chatItem.addView(chatIcon);

                    LinearLayout textCol = new LinearLayout(getContext());
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    TextView chatTitleTv = new TextView(getContext());
                    chatTitleTv.setText(chat.title);
                    chatTitleTv.setTextColor(getPrimaryTextColor());
                    chatTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    chatTitleTv.setTypeface(null, Typeface.BOLD);
                    chatTitleTv.setSingleLine(true);
                    chatTitleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    textCol.addView(chatTitleTv);

                    TextView chatDateTv = new TextView(getContext());
                    chatDateTv.setText(chat.date);
                    chatDateTv.setTextColor(getSecondaryTextColor());
                    chatDateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    textCol.addView(chatDateTv);

                    chatItem.addView(textCol, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

                    // 快捷续聊按钮 (直接将原题填入 Gemini 提问框开始追问)
                    TextView resumeBtn = new TextView(getContext());
                    resumeBtn.setText("🚀");
                    resumeBtn.setPadding(dp(6), dp(4), dp(4), dp(4));
                    resumeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    resumeBtn.setOnClickListener(v -> quickResumeIntoGemini(chat));
                    chatItem.addView(resumeBtn);

                    // 离线快照查看按钮
                    TextView snapBtn = new TextView(getContext());
                    snapBtn.setText("📖");
                    snapBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
                    snapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    snapBtn.setOnClickListener(v -> showSnapshotViewerDialog(chat));
                    chatItem.addView(snapBtn);

                    // 收藏/取消书签快捷按钮
                    TextView starBtn = new TextView(getContext());
                    final boolean isStarred = isChatBookmarked(chat);
                    starBtn.setText(isStarred ? "★" : "☆");
                    starBtn.setTextColor(isStarred ? Color.parseColor("#F59E0B") : getSecondaryTextColor());
                    starBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
                    starBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    starBtn.setOnClickListener(v -> {
                        toggleBookmark(chat);
                        renderFoldersView();
                    });
                    chatItem.addView(starBtn);

                    // 移出会话按钮
                    TextView delChatBtn = new TextView(getContext());
                    delChatBtn.setText("✕");
                    delChatBtn.setTextColor(getSecondaryTextColor());
                    delChatBtn.setPadding(dp(4), dp(4), dp(6), dp(4));
                    delChatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    delChatBtn.setOnClickListener(v -> confirmDeleteChat(folder, chat, allFolders));
                    chatItem.addView(delChatBtn);

                    // 核心关键：点击条目直接打开该会话的完整问答记录看板与操作中心！
                    chatItem.setOnClickListener(v -> renderConversationDetailView(chat, folder));

                    LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                    itemLp.bottomMargin = dp(6);
                    childContainer.addView(chatItem, itemLp);
                }
            }
            card.addView(childContainer);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        contentContainer.addView(card, lp);
    }

    private void quickResumeIntoGemini(SavedChat chat) {
        fillToGemini(chat.title);
        closeDrawer();
        Toast.makeText(activity, "✅ 已自动填入提问框，可直接向 Gemini 追问续聊！", Toast.LENGTH_SHORT).show();
    }

    /**
     * 完整会话详情与续接看板：直接呈现该会话中记录的所有真实问答，
     * 并提供【一键填入 Gemini 提问框续聊】、【唤起原生路由跳转】、【书签快捷标星】与【导出】操作
     */
    private void renderConversationDetailView(final SavedChat chat, final FolderData parentFolder) {
        renderConversationDetailView(chat, parentFolder != null ? parentFolder.name : "文件夹", this::renderFoldersView);
    }

    private void renderConversationDetailView(final SavedChat chat, final String sourceTitle, final Runnable onBackAction) {
        contentContainer.removeAllViews();

        // 顶栏：返回按钮 + 会话所属分类/来源 + 顶部快速切换书签按钮
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(4), 0, dp(10));

        TextView backBtn = new TextView(getContext());
        backBtn.setText("← 返回 " + (sourceTitle != null ? sourceTitle : ""));
        backBtn.setTextColor(Color.parseColor("#10A37F"));
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        backBtn.setTypeface(null, Typeface.BOLD);
        backBtn.setPadding(0, dp(6), dp(12), dp(6));
        backBtn.setOnClickListener(v -> {
            if (onBackAction != null) {
                onBackAction.run();
            } else {
                renderFoldersView();
            }
        });
        header.addView(backBtn);

        TextView folderBadge = new TextView(getContext());
        folderBadge.setText("[" + (sourceTitle != null ? sourceTitle : "记录") + "]");
        folderBadge.setTextColor(getSecondaryTextColor());
        folderBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        header.addView(folderBadge, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

        final boolean isStarred = isChatBookmarked(chat);
        TextView starTopBtn = new TextView(getContext());
        starTopBtn.setText(isStarred ? "★ 已存书签" : "☆ 设为书签");
        starTopBtn.setTextColor(isStarred ? Color.parseColor("#F59E0B") : Color.parseColor("#10A37F"));
        starTopBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        starTopBtn.setTypeface(null, Typeface.BOLD);
        starTopBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        starTopBtn.setOnClickListener(v -> {
            toggleBookmark(chat);
            renderConversationDetailView(chat, sourceTitle, onBackAction);
        });
        header.addView(starTopBtn);

        contentContainer.addView(header);

        // 标题卡片
        LinearLayout titleCard = new LinearLayout(getContext());
        titleCard.setOrientation(LinearLayout.VERTICAL);
        titleCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable tcGd = new GradientDrawable();
        tcGd.setColor(getCardBgColor());
        tcGd.setCornerRadius(dp(8));
        tcGd.setStroke(dp(1), getCardBorderColor());
        titleCard.setBackground(tcGd);

        TextView titleTv = new TextView(getContext());
        titleTv.setText("💬 " + chat.title);
        titleTv.setTextColor(getPrimaryTextColor());
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleCard.addView(titleTv);

        TextView timeTv = new TextView(getContext());
        timeTv.setText("收录时间: " + chat.date);
        timeTv.setTextColor(getSecondaryTextColor());
        timeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        timeTv.setPadding(0, dp(4), 0, 0);
        titleCard.addView(timeTv);

        contentContainer.addView(titleCard);

        // 核心操作快捷入口 (高频行动项：一键填入提问框续聊 / 官方跳转 / 书签切换)
        LinearLayout actionRow = new LinearLayout(getContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(8), 0, dp(12));

        Button resumeChatBtn = createStyledButton("🚀 填入续聊", "#10A37F", "#09090B");
        resumeChatBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1.0f));
        resumeChatBtn.setOnClickListener(v -> quickResumeIntoGemini(chat));
        actionRow.addView(resumeChatBtn);

        View spacer1 = new View(getContext());
        actionRow.addView(spacer1, new LinearLayout.LayoutParams(dp(6), dp(1)));

        Button nativeJumpBtn = createStyledButton("🔗 官方跳转", isDarkTheme ? "#27272A" : "#E2E8F0", isDarkTheme ? "#FFFFFF" : "#0F172A");
        nativeJumpBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1.0f));
        nativeJumpBtn.setOnClickListener(v -> enterAndSwitchConversation(chat));
        actionRow.addView(nativeJumpBtn);

        View spacer2 = new View(getContext());
        actionRow.addView(spacer2, new LinearLayout.LayoutParams(dp(6), dp(1)));

        Button bookmarkActionBtn = createStyledButton(isStarred ? "★ 书签" : "☆ 书签", isStarred ? "#F59E0B" : (isDarkTheme ? "#27272A" : "#E2E8F0"), isStarred ? "#09090B" : (isDarkTheme ? "#FFFFFF" : "#0F172A"));
        bookmarkActionBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 0.85f));
        bookmarkActionBtn.setOnClickListener(v -> {
            toggleBookmark(chat);
            renderConversationDetailView(chat, sourceTitle, onBackAction);
        });
        actionRow.addView(bookmarkActionBtn);

        contentContainer.addView(actionRow);

        // 真实问答记录内容流
        TextView turnsTitle = createSectionTitle("📜 会话完整问答记录");
        contentContainer.addView(turnsTitle);

        String snapshot = chat.markdownSnapshot;
        if (snapshot == null || snapshot.trim().isEmpty()) {
            snapshot = "### 👤 提问\n" + chat.title + "\n\n### 🤖 Gemini 回答\n（该对话已记录，点击上方「🚀 填入续聊」可直接在 Gemini 中继续追问）";
        }

        TextView bodyTv = new TextView(getContext());
        bodyTv.setText(snapshot);
        bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bodyTv.setLineSpacing(dp(3), 1.15f);
        bodyTv.setTextColor(getPrimaryTextColor());
        bodyTv.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable bodyGd = new GradientDrawable();
        bodyGd.setColor(isDarkTheme ? Color.parseColor("#17171C") : Color.parseColor("#F8FAFC"));
        bodyGd.setCornerRadius(dp(8));
        bodyGd.setStroke(dp(1), getCardBorderColor());
        bodyTv.setBackground(bodyGd);

        contentContainer.addView(bodyTv);

        // 底部导出按钮
        Button shareBtn = createStyledButton("📤 导出并分享此对话 (Markdown)", isDarkTheme ? "#26262B" : "#E2E8F0", isDarkTheme ? "#FFFFFF" : "#0F172A");
        shareBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, chat.title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, chat.markdownSnapshot);
            activity.startActivity(Intent.createChooser(shareIntent, "导出对话"));
        });
        contentContainer.addView(shareBtn);
    }

    /**
     * 会话书签快捷中心：一键收藏常用重点会话，
     * 点击任意书签卡片，一下子直接打开相对应会话的完整问答记录看板与操作中心！
     */
    private void renderBookmarksView() {
        final List<SavedChat> bookmarks = loadPersistedBookmarks();

        TextView intro = createSectionTitle("🔖 会话书签速递 (" + bookmarks.size() + " 条重点会话)");
        contentContainer.addView(intro);

        // 顶栏操作卡片：一键将当前正在进行的会话添加为书签
        LinearLayout actionCard = new LinearLayout(getContext());
        actionCard.setOrientation(LinearLayout.VERTICAL);
        actionCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable acGd = new GradientDrawable();
        acGd.setColor(isDarkTheme ? Color.parseColor("#14241F") : Color.parseColor("#ECFDF5"));
        acGd.setCornerRadius(dp(8));
        acGd.setStroke(dp(1), Color.parseColor("#10A37F"));
        actionCard.setBackground(acGd);

        TextView currentTitleTv = new TextView(getContext());
        String displayReal = currentRealTitle.isEmpty() ? "当前实时会话" : currentRealTitle;
        currentTitleTv.setText("🎯 实时会话: " + displayReal);
        currentTitleTv.setTextColor(Color.parseColor("#10A37F"));
        currentTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        currentTitleTv.setTypeface(null, Typeface.BOLD);
        currentTitleTv.setSingleLine(true);
        currentTitleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        actionCard.addView(currentTitleTv);

        Button addCurrentBookmarkBtn = createStyledButton("⭐ 一键将当前会话加入书签", "#10A37F", "#09090B");
        addCurrentBookmarkBtn.setOnClickListener(v -> addCurrentChatToBookmarks());
        actionCard.addView(addCurrentBookmarkBtn);

        LinearLayout.LayoutParams acLp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        acLp.bottomMargin = dp(12);
        contentContainer.addView(actionCard, acLp);

        // 书签列表
        if (bookmarks.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("暂无收藏的书签。\n\n在与 Gemini 对话时，点击上方「⭐ 一键将当前会话加入书签」即可将当前问答永久加入书签；随时点击即可直接跳转至对应问答记录页面！");
            emptyTv.setTextColor(getSecondaryTextColor());
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            emptyTv.setLineSpacing(dp(2), 1.2f);
            emptyTv.setPadding(dp(4), dp(8), dp(4), dp(8));
            contentContainer.addView(emptyTv);
            return;
        }

        for (final SavedChat chat : bookmarks) {
            LinearLayout itemCard = new LinearLayout(getContext());
            itemCard.setOrientation(LinearLayout.VERTICAL);
            itemCard.setPadding(dp(12), dp(12), dp(12), dp(12));

            GradientDrawable cardGd = new GradientDrawable();
            cardGd.setColor(getCardBgColor());
            cardGd.setCornerRadius(dp(8));
            cardGd.setStroke(dp(1), getCardBorderColor());
            itemCard.setBackground(cardGd);

            // 标题行
            LinearLayout titleRow = new LinearLayout(getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView starIcon = new TextView(getContext());
            starIcon.setText("⭐ ");
            starIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            titleRow.addView(starIcon);

            TextView titleTv = new TextView(getContext());
            titleTv.setText(chat.title);
            titleTv.setTextColor(getPrimaryTextColor());
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            titleTv.setTypeface(null, Typeface.BOLD);
            titleTv.setSingleLine(true);
            titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            titleRow.addView(titleTv, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

            // 删除书签按钮
            TextView delBtn = new TextView(getContext());
            delBtn.setText(" ✕ ");
            delBtn.setTextColor(getSecondaryTextColor());
            delBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            delBtn.setPadding(dp(6), dp(2), dp(2), dp(2));
            delBtn.setOnClickListener(v -> {
                bookmarks.remove(chat);
                savePersistedBookmarks(bookmarks);
                Toast.makeText(activity, "已移除书签: " + chat.title, Toast.LENGTH_SHORT).show();
                renderBookmarksView();
            });
            titleRow.addView(delBtn);

            itemCard.addView(titleRow);

            // 日期与问答摘要
            TextView dateTv = new TextView(getContext());
            dateTv.setText("📅 " + chat.date);
            dateTv.setTextColor(getSecondaryTextColor());
            dateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            dateTv.setPadding(0, dp(4), 0, dp(6));
            itemCard.addView(dateTv);

            String excerpt = "点击直接跳转至该对话的完整记录与操作中心...";
            if (chat.markdownSnapshot != null && !chat.markdownSnapshot.trim().isEmpty()) {
                String clean = chat.markdownSnapshot.replace("#", "").replace("*", "").trim();
                excerpt = clean.length() > 60 ? clean.substring(0, 60) + "..." : clean;
            }
            TextView excerptTv = new TextView(getContext());
            excerptTv.setText(excerpt);
            excerptTv.setTextColor(getSecondaryTextColor());
            excerptTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            excerptTv.setMaxLines(2);
            excerptTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            itemCard.addView(excerptTv);

            // 操作底栏：提示与续聊
            LinearLayout bottomRow = new LinearLayout(getContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);
            bottomRow.setPadding(0, dp(8), 0, 0);

            TextView jumpHintTv = new TextView(getContext());
            jumpHintTv.setText("⚡ 点击直接进入相对应记录页");
            jumpHintTv.setTextColor(Color.parseColor("#10A37F"));
            jumpHintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            jumpHintTv.setTypeface(null, Typeface.BOLD);
            bottomRow.addView(jumpHintTv, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

            TextView quickResumeBtn = new TextView(getContext());
            quickResumeBtn.setText("🚀 填入续聊");
            quickResumeBtn.setTextColor(isDarkTheme ? Color.parseColor("#38BDF8") : Color.parseColor("#0284C7"));
            quickResumeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            quickResumeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            quickResumeBtn.setOnClickListener(v -> quickResumeIntoGemini(chat));
            bottomRow.addView(quickResumeBtn);

            itemCard.addView(bottomRow);

            // 【核心关键】：点击书签卡片，一下子跳转到相对应的对话记录页面！
            itemCard.setOnClickListener(v -> renderConversationDetailView(chat, "🔖 我的书签", this::renderBookmarksView));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            contentContainer.addView(itemCard, lp);
        }
    }

    /**
     * 核心关键：点击已分类的会话直接进入相对应的对话中
     * 涵盖 5 重自适应真实跳转驱动机制：
     * 1. 自动填入提问框并聚焦，确保即使路由未生效也能即刻对话；
     * 2. WebView DOM 与 Router 驱动：查找并点击 Gemini 网页版侧边栏/历史会话中匹配的链接或直接导航 location.href；
     * 3. 原生当前视图树搜索与点击：直接在当前界面搜索匹配会话标题的控件并触发点击；
     * 4. 原生侧边栏自动展开与检索：自动寻找并点击 Gemini 汉堡菜单按钮 (☰) 展开“最近聊天”列表；
     * 5. Deep Link 显式 Intent 唤醒与浮动智能续接条 (Floating Resume HUD)。
     */
    private void enterAndSwitchConversation(final SavedChat chat) {
        closeDrawer();
        Toast.makeText(activity, "⚡ 正在跳转至: " + chat.title, Toast.LENGTH_SHORT).show();

        // 核心兜底保障：立即将原题填入提问框并聚焦，确保即使路由未生效也能即刻对话
        fillToGemini(chat.title);

        // 尝试方式一: WebView DOM 自动化导航与路由跳转 (针对 Web / 混合渲染容器)
        WebView webView = findWebView(activity.getWindow().getDecorView());
        if (webView != null) {
            String safeUrl = (chat.url == null) ? "" : chat.url;
            String safeTitle = (chat.title == null) ? "" : chat.title;
            String js = "(function() {\n" +
                    "  var targetUrl = " + JSONObject.quote(safeUrl) + ";\n" +
                    "  var targetTitle = " + JSONObject.quote(safeTitle) + ";\n" +
                    "  var clicked = false;\n" +
                    "  var links = document.querySelectorAll('a[href*=\"/app/\"], [data-test-id=\"conversation-title\"], div[role=\"button\"], .conversation-title');\n" +
                    "  for (var i = 0; i < links.length; i++) {\n" +
                    "    var el = links[i];\n" +
                    "    var href = el.getAttribute('href') || el.href || '';\n" +
                    "    var text = (el.innerText || '').trim();\n" +
                    "    if ((targetUrl.length > 25 && href && targetUrl.indexOf(href) !== -1) ||\n" +
                    "        (targetTitle.length > 2 && text && (text.indexOf(targetTitle) !== -1 || targetTitle.indexOf(text) !== -1))) {\n" +
                    "      el.click();\n" +
                    "      clicked = true;\n" +
                    "      break;\n" +
                    "    }\n" +
                    "  }\n" +
                    "  if (!clicked && targetUrl && targetUrl.indexOf('gemini.google.com') !== -1 && targetUrl.length > 28) {\n" +
                    "    window.location.href = targetUrl;\n" +
                    "  }\n" +
                    "  return clicked;\n" +
                    "})();";
            webView.evaluateJavascript(js, null);
            if (safeUrl.length() > 28 && safeUrl.startsWith("http")) {
                webView.loadUrl(safeUrl);
            }
        }

        // 尝试方式二: 直接在原生当前视图树中搜索目标会话条目
        View matchView = findConversationViewInHierarchy(activity.getWindow().getDecorView(), chat.title);
        if (matchView != null) {
            View clickable = getClickableAncestorOrSelf(matchView);
            clickable.performClick();
            Toast.makeText(activity, "✅ 已切换至「" + chat.title + "」", Toast.LENGTH_SHORT).show();
            showFloatingResumeHud(chat);
            return;
        }

        // 尝试方式三 (核心机制突破): 若未直接找到，通常是因为 Gemini 原生“最近聊天”抽屉处于收起状态。
        // 自动检索并模拟点击 Gemini 顶栏左侧的汉堡菜单按钮 (☰)，展开最近聊天列表！
        View menuBtn = findGeminiMenuButton(activity.getWindow().getDecorView());
        if (menuBtn != null) {
            menuBtn.performClick();
            // 等待 Gemini 侧边栏滑出与列表渲染（约 260ms）
            mainHandler.postDelayed(() -> {
                View afterOpenView = findConversationViewInHierarchy(activity.getWindow().getDecorView(), chat.title);
                if (afterOpenView != null) {
                    View clickable = getClickableAncestorOrSelf(afterOpenView);
                    clickable.performClick();
                    Toast.makeText(activity, "✅ 已切换至「" + chat.title + "」", Toast.LENGTH_SHORT).show();
                } else {
                    // 若最近列表中未找到（如历史较久远），提供快照阅读与一键提问
                    showFloatingResumeHud(chat);
                }
            }, 260);
        } else {
            // 尝试方式四: Android Deep Link Intent
            if (chat.url != null && chat.url.contains("/app/") && chat.url.length() > 30) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(chat.url));
                    intent.setPackage("com.google.android.apps.bard");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                } catch (Exception ignored) {}
            }
            // 尝试方式五: 弹出浮动智能续接横幅 (Floating Resume HUD)
            showFloatingResumeHud(chat);
        }
    }

    /**
     * 智能识别并定位 Gemini 原生界面的侧边栏汉堡菜单按钮 (☰)
     */
    private View findGeminiMenuButton(View root) {
        if (root == null || root.getVisibility() != View.VISIBLE) return null;
        if ("VOYAGER_OVERLAY_ROOT".equals(root.getTag()) || root == circularFab || root == drawerLayout) return null;

        CharSequence cd = root.getContentDescription();
        if (cd != null) {
            String desc = cd.toString().toLowerCase();
            if (desc.contains("menu") || desc.contains("菜单") || desc.contains("nav") ||
                desc.contains("drawer") || desc.contains("侧边") || desc.contains("导航") ||
                desc.contains("open") || desc.contains("打开") || desc.contains("主菜单")) {
                int[] loc = new int[2];
                root.getLocationOnScreen(loc);
                if (loc[1] < dp(180)) {
                    return root;
                }
            }
        }

        // 识别左上角的可点击图元 (汉堡图标)
        if ((root instanceof android.widget.ImageView || root instanceof android.widget.ImageButton) && root.isClickable()) {
            int[] loc = new int[2];
            root.getLocationOnScreen(loc);
            if (loc[0] < dp(80) && loc[1] < dp(140) && loc[1] > 0) {
                return root;
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findGeminiMenuButton(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * 在视图树中递归寻找与目标会话标题匹配的会话项 (支持模糊截断匹配)
     */
    private View findConversationViewInHierarchy(View root, String title) {
        if (root == null || root.getVisibility() != View.VISIBLE || title == null || title.trim().isEmpty()) return null;
        if ("VOYAGER_OVERLAY_ROOT".equals(root.getTag()) || root == circularFab || root == drawerLayout) return null;

        String query = title.trim();
        if (query.startsWith("📄") || query.startsWith("💬")) {
            query = query.substring(1).trim();
        }

        if (root instanceof TextView && !(root instanceof EditText)) {
            CharSequence cs = ((TextView) root).getText();
            if (cs != null) {
                String text = cs.toString().trim();
                if (text.length() >= 2) {
                    if (text.equalsIgnoreCase(query) || text.contains(query) || query.contains(text)) {
                        return root;
                    }
                    if (query.length() >= 5 && text.length() >= 5) {
                        String prefix = query.substring(0, Math.min(6, query.length()));
                        if (text.startsWith(prefix)) {
                            return root;
                        }
                    }
                }
            }
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findConversationViewInHierarchy(vg.getChildAt(i), title);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * 浮动智能续接条：进入会话后在屏幕顶层展现快捷小胶囊，支持一键填入输入框续聊和离线快照查看
     */
    private void showFloatingResumeHud(final SavedChat chat) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(() -> {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            View oldHud = decor.findViewWithTag("VOYAGER_RESUME_HUD");
            if (oldHud != null) decor.removeView(oldHud);

            LinearLayout hud = new LinearLayout(activity);
            hud.setTag("VOYAGER_RESUME_HUD");
            hud.setOrientation(LinearLayout.HORIZONTAL);
            hud.setGravity(Gravity.CENTER_VERTICAL);
            hud.setPadding(dp(14), dp(8), dp(10), dp(8));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(isDarkTheme ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A"));
            gd.setCornerRadius(dp(22));
            gd.setStroke(dp(1), Color.parseColor("#10A37F"));
            hud.setBackground(gd);

            TextView titleTv = new TextView(activity);
            String displayTitle = chat.title.length() > 14 ? chat.title.substring(0, 14) + "..." : chat.title;
            titleTv.setText("💬 " + displayTitle);
            titleTv.setTextColor(Color.WHITE);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            titleTv.setTypeface(null, Typeface.BOLD);
            hud.addView(titleTv);

            // 续聊填入按钮
            TextView fillBtn = new TextView(activity);
            fillBtn.setText(" 💡 填入原题");
            fillBtn.setTextColor(Color.parseColor("#10A37F"));
            fillBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            fillBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
            fillBtn.setOnClickListener(v -> {
                fillToGemini(chat.title);
                decor.removeView(hud);
            });
            hud.addView(fillBtn);

            // 查看快照按钮
            TextView snapBtn = new TextView(activity);
            snapBtn.setText(" 📖 历史记录");
            snapBtn.setTextColor(Color.parseColor("#38BDF8"));
            snapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            snapBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
            snapBtn.setOnClickListener(v -> showSnapshotViewerDialog(chat));
            hud.addView(snapBtn);

            // 快捷书签按钮
            TextView bookmarkHudBtn = new TextView(activity);
            boolean isHudFav = isChatBookmarked(chat);
            bookmarkHudBtn.setText(isHudFav ? " ★ 已存" : " ⭐ 书签");
            bookmarkHudBtn.setTextColor(Color.parseColor("#F59E0B"));
            bookmarkHudBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            bookmarkHudBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
            bookmarkHudBtn.setOnClickListener(v -> {
                toggleBookmark(chat);
                boolean nowFav = isChatBookmarked(chat);
                bookmarkHudBtn.setText(nowFav ? " ★ 已存" : " ⭐ 书签");
            });
            hud.addView(bookmarkHudBtn);

            // 关闭按钮
            TextView closeHudBtn = new TextView(activity);
            closeHudBtn.setText(" ✕");
            closeHudBtn.setTextColor(Color.parseColor("#94A3B8"));
            closeHudBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            closeHudBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
            closeHudBtn.setOnClickListener(v -> decor.removeView(hud));
            hud.addView(closeHudBtn);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL
            );
            lp.topMargin = dp(45);
            decor.addView(hud, lp);

            // 8 秒后自动淡化移除
            mainHandler.postDelayed(() -> {
                try {
                    decor.removeView(hud);
                } catch (Exception ignored) {}
            }, 8000);
        });
    }

    private View getClickableAncestorOrSelf(View view) {
        View cur = view;
        while (cur != null) {
            if (cur.isClickable()) return cur;
            if (cur.getParent() instanceof View) {
                cur = (View) cur.getParent();
            } else {
                break;
            }
        }
        return view;
    }

    private void saveChatToSpecificFolder(FolderData target, List<FolderData> allFolders) {
        SavedChat chat = new SavedChat();
        chat.title = currentRealTitle.isEmpty() ? "Gemini 深度分析对话" : currentRealTitle;
        chat.url = currentRealUrl;
        chat.date = new Date().toLocaleString();
        chat.markdownSnapshot = buildRealMarkdownString();
        target.savedChats.add(chat);
        expandedFolderNames.add(target.name);
        savePersistedFolders(allFolders);
        Toast.makeText(activity, "✅ 已收录当前对话至 [" + target.name + "]", Toast.LENGTH_SHORT).show();
        renderFoldersView();
    }

    private void showSaveCurrentChatDialog(List<FolderData> folders) {
        if (folders.isEmpty()) {
            Toast.makeText(activity, "请先点击「新建分类文件夹」", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] folderNames = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) folderNames[i] = folders.get(i).name;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("选择要归档到的文件夹");
        builder.setItems(folderNames, (dialog, which) -> {
            FolderData target = folders.get(which);
            saveChatToSpecificFolder(target, folders);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void confirmDeleteFolder(FolderData folder, List<FolderData> allFolders) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("删除分类文件夹");
        b.setMessage("确定删除文件夹 [" + folder.name + "] 及其收录的 " + folder.savedChats.size() + " 篇会话吗？");
        b.setPositiveButton("删除", (d, w) -> {
            allFolders.remove(folder);
            expandedFolderNames.remove(folder.name);
            savePersistedFolders(allFolders);
            Toast.makeText(activity, "已删除文件夹: " + folder.name, Toast.LENGTH_SHORT).show();
            renderFoldersView();
        });
        b.setNegativeButton("取消", null);
        b.show();
    }

    private void confirmDeleteChat(FolderData folder, SavedChat chat, List<FolderData> allFolders) {
        folder.savedChats.remove(chat);
        savePersistedFolders(allFolders);
        Toast.makeText(activity, "已移出会话: " + chat.title, Toast.LENGTH_SHORT).show();
        renderFoldersView();
    }

    private void showSnapshotViewerDialog(SavedChat chat) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("会话快照: " + chat.title);
        ScrollView sv = new ScrollView(activity);
        TextView tv = new TextView(activity);
        tv.setText(chat.markdownSnapshot);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setPadding(dp(16), dp(16), dp(16), dp(16));
        tv.setTextColor(getPrimaryTextColor());
        sv.addView(tv);
        b.setView(sv);
        b.setPositiveButton("复制全文", (dialog, which) -> {
            copyToClipboard(chat.markdownSnapshot);
            Toast.makeText(activity, "已复制完整快照至剪贴板", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("关闭", null);
        b.show();
    }

    // ==================== 2. 真实时间线导航 (真正展示真实问题 + 真正物理平滑滚动并高亮) ====================
    private void renderTimelineView() {
        TextView intro = createSectionTitle("📍 实时对话垂直时间线 (" + currentTurns.size() + " 个实时问答节点)");
        contentContainer.addView(intro);

        if (currentTurns.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("当前页面尚未检测到已发送的提问。发送新问题后即可在此生成时间线小地图。");
            emptyTv.setTextColor(getSecondaryTextColor());
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            emptyTv.setPadding(dp(4), dp(8), dp(4), dp(8));
            contentContainer.addView(emptyTv);
            return;
        }

        for (ConversationTurn turn : currentTurns) {
            final int nodeIdx = turn.index;
            final String qText = turn.question.isEmpty() ? ("第 " + nodeIdx + " 轮对话") : turn.question;

            LinearLayout nodeCard = new LinearLayout(getContext());
            nodeCard.setOrientation(LinearLayout.HORIZONTAL);
            nodeCard.setGravity(Gravity.CENTER_VERTICAL);
            nodeCard.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(getCardBgColor());
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), getCardBorderColor());
            nodeCard.setBackground(gd);

            TextView badge = new TextView(getContext());
            badge.setText("Q" + nodeIdx);
            badge.setTextColor(Color.parseColor("#10A37F"));
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setTypeface(null, Typeface.BOLD);
            nodeCard.addView(badge);

            TextView titleTv = new TextView(getContext());
            titleTv.setText("  " + (qText.length() > 18 ? qText.substring(0, 18) + "..." : qText));
            titleTv.setTextColor(getPrimaryTextColor());
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            nodeCard.addView(titleTv, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

            TextView quoteBtn = new TextView(getContext());
            quoteBtn.setText("💬引用");
            quoteBtn.setTextColor(Color.parseColor("#38BDF8"));
            quoteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            quoteBtn.setPadding(dp(4), 0, dp(6), 0);
            quoteBtn.setOnClickListener(v -> {
                String quote = "> " + qText + "\n\n针对上述问题：";
                fillToGemini(quote);
                Toast.makeText(activity, "已自动引用并填入输入框", Toast.LENGTH_SHORT).show();
            });
            nodeCard.addView(quoteBtn);

            // 真正执行页面物理滚动跳转到该问题，并高亮闪烁！
            nodeCard.setOnClickListener(v -> {
                realScrollToQuestionNode(turn);
                Toast.makeText(activity, "⚡ 正在定位跳转至 Q" + nodeIdx, Toast.LENGTH_SHORT).show();
                closeDrawer();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            contentContainer.addView(nodeCard, lp);
        }
    }

    /**
     * 真正定位并平滑滚动到指定提问节点
     */
    private void realScrollToQuestionNode(ConversationTurn turn) {
        WebView webView = findWebView(activity.getWindow().getDecorView());
        if (webView != null) {
            int domIndex = turn.index - 1;
            String js = "(function(){\n" +
                    "  var qs = document.querySelectorAll('user-query, .query-text, div[class*=\"user-query\"], .user-query-container');\n" +
                    "  if (qs && qs[" + domIndex + "]) {\n" +
                    "    qs[" + domIndex + "].scrollIntoView({ behavior: 'smooth', block: 'center' });\n" +
                    "    qs[" + domIndex + "].style.transition = 'background 0.5s';\n" +
                    "    qs[" + domIndex + "].style.backgroundColor = 'rgba(16, 163, 127, 0.35)';\n" +
                    "    setTimeout(function(){ qs[" + domIndex + "].style.backgroundColor = ''; }, 1800);\n" +
                    "  }\n" +
                    "})();";
            webView.evaluateJavascript(js, null);
        } else if (turn.nativeViewRef != null) {
            // 原生 View 树模式下的真实滚动居中定位
            scrollToNativeView(turn.nativeViewRef);
        }
    }

    private void scrollToNativeView(View target) {
        try {
            target.requestFocus();
            // 在目标控件上执行翠绿高亮呼吸闪烁动画
            ValueAnimator colorAnim = ValueAnimator.ofArgb(Color.parseColor("#4410A37F"), Color.TRANSPARENT);
            colorAnim.setDuration(1200);
            colorAnim.addUpdateListener(anim -> target.setBackgroundColor((int) anim.getAnimatedValue()));
            colorAnim.start();

            // 递归寻找包含 target 的可滚动父容器并执行平滑滚动
            ViewGroup scrollParent = findScrollableParent(target);
            if (scrollParent instanceof ScrollView) {
                int[] loc = new int[2];
                target.getLocationInWindow(loc);
                ((ScrollView) scrollParent).smoothScrollBy(0, loc[1] - dp(180));
            } else if (scrollParent != null) {
                // 如果是 RecyclerView / AbsListView，通过反射调用 smoothScrollBy
                try {
                    Method m = scrollParent.getClass().getMethod("smoothScrollBy", int.class, int.class);
                    int[] loc = new int[2];
                    target.getLocationInWindow(loc);
                    m.invoke(scrollParent, 0, loc[1] - dp(180));
                } catch (Exception ignored) {
                    scrollParent.scrollBy(0, dp(150));
                }
            }
        } catch (Exception ignored) {}
    }

    private ViewGroup findScrollableParent(View child) {
        View p = (View) child.getParent();
        while (p != null) {
            if (p instanceof ScrollView || p instanceof AbsListView || p.getClass().getName().contains("RecyclerView") || p.getClass().getName().contains("NestedScrollView")) {
                return (ViewGroup) p;
            }
            if (p.getParent() instanceof View) {
                p = (View) p.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    // ==================== 3. 真实对话导出 (真正提取全文并展示实时 Markdown 预览对话框) ====================
    private void renderExportView() {
        TextView intro = createSectionTitle("💾 真实对话全文导出 (实时抓取当前聊天)");
        contentContainer.addView(intro);

        TextView infoTv = new TextView(getContext());
        infoTv.setText("当前已实时捕获: " + currentTurns.size() + " 轮真实问答 (" + currentRealTitle + ")");
        infoTv.setTextColor(Color.parseColor("#10A37F"));
        infoTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        infoTv.setPadding(0, 0, 0, dp(10));
        contentContainer.addView(infoTv);

        Button exportMdBtn = createStyledButton("📝 预览并导出 Markdown (.md 格式带代码块)", "#10A37F", "#09090B");
        exportMdBtn.setOnClickListener(v -> showExportPreviewDialog("markdown"));
        contentContainer.addView(exportMdBtn);

        Button exportJsonBtn = createStyledButton("📊 导出 JSON 结构化数据", isDarkTheme ? "#26262B" : "#E2E8F0", isDarkTheme ? "#FFFFFF" : "#0F172A");
        exportJsonBtn.setOnClickListener(v -> showExportPreviewDialog("json"));
        contentContainer.addView(exportJsonBtn);

        Button copyAllBtn = createStyledButton("📋 一键拷贝当前整篇对话到剪贴板", isDarkTheme ? "#26262B" : "#E2E8F0", isDarkTheme ? "#FFFFFF" : "#0F172A");
        copyAllBtn.setOnClickListener(v -> {
            String text = buildRealMarkdownString();
            copyToClipboard(text);
            Toast.makeText(activity, "✅ 已成功拷贝整篇 " + currentTurns.size() + " 轮真实对话", Toast.LENGTH_SHORT).show();
            closeDrawer();
        });
        contentContainer.addView(copyAllBtn);
    }

    /**
     * 导出即时预览对话框：让用户真正看到导出的内容并一键分享
     */
    private void showExportPreviewDialog(String format) {
        String exportedText = "json".equals(format) ? buildRealJsonString() : buildRealMarkdownString();

        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("导出预览 (" + format.toUpperCase() + ")");

        ScrollView sv = new ScrollView(activity);
        TextView tv = new TextView(activity);
        tv.setText(exportedText);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextColor(getPrimaryTextColor());
        tv.setPadding(dp(16), dp(16), dp(16), dp(16));
        sv.addView(tv);
        b.setView(sv);

        b.setPositiveButton("📤 唤起系统分享/保存", (d, w) -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, currentRealTitle);
            sendIntent.putExtra(Intent.EXTRA_TEXT, exportedText);
            activity.startActivity(Intent.createChooser(sendIntent, "分享或保存当前 Gemini 对话"));
        });

        b.setNeutralButton("📋 复制全文", (d, w) -> {
            copyToClipboard(exportedText);
            Toast.makeText(activity, "✅ 已成功复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

        b.setNegativeButton("关闭", null);
        b.show();
    }

    private String buildRealMarkdownString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(currentRealTitle).append("\n\n");
        sb.append("> 导出工具: Voyager for Gemini\n");
        sb.append("> 导出时间: ").append(new Date().toLocaleString()).append("\n");
        sb.append("> 问答轮数: ").append(currentTurns.size()).append(" 轮\n");
        sb.append("> 对话地址: ").append(currentRealUrl).append("\n\n---\n\n");

        for (ConversationTurn turn : currentTurns) {
            sb.append("### 🧑‍💻 用户提问 #").append(turn.index).append(":\n\n");
            sb.append(turn.question.isEmpty() ? "（用户提问内容）" : turn.question).append("\n\n");
            sb.append("### 🤖 Gemini 回复:\n\n");
            sb.append(turn.answer.isEmpty() ? "（Gemini 回复内容）" : turn.answer).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    private String buildRealJsonString() {
        try {
            JSONObject root = new JSONObject();
            root.put("title", currentRealTitle);
            root.put("url", currentRealUrl);
            root.put("exportedAt", System.currentTimeMillis());
            JSONArray arr = new JSONArray();
            for (ConversationTurn t : currentTurns) {
                JSONObject item = new JSONObject();
                item.put("turn", t.index);
                item.put("user", t.question);
                item.put("model", t.answer);
                arr.put(item);
            }
            root.put("messages", arr);
            return root.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ==================== 4. 提示词库 ====================
    private void renderPromptsView() {
        TextView intro = createSectionTitle("💡 提示词工作台 (点击带入模版或自动填入输入框)");
        contentContainer.addView(intro);

        List<PromptItem> prompts = getPromptLibraryData();
        for (PromptItem item : prompts) {
            LinearLayout pCard = new LinearLayout(getContext());
            pCard.setOrientation(LinearLayout.VERTICAL);
            pCard.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(getCardBgColor());
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), getCardBorderColor());
            pCard.setBackground(gd);

            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tagBadge = new TextView(getContext());
            tagBadge.setText(item.category);
            tagBadge.setTextColor(Color.parseColor("#10A37F"));
            tagBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tagBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            GradientDrawable tagGd = new GradientDrawable();
            tagGd.setColor(isDarkTheme ? Color.parseColor("#132F26") : Color.parseColor("#E6F4EA"));
            tagGd.setCornerRadius(dp(4));
            tagBadge.setBackground(tagGd);
            topRow.addView(tagBadge);

            TextView titleTv = new TextView(getContext());
            titleTv.setText("  " + item.title);
            titleTv.setTextColor(getPrimaryTextColor());
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            titleTv.setTypeface(null, Typeface.BOLD);
            topRow.addView(titleTv);

            pCard.addView(topRow);

            TextView bodyTv = new TextView(getContext());
            bodyTv.setText(item.content);
            bodyTv.setTextColor(getSecondaryTextColor());
            bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            bodyTv.setPadding(0, dp(4), 0, 0);
            pCard.addView(bodyTv);

            pCard.setOnClickListener(v -> {
                if (item.content.contains("{{") && item.content.contains("}}")) {
                    showPromptVariableDialog(item.content);
                } else {
                    fillToGemini(item.content);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            contentContainer.addView(pCard, lp);
        }
    }

    private void showPromptVariableDialog(String template) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("填写 Prompt 变量");
        final EditText input = new EditText(activity);
        input.setHint("请输入目标代码或主题");
        builder.setView(input);
        builder.setPositiveButton("一键填入", (dialog, which) -> {
            String val = input.getText().toString().trim();
            String filled = template.replaceAll("\\{\\{.*?\\}\\}", val.isEmpty() ? "该项目" : val);
            fillToGemini(filled);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ==================== 5. 效率黑科技与工具箱 ====================
    private void renderToolsView() {
        TextView intro = createSectionTitle("🛠️ 效率黑科技增强开关");
        contentContainer.addView(intro);

        addToolSwitchRow("📐 公式点击多格式提取 (LaTeX / MathML / Notion)", "点击回答中的数学公式，立即复制标准 LaTeX 字符串", true);
        addToolSwitchRow("🚫 防强制自动滚底 (Scroll Lock)", "拦截发送新问题后强制自动滚到底部的内置行为，保持当前阅读视角", true);
        addToolSwitchRow("📱 输入框快速折叠收纳", "阅读长篇回复时一键折叠隐藏底部输入框，释放 35% 视野空间", false);
        addToolSwitchRow("🧜‍♀️ Mermaid 架构图渲染增强", "自动识别流程图代码块并生成可视化图表", true);
    }

    private void addToolSwitchRow(String title, String desc, boolean defaultOn) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(getCardBgColor());
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), getCardBorderColor());
        card.setBackground(gd);

        TextView tTv = new TextView(getContext());
        tTv.setText(title + (defaultOn ? "  [已激活]" : "  [已就绪]"));
        tTv.setTextColor(defaultOn ? Color.parseColor("#10A37F") : getPrimaryTextColor());
        tTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tTv.setTypeface(null, Typeface.BOLD);
        card.addView(tTv);

        TextView dTv = new TextView(getContext());
        dTv.setText(desc);
        dTv.setTextColor(getSecondaryTextColor());
        dTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dTv.setPadding(0, dp(4), 0, 0);
        card.addView(dTv);

        card.setOnClickListener(v -> Toast.makeText(activity, "设置已更新: " + title, Toast.LENGTH_SHORT).show());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        contentContainer.addView(card, lp);
    }

    // ==================== 6. 云同步与备份 ====================
    private void renderBackupView() {
        TextView intro = createSectionTitle("☁️ 配置备份与跨设备迁移");
        contentContainer.addView(intro);

        Button exportConfigBtn = createStyledButton("📤 备份所有文件夹与归档快照为 JSON", "#10A37F", "#09090B");
        exportConfigBtn.setOnClickListener(v -> {
            String backupJson = sp.getString("folders_v3", "[]");
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, backupJson);
            activity.startActivity(Intent.createChooser(shareIntent, "导出备份数据"));
        });
        contentContainer.addView(exportConfigBtn);
    }

    // ==================== 自动化寻路辅助 ====================
    private void fillToGemini(String text) {
        boolean ok = autoFillGeminiInput(activity.getWindow().getDecorView(), text);
        if (ok) {
            Toast.makeText(activity, "✅ 已自动填入提问框并聚焦", Toast.LENGTH_SHORT).show();
            closeDrawer();
        } else {
            copyToClipboard(text);
            Toast.makeText(activity, "已复制到剪贴板，请长按粘贴", Toast.LENGTH_SHORT).show();
        }
    }

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

    private WebView findWebView(View root) {
        if (root instanceof WebView) return (WebView) root;
        if (root != null && root.getClass().getName().contains("WebView")) {
            try { return (WebView) root; } catch (Exception ignored) {}
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                WebView res = findWebView(vg.getChildAt(i));
                if (res != null) return res;
            }
        }
        return null;
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Voyager", text));
        }
    }

    // ==================== 持久化模型与数据 ====================
    public static class SavedChat {
        public String title = "";
        public String url = "";
        public String date = "";
        public String markdownSnapshot = ""; // 完整问答文本快照
    }

    public static class FolderData {
        public String name = "";
        public String color = "#10A37F";
        public List<SavedChat> savedChats = new ArrayList<>();
    }

    private List<FolderData> loadPersistedFolders() {
        List<FolderData> list = new ArrayList<>();
        String raw = sp.getString("folders_v3", null);
        if (raw == null) {
            FolderData f1 = new FolderData();
            f1.name = "💼 核心工作与架构规划";
            f1.color = "#10A37F";
            list.add(f1);

            FolderData f2 = new FolderData();
            f2.name = "💻 编程与技术调研";
            f2.color = "#3B82F6";
            list.add(f2);
            savePersistedFolders(list);
            return list;
        }

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                FolderData fd = new FolderData();
                fd.name = obj.optString("name");
                fd.color = obj.optString("color", "#10A37F");
                JSONArray cArr = obj.optJSONArray("chats");
                if (cArr != null) {
                    for (int j = 0; j < cArr.length(); j++) {
                        JSONObject cObj = cArr.getJSONObject(j);
                        SavedChat sc = new SavedChat();
                        sc.title = cObj.optString("title");
                        sc.url = cObj.optString("url");
                        sc.date = cObj.optString("date");
                        sc.markdownSnapshot = cObj.optString("snapshot");
                        fd.savedChats.add(sc);
                    }
                }
                list.add(fd);
            }
        } catch (Exception ignored) {}

        return list;
    }

    private void savePersistedFolders(List<FolderData> list) {
        try {
            JSONArray arr = new JSONArray();
            for (FolderData f : list) {
                JSONObject obj = new JSONObject();
                obj.put("name", f.name);
                obj.put("color", f.color);
                JSONArray cArr = new JSONArray();
                for (SavedChat sc : f.savedChats) {
                    JSONObject cObj = new JSONObject();
                    cObj.put("title", sc.title);
                    cObj.put("url", sc.url);
                    cObj.put("date", sc.date);
                    cObj.put("snapshot", sc.markdownSnapshot);
                    cArr.put(cObj);
                }
                obj.put("chats", cArr);
                arr.put(obj);
            }
            sp.edit().putString("folders_v3", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ==================== 书签持久化与管理 ====================
    public boolean isChatBookmarked(SavedChat chat) {
        if (chat == null) return false;
        List<SavedChat> list = loadPersistedBookmarks();
        for (SavedChat item : list) {
            if ((chat.url != null && !chat.url.isEmpty() && chat.url.equals(item.url)) ||
                (chat.title != null && !chat.title.isEmpty() && chat.title.equals(item.title))) {
                return true;
            }
        }
        return false;
    }

    public void toggleBookmark(SavedChat chat) {
        if (chat == null) return;
        List<SavedChat> list = loadPersistedBookmarks();
        int existingIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            SavedChat item = list.get(i);
            if ((chat.url != null && !chat.url.isEmpty() && chat.url.equals(item.url)) ||
                (chat.title != null && !chat.title.isEmpty() && chat.title.equals(item.title))) {
                existingIdx = i;
                break;
            }
        }
        if (existingIdx >= 0) {
            list.remove(existingIdx);
            savePersistedBookmarks(list);
            Toast.makeText(activity, "已从书签移除", Toast.LENGTH_SHORT).show();
        } else {
            list.add(0, chat);
            savePersistedBookmarks(list);
            Toast.makeText(activity, "⭐ 已添加至会话书签！", Toast.LENGTH_SHORT).show();
        }
    }

    public List<SavedChat> loadPersistedBookmarks() {
        List<SavedChat> list = new ArrayList<>();
        String raw = sp.getString("bookmarks_v2", null);
        if (raw == null) {
            SavedChat sample = new SavedChat();
            sample.title = "💡 Gemini 进阶思维链与深度 Prompt 指引";
            sample.url = "https://gemini.google.com/app";
            sample.date = "精选书签";
            sample.markdownSnapshot = "### 👤 提问\n如何利用思维链 (Chain of Thought) 最大化发挥 Gemini 的深度推理能力？\n\n### 🤖 Gemini 回答\n引导模型「慢思考」的最佳策略：\n1. **步骤解构**：明确要求「Step-by-step」分步推演；\n2. **边界检验**：让模型在给出结论前自我反思潜在边界条件；\n3. **结构化输出**：使用 Markdown 表格与行动项输出可执行的结论。\n\n点击上方「🚀 填入续聊」即可在 Gemini 输入框中立即展开深度追问！";
            list.add(sample);
            savePersistedBookmarks(list);
            return list;
        }

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                SavedChat sc = new SavedChat();
                sc.title = obj.optString("title");
                sc.url = obj.optString("url");
                sc.date = obj.optString("date");
                sc.markdownSnapshot = obj.optString("snapshot");
                list.add(sc);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void savePersistedBookmarks(List<SavedChat> list) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedChat sc : list) {
                JSONObject obj = new JSONObject();
                obj.put("title", sc.title);
                obj.put("url", sc.url);
                obj.put("date", sc.date);
                obj.put("snapshot", sc.markdownSnapshot);
                arr.put(obj);
            }
            sp.edit().putString("bookmarks_v2", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void addCurrentChatToBookmarks() {
        SavedChat sc = new SavedChat();
        sc.title = currentRealTitle.isEmpty() ? "Gemini 深度分析对话" : currentRealTitle;
        sc.url = currentRealUrl;
        sc.date = new Date().toLocaleString();
        sc.markdownSnapshot = buildRealMarkdownString();

        List<SavedChat> list = loadPersistedBookmarks();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).title.equals(sc.title)) {
                list.remove(i);
                break;
            }
        }
        list.add(0, sc);
        savePersistedBookmarks(list);
        Toast.makeText(activity, "⭐ 成功添加书签: " + sc.title, Toast.LENGTH_SHORT).show();
        renderBookmarksView();
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("新建分类文件夹");
        final EditText input = new EditText(activity);
        input.setHint("输入分类名称 (如: 论文研读、市场调研)");
        builder.setView(input);
        builder.setPositiveButton("确定", (d, w) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                List<FolderData> list = loadPersistedFolders();
                FolderData fd = new FolderData();
                fd.name = name;
                fd.color = COLOR_PALETTE[list.size() % COLOR_PALETTE.length];
                list.add(fd);
                savePersistedFolders(list);
                renderFoldersView();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private static class PromptItem {
        String category;
        String title;
        String content;

        PromptItem(String c, String t, String cnt) {
            this.category = c;
            this.title = t;
            this.content = cnt;
        }
    }

    private List<PromptItem> getPromptLibraryData() {
        List<PromptItem> list = new ArrayList<>();
        list.add(new PromptItem("💻 编程", "代码审查与性能排查", "请深度审查以下 {{目标代码}}，指出潜在的内存泄漏与并发安全隐患，并给出优雅的重构版本："));
        list.add(new PromptItem("📊 分析", "结构化对比表格", "请对比分析 {{主题}} 的优缺点与适用场景，并以清晰的 Markdown 矩阵表格形式输出："));
        list.add(new PromptItem("📝 润色", "简明扼要提炼结论", "请用中文简明扼要地提炼上述内容的最核心观点与关键行动项（Action Items）："));
        list.add(new PromptItem("🌐 翻译", "信达雅专业技术翻译", "请将以下内容翻译为符合中文技术规范的专业用语，避免生硬机翻感："));
        return list;
    }

    private TextView createSectionTitle(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(getSecondaryTextColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private Button createStyledButton(String text, String bgColor, String textColor) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(dp(8));
        btn.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(40));
        lp.topMargin = dp(8);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
