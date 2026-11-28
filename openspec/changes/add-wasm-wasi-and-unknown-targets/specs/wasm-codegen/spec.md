## 新增需求

### 需求:CHIR 直接进入独立 Wasm 后端

系统必须提供独立于 LLVM 的 CHIR→Wasm codegen API。该 API 必须消费 `ChirPackage`，生成 Wasm IR，验证 IR 并由本项目的 Wasm binary writer 产生标准 Wasm Core module bytes。

#### 场景:生成 Wasm module
- **当** 调用方为有效 CHIR package 选择已支持的 Wasm target
- **那么** 后端返回该目标的 Wasm bytes 和模块元数据，且执行路径不初始化 LLVM JNI 或启动 LLVM 工具

#### 场景:拒绝不支持的 CHIR 语义
- **当** CHIR 包含当前 Wasm backend 未实现的 operation 或 type
- **那么** 后端以包含 operation/type 与 CHIR semantic id 的结构化失败结束，不生成占位指令或默认值

### 需求:支持两个非 JS WebAssembly targets

系统必须注册 `wasm-wasi` 与 `wasm32-unknown-unknown` 两个 target profile，并为其提供稳定 alias、wasm32 类型数据模型和明确的宿主入口/import/export 约定。WASI 的 `_start` 必须是宿主调用的 export，不能误写成 Core 实例化 start section。系统不得将 `wasm-js` 解析为这两个目标之一。

#### 场景:解析 WASI target
- **当** 调用方选择 `wasm-wasi`
- **那么** 后端应用 WASI Preview 1 的 module profile，并按 `_start` 入口约定构造并导出启动函数

#### 场景:解析无操作系统 core target
- **当** 调用方选择 `wasm32-unknown-unknown`
- **那么** 后端不生成自动 start function，保留调用方 imports/exports，并在 package 初始化函数存在时将其作为显式 exports 暴露

#### 场景:拒绝未实现的 JS target
- **当** 调用方传入 `wasm-js`
- **那么** target parser 明确返回不支持该目标的错误，不将其映射到 WASI 或 wasm32-unknown-unknown

### 需求:Wasm IR 具备验证与确定性编码

系统必须通过具名 Wasm IR 表达类型、imports、functions、locals、globals、function table/elements、memory、exports、start、code 与 data，并在编码前解析引用索引和执行结构验证。Binary writer 必须按 WebAssembly Core 格式输出 section、LEB128 immediates 与 UTF-8 names，并对相同输入产生相同 bytes。

#### 场景:验证并发射合法 IR
- **当** module 引用、函数签名、locals、控制流和 memory 配置一致
- **那么** binary writer 输出包含正确 magic/version 和 section 长度的 Wasm Core module

#### 场景:报告无效 IR 引用
- **当** module 存在越界函数/local/global 索引、重复 export 或无效 start signature
- **那么** verifier 在写出 bytes 之前返回定位到模块字段或 CHIR semantic id 的诊断

### 需求:保持后端依赖隔离

Wasm backend modules 必须依赖 CHIR public model 和 Wasm backend common/IR。它们不得依赖 CFIR、PSI、解析器、`compiler:frontend`、`compiler:codegen`、`llvm-interop` 或 `external/` 实现源码。

#### 场景:组装 Wasm backend
- **当** Gradle 解析新增 Wasm backend modules
- **那么** 模块依赖图保持从 CHIR 到 Wasm backend 单向，且不引入前端或 LLVM 依赖

#### 场景:现有后端行为不变
- **当** 调用现有 JVM 或 LLVM codegen API
- **那么** 新增 Wasm backend 不改变它们的默认配置、产物格式或调用路径

### 需求:明确运行时 imports

系统必须将 CHIR 声明的外部函数与 runtime symbols 编码为 Wasm imports，并记录 module/name/signature 映射。Wasm backend 不得静默移除未解析的 runtime call，也不得声称本变更提供了 Cangjie runtime 实现。

#### 场景:运行时函数作为 import
- **当** 被生成的函数调用 CHIR runtime symbol
- **那么** module 的 import section 包含该符号的精确 Wasm function type 和约定的 import namespace/name

#### 场景:runtime ABI 不可映射
- **当** runtime symbol 的类型无法映射到 Wasm Core function type
- **那么** codegen 以结构化错误指出符号与 CHIR 类型，不生成错误签名的 import

### 需求:运行时管理动态栈与 heap 的内存协调

系统必须将 CHIR `alloca` lowering 到显式 stack mark/alloc/restore imports，使 runtime 能与 heap 分配器共同管理 linear memory。backend 必须对 count、布局大小和 wasm32 地址运算做溢出检查；运行时分配失败必须 trap，不得返回伪造地址。

#### 场景:函数使用 alloca
- **当** CHIR 函数包含动态或固定 `alloca`
- **那么** 函数入口建立 stack mark，每次分配调用 runtime stack allocator，并在所有正常返回路径恢复该 mark

#### 场景:动态栈大小溢出
- **当** alloca count 与元素布局乘法超过 wasm32 地址范围
- **那么** Wasm function 在调用 runtime allocator 之前 trap
