import React, { useState } from 'react';
import { 
  Bot, 
  Send, 
  Layers, 
  Database, 
  Repeat, 
  Smartphone, 
  Radio, 
  Code2, 
  CheckCircle2, 
  AlertTriangle,
  Zap,
  ShieldAlert,
  ArrowRight
} from 'lucide-react';

export const ArchitectureView: React.FC = () => {
  const [activeStep, setActiveStep] = useState<number>(0);

  const steps = [
    {
      title: '1. 环境感知 (Perception)',
      actor: 'Xposed In-Process -> Brain',
      desc: '目标 App 触发状态变化或收到指令后，ViewInspector 抓取当前 Activity 顶层 DecorView 结构，过滤提取关键交互元素文本与 ID，生成结构化 JSON 状态摘要。',
      color: 'border-blue-500/50 bg-blue-500/10 text-blue-400'
    },
    {
      title: '2. 技能检索 (Skill Retrieval)',
      actor: 'Brain (LLM Controller)',
      desc: '大模型根据当前任务意图（如“签到领金币”），从本地向量技能库（Skill Library）中语义召回历史已验证的高相关 JS 技能片段（如 safeClickByText）。',
      color: 'border-purple-500/50 bg-purple-500/10 text-purple-400'
    },
    {
      title: '3. 动态代码生成 (Code Generation)',
      actor: 'Brain (LLM Controller)',
      desc: 'LLM 根据系统 Prompt 规范，直接组合现有技能函数，生成一段自包含的完整 JavaScript 匿名闭包代码，调用暴露的全局 app.* 接口。',
      color: 'border-emerald-500/50 bg-emerald-500/10 text-emerald-400'
    },
    {
      title: '4. 双向 IPC 传输 (Transport)',
      actor: 'WebSocket (127.0.0.1:28888)',
      desc: '通过 WebSocket 将代码封包下发至目标 App 进程内监听的 AgentServer。支持 adb forward 跨设备穿透，毫秒级无损传输。',
      color: 'border-amber-500/50 bg-amber-500/10 text-amber-400'
    },
    {
      title: '5. 进程内沙箱执行 (In-Process Eval)',
      actor: 'Xposed -> QuickJS / Rhino',
      desc: '目标进程内的 JS 引擎以沙箱形式执行该代码段，直接在主线程反射操作 Android 原生 View 或调用私有业务逻辑方法，捕获执行返回值。',
      color: 'border-cyan-500/50 bg-cyan-500/10 text-cyan-400'
    },
    {
      title: '6. 自纠错与技能沉淀 (Critic & Memorize)',
      actor: 'Brain Feedback Loop',
      desc: '若发生 ViewNotFoundException 或语法报错，错误调用栈作为 Feedback 注入下一轮 Prompt 实现自纠错；若执行成功，将代码提炼命名持久化到技能库。',
      color: 'border-rose-500/50 bg-rose-500/10 text-rose-400'
    }
  ];

  return (
    <div className="space-y-8">
      {/* Intro banner */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="space-y-1">
            <span className="text-xs font-mono text-emerald-400 tracking-wider uppercase">Architecture Blueprint</span>
            <h2 className="text-xl font-bold text-zinc-100">
              Voyager 在 Android 端的核心架构：大脑与手脚分层解耦
            </h2>
            <p className="text-sm text-zinc-400 max-w-3xl">
              Voyager 的灵魂在于“<strong className="text-zinc-200">代码即动作，终身技能库自演进</strong>”。结合 LSPosed 的能力，我们可以彻底摆脱传统 Accessibility 无障碍服务“动作迟缓、无法探测内部状态、易被反作弊检测”的缺陷，实现进程内的全知全能自动化。
            </p>
          </div>
          <div className="flex items-center space-x-2 text-xs font-mono text-zinc-400 bg-zinc-950 px-3 py-2 rounded-lg border border-zinc-800 shrink-0">
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
            <span>架构模式：Controller-Worker Pattern</span>
          </div>
        </div>
      </div>

      {/* 4-Layer visual block */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Layer 1: Brain */}
        <div className="bg-zinc-900/90 border border-purple-500/30 rounded-xl p-5 space-y-4 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-3 opacity-10">
            <Bot className="w-16 h-16 text-purple-400" />
          </div>
          <div className="flex items-center space-x-2 text-purple-400">
            <Bot className="w-4 h-4" />
            <h3 className="text-sm font-bold tracking-tight">第一层：LLM 控制大脑 (Brain)</h3>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            宿主于 PC / 服务器或独立手机 App，负责认知推理、自动课程规划、Prompt 拼装与向量检索。
          </p>
          <div className="space-y-2 text-xs">
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-purple-300 font-mono font-medium block">Automatic Curriculum</span>
              <span className="text-zinc-500 text-[11px]">任务难度梯度递增，自主探索</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-purple-300 font-mono font-medium block">Skill Manager (ChromaDB)</span>
              <span className="text-zinc-500 text-[11px]">持久化存储可组合的 JS 技能函数</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-purple-300 font-mono font-medium block">Self-Critic & Reprompt</span>
              <span className="text-zinc-500 text-[11px]">基于执行 StackTrace 自主排错</span>
            </div>
          </div>
        </div>

        {/* Layer 2: IPC */}
        <div className="bg-zinc-900/90 border border-amber-500/30 rounded-xl p-5 space-y-4 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-3 opacity-10">
            <Radio className="w-16 h-16 text-amber-400" />
          </div>
          <div className="flex items-center space-x-2 text-amber-400">
            <Radio className="w-4 h-4" />
            <h3 className="text-sm font-bold tracking-tight">第二层：高吞吐 IPC 信道</h3>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            负责跨进程 / 跨设备传输代码包、UI 树与报错日志，要求零阻塞、双向实时流式通信。
          </p>
          <div className="space-y-2 text-xs">
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-amber-300 font-mono font-medium block">HTTP / WS Server (28888)</span>
              <span className="text-zinc-500 text-[11px]">常驻在目标 App 内存线程中</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-amber-300 font-mono font-medium block">ADB Port Forwarding</span>
              <span className="text-zinc-500 text-[11px]">adb forward tcp:28888 tcp:28888 穿透</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-amber-300 font-mono font-medium block">Full-duplex Event Stream</span>
              <span className="text-zinc-500 text-[11px]">实时回传 console.log 与异常栈</span>
            </div>
          </div>
        </div>

        {/* Layer 3: In-Process Worker */}
        <div className="bg-zinc-900/90 border border-emerald-500/30 rounded-xl p-5 space-y-4 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-3 opacity-10">
            <Code2 className="w-16 h-16 text-emerald-400" />
          </div>
          <div className="flex items-center space-x-2 text-emerald-400">
            <Code2 className="w-4 h-4" />
            <h3 className="text-sm font-bold tracking-tight">第三层：Xposed 进程内模块</h3>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            由 LSPosed 注入到目标 App 进程空间，扮演执行手脚。包含动态脚本引擎与宿主反射桥。
          </p>
          <div className="space-y-2 text-xs">
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-emerald-300 font-mono font-medium block">QuickJS / Rhino Engine</span>
              <span className="text-zinc-500 text-[11px]">即时执行 LLM 下发的 JS 闭包代码</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-emerald-300 font-mono font-medium block">JsBridge (app.* API)</span>
              <span className="text-zinc-500 text-[11px]">封装点击、滑动、查找、Toast 接口</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-emerald-300 font-mono font-medium block">Java Reflection Proxy</span>
              <span className="text-zinc-500 text-[11px]">可直接调用宿主任意私有类与方法</span>
            </div>
          </div>
        </div>

        {/* Layer 4: Target App */}
        <div className="bg-zinc-900/90 border border-cyan-500/30 rounded-xl p-5 space-y-4 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-3 opacity-10">
            <Smartphone className="w-16 h-16 text-cyan-400" />
          </div>
          <div className="flex items-center space-x-2 text-cyan-400">
            <Smartphone className="w-4 h-4" />
            <h3 className="text-sm font-bold tracking-tight">第四层：目标宿主 App</h3>
          </div>
          <p className="text-xs text-zinc-400 leading-relaxed">
            运行在 Android ART 运行时中的任意被控应用，拥有自己的 View 树与本地内存数据。
          </p>
          <div className="space-y-2 text-xs">
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-cyan-300 font-mono font-medium block">DecorView & Window</span>
              <span className="text-zinc-500 text-[11px]">无需无障碍权限，直接遍历 View 树</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-cyan-300 font-mono font-medium block">Main Looper Dispatcher</span>
              <span className="text-zinc-500 text-[11px]">通过 UI Handler 调度安全事件</span>
            </div>
            <div className="bg-zinc-950 p-2 rounded border border-zinc-800/80">
              <span className="text-cyan-300 font-mono font-medium block">Internal Singletons</span>
              <span className="text-zinc-500 text-[11px]">绕过 UI 直接触发网络请求/数据刷新</span>
            </div>
          </div>
        </div>
      </div>

      {/* Voyager Loop Interactive Walkthrough */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-zinc-100 flex items-center space-x-2">
              <Repeat className="w-4 h-4 text-emerald-400" />
              <span>Voyager 智能体自演进闭环 (The Voyager Cycle)</span>
            </h3>
            <p className="text-xs text-zinc-400 mt-1">
              点击下方阶段，理解智能体从环境感知到生成代码、执行、自纠错与持久化技能库的完整生命周期：
            </p>
          </div>
          <div className="flex items-center space-x-1">
            <span className="text-xs text-zinc-500">当前阶段:</span>
            <span className="text-xs font-mono font-semibold text-emerald-400">Step {activeStep + 1} / 6</span>
          </div>
        </div>

        {/* Steps navigation bar */}
        <div className="grid grid-cols-2 md:grid-cols-6 gap-2">
          {steps.map((st, idx) => (
            <button
              key={idx}
              onClick={() => setActiveStep(idx)}
              className={`p-3 rounded-lg text-left border transition-all text-xs font-mono ${
                activeStep === idx 
                  ? 'bg-zinc-800 border-emerald-500 text-zinc-100 ring-1 ring-emerald-500/50' 
                  : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:border-zinc-700'
              }`}
            >
              <div className="font-semibold">{idx + 1}. {st.title.split(' ')[1]}</div>
              <div className="text-[10px] text-zinc-500 truncate mt-0.5">{st.actor}</div>
            </button>
          ))}
        </div>

        {/* Active step detail card */}
        <div className={`p-4 rounded-xl border ${steps[activeStep].color} space-y-2 transition-all`}>
          <div className="flex items-center justify-between">
            <span className="text-sm font-bold tracking-tight">{steps[activeStep].title}</span>
            <span className="text-xs font-mono bg-zinc-950/60 px-2.5 py-1 rounded border border-zinc-700/50">
              主体: {steps[activeStep].actor}
            </span>
          </div>
          <p className="text-xs leading-relaxed text-zinc-300">
            {steps[activeStep].desc}
          </p>
        </div>
      </div>

      {/* Xposed vs Accessibility Comparison Box */}
      <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-5 space-y-3">
        <h4 className="text-sm font-semibold text-zinc-200 flex items-center space-x-2">
          <Zap className="w-4 h-4 text-amber-400" />
          <span>核心设计优势：为什么用 Xposed 进程内 Hook，而不是传统无障碍服务 (AccessibilityService)？</span>
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs pt-1">
          <div className="space-y-1 bg-zinc-900/60 p-3 rounded-lg border border-zinc-800">
            <span className="text-zinc-200 font-semibold flex items-center space-x-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>零延迟 + 主线程操作</span>
            </span>
            <p className="text-zinc-400 text-[11px] leading-relaxed">
              无障碍服务依赖跨进程 IPC 事件通知与窗口轮询，延迟高达 200~800ms；Xposed 运行在 App 进程内，通过 View.performClick() 可以在微秒级完成调用。
            </p>
          </div>
          <div className="space-y-1 bg-zinc-900/60 p-3 rounded-lg border border-zinc-800">
            <span className="text-zinc-200 font-semibold flex items-center space-x-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>全知视界 (Internal Memory Access)</span>
            </span>
            <p className="text-zinc-400 text-[11px] leading-relaxed">
              无障碍只能看到带有文字或 contentDescription 的 UI；Xposed 可直接反射获取 View 的 Tag、ViewModel 状态、Adapter 数据源乃至全局单例对象。
            </p>
          </div>
          <div className="space-y-1 bg-zinc-900/60 p-3 rounded-lg border border-zinc-800">
            <span className="text-zinc-200 font-semibold flex items-center space-x-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>突破 UI 限制直接调用方法</span>
            </span>
            <p className="text-zinc-400 text-[11px] leading-relaxed">
              遇到弹窗遮挡或深度嵌套列表时，Agent 可以直接调用后台 Manager 的签到接口，无需模拟多次滑动，执行成功率提升数倍。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
