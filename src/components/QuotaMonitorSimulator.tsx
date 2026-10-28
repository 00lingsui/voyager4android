import React, { useState, useEffect } from 'react';
import { 
  RotateCw, 
  ExternalLink, 
  CheckCircle2, 
  AlertTriangle, 
  ShieldAlert, 
  Zap, 
  Clock, 
  Calendar, 
  Code2, 
  Sparkles, 
  Sliders, 
  Check, 
  Copy, 
  ChevronRight, 
  Layers, 
  Eye, 
  Smartphone 
} from 'lucide-react';

export const QuotaMonitorSimulator: React.FC = () => {
  // 核心数据状态 (支持实时倒计时和调整)
  const [shortLabel, setShortLabel] = useState('5h');
  const [shortPercent, setShortPercent] = useState(9);
  const [shortSecondsLeft, setShortSecondsLeft] = useState(24 * 60 + 35); // 24m 35s

  const [weeklyLabel, setWeeklyLabel] = useState('每周');
  const [weeklyPercent, setWeeklyPercent] = useState(6);
  const [weeklySecondsLeft, setWeeklySecondsLeft] = useState((4 * 24 * 3600) + (3 * 3600) + 1200); // 4d 3h 20m

  // 交互状态
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [copiedTab, setCopiedTab] = useState<string | null>(null);
  const [activeCodeTab, setActiveCodeTab] = useState<'floating' | 'ui' | 'parser' | 'hook' | 'spec'>('floating');
  const [lastScrapedTime, setLastScrapedTime] = useState<string>('刚刚 (来自 OkHttp x-ratelimit 响应头)');

  // 动态真实倒计时 Loop (每秒递减)
  useEffect(() => {
    const timer = setInterval(() => {
      setShortSecondsLeft((prev) => (prev > 0 ? prev - 1 : 24 * 60));
      setWeeklySecondsLeft((prev) => (prev > 0 ? prev - 1 : 7 * 24 * 3600));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // 格式化短期倒计时: (24m), (1h12m)
  const formatShortCountdown = (secs: number) => {
    if (secs <= 0) return '(已重置)';
    const hours = Math.floor(secs / 3600);
    const mins = Math.floor((secs % 3600) / 60);
    if (hours > 0) {
      return `(${hours}h${mins > 0 ? `${mins}m` : ''})`;
    }
    return `(${Math.max(1, mins)}m)`;
  };

  // 格式化周倒计时: (4d3h), (18h)
  const formatWeeklyCountdown = (secs: number) => {
    if (secs <= 0) return '(已重置)';
    const days = Math.floor(secs / 86400);
    const hours = Math.floor((secs % 86400) / 3600);
    const mins = Math.floor((secs % 3600) / 60);
    if (days > 0) {
      return `(${days}d${hours > 0 ? `${hours}h` : ''})`;
    }
    if (hours > 0) {
      return `(${hours}h${mins > 0 ? `${mins}m` : ''})`;
    }
    return `(${Math.max(1, mins)}m)`;
  };

  // 获取状态颜色 (绿色、黄色警示、红色告罄)
  const getProgressColor = (percent: number) => {
    if (percent >= 90) return { bg: 'bg-rose-500', text: 'text-rose-400', border: 'border-rose-500/50', glow: 'shadow-rose-500/30' };
    if (percent >= 70) return { bg: 'bg-amber-500', text: 'text-amber-400', border: 'border-amber-500/50', glow: 'shadow-amber-500/30' };
    return { bg: 'bg-emerald-500', text: 'text-emerald-400', border: 'border-emerald-500/50', glow: 'shadow-emerald-500/30' };
  };

  const shortColor = getProgressColor(shortPercent);
  const weeklyColor = getProgressColor(weeklyPercent);

  // 点击 ↻ 刷新
  const handleManualRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
      setLastScrapedTime(new Date().toLocaleTimeString('zh-CN', { hour12: false }) + ' (主动探针校验成功)');
    }, 700);
  };

  // 预设情景切换
  const applyPreset = (type: 'healthy' | 'warning' | 'limit') => {
    if (type === 'healthy') {
      setShortPercent(9);
      setShortSecondsLeft(24 * 60 + 15);
      setWeeklyPercent(6);
      setWeeklySecondsLeft((4 * 24 * 3600) + (3 * 3600));
    } else if (type === 'warning') {
      setShortPercent(78);
      setShortSecondsLeft(14 * 60 + 40);
      setWeeklyPercent(68);
      setWeeklySecondsLeft((1 * 24 * 3600) + (8 * 3600));
    } else if (type === 'limit') {
      setShortPercent(100);
      setShortSecondsLeft(42 * 60 + 10);
      setWeeklyPercent(96);
      setWeeklySecondsLeft(3 * 3600 + 45 * 60);
    }
  };

  const copyText = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedTab(id);
    setTimeout(() => setCopiedTab(null), 2000);
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* 顶部视觉介绍 */}
      <div className="bg-gradient-to-r from-zinc-900 via-zinc-900/95 to-emerald-950/40 border border-emerald-500/30 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-mono font-bold bg-emerald-500 text-zinc-950">
                <Sparkles className="w-3.5 h-3.5" />
                <span>核心功能重构 Milestone 1</span>
              </span>
              <span className="text-xs font-mono text-emerald-400 bg-emerald-950/60 border border-emerald-500/20 px-2.5 py-1 rounded-full">
                Gemini 额度与刷新时间监控 UI
              </span>
              <span className="text-xs font-mono text-zinc-400 bg-zinc-800 px-2.5 py-1 rounded-full border border-zinc-700">
                StatusBar / Bar 样式
              </span>
            </div>

            <h2 className="text-2xl sm:text-3xl font-extrabold text-zinc-100 tracking-tight">
              Gemini 额度与刷新倒计时悬浮条 (StatusBar Capsule)
            </h2>
            <p className="text-xs sm:text-sm text-zinc-300 max-w-3xl leading-relaxed">
              针对 Gemini 原生 Android 客户端深度打造的高灵敏度额度条。采用精致极简的<strong>圆角扁平胶囊 Bar</strong>形态，单行并排显示<strong>【短期/5h 额度】</strong>与<strong>【每周额度】</strong>用量百分比与下一次刷新精确倒计时，并集成 <strong>↻ 实时刷新</strong> 与 <strong>🗗 展开详情弹窗</strong>。
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 shrink-0">
            <button
              onClick={() => applyPreset('healthy')}
              className="px-3 py-2 rounded-xl text-xs font-mono font-semibold bg-emerald-950/60 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-900/40 transition-all cursor-pointer"
            >
              🟢 充裕状态 (9%)
            </button>
            <button
              onClick={() => applyPreset('warning')}
              className="px-3 py-2 rounded-xl text-xs font-mono font-semibold bg-amber-950/60 text-amber-400 border border-amber-500/30 hover:bg-amber-900/40 transition-all cursor-pointer"
            >
              🟡 预警状态 (78%)
            </button>
            <button
              onClick={() => applyPreset('limit')}
              className="px-3 py-2 rounded-xl text-xs font-mono font-semibold bg-rose-950/60 text-rose-400 border border-rose-500/30 hover:bg-rose-900/40 transition-all cursor-pointer"
            >
              🔴 额度告罄 (100%)
            </button>
          </div>
        </div>
      </div>

      {/* 真实交互模拟沙盒：模拟手机顶部 Status Bar 与 Gemini 对话界面 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 左侧：手机模拟屏幕 (渲染真实胶囊 Bar) */}
        <div className="lg:col-span-7 bg-zinc-950 border border-zinc-800 rounded-2xl p-6 shadow-2xl flex flex-col justify-between relative overflow-hidden">
          <div className="flex items-center justify-between border-b border-zinc-800/80 pb-3 mb-4">
            <div className="flex items-center space-x-2">
              <Smartphone className="w-4 h-4 text-emerald-400" />
              <span className="text-xs font-mono text-zinc-300 font-semibold">Gemini Android 原生界面挂载仿真</span>
            </div>
            <span className="text-[11px] font-mono text-emerald-400 bg-emerald-950/40 border border-emerald-500/20 px-2 py-0.5 rounded-full">WindowManager 顶部独立悬浮胶囊 Overlay</span>
          </div>

          {/* 模拟手机外壳区域 */}
          <div className="w-full max-w-md mx-auto bg-zinc-900 border-2 border-zinc-700/60 rounded-3xl p-3 shadow-2xl relative min-h-[460px] flex flex-col justify-between">
            {/* 手机系统状态栏 (模拟时间、WiFi、电量) */}
            <div className="flex items-center justify-between text-[11px] font-mono text-zinc-400 px-3 py-1">
              <span>09:41</span>
              <div className="w-16 h-3.5 bg-zinc-950 rounded-full mx-auto" />
              <div className="flex items-center space-x-1.5">
                <span>5G</span>
                <span>100%</span>
              </div>
            </div>

            {/* 核心需求组件：胶囊型/圆角扁平的悬浮栏 (Bar Style) */}
            <div className="my-3 flex justify-center">
              <div 
                className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-[#141418] border border-[#272732] shadow-xl text-xs font-mono select-none hover:border-emerald-500/50 transition-all group cursor-pointer"
                title="单击展开详情或拖拽移动"
              >
                {/* 元素 1: 短期额度 (如 5h) */}
                <div className="flex items-center space-x-1.5">
                  <span className="font-bold text-zinc-200">{shortLabel}</span>
                  {/* 进度小条与小绿点 */}
                  <div className="w-8 h-1 bg-zinc-800 rounded-full overflow-hidden flex items-center">
                    <div 
                      className={`h-full ${shortColor.bg} rounded-full transition-all duration-300`} 
                      style={{ width: `${Math.min(100, Math.max(8, shortPercent))}%` }} 
                    />
                  </div>
                  <span className={`w-1.5 h-1.5 rounded-full ${shortColor.bg} shrink-0 inline-block`} />
                  <span className="text-zinc-200 font-semibold">{shortPercent}%</span>
                  <span className="text-zinc-400 text-[10px]">{formatShortCountdown(shortSecondsLeft)}</span>
                </div>

                {/* 分隔圆点 */}
                <span className="text-zinc-600 text-xs font-bold">·</span>

                {/* 元素 2: 每周额度 */}
                <div className="flex items-center space-x-1.5">
                  <span className="font-bold text-zinc-200">{weeklyLabel}</span>
                  <div className="w-8 h-1 bg-zinc-800 rounded-full overflow-hidden flex items-center">
                    <div 
                      className={`h-full ${weeklyColor.bg} rounded-full transition-all duration-300`} 
                      style={{ width: `${Math.min(100, Math.max(8, weeklyPercent))}%` }} 
                    />
                  </div>
                  <span className={`w-1.5 h-1.5 rounded-full ${weeklyColor.bg} shrink-0 inline-block`} />
                  <span className="text-zinc-200 font-semibold">{weeklyPercent}%</span>
                  <span className="text-zinc-400 text-[10px]">{formatWeeklyCountdown(weeklySecondsLeft)}</span>
                </div>

                {/* 竖分割线 */}
                <span className="h-3.5 w-[1px] bg-zinc-700/80 mx-1" />

                {/* 元素 3: 右侧操作按钮 (↻ 刷新 与 🗗 展开/跳转) */}
                <div className="flex items-center space-x-1 text-zinc-400">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleManualRefresh();
                    }}
                    className="p-1 hover:text-emerald-400 active:scale-90 transition-transform cursor-pointer"
                    title="手动刷新额度"
                  >
                    <RotateCw className={`w-3 h-3 ${isRefreshing ? 'animate-spin text-emerald-400' : ''}`} />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setShowDetailModal(true);
                    }}
                    className="p-1 hover:text-emerald-400 active:scale-90 transition-transform cursor-pointer"
                    title="展开完整额度与账号状态页"
                  >
                    <ExternalLink className="w-3 h-3" />
                  </button>
                </div>
              </div>
            </div>

            {/* 模拟 Gemini 消息内容 */}
            <div className="flex-1 space-y-3 px-2 py-3 overflow-y-auto">
              <div className="bg-zinc-800/60 border border-zinc-700/50 rounded-2xl p-3 text-xs text-zinc-200 space-y-1">
                <div className="flex items-center space-x-1 text-[10px] text-emerald-400 font-mono">
                  <Sparkles className="w-3 h-3" />
                  <span>Gemini 2.0 Flash Thinking</span>
                </div>
                <p className="leading-relaxed">
                  你好！我是 Gemini。顶部已实时为你挂载用量胶囊：你当前的 5 小时滚动窗口仅消耗 {shortPercent}%，每周额度也十分充裕。
                </p>
              </div>

              <div className="bg-emerald-950/30 border border-emerald-500/20 rounded-xl p-2.5 text-[11px] font-mono text-zinc-300">
                <div className="flex items-center justify-between text-zinc-400 text-[10px]">
                  <span>最新网络探测抓取源</span>
                  <span className="text-emerald-400">OkHttp 拦截器</span>
                </div>
                <p className="text-emerald-300 mt-1 truncate">{lastScrapedTime}</p>
              </div>
            </div>

            {/* 模拟 Gemini 输入框 */}
            <div className="border-t border-zinc-800 pt-2 px-1">
              <div className="bg-zinc-800/90 border border-zinc-700 rounded-full px-4 py-2 flex items-center justify-between text-xs text-zinc-400">
                <span>给 Gemini 发送消息...</span>
                <span className="w-6 h-6 rounded-full bg-emerald-500 flex items-center justify-center text-zinc-950 font-bold">↑</span>
              </div>
            </div>
          </div>

          <div className="mt-4 text-center">
            <span className="text-xs text-zinc-400">
              💡 提示：点击胶囊条上的 <strong className="text-emerald-400">↻</strong> 可手动触发网络探针刷新，点击 <strong className="text-emerald-400">🗗</strong> 可展开完整额度详情卡片。
            </span>
          </div>
        </div>

        {/* 右侧：实时数据调整控制台与动态逻辑 */}
        <div className="lg:col-span-5 bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-5">
          <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
            <div className="flex items-center space-x-2">
              <Sliders className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-zinc-200">额度数据仿真控制器</h3>
            </div>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-zinc-800 text-zinc-400">动态双向联动</span>
          </div>

          {/* 控制短期额度 */}
          <div className="space-y-3 bg-zinc-950/70 border border-zinc-800/80 rounded-xl p-4">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-zinc-200">短期额度 (5h 滚动窗口)</span>
              <span className={`font-mono font-bold ${shortColor.text}`}>{shortPercent}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={shortPercent}
              onChange={(e) => setShortPercent(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
            <div className="flex items-center justify-between text-[11px] text-zinc-400">
              <span>模型标示:</span>
              <div className="flex items-center space-x-1">
                {['5h', '1h', 'Pro', 'Flash'].map((lbl) => (
                  <button
                    key={lbl}
                    onClick={() => setShortLabel(lbl)}
                    className={`px-2 py-0.5 rounded text-[10px] font-mono cursor-pointer ${
                      shortLabel === lbl ? 'bg-emerald-500 text-zinc-950 font-bold' : 'bg-zinc-800 text-zinc-300'
                    }`}
                  >
                    {lbl}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 控制每周额度 */}
          <div className="space-y-3 bg-zinc-950/70 border border-zinc-800/80 rounded-xl p-4">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-zinc-200">每周总额度 (Weekly)</span>
              <span className={`font-mono font-bold ${weeklyColor.text}`}>{weeklyPercent}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={weeklyPercent}
              onChange={(e) => setWeeklyPercent(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
            <div className="flex items-center justify-between text-[11px] text-zinc-400">
              <span>周刷新剩余秒数:</span>
              <span className="font-mono text-zinc-300">{formatWeeklyCountdown(weeklySecondsLeft)} ({weeklySecondsLeft}s)</span>
            </div>
          </div>

          {/* 倒计时与 Hook 数据解析规则速览 */}
          <div className="bg-zinc-950/50 border border-zinc-800 rounded-xl p-4 space-y-2 text-xs">
            <div className="flex items-center space-x-2 text-emerald-400 font-semibold">
              <Clock className="w-3.5 h-3.5" />
              <span>动态时间差计算逻辑 (Delta Parser)</span>
            </div>
            <div className="space-y-1.5 font-mono text-[11px] text-zinc-400">
              <p>• 毫秒差计算: <span className="text-zinc-200">delta = resetTimestamp - now</span></p>
              <p>• 天数转换: <span className="text-emerald-400">days &gt; 0 ? (4d3h) : (24m)</span></p>
              <p>• 状态着色: &gt;90% 红色告警，70-90% 黄色提醒，&lt;70% 绿色充裕</p>
              <p>• 持久存储: 写入 App 私有 <span className="text-zinc-200">gemini_quota_prefs</span> 毫秒级复用</p>
            </div>
          </div>
        </div>
      </div>

      {/* 弹窗：🗗 展开完整额度详情弹窗 (模拟) */}
      {showDetailModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-fade-in">
          <div className="bg-zinc-900 border border-zinc-700 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <div className="flex items-center space-x-2">
                <span className="text-xl">🪐</span>
                <h3 className="text-base font-bold text-zinc-100">Gemini 额度与用量监控中心</h3>
              </div>
              <button
                onClick={() => setShowDetailModal(false)}
                className="text-zinc-400 hover:text-zinc-100 text-sm font-mono cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* 卡片 1: 短期窗口 */}
            <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-4 space-y-1.5">
              <div className="flex items-center justify-between text-xs text-zinc-400">
                <span>短期滚动窗口 (5h Window)</span>
                <span className="text-emerald-400 font-mono font-bold">{shortPercent}% 已用</span>
              </div>
              <div className="w-full h-2 bg-zinc-800 rounded-full overflow-hidden">
                <div className={`h-full ${shortColor.bg} rounded-full`} style={{ width: `${shortPercent}%` }} />
              </div>
              <div className="flex items-center justify-between text-[11px] text-zinc-400 pt-1">
                <span>下次刷新时间:</span>
                <span className="font-mono text-zinc-200">约 {formatShortCountdown(shortSecondsLeft)} 之后</span>
              </div>
            </div>

            {/* 卡片 2: 每周额度 */}
            <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-4 space-y-1.5">
              <div className="flex items-center justify-between text-xs text-zinc-400">
                <span>每周额度配额 (Weekly Quota)</span>
                <span className="text-emerald-400 font-mono font-bold">{weeklyPercent}% 已用</span>
              </div>
              <div className="w-full h-2 bg-zinc-800 rounded-full overflow-hidden">
                <div className={`h-full ${weeklyColor.bg} rounded-full`} style={{ width: `${weeklyPercent}%` }} />
              </div>
              <div className="flex items-center justify-between text-[11px] text-zinc-400 pt-1">
                <span>周配额重置倒计时:</span>
                <span className="font-mono text-zinc-200">{formatWeeklyCountdown(weeklySecondsLeft)}</span>
              </div>
            </div>

            {/* 卡片 3: 账号与模型等级 */}
            <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-4 space-y-1 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-zinc-400">当前账号权限:</span>
                <span className="text-sky-400 font-semibold font-mono">Gemini Advanced 会员</span>
              </div>
              <p className="text-[11px] text-zinc-500 leading-relaxed pt-1">
                包含 Gemini 2.0 Flash / Pro 完整多模态调用权限，支持联网检索与代码执行容器。
              </p>
            </div>

            <div className="flex justify-end space-x-3 pt-2">
              <button
                onClick={() => setShowDetailModal(false)}
                className="px-4 py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-xs font-mono text-zinc-300 cursor-pointer"
              >
                关闭
              </button>
              <button
                onClick={() => {
                  window.open('https://one.google.com/explore-plan', '_blank');
                }}
                className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-xs font-mono font-bold text-zinc-950 cursor-pointer"
              >
                管理 Google One 订阅
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 源码清单与架构代码查看器 */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-zinc-800 pb-4">
          <div className="flex items-center space-x-2">
            <Code2 className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-semibold text-zinc-200">
              功能实现源码清单 (Xposed Hook & UI 绘制组件)
            </h3>
          </div>

          {/* 切换代码 Tab */}
          <div className="flex items-center space-x-1.5 overflow-x-auto">
            {[
              { id: 'floating', label: 'FloatingQuotaView.java (WindowManager)' },
              { id: 'ui', label: 'QuotaMonitorBar.java (DecorView)' },
              { id: 'parser', label: 'QuotaParser.java (数据解析)' },
              { id: 'hook', label: 'HookEntry.java (注入入口)' },
              { id: 'spec', label: 'QuotaData.java (数据模型)' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveCodeTab(tab.id as any)}
                className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-colors whitespace-nowrap cursor-pointer ${
                  activeCodeTab === tab.id
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-semibold'
                    : 'text-zinc-400 hover:text-zinc-200 bg-zinc-800/60'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* 代码展示与复制区 */}
        <div className="relative">
          <div className="absolute top-3 right-3 z-10">
            <button
              onClick={() => {
                copyText(activeCodeTab, activeCodeTab);
              }}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs font-mono border border-zinc-700 transition-colors cursor-pointer shadow-md"
            >
              {copiedTab === activeCodeTab ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400">已复制!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>复制代码</span>
                </>
              )}
            </button>
          </div>

          <div className="p-4 font-mono text-[11px] leading-relaxed text-zinc-300 max-h-96 overflow-y-auto bg-zinc-950 rounded-xl border border-zinc-800">
            {activeCodeTab === 'floating' && (
              <pre>{`// FloatingQuotaView.java (WindowManager 顶部悬浮胶囊 Overlay)
public class FloatingQuotaView extends FrameLayout implements QuotaParser.OnQuotaChangeListener {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;

    public static synchronized FloatingQuotaView show(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        FloatingQuotaView view = new FloatingQuotaView(activity);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, // 依附于 Activity Window
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.x = 0;
        lp.y = getStatusBarHeight(activity) + dp(activity, 8);

        wm.addView(view, lp);
        return view;
    }

    // 触控拖拽支持：实时更新 WindowManager 坐标
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE && mIsDragging) {
            mWindowLayoutParams.x = mInitParamX + (int)(event.getRawX() - mTouchDownRawX);
            mWindowLayoutParams.y = mInitParamY + (int)(event.getRawY() - mTouchDownRawY);
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
            return true;
        }
        return super.onTouchEvent(event);
    }
}`}</pre>
            )}

            {activeCodeTab === 'ui' && (
              <pre>{`// QuotaMonitorBar.java (DecorView 视图挂载实现)
public class QuotaMonitorBar extends LinearLayout implements QuotaParser.OnQuotaChangeListener {
    // 胶囊背景与绿色指示点
    private static final int COLOR_BG = Color.parseColor("#141418");
    private static final int COLOR_GREEN = Color.parseColor("#10B981");

    private TextView mTvShortLabel, mTvShortPercent, mTvShortCountdown;
    private TextView mTvWeeklyLabel, mTvWeeklyPercent, mTvWeeklyCountdown;
    private TextView mBtnRefresh, mBtnExpand;

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        // 圆角 18dp 胶囊外观
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_BG);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#272732"));
        setBackground(bg);

        // 1. 【5h 🟩 9% (24m)】
        buildShortTermSection(context);
        // 2. 中隔圆点
        addDividerDot();
        // 3. 【每周 🟩 6% (4d3h)】
        buildWeeklySection(context);
        // 4. 右侧分割线与操作按钮 [↻] [🗗]
        buildActionsSection(context);

        // 每秒自驱动更新倒计时
        postDelayed(mTimerRunnable, 1000);
    }
}`}</pre>
            )}

            {activeCodeTab === 'parser' && (
              <pre>{`// QuotaParser.java
public class QuotaParser {
    // 策略 1: Hook OkHttp / HttpURLConnection 的 RateLimit 响应头
    public boolean parseHttpHeaders(Map<String, List<String>> headers, Context context) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String lower = entry.getKey().toLowerCase();
            String val = entry.getValue().get(0);
            if (lower.contains("ratelimit-reset") || lower.contains("quota-reset-short")) {
                long sec = Long.parseLong(val);
                mCurrentQuota.shortTermResetTimestamp = System.currentTimeMillis() + (sec * 1000L);
            }
            if (lower.contains("ratelimit-remaining-percent")) {
                mCurrentQuota.shortTermPercent = Integer.parseInt(val);
            }
        }
        mCurrentQuota.saveToPrefs(context);
        notifyListener();
    }

    // 策略 2: 正则匹配 Gemini batchexecute / v1internal 响应体
    public boolean parseResponseBody(String responseText, Context context) {
        // 提取 5h / weekly 配额与毫秒级时间戳
    }
}`}</pre>
            )}

            {activeCodeTab === 'hook' && (
              <pre>{`// HookEntry.java
public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.apps.bard")) return;

        // 在 Activity onPostCreate / onResume 自动向 DecorView 挂载 QuotaMonitorBar
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity act = (Activity) param.thisObject;
                ViewGroup decor = (ViewGroup) act.getWindow().getDecorView();
                if (decor.findViewWithTag("GEMINI_QUOTA_BAR_TAG") == null) {
                    QuotaMonitorBar bar = new QuotaMonitorBar(act);
                    bar.setTag("GEMINI_QUOTA_BAR_TAG");
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    lp.topMargin = getStatusBarHeight(act) + dp(act, 8);
                    decor.addView(bar, lp);
                }
            }
        });
    }
}`}</pre>
            )}

            {activeCodeTab === 'spec' && (
              <pre>{`// QuotaData.java (格式化倒计时逻辑)
public String getFormattedShortCountdown() {
    long delta = shortTermResetTimestamp - System.currentTimeMillis();
    if (delta <= 0) return "(已重置)";
    long hours = TimeUnit.MILLISECONDS.toHours(delta);
    long mins = TimeUnit.MILLISECONDS.toMinutes(delta) % 60;
    if (hours > 0) return "(" + hours + "h" + (mins > 0 ? mins + "m" : "") + ")";
    return "(" + Math.max(1, mins) + "m)";
}

public String getFormattedWeeklyCountdown() {
    long delta = weeklyResetTimestamp - System.currentTimeMillis();
    if (delta <= 0) return "(已重置)";
    long days = TimeUnit.MILLISECONDS.toDays(delta);
    long hours = TimeUnit.MILLISECONDS.toHours(delta) % 24;
    if (days > 0) return "(" + days + "d" + (hours > 0 ? hours + "h" : "") + ")";
    if (hours > 0) return "(" + hours + "h" + ")";
    return "(" + Math.max(1, TimeUnit.MILLISECONDS.toMinutes(delta)) + "m)";
}`}</pre>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
