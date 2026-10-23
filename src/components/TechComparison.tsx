import React, { useState } from 'react';
import { 
  Check, 
  X, 
  Star, 
  ArrowRight, 
  HelpCircle, 
  Cpu, 
  Radio, 
  Zap, 
  ShieldCheck, 
  SlidersHorizontal 
} from 'lucide-react';
import { EXECUTION_TECH_OPTIONS, IPC_TECH_OPTIONS, TechOption } from '../data/architectData';

export const TechComparison: React.FC = () => {
  const [activeCategory, setActiveCategory] = useState<'execution' | 'ipc'>('execution');
  const [selectedTech, setSelectedTech] = useState<string>(
    activeCategory === 'execution' ? EXECUTION_TECH_OPTIONS[0].name : IPC_TECH_OPTIONS[0].name
  );

  const currentOptions = activeCategory === 'execution' ? EXECUTION_TECH_OPTIONS : IPC_TECH_OPTIONS;
  const currentDetail = currentOptions.find(o => o.name === selectedTech) || currentOptions[0];

  return (
    <div className="space-y-6">
      {/* Category selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-zinc-900 border border-zinc-800 p-4 rounded-xl">
        <div className="flex items-center space-x-2">
          <SlidersHorizontal className="w-4 h-4 text-emerald-400" />
          <span className="text-sm font-semibold text-zinc-100">技术选型评测维度</span>
        </div>

        <div className="flex bg-zinc-950 p-1 rounded-lg border border-zinc-800">
          <button
            onClick={() => {
              setActiveCategory('execution');
              setSelectedTech(EXECUTION_TECH_OPTIONS[0].name);
            }}
            className={`flex items-center space-x-2 px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
              activeCategory === 'execution'
                ? 'bg-zinc-800 text-emerald-400 border border-zinc-700 shadow-sm'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Cpu className="w-3.5 h-3.5" />
            <span>问题 1：进程内动态执行引擎 (JS vs Dex)</span>
          </button>

          <button
            onClick={() => {
              setActiveCategory('ipc');
              setSelectedTech(IPC_TECH_OPTIONS[0].name);
            }}
            className={`flex items-center space-x-2 px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
              activeCategory === 'ipc'
                ? 'bg-zinc-800 text-emerald-400 border border-zinc-700 shadow-sm'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>问题 2：大脑与手脚 IPC 信道 (WS vs LocalSocket)</span>
          </button>
        </div>
      </div>

      {/* Main comparison grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left option list */}
        <div className="lg:col-span-5 space-y-3">
          <h3 className="text-xs font-mono text-zinc-400 uppercase tracking-wider px-1">
            备选方案列表 (点击查看深度推导)
          </h3>
          <div className="space-y-2">
            {currentOptions.map((opt) => {
              const isSelected = opt.name === currentDetail.name;
              return (
                <div
                  key={opt.name}
                  onClick={() => setSelectedTech(opt.name)}
                  className={`p-4 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-zinc-800/90 border-emerald-500 shadow-lg ring-1 ring-emerald-500/40'
                      : 'bg-zinc-900/60 border-zinc-800 hover:border-zinc-700 hover:bg-zinc-800/40'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="text-sm font-semibold text-zinc-100">{opt.name}</span>
                        {opt.recommended && (
                          <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                            推荐方案
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-1 mt-1.5">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <Star
                            key={star}
                            className={`w-3 h-3 ${
                              star <= Math.floor(opt.rating)
                                ? 'text-amber-400 fill-amber-400'
                                : opt.rating >= star - 0.5
                                ? 'text-amber-400 fill-amber-400/50'
                                : 'text-zinc-700'
                            }`}
                          />
                        ))}
                        <span className="text-[11px] font-mono text-zinc-400 ml-1.5">{opt.rating.toFixed(1)} / 5.0</span>
                      </div>
                    </div>
                  </div>

                  <p className="text-xs text-zinc-400 mt-2 line-clamp-2 leading-relaxed">
                    {opt.verdict}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right detail deep-dive */}
        <div className="lg:col-span-7 bg-zinc-900 border border-zinc-800 rounded-xl p-6 space-y-5">
          <div className="flex items-center justify-between pb-4 border-b border-zinc-800">
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-base font-bold text-zinc-100">{currentDetail.name}</span>
                {currentDetail.recommended && (
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    首选推荐
                  </span>
                )}
              </div>
              <span className="text-xs text-zinc-400 mt-1 block">
                Voyager 范式匹配度: <strong className="text-emerald-400 font-normal">{currentDetail.voyagerFit}</strong>
              </span>
            </div>
            <div className="text-right">
              <span className="text-2xl font-mono font-bold text-amber-400">{currentDetail.rating}</span>
              <span className="text-xs text-zinc-500 block">综合评分</span>
            </div>
          </div>

          {/* Pros & Cons */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2 bg-zinc-950/70 p-4 rounded-lg border border-zinc-800">
              <span className="text-xs font-semibold text-emerald-400 flex items-center space-x-1.5">
                <Check className="w-4 h-4" />
                <span>核心优势 (Pros)</span>
              </span>
              <ul className="space-y-1.5 text-xs text-zinc-300">
                {currentDetail.pros.map((p, i) => (
                  <li key={i} className="flex items-start space-x-1.5">
                    <span className="text-emerald-500/70 mt-0.5">•</span>
                    <span className="leading-relaxed">{p}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="space-y-2 bg-zinc-950/70 p-4 rounded-lg border border-zinc-800">
              <span className="text-xs font-semibold text-rose-400 flex items-center space-x-1.5">
                <X className="w-4 h-4" />
                <span>潜在代价与局限 (Cons)</span>
              </span>
              <ul className="space-y-1.5 text-xs text-zinc-300">
                {currentDetail.cons.map((c, i) => (
                  <li key={i} className="flex items-start space-x-1.5">
                    <span className="text-rose-500/70 mt-0.5">•</span>
                    <span className="leading-relaxed">{c}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Architecture Verdict */}
          <div className="bg-zinc-950 p-4 rounded-lg border border-zinc-800 space-y-1.5">
            <span className="text-xs font-mono text-zinc-400 flex items-center space-x-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>架构师权威结论 (Architectural Verdict)</span>
            </span>
            <p className="text-xs leading-relaxed text-zinc-300">
              {currentDetail.verdict}
            </p>
          </div>

          {/* Why JS Engine wins over Dex Loading summary */}
          {activeCategory === 'execution' && (
            <div className="p-4 rounded-lg bg-emerald-500/5 border border-emerald-500/20 text-xs text-zinc-300 space-y-2">
              <span className="font-semibold text-emerald-400 block">💡 为什么在 Voyager 架构中强烈推荐 JS 引擎而非动态加载 Dex？</span>
              <p className="leading-relaxed text-[11px] text-zinc-400">
                Voyager 的成功核心在于<strong>“高频试错与自纠错反馈”</strong>。智能体为了探索一个新动作，可能需要连续尝试 5~10 次代码修改。
                如果采用 Dex 方案，每次都需要在外部启动 d8 工具链编译，不仅单次延迟达数秒，且 Dalvik/ART 加载新 Dex 类后几乎无法被 GC 回收，运行两小时就会引发 Metaspace OOM 崩溃。
                而 JS 引擎是即时解析执行，单次运行仅耗时数毫秒，且执行完毕后作用域即可被轻量回收，技能库直接保存为纯文本 .js 函数即可无缝复用。
              </p>
            </div>
          )}

          {/* Why WebSocket wins over Binder */}
          {activeCategory === 'ipc' && (
            <div className="p-4 rounded-lg bg-emerald-500/5 border border-emerald-500/20 text-xs text-zinc-300 space-y-2">
              <span className="font-semibold text-emerald-400 block">💡 为什么首选内嵌 WebSocket 而非 Android Binder/AIDL？</span>
              <p className="leading-relaxed text-[11px] text-zinc-400">
                1. <strong>开发解耦</strong>：LLM 大脑通常运行在 PC 端（Python / Node.js），便于调用各类向量数据库与重型大模型 SDK。WebSocket 天然与语言无关。
                2. <strong>无 1MB 事务上限</strong>：Android Binder 限制一次传输不超过 1MB，对于复杂的 App View 树（包含层级、文本、坐标）极易触发 <code>TransactionTooLargeException</code>。
                3. <strong>全双工流式</strong>：手机端执行 JS 代码时的 <code>console.log()</code> 和异常堆栈可以像 DevTools 一样实时流式打印到主脑终端。
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
