## 新增需求

### 需求:统一的 LLVM 后端操作接口
系统必须提供 `LlvmBackend` 接口（替代现有 `LlvmBackendApi`），支持以下操作：
- `createContext()`: 创建 LLVM 上下文
- `createModule(name, context)`: 创建 LLVM 模块
- `createBuilder(context)`: 创建 IR 构建器
- `verifyModule(module)`: 验证模块
- `writeBitcode(module, outputPath)`: 输出 bitcode 文件
- `writeIR(module)`: 输出 IR 文本
- `runPasses(module, passes)`: 运行优化 Pass
- `emitToFile(module, targetMachine, path, fileType)`: 输出目标文件

该接口必须被 JNI 实现所满足。IN_MEMORY 模式作为开发/测试降级，只需支持 `writeIR`。

#### 场景:通过 JNI 实现创建模块
- **当** 使用 `JniLlvmBackend` 实现调用 `createContext()` 和 `createModule("test", ctx)`
- **那么** 在 JVM 进程内创建 LLVM 上下文和模块，返回有效句柄

#### 场景:IN_MEMORY 模式输出 IR 文本
- **当** 使用 `InMemoryLlvmBackend` 实现调用 `writeIR(module)`
- **那么** 返回手动拼接的 LLVM IR 文本字符串

### 需求:后端能力探测
系统必须提供 `LlvmBackendCapabilities` 数据类，报告后端支持的能力：
- `supportsInProcessIR`: 是否支持进程内 IR 构建
- `supportsOptimization`: 是否支持运行优化 Pass
- `supportsTargetCodegen`: 是否支持目标代码生成
- `llvmVersion`: LLVM 版本字符串（JNI 可用时从 LLVM 查询，否则为空）

JNI 后端报告全部能力；IN_MEMORY 后端报告无能力（仅文本输出）。

#### 场景:JNI 后端全能力
- **当** 查询 `JniLlvmBackend` 的 capabilities
- **那么** `supportsInProcessIR`、`supportsOptimization`、`supportsTargetCodegen` 全部为 `true`

#### 场景:IN_MEMORY 后端无能力
- **当** 查询 `InMemoryLlvmBackend` 的 capabilities
- **那么** `supportsInProcessIR`、`supportsOptimization`、`supportsTargetCodegen` 全部为 `false`

### 需求:自动降级策略
系统必须实现后端自动降级机制：当 JNI 原生库不可用时（加载失败），自动降级到 `InMemoryLlvmBackend`（纯文本模式），并记录警告日志。降级必须在 `LlvmBackendFactory` 中透明完成，调用方无需关心具体使用的后端实现。

#### 场景:JNI 不可用时自动降级
- **当** JNI 原生库加载失败（如平台不支持或库文件缺失），且 `failOnUnavailable = false`
- **那么** 自动创建 `InMemoryLlvmBackend` 实例，并输出警告日志说明降级原因

#### 场景:严格模式下不降级
- **当** JNI 原生库加载失败，且 `failOnUnavailable = true`
- **那么** 抛出 `LlvmBackendUnavailableException`，不进行降级

### 需求:后端工厂配置
`LlvmBackendFactory` 必须支持通过 `CodegenOptions` 配置后端选择策略：
- `llvmBackendKind`: 首选后端类型（`JNI`（进程内绑定）、`IN_MEMORY`（纯文本降级））
- `failOnUnavailable`: 首选后端不可用时是否失败
- `requiredLlvmMajorVersion`: 所需 LLVM 主版本号

工厂必须在创建 JNI 后端时验证 LLVM 版本匹配。

#### 场景:指定 JNI 后端
- **当** `CodegenOptions.llvmBackendKind = JNI` 且 JNI 原生库可用
- **那么** 工厂创建并返回 `JniLlvmBackend` 实例

#### 场景:版本不匹配
- **当** JNI 后端报告的 LLVM 版本主版本号与 `requiredLlvmMajorVersion` 不匹配
- **那么** 抛出 `LlvmBackendVersionMismatchException`，包含期望版本和实际版本

### 需求:删除进程外后端并迁移现有代码
系统必须删除以下进程外后端代码：
- `NativeInteropLlvmBackendApi` 及其依赖的 `NativeInteropToolRunner`、`NativeInteropToolLocator`
- `LlvmBackendKind.NATIVE_INTEROP` 枚举值
- `tools/cangjie-llvm-interop/` 目录及其 CI 工作流

删除后，`LlvmBackendKind` 必须只包含 `JNI` 和 `IN_MEMORY` 两个值。

#### 场景:进程外代码完全移除
- **当** 完成迁移后搜索代码库中的 `cangjie-llvm-interop`、`NativeInteropTool`、`NATIVE_INTEROP` 关键词
- **那么** 搜索结果为空（除变更日志和提交记录外）

#### 场景:现有 IN_MEMORY 测试不受影响
- **当** 使用 `IN_MEMORY` 后端模式运行现有的 8 个对等基线测试
- **那么** 所有测试结果与变更前完全一致

### 需求:与现有 codegen 管线集成
新的 `LlvmBackend` 接口必须能被 `DefaultChirToLlvmCodeGenerator` 使用。集成后默认使用 JNI 后端（可用时），降级到 IN_MEMORY（不可用时）。

#### 场景:JNI 后端生成 IR
- **当** 使用 `JNI` 后端模式生成 `simple-return` 基线样本的 LLVM IR
- **那么** 输出的 IR 文本与现有文本拼接方式生成的 IR 结构等价
