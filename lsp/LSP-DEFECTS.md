# LSP 缺陷单（由测试发现，均未修改生产代码）

来源：`lsp` 模块真实语义测试（生产 `AnalysisApiCangjieAnalysisFacade` + 临时仓颉工作区）与协议契约测试。

全量口径：`.\gradlew-queue.bat :lsp:test` → **192 用例 / 5 失败 / 7 跳过**（跳过为宏用例 `@EnabledOnOs(LINUX)`）。

| # | 缺陷 | 级别 | 失败用例 |
| --- | --- | --- | --- |
| D1 | 限定名成员补全抛 NPE，客户端收到 `Internal error` | P1 功能性 | `CangjieRealSemanticsCapabilityTest > member completion after a dot lists the members` |
| D2 | 不可重命名位置返回 `(0,0)-(0,0)` 哨兵而非 `null` | P2 协议 | `CangjieRealSemanticsBoundaryTest > prepare rename only reports ranges that cover the request position` |
| D3 | 初始化带 `multiModuleOption` 时，新增 workspace folder 不进模块 | P1 功能性 | `CangjieRealSemanticsBoundaryTest > workspace folder added later becomes searchable` |
| D4 | 范围格式化已宣告 provider 却返回空编辑 | P2 协议 | `CangjieCapabilityMatrixContractTest > declared capabilities honour their contract` |
| D5 | 成员方法声明着色为 `function` 而非 `method` | P3 保真度 | `CangjieRealSemanticsCapabilityTest > member function declarations are coloured as methods` |
| D7 | 空文件（0 字节）补全抛 `IllegalStateException`，客户端收到 `Internal error` | P2 健壮性 | `CangjieRealSemanticsRobustnessTest > empty document answers every position based request` |
| D8 | 重命名到已存在的名字时静默生成重复声明，直接损坏文件 | P1 数据损坏 | `CangjieRealSemanticsRobustnessTest > rename to an existing symbol name never duplicates a declaration` |
| D11 | 符号种类不区分成员与顶层：字段→`Variable`、成员方法→`Function` | P3 保真度 | `CangjieRealSemanticsPresentationTest > document symbols map class members / member functions ...` |
| D10 | 语法错误（缺右括号）不产生任何诊断：编辑器全绿、`cjc` 编译失败 | P2 功能/一致性 | `CangjieRealSemanticsRobustnessTest > syntax errors surface as diagnostics` |
| D9 | 忽略 `hierarchicalDocumentSymbolSupport`：客户端不支持层级时仍返回 `DocumentSymbol` | P2 协议 | `CangjieRealSemanticsProtocolDetailTest > document symbols degrade to flat symbols when the client has no hierarchy support` |
| D6 | `documentHighlight` 读写种类错位：赋值目标返回空、声明被标 `Write`、`Read` 从未使用 | P2 保真度/功能 | `CangjieRealSemanticsEditorDetailTest > document highlight distinguishes reads and writes` |

修完对应缺陷后，**对应用例应当自动变绿**；不要通过放宽断言来消红（判定规则见 `LSP-TEST-MATRIX.md` 第 5 节）。

---

## D1 限定名成员补全 NPE（P1）

**现象**：在 `x.` 之后请求补全，服务端抛异常，客户端收到 JSON-RPC `-32603 Internal error`，候选为空。

**最小复现**（工作区源码）：

```cangjie
package sample.real

class Square {
    var side: Int64 = 2
    func area(): Int64 { return side * side }
}

func total(): Int64 {
    let square = Square()
    return square.
}
```

请求：`textDocument/completion`，位置为 `square.` 之后（行 10、列 17 附近）。

**期望**：返回包含 `area` / `side` 的候选列表。
**实际**：`ResponseErrorException: Internal error.`

**证据**：

```
严重: LSP request textDocument/completion failed
java.lang.reflect.InvocationTargetException
  at AnalysisApiCangjieAnalysisFacade.pipelineCompletion(AnalysisApiCangjieAnalysisFacade.kt:281)
  at AnalysisApiCangjieAnalysisFacade.completion(AnalysisApiCangjieAnalysisFacade.kt:246)
Caused by: java.lang.NullPointerException: Cannot invoke
  "com.intellij.psi.codeStyle.CodeStyleManager.reformat(com.intellij.psi.PsiElement, boolean)"
  because "codeStyleManager" is null
  at CompletionDummyIdentifierProvider.replaceExpressionViaPsi(CompletionDummyIdentifierProvider.kt:194)
  at CompletionDummyIdentifierProvider.replaceWithPlaceholder(CompletionDummyIdentifierProvider.kt:139)
  at CompletionDummyIdentifierProvider.provideAt(CompletionDummyIdentifierProvider.kt:105)
  at CompletionDummyIdentifierProvider.provideFor(CompletionDummyIdentifierProvider.kt:93)
```

**疑似原因**：`codeStyleManager` 来自服务查找，在 headless LSP 容器未被注册；该路径只在「需要 PSI 占位符替换 + 重排」时走到（限定名成员补全），标识符前缀补全不会走到——所以既有补全用例一直绿。

**修复方向（供实现方参考，未实施）**：在 LSP 容器里注册 `CodeStyleManager`，或在该路径上对 null 做降级（跳过 reformat 而不是抛错）。

---

## D2 prepareRename 用 `(0,0)` 哨兵表示「不可重命名」（P2）

**现象**：在关键字（如 `class`）上请求 `textDocument/prepareRename`，服务端返回一个位于文档开头的零宽范围，客户端因此会在错误位置弹出重命名框。

**复现**：打开任意含 `class Square` 的文件，`prepareRename` 位置取 `class` 关键字（如 line 6, character 1）。

**期望**：`null`（LSP：该位置不可重命名）。
**实际**：`Either3.first = Range((0,0),(0,0))`。

**证据**：

```
不可重命名位置必须返回空结果；当前返回了与请求无关的范围 Range[start=(0,0), end=(0,0)]
（请求位置 Position(line=6, character=1)）
```

**疑似位置**：`CangjieTextDocumentService.emptyPrepareRenameResult()` 把「facade 返回 null」映射成 `forFirst(Range(0,0)-(0,0))`。协议允许整个结果为 `null`，无需哨兵。

**修复方向**：facade 返回 null 时直接返回 `null`；同时确认客户端侧（IDE 插件）能处理 null 响应。

---

## D3 新增 workspace folder 不进入模块（P1）

**现象**：`initialize` 时带了 `initializationOptions.multiModuleOption`（真实客户端常态），之后通过 `workspace/didChangeWorkspaceFolders` 新增一个目录，该目录内的源码完全不可见：`workspace/symbol` 查不到、跳转也进不去。

**复现**：

1. 目录 A 含 `Square`/`twice`，`initializeParams` 的 `multiModuleOption` 只声明 A；
2. `didChangeWorkspaceFolders` 加入目录 B（含 `LaterType`）；
3. `workspace/symbol { query: "LaterType" }` → `[]`。

**对照**：同样的新增流程，但 `initialize` 时**不带** `multiModuleOption`，`DegradedType` 可被检索到（该对照用例为绿）。

**结论**：缺陷不在「目录变更通知」本身，而在配置模型——`LspProjectConfiguration.parseWorkspaceModules` 只要 `multiModuleOption` 非空，就只按它建模块；未在其中声明的 folder 没有被降级成「目录即模块」。

**修复方向**：`multiModuleOption` 未覆盖的 workspace folder，按无配置分支的规则补一个模块定义。

---

## D4 范围格式化：宣告了 provider 但返回空编辑（P2）

**现象**：`initialize` 结果里 `documentRangeFormattingProvider` 已声明，但 `textDocument/rangeFormatting` 恒返回 `[]`，且不进入 Analysis facade。

**期望**：返回覆盖请求范围的 `TextEdit`（编辑应用后文本应被格式化）。
**实际**：空列表。

**疑似位置**：`CangjieTextDocumentService.rangeFormatting` 直接 `completed(emptyList())`。

**两选一**：
1. 接入 Analysis facade（与 `formatting` 同一条共享格式化器，按范围裁剪）；
2. 或在 `CangjieServerCapabilitiesFactory` 里不再宣告 `documentRangeFormattingProvider`。

优先 1；若短期不做，至少不要宣告（宣告做不到的能力比不宣告更伤客户端）。

---

## D5 成员方法声明着色为 `function`（P3）

**现象**：语义 token 里，`class Square { override func area() }` 的 `area` 声明 token 类型是 `function`，而 legend 里同时提供了 `method`。

**期望**：成员方法声明使用 `method`（LSP 对 `method` 的定义就是「方法声明或调用」）。
**实际**：`function`。

**疑似位置**：`CangJieSemanticTokenCollector.collectStructuralTokens` 的 `visitNamedFunction` 一律产出 `FUNCTION`；`METHOD` 只用于「父节点是限定表达式」的调用点。

**修复方向**：`visitNamedFunction` 时判断所属容器（类/接口/结构体）内则产 `METHOD`，并保留 `DECLARATION|DEFINITION` 修饰符。

---

## D6 documentHighlight 的读写种类错位（P2）

**现象**：字段 `value` 有三处出现（声明、赋值、读取），高亮结果如下：

| 请求位置 | 实际返回 |
| --- | --- |
| 声明（`var value`，line 3 col 8） | `[Text@10:15, Write@3:8]` |
| 赋值目标（`value = 1`，line 6） | `[]`（空） |
| 读取（`return value`，line 10 col 15） | `[Text@10:15, Write@3:8]` |

**问题有三处**：

1. **赋值目标解析不出来**：在 `value = 1` 上请求高亮返回空列表，客户端点「高亮引用」没有任何反应；
2. **声明被标成 `Write`**：`Write@3:8` 指向声明本身，语义上应是 `Text`（或带 `Declaration` 语义，LSP 无此 kind）；
3. **`Read` 从未使用**：读取位置只拿到 `Text`，编辑器无法用不同颜色区分读写。

**期望**：赋值位置返回 `Write`（覆盖该标识符），读取位置返回 `Read`，声明按客户端惯例给 `Text`/`Write` 之一且不缺失。

**复现**：

```cangjie
package sample.kind

class Counter {
    var value: Int64 = 0

    func bump(): Unit {
        value = 1
    }

    func read(): Int64 {
        return value
    }
}
```

请求：`textDocument/documentHighlight`，分别取 line 3 col 8、line 6 col 8（`value`）、line 10 col 15。

**证据**（失败断言里直接打印三处结果）：

```
assertion: 读取位置必须给出 Read 高亮
actual: decl=[Text@10:15, Write@3:8] write=[] read=[Text@10:15, Write@3:8]
```

**疑似位置**：`AnalysisApiLspSemanticSupport.documentHighlight` 的读写判定——写目标（赋值左值）未做引用解析，`DocumentHighlightKind.Read/Write` 的映射看起来接在了声明与读引用上且方向相反。

---

## D7 空文件补全抛异常（P2）

**现象**：打开一个 0 字节的 `.cj` 文件，在 (0,0) 请求 `textDocument/completion`，服务端抛异常，客户端收到 `-32603 Internal error`。

**期望**：返回空候选（空文件没有任何可补全上下文），不是协议错误。

**证据**：

```
严重: LSP request textDocument/completion failed
java.lang.IllegalStateException: File import bindings are missing after IMPORTS phase: ...\src\empty.cj
  at CaCfirImportPlanningKt.resolveShortNameBinding(CaCfirImportPlanning.kt:263)
  at CaCfirImportPlanningKt.checkCompletionCandidate(CaCfirImportPlanning.kt:175)
  at CaCfirCompletionCandidateChecker.checkCompletionCandidate(CaCfirCompletionCandidateChecker.kt:24)
  at AnalysisApiCangjieAnalysisFacade.pipelineCompletion(AnalysisApiCangjieAnalysisFacade.kt:335)
```

**疑似原因**：空文件没有 `package` 声明，IMPORTS 阶段不产出 import bindings，而候选检查直接 `checkNotNull` 绑定表。

**修复方向**：候选检查对「无 import bindings」的文件降级为「不提供候选」，而不是抛 `IllegalStateException`。

---

## D8 重命名冲突静默损坏文件（P1）

**现象**：把 `first` 改名为已存在的 `second`，服务端返回编辑并**照改不误**，应用后文件里出现两个 `func second(`：

```cangjie
func second(): Int64 { return 1 }   // 原名 first
func second(): Int64 { return 2 }   // 原名 second
```

**期望**：拒绝（请求失败或空编辑），或按语义做冲突处理；唯一不可接受的是静默产出重复声明。

**复现**：

```cangjie
package sample.conflict

func first(): Int64 { return 1 }

func second(): Int64 { return 2 }

func caller(): Int64 { return first() }
```

`textDocument/rename`，位置取 `first`，newName = `second`。

**证据**：应用编辑后的文本：

```
package sample.conflict

func second(): Int64 {
    return 1
}

func second(): Int64 {
    return 2
}

func caller(): Int64 {
    return second()
}
```

**影响**：客户端一旦应用编辑，用户文件即出现重复声明；这类编辑通常直接落到磁盘，属于数据损坏级别。

**修复方向**：改名前的 `prepareRename`/`rename` 增加「目标名在作用域内已存在」校验，返回错误而不是编辑。

---

## D9 忽略 `hierarchicalDocumentSymbolSupport`（P2）

**现象**：客户端 `textDocument.documentSymbol` 能力里**没有**打开 `hierarchicalDocumentSymbolSupport`，
服务端仍返回 `DocumentSymbol`（带 `children`）；同一份代码在打开该能力时也返回 `DocumentSymbol`——
两个客户端形态拿到完全相同的形状。

```
客户端能力：DocumentSymbolCapabilities()（hierarchical=false）
请求：      textDocument/documentSymbol  structure.cj
实际：      Either.right = DocumentSymbol(name=Shape, children=[area]), Either.right = DocumentSymbol(name=Square, ...)
期望：      Either.left  = SymbolInformation(...) 扁平列表
```

**规范依据**（LSP 3.17 `DocumentSymbolClientCapabilities.hierarchicalDocumentSymbolSupport`）：

> Whether the client support hierarchical document symbols.

服务端 `documentSymbolProvider` 的返回类型是 `DocumentSymbol[] | SymbolInformation[] | null`，
具体给哪一种由客户端的这个协商位决定。不降级的后果不是「报错」而是**静默空白**：
老客户端拿到 `DocumentSymbol` 时按 `SymbolInformation` 解析，`name`/`kind` 字段对不上（它们嵌套在
`right` 里、且没有 `location`），大纲视图直接空掉——用户看不到任何符号，也没有错误提示。

**证据**：

```
CangjieRealSemanticsProtocolDetailTest > document symbols degrade to flat symbols when the client has no hierarchy support
AssertionFailedError: 客户端未声明层级符号支持时不得返回 DocumentSymbol（实际 2 个）
```

对照组 `document symbols keep their hierarchy when the client supports it` **通过**，说明层级本身是对的，
缺的只是「按协商位切换形状」。

**疑似位置**：`CangjieTextDocumentService.documentSymbol` 直接返回 `analysisFacade.documentSymbols(...)`；
`lsp/src` 全库检索 `hierarchicalDocumentSymbolSupport` 无命中——协商位从未被读取。

**修复方向**：在 initialize 阶段把 `textDocument.documentSymbol.hierarchicalDocumentSymbolSupport`
存进 `serverContext`（与其它协商位同一处），`documentSymbol` 在未支持时把层级拍平成
`SymbolInformation`（`location` 用符号的 `range`，`containerName` 用父符号名）。

---

## D10 语法错误不上报（P2）

**现象**：文件少一个右括号（`func dangling(): Int64 {` 之后再无 `}`），`textDocument/diagnostic`
返回**空列表**；push 通道在打开后也只发过一条空诊断。同一份工作区里类型错误（`return "text"`）是能正常
上报的（`broken.cj` 的用例通过），所以这不是「诊断整体失效」，而是**语法/解析错误没有进入诊断**。

```
package sample.syntax

class Holder {
    func value(): Int64 {
        return 1
    }
}

func dangling(): Int64 {
    return 1
        ← 文件到此结束，缺两个右括号
```

**实际**：`relatedFullDocumentDiagnosticReport.items = []`
**期望**：至少一条 `Error`，指向未闭合的结构（`cjc` 对该文件必然编译失败）。

**为什么值得记录**：这是「编辑器全绿、构建报错」的典型来源。用户写代码时括号没补全很常见，
若服务端始终不报，用户只能在跑构建时才发现，且无法定位到具体行。

**已知的反驳与我的核对**：

- 「打字中间态应该抑制语法错误」——多数 IDE 的确会弱化中间态，但抑制通常只针对**当前编辑行**，
  且**保存后**必须恢复；这里 `didSave` 走的还是同一套 `collectDiagnostics`，仍然为空，
  因此不是「暂时抑制」而是「从不报告」。
- 「也许诊断要等编译」——`pull`（`textDocument/diagnostic`）是按需即时计算的，仍然为空。

**疑似原因**：诊断来自 `CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS` 的语义检查器；
解析恢复后的 PSI 上没有对应 checker，parse error 没有并进 LSP 诊断列表。

**修复方向**：收集 `PsiErrorElement` / parse diagnostics 并合入 `collectDiagnostics`;
或者确认语法错误由编译前端报告，并把它接到同一通道。

---

## D11 符号种类未区分成员与顶层（P3）

**现象**：`textDocument/documentSymbol` 对同一个类的成员给出：

```cangjie
class Square <: Shape {
    var side: Int64 = 2          // 实际 kind = Variable，期望 Field
    override func area(): Int64  // 实际 kind = Function，期望 Method
}
```

实测（失败断言里的原样输出）：

```
Square.children = [(side, Variable), (area, Function)]
```

类型本身是对的（`interface Shape` → `Interface`、`class Square` → `Class`、顶层 `func twice` → `Function`），
错的是**成员与顶层的区分**。

**影响**：

- 大纲 / 面包屑的图标全部统一，用户无法区分字段与方法；
- 客户端按 kind 过滤符号时（「只看字段」「只看方法」、部分 IDE 的 `workspace/symbol` 快捷筛选）
  成员会被筛错类目；
- 与 D5 同源：`method` 这个概念在符号映射里只出现在调用点，声明侧一律降级成 `function`。

**证据**：

```
CangjieRealSemanticsPresentationTest > document symbols map class members to the field kind
  expected: <Field> but was: <Variable>
CangjieRealSemanticsPresentationTest > document symbols map member functions to the method kind
  expected: <Method> but was: <Function>
```

**疑似位置**：符号收集里对 `CjNamedDeclaration` 的 kind 映射按「变量 / 函数」两档处理，
没有看它是否位于类型体内（`CjTypeStatement` 的成员）。

**修复方向**：kind 映射增加「是否类型成员」判定：成员变量 → `Field`，成员函数 → `Method`，
顶层分别保持 `Variable` / `Function`。修好后 D5（语义 token 的 `method`）可以一并解决。

---

## 附：一个与本轮改动无关的既有抖动

`LspMacroFailureDiagnosticsIntegrationTest > unavailable executor keeps declarations and publishes dependency macro diagnostics`
在**全量**运行时偶发失败（单独运行稳定通过），报错是 raw CFIR 解析阶段抛
`CangJieIllegalArgumentExceptionWithAttachments: Error while resolving CfirFileImpl from RAW_CFIR to IMPLICIT_TYPES`。

- 复现：连续跑两次全量，第一次红、第二次绿；
- 单独跑该类：绿；
- 我未改动该用例与任何生产代码，判定为**跨用例共享静态状态导致的抖动**（同一 JVM 内多个临时工程共存）。
- 处置：不掩盖、不忽略，记录在此；跑全量若只多出这一条，重跑即可确认不是回归。

## 附：这些缺陷之外，本轮验证为「通过」的关键行为

真实语义（生产 facade）已验证：类型符号种类（interface/class/顶层 function）、注释与字符串里的 hover 为空、
code action 携带触发它的诊断且与请求范围重叠、多根工作区（两个 folder 无显式依赖）如实报 `unresolved import` 且不跨根乱跳、跨模块（`app`→`lib`）的依赖解析 / 跳转 / 反向引用、语法残缺文件仍能给出已完整声明的大纲、
`tabSize` 与 `insertSpaces` 真的影响格式化结果、`publishDiagnostics.version` 随文档版本推进、
「接受候选」整链（completion → executeCommand(applyCompletion) → applyEdit，且失效会话/变更版本会返回协议错误而不发编辑）、
引用结果按 `includeDeclaration` 增删声明、`semanticTokens/range` 与全量在范围内逐 token 一致、
codeAction 按请求范围收敛（接口声明处不返回类的修复）、inlayHint 按范围收敛、非 BMP 字符之后的增量编辑坐标正确（改完仍可解析、仍可跳转）、
格式化幂等（第二次为空编辑且仍能解析出符号）、`didClose` 发布空诊断清掉波浪线且后续请求不报协议错误、导航目标行、hover 文本、签名标签、符号层级（成员是子符号）、`documentSymbol.selectionRange ⊆ range`、token 覆盖源文本、格式化/重命名应用后文本、折叠结构、选择范围链（含多位置批量）、inlay 类型提示、诊断 push/pull 指向同一错误行、诊断增量刷新、pull 诊断即时性、CDoc resolve、inlay resolve 幂等、目录降级（无 multiModule 配置时）、watched files 新建/删除后的可见性端态。
