## 为什么

当前仓颉编译器的 LLVM 后端采用**文本拼接 + 外部进程**模式：`CGModule`/`CGFunction` 手动拼接 LLVM IR 文本字符串，再通过外部 `cangjie-llvm-interop` C++ 工具转换为 bitcode。这种方式存在根本性瓶颈：

1. **无法进行优化 Pass**：文本 IR 无法在 JVM 进程内运行 LLVM 优化管线（如 mem2reg、内联、死代码消除），生成质量远低于生产级编译器。
2. **无法进行目标代码生成**：无法调用 `LLVMTargetMachine` 直接生成 `.o`/`.obj` 文件，必须依赖外部 `llc`。
3. **无法使用 LLVM 类型系统和验证**：结构体布局、对齐计算、ABI 查询全靠手工模拟，容易出错。
4. **跨进程通信开销大**：每次 `emitBitcode` 启动子进程，序列化/反序列化 IR 文本，大型模块会成为编译性能瓶颈。
5. **`cangjie-llvm-interop` 工具本身能力有限**：只支持 `probe` 和 `emit-bitcode` 两个命令，无法支撑后续优化和目标代码生成需求，维护独立 C++ 工具的成本不值得继续投入。

参考 Kotlin/Native 编译器的方案：通过 cinterop 自动生成 LLVM C API 的 Kotlin/Native 绑定，后端直接调用 `LLVMContextCreate()`、`LLVMAddFunction()`、`LLVMBuildRet()` 等 API。但本项目运行在 **Kotlin/JVM** 平台，无法使用 cinterop，需要设计适合 JVM 的 LLVM 互操作框架。

现在实现这一基础设施，是因为 `compiler/codegen` 模块已具备基本的 CHIR→LLVM IR lowering 能力（26 个源文件、8 个对等基线样本），后端架构已稳定，是引入进程内 LLVM 绑定、替换文本拼接方式的最佳时机。

## 变更内容

- 设计并实现 JVM 平台的 LLVM C API 绑定框架，提供类型安全的 Kotlin API 层。
- 实现 JNI 原生桥接层（C/C++ → JNI → Kotlin），覆盖 LLVM Core、BitWriter、Analysis、PassBuilder、TargetMachine 核心模块。
- **删除 `tools/cangjie-llvm-interop/` 及其 CI 工作流**——JNI 绑定完全取代其功能，不再需要独立的 C++ 进程工具。
- **删除 `compiler/codegen` 中的 `NativeInteropLlvmBackendApi` 和相关进程调用代码**（`NativeInteropToolRunner`、`NativeInteropToolLocator`）。
- 重构 `compiler/codegen` 的后端抽象（`LlvmBackendApi`），使其支持进程内 LLVM 操作（构建 IR、运行 Pass、生成目标代码）。
- 参考 Kotlin/Native 编译器的 Gradle-native 构建方式，建立多平台原生库构建与分发机制。

## 功能 (Capabilities)

### 新增功能
- `llvm-jvm-binding`: LLVM C API 的 JVM 绑定核心——JNI 原生库和 Kotlin 类型安全 API 封装，覆盖 Context/Module/Type/Value/Builder/PassBuilder/TargetMachine 等核心对象。
- `llvm-backend-abstraction`: 编译器后端的 LLVM 操作抽象层——统一 JNI 进程内绑定和 IN_MEMORY 纯文本两种模式，JNI 不可用时降级到文本模式。
- `llvm-native-build`: 原生库跨平台构建与分发——参考 Kotlin/Native 的 Gradle-native 构建方式，通过自定义 Gradle 插件驱动 clang 编译 JNI 原生库。

### 修改功能

无。

## 影响

- **代码模块**：
  - 新增 `llvm-interop/` 顶层模块（含 `api` 和 `jni` 子模块）。
  - 重构 `compiler/codegen/backend/` 中的 `LlvmBackendApi` 接口和实现。
  - **删除** `tools/cangjie-llvm-interop/` 目录及其 CI 工作流。
  - **删除** `compiler/codegen` 中的 `NativeInteropLlvmBackendApi`、`NativeInteropToolRunner`、`NativeInteropToolLocator` 及相关测试。
- **外部依赖**：
  - 构建时依赖：LLVM 开发库（头文件 + 静态/动态库），版本 ≥ 18。
  - 运行时依赖：LLVM 共享库或静态链接的 JNI 原生库。
- **构建系统**：
  - 参考 Kotlin/Native 的 `native-interop-plugin`，实现 Gradle-native 的 C/C++ 编译任务。
  - CI 需在多平台上编译和测试 JNI 原生库。
- **风险**：
  - 跨平台 ABI 差异（LLVM 库的链接方式、符号可见性）。
  - JNI 内存管理（LLVM 对象生命周期与 JVM GC 的协调）。
  - LLVM 版本升级时 API 变更的维护成本。
