## 1. Wasm 后端模块边界

- [x] 1.1 在 `settings.gradle.kts` 注册 wasm-common、wasm-ir、wasm-codegen 后端模块，保证依赖不触达 CFIR/PSI/frontend/LLVM。
- [x] 1.2 实现 target aliases、WASI Preview 1 与 unknown-unknown profile、宿主入口/import/export 配置。
- [x] 1.3 实现 CHIR-only Wasm 后端公开 API、输入/输出模型与结构化错误模型。

## 2. 独立 Wasm IR 与二进制发射

- [x] 2.1 实现 wasm32 Core 值类型、函数签名、具名 module fields、符号引用、函数 table/call_indirect 和标准指令/立即数模型。
- [x] 2.2 实现索引链接与 IR verifier，覆盖类型、局部变量、分支深度、函数调用、memory、imports/exports 与 start 约束。
- [x] 2.3 实现 UTF-8、LEB128、section size 与 Core module sections 的二进制 writer，确保确定性输出。
- [x] 2.4 实现 name custom section 与原子 `.wasm` artifact writer。

## 3. CHIR 到 Wasm lowering

- [x] 3.1 建立 target-aware CHIR type mapper 和函数/全局/import 声明 lowering。
- [x] 3.2 建立 CHIR SSA 值到 Wasm locals 的映射及 basic block 到 structured control flow 的 lowering。
- [x] 3.3 覆盖 CHIR unary/binary/cast/select/phi/direct+indirect call/memory/tuple/string operation families，按 Wasm ABI 生成直接指令或 runtime imports。
- [x] 3.4 覆盖 package entry、global data、WASI `_start` 与 unknown-unknown exports，并在有不支持的语言语义时给出精确失败。

## 4. 集成与边界验证

- [x] 4.1 从 Wasm 后端依赖图移除 LLVM/前端耦合，保留现有 JVM 与 LLVM codegen 调用路径不变。
- [x] 4.2 增加后端/ABI 文档，记录 target aliases、imports、exports、内存与入口契约。
- [x] 4.3 运行新增模块的 Gradle assemble/build 编译验证，并检查工作区 diff 不触及 CFIR、PSI 或 frontend 实现。
