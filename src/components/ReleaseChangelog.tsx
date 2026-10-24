import React, { useState } from 'react';
import { Tag, Sparkles, Copy, Check, Download, GitBranch, Shield, ArrowRight, ExternalLink } from 'lucide-react';

export const ReleaseChangelog: React.FC = () => {
  const [copiedChangelog, setCopiedChangelog] = useState(false);
  const [copiedAction, setCopiedAction] = useState(false);

  const releaseVersion = "Voyager for Gemini 1.00";
  const releaseTag = "v1.00";
  const releaseDate = "2026-10-24";

  const changelogPoints = [
    {
      category: "悬浮交互",
      icon: "🔘",
      title: "圆形发光悬浮球",
      desc: "升级为 52dp 翡翠绿圆形发光悬浮球，支持全屏自由拖拽、松手平滑自动贴边吸附、闲置自动半透明淡化。"
    },
    {
      category: "书签速递",
      icon: "🔖",
      title: "会话书签与极速直达",
      desc: "新增星标会话书签功能，一键收藏重点对话；点击书签卡片即刻直达相对应对话记录与完整问答详情页面。"
    },
    {
      category: "分类管理",
      icon: "📂",
      title: "三级分类文件夹",
      desc: "精准捕获并收录当前会话，支持树状文件夹归类管理与点击回溯问答记录。"
    },
    {
      category: "时间线",
      icon: "📍",
      title: "对话时间线导航",
      desc: "自动提炼聊天提问节点为可视化小地图，点击平滑滚动并高亮定位当前对话。"
    },
    {
      category: "数据导出",
      icon: "💾",
      title: "多格式数据导出",
      desc: "一键导出包含标准代码高亮渲染的 Markdown / JSON 格式文本，自动唤起系统级分享。"
    },
    {
      category: "提示词库",
      icon: "💡",
      title: "提示词库与变量模版",
      desc: "内置 Prompt 词库，支持 {{变量}} 占位符一键填入输入框并聚焦。"
    },
    {
      category: "排版增强",
      icon: "📐",
      title: "数学公式多平台复制",
      desc: "支持一键提取公式并转换为 LaTeX、MathML (专为 Word 优化) 及 Notion 兼容格式。"
    },
    {
      category: "打包规范",
      icon: "🔑",
      title: "永久固定签名",
      desc: "内置统一 Release 签名配置，后续版本覆盖更新无需重复卸载安装。"
    }
  ];

  const rawMarkdownChangelog = `## 🪐 ${releaseVersion} 更新日志

- 🔘 **圆形发光悬浮球**：升级为 52dp 翡翠绿圆形发光悬浮球，支持全屏自由拖拽、松手平滑自动贴边吸附、闲置自动半透明淡化。
- 🔖 **会话书签与极速直达**：新增星标会话书签功能，一键收藏重点对话；点击书签卡片即刻直达相对应对话记录与完整问答详情页面。
- 📂 **三级分类文件夹**：精准捕获并收录当前会话，支持树状文件夹归类管理与点击回溯问答记录。
- 📍 **对话时间线导航**：自动提炼聊天提问节点为可视化小地图，点击平滑滚动并高亮定位当前对话。
- 💾 **多格式数据导出**：一键导出包含标准代码高亮渲染的 Markdown / JSON 格式文本，自动唤起系统级分享。
- 💡 **提示词库与变量模版**：内置 Prompt 词库，支持 {{变量}} 占位符一键填入输入框并聚焦。
- 📐 **数学公式多平台复制**：支持一键提取公式并转换为 LaTeX、MathML (专为 Word 优化) 及 Notion 兼容格式。
- 🔑 **永久固定签名**：内置统一 Release 签名配置，后续版本覆盖更新无需重复卸载安装。`;

  const copyChangelog = () => {
    navigator.clipboard.writeText(rawMarkdownChangelog);
    setCopiedChangelog(true);
    setTimeout(() => setCopiedChangelog(false), 2000);
  };

  const copyWorkflow = () => {
    const actionSnippet = `      - name: 自动发布到 GitHub Releases (免解压直链 APK)
        uses: softprops/action-gh-release@v2
        if: github.event_name != 'pull_request'
        with:
          tag_name: "v1.00"
          name: "Voyager for Gemini 1.00"
          body: |
            ## 🪐 Voyager for Gemini 1.00 更新日志

            - 🔘 **圆形发光悬浮球**：升级为 52dp 翡翠绿圆形发光悬浮球，支持全屏自由拖拽、松手平滑自动贴边吸附、闲置自动半透明淡化。
            - 🔖 **会话书签与极速直达**：新增星标会话书签功能，一键收藏重点对话；点击书签卡片即刻直达相对应对话记录与完整问答详情页面。
            - 📂 **三级分类文件夹**：精准捕获并收录当前会话，支持树状文件夹归类管理与点击回溯问答记录。
            - 📍 **对话时间线导航**：自动提炼聊天提问节点为可视化小地图，点击平滑滚动并高亮定位当前对话。
            - 💾 **多格式数据导出**：一键导出包含标准代码高亮渲染的 Markdown / JSON 格式文本，自动唤起系统级分享。
            - 💡 **提示词库与变量模版**：内置 Prompt 词库，支持 {{变量}} 占位符一键填入输入框并聚焦。
            - 📐 **数学公式多平台复制**：支持一键提取公式并转换为 LaTeX、MathML (专为 Word 优化) 及 Notion 兼容格式。
            - 🔑 **永久固定签名**：内置统一 Release 签名配置，后续版本覆盖更新无需重复卸载安装。
          files: single_apk/Voyager-Gemini-Enhancer.apk
        env:
          GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}`;
    navigator.clipboard.writeText(actionSnippet);
    setCopiedAction(true);
    setTimeout(() => setCopiedAction(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Release Header Card */}
      <div className="bg-zinc-900/90 border border-emerald-500/30 rounded-2xl p-6 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-mono font-bold bg-emerald-500 text-zinc-950">
                <Tag className="w-3.5 h-3.5" />
                <span>{releaseTag}</span>
              </span>
              <span className="text-xs font-mono text-emerald-400 bg-emerald-950/60 border border-emerald-500/20 px-2.5 py-1 rounded-full">
                🚀 起始发行版 · 往后版本将在此基础上迭代
              </span>
              <span className="text-xs font-mono text-zinc-400 bg-zinc-800 px-2.5 py-1 rounded-full border border-zinc-700">
                发布日期: {releaseDate}
              </span>
            </div>

            <h2 className="text-2xl sm:text-3xl font-extrabold text-zinc-100 tracking-tight flex items-center space-x-2">
              <span>{releaseVersion}</span>
              <span className="text-emerald-400 text-lg font-mono">Official Release</span>
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 max-w-3xl leading-relaxed">
              严格遵循版本日志精炼规范：每个版本更新日志仅直接陈述本次更新包含的具体功能与变更点，零冗余叙述。后续版本将按 1.01、1.02... 持续往后迭代。
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5 shrink-0">
            <button
              onClick={copyChangelog}
              className="flex items-center space-x-1.5 px-4 py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-100 text-xs font-mono border border-zinc-700 transition-all cursor-pointer shadow-sm"
            >
              {copiedChangelog ? (
                <>
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span className="text-emerald-400 font-semibold">已复制 Release Markdown!</span>
                </>
              ) : (
                <>
                  <Copy className="w-4 h-4 text-emerald-400" />
                  <span>复制 GitHub Release 日志</span>
                </>
              )}
            </button>

            <button
              onClick={copyWorkflow}
              className="flex items-center space-x-1.5 px-4 py-2.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 text-xs font-mono border border-emerald-500/30 transition-all cursor-pointer"
            >
              {copiedAction ? (
                <>
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span className="text-emerald-400 font-semibold">已复制 CI Release 片段!</span>
                </>
              ) : (
                <>
                  <GitBranch className="w-4 h-4 text-emerald-400" />
                  <span>复制 Actions Release 配置</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Changelog Items Grid */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
          <div className="flex items-center space-x-2">
            <Sparkles className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-semibold text-zinc-200">
              更新内容清单 (Strict Changelog)
            </h3>
          </div>
          <span className="text-xs font-mono text-zinc-500">共计 8 项核心功能演进</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
          {changelogPoints.map((item, idx) => (
            <div
              key={idx}
              className="bg-zinc-950/70 border border-zinc-800/80 hover:border-emerald-500/40 rounded-xl p-4 transition-all group"
            >
              <div className="flex items-start space-x-3">
                <span className="text-xl shrink-0 mt-0.5">{item.icon}</span>
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-bold text-zinc-100 group-hover:text-emerald-300 transition-colors">
                      {item.title}
                    </span>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-400 border border-zinc-700/60">
                      {item.category}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    {item.desc}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Version Iteration Plan & Specifications */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="bg-zinc-900/40 border border-zinc-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400 font-semibold">
            <Tag className="w-3.5 h-3.5" />
            <span>版本规格统一</span>
          </div>
          <div className="space-y-1 font-mono text-[11px] text-zinc-300">
            <p>• Release Tag: <span className="text-emerald-400">v1.00</span></p>
            <p>• Release Name: <span className="text-zinc-100">Voyager for Gemini 1.00</span></p>
            <p>• Android versionName: <span className="text-emerald-400">1.00</span></p>
            <p>• Android versionCode: <span className="text-emerald-400">100</span></p>
          </div>
        </div>

        <div className="bg-zinc-900/40 border border-zinc-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400 font-semibold">
            <Shield className="w-3.5 h-3.5" />
            <span>覆盖安装保障</span>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            内置统一 Release 签名秘钥（`voyager-release.jks`）。从 1.00 起，后续所有迭代版本均维持相同证书，直接升级安装永不提示签名冲突或需重新卸载。
          </p>
        </div>

        <div className="bg-zinc-900/40 border border-zinc-800 rounded-xl p-4 space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400 font-semibold">
            <ArrowRight className="w-3.5 h-3.5" />
            <span>后续迭代策略</span>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            本版本正式重置为 1.00 起点。后续发布将保持纯粹更新日志准则，每个版本只列出变更项，便于手机端用户快速明确新版功能。
          </p>
        </div>
      </div>
    </div>
  );
};
