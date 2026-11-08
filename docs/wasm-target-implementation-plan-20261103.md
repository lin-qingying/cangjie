# Wasm 目标实现计划

- 状态：草案（待评审）
- 日期：2026-11-03
- 范围：`wasm` 通用层（wasm-generic）+ `wasm-wasi` 目标
- 明确不在本期范围：`wasm-js`、JS 运行时、浏览器调试（见 §8）

## 1. 背景与目标

本项目当前只有一条目标通路：CFIR → CHIR → LLVM IR → LLVM 目标机器 → native 目标文件。
本计划为编译器增加 WebAssembly 目标支持，对标 Kotlin/Wasm 的目标组织方式：

1. **wasm 通用层**：所有 wasm 目标共享的基础设施——工具链、目标配置、ABI/数据模型约定、
   lowering 的 wasm 约束、目标文件发射与链接、测试框架。
2. **wasm-wasi 目标**：本期第一个落地目标，产出可在 Wasmtime / WasmEdge / Wazero / Node.js
   等 WASI 宿主上直接运行的独立 `.wasm` 程序。

### 1.1 与 Kotlin/Wasm 的对标与差异

本地参考镜像 `external/kotlin/wasm/` 的模块划分：

| Kotlin 模块 | 职责 | 本项目对应 |
|---|---|---|
| `wasm.config` | `WasmTarget` 枚举（`JS("wasm-js")` / `WASI("wasm-wasi")`）、`WasmPlatform` | `compiler:codegen` 内新增 `WasmTarget` 枚举 + 目标预设 |
| `wasm.frontend` | wasm 相关的 analyze / resolve / diagnostics | 前期不单设，wasm 前端约束并入既有 analysis/checkers |
| `wasm.ir` | `WasmInstruction`、`WasmExpressionBuilder`、`WasmIrToBinary`、`WasmBinaryToIR` 等——**直接 lowering 到 wasm 指令并输出 wasm 二进制，不经过 LLVM** | **不照搬**：复用既有 CHIR→LLVM 管线，wasm 作为 LLVM 目标（见 §4.1 路线决策） |
| `wasm.tests` | wasm 目标测试 | codegen parity fixtures（wasm 变体）+ wasm E2E fixtures |
| `core/compiler.common.wasm`、`generators/wasm` | 跨模块共享的 wasm 配置与生成器 | 前期放入既有模块，膨胀后再拆独立模块 |
| `wasm.debug.browsers` | 浏览器调试 | 不在本期范围 |

**核心差异**：Kotlin/Wasm 从零自研了 wasm 指令发射器（因为它的 wasm 目标独立于
Kotlin/Native 工具链，`external/kotlin/native/llvm` 中无任何 wasm 代码）；
本项目已有 CHIR→LLVM 管线和 `llvm-interop` 基础设施，LLVM 的 WebAssembly 后端是
一等公民（`wasm32-unknown-unknown` 与 `wasm32-wasi` 均为官方 target），因此走
LLVM 路线是成本最低、且与官方 cjc（同样 LLVM-based）架构一致的选择。

## 2. 现状盘点（本计划的事实依据）

| # | 事实 | 证据 |
|---|---|---|
| 1 | 既有管线已成型：CFIR → CHIR → LLVM IR → 目标机器 → 目标文件 | `chir/chir-tree`、`compiler:codegen`（`DefaultChirToLlvmCodeGenerator`）、`llvm-interop-jni` |
| 2 | 目标参数已是 triple 驱动的通用抽象：`CodegenOptions` 含 `targetTriple`（当前占位值 `"unknown-cangjie"`）、`targetDataLayout`、`targetCpu`、`targetFeatures`、`relocationMode`、`codeModel`，`requiredLlvmMajorVersion = 18` | `compiler/codegen/src/org/cangnova/cangjie/codegen/api/CodegenOptions.kt` |
| 3 | JNI 互操作层**纯 triple 驱动**：`targetCreateMachine(triple, cpu, features, ...)` 经 `LLVMGetTargetFromTriple` 解析目标，新增 wasm 目标在互操作层零 Kotlin 改动 | `llvm-interop/llvm-interop-jni/native/jni_target.cpp` |
| 4 | 打包的 LLVM 15.0.4（`llvm-interop/third_party/llvm/`）**未编译 WebAssembly 后端**：`llc --version` 仅注册 aarch64/arm/x86 系目标；`wasm-ld.exe` 存在但缺 DLL 不可用（exit 127） | 实测 |
| 5 | 打包 LLVM 版本（15）与 codegen 声明需求（18）不一致，工具链本就需要重打 | `CodegenOptions.requiredLlvmMajorVersion` |
| 6 | CHIR→LLVM lowering 尚不完整：表达式 dispatch 基于操作符字符串、控制流/phi/异常语义不全、运行时集成仅骨架、parity fixtures 最小（`simple-return` 为主） | `compiler/codegen/docs/chir-codegen-state-inventory.md` |
| 7 | 运行时（`external/cangjie_runtime`，C++）零 wasm 构建支持；含 `CJThread`/`Concurrency`/`Sync`/`Signal`/`CjTimer`/`Loader` 等组件，其中线程与信号在 wasm 下不可用 | 全树搜索无 wasm 构建配置 |
| 8 | 官方 cjc（`external/cangjie_compiler`）没有 wasm 后端——本目标是本项目增量，ABI 约定由本项目定义 | 全树搜索仅 `third_party/llvmPatch.diff` 命中且与 wasm 实现无关 |

## 3. 总体架构

### 3.1 管线

```
.cj 源
  │  frontend / CFIR（既有，目标无关）
  ▼
CFIR
  │  cfir2chir（既有，目标无关）
  ▼
CHIR
  │  CHIR→LLVM IR lowering（既有 compiler:codegen，wasm 约束见 §5.2）
  ▼
LLVM IR（target triple = wasm32-wasi）
  │  LLVM WebAssembly 后端（llvm-interop-jni，triple 驱动，零改动）
  ▼
.wasm.o 目标文件
  │  wasm-ld + wasi-libc + 运行时/标准库 wasm 产物（wasi-sdk 工具链）
  ▼
program.wasm  ──►  Wasmtime / WasmEdge / Wazero / Node.js
```

### 3.2 两层划分

**wasm 通用层**（wasm-generic，所有 wasm 目标共享）：

- LLVM 工具链：内置 WebAssembly 后端的 LLVM 构建（§5.1）。
- 目标配置：`WasmTarget` 枚举、triple/data layout/CPU/features/relocation/code model
  预设（§5.2 的 ABI 约定表即其内容）。
- lowering 的 wasm 约束检查（§5.2~§5.5）：32 位指针、无 C++ EH、单线性内存、递归深度。
- 目标文件发射与链接工具契约（wasm-ld）。
- 测试框架：parity fixtures 的 wasm 变体 + wasm 宿主运行验证（§6 P4）。

**wasm-wasi 目标**（本期落地）：

- triple：`wasm32-wasi`（wasi-libc 为 musl 系；宿主为 WASI 兼容运行时）。
- 运行时的 wasm32-wasi 移植（单线程 v1，§6 P3）。
- stdlib 的 wasm32-wasi 交叉构建。
- E2E：`.cj → .wasm`，Wasmtime + Node.js 双宿主验证。

预留（不在本期）：`wasm32-unknown-unknown`（无 OS、纯 import/export 的独立模块，
面向嵌入式/自定义宿主）；`wasm64-*`（LLVM 实验性，地址空间 2^64）。

### 3.3 模块落位（前期最小化）

| 内容 | 落位 |
|---|---|
| `WasmTarget` 枚举与目标预设 | `compiler:codegen` 新包 `org.cangnova.cangjie.codegen.target` |
| wasm 目标接入（driver 层） | `compiler:entrypoint`（编译驱动入口，随现有 target 参数透传） |
| wasm 模块契约/ABI 文档 | `compiler/codegen/docs/wasm-module-contract.md`（新建，随 P1） |
| 运行时/stdlib 的 wasm 构建 | `external/cangjie_runtime` 内新增 wasm 构建配置（只读镜像外的一方构建脚本，放 `tools/wasm/` 或运行时仓内 CMake 变体，P3 时定） |
| E2E 测试 | `compiler/codegen/tests`（parity）+ 新建 `tests/wasm-e2e`（P4） |

按 AGENTS.md §10 接口优先原则：wasm 模块契约（导出表、import 表、错误码、内存布局）
先成文、后实现；若 wasm 专用代码量增长，再拆 `compiler:wasm-config` 独立模块，
不在本期预建空模块。

## 4. 路线决策：为什么 v1 走 LLVM 而不是 Kotlin 式直接发射

### 4.1 两条路线

- **路线 A（v1 采用）**：复用既有 CHIR→LLVM 管线，wasm 作为 LLVM 的 WebAssembly
  目标（triple 驱动，JNI 层零改动）。
- **路线 B（Kotlin 式）**：wasm 独立于 Native/LLVM 工具链，自持一套
  CHIR→wasm IR→wasm 二进制发射器。Kotlin 的实证形态（本地镜像
  `external/kotlin/wasm/wasm.ir/`）：`WasmInstruction` 指令集 +
  `WasmExpressionBuilder` + `WasmIrToBinary` 二进制编码 + `WasmBinaryToIR`
  解码 + `convertors/InstructionsFlowOptimizers` 指令级优化 + `debug/`；
  且 `external/kotlin/native/llvm/` 中无任何 wasm 代码，确认与 Native 工具链完全解耦。

### 4.2 Kotlin 为什么选择独立（背景，非全部可考证，标注推断处）

1. JS 目标（`js.translator` 直接发射 JS）早于 LLVM 可用 JS 后端，直接发射成为
   既定架构，wasm 沿用了同一哲学；
2. **wasm-js 双运行时设计是强动因**：运行时同一份源码编成 JS 与 wasm 两半并互相
   调用，两半 ABI 必须受自己精确控制，自研发射器是为该设计服务的（推断）；
3. 大团队可负担长期维护第二套完整 codegen（发射器+优化+二进制格式+调试）。

仓颉侧：wasm-js 已排除（§8），动因 2 不存在；动因 3 与本项目现状相反
（§2 #6：共享 lowering 尚未完成）。

### 4.3 对比

| 维度 | 路线 A：复用 CHIR→LLVM | 路线 B：Kotlin 式直接发射 |
|---|---|---|
| 新增工作量 | 目标配置 + 工具链 + 运行时移植 | 全套 wasm 指令发射器 + 优化器 + 调试信息 + 二进制格式（`external/kotlin/wasm/` 可作移植起点，非从零，但仍是大量移植与适配） |
| lowering 成本 | 与 native 目标**共享**（§2 #6 的缺口补一次、两个目标受益） | native 与 wasm 各一套，总 codegen 投入 ≈ 两套 |
| 优化器 | LLVM 工业级 O0/O1/O2 直接可用 | 自研（Kotlin 参考为有限指令级优化），否则产物性能/体积受损 |
| 调试信息 | LLVM wasm 后端已有 DWARF（成熟度持续提升） | 自研 debug section（DWARF 手写） |
| 工具链解耦 | 依赖捆绑 LLVM（wasm 目标受其版本/运维牵制，§2 #4/#5） | wasm 目标纯 Kotlin 实现，不碰原生工具链、不被 LLVM 版本问题阻塞 |
| 语义隔离 | wasm 约束（无 EH/32 位指针/单内存）以条件分支进入共享 lowering | wasm 语义独立演进，不污染共享 lowering |
| 发布节奏 | 与 native 后端共同节奏 | wasm 目标独立演进 |
| 与官方 cjc 一致性 | 一致（cjc 即 LLVM-based；本仓 parity 锚点为官方 LLVM IR，`compiler/codegen/testData/official-generated-llvm-o*/`） | wasm 目标本身官方无参照，不构成偏离，但也失去「复用经官方基线验证的 lowering」的加成 |
| 对 JNI 层改动 | 零（已 triple 驱动，§2 #3） | 零（管线另起，JNI 层闲置不用） |
| 分发体积 | 无变化（native 目标本已捆绑 LLVM） | 无变化（native 仍需 LLVM，wasm 侧不减少总体积） |

### 4.4 路线 B 的真实收益（不回避）

1. wasm 目标**不被工具链问题阻塞**：§2 #4/#5 的全部痛点（缺 wasm 后端、版本错位、
   坏 `wasm-ld.exe`）对路线 B 不存在；
2. **语义隔离**：wasm 约束不进入共享 lowering；
3. **参考实现现成**：`external/kotlin/wasm/` 在本仓库内，移植成本显著低于自研；
4. **独立演进**：不受 native 后端进度与 LLVM 运维牵制。

### 4.5 决策与留缝设计

**v1 决策：路线 A。** 理由：wasm 对编译器的要求（ILP32、32 位指针、无 EH、单内存）
全部是 LLVM wasm 后端原生支持，无需要绕过 LLVM 的硬需求；且本项目共享 lowering
尚未完成，先补一套让两个目标共享，比同时做两套更省。

为保留切换到路线 B 的能力，设计上留两处缝：

1. **后端边界接口化**：wasm 目标的 codegen 入口收敛在 CHIR 之下的接口层
   （现有 `LlvmBackendApi` / `LlvmBackendKind` 即此形状），未来以直接发射器替换
   时 CHIR 以上零改动；
2. **明确再评估触发条件**（任一成立即启动路线 B 评估，移植起点为
   `external/kotlin/wasm/wasm.ir`）：
   - P0 重打 LLVM 暴露出超出预期的工具链/运维问题（P0 结果本身是决策输入）；
   - LLVM wasm 后端在调试信息 / LTO 等能力上持续不满足产品要求；
   - 捆绑 LLVM 的版本运维成为常态性阻塞；
   - wasm-js（或任何要求运行时 JS/wasm 双半 ABI 精确互控的目标）重新进入范围。

## 5. 关键技术点

### 5.1 工具链（双来源）

| 用途 | 来源 | 说明 |
|---|---|---|
| JVM 内 codegen（`llvm-interop-jni` 链接的 LLVM） | **重打 LLVM 18**（对齐 `requiredLlvmMajorVersion`），`-DLLVM_TARGETS_TO_BUILD="host;WebAssembly"` | 修复现状 #4/#5；打包产物替换 `llvm-interop/third_party/llvm/` |
| 链接 / wasi-libc / 运行时交叉编译 / 产物验证 | **wasi-sdk**（自带 LLVM+clang、wasi-libc、wasm-ld、sysroot） | 不依赖重打 LLVM 的链接器；`wasm-ld` 用 wasi-sdk 版，绕开现状 #4 的坏 `wasm-ld.exe` |

### 5.2 ABI 与数据模型（wasm32）

| 项 | 约定 |
|---|---|
| 位宽模型 | ILP32：i32 为自然指针位宽 |
| 指针 | 一律 i32；CHIR 指针/引用类型 lowering 为 i32（对齐 4 字节） |
| 64 位数据 | i64 为合法数据类型（Cangjie 64 位整型直接映射），仅指针不受 64 位化 |
| 地址空间 | 单一线性内存，上限 4 GiB（64 KiB 页 × 65536 页）；`memory.grow` 扩页 |
| data layout | 以 `LLVMCreateTargetMachine` 返回值（`LLVMGetDataLayoutStr`）为准，不在本计划硬编码 |
| relocation / code model | STATIC / SMALL（wasm 无 PIC 需求，v1 固定） |
| 对齐 | 遵循 wasm 规范（i64 对齐 8，v128 对齐 16） |

### 5.3 栈与递归

wasm 调用栈独立于线性内存，由引擎管理且有深度上限（各引擎数千至数万帧量级）。
约束：

- 深度递归场景（尾递归除外）在 v1 文档中标注为已知限制；
- 函数 lowering 不假设无界栈（现有 native 栈溢出保护语义在 wasm 下由引擎栈截断兜底）；
- 尾调用：不假设引擎支持 tail-call 提案，lowering 按普通调用生成。

### 5.4 异常处理（P3 需先审计）

wasm 无 C++ 异常展开基础设施（wasm EH 提案引擎支持不一致，v1 不依赖）。
Cangjie 运行时的 `Exception`/`ExceptionManager`/`UnwindStack` 在 wasm 下的适配
依赖其现有实现方式（C++ EH 还是用户态展开），**P3 第一个任务是审计**，选项：

| 选项 | 说明 | 评估 |
|---|---|---|
| a. wasm EH 提案 | `try_table` 等 | 引擎支持不一致，v1 否决 |
| b. 运行时异常改返回码/用户态展开 | C++ 运行时移植时关闭 EH（`-fno-exceptions`），异常走显式状态传递 | 推荐，改动集中在运行时 |
| c. 编译器侧显式 result 传播 | 语言层改动 | 影响面大，不选 |

同时要求 CHIR→LLVM lowering 的 `throwUnwind` 路径（现有 parity fixture 已覆盖
`throwUnwind`）在 wasm 目标下按选项 b 的约定生成，不复用 LLVM EH 机制。

### 5.5 线程（v1 单线程）

- v1 单线程：运行时 `CJThread`/`Concurrency`/`Sync` 禁用或桩化，`Signal`/`SignalManager` 桩化；
- 语言侧：wasm-wasi 目标下禁止使用线程相关 API（前端诊断，P4 前完成）；
- 多线程路线（wasi-threads / Web Worker）独立立项，不在本期。

### 5.6 运行时组件归属（wasm-wasi v1）

| 运行时组件 | wasm 下处理 |
|---|---|
| `Heap`/`HeapManager`/`Mutator`/`TracingCollector` | 保留，`mmap` → `memory.grow` 适配 |
| `Exception`/`ExceptionManager`/`UnwindStack` | 按 §5.4 选项 b 改造 |
| `CJThread`/`Concurrency`/`Sync` | v1 禁用/桩 |
| `Signal`/`SignalManager` | 桩 |
| `CjTimer`/`CjScheduler` | wasi clocks / 单线程协作调度 |
| `Loader`/`LoaderManager` | wasi fs 读取 .cj 包 |
| `LogManager` | 桥接 wasi stdout/stderr |
| `Inspector`/`CpuProfiler`/`Sanitizer` | v1 关闭 |
| `TypeInfoManager`/`Demangler`/`StackManager`/`StackMap` | 保留（纯内存操作） |
| `arch/`、`os/` | 新增 wasm 适配层 |

## 6. 分阶段计划

### P0 工具链就绪（约 1 周）

内容：

1. 重打 LLVM 18：`-DLLVM_TARGETS_TO_BUILD="host;WebAssembly"`，替换
   `llvm-interop/third_party/llvm/`（保留旧构建可回滚）。
2. 引入 wasi-sdk（工具链目录纳入 `third_party` 或构建时下载缓存）。
3. 冒烟验证：手写 LLVM IR → `llc -mtriple=wasm32-wasi` → wasm-ld 链接 wasi-libc → wasmtime 运行。

验收（DoD）：

- `llc --version` 注册目标包含 `wasm32`/`wasm64`；
- hello-world 程序在 wasmtime 下运行输出正确；
- `llvm-interop-jni` 集成测试在新 LLVM 构建下仍全绿（回归）。

### P1 目标接线（约 1~2 周）

内容：

1. `WasmTarget` 枚举（`WASI`；`UNKNOWN_UNKNOWN` 预留不实现）+ 目标预设
   （triple `wasm32-wasi`、relocation STATIC、code model SMALL、data layout 取自 TargetMachine）。
2. 驱动层（`compiler:entrypoint`）透传 wasm 目标参数。
3. 新建 `compiler/codegen/docs/wasm-module-contract.md`：导出表（入口、memory）、
   import 表、错误码、内存布局的正式契约。
4. JNI 侧验证：`targetCreateMachine("wasm32-wasi", ...)` + `targetMachineEmitObjectBytes`
   产出 wasm 目标文件（此时无需完整 lowering，手工 LLVM IR 即可）。

验收（DoD）：

- 单测：wasm32-wasi TargetMachine 创建与目标文件发射通过；
- 目标参数从驱动入口到 JNI 全链路贯通（集成测试覆盖）；
- 契约文档评审通过。

### P2 lowering 补全（主体工作量，与 native 目标共享）

内容（对照 `chir-codegen-state-inventory.md` 的 gap 清单）：

1. 字符串 dispatch → 显式语义族（表达式/控制流/调用/全局）。
2. 控制流/phi/异常 lowering（按 §5.4 选项 b，wasm 目标不复用 LLVM EH）。
3. 运行时符号表与调用约定在 wasm 目标下的落地（i32 指针、STATIC relocation）。
4. wasm 约束检查器：32 位指针、单内存、无 EH、递归深度告警。

验收（DoD）：

- `compiler/codegen/testResources/chir-parity` 的 basic + advanced 全集在
  wasm32-wasi 下 lowering 通过（IR 级 verify + 目标文件级运行验证）；
- native 目标（x86_64/aarch64）parity 不回归。

### P3 运行时 + stdlib 移植（约 2~4 周）

内容：

1. **审计**（第一个任务）：运行时异常机制现状（§5.4）、内存分配路径、平台相关代码清单。
2. wasi-sdk clang `--target=wasm32-wasi --sysroot=<wasi-sdk sysroot>` 交叉编译运行时，
   按 §5.6 表处理各组件。
3. stdlib 可移植性审计 + wasm32-wasi 交叉构建。
4. 运行时入口约定对齐 `wasm-module-contract.md`（main、memory、data 段）。

验收（DoD）：

- 最小运行时程序（print / 堆分配 / 异常抛出与捕获）在 wasmtime 下行为正确；
- stdlib 核心子集（字符串、集合、基础 IO）通过 wasm 下的运行时自测。

### P4 wasm-wasi E2E（约 2 周）

内容：

1. 全链路：`.cj 源 → program.wasm`（编译 + 运行时/stdlib 链接 + 打包脚本）。
2. 前端诊断：wasm-wasi 目标下禁用线程相关 API。
3. E2E 测试夹具：Wasmtime + Node.js 双宿主 runner，纳入 CI（`tests/wasm-e2e`）。

验收（DoD）：

- E2E 套件（字符串/堆/异常/文件读取/定时器）双宿主全绿并进 CI；
- 示例程序文档（编译命令 + 运行命令）随 `docs/` 发布。

### P5 后续（立项另议，不在本期承诺）

- `wasm32-unknown-unknown` 目标（独立模块、纯 import/export）；
- 调试信息（wasm DWARF）与源码映射；
- LTO / 多模块链接；
- 多线程（wasi-threads）与 component model；
- `wasm64`（待 LLVM 稳定）。

## 7. 风险与缓解

| 风险 | 级别 | 缓解 |
|---|---|---|
| LLVM 重打与版本对齐（现状 15 vs 需求 18，且缺 wasm 后端） | 中 | P0 最先做并全量回归 `llvm-interop-jni`；保留旧构建可回滚 |
| lowering 补全是最大成本 | 高 | 与 native 目标共享投入；wasm v1 约束（无 EH/单内存/单线程）反而缩小 v1 语义面 |
| 运行时异常机制不兼容（§5.4） | 中 | P3 首任务审计；选项 b（返回码/用户态）为兜底，不依赖 wasm EH 提案 |
| 4 GiB 地址空间上限 | 低~中 | P2/P3 审计大对象分配假设；运行时堆增长策略适配 `memory.grow` 失败路径 |
| 打包 `wasm-ld.exe` 损坏 | 低 | 链接一律走 wasi-sdk 工具链 |
| 与官方 cjc 的 ABI 分歧 | 低 | 官方无 wasm（§2 #8），契约由本项目定义并成文（P1），后续可上游 |
| 深度递归受引擎栈限制 | 低 | §5.3 文档化已知限制；lowering 不假设无界栈 |

## 8. 不在本期范围

- `wasm-js` 目标、JS 运行时、JS 胶水层、浏览器调试（Kotlin `wasm.debug.browsers` 对应物）；
- 纯 JS 编译目标（CHIR→JS）；
- 多线程（wasi-threads / Web Worker）、component model、`wasm64`；
- 运行时的 Inspector / Profiler / Sanitizer wasm 移植。

## 9. 参考资料

- 本仓：
  - `compiler/codegen/docs/chir-codegen-state-inventory.md`（lowering 现状与 gap 清单）
  - `llvm-interop/README.md`（JNI 互操作层与构建方式）
  - `external/cangjie_runtime/`（运行时组件结构，§5.6 依据）
- Kotlin 参考镜像（只读，仅作结构对标）：
  - `external/kotlin/wasm/wasm.config/.../WasmTarget.kt`（wasm-js / wasm-wasi 目标定义）
  - `external/kotlin/wasm/`（wasm.config / wasm.frontend / wasm.ir / wasm.tests 模块划分）
- 外部：
  - LLVM WebAssembly 后端：triple `wasm32-unknown-unknown`（无 OS 独立模块）、
    `wasm32-wasi`（WASI 0.1，wasi-libc 为 musl 系；新版 LLVM 更名为
    `wasm32-wasip1` 并保留旧名兼容）
  - wasi-sdk（clang/LLVM + wasi-libc + wasm-ld + sysroot）
  - WASI 宿主运行时：Wasmtime、WasmEdge、Wazero、Node.js
