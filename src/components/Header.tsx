import React from 'react';
import { Cpu, Terminal, ArrowRightLeft, Database, Layers, Sparkles } from 'lucide-react';

interface HeaderProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export const Header: React.FC<HeaderProps> = ({ activeTab, setActiveTab }) => {
  const navItems = [
    { id: 'arch', label: '核心架构全景', icon: Layers },
    { id: 'compare', label: '技术选型深度对比', icon: ArrowRightLeft },
    { id: 'poc', label: '第一阶段 PoC 代码清单', icon: Terminal },
    { id: 'sandbox', label: 'Voyager 循环仿真器', icon: Cpu },
    { id: 'skills', label: '终身技能库 (Skill Library)', icon: Database },
  ];

  return (
    <header className="border-b border-zinc-800 bg-zinc-950/80 backdrop-blur sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center space-x-2.5 min-w-0">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 font-mono font-bold text-base shrink-0">
              V
            </div>
            <div className="min-w-0">
              <div className="flex items-center space-x-2">
                <h1 className="text-sm sm:text-base font-semibold text-zinc-100 tracking-tight truncate">
                  Voyager AI 对话增强
                </h1>
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-800 text-emerald-400 border border-zinc-700 shrink-0">
                  Xposed
                </span>
              </div>
              <p className="text-[11px] text-zinc-400 truncate hidden sm:block">
                ChatGPT / Gemini / Claude 网页文件夹与 Prompt 库增强插件
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2 shrink-0 ml-2">
            <button
              onClick={() => {
                const link = document.createElement('a');
                link.href = '/voyager-web-enhancer-xposed.zip';
                link.download = 'voyager-web-enhancer-xposed.zip';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
              }}
              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-zinc-950 font-mono text-xs font-bold shadow-md transition-all shrink-0 cursor-pointer"
            >
              <span>⬇ 下载源码 ZIP</span>
            </button>
          </div>
        </div>

        {/* Tab navigation */}
        <div className="flex space-x-1 overflow-x-auto pb-2 scrollbar-none">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center space-x-2 px-3 py-2 text-xs font-medium rounded-md whitespace-nowrap transition-colors ${
                  isActive
                    ? 'bg-zinc-800 text-emerald-400 border border-zinc-700 shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-emerald-400' : 'text-zinc-500'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </header>
  );
};
