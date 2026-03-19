# 当前项目模块组织（实际接入 Gradle 的现状）

> 本文基于当前仓库的 `settings.gradle.kts` 与各模块 `build.gradle.kts` 整理。
> 目标是描述"当前态"，不是理想拆分方案，也不是未来模块规划。
>
> 更新日期：2026-03-19

---

## 范围

- 仅覆盖当前已经通过 `settings.gradle.kts` 接入构建的 first-party modules。
- `external/` 不在本文范围内。
- 文中"直接项目依赖"只统计主源码依赖，不展开测试依赖和第三方库依赖。

---

## 阅读方式

- `实装模块`：已有实际源码和明确职责。
- `聚合模块`：用于分组或命名空间，本身几乎不承载源码职责。
- `占位模块`：已接入 Gradle，但当前源码极少或职责尚未真正落地。

---

## 当前模块全景

```text
基础设施
  :util
  :common
  :common:diagnostics
  :compiler:config
  :compiler:phaser
  :compiler:arguments
  :compiler:cli-arguments-generator
  :dependencies:intellij-core
  :generators
  :flatbuffers-gen

源码与入口
  :compiler                  (聚合)
  :compiler:cli
  :psi

CFIR 数据模型
  :cfir                      (聚合)
  :cfir:cfir-common
  :cfir:cfir-cones
  :cfir:cfir-tree
  :cfir:cfir-tree:tree-generator
  :cfir:symbols

CFIR 处理链
  :cfir:raw-cfir             (聚合)
  :cfir:raw-cfir:raw-cfir-common
  :cfir:raw-cfir:psi2cfir
  :cfir:raw-cfir:light-tree2cfir
  :cfir:resolve
  :cfir:checkers
  :cfir:checkers:checkers-component-generator
  :cfir:diagnostic-renderers
  :cfir:entrypoint
  :cfir:cfir-serialization

CFIR 测试
  :cfir:analysis-tests

后端
  :compiler:chir
  :compiler:codegen
  :compiler:plugin           (占位)

LLVM 互操作
  :llvm-interop              (聚合)
  :llvm-interop:llvm-interop-api
  :llvm-interop:llvm-interop-jni

Analysis API
  :analysis:analysis-api
  :analysis:analysis-api-impl-base
  :analysis:analysis-api-cfir
  :analysis:analysis-test-framework

测试
  :tests                     (聚合)
  :tests:test-infrastructure
```

---

## 分层摘要

### 1. 基础设施层

- `:util` 提供通用工具。
- `:common` 提供名称、描述符、基础领域模型。
- `:common:diagnostics` 提供诊断框架核心（DiagnosticFactory、Reporter、Severity、Collector、PositioningStrategy）。
- `:compiler:config` 提供编译配置模型。
- `:compiler:phaser` 提供编译阶段管理框架（CompilerPhase、PhaseSet、PhaserState）。
- `:compiler:arguments` 提供编译器命令行参数定义。
- `:dependencies:intellij-core` 聚合 IntelliJ Platform 依赖。

### 2. 源码表示与编译器入口层

- `:psi` 负责词法、语法、PSI。
- `:compiler:cli` 提供 CLI 入口、编译管线协调。

### 3. CFIR 数据模型层

- `:cfir:cfir-common`、`:cfir:cfir-cones`、`:cfir:cfir-tree` 构成 CFIR 核心。
- `:cfir:symbols` 提供符号提供者接口与实现、Scope 管理、内置符号。

### 4. CFIR 处理层

- `:cfir:raw-cfir:*` 负责从源码树到 Raw CFIR。
- `:cfir:resolve` 负责多阶段语义解析（类型推断、重载解析、诊断检查）。
- `:cfir:checkers` 负责检查器与诊断组件。
- `:cfir:entrypoint` 负责 CFIR 前端入口（Session 工厂、Pipeline 配置）。
- `:cfir:cfir-serialization` 负责 .cjo 文件反序列化、跨模块符号加载。

### 5. 后端层

- `:compiler:chir` 提供 CHIR 数据模型定义（接口 + 数据类）。
- `:compiler:codegen` 提供 CHIR → LLVM IR 代码生成框架。
- `:llvm-interop:*` 提供 LLVM JNI 接口与本地实现。

### 6. IDE 分析层

- `:analysis:*` 建立在 `:psi`、`:cfir:cfir-tree`、`:cfir:resolve` 之上。

### 7. 测试与构建工具层

- `:tests:test-infrastructure` 和 `:analysis:analysis-test-framework` 提供共享测试基础设施。
- `:cfir:analysis-tests` 提供 CFIR 分析测试套件。
- `:generators`、`:flatbuffers-gen` 提供构建时代码生成支持。

---

## 模块清单

### 基础设施与构建支持

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:util` | 实装模块 | 通用工具、基础辅助代码 | 无 |
| `:common` | 实装模块 | 名称系统、描述符、内置类型、基础领域模型 | `:util` |
| `:common:diagnostics` | 实装模块 | 诊断框架核心（DiagnosticFactory、Reporter、Severity、Collector、PositioningStrategy） | `:common` |
| `:compiler:config` | 实装模块 | 编译配置模型、ContentRoots、环境设置 | 无 |
| `:compiler:phaser` | 实装模块 | 编译阶段管理框架（CompilerPhase、PhaseSet、PhaserState） | 无 |
| `:compiler:arguments` | 实装模块 | 编译器命令行参数定义 | 无 |
| `:compiler:cli-arguments-generator` | 实装模块 | CLI 参数代码生成器 | 无 |
| `:dependencies:intellij-core` | 实装模块 | IntelliJ Platform 依赖聚合 | 无 |
| `:generators` | 实装模块 | 构建时代码生成工具支持 | `:util`, `:common` |
| `:flatbuffers-gen` | 实装模块 | FlatBuffers 代码生成与 `flatc` 管理 | 无 |

### 源码表示与编译器入口

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:compiler` | 聚合模块 | `compiler` 命名空间父模块 | 无 |
| `:compiler:cli` | 实装模块 | CLI 入口、编译管线协调、测试环境初始化 | `:compiler:config` |
| `:psi` | 实装模块 | JFlex Lexer、Parser、PSI 节点与相关基础设施 | `:util`, `:common` |

### CFIR 核心与生成

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:cfir` | 聚合模块 | `cfir` 命名空间父模块 | 无 |
| `:cfir:cfir-common` | 实装模块 | CFIR session、module data、source element 抽象 | `:common`, `:compiler:config`, `:util` |
| `:cfir:cfir-cones` | 实装模块 | CFIR 类型系统核心（ConeCangjieType 及子类） | `:cfir:cfir-common`, `:common` |
| `:cfir:cfir-tree` | 实装模块 | CFIR 节点、visitors、Transformer、部分 resolve/provider 抽象 | `:cfir:cfir-common`, `:cfir:cfir-cones`, `:common`, `:util` |
| `:cfir:cfir-tree:tree-generator` | 实装模块 | CFIR tree 代码生成器 | 无 |
| `:cfir:symbols` | 实装模块 | 符号提供者接口与实现、Scope 管理、内置符号 | `:cfir:cfir-tree`, `:cfir:cfir-cones` |

### CFIR 处理链

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:cfir:raw-cfir` | 聚合模块 | `raw-cfir` 命名空间父模块 | 无 |
| `:cfir:raw-cfir:raw-cfir-common` | 实装模块 | Raw CFIR 构建共享基类与基础转换抽象 | `:cfir:cfir-tree`, `:psi` |
| `:cfir:raw-cfir:psi2cfir` | 实装模块 | PSI 到 Raw CFIR 的主实现，带较完整测试支撑 | `:cfir:cfir-tree`, `:cfir:raw-cfir:raw-cfir-common`, `:psi` |
| `:cfir:raw-cfir:light-tree2cfir` | 实装模块 | LightTree 到 Raw CFIR 的实现 | `:cfir:cfir-tree`, `:cfir:raw-cfir:raw-cfir-common` |
| `:cfir:resolve` | 实装模块 | 多 Phase 语义解析：类型推断、重载解析、导入解析 | `:cfir:cfir-tree`, `:cfir:cfir-cones`, `:cfir:checkers`, `:common`, `:util` |
| `:cfir:checkers` | 实装模块 | 诊断检查器（Declaration/Expression/Type，17 个检查器） | `:cfir:cfir-common`, `:cfir:cfir-tree`, `:cfir:diagnostic-renderers` |
| `:cfir:checkers:checkers-component-generator` | 实装模块 | 检查器组件与诊断相关生成器 | 无 |
| `:cfir:diagnostic-renderers` | 实装模块 | 诊断信息渲染 | `:common:diagnostics` |
| `:cfir:entrypoint` | 实装模块 | CFIR 前端入口（Session 工厂、Pipeline 配置、分析入口） | `:cfir:resolve`, `:cfir:checkers`, `:cfir:raw-cfir:*` |
| `:cfir:cfir-serialization` | 实装模块 | .cjo 文件反序列化、跨模块符号加载 | `:cfir:cfir-tree`, `:cfir:cfir-cones` |

### 后端

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:compiler:chir` | 实装模块 | CHIR 数据模型（接口 + 数据类，39 个文件）、Pipeline/Pass 框架 | `:cfir:cfir-tree` |
| `:compiler:codegen` | 实装模块 | CHIR → LLVM IR 代码生成框架、JNI LLVM 后端 | `:compiler:chir` |
| `:compiler:plugin` | 占位模块 | 编译器插件框架（尚未实现） | 无 |

### LLVM 互操作

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:llvm-interop` | 聚合模块 | LLVM 互操作命名空间 | 无 |
| `:llvm-interop:llvm-interop-api` | 实装模块 | LLVM JNI 接口定义 | 无 |
| `:llvm-interop:llvm-interop-jni` | 实装模块 | LLVM JNI 本地实现（C++） | `:llvm-interop:llvm-interop-api` |

### Analysis API

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:analysis:analysis-api` | 实装模块 | 面向 IDE 的公共分析 API | `:psi`, `:cfir:cfir-tree` |
| `:analysis:analysis-api-impl-base` | 实装模块 | Analysis API 的基础实现层 | `:analysis:analysis-api`, `:psi` |
| `:analysis:analysis-api-cfir` | 实装模块 | 基于 CFIR 的 analysis backend | `:analysis:analysis-api`, `:analysis:analysis-api-impl-base`, `:cfir:cfir-tree`, `:cfir:resolve`, `:psi` |
| `:analysis:analysis-test-framework` | 实装模块 | analysis 相关测试框架，采用 `testFixtures` 组织 | `:analysis:analysis-api`, `:analysis:analysis-api-impl-base`, `:analysis:analysis-api-cfir` |

### 测试支撑

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:tests` | 聚合模块 | `tests` 命名空间父模块 | 无 |
| `:tests:test-infrastructure` | 实装模块 | 共享测试基础设施，采用 `testFixtures` 组织 | `:compiler:cli`, `:psi` |
| `:cfir:analysis-tests` | 实装模块 | CFIR 分析测试套件 | `:cfir:resolve`, `:cfir:checkers`, `:tests:test-infrastructure` |

---

## 与旧版文档的主要变化

以下模块已从"规划中"变为实际接入构建：

| 模块 | 旧状态 | 新状态 |
|---|---|---|
| `:cfir:entrypoint` | 未接入构建 | ✅ 实装模块 |
| `:cfir:cfir-serialization` | 未接入构建 | ✅ 实装模块（反序列化） |
| `:compiler:chir` | 未接入构建 | ✅ 实装模块（数据模型） |
| `:compiler:codegen` | 未接入构建 | ✅ 实装模块 |
| `:compiler:plugin` | 未接入构建 | 占位模块 |
| `:llvm-interop:*` | 未提及 | ✅ 实装模块 |
| `:common:diagnostics` | 不存在（诊断在 cfir-common 中） | ✅ 实装模块（已从 cfir-common 拆出） |
| `:compiler:phaser` | 未提及 | ✅ 实装模块 |
| `:compiler:arguments` | 未提及 | ✅ 实装模块 |
| `:cfir:analysis-tests` | 未提及 | ✅ 实装模块 |
| `:cfir:symbols` | 未提及 | ✅ 实装模块 |

以下模块已被移除或合并：

| 模块 | 变化 |
|---|---|
| `:cfir:cfir-common-psi` | 已删除 |
| `:compiler:frontend.common` | 已删除（职责合并到其他模块） |

---

## 当前未接入构建的规划模块

下列模块经常出现在设计讨论中，但**当前并未接入 `settings.gradle.kts`**：

- `:cfir:providers`（符号提供者当前在 `:cfir:symbols` 中）
- `:cfir:semantics`
- `:compiler:condition-compile`
- `:compiler:macro`
- `:compiler:finalize`
- `:compiler:mangling`
- `:compiler:pipeline`

---

## 一句话总结

当前项目的模块组织已经覆盖了 `基础设施 → PSI → CFIR 数据模型 → raw-cfir / resolve / checkers → entrypoint → serialization → CHIR / codegen → LLVM → analysis / test` 的完整轮廓，相比早期版本新增了后端（CHIR + codegen + LLVM 互操作）、诊断独立模块、编译管线框架等关键模块，模块结构正在向 12 阶段编译流水线的完整形态演进。