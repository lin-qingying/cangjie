# 宏 IDE 辅助功能实现对照（Macro IDE Implementation Status）

本文是 [`macro-ide-feature-spec.md`](macro-ide-feature-spec.md) 41 条需求（`#N`）与当前实现的**逐条对照快照**。快照时间 2026-10-15。产品宿主测试覆盖 #9/#17 的真实 PSI → MacroExpansionService → LSPMacroServer → 预览面板、来源着色与嵌套下钻；#26 的宏执行日志、宏源码断点和单步；#38 的真实宏 artifact 编译、变化后的重编译、语义快照刷新、Analysis API 补全 / 解析 / 类型查询和重载诊断回归；#39 真实生成节点导航和 #40 真实 LL 快照 Find Usages。矩阵状态按源码链路、扩展点、产物装配和已执行的宿主测试评估；**41 项实现状态为完成不等于全部真实桌面交互已经验收**，本轮的桌面交互验证仍待 computer-use 窗口控制恢复。

状态符号：

- ✅ 已落地 —— 有实现且有注册/调用链路。
- 🔶 部分完成 —— 有实现与调用链，但至少一个需求行为尚未完成，或缺少能证明完整运行链路的验证。
- ⬜ 未实现 —— 无实现，或实现存在但无任何调用方。
- ⚠️ 阻断 —— 已写代码但无法生效（装配或编译层面）。

判定依据：源码引用关系 + 扩展点/描述符注册 + 产物装配（Gradle 依赖树），不是一次运行结论。

## 0. 阻断项（本轮全部解除）

| # | 事实（首轮快照） | 证据 | 处置 |
|---|---|---|---|
| B1 | `:modules:ide:macro` 未被装配进插件产物 | `intellij-ide/product/idea-plugin/build.gradle.kts` 依赖列表无该模块 | ✅ `product/idea-plugin/build.gradle.kts` 增加 `implementation(project(":modules:ide:macro"))`；`product/idea-plugin/src/main/resources/META-INF/plugin.xml` 增加 `<xi:include href="/org.cangnova.cangjie.ide.macro.xml"/>` |
| B2 | Action 描述符指向未打包的类 | `modules/ide/base/.../META-INF/cangjie-actions.xml:42` 注册 `CangJie.ShowMacroExpansion`，类位于 macro 模块 | ✅ 动作注册迁入宏责任域的 `modules/ide/macro/.../META-INF/cangjie-macro.xml` 的 `<actions>`；base 侧只留一条指向说明（base 不依赖宏模块） |
| B3 | 行标类引用不存在的索引，macro 模块无法编译 | `modules/ide/macro/.../ui/CjMacroExpansionLineMarkerProvider.kt:48` 引用 `org.cangnova.cangjie.stubindex.CangJieMacroDeclarationShortNameIndex` | ✅ 改读**宿主符号索引**：`CangJieStubIndexKeys.TOP_LEVEL_CALLABLE` + 按 `CjMacroDeclaration` 收敛（写侧 `CangJieStubIndexService.indexMacroFunction` 已在索引宏声明）；dumb 期间保守返回 false |
| B4 | 宏 LSP 协议依赖的 flatbuffers 生成类不在模块类路径（首轮未发现） | `modules/ide/macro/.../server/protocol/MacroMsgCodec.kt:13` `import MacroMsgFormat.*`；生成物只存在于上游 `flatbuffers-gen/gen/` | ✅ `settings.gradle.kts` 增加 `substitute(module("org.cangnova.cangjie:flatbuffers-gen")).using(project(":flatbuffers-gen"))`，模块增加 `implementation(libs.cangjieFlatbuffersGen)`——生成物仍只在上游一份，宿主只消费 |
| B5 | 宏模块描述符声明的 `<module name="…"/>` 跨 jar 解析不到（首轮未发现） | `modules/ide/macro/.../org.cangnova.cangjie.ide.macro.xml` 的 `<dependencies>`；同坑记录见 `modules/test-support/src/test/hostPlugin/META-INF/plugin.xml:14` | ✅ 描述符改为「只有 `xi:include`」的形态，与其它模块描述符同构；模块间依赖由 Gradle 依赖图表达，不在描述符里重复声明 |

结论：B1–B5 解除后，宏模块**首次进入编译与装配链路**；`#7`、`#8`、`#32`、`#34`、`#35` 由「源码层成立」变为「产物层可用」。

## 1. 编写宏调用

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 1 | `@` 补全宏名 + 自动 import | ✅ | `macro/references/CjMacroCompletionContributor.kt` + 描述符注册；宏专用 stub 索引结合 public/protected 重导出视图召回公开宏，插入时经 `CjPsiFactory` 为可见宏包添加 `import <package>.*`；`ImportPath.organizationName` 保留 `organization::package` 身份，并检查同组织下重复项 | — |
| 2 | 跳宏定义 / Find Usages / Quick Doc | ✅ | `macro/references/CjMacroReferenceProvider.kt` 注册到上游 `psiReferenceProvider`，引用按显式导入、wildcard、别名、全限定名及组织限定匹配；`MacroDeclarationIndex.allVisible` 将宏包间 `public/protected import`（含组织限定、别名、多级重导出）投影回原始声明，供引用解析、Find Usages 名称候选和补全共用；`CangJieDocumentationTarget` 复用 `CjMacroDeclaration` 的 Analysis API 签名/文档渲染。产品集成测试覆盖组织隔离、wildcard/alias 重导出、`ReferencesSearch` 与组织限定自动导入 | — |
| 3 | Quick Doc 标注可否展开 / debug | ✅ | `CangJieDocumentationTarget` 在宏声明/调用 Quick Doc 中标注展开预览可用条件（SDK + 引擎）及 `debugMacro` 运行配置的可用性 | — |
| 4 | input 参数语法高亮与校验 | ✅ | lexer 将宏专用转义识别为独立 token；解析器在 Tokens 输入中校验括号式 / 属性式转义并对非法上下文转义报错，quote 转义仍走独立集合。`MacroTokenEscapeAnnotator` 只为匹配当前输入形态的转义着色，并排除声明式输入；`MacroCallSignatureAnnotator` 按已解析声明校验括号式一参与属性式二参 Tokens 签名 | — |
| 5 | 上下文约束提示 | ✅ | `MacroContextAnnotator` 从已解析宏声明体中识别 `std.ast.assertParentContext` 与 `std.ast.getChildMessages` 的字符串字面量实参，在宏调用点提示外层宏约束 / 子宏消息来源；只作信息提示，不把条件执行约束误报为错误，动态表达式不推测 | — |
| 6 | 快速修复（缺 import / 未编译 / 拼写） | ✅ | `MacroCallQuickFixAnnotator` 为未解析调用提供精确宏包导入与受限编辑距离拼写修正；已解析但宏库缺失/过期时提供按源宏包编译动作，编译成功后清除展开缓存；工具链不可用或产物新鲜时不显示不可执行动作 | — |

## 2. 展开查看

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 7 | 展开单个宏 | ✅ | `ui/ShowMacroExpansionAction.kt`（Ctrl+Alt+M，右键 + Tools 菜单）、`ui/CjMacroExpansionLineMarkerProvider.kt`（gutter 点击）、`MacroExpansionService.expandMacroAtOffset` | B1/B2/B3 解除后进产物 |
| 8 | 展开整文件全部宏（列表定位） | ✅ | `expandAllMacrosInFile` + `ui/MacroExpansionListPanel`（每条一个只读编辑器 + 标题） | 同上 |
| 9 | 递归 / 单层切换、逐层下钻 | ✅ | `MacroExpansionEngine.supportsRecursiveExpansion` 显式描述引擎能力；递归请求遇到不支持的 cjc-frontend 时，服务改选可用的 LSPMacroServer，并保留实际引擎元信息；没有递归能力引擎时返回明确能力错误。`CangJieMacroConfigurable` 提供递归 / 单层选项；`MacroExpansionPanel` 可从嵌套贡献按钮逐层进入子宏片段，并通过面包屑返回上层；`MacroExpansionDrilldownView` 重定位后代区间和源锚点；生产递归器与测试共用 `NestedMacroExpansionResolver`。真实产品宿主测试使用 PSI 调用分别请求单层与递归结果，再从递归预览下钻到内层结果 |
| 10 | 弹窗 / 只读虚拟文档 / diff | ✅ | `JBPopup` + `MacroExpansionPanel` 提供弹窗预览；「在编辑器中打开」以只读 `LightVirtualFile` 创建具备 `.cj` 高亮的标签页；Diff 按钮用 `DiffManager` 对比调用源码与展开文本。cjc 引擎仅在 PSI 文本与本次编译输入一致时提供完整调用源码，LSP 引擎直接保留对应 PSI 文本 | — |
| 11 | 元信息（名/调用点/引擎/缓存/耗时/诊断数） | ✅ | `MacroExpansionPanel.createInfoPanel()`：来源（Engine/Cache + 引擎名）、宏名、调用点 `文件:行:列`、耗时、诊断数；缓存命中时耗时显式显示「缓存命中」而非 0ms | — |
| 12 | 陈旧标记 | ✅ | `MacroExpansionServiceImpl` 将展开时的 Document modification stamp 附在结果上；单宏预览和全文件列表监听源 Document，结果版本不匹配或打开后发生编辑均显示「结果已过期，需要重新展开」 | — |
| 13 | 可复制 / 导出 | ✅ | 面板「复制」按钮（剪贴板）+「导出…」按钮（`FileChooserFactory` 存为 `.cj`） | — |

## 3. 位置映射与导航

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 14 | 展开块 → 调用点 | ✅ | `MacroCallFileParser` 解析 `.macrocall` 的 `/* 行.列 */` 前缀为**逐行锚点**（`MacroLineAnchor`，与展开文本行一一对应）；面板以 cjc UTF‑8 字节列经 `MacroSourcePosition` 转为 IntelliJ UTF‑16 文档偏移，当前版本 Ctrl/Cmd+单击可跳回源位置 | — |
| 15 | 调用点 → 展开片段高亮 | ✅ | `MacroExpansionPanel.highlightExpansionLinesFromCallSite()` 将 UTF‑8 source anchor 列转为 UTF‑16 文档偏移，命中当前调用范围的展开行；源文档版本已过期时不使用旧锚点高亮 | — |
| 16 | 展开代码内符号跳转 / 引用 | ✅ | `MacroExpansionPanel` 用调用点 PSI 创建带 `elementContext` 的只读 CangJie PSI 虚拟文件；快捷展开、行标与诊断修复均传递真实宏调用上下文；产品集成测试打开实际编辑器标签后验证 `mainReference.multiResolve` 跳到项目声明，`ReferencesSearch` 在展开文件作用域找到该引用 | 跨展开文件到原调用源码的位置映射由 #40 覆盖 |
| 17 | 嵌套宏贡献着色 | ✅ | 递归请求路由到声明支持来源区间的 LSPMacroServer；`NestedMacroExpansionResolver` 在替换同级宏后累计偏移并平移子贡献区间，逐条记录宏名、文本范围和层级；`MacroExpansionResult.withSourceDocumentModificationStamp()` 保留来源贡献元数据；`MacroExpansionPanel` 按层级覆盖着色并显示宏名图例。真实产品宿主测试验证实际 PSI 调用产生的外层与内层图例、编辑器着色范围及嵌套下钻 |

## 4. 诊断与错误

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 18 | `diagReport` 落在调用点 | ✅ | `compiler/MacroCompilerOutputParser` 解析编译器诊断段（级别 / message / `==> 文件:行:列` / `^` hint）→ `MacroDiagnostic`（新增 `hint`/`fileName`/`line`/`column`）→ `analysis/MacroCallSiteAnnotator`（注册在 `cangjie-macro.xml`）贴回调用点，hint 进 tooltip；诊断来源是**展开结果缓存**，annotator 不触发展开 | — |
| 19 | 错误映射回调用点 + 跳展开位置 | ✅ | `MacroExpansionDiagnosticMapper` 将 `.macrocall` 的 UTF‑8 字节列（含行锚前缀与多字节生成文本）映为 UTF‑16 展开偏移，再通过逐行 source anchor 定位调用点；`MacroCallSiteAnnotator` 同样执行字节列换算，修复动作打开展开面板并定位/高亮生成错误 | 诊断取自展开结果缓存；默认需开启 `cangjie.macro.expansion.analysis.enabled` |
| 20 | 展开失败分类诊断 | ✅ | `service/MacroFailureKind`（宏包未编译 / 动态库不可用 / 宏函数异常 / 超时 / 引擎不可用 / SDK 未配置 / 解析失败 / 未分类）+ `service/MacroFailureClassifier`（纯函数，只按已知特征串判定，匹配不到归 UNKNOWN）；`MacroExpansionError.failureKind` 贯通到通知标题 | — |
| 21 | 失败 quick fix | ✅ | `ui/MacroFailureNotification`：按分类给动作——编译宏包（`MacroCompilationService` + 清缓存 + 成功后重试）、重试、切换引擎（写 `MacroExpansionSettings`）、延长超时（翻倍，上限 10 分钟）；SDK 未配置一类**不给**动作（配 SDK 不在这些入口里） | — |
| 22 | debug 模式限制提示 | ✅ | 宏 Quick Doc 中标注 cjc debug 模式的三类已知限制：换行敏感调用、宏定义内调用和带括号宏调用；并提示可使用 `debugMacro` 运行配置调试展开后代码 | — |
| 23 | 同因诊断聚合 | ✅ | `MacroCallSiteAnnotator.relatedCalls()` 从文件级展开缓存按宏身份、级别、message、hint 和精确调用范围分组；只在最早调用点生成一条包含数量的标注，并在 tooltip 列出全部行号；产品集成测试确认两处相同诊断只显示一次 | — |

## 5. 宏作者侧

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 24 | 宏定义识别与签名校验 | ✅ | `analysis/MacroDeclarationSignatureAnnotator` 注册在宏描述符；校验宏包、`public`、1–2 个位置参数、每个参数为 `Tokens`、显式返回类型为 `Tokens`；对照官方 `ParseMacro.cpp`，省略返回类型合法（编译器补为 `Tokens`）；产品测试覆盖有效/无效签名与宏包位置 | — |
| 25 | 分包约束校验 | ✅ | `analysis/MacroPackageConstraintAnnotator` 按原始声明包（含组织名）诊断“宏定义与调用不能同包”，引用仍可跳转到定义；避免把 facade 重导出名称当作原始定义包。宏包未编译/过期时调用点编译 quick fix（#6）与展开前自动编译管线（#32）保证先编译 | — |
| 26 | 宏函数调试（编译期执行） | ✅ | 「调试宏函数」调用点动作与显式引擎能力契约；强制编译隔离的 `macro-debug` 产物并请求 `-g -O0`；独立 LSPMacroServer 先由 DAP attach 并等待 `configurationDone`，再发送目标宏调用；`LspMacroProcessHandler` 把 stderr 以及 Windows 独立协议管道下的 stdout 转给调试输出监听器，`LspMacroServerProvider` 将其送入 DAP 控制台；服务停止或项目服务释放时关闭服务端 | 真实 1.0.0 SDK 产品集成用例验证宏源码断点精确命中、单步至下一源码行、继续后展开成功、DAP `library-loaded` 事件，以及宏体 `println`（显式 flush 后）、`print(flush: true)` 和 `eprint` 的 stdout/stderr 文本到达服务进程输出监听器。1.0.5 SDK 所配 DAP server 缺少 MinGW runtime DLL，故该 DAP 集成用例以具备完整运行库的 1.0.0 SDK 执行 |
| 27 | 展开快照回归 | ✅ | `MacroExpansionPanel` 的「保存展开快照…」生成可提交的 `.cj` 基线文件，文件名包含源文件、宏名和调用偏移；再次展开后可从「对比快照…」加载基线 | 回归基线由宏作者选择保存位置并提交 |
| 28 | 改宏前后展开 diff | ✅ | 展开面板的「对比快照…」把保存的旧结果和当前展开结果交给 IDE `DiffManager` 并标注左右版本；修改宏实现后重新展开并选择原快照即可比较 | — |

## 6. 调试

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 29 | `--debug-macro` 构建开关 | ✅ | `CangJie.ToggleMacroDebugBuildOption` 注册在编辑器上下文菜单和 Tools 菜单，可切换当前 `cjpm.toml` 的 `[package].compile-option` 独立参数；`CangJieProgramRunConfigurationEditor` 另提供继承 / 启用 / 禁用三态运行配置覆盖，由 CJPM 构建执行器落入同一 TOML 字段；均保留其它编译选项和注释 | — |
| 30 | `debugMacro` 运行配置 | ✅ | `CangJieProgramRunConfiguration.macroDebugOverride` 可持久化项目默认 / 启用 / 禁用；`CangJieBuildManager` 将选项传给构建配置，`CjpmCommandExecutor` 通过 `CjpmMacroDebugOption` 更新 `package.compile-option`；启用宏调试时普通 Run runner 不接收该配置；`.macrocall` 注册为 `CangJieFileType`，现有 DAP / Proto 行断点类型可在展开文件设断点 | — |
| 31 | 单步位置跟随 | ✅ | `debugger-api/MacroCallSourceMapping` 解析 `.macrocall` 行锚点，`MacroCallFileParser` 复用同一行前缀规则；注册的项目服务由 DAP 与 Proto 每次暂停时传入栈帧位置，打开原始 `.cj` 与 `.macrocall` 编辑器并用双向 caret listener 同步，返回展开侧时落在锚点行的代码字符处 | `.macrocall` 头部无法唯一解析到项目内原始文件时不建立映射 |

## 7. 工程与性能

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 32 | 宏包按需编译 + 缓存失效 | ✅ | `compiler/CompilerMacroCompilationProvider.kt`（`cjc -p <dir> --compile-macro`）、`pipeline/MacroPipelineCoordinator{Impl}.kt`、`MacroPipelineProjectSyncListener`、`MacroFileChangeListener` | B1 解除后进产物 |
| 33 | 缓存 / 去重 / 超时 / 取消 / 进度 | ✅ | `MacroExpansionCache` 按文件版本（含未保存 Document）、递归/自动编译选项和引擎偏好分隔缓存，并监听 Document/VFS 变更；文档戳读取在 `ReadAction` 中执行，可由后台 suspend 展开服务安全调用；服务对相同文件版本/位置/选项的并发请求合并执行，执行方取消后活动等待者接手重试；用户触发展开进入可取消的 `Task.Backgroundable` 并把进度取消传播到协程、cjc 进程和 LSP 请求；cjc 与 LSP 均应用超时，LSP 超时会终止服务端进程 | — |
| 34 | 引擎选择与自动回退 | ✅ | `MacroExpansionServiceImpl.selectProvider` / `executeWithFallback` / `shouldFallback`；`MacroExpansionSettings.preferredEngineId`；两个 `macroExpansionProviderFactory` 注册（cjc-frontend / LSPMacroServer） | B1 解除后进产物 |
| 35 | 全局开关 | ✅ | `META-INF/cangjie-macro.xml` 注册并由代码读取自动预热、递归、缓存、超时、预览行标/缓存、文件可见性等 Registry keys；`CangJieMacroConfigurable` 另提供引擎选择 | — |
| 36 | `.macrocall` 可见性与落盘 | ✅ | registry `cangjie.macro.expansion.file.visible` + `CjMacroCallFileHiddenProvider` 控制项目视图；项目级 `MacroExpansionSettings.keepCompilerMacroCallFiles` 控制 cjc 生成文件的清理；`CangJieMacroConfigurable` 提供独立可见性/保留设置 | — |
| 37 | 执行日志与排障信息 | ✅ | `MacroExpansionServiceImpl` 按项目保留最近 50 次请求（时间、单宏/文件操作、路径、耗时、引擎、缓存命中、宏数量、递归/超时选项、成功/失败/取消与分类原因）；Tools → Copy Macro Expansion Diagnostics 复制引擎状态、缓存占用和有界请求历史 | — |

## 8. 语义集成

| # | 需求 | 状态 | 落点 / 证据 | 缺口 |
|---|---|---|---|---|
| 38 | 展开参与补全 / 解析 / 类型 | ✅ | `LLCfirSession.buildPreMacroRawBuildResultForModule` 收集完整模块 raw CFIR，保留 source PSI identity 与 construction identity；产品配置器异步完成分类、真实宏包编译、LSPMacroServer 执行和原子发布。Cangjie 1.0.5 产品宿主测试覆盖 `generated` 的 Analysis API 名称补全、引用解析及 `Int64` 类型；同一用例保存宏源码变更并确认新 `.cjo` 生成 `generatedFromUpdatedArtifact`、旧名消失，再编辑未保存宿主 Document，验证旧 session 失效、新快照采用缓冲区 PSI 且不残留已删除的宏生成声明；并回归宏生成函数体引用唯一 `ideTarget` 时无 `CONFLICTING_OVERLOADS`；生命周期用例覆盖注册配置器的自动发布与 session 失效重建 | — |
| 39 | 结构视图显示宏生成成员 | ✅ | `CjMacroGeneratedStructureViewFactory` 以 `order="first"` 装饰现有 LSP4IJ 文档符号模型，合并文件根和匹配 class 节点下的 #38 LL-CFIR snapshot 声明；无 LSP 文档符号或 LSP 返回空树时从仓颉 PSI 声明树恢复基础大纲；宏生成节点标注“由宏生成”，点击跳转到 registry 记录的宏调用。产品宿主集成测试使用真实 LL snapshot 验证生成函数节点和调用点导航 | — |
| 40 | 引用查找覆盖展开符号 | ✅ | `cj-references` 的 `generatedReferenceSearchProvider` 接入统一 `ReferencesSearch`；IDE `LLCfirMacroGeneratedReferenceSearchProvider` 遍历已发布 LL-CFIR 快照的 resolved references，从 CFIR 目标符号恢复语义目标，并把生成 PSI reference 包装为宏调用 PSI 上可导航的只读 usage。产品宿主集成测试验证真实 source-module 快照的 `target()` 被 Find Usages 找到，结果映射回宏调用且仍解析到真实 `target` 声明 | — |
| 41 | 失败降级不污染语义 | ✅ | 不注入策略（`cangjie.macro.expansion.analysis.enabled` 注释"for IDE display/debug only"）；annotator 消费缓存但不产生任何 PSI/语义改动 | — |

## 9. 统计与结论

| 状态 | 数量 | 条目 |
|---|---|---|
| ✅ | 41 | #1–#41 |
| 🔶 | 0 | — |
| ⬜ | 0 | — |
| ⚠️ 阻断 | 0 | 原 B1–B3 与首轮未发现的 B4/B5 均已解除 |

本轮改变了前两轮的四条结构性结论：

1. **装配阻断已解除**：工程侧（编译调度、缓存、多引擎回退、全局开关）与展示侧（单个/整文件展开、锚点跳转、元信息）均进入产品装配链路。
2. **源与展开位置已双向可视**：编译器产物锚点（`MacroCallFileParser` → `MacroLineAnchor`）既支持展开行跳回调用点（#14），也支持把当前调用范围对应的展开行反向高亮（#15）；弹窗、只读编辑器标签与 diff 三种呈现已具备（#10）。`CaMacroExpansionMapping` 暂无宿主消费方；它返回源文件中的宏展开面区间，不能替代 `.macrocall` 的逐行输出位置锚点。
3. **诊断可以往返定位**：`diagReport` 诊断由 annotator 落在调用点（#18）；生成代码诊断经 `.macrocall` 行列与 source anchor 映射到调用点，并可从修复动作打开且定位展开文本（#19）；展开失败可分类并提供 quick fix（#20/#21）。诊断显示依赖展开缓存，且 `cangjie.macro.expansion.analysis.enabled` 默认关闭。
4. **宿主开始认识宏调用**：#1 已接入补全与自动 wildcard import；#2 已接入直接导入、别名、wildcard、全限定宏调用及含组织限定的 `public/protected import` 重导出，Ctrl+B、Find Usages 与 Quick Doc 共用解析目标。#9/#17 的真实 PSI 调用、递归服务响应、编辑器着色与嵌套下钻已通过产品宿主验证；#26 的宏执行日志、断点和单步已通过真实 DAP 产品宿主用例；#38 的真实宏 artifact 更新、普通 Analysis API 查询及重载诊断回归、#39/#40 的真实生成节点与引用搜索也有产品宿主测试。该矩阵不是桌面 UI 验收报告；当前桌面交互验证状态见本文末尾。

### #38–#40 的实现边界

- Kotlin 的 `FirDeclarationGenerationExtension` 是声明追加机制；仓颉宏会替换语法并改变既有 CFIR 节点，因此不能用 names-only generated symbol provider 代替真实展开 CFIR。
- LL-CFIR 必须让展开文件进入现有 package/class scopes、symbol lookup、container-file 映射和 lazy resolve。真实 stable-splice CFIR 现经 `LLCfirMacroSemanticSnapshot` 原子发布，并已接入上述 LL 查询边界；产品宿主集成测试已在同一 IDE session 里验证补全、引用解析和类型推导。
- LSP 宏执行和 artifact 准备由 `LLCfirMacroSessionConfigurator` 在应用线程池异步调度；compiler/frontend 的 `MacroConstructionReadAccess` 将 fragment reparse、stable splice 和结果快照哈希分别放入短 PSI 读访问，外部执行不持有读锁。任务固定捕获 SDK，并在外部产物准备后和发布前校验 PSI 修改计数、编译设置修改计数、SDK ID、construction identity、文件数与逐个 source PSI identity。真实宏产物变化后的重新编译与重新发布已由产品宿主用例单独验证。
- #39 结构视图与 #40 ReferencesSearch 已由同一产品宿主集成测试用真实 LL 快照验证生成声明展示/导航和引用搜索/目标映射；消费者读取 LL-CFIR 及 registry，不从 `.macrocall` 展示文本构造语义。
- 产品宿主集成测试分别验证真实 SDK 宏包调用经已注册配置器发布后参与 Analysis API 查询、无宏输入下配置器的后台发布及 session 失效重建，以及真实 LL 快照的 StructureView 和 Find Usages 消费。

## 10. 本轮验证

**#38 构造输入隔离（早期阶段记录）：** `LLCfirFileBuilder.buildRawCfirFileUncached` 使用相同 PSI raw builder 创建独立文件树；模块级 pre-macro 收集器从声明索引枚举文件并使用该入口，避免把普通 LL 文件缓存中的可变 CFIR 树交给 stable splicer。后续状态见本节最新配置器编译记录。

```
.\gradlew-queue.bat :analysis:low-level-api-cfir:compileKotlin --console=plain
→ BUILD SUCCESSFUL
```

**#38 PSI 阶段读访问调度（早期阶段记录）：** `FrontendMacroConstructionService` 现在通过 `MacroConstructionReadAccess` 分别执行片段解析、stable splice 和读取 PSI source 文本的结果快照哈希；executor 调用仍在这些读访问之外。后续 IDE 配置器已安装 IntelliJ `ReadAction` 适配器并在后台调度构造，端到端语义查询仍待验证。

```
.\gradlew-queue.bat :compiler:frontend:compileKotlin --console=plain
→ BUILD SUCCESSFUL
```

**#14/#15/#19 UTF‑8 字节列映射与过期锚点门控：通过。** cjc 的 1-based 字节列先扣除 `.macrocall` 前缀字节数，再按 UTF‑8 code point 映为展开文本和源文档的 UTF‑16 偏移；预览结果带源 Document 修改戳，文档版本变更后源锚点不再可导航或用于反向高亮。定向测试覆盖中文、补充平面字符、列落在多字节字符内部、行尾列和过期状态保持。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --tests "org.cangnova.cangjie.macro.compiler.MacroCallFileParserTest" --tests "org.cangnova.cangjie.macro.compiler.MacroExpansionDiagnosticMapperTest" --tests "org.cangnova.cangjie.macro.service.MacroSourcePositionTest" --console=plain
→ BUILD SUCCESSFUL；13 tests completed，0 failures
```

**本轮宏模块验证：通过。** 本目标明确要求实施并验证，因此运行了宿主宏模块测试；此前的 `intellij-ide/AGENTS.md` 默认不主动测试规则不适用于这一明确目标。

- 宏专用 stub index 在 `CangJieStubIndexService.indexMacroFunction` 写入，并由 `cangjie-decompiled.xml` 注册；索引版本升至 2。
- completion 与 reference provider 在宏模块描述符注册；引用进入 `CangJieReferenceProvidersService` 的现有解析和 Find Usages 链路。
- Quick Doc 的宏签名/文档复用 IDE 已有 Analysis API 声明渲染，并标注预览的环境条件与 `debugMacro` 专用运行配置尚未实现。
- 宏调用可见 API 索引把宏包内 `public/protected import` 的单项、wildcard、别名与多级重导出解析到原始声明；缓存依赖 PSI 修改计数与项目根修改计数失效。
- 单宏预览和全文件展开列表均监听源 Document 变更并显示陈旧提示；结果记录生成时的 Document modification stamp，单宏面板不会用过期锚点导航或反向高亮。
- 单宏展开面按 `.macrocall` source anchors 将调用范围映射到展开行并高亮；cjc UTF‑8 字节列转换为 IntelliJ UTF‑16 偏移。
- `.macrocall` 生成诊断将字节列映射为展开 UTF‑16 偏移，再经源锚点映回调用点；「查看宏展开」动作将编辑器定位并高亮在生成错误偏移处。
- 宏展开服务记录最近请求及结果分类；Tools 菜单复制诊断快照。宏模块单测覆盖报告字段、失败原因换行归一和有界顺序；产品集成测试触发已注册动作并读取剪贴板报告。
- LSP 递归展开为内层宏替换记录输出文本范围，展开面板用层级色块与宏名图例展示来源；递归能力元数据使服务不再把递归请求交给不支持该协议的 cjc-frontend。现有产品用例构建并打开带来源图例的面板，但尚未执行真实服务端嵌套替换。
- cjc 结果通过精确匹配 PSI 调用节点保存原始调用片段与范围；LSP 结果保留提取时的 PSI 调用文本。面板可打开只读 `.cj` 虚拟标签，并以 `DiffManager` 对比调用和生成文本。
- 宏展开面板提供保存 `.cj` golden 快照和加载旧快照与当前结果比较的入口；产品集成测试验证两个按钮在最终插件宿主中可见。
- cjc 宏执行 stdout 随展开结果缓存；展开面板成功时可查看，进程失败通知也可查看保留下来的 stdout。

**#29 `--debug-macro` 构建开关：通过。** 注册的动作仅对 `cjpm.toml` 的 `[package].compile-option` 可用；启用/关闭独立参数时使用 TOML PSI 替换原值，保留其它标志与注释。行为与官方 CJPM 调试配置约定一致。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；新增 MacroDebugBuildOptionTest 3 例通过

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；3 tests completed, 0 failures；包含实际触发已注册动作、切换参数并验证原有选项和 TOML 注释保留
```

**#30 `debugMacro` 运行配置与展开源码断点：通过。** 仓颉程序运行配置提供继承项目设置、对此配置启用或禁用三态；配置序列化后由 `CangJieBuildManager` 传递至构建配置，CJPM 执行器通过 TOML PSI 更新 `[package].compile-option`。`.macrocall` 注册为仓颉语言文件，既有 DAP / Proto 断点类型按该语言文件类型接受行断点。产品集成测试覆盖配置持久化、构建参数传递、修改/恢复 `compile-option` 时保留已有 flags 与行尾注释，以及 `.macrocall` 文件类型解析。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:domain:package-manager:test :modules:ide:run:compileKotlin :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；CJPM 宏选项单测 3 例通过，运行配置与宏模块编译通过

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；5 tests completed, 0 failures
```

**#31 调试位置双向跟随：通过。** 产品描述符装配了 `MacroDebugPositionSynchronizer` 项目服务；DAP 与 Proto 暂停路径都传入活动栈帧的 `XSourcePosition`。服务解析 `.macrocall` 块和源码锚点，分别监听源码与展开文件的 caret；展开行跳回原始源码锚点，源码移动则选择最近的展开锚点并将光标放在生成代码字符处。找不到唯一原始源码文件时不猜测映射目标。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:debugger-api:test :modules:ide:debugger-dap:compileKotlin :modules:ide:debugger-proto:compileKotlin :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；映射单测通过，DAP / Proto 编译通过，宏模块测试通过

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:debugger-api:test :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；6 tests completed, 0 failures；产品夹具验证暂停到展开位置、原始源码 caret 跳回最近展开行、展开 caret 再映回源码
```

**#27/#28 展开快照与宏实现前后对比：通过。** 面板的快照保存使用带调用身份信息的默认文件名并允许宏作者选定版本控制位置；快照对比可读取旧 `.cj` 文件并打开 IDE diff。产品集成用例确认这两个操作入口随宏展开面板装配。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroExpansionContextResolutionTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed, 0 failures；验证展开面板打开、宿主 PSI 符号解析及快照保存/对比入口
```

**历史基线（当时 #26 仅部分完成）。** cjc `--debug-macro` stdout 在成功展开时随结果保留并在缓存副本中延续；失败时从进程错误对象送入通知日志窗口。后续的真实 DAP 产品宿主验证补齐了宏源码断点、单步及宏 stdout/stderr 输出，见本文末尾的 #26 完成记录。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；MacroExpansionDiagnosticsTest 3 例全绿，新增断言验证缓存命中保留宏 stdout

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroExpansionContextResolutionTest" --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；4 tests completed, 0 failures
```

```
.\\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL in 16s
→ `MacroCallFileParserTest` 7、`MacroCompilerOutputParserTest` 5、`MacroExpansionDiagnosticMapperTest` 2、`MacroFailureClassifierTest` 9；23 例全绿
```

**本轮产品集成验证：通过。** 宏调用解析与 IDE 交互均在 `:product:idea-plugin` 中验证；4 个产品级用例覆盖组织限定的直接导入隔离、public wildcard/alias 重导出、Find Usages 搜索及补全自动导入。

```
.\\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOrganizationQualifiedReferenceTest" --console=plain
→ BUILD SUCCESSFUL in 1m 14s
→ 4 tests completed, 0 failures
```

**#4 宏输入校验验证：通过。** 主仓解析 / 词法测试覆盖合法与非法宏转义、quote 转义及普通代码隔离（15 例）；产品集成测试覆盖宏调用签名形态、合法 Tokens 转义高亮，以及非法属性转义、普通代码转义和声明式宏输入不使用 Tokens 转义高亮（2 例）。

```
.\gradlew-queue.bat :psi:test --tests "org.cangnova.cangjie.psi.MacroCallTokenSyntaxParsingTest" :code-insight:highlighting:test --tests "org.cangnova.cangjie.codeinsight.highlighting.CangJieHighlightingLexicalKeysTest" --console=plain
→ BUILD SUCCESSFUL；15 tests completed, 0 failures

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroCallSignatureAnnotatorTest" --console=plain
→ BUILD SUCCESSFUL；2 tests completed, 0 failures
```

**#5 宏上下文提示验证：通过。** 产品集成测试覆盖已解析宏声明中的 `std.ast.assertParentContext("Outer")` 与 `getChildMessages("Inner")` 字面量提示，并确认变量实参不产生静态推测提示。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroContextAnnotatorTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed, 0 failures
```

**#6 宏调用快速修复验证：通过。** 产品集成测试实际执行了缺失宏包导入和宏名拼写修复，并检查调用文本和 import 结果。编译动作测试按当前测试宿主的宏编译器可用性及产物状态检查门控：只有编译器可用且宏源文件需要重编译时才要求出现该动作；不可执行时确认动作不显示。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroCallQuickFixAnnotatorTest" --console=plain
→ BUILD SUCCESSFUL；3 tests completed, 0 failures
```

**#16 展开代码符号跳转与局部引用查找：通过。** 测试点击展开面板的「在编辑器中打开」，从打开标签取得 IntelliJ 管理的 PSI，验证它保留对应宏调用 `elementContext`；展开代码中的调用引用解析到项目源声明，`ReferencesSearch` 在该展开文件作用域返回此引用。快捷展开、gutter 行标和调用点诊断修复入口均传入宏调用 PSI，避免依赖本地磁盘反查导致临时 VFS 或未保存文件丢失上下文。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroExpansionContextResolutionTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed, 0 failures
```

**#23 同因诊断聚合：通过。** 两处 `@Build` 调用缓存相同 error 后，产品高亮只生成一条带“2 处调用”的标注，tooltip 包含聚合调用位置。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroDiagnosticAggregationTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed, 0 failures
```

**#24 宏定义识别与签名校验：通过。** 产品测试覆盖宏声明必须位于宏包、必须公开、参数数目为 1–2、位置参数类型必须为 `Tokens`、不允许命名参数、显式返回类型必须为 `Tokens`；同时确认省略返回类型的合法声明不误报。规则对照只读的官方编译器 `ParseMacro.cpp`。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroDeclarationSignatureAnnotatorTest" --console=plain
→ BUILD SUCCESSFUL；2 tests completed, 0 failures
```

**#25 分包约束校验：通过。** 官方 `MacroExpansion.cpp` 在同包检测到宏定义与宏调用时发出诊断并停止展开。产品集成测试覆盖宏包内同包调用、同名普通包身份下的同包调用、专属错误与引用导航，并确认宏从原定义包重导出后，在 facade 包中的调用仍能解析且不误报。宏包先编译由 #6 的按包编译 quick fix 与 #32 的展开前编译管线覆盖。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroPackageConstraintTest" --console=plain
→ BUILD SUCCESSFUL；3 tests completed, 0 failures
```

**#9 递归展开选项与 #36 `.macrocall` 可见性/落盘策略：通过。** `CangJieMacroConfigurable` 提供递归 / 单层开关，并将项目视图可见性（Registry）与编译器输出保留（项目设置）分开控制；cjc 展开器仅在保留选项关闭时删除输出目录/源目录中的临时 `.macrocall` 文件。产品集成测试验证保留设置持久化、可见性设置彼此独立，且展开默认选项跟随递归 Registry 值。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；2 tests completed, 0 failures（包括 #37 复制动作）
```

**此前 #9 嵌套宏逐层下钻：模型测试通过。** 预览图例中的嵌套宏按钮切入对应的生成片段，面包屑返回父层；切层同时重定位后代 contribution 区间和逐行 source anchors。单测覆盖两级下钻的文本、偏移、深度和源行锚点重映射。此阶段尚未以真实产品宿主链证明调用点提取到实际预览着色；该缺口由本节后续的 #9/#17 产品宿主集成测试关闭。cjc `.macrocall` 仍无等价的嵌套来源区间。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --tests "org.cangnova.cangjie.macro.ui.MacroExpansionDrilldownViewTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures
```

**#38 前端构造依赖装配（早期阶段记录）：** `cangjie-frontend-cfir-for-ide` 现包含 `:compiler:frontend`；IDE 宏模块依赖 LL-CFIR API 与 `macro-process`，并在 composite build 中把 `org.cangnova.cangjie:macro-process` 映射到本仓 `:macro:macro-process`。后续配置器编译成功，但这一早期构建本身不证明语义查询可用。

```
.\gradlew-queue.bat :prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module:compileKotlin --console=plain
→ BUILD SUCCESSFUL

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:compileKotlin --console=plain
→ BUILD SUCCESSFUL；包含 :macro:macro-process、:compiler:frontend 与 IDE 宏模块
```

**#38 LL session 宏语义配置器（早期记录）：版本门控编译和定向测试通过；当时自动生命周期尚未验证。** `LLCfirMacroSessionConfigurator` 已在宏模块描述符注册；后台任务执行真实 `FrontendMacroConstructionService`，成功结果校验 session、输入 construction identity、source PSI identity、文件数、PSI 修改计数、编译设置修改计数和 SDK ID 后原子发布。`LLCfirSourceSymbolProvider`、文件缓存、容器查找、包文件枚举和 lazy resolve 缓存读取已接入该快照。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:compileKotlin --console=plain
→ BUILD SUCCESSFUL；配置器与低阶快照消费者编译通过
```

当时的模块构建没有验证产品宿主中的配置器和语义查询；后续产品集成测试先分别验证了同 session Analysis API 查询与无宏输入生命周期。真实宏 SDK 工具链到产品语义快照的闭环当时仍待验证，因此这一阶段将 #38 标为部分完成。最新闭环测试和当前状态见本节末尾。

**#38 异步快照输入版本门控：定向测试通过。** 构造输入版本同时记录 PSI modification count、`CangJieCompilerSettingsTracker` modification count 和项目 SDK ID；后台宏产物准备完成后及原子发布前都重新检查，SDK 切换时仍使用捕获的 SDK 对象构造，过期结果不会发布。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --tests "org.cangnova.cangjie.macro.analysis.MacroSemanticSnapshotInputRevisionTest" --tests "org.cangnova.cangjie.macro.service.MacroExpansionProviderCapabilitySelectionTest" --tests "org.cangnova.cangjie.macro.ui.MacroExpansionDrilldownViewTest" --console=plain
→ BUILD SUCCESSFUL；9 tests completed，0 failures
```

配置器版本门控改动后复跑产品宿主集成用例：同一真实 LL session 的语义构造 / 发布、Analysis API 补全 / 解析 / 类型、StructureView 导航和 Find Usages 仍通过。此用例仍显式驱动构造与发布，不作为自动配置器生命周期的证明。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroSemanticSnapshotIntegrationTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures
```

**#38 LL 原始输入收集与 IDE 构造/发布接线：语义与无宏自动生命周期均已通过产品宿主验证。** `LLCfirFileBuilder` 把 expression、declaration、parameter 等 PSI raw builder surface 按 CFIR 文件和 source identity 写入 `CfirRawMacroSurfaceUsageStore`；模块输入携带 construction identity 与有序 source PSI identity。真实构造测试验证普通 Analysis API 和 IDE 消费者；生命周期测试通过产品中注册的配置器异步发布 identity snapshot，调用 LL session invalidation，再取得新 session 并验证再次发布。生命周期用例不涉及宏 artifact 编译或宏执行。

```
.\gradlew-queue.bat :cfir:providers:test --tests "org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionArchitectureTest" --console=plain
→ BUILD SUCCESSFUL；14 tests completed，0 failures / 0 errors

.\gradlew-queue.bat :analysis:low-level-api-cfir:compileKotlin --console=plain
→ BUILD SUCCESSFUL；包含 `LLCfirSession.buildPreMacroRawBuildResultForModule`
```

**#37 宏执行记录与复制入口：通过。** 宏模块测试验证诊断报告包含请求时间、耗时、引擎、缓存占用和分类失败原因，并验证请求历史有界且最新优先；产品集成测试实际触发 Tools 菜单动作并读取剪贴板，确认报告已复制。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；宏模块测试通过

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；2 tests completed，0 failures（含实际触发动作并读取剪贴板）
```

**#33 请求去重、缓存隔离、进度与取消：通过。** 新增并发同键共享执行、执行方取消后活动等待者重试两项单测；宏模块构建通过。产品宿主测试再次通过，确认宏模块仍在最终插件中装配。代码路径同时验证了 Document 修改缓存失效、选项/引擎缓存隔离、后台进度任务、cjc 进程取消和 LSP 超时终止逻辑。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --console=plain
→ BUILD SUCCESSFUL；宏模块测试通过，新增去重测试 2 例

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；2 tests completed，0 failures
```

**第二轮历史验证：宏模块通过（不是本轮新改动的验证）。**

```
cd intellij-ide
./gradlew -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test
→ BUILD SUCCESSFUL in 11m 53s
```

| 任务 | 结果 |
|---|---|
| `:modules:ide:macro:compileKotlin` | 通过（宏模块首次编译成功，B3/B4 解除） |
| `:modules:ide:macro:compileTestKotlin` | 通过 |
| `:modules:ide:macro:test` | **当时 19 例全绿**：`MacroFailureClassifierTest` 9、`MacroCompilerOutputParserTest` 5、`MacroCallFileParserTest` 5；0 失败 0 错误 |

新增单测（纯逻辑，不依赖测试宿主）：

- `MacroFailureClassifierTest` —— 分类判据逐条钉死，含优先级（动态库线索优先于「宏包未编译」）与「识别不到就归 UNKNOWN」。
- `MacroCompilerOutputParserTest` —— 官方 `diagReport` 输出样例的解析（位置 + hint + 多诊断 + 换行符）。
- `MacroCallFileParserTest` —— `.macrocall` 前缀剥离与锚点对齐，核心不变量「展开文本行数 == 锚点数」。

**第二轮历史产品级验证（装配与描述符）：跑到插件加载，未取到全绿；不是本轮新改动的验证。**

```
./gradlew -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test \
    --tests "org.cangnova.cangjie.ide.quickfix.CangJieQuickFixRegistrationTest"
→ :product:idea-plugin:composedJar 通过（各模块 jar 与描述符合入产物，含宏模块）
→ 插件在测试宿主里加载成功；用例首条断言（descriptor 驱动的扩展注册非空）通过
→ 该用例随后失败于**上游**：LLCfirDiagnosticReporter.toPsiDiagnostic 抛
  IllegalStateException: Unknown diagnostic type CjOffsetsOnlyDiagnosticWithParameters1
  （新诊断类型未在 reporter 的映射里登记，来自 cfir/checkers + analysis-api-cfir 的在改代码）
```

即：`B1/B2/B5` 的接线已被「插件能带着全部模块描述符加载并注册扩展」验证；该用例自身的失败点在上游诊断映射，与本轮改动无关。

**环境说明（复现构建时必读）**：intellij-ide 通过 `includeBuild` 与主仓**共用上游 build 目录**，本机并行会话（另一个 Codex 会话 / 主仓直连构建）会在同一批目录上反复重建，表现为
`Unable to delete directory 'D:\code\intellij\cangjie\cfir\cfir-tree\build\classes\kotlin\main'`、
`FileNotFoundException: …cfir-common\build\classes\…\CfirConditionalCompilationFailure.class`，
与被改模块无关。为此本轮补了一个**上游构建隔离** init script：

- 位置：`intellij-ide/build/tmp/isolate-upstream.init.gradle.kts`（未入库，随 `build/` 清理重建即可保留一份副本）
- 做法：把主仓（及其 `repo/gradle-*-conventions` 子构建）各工程的 `buildDir` 重定向到 `D:/code/intellij/.ide-verify-build/`（**主仓之外**，不污染 `git status`），宿主 `intellij-ide` 自身与 `intellij-ide/build-logic` 不重定向；
- 缓存不受影响：Gradle 本地构建缓存目录按 `rootDir` 计算，上游任务仍能 FROM-CACHE（实测 256 个任务里 78 个命中缓存）；
- 用法：在 intellij-ide 下加 `-I build/tmp/isolate-upstream.init.gradle.kts` 跑任意任务。加上它之前，同样的 `:modules:ide:macro:test` 连续 6 轮全部停在上游模块；加上之后一次通过。

**运行前提**：`#18/#19` 的诊断取自展开结果缓存，需要 registry `cangjie.macro.expansion.analysis.enabled` 为 true（默认 false，开启后文件变更会触发后台展开）才会产出标注。

**#39 结构视图快照合并：真实生成节点展示和导航通过。** `CjMacroGeneratedStructureViewFactory` 已注册为 CangJie 的优先 `lang.psiStructureViewFactory`，直接装饰 LSP4IJ 0.19.2 文档符号模型根节点及 PSI class 匹配节点；服务器模型为空时也从 PSI declaration container 恢复普通声明。产品宿主集成测试把真实 LL snapshot 中的 `generated` 函数显示为“由宏生成”，并验证导航打开来源文件且 caret 落在宏调用 PSI 上。

**#40 生成引用搜索：真实 LL 快照查询与目标映射通过。** `CangJieReferencesSearchExecutor` 调用统一 generated-reference 扩展点；LL-CFIR IDE provider 从已发布快照的 resolved CFIR references 取得符号目标，不依赖临时生成 PSI 二次解析。产品宿主集成测试确认生成体内 `target()` 出现在 Find Usages 结果中，结果元素映射到宏调用，且 `resolve()` / `isReferenceTo()` 仍指向真实源声明。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:compileKotlin --console=plain
→ BUILD SUCCESSFUL in 2m 16s；包含 StructureView 扩展、LL session 配置器和 generated-reference provider

.\gradlew-queue.bat :analysis:cj-references:compileKotlin --console=plain
→ BUILD SUCCESSFUL

.\gradlew-queue.bat :analysis:analysis-api-cfir:test --tests "org.cangnova.cangjie.analysis.api.cfir.test.AnalysisApiFindUsagesTest" --console=plain
→ BUILD SUCCESSFUL；5 tests completed，0 failures

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroGeneratedStructureViewTest" --console=plain
→ BUILD SUCCESSFUL in 1m 17s；1 test completed，0 failures；宿主确认选择宏工厂、建立文件根模型并显示普通 PSI class（测试环境无可用 CangJie LSP server）

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroSemanticSnapshotIntegrationTest" --console=plain
→ BUILD SUCCESSFUL；本次 2 tests completed，0 failures；一个用例验证普通 Analysis API 补全 / 解析 / 类型、生成 StructureView 节点导航和真实 Find Usages 映射，另一个验证已注册配置器异步发布及 session 失效重建
```

本次生命周期用例首先复现了配置器在后台访问 `CaSourceModule.psiRoots` 时缺少 IntelliJ read action 的失败；已将 source/classpath roots 移入初始 `ReadAction` 捕获，并让后台配置只使用捕获的不可变路径。随后产品宿主两条集成用例均通过。该记录写入时本机缺少 `cjc` / `LSPMacroServer`；本轮已在既有 `CANGJIE_HOME` 路径恢复官网 1.0.5 SDK，并增加真实 SDK 宏编译、LSPMacroServer 嵌套展开与 contribution 验证。产品宿主宏调用到语义快照及 DAP 断点链路仍未验证。

**此前 #9 cjc 递归选项参数修正：编译通过。** `CompilerMacroExpansionProvider` 不再把 IDE 的递归显示选项误传为 `--parallel-macro-expansion`；官方选项定义的是并行执行，不能生成嵌套来源区间。cjc `.macrocall` 尚无嵌套来源协议；递归和嵌套来源由 LSPMacroServer 能力实现。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:compileKotlin --console=plain
→ BUILD SUCCESSFUL in 2m 58s
```

**#9 递归引擎能力路由与 Drilldown：定向测试通过。** 宏引擎能力选择的 4 个用例验证：递归请求路由到首个可用能力引擎、保留已支持递归的配置引擎、非递归请求保留配置引擎，以及没有能力引擎时返回缺失结果；`MacroExpansionDrilldownViewTest` 验证嵌套区间、层级和锚点重定位。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --tests "org.cangnova.cangjie.macro.service.MacroExpansionProviderCapabilitySelectionTest" --tests "org.cangnova.cangjie.macro.ui.MacroExpansionDrilldownViewTest" --console=plain
→ BUILD SUCCESSFUL；5 tests completed，0 failures
```

**#9/#17 递归替换与来源区间：生产算法定向测试通过。** `LspMacroServerProvider` 通过 `NestedMacroExpansionResolver` 处理 PSI 提取与 LSP 请求；算法测试验证同级替换导致的后续偏移变化、子宏范围平移、`parentNames` 逐层传递、同轮嵌套调用只展开外层后再递归、最大递归深度、失败结果保留原调用，以及协议响应数量校验。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:test --tests "org.cangnova.cangjie.macro.server.NestedMacroExpansionResolverTest" --console=plain
→ BUILD SUCCESSFUL；5 tests completed，0 failures
```

**此前 #26 调试验证（当时日志仍未验证）。** `MacroFunctionDebugProvider`、隔离调试产物、独立 LSPMacroServer 和 DAP attach / `configurationDone` 顺序已进入宏模块与最终插件；全量宏模块测试通过，产品宿主测试验证动作及唯一 DAP session launcher 已注册。本机 Cangjie 1.0.5 SDK 曾用于验证宏包编译与 LSPMacroServer 执行；其 DAP server 因缺少 MinGW runtime DLL 无法运行。带齐 MinGW runtime DLL 的 1.0.0 SDK 用例验证了真实宏源码断点、`next` 单步、继续后的宏展开和 DAP `library-loaded` 输出事件；当时的测试未捕获宏体 `println`，因此暂留 #26。之后追加的进程 stdout/stderr 输出监听和下方真实日志用例关闭了该缺口。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:clean :modules:ide:macro:test --no-build-cache --console=plain
→ BUILD SUCCESSFUL；51 tests completed，0 failures

.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroOutputRetentionSettingsTest" --console=plain
→ BUILD SUCCESSFUL；7 tests completed，0 failures；包括调试动作注册及唯一 DAP launcher 扩展验证
```

**此前 #9/#17 真实 SDK 与 LSPMacroServer 嵌套链：协议集成验证。** 本机恢复的 Cangjie 1.0.5 SDK 用 `cjc -g -O0` 编译外层和内层宏，真实 LSPMacroServer 返回 `EVAL(2)` 状态及内层宏 token；官方编译器并行服务在完成请求后保留该状态，客户端据此把已完成结果识别为成功。`NestedMacroExpansionResolver` 对真实响应产出正确文本、宏名、层级与 contribution 范围。此前用例的调用提取器通过受限 regex 构造协议请求，未覆盖产品 PSI 提取和预览消费；这一产品宿主缺口由下面的新集成测试覆盖。

```
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :modules:ide:macro:clean :modules:ide:macro:test --no-build-cache --console=plain
→ BUILD SUCCESSFUL；54 tests completed，0 failures，0 errors，0 skipped；包含真实 SDK/LSPMacroServer 嵌套宏测试
```

**此前 #26 DAP 断点 / 单步 / 输出事件验证。** 测试以可用 `dap_server.exe` 和带齐 MinGW runtime DLL 的 Cangjie 1.0.0 SDK，编译 `-g -O0` 宏包，启动实际 LSPMacroServer，attach 到其 PID，在宏源码中设置行断点，命中后读取精确源码位置并单步至后一行；继续后确认宏展开成功，并接收到 DAP `library-loaded` 输出事件。最初用例尚未验证宏体日志；后续完整 stdout/stderr 证据见下。

```
`$env:CANGJIE_HOME` 指向 `~/.cangjie/sdks/cangjie-1.0.0` 后：
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroDapDebugIntegrationTest" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures，0 errors，0 skipped
```

**#26 宏函数日志、断点与单步：真实产品宿主验证通过。** 扩展后的用例在同一宏调用中执行 `println` 并显式清空 stdout 缓冲区、`print(..., flush: true)` 和 `eprint`，通过 `LspMacroServerClient.addProcessOutputListener` 收到 stdout 与 stderr 标记；该监听器由 `LspMacroServerProvider` 接到 DAP 调试控制台。测试同时验证 DAP 源码断点、`next` 到下一宏源码行、继续后展开成功及 DAP `library-loaded` 事件。官方 `print` API 默认不清空 stdout 缓冲区，因此集成用例显式 flush，以验证仍运行中的服务进程能及时转发日志。

```
$env:CANGJIE_HOME = Join-Path $env:USERPROFILE '.cangjie/sdks/cangjie-1.0.0'
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroDapDebugIntegrationTest.testMacroSourceBreakpointAndStepThroughLspServer" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures，0 errors，0 skipped
```

**#38 真实宏 artifact 到 Analysis API 语义快照：产品宿主集成测试通过。** 测试设置实际 Cangjie 1.0.5 SDK，在同一 IntelliJ module 中建立独立的宿主与 macro-package source roots；已注册的 `LLCfirMacroSessionConfigurator` 自动发现并通过 `cjc --compile-macro` 编译宏包，调用 LSPMacroServer 展开真实宏并发布 LL 语义快照。随后普通 Analysis API 的名称前缀补全包含 `generated`、引用解析到该生成声明、表达式类型得到 `Int64`。宏包与宿主源码放在同一 source root 会触发官方 `package_name_inconsistent_with_macro`；把宏包放进另一个 module 也不会进入当前模块的 pre-macro 源码发现，因此测试按产品发现流程使用两个独立 source roots。

```
$env:CANGJIE_HOME = Join-Path $env:USERPROFILE '.cangjie/sdks/cangjie-1.0.5'
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroSemanticSnapshotIntegrationTest.testRealMacroArtifactCompilationFeedsPublishedSemanticSnapshot" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures，0 errors，0 skipped
```

**#9/#17 真实 PSI 到预览编辑器：产品宿主集成测试通过。** 使用 Cangjie 1.0.5 SDK 编译嵌套宏 artifact；从产品模块真实 `CjMacroExpression` 调用 `MacroExpansionService`，分别验证单层结果保留内层调用和递归结果生成最终文本，再将递归结果放入 `MacroExpansionPanel`，验证两层图例、两段编辑器高亮范围及点击内层贡献后显示子宏文本。测试还发现 service 添加源码文档戳时使用数据类 `copy()` 丢弃 `sourceContributions`；结果元数据复制现保留这些非构造器字段，文档戳读取也已在 `ReadAction` 中保护。

```
$env:CANGJIE_HOME = Join-Path $env:USERPROFILE '.cangjie/sdks/cangjie-1.0.5'
.\gradlew-queue.bat -p intellij-ide -I build/tmp/isolate-upstream.init.gradle.kts :product:idea-plugin:test --tests "org.cangnova.cangjie.ide.references.CjMacroSemanticSnapshotIntegrationTest.testRealNestedMacroArtifactFeedsExpansionPreviewAndContributionDrilldown" --console=plain
→ BUILD SUCCESSFUL；1 test completed，0 failures，0 errors，0 skipped
```

## 11. 2026-10-15 上游输入过滤、编辑缓冲区与弹窗复验

**宏快照重复输入根因在上游 LL-CFIR 模块输入枚举，不在 `intellij-ide`。** 源模块声明索引会把 `.cjo` 二进制 PSI 同样报告为 `SOURCE`；pre-macro 收集器此前按包名直接把这些 PSI 当作实现源，导致同一宏产物再次进入宏分类/展开输入。修复位于上游 `common/CjSourceKind` 与 `analysis/low-level-api-cfir/LLCfirMacroConstructionInput`：真实实现源输入同时要求 `CjSourceKind.SOURCE` 和精确 `.cj` 后缀；二进制 `.cjo` 仍可供声明索引使用，但不会进入源宏语义快照。`CjSourceKindTest.sourceFileNamesRequireTheImplementationSuffix` 覆盖 `.cj` 接受及 `.cjo`、`.cj.d`、`.cj.macrocall`、大小写不匹配和空后缀拒绝。

上游构建与测试：

```
.\gradlew-queue.bat :common:test :analysis:low-level-api-cfir:compileKotlin --console=plain --max-workers=1
→ BUILD SUCCESSFUL；107 tasks

.\gradlew-queue.bat :common:test --console=plain --max-workers=1
→ BUILD SUCCESSFUL；CjSourceKindTest 5 tests，0 failures/errors/skips
```

**追加发现：宏快照发布后，唯一的宿主函数仍被报告为重载冲突。** `untitled89` 的 `ideTarget` 在物理 `.cj` 源码中仅定义一次，官方 `cjpm build` 成功；产品宿主诊断回归先稳定复现 `ideTarget` 和 `useGenerated` 的 `CONFLICTING_OVERLOADS`。原因是普通 PSI 查询仍取到发布前缓存的 pre-macro CFIR，而 package scope 已切换到 expanded 快照，冲突检查器将同一 PSI 声明看成两棵 CFIR 树中的不同声明。修复放在上游 `analysis/low-level-api-cfir/ModuleFileCacheImpl`：按 PSI identity 优先返回快照中的 expanded CFIR，并让缓存查询和文件构建查询使用同一语义视图。

验证：精确失败用例加上修复后通过；随后宏模块 61 项测试、产品宿主快照集成 4 项及布局/上下文 1 项均为 0 failures/errors/skips。该修复位于上游，不是 `intellij-ide` 侧过滤诊断。

**真实宏 artifact 更新与 IDE dirty-buffer 生命周期：产品宿主集成验证通过。** `CjMacroSemanticSnapshotIntegrationTest.testRealMacroArtifactCompilationFeedsPublishedSemanticSnapshot` 先以真实 SDK 编译宏包并确认生成名称补全、引用解析、类型推断；随后在产品模块中保存宏源码的生成名称变更，确认配置器重新编译 artifact、新快照包含 `generatedFromUpdatedArtifact` 且不再包含旧 `generated`。再只改宿主编辑器 Document 并提交到 PSI、保持文件未保存，确认旧 LL session 失效、新 session 从当前缓冲区发布快照，移除的生成声明不再出现在宏生成名称或完整 callable 集合中。这验证了真实 IntelliJ 文档缓冲区与宏产物变化；产品宿主测试负责驱动生命周期，生产源文件筛选修复仍位于上游。

**宏展开面板布局：产品宿主 Swing 测试通过。** `CjMacroExpansionContextResolutionTest` 检查面板实际 preferred width、弹窗高度上限、代码编辑器可用区域，以及 header 内 FlowLayout 信息行不超出面板宽度；同一测试还确认编辑器打开、快照保存、Diff 与贡献图例操作存在，并验证展开文件 PSI 继承调用上下文。

```
cd intellij-ide
.\gradlew.bat :product:idea-plugin:test `
  --tests "org.cangnova.cangjie.ide.references.CjMacroExpansionContextResolutionTest" `
  --tests "org.cangnova.cangjie.ide.references.CjMacroSemanticSnapshotIntegrationTest.testRealMacroArtifactCompilationFeedsPublishedSemanticSnapshot" `
  --console=plain --max-workers=1
→ BUILD SUCCESSFUL；2 tests，0 failures/errors/skips
```

**重新启动 `untitled89` 的开发 IDE：** `:product:idea-plugin:runIde` 成功打开 `C:/Users/lin17/IdeaProjects/untitled89`；CJPM 同步成功，宏管线完成，随后只发布一份 production snapshot：`sourceFiles=[src/macros/ide_smoke_macros.cj, src/main.cj]`、`expandedFiles=2`、`generatedSources=40`。不再包含 `src/.macro-build/**/*.cjo`；此前日志中这类文件导致 `expandedFiles=5`。启动后未发现新的仓颉宏快照构造/发布错误。IDE 日志仍有 JetBrains Kubernetes 插件 `No remote API found` 异常，属于另一插件，需与仓颉宏错误区分。

本轮未取得真实桌面截图；弹窗已由产品宿主 Swing 测试验证首选宽度、高度上限、编辑器可视区和信息行溢出。开发 IDE 已重新打开并保持运行，仍可由用户检查当前屏幕表现。
