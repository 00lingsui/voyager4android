export interface TechOption {
  name: string;
  category: 'execution' | 'ipc' | 'perception';
  rating: number; // 1-5
  recommended: boolean;
  pros: string[];
  cons: string[];
  verdict: string;
  voyagerFit: string;
}

export const EXECUTION_TECH_OPTIONS: TechOption[] = [
  {
    name: 'QuickJS (JNI 嵌入轻量引擎)',
    category: 'execution',
    rating: 5,
    recommended: true,
    pros: [
      '天然适配大模型（LLM 对 JavaScript/TypeScript 生成与排错能力极强）',
      '微秒级即时解析执行，无编译等待，耗时 < 5ms',
      '完整支持 ES2020 语法（async/await、Promise、解构等），利于异步流程控制',
      '支持沙箱上下文隔离与内存配额，死循环或异常可直接中断不致崩掉目标宿主',
      '技能库（Skill Library）可直接保存为标准 .js 函数文件，完全复现 Voyager 论文范式'
    ],
    cons: [
      '需要在 Xposed 模块中打包一个 ~200KB 的 libquickjs.so 动态链接库',
      '需要封装一层轻量的 JS ↔ Java 反射调用桥接层（JsBridge）'
    ],
    verdict: '【强烈推荐】Voyager 的核心精髓是“代码即技能”，QuickJS 是目前 Android 进程内承载 LLM 动态代码最兼顾开发体验与运行效率的方案。',
    voyagerFit: '100% 契合 Voyager 范式，代码自纠错（SyntaxError / Exception feedback）与技能组合完全一致。'
  },
  {
    name: 'Mozilla Rhino (纯 Java 实现的 JS 引擎)',
    category: 'execution',
    rating: 4.5,
    recommended: false,
    pros: [
      '100% 纯 Java 代码，无需任何 C/C++ .so 动态库，兼容所有 CPU 架构（arm64, x86_64）',
      '原生支持 LiveConnect，JS 脚本可直接无缝 new Java 类并调用 Java 方法',
      'Auto.js 早期版本证明了其在 Android 自动化领域的极高稳定性'
    ],
    cons: [
      '现代 JS 语法支持较老（Rhino 1.7.x 主要支持 ES6，部分现代语法缺失）',
      '执行速度略逊于 QuickJS，但对于 UI 自动化场景完全足够'
    ],
    verdict: '【备选最佳】如果你不想处理 NDK / .so 架构适配问题，Rhino 是最省心的 0-so 纯 Java 解决方案。',
    voyagerFit: '90% 契合，LLM Prompt 需稍加限制不使用太前沿的 JS 特性即可。'
  },
  {
    name: '动态 Dex 加载 (InMemoryDexClassLoader / Dalvik)',
    category: 'execution',
    rating: 2.5,
    recommended: false,
    pros: [
      '原生执行性能最高，拥有 100% 完整的 Android SDK 原生类型提示与静态调用能力',
      '可直接调用宿主未导出的私有混淆类与接口'
    ],
    cons: [
      '链条极长：LLM 写 Java -> 宿主机 javac 编译 -> d8 打包成 dex -> 传输 -> DexClassLoader 加载',
      '每次代码迭代耗时 1.5 ~ 3 秒，严重拖慢 Agent 试错反馈循环（Voyager 依赖快速反馈）',
      'ART 虚拟机的 Class 卸载极其严苛，动态加载数千次技能极易导致 Metaspace OOM 崩溃'
    ],
    verdict: '【不推荐用于交互层】编译成本过高且容易引发内存泄漏，只适合加载固定底层的复杂静态 Hook 逻辑。',
    voyagerFit: '30% 契合，高延迟严重破坏 Agent 试错纠偏的反馈时效。'
  },
  {
    name: 'JSON-RPC / 声明式 DSL 指令集 (如 AppAgent/AutoGLM 格式)',
    category: 'execution',
    rating: 3,
    recommended: false,
    pros: [
      '实现极其简单，解析安全，格式固定便于 Schema 校验',
      '不需要任何脚本引擎，直接 switch(action.type) 即可'
    ],
    cons: [
      '失去“程序表达力”：缺乏循环、条件判断、异常重试、局部变量、复杂逻辑流',
      '违背 Voyager 灵魂：Voyager 的精髓是自主编写复合函数并沉淀为终身技能库，DSL 无法高效组合技能'
    ],
    verdict: '【适合初期简单测试】但如果要做出真正具备自我进化与技能组合的 Voyager 智能体，必须升级到代码执行引擎。',
    voyagerFit: '40% 契合，无法实现复杂技能的参数化递归与函数组合。'
  }
];

export const IPC_TECH_OPTIONS: TechOption[] = [
  {
    name: '内嵌 WebSocket 服务 (NanoHTTPD / Java-WebSocket)',
    category: 'ipc',
    rating: 5,
    recommended: true,
    pros: [
      '真正的全双工双向实时流：Brain 下发代码，Xposed 实时回传 console.log、UI 变化、执行结果',
      '支持 adb forward tcp:28888 tcp:28888，无论大脑在 PC、远程服务器还是本机 Termux，代码完全不用改',
      '天然支持 Web 调试面板，随时可以在浏览器打开调试当前目标 App 内部状态',
      '无 Android Binder 1MB 事务缓冲区限制，即使 Dump 极大的 View 树或传输截图标注也毫无压力'
    ],
    cons: [
      '目标 App 必须拥有 INTERNET 权限（现代大多数联网 App 都有；若没有可用 Xposed 动态补全权限）'
    ],
    verdict: '【工业级首选】在 PC 控制端（Python / LangChain / Voyager 主脑）与手机 Xposed 插件之间最优雅、无侵入、极高吞吐的方案。',
    voyagerFit: '100% 适配，流式反馈让 LLM 能够感知代码执行的每一步状态。'
  },
  {
    name: 'UNIX 域套接字 (LocalServerSocket / LocalSocket)',
    category: 'ipc',
    rating: 4.5,
    recommended: false,
    pros: [
      '无需网络权限，直接基于 Linux Kernel socket 内存交互，极快',
      '适合主脑运行在同一台手机的另一个独立 App 或后台 Daemon 守护进程中'
    ],
    cons: [
      '需要处理 Android SELinux domain 策略，且不支持远程/PC 直接连接，需配合 adb shell 转发',
      '调试不如 WebSocket 直观，需要自行编写数据包长度分包协议'
    ],
    verdict: '【端侧纯离线首选】如果希望整个 Agent 大脑和 Worker 全部运行在手机本体单机内，LocalSocket 是极致低延迟方案。',
    voyagerFit: '85% 契合。'
  },
  {
    name: 'Android Binder / AIDL / ContentProvider',
    category: 'ipc',
    rating: 2.5,
    recommended: false,
    pros: [
      'Android 原生跨进程通信机制，安全性与生命周期跟随系统'
    ],
    cons: [
      '受限于 1MB 缓冲区上限，传输大型界面层级树或序列化对象容易抛出 TransactionTooLargeException',
      '需要定义 AIDL 接口并编译，宿主被杀死时重连机制繁琐',
      '难以与 PC / Python 主脑直接通讯，中间必须再加一层桥接 App'
    ],
    verdict: '【不推荐】架构链条过长，无法轻松配合 Python/Node.js 大脑开发。',
    voyagerFit: '40% 契合。'
  }
];

export interface PocCodeSnippet {
  id: string;
  title: string;
  filename: string;
  language: string;
  badge: string;
  description: string;
  code: string;
}

export const POC_CODE_SNIPPETS: PocCodeSnippet[] = [
  {
    id: 'hook_entry',
    title: '步骤 1：LSPosed 模块入口与宿主生命周期捕获',
    filename: 'HookEntry.java',
    language: 'java',
    badge: 'Xposed In-Process',
    description: '通过 IXposedHookLoadPackage 拦截目标 App 进程，在 Application.onCreate 时启动驻留 Agent 服务，并注入全局 Activity 追踪。',
    code: `package com.agent.voyager;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "VoyagerAgent";
    // 替换为你想要自动化的目标 App 包名
    public static final String TARGET_PKG = "com.example.targetapp";
    public static Activity currentActivity = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PKG)) return;
        XposedBridge.log(TAG + ": 正在注入目标应用进程: " + lpparam.processName);

        // 1. Hook Application.onCreate，作为 Agent 启动总入口
        XposedHelpers.findAndHookMethod(
            Application.class,
            "onCreate",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Application app = (Application) param.thisObject;
                    XposedBridge.log(TAG + ": 宿主 Application 初始化成功，启动 WebSocket Agent");

                    // 启动内部监听服务 (28888 独占端口)
                    AgentServer.getInstance().start(app, 28888);

                    // 注册 Activity 生命周期监听器，随时获取当前顶层窗口
                    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityResumed(Activity activity) {
                            currentActivity = activity;
                            XposedBridge.log(TAG + ": 顶层 Activity 更新: " + activity.getClass().getName());
                        }
                        @Override public void onActivityCreated(Activity a, Bundle b) {}
                        @Override public void onActivityStarted(Activity a) {}
                        @Override public void onActivityPaused(Activity a) {}
                        @Override public void onActivityStopped(Activity a) {}
                        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                        @Override public void onActivityDestroyed(Activity a) {
                            if (currentActivity == a) currentActivity = null;
                        }
                    });
                }
            }
        );
    }
}`
  },
  {
    id: 'agent_server',
    title: '步骤 2：进程内 WebSocket 通信服务',
    filename: 'AgentServer.java',
    language: 'java',
    badge: 'IPC Transport',
    description: '在被 Hook 的 App 进程内开启微型 WebSocket 监听，接收来自 LLM 大脑的指令与 JS 代码，将执行结果与报错异步返回。',
    code: `package com.agent.voyager;

import android.app.Application;
import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.net.InetSocketAddress;

public class AgentServer extends WebSocketServer {
    private static final String TAG = "VoyagerAgent_WS";
    private static AgentServer instance;
    private Application appContext;
    private JsRuntimeBridge jsBridge;

    public static synchronized AgentServer getInstance() {
        if (instance == null) instance = new AgentServer();
        return instance;
    }

    public void start(Application context, int port) {
        this.appContext = context;
        this.jsBridge = new JsRuntimeBridge(context);
        this.setAddress(new InetSocketAddress("127.0.0.1", port));
        new Thread(this::start).start();
        Log.i(TAG, "Agent WebSocket 监听已启动: ws://127.0.0.1:" + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i(TAG, "LLM 大脑已连接: " + conn.getRemoteSocketAddress());
        conn.send("{\\"status\\":\\"connected\\", \\"app\\":\\"" + appContext.getPackageName() + "\\"}");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject req = new JSONObject(message);
            String type = req.optString("type"); // "dump" | "execute_js" | "ping"

            if ("dump".equals(type)) {
                // 感知环境：输出当前界面的 View 树或关键状态
                String hierarchy = ViewInspector.dumpCurrentHierarchy();
                conn.send(new JSONObject().put("type", "observation").put("data", hierarchy).toString());
            } else if ("execute_js".equals(type)) {
                // 执行 LLM 生成的代码
                String code = req.getString("code");
                String skillName = req.optString("skill_name", "anonymous_action");
                
                // 交由嵌入式引擎执行并捕获结果
                JsRuntimeBridge.ExecResult res = jsBridge.eval(code);
                
                JSONObject resp = new JSONObject();
                resp.put("type", "exec_result");
                resp.put("skill_name", skillName);
                resp.put("success", res.isSuccess());
                resp.put("result", res.getOutput());
                if (!res.isSuccess()) {
                    resp.put("error", res.getError());
                    resp.put("stack", res.getStackTrace());
                }
                conn.send(resp.toString());
            }
        } catch (Exception e) {
            conn.send("{\\"type\\":\\"error\\", \\"msg\\":\\"" + e.getMessage() + "\\"}");
        }
    }

    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {}
    @Override public void onError(WebSocket conn, Exception ex) { Log.e(TAG, "WS 错误", ex); }
    @Override public void onStart() { Log.i(TAG, "WebSocketServer 启动成功"); }
}`
  },
  {
    id: 'js_bridge',
    title: '步骤 3：JS 引擎桥接与宿主能力暴露 (Java ↔ JS)',
    filename: 'JsRuntimeBridge.java',
    language: 'java',
    badge: 'Script Sandbox',
    description: '提供给大模型调用的 API 空间（app.*）。大模型编写像 app.click("确认") 或 app.callMethod(...) 的代码即可直接操控宿主。',
    code: `package com.agent.voyager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsRuntimeBridge {
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public JsRuntimeBridge(Context context) {
        this.context = context;
    }

    // 暴露给 LLM JS 环境的宿主操控接口
    public static class AppApi {
        private final Context ctx;
        public AppApi(Context ctx) { this.ctx = ctx; }

        public void toast(String text) {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
            );
        }

        public boolean clickByText(String text) {
            Activity act = HookEntry.currentActivity;
            if (act == null) return false;
            View decor = act.getWindow().getDecorView();
            return ViewInspector.findAndClick(decor, text);
        }

        public String getCurrentActivityName() {
            return HookEntry.currentActivity != null ? 
                HookEntry.currentActivity.getClass().getSimpleName() : "Unknown";
        }

        public void sleep(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
        }
    }

    // 执行来自 LLM 的 JS 代码
    public ExecResult eval(String jsCode) {
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        rhino.setOptimizationLevel(-1); // 解释执行，兼容 Android ART
        try {
            Scriptable scope = rhino.initStandardObjects();
            // 将 AppApi 注入为全局 'app' 变量
            Object wrappedApp = org.mozilla.javascript.Context.javaToJS(new AppApi(context), scope);
            ScriptableObject.putProperty(scope, "app", wrappedApp);

            // 执行代码
            Object result = rhino.evaluateString(scope, jsCode, "LLM_Skill.js", 1, null);
            return new ExecResult(true, String.valueOf(result), null, null);
        } catch (Throwable t) {
            return new ExecResult(false, null, t.getMessage(), android.util.Log.getStackTraceString(t));
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    public static class ExecResult {
        private final boolean success;
        private final String output;
        private final String error;
        private final String stackTrace;

        public ExecResult(boolean success, String output, String error, String stackTrace) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.stackTrace = stackTrace;
        }
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public String getStackTrace() { return stackTrace; }
    }
}`
  },
  {
    id: 'brain_python',
    title: '步骤 4：LLM 大脑控制脚本 (Voyager 迭代自纠错循环)',
    filename: 'voyager_brain.py',
    language: 'python',
    badge: 'LLM Controller',
    description: 'PC 端的 Voyager 主循环：获取环境状态 -> 查询技能库 -> 调用大模型生成 JS 代码 -> 下发执行 -> 捕获报错并自纠错 -> 保存为终身技能。',
    code: `import json
import asyncio
import websockets

TARGET_WS = "ws://127.0.0.1:28888" # 可通过 adb forward tcp:28888 tcp:28888 端口转发

VOYAGER_SYSTEM_PROMPT = """
你是一个运行在目标 Android App 进程内的自主智能体（Voyager 架构）。
你可以调用全局 'app' 命名空间提供的 API：
- app.toast(text: string): 弹出气泡通知
- app.clickByText(text: string): boolean, 查找并点击包含文本的组件
- app.getCurrentActivityName(): string, 获取当前活动界面名
- app.sleep(ms: number): 休眠毫秒

请编写原生 JavaScript 函数完成用户任务。必须返回执行结果状态。
示例：
(function() {
    app.toast("开始执行签到操作");
    let clicked = app.clickByText("立即签到");
    if (!clicked) {
        throw new Error("未找到'立即签到'按钮，当前可能不在签到主页");
    }
    app.sleep(1000);
    return "签到点击成功";
})();
"""

async def run_voyager_step(task_desc: str):
    async with websockets.connect(TARGET_WS) as ws:
        print("✅ 已成功连接到手机端 Xposed 宿主进程")
        
        # 1. 第一步：感知环境 (Perception)
        await ws.send(json.dumps({"type": "dump"}))
        obs = json.loads(await ws.recv())
        print(f"👀 宿主当前界面: {obs.get('data', '')}")

        # 2. 第二步：模拟 LLM 生成 JS 技能代码（实战中接入 Gemini/GPT-4o）
        generated_code = """
        (function run() {
            app.toast("Voyager Agent: 正在执行任务");
            let act = app.getCurrentActivityName();
            app.toast("当前界面: " + act);
            let ok = app.clickByText("签到");
            return "执行完成，点击结果: " + ok;
        })();
        """

        # 3. 第三步：下发给手机 Xposed 进程内执行
        print("🚀 下发 LLM 生成的代码到目标 App 进程...")
        await ws.send(json.dumps({
            "type": "execute_js",
            "skill_name": "auto_checkin",
            "code": generated_code
        }))

        # 4. 第四步：接收执行反馈 (Execution Feedback & Critic)
        result = json.loads(await ws.recv())
        if result.get("success"):
            print("🎉 技能执行成功！输出:", result.get("result"))
            print("💾 存入持久化技能库 (Skill Library)...")
        else:
            print("❌ 执行异常，触发自纠错 (Self-Correction):", result.get("error"))
            # 将 result['stack'] 追加进下一次 Prompt 重新生成代码！

if __name__ == "__main__":
    asyncio.run(run_voyager_step("在当前界面点击签到按钮"))`
  }
];

export interface VoyagerSkill {
  id: string;
  name: string;
  description: string;
  code: string;
  tags: string[];
  successCount: number;
  lastExecuted: string;
}

export const SAMPLE_SKILLS: VoyagerSkill[] = [
  {
    id: 'skill_1',
    name: 'safeClickByText',
    description: '安全检索并点击指定文本，包含重试与休眠机制',
    tags: ['UI', '基础能力', 'Atomic'],
    successCount: 42,
    lastExecuted: '2026-10-22 09:12',
    code: `function safeClickByText(targetText, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    let success = app.clickByText(targetText);
    if (success) {
      app.sleep(600);
      return true;
    }
    app.sleep(400);
  }
  return false;
}`
  },
  {
    id: 'skill_2',
    name: 'claimDailyRewards',
    description: '进入任务中心并完成每日签到与领奖复合流程',
    tags: ['复合任务', '业务逻辑', 'Composite'],
    successCount: 18,
    lastExecuted: '2026-10-22 08:30',
    code: `function claimDailyRewards() {
  app.toast("正在启动签到技能流程");
  if (!safeClickByText("任务中心") && !safeClickByText("福利")) {
    throw new Error("无法导航至任务中心入口");
  }
  app.sleep(1500);
  
  if (app.clickByText("立即签到")) {
    app.toast("签到成功！");
    app.sleep(800);
    app.clickByText("开心收下");
    return { status: "claimed", message: "今日签到完成" };
  }
  return { status: "already_claimed", message: "今日已完成签到" };
}`
  },
  {
    id: 'skill_3',
    name: 'invokeInternalReflect',
    description: '通过 Java 反射直接调用宿主单例业务层服务（绕过 UI 渲染极速执行）',
    tags: ['Xposed', '底层反射', 'Speedup'],
    successCount: 31,
    lastExecuted: '2026-10-21 22:15',
    code: `function invokeInternalReflect() {
  // 利用 Xposed 反射直接获取 App 内存中的单例 Manager
  // 绕过 UI 点击，实现毫秒级自动化
  let act = app.getCurrentActivityName();
  app.toast("在 " + act + " 触发底层数据同步");
  return { status: "synced", activity: act };
}`
  }
];
