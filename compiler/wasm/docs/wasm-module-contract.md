# Cangjie Wasm module contract

本后端从 `ChirPackage` 开始，独立 lowering 到 Wasm IR，再自行验证并发射 WebAssembly Core v1 module。它不依赖 LLVM，也不改动 CFIR、解析器或前端 pipeline。

## Targets

| API alias | Host ABI | Entry | Default exports |
| --- | --- | --- | --- |
| `wasm-wasi` | WASI Preview 1 | `_start` export，由 WASI runner 调用；不使用 Core start section | `_start`、`memory` |
| `wasm32-unknown-unknown` | Wasm Core embedding host | 无自动入口 | public CHIR functions、package init exports（若存在）、`memory` |

`wasm-js` 未实现，`WasmTarget.fromAlias("wasm-js")` 返回 `null`。WASI entry 默认取唯一的 `main()`，允许返回 `Unit` 或 `Int32`/`UInt32`。返回整数时 wrapper 调用 `wasi_snapshot_preview1.proc_exit(i32)`。wrapper 先调用 package literal initializer，再调用 package initializer，然后调用 `main`。

两个目标都使用 wasm32/memory32 和 i32 指针。module 定义一块 linear memory；默认初始为 1 页，宿主可通过 `WasmCodegenOptions` 指定 limits。backend 导出名为 `memory` 的 memory。当前不实现共享 memory、table import、Wasm GC、threads、SIMD、component model 或 DWARF。

## Imports and runtime ABI

Wasm imports 从 `ChirPackage.members.importedFunctions`、imported variables 与 CHIR runtime operations 建立。外部 CHIR declaration/value 可以用 `wasm.import.module` 和 `wasm.import.name` 属性覆盖默认 import module/field；未指定时 module 为 `cangjie_runtime`，field 使用 CHIR 符号名。相同 module/field 必须具有同一 Wasm signature。

当前 backend 生成以下显式 runtime imports：

| Import field | Wasm signature | 用途 |
| --- | --- | --- |
| `cangjie.alloc` | `(i32) -> i32` | tuple pack 的持久线性内存分配 |
| `cangjie.stack.mark` | `() -> i32` | 建立函数栈 frame token；仅含 `alloca` 的函数需要 |
| `cangjie.stack.alloc` | `(i32) -> i32` | 在当前函数 frame 中分配 16 字节对齐的 `alloca` 存储 |
| `cangjie.stack.restore` | `(i32) -> ()` | 函数返回前恢复 frame token |
| `cangjie.throw` | `(i32) -> ()` | 无本地 unwind handler 时抛出异常；若返回则紧接 `unreachable` |
| `cangjie.exception.matches` | `(i32, i32) -> i32` | catch type 分派；第二个参数是 UTF-8 type name 的 32-bit FNV-1a id |
| `cangjie.float.rem.f32` | `(f32, f32) -> f32` | Core Wasm 无浮点 remainder 指令时由 runtime 实现 |
| `cangjie.float.rem.f64` | `(f64, f64) -> f64` | 同上 |
| `cangjie.float16.binary.<op>` | `(i32, i32) -> i32` | Float16 operands/results 使用 i32 low-16 binary16 bits；comparison 的 i32 结果为 Bool |
| `cangjie.float16.unary.<op>` | `(i32) -> i32` | Float16 unary operation |
| `cangjie.float16.cast.<op>.<src>_<dst>` | 按 CHIR source/result Wasm types 推导 | Float16 conversion/reinterpretation |
| `cangjie.generic.binary.<op>.<type-id>` | `(i32, i32, i32) -> i32` | boxed generic values 与 type id 的 runtime operation |
| `cangjie.generic.unary.<op>.<type-id>` | `(i32, i32) -> i32` | boxed generic unary operation |
| `cangjie.generic.cast.<op>.<src-id>_<dst-id>` | source value、source type id、target type id → mapped result | generic boxing/conversion |
| `cangjie.string.build.<signature>` | 从 CHIR operands/result 推导 | 构造 string builder |
| `cangjie.string.append.<signature>` | 从 CHIR operands/result 推导 | 追加 string builder 内容 |
| `cangjie.string.result.<signature>` | 从 CHIR operands/result 推导 | 完成 string 构造 |

`generic` helpers 的 type id 是 type name 或 `ChirGenericType.identifier` 的 UTF-8 FNV-1a 32-bit hash；cast import field 携带 source/target type name hash。通用值在 Wasm function/table/locals 中以 i32 handle 传递。Float16 import 收发 i32，其低 16 位按 IEEE binary16 编码。

本变更只生成这些 import declarations，不包含 Cangjie runtime 实现。宿主或后续编译的 runtime module 必须提供实际 imports。`stack.alloc` runtime 负责与 heap allocator 协调 linear memory growth；backend 用 mark/restore 保持栈分配的函数生命周期。分配失败时 runtime 必须 trap，不能返回伪造地址。WASI 系统调用 imports 使用 `wasi_snapshot_preview1` namespace；本 backend 自身只在 `Int32` main 的退出路径生成 `proc_exit`。

## Values, memory, and control flow

- Bool 与 8/16/32-bit integers、rune、wasm32 pointers 映射为 i32；64-bit integers 映射为 i64；Float32/Float64 映射为 f32/f64。
- i8/u8/i16/u16 arithmetic 通过 Core narrow loads/stores 与显式 normalize 指令保持位宽和 signedness。Float16 以 i32 low-16 IEEE binary16 编码，并由 runtime imports 执行运算；generic values 以 runtime-managed i32 handles 表示，并通过 FNV-1a type id 分派操作。
- Core Wasm 无法表示且没有 runtime ABI 的语义以带 CHIR semantic id 的 `WasmCodegenException` 失败。
- Tuple values 使用 i32 指向线性内存；元素按 wasm32 natural alignment 排列。Tuple pack 通过 `cangjie.alloc` 分配，tuple extract 读取对应字段。
- C string literal 以 UTF-8 加 NUL 结尾存储；language string literal 使用 little-endian i32 byte length 后跟 UTF-8 bytes。静态地址从 16 开始，供 null 与低地址哨兵保留。
- `alloca` 先把 count 与元素布局转成经过溢出检查、16 字节对齐的 byte size，再调用 `cangjie.stack.alloc`；返回前调用 `cangjie.stack.restore`。栈/堆边界及 `memory.grow` 由 runtime allocator 统一管理。
- CHIR block id 映射为 PC local。嵌套 `block` labels 与 `br_table` 组成 dispatcher loop；所有 CHIR SSA results 均驻留于 locals，phi inputs 在对应 CFG edge 上复制。
- Direct calls 编为 `call`；first-class function values 编为函数 table index，动态调用编为有类型的 `call_indirect`。

## Exports

显式 `WasmCodegenOptions.exportedFunctionNames` 覆盖默认导出选择。未提供时，`wasm32-unknown-unknown` 根据 `visibility`/`access` string 属性、`public`/`wasm.export` boolean 属性与 package access level 选择 public 函数。单个 external export name 必须唯一。`wasm.export.name` 可以给 public 函数指定 host export 名称。

`wasm32-unknown-unknown` 不自动调用任何函数。若 CHIR package 定义 package 初始化函数，backend 导出 `__cangjie_package_literal_init` 与 `__cangjie_package_init`；embedding host 应先按此顺序调用它们，再调用程序导出。

`WasmArtifactWriter` 以同目录临时文件写入并原子替换目标 `.wasm` 文件。Wasm bytes 在写入前经 Wasm IR verifier 检查；writer 不启动外部 linker 或 LLVM 工具。
