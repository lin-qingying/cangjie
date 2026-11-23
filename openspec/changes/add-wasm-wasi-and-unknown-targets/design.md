## 上下文

Kotlin 的 Wasm 后端有独立的 `wasm.config`、`wasm.ir` 与 backend.wasm：先形成 Wasm 专用 IR，再由自有编码器输出 WebAssembly 二进制。本项目已有独立 CHIR 模型，当前 `compiler:codegen` 是 CHIR→LLVM，不能作为本后端的实现依赖。用户明确要求按 Kotlin 的独立发射路线实现，并限定不得修改 CFIR、解析与前端模型。

仓库 Kotlin 镜像的 `WasmTarget` 只包含 `JS("wasm-js")` 与 `WASI("wasm-wasi")`。第二个非 JS 目标按仓库 Wasm 计划中的裸模块预留目标确定为 `wasm32-unknown-unknown`。

## 目标 / 非目标

**目标：**

- 提供仅依赖 CHIR 的 Wasm codegen 入口，并生成确定、可验证的 Wasm Core 二进制。
- 在共享目标层区分 WASI Preview 1 与无操作系统模块的入口、导入和导出约定。
- 以 Wasm IR、验证器、索引解析与二进制编码作为独立后端层，不初始化或链接 LLVM。
- 把新增代码限制在 `compiler/wasm`、后端必要配置、`settings.gradle.kts` 和 OpenSpec/后端文档。

**非目标：**

- `wasm-js`、JavaScript glue、浏览器运行时、调试器。
- 修改 PSI、CFIR、CFIR→CHIR、前端 pipeline 或编译器前端模型。
- 移植 C++ runtime/stdlib、提供 WASI SDK 或在宿主侧链接 runtime。
- Wasm GC、component model、threads、SIMD、source maps 与 DWARF。

## 决策

### 模块边界

- `:compiler:wasm:wasm-common`：`WasmTarget`、目标 profile、ABI 和结构化后端诊断；无 CHIR/LLVM 依赖。
- `:compiler:wasm:wasm-ir`：MVP/Core type、module/function/import/export/section/instruction 模型、引用索引解析、IR 验证与二进制编码；只依赖 `wasm-common`。
- `:compiler:wasm:wasm-codegen`：将 `ChirPackage` 直接转换为 Wasm IR，再验证并发射 `.wasm`；只依赖 `chir:chir-tree` 与两个 Wasm 模块。

这对应 Kotlin 后端的配置、IR、codegen 分层，但 CHIR lowering 按仓颉的 CHIR 结构编写，不移植 Kotlin IR/JS 语义。后端模块不依赖 `compiler:codegen`、`llvm-interop` 或任何前端模块。

### 目标契约

- `WASI` 的稳定外部 alias 为 `wasm-wasi`；profile 表达 WASI Preview 1 的 `_start` entry export 约定及 `wasi_snapshot_preview1` import namespace。WASI 宿主调用 `_start`，不把它写成 Core 实例化 start section。
- `UNKNOWN_UNKNOWN` 的稳定外部 alias 为 `wasm32-unknown-unknown`；没有自动 start function，宿主通过显式导入/导出与导出的 linear memory 交互。
- 两者共享 wasm32 Core 指令和 ILP32 数据模型。目标差异只进入 profile 和 module assembly，不散落在 CHIR 指令 lowering 中。
- Cangjie runtime 符号保持显式 Wasm imports；本变更不伪造宿主实现或静默删除 runtime calls。`alloca` 的 mark/alloc/restore 由 runtime SPI 与 heap/memory.grow 协同，backend 不擅自划分冲突的线性内存区域。

### Wasm IR 与二进制

- 使用符号引用构造 module，完成类型、函数、全局、memory 和导出索引分配后再编码，避免 lowering 阶段手工维护 section index。
- Binary writer 按 WebAssembly Core section 顺序输出 type/import/function/table/memory/global/export/start/element/code/data/custom-name sections，并负责 LEB128、UTF-8 names、vector 与 section size 编码。
- Wasm function values 使用 table slot 表示，`call_indirect` 带显式 function type；element section 初始化 module 内的 imported/defined function slots。
- Verifier 在编码前检查函数签名与 body 对齐、符号索引范围、控制流栈平衡、local/global 类型、memory/table limits、import/export 唯一性和 start 约束；非法 IR 产生带 CHIR semantic id 的后端诊断。
- 编码器使用确定性排序。输出携带完整 Wasm bytes 与目标 profile；文件写出由 Wasm artifact writer 原子完成。

### CHIR lowering

- CHIR 值采用 Wasm locals 表示，基本块与 terminator 映射到 structured `block`/`loop`/`br`/`br_if` 控制流；PHI/跨块值由显式 local copy 实现。
- Primitive integer/float/bool 与 32-bit reference/pointer 映射到 Wasm Core value types；函数和数据布局由一个 target-aware type mapper 管理。
- 所有 CHIR operation families 必须有显式 lowering 或携带 semantic id 的 unsupported-operation 失败；禁止输出默认常量、空函数体或 LLVM fallback。
- 复用 CHIR validator 与 semantic ids，但不更改其模型或校验逻辑。

### 备选方案

- 继续用 LLVM target machine：被用户明确排除，且不能实现 Kotlin 所采用的独立发射器。
- 在 `compiler:codegen` 中加入 Wasm 输出分支：会使 LLVM 专属模块承载第二套 codegen 实现，边界不清；独立后端模块更贴合 Kotlin 结构。
- 直接由 CHIR lowering 手写 bytes：缺乏稳定 IR、索引解析与验证边界，选择独立 Wasm IR。

## 风险 / 权衡

- CHIR 中有超出 Core Wasm MVP 的语义 → 逐项盘点 operation/type，并对未实现语义结构化失败；不做失真降级。
- 运行时导入使某些 `.wasm` 需要外部宿主/runtime 才可实例化 → 明确 module imports/exports 契约；不声称已移植仓颉 runtime。
- 非结构化 CHIR CFG 难以映射到 Wasm structured control flow → 在 lowering 中建立基本块深度与分支目标计划，并在 Wasm IR verifier 检查栈与目标一致。
- 当前目标数为两个非 JS profile → 目标分歧只保留在 profile/SPI，新增宿主不复制指令集或 binary writer。

## 迁移计划

新增后端为独立模块，不改变现有 JVM、LLVM codegen 默认行为。使用者通过 `ChirToWasmCodeGenerator` 显式选择 `WasmTarget` 并取得模块 bytes；后续需要 CLI 接线时另在后端驱动层立项，不扩展前端 pipeline。

## Open Questions

- 仓颉 runtime 的 Wasm import module/name/signature 契约目前未在本仓定义；本次从现有 CHIR import/runtime declarations 生成明确 imports，并在契约文档中固定映射规则。
- `wasm32-unknown-unknown` 没有既有官方仓颉 host ABI；本次定义可由宿主显式提供 imports 的 Wasm Core module ABI，不承诺运行时服务。
