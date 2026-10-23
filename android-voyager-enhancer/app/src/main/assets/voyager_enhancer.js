(function() {
  if (window.__VOYAGER_ENHANCER_INITED__) return;
  window.__VOYAGER_ENHANCER_INITED__ = true;
  console.log('[Voyager] AI 手机端对话增强已成功注入当前页面');

  // 1. 注入适配手机的 CSS
  const style = document.createElement('style');
  style.id = 'voyager-mobile-style';
  style.textContent = `
    #voyager-fab-btn {
      position: fixed;
      left: 0;
      top: 40%;
      z-index: 2147483646;
      background: #10a37f;
      color: #fff;
      padding: 8px 10px 8px 6px;
      border-radius: 0 16px 16px 0;
      font-size: 14px;
      font-weight: bold;
      cursor: pointer;
      box-shadow: 2px 2px 12px rgba(0,0,0,0.35);
      user-select: none;
      opacity: 0.9;
    }
    #voyager-fab-btn:active { transform: scale(0.95); opacity: 1; }
    #voyager-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.6);
      z-index: 2147483646;
      display: none;
      backdrop-filter: blur(2px);
    }
    #voyager-backdrop.show { display: block; }
    #voyager-drawer {
      position: fixed;
      left: -290px;
      top: 0;
      width: 280px;
      height: 100vh;
      background: #18181b;
      color: #f4f4f5;
      z-index: 2147483647;
      box-shadow: 4px 0 24px rgba(0,0,0,0.5);
      transition: left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      flex-direction: column;
      font-family: system-ui, -apple-system, sans-serif;
    }
    #voyager-drawer.show { left: 0; }
    .v-header {
      padding: 16px;
      background: #121215;
      border-bottom: 1px solid #27272a;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .v-title { font-size: 15px; font-weight: bold; color: #10a37f; }
    .v-close { cursor: pointer; font-size: 18px; color: #a1a1aa; padding: 4px; }
    .v-content { flex: 1; overflow-y: auto; padding: 14px; }
    .v-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #71717a;
      margin-bottom: 8px;
    }
    .v-folder-item {
      padding: 10px 12px;
      margin-bottom: 6px;
      background: #27272a;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      cursor: pointer;
    }
    .v-folder-item:active { background: #3f3f46; }
    .v-btn {
      width: 100%;
      padding: 9px;
      background: #10a37f;
      color: #fff;
      border: none;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      margin-top: 8px;
    }
    .v-btn-secondary { background: #3f3f46; }
    .v-tag {
      display: inline-block;
      padding: 6px 10px;
      background: #27272a;
      border-radius: 6px;
      font-size: 12px;
      margin: 4px 4px 0 0;
      cursor: pointer;
    }
    .v-tag:active { background: #10a37f; color: #fff; }
  `;
  document.head.appendChild(style);

  // 2. 本地存储数据
  const STORAGE_KEY = 'voyager_ai_data';
  let appData = {
    folders: [
      { id: '1', name: '💼 工作与日常', chats: [] },
      { id: '2', name: '💻 代码开发与Debug', chats: [] },
      { id: '3', name: '📚 学习提炼与笔记', chats: [] }
    ],
    prompts: [
      '用中文简要提炼以上对话的核心结论：',
      '审查这段代码的潜在 Bug 并给出优化建议：',
      '请用更具条理的表格形式重新输出上述内容：'
    ]
  };

  try {
    const local = localStorage.getItem(STORAGE_KEY);
    if (local) appData = JSON.parse(local);
  } catch (e) {}

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appData));
  }

  // 3. 页面挂载 UI 结构
  const container = document.createElement('div');
  container.id = 'voyager-root';
  container.innerHTML = `
    <div id="voyager-fab-btn">📁</div>
    <div id="voyager-backdrop"></div>
    <div id="voyager-drawer">
      <div class="v-header">
        <span class="v-title">Voyager 对话增强</span>
        <span class="v-close" id="v-close">✕</span>
      </div>
      <div class="v-content">
        <div class="v-label">会话文件夹分类</div>
        <div id="v-folders"></div>
        <button class="v-btn" id="v-add-folder">+ 新建文件夹</button>

        <div class="v-label" style="margin-top:20px;">常用快捷 Prompt</div>
        <div id="v-prompts"></div>

        <div style="margin-top:20px;">
          <button class="v-btn v-btn-secondary" id="v-save-cur-chat">
            📌 将当前会话收藏至文件夹
          </button>
        </div>
      </div>
    </div>
  `;
  document.body.appendChild(container);

  const fab = document.getElementById('voyager-fab-btn');
  const drawer = document.getElementById('voyager-drawer');
  const backdrop = document.getElementById('voyager-backdrop');
  const closeBtn = document.getElementById('v-close');
  const folderBox = document.getElementById('v-folders');
  const promptBox = document.getElementById('v-prompts');

  const openDrawer = () => { drawer.classList.add('show'); backdrop.classList.add('show'); };
  const closeDrawer = () => { drawer.classList.remove('show'); backdrop.classList.remove('show'); };

  fab.onclick = openDrawer;
  closeBtn.onclick = closeDrawer;
  backdrop.onclick = closeDrawer;

  function render() {
    folderBox.innerHTML = '';
    appData.folders.forEach((f) => {
      const item = document.createElement('div');
      item.className = 'v-folder-item';
      item.innerHTML = `<span>${f.name}</span><span style="color:#71717a;font-size:11px;">(${f.chats.length})</span>`;
      item.onclick = () => {
        if (!f.chats.length) {
          alert(`文件夹 [${f.name}] 暂无记录，可点击下方按钮收藏当前页面`);
          return;
        }
        const summary = f.chats.map((c, i) => `${i + 1}. ${c.title}`).join('\n');
        const pick = prompt(`【${f.name}】中的会话：\n${summary}\n\n输入序号跳转:`);
        const idx = parseInt(pick) - 1;
        if (f.chats[idx]) window.location.href = f.chats[idx].url;
      };
      folderBox.appendChild(item);
    });

    promptBox.innerHTML = '';
    appData.prompts.forEach((p) => {
      const tag = document.createElement('span');
      tag.className = 'v-tag';
      tag.textContent = p.length > 15 ? p.substring(0, 15) + '...' : p;
      tag.onclick = () => {
        const input = document.querySelector('textarea') || 
                      document.querySelector('div[contenteditable="true"]') ||
                      document.querySelector('#prompt-textarea');
        if (input) {
          if (input.tagName === 'TEXTAREA') {
            input.value = p;
            input.dispatchEvent(new Event('input', { bubbles: true }));
          } else {
            input.innerText = p;
          }
          input.focus();
          closeDrawer();
        } else {
          alert('未找到当前网页的提问输入框');
        }
      };
      promptBox.appendChild(tag);
    });
  }

  document.getElementById('v-add-folder').onclick = () => {
    const name = prompt('请输入新文件夹名称:');
    if (name && name.trim()) {
      appData.folders.push({ id: Date.now().toString(), name: '📁 ' + name.trim(), chats: [] });
      save();
      render();
    }
  };

  document.getElementById('v-save-cur-chat').onclick = () => {
    const url = window.location.href;
    const title = document.title || '当前对话';
    const names = appData.folders.map((f, i) => `${i + 1}. ${f.name}`).join('\n');
    const pick = prompt(`选择存入的文件夹编号：\n${names}`);
    const idx = parseInt(pick) - 1;
    if (appData.folders[idx]) {
      appData.folders[idx].chats.push({ title, url, time: new Date().toLocaleDateString() });
      save();
      render();
      alert(`已成功将当前对话加入 [${appData.folders[idx].name}]！`);
    }
  };

  render();

  // 监听 SPA 路由变化（Next.js / Angular 单页面无刷新切换）
  const _pushState = history.pushState;
  history.pushState = function() {
    _pushState.apply(this, arguments);
    if (!document.getElementById('voyager-root')) {
      setTimeout(render, 500);
    }
  };
})();
