# 全量测试结果（2026-11-09）

- 命令：`gradlew test --continue --rerun --max-workers=1 -Dorg.gradle.parallel=false`（走 gradle-queue）
- 代码状态：分支 `codex/push-commits-pr` + 工作区未提交改动
- 运行时间：18:49:57 – 19:53:16（1h03m），`BUILD FAILED`（3 个测试任务失败 + 2 个测试编译失败）
- 首次并行运行因系统可用内存不足（hs_err：native malloc 失败）守护进程崩溃，改为串行重跑

## 总览

| 指标 | 数量 |
|---|---|
| 执行用例 | 17 751（XML 17 562 + low-level-api-cfir 日志 190 − 被覆盖的 1） |
| 通过 | 17 204 |
| 失败 | 61 |
| 跳过 | 486 |
| 测试未能编译、未运行的模块 | 2（`:code-insight:hints`、`:code-insight:completion:impl-cfir`） |

> `:analysis:low-level-api-cfir` 的 XML 在本次运行结束后（19:57）被其他会话的聚焦测试覆盖，该模块数字取自 Gradle 日志（190 completed, 1 failed）。

## 各模块

| 模块 | 用例 | 通过 | 失败 | 跳过 |
|---|---|---|---|---|
| `:cfir:analysis-tests` | 9305 | 8778 | **59** | 468 |
| `:cfir:raw-cfir:psi2cfir` | 3232 | 3232 | 0 | 0 |
| `:analysis:analysis-api-cfir` | 1694 | 1694 | 0 | 0 |
| `:cfir:raw-cfir:light-tree2cfir` | 1131 | 1131 | 0 | 0 |
| `:analysis:analysis-api-standalone` | 720 | 720 | 0 | 0 |
| `:psi` | 285 | 285 | 0 | 0 |
| `:analysis:low-level-api-cfir` | 190 | 189 | **1** | 0 |
| `:cfir:resolve` | 153 | 153 | 0 | 0 |
| `:compiler:frontend` | 130 | 130 | 0 | 0 |
| `:cfir:cfir-serialization` | 125 | 120 | 0 | 5 |
| `:lsp` | 121 | 114 | 0 | 7 |
| `:code-insight:completion:impl-shared` | 85 | 85 | 0 | 0 |
| `:compiler:codegen` | 78 | 74 | 0 | 4 |
| `:compiler:jvm-codegen` | 66 | 66 | 0 | 0 |
| `:common` | 44 | 44 | 0 | 0 |
| `:analysis:decompiled:decompiler-to-psi` | 43 | 43 | 0 | 0 |
| `:cfir:providers` | 43 | 43 | 0 | 0 |
| `:chir:chir-tree` | 41 | 40 | 0 | 1 |
| `:analysis:analysis-performance-test` | 32 | 32 | 0 | 0 |
| `:chir:cfir2chir` | 28 | 28 | 0 | 0 |
| `:macro:macro-process` | 22 | 21 | 0 | 1 |
| `:cfir:cfir-cones` | 21 | 21 | 0 | 0 |
| `:code-insight:highlighting` | 16 | 16 | 0 | 0 |
| `:llvm-interop:llvm-interop-jni` | 15 | 15 | 0 | 0 |
| `:analysis:analysis-real-project-test` | 14 | 13 | **1** | 0 |
| `:resolution.common` | 14 | 14 | 0 | 0 |
| `:compiler:config` | 11 | 11 | 0 | 0 |
| `:analysis:decompiled:decompiler-to-stubs` | 10 | 10 | 0 | 0 |
| `:analysis:macro-expansion` | 10 | 10 | 0 | 0 |
| `:analysis:stubs` / `:analysis:symbol-light-declarations` / `:cfir:cfir-tree` / `:llvm-interop:llvm-interop-api` | 8 各 | 全过 | 0 | 0 |
| `:macro:macro-stub` | 7 | 7 | 0 | 0 |
| `:analysis:decompiled:decompiler-to-file-stubs` / `:compiler:phaser` | 6 各 | 全过 | 0 | 0 |
| `:analysis:cj-references` / `:code-insight:folding` | 5 各 | 全过 | 0 | 0 |
| `:cfir:cfir-common` | 4 | 4 | 0 | 0 |
| `:analysis:light-declarations` | 3 | 3 | 0 | 0 |
| `:compiler:cli` | 2 | 2 | 0 | 0 |
| `:code-insight:formatting` / `:code-insight:override-implement` | 1 各 | 全过 | 0 | 0 |

## 期望核查结论（以 cjc 实测 + 官方源码为准）

| 类别 | 用例数 | 期望是否正确 | 结论 |
|---|---|---|---|
| A 宏库加载失败 | 56 | 与期望无关 | **测试环境问题**。PATH 补上 1.1.3 运行时后重跑 `CfirAnalysisMacroTestGenerated`：335/335 通过（未补时 28 个失败，可稳定复现） |
| B 枚举成员被同名类型遮蔽 | 3 | **期望错误** | cjc 1.0.0/1.0.5/1.1.3 都报 6 个 error，CFIR 的实际输出与官方一致，应改测试数据，不改实现 |
| C `private package` | 1 | 期望正确 | cjc 三个版本都报 `parse_illegal_modifier_in_scope`；**PSI 路径实现缺陷** |
| D mariadb 真实项目 | 1 | 期望正确（不应抛异常） | `.cjo` 反序列化实现缺陷 |
| E 测试编译失败 | 2 个模块 | — | 测试代码与主代码不同步 |

### B 的证据
探针：`.scratch/cjprobe-20261109/enum/`（测试数据原样拆成 decl.cj / use.cj，加一个 main.cj）。
cjc 1.0.0 / 1.0.5 / 1.1.3 结果一致（JSON DiagKind 来自 1.1.3）：

| 位置 | 官方 kind |
|---|---|
| `case 0x01 => AuthMoreData` | `sema_mismatched_types`（expected 'Enum-ResponseCode', found 'Class-AuthMoreData'）+ `sema_ref_not_be_type` |
| `case 0xFF => Error` | `sema_mismatched_types`（found 'Class-Error'，即 std.core.Error）+ `sema_ref_not_be_type` |
| `case 0xFE => String` | `sema_mismatched_types`（found 'Struct-String'）+ `sema_ref_not_be_type` |

官方实现（`src/Sema/TypeCheckExpr/NameReferenceExpr.cpp` `InferRefExpr`，v1.0.0 / v1.1.0 / 当前 HEAD 逻辑相同）：
先走普通 `Lookup`，**只有结果为空或全是宏函数时**才进入 `EnumSugarChecker`。同名 class/struct 会被 `Lookup` 找到，
于是走普通引用路径并报 `sema_ref_not_be_type`。测试注释引用的 `EnumSugarTargetsFinder::RefineTargets` 只是在
**枚举构造器候选之间**按 target type 筛选，与同名类型的竞争无关，所以注释的推理不成立。
受影响的测试数据：
- `cfir/analysis-tests/testData/diagnostics2/enum/enumEntryShadowedByTypeName.cj`（LightTree + PSI 两个套件）
- `analysis/low-level-api-cfir/testData/diagnostics/postSema/enumEntryShadowedByTypeName.cj`

**已改正（2026-11-09）**：diagnostics2 两套已转绿。LL 用例的断言改为「3 个 REF_NOT_BE_TYPE + 3 个 TYPE_MISMATCH」（cjc 的数量），
现在因 LL 层每处多报一条 TYPE_MISMATCH（实际是 6 条）而失败。diagnostics2 管线每处只报 1 条，
所以这是 LL 诊断收集路径的重复报告缺陷，测试期望本身没有问题。

### C 的证据
cjc 1.0.0 / 1.0.5 / 1.1.3：`private package pkgmodprivate` → `parse_illegal_modifier_in_scope`
"unexpected modifier 'private' on package declaration"，主锚点 1:1–1:8（`private` token），与测试的内联标记位置一致。
官方 `src/Parse/ParserModifierRules.cpp` 的 `PACKAGE_MODIFIERS` 只有 PUBLIC / INTERNAL / PROTECTED。
LightTree 套件已报出 WRONG_MODIFIER_TARGET，PSI 套件漏报，属于实现缺陷。

### A 的证据
- 本机用户级 PATH 中的仓颉运行时目录全部失效（`sdks/cangjie-1.0.5/...` 已不存在，`Temp/cj120/...` 已被清理），
  `CANGJIE_HOME` 也指向已删除的 `Temp/cj120`。
- 测试 fixture 解析到的 SDK 是 1.1.3（日志：`LSPMacroServer 已启动: ...cangjie-sdk-windows-x64-1.1.3\cangjie\tools\bin\LSPMacroServer.exe`）。
  `LspMacroServerMacroExecutor` 在 Windows 上依赖继承的 PATH（只有 Unix 会主动补 `runtime/lib/<platform>`），
  宏 DLL 依赖的 `libcangjie-runtime.dll` 等找不到，DefLib 返回失败路径。
- 把 `<1.1.3>/runtime/lib/windows_x86_64_cjnative` 等目录加入 PATH 后重跑：335/335 全过。
- 修复方向（二选一）：修正本机 PATH / CANGJIE_HOME；或让 Windows 分支也像 Unix 一样，主动把所选 SDK 的
  `runtime/lib/<platform>` 前置到子进程 PATH（后者能让测试不依赖本机环境，更稳）。

> 注：重跑期间主检出又被合入了其他分支（6a01acc9c 等），`low-level-api-cfir` 单独重跑出现 22 个新的
> 异常型失败（`Exception in declaration checkers` / `Class name must not be root: T`），这些不属于本次全量基线。

## 失败归类

### A. 宏动态库加载失败 — 56 个（`:cfir:analysis-tests`，Macro / MacroPsi 两套各 28）
全部是 `MACRO_CANNOT_OPEN_LIB [EXECUTOR]`：宏执行器无法打开测试临时目录
（`C:\Users\lin17\AppData\Local\Temp\cangjie-test-*\macro-out-*\lib-macro_*.dll`）里刚编译出的宏动态库。
涉及 `testSameProject*Macro*`（17 个/套）、`Llt/UnusedImport/*`（6）、`Llt/APILevelChecker/Hide/testMixMacro*`（4）、`Llt/Typealias/ImportMacroAlias`、`Llt/Annotation/Globals/testDerive`。
其中 `MultipleFailuresError` 类的用例，第二条失败同样是这个原因（golden 差异只是宏未展开的连带结果）。
疑似环境问题（C 盘 99% 满、可用内存紧张，测试 JVM 临时目录在 C 盘），未验证。

### B. 枚举成员被同名类型遮蔽 — 3 个
- `CfirAnalysisDiagnostics2TestGenerated$Enum.testEnumEntryShadowedByTypeName`
- `CfirAnalysisDiagnostics2PsiTestGenerated$Enum.testEnumEntryShadowedByTypeName`
- `:analysis:low-level-api-cfir` `SourcePostSemaDiagnosticsTest.enumEntryShadowedByTypeName`

match 分支里的 `AuthMoreData` / `Error` / `String`（与类型同名的枚举构造器）被解析成类型，多报 `REF_NOT_BE_TYPE` + `TYPE_MISMATCH`。

### C. `private package` 修饰符诊断缺失 — 1 个
- `CfirAnalysisDiagnostics2PsiTestGenerated$Modifier.testPackageAccessModifier`：期望 `<!WRONG_MODIFIER_TARGET!>private<!> package pkgmodprivate`，实际没报。只有 PSI 套件失败，LightTree 套件通过。

### D. 真实项目 mariadb 分析失败 — 1 个
- `RealProjectAnalysisTest.realProject`：3 个真实项目中 mariadb 失败，49/49 个文件的诊断收集抛出
  `Exception in declaration checkers`，根因一致：
  `IllegalStateException: Cannot resolve CJO reference 'None' to its serialized target`
  （`CfirDeclDeserializer.deserializeReference:719` ← `convertVariableOrEnumConstructor:1775`，反序列化依赖 `.cjo` 中类成员的变量初始化表达式时引用为 None）。
  报告：`analysis/analysis-real-project-test/build/real-projects/reports/mariadb-master/cfir.json`

### E. 测试源码编译失败（测试未运行）— 2 个模块
- `:code-insight:hints:compileTestKotlin`：`CjAnnotationArgumentHintsTest`、`CjCallChainHintsCollectorCandidatesTest`、`CjLambdaHintsCollectorCandidatesTest`、`CjRangeValueHintsCollectorCandidatesTest` 引用了不存在的 API（`CjAnnotationArgumentHintCandidate`、`collectAnnotationCandidates`、`CjCallChainHintsCollector`、`CjLambdaHintsCollector` 等）—— 测试与主代码不同步。
- `:code-insight:completion:impl-cfir:compileTestKotlin`：`DeclarationSuggestionTestServiceRegistrar.kt:35` 的 `FakeGenerationService` 未实现接口新增的抽象成员。
