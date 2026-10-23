package com.voyager.gemini.enhancer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 Java 原生动态构建的 Voyager 侧边栏与悬浮球，支持：
 * 1. 类似网页插件的【文件夹管理】（工作、编程、写作分类）
 * 2. 【常用 Prompt 库】（点击自动填入 Gemini 客户端原生输入框并聚焦）
 * 3. 【对话收藏】：保存当前 Gemini 会话标题
 */
public class VoyagerOverlayView extends FrameLayout {

    private final Activity activity;
    private View fabButton;
    private View backdropView;
    private LinearLayout drawerLayout;
    private LinearLayout folderListLayout;
    private LinearLayout promptListLayout;

    private boolean isDrawerOpen = false;
    private SharedPreferences sp;

    public VoyagerOverlayView(Activity activity) {
        super(activity);
        this.activity = activity;
        this.sp = activity.getSharedPreferences("voyager_gemini_config", Context.MODE_PRIVATE);
        initViews();
        loadDataAndRender();
    }

    private void initViews() {
        int dp8 = dp(8);
        int dp12 = dp(12);
        int dp16 = dp(16);

        // 1. 半透明遮罩层 (点击收起侧边栏)
        backdropView = new View(getContext());
        backdropView.setBackgroundColor(Color.parseColor("#99000000"));
        backdropView.setVisibility(GONE);
        backdropView.setOnClickListener(v -> closeDrawer());
        addView(backdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 2. 原生抽屉侧边栏 (从左侧滑出)
        drawerLayout = new LinearLayout(getContext());
        drawerLayout.setOrientation(LinearLayout.VERTICAL);
        drawerLayout.setBackgroundColor(Color.parseColor("#18181B")); // 深色高质感
        drawerLayout.setPadding(dp16, dp(48), dp16, dp16); // 顶部留出状态栏高度

        int drawerWidth = dp(290);
        LayoutParams drawerLp = new LayoutParams(drawerWidth, LayoutParams.MATCH_PARENT, Gravity.START);
        drawerLayout.setLayoutParams(drawerLp);
        drawerLayout.setTranslationX(-drawerWidth); // 初始隐藏在屏幕左侧
        addView(drawerLayout);

        // 抽屉头部
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(getContext());
        title.setText("Voyager for Gemini");
        title.setTextColor(Color.parseColor("#10A37F"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView closeBtn = new TextView(getContext());
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#A1A1AA"));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setPadding(dp8, dp8, dp8, dp8);
        closeBtn.setOnClickListener(v -> closeDrawer());
        header.addView(closeBtn);

        drawerLayout.addView(header);

        // 内容可滚动区域
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp12, 0, dp16);

        // 模块 1: 文件夹分类
        TextView folderSection = createSectionTitle("📂 对话分类文件夹 (Folders)");
        content.addView(folderSection);

        folderListLayout = new LinearLayout(getContext());
        folderListLayout.setOrientation(LinearLayout.VERTICAL);
        content.addView(folderListLayout);

        Button addFolderBtn = createStyledButton("+ 新建文件夹", "#27272A", "#FFFFFF");
        addFolderBtn.setOnClickListener(v -> showAddFolderDialog());
        content.addView(addFolderBtn);

        // 模块 2: 常用 Prompt 库
        TextView promptSection = createSectionTitle("⚡ 常用快捷 Prompt 库");
        content.addView(promptSection);

        promptListLayout = new LinearLayout(getContext());
        promptListLayout.setOrientation(LinearLayout.VERTICAL);
        content.addView(promptListLayout);

        Button addPromptBtn = createStyledButton("+ 添加常用 Prompt", "#27272A", "#FFFFFF");
        addPromptBtn.setOnClickListener(v -> showAddPromptDialog());
        content.addView(addPromptBtn);

        scroll.addView(content);
        drawerLayout.addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 3. 悬浮召唤球 (吸附在左边缘)
        fabButton = createFabButton();
        LayoutParams fabLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        fabButton.setLayoutParams(fabLp);
        addView(fabButton);
    }

    private View createFabButton() {
        TextView fab = new TextView(getContext());
        fab.setText("📁 Voyager");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fab.setTypeface(null, android.graphics.Typeface.BOLD);
        fab.setPadding(dp(8), dp(10), dp(12), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#10A37F"));
        gd.setCornerRadii(new float[]{0, 0, dp(18), dp(18), dp(18), dp(18), 0, 0});
        fab.setBackground(gd);

        fab.setOnClickListener(v -> openDrawer());
        return fab;
    }

    public void openDrawer() {
        isDrawerOpen = true;
        backdropView.setVisibility(VISIBLE);
        drawerLayout.animate().translationX(0).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
    }

    public void closeDrawer() {
        isDrawerOpen = false;
        backdropView.setVisibility(GONE);
        drawerLayout.animate().translationX(-drawerLayout.getWidth()).setDuration(200).start();
    }

    private void loadDataAndRender() {
        renderFolders();
        renderPrompts();
    }

    private void renderFolders() {
        folderListLayout.removeAllViews();
        List<String> folders = getSavedFolders();
        for (String fName : folders) {
            TextView item = new TextView(getContext());
            item.setText("📁  " + fName);
            item.setTextColor(Color.parseColor("#E4E4E7"));
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            item.setPadding(dp(12), dp(10), dp(12), dp(10));

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#27272A"));
            gd.setCornerRadius(dp(8));
            item.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            item.setLayoutParams(lp);

            item.setOnClickListener(v -> {
                Toast.makeText(activity, "已选择分类: " + fName, Toast.LENGTH_SHORT).show();
            });

            folderListLayout.addView(item);
        }
    }

    private void renderPrompts() {
        promptListLayout.removeAllViews();
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
            lp.bottomMargin = dp(6);
            pView.setLayoutParams(lp);

            // 点击核心功能：自动寻找 Gemini 客户端当前原生输入框并填入文字！
            pView.setOnClickListener(v -> {
                boolean filled = autoFillGeminiInput(activity.getWindow().getDecorView(), pText);
                if (filled) {
                    Toast.makeText(activity, "✅ 已自动填入 Gemini 原生输入框", Toast.LENGTH_SHORT).show();
                    closeDrawer();
                } else {
                    Toast.makeText(activity, "已复制到剪贴板，请长按粘贴", Toast.LENGTH_SHORT).show();
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("prompt", pText));
                    }
                }
            });

            promptListLayout.addView(pView);
        }
    }

    /**
     * 核心自动化：递归遍历 Gemini 客户端原生 View 树，自动定位输入框填入文字
     */
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

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("新建文件夹");
        final EditText input = new EditText(activity);
        input.setHint("如: 业务调研、代码优化、论文翻译");
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                List<String> list = getSavedFolders();
                list.add(name);
                saveFolders(list);
                renderFolders();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAddPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("添加常用 Prompt 模版");
        final EditText input = new EditText(activity);
        input.setHint("输入你的常用提示词，点击可一键填入 Gemini 输入框");
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String p = input.getText().toString().trim();
            if (!p.isEmpty()) {
                List<String> list = getSavedPrompts();
                list.add(p);
                savePrompts(list);
                renderPrompts();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private List<String> getSavedFolders() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("folders", "["💼 工作与业务","💻 编程与Bug排查","📚 学习提炼与笔记"]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
        return res;
    }

    private void saveFolders(List<String> list) {
        JSONArray arr = new JSONArray(list);
        sp.edit().putString("folders", arr.toString()).apply();
    }

    private List<String> getSavedPrompts() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("prompts", "["用中文简明扼要提炼以上内容的核心要点：","请审查以下代码并指出潜在的性能隐患：","请将上述内容转换为结构清晰的对比表格："]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) res.add(arr.getString(i));
        } catch (Exception ignored) {}
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
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private Button createStyledButton(String text, String bgColor, String textColor) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(dp(6));
        btn.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        lp.topMargin = dp(4);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
