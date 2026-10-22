# 编译器实现度分析（2026-10-18）

本文从"整个编译器"的角度盘点当前实现：各管线阶段、各子系统的完成度、主要缺口与收尾优先级。模块清单以 `settings.gradle.kts` 为准；阶段划分沿用 `cjfir-compiler-stages.md` 与 `CLAUDE.md` 的管线定义。

## 0. 评估方法与证据边界

- 证据来源：一方源码统计（文件数/行数）、模块 README 与维护文档（`docs/`）、生成测试清单（`cfir/analysis-tests`）、专项已知问题文档（`analysis/docs/analysis-performance-known-issues.md`）。
- 未执行全量 Gradle 构建/测试，**各测试套件的当前通过率未验证**；本文的"完成度"指实现规模与接缝完整性，不代表测试全绿。
- 评级口径：空壳（无实现）/ 骨架（接缝与少量实现）/ 部分实现（主路径可用，语义覆盖明显不全）/ 基本完整（主路径+测试齐备，剩余为边角）/ 完整。

## 1. 总览

### 1.1 管线阶段实现度矩阵

| 管线阶段 | 实现度 | 承接模块 | 要点 |
|---|---|---|---|
| LOAD_PLUGINS | 空壳 | `:compiler:plugin` | 目录仅有 build.gradle.kts，零源码 |
| PARSE | 基本完整 | `:psi` | 解析约 1.37 万行；缺口见 §2.1 |
| CONDITION_COMPILE | 部分实现 | `:cfir:resolve` | 声明级 `@When` 裁剪可用；表达式级与默认环境缺 |
| IMPORT_PACKAGE | 基本完整 | `:cfir:resolve` | IMPORTS phase + import 冲突/别名诊断 |
| MACRO_EXPAND | 基本完整 | `:macro:*`、`:compiler:frontend`、`:cfir:providers` | 基线 Batch 0–9 ✅、Batch 10 🔶 |
| CFIR_BUILD | 基本完整 | `:cfir:raw-cfir:*` | PSI/LightTree 双路径真实实现 |
| CFIR_RESOLVE | 部分→基本完整 | `:cfir:resolve` | 7 个 phase 均有真实 processor；BODY_RESOLVE 深度见 §2.3 |
| 诊断（CHECKERS） | 部分实现 | `:cfir:checkers` 等 | checker 覆盖 vs 官方诊断目录差距大（§2.4） |
| FINALIZE | 缺失 | — | 无实现、无阶段挂点 |
| MANGLING | 缺失 | — | 全仓无名字修饰逻辑，管线无此阶段 |
| SAVE_CJO | 部分 | `:cfir:cfir-serialization` | 读侧基本完整；写侧仅包级元数据 |

### 1.2 子系统评分

| 子系统 | 评分 | 一句话结论 |
|---|---|---|
| 编译器驱动与语法层 | 72 | PSI 与前端管线真实且质量较高；驱动入口（CLI main、插件加载）与后段（MANGLING/FINALIZE）断链 |
| .cjo 序列化 + 宏 | 76 | 宏与 .cjo 读侧深度接通；写侧止步元数据，MANGLING 缺失 |
| Analysis / LSP / 编辑器服务 | 63 | 接口面高度对齐 K2、LLT 规模大；code-insight 多数能力是骨架，fixes 空壳 |
| CHIR / CodeGen / LLVM 后端 | ~50 | 数据模型与 lowering 有真实实现和测试；离端到端产物仍差多步 |
| **整体（前端为核心加权）** | **约 65** | 前端"读懂仓颉"的能力基本成形；"编译到产物"的闭环（FINALIZE→MANGLING→SAVE_CJO→后端）尚未打通 |

## 2. 前端管线各阶段详述

### 2.1 PARSE（`:psi`）— 基本完整

- 规模：`psi/src` 868 个 kt 文件，其中 parsing 约 1.37 万行。
- 已覆盖：class/interface/enum/struct/extend、prop、泛型、操作符重载、宏调用（专用 parser definition）、pattern match（match/case/deconstruct/enum pattern）、quote、spawn/try/unsafe、string interpolation、cdoc 子系统。
- 缺口（对照 `psi/docs/psi-parser-comparison.md` 与官方解析器）：
  - `inout` 参数传递修饰符未实现；
  - `@!` 编译时可见注解未实现；
  - VArray 常量类型参数、宏参数转义/校验为部分实现；
  - 内置/自定义注解区分与 `@Deprecated` 参数校验不完整；
  - **文档过时**：对比文档仍称 Effect 系统"未实现"，但代码已有 `parsePerform/parseResume/parseHandleClause`。

### 2.2 CONDITION_COMPILE + MACRO_EXPAND

- 条件编译：`CfirRawConditionalCompilationPruner` + `CfirConditionalCompilationEvaluator`，支持按 `@When` 裁剪文件/导入/顶层/成员声明；诊断与 checker 齐备。缺口：条件环境需显式注入才生效，覆盖以声明级为主，表达式级与完整官方语义待补。
- 宏展开（`docs/macro-construction-baseline.md` 冻结基线）：
  - Batch 0–9 全部落地：provider 状态机、MacroSymbolIndex、`MACRO_EXPAND` phase 移除、PreMacro/MacroSurface 模型、PSI+LightTree 双 builder 采集、解析规则、token 路径、宏森林 child-first 求值、fragment parser + stable splice、Registry/诊断/IDE 降级；
  - Batch 10 部分落地：旧 text-patch 语义路径已清除并有守门测试；剩余扩展点为 `MacroReplaceSlot` 的 owner/source/scope/symbol 修复（生产装配已用 `CfirExpressionMacroStableSplicer` / `CangJieBuiltinNonMacroDesugarer`，`Identity*` 仍为占位）；
  - 协议侧完整：`MacroMsgCodec`（FlatBuffers 编解码）、`LspMacroServerMacroExecutor`（进程生命周期、Windows 命名管道、SDK 环境注入）、`StubMacroExecutor`。

### 2.3 CFIR_BUILD + CFIR_RESOLVE

- Raw CFIR 双路径均为真实实现：`PsiRawCfirBuilder.kt` 4102 行；`light-tree2cfir` 7 文件 6701 行（含表达式 builder 2407 行）。
- `:cfir:resolve` 158 个源文件，全部 7 个 ordinary phase（IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS/IMPLICIT_TYPES/BODY_RESOLVE）都有真实 processor/transformer，无空转阶段。
- BODY_RESOLVE 深度（对齐 K2 FIR 的结构）：调用解析（tower resolve、candidate 工厂/收集、重载冲突裁决族）、13+ 个 resolution stage（参数映射、可见性、receiver/extension 检查、新类型变量替换等）、约束推断（`InferenceComponents`/`ConstraintSystemCompleter`/`CfirCallCompleter`）、数据流分析（CFG builder + `CfirDataFlowAnalyzer`）、模式绑定解析（`CfirPatternBindingResolution`）、SAM、lambda 体再推断、cjmp 匹配。
- 缺口：推断组件存在少量 TODO（`resolution.common` 的 `ConstraintSystemImpl`、`VariableReadinessCalculator` 等）；`session 显式注入`类语义面未全接。

### 2.4 诊断（`:cfir:checkers` 等）— 前端最大的语义覆盖缺口

- checker 分类规模：declaration 68、expression 63、type 10（modifier/annotation 目录为 0，相关检查分散在别的分类），共 164 个 `*Checker*.kt` 文件。
- 诊断定义：`CfirDiagnosticsList.kt`（生成侧）约 531 个定义；而官方诊断目录快照（`docs/official-compiler-diagnostics.md`）合计 **1022 条**（Sema 520、Parser/Lexer/Macro/CHIR 余量）。
- 渲染文案：`CfirErrorsDefaultMessages.kt` 当前仅约 17 条渲染条目，其余依赖诊断名直出。
- 测试数据：`testData/diagnostics(2)` 479 个用例文件 ×3 变体（含 PSI/无别名展开）已生成 7380 个测试方法。

## 3. 各子系统模块明细

### 3.1 基础设施（common / util / generators / flatbuffers-gen）

| 模块 | 评级 | 证据与缺口 |
|---|---|---|
| `:common` | 基本完整 | 179 文件；名称系统/内置类型/描述符齐 |
| `:common:diagnostics` | 基本完整 | 68 文件；渲染器/工厂齐；零测试 |
| `:util` | 基本完整 | 58 文件；无 TODO |
| `:resolution.common` | 基本完整 | 75 文件 9479 行；K2 约束系统移植；推断组件少量 TODO |
| `:generators` / `:flatbuffers-gen` | 完整（就其定位） | 支撑 cfir-tree 生成与宏协议 |

### 3.2 编译器驱动（`:compiler:*`）

| 模块 | 评级 | 证据与缺口 |
|---|---|---|
| `:compiler:config` | 基本完整 | 29 文件；CLI 诊断工厂存储初始化 TODO（CompilerConfiguration.kt:208-217） |
| `:compiler:phaser` | 基本完整 | CompilerPhase/PhaseConfig/PhaseSet/Profiler 全套 |
| `:compiler:arguments` | 基本完整 | 参数 DSL + 注册表；**零测试** |
| `:compiler:frontend-arguments-generator` | 骨架 | 生成机制真实，但产物仅 74 行 11 属性的参数集 |
| `:compiler:frontend` | 基本完整 | 48 文件；`CfirFrontendPipelinePhase then CjoWritePipelinePhase` 组合 + 宏 construction 编排 |
| `:compiler:cli` / `cli-base` | 骨架 | CLICompiler 均 abstract，**无可执行 main**，目前不能作为独立编译器命令运行 |
| `:compiler:plugin` | 空壳 | 零源码 → LOAD_PLUGINS 断链 |

### 3.3 CFIR 数据与解析（`:cfir:*`）

| 模块 | 评级 | 证据与缺口 |
|---|---|---|
| `:cfir:cfir-tree`（含 gen） | 基本完整 | 生成代码 360 文件；生成声明节点 40、表达式节点 57、模式 12 类；覆盖 lambda/match/quote/spawn/perform/resume/inout 参数/optional chain 等 |
| `:cfir:cfir-cones` | 基本完整 | 40 文件 3829 行；类型族含 union/tuple/VArray/typealias 等 |
| `:cfir:providers` | 基本完整 | 120 文件 21914 行，含宏子系统全量落点 |
| `:cfir:raw-cfir:psi2cfir` | 基本完整 | 单文件 builder 4102 行 |
| `:cfir:raw-cfir:light-tree2cfir` | 基本完整 | 6701 行，双路径对等（架构守门测试约束） |
| `:cfir:resolve` | 基本完整 | 见 §2.3 |
| `:cfir:checkers` | 部分实现 | 见 §2.4；checker 与官方诊断目录差距是前端最大缺口 |
| `:cfir:diagnostic-renderers` | 骨架~部分 | 渲染条目仅 ~17 条 |
| `:cfir:entrypoint` | 基本完整 | 16 文件；Session 工厂 + pipeline 装配，全链路可跑通到诊断 |
| `:cfir:cfir-serialization` | 部分实现 | 读侧基本完整（`CfirDeclDeserializer` 2318 行、真实 std.core.cjo 集成测试、CJD sidecar overlay）；写侧仅包级元数据（`CjoPackageWriter` 1492 行），**无声明体序列化** |

### 3.4 Analysis API / 编辑器服务 / LSP（评分 63）

| 模块组 | 评级 | 证据与缺口 |
|---|---|---|
| `:analysis:analysis-api(+cfir, standalone, impl-base)` | 基本完整 | `CaSession`/analyze + 20+ 组件 + symbols/types/renderer 全套，259 文件 |
| `:analysis:low-level-api-cfir` | 基本完整 | 208 文件；sessions/lazy resolve/caches/stub-based 反序列化/per-phase transformers 镜像 K2 |
| LLT 测试 | 规模大 | `testData/llt` 2980 文件，生成用例 7258×2；**通过率未验证**，仓库存在 LLT 修复 skill 表明仍在收敛 |
| `:analysis:decompiled:*` | 部分实现 | .cjo→stubs/PSI 三模块 + 集成测试在；跨模块全场景证据不足 |
| `:code-insight:completion/formatting` | 部分实现 | 有 Analysis 支撑的管线 |
| `:code-insight:highlighting/folding/refactoring/override-implement` | 骨架~部分 | 接口与少量实现 |
| `:code-insight:fixes` | 空壳 | fixes-k2 零文件 |
| `:lsp` | 基本完整 | 42 源文件 + 30 集成测试：能力协商/文档同步/补全/rename/folding/format/semantic tokens/诊断推送；goto/hover 待确认 |
| 性能遥测 | 见已知问题 | `analysis/docs/analysis-performance-known-issues.md`：无性能断言（实为遥测接线测试）、覆盖清单存在假登记、OTLP 导出零验证、上报链路 8 项缺陷（P1–P9） |

### 3.5 后端（CHIR / CodeGen / LLVM，评分约 50）

| 模块 | 评级 | 证据与缺口 |
|---|---|---|
| `:chir:chir-tree` | 基本完整（就其定位） | 40 文件 6116 行：值/指令/控制流/symbol table/pass 调度/序列化 round-trip + 19 个测试类 |
| `:chir:cfir2chir` | 部分实现 | 9 文件 3100 行；值 if→phi、短路→显式控制流、类/接口/struct/extend/枚举转换有测试（`Cfir2ChirCoverageTest` 1247 行）；表达式面未全覆盖 |
| `:compiler:codegen`（CHIR→LLVM IR） | 部分实现 | 26 文件：lowering pipeline、CGModule、IRBuilder、LLVM 产物写出、parity comparator；覆盖面与真实度待端到端验证 |
| `:compiler:jvm-codegen` | 部分实现 | 17 文件：class/function/module codegen、ABI、类型映射；无字节码级端到端证据 |
| `:llvm-interop:*` | 骨架→部分 | API 层有绑定接口；JNI 需原生库（`LlvmNative` 按显式路径→native home→classpath→系统库顺序加载），**未装原生库时抛 `LlvmBackendUnavailableException`** |
| 端到端 | 未打通 | 源码→CHIR→LLVM IR 的演示链路差：FINALIZE/MANGLING 缺失、CJO 写侧不完整、codegen 覆盖面与 CLI 入口 |

### 3.6 测试基建（`:tests` / `:cfir:analysis-tests`）

- `tests/test-infrastructure` 115 个 kt 文件，Directive/TestServices 风格对齐 Kotlin test-infrastructure。
- `cfir/analysis-tests`：testData 3725 个 .cj（llt 2980 / diagnostics 479 / macro 253 / libraries 8 / macro-semantic-integration 5），生成测试方法约 **18,750** 个（LLT 7258×2、diagnostics 819×3 + 415×3、macro 731×2）。
- 风险：本轮未跑测试，以上规模不代表通过率；`testData/diagnostics2` 与 `llt` 的收敛状态以 CI 为准。

## 4. 差距清单（按优先级）

**P0（决定"是不是一个编译器"）**
1. 语义诊断覆盖：checker 数量与官方 1022 条诊断目录的差距（当前 ~531 定义、~141 checker 文件、~17 渲染条目）。
2. SAVE_CJO 写入侧：从包级元数据推进到声明体序列化，形成可被官方工具链读取的 .cjo。
3. MANGLING：名字修饰逻辑与管线阶段（当前完全没有），是链接/ABI/互操作的前置。

**P1（决定"能不能独立运行"）**
4. CLI 可执行入口：具体命令子类 + main + 参数集扩充（当前 `compiler:cli` abstract、参数仅 11 项）。
5. FINALIZE 阶段接缝与语义（当前无实现无挂点）。
6. LOAD_PLUGINS：至少提供注解/宏插件加载的最小实现（当前空壳）。
7. 条件编译默认环境与表达式级语义；宏 Batch 10 收尾（`MacroReplaceSlot` 修复、Identity* 占位替换）。

**P2（决定"好不好用"）**
8. code-insight 短板：fixes 空壳、highlighting/refactoring 骨架化；LSP goto/hover 补齐。
9. 类型 checker 扩容（当前仅 10 个文件）与 `@!` 注解、inout 修饰符等语法缺口。
10. 性能遥测已知问题清单落地（P1–P4 上报链路、T1–T4 测试自证问题）。
11. 文档修复：`psi/docs/psi-parser-comparison.md` 过时（Effect 已实现）；`compiler:arguments`/`config`/`diagnostics` 补测试。

## 5. 结论

以"Kotlin K2 架构 + 官方 cjc 语义"为标尺：**前端读路径（parse → raw CFIR → resolve → LLT/Analysis API）是全项目最成熟的部分**，架构对齐度高、双源前端与惰性 phase 化解析真实可用，宏展开与 .cjo 读侧已达基本完整；**写路径与产物侧（诊断全覆盖、FINALIZE、MANGLING、CJO 声明体序列化、后端 codegen、CLI 入口）是当前与"完整编译器"之间的主要差距**。若目标是先成为语义分析/IDE 引擎，路线已接近；若目标是替代 cjc 的编译闭环，应按 §4 的 P0→P1 顺序补齐。

## 相关文档

- [编译阶段](cjfir-compiler-stages.md)
- [模块目录](module-catalog.md)
- [宏展开基线](macro-construction-baseline.md)
- [官方诊断目录](official-compiler-diagnostics.md)
- [Analysis 性能已知问题](../analysis/docs/analysis-performance-known-issues.md)
- [PSI 解析器对比](../psi/docs/psi-parser-comparison.md)
