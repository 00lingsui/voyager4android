import React, { useState } from 'react';
import { Header } from './components/Header';
import { ArchitectureView } from './components/ArchitectureView';
import { TechComparison } from './components/TechComparison';
import { PocCodeViewer } from './components/PocCodeViewer';
import { SimulationSandbox } from './components/SimulationSandbox';
import { SkillLibrary } from './components/SkillLibrary';
import { DownloadBanner } from './components/DownloadBanner';
import { ReleaseChangelog } from './components/ReleaseChangelog';
import { Terminal, Shield, Sparkles, BookOpen } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<string>('releases');

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans selection:bg-emerald-500/30 selection:text-emerald-300">
      <Header activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Unmissable Download Banner at the top */}
        <DownloadBanner />

        {activeTab === 'releases' && <ReleaseChangelog />}
        {activeTab === 'arch' && <ArchitectureView />}
        {activeTab === 'compare' && <TechComparison />}
        {activeTab === 'poc' && <PocCodeViewer />}
        {activeTab === 'sandbox' && <SimulationSandbox />}
        {activeTab === 'skills' && <SkillLibrary />}
      </main>

      {/* Footer with key architectural principles */}
      <footer className="border-t border-zinc-900 bg-zinc-950 py-6 text-xs text-zinc-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center space-x-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
            <span className="font-mono text-zinc-400">Voyager for Gemini 1.00 · LSPosed Specification</span>
          </div>
          <div className="flex items-center space-x-4 font-mono text-[11px] text-zinc-500">
            <span>Framework: LSPosed API 82</span>
            <span>·</span>
            <span>Engine: QuickJS / Rhino</span>
            <span>·</span>
            <span>IPC: 独占端口 28888</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
