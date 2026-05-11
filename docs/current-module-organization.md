# 当前项目模块组织（实际接入 Gradle 的现状）

> 本文基于当前仓库的 `settings.gradle.kts` 与各模块 `build.gradle.kts` 整理。
> 目标是描述"当前态"，不是理想拆分方案，也不是未来模块规划。
>
> 更新日期：2026-05-11
> 真相源：`settings.gradle.kts`

---

## 范围

- 仅覆盖当前已经通过主 `settings.gradle.kts` 接入构建的 first-party modules。
- `external/` 不在本文范围内。
- `intellij-ide/`、`deveco/` 拥有独立的 `settings.gradle.kts`，作为独立子项目存在，本文最后单列说明。
- 文中"主要依赖"仅给出方向，不展开测试依赖与第三方库依赖。

---

## 阅读方式

- `实装模块`：已有实际源码与明确职责。
- `聚合模块`：用于分组或命名空间，本身几乎不承载源码职责。
- `占位模块`：已接入 Gradle，但当前源码极少或职责尚未真正落地。
- `工件模块`：用于发布工件聚合，本身不承载新逻辑。

---

## 当前模块全景

```text
基础设施
  :util
  :common
  :common:diagnostics
  :generators
  :flatbuffers-gen
  :dependencies:intellij-core

编译器驱动与配置
  :compiler                                (聚合)
  :compiler:config
  :compiler:phaser
  :compiler:arguments
  :compiler:frontend-arguments-generator
  :compiler:frontend
  :compiler:plugin                         (占位)
  :resolution.common

源码解析
  :psi

CFIR 数据模型
  :cfir                                    (聚合)
  :cfir:cfir-common
  :cfir:cfir-cones
  :cfir:cfir-tree
  :cfir:cfir-tree:tree-generator
  :cfir:semantics
  :cfir:diagnostic-renderers
  :cfir:providers

CFIR 处理链
  :cfir:raw-cfir                           (聚合)
  :cfir:raw-cfir:raw-cfir-common
  :cfir:raw-cfir:psi2cfir
  :cfir:raw-cfir:light-tree2cfir
  :cfir:resolve
  :cfir:checkers
  :cfir:checkers:checkers-component-generator
  :cfir:cfir-serialization
  :cfir:entrypoint

CFIR 测试
  :cfir:analysis-tests

宏展开
  :macro:macro-common
  :macro:macro-process
  :macro:macro-stub

LSP
  :lsp

Analysis API
  :analysis:analysis-api
  :analysis:analysis-api-platform-interface
  :analysis:analysis-api-impl-base
  :analysis:analysis-api-standalone
  :analysis:analysis-api-cfir
  :analysis:analysis-api-cfir:analysis-api-cfir-generator
  :analysis:low-level-api-cfir
  :analysis:analysis-internal-utils
  :analysis:cj-references
  :analysis:stubs
  :analysis:decompiled                     (聚合)
  :analysis:decompiled:decompiler-to-file-stubs
  :analysis:decompiled:decompiler-to-stubs
  :analysis:decompiled:decompiler-to-psi
  :analysis:decompiled:light-declarations-for-decompiled
  :analysis:light-declarations
  :analysis:symbol-light-declarations
  :analysis:analysis-tools
  :analysis:analysis-test-framework

可选后端（CHIR / CodeGen / LLVM）
  :compiler:chir
  :compiler:codegen
  :llvm-interop                            (聚合)
  :llvm-interop:llvm-interop-api
  :llvm-interop:llvm-interop-jni

测试支撑
  :tests
  :tests:test-infrastructure

发布工件
  :prepare:frontend
  :prepare:frontend-embeddable
  :prepare:test-infrastructure
  :prepare:analysis-test-framework
  :prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide
  :prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide
  :prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide
  :prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide
  :prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide
  :prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide
  :prepare:ide-plugin-dependencies-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-psi-for-ide-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-for-ide-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-cfir-for-ide-module
  :prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-standalone-for-ide-module
```

---

## 分层摘要

### 1. 基础设施层

- `:util` 提供通用工具（打印机、异常框架、集合扩展）。
- `:common` 提供名称系统（Name / FqName / ClassId / CallableId）、内置类型、描述符、消息收集、`LanguageVersionSettings`。
- `:common:diagnostics` 提供诊断框架核心（DiagnosticFactory / Reporter / Severity / Collector / PositioningStrategy）。
- `:generators` 提供构建时代码生成框架。
- `:flatbuffers-gen` 提供 FlatBuffers schema 与生成产物（宏协议序列化）。
- `:dependencies:intellij-core` 聚合 IntelliJ Platform 依赖。

### 2. 编译器驱动与配置层

- `:compiler:config` 提供 `CompilerConfiguration` / Content Roots / 环境模型。
- `:compiler:phaser` 提供编译阶段管理框架（`CompilerPhase` / `PhaseSet` / `PhaserState`）。
- `:compiler:arguments` 与 `:compiler:frontend-arguments-generator` 提供命令行参数定义与生成。
- `:compiler:frontend` 提供前端基础设施与编译管线协调。
- `:compiler:plugin` 为编译器插件加载占位（阶段 1 LOAD_PLUGINS）。
- `:resolution.common` 提供类型推断 / 约束系统公共层（对齐 Kotlin `resolution.common`）。

### 3. 源码解析层

- `:psi` 负责词法、语法、PSI 树、注解器。

### 4. CFIR 数据模型层

- `:cfir:cfir-common` 提供 `CfirSession` / `CfirModuleData` / `CfirElement`。
- `:cfir:cfir-cones` 提供类型系统核心（`ConeCangjieType` 及其子类）。
- `:cfir:cfir-tree`（含 `tree-generator`）提供生成式 CFIR 节点、visitors、transformer。
- `:cfir:semantics` 提供 CFIR 语义工具。
- `:cfir:providers` 提供符号 / 扩展点 providers。
- `:cfir:diagnostic-renderers` 提供诊断渲染器。

### 5. CFIR 处理链

- `:cfir:raw-cfir:*` 负责从源码树到 Raw CFIR（PSI / LightTree 两条路径）。
- `:cfir:resolve` 负责多 Phase 语义解析（IMPORTS / SUPER_TYPES / TYPES / STATUS / EXTENSIONS / IMPLICIT_TYPES / BODY_RESOLVE / CHECKERS）。
- `:cfir:checkers`（含 `checkers-component-generator`）提供诊断检查器与组件生成。
- `:cfir:cfir-serialization` 负责 `.cjo` 文件反序列化与跨模块符号加载（序列化写入侧仍在补齐）。
- `:cfir:entrypoint` 提供 CFIR 前端入口（Session 工厂、Pipeline 配置）。

### 6. 宏展开层

- `:macro:macro-common` 定义宏展开接口、数据模型与 FlatBuffers 协议编解码。
- `:macro:macro-process` 提供 `ProcessMacroExecutor`（外部进程 LSPMacroServer 实现）。
- `:macro:macro-stub` 提供 `StubMacroExecutor`（测试 / IDE 桩）。

### 7. LSP 层

- `:lsp` 是基于 `lsp4j` 的 Language Server 框架，覆盖能力协商、文档同步、工作区状态与 `AnalysisFacade` 接缝。

### 8. Analysis API 层（IDE 层）

- `:analysis:analysis-api` 与 `:analysis:analysis-api-platform-interface` 定义对外 API 与平台抽象。
- `:analysis:analysis-api-impl-base` 提供基础实现。
- `:analysis:analysis-api-standalone` 提供 standalone 模式实现。
- `:analysis:analysis-api-cfir`（含 `analysis-api-cfir-generator`）与 `:analysis:low-level-api-cfir` 提供 CFIR 后端实现。
- `:analysis:analysis-internal-utils` 提供模块内工具。
- `:analysis:cj-references` 提供跨语言引用。
- `:analysis:stubs` 提供 stub 索引与数据模型。
- `:analysis:decompiled`（聚合）下挂 `decompiler-to-file-stubs` / `decompiler-to-stubs` / `decompiler-to-psi` / `light-declarations-for-decompiled`。
- `:analysis:light-declarations` 与 `:analysis:symbol-light-declarations` 提供 light declaration 模型。
- `:analysis:analysis-tools` 提供工具集合。
- `:analysis:analysis-test-framework` 提供分析 API 测试基础设施。

### 9. 可选后端层（CHIR / CodeGen / LLVM）

- `:compiler:chir` 提供 CHIR 数据模型、context、builder、validator、pass pipeline、analyses、rewrites、serializer、printer / inspector。
- `:compiler:codegen` 提供 CHIR → LLVM IR 后端，含 `ChirCodegenInput/Output` 与 parity 测试基线。
- `:llvm-interop:llvm-interop-api` 提供 JVM 调 LLVM 的纯 Kotlin API。
- `:llvm-interop:llvm-interop-jni` 提供 JNI 绑定与原生库加载。

### 10. 测试支撑层

- `:tests:test-infrastructure` 提供 Kotlin 风格 Directive / TestServices / 配置 DSL，作为编译器与 analysis 测试的共享基础设施。
- `:cfir:analysis-tests` 提供 CFIR 分析测试套件。

### 11. 发布工件层

- `:prepare:frontend`、`:prepare:frontend-embeddable` 是前端发布工件门面。
- `:prepare:test-infrastructure`、`:prepare:analysis-test-framework` 是测试基建发布工件。
- `:prepare:ide-plugin-dependencies:*`（fat jar 形态）与 `:prepare:ide-plugin-dependencies-module:*`（module 形态）按功能分组打包 IDE 插件依赖：`common` / `psi` / `cfir` / `analysis-api` / `analysis-api-cfir` / `analysis-api-standalone`。

---

## 与上一份现状文档（2026-03-19）的主要差异

| 模块 | 变化 |
|---|---|
| `:cfir:symbols` | ❌ 不再存在；职责拆入 `:cfir:semantics` 与 `:cfir:providers` |
| `:cfir:semantics` | ✅ 新接入（语义工具） |
| `:cfir:providers` | ✅ 新接入（符号 / 扩展点 providers） |
| `:resolution.common` | ✅ 新接入（类型推断 / 约束公共层） |
| `:lsp` | ✅ 新接入（基于 lsp4j 的 Language Server 框架） |
| `:macro:macro-common`、`:macro:macro-process`、`:macro:macro-stub` | ✅ 新接入（宏展开三件套） |
| `:analysis:analysis-api-platform-interface` | ✅ 新接入 |
| `:analysis:analysis-api-standalone` | ✅ 新接入 |
| `:analysis:low-level-api-cfir` | ✅ 新接入 |
| `:analysis:analysis-internal-utils`、`:analysis:cj-references` | ✅ 新接入 |
| `:analysis:stubs` | ✅ 新接入 |
| `:analysis:decompiled` 及 4 个子模块 | ✅ 新接入 |
| `:analysis:light-declarations`、`:analysis:symbol-light-declarations` | ✅ 新接入 |
| `:analysis:analysis-tools` | ✅ 新接入 |
| `:prepare:ide-plugin-dependencies:*`、`:prepare:ide-plugin-dependencies-module:*` | ✅ 新接入（fat jar / module 双形态） |
| `:cfir:diagnostic-renderers` | 位置不变，仍为实装模块 |

---

## 仍未接入构建的规划模块

下列模块经常出现在设计讨论中（见 `compiler-module-design.md` / `module-organization.md`），但**当前并未接入 `settings.gradle.kts`**：

- `:compiler:condition-compile`（阶段 3 CONDITION_COMPILE）
- `:compiler:macro`（阶段 5 整体编排层；当前阶段 5 由 `:macro:*` 三件套承载，无统一编排模块）
- `:compiler:finalize`（阶段 8 FINALIZE）
- `:compiler:mangling`（阶段 9 MANGLING）
- `:compiler:pipeline`（统一管线编排）

---

## 独立子项目

下列子项目位于主仓库下但拥有**独立的 `settings.gradle.kts`**，不在主构建图内：

- `intellij-ide/` — IntelliJ Platform 仓颉插件（基于 IntelliJ Platform Gradle Plugin 2.x，多模块组织：`build-logic` / `product/idea-plugin` / `modules/foundation` / `modules/domain/*` / `modules/ide/*` / `modules/test-support`）。
- `deveco/` — DevEco Studio 仓颉增强插件，独立产品构建，通过 `includeBuild("../")` 接入主仓库源码。

---

## 一句话总结

主仓库的模块组织已经覆盖了 `基础设施 → PSI → CFIR 数据模型 → raw-cfir / resolve / checkers → serialization / entrypoint → macro → LSP / Analysis API → CHIR / codegen / LLVM → tests / prepare` 的完整轮廓，且 IDE 插件依赖工件（fat jar + module 双形态）已具备分发能力；按阶段维度，前端管线 LOAD_PLUGINS / CONDITION_COMPILE / FINALIZE / MANGLING 仍未落地为独立模块。
