# Cangjie 模块 ↔ Kotlin K2 模块对照表

> 以 `compiler-module-design.md` 中规划的仓颉编译器模块为主线，逐一对照 Kotlin K2 编译器中的对应模块。
> 标注哪些是 1:1 映射、哪些是合并/拆分、哪些是仓颉独有。
>
> **现状提示（2026-05-11）**：表格描述的是模块**对照关系**（设计层面），不是实装状态。文中标为"（规划）"的 `:cfir:symbols` 当前已演化为 `:cfir:semantics` + `:cfir:providers`；其它实装状态以 [`current-module-organization.md`](current-module-organization.md) 为准。

---

## 对照总表

### 基础设施

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:util` | `compiler/util` | 1:1 | 纯工具。K2 另有 `util-io`、`util-klib`、`util-klib-abi` 等细分，仓颉暂不需要 |
| `:common` | `core/compiler.common` | 1:1 | 名称系统、修饰符、基本类型。K2 另有 `.jvm`/`.js`/`.native` 平台变体，仓颉单目标无需拆分 |
| `:compiler:config` | `compiler/config` | 1:1 | 编译器配置。K2 另有 `config.jvm`，仓颉无需 |

### CFIR 数据模型 ↔ FIR 数据模型

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:cfir-common` | `compiler/fir/cones`（session 部分） | 部分对应 | K2 的 `cones` 既包含 Session/SessionComponent 又包含 ConeKotlinType。仓颉把 Session 部分放在 `cfir-common`，类型部分放在 `cfir-cones` |
| `:cfir:cfir-cones` | `compiler/fir/cones`（类型部分） | 部分对应 | ConeCangJieType ↔ ConeKotlinType。K2 的 `cones` 还含 `symbols/`、`diagnostics/`、`resolve/`、`renderer/`、`util/`，仓颉拆得更细 |
| `:cfir:cfir-tree` | `compiler/fir/tree` | 1:1 | IR 节点、访问者、Transformer。结构高度对齐 |
| `:cfir:cfir-tree:tree-generator` | `compiler/fir/tree/tree-generator` | 1:1 | 代码生成器 |
| — | `core/descriptors` | 无对应 | K1 遗留的描述符系统，K2 通过 FIR 符号替代，仓颉无此历史包袱 |
| — | `core/metadata` | 无对应 | Kotlin metadata 格式（.kotlin_module），仓颉使用 .cjo（FlatBuffers） |

### 诊断子系统

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:diagnostics`（规划） | `compiler/fir/cones` 中的 `diagnostics/` | 拆出独立 | K2 的诊断核心分散在 `cones/diagnostics/` 和 `checkers/gen/`。仓颉规划将框架部分集中为独立模块 |
| `:cfir:diagnostic-renderers` | `compiler/fir/diagnostic-renderers` | 1:1 | 诊断消息的人类可读渲染 |
| `:cfir:checkers:checkers-component-generator` | `compiler/fir/checkers/checkers-component-generator` | 1:1 | 检查器组件代码生成 |

### 源码表示

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:psi` | `compiler/psi` | 1:1 | 词法分析、语法分析、PSI 节点定义。仓颉用 JFlex + PsiParser，Kotlin 同理 |

### IR 构建（阶段 6）

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:raw-cfir:raw-cfir-common` | `compiler/fir/raw-fir/raw-fir.common` | 1:1 | 共享基类 `AbstractRawCfirBuilder<T>` ↔ `AbstractRawFirBuilder` |
| `:cfir:raw-cfir:psi2cfir` | `compiler/fir/raw-fir/psi2fir` | 1:1 | PSI → Raw IR |
| `:cfir:raw-cfir:light-tree2cfir` | `compiler/fir/raw-fir/light-tree2fir` | 1:1 | LightTree → Raw IR |

### 符号管理与语义

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:symbols`（规划） | `compiler/fir/providers` | 对应 | K2 `providers` 含 SymbolProvider、Scope 工具、声明工具函数、可见性检查。仓颉 `symbols` 范围类似 |
| `:cfir:semantics`（规划） | `compiler/fir/semantics` | 对应 | K2 `semantics` 含 TypeUtils、ScopeUtils、EffectiveVisibilityUtils 等纯函数。仓颉对齐 |
| `:cfir:resolve` | `compiler/fir/resolve` | 1:1 | 多 Phase 语义解析引擎。K2 内部有 `resolve/`、`scopes/`、`extensions/` 子包 |
| `:cfir:checkers` | `compiler/fir/checkers/src` | 1:1 | 检查器定义与公共检查器。K2 另有 `checkers.jvm`/`.js`/`.native`/`.wasm`/`.web.common` 平台变体，仓颉单目标无需 |
| — | `compiler/fir/fir-jvm` | 无对应 | JVM 平台特定解析（Java 互操作、模块系统）。仓颉无 JVM 目标 |
| — | `compiler/fir/fir-js` | 无对应 | JS 平台特定。仓颉无 JS 目标 |
| — | `compiler/fir/fir-native` | 无对应 | Native 平台特定（ObjC 互操作等）。仓颉无此需求 |

### CFIR 入口与编排

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:entrypoint`（规划） | `compiler/fir/entrypoint` | 对应 | K2 `entrypoint` 含 `pipeline/`（FirModuleResolveState 编排）、`session/`（会话构建）、`extensions/`、`checkers/`。仓颉功能类似 |
| — | `compiler/fir/plugin-utils` | 暂无对应 | K2 编译器插件工具。仓颉插件系统尚未设计 |
| — | `compiler/fir/dump` | 暂无对应 | FIR 树的调试输出工具。仓颉可在 `cfir-tree` 内用 CfirRenderer 替代 |

### 序列化

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:cfir:serialization`（规划） | `compiler/fir/fir-serialization` | 对应 | K2 序列化为 Kotlin metadata（ProtoBuf），仓颉序列化为 .cjo（FlatBuffers）。格式不同，职责相同 |
| `:cfir:deserialization`（规划） | `compiler/fir/fir-deserialization` | 对应 | K2 反序列化含 `deserialization/` + `resolve/` 子包。仓颉需要类似结构来支持 IMPORT_PACKAGE |
| `:flatbuffers-gen` | — | 仓颉独有 | FlatBuffers Schema 生成代码。K2 使用 ProtoBuf，无此模块 |

### 后端

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:backend:chir`（规划） | `compiler/fir/fir2ir` + `compiler/ir/ir.tree` | 合并对应 | K2 的 `fir2ir` 负责 FIR → IR 转换，`ir.tree` 定义后端 IR 节点。仓颉的 CHIR 将数据模型和转换放在同一模块 |
| `:backend:codegen`（规划） | `compiler/backend` + `compiler/ir/backend.*` | 合并对应 | K2 按平台拆分后端（`backend.jvm`/`.js`/`.native`/`.wasm`），仓颉单目标（LLVM）只需一个模块 |
| — | `compiler/ir/ir.tree` | 对应 CHIR 数据模型 | K2 的 Backend IR 节点定义。仓颉的 CHIR 节点定义在 `:backend:chir` 内部 |
| — | `compiler/ir/ir.psi2ir` | 无对应 | K1 遗留的 PSI → IR 路径，K2 通过 fir2ir 替代。仓颉无此历史包袱 |
| — | `compiler/ir/ir.inline` | 暂无对应 | IR 级内联。仓颉的内联在 CHIR 优化 Pass 中处理 |
| — | `compiler/ir/ir.interpreter` | 暂无对应 | 编译期常量求值。仓颉暂未规划 |
| — | `compiler/ir/serialization.*` | 暂无对应 | K2 的 klib IR 序列化。仓颉使用 .cjo 在 CFIR 层序列化，不在 CHIR 层 |

### 源码变换（仓颉独有）

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:compiler:condition-compile`（规划） | — | 仓颉独有 | @When 条件编译。Kotlin 通过 expect/actual 处理多平台，无条件编译 |
| `:compiler:macro`（规划） | — | 仓颉独有 | 宏展开系统。Kotlin 无宏，使用编译器插件在 FIR Phase 间生成合成节点 |

### IR 变换（仓颉独有）

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:compiler:finalize`（规划） | `compiler/fir/fir2ir`（部分） | 部分对应 | K2 在 fir2ir 过程中做脱糖。仓颉的泛型单态化和溢出策略标注是独有的（Kotlin 使用类型擦除，不做单态化） |
| `:compiler:mangling`（规划） | — | 仓颉独有 | 独立的名称修饰阶段。K2 的 mangling 分散在各平台后端（`ir.backend.common`）中，不作为独立编译阶段 |

### 编译驱动

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:compiler:frontend` | `compiler/cli` | 调整后对应 | Kotlin 将前端编排放在 cli 模块内，仓颉现改为无 CLI 语义的前端基础设施模块 |
| `:compiler:pipeline`（规划） | `compiler/cli`（内部） | 对应 | K2 的管线编排在 cli 模块内部。仓颉若未来独立出来，将建立在 frontend 模块之上 |
| `:compiler:plugins`（规划） | `compiler/plugin-api` | 对应 | K2 的编译器插件 API。仓颉的插件系统（MetaTransform）机制不同 |
| — | `compiler/frontend` | 无对应 | K1 旧前端。仓颉无此历史包袱 |
| — | `compiler/frontend.common` | 部分对应 cfir-common | K2 的前端通用设施（SourceElement 桥接等）。仓颉目前放在 `cfir-common` 中 |
| — | `compiler/frontend.common-psi` | 暂无对应 | PSI 专属前端通用设施。仓颉暂不需要独立模块 |
| — | `compiler/frontend.java` | 无对应 | Java 源码解析。仓颉无 Java 互操作 |
| — | `compiler/resolution` | 无对应 | K1 旧解析。仓颉无此历史包袱 |
| — | `compiler/serialization` | 对应 cfir:serialization | K1/K2 共享的序列化基础。仓颉用 FlatBuffers |
| — | `compiler/incremental-compilation-impl` | 暂无对应 | 增量编译。仓颉暂未规划独立模块（逻辑内嵌在 CFIR_BUILD） |

### Analysis API

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:analysis:analysis-api` | `analysis/analysis-api` | 1:1 | 公共分析接口 |
| `:analysis:analysis-api-impl-base` | `analysis/analysis-api-impl-base` | 1:1 | 基础实现 |
| `:analysis:analysis-api-cfir` | `analysis/analysis-api-fir` | 1:1 | FIR/CFIR 后端实现 |
| `:analysis:analysis-test-framework` | `analysis/analysis-test-framework` | 1:1 | 测试框架 |
| — | `analysis/analysis-api-fe10` | 无对应 | K1 后端实现。仓颉无 K1 |
| — | `analysis/analysis-api-standalone` | 暂无对应 | 脱离 IDE 的独立分析入口。仓颉暂不需要 |
| — | `analysis/analysis-api-platform-interface` | 暂无对应 | 平台接口层。仓颉单目标暂不需要 |
| — | `analysis/low-level-api-fir` | 暂无对应 | FIR 底层访问 API（惰性解析调度、模块级缓存）。仓颉可能在 `analysis-api-cfir` 内部实现 |
| — | `analysis/analysis-internal-utils` | 暂无对应 | 分析内部工具。仓颉规模较小暂不需要 |
| — | `analysis/symbol-light-declarations` | 无对应 | 仓颉的只读声明视图投影，不涉及 Java PSI 互操作 |
| — | `analysis/decompiled` | 无对应 | .class 反编译支持。仓颉无 JVM 目标 |

### 测试基础设施

| 仓颉模块 | Kotlin K2 模块 | 关系 | 说明 |
|---|---|---|---|
| `:tests:test-infrastructure` | `compiler/test-infrastructure` | 1:1 | 编译器测试基础设施 |
| `:generators` | — | 仓颉独有 | 构建时代码生成统一入口 |
| `:dependencies:intellij-core` | — | 仓颉独有 | IntelliJ 依赖聚合（K2 直接在根 build 脚本中处理） |

---

## K2 模块中仓颉不需要的部分（及原因）

| K2 模块 | 不需要的原因 |
|---|---|
| `core/compiler.common.jvm` / `.js` / `.native` / `.wasm` / `.web` | 仓颉单目标（LLVM），无多平台变体 |
| `core/descriptors` / `descriptors.jvm` / `descriptors.runtime` | K1 描述符系统，仓颉无历史包袱 |
| `compiler/frontend` / `frontend.java` / `resolution` | K1 旧前端，仓颉直接使用 CFIR |
| `compiler/fir/fir-jvm` / `fir-js` / `fir-native` | 平台特定 FIR 扩展，仓颉不分平台 |
| `compiler/fir/checkers/checkers.jvm` / `.js` / `.native` / `.wasm` / `.web.common` | 平台特定检查器，仓颉不分平台 |
| `compiler/ir/backend.jvm` / `backend.js` / `backend.native` / `backend.wasm` | 平台后端，仓颉统一走 LLVM |
| `compiler/ir/ir.psi2ir` | K1 → IR 桥接，仓颉无 K1 |
| `compiler/ir/serialization.jvm` / `.js` / `.native` | 平台 klib 序列化，仓颉用 .cjo |
| `compiler/light-classes` / `analysis/symbol-light-declarations` | Kotlin → Java Light Class；仓颉改为非 Java 的 declaration view 体系 |
| `analysis/analysis-api-fe10` | K1 分析后端，仓颉无 K1 |
| `analysis/decompiled` / `analysis/stubs` | .class 反编译/桩，仓颉无 JVM |
| `compiler/javac-wrapper` | Java 编译器包装，仓颉无 Java 互操作 |
| `compiler/multiplatform-parsing` | 多平台解析，仓颉单目标 |

---

## 仓颉独有模块（K2 无对应）

| 仓颉模块 | 原因 |
|---|---|
| `:compiler:condition-compile` | 仓颉有 @When 条件编译，Kotlin 用 expect/actual |
| `:compiler:macro` | 仓颉有独立宏系统，Kotlin 用编译器插件替代 |
| `:compiler:finalize`（泛型单态化部分） | Kotlin 使用类型擦除（JVM）或 IR 降级，不做源码级单态化 |
| `:compiler:mangling` | Kotlin 的 mangling 分散在各平台后端，不作为独立编译阶段 |
| `:flatbuffers-gen` | .cjo 使用 FlatBuffers 格式，Kotlin 使用 ProtoBuf |
| `:backend:chir` | 仓颉的 CHIR 是基于 CFG 的高级 IR，K2 的后端 IR 是基于 Tree 的 |

---

## 模块数量对比

| 类别 | Kotlin K2 | 仓颉（规划） | 差异原因 |
|---|---|---|---|
| 基础设施 | ~15（含平台变体） | 3 | 仓颉单目标，无平台变体 |
| FIR/CFIR 数据模型 | 3（cones + tree + tree-generator） | 4（cfir-common + cfir-cones + cfir-tree + generator） | 仓颉将 Session 从 cones 中拆出 |
| FIR/CFIR 处理 | ~12（含平台变体） | ~8 | 仓颉无平台特定模块 |
| 后端 IR | ~15（含平台后端） | 2 | 仓颉统一 LLVM 后端 |
| Analysis API | ~12 | 4 | 仓颉无 K1 后端、无 standalone、无 light-classes |
| 仓颉独有 | 0 | ~5 | 条件编译、宏、单态化、mangling、FlatBuffers |
| **总计** | **~60+** | **~26** | 仓颉无多平台、无 K1 兼容、无 Java 互操作 |
