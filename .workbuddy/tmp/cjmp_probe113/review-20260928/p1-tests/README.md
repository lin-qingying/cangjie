# P1 复现草稿（未执行）

本目录不在 Gradle source set / testData 中。没有修改生产文件、正式 fixture 或生成器，没有运行 Gradle。

## 官方依据

- CangjieSemanticsAuthority 已核对 v1.1.3：`TypeChecker.cpp:2027–2047` 在函数体前配对；`CheckCJMP.cpp:948–963` 将 common 默认参数的 assignment/desugarDecl 复制到 specific 并置 HAS_INITIAL。
- 返回类型边界：`TypeManager.cpp:996–1003` 接受 Quest、拒绝错误类型；`CheckCJMP.cpp:535–569` 在后验阶段检查 specific 返回类型是 common 返回类型的子类型。
- docs MCP 的函数定义规则：省略返回类型时，末项调用的类型决定函数返回类型。本目录两个 A1 场景均应推断 Int64。
- 已有官方探针目录内没有 `use3()/use4()` 的运行证据。这两个草稿的期望来自上述源码和文档核对，不能登记成“本形状已由 cjc 实测”。需要新增 cjc 记录时，将模块拆成独立 common/specific 源文件并分离输出目录。

## 文件与验收点

| 草稿 | 正式目标 | 验收 |
|---|---|---|
| `cjmpDefaultReadThroughBeforeImplicitTypes.cj` | `cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/` | 省略返回类型的 use3 调用无错误；默认参数可读穿 |
| `cjmpSpecificShadowingBeforeImplicitTypes.cj` | 同上 | 省略返回类型的 use4 不产生 common/specific 歧义 |
| `defaultArgumentCallerFirst.cj` | `analysis/low-level-api-cfir/testData/diagnostics/cjmpCallerFirst/` | caller-first 文件诊断无错误且 call 符号必须是 callee.cj 的 specific a |
| `shadowedCommonCallerFirst.cj` | 同上 | caller-first 文件诊断无错误且 call 符号必须是 callee.cj 的 specific platform |
| `CfirCjmpCallerFirstDiagnosticsTest.kt.draft` | `analysis/low-level-api-cfir/test/.../diagnostic/CfirCjmpCallerFirstDiagnosticsTest.kt` | 手写 JUnit 用例，无需新增生成器；第一语义入口固定为 collectDiagnosticsForFile(caller) |

A1 的两个纯诊断 fixture 不能独自证明调用目标正确：默认值缺失时，错误实现可能静默选中 common 并仍保持零诊断。A2 的 identity 断言补齐这一盲点。若 P1 需要逐条证明 A1 的推断调用目标，应再以相同 LL test 的方式增加两个省略返回类型变体，或在现有 frontend 两段式测试中读取已解析 call 的 symbol；不要靠新增无意义诊断标记制造失败。

## A2 不预热约束

草稿只在准备阶段调用 `getOrBuildCfirFile` 获取 raw 声明与 session，并在首个诊断请求前断言 callee 尚未到 CJMP_MATCHING 且 storage 为空。不得在该断言前调用 callee 的 `lazyResolveToPhase`、`getOrBuildCfir`、`resolveToCfirSymbol`，不得先收集 callee/common 文件诊断，也不能以 eager transformer 代替真实 LL 请求。

当前 LL 测试没有生产 CJMP 模式入口（计划 G1/P7），故草稿明确使用 `CJMP_MODE` 指令准备 session；这不能当作 G1 已修复。正式合入时优先复用主线程统一的测试配置 owner。如 P1 暂保留该准备代码，它不得包含语义解析；P7 应删除注入代码并用相同 caller-first 测试验证生产装配。

模式设置之后第一次 `collectDiagnosticsForFile` 必须通过真实 LL FileStructureElement 路径。Kotlin 框架参照：`external/kotlin/analysis/low-level-api-fir/testFixtures/.../AbstractGetOrBuildFirTest.kt` 按请求元素顺序进入 facade；本仓可复用 `SourcePostSemaDiagnosticsTest.kt` / `AbstractAnalysisApiExecutionTest` 的真实诊断入口与独立用例生命周期。

## 快照工具

工具：`D:/code/intellij/cangjie/tools/verification/cfir-test-result-ledger.ps1`。

参数：必填 `-SnapshotName`，可选 `-CompareTo`（已有快照目录，内部读取 cases.json）。输入固定为 `cfir/analysis-tests/build/test-results/test/*.xml`，输出到 `cfir/analysis-tests/build/ffi-annotation-verification/<SnapshotName>/`，包含 results.zip、cases.json、summary.json，比较时另有 comparison.json。已存在的快照名会报错，不能覆盖。

应在全量运行结束且下一次定向测试覆盖 XML 前生成。P10 比较目标为 `cfir/analysis-tests/build/ffi-annotation-verification/20260928-cjmp-final-full-v10`；当前开工现状参考还包括 `import-group-20260929-final`。工具统计所有 testcase-key，本身不另列 PSI/LightTree 统计；需要按 cases.json 的 classname 分类补充报告。
