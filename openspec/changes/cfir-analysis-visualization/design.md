## 上下文

开发者需要检查 CFIR 从 raw 树到 resolve 各阶段的状态，并理解 Analysis diagnostics 如何与阶段结果对应。采集能力属于 IDE 开发工具，不应把上下文检查、事件分配或序列化代码放进 CFIR/Analysis 常规执行路径。

## 目标与边界

**目标**

- 在 IntelliJ 中通过 Tool Window 对当前打开的仓颉文件显式运行一次阶段检查。
- 使用已有 Low-level CFIR API 取得文件、逐阶段调用 resolve，再由 IDE 工具计时并冻结每个阶段的 CFIR 快照。
- 运行默认、额外和实验性 checker 集合，并记录诊断。
- 以树、节点关系图、文本、阶段差异和诊断视图检查 `.cjtrace` 工件。
- 普通 CFIR/Analysis 操作不注册采集服务、不执行额外分支、不分配轨迹对象。

**边界**

- 首版检查当前文件的 IDE lazy resolve 流程，不拦截任意后台 Analysis API 请求，也不记录完整 CLI 批量编译的宏构造与全量 pipeline 事件。
- 工具复用 IDE 当前的 session/cache。若某阶段此前已完成，事件会记录 `alreadyResolved`，不会把缓存命中伪装成实际阶段工作。
- 工具执行期间会显式触发解析、快照遍历和 diagnostics 收集；这部分开销只发生在用户主动运行工具时。

## 决策

### 采集代码完全位于 IDE 工具

DTO、采集 runner、CFIR visitor、JSON codec 和 Tool Window 都属于 `intellij-ide/modules/ide/base`。`ide/project` 只注册 Tool Window 和 project service。CFIR、Analysis API、session provider、resolver 和阶段 observer 中不包含可视化专用代码或采集钩子。

### 显式执行阶段

点击“分析当前文件”后，后台读操作通过现有公开入口取得 `LLResolutionFacade` 和 raw `CfirFile`，并按 `CfirResolvePhase` 顺序调用 `lazyResolveToPhase`。IDE runner 在调用外侧测量时间，并在每个阶段后立即通过 visitor 生成不可变快照。常规编辑器操作不会触发这条 runner。

诊断通过现有 `collectDiagnosticsForFile` API 分别查询 `DEFAULT`、`EXTRA` 和 `EXPERIMENTAL` checker 集合。每次查询作为独立事件，诊断被冻结为工件内的标量字段。

### 本地工件和回放

工件采用版本化 UTF-8 JSON `.cjtrace`，保存在项目的 `build/cfir-traces/`。工件不持有 PSI、symbol 或 CFIR 对象。检查器支持重新打开工件、源码定位、事件时间线、CFIR 树、父子节点关系图、阶段差异和诊断列表。

### 成本与限制

- 工具未运行时，CFIR/Analysis 热路径没有新增工作。
- 显式运行会处理当前文件全部阶段并为每阶段做一次有界快照；单快照节点数限制为 12,000，渲染文本限制为 120,000 字符，关系图最多绘制 1,200 个节点。
- 缓存中的高阶段状态只能如实展示；工具不清理或失效用户的 Analysis cache 来伪造一条“新鲜编译”。

## 验证

- 主仓受影响 CFIR/Analysis 模块编译。
- IDE `:modules:ide:base:compileKotlin` 编译。
- IDE `.cjtrace` codec 定向测试。
- 源码搜索确认 `cfir/` 与 `analysis/` 中没有可视化 trace 类型或调用点。
