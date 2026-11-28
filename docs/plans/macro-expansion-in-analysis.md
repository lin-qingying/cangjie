# 宏语义展开归位 Analysis：计划方案

> **状态**：Phase 0 / 1b 已落地（工作区，未提交）；Phase 1c 部分落地；Phase 2 容器探针完成、LSP 结构接线完成、真实引擎待决策
> **最后更新**：2026-10-30
> **涉及仓库**：主仓库 `analysis/`、`cfir/`、`macro/`、`lsp/`；宿主插件 `intellij-ide/
> **前置说明**：Phase 0 的修复已在工作区落地但**尚未提交**；第 2 节按「修复前症状 / 当前代码事实」分栏，避免把已完成项当成待修项。
>
> **完成度（截至本轮）**
> - 已完成（仓库侧）：Phase 0；Phase 1b 编排 + SPI；Phase 1c 入口/计数/基础遥测；Phase 2 产品壳
>   `--sdk-home/--macro-server/--stdlib`、发布后诊断刷新、真实宏 e2e、协议级 definition/hover/diagnostics；
>   Phase 3 会话失效 / 项目关闭 / 构造中写动作 / 发布 barrier 测试与 `metrics(module)`。
> - Linux 最终验证：宏 LLT 335/0；LSP 110/4（4 个既有 completion resolve）；analysis 10/0；
>   macro-process 22/0。
> - references（已支持）：目标键优先从当前位置的 `CjReferenceExpression` 解析 `CallableId`，见
>   `docs/plans/references-macro-generated-declarations.md`；definition 仍回到宏调用来源。
>   `didChange` 重建由 `refreshProjectStructure→configure→revision` 链覆盖。
> - Windows 实机（2026-11-01）：用户回退 KB5127648（24H2/26100.1）后服务端不再崩溃；已把 JNA 与 IDE
>   两条启动路径改为官方 cjc 风格（argv[4]=cjc.exe 完整路径、std 继承、环境/工作目录继承），
>   并给 cjc 宏编译工作目录加 8.3 短路径回退；`CjMacroSemanticSnapshotIntegrationTest` 从
>   8/3 失败降到 **8/1**：仅 `testRealNestedMacroArtifactFeedsExpansionPreviewAndContributionDrilldown`
>   失败，断言“多层宏信息应产生可滚动内容；content=192px，viewport=192px”，不再有
>   `0xC0000005`/`ReadFile 109`/`管道已关闭`。

## 0. 修订记录

- v5（本次）：LSP 结构接线（`LspMacroSemanticExpansionEnvironment` + `LspMacroExpansionOptions` +
  `CangjieLspServerOptions.macroExpansion`）；按源码事实核对并修正了 LSP revision/读访问、
  headless 白名单、Phase 1c 验收等表述，删除未经验证的结论。
- v4：Phase 2 文字探针固化（`MacroExpansionHostContainerProbeTest`），确认 headless 容器
  `invokeAndWait + WriteAction` 可用；新增 `docs/macro-expansion-host-integration.md`；
  Phase 2 接入受引擎/SDK 决策阻塞（开放问题 1）。
- v3（本次）：Phase 0 收尾（`[nav]` 探针删除、判据合并为 `MacroSemanticSnapshotContract`）；
  Phase 1b 落地（新增 `analysis:macro-expansion`、`macro:macro-common` 引擎 SPI、
  `compiler:frontend` 引擎实现、IDE 环境适配器，删除旧 configurator / snapshot service）；
  Phase 1c 落地廉价 PSI 预扫描门禁与构造耗时日志，revision 细化暂缓（原因见 4.4）。
- v2：修正依赖方向与 SPI 类型；补触发模型、宿主编排抽象、测试载体、回滚方案；纠正「无宏零成本」的成本假设；Phase 1 拆为 1a/1b/1c；重命名以避免与 `intellij-ide` 现有宏类型冲突。
- v1：初稿。


## 1. 目标

把「宏展开的语义视图」从 IDE 宿主私有生命周期，提升为 **Analysis 层的一等能力**，使 IDE、LSP、standalone、测试共享同一套展开、快照、失效与解析语义。

成功定义（可检验）：

1. 同一宏工程在 IDE 与 LSP 得到相同的 definition / hover / diagnostics；
2. 无宏工程不发布宏快照，且不存在「同一位置两份 CFIR」导致的歧义；
3. 新增宿主接入宏展开时，只需实现一个环境接口，不复制构造/发布编排。

非目标（本计划不做）：

- 不重写 `compiler:frontend` 的宏展开算法本身（`FrontendMacroConstructionService` 保持不动）；
- 不改变宏语法/宏运行时协议（FlatBuffers、LSPMacroServer）；
- 不在本计划内优化宏编译器（cjc / LSPMacroServer）自身性能。

## 2. 现状

### 2.1 职责分布（代码事实）

| 职责 | 所在位置 | 说明 |
|---|---|---|
| 宏展开引擎 | `compiler:frontend`（`FrontendMacroConstructionService`、`MacroExpandPhase`、`prepareMacroArtifactDefinitionsForExpansion`） | 编译期与 IDE 共用；经 `FrontendMacroSemanticExpansionEngine` 实现引擎 SPI |
| 展开后的语义视图/快照 | `analysis/low-level-api-cfir`（`LLCfirMacroSemanticSnapshot`、`LLCfirMacroSemanticSnapshotStore`、`prepareMacroSemanticSnapshot`、`publishMacroSemanticSnapshot`） | 已是 analysis API |
| 无宏效果判据 | `analysis/low-level-api-cfir`（`MacroSemanticSnapshotContract`） | 唯一判定入口：展开前 `canHaveMacroSemanticEffect`，展开后 `hasMacroSemanticEffect` |
| 构造编排（roots/SDK/revision/线程/写屏障/重试） | `analysis:macro-expansion`（`MacroSemanticExpansionService`、`MacroSemanticExpansionConstruction`） | Analysis 层框架，宿主无关 |
| 宿主取值与屏障 | `intellij-ide/modules/ide/macro`（`IntelliJMacroSemanticExpansionEnvironment`） | 只提供 SDK / roots / revision / 读写屏障 / 编译器配置 |
| 发布后屏障（高亮失效、daemon 合并） | `intellij-ide/modules/ide/macro`（`MacroSemanticHighlightingRefreshService`） | 经 `onPublished` / `onAbandoned` 接入 |
| 扩展点声明 | `analysis/low-level-api-cfir/resources/META-INF/analysis-api/cangjie-low-level-api-cfir.xml`、`analysis/macro-expansion/resources/META-INF/analysis-api/cangjie-macro-expansion.xml` | 前者声明 `llCfirSessionConfigurator`，后者声明 `macroExpansionEnvironment` 并注册触发器与服务 |
| 触发 | `analysis:macro-expansion`（`MacroSemanticSessionConfigurator`） | 宿主不再注册 configurator，只决定是否提供环境 |
| IDE 注册 | `intellij-ide/modules/ide/macro/src/main/resources/META-INF/cangjie-macro.xml` | 仅注册 `macroExpansionEnvironment` |
| LSP | 根 `lsp` 模块 | 已加入 `analysis:macro-expansion` 依赖；尚未注册环境（Phase 2） |
| 测试 / 批处理 | `tests/test-infrastructure`（`CfirAnalyzerFacade`）、`cfir:entrypoint`（`analyse`）、`cfir:analysis-tests` | 走整程序管线或 `MacroConstructionService.Identity`，**不经过** LL-CFIR 快照 |
| 第二个宏子系统 | `intellij-ide/modules/ide/macro`（`macro.engine`、`macro.service`、`macro.pipeline`、`macro.server`） | 面向 UI 展开/预览/调试的独立链路，与 LL-CFIR 快照链路并存 |

LSP 无宏展开的**根因**（比「没注册」更具体）：`llCfirSessionConfigurator` 扩展点所在的 XML 由 `cangjie-analysis-api-cfir.xml` 以 `xi:include` 加载，因此 LSP 容器里 EP **已声明**，只是没有实现注册；同时 LSP 的插件 XML 是手工白名单（`AnalysisApiLspServiceRegistrar.analysisPluginXmls`），新增 EP 必须显式接入。Phase 1b 后，触发器改由 `analysis:macro-expansion` 提供，LSP 只差「注册 environment」一步（Phase 2）。

### 2.2 修复前症状（Phase 0 已处理）

1. **LSP 没有宏展开**，宏生成成员在 LSP 中不可见；
2. IDE 在「无宏」模块上仍构建并发布 **identity 快照**（`allSurfaces=[]`），为同一处声明制造第二棵 CFIR；
3. 同一处源码声明在实时树与展开树中各有一份 CFIR（成员可能不同），候选聚合混用两个视图 → `ConeAmbiguityError` → 类型解析退化为 error type → 导航/补全丢失；
4. 快照的失效/重建/发布时机是宿主私有逻辑，任何宿主接入都要复制一遍，且行为难以对齐。

### 2.3 当前代码事实（Phase 0 / 1b / 1c 之后，工作区状态）

- **identity 快照已被拒绝**：判据统一收敛到 `MacroSemanticSnapshotContract.hasMacroSemanticEffect`；
  `prepareMacroSemanticSnapshot` 只调用该入口，宿主侧不再判断。
- **声明身份已按源码位置收敛**：`CfirClassLikeSymbol.declarationSiteId()`（真实 PSI 用 `路径:起:止`，生成声明用来源对象身份）已用于候选聚合、可用性聚合与元素构建，且当前已发布视图优先。
- **失效与写屏障已存在**：`LLCfirResolvableModuleSession.invalidateMacroSemanticSnapshot()` 已提供；`publishMacroSemanticSnapshot` 已包含 `isWriteAccessAllowed` 断言、registry/文件数/文件名身份三重校验，并在发布后失效 raw CFIR 与 file structure。
- **判据只有一份实现**：`MacroSemanticSnapshotContract.canHaveMacroSemanticEffect`（展开前必要条件下界）与
  `hasMacroSemanticEffect`（展开后权威判定）定义在同一文件，两者语义包含关系有文档约束。
- **`[nav]` 探针已全部删除**：`analysis-api-cfir`、`cj-references`、`low-level-api-cfir` 中不再有 `[nav]` 日志。
- **编排已归位 Analysis**：`analysis:macro-expansion` 提供服务、环境 SPI、触发与会话级/模块级调度；
  IDE 只保留 `IntelliJMacroSemanticExpansionEnvironment`；`LLCfirMacroSessionConfigurator` 与
  `LLCfirMacroSnapshotService` 已删除。
- **成本门禁已落地**：`hasMacroCallCandidateForModule()` 在整模块 raw 构建前做 PSI 级注解 / 表达式宏调用扫描；
  无候选的模块直接短路，不再付出整模块 raw CFIR 构建。
- **revision 仍为项目级**：`PsiModificationTracker.modificationCount` 仍是主维度；模块级细化暂缓（见 4.4）。

### 2.4 本计划真正要解决的问题

1. **LSP 无宏展开**：需共享构造编排，而不是复制 IDE 代码（见 2.1 根因）。**触发与编排已共享（Phase 1b），差 LSP 环境注册（Phase 2）。**
2. **编排在宿主**：`LLCfirMacroSessionConfigurator` 的取值、调度、写屏障、重试、发布、取消与高亮屏障全部绑定 IDE，第二宿主必须重写。**已解决：编排在 `analysis:macro-expansion`，宿主只实现环境。**
3. **无宏工程仍有显著成本**：要判定 `allSurfaces.isEmpty()`，必须先 `buildPreMacroRawBuildResultForModule()`，它对**模块内每个源文件**做 uncached raw CFIR 构建；无宏模块等于「每 revision 一次全模块构建再丢弃」。因此「无宏零成本」不成立，成本门禁是独立优化项。**已加 PSI 预扫描门禁；整模块构建只在存在注解 / 表达式宏调用候选时发生。**
4. **revision 过宽**：现有 revision 用全局 `PsiModificationTracker.modificationCount`，工程内任意编辑都会使所有模块快照失效并触发整模块重建。**未解决（4.4）。**
5. **判据重复**：两份「无宏效果」判断可能漂移，与「宿主不得决定语义」相冲突。**已解决：单一入口。**

### 2.5 验证证据（工作区命令）

- `:analysis:macro-expansion:test` 通过（重试语义、revision 相等语义）。
- `:analysis:macro-expansion:compileKotlin`、`:compileTestFixturesKotlin` 通过。
- `:compiler:frontend:compileKotlin` 通过。
- `intellij-ide` `:modules:ide:macro:compileKotlin`、`:compileTestKotlin` 通过。
- 依赖方向断言（**compileClasspath 与 runtimeClasspath 均验证**）：`macro-expansion` 不含 `compiler:frontend`；
  `compiler:frontend` 不含 `analysis:*`。
- 引用回归（`[nav]` 探针删除后）：`AnalysisApiReferenceServiceTest`、`AnalysisApiImportGroupReferenceTest`、
  `AnalysisApiIdeDecompiledReferenceTest`、`:analysis:cj-references:test` 全部通过。
- `:lsp:test` 全量：4 个 `LspCompletionResolveTest`/`LspCompletionStatisticsTest` 失败，堆栈是
  `java.lang.UnsupportedOperationException`，直接抛自 LSP4J 默认实现
  `org.eclipse.lsp4j.services.TextDocumentService.resolveCompletionItem
  ⇒ LSP 服务端未覆盖 completion resolve 路由（既有功能缺口，与宏迁移无关）；
  其余 LSP 测试与宏相关 6 个用例通过。
- raw 构建计数断言：IDE 用例实测无宏 session（含失效重建）`preMacroRawBuildCount()==0`、
  有宏 session `==1`；`:analysis:low-level-api-cfir` 的 `DecompiledStdlibMacroApiTest` 与
  `:analysis:macro-expansion:test` 通过。
- `:lsp:test` 覆盖两个新用例：
  - `MacroExpansionHostContainerProbeTest`：描述符可加载、`MacroSemanticExpansionService` 可解析、
    `MacroSemanticSessionConfigurator` 已被 headless 白名单装配、environment 扩展点可动态注册、
    后台线程 `invokeAndWait { WriteAction }` 与 `runWriteAction` 可用；
  - `LspMacroSemanticExpansionEnvironmentTest`（4 tests）：未配置 options 时不注册 environment；
    配置后注册、引擎/读访问委托、headless 屏障可用；**真实工作区模块**下 `roots()` 返回源码根目录、
    `revision()` 可比较且重新 `configure` 后变化（该用例暴露并修正了 LSP `psiRoots` 是文件集合、
    不是目录的事实差异）；`frontendMacroExpansionOptions(sdk)` 的装配（SDK/模式/源码根/stdlib/
    执行器工厂/fragment parser）全部按 `SdkHandle` 值写入，测试不启动真实执行器。
- `CjMacroSemanticSnapshotIntegrationTest`（真实 SDK `CANGJIE_HOME=...cangjie-1.0.5`，8 tests / 5 通过 / 3 失败）：
  - 通过：`testPublishedMacroSnapshotFeedsAnalysisStructureViewAndFindUsages`、
    `testAnalysisOrchestrationPublishesAndRebuildsSnapshotWithDeterministicEngine`（**新编排端到端**：environment →
    service → pre-macro 构建 → 展开 → 屏障发布；断言 `onConstructionStarted` / `onPublished` 各 1 次，
    快照含 `generated`，发布后名称解析得到 `generated`；随后编辑并失效 session，断言新 revision 发布**新**快照，
    `onConstructionStarted` / `onPublished` 各累计 2 次）、
    `testSessionInvalidationCancelsInFlightConstruction`（阻塞引擎停在 expand，失效 session 后
    `onAbandoned` 在 60s 内到达，outcome 只能是 CANCELLED / INPUT_CHANGED，且 `published=0`）、
    `testDegradedConstructionPublishesSuccessfulMacroResultsWithItsDiagnostic`、
    `testRegisteredSessionConfiguratorSkipsSnapshotForMacroFreeModule`；
  - 真实执行失败（**非沙箱；已修复启动参数，仍有一个服务端启动后退出问题**）：
    2026-10-30 实跑确认：官方启动器传的 argv[4] 是 **cjc 可执行文件路径**（`globalOptions.executablePath`），
    服务端对其取目录得到 `<sdk>/bin`，再定位 `<sdk>/bin/../runtime`；此前传 `tools/bin`/`<sdk>` 导致
    `could not load ... libcangjie-runtime.dll`。改为 `<sdk>/bin/cjc[.exe]` 后 DLL 错误消失、服务端成功启动；
    但服务端随后仍在 5s 内以 `exitCode=0` 静默退出（stderr 无输出），父进程 `ReadFile 109`。
    单条用例曾通过一次（failures=0），重复运行仍失败，表现为启动阶段的竞态/服务端行为。
    子进程侧探针（PowerShell 子进程对 argv 句柄调用 `GetFileType`/`PeekNamedPipe`）结果：
    `types=3,3`（FILE_TYPE_PIPE）、`PeekNamedPipe=True` ⇒ **句柄继承有效**，
    服务端拿到的是可用的匿名管道句柄。
    **WER 崩溃报告（本机）**：`C:\ProgramData\Microsoft\Windows\WER\ReportArchive\AppCrash_LSPMacroServer...
    的 `Report.wer` 记录 `故障模块=msvcrt.dll`（7.0.26100.9711）、`异常代码=c0000005`、
    `异常偏移=0x62f1e`。对 0.53.13 / 0.53.18 / 1.0.0 / 1.0.5 / 1.1.3 五个服务端构建逐一验证：
    argv[4] 传目录时进程驻留但报 DLL 找不到（路径按 `dirname(argv4)/../runtime` 解析）；
    传 `<sdk>/bin/cjc.exe`（运行时路径正确）时全部**立即 0xC0000005**、stderr 为空。
    对照：非法句柄 + 正确 cjc 路径 → 干净报错 `Read handle is not available` 并退出 -1。
    结论：崩溃发生在 msvcrt 内部，只在运行时库真正加载后触发，所有已安装服务端构建一致；
    **注意归因边界**：以上崩溃只在 `macro:macro-process` 的 JNA 启动 + Gradle 测试容器（以及我手写的
Win32 探针）中稳定复现；IDE 用户实际使用的 `LspMacroServerProcess`（`GeneralCommandLine`）路径
**尚未被证明失败**，且用户报告该服务端此前在 IDE 中正常。结合这一点，更可能是我在排查中传递的
启动契约（尤其 argv[4] 与句柄端）与真实 IDE 路径不一致，而不是服务端二进制本身的问题。
排查期间对 `LspMacroServerProcess` / `LspMacroServerMacroExecutor` 的 argv[4] 改动已全部恢复为
原始的 `executable.parent`。后续验证应以真实 IDE 路径为准，而不是探针结论。
**实测补充（GUI 路径）**：把 argv[4] 恢复为 `executable.parent` 后运行
`testRealNestedMacroArtifactFeedsExpansionPreviewAndContributionDrilldown`（走 `LspMacroServerProcess`），
日志显示 `LSPMacroServer 已启动 (PID=...)` 后 262ms 即 `已退出 (exitCode=-1073741819)`，
随后 `管道已关闭（已读 0/8 字节）`。⇒ 崩溃在 IDE 的真实启动类中同样复现，不只是 JNA 探针。
把 `CANGJIE_HOME` 切到 1.1.3（`cangjie-sdk-windows-x64-1.1.3\cangjie`）后同一 GUI 用例仍以
`exitCode=-1073741819` 退出；IDE/DevEco 日志中没有可用的历史运行记录（只有文件打开事件），
无法从日志还原"曾经正常"时的 SDK/IDE 组合。
**PE 依赖分析**（dumpbin）：`LSPMacroServer.exe` 是 x64 CUI，导入 `libcangjie-lsp.dll`（140MB，实际逻辑）
与 mingw 运行时 `libc++.dll`、`libunwind.dll`、`libwinpthread-1.dll`、`msvcrt.dll`；
`libcangjie-lsp.dll` 本身不静态依赖 `libcangjie-runtime.dll`，而是在启动后用 `LoadLibraryW` 加载它并调用 `RunCJTask`。
PATH 中 runtime 指向本 SDK 的 `windows_x86_64_cjnative`（日期 2023-02-28），无跨 SDK 冲突。
结合 WER 的 `msvcrt.dll+0x62f1e`，崩溃发生在"运行时已加载、进入 libcangjie-lsp 逻辑"之后，
更像是 mingw CRT（msvcrt）与当前 Windows 构建的兼容问题，而不是启动参数或 SDK 版本配对。
**WSL2 Linux 端到端验证（2026-10-31，成功）**：Ubuntu 26.04 + Cangjie Linux SDK
`1.3.0-alpha.20261004001050`（`~/cj/cangjie`）。用仓库的 `LspMacroServerMacroExecutor` 在 Linux 上
执行 `loadLibraries([libcangjie-std-deriving.so])`，测试通过：
`Success(loadedLibPaths=[.../libcangjie-std-deriving.so])`；另用 Python 协议客户端发 DefLib，
服务端返回 15 字节 `ResponseFindDef...`。
**Linux 回归对比（2026-10-31）**：`:analysis:macro-expansion:test` 在 WSL 通过；
`CfirAnalysisMacroTestGenerated` 在 Linux 上 **335 tests / 24 failed**。
用基线工作树（`.claude/worktrees/baseline-b1063824`，commit `b10638242`）跑同一测试：
**326 tests / 20 failed**。两者 **15 个失败相同**；当前版本额外失败的 9 个全部是
`testSameProject*` / `testSamePackageCrossFile*`，其测试数据在基线不存在（属新增用例），
失败诊断是 `MACRO_DEPENDENCY_COMPILE_FAILED`（宏包编译失败，Linux 侧功能缺口）；
当前版本还修复了基线中 5 个失败（`testDerive`、`testEnumconsInsideMacro`、`testUnused002/003/012`）。
⇒ 迁移未造成既有用例回归，剩余差异集中在新增 same-project 宏用例与 Linux 宏包编译。
**Linux 修复与全绿（2026-10-31）**：根因是宏服务端在 Linux 上 dlopen 宏库时找不到其依赖的
仓颉运行时库（`libcangjie-std-*.so`），报 `MACRO_CANNOT_OPEN_LIB`。修复：
`LspMacroServerMacroExecutor` 启动非 Windows 服务端时，把
`<sdkHome>/runtime/lib/<platform>` 与 `<sdkHome>/lib/<platform>` 注入
`LD_LIBRARY_PATH`（macOS 为 `DYLD_LIBRARY_PATH`）；`LspMacroServerUnixEnvironmentTest`（3/3，Windows 与 Linux 均通过）锁定变量名与目录集合。修复后
`CfirAnalysisMacroTestGenerated` **335 tests / 0 failed**。
WSL 侧另需 `binutils`（提供 `ar`）与 `gcc`/`libc6-dev`（提供 `crti.o`/`crtbeginS.o`），
否则 `cjc -p <root> --compile-macro` 会失败。
**Linux 其余验证（2026-10-31）**：`:lsp:test --tests '*Macro*'` **6 tests / 0 failed**
（`LspMacroSemanticExpansionEnvironmentTest` 4、`MacroExpansionHostContainerProbeTest` 2）；
`:lsp:test` 全量 **106 tests / 4 failed**（新增 `LspMacroExpansionRealEngineTest` 通过），全部是既有 completion resolve（LSP4J
`resolveCompletionItem` 未实现导致的 Internal error），与宏改动无关；
`:macro:macro-process:test` 全量通过（含真实 Linux `LSPMacroServer` 的
`LspMacroServerLinuxProbeTest`）；`:analysis:macro-expansion:test` 通过。
⇒ **同一套仓库代码、同一协议、同样的 cwd（exe 目录）在 Linux 正常，Windows 失败**，
进一步确认问题在 Windows 侧的 `LSPMacroServer`/运行时初始化，而不是启动器或宏库。
**Windows argv[4] 修正（2026-10-31）**：Windows 探针（cwd=`<sdk>/tools/bin`）实测：
argv[4]=`<sdk>/tools/bin` → `0xC0000005`；argv[4]=`<sdk>/bin`（cjc 目录）→ 存活；
argv[4]=`<sdk>` → 存活但 `<dir>/../runtime` 解析错误；argv[4]=`<sdk>/bin/cjc.exe` → 崩溃。
已把 `macro-process` 与 IDE `LspMacroServerProcess` 的 argv[4] 改为 `cjcFolder = <sdk>/bin`，
Linux e2e 仍通过。但 Windows 真实 `runIde`/集成测试仍以 `0xC0000005` 退出，说明还有
std 句柄/环境块差异未对齐（探针用继承 std、真实路径用 Java `ProcessImpl`/JNA std 管道）。
又说验证：把 JNA 启动改为继承父环境块（env=null）后真实测试仍 `0xC0000005`，排除环境块内容为主因。
**26H2 更新后复测（2026-10-30 22:00）**：系统已升到 Windows 11 build **26300**，但
`C:\Windows\System32\msvcrt.dll` 仍是 **7.0.26100.9711**；15 秒探针（计划任务、DSH 外）中
`LSPMacroServer` 仍以 `0xC0000005` 退出，真实宏用例同样 `ReadFile 109` 失败，
WER 22:01:48 仍是 `msvcrt.dll+0x62f1e`。⇒ 26H2 升级未替换该 msvcrt，崩溃未消失。
**时间线证据**：`C:\Windows\System32\msvcrt.dll` 的修改时间为 **2026-10-27 18:58**（版本 7.0.26100.9711）；
WER 里 `AppCrash_LSPMacroServer` 的**最早报告是 2026-10-28 01:04**，此前没有任何崩溃报告；
至 2026-10-30 21:47 共 **243 份**报告，故障模块/偏移全部是 `msvcrt.dll+0x62f1e`。
另在 Windows 计划任务（完全独立进程上下文）中用 JNA 与 Java `GeneralCommandLine` 两种方式启动，
同样崩溃 ⇒ 排除 DSH/测试容器/启动器差异。
**混合 CRT 实验**：在临时目录镜像 `tools/bin`，把 1.0.5 的 `LSPMacroServer.exe`+`libcangjie-lsp.dll
与 1.1.3 的 `libc++.dll`/`libunwind.dll`/`libwinpthread-1.dll` 组合（以及反向组合、1.0.5 全量基线），
三种组合传对应 `bin/cjc.exe` 均为 `0xC0000005`。⇒ 与 mingw 运行时 DLL 版本也无关。
**崩溃偏移定位**：用 `dumpbin /exports msvcrt.dll` 解析导出表，`0x62f1e` 落在导出
`wcslen`（RVA `0x62ed0`）与 `wcsncat`（RVA `0x62ff0`）之间，最近的下方导出是 `wcslen`。
说明崩溃发生在对**宽字符串指针**的读取上（空指针或 ABI 不一致导致的坏指针），
而不是管道/参数校验路径。下一步需要调试器栈回溯（本机未安装 cdb/windbg）才能定位调用方。
**自建调试循环的栈回溯**（`DebugActiveProcess` + `WaitForDebugEvent` + `GetThreadContext` + 栈扫描/导出表映射）：
调用链为
`Cangjie::RuntimeInit::InitRuntime`（`libcangjie-lsp.dll+0x4c4ee0`）
→ `Cangjie::InvokeRuntime::PrepareRuntime/CallRuntime`（`+0x68e9f0` / `+0x68e800`）
→ `InitCJRuntime`（`libcangjie-runtime.dll+0x15660`）→ `wcslen`（msvcrt+0x62f1e）。
异常信息：`read` 访问、`faultAddr=0x620026`（极低的非法地址）、`rcx=libcangjie-runtime+0x11ca60`、`rdx=0`。
**官方源码更新后的关键发现**（`external/cangjie_compiler` 已更新到 `3431f44d`，2026-10-30）：
`src/main-macrosrv.cpp` 在提交 `e5bf94a3`（2026-07-09）新增了
`StringifyEnvironmentPointer(envp)` → `gpt.ReadPathsFromEnvironmentVars(...)` →
`gpt.cangjieHome = env CANGJIE_HOME or dirname(cjcFolder)`，并把 `envp` 从 `[[maybe_unused]]` 改为使用。
本机 8 份服务端构建的文件时间为 2023-02/2025-07～2025-12，**全部早于该修复**；
另外从官网下载了最新发布版 **1.2.0 免安装 zip**（`cangjie-sdk-windows-x64-1.2.0.zip`），
包内 `LSPMacroServer.exe` 的 SHA256 与 1.2.0 安装包一致、时间仍为 2025-07-30，
用其 SDK 跑真实宏用例依旧 `exitCode=-1073741819`（`0xC0000005`）⇒ **最新发布版也不含 2026-07 的修复**；
该修复目前只在官方编译器 main 源码中，需要 nightly/CI 构建或自行编译。
**DSH 外对照（2026-10-30 晚）**：用 Explorer 启动（脱离 DSH 进程树）后，
裸探针 3s 内 `wait=258`（看似存活），但 Gradle 测试中服务端约 **5s 后**仍以同一
`msvcrt+0x62f1e` 的 `0xC0000005` 退出；GUI 路径在 DSH 外也在启动约 3s 后同签名退出。
⇒ **不是 DSH 上下文**；崩溃是 InitRuntime 的延迟崩溃（裸探针 3s 窗口是假阳性）。
同时把 `macro:macro-process` 的 `CreateProcessW` 对齐官方 C++（`lpApplicationName=null`、
`flags=0`、环境/cwd 继承、不设 `STARTF_USESTDHANDLES`），但崩溃不因此改变；
DefLib 日志显示：send 后约 4.8s 才因 `ReadFile 109` 失败，即服务端在**加载首个宏库前后**退出。
**宏库对照实验（2026-10-30 21:08，DSH 外）**：用 `MacroServerLibraryProbeTest` 直接调用
`LspMacroServerMacroExecutor.loadLibraries`，分别加载
(a) SDK 自带 `libcangjie-std-deriving.dll`、(b) 我们构建产物 `lib-macro_macros.dll`：
两者都返回 `SERVER_DISCONNECTED: ReadFile 109`。⇒ **不是我们生成的宏库有问题**；
崩溃发生在服务端“加载任意宏库 → `PrepareRuntime → InitCJRuntime`”这一步。
构建产物本身有效：x64、导出与 std.deriving 同构，且在已加载 runtime 的进程里 `LoadLibrary` 成功。
这解释了为什么所有已安装版本都在 `InitCJRuntime` 的初始化路径上崩溃，而最新源码里这段已经被修正。
该形态与 **std::string/ABI 布局不一致导致的指针误读**一致（把字符串内容当成了指针），
说明问题在 SDK 的 `libcangjie-lsp.dll` 与 `libcangjie-runtime.dll` 的 ABI 配对，而不是启动参数或 Windows 沙箱。
    补充交叉验证：1.1.3 SDK（`cangjie-sdk-windows-x64-1.1.3`）单独、以及 1.1.3 server + 1.0.5 runtime、
    1.0.5 server + 1.1.3 runtime 两组交叉组合，传各自 `bin/cjc.exe` 时全部同样 `0xC0000005`；
    在 `<sdk>/bin` 下新建目录作为 argv[4]（dirname=bin，运行时路径正确）也崩溃。
    ⇒ 与 SDK/服务端版本配对无关。
    以下为修复前记录：
    失败用例：`testMacroSourceFixtureDiagnosticsAndGeneratedApiNavigation`、
    `testRealMacroArtifactCompilationFeedsPublishedSemanticSnapshot` 在真实 `LSPMacroServer` 阶段失败
    （`MACRO_EXECUTOR_SERVER_DISCONNECTED`，`ReadFile failed: 109`）。本轮实测：
    - `--no-daemon` 重跑仍失败 ⇒ 不是 Gradle daemon 令牌；
    - 为 `macro:macro-process` 增加 stderr 重定向与退出码观察后：子进程 5s 内退出，
      `exitCode=-1073741819`（0xC0000005 ACCESS_VIOLATION），stderr 无输出；
    - 在**纯 Win32**（PowerShell/.NET `CreatePipe` + `CreateProcessW(bInheritHandles=TRUE)`，
      不经过 Java/JNA）复现：同样 0xC0000005；
    - 手工 `LSPMacroServer.exe 0 1 0 <sdk>/tools/bin` 能启动并报 `Read handle is not available
      ⇒ exe/DLL 本身可运行。
    结论：不是本会话的 DSH 沙箱或 daemon 限制；证据指向服务端在"继承匿名管道句柄启动"时崩溃，
    但缺少子进程侧句柄有效性验证，**根因仍未定位**。现有 stderr/退出码诊断已保留在
    `LspMacroServerMacroExecutor`，供后续定位。
  - `testRealNestedMacroArtifactFeedsExpansionPreviewAndContributionDrilldown` 走独立 UI 展开管线，
    同样以 `管道已关闭` 失败，与本迁移无关。
- **宏 LLT 交叉验证**（`cfir:analysis-tests`，走 entrypoint 整程序管线，不经过新编排）：
  `--tests "*CfirAnalysisMacroTestGenerated" --tests "*CfirAnalysisMacroPsiTestGenerated"` →
  670 tests / 57 failed；逐条检查失败文本，**57/57 都包含**
  `MACRO_EXECUTOR_SERVER_DISCONNECTED` 或 `ReadFile(LSPMacroServer) failed: 109`，
  **0 条**是测试数据/语义差异。结论：本机所有"真实宏执行"路径（IDE 集成、CFIR LLT、UI 展开管线）
  都卡在同一个 `LSPMacroServer` 子进程管道问题上，属于环境限制，不是本次迁移的回归。
  非执行器的宏统计用例 `MacroConstructionTimingObserverTest` 通过。
- **顺带修复的根因**：`buildPreMacroRawBuildResultForModule()` 在构建前读取
  `rawMacroSurfaceUsageStoreOrNull`，而 store 是懒创建的（首次记录 surface 时才创建）。
  新 session 的第一次构造因此拿到 null，录制到新 store 的 surface 全部被丢弃，表现为
  「有宏调用但 `allSurfaces` 为空」。现改为构建前 `ensureRawMacroSurfaceUsageStore()`，
  与记录端使用同一实例。
- **尚未验证**：IDE 真机 `parameter.cj` 导航（需要交互式 IDE 与真实工程）、`analysis-real-project-test`。

## 3. 目标架构

### 3.1 模块与依赖（修正方向）

``
  :macro:macro-common（引擎 SPI：Engine / Revision / Barrier / SdkHandle / ModuleRoots）
           ▲                        ▲
 implements│                        │ api
  compiler:frontend        analysis:macro-expansion ──► analysis/low-level-api-cfir
                                │（环境 SPI + 服务 + 触发）  ──► cfir:providers
                                │                            ──► compiler:config
                                ▲
              ┌─────────────────┼──────────────────┐
        intellij-ide            lsp           standalone / tests
      （环境适配器）        （环境适配器）      （环境适配器）
``

硬性规则：

1. **SPI 分两层放**，不能整块塞进 `macro:macro-common`：
   - 引擎/取值类型（`MacroSemanticExpansionEngine`、`MacroExpansionRevision`、`MacroExpansionBarrier`、`SdkHandle`、`ModuleRoots`、`MacroExpansionOutcome`）放 `macro:macro-common`（需新增 `api(project(":cfir:providers"))`），**且不得引用任何 `analysis` 类型**；
   - 环境接口 `MacroSemanticExpansionEnvironment`、服务接口与会话触发放 `analysis:macro-expansion`（它们引用 `LLCfirResolvableModuleSession`，若下沉会让 frontend 传递依赖 analysis）。
2. `compiler:frontend` 只 implement 引擎 SPI，**禁止**依赖 `analysis:*`（当前 `compiler/frontend/build.gradle.kts` 对 analysis 零依赖，必须保持）。
3. `analysis:macro-expansion` **不依赖** `compiler:frontend`，也**不依赖** `intellij-ide` 的 `domain/toolchain`、`ide/base` 等宿主模块。
   注意：**宿主**（如 `lsp`、`intellij-ide`）可以依赖 `compiler:frontend` 来提供引擎与配置装配，
   受限方向只有「frontend ↛ analysis」和「analysis:macro-expansion ↛ frontend」两条。
4. 宿主只提供「值的来源」与「屏障/钩子」，不提供「何时构造、何时发布」的决策。

### 3.2 SPI 草案（类型自洽，可在主仓库编译）

**A. 引擎与取值类型（`macro:macro-common`，不引用 analysis 类型）**

```kotlin
/** 引擎：宏编译 + 展开，实现由 compiler:frontend 提供；Unavailable 为无执行器实现。 */
interface MacroSemanticExpansionEngine {
    fun prepareArtifacts(configuration: CompilerConfiguration, classifications: List<MacroDemandClassification>): MacroExpansionArtifactPreparation
    fun buildSymbolIndex(pre: PreMacroRawBuildResult, artifacts: MacroExpansionArtifactPreparation): MacroSymbolIndex
    fun bindImports(pre: PreMacroRawBuildResult, index: MacroSymbolIndex): MacroResolutionContext
    fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        configuration: CompilerConfiguration,   // v3：构造服务需要配置，显式传入
        diagnostics: List<MacroConstructionDiagnostic> = emptyList(),
        mode: MacroConstructionService.Mode = Mode.DEGRADED,
    ): MacroConstructionResult
}

/** 宿主无关的自洽类型，禁止在 SPI 中出现 CjSdk / CjProjectSdkConfig 等宿主类型。 */
data class SdkHandle(
    val id: String,
    val homePath: String,
    val environment: Map<String, String> = emptyMap(),
    val stdlibPath: String? = null,   // v3：classpath 组装需要
)
data class ModuleRoots(val sourceRoots: List<String>, val classpathRoots: List<String>)
data class MacroExpansionRevision(
    val moduleKey: String,
    val psiModificationCount: Long,
    val projectRootModificationCount: Long,
    val compilerSettingsModificationCount: Long,
    val sdkKey: String?,
    val artifactFingerprint: String = NO_ARTIFACT_FINGERPRINT,
)
enum class MacroExpansionOutcome { PUBLISHED, NO_MACRO_SURFACE, FAILED, CANCELLED, INPUT_CHANGED }

/** 发布屏障：保证「发布 + 缓存失效」对其他读者原子可见。 */
interface MacroExpansionBarrier {
    fun <T> publish(block: () -> T): T?
}

/** 构造期 PSI 短读访问；从 frontend 下沉，frontend 保留类型别名。 */
interface MacroConstructionReadAccess {
    fun <T> compute(action: () -> T): T
}
``


**B. 环境、服务与触发（`analysis:macro-expansion`）**

```kotlin
/** 宿主环境：提供输入、屏障与回调，不承载语义决策。 */
interface MacroSemanticExpansionEnvironment {
    fun sdk(module: CaSourceModule): SdkHandle?
    fun roots(module: CaSourceModule): ModuleRoots
    fun revision(module: CaSourceModule): MacroExpansionRevision

    /** 逐文件读访问（PSI/解析器读取），实现可以是 IntelliJ ReadAction。 */
    fun readAccess(): MacroConstructionReadAccess

    fun barrier(): MacroExpansionBarrier

    /** 含宏 SDK、执行器、宏包编译编排的编译器配置。 */
    fun compilerConfiguration(
        module: CaSourceModule,
        roots: ModuleRoots,
        sdk: SdkHandle?,
    ): CompilerConfiguration

    /** 宿主相关装配（fragment parser 等）。 */
    fun installFragmentParser(module: CaSourceModule, configuration: CompilerConfiguration)

    fun engine(): MacroSemanticExpansionEngine

    fun onConstructionStarted(session: LLCfirResolvableModuleSession)
    fun onPublished(session: LLCfirResolvableModuleSession, files: Set<VirtualFile>)
    fun onAbandoned(session: LLCfirResolvableModuleSession, outcome: MacroExpansionOutcome)
}

/** 每工程一个：调度、revision、失效、发布、单视图。接口优先，实现内部可见。 */
interface MacroSemanticExpansionService {
    fun request(session: LLCfirResolvableModuleSession)
    fun invalidate(module: CaSourceModule)
    fun awaitIdle(module: CaSourceModule)   // 测试/批处理
}
``

设计约束：

- `barrier()` 是**唯一**允许触碰写访问的地方；IDE 用 `invokeAndWait + WriteAction` 实现，LSP/standalone 用容器实际能力实现。
- `revision()` 由宿主计算并必须可比较（`equals`）；框架只做相等比较（不等即失效并丢弃旧结果），
  宿主不得自建第二套 tracker。
- `engine()` 所有权唯一：由 environment 返回，EP 不再单独注入 engine。
- `SdkHandle.environment` 供 LSPMacroServer / cjc 进程使用，避免把工具链对象泄漏进 analysis。
- 类型命名统一使用 `MacroSemantic*`，避免与 `intellij-ide` 现有 `macro.engine.MacroExpansionEngine`、`macro.service.MacroExpansionService` 概念冲突。

### 3.3 不变量（必须由框架保证）

1. **快照契约**：只有实际发生语义变化时才发布快照；identity 快照一律不生成。判据在框架内**唯一一份**
   （`MacroSemanticSnapshotContract`：展开前必要条件下界 + 展开后权威判定），宿主侧不得重复判断。
2. **单视图**：一个模块在任一时刻只有一个权威语义视图。快照发布必须在 `barrier()` 内完成，发布后立即失效 raw CFIR 与 file structure，读者不得混用两个视图。
3. **声明身份**：声明身份 = 源码位置（文件 + 文本范围），与 CFIR 对象、成员内容无关；生成/合成声明用来源对象身份。同一位置的多份表示视为同一声明，当前已发布视图优先；不同位置才是重声明歧义。
4. **revision 足够细**：输入 revision 变化即失效旧快照；旧 revision 的构造结果必须丢弃；失败 revision 记录后不重复构建。revision 至少覆盖 PSI、roots、编译器设置、SDK、宏包 artifact 指纹。**现状仍用项目级 PSI 计数（模块级细化暂缓，见 4.4）。**
5. **宿主中立**：宿主只提供值、屏障与回调；「什么时候发布、如何失效」的决策不得写进宿主。
6. **不承诺零成本**：无宏模块仍会先做一次 PSI 预扫描（Phase 1c）；整模块 raw 构建只在存在候选时发生，但成本优化本身不写成不变量。

### 3.4 触发模型（已实现）

- `analysis:macro-expansion` 自带 `MacroSemanticSessionConfigurator : LLCfirSessionConfigurator`，
  声明在本模块 XML（`cangjie-macro-expansion.xml`，由 `cangjie-analysis-api-cfir.xml` 与 IDE 镜像描述符
  `cangjie-ide-analysis-api-cfir.xml` 分别 `xi:include`），在 `configure()` 中「存在 environment 才 request」。
- IDE：`cangjie-macro.xml` 只注册 `macroExpansionEnvironment`，**不再注册** configurator。
- LSP：在 `AnalysisApiLspServiceRegistrar` 里编程注册 environment（EP 为 `dynamic="true"`，可运行时注册）。**待 Phase 2。**
- tests / standalone：向 `macroExpansionEnvironment` EP 注册 fake environment（`analysis:macro-expansion
  的 testFixtures 已提供 `FakeMacroSemanticExpansionEnvironment` / `FakeMacroSemanticExpansionEngine`）。
- 结论：触发点属于 analysis 层框架，宿主只决定「是否提供环境」。

## 4. 分阶段实施

### Phase 0（已完成）

已完成（工作区，未提交）：

- 诊断：`SqlParameter` 被解析为 `ConeAmbiguityError`，两个候选位于同一源码范围、成员可能不同；
- 快照契约修复：无宏效果拒绝生成/发布快照；
- 声明身份修复：`declarationSiteId()` 与候选/构建侧收敛；
- 失效 API 与写访问断言补齐；
- `[nav]` 探针已全部移除（v3）；
- 两份「无宏」判据合并为 `MacroSemanticSnapshotContract`（v3）。

剩余：IDE 真机验证与 Phase 0 变更提交。

**验收**：`parameter.cj` 中 `SqlParameter` 稳定导航（打开/编辑/重开）；`[nav] type-target-null` 不再出现（探针已删，以导航行为为准）；`analysis` 与 IDE 宏测试全绿。

### Phase 1a：行为锁定与判据收敛（已随 1b 完成）

1. 删除宿主侧「无宏」判据，统一由 `MacroSemanticSnapshotContract` 一处决定；
2. 行为锁定用例迁移到 `analysis:macro-expansion`：`MacroSemanticExpansionRetryTest`、`MacroExpansionRevisionTest`；
3. parity 用例（无宏模块不发布快照、不创建第二视图）待 Phase 6.2 的 Analysis API 测试载体落地后补齐。

**验收**：行为与当前一致；判据只有一份；重试与 revision 语义用例绿。

### Phase 1b：抽模块与 SPI（已完成，IDE 行为不变）

1. `macro:macro-common` 新增 `org.cangnova.cangjie.macro.expansion` SPI（Engine / Revision / Barrier /
   SdkHandle / ModuleRoots / Outcome / ReadAccess / ArtifactPreparation），并新增 `api(":cfir:providers")`；
2. `compiler:frontend` 新增 `FrontendMacroSemanticExpansionEngine`（对现有入口的 1:1 包装），
   `MacroConstructionReadAccess` 下沉到 macro-common（frontend 保留 typealias）；
3. 新建 `analysis:macro-expansion`：`MacroSemanticExpansionService`（接口 + internal 实现）、
   `MacroSemanticExpansionConstruction`（编排）、`MacroSemanticSessionConfigurator`（触发）、
   `MacroSemanticExpansionEnvironment`（环境 SPI）、XML 描述符；
4. 迁移编排：in-flight 去重、模块级串行、revision 记录、失败记录、pre-macro 构建、prepare、
   barrier 发布、raw/fileStructure 失效、输入稳定重试、取消、构造耗时日志；
5. IDE 侧只留 `IntelliJMacroSemanticExpansionEnvironment`（roots / SDK / revision / readAccess /
   barrier / compilerConfiguration / 回调）；
6. 删除 `LLCfirMacroSessionConfigurator` 与 `LLCfirMacroSnapshotService`，`cangjie-macro.xml
   只注册 `macroExpansionEnvironment`；IDE 的 analysis 描述符镜像新增
   `cangjie-macro-expansion.xml` 的 `xi:include`；`CjMacroSemanticSnapshotIntegrationTest` 中
   依赖旧类名/旧 identity 快照契约的断言同步更新（无宏模块改为 parity 断言）。

**验收**：IDE 编译通过（已验证）；无宏项目不产生快照；宏项目快照每 revision 只发布一次
（确定性引擎端到端用例已验证一次构造恰好一次 `onPublished`）；`analysis:macro-expansion` 的
compileClasspath 里没有 `compiler:frontend`（已验证）。

### Phase 1c：成本门禁（部分完成）

1. **已实现**：`hasMacroCallCandidateForModule()` 在整模块 raw 构建前做 PSI 级预扫描
   （注解 / `CjMacroExpression`），无候选模块直接短路；构造耗时按模块记录到日志
   （`... finished in Nms with outcome=...`）。验证过程中同时修复了 surface store 懒创建导致的
   「首次构造丢 surface」根因（见 2.5）。
2. **暂缓**：revision 细化为模块级/文件级；
3. 指标（已补基础域）：构造耗时日志 + `MacroSemanticExpansionService.metrics(module)` 提供
   `lastDurationMillis / lastOutcome / constructionsStarted / published / abandoned`，
   可区分 `NO_MACRO_SURFACE`（无宏短路）与真实构造耗时。

**暂缓 revision 细化的原因**：在 IntelliJ 容器内，模块级输入变化缺少既正确又廉价的判定源。
VFS 修改戳不反映未保存编辑，PSI 修改计数是项目级的；用 VFS 指纹替代会让未保存编辑复用旧快照
（正确性风险高于收益）。保留项目级计数，等有可靠的模块级修改跟踪（或把快照失效改为按文件订阅）
再落地，避免以性能换语义错误。

**验收状态**：
- 构造耗时可在日志读到（已实现并按模块输出）；
- 预扫描的**两种结果**都有用例覆盖：无宏模块走 false 分支（IDE parity 用例日志
  `no macro call candidates in module sources`），有宏模块走 true 分支并完成发布
  （确定性编排用例）；
- **raw 构建计数断言已落地**：`LLCfirMacroConstructionInput` 新增
  `PreMacroRawBuildCounter`（session 组件）与公开只读访问器 `CfSession.preMacroRawBuildCount()`；
  IDE 用例实跑断言：无宏模块（含失效重建）计数为 **0**，有宏模块计数为 **1**；
- 「无宏工程不做整模块 raw 构建」因此已由计数断言直接证明，不再是代码顺序推断。

**Phase 3 加固进展（v7）**：`MacroSemanticExpansionServiceImpl.runTask` 在构造开始前与构造完成后检查
`project.isDisposed`，项目关闭期间丢弃回调（日志记录 outcome），避免在已释放的工程上发布/回调。
该路径尚无专用生命周期测试（需要项目关闭场景的集成测试基建），现有框架用例保持通过。

### Phase 2：LSP 接入

**方向已确定（v6，基于代码事实）**：LSP 打包 `compiler:frontend`，使用
`FrontendMacroSemanticExpansionEngine`。依据：
- 完整宏引擎（artifact 定位/编译/校验、符号索引、import 绑定、stable splice）只存在于
  `compiler:frontend`；`macro:macro-process` 只有执行器；
- LSP 已经通过 `analysis:analysis-api-cfir` → `cfir:entrypoint` 带入大部分 CFIR 依赖，
  加 frontend 主要增加 `compiler:phaser` / `chir:cfir2chir` / `compiler:jvm-codegen`，无循环、无平台冲突；
- 使用与 IDE **同一个引擎**是满足"IDE/LSP 一致"验收的必要条件；
- 若分发体积成为硬约束，再走"宏配置键 + artifact 准备下沉 + macro-process"的重构（非当前选项）。

0. **前置探针（已完成）**：`lsp/test/.../MacroExpansionHostContainerProbeTest` 在 `MockApplication` /
   `MockProject` 容器实测：
   - `WriteAction.compute` 直接跑在后台线程会触发 EDT 断言；
   - 后台线程 `invokeAndWait { WriteAction.compute { ... } }` **可用**（写访问在 runnable 内成立）；
   - 后台线程直接 `Application.runWriteAction` 也可用；
   - `MacroSemanticExpansionService` 能从描述符解析、`macroExpansionEnvironment` 扩展点可动态注册。

   结论：LSP 可以复用 IDE 的 barrier 形态（`invokeAndWait + WriteAction`），不需要另造发布原语。
   探针同时断言：`macroExpansionEnvironment` 扩展点已声明、描述符注册的
   `MacroSemanticSessionConfigurator` 已被 headless 白名单装配（这两条是白名单改动的实测依据）。
1. **LSP 环境与前端装配（已完成结构 + 装配层）**：
   - `LspMacroSemanticExpansionEnvironment` + `LspMacroExpansionOptions`（引擎/SDK/配置注入）；
   - `LspMacroExpansionDefaults.kt` 的 `frontendMacroExpansionOptions(sdk)`：引擎 =
     `FrontendMacroSemanticExpansionEngine`，配置里写入 `macroSdkHome` / `macroCompilerEnvironment` /
     `macroConstructionMode=DEGRADED` / 源码根 / stdlib classpath / `LspMacroServerMacroExecutor`；
     `SdkHandle` 新增 `macroServerPath`，**所有 SDK 路径由宿主传入，LSP 不推导目录布局**；
   - LSP 没有宏包编译编排器，显式 `macroExpansionDemandAutoCompilationEnabled = false`（只用已编译 artifact）。
   - 测试 `frontend defaults assemble config from sdk handle` 覆盖上述装配（不启动真实执行器）。
2. 注册：`CangjieLspEnvironment.create(mode, macroExpansion)` 在 Analysis API 服务注册完成后
   调用 `MacroSemanticExpansionEnvironment.registerExtension`。
3. 已完成：产品启动壳解析 `--sdk-home` / `--macro-server` / `--stdlib`，构造真实 `SdkHandle
   并调用 `frontendMacroExpansionOptions(sdk)` 注入 `CangjieLspServerOptions.macroExpansion
   （`CangjieLspServerOptionsTest` 3/3）。
   已实现：`CangjieLanguageServer` 为宏快照发布挂接 `publishListener`，异步调用
   `CangjieServerContext.republishOpenDiagnostics()` 重发打开文档诊断（semantic tokens 由客户端按需重取）。
   已完成：真实工程宏展开 e2e——`LspMacroExpansionRealEngineTest`（Linux）在 LSP 工作区里
   用真实 SDK 预编译宏包、注册真实 environment + frontend 引擎 + `LSPMacroServer`，
   请求构造并 `awaitIdle` 后断言发布了宏语义快照。
   `MacroDemandClassification.create` 保留原 3 参重载、新增 4 参重载，避免 Kotlin 默认参数签名变化
   导致已编译调用方 `NoSuchMethodError`（IDE 测试实际踩到）。该 e2e 现断言 `generatedByMacro` 进入快照且可按 `CallableId` 解析（definition 目标物化）；
   `tests=1 / failures=0`。
   已断言生成声明可按 `CallableId` 解析、且 `macroSurfaceIdsForDeclaration` 能映射回宏调用 surface
   （导航/引用元数据）。`didChange` 重建路径已闭环：`CangjieTextDocumentService.didChange` →
   `CangjieServerContext.refreshProjectStructure()` → `projectStructureState.configure(...)`，
   现有环境测试已断言 `configure` 后 revision 变化；直接改 document 不递增 revision 属测试手法问题。
   LSP 协议级已验证：对生成符号的 definition 会回到宏调用来源（`@Make`，生成声明无物理 PSI）、
   pull diagnostics 无 `MACRO_`/`UNRESOLVED` 诊断
   （`LspMacroExpansionProtocolIntegrationTest`）；hover 请求成功但内容为空（生成函数无 cdoc）。
   references 已通过“直接解析引用目标键”修复，协议测试覆盖 A.cj/B.cj 使用点；
   根因是 `AnalysisApiLspSemanticSupport.targetKeyFor(PsiElement)` 依赖物理 PSI，而宏生成声明只有
   CFIR 没有 PSI，当前架构下无法形成目标键。需为 `CfirDeclaration` 单独设计目标键/定位后才能支持。
   **SDK 发现为何不放进框架**：仓库内唯一的 SDK 布局实现是 IDE 侧 `CjSdk`；其
   `detectPlatform()` 生成 `{os}_{arch}_llvm`，而实际 SDK 1.0.5 的模块目录是
   `modules/windows_x86_64_cjnative`，`stdlibPath` 会走 `modules` 回退分支。复制这套逻辑到 LSP
   会把一个已知不一致的推导固化到框架里，因此 LSP 只接受宿主传入的 `SdkHandle
   （home/environment/stdlib/macroServerPath 全部显式），不做任何路径推导。

**验收**：同一宏工程在 IDE 与 LSP 的 definition/hover/diagnostics 一致；LSP 容器内无写访问断言失败、无死锁。

### Phase 3：加固与收尾

1. 并发/取消：会话失效、项目关闭、构造中写动作、barrier 阻塞；
   **已覆盖**：`LspMacroExpansionLifecycleTest`（真实 LSP 环境 + 阻塞引擎）验证会话失效与
   项目关闭都会取消在途构造 → `onAbandoned(CANCELLED)`，`metrics.abandoned=1`；
   `LspMacroExpansionRealEngineTest` 覆盖真实发布 barrier；`write during construction` 用例
   在构造阻塞期间触发 workspace 写动作，断言不发布快照（`abandoned != PUBLISHED`）。
2. 性能：Phase 1c 指标纳入统计域，加回归阈值（现有 `LLMacroConstructionStatistics` 已覆盖
   construction 三段耗时与结果归类，挂到 session 的 `CfirMacroConstructionTimingObserver`）；
3. 遥测（已完成基础指标）：`MacroSemanticExpansionService.metrics(module)` 返回每模块
   `constructionsStarted / published / abandoned / lastOutcome / lastDurationMillis`；
   LSP 真实 e2e 断言 `published>=1` 且 `lastOutcome=PUBLISHED`。命中率等聚合指标仍待宿主侧采集；
4. 文档：`docs/` 架构说明与「新宿主接入指南」——**已完成**
   `docs/macro-expansion-host-integration.md`（只实现 environment 的最小示例）；
5. 清理：`[nav]` 探针、旧 configurator、重复 revision tracker、`MacroExpansionEngine` 的命名歧义说明。

## 5. 文件/模块变更清单

新增（已落地）：

- `analysis/macro-expansion/build.gradle.kts
  - `api(project(":analysis:low-level-api-cfir"))
  - `api(project(":macro:macro-common"))`（引擎/取值 SPI）
  - `api(project(":compiler:config"))`（环境接口公开暴露 `CompilerConfiguration`）
  - `compileOnly(intellijCore())`；`java-test-fixtures
- `analysis/macro-expansion/src/.../MacroSemanticExpansionEnvironment.kt`（环境接口 + EP 描述符）
- `analysis/macro-expansion/src/.../MacroSemanticExpansionService.kt`（接口 + internal 实现）
- `analysis/macro-expansion/src/.../MacroSemanticExpansionConstruction.kt`（编排 + 重试）
- `analysis/macro-expansion/src/.../MacroSemanticSessionConfigurator.kt`（触发）
- `analysis/macro-expansion/resources/META-INF/analysis-api/cangjie-macro-expansion.xml`（EP 声明 + configurator + service）
- `analysis/macro-expansion/testFixtures/.../FakeMacroSemanticExpansionEnvironment.kt` / `FakeMacroSemanticExpansionEngine.kt
- `analysis/macro-expansion/test/.../MacroSemanticExpansionRetryTest.kt` / `MacroExpansionRevisionTest.kt
- `macro/macro-common/src/.../expansion/`（SPI：`MacroSemanticExpansionEngine` / `MacroExpansionArtifactPreparation` /
  `MacroExpansionRevision` / `MacroExpansionBarrier` / `MacroConstructionReadAccess` / `SdkHandle` / `ModuleRoots` /
  `MacroExpansionOutcome`）
- `compiler/frontend/src/.../FrontendMacroSemanticExpansionEngine.kt
- `analysis/low-level-api-cfir/src/.../macro/MacroSemanticSnapshotContract.kt`（唯一判据）
- `lsp/test/.../MacroExpansionHostContainerProbeTest.kt`（Phase 2 容器探针，已固化为回归用例）
- `docs/macro-expansion-host-integration.md`（新宿主接入指南）

修改（已落地）：

- `settings.gradle.kts`：注册 `:analysis:macro-expansion`；
- `macro/macro-common/build.gradle.kts`：新增 `api(project(":cfir:providers"))`（引擎 SPI 依赖其宏构造类型；无环）；
- `compiler/frontend`：`MacroConstructionReadAccess` 改为指向 macro-common 的类型别名（**不新增** 对 analysis 的依赖）；
- `intellij-ide/modules/ide/macro`：`IntelliJMacroSemanticExpansionEnvironment`；`cangjie-macro.xml` 只注册 environment；
- `intellij-ide/modules/ide/base`：`cangjie-ide-analysis-api-cfir.xml` 新增宏展开描述符的 `xi:include`；
- `analysis/low-level-api-cfir`：`prepareMacroSemanticSnapshot` 改用 `MacroSemanticSnapshotContract`；
  `LLCfirMacroConstructionInput.kt` 新增 `hasMacroCallCandidateForModule()`（Phase 1c 成本门禁），
  并修复 surface store 懒创建导致的首次构造丢 surface；
- `analysis/analysis-api-cfir`：描述符 `cangjie-analysis-api-cfir.xml` 新增 `xi:include`；删除 `[nav]` 探针；
- `analysis/cj-references`：删除 `[nav]` 探针；
- `analysis/analysis-api-standalone`：headless 容器扩展点白名单加入
  `llCfirSessionConfigurator` / `macroExpansionEnvironment`，使 LSP 与 Analysis 测试容器
  能装配触发器与服务（环境仍需宿主显式注册）；
- `prepare/ide-plugin-dependencies[-module]`：fat jar / module 形态加入 `:analysis:macro-expansion`；
- `lsp/build.gradle.kts`：加入 `implementation(project(":analysis:macro-expansion"))`（环境注册待 Phase 2）。

已落地（Phase 2 结构 + 装配）：

- `lsp/build.gradle.kts`：新增 `implementation(":compiler:frontend")`、`implementation(":macro:macro-process")
  （方向已确定为进程内 frontend 引擎，理由见 Phase 2 开头）；
- `lsp/src/.../LspMacroSemanticExpansionEnvironment.kt` / `LspMacroExpansionOptions`；
- `lsp/src/.../LspMacroExpansionDefaults.kt`：`frontendMacroExpansionOptions(sdk)` 装配引擎与编译配置；
- `lsp/src/.../AnalysisApiLspProjectStructure.kt`：`CaLspSourceModule` 暴露 `internal sourceRoots
  （LSP `psiRoots` 是文件集合，不是目录）；
- `lsp/src/.../CangjieLspEnvironment.kt` / `CangjieLspServerOptions.kt`：environment 注册与 options 注入；
- `lsp/test/.../MacroExpansionHostContainerProbeTest.kt`、`LspMacroSemanticExpansionEnvironmentTest.kt`；
- `macro/macro-common`：`SdkHandle` 新增 `macroServerPath`（宿主传入，不推导布局）；
- `intellij-ide`：`IntelliJMacroSemanticExpansionEnvironment.sdk()` 填充 `macroServerPath`。

待落地（Phase 2 剩余）：

- LSP 产品启动壳提供真实 `SdkHandle`（SDK 发现属于宿主职责）并启用 `macroExpansion`；
- 发布后 diagnostics / semantic tokens 刷新（`publishListener` 钩子已就绪）；
- 端到端用例（真实执行受本机 LSPMacroServer 管道限制）。
- `analysis/low-level-api-cfir/testFixtures`（或 `analysis:analysis-test-framework`）：Analysis API 级
  parity 用例载体（同一位置两份 CFIR、无宏不发布等）。

已删除（Phase 1b 末尾）：

- `intellij-ide/.../LLCfirMacroSessionConfigurator.kt
- `intellij-ide/.../LLCfirMacroSnapshotService.kt`（能力迁入 `analysis:macro-expansion`）
- IDE 侧随之迁移的 `MacroSemanticSnapshotInputRevisionTest` / `MacroSemanticSnapshotConstructionRetryTest
  （语义已在 `analysis:macro-expansion` 重新落地）

删除（Phase 1b 末尾）：

- `intellij-ide/.../LLCfirMacroSessionConfigurator.kt
- `intellij-ide/.../LLCfirMacroSnapshotService.kt`（能力迁入新服务）

保持不变（明确非目标）：

- `intellij-ide/.../MacroSemanticHighlightingRefreshService.kt`：宿主屏障实现，通过 `onPublished/onAbandoned` 接入；
- `intellij-ide` 的 `macro.engine` / `macro.service` / `macro.pipeline` / `macro.server`：面向 UI 展开的独立链路，本计划不改。

## 6. 测试计划

### 6.1 单元（fake host/engine，落在 `analysis:macro-expansion`）

已落地（v3）：

- `MacroSemanticExpansionRetryTest`：输入变化重试、会话失效后停止；
- `MacroExpansionRevisionTest`：PSI / roots / 编译器设置 / SDK / artifact 指纹任一变化都使 revision 不相等；
- testFixtures：`FakeMacroSemanticExpansionEnvironment`（取值可编程、回调计数）、`FakeMacroSemanticExpansionEngine
  （调用计数与结果注入）。

待补（需要真实 `LLCfirResolvableModuleSession` 的测试载体，建议落在 `analysis:analysis-test-framework`）：

- 无 surface / 无候选：`expand` 不被调用、不发布快照、保持实时视图；
- 有 surface：快照发布一次；revision 未变不重建；
- revision 变化：旧快照失效、新快照发布、旧结果丢弃；
- 并发 request：同模块单 in-flight；
- 失败：记录失败 revision，不发布，不无限重试；
- 发布必须在 barrier 内，发布后 raw/fileStructure 已失效；
- 宿主侧调用计数：`onConstructionStarted` / `onPublished` / `onAbandoned` 恰好各一次。

### 6.2 Analysis API（落在 `analysis/low-level-api-cfir` testFixtures）

harness：把 fake environment（`analysis:macro-expansion` testFixtures）注册到 `macroExpansionEnvironment` EP，
再由框架自带的 `MacroSemanticSessionConfigurator` 触发；若测试容器不加载该 XML，则用
`LLCfirSessionConfigurator.registerExtension(project, ...)` 直接安装 configurator。

- 同一位置两份 CFIR（成员不同）：只保留展开视图，解析到展开成员，不产生歧义；
- 不同文件同 ClassId：仍然 `ConeAmbiguityError`；
- 宏生成声明的身份：与物理声明位置区分；
- 无宏模块：不发布快照，实时视图成员可见（同时断言未发生整模块 raw 构建）。

### 6.3 集成 / 端到端

- IDE：`CjMacroSemanticSnapshotIntegrationTest` 等现有用例，外加「无宏模块不发布」用例；
- LSP：definition/hover/diagnostics 与 IDE 对齐；无宏工程无快照；编辑往返后重建；
- `analysis-real-project-test`：**依赖 Phase 2 引擎接线**，需在 standalone 平台注册 environment 后才能跑宏工程；顺序上排在 Phase 2 之后；
- 回归命令（示例）：
  - `gradlew-queue.bat :analysis:macro-expansion:test
  - `gradlew-queue.bat :analysis:low-level-api-cfir:test --tests "*Macro*"
  - `gradlew-queue.bat :lsp:test --tests "*Macro*"

## 7. 验收标准

1. 同一宏工程 IDE 与 LSP 的 definition/hover/diagnostics 一致；（待 Phase 2）
2. 无宏工程不发布宏快照，`SqlParameter` 类导航稳定（打开、编辑、重开）；（待真机验证）
3. 同一位置多视图不产生歧义，且展开视图成员优先；（待 6.2 parity 用例）
4. 不同位置同名声明仍正确报告重声明歧义；
5. `analysis:macro-expansion` 的依赖图不含 `compiler:frontend`，`compiler:frontend` 的依赖图不含 `analysis:*
   —— **已验证**（`dependencies --configuration compileClasspath`）；
6. 现有 analysis/IDE 宏测试与 real-project 测试全绿；（部分验证：模块单测与 IDE 编译通过）
7. 宏构造失败/取消/项目关闭无泄漏、无死锁；LSP 容器内不出现写访问断言失败。（待 Phase 2/3）

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 分层反转（frontend → analysis） | SPI 放叶子模块；frontend 只 implement；依赖图断言 |
| SPI 出现宿主类型（CjSdk 等） | 统一 `SdkHandle` / `ModuleRoots`；code review 检查 analysis 模块 import |
| 写屏障在 LSP 容器不可用 | Phase 2 前置探针；`barrier()` 抽象按容器能力实现 |
| 触发点缺失导致完全不展开 | 新模块自带 configurator；LSP 显式注册 environment；集成测试覆盖 |
| 「无宏零成本」误判 | 明确不承诺；Phase 1c 预扫描门禁；指标监控 |
| revision 过宽导致全模块重建 | 模块级/文件级 revision；纳入 Phase 1c |
| 快照内存/性能 | 软引用 + identity 跳过 + 按模块共享（已有）；指标监控 |
| 迁移期行为不一致 | Phase 1a 先锁行为，Phase 1b 只搬不改；parity 用例先行 |
| 命名与既有宏子系统混淆 | 统一 `MacroSemantic*` 前缀，文档写明两套链路边界 |
| LSP 打包宏执行器成本 | **已决策**：走进程内 `compiler:frontend`（唯一完整引擎；macro-process 只有执行器）。若分发体积成为硬约束，再评估"宏配置键 + artifact 准备下沉 + macro-process"重构 |
| IDE fat jar 集合不完整（既有） | 实测：`prepare/ide-plugin-dependencies/*` 的 jar 均不含 `compiler:frontend` / `macro:macro-common`（README 声称 cfir-for-ide 含 frontend，但列表里没有）。IDE 插件实际走 `-module` 变体，集成测试证明该路径可用；本次只把 `:analysis:macro-expansion` 加入与描述符 include 对齐。若要让 fat jar 独立可运行，需另行补齐 `:compiler:frontend` / `:macro:macro-common`，属于既有打包缺口，不在本计划内 |
| 真实宏执行在开发机崩溃（msvcrt/OS 兼容） | WER：`msvcrt.dll+0x62f1e`，`0xC0000005`；5 个服务端构建（0.53.13/0.53.18/1.0.0/1.0.5/1.1.3）在运行时库正确加载后全部立即崩溃，非法句柄则干净报错。句柄继承已由子进程探针证明有效，启动参数已对齐官方。非仓库代码问题，需换 Windows 构建/机器验证或由仓颉团队按 WER 签名修复 |

## 9. 回滚与兼容

- Phase 1b 的删除已落地；回滚方式是按 Git 历史恢复 `LLCfirMacroSessionConfigurator.kt` /
  `LLCfirMacroSnapshotService.kt` 并恢复 `cangjie-macro.xml` 的 `llCfirSessionConfigurator` 注册
  （分析框架的 `MacroSemanticSessionConfigurator` 与之并存不会冲突，但会造成重复构造，回滚时应同时移除）；
- 快照 API（`prepare`/`publish`/`invalidate`）签名不变，Analysis API 上层无需改动；
- LSP 侧默认行为不变：未注册 environment 时不展开、不发布快照（与现状一致）；
- 失败降级：engine 抛错 → 记录失败 revision + `onAbandoned`，保持实时视图，不阻塞编译/分析。

## 10. 开放问题

1. ~~LSP 是否接受打包 `compiler:frontend`~~ **已决策（v6）**：接受。完整宏引擎只存在于
   `compiler:frontend`（artifact 定位/编译/校验、符号索引、import 绑定、stable splice）；
   `macro:macro-process` 只有执行器，单独使用不构成引擎。LSP 已新增
   `implementation(":compiler:frontend")` 与 `implementation(":macro:macro-process")`，
   由 `frontendMacroExpansionOptions(sdk)` 装配；`SdkHandle` 新增 `macroServerPath`，
   所有 SDK 路径由宿主传入。`macro-process` 仅作为执行器被 frontend 引擎使用。
   若未来分发体积成为硬约束，再做"宏配置键 + artifact 准备下沉"的重构。
2. 快照是否继续「按模块全局共享」，还是随会话隔离（影响 revision 与失效粒度）。
3. 宏生成声明的 `declarationSiteId` 语义：生成文件路径 + 范围是否稳定？是否需要额外的 origin surface id？
4. 是否需要在 `analysis-api` 公开宏展开状态（供 IDE/LSP 展示「展开中/失败」）。
5. ~~预扫描门禁的落点~~ **已定**：PSI 语法扫描（注解 / `CjMacroExpression`），保守下界；索引方案留作后续优化。

## 11. 工作量
| 阶段 | 内容 | 估算（单人） | 状态 |
|---|---|---|---|
| Phase 0 收尾 | 真机验证 + 探针清理 + 判据合并 + 提交 | 1–2 天 | 探针清理、判据合并已完成；待真机验证与提交 |
| Phase 1a | 行为锁定与判据收敛 | 2–3 天 | 已完成 |
| Phase 1b | 新模块 + SPI + IDE 迁移 + parity | 6–8 天 | 已完成（parity 用例待 6.2 载体） |
| Phase 1c | 成本门禁 + revision 细化 | 3–4 天 | 预扫描门禁 + 耗时日志 + raw 构建计数断言已完成；revision 细化暂缓 |
| Phase 2 | 容器探针 + LSP 接入 + e2e | 6–8 天 | 探针、LSP 环境、前端装配、SdkHandle 落地；剩产品壳 SDK 注入、发布后刷新、真实执行 e2e（本机服务端 msvcrt 崩溃，WER 已定位） |
| Phase 3 | 加固、指标、文档 | 3–4 天 | 会话失效取消已覆盖、接入指南已完成；项目关闭/写动作/barrier 阻塞、遥测指标未做 |
| 合计 | | 约 3.5–5 周（含评审与回归） |

## 12. 后续工作清单（可直接执行）

按依赖顺序，每项都给出落点与验收方式：

1. **Phase 2 宿主启用**：`lsp` 产品启动壳构造真实 `SdkHandle`（home / environment / stdlib /
   `macroServerPath` 全部显式，遵循上节"不推导布局"），调用 `frontendMacroExpansionOptions(sdk)` 并传入
   `CangjieLspServerOptions.macroExpansion`；验收：LSP 容器内 `MacroSemanticExpansionEnvironment.getInstance` 非空、
   `frontend defaults` 用例保持绿。
2. **Phase 2 发布后刷新**：实现 `LspMacroExpansionOptions.publishListener`，把受影响文件交给现有
   diagnostics / semantic-token 通知通道；验收：发布后客户端收到刷新（可在 LSP 集成测试中断言）。
3. ~~Phase 1c raw 构建计数~~ **已完成**：`PreMacroRawBuildCounter`（session 组件）+
   `CfirSession.preMacroRawBuildCount()`；IDE 用例断言无宏模块（含重建）为 0、有宏模块为 1。
4. **Phase 3 并发补充**：项目关闭、构造中写动作、barrier 阻塞；可复用
   `testSessionInvalidationCancelsInFlightConstruction` 的 latch 模式，断言 `onAbandoned` 恰好一次且无发布。
5. **Phase 3 遥测（暂缓，已查明原因）**：编排层耗时 / 结果（PUBLISHED、NO_MACRO_SURFACE、FAILED、
   CANCELLED、INPUT_CHANGED）目前只在日志里。要进统计域，需要先给
   `LLStatisticsService` 提供公开的指标入口——其 `openTelemetry` 是 `internal`，
   `analysis:macro-expansion` 无法直接取 meter；这是跨模块的小幅 API 扩展，应与统计域 owner 一起设计，
   不应在框架侧私自建 meter。现有 `LLMacroConstructionStatistics` 已覆盖 CFIR 构造三段耗时与结果。
6. **真机验收**：在非受限环境运行 `CjMacroSemanticSnapshotIntegrationTest` 与
   `cfir:analysis-tests` 的宏 LLT（本机被 `LSPMacroServer` 子进程管道限制），并完成 Phase 0 提交。
