# 独立 Wasm 后端实施计划

- 状态：实现中
- OpenSpec：`openspec/changes/add-wasm-wasi-and-unknown-targets/`
- 源码入口：`ChirToWasmCodeGenerator`，输入为 `ChirPackage`

## 目标识别

仓库 Kotlin 镜像的 `external/kotlin/wasm/wasm.config/.../WasmTarget.kt` 只定义 `wasm-js` 与 `wasm-wasi`。本项目实现 `wasm-wasi`，以及仓库早期 Wasm 计划明确预留的裸 wasm32 模块目标 `wasm32-unknown-unknown`。`wasm-js` 不实现；Kotlin 上游没有第三个 WasmTarget。

## 架构

```text
ChirPackage
   │
   ▼
:compiler:wasm:wasm-codegen   CHIR declarations / values / CFG → Wasm IR
   │
   ▼
:compiler:wasm:wasm-ir       symbolic IR → verify → section/index resolution → binary
   │
   ▼
standard WebAssembly Core v1 module (.wasm)
```

- `wasm-common`：target aliases、host ABI profiles。
- `wasm-ir`：Core value/function types、instructions、module fields、validator、binary writer、artifact metadata。
- `wasm-codegen`：CHIR type/value lowering、function/table/global imports、CFG/phi lowering、host entry/export assembly。
- 依賴方向只從 backend 到 `chir:chir-tree`；沒有 CFIR、PSI、frontend、LLVM 或外部参考源码依赖。
- 现有 `compiler:codegen` 与 JVM backend 维持原调用路径。

## 两个 host profile

| Target | Entry contract | Imports | Exports |
| --- | --- | --- | --- |
| `wasm-wasi` | 默认包装唯一 `main()` 为 `_start` export；WASI runner 调用该 export，不用 Core start section | 显式 CHIR imports 默认来自 `cangjie_runtime`；整数 main 通过 `wasi_snapshot_preview1.proc_exit` 退出 | `_start`、`memory` |
| `wasm32-unknown-unknown` | 无自动入口 | 所有外部函数由 embedding host 提供 | 默认按 visibility 导出 public CHIR functions 和 `memory`，也支持显式 export 集合 |

两个目标共享一套 wasm32 指令、linear-memory 布局、函数 table 与 binary writer。详细 import signatures、字符串布局、内存增长和异常 ABI 见 [wasm-module-contract.md](wasm-module-contract.md)。

## 当前实现阶段

1. 模块与 profile：已实现。
2. Wasm Core IR、索引/类型校验、section/LEB128/name 编码：已实现。
3. CHIR direct lowering：已覆盖数值/Float16/generic ops、cast、select/phi、direct/indirect call、memory、tuple/string runtime imports、异常 edges 与 package entry。
4. ABI 文档、OpenSpec 验证和三个模块 Gradle assemble：已完成。未新增或运行测试。

## 明确限制

- 本变更提供 backend 与 runtime import contract，不移植 C++ runtime/stdlib。
- JVM 专属 CHIR intrinsic、未解析类型以及没有直接 Wasm 或 runtime import ABI 的操作会以 CHIR semantic id 明确失败。
- WASI 输出若引用 `cangjie_runtime` imports，需要宿主提供对应实现；Core module 可以保留这些 imports 供 embedding host 绑定。
