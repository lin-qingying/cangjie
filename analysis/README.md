# analysis/ — Analysis API

对齐 Kotlin `analysis/analysis-api`，为 IDE 与 standalone 工具提供面向仓颉语言的稳定分析接口。本目录是**聚合命名空间**（自身无 build.gradle.kts），下挂多个子模块。

模块清单以 `settings.gradle.kts` 为准。

## 子模块

### 接口层

| 子模块 | 职责 |
|---|---|
| `analysis-api` | 对外公共 API（Session、Lifetime、Permissions） |
| `analysis-api-platform-interface` | 平台接口抽象（对齐 Kotlin `analysis-api-platform-interface`） |

### 基础实现

| 子模块 | 职责 |
|---|---|
| `analysis-api-impl-base` | 公共实现基础层 |
| `analysis-api-standalone` | Standalone 模式实现 |

### CFIR 后端

| 子模块 | 职责 |
|---|---|
| `analysis-api-cfir` | 基于 CFIR 的 analysis 后端，含 `analysis-api-cfir-generator` |
| `low-level-api-cfir` | 低层分析 API 的 CFIR 实现 |

### 工具与基础设施

| 子模块 | 职责 |
|---|---|
| `analysis-internal-utils` | 模块内部工具 |
| `cj-references` | 跨语言引用支持 |
| `analysis-tools` | 分析工具集 |

### Stubs / Decompiled / Light Declarations

| 子模块 | 职责 |
|---|---|
| `stubs` | Stub 索引与 stub 数据模型 |
| `decompiled` | 反编译聚合，下挂：`decompiler-to-file-stubs` / `decompiler-to-stubs` / `decompiler-to-psi` / `light-declarations-for-decompiled` |
| `light-declarations` | Light declaration 模型 |
| `symbol-light-declarations` | Symbol-based light declarations |

### 测试

| 子模块 | 职责 |
|---|---|
| `analysis-test-framework` | 分析 API 测试框架（对齐 Kotlin `analysis-api-impl-base` testFixtures） |

## 设计原则

- **接口优先**：所有能力通过 `analysis-api` 暴露，调用方不依赖具体实现模块。
- **对齐 Kotlin K2**：包结构、类名、processor 层级尽量与 `external/kotlin/analysis/*` 一一对应，除非仓颉语义强制要求偏离。
- **测试**：分析模块测试默认接入 `AbstractAnalysisApiExecutionTest` / `AbstractAnalysisApiBasedTest`，纯模型 / 格式化 / 二进制头读取测试允许保留直接 JUnit。详见 `../TESTING_CONVENTIONS.md` 第 1.1 节。

## 相关文档

- `../TESTING_CONVENTIONS.md` — Analysis 模块测试分类清单
- `../docs/k2-module-alignment.md` — 与 Kotlin K2 模块对照
