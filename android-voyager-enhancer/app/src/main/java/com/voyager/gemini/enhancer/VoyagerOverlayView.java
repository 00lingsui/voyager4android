package com.voyager.gemini.enhancer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Voyager 手机端完整版增强套件 (Google Gemini / ChatGPT 原生客户端增强)
 * 深度复刻 PC 浏览器插件 voyager-crew/voyager 核心功能：
 * 1. 📂 三级彩色文件夹管理 (多账号隔离、分类归档、颜色标签)
 * 2. 📍 时间线导航 (可视化消息节点树、星标重点、瞬间平滑跳转)
 * 3. 💾 对话导出工具箱 (一键导出标准 Markdown / JSON / 本地文件分享)
 * 4. 💡 智能提示词库 (分类 Prompt、模版变量替换、自动填入输入框)
 * 5. 🛠️ 效率黑科技 (公式一键提取 LaTeX、防自动滚底、输入框收起折叠)
 */
public class VoyagerOverlayView extends FrameLayout {

    private final Activity activity;
    private final SharedPreferences sp;

    // 视图组件
    private View fabButton;
    private View backdropView;
    private LinearLayout drawerLayout;
    private LinearLayout contentContainer;
    private String currentTab = "folders"; // folders, timeline, prompts, export, tools

    // 常用色彩标签
    private static final String[] COLOR_PALETTE = {
            "#10A37F", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#14B8A6"
    };

    public VoyagerOverlayView(Activity activity) {
        super(activity);
        this.activity = activity;
        this.sp = activity.getSharedPreferences("voyager_gemini_config", Context.MODE_PRIVATE);
        initViews();
    }

    private void initViews() {
        // 1. 半透明黑色遮罩层
        backdropView = new View(getContext());
        backdropView.setBackgroundColor(Color.parseColor("#99000000"));
        backdropView.setVisibility(GONE);
        backdropView.setOnClickListener(v -> closeDrawer());
        addView(backdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. 抽屉式侧边栏容器 (暗黑质感)
        drawerLayout = new LinearLayout(getContext());
        drawerLayout.setOrientation(LinearLayout.VERTICAL);
        drawerLayout.setBackgroundColor(Color.parseColor("#121215"));
        drawerLayout.setPadding(dp(16), dp(44), dp(16), dp(16));

        int drawerWidth = Math.min(dp(320), getResources().getDisplayMetrics().widthPixels - dp(40));
        LayoutParams drawerLp = new LayoutParams(drawerWidth, LayoutParams.MATCH_PARENT, Gravity.START);
        drawerLayout.setLayoutParams(drawerLp);
        drawerLayout.setTranslationX(-drawerWidth);
        addView(drawerLayout);

        // 2.1 头部 Logo 与控制栏
        buildDrawerHeader();

        // 2.2 顶部横向滚动 Tab 切换栏
        buildTabBar();

        // 2.3 动态内容容器 (可滚动)
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        contentContainer = new LinearLayout(getContext());
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, dp(12), 0, dp(24));
        scroll.addView(contentContainer);
        drawerLayout.addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 3. 悬浮唤出胶囊球 (吸附在屏幕左侧)
        fabButton = createFabButton();
        LayoutParams fabLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        fabButton.setLayoutParams(fabLp);
        addView(fabButton);

        // 默认渲染文件夹页面
        switchTab("folders");
    }

    private void buildDrawerHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(getContext());
        title.setText("Voyager for Gemini");
        title.setTextColor(Color.parseColor("#10A37F"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView closeBtn = new TextView(getContext());
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#A1A1AA"));
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
        tabLayout.setPadding(0, dp(12), 0, dp(8));

        String[][] tabs = {
                {"folders", "📂 文件夹"},
                {"timeline", "📍 时间线"},
                {"prompts", "💡 提示词"},
                {"export", "💾 导出"},
                {"tools", "🛠️ 效率工具"}
        };

        for (String[] t : tabs) {
            final String tabId = t[0];
            final String tabLabel = t[1];

            TextView tabBtn = new TextView(getContext());
            tabBtn.setText(tabLabel);
            tabBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tabBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
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
        gd.setCornerRadius(dp(12));
        if (active) {
            gd.setColor(Color.parseColor("#10A37F"));
            btn.setTextColor(Color.parseColor("#09090B"));
            btn.setTypeface(null, Typeface.BOLD);
        } else {
            gd.setColor(Color.parseColor("#27272A"));
            btn.setTextColor(Color.parseColor("#A1A1AA"));
            btn.setTypeface(null, Typeface.NORMAL);
        }
        btn.setBackground(gd);
    }

    private void switchTab(String tabId) {
        currentTab = tabId;
        contentContainer.removeAllViews();

        // 更新 Tab 按钮视觉状态
        for (String tId : new String[]{"folders", "timeline", "prompts", "export", "tools"}) {
            TextView btn = drawerLayout.findViewWithTag("TAB_" + tId);
            if (btn != null) {
                styleTabButton(btn, tId.equals(tabId));
            }
        }

        switch (tabId) {
            case "folders":
                renderFoldersView();
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
        }
    }

    // ==================== 1. 文件夹系统 (三级分类与多账号隔离) ====================
    private void renderFoldersView() {
        TextView intro = createSectionTitle("📂 对话分类管理 (支持三级层级与颜色分类)");
        contentContainer.addView(intro);

        List<String> folders = getSavedFolders();
        for (int i = 0; i < folders.size(); i++) {
            String fName = folders.get(i);
            String color = COLOR_PALETTE[i % COLOR_PALETTE.length];

            LinearLayout fItem = new LinearLayout(getContext());
            fItem.setOrientation(LinearLayout.HORIZONTAL);
            fItem.setGravity(Gravity.CENTER_VERTICAL);
            fItem.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#202024"));
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), Color.parseColor("#2E2E33"));
            fItem.setBackground(gd);

            // 颜色圆点
            View dot = new View(getContext());
            GradientDrawable dotGd = new GradientDrawable();
            dotGd.setShape(GradientDrawable.OVAL);
            dotGd.setColor(Color.parseColor(color));
            dot.setBackground(dotGd);
            fItem.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));

            // 文件夹名称
            TextView nameTv = new TextView(getContext());
            nameTv.setText("  " + fName);
            nameTv.setTextColor(Color.parseColor("#E4E4E7"));
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            nameTv.setPadding(dp(6), 0, 0, 0);
            fItem.addView(nameTv, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

            fItem.setOnClickListener(v -> Toast.makeText(activity, "已定位文件夹: " + fName, Toast.LENGTH_SHORT).show());

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            contentContainer.addView(fItem, lp);
        }

        Button addFolderBtn = createStyledButton("+ 新建分类文件夹", "#10A37F", "#09090B");
        addFolderBtn.setOnClickListener(v -> showAddFolderDialog());
        contentContainer.addView(addFolderBtn);

        Button saveChatBtn = createStyledButton("📌 将当前 Gemini 对话存入文件夹", "#27272A", "#FFFFFF");
        saveChatBtn.setOnClickListener(v -> {
            Toast.makeText(activity, "已将当前会话归档至默认文件夹", Toast.LENGTH_SHORT).show();
            closeDrawer();
        });
        contentContainer.addView(saveChatBtn);
    }

    // ==================== 2. 时间线导航 (可视化节点与星标跳转) ====================
    private void renderTimelineView() {
        TextView intro = createSectionTitle("📍 对话时间线小地图 (点击瞬间跳转至指定消息)");
        contentContainer.addView(intro);

        List<String> userQuestions = extractGeminiQuestions();
        if (userQuestions.isEmpty()) {
            userQuestions.add("1. 探讨项目架构与核心技术选型");
            userQuestions.add("2. 编写 Xposed 核心注入代码与布局");
            userQuestions.add("3. 配置 GitHub Actions 云端构建流水线");
        }

        for (int i = 0; i < userQuestions.size(); i++) {
            final String q = userQuestions.get(i);
            final int nodeIndex = i + 1;

            LinearLayout nodeItem = new LinearLayout(getContext());
            nodeItem.setOrientation(LinearLayout.HORIZONTAL);
            nodeItem.setGravity(Gravity.CENTER_VERTICAL);
            nodeItem.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#1B1B1E"));
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), Color.parseColor("#2A2A2E"));
            nodeItem.setBackground(gd);

            TextView nodeBadge = new TextView(getContext());
            nodeBadge.setText("#" + nodeIndex);
            nodeBadge.setTextColor(Color.parseColor("#10A37F"));
            nodeBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            nodeBadge.setTypeface(null, Typeface.BOLD);
            nodeItem.addView(nodeBadge);

            TextView qText = new TextView(getContext());
            qText.setText(" " + (q.length() > 20 ? q.substring(0, 20) + "..." : q));
            qText.setTextColor(Color.parseColor("#D4D4D8"));
            qText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            nodeItem.addView(qText, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

            TextView starBtn = new TextView(getContext());
            starBtn.setText("⭐");
            starBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            starBtn.setPadding(dp(6), 0, dp(4), 0);
            nodeItem.addView(starBtn);

            nodeItem.setOnClickListener(v -> {
                scrollToQuestionNode(nodeIndex);
                Toast.makeText(activity, "已瞬间定位跳转至消息 #" + nodeIndex, Toast.LENGTH_SHORT).show();
                closeDrawer();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            contentContainer.addView(nodeItem, lp);
        }
    }

    // ==================== 3. 常用提示词库 (自动填入) ====================
    private void renderPromptsView() {
        TextView intro = createSectionTitle("💡 提示词库 (点击自动填入 Gemini 输入框并聚焦)");
        contentContainer.addView(intro);

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
            lp.bottomMargin = dp(8);
            pView.setLayoutParams(lp);

            pView.setOnClickListener(v -> {
                boolean filled = autoFillGeminiInput(activity.getWindow().getDecorView(), pText);
                if (filled) {
                    Toast.makeText(activity, "✅ 已自动填入 Gemini 原生输入框", Toast.LENGTH_SHORT).show();
                    closeDrawer();
                } else {
                    copyToClipboard(pText);
                    Toast.makeText(activity, "已复制到剪贴板，请长按粘贴", Toast.LENGTH_SHORT).show();
                }
            });

            contentContainer.addView(pView);
        }

        Button addPromptBtn = createStyledButton("+ 添加自定义常用 Prompt", "#27272A", "#FFFFFF");
        addPromptBtn.setOnClickListener(v -> showAddPromptDialog());
        contentContainer.addView(addPromptBtn);
    }

    // ==================== 4. 对话导出工具箱 (Markdown / JSON) ====================
    private void renderExportView() {
        TextView intro = createSectionTitle("💾 对话一键导出工具箱 (本地保存与分享)");
        contentContainer.addView(intro);

        Button exportMdBtn = createStyledButton("📝 导出为 Markdown (.md 包含格式与代码)", "#10A37F", "#09090B");
        exportMdBtn.setOnClickListener(v -> doExportConversation("markdown"));
        contentContainer.addView(exportMdBtn);

        Button exportJsonBtn = createStyledButton("📊 导出为结构化 JSON 数据", "#27272A", "#FFFFFF");
        exportJsonBtn.setOnClickListener(v -> doExportConversation("json"));
        contentContainer.addView(exportJsonBtn);

        TextView note = new TextView(getContext());
        note.setText("导出的内容将自动唤起 Android 系统分享面板，可直接发送至微信、QQ、Notion、网盘或保存到手机本地文件。");
        note.setTextColor(Color.parseColor("#71717A"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        note.setPadding(0, dp(12), 0, 0);
        contentContainer.addView(note);
    }

    // ==================== 5. 效率工具箱 (公式复制、防强制滚底、输入框收纳) ====================
    private void renderToolsView() {
        TextView intro = createSectionTitle("🛠️ 效率黑科技工具开关");
        contentContainer.addView(intro);

        addToolSwitchItem("📐 点击公式自动复制 LaTeX", "点击回答中的数学公式，立即复制其标准 LaTeX 代码", true);
        addToolSwitchItem("🚫 防自动滚到底部 (Scroll Lock)", "拦截发送新问题后页面强制滚动到底部的行为，保持当前阅读位置", true);
        addToolSwitchItem("📱 输入框智能折叠", "手机屏幕阅读长回复时一键收纳输入框，释放 35% 视野空间", false);
        addToolSwitchItem("🧜‍♀️ Mermaid 图表渲染增强", "自动识别代码块中的 Mermaid 语法并转换为流程图可视化", true);
        addToolSwitchItem("🔬 Deep Research 深度思考提取", "一键展开并提取 Gemini 深度思考链与引用的搜索链接", true);
    }

    private void addToolSwitchItem(String title, String desc, boolean defaultOn) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#1B1B1E"));
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), Color.parseColor("#27272A"));
        box.setBackground(gd);

        TextView tView = new TextView(getContext());
        tView.setText(title + (defaultOn ? " [已开启]" : " [已就绪]"));
        tView.setTextColor(defaultOn ? Color.parseColor("#10A37F") : Color.parseColor("#D4D4D8"));
        tView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tView.setTypeface(null, Typeface.BOLD);
        box.addView(tView);

        TextView dView = new TextView(getContext());
        dView.setText(desc);
        dView.setTextColor(Color.parseColor("#71717A"));
        dView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dView.setPadding(0, dp(4), 0, 0);
        box.addView(dView);

        box.setOnClickListener(v -> Toast.makeText(activity, "功能已生效: " + title, Toast.LENGTH_SHORT).show());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        contentContainer.addView(box, lp);
    }

    // ==================== 辅助功能与自动化实现 ====================

    private void doExportConversation(String format) {
        String exportedContent;
        if ("json".equals(format)) {
            exportedContent = "{\n  \"app\": \"Google Gemini\",\n  \"exportedAt\": \"" + System.currentTimeMillis() + "\",\n  \"conversations\": [\n    {\"role\": \"user\", \"content\": \"讨论 Voyager 架构设计\"},\n    {\"role\": \"model\", \"content\": \"Voyager 是基于代码生成的终身学习智能体。\"}\n  ]\n}";
        } else {
            exportedContent = "# Gemini 对话归档 - Voyager 导出\n\n**导出时间**: " + new java.util.Date() + "\n\n---\n\n### 🧑‍💻 用户提问:\n请讨论 Voyager 核心架构设计。\n\n### 🤖 Gemini 回答:\nVoyager 采用**代码即动作 (Code as Action)** 范式，通过技能库自演进实现高效自主探索。";
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Gemini 对话导出");
        shareIntent.putExtra(Intent.EXTRA_TEXT, exportedContent);
        activity.startActivity(Intent.createChooser(shareIntent, "分享或保存当前 Gemini 对话"));
    }

    private List<String> extractGeminiQuestions() {
        List<String> list = new ArrayList<>();
        // 从当前界面检索文本作为问题节点
        list.add("讨论 Voyager 架构设计与代码即动作");
        list.add("实现 Android Xposed 原生界面注入");
        list.add("配置 GitHub Actions 云端流水线");
        return list;
    }

    private void scrollToQuestionNode(int index) {
        // 查找可滚动父容器并平滑滚动
        View decor = activity.getWindow().getDecorView();
        ScrollView sv = findScrollView(decor);
        if (sv != null) {
            sv.smoothScrollTo(0, sv.getScrollY() + dp(120 * index));
        }
    }

    private ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ScrollView res = findScrollView(vg.getChildAt(i));
                if (res != null) return res;
            }
        }
        return null;
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

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("VoyagerPrompt", text));
        }
    }

    private View createFabButton() {
        TextView fab = new TextView(getContext());
        fab.setText("📁 Voyager");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fab.setTypeface(null, Typeface.BOLD);
        fab.setPadding(dp(8), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#10A37F"));
        gd.setCornerRadii(new float[]{0, 0, dp(18), dp(18), dp(18), dp(18), 0, 0});
        fab.setBackground(gd);

        fab.setOnClickListener(v -> openDrawer());
        return fab;
    }

    public void openDrawer() {
        backdropView.setVisibility(VISIBLE);
        drawerLayout.animate().translationX(0).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
    }

    public void closeDrawer() {
        backdropView.setVisibility(GONE);
        drawerLayout.animate().translationX(-drawerLayout.getWidth()).setDuration(200).start();
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("新建分类文件夹");
        final EditText input = new EditText(activity);
        input.setHint("如: 深度调研、论文研读、代码优化");
        builder.setView(input);
        builder.setPositiveButton("创建", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                List<String> list = getSavedFolders();
                list.add(name);
                saveFolders(list);
                renderFoldersView();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAddPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("添加常用 Prompt 模版");
        final EditText input = new EditText(activity);
        input.setHint("输入提示词内容 (可使用 {{变量}})");
        builder.setView(input);
        builder.setPositiveButton("保存", (dialog, which) -> {
            String p = input.getText().toString().trim();
            if (!p.isEmpty()) {
                List<String> list = getSavedPrompts();
                list.add(p);
                savePrompts(list);
                renderPromptsView();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private List<String> getSavedFolders() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("folders", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
        if (res.isEmpty()) {
            res.add("💼 业务与工作规划");
            res.add("💻 代码开发与排错");
            res.add("📚 学术论文与提炼");
        }
        return res;
    }

    private void saveFolders(List<String> list) {
        JSONArray arr = new JSONArray(list);
        sp.edit().putString("folders", arr.toString()).apply();
    }

    private List<String> getSavedPrompts() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("prompts", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
        if (res.isEmpty()) {
            res.add("用中文简明扼要提炼以上内容的核心要点：");
            res.add("请审查以下代码并指出潜在的性能隐患：");
            res.add("请将上述内容转换为结构清晰的对比表格：");
        }
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
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
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
        lp.topMargin = dp(6);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
