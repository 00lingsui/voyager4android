import React, { useState } from 'react';
import { 
  FileCode, 
  Copy, 
  Check, 
  CheckSquare, 
  Terminal, 
  HelpCircle, 
  Sparkles,
  ExternalLink,
  ChevronRight
} from 'lucide-react';
import { POC_CODE_SNIPPETS, PocCodeSnippet } from '../data/architectData';

export const PocCodeViewer: React.FC = () => {
  const [activeSnippetId, setActiveSnippetId] = useState<string>('hook_entry');
  const [copied, setCopied] = useState<boolean>(false);

  const activeSnippet = POC_CODE_SNIPPETS.find(s => s.id === activeSnippetId) || POC_CODE_SNIPPETS[0];

  const handleCopy = () => {
    navigator.clipboard.writeText(activeSnippet.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const pocSteps = [
    {
      num: '01',
      title: '环境就绪 (5 分钟)',
      desc: '手机已 Root 并安装 LSPosed (Zygisk 版本)，启用模块并勾选目标 App（如网易云音乐、淘宝或测试 Demo）。'
    },
    {
      num: '02',
      title: 'Gradle 依赖配置 (2 分钟)',
      desc: '在 build.gradle 中引入 Xposed API、Java-WebSocket 与 Rhino 引擎依赖，无需配置复杂 NDK。'
    },
    {
      num: '03',
      title: '编译注入与 Logcat 校验 (10 分钟)',
      desc: '安装 APK 到手机，在 LSPosed 勾选目标应用并强制停止后重新打开目标应用。'
    },
    {
      num: '04',
      title: 'ADB 端口映射与 Python 联调 (5 分钟)',
      desc: '终端执行 adb forward tcp:8080 tcp:8080，运行 Python 脚本发送代码，亲眼目睹宿主内弹出 Toast 并执行点击！'
    }
  ];

  return (
    <div className="space-y-6">
      {/* PoC Roadmap Header */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 space-y-4">
        <div>
          <span className="text-xs font-mono text-emerald-400 tracking-wider uppercase">PoC Implementation Guide</span>
          <h2 className="text-xl font-bold text-zinc-100 mt-1">
            第一步 PoC 极简可行性验证清单 (30 分钟跑通全链路)
          </h2>
          <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
            不要一上来就写复杂的向量检索或庞大的 UI 解析器。第一阶段只需要验证最核心的最小闭环：
            <strong className="text-zinc-200">「PC 端发送一段 JS 字符串 &rarr; 手机目标 App 进程内接收 &rarr; Rhino 解释执行 &rarr; 弹出 Toast 并点击指定按钮 &rarr; 结果回传 PC」</strong>。
          </p>
        </div>

        {/* 4-step cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 pt-2">
          {pocSteps.map((s) => (
            <div key={s.num} className="bg-zinc-950 p-3.5 rounded-lg border border-zinc-800 space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono text-emerald-400 font-bold">{s.num}</span>
                <CheckSquare className="w-3.5 h-3.5 text-zinc-600" />
              </div>
              <h4 className="text-xs font-bold text-zinc-200">{s.title}</h4>
              <p className="text-[11px] text-zinc-400 leading-relaxed">{s.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Code Viewer */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
        {/* File tabs */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-800 bg-zinc-950/80">
          <div className="flex items-center space-x-1 overflow-x-auto scrollbar-none">
            {POC_CODE_SNIPPETS.map((snip) => {
              const isActive = snip.id === activeSnippetId;
              return (
                <button
                  key={snip.id}
                  onClick={() => setActiveSnippetId(snip.id)}
                  className={`flex items-center space-x-2 px-3 py-1.5 rounded-md text-xs font-mono transition-all whitespace-nowrap ${
                    isActive
                      ? 'bg-zinc-800 text-emerald-400 border border-zinc-700 shadow-sm font-semibold'
                      : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900'
                  }`}
                >
                  <FileCode className="w-3.5 h-3.5" />
                  <span>{snip.filename}</span>
                </button>
              );
            })}
          </div>

          <div className="flex items-center space-x-3 shrink-0">
            <span className="text-[11px] font-mono text-zinc-500 hidden sm:inline">
              {activeSnippet.badge}
            </span>
            <button
              onClick={handleCopy}
              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-md bg-zinc-800 hover:bg-zinc-700 text-xs font-mono text-zinc-200 transition-colors border border-zinc-700"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400">已复制!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5 text-zinc-400" />
                  <span>复制代码</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Snippet explanation */}
        <div className="px-6 py-3 bg-zinc-950/40 border-b border-zinc-800/80 flex items-center justify-between text-xs text-zinc-400">
          <div className="space-y-0.5">
            <span className="text-zinc-200 font-semibold">{activeSnippet.title}</span>
            <p className="text-[11px] text-zinc-400">{activeSnippet.description}</p>
          </div>
        </div>

        {/* Code Content */}
        <div className="p-4 sm:p-6 bg-zinc-950 font-mono text-xs overflow-x-auto max-h-[520px] scrollbar-thin scrollbar-thumb-zinc-800">
          <pre className="text-zinc-300 leading-relaxed">
            <code>{activeSnippet.code}</code>
          </pre>
        </div>
      </div>

      {/* Gradle Dependencies and Config snippet */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Gradle build config */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-mono font-bold text-zinc-200 flex items-center space-x-2">
              <Terminal className="w-3.5 h-3.5 text-emerald-400" />
              <span>build.gradle (Module: app) 必须依赖</span>
            </h4>
            <span className="text-[10px] font-mono text-zinc-500">Groovy / KTS</span>
          </div>
          <div className="bg-zinc-950 p-3 rounded-lg border border-zinc-800 font-mono text-[11px] text-zinc-300 overflow-x-auto">
            <pre>
{`dependencies {
    // 1. Xposed 核心 API (注意必须是 compileOnly，不能打包进 APK)
    compileOnly 'de.robv.android.xposed:api:82'

    // 2. 进程内微型 WebSocket 服务器
    implementation 'org.java-websocket:Java-WebSocket:1.5.3'

    // 3. 嵌入式 JS 引擎 (Rhino: 纯 Java 零 so 依赖)
    implementation 'org.mozilla:rhino:1.7.14'
}`}
            </pre>
          </div>
          <p className="text-[11px] text-zinc-400 leading-relaxed">
            注：Xposed 插件必须在 <code>assets/xposed_init</code> 中写入模块完整类名 <code>com.agent.voyager.HookEntry</code>。并在 <code>AndroidManifest.xml</code> 的 <code>&lt;application&gt;</code> 下配置 <code>xposedmodule=true</code>。
          </p>
        </div>

        {/* Terminal commands for PoC validation */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-mono font-bold text-zinc-200 flex items-center space-x-2">
              <Terminal className="w-3.5 h-3.5 text-amber-400" />
              <span>验证终端指令 (一行行执行)</span>
            </h4>
            <span className="text-[10px] font-mono text-zinc-500">Bash / CMD</span>
          </div>
          <div className="bg-zinc-950 p-3 rounded-lg border border-zinc-800 font-mono text-[11px] text-zinc-300 overflow-x-auto space-y-2">
            <div>
              <span className="text-zinc-500"># 1. 端口转发：将 PC 28888 映射至手机内部 28888 (避开常用端口)</span>
              <div className="text-emerald-400">$ adb forward tcp:28888 tcp:28888</div>
            </div>
            <div>
              <span className="text-zinc-500"># 2. 启动 Logcat 实时监听 HookEntry 模块日志</span>
              <div className="text-emerald-400">$ adb logcat -s VoyagerAgent VoyagerAgent_WS</div>
            </div>
            <div>
              <span className="text-zinc-500"># 3. 运行 Python 客户端下发首段代码</span>
              <div className="text-emerald-400">$ python voyager_brain.py</div>
            </div>
          </div>
          <p className="text-[11px] text-zinc-400 leading-relaxed">
            执行完毕后，手机屏幕上会立即浮现来自大模型的 Toast 提示，并自动点击界面上的按钮，同时控制台打印出毫秒级的执行结果报告！
          </p>
        </div>
      </div>
    </div>
  );
};
