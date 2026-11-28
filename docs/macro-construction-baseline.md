# CFIR 宏展开实施基线（Macro Construction Baseline）

本文件是 CFIR 宏展开 construction step 的**冻结基线**：宏管线代码注释里的「Baseline 第 N 节」「硬性边界 #N」「Batch Xy」「PLAN.md §N」等引用，全部指向本文件的对应章节。

## 0. 本文件的性质、来源与引用约定

**性质**：设计基线 + 落地状态。第 1–11、13、14 节是实现必须服从的冻结基线；第 12 节给出批次划分与当前状态；附录 A 是代码引用到章节的完整索引；附录 B 记录基线与实际落点目录的偏差。

**来源**：本文原以 `PLAN.md` 存在于仓库根目录（提交 `5eb09eb87` 加入，提交 `57553854e` 一并删除），原标题 `# CFIR 宏展开完整实施基线（冻结版）`。此后宏管线代码注释持续引用它，文件本身却已不在仓库内，故原样迁回 `docs/` 并补上落点状态。第 1–11、13、14 节的冻结原文逐字保留，另有三处增补（均为追加，不改动原文）：

- §2 补条目编号 —— 代码按「硬性边界 #N」引用。
- §7 补字段落点表 —— 把字段集合逐项映射到 `MacroSurface` 成员。
- §11 补 13 维对照表 —— 代码按「cache key 第 N 维」引用。

第 12 节在 Batch 4 下补入代码注释已在使用的 4a/4b 拆分，并为每个 Batch 标注状态。

**引用约定**：

- 「第 N 节」/「§N」= 本文档 `## N.` 标题所在章节；「硬性边界 #N」= 第 2 节第 N 条。
- 「Batch Xy」= 第 12 节的批次条目，其中 `Batch 4a` / `Batch 4b` 是 Batch 4 的两个子批次。
- 「PLAN.md §N」是同一基线在旧文件名下的引用，等价于本文档第 N 节。

**状态来源**：第 12 节的状态列依据仓库内实现与守门测试（[`MacroConstructionArchitectureGuardTest.kt`](../compiler/frontend/test/org/cangnova/cangjie/frontend/pipeline/MacroConstructionArchitectureGuardTest.kt)）判定，不是一次测试运行的结论。含义：

- ✅ 已落地 —— 实现与对应守门测试都在仓库内。
- 🔶 部分落地 —— 主体已落地，条目内仍有代码注释显式标注的扩展点（「Batch N 起…」「留给 Batch N」）。
- ⬜ 未落地。

## 1. 总目标与主流程

宏展开从 ordinary `CfirResolvePhase` 移出，成为 **source final provider 注册前的 construction step**。

```text
pre = buildPreMacroRawFilesNoRecord(...): PreMacroRawBuildResult
macroSymbols = buildMacroSymbolIndex(pre, libraries, macroArtifacts)
context = bindMacroImports(pre, macroSymbols, ...)
result = MacroConstructionService.expand(pre, context, executor, mode)
if result is Success/Degraded:
  recordExpandedRawFilesOnce(sourceProvider, result.recordableFiles, result.registry)
  runResolution(result.recordableFiles)
else:
  report result.registry diagnostics; do not record; do not run ordinary resolve
```

无宏源码也走新边界：`PreMacroRawBuildResult -> identity RecordableRawCfirFiles -> recordExpandedRawFilesOnce`，不是旧 pass-through。

## 2. 硬性边界

条目编号即代码注释中的「硬性边界 #N」。

1. `PreMacroRawBuildResult` 不返回 `List<CfirFile>`；`runResolution` 只接受 `RecordableRawCfirFiles`。
2. source provider 在 construction 前必须为空，只能经 `recordExpandedRawFilesOnce` 进入 `FINALIZED`。
3. library/shared/builtins provider 不受 source provider 状态机约束。
4. `CfirResolvePhase.MACRO_EXPAND` enum member 必须删除；宏状态只能存在于 `MacroExpansionRegistry` / metadata / construction result。
5. `MacroExpandAction` 不再作为 session ordinary resolve 注入通道。
6. `CjMacroExpression` 可保留在 PSI/editor；`CfirMacroDeclaration` 可保留为宏定义；旧 `CfirMacroExpression` 不能进入 provider-visible final CFIR。
7. `MacroSurface*` 不实现 `CfirElement`，不进入 generated `cfir-tree` visitor/checker。
8. text patch、single-token input、full-file rebuild 只可用于 debug/display，不得作为 semantic 或 IDE degraded mode 回退实现。

## 3. 模块与 API

模块拆分：

```text
cfir:raw-cfir:macro-construction-core
  PreMacroRawBuildResult / PreMacroCfirFile
  MacroSurface* / BuiltinNonMacroSurface / IfAvailableSurface
  MacroSymbolIndex / MacroResolutionContext
  token store / scanner / MacroCallForest
  MacroExpansionRegistry data + construction diagnostics

cfir:raw-cfir:macro-construction-cfir 或 cfir:entrypoint bridge
  token fragment -> final Cfir* nodes
  builtin non-macro desugar
  stable splice
  RecordableRawCfirFiles
  recordExpandedRawFilesOnce
```

核心 API：

```text
buildPreMacroRawFilesNoRecord(...): PreMacroRawBuildResult
buildMacroSymbolIndex(pre, libraries, macroArtifacts): MacroSymbolIndex
bindMacroImports(pre, macroSymbols, ...): MacroResolutionContext
MacroConstructionService.expand(pre, context, executor, mode): MacroConstructionResult
recordExpandedRawFilesOnce(sourceProvider, files: RecordableRawCfirFiles, registry)
runResolution(files: RecordableRawCfirFiles)
```

`MacroConstructionResult`：

```text
Success(recordableFiles, registry)
Degraded(recordableFilesWithErrorPlaceholders, registry)
Failed(registry, diagnostics)
ExecutorUnavailable(registry, diagnostics)
Blocked(registry, diagnostics)
```

CLI strict mode 只接受 `Success`；IDE/analysis degraded mode 可接受 `Degraded`。

## 4. MacroSymbolIndex 与 bindMacroImports

`MacroSymbolIndex` 解决宏解析前 source provider 必须为空的问题，但不是 provider 替代品。

索引分区：

```text
source macro declarations from PreMacroRawBuildResult
library/shared/builtins macro definitions
macro artifact definitions
package/import/alias metadata
```

规则：

- source `CfirMacroDeclaration` 只进入 `MacroSymbolIndex`，不进入 source final provider。
- 官方同包 macro def/call 禁止：当前 source package 内发现同名 def + call 直接诊断，不作为合法 lookup。
- 合法宏调用目标来自 imported macro package / macro artifact / builtins；若同构建内有宏源包，必须作为独立 macro package dependency 或已有关联 artifact，不能读取 source final provider。
- `bindMacroImports` 是 mini import binding，只绑定宏相关信息：explicit import、wildcard import、package alias、default macro imports、builtin macro/annotation/non-macro registry。
- ordinary `IMPORTS` phase 后续仍在 expanded files record 后独立运行；可复用不可变 package alias 结果，但不得复用 source provider 符号状态。
- `bindMacroImports` 产物写入 registry/cache key，保证 IDE 缓存可失效。

## 5. Provider 状态机

只约束 source `CfirProviderImpl`：

```text
EMPTY -> OPEN_FOR_EXPANDED_RECORD -> FINALIZED
```

规则：

```text
construction 前 getAllFiles() 必为空
recordExpandedRawFilesOnce 是唯一 source file 注册入口
FINALIZED 后禁止 record
recordFile 改为 private/internal 或 registration-token guarded
duplicate / finalized / pre-macro / surface / old CfirMacroExpression record 失败
```

`recordExpandedRawFilesOnce` 检查：

```text
source provider state
file id/version
no PreMacroCfirFile
no MacroSurface*
no BuiltinNonMacroSurface / IfAvailableSurface
no old CfirMacroExpression macro call
no unresolved macro site
no stale captured declaration
```

## 6. Phase 与旧入口移除

必须删除：

```text
CfirResolvePhase.MACRO_EXPAND
CfirMacroExpandResolveProcessor ordinary registration
LLCfirMacroExpandLazyResolver
MacroExpandAction ordinary resolve injection
```

同步改：

```text
CfirResolveState ordinal cache
CfirResolvePhase.entries / next / previous users
CfirTotalResolveProcessor
CfirResolveProcessors
LL lazy resolver map
LLFlightRecorder
CfirFrontendPipelinePhase
ComponentsContainers / session factories
phase tests and renderers
```

ordinary phases 固定为：

```text
RAW_CFIR -> IMPORTS -> SUPER_TYPES -> TYPES -> STATUS -> EXTENSIONS -> IMPLICIT_TYPES -> BODY_RESOLVE
```

## 7. Pre-Macro Model

`PreMacro*` / `MacroSurface*` 只存在于 construction core。

```text
PreMacroCfirFile
PreMacroDeclaration / Expression / Parameter / Statement
MacroSurfaceDecl / Expr / Param / Node
BuiltinNonMacroSurface
IfAvailableSurface
```

PSI 与 LightTree 两条 raw builder 都必须产出该模型；单边遗漏由 architecture guard 拒绝。

`MacroSurface*` 保存：

```text
surface id
qualified name
@ / @! kind
hasParenthesis
attr/input tokens
source range
scope/container context
modifiers / carried annotations
captured raw syntax
outer declaration / primary constructor / enum / block / comma-list context
stable replace handle
```

上述字段集合的实现落点是 [`MacroSurface`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroSurface.kt)（`sealed class` + `MacroSurfaceDecl` / `MacroSurfaceExpr` / `MacroSurfaceParam` / `MacroSurfaceNode` 四个 `data class` 形态），逐项对应关系为：

| Baseline §7 字段 | `MacroSurface` 成员 |
| --- | --- |
| surface id | `surfaceId` |
| qualified name | `qualifiedName`（另有 `isQualifiedName` 保留"源码是否写出包限定"这一语法区别） |
| `@` / `@!` kind | `kind: Kind`（`PLAIN` / `FORCED`） |
| hasParenthesis | `hasParenthesis` |
| attr / input tokens | `attrTokens` / `inputTokens: List<MacroSurfaceToken>` |
| source range | `sourceRange: MacroSurfaceSourceRange?` |
| scope / container context | `scopeContext: MacroSurfaceScopeContext`（包名、语言模块名、外层类、外层函数） |
| modifiers / carried annotations | `modifiers` / `carriedAnnotations: List<String>` |
| captured raw syntax | `capturedRawSyntax: String?` |
| outer declaration / primary constructor / enum / block / comma-list context | `containerContext: MacroSurfaceContainerContext`（`outerDeclarationKind` / `isInsidePrimaryConstructor` / `isInsideEnumBody` / `isInsideBlock` / `commaListPosition`） |
| stable replace handle | `replaceHandle: CfirReplaceHandle`（splice 只能按 `carrier` 对象身份匹配，禁止退回源码 offset） |

`BuiltinNonMacroSurface` / `IfAvailableSurface` 与 `MacroSurface*` 同文件，`IfAvailableSurface` 额外携带 `branchTokens`。

## 8. 宏语义

- builtin macro（`sourcePackage/sourceFile/sourceLine`）由 evaluator 内建生成 tokens，不走 dynamic lib/external executor，支持 nested `useParentPos`。
- `@IfAvailable` 等 builtin non-macro surface 不送 executor；fragment parse 后仍是 construction surface，必须在 stable splice 前 desugar 成 final Cfir*。
- custom annotation 只有两条路径：
  - `ResolveMacroCall -> ReclassifiedAnnotation`
  - fragment parser `custom-annotation mode`
  - parse 后 residual `MacroSurface*` 默认失败，不做第三次补救。
- macro roots 按 source/token order 确定性排序；即使未来并行，registry 和 diagnostics 输出仍按该顺序。
- macro call forest child-first；children 成功后 refresh parent args。
- `newTokens` 先 token-stage re-eval 到 stable，再 fragment parse。
- fragment parser 输出 construction-only `MacroFragmentResult`，不新增 final 泛型 `CfirNode`。
- stable splice 使用 `CfirReplaceHandle`，禁止 source offset fallback。

## 9. Fragment / Error Placeholder 策略

IDE degraded mode 不保留 `MacroSurface*` 到 final CFIR，而是使用现有 error-bearing final CFIR：

```text
expression site -> CfirErrorExpression + macroOriginId
function-like decl site -> CfirErrorFunction + macroOriginId
value/property-like decl site -> CfirErrorNamedValue 或 existing invalid declaration + macroOriginId
parameter site -> CfirValueParameter with CfirErrorTypeRef + macroOriginId
unsupported decl/node site -> CfirInvalidDeclaration / nearest existing invalid final node + macroOriginId
```

不新增 `CfirMacroErrorPlaceholder` 主模型；若现有节点不足，只补最小 error final node，不引入 surface final node。

fragment parse failure：

```text
CLI strict -> Failed, no record, no ordinary resolve
IDE degraded -> typed error placeholders + MACRO_NOT_EXPANDED / MACRO_EXPANSION_FAILED diagnostic
```

construction/splice 阶段不依赖 `ScopeSession`；`ScopeSession` 只在 provider finalized 后 ordinary resolution 中使用。hygiene 与符号合法性由 expanded raw consistency + ordinary resolve/check 验证。

## 10. MacroExpansionRegistry

Registry 是 session/analysis 级长生命周期对象，供 diagnostics、IDE、LSP/debug 使用。

记录：

```text
original surface tree
macro symbol/import binding
macro call forest
deterministic root order
resolved/reclassified/builtin status
construction diagnostics
no-executor / unresolved / same-package / cannot-open-lib / REEVALFAILED diagnostics
replace handle history
generated nodes
macroOriginId -> original site
token origin maps
generated display text optional
LSP/debug original macro semantic entry
```

ordinary checker 只看 final CFIR；diagnostic renderer 通过 `macroOriginId` 查 registry 映射回 original macro site。

## 11. Cache 与测试 DSL

Cache scope：

```text
per module session + per file expanded result
file cache key 包含 module macro dependency signature
macro artifact/import/builtin registry 改变时模块级失效
```

cache key 包含：

```text
source content / file identity / macro surface ranges
imports/default imports/resolved macro bindings
module/package identity
SDK / stdlib / macro artifacts
compiler/language options
executor ABI/protocol/version
macro construction algorithm version
token scanner version
fragment parser mode/version
builtin macro/non-macro registry version
macro expand iteration limit
macro result token hash
```

上述口径是实现侧的字段来源，不是维度编号。实现把它收敛为 **13 维** [`MacroExpansionCacheKey`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroExpansionCacheKey.kt)（`DIMENSION_COUNT = 13`），代码注释中的「cache key 第 N 维」即指下表编号：

| # | 维度 | 对应上面口径 |
| --- | --- | --- |
| 1 | `sourceContentHash` | source content |
| 2 | `fileIdentity` | file identity |
| 3 | `macroSurfaceRangesHash` | macro surface ranges |
| 4 | `importsHash` | imports / default imports / resolved macro bindings |
| 5 | `modulePackageIdentity` | module / package identity |
| 6 | `sdkSignature` | SDK / stdlib / macro artifacts 的 SDK 部分 |
| 7 | `macroDependencySignature` | SDK / stdlib / macro artifacts 的依赖部分 |
| 8 | `compilerOptionsHash` | compiler / language options |
| 9 | `executorAbi` | executor ABI / protocol / version |
| 10 | `constructionAlgorithmVersion` | macro construction algorithm version |
| 11 | `tokenScannerVersion` | token scanner version |
| 12 | `fragmentParserVersion` | fragment parser mode / version |
| 13 | `runtimeFingerprint` | builtin macro/non-macro registry version + macro expand iteration limit + macro result token hash |

拆分规则：口径第 1 行拆成维度 1–3，口径第 4 行拆成维度 6–7，其余各行一一对应；口径末三行（builtin registry 版本 / iteration limit / result token hash）合成维度 13 的运行期指纹。13 这一维度数由守门测试 `macroExpansionCacheKeyExposesThirteenDimensions` 固定。

**不在 cache key 里**：源码 PSI 节点 identity、line/column 等会因 IDE 重排而漂移的字段。

测试 directive：

```text
// MACRO_EXECUTOR: none|stub|real
// EXPECT_DEGRADED: true|false
```

## 12. Implementation Batches

批次划分与当前状态（状态判定依据见 §0）：

| Batch | 内容 | 状态 | 主要落点 |
| --- | --- | --- | --- |
| 0 | 只读预审 | ✅ | 结论已体现在 §2 / §6，无独立产物 |
| 1 | Provider + PreMacro skeleton | ✅ | [`MacroConstructionApi.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroConstructionApi.kt)、[`RecordExpandedRawFilesOnce.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/RecordExpandedRawFilesOnce.kt)、[`analyse.kt`](../cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt) |
| 2 | MacroSymbolIndex | ✅ | [`MacroSymbolIndex.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroSymbolIndex.kt) |
| 3 | Phase and injection removal | ✅ | `CfirResolvePhase`、`LLFlightRecorder`（旧序号 14 已废弃） |
| 4a | Pre-Macro surface 模型 | ✅ | [`MacroSurface.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroSurface.kt) |
| 4b | PSI / LightTree builder 双覆盖 | ✅ | [`PsiRawCfirBuilder.kt`](../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[`LightTreeRawCfirDeclarationBuilder.kt`](../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt)、`LightTreeRawCfirExpressionBuilder.kt` |
| 5 | Resolution rules | ✅ | [`MacroResolutionContext.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroResolutionContext.kt)、`CfirDiagnosticsList.kt` |
| 6 | Token path | ✅ | [`MacroPayloadTokenizer.kt`](../cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/macro/MacroPayloadTokenizer.kt) |
| 7 | Macro forest + re-eval | ✅ | [`MacroCallForest.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroCallForest.kt) |
| 8 | Fragment parser + splice | ✅ | [`MacroFragmentParser.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroFragmentParser.kt)、[`TokenBackedMacroFragmentParser.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/TokenBackedMacroFragmentParser.kt) |
| 9 | Registry / diagnostics / IDE degraded | ✅ | `MacroExpansionRegistry`（在 `MacroConstructionApi.kt`）、[`MacroExpansionRegistrySessionComponent.kt`](../cfir/providers/src/org/cangnova/cangjie/cfir/session/MacroExpansionRegistrySessionComponent.kt)、[`MacroConstructionDiagnosticCollectorComponent.kt`](../cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt) |
| 10 | Cleanup / integration | 🔶 | [`MacroExpandPhase.kt`](../compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt)；剩余扩展点见下 |

Batch 10 已确认落地的部分：`DefaultMacroReplacer` / `MacroPsiExpansionService` / `MacroCallInfoFactory` / `expandedText` 语义路径在仓库内已无引用（由守门测试 `semanticMacroPathDoesNotReferenceTextPatchExpansion` 固定）；`CfirMacroExpression` 已从 `cfir-tree` / `raw-cfir` 物理删除；`docs/spec/cache key` 即本文档 §11。仍标注为扩展点的是 `MacroReplaceSlot` 的 owner / source / scope / symbol 修复：`MacroFragmentParser.kt` 里 `IdentityMacroStableSplicer`、`TokenBackedMacroFragmentParser.kt` 里 `IdentityBuiltinNonMacroDesugarer` 是占位实现，生产装配由 frontend 侧的 `CfirExpressionMacroStableSplicer` / `CangJieBuiltinNonMacroDesugarer` 承担。

### Batch 0: 只读预审
```text
grep :compiler:chir / :compiler:codegen 对 CfirMacroExpression 的引用
确认现有 CfirErrorExpression / CfirErrorFunction / CfirErrorNamedValue / invalid declaration 覆盖 degraded placeholders
列出 CfirResolvePhase.MACRO_EXPAND 所有 ordinal/entries/next/previous/flight recorder 使用点
```

### Batch 1: Provider + PreMacro skeleton
合并 provider 状态机与 raw no-record，避免中间破损态。

```text
source provider 状态机 + recordExpandedRawFilesOnce
封口 recordFile
raw-build-no-record 返回 PreMacroRawBuildResult
新增 RecordableRawCfirFiles
no-macro identity RecordableRawCfirFiles
runResolution(RecordableRawCfirFiles)
CfirFrontendPipelinePhase 切到 construction stub + identity path
旧 CfirMacroExpression 待记录 hard fail
architecture guard: recordFile( 主路径只在 final registrar
```

### Batch 2: MacroSymbolIndex
```text
收集 source CfirMacroDeclaration 最小宏定义信息
合并 library/shared/builtin/macro artifact definitions
实现 same-package def/call diagnostic
实现 imported macro package/artifact lookup
确保不读 source final provider
```

### Batch 3: Phase and injection removal
```text
删除 CfirResolvePhase.MACRO_EXPAND enum member
移除 ordinary processor / LL lazy / flight recorder 编码
删除 MacroExpandAction ordinary 注入通道
审计 next/previous/ordinal/entries
```

### Batch 4: Pre-Macro surface + builder

分两个子批次落地：

#### Batch 4a: surface 模型立起来

```text
完整 PreMacro* / MacroSurface* / BuiltinNonMacroSurface / IfAvailableSurface 类型
字段为最小可用类型（token 承载、handle 先用 token 占位）
本阶段 surface 列表默认填空，由 buildPreMacroRawFiles 的 fileSurfaces 参数省略时产生
```

#### Batch 4b: PSI 与 LightTree builder 都产 surface

```text
PSI raw builder 与 LightTree raw builder 都采集并产出 surface（本条即"Batch 4 第二项"）
两条 builder 的 surface 覆盖不得单边遗漏，由 architecture guard 拒绝
CfirMacroExpression 标记 construction-only 或 provider guard hard fail
```

4a / 4b 的划分以代码注释为准：`MacroSurface.kt` 与 `MacroConstructionApi.kt` 用 `Batch 4a` 指"模型 + 默认填空"、用 `Batch 4b` 指"双 builder 覆盖"；`PsiRawCfirBuilder.kt` / `LightTreeRawCfirDeclarationBuilder.kt` 的 surface 累加器注释也标注为 `Batch 4b`。

### Batch 5: Resolution rules
```text
MacroResolutionContext
bindMacroImports mini import binding
alias conflict / macro package / builtin macro / builtin annotation / custom annotation fallback / @! / plain-attr overload / macro lib path / executor ABI
每项至少一条独立 testdata
```

### Batch 6: Token path
```text
真实 token capture
attr/input token capture
TokenInfo mapping
quote/string interpolation scanner
useParentPos mapping
移除 single-token semantic dependency
```

### Batch 7: Macro forest + re-eval
```text
deterministic root order
child-first forest evaluator
refresh parent args
parentNames
newTokens token-stage re-eval
fingerprint cycle detection + iteration limit
cycle chain 写 registry
```

### Batch 8: Fragment parser + splice
```text
token-backed parser with custom-annotation mode
construction-only MacroFragmentResult
all replace slots
builtin non-macro desugar before final splice
stable handle
owner/source/scope/symbol fix
```

### Batch 9: Registry / diagnostics / IDE degraded
```text
session/analysis 长生命周期 registry
construction + checker diagnostic mapping
typed error placeholders
MACRO_NOT_EXPANDED
LSP/debug original macro semantic pass
```

### Batch 10: Cleanup / integration
```text
删除/降级 DefaultMacroReplacer、MacroCallInfoFactory single-token、expandedText semantic path、IDE text semantic path
处理 CfirMacroExpression generator 命运：物理删除或 construction-only 降级，不能进 provider
compiler frontend / analysis / IDE / tests 全接入
regenerate affected generated tests/fixtures
docs/spec/cache key
```

## 13. Tests and Guards

- Provider：raw build 不 mutate source provider；no-macro identity record；duplicate/finalized/pre-macro/surface/old-expression record fail。
- Macro index：同包 def/call 报错；imported macro artifact 可 lookup；不读 source final provider。
- Phase：`CfirResolvePhase.MACRO_EXPAND` 不存在；LL lazy/flight recorder/ordinal 无残留。
- IDE degraded：无 executor 不 text patch，typed error placeholders，`MACRO_NOT_EXPANDED` 映射 original site。
- Semantics：builtin macro nested position、`@IfAvailable` desugar、custom annotation 双路径、newTokens pre-parse re-eval、quote/string interpolation scan。
- Performance：无宏文件 surface 扫描 microbench 建基线，目标不超过旧 raw build 明显回退；超过阈值需 profile 后再扩大实现。
- Architecture guards：
  - `recordFile(` 只在 final registrar 内。
  - semantic path 不引用 `DefaultMacroReplacer`、`MacroPsiExpansionService`、`expandedText` replacement。
  - `PreMacro*` 不在 cfir-tree generator/visitor/checker。
  - PSI 和 LightTree raw builder 都覆盖 macro surface。

## 14. Final Acceptance Criteria

```text
1. 无宏源码走 identity RecordableRawCfirFiles。
2. 宏定义解析通过 MacroSymbolIndex，不读 source final provider。
3. 同包 macro def/call 按官方规则诊断。
4. source provider 只经 recordExpandedRawFilesOnce finalized。
5. successful final CFIR 无 MacroSurface*、BuiltinNonMacroSurface、旧 CfirMacroExpression。
6. IDE degraded 使用 registry + typed error placeholders，不把 surface 放进 final tree。
7. CfirResolvePhase.MACRO_EXPAND 删除。
8. MacroExpandAction 不接 ordinary resolve。
9. builtin macro 内建执行，@IfAvailable splice 前 desugar。
10. macro forest deterministic、child-first，newTokens parse 前 re-eval。
11. construction diagnostics 和 checker diagnostics 都映射 original macro site。
12. text patch 不参与语义。
```

## 附录 A：代码引用索引

行号为 2026-10-10 工作树状态，会随代码演进漂移；定位时以下表中的锚点短语为准。共 24 个文件、106 处引用。复现命令：

```bash
grep -rn "baseline 第\|Baseline 第\|baseline §\|Baseline §\|baseline PLAN\|PLAN\.md §\|baseline Batch\|Baseline Batch\|\bBatch [0-9]\|硬性边界" --include=*.kt . | grep -v "^./external/"
```

### A.1 「Baseline 第 N 节」/「§N」

| 章节 | 引用它的文件（行号） | 引用时使用的锚点短语 |
| --- | --- | --- |
| §1 | `cfir/entrypoint/.../pipeline/analyse.kt`(245, 267)；`MacroConstructionApi.kt`(136, 764, 875)；`CfirResolveProcessors.kt`(11)；`CfirFrontendPipelinePhase.kt`(80)；`LLFlightRecorder.kt`(313) | 「主流程」「construction step」「建议显式分三步」 |
| §2 | `analyse.kt`(175)；`MacroConstructionApi.kt`(47)；`RecordExpandedRawFilesOnce.kt`(15)；`MacroSymbolIndex.kt`(126)；`MacroFragmentParser.kt`(71)；`MacroSurface.kt`(10) | 「硬性边界」「硬性边界 #1/#2/#6/#7」 |
| §4 | `MacroSymbolIndex.kt`(22, 118, 150)；`MacroResolutionContext.kt`(44, 92)；`MacroConstructionApi.kt`(828)；`CfirDiagnosticsList.kt`(2796, 2808)；`FrontendMacroConstructionExecutionTest.kt`(771) | 「合法宏调用目标来自…」「同包 macro def/call」「bindMacroImports」「no executor / unresolved / cannot-open-lib」 |
| §5 | `CfirSourceSymbolProvider.kt`(82)；`RecordExpandedRawFilesOnce.kt`(8, 17)；`MacroResolutionContext.kt`(120) | 「Provider 状态机」「`recordExpandedRawFilesOnce` 检查」 |
| §7 | `MacroSurface.kt`(15, 160)；`MacroConstructionApi.kt`(23)；`MacroCallForest.kt`(70)；`MacroFragmentParser.kt`(156) | 「surface 必须承载的字段集合」「语法容器」「parentNames」「replace slots」 |
| §8 | `MacroCallForest.kt`(6, 12, 124)；`MacroFragmentParser.kt`(10, 71, 137, 156, 212)；`MacroResolutionContext.kt`(80, 139)；`MacroSymbolIndex.kt`(82, 172)；`MacroSurface.kt`(212, 373)；`MacroPayloadTokenizer.kt`(97) | 「确定性 source / token order」「custom annotation only has two routes」「builtin non-macro surface」「stable splice」「newTokens 先 token-stage re-eval」 |
| §9 | `MacroConstructionApi.kt`(136, 169)；`MacroExpandPhase.kt`(360, 949)；`CfirDiagnosticsList.kt`(2697, 2705, 2718) | 「typed factory」「IDE degraded」「MACRO_NOT_EXPANDED」「MACRO_EXPANSION_FAILED」「session/analysis 长生命周期 registry」 |
| §10 | `MacroConstructionApi.kt`(293)；`MacroExpandPhase.kt`(387)；`MacroExpansionRegistrySessionComponent.kt`(6) | 「Batch 9 扩展」「session/analysis 长生命周期 registry」 |
| §11 | `MacroExpansionCacheKey.kt`(14, 34)；`MacroConstructionApi.kt`(548, 565, 816)；`MacroResolutionContext.kt`(64)；`MacroSymbolIndex.kt`(70)；`MacroFragmentParser.kt`(35)；`MacroPayloadTokenizer.kt`(44)；`MacroExecutor.kt`(40)；`MacroExpandPhase.kt`(382, 1217)；`MacroConstructionArchitectureGuardTest.kt`(612, 617) | 「cache key 包含」「第 9/10/11/12 维」「第 12/13 维之一」「模块级失效」 |

**§9 与 §10 的一处交叉**：`MacroExpandPhase.kt`(360) 用「第 9 节：session/analysis 长生命周期 registry」引用 registry 的 session 挂载。该表述对应的是 **§10**（§10 开篇即「Registry 是 session/analysis 级长生命周期对象」，§9 讲的是 degraded placeholder 策略）。两节相邻，此处按 §10 定位。

### A.2 「Batch Xy」

| Batch | 引用它的文件（行号） | 引用时使用的锚点短语 |
| --- | --- | --- |
| Batch 1 | `MacroConstructionApi.kt`(19) | 「Batch 1 阶段仅作为 cfirFile 的薄包装」 |
| Batch 4 / 4a | `MacroConstructionApi.kt`(20, 24)；`MacroSurface.kt`(25, 215) | 「Batch 4 引入完整 PreMacro / MacroSurface 模型后」「Batch 4a 阶段由 buildPreMacroRawFiles 默认填空」「Batch 4a 阶段是 token 占位实现」 |
| Batch 4b | `MacroConstructionApi.kt`(91)；`PsiRawCfirBuilder.kt`(142)；`LightTreeRawCfirDeclarationBuilder.kt`(132)；`LightTree2Cfir.kt`(67)；`MacroSurface.kt`(26) | 「Batch 4b 起由 PSI / LightTree builder 填入」「PSI 与 LightTree builder 都产 surface」「baseline Batch 4 第二项」 |
| Batch 5 | `MacroResolutionContext.kt`(26, 120, 139)；`MacroSymbolIndex.kt`(82)；`MacroExpandPhase.kt`(369)；`FrontendMacroConstructionExecutionTest.kt`(766)；`CfirDiagnosticsList.kt`(2802) | 「alias conflict」「macro package」「@! 强制形式」「Batch 5 独立用例（PLAN.md §12 Batch 5）」 |
| Batch 6 | `MacroPayloadTokenizer.kt`(30)；`MacroSurface.kt`(114, 118) | 「真实 lexer 输出」「Baseline Batch 6 入口」 |
| Batch 7 | `MacroCallForest.kt`(6, 280, 388, 389)；`MacroSurface.kt`(121)；`MacroPayloadTokenizer.kt`(34, 38)；`CfirDiagnosticsList.kt`(2869) | 「fingerprint cycle detection」「forest evaluator」「useParentPos」「macro forest」 |
| Batch 8 | `MacroFragmentParser.kt`(4, 159, 160, 178, 192, 193)；`TokenBackedMacroFragmentParser.kt`(7, 59, 148)；`MacroCallForest.kt`(389)；`MacroSurface.kt`(118, 215)；`MacroPayloadTokenizer.kt`(39) | 「fragment parser」「newTokens token-stage re-eval」「stable handle」「token-backed 实现」 |
| Batch 9 | `MacroConstructionApi.kt`(164, 293)；`RecordExpandedRawFilesOnce.kt`(26) | 「diagnostic renderer 通过该 id 反查」「持久化到 session 上」 |
| Batch 10 | `MacroFragmentParser.kt`(160, 178)；`TokenBackedMacroFragmentParser.kt`(148) | 「owner / source / scope / symbol 修复」「stable-splice 接入阶段」「desugar 逻辑由 Batch 10」 |

### A.3 「PLAN.md §N」

历史文件名引用，等价于本文件同名章节：`MacroExpansionCacheKey.kt`(12)、`MacroFragmentParser.kt`(35)、`MacroPayloadTokenizer.kt`(44, 97)、`MacroExecutor.kt`(40)、`FrontendMacroConstructionExecutionTest.kt`(766)。

## 附录 B：实现落点对照（与 §3 模块拆分的偏差）

§3 提议拆出 `cfir:raw-cfir:macro-construction-core` 与 `cfir:raw-cfir:macro-construction-cfir`（或 `cfir:entrypoint` bridge）两个新模块。实际落地**没有新增 Gradle 模块**，同样职责按既有模块边界分布如下：

| 基线职责 | 实际落点 |
| --- | --- |
| PreMacro / MacroSurface / BuiltinNonMacroSurface 模型 | `cfir/providers/src/.../resolve/providers/macro/MacroSurface.kt`、`MacroConstructionApi.kt` |
| MacroSymbolIndex / bindMacroImports / MacroResolutionContext | `cfir/providers/src/.../resolve/providers/macro/`（同名文件） |
| token scanner | `cfir/raw-cfir/raw-cfir-common/src/.../builder/macro/MacroPayloadTokenizer.kt` |
| surface 采集 | `cfir/raw-cfir/psi2cfir/src/.../builder/PsiRawCfirBuilder.kt`、`cfir/raw-cfir/light-tree2cfir/src/.../lightTree/` |
| token fragment → final CFIR、builtin non-macro desugar、stable splice | `compiler/frontend/src/.../pipeline/MacroExpandPhase.kt`（`FrontendMacroConstructionService` / `CfirExpressionMacroStableSplicer` / `CangJieBuiltinNonMacroDesugarer`） |
| RecordableRawCfirFiles / recordExpandedRawFilesOnce / runResolution | `cfir/providers/src/.../macro/MacroConstructionApi.kt`、`RecordExpandedRawFilesOnce.kt`、`cfir/entrypoint/src/.../pipeline/analyse.kt` |
| MacroExpansionRegistry 挂到 session | `cfir/providers/src/.../session/MacroExpansionRegistrySessionComponent.kt` |
| construction diagnostic → checker diagnostic 映射 | `cfir/checkers/src/.../collectors/components/MacroConstructionDiagnosticCollectorComponent.kt` |
| 统计观测 | `analysis/low-level-api-cfir/src/.../statistics/domains/LLMacroConstructionStatistics.kt` |

命名偏差：§3 的入口名 `buildPreMacroRawFilesNoRecord(...)` 在实现中为 `buildPreMacroRawFiles(...)`（`MacroConstructionApi.kt`）；「no record」不变量由返回类型 `PreMacroRawBuildResult` 本身承担 —— 该类型不暴露注册入口，调用方只能经 `MacroConstructionService.expand` + `recordExpandedRawFilesOnce` 进入 source provider。

其余偏差（`MacroConstructionResult` 五个变体、`CfirReplaceHandle` 的 `carrier` 身份匹配、13 维 cache key、§12 的 4a/4b 拆分）均已体现在 §3 / §7 / §11 / §12 正文中。
