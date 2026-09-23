# LanguageVersion / ApiVersion 官方版本覆盖缺口分析（2026-09-22）

## 0. 官方版本史（以 `external/cangjie_compiler` 全部 47 个 tag 为准）

| 版本 | 类型 | tag | 前端诊断增量（相对上一稳定版） |
|---|---|---|---|
| 1.0.0 | 稳定 | `v1.0.0` | 基线 812 |
| 1.0.2 | 稳定 | `v1.0.2` | +16 / −19 |
| 1.0.3-beta | 预发布 | `v1.0.3-beta` | +1（`sema_apilevel_missing_arg`） |
| 1.0.5 | 稳定 | `v1.0.5` | +2（driver）/ −12（effect handler 撤回） |
| 1.1.0 | 稳定 | `v1.1.0`（另有 22 个 alpha/beta tag） | +210 / −1 |
| 1.1.1 | 稳定 | `v1.1.1` | 0 |
| 1.1.2 | 稳定 | `v1.1.2` | +1（`driver_cfg_apilevel_too_low`，driver 侧） |
| 1.1.3 | 稳定 | `v1.1.3` | 0 |
| 1.2.0 | 开发中 | `v1.2.0-alpha.06` → `v1.2.0-beta.rc2`（12 个 tag） | 语义层少量新增 |
| 1.3.0 | 开发中 | `v1.3.0-alpha.02` → `v1.3.0-alpha.05` | — |

## 1. LanguageVersion 缺口

现状（[LanguageVersionSettings.kt](../../../common/src/org/cangnova/cangjie/LanguageVersionSettings.kt#L64-68)）：
`CANGJIE_1_0_0`、`CANGJIE_1_0_2`、`CANGJIE_1_0_5`、`CANGJIE_1_1_0`、`CANGJIE_1_1_3`；`LATEST_STABLE = 1_1_3`。

| 官方版本 | 状态 | 影响 |
|---|---|---|
| **1.1.1、1.1.2** | **缺失（官方稳定版）** | `LanguageVersion.parse("1.1.1"/"1.1.2")` 抛 `IllegalStateException`。两个真实消费方均受影响：CLI 配置 [LanguageVersionSettingsConfigurator.kt](../../../compiler/config/src/org/cangnova/cangjie/config/LanguageVersionSettingsConfigurator.kt#L21) 与测试构建器 [LanguageVersionSettingsBuilder.kt](../../../tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/builders/LanguageVersionSettingsBuilder.kt#L82)。即用户代码若以 cjc 1.1.1/1.1.2 为目标版本，本仓库无法表达 |
| 1.0.3-beta | 缺失（官方预发布） | `parse("1.0.3-beta")` 失败；且现 `parse()` 无预发布后缀解析（`"3-beta"` → `toIntOrNull` 失败 → patch=0 → 查找失败报错，不会静默错配，但也不可解析）。枚举已有 `preReleaseTag` 字段但零使用 |
| 1.2.0 / 1.3.0 | 缺失（官方未发布，开发中） | 暂缓合理；`1.2.0` 已到 beta.rc2，接近发布 |

**语义影响评估**：`sinceVersion` 门禁全部走 `>=` 比较（如 `JavaInteropAnnotations >= 1_1_0`），1.1.1/1.1.2 落在 1_1_0 与 1_1_3 之间且官方前端诊断零增量（仅 1.1.2 一个 driver 诊断），因此补条目**不改变任何现有门禁语义**，纯粹补齐版本解析覆盖与官方发布史一致性。

## 2. ApiVersion 缺口（以标准库 `cangjie_runtime` 版本史为基准）

> 语义澄清：`ApiVersion` 的 KDoc 定义为**标准库 API 版本**（"语言版本表达语义，API 版本表达允许引用的标准库表面"），其官方基准是标准库仓库 `external/cangjie_runtime` 的版本史，而非 compiler tag 史。两仓 tag 集**不同步**。

### 2.1 标准库官方版本史（`cangjie_runtime` tag 全量，截至 v1.3.0-alpha.05，HEAD 已越过）

| 系列 | 版本 |
|---|---|
| 1.0.x | 1.0.0、**1.0.1**、1.0.2、1.0.3-beta、~~1.0.3-cjmp-beta~~（common 变体 tag）、1.0.5、~~1.0.5.bep~~（紧急补丁变体 tag）、**1.0.6** |
| 1.1.x | 1.1.0、1.1.1、1.1.2、1.1.3（alpha/beta tag 与 compiler 镜像互有出入） |
| 1.2.x | **1.2.0（正式 tag，compiler 镜像仅到 beta.rc2）** |
| 1.3.x | 1.3.0-alpha.02 ~ alpha.05 |

注意：compiler 镜像**没有** `v1.0.1`、`v1.0.6`、`v1.2.0` 对应 tag。无法仅凭本地镜像确证这三个是"对外 SDK 整包版本"还是 stdlib 侧单独发版（也可能 compiler 镜像不全）；登记待官方 SDK 发布记录确认。

### 2.2 ApiVersion 常量缺口

现状（[ApiVersion.kt](../../../common/src/org/cangnova/cangjie/config/ApiVersion.kt#L63-86)）：命名常量仅 `CANGJIE_1_0_0`、`CANGJIE_1_0_5`、`CANGJIE_1_1_0`、`CANGJIE_1_1_3`。

| 标准库版本 | ApiVersion 现状 | 说明 |
|---|---|---|
| 1.0.2 | 常量缺失（运行时可 parse） | 两仓共有稳定版；`parse("1.0.2")` 间接走 `LanguageVersion.parse` 可成功，但 `sinceApiVersion` 门禁无法以常量引用 |
| 1.1.1、1.1.2 | 常量缺失（parse 失败） | 两仓共有稳定版；根因在 `LanguageVersion` 枚举缺条目 |
| 1.0.1、1.0.6 | **完全无法表达** | runtime 独有版本，`LanguageVersion` 枚举无（且不该有——它们不是语言版本），当前实现下 `parse` 亦失败。是否需要取决于其是否为对外 SDK 版本（待确认） |
| 1.0.3-beta | 完全无法表达 | 两仓共有预发布版；`parse()` 无预发布后缀解析 |
| 1.2.0 | **完全无法表达** | runtime 已正式发版；本仓库编译 stdlib（`stdlibCompilation` 模式）若目标为 1.2.0 表面，无法声明 |

### 2.3 定位裁定：ApiVersion 到底承担什么（对比 Kotlin / 官方 cjc / 本项目）

| 维度 | Kotlin | 官方 cjc (cpp) | 本项目 |
|---|---|---|---|
| CLI 入口 | `-api-version X` | **无**（`Options.inc` 无 api-version/language-version；唯一"API 轴"是 `apilevel-check`，即 OpenHarmony API level 系统能力轴，`CheckAPILevel.cpp`，语义完全不同） | `-api-version`（`LanguageVersionSettingsConfigurator`） |
| 声明级标注 | `@SinceKotlin("1.x")` → 编译器按 apiVersion 过滤 stdlib/库表面（旧版本下新 API 不可见） | **无**（stdlib 源码仅有 `@since 0.x.y` 文档注释，指向 0.x 旧内部版本线，编译器不消费） | **无**（无 @SinceKotlin 等价物，cjo 序列化无 API 版本轴） |
| 元数据 | KLIB 记录编译时 ApiVersion | 无 | cjo 序列化无此字段 |
| 语言特性门禁 | `LanguageFeature.sinceApiVersion` | 不适用（单轴 SDK 版本模型） | 同名机制存在，非默认值仅 5 个 feature（ApiLevelSinceParameter=1.0.5、JavaInterop/ObjC/ForeignName/PackageProductMetadata=1.1.0） |
| 诊断 | UNSUPPORTED_API_VERSION 等 | 无 | `LANGUAGE_FEATURE_SUPPORT` 渲染 "only available since API version X" |

**结论**：ApiVersion 在 Kotlin 中的核心职责（声明级 API 表面过滤 + 元数据版本轴）在官方 cjc 生态**不存在对应机制**；本项目继承 Kotlin 布局后，ApiVersion 的**全部真实职责**收窄为三件：① `LanguageFeature.sinceApiVersion` 门禁的 API 分量（5 个互操作/平台类 feature）；② CLI/测试的配置轴入口；③ 诊断文案渲染。它是**无官方对标的预留设计**。

### 2.4 定性结论：ApiVersion 不应作为独立配置轴存在（第一性原理裁定）

单轴事实链：官方 cjc 无 api-version 配置空间（§2.3）→ 仓颉生态无 `@SinceKotlin` 式声明标注 → cjo 序列化无 API 版本字段 → 本仓库也没有任何"按 API 版本过滤 stdlib 表面"的机制。因此：

1. **`sinceApiVersion` 门禁是冗余自由度**。合法配置中 apiVersion 与 languageVersion 同源同值（同一个 SDK 版本），只要 `languageVersion >= sinceVersion` 就必然 `apiVersion >= sinceApiVersion`；`UNSUPPORTED_API_VERSION` 分支只可能被"languageVersion ≥ X 且 apiVersion < X"的组合触发——该组合在官方模型中不存在合法来源，是本仓库自造的配置空间。
2. **CLI `-api-version` 是假语义**。用户设置 apiVersion=1.0.5 后不会有任何 stdlib 声明被过滤（无标注体系可过滤），唯一"效果"是误伤 5 个互操作 feature 的门禁——一个只有副作用的配置项。
3. **KDoc"独立演进"自述不成立**。官方 stdlib 与编译器同 SDK 发布，对用户只有一个版本号；"IDE 分析旧 SDK"场景由 languageVersion 表达即可（旧 SDK 版本 = 旧语言版本），不需要第二根轴。
4. Kotlin 的 ApiVersion 存在的前提（声明级标注 + 库元数据 + 库生态兼容承诺）在仓颉生态**整体缺失**——本项目继承 Kotlin 布局带入的是**赘生物**，"保留但冻结"是兼容性妥协而非正确结论。

**正确形态**：删除 ApiVersion 独立轴——`featureSupportStatus` 简化为纯 `sinceVersion` 判断，`LanguageFeature` 去掉 `sinceApiVersion` 参数，`LanguageVersionSettings` 接口去掉 `apiVersion` 属性，CLI 参数与测试指令删除，`ApiVersion.kt` 整类移除（`EffectHandlers` KDoc 中"毕业时钉 sinceApiVersion"的预案一并作废）。

**落地面（已核实，影响极小）**：全仓仅 13 个文件引用（生产 6：`LanguageVersionSettings.kt`、`ApiVersion.kt`、`LanguageVersionSettingsConfigurator.kt`、`AbstractFrontendPipeline.kt`、`CommonCompilerArguments.kt`(gen)+参数生成器、`CfirDiagnosticRenderers.kt`；测试 4；builder 1），无 deveco/ 或其它下游依赖。`LANGUAGE_FEATURE_SUPPORT` 渲染器去掉 API 分句即可。

## 3. 建议

1. **P1**：`LanguageVersion` 增加 `CANGJIE_1_1_1(1, 1, 1)`、`CANGJIE_1_1_2(1, 1, 2)`（插在 `CANGJIE_1_1_0` 与 `CANGJIE_1_1_3` 之间，枚举序即版本序）；`LATEST_STABLE` 保持钉 1_1_3 不变。
2. **P2**：`LanguageVersion` 增加预发布条目 `CANGJIE_1_0_3(1, 0, 3, preReleaseTag = "beta")`；同时为 `parse()` 增加预发布后缀解析（`1.0.3-beta`），否则该条目无法从字符串到达。
3. **P1（结构性，替代此前"补常量/解耦"两案）**：删除 ApiVersion 独立轴（清单见 §2.4），一次性消除假配置空间与冗余门禁分支；删除后 ApiVersion 相关的"补 1.0.2/1.1.1/1.1.2 常量""解耦 stdlib 版本空间"等议题全部消失。
4. **暂缓**：`LanguageVersion` 侧 1.2.0/1.3.0 条目待官方正式发布、且本仓库开始实现其语义（诊断 delta 见 v1.2.0 前瞻清单）后再加入；加入时须复核 `LATEST_STABLE`/`isStable` 关系。
5. **测试**：[LanguageVersionSettingsTest.kt](../../../common/test/org/cangnova/cangjie/LanguageVersionSettingsTest.kt#L24-27) 补 `parse("1.1.1")`/`parse("1.1.2")`（及若采纳 P2 的 `parse("1.0.3-beta")`）用例；现有测试仅覆盖 1.0.5 与非法格式；同步删除 apiVersion 相关断言/指令。

## 4. 与诊断版本分析的关系

- 1.0.3-beta 的唯一语义诊断 `sema_apilevel_missing_arg` 已存在于 CFIR（`CFIR_APILEVEL_MISSING_ARG`，门禁缺口见 [diagnostic-gap-and-dead-analysis-20260922.md](diagnostic-gap-and-dead-analysis-20260922.md) §2.1）；若采纳 P2 条目，该诊断的门禁可精确钉到 1.0.3-beta 而非现在的隐式 1.0.5。
- 1.1.1/1.1.2/1.1.3 前端零增量，故不存在"需要以 1.1.2 为 sinceVersion 的门禁"——补条目仅为版本表达完整性。
