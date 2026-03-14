## 1. 删除 cangjie-llvm-interop 及进程外后端

- [ ] 1.1 删除 `tools/cangjie-llvm-interop/` 整个目录（C++ 源码、CMake、README、.idea）
- [ ] 1.2 删除 `.github/workflows/build-cangjie-llvm-interop.yml` CI 工作流
- [ ] 1.3 删除 `compiler/codegen` 中的 `NativeInteropLlvmBackendApi.kt`、`NativeInteropToolRunner.kt`、`NativeInteropToolLocator.kt`
- [ ] 1.4 从 `LlvmBackendKind` 枚举中移除 `NATIVE_INTEROP` 值，只保留 `IN_MEMORY`（后续添加 `JNI`）
- [ ] 1.5 更新 `LlvmBackendFactory` 和 `LlvmBackendFactoryTest`，移除进程外后端相关逻辑和测试
- [ ] 1.6 验证现有 8 个对等基线测试在 `IN_MEMORY` 模式下全部通过（回归保护）

## 2. 模块脚手架与构建基础

- [ ] 2.1 创建 `llvm-interop/llvm-interop-api` Gradle 子模块，配置 Kotlin/JVM 编译，注册到 `settings.gradle.kts`
- [ ] 2.2 创建 `llvm-interop/llvm-interop-jni` Gradle 子模块，配置依赖 `llvm-interop-api`，注册到 `settings.gradle.kts`
- [ ] 2.3 在 `build-tools/` 中实现 `native-compile-plugin` Gradle 插件，提供 `nativeCompile {}` DSL（参考 Kotlin/Native 的 `NativePlugin`）
- [ ] 2.4 实现 LLVM 开发库自动检测逻辑（Gradle 属性 → 环境变量 → llvm-config → 默认路径）
- [ ] 2.5 实现平台检测与工具链选择（clang++ 优先，平台宏定义，链接选项差异）
- [ ] 2.6 在 `llvm-interop-jni/build.gradle.kts` 中应用 `native-compile-plugin`，配置 JNI 头文件生成和原生编译任务
- [ ] 2.7 创建 `src/main/native/CMakeLists.txt`（仅用于 IDE/CLion 代码导航，不参与构建）
- [ ] 2.8 验证 LLVM 不可用时原生构建任务优雅跳过，Kotlin 代码正常编译

## 3. 句柄类型与核心 API（llvm-interop-api）

- [ ] 3.1 定义 value class 句柄体系：`LlvmContextRef`、`LlvmModuleRef`、`LlvmTypeRef`、`LlvmValueRef`、`LlvmBasicBlockRef`、`LlvmBuilderRef`、`LlvmTargetMachineRef`，包含 `NULL` 常量和 `isNull` 检查
- [ ] 3.2 定义 `LlvmContext` 类（`AutoCloseable`），实现 Context 创建/关闭和子对象工厂方法（`createModule`、`createBuilder`）
- [ ] 3.3 定义 `LlvmModule` 类（`AutoCloseable`），实现 target triple/data layout 设置、函数添加、全局变量添加、IR 文本获取、模块验证
- [ ] 3.4 定义类型系统 API：在 `LlvmContext` 上提供 `int1Type`、`int8Type`…`int64Type`、`floatType`、`doubleType`、`voidType`、`ptrType`、`functionType()`、`namedStructType()`、`arrayType()` 属性/方法
- [ ] 3.5 定义 `LlvmBuilder` 类（`AutoCloseable`），声明所有核心指令方法：终止、算术/位运算、比较、内存、转换、call/phi/select/extractvalue/insertvalue
- [ ] 3.6 定义 `LlvmPassManager` 接口和 `LlvmTargetMachine` 接口
- [ ] 3.7 定义异常体系：`LlvmException`、`LlvmVerificationException`、`LlvmBackendUnavailableException`、`LlvmVersionMismatchException`
- [ ] 3.8 为所有 API 类添加单元测试（mock JNI 层，验证接口契约和 AutoCloseable 行为）

## 4. JNI 原生绑定层（llvm-interop-jni）

- [ ] 4.1 定义 `LlvmNative` Kotlin object，声明所有 JNI `external fun`（Context/Module/Type/Value/Builder/Analysis/BitWriter 系列）
- [ ] 4.2 实现 `jni_context.cpp`：`LLVMContextCreate`/`Dispose` 的 JNI 包装，Long↔指针转换
- [ ] 4.3 实现 `jni_module.cpp`：Module 创建/销毁、target triple/data layout 设置、IR 打印、验证
- [ ] 4.4 实现 `jni_types.cpp`：所有类型构造 API（整数、浮点、void、指针、函数、结构体、数组）
- [ ] 4.5 实现 `jni_values.cpp`：常量创建（ConstInt、ConstReal、ConstNull）、Value 查询（getName、getType）
- [ ] 4.6 实现 `jni_builder.cpp`：IRBuilder 所有指令（终止、算术、位运算、比较、内存、转换、call/phi 等）
- [ ] 4.7 实现 `jni_analysis.cpp`：`LLVMVerifyModule`/`LLVMVerifyFunction`，错误消息转 Java 异常
- [ ] 4.8 实现 `jni_bitwriter.cpp`：`LLVMWriteBitcodeToFile`/`LLVMWriteBitcodeToMemoryBuffer`
- [ ] 4.9 实现 JNI 层的字符串转换工具（`jstring`↔`const char*`）和错误处理宏
- [ ] 4.10 编写 JNI 集成测试：在有 LLVM 环境的 CI 上验证基本流程

## 5. 原生库加载与平台检测

- [ ] 5.1 实现 `NativeLibraryLoader`：按优先级加载 JNI 库（系统属性 → classpath 资源 → 系统路径），失败时输出诊断信息
- [ ] 5.2 实现 `PlatformDetector`：从 `os.name`/`os.arch` 映射到标准化 `<os>-<arch>` 标识符
- [ ] 5.3 在 `LlvmNative` 的 `init` 块中调用 `NativeLibraryLoader`，加载失败时设置标志
- [ ] 5.4 编写 `PlatformDetector` 和 `NativeLibraryLoader` 单元测试

## 6. 后端抽象层重构

- [ ] 6.1 定义 `LlvmBackend` 接口和 `LlvmBackendCapabilities` 数据类
- [ ] 6.2 实现 `JniLlvmBackend`：基于 `llvm-interop-jni` 的进程内实现，报告全能力
- [ ] 6.3 重构 `LlvmBackendFactory`：支持 `JNI`/`IN_MEMORY` 两种后端，JNI 不可用时降级到 IN_MEMORY
- [ ] 6.4 更新 `CodegenOptions`：`LlvmBackendKind` 改为 `JNI` 和 `IN_MEMORY`
- [ ] 6.5 更新 `DefaultChirToLlvmCodeGenerator`：适配新的 `LlvmBackend` 接口
- [ ] 6.6 编写后端工厂测试：覆盖 JNI 可用/不可用、降级策略、版本检查

## 7. CI 构建流水线

- [ ] 7.1 创建 `.github/workflows/build-llvm-jni.yml`，配置多平台构建矩阵（Linux/macOS/Windows × x86_64/aarch64）
- [ ] 7.2 实现构建步骤：安装 LLVM → Gradle nativeCompile → 上传原生库制品
- [ ] 7.3 添加制品聚合任务：将各平台原生库打包到 `native/<os>-<arch>/`
- [ ] 7.4 在 CI 中添加 JNI 集成测试步骤（至少覆盖 Linux x86_64）

## 8. 文档与验证

- [ ] 8.1 在 `llvm-interop/` 中创建 README.md，说明模块架构、构建步骤和使用方式
- [ ] 8.2 验证现有 8 个对等基线测试在删除进程外后端后全部通过
- [ ] 8.3 编写端到端验证：使用 JNI 后端从 CHIR 生成 LLVM IR，与文本拼接结果对比
