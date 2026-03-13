# 模块组织规划

> 基于 12 阶段编译管线和 Kotlin K2 FIR 架构对齐目标，规划仓颉编译器的模块组织。
> 不以当前代码量为依据，以职责边界和依赖方向为准则。

---

## 设计原则

1. **依赖严格单向**：上层模块依赖下层，禁止反向或循环依赖
2. **每模块单一职责**：一个模块对应一个明确的编译器子系统或阶段
3. **K2 结构对齐**：模块划分、包层级、命名尽量对齐 Kotlin K2 FIR，仓颉独有语义除外
4. **按需创建**：未实现的阶段模块在实际开发时再加入 `settings.gradle.kts`

---

## 依赖流向总览

```
                                ┌─────────────────────────────────────┐
                                │          analysis (IDE 层)           │
                                │  analysis-api → impl-base → api-cfir │
                                └──────────────────┬──────────────────┘
                                                   │
                        ┌──────────────────────────┼──────────────────────┐
                        │                  cfir:entrypoint                │
                        └──────────────────────────┬──────────────────────┘
                                                   │
          ┌────────────────────────────────────────┼────────────────────────────┐
          │                                        │                            │
  ┌───────┴────────┐                    ┌──────────┴──────────┐    ┌────────────┴────────┐
  │ cfir:checkers  │                    │  cfir:serialization  │    │ cfir:deserialization │
  └───────┬────────┘                    └─────────────────────┘    └─────────────────────┘
          │
  ┌───────┴──────────┐
  │  cfir:resolve    │
  └───────┬──────────┘
          │
  ┌───────┴──────────┐
  │  cfir:providers  │
  └───────┬──────────┘
          │
  ┌───────┴──────────────────────────────┐
  │  cfir:raw-cfir                        │
  │  ├── raw-cfir-common                  │
  │  ├── psi2cfir                         │
  │  └── light-tree2cfir                  │
  └───────┬──────────────────────────────┘
          │
  ┌───────┴──────────┐     ┌─────────────┐
  │  cfir:cfir-tree  │     │    psi       │
  └───────┬──────────┘     └──────┬──────┘
          │                       │
  ┌───────┴──────────┐            │
  │  cfir:cfir-cones │            │
  └───────┬──────────┘            │
          │                       │
  ┌───────┴──────────┐            │
  │ cfir:diagnostics │            │
  └───────┬──────────┘            │
          │                       │
  ┌───────┴──────────┐            │
  │ cfir:cfir-common │            │
  └───────┬──────────┘            │
          │                       │
  ┌───────┴──────────┐   ┌───────┴──────┐
  │ compiler:config  │   │    common     │
  └───────┬──────────┘   └───────┬──────┘
          │                      │
          └──────────┬───────────┘
                     │
              ┌──────┴──────┐
              │     util    │
              └─────────────┘
```

---

## 模块详细说明

### 第一层：基础设施

无编译器语义，提供纯工具和领域基础模型。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:util` | 纯工具类：集合扩展、字符串工具、Printer、异常基类 | `compiler/util` | 无 |
| `:common` | 领域基础模型：Name、FqName、ClassId、Modality、Visibility、PrimitiveType | `core/compiler.common` | `:util` |
| `:compiler:config` | 编译器配置：LanguageVersionSettings、CompilerMessageSeverity | `compiler/config` | `:util` |

**边界原则：**
- `util` = 与仓颉语言无关的通用工具，可被任意模块依赖
- `common` = 仓颉语言通用的名称系统和修饰符模型
- `config` = 编译器运行时配置和消息类型

---

### 第二层：CFIR 数据模型

定义 CFIR 的核心数据结构，不包含任何转换或解析逻辑。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:cfir:cfir-common` | CFIR 基础设施：CfirSession、CfirSessionComponent、CfirModuleData、SourceElement | `fir/cones`（session 部分） | `:common`, `:compiler:config`, `:util` |
| `:cfir:diagnostics` | 诊断框架：DiagnosticFactory、DiagnosticReporter、Severity、Renderer、DiagnosticsCollector | K2 诊断分散在多模块，此处集中管理 | `:cfir:cfir-common` |
| `:cfir:cfir-cones` | 类型系统核心：ConeCangjieType 及其子类层级 | `fir/cones` | `:cfir:cfir-common`, `:common` |
| `:cfir:cfir-tree` | IR 节点：声明、表达式、类型引用、模式匹配、引用、访问者 | `fir/tree` | `:cfir:cfir-common`, `:cfir:cfir-cones`, `:common`, `:util` |
| `:cfir:cfir-tree:tree-generator` | cfir-tree 的代码生成器 | `fir/tree/tree-generator` | 构建时工具 |

**关键设计：**
- `cfir-common` 只管 Session 生命周期和模块元数据，不含诊断逻辑
- `diagnostics` 独立模块，供 checkers、resolve、raw-cfir 共同依赖
- `cfir-cones` 只定义类型，不做类型推断

---

### 第三层：PSI

与 CFIR 平行，不互相依赖，通过 raw-cfir 桥接。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:psi` | 仓颉语言的词法分析（JFlex）、语法分析、PSI 节点定义 | `compiler/psi` | `:common`, `:util` |

---

### 第四层：CFIR 构建（阶段 6 CFIR_BUILD）

将 PSI 或 LightTree 转换为 Raw CFIR（无类型信息的结构化 IR）。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:cfir:raw-cfir:raw-cfir-common` | 共享基类：AbstractRawCfirBuilder | `fir/raw-fir/raw-fir.common` | `:cfir:cfir-tree`, `:psi` |
| `:cfir:raw-cfir:psi2cfir` | PSI → Raw CFIR 转换 | `fir/raw-fir/psi2fir` | `:cfir:raw-cfir:raw-cfir-common`, `:psi` |
| `:cfir:raw-cfir:light-tree2cfir` | LightTree → Raw CFIR 转换（高性能路径） | `fir/raw-fir/light-tree2fir` | `:cfir:raw-cfir:raw-cfir-common` |

---

### 第五层：符号提供与语义解析（阶段 4 + 7）

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:cfir:providers` | 符号提供者：BuiltinSymbolProvider、CjoSymbolProvider、SourceSymbolProvider | `fir/providers` | `:cfir:cfir-tree`, `:cfir:cfir-cones` |
| `:cfir:semantics` | 语义工具纯函数：类型判断、可见性判断、作用域工具 | `fir/semantics` | `:cfir:cfir-tree`, `:cfir:cfir-cones` |
| `:cfir:resolve` | 多 Phase 语义解析：类型推断、重载解析、导入解析、超类解析 | `fir/resolve` | `:cfir:cfir-tree`, `:cfir:cfir-cones`, `:cfir:providers`, `:cfir:diagnostics` |

**关键约束：** `resolve` 不依赖 `checkers`。resolve 阶段需要的少量检查接口通过 `cfir:cfir-tree` 或 `cfir:cfir-common` 中的抽象定义传递。

---

### 第六层：诊断检查（阶段 7 末尾 CHECKERS）

在 resolve 完成后运行，对已解析的 CFIR 执行语义检查。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:cfir:checkers` | 诊断检查器：声明检查、表达式检查、类型检查 | `fir/checkers/src` | `:cfir:cfir-tree`, `:cfir:resolve`, `:cfir:diagnostics` |
| `:cfir:checkers:checkers-component-generator` | 检查器组件代码生成 | `fir/checkers/checkers-component-generator` | 构建时工具 |
| `:cfir:diagnostic-renderers` | 诊断信息渲染为人类可读文本 | `fir/diagnostic-renderers` | `:cfir:diagnostics` |

**依赖方向：** `checkers → resolve → providers → cfir-tree`（严格单向）。

---

### 第七层：CFIR 入口

编排 CFIR 子系统的完整编译流程（阶段 6 → 7）。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:cfir:entrypoint` | CFIR 编译流程编排：Raw CFIR 构建 → Resolve → Checkers | `fir/entrypoint` | `:cfir:resolve`, `:cfir:checkers`, `:cfir:raw-cfir:*` |

---

### 第八层：仓颉特有阶段

K2 无对应模块，为仓颉语言独有的编译阶段。

| Gradle 路径 | 职责 | 对应阶段 | 依赖 |
|---|---|---|---|
| `:compiler:condition-compile` | @When 条件编译，操作 PSI 层裁剪分支 | 阶段 3 | `:psi`, `:compiler:config` |
| `:compiler:macro` | 宏展开：语法宏 + 属性宏 | 阶段 5 | `:psi` |
| `:compiler:finalize` | 语义后处理：脱糖、泛型单态化、溢出策略标注 | 阶段 8 | `:cfir:cfir-tree`, `:cfir:cfir-cones` |
| `:compiler:mangling` | 符号名称修饰 | 阶段 9 | `:cfir:cfir-tree` |

---

### 第九层：序列化与后端

| Gradle 路径 | 职责 | 对应阶段 | 依赖 |
|---|---|---|---|
| `:cfir:serialization` | CFIR → .cjo 序列化 | 阶段 10 | `:cfir:cfir-tree` |
| `:cfir:deserialization` | .cjo → CFIR 反序列化（供阶段 4 IMPORT_PACKAGE） | 阶段 4 | `:cfir:cfir-tree`, `:cfir:providers` |
| `:compiler:chir` | CHIR 数据模型 + CFIR → CHIR 转换 + 优化 | 阶段 11 | `:cfir:cfir-tree` |
| `:compiler:codegen` | CHIR → LLVM IR → 机器码 | 阶段 12 | `:compiler:chir` |

---

### 第十层：编译器入口

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:compiler:frontend.common` | 前端共享设施：SourceElement 等 PSI ↔ CFIR 桥接类型 | `compiler/frontend.common` | `:compiler:config`, `:util` |
| `:compiler:cli` | 命令行入口，串联全部 12 个阶段 | `compiler/cli` | 全部编译器模块 |

---

### Analysis API（IDE 层）

与编译器平行的顶层模块，为 IntelliJ 插件提供语义分析能力。

| Gradle 路径 | 职责 | K2 对齐 | 依赖 |
|---|---|---|---|
| `:analysis:analysis-api` | 面向 IDE 的公共分析 API | `analysis/analysis-api` | `:psi`, `:cfir:cfir-tree` |
| `:analysis:analysis-api-impl-base` | 分析 API 基础实现 | `analysis/analysis-api-impl-base` | `:analysis:analysis-api`, `:psi` |
| `:analysis:analysis-api-cfir` | CFIR 后端实现 | `analysis/analysis-api-fir` | `:analysis:analysis-api-impl-base`, `:cfir:resolve` |
| `:analysis:analysis-test-framework` | 分析 API 测试基础设施 | `analysis/analysis-test-framework` | `:analysis:analysis-api` |

---

### 测试与构建基础设施

| Gradle 路径 | 职责 |
|---|---|
| `:tests:test-infrastructure` | 共享测试基础设施：环境搭建、testFixtures |
| `:generators` | 构建时代码生成工具 |
| `:dependencies:intellij-core` | IntelliJ Platform 依赖聚合 |
| `:flatbuffers-gen` | FlatBuffers 生成代码 |

---

## 阶段 → 模块映射

| # | 阶段 | 主要模块 | 状态 |
|---|---|---|---|
| 1 | LOAD_PLUGINS | `:compiler:cli` | 未实现 |
| 2 | PARSE | `:psi` | 已实现 |
| 3 | CONDITION_COMPILE | `:compiler:condition-compile` | 未实现 |
| 4 | IMPORT_PACKAGE | `:cfir:deserialization`, `:cfir:providers` | 未实现 |
| 5 | MACRO_EXPAND | `:compiler:macro` | 未实现 |
| 6 | CFIR_BUILD | `:cfir:raw-cfir:psi2cfir`, `:cfir:raw-cfir:light-tree2cfir` | 开发中 |
| 7 | CFIR_RESOLVE | `:cfir:resolve` + `:cfir:checkers` | 开发中 |
| 8 | FINALIZE | `:compiler:finalize` | 未实现 |
| 9 | MANGLING | `:compiler:mangling` | 未实现 |
| 10 | SAVE_CJO | `:cfir:serialization` | 未实现 |
| 11 | CFIR2CHIR | `:compiler:chir` | 未实现 |
| 12 | CODEGEN | `:compiler:codegen` | 未实现 |

---

## 与当前结构的差异

### 需要新增的模块

| 模块 | 来源 | 优先级 |
|---|---|---|
| `:cfir:diagnostics` | 从 `:cfir:cfir-common` 拆出诊断框架（~25 个文件） | **高** — 解决 cfir-common 职责过重 |
| `:cfir:providers` | 从 `:cfir:cfir-tree` 拆出 BuiltinSymbolProvider 等 | **高** — 对齐 K2 `fir/providers` |
| `:cfir:semantics` | 新建，放置语义工具纯函数 | 中 — resolve 复杂度上升时拆出 |
| `:cfir:entrypoint` | 新建，编排 CFIR 编译流程 | 中 — 集成测试时需要 |
| `:cfir:serialization` | 新建 | 低 — 阶段 10 开发时创建 |
| `:cfir:deserialization` | 新建 | 低 — 阶段 4 开发时创建 |
| `:compiler:condition-compile` | 新建 | 低 — 阶段 3 开发时创建 |
| `:compiler:macro` | 新建 | 低 — 阶段 5 开发时创建 |
| `:compiler:finalize` | 新建 | 低 — 阶段 8 开发时创建 |
| `:compiler:mangling` | 新建 | 低 — 阶段 9 开发时创建 |
| `:compiler:chir` | 新建 | 低 — 阶段 11 开发时创建 |
| `:compiler:codegen` | 新建 | 低 — 阶段 12 开发时创建 |

### 需要删除或合并的模块

| 模块 | 操作 | 原因 |
|---|---|---|
| `:cfir:cfir-common-psi` | 删除 | 空模块，无源码，无依赖者 |
| `:cfir:raw-cfir`（父模块） | 保留为聚合 | 不含源码，仅作子模块分组 |

### 需要修复的依赖方向

| 当前依赖 | 修正为 | 原因 |
|---|---|---|
| `:cfir:resolve` → `:cfir:checkers` | `:cfir:checkers` → `:cfir:resolve` | checkers 在 resolve 之后运行，应该是 checkers 依赖 resolve |

### 需要统一的包名

| 当前包名 | 统一为 | 涉及模块 |
|---|---|---|
| `org.cangnova.cangjie.*` | `org.cangjie.*` | `:common`, `:util`, `:psi`, `:compiler:cli` |

---

## settings.gradle.kts 目标形态

```kotlin
// ===== 基础设施 =====
include(":util")
include(":common")
include(":generators")
include(":dependencies:intellij-core")

// ===== 编译器配置 =====
include(":compiler:config")
include(":compiler:cli")
include(":compiler:frontend.common")

// ===== PSI =====
include(":psi")

// ===== CFIR 数据模型 =====
include(":cfir:cfir-common")
include(":cfir:diagnostics")
include(":cfir:cfir-cones")
include(":cfir:cfir-tree")
include(":cfir:cfir-tree:tree-generator")

// ===== CFIR 构建 (阶段 6) =====
include(":cfir:raw-cfir:raw-cfir-common")
include(":cfir:raw-cfir:psi2cfir")
include(":cfir:raw-cfir:light-tree2cfir")

// ===== CFIR 语义 (阶段 7) =====
include(":cfir:providers")
include(":cfir:resolve")
include(":cfir:semantics")

// ===== 诊断检查 (阶段 7 末尾) =====
include(":cfir:checkers")
include(":cfir:checkers:checkers-component-generator")
include(":cfir:diagnostic-renderers")

// ===== CFIR 入口 =====
include(":cfir:entrypoint")

// ===== 仓颉特有阶段 (按需启用) =====
// include(":compiler:condition-compile")   // 阶段 3
// include(":compiler:macro")               // 阶段 5
// include(":compiler:finalize")            // 阶段 8
// include(":compiler:mangling")            // 阶段 9

// ===== 序列化 (按需启用) =====
// include(":cfir:serialization")           // 阶段 10
// include(":cfir:deserialization")         // 阶段 4

// ===== 后端 (按需启用) =====
// include(":compiler:chir")               // 阶段 11
// include(":compiler:codegen")            // 阶段 12

// ===== Analysis API =====
include(":analysis:analysis-api")
include(":analysis:analysis-api-impl-base")
include(":analysis:analysis-api-cfir")
include(":analysis:analysis-test-framework")

// ===== 测试 =====
include(":tests:test-infrastructure")

// ===== 其他 =====
include(":flatbuffers-gen")
```
