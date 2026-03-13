# 当前项目模块组织（实际接入 Gradle 的现状）

> 本文基于当前仓库的 `settings.gradle.kts` 与各模块 `build.gradle.kts` 整理。
> 目标是描述“当前态”，不是理想拆分方案，也不是未来模块规划。

---

## 范围

- 仅覆盖当前已经通过 `settings.gradle.kts` 接入构建的 first-party modules。
- `external/` 不在本文范围内。
- 文中“直接项目依赖”只统计主源码依赖，不展开测试依赖和第三方库依赖。

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
  :compiler:config
  :dependencies:intellij-core
  :generators
  :flatbuffers-gen

源码与入口
  :compiler                  (聚合)
  :compiler:frontend.common
  :compiler:cli
  :psi

CFIR
  :cfir                      (聚合)
  :cfir:cfir-common
  :cfir:cfir-common-psi      (占位)
  :cfir:cfir-cones
  :cfir:cfir-tree
  :cfir:cfir-tree:tree-generator
  :cfir:diagnostic-renderers (占位)
  :cfir:checkers
  :cfir:checkers:checkers-component-generator
  :cfir:resolve
  :cfir:raw-cfir             (聚合)
  :cfir:raw-cfir:raw-cfir-common
  :cfir:raw-cfir:psi2cfir
  :cfir:raw-cfir:light-tree2cfir

Analysis
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
- `:compiler:config` 提供语言版本和编译消息相关配置。
- `:dependencies:intellij-core` 聚合 IntelliJ Platform 依赖。

### 2. 源码表示与编译器入口层

- `:psi` 负责词法、语法、PSI。
- `:compiler:frontend.common` 负责前端共享的源码元素抽象。
- `:compiler:cli` 当前主要提供 CLI 环境与参数相关基础设施。

### 3. CFIR 数据模型层

- `:cfir:cfir-common`、`:cfir:cfir-cones`、`:cfir:cfir-tree` 构成当前 CFIR 核心。
- 这三层之外，还存在生成器和占位模块。

### 4. CFIR 处理层

- `:cfir:raw-cfir:*` 负责从源码树到 Raw CFIR。
- `:cfir:checkers` 负责检查器与诊断组件。
- `:cfir:resolve` 负责多阶段 resolve。

### 5. IDE 分析层

- `:analysis:*` 在当前结构中直接建立在 `:psi`、`:cfir:cfir-tree`、`:cfir:resolve` 之上。

### 6. 测试与构建工具层

- `:tests:test-infrastructure` 和 `:analysis:analysis-test-framework` 提供共享测试基础设施。
- `:generators`、`:flatbuffers-gen` 提供构建时代码生成支持。

---

## 模块清单

### 基础设施与构建支持

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:util` | 实装模块 | 通用工具、基础辅助代码 | 无 |
| `:common` | 实装模块 | 名称系统、描述符、基础领域模型 | `:util` |
| `:compiler:config` | 实装模块 | 语言版本设置、编译消息类型、编译配置模型 | 无 |
| `:dependencies:intellij-core` | 实装模块 | IntelliJ Platform 依赖聚合 | 无 |
| `:generators` | 实装模块 | 构建时代码生成工具支持 | `:util`, `:common` |
| `:flatbuffers-gen` | 实装模块 | FlatBuffers 代码生成与 `flatc` 管理 | 无 |

### 源码表示与编译器入口

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:compiler` | 聚合模块 | `compiler` 命名空间父模块 | 无 |
| `:compiler:frontend.common` | 实装模块 | 前端共享源码元素类型，目前核心是 `CjSourceElement` | `:compiler:config`, `:util` |
| `:compiler:cli` | 实装模块 | CLI 环境、参数模型、测试环境初始化 | `:compiler:config` |
| `:psi` | 实装模块 | JFlex Lexer、Parser、PSI 节点与相关基础设施 | `:util`, `:common` |

### CFIR 核心与生成

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:cfir` | 聚合模块 | `cfir` 命名空间父模块 | 无 |
| `:cfir:cfir-common` | 实装模块 | CFIR session、module data、source element 抽象，以及当前仍保存在此处的部分 diagnostics 代码 | `:common`, `:compiler:config`, `:util` |
| `:cfir:cfir-common-psi` | 占位模块 | 已接入构建，但当前几乎不承载源码职责 | `:cfir:cfir-common` |
| `:cfir:cfir-cones` | 实装模块 | CFIR 类型系统核心 | `:cfir:cfir-common`, `:common` |
| `:cfir:cfir-tree` | 实装模块 | CFIR 节点、symbols、visitors、部分 resolve/provider 抽象 | `:cfir:cfir-common`, `:cfir:cfir-cones`, `:common`, `:util` |
| `:cfir:cfir-tree:tree-generator` | 实装模块 | CFIR tree 代码生成器 | 无 |
| `:cfir:diagnostic-renderers` | 占位模块 | 已接入构建，但当前基本是壳模块 | `:cfir:cfir-common` |

### CFIR 处理链

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:cfir:checkers` | 实装模块 | 检查器定义、诊断组件生成接入、默认消息映射 | `:cfir:cfir-common`, `:cfir:cfir-tree`, `:cfir:diagnostic-renderers` |
| `:cfir:checkers:checkers-component-generator` | 实装模块 | 检查器组件与诊断相关生成器 | 无 |
| `:cfir:resolve` | 实装模块 | resolve processors、phase registry、import/type/status/checkers processor 编排 | `:cfir:cfir-tree`, `:cfir:cfir-common`, `:cfir:cfir-cones`, `:cfir:checkers`, `:common`, `:util` |
| `:cfir:raw-cfir` | 聚合模块 | `raw-cfir` 命名空间父模块 | 无 |
| `:cfir:raw-cfir:raw-cfir-common` | 实装模块 | Raw CFIR 构建共享基类与基础转换抽象 | `:cfir:cfir-tree`, `:psi` |
| `:cfir:raw-cfir:psi2cfir` | 实装模块 | PSI 到 Raw CFIR 的主实现，带较完整测试支撑 | `:cfir:cfir-tree`, `:cfir:raw-cfir:raw-cfir-common`, `:psi` |
| `:cfir:raw-cfir:light-tree2cfir` | 实装模块 | LightTree 到 Raw CFIR 的实现 | `:cfir:cfir-tree`, `:cfir:raw-cfir:raw-cfir-common` |

### Analysis API

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:analysis:analysis-api` | 实装模块 | 面向 IDE 的公共分析 API | `:psi`, `:cfir:cfir-tree` |
| `:analysis:analysis-api-impl-base` | 实装模块 | Analysis API 的基础实现层 | `:analysis:analysis-api`, `:psi` |
| `:analysis:analysis-api-cfir` | 实装模块 | 基于 CFIR 的 analysis backend | `:analysis:analysis-api`, `:analysis:analysis-api-impl-base`, `:cfir:cfir-tree`, `:cfir:resolve`, `:psi` |
| `:analysis:analysis-test-framework` | 实装模块 | analysis 相关测试框架，采用 `testFixtures` 组织 | `:analysis:analysis-api`, `:analysis:analysis-api-impl-base`, `:analysis:analysis-api-cfir`, `:psi`, `:cfir:cfir-tree`, `:tests:test-infrastructure`, `:compiler:cli` |

### 测试支撑

| Gradle 路径 | 类型 | 当前职责 | 直接项目依赖 |
|---|---|---|---|
| `:tests` | 聚合模块 | `tests` 命名空间父模块 | 无 |
| `:tests:test-infrastructure` | 实装模块 | 共享测试基础设施，采用 `testFixtures` 组织 | `:compiler:cli`, `:psi` |

---

## 当前结构特征

- 当前仓库同时存在 `聚合模块`、`实装模块` 和 `占位模块`，因此整体不是“完全收敛”的终态结构。
- `:cfir:cfir-common` 当前不只是 session/common 基础设施，diagnostics 相关代码也仍然放在里面。
- `:cfir:diagnostic-renderers` 虽然已接入构建，但当前并没有形成独立而完整的 diagnostics 子系统。
- `:cfir:resolve` 当前直接依赖 `:cfir:checkers`，说明 resolve 与 checkers 的编排仍耦合在一起。
- provider 相关抽象与实现当前仍主要放在 `:cfir:cfir-tree` 源码树中，还没有单独的 `providers` 模块。
- `:analysis:analysis-api` 当前已经直接依赖 `:psi` 与 `:cfir:cfir-tree`，Analysis API 还没有完全收敛成 backend-neutral 形态。
- `:compiler:frontend.common` 当前只有少量源码，但职责比较集中，主要承载前端源码元素桥接。

---

## 当前未接入构建的规划模块

下列模块经常出现在设计讨论中，但**当前并未接入 `settings.gradle.kts`**：

- `:cfir:entrypoint`
- `:cfir:providers`
- `:cfir:semantics`
- `:cfir:serialization`
- `:cfir:deserialization`
- `:compiler:condition-compile`
- `:compiler:macro`
- `:compiler:finalize`
- `:compiler:mangling`
- `:compiler:pipeline`
- `:compiler:plugins`
- `:compiler:chir`
- `:compiler:codegen`

这意味着当前项目的模块组织仍然以“CFIR 核心 + raw-cfir + resolve/checkers + analysis/test 支撑”为主，尚未扩展到完整 12 阶段编译流水线的分模块形态。

---

## 一句话总结

当前项目的模块组织已经具备了 `基础设施 -> PSI / CFIR 数据模型 -> raw-cfir / resolve / checkers -> analysis / test` 的基本轮廓，但仍保留明显的聚合模块、占位模块和若干历史耦合点，整体更接近“演进中的当前结构”而非“最终稳定模块图”。
