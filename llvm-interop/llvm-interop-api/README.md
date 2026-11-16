# llvm-interop/llvm-interop-api/ — LLVM 互操作纯 Kotlin API 层

仓颉编译器在 JVM 内调用 LLVM 的纯 Kotlin API 层：定义不透明句柄、上下文、模块、Builder、pass 管理器、目标机器与异常体系，不包含任何原生代码。具体绑定实现由兄弟子模块 `:llvm-interop:llvm-interop-jni` 通过 `LlvmBindings` 契约注入。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.llvm.api` | 不透明句柄（`LlvmContextRef` / `LlvmModuleRef` / `LlvmTypeRef` / `LlvmValueRef` / `LlvmBasicBlockRef` 等 value class）；`LlvmBindings` 绑定契约与进程内注册表；`LlvmContext` / `LlvmModule` / `LlvmBuilder` / `LlvmPassManager` / `LlvmTargetMachine` 资源封装（均 `AutoCloseable`）；`LlvmException` 异常体系（校验失败 / 后端不可用 / 版本不匹配） |

## 依赖

- 无项目内依赖（纯 Kotlin；仅测试依赖）

## 命令

```bash
./gradlew :llvm-interop:llvm-interop-api:assemble
./gradlew :llvm-interop:llvm-interop-api:test
```

## 相关文档

- `../README.md` — 父目录总览（构建方式、JNI 集成测试、与 codegen 集成）
