import React, { useState } from 'react';
import { 
  Play, 
  RotateCcw, 
  Bot, 
  Smartphone, 
  Code2, 
  CheckCircle2, 
  AlertCircle, 
  ArrowRight, 
  Terminal,
  Database,
  Sparkles,
  RefreshCw
} from 'lucide-react';

interface TaskScenario {
  id: string;
  title: string;
  goal: string;
  viewDump: {
    activity: string;
    elements: { id: string; type: string; text: string; clickable: boolean }[];
  };
  retrievedSkills: string[];
  initialJsCode: string;
  initialError?: string;
  correctedJsCode?: string;
  successResult: string;
}

const PRESET_TASKS: TaskScenario[] = [
  {
    id: 'daily_checkin',
    title: '任务 1：自动签到与每日奖励领取 (正常流)',
    goal: '检测当前页面是否在福利页，自动点击“立即签到”并关闭成功弹窗',
    viewDump: {
      activity: 'MainActivity -> BenefitCenterFragment',
      elements: [
        { id: 'tv_title', type: 'TextView', text: '每日签到福利中心', clickable: false },
        { id: 'tv_points', type: 'TextView', text: '当前积分: 1250', clickable: false },
        { id: 'btn_sign', type: 'Button', text: '立即签到', clickable: true },
        { id: 'btn_rules', type: 'TextView', text: '活动规则', clickable: true }
      ]
    },
    retrievedSkills: ['safeClickByText(text, retries)', 'waitForView(id, timeout)'],
    initialJsCode: `(function execute() {
  app.toast("Voyager Agent: 启动签到任务");
  let act = app.getCurrentActivityName();
  
  // 检索并点击签到按钮
  let clicked = app.clickByText("立即签到");
  if (!clicked) {
    throw new Error("未找到'立即签到'按钮");
  }
  
  app.sleep(500);
  app.toast("签到成功！积分已入账");
  return { success: true, reward: "+50 积分", activity: act };
})();`,
    successResult: '{"success": true, "reward": "+50 积分", "activity": "BenefitCenterFragment"}'
  },
  {
    id: 'self_correction',
    title: '任务 2：报错驱动的 LLM 迭代自纠错 (Self-Correction Loop)',
    goal: '点击商品结算，遭遇弹窗遮挡导致未找到目标按钮，触发异常并自主修复',
    viewDump: {
      activity: 'CartActivity (有全屏优惠券弹窗遮挡)',
      elements: [
        { id: 'dialog_coupon', type: 'DialogView', text: '恭喜获得新人优惠券！', clickable: false },
        { id: 'btn_close_coupon', type: 'ImageView', text: '关闭弹窗', clickable: true },
        { id: 'btn_checkout', type: 'Button', text: '立即结算 (2件)', clickable: true }
      ]
    },
    retrievedSkills: ['safeClickByText', 'dismissDialogIfPresent'],
    initialJsCode: `(function execute() {
  // 盲目直接点击结算
  let ok = app.clickByText("立即结算");
  if (!ok) {
    throw new Error("ViewNotFoundException: 目标按钮被弹窗层遮蔽或不存在");
  }
  return "结算成功";
})();`,
    initialError: 'ViewNotFoundException: 目标按钮被弹窗层遮蔽或不存在 at Line 4 in LLM_Skill.js',
    correctedJsCode: `(function execute() {
  app.toast("检测到优惠券弹窗，尝试先关闭遮罩");
  // 自纠错后的代码：先处理弹窗再执行结算
  app.clickByText("关闭弹窗");
  app.sleep(400);

  let ok = app.clickByText("立即结算");
  if (!ok) {
    throw new Error("二次重试仍未找到按钮");
  }
  app.toast("已成功穿透弹窗并点击结算！");
  return { success: true, status: "checkout_entered" };
})();`,
    successResult: '{"success": true, "status": "checkout_entered", "self_corrected": true}'
  }
];

export const SimulationSandbox: React.FC = () => {
  const [selectedTask, setSelectedTask] = useState<TaskScenario>(PRESET_TASKS[0]);
  const [currentStep, setCurrentStep] = useState<number>(0);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [terminalLogs, setTerminalLogs] = useState<string[]>([]);
  const [phoneToast, setPhoneToast] = useState<string | null>(null);
  const [clickedTarget, setClickedTarget] = useState<string | null>(null);

  const resetSandbox = () => {
    setCurrentStep(0);
    setIsRunning(false);
    setTerminalLogs([]);
    setPhoneToast(null);
    setClickedTarget(null);
  };

  const runSimulation = async () => {
    resetSandbox();
    setIsRunning(true);

    const log = (msg: string) => {
      setTerminalLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`]);
    };

    // Step 1: Perception
    setCurrentStep(1);
    log(`[Perception] 正在连接目标 App 进程 (ws://127.0.0.1:28888)...`);
    await new Promise(r => setTimeout(r, 600));
    log(`[Perception] 成功抓取 DecorView 结构，当前 Activity: ${selectedTask.viewDump.activity}`);
    log(`[Perception] 过滤出 ${selectedTask.viewDump.elements.length} 个关键交互元素`);

    // Step 2: Skill retrieval
    setCurrentStep(2);
    await new Promise(r => setTimeout(r, 700));
    log(`[Skill Manager] 向量语义检索技能库 (ChromaDB)，召回技能: ${selectedTask.retrievedSkills.join(', ')}`);

    // Step 3: LLM Code Generation
    setCurrentStep(3);
    await new Promise(r => setTimeout(r, 900));
    log(`[LLM Planner] 模型完成 Prompt 拼装，开始生成并校验 JavaScript 技能代码...`);

    // Step 4: IPC Transfer & In-process execution
    setCurrentStep(4);
    await new Promise(r => setTimeout(r, 600));
    log(`[IPC] WebSocket 下发代码到 Xposed QuickJS 引擎执行`);

    if (selectedTask.initialError) {
      // Failed case -> Trigger Self-Correction
      await new Promise(r => setTimeout(r, 800));
      log(`❌ [QuickJS Runtime Exception] ${selectedTask.initialError}`);
      log(`🔄 [Self-Critic Loop] 捕获异常堆栈，将错误回传给 LLM 进行自纠错重新生成...`);

      setCurrentStep(5); // Self-correcting
      await new Promise(r => setTimeout(r, 1200));
      log(`✨ [LLM Planner] 修复代码生成完毕：先执行 app.clickByText("关闭弹窗") 消除阻碍`);
      log(`[IPC] 重新下发修复版 JS 代码至宿主执行...`);

      // Execute repaired code
      await new Promise(r => setTimeout(r, 700));
      setPhoneToast("检测到优惠券弹窗，尝试先关闭遮罩");
      setClickedTarget("关闭弹窗");
      await new Promise(r => setTimeout(r, 600));
      setPhoneToast("已成功穿透弹窗并点击结算！");
      setClickedTarget("立即结算");

      log(`🎉 [Execution Success] 修复版代码执行成功: ${selectedTask.successResult}`);
      log(`💾 [Skill Library] 将修复技能保存为新复合动作 'dismissAndCheckout' 写入本地库！`);
      setCurrentStep(6);
    } else {
      // Normal success case
      await new Promise(r => setTimeout(r, 700));
      setPhoneToast("Voyager Agent: 启动签到任务");
      setClickedTarget("立即签到");
      await new Promise(r => setTimeout(r, 700));
      setPhoneToast("签到成功！积分已入账");
      log(`🎉 [Execution Success] 执行结果返回: ${selectedTask.successResult}`);
      log(`💾 [Skill Library] 技能验证通过，更新引用计数与成功率统计`);
      setCurrentStep(6);
    }

    setIsRunning(false);
  };

  return (
    <div className="space-y-6">
      {/* Sandbox Header */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-mono text-emerald-400 tracking-wider uppercase">Interactive Simulator</span>
          <h2 className="text-xl font-bold text-zinc-100 mt-1">
            Voyager 核心循环实机仿真器
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            直观体验“感知 -&gt; 技能检索 -&gt; JS 代码生成 -&gt; 进程内执行 -&gt; 报错自纠错 -&gt; 沉淀入库”的端到端数据流动。
          </p>
        </div>

        <div className="flex items-center space-x-3 shrink-0">
          <select
            value={selectedTask.id}
            onChange={(e) => {
              const found = PRESET_TASKS.find(t => t.id === e.target.value);
              if (found) {
                setSelectedTask(found);
                resetSandbox();
              }
            }}
            disabled={isRunning}
            className="bg-zinc-950 border border-zinc-700 text-xs text-zinc-200 rounded-lg px-3 py-2 font-mono focus:outline-none focus:border-emerald-500"
          >
            {PRESET_TASKS.map(t => (
              <option key={t.id} value={t.id}>{t.title}</option>
            ))}
          </select>

          <button
            onClick={runSimulation}
            disabled={isRunning}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-xs font-semibold font-mono transition-all ${
              isRunning
                ? 'bg-zinc-800 text-zinc-500 cursor-not-allowed'
                : 'bg-emerald-500 hover:bg-emerald-400 text-zinc-950 shadow-md shadow-emerald-500/20'
            }`}
          >
            {isRunning ? (
              <>
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                <span>执行中...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>运行仿真流程</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Main Sandbox Area */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Simulated Phone Display */}
        <div className="lg:col-span-4 bg-zinc-900 border border-zinc-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold text-zinc-200 flex items-center space-x-1.5">
              <Smartphone className="w-3.5 h-3.5 text-cyan-400" />
              <span>目标手机屏幕实时渲染 (Mock View)</span>
            </span>
            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-400">
              Host Process
            </span>
          </div>

          {/* Virtual Phone Canvas */}
          <div className="bg-zinc-950 border-2 border-zinc-800 rounded-2xl p-4 min-h-[400px] flex flex-col justify-between relative shadow-inner">
            {/* Phone status bar */}
            <div className="flex items-center justify-between text-[10px] font-mono text-zinc-600 pb-3 border-b border-zinc-900">
              <span>09:41</span>
              <span>LSPosed Active</span>
              <span>100%</span>
            </div>

            {/* In-app UI Elements */}
            <div className="space-y-3 py-4 flex-1">
              <div className="bg-zinc-900/80 p-2.5 rounded-lg border border-zinc-800 text-center">
                <span className="text-xs font-semibold text-zinc-200">
                  {selectedTask.viewDump.elements[0].text}
                </span>
                <span className="text-[10px] text-zinc-500 block font-mono mt-0.5">
                  {selectedTask.viewDump.activity}
                </span>
              </div>

              {/* View elements */}
              <div className="space-y-2 pt-2">
                {selectedTask.viewDump.elements.slice(1).map((el) => {
                  const isClicked = clickedTarget && (el.text.includes(clickedTarget) || clickedTarget.includes(el.text));
                  return (
                    <div
                      key={el.id}
                      className={`p-3 rounded-lg border text-xs transition-all flex items-center justify-between ${
                        isClicked
                          ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300 ring-2 ring-emerald-400/50 scale-102'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-300'
                      }`}
                    >
                      <div>
                        <span className="font-medium">{el.text}</span>
                        <span className="text-[10px] text-zinc-500 font-mono block">id: {el.id}</span>
                      </div>
                      <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                        el.clickable ? 'bg-zinc-800 text-zinc-400' : 'text-zinc-600'
                      }`}>
                        {el.clickable ? '可点击' : '静态'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Simulated Android Toast Notification */}
            {phoneToast && (
              <div className="animate-fade-in bg-zinc-800/95 border border-zinc-700 text-zinc-100 text-xs px-3.5 py-2 rounded-full text-center shadow-xl backdrop-blur mx-auto my-2 font-sans font-medium">
                {phoneToast}
              </div>
            )}

            {/* Phone bottom indicator */}
            <div className="w-24 h-1 bg-zinc-800 rounded-full mx-auto mt-2"></div>
          </div>
        </div>

        {/* Right: Agent Brain & Code Sandbox */}
        <div className="lg:col-span-8 space-y-4">
          {/* Active Phase Breadcrumbs */}
          <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl">
            <span className="text-xs font-mono text-zinc-400 block mb-2">当前智能体推进状态</span>
            <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
              {[
                { label: '环境感知', step: 1 },
                { label: '检索技能', step: 2 },
                { label: '代码生成', step: 3 },
                { label: '进程内执行', step: 4 },
                { label: '自纠错循环', step: 5 },
                { label: '沉淀入库', step: 6 },
              ].map((item) => (
                <div
                  key={item.step}
                  className={`p-2 rounded text-center text-xs font-mono border transition-all ${
                    currentStep === item.step
                      ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400 font-bold'
                      : currentStep > item.step
                      ? 'bg-zinc-950 border-zinc-800 text-zinc-500'
                      : 'bg-zinc-950/40 border-zinc-900 text-zinc-700'
                  }`}
                >
                  <span className="block text-[10px] opacity-75">#{item.step}</span>
                  <span className="text-[11px] truncate block">{item.label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Generated Code & Terminal Split */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* LLM Generated JavaScript */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-zinc-200 flex items-center space-x-1.5">
                  <Code2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>LLM 生成的 JS 技能代码</span>
                </span>
                <span className="text-[10px] font-mono text-zinc-500">
                  {currentStep >= 5 && selectedTask.correctedJsCode ? 'Self-Corrected' : 'Draft v1'}
                </span>
              </div>
              <div className="bg-zinc-950 p-3 rounded-lg border border-zinc-800 font-mono text-[11px] text-zinc-300 overflow-x-auto max-h-56 leading-relaxed">
                <pre>
                  {currentStep >= 5 && selectedTask.correctedJsCode
                    ? selectedTask.correctedJsCode
                    : selectedTask.initialJsCode}
                </pre>
              </div>
            </div>

            {/* WebSocket Terminal & Log Stream */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-zinc-200 flex items-center space-x-1.5">
                  <Terminal className="w-3.5 h-3.5 text-amber-400" />
                  <span>IPC 实时事件流 (WebSocket)</span>
                </span>
                <span className="text-[10px] font-mono text-emerald-400">127.0.0.1:28888</span>
              </div>
              <div className="bg-zinc-950 p-3 rounded-lg border border-zinc-800 font-mono text-[11px] text-zinc-300 overflow-y-auto max-h-56 space-y-1.5 leading-relaxed">
                {terminalLogs.length === 0 ? (
                  <span className="text-zinc-600">点击右上角「运行仿真流程」观察数据流...</span>
                ) : (
                  terminalLogs.map((lg, i) => (
                    <div
                      key={i}
                      className={
                        lg.includes('❌')
                          ? 'text-rose-400 font-semibold'
                          : lg.includes('🎉')
                          ? 'text-emerald-400 font-semibold'
                          : lg.includes('✨') || lg.includes('🔄')
                          ? 'text-cyan-300'
                          : 'text-zinc-400'
                      }
                    >
                      {lg}
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
