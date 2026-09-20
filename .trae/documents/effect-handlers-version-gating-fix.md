# 修复 EffectHandlers 版本门控（对齐官方实验语义）

## Context

`LanguageFeature.EffectHandlers` 当前钉在 `sinceVersion = CANGJIE_1_0_0`（44faa9dd3，2026-08-16 钉入），而版本枚举最低就是 1.0.0、`FIRST_SUPPORTED = 1.0.0`，导致 `isEnabledByDefault`（[LanguageVersionSettings.kt#L600](file:///d:/code/intellij/cangjie/common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L600)）恒真——feature 在任何配置下默认开启，resolve 层 3 处 `requireFeatureSupport` 门禁（perform/resume/handle）与 `EFFECTS_FEATURE_DISABLED` 诊断实际不可达。

官方将 effect handlers 定位为**实验性 opt-in 特性**（官方测试 directive `%enableEH`；OCX 2026 公开介绍；已开源镜像无实现）。仓库已有完全对应的先例：`AllowIntersectionTypesInInference(null)`（[LanguageVersionSettings.kt#L212](file:///d:/code/intellij/cangjie/common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L212)，KDoc 明确"默认全版本关闭、对齐官方 cjc"）。2026-04-08 时 `EffectHandlers` 正是无 sinceVersion 形态；钉到 1.0.0 的翻转导致 `effectsFeatureDisabledRich.cj` 期望不可达、被清空成 0 字节空壳测试。

**用户已确认的决策**：

* 默认语义：恢复实验形态（sinceVersion=null，默认关，显式 opt-in）——对齐官方与仓库先例

* CLI 特性开关通道：**不加**，保持最小范围（先例同样无 CLI 通道）

**影响面（已核实）**：`EffectHandlers` 仅 3 处 resolve 门禁消费（[CfirExpressionsResolveTransformer.kt#L3707、L3736、L5467](file:///d:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt#L3707)），parser/checker/CFG/LSP 均无消费；testdata 中仅 4 个文件涉及 effect 语法/诊断。

## 变更清单

### 1. 核心：恢复实验形态（1 处）

[common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L159](file:///d:/code/intellij/cangjie/common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L159)

```kotlin
// 现状
EffectHandlers(LanguageVersion.CANGJIE_1_0_0),
// 改为（KDoc 见下方模板）
EffectHandlers(),
```

KDoc 模板（回答"从哪个版本开始支持"——当前**没有任何官方发布版本包含该特性**，故 sinceVersion 为 null）：

```kotlin
/**
 * Effect handlers（`perform`/`resume`/`handle`/`throwing` 语法与 `Command<T>` 语义）。
 *
 * 实验特性，默认全版本关闭（sinceVersion = null）：官方将 effect handlers 定位为
 * 实验性 opt-in 特性（官方测试体系以 `%enableEH` 显式开启；截至 OCX 2026 公开介绍时
 * 官方仍标注"积极开发中的实验性部分"，**没有任何已发布官方编译器版本包含它**，
 * 已开源镜像亦无实现）。关闭时 perform/resume/handle 在 resolve 层报
 * EFFECTS_FEATURE_DISABLED；语法层因 perform/resume/throwing 为硬关键字仍可解析。
 *
 * 毕业条件（满足后才钉 sinceVersion，对齐 JavaInteropAnnotations 形态）：
 * ① 官方规范/发布说明将 effect handlers 列入稳定语言表面；② 本仓库 stdlib
 * 提供 stdx.effect 包；③ perform→handle→resume 端到端 LLT 通过。毕业时必须
 * 同时钉 sinceVersion 与 sinceApiVersion（Command/Resumption 是 stdlib 类型，
 * API 版本不得留默认 1.0.0），并以 CanStillBeDisabledForNow 设过渡窗。
 */
EffectHandlers(),
```

机制无需其他改动：`featureSupportStatus`（L478-484）已处理 `sinceVersion == null` → 无显式状态时返回 `EXPERIMENTAL`、显式 DISABLED 返回 `DISABLED`；显式 ENABLED 优先返回 `SUPPORTED`（L460-462）；`init` 校验（L220-224）不受影响。

### 1b. 特性生命周期与毕业路径（未来如何"从某个版本开始支持"）

本次把 feature 归位到实验期；未来官方毕业时按下述程序升格（全部先例已在本文件内）：

**毕业触发条件**（三条同时满足才允许钉版本）：

1. 官方把 effect handlers 列入稳定语言表面——以官方语言规范章节或发布说明为准（不再是"实验性"表述）
2. 本仓库 stdlib 提供 `stdx.effect` 包（`Command`/`Resumption` 可解析）——否则开启后 `import` 必然失败，默认启用毫无意义
3. 端到端 LLT（perform → handle → resume，含正向与负向用例）通过

**毕业时的改动形态**（唯一先例：[JavaInteropAnnotations L176-180](file:///d:/code/intellij/cangjie/common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L176-L180)）：

```kotlin
EffectHandlers(
    LanguageVersion.CANGJIE_X_Y_Z,   // 官方首次纳入稳定表面的版本（X.Y.Z 以官方发布为准，本计划现在不预设）
    ApiVersion.CANGJIE_X_Y_Z,        // stdx.effect 随 stdlib 提供的 API 版本——必须与语言版本一起钉，不得留默认 1.0.0
    // Command/Resumption 是 stdlib 类型，API 版本不钉会导致旧 API 版本下类型不可解析却默认启用
    behaviorAfterSinceVersion =
        LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED),
    // 过渡窗内允许 -EffectHandlers 显式关闭；收口后改 CannotBeDisabled（填关联工单号）
),
```

**毕业后的门控行为**（自动生效，无需改 resolve 门禁代码）：

* `languageVersion >= X.Y.Z` → `isEnabledByDefault` 为 true，默认启用；`behaviorAfterSinceVersion` 决定过渡窗内能否显式关闭

* `languageVersion < X.Y.Z` → `featureSupportStatus` 在 L468-471 返回 `UNSUPPORTED_LANGUAGE_VERSION`，`EFFECTS_FEATURE_DISABLED` 诊断保留"版本不支持"作为主原因（该路径本就是为此设计的）

* 测试数据相应调整：毕业版本下的 effect fixture 移除 `// LANGUAGE: +EffectHandlers`（默认已开）；effectsFeatureDisabledRich 改为 `// LANGUAGE_VERSION:` 指向早于 X.Y.Z 的版本、或过渡窗内用 `// LANGUAGE: -EffectHandlers`

**本次 KDoc 中应预留指向**：在 KDoc 末尾写明"毕业操作与触发条件"一句话注释，避免未来只改版本号、漏掉 sinceApiVersion 与过渡窗配套。

### 1c. Kotlin 对位（external/kotlin 实证）

Kotlin 对"实验性支持"与"正式默认支持"的建模（[LanguageVersionSettings.kt](file:///d:/code/intellij/cangjie/external/kotlin/compiler/util/src/org/jetbrains/kotlin/config/LanguageVersionSettings.kt)）——**只有两态，没有"自 X 版本起可用但默认实验"的中间态**：

| 阶段     | Kotlin 表达                                                                                                                     | 语义                                                                                                                                                                                                                                     |
| ------ | ----------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 实验期    | `sinceVersion = null`（枚举 L562 起整段注释 `// Experimental features`：ExplicitBackingFields、LocalTypeAliases 等）                      | `isEnabledByDefault`（L846-847）恒 false → 默认 DISABLED；必须 `-XXLanguage:+Feature` 显式 opt-in；"no stability guarantees"                                                                                                                      |
| 正式默认支持 | `sinceVersion = KotlinVersion.X_Y`（KDoc L19-49："determines in which Language Version the feature becomes enabled by default"） | 该语言版本起自动 ENABLED，更早版本 `UNSUPPORTED_FEATURE`（ERROR，[FirErrors.kt L158](file:///d:/code/intellij/cangjie/external/kotlin/compiler/frontend/common-psi/src/org/jetbrains/kotlin/diagnostics/rendering/LanguageFeatureMessageRenderer.kt)） |

* **实验→稳定迁移**：把 null 改为具体版本。若语义有破坏性变化，按 `Coroutines(KOTLIN_1_1)` → `ReleaseCoroutines(KOTLIN_1_3)` 模式（L83/L113）**新开条目**而非改旧条目版本号

* **progressive 提前启用**：`enabledInProgressiveMode = true` 且 `sinceVersion != null` 的特性可在 sinceVersion 之前的版本上以 `-progressive` 提前生效（`CommonCompilerArgumentsConfigurator.kt` L74-80 直接 put 进 specificFeatures，绕过版本检查）；1.3 起大量 `Prohibit*(KOTLIN_1_3, enabledInProgressiveMode = true)` 是标准部署形态

* **二进制标记**：`forcesPreReleaseBinariesIfEnabled()`（L829-832）——`sinceVersion?.isStable != true`（含 null）时显式启用即强制 pre-release 二进制；实验特性先例 `LocalTypeAliases(sinceVersion = null, forcesPreReleaseBinaries = true)`

* **本仓库模型的对位状态**：`LanguageFeature`/`LanguageVersionSettingsImpl`/`isEnabledByDefault` 均为 Kotlin 同构移植；差异仅两点——① 本仓库 `LanguageFeatureSupportStatus` 额外有 `EXPERIMENTAL` 枚举值（Kotlin State 只有 ENABLED/DISABLED），仅用于诊断原因分层，布尔门禁 `requireFeatureSupport` 语义与 Kotlin 完全一致；② 本仓库 CLI 无 `-XXLanguage` 通道（用户已确认本计划不加）

结论：本计划的"null → 未来钉版本"两阶段设计与 Kotlin 生命周期完全同构，无结构性偏差。

### 2. 测试 fixture（4 个）

**加 directive**（语法 `// LANGUAGE: +FeatureName`，解析见 [LanguageVersionSettingsBuilder.kt#L112-126](file:///d:/code/intellij/cangjie/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/builders/LanguageVersionSettingsBuilder.kt#L112-L126)，先例 [plus.cj](file:///d:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics/operator/plus.cj) 首行）：

* [cfir/analysis-tests/testData/llt/effect/perform\_incorrect\_type.cj](file:///d:/code/intellij/cangjie/cfir/analysis-tests/testData/llt/effect/perform_incorrect_type.cj) — 首行加 `// LANGUAGE: +EffectHandlers`

* [cfir/analysis-tests/testData/llt/effect/command\_class\_not\_available.cj](file:///d:/code/intellij/cangjie/cfir/analysis-tests/testData/llt/effect/command_class_not_available.cj) — 同上

* [cfir/analysis-tests/testData/llt/effect/resume\_outside\_handle.cj](file:///d:/code/intellij/cangjie/cfir/analysis-tests/testData/llt/effect/resume_outside_handle.cj) — 同上

**恢复被清空的 fixture**（不加 directive，默认关 → 走 disabled 路径）：

* [cfir/analysis-tests/testData/diagnostics/effects/effectsFeatureDisabledRich.cj](file:///d:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics/effects/effectsFeatureDisabledRich.cj) — 恢复 git 历史原内容（`ade0381e1`/`1bfa0ed98` 版本：3 个函数分别测 perform/resume/handle 的 `EFFECTS_FEATURE_DISABLED`）

唯一不确定点：该 fixture 期望写于 2026-04，此后 resolve 代码有变化。若恢复后出现**预期外的级联诊断**，逐个按证据分析；handle disabled 分支已保留 delegatedType 防级联（L5469-5475），perform/resume disabled 分支直接替换类型为 `ConeErrorType(ConeEffectsFeatureDisabledError)`，需确认函数返回类型检查不级联。若有级联，按 `ConeUnreportedDuplicateDiagnostic` 模式（同文件 `resolveCatchPatternType` L6431-6433 先例）补抑制——仍属门控问题族。

### 3. 明确不在本计划范围

* **perform\_incorrect\_type 的** **`COMMAND_INCOMPATIBLE_TYPE`** **级联**：独立问题族（`transformPerformExpression` L3714-3720 对错误类型操作数无短路；官方测试数据只期望 `sema_invalid_binary_expr` 一个诊断）。修复后该 fixture 在加 directive 前提下仍会 FAIL，属预期

* **恢复 23 个已删除的官方移植 fixture**：依赖 `stdx.effect` stdlib 缺失问题的解决（独立基建问题）

* **CLI 特性开关通道**：用户已确认不加

## 验证（仓库规则：多进程并发一律用 gradlew-queue.bat）

1. 定向（PowerShell 单引号防 `$` 展开）：

```powershell
.\gradlew-queue.bat :cfir:analysis-tests:test --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Effect' --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTPsiTestGenerated$Effect' --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsTestGenerated$Effects' --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsPsiTestGenerated$Effects' --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsWithoutAliasExpansionTestGenerated$Effects'
```

预期：`effectsFeatureDisabledRich` ×3 与 `testCommandClassNotAvailable`/`testResumeOutsideHandle` ×2 路径 PASS；`testPerformIncorrectType` ×2 路径仍 FAIL（级联 bug，范围外，失败内容应与现在一致）。

1. 全量回归：`.\gradlew-queue.bat :cfir:analysis-tests:test`，对比失败集合不含**新增**失败（从 XML 判定，不依赖 HTML/控制台截断输出）。

2. 按 cangjie-cfir-llt-repair 技能要求，向 [cfir/analysis-tests/REPAIR\_LOG.md](file:///d:/code/intellij/cangjie/cfir/analysis-tests/REPAIR_LOG.md) 追加本问题族条目（problem type / root cause / official evidence：官方 `%enableEH` + OCX 2026 报道 + TokenKind 文档 / Kotlin 对位：Kotlin FIR "parse everything, gate in resolution" / 修复原则 / fixtures covered / 验证命令与结果）。

## 可选对位项（默认不做，需用户在批准时明示）

1. **附带修复 perform 级联**：`transformPerformExpression` 操作数类型为 `ConeErrorType` 时传播 `ConeUnreportedDuplicateDiagnostic`（非报告错误），使 effect 测试切片全绿。改动约 5 行，有同文件先例。
2. **实验期二进制标记**：给 `EffectHandlers()` 加 `forcesPreReleaseBinaries = true`（Kotlin 实验特性先例 `LocalTypeAliases(sinceVersion = null, forcesPreReleaseBinaries = true)`）——显式开启后编译产物标记 pre-release。本仓库 `isPreRelease()` 管线已支持（LanguageVersionSettings.kt L561-564），但 effect 系统连端到端编译都不可用（stdlib 缺失），标记暂无实际意义。
3. **诊断消息双分支渲染**：当前 `EFFECTS_FEATURE_DISABLED` 消息固定为 "effects feature is disabled for '{0}'."（[CfirErrorsDefaultMessages.kt#L558-562](file:///d:/code/intellij/cangjie/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt#L558-L562)）。Kotlin 按原因区分：`since==null` → "experimental and should be enabled explicitly... no stability guarantees"；`since>languageVersion` → "only available since language version X"（LanguageFeatureMessageRenderer.kt L43-46）。实验期把消息改为实验措辞即可（消息文本不影响 inline fixture 期望，诊断名不变）；完整的双分支渲染等毕业时再建。

