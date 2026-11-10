# 全仓库测试运行报告

生成时间：2026/11/1 18:21:46

## 1. 执行信息

| 项目 | 内容 |
| --- | --- |
| 命令 | `gradlew-queue.bat test --continue --console=plain`（仓库根目录） |
| 结果 | BUILD FAILED in 18m 25s（393 tasks：45 executed、2 from cache、346 up-to-date） |
| 测试任务 | 140 个 `:test` 任务；44 个模块有实际用例，其余模块无测试源（NO-SOURCE） |
| 用例总数 | 17683 |
| 失败 / 错误 / 跳过 | 123 / 0 / 478 |
| 用例耗时合计 | 2211.3 s（并行 fork 累计；实际墙钟 18m25s） |
| 数据来源 | 各模块 `build/test-results/test/*.xml`（全部晚于本次运行开始时间，无陈旧结果） |

## 2. 分模块结果

| 模块 | 用例 | 失败 | 跳过 | 耗时(s) |
| --- | ---: | ---: | ---: | ---: |
| analysis/analysis-api-cfir | 1678 | 37 | 0 | 183.1 |
| cfir/raw-cfir/psi2cfir | 3231 | 33 | 0 | 101.8 |
| cfir/raw-cfir/light-tree2cfir | 1121 | 15 | 0 | 40.9 |
| analysis/analysis-api-standalone | 720 | 15 | 0 | 59.7 |
| compiler/frontend | 130 | 6 | 0 | 126.6 |
| lsp | 112 | 4 | 7 | 84.1 |
| cfir/analysis-tests | 9260 | 3 | 460 | 1022.5 |
| analysis/low-level-api-cfir | 183 | 3 | 0 | 30.4 |
| code-insight/completion/impl-cfir | 75 | 2 | 0 | 65.5 |
| psi | 273 | 1 | 0 | 29.7 |
| cfir/resolve | 153 | 1 | 0 | 5.7 |
| cfir/providers | 43 | 1 | 0 | 18.5 |
| code-insight/highlighting | 16 | 1 | 0 | 6.7 |
| analysis/stubs | 8 | 1 | 0 | 65 |
| cfir/cfir-serialization | 121 | 0 | 5 | 34.8 |
| code-insight/completion/impl-shared | 85 | 0 | 0 | 8.9 |
| compiler/codegen | 78 | 0 | 4 | 3.6 |
| compiler/jvm-codegen | 66 | 0 | 0 | 4.1 |
| common | 44 | 0 | 0 | 0.9 |
| chir/chir-tree | 41 | 0 | 1 | 2.1 |
| analysis/analysis-performance-test | 32 | 0 | 0 | 61.5 |
| chir/cfir2chir | 28 | 0 | 0 | 1.6 |
| analysis/decompiled/decompiler-to-psi | 22 | 0 | 0 | 13.1 |
| macro/macro-process | 22 | 0 | 1 | 4.2 |
| cfir/cfir-cones | 18 | 0 | 0 | 0.2 |
| llvm-interop/llvm-interop-jni | 15 | 0 | 0 | 1.5 |
| resolution.common | 14 | 0 | 0 | 0.3 |
| compiler/config | 11 | 0 | 0 | 4.3 |
| analysis/macro-expansion | 10 | 0 | 0 | 0.4 |
| analysis/decompiled/decompiler-to-stubs | 8 | 0 | 0 | 13 |
| analysis/symbol-light-declarations | 8 | 0 | 0 | 33.3 |
| cfir/cfir-tree | 8 | 0 | 0 | 1.5 |
| llvm-interop/llvm-interop-api | 8 | 0 | 0 | 0.4 |
| macro/macro-stub | 7 | 0 | 0 | 0.2 |
| analysis/decompiled/decompiler-to-file-stubs | 6 | 0 | 0 | 0.5 |
| compiler/phaser | 6 | 0 | 0 | 0.4 |
| analysis/cj-references | 5 | 0 | 0 | 13.3 |
| code-insight/folding | 5 | 0 | 0 | 5.4 |
| cfir/cfir-common | 4 | 0 | 0 | 0.4 |
| analysis/light-declarations | 3 | 0 | 0 | 2.1 |
| compiler/cli | 2 | 0 | 0 | 0.5 |
| analysis/analysis-real-project-test | 1 | 0 | 0 | 136.7 |
| code-insight/formatting | 1 | 0 | 0 | 7.3 |
| code-insight/override-implement | 1 | 0 | 0 | 14.6 |
| **合计** | **17683** | **123** | **478** | **2211.3** |

## 3. 失败分组与初步定位

123 个失败集中在 13 个模块；其余 31 个有测试的模块全绿。按现象可分为 5 组：

### A. Dangling file 诊断重复（40 例）

- 模块：`analysis/analysis-api-cfir`（28 例）、`analysis/analysis-api-standalone`（12 例）
- 用例：`testUnresolvedReference`、`testUnresolvedReturnType`、`testDanglingUnresolvedReference`、`testDanglingUnresolvedReferencePreferSelf`
- 场景：4 个 fixture × 7 种 IDE module 形态（Normal/Dependent × Source/LibrarySource/LibraryBinary/LibraryBinaryDecompiled/CodeFragment）与 4 × 3 种 Standalone 形态。
- 现象：同一 PSI 元素、同一 text range 的 `UNRESOLVED_REFERENCE` 被报告两次（期望 1 条，实际 2 条），即 dangling 文件诊断去重/合并失效。

### B. Vararg 参数类型未解析（10 例）

- 模块：`analysis/analysis-api-cfir`（7 例，覆盖 Normal/Dependent 各形态）、`analysis/analysis-api-standalone`（3 例）
- 用例：`VarargArrayTypeTestGenerated.testIntVarargParameter`
- 现象：`intVarargParameter.txt` 期望 `Array<Int64>`，实际得到 `NOT_VARARG`；即 vararg 形参的类型提供者判定丢失。

### C. Raw CFIR golden 不一致（48 例）

- 模块：`cfir/raw-cfir/psi2cfir`（33 例）、`cfir/raw-cfir/light-tree2cfir`（15 例）
- 场景：5 个语法用例 × `.txt` / `.lazyBodies.txt` 两种 golden，共 8 个 golden 文件：
  `builtinAnnotationForms`、`macroCallForms`、`functionTypeRefs`、`functionTypeParamDefault`、`functionTypeMixedParamNames`
- 涉及 PSI 构建、AST 惰性体构建、Stub 惰性体构建三条路径以及 LightTree 转换器，说明是 raw CFIR 构建层的共有行为差异（宏调用/注解/函数类型），而非单一路径偏差。
- 提示：`-Dupdate.test.data=true` 可重新生成 golden，但需先确认是行为修复还是回归。

### D. 宏语义展开相关（13 例）

- `compiler/frontend`（6）：`FrontendMacroConstructionExecutionTest` 抛出 `MacroConstructionDiagnostic`（`macros.ReplaceClass`、`macros.GenerateDeclaration`、`macros.Generated` 未产出 macro fragment）；另有 `MacroArtifactLocatorTest`、`MacroConstructionArchitectureGuardTest`、`CjmpTwoPhaseCompilationTest` 各 1 例。
- `cfir/analysis-tests`（3）：`CfirMacroAnnotationSourceModuleTest` 2 例、`CfirAnalysisMacroPsiTestGenerated$Llt$Annotation$Globals` 1 例（重导出宏 `std.unittest.mock.mockmacro.enumerate` 解析到 `std.collection.enumerate`）。
- `lsp`（4）：`LspCompletionResolveTest` 3 例（completion resolve 返回 `Internal error.`）、`LspCompletionStatisticsTest` 1 例。
- 与工作区未提交的「宏语义展开」改动（`compiler/frontend/FrontendMacroSemanticExpansionEngine.kt`、`lsp/.../LspMacroSemanticExpansionEnvironment.kt`、`MacroExpandPhase.kt`）高度相关。

### E. 单点回归（12 例）

| 模块 | 用例 | 现象 |
| --- | --- | --- |
| `cfir/resolve` | `CfirMapArgumentsTest$Defaults.number of default arguments is counted` | 期望 `RESOLVED`，实得 `INAPPLICABLE_ARGUMENTS_MAPPING_ERROR` |
| `analysis/low-level-api-cfir` | `CfirCjmpCallerFirstDiagnosticsTest`（2 例） | 期望无诊断，实得 `CFIR_NOT_MATCHED` |
| `analysis/low-level-api-cfir` | `ContextCollectorSourceTestGenerated` | `Found no element of type CjElement inside <expr>` |
| `cfir/providers` | `CfirOffsetsOnlyDiagnosticAnchorGuardTest` | 已知 offsetsOnly 站点清单未同步（测试自身提示需更新清单） |
| `analysis/stubs` | `BuiltinsStubsTest` | `std.ast.cjo.stubs.txt` 期望/实际不一致 |
| `psi` | `ForeignAndAnnotationParsingTest` | NPE：`ApplicationManager.getApplication()` 为 null |
| `code-insight/highlighting` | `CDocRendererTest` | CDoc 参数分区渲染文本不一致 |
| `code-insight/completion/impl-cfir` | `CfirInsertionIntegrationTest` / `CfirIndexCompletionSectionTest` | 插入目标 `insertion.dependency` vs `insertion.dependency.insertionTarget`；`CANNOT_REF_TO_PKG_NAME` 无补全 |
| `analysis/analysis-api-cfir` | `AnalysisApiCfirComponentExecutionTest.packageQualifierTypeResolution` / `AnalysisApiImportPackageNavigationTest.organizationDirectoriesAndRename` | 类型解析失败；期望 `shared.changed` 实得 `shared.lib` |

## 4. 结论与后续建议

1. 基线可运行性：全仓 `test` 能完整跑完，编译与测试基础设施本身没有阻塞性问题。
2. 当前工作区有 135 项未提交改动（`cfir` 52、`analysis` 40、`lsp` 13、`macro` 8 等），失败簇 A–D 与这些在途改动（宏语义展开、analysis-api 诊断）方向一致，建议先在对应模块做定向单测收敛，再回归全量。
3. 收敛顺序建议：`cfir/raw-cfir/*`（golden 48 例，一次修复可覆盖两模块）→ `analysis/analysis-api-cfir` + `analysis-api-standalone`（诊断去重 + vararg）→ 宏展开簇（`compiler/frontend`、`lsp`、`cfir/analysis-tests`）→ 单点回归。
4. `cfir/analysis-tests` 共 9260 例（460 跳过），耗时约 1022 s，是主要时间开销；日常验证建议按需 `--tests` 过滤。
5. `intellij-ide/`、`deveco/` 为独立 Gradle 构建，不在本次 `test` 范围内（其 build 目录中是历史结果，未计入统计）。

## 5. 复现方式

```powershell
# 全量（等价本次运行）
.\gradlew-queue.bat test --continue
# 单模块定向
.\gradlew-queue.bat :cfir:raw-cfir:psi2cfir:test --tests "*RawCfirBuilderTestCaseGenerated*Syntax*Types*"
.\gradlew-queue.bat :analysis:analysis-api-cfir:test --tests "*DanglingFileCollectDiagnostics*"
```

原始产物：`.dsh-tmp/all-tests/run.log`（Gradle 全量日志）、`.dsh-tmp/all-tests/summary.json`（结构化结果）。

## 6. 失败用例清单（123）

| # | 模块 | 测试类 | 用例 | 摘要 |
| ---: | --- | --- | --- | --- |
| 1 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 2 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 3 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 4 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 5 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisSourceModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 6 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisCodeFragmentModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 7 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryDecompiledModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 8 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryDecompiledModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 9 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryDecompiledModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 10 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryDecompiledModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 11 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibrarySourceModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 12 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 13 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryDecompiledModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 14 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisSourceModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 15 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisCodeFragmentModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 16 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 17 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 18 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 19 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 20 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 21 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 22 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 23 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 24 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibrarySourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 25 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibrarySourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 26 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibrarySourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 27 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisLibrarySourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 28 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 29 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 30 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 31 | analysis/analysis-api-cfir | CfirIdeNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 32 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 33 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 34 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 35 | analysis/analysis-api-cfir | CfirIdeDependentAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 36 | analysis/analysis-api-cfir | AnalysisApiCfirComponentExecutionTest | packageQualifierTypeResolution(CjFile) | org.opentest4j.AssertionFailedError: CANNOT_REF_TO_PKG_NAME@completion.provider.ProviderType ==> expected: <[completion]> but was: <[]> |
| 37 | analysis/analysis-api-cfir | AnalysisApiImportPackageNavigationTest | organizationDirectoriesAndRename(CjFile) | org.opentest4j.AssertionFailedError: expected: <shared.changed> but was: <shared.lib> |
| 38 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisSourceModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 39 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 40 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 41 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 42 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisLibraryBinaryModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 43 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 44 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 45 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 46 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisCodeFragmentModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 47 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisLibraryBinaryModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 48 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisCodeFragmentModuleVarargArrayTypeTestGenerated | testIntVarargParameter() | org.opentest4j.AssertionFailedError: Actual data differs from file content: intVarargParameter.txt =====预期====== Array<Int64> =====得到====== NOT_VARARG |
| 49 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReferencePreferSelf() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReferencePreferSelf.txt =====预期====== Diagnostics from file: for P… |
| 50 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReturnType() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReturnType.txt =====预期====== Diagnostics from file: for PSI element of typ… |
| 51 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: unresolvedReference.txt =====预期====== Diagnostics from file: for PSI element of type… |
| 52 | analysis/analysis-api-standalone | CfirStandaloneNormalAnalysisSourceModuleDanglingFileCollectDiagnosticsTestGenerated | testDanglingUnresolvedReference() | org.opentest4j.AssertionFailedError: Actual data differs from file content: danglingUnresolvedReference.txt =====预期====== Diagnostics from file: for PSI element… |
| 53 | analysis/low-level-api-cfir | ContextCollectorSourceTestGenerated | testLocalPatternVariableInitializerRebuild() | java.util.NoSuchElementException: Found no element of type CjElement inside <expr> |
| 54 | analysis/low-level-api-cfir | CfirCjmpCallerFirstDiagnosticsTest | defaultArgumentInferredCallerFirst(CjFile, CjTestModule, TestServices) | org.opentest4j.AssertionFailedError: expected: <[]> but was: <[CFIR_NOT_MATCHED]> |
| 55 | analysis/low-level-api-cfir | CfirCjmpCallerFirstDiagnosticsTest | defaultArgumentCallerFirst(CjFile, CjTestModule, TestServices) | org.opentest4j.AssertionFailedError: expected: <[]> but was: <[CFIR_NOT_MATCHED]> |
| 56 | analysis/stubs | BuiltinsStubsTest | builtinsStubsAndDecompiledText() | org.opentest4j.AssertionFailedError: Mismatch in std.ast.cjo.stubs.txt ==> expected: <CangJieFileStubImpl[MultifileClass[packageFqName=std.ast, facadeFqName=std… |
| 57 | cfir/analysis-tests | CfirAnalysisMacroPsiTestGenerated$Llt$Annotation$Globals | testAllkinds() | org.opentest4j.AssertionFailedError: Actual data differs from file content: allkinds.cj =====预期====== @Annotation public class APILevel { let a: UInt8 const ini… |
| 58 | cfir/analysis-tests | CfirMacroAnnotationSourceModuleTest | lightTreeMacroTokensKeepTheirSourceModuleThroughTypes() | java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class org.cangnova.cangjie.cfir.declarations.CfirNamedFunction (java.util.ArrayList is… |
| 59 | cfir/analysis-tests | CfirMacroAnnotationSourceModuleTest | psiMacroTokensKeepTheirSourceModuleThroughTypes() | java.lang.ClassCastException: class kotlin.collections.builders.ListBuilder cannot be cast to class org.cangnova.cangjie.cfir.declarations.CfirNamedFunction (ko… |
| 60 | cfir/providers | CfirOffsetsOnlyDiagnosticAnchorGuardTest | knownOffsetsOnlySitesRemainReferenced() | org.opentest4j.AssertionFailedError: These files no longer construct CjOffsetsOnlySourceElement; remove them from knownOffsetsOnlySites so the exemption set can… |
| 61 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 62 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 63 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 64 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 65 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 66 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 67 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 68 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 69 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 70 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 71 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 72 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 73 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 74 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 75 | cfir/raw-cfir/light-tree2cfir | LightTree2CfirConverterTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 76 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 77 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 78 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 79 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 80 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 81 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 82 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 83 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 84 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByAstTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 85 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 86 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 87 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 88 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 89 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.lazyBodies.txt Run with -… |
| 90 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.lazyBodies.txt Run with -Dup… |
| 91 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 92 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 93 | cfir/raw-cfir/psi2cfir | RawCfirBuilderLazyBodiesByStubTestGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.lazyBodies.txt Run with -Dupdate.test.d… |
| 94 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 95 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 96 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 97 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 98 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testMacroCallForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\macroCallForms.txt Run with -Dupdate.test.data=tr… |
| 99 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Declarations | testBuiltinAnnotationForms | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\declarations\builtinAnnotationForms.txt Run with -Dupdate.test… |
| 100 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 101 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 102 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 103 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 104 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeMixedParamNames | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeMixedParamNames.txt Run with -Dupdate.tes… |
| 105 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Recovery | testFunctionTypeParamDefault | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\recovery\functionTypeParamDefault.txt Run with -Dupdate.test.d… |
| 106 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 107 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 108 | cfir/raw-cfir/psi2cfir | RawCfirBuilderTestCaseGenerated$Syntax$Types | testFunctionTypeRefs | java.lang.AssertionError: Golden file mismatch: cfir\raw-cfir\psi2cfir\testData\rawBuilder\syntax\types\functionTypeRefs.txt Run with -Dupdate.test.data=true to… |
| 109 | cfir/resolve | CfirMapArgumentsTest$Defaults | number of default arguments is counted() | org.opentest4j.AssertionFailedError: expected: <RESOLVED> but was: <INAPPLICABLE_ARGUMENTS_MAPPING_ERROR> |
| 110 | code-insight/completion/impl-cfir | CfirIndexCompletionSectionTest | indexSectionSuppressesShadowedCandidate(CjFile) | org.opentest4j.AssertionFailedError: 被本文件声明遮蔽的索引候选不得存活，实际 [indexSite, shadowedByFile, ArgOpt, ArgumentMode, ArgumentSpec, ParsedArguments, ArgumentParseExceptio… |
| 111 | code-insight/completion/impl-cfir | CfirInsertionIntegrationTest | insertsUnimportedCandidateAndReportsImport(CjFile) | org.opentest4j.AssertionFailedError: expected: <insertion.dependency> but was: <insertion.dependency.insertionTarget> |
| 112 | code-insight/highlighting | CDocRendererTest | sectionsWithLinkSubjectsAreRendered() | org.opentest4j.AssertionFailedError: 应渲染参数分�? <div class='content'><p style='margin-top:0;padding-top:0;'>Greets the caller.</p></div><table class='sections'><t… |
| 113 | compiler/frontend | CjmpTwoPhaseCompilationTest | deserialized generic extend peers honor target substitutions and bounds() | java.lang.NullPointerException: Cannot invoke "com.intellij.openapi.application.Application.assertReadAccessAllowed()" because the return value of "com.intellij… |
| 114 | compiler/frontend | FrontendMacroConstructionExecutionTest | defaultStableSplicerReplacesDeclarationCarrierWithParsedDeclarationPayload() | org.opentest4j.AssertionFailedError: Construction failed: Failed(registry=org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry@492fa72a); di… |
| 115 | compiler/frontend | FrontendMacroConstructionExecutionTest | parentDeclarationExpansionConsumesNestedMacroCarriers() | org.opentest4j.AssertionFailedError: Construction failed: Failed(registry=org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry@8ce3f27); dia… |
| 116 | compiler/frontend | FrontendMacroConstructionExecutionTest | constructionCacheSeparatesParserContextForIdenticalSourceAndSurface() | org.opentest4j.AssertionFailedError: Construction failed: Failed(registry=org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry@14ca88bc); di… |
| 117 | compiler/frontend | MacroArtifactLocatorTest | locatesAndResolvesFixedSdkStdMacroArtifact() | org.opentest4j.AssertionFailedError: Unexpected diagnostics: [MacroConstructionDiagnostic(severity=ERROR, message=Re-exported macro `std.unittest.mock.mockmacro… |
| 118 | compiler/frontend | MacroConstructionArchitectureGuardTest | rawBuildersCoverAllMacroSurfaceShapes() | org.opentest4j.AssertionFailedError: PSI and LightTree raw builders must not diverge in macro surface coverage: PSI raw builder: missing explicit IfAvailableSur… |
| 119 | lsp | LspCompletionResolveTest | resolveToleratesExpiredSession() | java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Internal error. |
| 120 | lsp | LspCompletionResolveTest | resolveLeavesItemUnchangedWhenNoCDoc() | java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Internal error. |
| 121 | lsp | LspCompletionResolveTest | resolveFillsDocumentationForDocumentedCandidate() | java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Internal error. |
| 122 | lsp | LspCompletionStatisticsTest | resolveRecordsItsOwnStage() | java.util.concurrent.ExecutionException: org.eclipse.lsp4j.jsonrpc.ResponseErrorException: Internal error. |
| 123 | psi | ForeignAndAnnotationParsingTest | platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade() | junit.framework.AssertionFailedError |
