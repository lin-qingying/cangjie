# LSP 能力覆盖测试矩阵

本文件是 `lsp` 模块测试的可审计清单。矩阵的代码事实来源是
`testFixtures/org/cangnova/cangjie/lsp/testkit/LspCapabilityMatrix.kt`：
契约测试逐行驱动它，`CangjieCapabilityMatrixContractTest#matrix covers every declared feature and protocol entry`
再把矩阵与 `CangjieLspFeatureSet`、`CangjieLspRequest` 反向比对。因此**漏写一行或写错能力名会让测试失败**，
不需要靠人工核对本文件。

运行入口：

```bash
.\gradlew-queue.bat :lsp:test
.\gradlew-queue.bat :lsp:test --tests "*CangjieCapabilityMatrixContractTest*"
.\gradlew-queue.bat :lsp:test --tests "*CangjieLspProcessTransportTest*"
```

## 1. 宣告能力矩阵

`status` 说明：`契约+语义` = 协议契约测试 + 真实 Analysis 断言；`仅契约` = 只有协议契约；
`已宣告缺陷` = 服务端宣告但实现未接入，按已宣告契约断言并把缺陷留在矩阵里。

| 能力 | 协议入口 | 正向（能力开启） | 关闭 / 缺失 | 真实语义断言 | status |
| --- | --- | --- | --- | --- | --- |
| completion | `textDocument/completion` | 契约候选 `contractCompletion` 抵达 + facade 记录 | 关闭/缺文档 → 空列表且不进 facade | `CangjieSemanticFeatureIntegrationTest`（候选含 `buildUser`/`greet`） | 契约+语义 |
| completion.resolve | `completionItem/resolve` | 文档被补齐 + facade 记录 | 关闭 → 原样返回候选 | `LspCompletionResolveTest` | 契约+语义 |
| hover | `textDocument/hover` | Markdown 内容回传 | 关闭/缺文档 → 空 contents | 语义用例断言含 `buildUser` | 契约+语义 |
| signatureHelp | `textDocument/signatureHelp` | 签名 + activeParameter | 关闭/缺文档 → 空签名表 | 语义用例断言真实签名 | 契约+语义 |
| declaration | `textDocument/declaration` | 位置列表 | 关闭/缺文档 → 空 | 跨文件跳转断言 | 契约+语义 |
| definition | `textDocument/definition` | 位置列表 | 关闭/缺文档 → 空 | 跨文件跳转断言 | 契约+语义 |
| typeDefinition | `textDocument/typeDefinition` | 位置列表 | 关闭/缺文档 → 空 | 类型定义跳转断言 | 契约+语义 |
| implementation | `textDocument/implementation` | 位置列表 | 关闭/缺文档 → 空 | 接口 → 实现断言 | 契约+语义 |
| references | `textDocument/references` | 位置列表 | 关闭/缺文档 → 空 | 跨文件引用断言 | 契约+语义 |
| documentHighlight | `textDocument/documentHighlight` | 高亮范围 + kind | 关闭/缺文档 → 空 | 非空断言 | 契约+语义 |
| documentSymbol | `textDocument/documentSymbol` | 层级符号 | 关闭/缺文档 → 空 | 符号名固定断言 | 契约+语义 |
| workspaceSymbol | `workspace/symbol` | 符号列表 | 关闭 → 空列表且不进 facade | 跨工作区符号断言 | 契约+语义 |
| codeAction | `textDocument/codeAction` | quick fix 标题 | 关闭/缺文档 → 空 | `CangjieCodeActionIntegrationTest` | 契约+语义 |
| codeAction.resolve | `codeAction/resolve` | 编辑被补齐 + facade 记录 | 关闭 → 原样返回动作 | 真实实现成员编辑断言 | 契约+语义 |
| formatting | `textDocument/formatting` | 整篇编辑 | 关闭/缺文档 → 空 | `CangjieFormattingIntegrationTest` | 契约+语义 |
| rangeFormatting | `textDocument/rangeFormatting` | **按契约返回覆盖范围的编辑**（当前返回空 → 用例红灯） | — | 无（未接入 Analysis） | 已宣告缺陷 |
| rename | `textDocument/rename` | WorkspaceEdit | 关闭/缺文档 → 空 changes | `CangjieRenameIntegrationTest` | 契约+语义 |
| prepareRename | `textDocument/prepareRename` | 可重命名范围 | 关闭/缺文档 → Range 分支 | 与 rename 同用例 | 契约+语义 |
| foldingRange | `textDocument/foldingRange` | 折叠区域 | 关闭/缺文档 → 空 | `CangjieFoldingIntegrationTest` | 契约+语义 |
| selectionRange | `textDocument/selectionRange` | 带 parent 的范围链 | 关闭/缺文档 → 空 | 语义用例断言 parent 存在 | 契约+语义 |
| semanticTokens | `textDocument/semanticTokens/full` | 编码数据 | 关闭/缺文档 → 空 data | `CangJieSemanticTokenEncodingTest` | 契约+语义 |
| semanticTokens.range | `textDocument/semanticTokens/range` | 编码数据 | 关闭/缺文档 → 空 data | 同上（子集性质） | 契约+语义 |
| inlayHints | `textDocument/inlayHint` | 提示列表 | 关闭/缺文档 → 空 | `CangjieInlayHintIntegrationTest`（11 例） | 契约+语义 |
| inlayHints.resolve | `inlayHint/resolve` | tooltip 补齐 + facade 记录 | 关闭 → 原样返回提示 | `CangjieInlayHintIntegrationTest` | 契约+语义 |
| diagnostics（pull） | `textDocument/diagnostic` | 诊断项 | 缺文档 → 空报告（provider 默认不广告） | `CangjieSemanticDiagnosticsIntegrationTest` | 契约+语义 |
| diagnostics（push） | `textDocument/publishDiagnostics` | 通知含诊断 | 负向由诊断集成用例覆盖（通知通路无 capability 字段） | `CangjieLanguageServerTest` | 契约+语义 |
| diagnostics（workspace） | `workspace/diagnostic` | 工作区报告 | 关闭 → 空报告 | 未打开磁盘文件纳入断言 | 契约+语义 |
| — | `workspace/executeCommand` | 未识别命令返回非空占位 | — | `LspCompletionCommandHandlerTest`（补全命令） | 契约+语义 |

生命周期与状态（不属于「能力」，由独立用例覆盖）：

| 入口 | 用例 |
| --- | --- |
`initialize` / `initialized` | `CangjieLanguageServerLifecycleTest`、`LspStartupIntegrationTest`、`CangjieCapabilityNegotiationIntegrationTest`
`didOpen` / `didChange` / `didSave` / `didClose` | `CangjieLanguageServerTest`、`CangjieWorkspaceOverlayIntegrationTest`、`CangjieProtocolContractIntegrationTest`
`workspace/didChangeConfiguration` / `didChangeWatchedFiles` / `didChangeWorkspaceFolders` | `CangjieProtocolContractIntegrationTest`、`CangjieSemanticDiagnosticsIntegrationTest`
`shutdown` / `exit` / `close` | `CangjieLanguageServerLifecycleTest`、`CangjieLspCancellationAndShutdownTest`
notebook 通知 | `CangjieProtocolContractIntegrationTest`（仅验证占位与会话稳定性）

## 2. 场景与边界类别

每个能力统一按四个阶段取断言（是否适用见矩阵 `stages`）：

1. **ENABLED**：provider 已声明 + 请求进入 facade + 正向结果；
2. **DISABLED**：provider 不声明 + 协议中性值 + 不进入 facade；
3. **MISSING_DOCUMENT**：文档级入口缺文档时中性返回且不进入 facade；
4. **FACADE_FAILURE**：facade 抛错必须以 JSON-RPC 错误结束，且会话随后仍可服务。

编辑类用例（formatting / rename / codeAction.resolve / completion 插入）一律把编辑应用到文本再看结果，
断言写在**最终文本**上（`LspEditApplicator`），不比较编辑条数。

固定种子随机测试（种子 `20261111`）：

| 用例 | 性质 |
| --- | --- |
`LspTextDocumentPropertyTest` | 位置↔偏移互逆、越界夹取、范围替换与独立模型一致、换行规范化、位置单调
`LspProjectConfigurationPropertyTest` | 任意 `initializationOptions` 解析不抛错且确定、路径去重、URI 原样保留、系统属性设置/清除
`LspCompletionEditPropertyTest` | 应用编辑保留前后文、插入以候选名开头、CRLF 行尾不变、非法区间被拒绝
`CangJieSemanticTokenEncodingTest` | 5 元组、非负增量、长度为正、type/modifier 落在 legend 内、不重叠、范围 ⊆ 全文

进程级用例（`CangjieLspProcessTransportTest`）：

| 场景 | 断言 |
| --- | --- |
| stdio 传输 + 非 ASCII 帧 | 含 CJK/emoji 的 didOpen 之后，后续请求仍能对齐应答 |
| `Content-Length` 语义 | 声明长度 == 帧体 UTF-8 字节数（不是字符数） |
| 连续消息 | 一次写入 initialize+shutdown，逐帧解析并各自应答（不约束响应先后） |
| stdout 纯净 | 全部字节可解析为协议帧，畸形帧/协议外文本即失败（日志走 stderr） |
| 退出流程 | shutdown 后 exit → 0；未 shutdown 直接 exit → 1；stdio 结束时读到 EOF |
| socket 传输 | 真实进程在 TCP 连接上完成初始化与退出 |

进程级夹具的既定前提（写进用例注释，避免后人误判为缺陷）：

- **响应可以乱序**：`initialize` 走串行语义执行器，`shutdown` 立即返回；JSON-RPC 不要求响应保序。
- **exit 不放进同一批帧**：`exit` 会立刻销毁环境，若 `initialize` 还在构造会得到
  `IncorrectOperationException`。真实客户端必然等 `initialize` 返回，因此这是客户端违约而非服务端缺陷。
- **socket 收尾会有 `Connection reset`**：`System.exit` 直接掐断连接，因此 socket 用例只校验帧结构，
  「干净关闭」断言只用于 stdio。
- **子进程 classpath**：测试运行 classpath 有 300+ 条目、50 KB 上下，直接拼命令行会触发 Windows
  `CreateProcess error=206`。构建脚本把它落盘到 `build/lsp-test-classpath.txt`，经系统属性
  `lsp.test.classpath.file` 传给测试，再用 JDK argument file（`java @args`）喂给子进程。

## 3. 宏展开

- 进程内宏引擎端到端：`LspMacroExpansionRealEngineTest`（预编译宏包 + 真实 `LSPMacroServer`）。
- 快照生命周期：`LspMacroExpansionLifecycleTest`、`LspMacroSemanticExpansionEnvironmentTest`。
- 失败与诊断：`LspMacroFailureDiagnosticsIntegrationTest`（不可用执行器、缺失 artifact、成功后清除）。
- 协议可见性：`LspMacroExpansionProtocolIntegrationTest`（宏生成声明可被 hover/definition 看见）。
- **SDK 缺失即失败**：`CangjieSdkFixture.requireSdkHome/requireMacroServer/requireCompiler` 直接让作业失败并给出
  环境变量名（`LSP_TEST_CANGJIE_HOME` 优先，其次 `CANGJIE_HOME`），不再用 `assumeTrue` 静默跳过。

## 4. 已知缺陷与缺口（显式记录，不隐藏）

| 项 | 现状 | 处置 |
| --- | --- | --- |
| 范围格式化 | 服务端宣告 `documentRangeFormattingProvider`，但服务层直接返回空编辑且不进 facade | 矩阵标记 `DEFECT_RECORDED`；`declared capabilities honour their contract` 按已宣告契约断言并保持红灯（D4）。**未修改生产实现**（按计划约束） |
| 成员补全 | `square.` 之后补全 NPE（`CodeStyleManager` 为 null） | `member completion after a dot lists the members` 红灯（D1） |
| prepareRename 哨兵 | 不可重命名返回 `(0,0)-(0,0)` 而非 `null` | `prepare rename only reports ranges that cover the request position` 红灯（D2） |
| 新增工作区目录 | 有 `multiModuleOption` 时新目录不进模块 | 成对用例：有配置→红灯、无配置→绿（D3，已定位到「未降级」） |
| 符号形状协商 | 忽略 `hierarchicalDocumentSymbolSupport`，始终返回 `DocumentSymbol` | 成对用例：无层级能力→红灯、有层级能力→绿（D9） |
| pull diagnostics provider | `descriptor.pullDiagnosticsEnabled` 默认 false：能力可请求，但 provider 有意不广告 | 矩阵用 `LspAdvertisement.FORBIDDEN` **断言它不广告**（不是跳过校验）；要广告需先完成端到端语义对齐 |
| notebook | 仅是占位服务 | 只断言通知不破坏会话，不作为已实现语义能力 |
| Kover | 版本目录已有 `kover = 0.9.1`，但没有任何模块应用插件；当前 Gradle 9.4 上未验证 kover 兼容性 | 本计划未改构建脚本。启用方式：在 `lsp/build.gradle.kts` 的 `plugins {}` 加 `id("org.jetbrains.kotlinx.kover") version "0.9.1"`，随后用 `.\gradlew-queue.bat :lsp:koverHtmlReport` 查漏 |
| 能力声明漂移 | 矩阵漂移用例只约束「能力字段 / 协议枚举」两侧 | 新增能力只要补一行矩阵即可自动纳入四阶段契约测试 |

## 4.0 测试数据约定（testData）

测试源码属于**数据**，统一放 `lsp/testData/`，与仓库既有约定一致（`psi/testData`、`code-insight/completion/testData`、
`analysis/*/testData` 都是「源码 + sidecar 期望」的布局）。

```
lsp/testData/realSemantics/
├── model.cj / use.cj          # 跨文件导航、引用、重命名、补全共用
├── cdoc.cj                    # completionItem/resolve 的 CDoc
├── structure.cj               # documentSymbol 层级
├── signature.cj / highlight.cj
├── formatting.cj / host.cj / broken.cj
├── incremental/               # main / watched / doomed / later / degraded / pull-*
└── robustness/                # crlf.cj（含 \r\n）、empty.cj（0 字节）、rename.cj、conflict.cj
```

加载入口（`lsp/testFixtures`）：

| API | 用途 |
| --- | --- |
| `LspWorkspaceFixtureBuilder.sourceFromTestData(target, path)` | 把 testData 源码放到临时工作区 `src/` 下 |
| `LspWorkspaceFixtureBuilder.fileFromTestData(target, path)` | 放到任意相对路径（`src/` 之外也行） |
| `LspTestData.text(path)` / `path(path)` / `copyTo(path, target)` | 原样读取 / 定位 / 运行期拷贝（watched 文件用例） |

约束与收益：

- **字节原样**：`readText` 保留 `\r\n`，空文件读到空串——CRLF 与空文档本身是被测输入，不能被夹具「顺手规范化」。
- **共用而非各写一份**：迁移前同一份 `model.cj` 在能力/边界两个用例里各写一遍（4 处近似重复），
  改一处忘另一处就会让结论分叉；现在只有一份。
- **可被编辑器检查**：`.cj` 文件有真实语法高亮/缩进检查，内联字符串没有。
- **迁移等价性**：改完后全量仍是 `201 tests / 8 failed / 7 skipped`，失败集合完全一致，说明迁移没有改变被测输入。

## 4.1 真实语义断言（与契约测试的分工）

契约测试用**假 facade**，只能证明「请求到了适配层、能力开关生效」；它**证明不了语义正确**。
`CangjieRealSemanticsCapabilityTest` 用生产 `AnalysisApiCangjieAnalysisFacade` + 临时仓颉工作区，
对每项能力断言具体内容/位置/应用后的文本：

| 用例 | 断言的是什么 |
| --- | --- |
| `navigation resolves to the real declaration sites` | definition/declaration 落到 `func twice` 的行；typeDefinition 落到 `class Square`；implementation 落在 Square 之后 |
| `editor assist content matches the symbol under the cursor` | hover 文本含 `twice` + `Int64`；签名标签来自真实声明 |
| `member completion after a dot lists the members` | `square.` 后必须列出 `area`/`side`（**当前 NPE，红灯**） |
| `symbols and highlights describe the real declarations` | 符号集合含 Shape/Square/twice；selectionRange ⊆ range；kind 合理；side 高亮 ≥2 且都在 Square 内 |
| `semantic tokens cover the right source text` | 按 token 覆盖的**源文本**断言类型（Square=class、twice=function、Shape=interface） |
| `member function declarations are coloured as methods` | 成员方法声明应为 `method`（**当前是 function，红灯**） |
| `formatting produces applicable text` | 应用格式化编辑后的文本形态 |
| `rename edits every declaration and use when applied` | 应用 WorkspaceEdit 后声明与跨文件调用都改名、旧名消失 |
| `folding and selection ranges have real structure` | 折叠覆盖类体；选择范围链逐级包含 |
| `inlay hints describe inferred types at real positions` | `let square = Square()` 上有含 `Square` 的类型提示 |
| `diagnostics point at the real error line in push and pull channels` | push 与 pull 都指向真实错误行 |
| `code action edits implement the missing member when applied` | resolve 的编辑应用后真的出现 `override func greet` |

## 4.2 边界与二次请求（`CangjieRealSemanticsBoundaryTest`）

一次请求的结果正确，不代表延迟补全、不可重命名、目录变更、诊断刷新这些路径正确。这里逐条覆盖：

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `completion resolve fills documentation from cdoc` | resolve 后 documentation 必须含 CDoc 文本（覆盖「候选 → 会话 → 重进分析 → 渲染」整链） | 通过 |
| `inlay hint resolve keeps the hint usable` | resolve 不得移动位置、label 仍非空 | 通过 |
| `prepare rename only reports ranges that cover the request position` | 可重命名处给出覆盖请求位置的 range；不可重命名处必须为空 | **失败**：关键字位置返回 `(0,0)-(0,0)` 哨兵 |
| `rename on a non renamable position produces no edits` | 关键字位置不得产生编辑 | 通过 |
| `workspace folder added later becomes searchable` | 初始化给了 `multiModuleOption` 时，后加目录的符号必须可检索 | **失败**：查不到 |
| `workspace folder added later degrades to a module when no multi module option was given` | 无 `multiModuleOption` 时按「目录即模块」降级后可用 | 通过 |
| `diagnostics refresh incrementally on the same document` | 修好 → push 变空；再引入错误 → push 指向新行且旧行消失 | 通过 |

后两条 workspace folder 用例是**成对**的：一条红一条绿，说明缺陷被定位到
「存在 `multiModuleOption` 时新增目录未降级为模块」，而不是「新增目录完全不支持」。

## 4.3 增量可见性（`CangjieRealSemanticsIncrementalTest`）

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `watched file creation becomes visible to workspace symbols` | 磁盘新建 `.cj` + `didChangeWatchedFiles(Created)` 后，新类型可被 `workspace/symbol` 检索 | 通过 |
| `watched file deletion removes symbols from workspace` | 删除前可检索、发 `Deleted` 后必须消失 | 通过 |
| `selection range answers one chain per requested position` | 3 个位置 → 3 条链，按序，每条覆盖自己的位置且逐级包含 | 通过 |
| `pull diagnostics follow the current document state` | 打开有错 → pull 报错；修好后 pull 立即不报错（不是缓存） | 通过 |

**这两条 watched-file 用例只证明「通知处理的端态正确」**，不证明「通知是可见性的唯一原因」——
若实现本来就是磁盘直读，它们同样会绿。要证明因果需要「不发通知」的基线，而那不是稳定契约，
因此这里不假装测到了因果。

## 4.4 协议形状与范围收敛（`CangjieRealSemanticsProtocolDetailTest`）

请求「有结果」不等于「结果对」。这一组断言的是**形状**和**范围**，因为客户端拿到错形状时
通常不报错，而是静默丢弃：

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `document symbols degrade to flat symbols when the client has no hierarchy support` | 未声明 `hierarchicalDocumentSymbolSupport` 时必须返回 `SymbolInformation` | **失败**（D9：始终返回 `DocumentSymbol`，老客户端大纲空白） |
| `document symbols keep their hierarchy when the client supports it` | 声明了层级时 `Square.area` 必须挂在 `children` 下 | 通过（对照） |
| `references honour the include declaration flag` | `includeDeclaration=false` 必须去掉声明位置，但仍保留调用点 | 通过 |
| `semantic tokens range matches the full result inside the range` | 范围请求的 token 必须全部落在范围内，且与全量结果在该范围内**逐个一致** | 通过 |
| `code actions stay scoped to the requested range` | 类声明处有修复；接口声明处（与诊断不重叠）必须为空 | 通过 |
| `inlay hints stay inside the requested range` | 目标行有类型提示；包声明行必须没有提示 | 通过 |

## 4.5 编辑与生命周期（`CangjieRealSemanticsEditingTest`）

连续操作才暴露的位置：

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `incremental edits after a non BMP line keep coordinates correct` | 文件前几行有 emoji（4 字节 / 2 码元），其后做增量编辑：改完必须仍可解析（pull 无 Error）且仍能跳到 `func twice` | 通过（按字节解释 `Range` 会直接写坏语法） |
| `formatting is idempotent and keeps the document parseable` | 第一次有编辑；应用后再格式化必须为空编辑；格式化后仍能解析出 `f` | 通过 |
| `closing a document clears its diagnostics and keeps serving requests` | `didClose` 后必须发布空诊断；随后的 hover/definition 不得变成协议错误 | 通过 |

## 4.6 多模块工作区（`CangjieRealSemanticsMultiModuleTest`）

`app` 依赖 `lib`，两者各有独立源码根（`testData/multiModule/`）。单模块测试里所有文件同属一个
source set，符号天然可见；真实工程的跨模块可见性要项目结构把两个模块都装进去。

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `dependency module symbols resolve without diagnostics` | `app` 里调用 `helper()` 不得产生任何错误诊断 | 通过 |
| `definition crosses the module boundary` | 从 `app` 的 `helper()` 跳到 `lib` 的 `public func helper` | 通过 |
| `references cross the module boundary` | 在 `lib` 的声明上查引用，能找到 `app` 的调用行 | 通过 |

反向引用这条是关键：只测正向跳转时，「模块内符号表各自独立、只有 import 解析走桥接」的实现也能过；
反向查找必须真的遍历依赖方模块。

## 4.7 客户端配置（`CangjieRealSemanticsClientSettingsTest`）

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `formatting honours the client tab size` | 同一会话用 `tabSize=2` 与 `4` 格式化，结果必须不同，且函数体缩进分别为 2 / 4 空格 | 通过（**两条对照**，避免「忽略字段、永远 4 空格」也过） |
| `formatting honours tab indentation when the client asks for it` | `insertSpaces=false` 时函数体必须以制表符缩进 | 通过 |
| `published diagnostics carry the document version and follow changes` | 客户端声明 `versionSupport` 时诊断带版本：首版为 1，改好后重发的空诊断为 2 | 通过 |

## 4.8 接受候选整链（`CangjieRealSemanticsCompletionApplyTest`）

`completion → executeCommand(applyCompletion) → applyEdit`，编辑器里最常见的一次交互，分层最多的一条链：

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `accepting a candidate applies an edit that inserts it` | 候选必须带 `applyCompletion` 命令；执行后发出恰好一次 `applyEdit`，应用编辑后文本出现 `return twice` 且残留的 `twi` 被替换 | 通过 |
| `applying with an unknown session key reports a protocol error` | 会话键失效必须返回 JSON-RPC 错误，且**不得**发出 `applyEdit`（旧区间可能已经不对） | 通过 |
| `applying after the document changed reports a protocol error` | 文档版本变化后旧候选不得生效 | 通过 |

## 4.9 展示层保真度（`CangjieRealSemanticsPresentationTest`）

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `document symbols use the right kinds for types` | `interface`→`Interface`、`class`→`Class` | 通过 |
| `top level functions use the function kind` | 顶层函数→`Function` | 通过 |
| `document symbols map class members to the field kind` | 成员变量→`Field` | **失败**（实际 `Variable`，D11） |
| `document symbols map member functions to the method kind` | 成员方法→`Method` | **失败**（实际 `Function`，D11，与 D5 同源） |
| `hover inside comments and strings returns nothing` | 注释 / 字符串里的同名文本不得返回 hover | 通过 |
| `code actions carry the diagnostic that triggered them` | 动作的 `kind` 为 `quickfix`，且带 `diagnostics` 并与请求范围重叠 | 通过 |

## 4.10 多根工作区（`CangjieRealSemanticsWorkspaceRootsTest`）

两个 `workspace folder`、客户端**不**给 `multiModuleOption`（VS Code 多根工作区的标准形态）。

| 用例 | 断言 | 当前 |
| --- | --- | --- |
| `each root resolves its own declarations` | 每个根目录内的代码无错误诊断 | 通过 |
| `cross root import without a declared dependency is reported as unresolved` | 未声明依赖的跨根 `import` 必须报 `unresolved import`；`definition` 不得跨根跳转 | 通过 |

第二条是**反向断言**：多根 ≠ 共享符号空间。Cangjie 的跨模块引用需要显式依赖，
客户端只给目录时服务端没有依据建边；此时报错是正确行为，静默解析反而会让用户在构建时才失败。
（要验证「声明了依赖就能跨根」见 4.6 的多模块用例。）

## 5. 预期红灯（重要）

> 缺陷明细（复现、期望/实际、证据栈、疑似位置、修复方向）见 [`LSP-DEFECTS.md`](LSP-DEFECTS.md)。
>
> 全量口径：**229 用例 / 12 失败 / 7 跳过**（跳过均为宏用例的 `@EnabledOnOs(LINUX)`）。
> 12 个失败与 [`LSP-DEFECTS.md`](LSP-DEFECTS.md) 的 D1–D11 一一对应（D11 占两条），没有额外红灯：

| 失败用例 | 缺陷 | 影响 |
| --- | --- | --- |
| `CangjieRealSemanticsCapabilityTest > member completion after a dot lists the members` | 限定名成员补全 NPE：`CodeStyleManager` 在 LSP 容器里为 null（`CompletionDummyIdentifierProvider.replaceExpressionViaPsi:194`） | **功能性**：`square.` 后补全返回 `Internal error` |
| `CangjieRealSemanticsBoundaryTest > prepare rename only reports ranges that cover the request position` | 不可重命名位置返回 `(0,0)-(0,0)` 哨兵而非 `null`（`CangjieTextDocumentService.emptyPrepareRenameResult`） | **协议**：客户端会在文档开头弹出重命名框 |
| `CangjieRealSemanticsBoundaryTest > workspace folder added later becomes searchable` | 初始化带 `multiModuleOption` 时，`didChangeWorkspaceFolders` 新增的目录不进任何模块（`LspProjectConfiguration.parseWorkspaceModules` 只认 `multiModuleOption`） | **功能性**：新增目录的符号不可检索、不可跳转 |
| `CangjieCapabilityMatrixContractTest > declared capabilities honour their contract` | 范围格式化已宣告 provider 却返回空编辑 | **协议**：宣告了做不到的能力 |
| `CangjieRealSemanticsCapabilityTest > member function declarations are coloured as methods` | 成员方法声明被着色为 `function` | 保真度（不影响功能） |
| `CangjieRealSemanticsEditorDetailTest > document highlight distinguishes reads and writes` | 赋值目标高亮为空、声明被标 `Write`、`Read` 从未使用 | 保真度/功能：读写无法用颜色区分，赋值处点高亮无反应 |
| `CangjieRealSemanticsRobustnessTest > empty document answers every position based request` | 空文件补全抛 `IllegalStateException`（import bindings 缺失） | 健壮性：新建空文件里打不出补全 |
| `CangjieRealSemanticsRobustnessTest > rename to an existing symbol name never duplicates a declaration` | 改名到已存在名字时静默生成重复声明 | **数据损坏**：应用编辑即写坏用户文件 |
| `CangjieRealSemanticsProtocolDetailTest > document symbols degrade to flat symbols when the client has no hierarchy support` | 忽略 `hierarchicalDocumentSymbolSupport`，始终返回 `DocumentSymbol` | **协议**：不支持层级的客户端大纲视图静默空白 |
| `CangjieRealSemanticsRobustnessTest > syntax errors surface as diagnostics` | 缺右括号的文件不产生任何诊断 | **一致性**：编辑器全绿、`cjc` 编译失败 |
| `CangjieRealSemanticsPresentationTest > document symbols map class members to the field kind` | 成员变量映射为 `Variable` 而非 `Field` | 保真度：按 kind 过滤「只看字段」筛不出成员（D11） |
| `CangjieRealSemanticsPresentationTest > document symbols map member functions to the method kind` | 成员方法映射为 `Function` 而非 `Method` | 保真度：与 D5 同源（D11） |

其中第一条的判定语义：

- 计划明确要求「范围格式化已宣告 → 按已宣告契约验证；若失败，保留失败并记录缺陷，不改生产代码」；
- 因此这里不写 `assertTrue(edits.isEmpty())` 这类顺着实现的断言——那会把缺陷洗成绿灯；
- 判定方法：失败消息必须是上面这条契约语义（「必须返回编辑」）。若消息变成别的行 id 或别的语义，那就是**新缺陷**，按新缺陷处理。

矩阵里 `DEFECT_RECORDED` 的行不会混进常规四阶段循环（避免一条缺陷挡住其余 27 行的断言），
而是由 `declared capabilities honour their contract` 逐行断言；该集合一旦为空，这个用例会要求你删掉它，
避免「缺陷修好了测试还挂着红灯」。

其余红线原则（写测试时容易犯的错，这里显式禁止）：

| 反模式 | 本文档的做法 |
| --- | --- |
| 把当前实现行为写成期望（`assertTrue(result.isEmpty())`） | 只断言已宣告契约或规范不变量 |
| 用 `advertisementChecked=false` 跳过断言 | 用 `REQUIRED`/`FORBIDDEN` 表达「必须/必须不」，两者都可失败 |
| 中性结果把 `null` 当空结果 | `null` 直接失败：客户端无法区分「无结果」与「字段缺失」 |
| 越界坐标固化「回落到最后一行」 | 只断言不抛错、落在文档内、夹取幂等（越界行为 LSP 未定义） |
| 用 `toString().contains(...)` 断言结构化响应 | 断言字段值（如 hover 的 `MarkupKind` + 文本、inlay 的 `label.left`、prepareRename 的精确 `Range`） |
| 只用假 facade 就宣称「能力已覆盖」 | 语义断言必须走生产 facade（`CangjieRealSemanticsCapabilityTest`）；契约测试只负责路由 |
| 只测标识符前缀补全 | 成员补全（`.` 之后）单独覆盖——它是 PSI 占位符替换路径，前缀补全走不到 |

## 6. 覆盖声明

- 覆盖 `CangjieLspFeatureSet` 全部 19 项能力，以及 3 个 resolve 入口、push/pull 诊断、`workspace/executeCommand`。
- 真实语义断言覆盖：补全（含「接受候选」整链）、hover、签名帮助、declaration/definition/typeDefinition/implementation、
  引用（含 `includeDeclaration`、跨模块）、符号（含层级协商、语法残缺）、codeAction（含范围收敛）、
  格式化（含 `tabSize`/`insertSpaces`/幂等）、重命名（含冲突与非引用文本）、折叠、选择范围、语义 token（含 range）、
  内联提示（含范围收敛）、诊断（push/pull/版本/关闭清理/增量刷新）、多模块工作区、宏展开（Linux-only）。
- 协议形状与协商：`hierarchicalDocumentSymbolSupport`、`includeDeclaration`、`semanticTokens.range`、
  codeAction/inlayHint 的范围收敛、`publishDiagnostics.version`、`workspace/applyEdit` 的客户端声明。
- 不声称穷举任意输入的全部组合：随机测试是「固定种子 + 系统化边界类别」，用途是可复现的回归，
  不是形式化证明。
