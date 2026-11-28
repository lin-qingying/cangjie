# CFIR 真实项目问题修复 — 审查输入摘要（第 2 版，含首轮审查整改）

工作区：`D:\code\intellij\cangjie`。工作树包含本任务之前就存在的大量未提交改动（尤其 pattern 树所有权、宏/CJMP session）。
本任务改动以 `.scratch/cfir-repair/repair.diff` 为准；该 diff 已包含 tracked diff 和本任务新增的 untracked 文件（含 `analysis-real-project-test` 三个 fixtures、新诊断隔离文件、testData）。
`CfirPatternBindingUtils.kt` 所在文件在 HEAD 不存在（前置 pattern 重构引入），因此 diff 不能单独应用到 HEAD；基线应为“当前工作树减去本 diff”。

## 1. 归因表（证据）

| 症状（基线） | 模块 | 证据 / 根因 / 修复 |
|---|---|---|
| amqp 310 INVALID_ACCESS_CONTROL、86 NO_MATCH_FUNCTION_DECLARATION_FOR_CALL | Analysis / LL | eager 路径不报；LL 懒解析进入类体时 `withContainingClassLike` 未把 class-like 压入 `BodyResolveContext.containers`，`containingDeclarations` 缺 owner class，`privateAccessible` 失败、private 调用被候选过滤。修复：LL 入口补 `withContainer(classLike)`（与 eager `forRegularClassBody`、`ContextCollector` 一致）。测试 `privateMemberAccess`。 |
| amqp connection.cj collector interrupted（CfirPatternBindingVariable 返回类型 implicit） | Analysis / LL | LL 直接把顶层 `const/let` 的 binding 推到 IMPLICIT_TYPES 时没有从 owner 投影类型。修复：`LLCfirImplicitBodyTargetResolver` 对 unique whole-value binding 先推进 owner，再投影 `CfirResolvedTypeRef`。测试 `topLevelPatternVariable`、`patternBindingImplicitType`（后者直接断言 `returnTypeRef is CfirResolvedTypeRef`）。 |
| amqp types.cj / Cliver 6 文件异常（EnumConstructor/ClassImpl） | Analysis / LL | `forceBodyResolve` 把无 body 的 `CfirEnumConstructor` 当 BODY 目标；`BodyStateKeepers.VARIABLE` 改写 pattern binding 的派生 initializer；宏生成的 specific 函数在 raw rebuild 中找不到（PSI 复制）。修复：枚举构造器 body no-op；状态保持器跳过派生 initializer；`CfirElementFinder` 增加受限回退（仅非 local 命名函数、owner class 匹配、唯一同名）。测试 `enumConstructor`、`topLevelPatternVariable`。 |
| 单元素异常终止整文件诊断 | Analysis / LL | `Logger.error` 在测试中变 AssertionError，FileStructure 无 element 级隔离。修复：element 级隔离 + `DiagnosticCollectionFailures` 显式记录（每次顶层收集 clear）；测试/真实项目断言隔离异常为 0。测试 `DiagnosticCollectionIsolationTest`、`assertNoDiagnostics` 的 `collectDiagnostics`。 |
| amqp 3 WRONG_NUMBER_OF_ARGUMENTS（跨文件 typealias 函数值 `handler(msg)`） | CFIR | `Option` 元素类型未展开，`Some(handler)` 的 binding 仍是 alias 类型。修复：`resolveStdlibOptionArgumentTypes` 对 `optionElementType.fullyExpandedType(session)`。测试 `matchedCrossFileFunctionTypeAlias`。 |
| amqp 93 UNRESOLVED_REFERENCE（CLASS_BASIC/FRAME_END/rootPackagePath 等包级 const/let） | Analysis / LL + platform | 顶层 const/let 是 `CjPatternVariable`，名称在 binding 上；平台名称/文件索引、LL provider 扫描、宏快照索引都按 owner 占位名。修复：三处都按 binding 名登记。测试 `useCrossFileConstant`、`useCrossFileLet`。 |
| Cliver 3 NOT_A_TYPE（`@Test`） | CFIR | 标准库宏注解在 `.cjo` 中暴露为普通 callable，类型解析误报 not-a-type。修复：注解位置仅当 callable 属于宏命名空间（宏 symbol、官方标准宏包、session 已声明宏包）时抑制 NOT_A_TYPE；普通 callable 在类型位置仍报 NOT_A_TYPE（`functionNameAsType`）。测试 `macroAnnotationType`（关闭修复时红：CFIR_NOT_A_TYPE@(70,74)）。 |

## 2. 测试与红绿证据

新增/修改：
- `analysis/low-level-api-cfir/test/.../diagnostic/SourcePostSemaDiagnosticsTest.kt`
  - privateMemberAccess / patternBindingComparison / patternBindingInClassMember / matchPatternBindingComparison / enumConstructor / topLevelPatternVariable / patternBindingImplicitType / functionTypeAliasCall / matchedFunctionTypeAliasCall / matchedFunctionTypeAliasFromMap / matchedCrossFileFunctionTypeAlias / useCrossFileConstant / useCrossFileLet / macroAnnotationType / functionNameAsType
- `analysis/low-level-api-cfir/test/.../diagnostics/DiagnosticCollectionIsolationTest.kt`
  - failingElementDoesNotEscape / laterElementsContinueAfterFailure（走 `forEachWithDiagnosticIsolation`，并断言隔离异常计数）
- `analysis/low-level-api-cfir/testData/diagnostics/postSema/*.cj`（多文件用例使用 `// MODULE`/`// FILE`，无网络）

红→绿证据（隔离已开启之后）：
- 枚举构造器：关闭修复 → `analysis/low-level-api-cfir/build/isolation-red-proof.log`，测试失败 `Diagnostic collection isolated 1 exception(s)`（CfirEnumConstructorImpl IMPORTS→BODY_RESOLVE）；恢复 → 全绿。
- pattern binding 投影：关闭修复 → `isolation-red-proof4.log`，`binding.lazyResolveToPhase(IMPLICIT_TYPES)` 失败；恢复 → `projection-green.log` BUILD SUCCESSFUL。
- 宏注解：关闭修复 → `llt-macro-anno-red.log`（CFIR_NOT_A_TYPE@(70,74)）；恢复 → 绿。
- 跨文件 typealias：`llt-fnalias4.log` 红；`llt-fnalias6.log` 绿。
- 跨文件 const/let：`llt-cross-debug*.log` 红；最终绿。
- 最终：`after-review-new4.log` BUILD SUCCESSFUL（SourcePostSema 17 + Isolation 2）。

首轮/第二轮审查整改对应关系：
- B1：`DiagnosticCollectionFailures` 记录被隔离异常；`FileStructure.getAllDiagnosticsForFile` 每次收集前 clear，测试 `try/finally` drain；`collectDiagnostics`、`DiagnosticCollectionIsolationTest`、真实项目 `assertNoAnalysisExceptions` 都断言隔离异常=0；`isolation-red-proof.log`/`isolation-red-proof4.log` 证明隔离开启后仍能红。
- B2/B3：唯一同名回退仅限非 local 命名函数 + 直接 owner 的完整 `ClassId`（`containerClassIds.last()`；空链仅顶层）+ 唯一候选；extend 成员禁用；删除文本/范围宽松匹配和 singleOrNull 绕过。
- B4：`isMacroOnly` 改为 `all`；限定/包级路径仅在注解位置且全部宏名时抑制；enum 构造器不参与注解遮蔽；标准宏包复用 providers 的 `isOfficialStdlibMacroPackage`；按官方 `PreCheck` 更新 `CfirRawQualifiedTypeResolutionTest` 的宏名类型位置期望。
- B5：diff 含 `analysis-real-project-test` 三个 fixtures、新增诊断隔离文件、testData、B4 测试更新（38 个文件）；无法单独应用到 HEAD 的原因见顶部。

注意：`isolation-red-proof.log`/`isolation-red-proof4.log` 是首轮审查后补的“隔离开启 + 修复关闭”对照，证明 new gate 能独立失败。

## 3. 修复文件（本任务）

- `analysis/low-level-api-cfir/.../transformers/LLCfirAbstractBodyTargetResolver.kt`（class-like 容器）
- `.../transformers/LLCfirImplicitTypesLazyResolver.kt`（pattern binding 类型投影）
- `.../transformers/LLCfirBodyLazyResolver.kt`（枚举 body、派生 initializer）
- `.../diagnostics/LLCfirDiagnosticVisitor.kt`（warn + 失败记录）
- `.../diagnostics/DiagnosticCollectionFailures.kt`（新，失败记录）
- `.../diagnostics/DiagnosticCollectionIsolation.kt`（新，FileStructure 实际使用的 element 级隔离入口）
- `.../file/structure/FileStructure.kt`（使用隔离入口）
- `.../symbolProviders/LLCangJieSourceSymbolProvider.kt`、`.../macro/LLCfirMacroSemanticSnapshot.kt`（binding 名索引）
- `.../util/CfirElementFinder.kt`（受限宏复制回退：仅非 local 命名函数 + 完整 ClassId 链 + 唯一同名；空链只接受顶层函数；`CjExtend` 成员禁用回退；移除文本/范围宽松匹配和绕过 acceptor 的 singleOrNull）
- `.../lazy/resolve/RawCfirNonLocalDeclarationBuilder.kt`（错误信息 + 传入 owner class 链）
- `analysis/analysis-api-platform-interface/.../CangJieFileBasedDeclarationProvider.kt`（pattern binding 名称/文件索引）
- `cfir/cfir-tree/src/.../patterns/CfirPatternBindingUtils.kt`（公开 wholeValueBindingOrNull）
- `cfir/resolve/src/.../CfirTypeResolver.kt`、`CfirTypeCandidateCollector.kt`（宏命名空间判定：仅“全部 callable 都是宏”时在注解位置抑制 NOT_A_TYPE；普通 callable/非注解限定类型位置保留 NOT_A_TYPE；enum 构造器不参与注解遮蔽；标准宏包判定复用 `cfir/providers` 的单一来源）
- `cfir/resolve/test/.../CfirRawQualifiedTypeResolutionTest.kt`（按官方 `PreCheck.cpp` 语义更新既有宏名类型位置期望：`ConeNotATypeError`；测试名改为 `macro name used as type reports not-a-type without package diagnostic`）
- `cfir/providers/src/.../macro/MacroDemandClassification.kt`（公开 `isOfficialStdlibMacroPackage`，避免宏包 allowlist 漂移）
- `cfir/resolve/src/.../body/CfirPatternBindingResolution.kt`（Option 元素类型展开）
- `analysis/analysis-real-project-test/testFixtures/.../RealProjectReport.kt`、`RealProjectSession.kt`、`RealProjectTestBase.kt`（collectionFailures 计入 JSON/断言）

## 4. 真实项目前后对比

基线：`analysis/analysis-real-project-test/build/baseline-20261021/{amqp-main,Cliver4cj-main}/cfir.json`
修复后：`analysis/analysis-real-project-test/build/real-projects/reports/{amqp-main,Cliver4cj-main}/cfir.json`

| 项目 | 修复前 | 修复后 |
|---|---|---|
| amqp | 405 ERROR / 2 异常文件；310 INVALID_ACCESS、86 NO_MATCH、8 UNRESOLVED 等 | **0 ERROR / 0 异常 / 0 collectionFailure**；仅 12 UNUSED_IMPORT + 6 UNUSED_VARIABLE warning |
| Cliver4cj | 0 ERROR（假象）/ 6 异常文件 | **0 ERROR / 0 异常 / 0 collectionFailure / 0 诊断** |

两项目 cjpm clean/build exit 0；最终 XML `interrupted by an exception` = 0；报告新增 `collectionFailureFileCount/collectionFailureCount`，均为 0。

## 5. 回归

- `:analysis:low-level-api-cfir:test`：183 tests / 3 failed（均为任务前既有，见下）。
- `:cfir:resolve:test`：152 / 1 failed（`CfirMapArgumentsTest$Defaults`，与本 diff 无调用关系；`CfirRawQualifiedTypeResolutionTest` 已按官方语义更新并通过 9/9）。
- `:cfir:providers:test`：43 / 1 failed（`CfirOffsetsOnlyDiagnosticAnchorGuardTest`，报告文件不在 diff）。
- 定向 LLT：`CfirAnalysisLLTTestGenerated$Class.testClassAccessControl`、PSI 同名、`$Extend.testExtendAccessControl1`、`$Class.testClassStatic04` → BUILD SUCCESSFUL。
- 真实项目：BUILD SUCCESSFUL（2m 11s）；`collectionFailureCount=0`、XML `interrupted=0`；amqp/Cliver 均 0 ERROR、0 异常。

既有失败（未由本 diff 修改，未能用 clean HEAD 复跑，但调用路径/测试数据/文件均不在 diff）：
- `ContextCollectorSourceTestGenerated.testLocalPatternVariableInitializerRebuild`：`localPatternVariableInitializerRebuild.cj` 无 `<expr>` marker，生成器仍当 golden 用例。
- `CfirCjmpCallerFirstDiagnosticsTest.defaultArgument{,Inferred}CallerFirst`：`CFIR_NOT_MATCHED`；测试数据无 pattern/typealias/Option/注解，diff 无 CJMP 默认参数 hunk；首轮审查时关闭 class 容器、关闭 finder 回退后仍失败。
- `CfirMapArgumentsTest` / `CfirOffsetsOnlyDiagnosticAnchorGuardTest`：见上。

## 6. 已知偏差 / 未解决

- 普通函数作为注解目标（`@foo`）官方报 `sema_no_match_function_declaration_for_call`；CFIR 目前没有注解宏调用失败诊断，因此不报错（不会误报 NOT_A_TYPE）。已移除容易误导的 `@ordinaryFunction` 测试；作为后续宏注解诊断项。
- 顶层拆分 pattern（`let (a,b)=...`）的 binding 类型仍由 body resolve 逐元素投影；直接强制 IMPLICIT_TYPES 的拆分 binding 未单独覆盖。
- `repair.diff` 依赖工作树中前置 pattern 重构；不能单独应用到 HEAD（见顶部说明）。
