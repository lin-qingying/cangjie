# LLVM Interop Modules

`llvm-interop` 提供仓颉编译器在 JVM 内调用 LLVM 的基础设施，包含两个子模块：

- `llvm-interop-api`: 纯 Kotlin API 层，定义句柄、上下文、模块、Builder 和异常体系。
- `llvm-interop-jni`: JNI 绑定实现，负责原生库加载与 LLVM C API 桥接。

## 目录结构

```text
llvm-interop/
├── llvm-interop-api/
└── llvm-interop-jni/
    ├── src/main/native/      # JNI C++ 代码（Gradle nativeCompile 构建）
    ├── src/.../jni/          # Kotlin JNI 声明与加载器
    └── tests/...             # 单测和集成测试
```

## 构建方式

在仓库根目录执行：

```bash
./gradlew :llvm-interop:llvm-interop-jni:nativeCompile
./gradlew :llvm-interop:llvm-interop-jni:aggregateNativeArtifacts
```

`nativeCompile` 由 `native-compile-plugin` 驱动。LLVM 查找优先级：

1. `-Pllvm.dir=<path>`
2. 环境变量 `LLVM_DIR`
3. `llvm-config --prefix`
4. 平台默认路径

若找不到 LLVM 开发库，任务会跳过并输出告警，不阻断 Kotlin 编译。

## 运行 JNI 集成测试

集成测试默认关闭。启用方式：

1. 先完成 `nativeCompile`
2. 设置：
   - `CANGJIE_LLVM_JNI_INTEGRATION=true`
   - `CANGJIE_LLVM_JNI_LIBRARY_PATH=<native library absolute path>`
3. 执行：

```bash
./gradlew :llvm-interop:llvm-interop-jni:test --tests "org.cangnova.cangjie.llvm.jni.LlvmNativeIntegrationTest"
```

## 与 codegen 集成

`compiler/codegen` 通过 `LlvmBackendFactory` 在 `JNI` 与 `IN_MEMORY` 两种后端间选择：

- JNI 可用时优先使用 `JniLlvmBackend`
- 不可用时按配置降级到 `IN_MEMORY`
