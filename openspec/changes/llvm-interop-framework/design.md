## 上下文

当前仓颉编译器（Kotlin/JVM）的代码生成模块 `compiler/codegen` 已实现基本的 CHIR→LLVM IR lowering 能力：

- **26 个源文件**：涵盖模块降级（CGModule）、函数降级（CGFunction）、表达式分发（ExpressionLoweringDispatcher）、类型降级（TypeLowering）等。
- **文本拼接方式**：通过 `IRBuilder` 手动拼接 LLVM IR 文本字符串（如 `"define i32 @main() {"`）。
- **双后端架构**：`LlvmBackendApi` 接口支持两种实现：
  - `NativeInteropLlvmBackendApi` — 调用外部 `cangjie-llvm-interop` C++ 工具（即将删除）。
  - `InMemoryLlvmBackendApi` — 直接返回 IR 文本字节（降级模式）。
- **8 个对等基线样本**：对标官方 C++ 编译器的 LLVM IR 输出。

参考 Kotlin/Native 编译器的 LLVM 互操作：
- 通过 `cinterop` + `.def` 文件自动从 `llvm-c/Core.h` 等头文件生成 Kotlin/Native 绑定。
- 后端直接调用 `LLVMContextCreate()`、`LLVMAddFunction()`、`LLVMBuildRet()` 等 API。
- 自定义扩展库 `libllvmext`（Pass 管理）和 `llvmDebugInfoC`（DIBuilder 包装）补充 C API 不足。
- **构建方式**：不使用 CMake 作为主构建系统，而是通过自定义 Gradle `native {}` DSL 直接驱动 clang 编译 C/C++ 代码，CMake 仅用于 IDE 代码导航。

**约束条件**：
- 本项目运行在 **Kotlin/JVM + JDK 17**，无法使用 cinterop。
- 需要跨平台支持（Windows/macOS/Linux × x86_64/aarch64）。
- 必须与现有 `compiler/codegen` 平滑集成，不破坏已有测试。
- **`tools/cangjie-llvm-interop/` 将被删除**，JNI 绑定完全取代其功能。

## 目标 / 非目标

**目标：**
- 提供进程内 LLVM C API 调用能力，支持 IR 构建、模块验证、优化 Pass、目标代码生成。
- 设计类型安全的 Kotlin API 层，将 LLVM 的 C 指针语义映射为 Kotlin 惯用模式。
- 删除 `cangjie-llvm-interop` 及进程外调用机制，简化架构。
- 参考 Kotlin/Native 的 Gradle-native 构建方式，建立跨平台原生库构建管线。

**非目标：**
- 不实现自动绑定生成器（类似 cinterop 的工具）——手动编写 JNI 绑定，只覆盖编译器后端需要的 API 子集。
- 不覆盖 LLVM 全量 API（~2000+ 函数）——仅绑定 Core、BitWriter、Analysis、PassBuilder、TargetMachine 等核心模块。
- 不实现 Clang 绑定——仓颉编译器有自己的前端，不需要 libclang。
- 不替换 `compiler/codegen` 的 lowering 逻辑——本变更只提供 LLVM API 基础设施，lowering 重构属于后续工作。

## 决策

### D1: JNI 绑定 vs JNA vs Panama FFM

| 方案 | 优点 | 缺点 |
|------|------|------|
| **JNI** | 性能最优；无额外依赖；JDK 17 完全支持 | 需编写 C/C++ 胶水代码；编译维护成本 |
| JNA | 无需 native 代码；纯 Java 声明 | 反射开销大（~10x JNI）；LLVM 调用密集场景不可接受 |
| Panama FFM | 最现代的方案；自动绑定 | JDK 22+ 才稳定；JDK 17 为 incubator，API 不稳定 |

**选择 JNI**。理由：
1. 编译器后端对 LLVM API 调用极其密集（单个函数编译可能调用数百次），JNI 的直接函数调用无开销。
2. 项目锁定 JDK 17，Panama FFM 不可用。
3. JNI 是 Kotlin/JVM 生态中调用原生库的标准做法，团队有维护能力。
4. 未来若升级到 JDK 22+，可用 Panama FFM 渐进替换 JNI 层，API 层不变。

### D2: 模块拆分策略

```
llvm-interop/                    ← 新增顶层模块
├── llvm-interop-api/            ← 纯 Kotlin，无原生依赖
│   └── src/main/kotlin/
│       └── org/cangnova/cangjie/llvm/api/
│           ├── LlvmContext.kt
│           ├── LlvmModule.kt
│           ├── LlvmTypes.kt
│           ├── LlvmValues.kt
│           ├── LlvmBuilder.kt
│           ├── LlvmPassManager.kt
│           ├── LlvmTargetMachine.kt
│           └── LlvmNativeHandle.kt
│
└── llvm-interop-jni/            ← JNI 绑定实现
    ├── src/main/kotlin/         ← Kotlin JNI 桩（external fun）+ 实现类
    ├── src/main/native/         ← C/C++ JNI 实现
    │   ├── jni_context.cpp
    │   ├── jni_module.cpp
    │   ├── jni_types.cpp
    │   ├── jni_builder.cpp
    │   ├── jni_pass.cpp
    │   ├── jni_target.cpp
    │   └── CMakeLists.txt       ← 仅用于 IDE 代码导航（CLion）
    └── build.gradle.kts         ← Gradle-native 编译任务
```

**理由**：
- `api` 模块纯 Kotlin，可独立编译和测试，`compiler/codegen` 只依赖此模块。
- `jni` 模块包含原生代码和 JNI 绑定实现。
- 两模块分离使得：CI 可在无 LLVM 环境下编译/测试核心逻辑；集成测试单独依赖 JNI 模块。
- **不再需要 `llvm-interop-process` 模块**——删除 `cangjie-llvm-interop` 后无进程外后备。JNI 不可用时降级到 `InMemoryLlvmBackendApi`（纯文本模式），足以支持开发和测试。

### D3: 句柄管理策略 — JVM value class 包装 native 指针

```kotlin
// 使用 JVM inline value class 包装原生指针，提供类型安全且零开销
@JvmInline
value class LlvmContextRef(val address: Long) {
    companion object { val NULL = LlvmContextRef(0L) }
    val isNull: Boolean get() = address == 0L
}

@JvmInline
value class LlvmModuleRef(val address: Long)
@JvmInline
value class LlvmTypeRef(val address: Long)
@JvmInline
value class LlvmValueRef(val address: Long)
@JvmInline
value class LlvmBuilderRef(val address: Long)
```

**对比 Kotlin/Native 的方案**：Kotlin/Native 用 `CPointer<LLVMOpaqueModule>` 实现类型安全。JVM 上无法使用 CPointer，但 `value class` 提供等价的编译时类型检查，运行时零装箱开销。

**选择此方案**而非 `Long` 裸指针的理由：防止误将 `LlvmTypeRef` 传入期望 `LlvmValueRef` 的位置。

### D4: 资源管理策略 — AutoCloseable + 分层所有权

```kotlin
class LlvmContext : AutoCloseable {
    val ref: LlvmContextRef = LlvmNative.contextCreate()
    private val ownedModules = mutableListOf<LlvmModule>()

    fun createModule(name: String): LlvmModule {
        val module = LlvmModule(LlvmNative.moduleCreateInContext(name, ref), this)
        ownedModules.add(module)
        return module
    }

    override fun close() {
        ownedModules.forEach { it.close() }
        LlvmNative.contextDispose(ref)
    }
}
```

遵循 LLVM 的所有权层级：`Context` 拥有 `Module`、`Builder`；`Module` 拥有 `Function`、`GlobalVariable`。JVM 侧通过 `AutoCloseable` + `use {}` 确保资源释放。

**不使用 `Cleaner`/`PhantomReference`**：LLVM 对象释放有严格顺序依赖，析构器的非确定性调用时机会导致 use-after-free。强制显式关闭更安全。

### D5: API 覆盖范围 — 按编译器后端需求分批绑定

参考 Kotlin/Native 的 `llvm.def` 配置和后端实际调用模式，分三个优先级批次：

| 批次 | LLVM C API 模块 | 函数数量（估） | 用途 |
|------|----------------|--------------|------|
| P0 | Core（Context/Module/Type/Value/BasicBlock） | ~80 | IR 构建基础 |
| P0 | Core（IRBuilder 指令） | ~60 | 指令生成 |
| P0 | Analysis（Verifier） | ~5 | 模块/函数验证 |
| P1 | BitWriter | ~3 | Bitcode 输出 |
| P1 | PassBuilder | ~10 | 优化管线 |
| P1 | TargetMachine | ~15 | 目标代码生成 |
| P2 | DebugInfo（DIBuilder） | ~40 | 调试信息生成 |

P0 批次即可替换当前的文本拼接模式，P1 补齐优化和目标代码能力，P2 支持调试信息。

### D6: 原生库构建方式 — Gradle-native 编译（对齐 Kotlin/Native）

**Kotlin/Native 的做法**（不使用 CMake 作为构建系统）：
- 自定义 `native-interop-plugin` Gradle 插件，提供 `native {}` DSL。
- 通过 `suffixes {}` 定义编译规则（`.cpp` → `.o`），直接调用 `clang`/`clang++`。
- 用 `llvm-ar` 创建静态库，最终链接为共享库。
- CMakeLists.txt 仅生成用于 IDE（CLion）代码导航，不参与构建。
- 平台差异通过 `PlatformInfo`/`HostManager` 等 Kotlin 类处理。

**本项目对齐方案**：
1. 在 `build-tools/` 中实现轻量级 `native-compile-plugin` Gradle 插件：
   - 提供 `nativeCompile {}` DSL，声明源文件、编译选项、链接选项。
   - 自动检测 LLVM 安装路径（`llvm-config --prefix` 或环境变量 `LLVM_DIR`）。
   - 调用系统 `clang++` 编译 `.cpp` → `.o`，调用链接器生成共享库。
2. CMakeLists.txt 保留但仅用于 IDE 支持（CLion 打开项目时的代码补全和导航）。
3. LLVM 不可用时，Gradle 任务优雅跳过（输出警告），Kotlin 代码正常编译。

| 对比 | CMake 方案 | Gradle-native 方案（选择） |
|------|-----------|-------------------------|
| 构建统一性 | Gradle + CMake 两套系统 | 纯 Gradle，与项目其余部分一致 |
| 平台检测 | CMake 内置 | Kotlin 代码，可复用、可测试 |
| LLVM 查找 | `find_package(LLVM)` | `llvm-config` 或环境变量 |
| IDE 支持 | CLion 原生 | CLion 通过辅助 CMakeLists.txt |
| 对齐 Kotlin/Native | 不对齐 | 完全对齐 |

### D7: JNI 原生库加载策略

```
加载优先级：
1. 系统属性 cangjie.llvm.native.library.path → 直接加载指定路径
2. 类路径资源 /native/<os>-<arch>/libcangjie_llvm_jni.{so|dylib|dll}
3. 系统 PATH/LD_LIBRARY_PATH 中查找
4. 加载失败 → 标记 JNI 不可用，降级到 InMemoryLlvmBackendApi
```

不再有进程外降级（`cangjie-llvm-interop` 已删除）。JNI 加载失败时直接降级到纯文本 `IN_MEMORY` 模式，足以支持开发调试和 CI 测试。

### D8: 删除 cangjie-llvm-interop 的迁移计划

**删除范围**：
- `tools/cangjie-llvm-interop/` — 整个目录（C++ 源码、CMake 配置、README）
- `.github/workflows/build-cangjie-llvm-interop.yml` — CI 构建工作流
- `compiler/codegen` 中的进程外调用代码：
  - `NativeInteropLlvmBackendApi.kt`
  - `NativeInteropToolRunner.kt`
  - `NativeInteropToolLocator.kt`
  - `LlvmBackendKind.NATIVE_INTEROP` 枚举值
  - 相关测试

**保留内容**：
- `LlvmBackendKind.IN_MEMORY` — 纯文本降级模式（开发/测试用）
- `LlvmBackendFactory` — 重构为 `JNI` / `IN_MEMORY` 两种模式

## 风险 / 权衡

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 跨平台 LLVM 库版本/ABI 不一致 | 运行时崩溃或符号找不到 | 锁定 LLVM 18.x；CI 上对每个平台编译+测试；运行时版本检查 |
| JNI 内存泄漏（忘记 dispose） | 长时间运行的编译进程 OOM | AutoCloseable 强制模式；测试中检查泄漏；考虑 JUnit 扩展自动检测 |
| JNI 异常传播不透明 | LLVM 内部错误难以诊断 | JNI 层统一转换 LLVM 错误消息为 Java 异常；保留 LLVM 的 error handler |
| 删除 cangjie-llvm-interop 后无原生后备 | LLVM 环境缺失时只有纯文本模式 | IN_MEMORY 模式足以支持开发和 CI 验证；CI 预构建 JNI 产物供下载 |
| Gradle-native 编译插件开发成本 | 需要编写自定义 Gradle 插件 | 参考 Kotlin/Native 的 `NativePlugin` 实现，只实现最小子集 |
| LLVM API 变更（18→19 升级） | 绑定代码需要适配 | 只绑定稳定的 C API（不用 C++ API）；用版本宏条件编译差异部分 |
| 单次编译大量 JNI 调用的性能 | 可能比 Kotlin/Native 直接调用慢 | JNI Critical Native（JDK 内联优化）；批量操作 API；基准测试验证 |
