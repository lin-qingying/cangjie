## 为什么

项目已有以 CHIR 为输入的 LLVM codegen，但还没有 WebAssembly 目标配置或产物路径。Kotlin 源码定义的两个 Wasm target 是 `wasm-js` 与 `wasm-wasi`；结合仓库现有 Wasm 计划中预留的裸模块目标，本变更覆盖 `wasm-wasi` 与 `wasm32-unknown-unknown`，并保持从 CHIR 开始的后端边界。

## 变更内容

- 新增独立于 LLVM 的 WebAssembly 后端分层：目标配置、Wasm IR、CHIR→Wasm lowering 与 Wasm 二进制编码。
- 生成符合 WebAssembly Core 格式的 `.wasm` 模块，并按 WASI Preview 1 (`wasm-wasi`) 与无操作系统核心目标 (`wasm32-unknown-unknown`) 设置入口、导入、导出和宿主契约。
- 让 Wasm IR 验证、索引分配和二进制编码成为独立后端责任；不依赖 LLVM JNI、LLVM 工具链或外部 wasm linker。
- 保持 Wasm API 直接消费 CHIR；不修改解析器、CFIR、CFIR 到 CHIR 转换、前端模型或前端 pipeline，也不实现 `wasm-js`、JS glue 或浏览器运行时。

## 功能 (Capabilities)

### 新增功能

- `wasm-codegen`: 从 CHIR 为 WASI Preview 1 与 wasm32 unknown-unknown 生成 WebAssembly 目标产物。

### 修改功能

无。

## 影响

- 新增 `compiler/wasm` 后端模块族：目标/宿主配置、Wasm IR 与二进制编码、CHIR 到 Wasm 的 codegen 入口。
- `settings.gradle.kts`：注册后端模块；依赖方向从 Wasm 后端到 `chir:chir-tree`，不依赖 CFIR 或前端。
- 不修改 `psi`、`cfir`、`chir:cfir2chir`、`compiler/frontend` 或 `external/` 中的参考源码。
