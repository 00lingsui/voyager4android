import React, { useState } from 'react';
import { 
  Database, 
  Code2, 
  Plus, 
  Download, 
  Search, 
  Tag, 
  CheckCircle2, 
  Layers, 
  Play,
  Copy,
  Check
} from 'lucide-react';
import { SAMPLE_SKILLS, VoyagerSkill } from '../data/architectData';

export const SkillLibrary: React.FC = () => {
  const [skills, setSkills] = useState<VoyagerSkill[]>(SAMPLE_SKILLS);
  const [selectedSkill, setSelectedSkill] = useState<VoyagerSkill>(skills[0]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copied, setCopied] = useState<boolean>(false);
  const [isAddingNew, setIsAddingNew] = useState<boolean>(false);

  // New skill form state
  const [newName, setNewName] = useState<string>('');
  const [newDesc, setNewDesc] = useState<string>('');
  const [newCode, setNewCode] = useState<string>('');
  const [newTag, setNewTag] = useState<string>('Custom');

  const filteredSkills = skills.filter(
    s => s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
         s.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
         s.tags.some(t => t.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedSkill.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSaveNewSkill = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newCode.trim()) return;

    const created: VoyagerSkill = {
      id: `skill_${Date.now()}`,
      name: newName.trim(),
      description: newDesc.trim() || '自定义自动化技能',
      code: newCode.trim(),
      tags: [newTag, 'UserDefined'],
      successCount: 1,
      lastExecuted: new Date().toLocaleString()
    };

    setSkills([created, ...skills]);
    setSelectedSkill(created);
    setIsAddingNew(false);
    setNewName('');
    setNewDesc('');
    setNewCode('');
  };

  const exportSkillsJson = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(skills, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", "voyager_skills_export.json");
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  return (
    <div className="space-y-6">
      {/* Intro Header */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-mono text-emerald-400 tracking-wider uppercase">Lifelong Learning Paradigm</span>
          <h2 className="text-xl font-bold text-zinc-100 mt-1">
            终身技能库 (Voyager Lifelong Skill Library)
          </h2>
          <p className="text-xs text-zinc-400 mt-1 max-w-2xl">
            Voyager 的关键创新是将大模型编写并自纠错验证通过的代码沉淀为函数资产。后续高阶任务可以像调用积木一样层层组合已有技能，越用越聪明。
          </p>
        </div>

        <div className="flex items-center space-x-2 shrink-0">
          <button
            onClick={() => setIsAddingNew(true)}
            className="flex items-center space-x-1.5 px-3 py-2 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-zinc-950 text-xs font-semibold font-mono transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>新建技能</span>
          </button>

          <button
            onClick={exportSkillsJson}
            className="flex items-center space-x-1.5 px-3 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs font-mono transition-colors border border-zinc-700"
          >
            <Download className="w-3.5 h-3.5" />
            <span>导出 JSON 库</span>
          </button>
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Skill List & Search */}
        <div className="lg:col-span-5 space-y-3">
          {/* Search bar */}
          <div className="relative">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="搜索技能名称、描述或标签..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-lg pl-9 pr-4 py-2 text-xs text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-emerald-500 font-mono"
            />
          </div>

          {/* List items */}
          <div className="space-y-2 max-h-[580px] overflow-y-auto pr-1 scrollbar-thin scrollbar-thumb-zinc-800">
            {filteredSkills.map((sk) => {
              const isSelected = sk.id === selectedSkill.id && !isAddingNew;
              return (
                <div
                  key={sk.id}
                  onClick={() => {
                    setSelectedSkill(sk);
                    setIsAddingNew(false);
                  }}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-zinc-800/90 border-emerald-500 shadow-md ring-1 ring-emerald-500/40'
                      : 'bg-zinc-900/60 border-zinc-800 hover:border-zinc-700 hover:bg-zinc-800/40'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-xs font-bold font-mono text-zinc-100">{sk.name}</span>
                      <p className="text-[11px] text-zinc-400 mt-1 line-clamp-1 leading-relaxed">
                        {sk.description}
                      </p>
                    </div>
                    <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded border border-emerald-500/20 shrink-0 ml-2">
                      {sk.successCount} 次验证
                    </span>
                  </div>

                  <div className="flex items-center space-x-1.5 mt-2.5">
                    {sk.tags.map((t, i) => (
                      <span key={i} className="text-[10px] font-mono bg-zinc-950 text-zinc-400 px-1.5 py-0.5 rounded border border-zinc-800">
                        #{t}
                      </span>
                    ))}
                    <span className="text-[10px] font-mono text-zinc-500 ml-auto">
                      {sk.lastExecuted.split(' ')[0]}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: Code Viewer or New Form */}
        <div className="lg:col-span-7 bg-zinc-900 border border-zinc-800 rounded-xl p-6 space-y-4">
          {isAddingNew ? (
            <form onSubmit={handleSaveNewSkill} className="space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-zinc-800">
                <span className="text-sm font-bold text-zinc-100 flex items-center space-x-2">
                  <Plus className="w-4 h-4 text-emerald-400" />
                  <span>登记新技能 (Register New Skill)</span>
                </span>
                <button
                  type="button"
                  onClick={() => setIsAddingNew(false)}
                  className="text-xs text-zinc-400 hover:text-zinc-200"
                >
                  取消
                </button>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-mono text-zinc-400">技能函数名称</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. searchAndFilterGoods"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-xs font-mono text-zinc-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-mono text-zinc-400">语义描述 (供向量模型 Embedding 匹配)</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 在首页搜索框输入关键词并点击综合排序"
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-mono text-zinc-400">JavaScript 函数源码</label>
                <textarea
                  required
                  rows={8}
                  placeholder={`function ${newName || 'customSkill'}() {\n  app.toast("执行自定义技能");\n  // ...\n  return true;\n}`}
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg p-3 text-xs font-mono text-zinc-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-zinc-950 rounded-lg text-xs font-semibold font-mono"
                >
                  确认保存并入库
                </button>
              </div>
            </form>
          ) : (
            <>
              <div className="flex items-start justify-between pb-4 border-b border-zinc-800">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="text-base font-bold font-mono text-zinc-100">{selectedSkill.name}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-zinc-800 text-emerald-400 border border-zinc-700">
                      ID: {selectedSkill.id}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
                    {selectedSkill.description}
                  </p>
                </div>

                <button
                  onClick={handleCopy}
                  className="flex items-center space-x-1.5 px-3 py-1.5 rounded-md bg-zinc-800 hover:bg-zinc-700 text-xs font-mono text-zinc-200 transition-colors border border-zinc-700 shrink-0"
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

              {/* Code display */}
              <div className="space-y-2">
                <span className="text-xs font-mono text-zinc-400 flex items-center space-x-1.5">
                  <Code2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>JavaScript 源码实现</span>
                </span>
                <div className="bg-zinc-950 p-4 rounded-lg border border-zinc-800 font-mono text-xs text-zinc-300 overflow-x-auto leading-relaxed max-h-[380px]">
                  <pre>
                    <code>{selectedSkill.code}</code>
                  </pre>
                </div>
              </div>

              {/* Composition notice */}
              <div className="p-3.5 rounded-lg bg-zinc-950/60 border border-zinc-800 text-xs text-zinc-400 space-y-1">
                <span className="text-zinc-200 font-semibold block">🧩 Voyager 技能组合调用原理：</span>
                <p className="text-[11px] leading-relaxed">
                  当大模型需要完成复杂业务（如“每日签到”）时，Prompt 会将此技能函数的代码签名（JSDoc）注入上下文，智能体可以直接在生成的匿名闭包中调用该函数名，形成技能调用技能的树状生长体系。
                </p>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
