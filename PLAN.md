# CFIR 宏展开完整实施基线（冻结版）

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

- `PreMacroRawBuildResult` 不返回 `List<CfirFile>`；`runResolution` 只接受 `RecordableRawCfirFiles`。
- source provider 在 construction 前必须为空，只能经 `recordExpandedRawFilesOnce` 进入 `FINALIZED`。
- library/shared/builtins provider 不受 source provider 状态机约束。
- `CfirResolvePhase.MACRO_EXPAND` enum member 必须删除；宏状态只能存在于 `MacroExpansionRegistry` / metadata / construction result。
- `MacroExpandAction` 不再作为 session ordinary resolve 注入通道。
- `CjMacroExpression` 可保留在 PSI/editor；`CfirMacroDeclaration` 可保留为宏定义；旧 `CfirMacroExpression` 不能进入 provider-visible final CFIR。
- `MacroSurface*` 不实现 `CfirElement`，不进入 generated `cfir-tree` visitor/checker。
- text patch、single-token input、full-file rebuild 只可用于 debug/display，不得作为 semantic 或 IDE degraded mode 回退实现。

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

测试 directive：

```text
// MACRO_EXECUTOR: none|stub|real
// EXPECT_DEGRADED: true|false
```

## 12. Implementation Batches

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
```text
完整 PreMacro* / MacroSurface* / BuiltinNonMacroSurface
PSI 与 LightTree builder 都产 surface
CfirMacroExpression 标记 construction-only 或 provider guard hard fail
```

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
