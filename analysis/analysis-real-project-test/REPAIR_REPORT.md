# 真实项目 CFIR 问题修复报告

范围：`analysis/analysis-real-project-test`（amqp、Cliver4cj，cjc 1.0.5）。
基线报告：`analysis/analysis-real-project-test/build/baseline-20261021/`。
修复后报告：`analysis/analysis-real-project-test/build/real-projects/reports/`。
完整 diff：`.scratch/cfir-repair/repair.diff`；审查摘要：`.scratch/cfir-repair/summary.md`。

## 1. 归因表

| 症状 | 模块 | 证据 | 判定 |
|---|---|---|---|
| 310 INVALID_ACCESS_CONTROL、86 NO_MATCH_FUNCTION_DECLARATION_FOR_CALL | Analysis / LL | 纯 CFIR eager 路径不报；LL 类体容器缺失导致 `containingDeclarations` 无 owner class；`privateAccessible` 与调用候选过滤同时失败。修复后 `privateMemberAccess` 由 5 诊断转 0。 | LL 集成缺陷（框架级） |
| connection.cj collector interrupted：pattern binding 返回类型 implicit | Analysis / LL | LL 直接把顶层 `const/let` binding 推到 IMPLICIT_TYPES，未从 owner 投影。修复后 `patternBindingImplicitType` 断言 binding `returnTypeRef is CfirResolvedTypeRef`。 | LL/resolve 阶段推进缺陷 |
| types.cj / Cliver 6 文件异常（EnumConstructor、ClassImpl、pattern initializer） | Analysis / LL | 枚举构造器无 body 却被 BODY 目标解析；状态保持器改写派生 initializer；宏复制 PSI 使 raw rebuild 找不到声明。三项均为 LL 目标/状态保持/宏身份问题。 | LL 集成缺陷（框架级） |
| 单元素异常终止整文件诊断 | Analysis / LL | `Logger.error` 在测试 logger 下抛 AssertionError；FileStructure 无 element 隔离。 | 诊断隔离粒度缺陷 |
| 3 WRONG_NUMBER_OF_ARGUMENTS（`handler(msg)`，跨文件 `MessageHandler = (Message) -> Unit`） | CFIR | `Option` 元素类型未 `fullyExpandedType`，binding 仍是 alias。 | resolve 类型展开缺陷 |
| 93 UNRESOLVED_REFERENCE（CLASS_BASIC/FRAME_END/rootPackagePath 等包级 const/let） | Analysis / LL + platform | 顶层 `CjPatternVariable` 的名称在 binding 上；平台名称/文件索引、LL provider、宏快照都按 owner 占位名。 | 符号/名称索引缺陷 |
| 3 NOT_A_TYPE（`@Test`） | CFIR | 标准库宏注解在 `.cjo` 中暴露为普通 callable；类型解析误报。修复：仅宏命名空间在注解位置抑制 NOT_A_TYPE；普通 callable 类型位置仍报。 | resolve 诊断归属缺陷 |

## 2. 新增/修改测试

`analysis/low-level-api-cfir/test/.../diagnostic/SourcePostSemaDiagnosticsTest.kt`：
`privateMemberAccess`、`patternBindingComparison`、`patternBindingInClassMember`、`matchPatternBindingComparison`、`enumConstructor`、`topLevelPatternVariable`、`patternBindingImplicitType`、`functionTypeAliasCall`、`matchedFunctionTypeAliasCall`、`matchedFunctionTypeAliasFromMap`、`matchedCrossFileFunctionTypeAlias`、`useCrossFileConstant`、`useCrossFileLet`、`macroAnnotationType`、`functionNameAsType`。

`analysis/low-level-api-cfir/test/.../diagnostics/DiagnosticCollectionIsolationTest.kt`：
`failingElementDoesNotEscape`、`laterElementsContinueAfterFailure`。

测试数据：`analysis/low-level-api-cfir/testData/diagnostics/postSema/*.cj`（多文件用例使用 `// MODULE` / `// FILE`，无网络、无真实项目）。

红→绿：
- 初版红：`analysis/low-level-api-cfir/build/red-llt.log`（privateMemberAccess 5 诊断；enumConstructor InvalidCfirElementTypeException；topLevelPatternVariable collector interrupted）。
- 隔离开启后的红：`isolation-red-proof.log`（关闭枚举修复 → `Diagnostic collection isolated 1 exception(s)`）、`isolation-red-proof4.log`（关闭 pattern 投影 → 强制 IMPLICIT_TYPES 失败）；恢复后 `projection-green.log` 绿。
- 宏注解红：`llt-macro-anno-red.log`（CFIR_NOT_A_TYPE@(70,74)）；恢复后绿。
- 跨文件 typealias 红/绿：`llt-fnalias4.log` / `llt-fnalias6.log`。
- 跨文件 const/let 红/绿：`llt-cross-debug*.log` / 最终绿。
- 最终：`after-review-new2.log`（SourcePostSema + Isolation）BUILD SUCCESSFUL。

## 3. 框架级修复

1. **LL 类体容器**：`LLCfirAbstractBodyTargetResolver.withContainingClassLike` 在 `withScopesForClass` 内压入 class-like 容器，与 eager `forRegularClassBody`/`ContextCollector` 对齐，修复 private 可见性与 private 调用候选。
2. **隐式类型阶段推进**：`LLCfirImplicitTypesLazyResolver` 对 `CfirPatternBindingVariable` 投影 owner 的 resolved type（仅 whole-value binding），拆分 pattern 仍由 body resolve 的元素级投影处理。
3. **BODY 目标与状态保持**：枚举构造器 no-op；`BodyStateKeepers.VARIABLE` 跳过 pattern binding 的派生 initializer；`CfirElementFinder` 的宏复制回退限定为“非 local 命名函数 + 直接 owner ClassId + 唯一同名”，空链只接受顶层函数，extend 成员禁用回退。
4. **诊断隔离可观测**：`DiagnosticCollectionFailures` 记录每个被隔离异常；FileStructure 使用 `forEachWithDiagnosticIsolation`；测试和真实项目断言隔离异常为 0；真实项目 JSON 增加 `collectionFailureCount`。
5. **包级 pattern binding 名称索引**：平台 `CangJieFileBasedDeclarationProvider`、LL `LLCangJieSourceSymbolProvider`、`LLCfirMacroSemanticSnapshot` 都按 binding 名登记，跨文件 const/let 可解析。
6. **类型解析**：`Option` 元素类型展开；宏命名空间判定（宏 symbol、官方 std 宏包、session 声明宏包）用于注解位置的 NOT_A_TYPE 归属；普通 callable 类型位置仍报 NOT_A_TYPE。

## 4. 真实项目对比

| 项目 | 修复前 | 修复后 |
|---|---|---|
| amqp | 405 ERROR / 2 异常文件；310 INVALID_ACCESS_CONTROL、86 NO_MATCH、8 UNRESOLVED、1 LAMBDA_MUST_HAVE_TYPE_ANNOTATION、22 UNUSED_VARIABLE 等 | 0 ERROR / 0 异常 / 0 隔离异常；12 UNUSED_IMPORT + 6 UNUSED_VARIABLE warning |
| Cliver4cj | 0 ERROR（假象）/ 6 异常文件 | 0 ERROR / 0 异常 / 0 隔离异常 / 0 诊断 |

两项目 `cjpm clean/build` 均 exit 0；最终 test XML 中 `interrupted by an exception` = 0；报告 `collectionFailureCount` = 0（amqp 总耗时约 16s、Cliver 约 14s，均低于基线）。

诊断差异：
- 消失的 431 条 amqp 诊断中，310/86/8 为本报告归因的误报；其余 warning（UNUSED_EXPRESSION、部分 UNUSED_VARIABLE）经源码核对也是异常中断/部分解析导致的误报。
- 新增 8 条 warning（connection.cj/types.cj 的 UNUSED_IMPORT、connection.cj:534/536/557/559 的 UNUSED_VARIABLE）是文件恢复完整分析后才可见的真实未使用声明。
- Cliver 从“0 诊断 + 6 异常”变为“0 诊断 + 0 异常”。

## 5. 子代理审查

首轮两名独立子代理（架构/K2 对齐、对抗审查）均给出“不通过”，阻塞项：B1 异常隔离把 error 降 warn；B2/B3 finder 回退过宽；B4 注解 NOT_A_TYPE 抑制过宽；B5 diff 不完整。第二轮又发现编译错误（`name` vs `nameAsName`）与 B4 测试未同步。

整改后最终两轮独立复核均为**通过（无阻塞）**：B0 编译、B1 异常门禁、B2 受限 finder、B3 singleOrNull、B4 宏命名空间、B5 diff 完整性全部关闭；剩余失败均为既有（ContextCollector 1、CJMP 2、MapArguments 1、OffsetsOnly guard 1），非本 diff 引入。完整整改细节见 `.scratch/cfir-repair/summary.md` 与 `repair.diff`。

## 6. 未解决项

1. 普通函数作为注解目标（`@foo`）官方报 `sema_no_match_function_declaration_for_call`；CFIR 尚未实现注解宏调用失败诊断，当前不报错（不误报 NOT_A_TYPE）。属宏注解诊断缺口，未用 suppress/allowlist 掩盖。
2. 顶层拆分 pattern（`let (a,b)=...`）的 binding 类型仍由 body resolve 逐元素投影；直接强制 IMPLICIT_TYPES 的拆分 binding 未单独覆盖。
3. 既有失败（非本 diff）：`ContextCollectorSourceTestGenerated.testLocalPatternVariableInitializerRebuild`（无 `<expr>` marker 的 testData 被生成器当 golden 用例）、`CfirCjmpCallerFirstDiagnosticsTest.defaultArgument*`（`CFIR_NOT_MATCHED`，CJMP 默认参数；测试数据/diff 均无相关路径，工作树既有 CJMP session 改动更可疑）、`CfirMapArgumentsTest$Defaults`、`CfirOffsetsOnlyDiagnosticAnchorGuardTest`（guard 列表陈旧）。
4. `-PrealProjectRefresh=false` 实际未被 `RealProjects` 读取（GitFixture 只认 config.refresh）；本次网络可用，离线复跑仍需配置级 refresh=false。属测试基建问题，不影响修复判定。
