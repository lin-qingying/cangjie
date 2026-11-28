# llvm-interop/llvm-interop-jni/ — LLVM JNI 绑定实现

LLVM 互操作层的 JNI 绑定实现：负责原生库加载与 LLVM C API 桥接。Kotlin 侧声明 `external` 原生方法，并可将 `JniLlvmBindings` 安装为 `:llvm-interop:llvm-interop-api` 中 `LlvmBindings` 契约的实现；C++ 侧实现（本模块 `native/` 目录下的 `jni_*.cpp`）由 `nativeCompile` 任务编译为 `cangjie_llvm_jni` 动态库。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.llvm.jni` | `LlvmNative`（Kotlin 侧到 LLVM C API 的静态桥接方法、原生库加载状态与诊断）；`JniLlvmBindings`（`LlvmBindings` 的 JNI 生产实现）；`NativeLibraryLoader`（动态库加载，按“显式路径 → native home → classpath 资源 → 系统库名”顺序尝试）；`PlatformDetector`（`<os>-<arch>` 平台标识检测） |

## 原生代码

- C++ 源码位于本模块 `native/` 目录（`jni_*.cpp` / `jni_utils.h` / `CMakeLists.txt`），由 `nativeCompile`（`native-compile-plugin` 驱动）编译，需要本地 LLVM 开发库
- `aggregateNativeArtifacts` 将产物复制到 `build/native/<os>-<arch>/` 布局供打包与测试使用
- LLVM 查找优先级、跳过策略与集成测试开关详见 `../README.md`

## 依赖

- `:llvm-interop:llvm-interop-api`

## 命令

```bash
./gradlew :llvm-interop:llvm-interop-jni:assemble
./gradlew :llvm-interop:llvm-interop-jni:nativeCompile
./gradlew :llvm-interop:llvm-interop-jni:test
```

## 相关文档

- `../README.md` — 父目录总览（构建方式、运行 JNI 集成测试、与 codegen 集成）
