# Batch 5 规范测试用例（runner 接线仍待最后一英里）

本目录的 5 个 `.cj` 文件是 `PLAN.md §12 Batch 5` 所要求的"每项至少一条独立 testdata"
的**规范层**载体——alias conflict / lib path / executor ABI / `@!` forced / plain-attr
overload 各 1 条。

目前 cfir/analysis-tests 已经接入了：

- `tests/test-infrastructure/.../MacroConstructionDirectives` 加入 `MACRO_DEFINITION`
  directive，由 `MacroDefinitionSpec.parse` 解析为结构化的 macro 定义。
- `cfir/analysis-tests/.../services/MacroConstructionEnvironmentConfigurator` 把
  `MACRO_EXECUTOR` / `MACRO_DEFINITION` / `EXPECT_DEGRADED` directive 翻译成
  `CompilerConfiguration` 上的 `macroExecutorFactory` / `macroArtifactDefinitionsOverride`
  / `macroConstructionMode`。
- `compiler/frontend/.../MacroExpandPhase` 新加 `MACRO_ARTIFACT_DEFINITIONS_OVERRIDE`
  和 `MACRO_CONSTRUCTION_MODE` 两个 `CompilerConfigurationKey`，
  `CfirFrontendPipelinePhase` 在主流程中读取它们；
- `AbstractCfirPhasedDiagnosticTest.useConfigurators(::MacroConstructionEnvironmentConfigurator)`
  注册新 configurator；
- testdata 的 `// MACRO_DEFINITION:` 头部使用无空格逗号分隔的 key=value 形式，
  与 `RegisteredDirectivesParser` 的 `[,]?[ \t]+` 切分规则兼容。

## 仍未打通的一段：construction 诊断 → inline marker

`MacroConstructionDiagnosticCollectorComponent` 在 ordinary checker 阶段会把 registry
里的 `MacroConstructionDiagnostic` 通过 `PendingDiagnosticReporter.reportOn` 上报到
`CfirErrors.MACRO_*` 工厂，但 `diagnostics2` 的 inline-marker 框架（依赖
`assertEqualsToFile` 比较预期源码与重建源码）目前并没有把这条诊断映射回 macro 调用
位点的 `<!MACRO_*!>` 标记。实测 5 条 testdata 时，runner 只看到 ordinary checker 的
`<!RETURN_TYPE_MISMATCH!>return<!>`，看不到 `<!MACRO_EXECUTOR_UNAVAILABLE!>` 等
construction-phase 标记。

可能的根因（按概率排序）：

1. `GlobalMetadataInfoHandler.compareAllMetaDataInfos` 用的 diagnostic stream 没有
   读 `MacroConstructionDiagnosticCollectorComponent` 写入的诊断；
2. surface 的 `sourceRange.source` 与 inline-marker 的位置算法不兼容；
3. `MacroConstructionResult.Failed` 路径（alias_conflict 落在这里）下，源码根本
   不写入 source provider，inline-marker handler 看不到对应 file 的诊断。

## 程序级覆盖

Batch 5 五个场景由 `compiler/frontend/test/...FrontendMacroConstructionExecutionTest.kt`
的 `batch5_*` 用例在程序 API 层完整覆盖并通过（含 `MacroResolutionContext`、
`MacroDefinitionEntry`、`FrontendMacroConstructionService`），所以 baseline §14
验收并未因为 inline-marker 不通而退化。

## 把 testdata 接进 runner 的剩余工作

完成上面"未打通的一段"——可以通过：

- 让 `MacroConstructionDiagnosticCollectorComponent` 在 visitFile 之前 / 之外提交
  registry 内非 surface 绑定的 source 诊断（如 alias conflict 是 import 位点）；
- 验证 `PendingDiagnosticReporter` 写入的诊断会进入 `GlobalMetadataInfoHandler` 比对
  的 stream（参考 ordinary diagnostics2 testdata 的写入路径）；
- 对 `MacroConstructionResult.Failed` 路径，在 facade/processError 流程里把 registry
  诊断 lift 到 frontendOutput 上对应 CFIR file 的 source 元素。

完成后，把这 5 个 `.cj` 文件移回 `testData/diagnostics2/macro/` 即可被
`TestGeneratorForCfirAnalysisTests` 自动生成对应 `Macro` inner class（生成器入口已存在；
本次提交把它从 `tests-gen` 中也恢复了）。
